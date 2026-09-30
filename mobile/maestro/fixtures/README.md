# Avize de probă — toate INVENTATE

Nicio poză de client aici (regula: fără fișiere de client în repo). Un CUI de pe un aviz de probă trebuie să aibă
cifra de control corectă și să **nu existe la ANAF** — `28104567`, folosit până pe 26.09.2026, era al unui PFA real.

| Fișier | Pentru ce |
|---|---|
| `aviz-partener.png` | `m1b-aviz.yaml`: cumpărătorul e partenerul demo Eco Valorificare SA (`RO 99887766`, din `DevDataSeeder`), 15 01 02, 1.250 kg, seria DRS nr. 000417. Se pune în galerie chiar înainte de flux: fluxul alege cea mai nouă poză. Tot el pentru `proba-60s.yaml` (două predări cronometrate) și `b3-b5-ciorna-poza.yaml` (ciorna cu poză). |
| `aviz-necunoscut.png` | `m1b-cui-necunoscut.yaml`, CUI-ul care nu e printre parteneri: „Caută la ANAF” → „ANAF nu găsește CUI-ul ăsta” (fluxul se oprește acolo; „Adaugă partenerul” apare doar pentru un CUI găsit la ANAF). Cumpărătorul Eco Deal SRL, `CUI: RO 99900010` (404 la ANAF), nu e în lista demo. Se pune ULTIMUL în galerie. |

`f7-repeta-poza.yaml` ia oricare poză din galerie (o pune pe o predare deja salvată).

Randarea, din folderul acesta:

    "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" --headless=new --hide-scrollbars \
      --window-size=1200,1300 --screenshot="$PWD/aviz-<nume>.png" "file://$PWD/aviz-<nume>.html"

Pus în galerie: `xcrun simctl addmedia booted aviz-<nume>.png` (iOS), `adb push aviz-<nume>.png /sdcard/Pictures/`
și scanarea media (Android).
Pe iOS, o poză cu aceiași octeți deja pusă în galerie nu devine „cea mai nouă” la o nouă adăugare (27.09.2026: fluxul a luat
avizul partenerului în locul celui necunoscut). Între rulări se pune o copie cu octeții schimbați, de pildă cu un sufix după `IEND`:

    f=/tmp/aviz-<nume>-$(date +%s).png; cp aviz-<nume>.png "$f" && printf run >> "$f" && xcrun simctl addmedia booted "$f"

Pe Android, `adb push` păstrează data fișierului din repo, iar galeria ordonează după ea: se atinge poza pe emulator înainte
de scanare, cu un nume nou la fiecare rulare:

    n=aviz-<nume>-$(date +%s).png; adb push aviz-<nume>.png /sdcard/Pictures/$n && adb shell touch /sdcard/Pictures/$n \
      && adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Pictures/$n
