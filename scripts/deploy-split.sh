#!/usr/bin/env bash
# Deployul pe Heroku prin repo-urile split, fără pașii de mână din `prompt-continuare.md`.
#
#   scripts/deploy-split.sh backend|frontend|both [--ref main] [--push]
#
# Ce face, pe fiecare parte:
#   1. `git subtree split` pe ref (implicit `main`) într-o ramură temporară cu nume unic;
#   2. GARDA: caută, de la vârf în jos, primul commit al split-ului al cărui CONȚINUT e egal cu
#      `newrepo/main` / `ferepo/main`, cu excepția divergenței stabile (backend: `.gitignore` și
#      newline-ul de la finalul lui `SecurityConfiguration.java`; frontend: `.gitignore`,
#      `vite.config.js`, `vite.config.d.ts`). Commiturile de deasupra lui sunt cele de deployat —
#      toate, în ordine, nu doar vârful (s-a plătit pe 09.09.2026). Dacă nu există un asemenea commit,
#      producția are ceva ce `ref` n-are (altă sesiune a deployat direct): scriptul se oprește;
#   3. cherry-pick într-un worktree temporar, pornit detașat din capul split-ului — fără ramurile
#      `split-*`, pe care le țin worktree-uri de-ale altor sesiuni;
#   4. garda încă o dată, pe rezultat;
#   5. cu `--push`, `git push <remote> HEAD:main` (Heroku pornește build-ul singur, prin webhook);
#      fără, scrie comanda. Worktree-ul și ramura temporară se șterg oricum la ieșire.
#
# Nu atinge `origin`: `main` și `deploy/heroku-split` se împing înainte, ca până acum.
set -euo pipefail

usage() { echo "folosire: $0 backend|frontend|both [--ref <commit>] [--push]" >&2; exit 2; }

[[ $# -ge 1 ]] || usage
target=$1; shift
ref=main
push=0
while [[ $# -gt 0 ]]; do
  case $1 in
    --ref) ref=${2:?}; shift 2 ;;
    --push) push=1; shift ;;
    *) usage ;;
  esac
done
case $target in backend|frontend|both) ;; *) usage ;; esac

repo=$(git rev-parse --show-toplevel)
cd "$repo"
stamp=$(date +%Y%m%d-%H%M%S)-$$
tmpdirs=()
tmpbranches=()
cleanup() {
  for d in "${tmpdirs[@]:-}"; do [[ -n $d ]] && git worktree remove --force "$d" >/dev/null 2>&1 || true; done
  for b in "${tmpbranches[@]:-}"; do [[ -n $b ]] && git branch -D "$b" >/dev/null 2>&1 || true; done
}
trap cleanup EXIT

# Fișierele care au voie să difere între split și repo-ul de pe Heroku.
allowed_for() {
  case $1 in
    backend) printf '%s\n' .gitignore src/main/java/ro/ecoregistru/config/SecurityConfiguration.java ;;
    frontend) printf '%s\n' .gitignore vite.config.js vite.config.d.ts ;;
  esac
}

# 0 dacă `a` și `b` diferă numai prin fișierele permise. SecurityConfiguration are voie să difere
# doar prin newline-ul de la final — orice altă schimbare în el e cod nedeployat.
same_content() {
  local part=$1 a=$2 b=$3 f allowed
  allowed=$(allowed_for "$part")
  while IFS= read -r f; do
    [[ -z $f ]] && continue
    if ! grep -qxF "$f" <<<"$allowed"; then return 1; fi
    if [[ $f == */SecurityConfiguration.java ]]; then
      [[ "$(git show "$a:$f" 2>/dev/null)" == "$(git show "$b:$f" 2>/dev/null)" ]] || return 1
    fi
  done < <(git diff --name-only "$a" "$b")
  return 0
}

deploy_part() {
  local part=$1 remote
  case $part in backend) remote=newrepo ;; frontend) remote=ferepo ;; esac

  echo "━━━ $part → $remote/main"
  git fetch -q "$remote"
  local split=tmp-split-$part-$stamp
  tmpbranches+=("$split")
  git subtree split --prefix="$part" -b "$split" -q "$ref" >/dev/null

  local base="" c n=0
  for c in $(git rev-list --first-parent "$split"); do
    if same_content "$part" "$c" "$remote/main"; then base=$c; break; fi
    n=$((n + 1))
    [[ $n -ge 200 ]] && break
  done
  if [[ -z $base ]]; then
    echo "✗ GARDA: niciun commit din $ref/$part nu are conținutul de pe $remote/main." >&2
    echo "  Producția are ceva ce $ref n-are (un deploy făcut din altă ramură?). Vezi: git log -3 $remote/main" >&2
    return 1
  fi

  local pending
  pending=$(git rev-list --reverse "$base..$split")
  if [[ -z $pending ]]; then
    echo "✓ nimic de deployat: $remote/main are deja conținutul lui $ref"
    return 0
  fi
  echo "de deployat, în ordine:"
  git log --reverse --format='  %h %s' "$base..$split"

  local wt
  wt=$(mktemp -d "${TMPDIR:-/tmp}/deploy-$part-XXXXXX")
  tmpdirs+=("$wt")
  git worktree add -q --detach "$wt" "$remote/main"
  # shellcheck disable=SC2086
  git -C "$wt" cherry-pick $pending >/dev/null

  if ! (cd "$wt" && same_content "$part" HEAD "$split"); then
    echo "✗ GARDA după cherry-pick: rezultatul diferă de $split în afara divergenței stabile." >&2
    git -C "$wt" diff --stat HEAD "$split" >&2
    return 1
  fi
  echo "✓ garda curată după cherry-pick ($(git -C "$wt" rev-parse --short HEAD))"

  if [[ $push -eq 1 ]]; then
    git -C "$wt" push "$remote" HEAD:main
    echo "✓ împins; releaseul apare în Heroku în ~30 s (backend ~1 min)"
  else
    echo "  pentru deploy: rulează din nou cu --push"
  fi
}

status=0
if [[ $target == backend || $target == both ]]; then deploy_part backend || status=1; fi
if [[ $target == frontend || $target == both ]]; then deploy_part frontend || status=1; fi
exit $status
