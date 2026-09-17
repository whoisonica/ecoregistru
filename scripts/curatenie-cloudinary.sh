#!/usr/bin/env bash
# Șterge de pe Cloudinary toate fișierele aplicației: atașamentele mișcărilor și buletinele de analiză,
# adică tot ce stă sub folderul `app.cloudinary.folder` (implicit `ecoregistru`). Pasul 2.6 din
# ecoregistru-docs/docs/plan-curatenie-prod-demo.md — DUPĂ curățenia bazei și după ce aplicația merge,
# fiindcă fișierele șterse nu se mai recuperează (backupul Heroku ține doar baza).
#
#   CLOUDINARY_URL=$(heroku config:get CLOUDINARY_URL -a ecoregistru-api) scripts/curatenie-cloudinary.sh --dry-run
#   CLOUDINARY_URL=$(heroku config:get CLOUDINARY_URL -a ecoregistru-api) scripts/curatenie-cloudinary.sh
#
# `--dry-run` numără fișierele din prefix pe fiecare resource_type/type și listează folderele de la
# rădăcina contului, ca să se vadă că în cont nu stă altceva care ar trebui păstrat. Se șterge numai
# prefixul, nimic din afara lui. Logourile cabinetelor sunt în bază (V48), nu aici.
set -euo pipefail

: "${CLOUDINARY_URL:?lipsește CLOUDINARY_URL}"
folder=${CLOUDINARY_FOLDER:-ecoregistru}
[[ $folder =~ ^[A-Za-z0-9_-]+$ ]] || { echo "folder invalid: $folder" >&2; exit 2; }
prefix="$folder/"
dry=0
case "${1:-}" in
  --dry-run) dry=1 ;;
  "") ;;
  *) echo "folosire: $0 [--dry-run]" >&2; exit 2 ;;
esac

# cloudinary://<key>:<secret>@<cloud>
creds=${CLOUDINARY_URL#cloudinary://}
key=${creds%%:*}
secret=${creds#*:}; secret=${secret%@*}
cloud=${creds#*@}
api="https://api.cloudinary.com/v1_1/$cloud"

call() { curl -fsS -u "$key:$secret" "$@"; }
json() { python3 -c "import sys, json; d = json.load(sys.stdin); $1"; }

echo "Folderele de la rădăcina contului $cloud:"
call "$api/folders" | json 'print("\n".join("  " + f["path"] for f in d.get("folders", [])) or "  (niciunul)")'

total=0
for rtype in image raw video; do
  for dtype in authenticated upload private; do
    # numărătoarea, paginată (max 500 pe pagină)
    n=0 cursor=""
    while :; do
      page=$(call -G "$api/resources/$rtype/$dtype" --data-urlencode "prefix=$prefix" -d max_results=500 \
        ${cursor:+--data-urlencode "next_cursor=$cursor"})
      n=$((n + $(json 'print(len(d.get("resources", [])))' <<<"$page")))
      cursor=$(json 'print(d.get("next_cursor") or "")' <<<"$page")
      [[ -n $cursor ]] || break
    done
    [[ $n -gt 0 ]] || continue
    echo "$rtype/$dtype: $n fișiere sub $prefix"
    total=$((total + n))
    [[ $dry -eq 1 ]] && continue

    # ștergerea: max 1000 pe apel; `partial: true` = mai sunt, se reapelează
    while :; do
      res=$(call -X DELETE -G "$api/resources/$rtype/$dtype" --data-urlencode "prefix=$prefix")
      json 'print("  șterse:", sum(1 for v in d.get("deleted", {}).values() if v == "deleted"), "| partial:", d.get("partial"))' <<<"$res"
      [[ $(json 'print("1" if d.get("partial") else "")' <<<"$res") == 1 ]] || break
    done
  done
done

if [[ $dry -eq 1 ]]; then
  echo "TOTAL: $total fișiere sub $prefix (dry-run, nu s-a șters nimic)"
else
  echo "TOTAL: $total fișiere găsite și trimise la ștergere. Rulează din nou cu --dry-run: trebuie să iasă 0."
fi
