#!/usr/bin/env bash
# Exportul complet al datelor unei firme, la încetarea contractului (DPA §10.1, contract art. 14.1).
#
#   DATABASE_URL=… CLOUDINARY_URL=… scripts/export-client.sh <id-firmă sau CUI> [director]
#
# Pe producție: DATABASE_URL=$(heroku config:get DATABASE_URL -a ecoregistru-api)
#               CLOUDINARY_URL=$(heroku config:get CLOUDINARY_URL -a ecoregistru-api)
#
# Ce scrie în director (implicit `export-<CUI>-<data>`):
#   - câte un CSV pe tabel, cu toate rândurile firmei, dintr-o singură tranzacție read-only (aceeași
#     fotografie a bazei pentru toate). Tabelele cu `company_id` se descoperă din schemă, deci o
#     tabelă nouă intră singură; pe cele fără `company_id` le leagă `LINKED` de mai jos.
#   - `fisiere/`: atașamentele mișcărilor și buletinele de analiză, luate de pe Cloudinary cu URL
#     semnat (ca `CloudinaryStorageService#signedUrl`); fiecare fișier începe cu id-ul rândului lui.
# Nu intră: parolele utilizatorilor, sesiunile și tokenurile, abonamentul și plățile (sunt ale
# WasteHouse, facturile vin din FGO). Documentele tipărite (fișa, anexele) se refac din date sau
# se descarcă din aplicație în cele 90 de zile de doar-citire.
#
# Exportul are CNP-uri (șoferi, persoane fizice, avize): se trimite arhivat cu parolă
# (`zip -er`), parola pe alt canal.
set -euo pipefail

[[ $# -ge 1 ]] || { echo "folosire: $0 <id-firmă sau CUI> [director]" >&2; exit 2; }
: "${DATABASE_URL:?lipsește DATABASE_URL}"
: "${CLOUDINARY_URL:?lipsește CLOUDINARY_URL}"
key=$1
[[ $key =~ ^[A-Za-z0-9-]+$ ]] || { echo "id sau CUI invalid: $key" >&2; exit 2; }

export PGOPTIONS='-c default_transaction_read_only=on'
q() { psql "$DATABASE_URL" -X -v ON_ERROR_STOP=1 -Atq "$@"; }

company=$(q -F '|' -c "select id, cui from companies where id::text = '$key' or cui = '$key' or cui = 'RO$key'")
[[ -n $company && $(wc -l <<<"$company") -eq 1 ]] || { echo "firma „$key” nu există sau nu e unică" >&2; exit 1; }
cid=${company%%|*}
cui=${company#*|}
out=${2:-export-${cui:-$cid}-$(date +%F)}
mkdir -p "$out/fisiere"

# Tabelele fără company_id, legate prin părinte. `companies` și `app_users` au coloane alese.
LINKED="
companies|select * from companies where id = '$cid'
app_users|select id, email, role, first_name, last_name, enabled, created_at, deactivated_at from app_users where company_id = '$cid'
partner_work_points|select w.* from partner_work_points w join partners p on p.id = w.partner_id where p.company_id = '$cid'
attachments|select a.* from attachments a join waste_movements m on m.id = a.movement_id where m.company_id = '$cid'
waste_movement_transport_destinations|select d.* from waste_movement_transport_destinations d join waste_movements m on m.id = d.waste_movement_id where m.company_id = '$cid'
waste_codes|select * from waste_codes"
SKIP="app_users subscriptions"

tables=$(q -c "select table_name from information_schema.columns
               where table_schema = 'public' and column_name = 'company_id' order by 1")
sql="begin isolation level repeatable read read only;"
while IFS='|' read -r t select; do
  [[ -n $t ]] && sql+=$'\n'"\\copy ($select) to '$out/$t.csv' csv header"
done <<<"$LINKED"
for t in $tables; do
  [[ " $SKIP " == *" $t "* ]] && continue
  sql+=$'\n'"\\copy (select * from $t where company_id = '$cid') to '$out/$t.csv' csv header"
done
# `analysis_bulletins` (V38) e tabelă istorică: buletinele de analiză au fost închise prin decizia
# proprietarului şi n-au entitate în aplicaţie, deci nu se mai scrie nimic în ea. Rămâne citită aici
# fiindcă exportul promis în DPA §10.1 e „tot ce ţine de client”, iar o bază veche poate avea rânduri;
# pe una nouă `union all` întoarce zero şi nu costă nimic. Decizia din 18.09.2026: rămâne, nu se
# şterge cu migrare.
files="coalesce(public_id, ''), coalesce(resource_type, ''), coalesce(delivery_type, ''), coalesce(format, ''), coalesce(url, ''), coalesce(translate(file_name, E'\\t/', ' _'), '')"
sql+=$'\n'"\\copy (select id, $files from attachments where movement_id in (select id from waste_movements where company_id = '$cid') union all select id, $files from analysis_bulletins where company_id = '$cid') to '$out/.fisiere.tsv' (delimiter E'\x1f')"
sql+=$'\n'"commit;"
q <<<"$sql"

# cloudinary://<key>:<secret>@<cloud>
creds=${CLOUDINARY_URL#cloudinary://}
secret=${creds#*:}; secret=${secret%@*}
cloud=${creds#*@}

failed=0
while IFS=$'\x1f' read -r id public_id rtype dtype format url name; do
  if [[ -n $public_id ]]; then
    source=$public_id${format:+.$format}
    sig=$(printf '%s%s' "$source" "$secret" | openssl dgst -sha1 -binary | base64 | tr '+/' '-_' | cut -c1-8)
    url="https://res.cloudinary.com/$cloud/${rtype:-image}/${dtype:-authenticated}/s--$sig--/v1/$source"
  fi
  curl -fsS "$url" -o "$out/fisiere/$id-${name:-fisier}" || { echo "NU s-a descărcat: $id ($name)" >&2; failed=$((failed + 1)); }
done <"$out/.fisiere.tsv"
rm "$out/.fisiere.tsv"

echo "Export în $out: $(ls "$out"/*.csv | wc -l | tr -d ' ') tabele, $(ls "$out/fisiere" | wc -l | tr -d ' ') fișiere, $failed eșuate."
[[ $failed -eq 0 ]]
