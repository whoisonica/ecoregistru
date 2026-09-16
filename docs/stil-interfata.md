# Stilul interfeței — direcția „Cântar”

*Decis de proprietar pe 15.09.2026, noaptea, din trei direcții desenate (Ghișeu · **Cântar** · Dosar), după ce
stilul „Prietenos” din aceeași zi a fost respins („arată ca orice SaaS făcut cu AI”). Documentul ăsta e regula
pentru orice ecran sau formular nou și pentru orice ecran vechi care se atinge. Dacă un ecran nou nu arată așa,
e un defect, nu o preferință.*

---

## De unde vine

Pornește de la cântarul de la colector și de la bonul lui: **panou grafit** cu taste și un **afișaj LCD**, pagina
**albă ca bonul**, cifre în mono aliniate în coloane, linii subțiri, nicio umbră. Verdele e cel de pe Punctul
Verde al ambalajelor. Nimic nu plutește: cutiile stau pe chenar, nu pe umbră; colțurile sunt mici, ca la un
aparat, nu rotunde ca la o aplicație de telefon.

Clientul care cumpără modulul de generator e patronul unei brutării, nu specialistul de mediu. Ecranele vorbesc
în limbajul lucrurilor pe care omul chiar le face („Adaugă deșeuri”, „Intrare”, „Ieșire”), fără să schimbe ce se
salvează sau ce se tipărește.

---

## Tokenii — unde stau și ce valori au

Nicio culoare, colț sau umbră nu se scrie de mână într-un ecran. Se folosesc clasele de mai jos.

| Ce | Unde | Valoare |
|---|---|---|
| Fonturi | `src/index.css` (`@font-face`), `tailwind.config.js` (`fontFamily`) | **IBM Plex Sans** (text) + **IBM Plex Mono** (cifre, CUI, coduri, date, etichete mici), servite din `public/fonts/` (OFL lângă fișiere), subseturi `latin` + `latin-ext`. **Nu** de pe Google Fonts: ar trimite IP-ul clientului unui terț |
| Mărimi de text | `tailwind.config.js` (`fontSize`) | `text-sm` = 14,5px, `text-xs` = 12,5px; tabelele la 14px (`table.tsx`). Cifrele cu `tabular-nums` peste tot (`body`) |
| Greutăți | `tailwind.config.js` (`fontWeight`) | `font-bold` cade pe **600**; 700 doar prin `font-extrabold`, pentru titluri mari |
| Pagina | `bg-surface` | **alb** `#FFFFFF`; `bg-surface-muted` `#F4F6F4` pentru rândul evidențiat, `bg-surface-sunken` `#E8ECE9` pentru câmpuri dezactivate |
| Linii | `border-line` / `border-line-strong` | `#E3E6E3` între rânduri / `#C9D0CC` la câmpuri și butoane; **linia de 2px `border-content`** sub capul de tabel și sub titlul unei secțiuni de formular |
| Text | `text-content` · `-strong` · `-muted` · `-subtle` | `#161A18` · `#2B322E` · `#5C6660` · `#7E8880` |
| Acțiunea principală | `bg-brand`, scara `brand-50…950` | Punctul Verde `#009A44`, apăsat `#007A36`, tasta `#00622F` |
| Intrarea în depozit | `bg-inbound`, `variant="inbound"` la `Button` | albastru `#1C6FD1` — cealaltă direcție față de verde |
| Panoul (bara stângă) | `bg-panel`, `-hover`, `-active`, `-line`, `-key`, `text-panel-text/-mid/-dim/-faint` | grafit `#1B201D`; intrarea activă `#2A322E` cu linie interioară de 3px `lcd-digit` la stânga |
| Afișajul LCD | `bg-lcd`, `text-lcd-digit/-unit/-warn/-bad` | `#0D1210`; cifra `#7CF2A9`, unitatea `#4FB57C`, galben `#FFB020`, roșu `#FF6B5E` |
| Stări | `Badge` (LED) · `text-state-ok/-warn/-bad-text` | **pătrățel de 8px + cuvânt**, nu pastilă colorată. Sensul nu se schimbă: verde = gata; **galben = o așteptare legitimă** (cântarul); **roșu = nu se poate depune așa** |
| Pubela | `BinSwatch` + `lib/binColor.ts` | pătrățel de 10×12px înaintea codului de deșeu: albastru hârtie, galben plastic/metal-ambalaje, verde sticlă, maro bio, gri rezidual, roșu periculoase, gri-metal fier vechi. **Listă explicită cod → pubelă, nu prefix ghicit**; codul necunoscut n-are culoare. Galbenul pubelei nu înseamnă „așteaptă” |
| Colțuri | scara `borderRadius` (înlocuită, nu extinsă) | 4–8px peste tot: `rounded-md` 5px butoane și câmpuri, `rounded-lg` 6px cutii; `rounded-xl/2xl/3xl` scrise în ecrane cad tot pe 6–8px. **Fără pastile.** `rounded-full` doar pentru avatar și rotița de încărcare |
| Umbre | `shadow-card` = **niciuna**; `shadow-popover` | umbra există doar pe ce plutește cu adevărat: popover, meniu, dialog |
| Tasta | `.key` + `.key-dark` / `.key-light` (`index.css`) | chenar de 1px, „grosime” de 3px jos, mono 10,5px. Butoanele o arată prin `hotkey="N"` |
| Eticheta mică | `.eyebrow` | mono 11px, majuscule, spațiere 0,07em — capul de tabel, eticheta unui total, luna de pe afișaj |

**Fără temă întunecată.** Panoul e grafit, pagina e albă — o singură lume de culori, cerută explicit.

---

## Panoul — bara din stânga (262px; 64px strâns cu `[`)

`components/panel/*`, orchestrate de `Layout.tsx`. De sus în jos:

1. Logo + tasta `[` (strânge la o șină de iconițe; ținută în `localStorage`, cu try/catch).
2. **Firma, ca eticheta de pe cântar** (`CompanyLabel`): nume, `CUI · tip` în mono. La consultant și platformă e
   buton → popover cu căutare, grupat „Cer atenție” (cu motivul) și „În regulă · N”.
3. **Afișajul lunii** (`MonthDisplay`): luna și ce măsoară („SEPTEMBRIE · INTRAT” la colector, „GENERAT” la
   generator), cifra în kg, iar jos **cel mai urgent lucru** — același verdict ca banda de pe Acasă, din aceeași
   socoteală (`useDashboardData`). O sursă căzută → „?”, nu „0”.
4. **Tastele de adăugare**, după tipul firmei: generator **+ Adaugă deșeuri** (N); colector **↓ Intrare** (I) ·
   **↑ Ieșire** (E); amândouă: **+ Deșeuri proprii** · Intrare · Ieșire. Ascunse fără drept de scriere.
5. „Caută oriunde · Ctrl K” — paleta.
6. **Meniul** (`lib/navItems.ts`): o tastă (1–9, 0) și un indicator pe fiecare intrare („1 de cântărit”, „1 fără cod
   R/D”, „2 depășite”, „2 expiră”). Fiecare ecran de lucru e intrare proprie: Acasă · Generare · Intrări · Ieșiri ·
   Ambalaje · Evidențe · Termene · Dosar de control · Parteneri · Setări; grupul **Cabinet** (F, C) la consultant și
   platformă. Import din Excel stă la Setări și în paletă; Abonament jos în panou.
7. Jos: Abonament (doar cine administrează) și contul, cu meniul Termeni · Confidențialitate · Scurtături · Deconectare.

**Telefon:** sus o bandă grafit cu firma, cifra lunii și alerta; jos bara **Acasă · lista principală · + · Termene ·
Mai mult** (`MobileBar`). „+” oferă doar ce e permis tipului de firmă.

**Tastele:** cifrele deschid intrările meniului; **N = acțiunea principală a ecranului curent** (pe ecranele fără
adăugare, N = adaugă deșeuri); I și E deschid formularele de intrare și ieșire de oriunde; `/` sare în căutarea
listei; `[` strânge panoul. Tastele tac în câmpuri de text (`useHotkey`).

---

## Primitivele — `frontend/src/components/ui`

| Primitivă | Când |
|---|---|
| `Button`, `LinkButton` | Orice acțiune. Colț de 5px, text 600. `default` = pasul principal al ecranului (unul singur, verde), `inbound` = intrarea (albastru), `outline` = celelalte, `ghost` = acțiunile de rând, `danger` = ce nu se ia înapoi. `hotkey="N"` arată tasta |
| `Input`, `Select`, `Textarea`, `DateInput` | Rubrici. Aceleași clase (`fieldClasses`): chenar de 1px, focus verde |
| `Badge` | Starea unui rând, ca LED: pătrățel + cuvânt („De cântărit”, „Fără cod R/D”, „Activ”) |
| `BinSwatch` | Pubela dinaintea unui cod de deșeu, pe liste și în formular |
| `Table` | Liste: fără cutie, cap de tabel mono cu linie de 2px, coloana de acțiuni fixată la dreapta (`sticky="right"`) |
| `Card`, `CardHeader` | Orice bloc de conținut: chenar de 1px, fără umbră |
| `Dialog` | Formulare scurte și confirmări. Pe telefon urcă de jos |
| `FormSection` | Titlul unei secțiuni dintr-un formular, cu linia de 2px sub el |
| `PageHeader` | Titlul paginii (26px, 600), o propoziție sub el, acțiunile la dreapta |
| `ChoiceCards` | O alegere între 2–6 variante, fiecare cu o propoziție. Radio nativ dedesubt |
| `PillGroup` | Răspunsuri scurte ca **taste**: cea apăsată e grafit cu text alb. Codul oficial stă mic lângă cuvinte |
| `Switch` | Un da/nu cu o propoziție |
| `Stepper` | Un formular împărțit pe întrebări. **Împarte, nu rearanjează** ordinea rubricilor |
| `SectionNav` | Cuprinsul unei pagini lungi, ca taste; secțiunea curentă e grafit |
| `Menu`, `MenuItem` | Un buton cu listă (descărcări, acțiunile rândului) |
| `Tooltip` | Orice explicație la cerere, pe grafit. **Niciodată `title=`**; și niciodată în jurul unui `Button` — bula își randează propriul buton |
| `Toast` | Mesaje scurte, pe grafit, ca de la aparat |

Select-ul nativ rămâne pentru liste lungi (parteneri, puncte de lucru, coduri R/D din profil). **Sub șapte opțiuni,
într-un formular nou, se folosesc `ChoiceCards` sau `PillGroup`.**

---

## Paginile de dinaintea contului — direcția „Poster”

*Aleasă de proprietar pe 16.09.2026 din trei machete (Aurora · **Poster** · Editorial), după ce trei variante în
stilul panoului (LCD, taste, bon) fuseseră respinse: pe login și pe cerere nu se pune „aparatul”.*

`components/PublicShell.tsx`, folosit de login și de cererea de cont. **Stânga, verde** (`from-brand-400 via-brand-700
to-brand-900`, cu o lumină difuză `lcd-digit` în colț): semnul, un titlu mare (56px pe login, 44px pe cerere), o
propoziție, apoi ce vrea pagina să arate — pe login trei tile-uri cu ecrane care chiar există (Anexa 1, 15 martie,
Dosarul de control), pe cerere pașii 01–03 (`PosterStep`, un `ol`). **Dreapta, alb**: formularul, cu rubrici de
48px (`h-12`), titlul de 32px, linkul spre cealaltă pagină în colț și subsolul juridic o singură dată. Pe telefon
verdele devine o bandă în capul paginii; loginul își ascunde tile-urile, cererea își ține pașii. Cererea începe cu
CUI-ul, „Ce faceți cu deșeurile” e `ChoiceCards` pe trei coloane, secțiunile sunt `FormSection size="lg"` (titlu de
20px, fără linia de 2px). Parola uitată și alegerea parolei rămân pe cardul vechi, de rescris când se atinge.

## Ecranele de mișcări

Trei ecrane, după registru și direcție (`lib/movementScreens.ts`): **Generare** (`/generare`, Anexa 1), **Intrări**
(`/intrari`, art. 48, direcția `IN`) și **Ieșiri** (`/iesiri`, art. 48, `OUT`). Colectorul pur nu vede Generare;
`/miscari` și `/intrari-iesiri` sunt redirecturi. Deasupra fiecărei liste stau **patru totaluri** socotite de server
peste toate rândurile filtrului (`GET /api/v1/movements/totals`), nu peste pagina adusă:

- Generare: generat · valorificat · eliminat · de cântărit
- Intrări: primit · de la firme · de la persoane fizice · de cântărit
- Ieșiri: plecat · valorificat · eliminat · fără cod R/D

Formularul e același (`MovementFormDialog`); ecranul îi dă registrul și direcția, deci pornește pe preluare la
Intrări și pe valorificare la Ieșiri. **Ordinea rubricilor nu se schimbă.**

---

## Cele douăsprezece reguli ale formularelor

1. **O întrebare pe ecran, cu verb.** „Cât ai predat?”, nu „Cantitate”. Pașii păstrează ordinea stabilită cu specialista.
2. **Taste în loc de liste derulante**, sub șapte opțiuni.
3. **Codul legii rămâne, dar mic.** „Container transportabil” mare, `CT` mic în mono. Pe hârtie se tipărește codul.
4. **„Ca data trecută” e un buton (varianta A, decizia proprietarului, 15.09.2026).** Aplică numai la clic. Nimic nu pornește bifat, nicio cifră nu se copiază.
5. **CUI-ul completează firma** (ANAF, `GET /api/v1/company-lookup/{cui}`), numai în rubricile goale.
6. **Poza în loc de tastat:** bonul de cântar, avizul, autorizația — prin atașamentele existente.
7. **Ciorna se salvează singură** (`useFormDraft`).
8. **„Nu știu” e un răspuns bun** acolo unde golul e valid, și are nume.
9. **Erori care spun ce să faci**, cu exemplu, la ieșirea din rubrică, nu abia la salvare.
10. **Explicația legală sub „De ce?”**, nu în titlu.
11. **Bonul din dreapta:** ce s-a completat și ce documente ies din răspunsuri.
12. **Salvarea spune pasul următor** („Tipărește Anexa 3”, „Încă una la fel”).

## Limbajul

- Textele stau în `frontend/src/lib/strings.ts`, în română, și numesc **ce face omul**: „Adaugă deșeuri”, „Intrare”, „Ieșire”, „Poate vinde metal”.
- Temeiul legal (actul, articolul) se păstrează, dar în explicație, nu în etichetă.
- Numeralul are trei forme (`1 linie · 2 linii · 20 de linii`) — trece prin `countOf` / `withCount`.

## Ce nu se schimbă, orice stil

- Ordinea rubricilor din formularul de mișcare (stabilită cu specialista).
- Documentele tipărite: numele din acte rămân pe hârtie. Limbajul de om e doar pe ecran.
- Nicio cifră ghicită sau precompletată pe un formular oficial.
- Roșu ≠ galben. Un indicator care n-a putut încărca arată „?”, nu „0”, și nu se ascunde.

---

## Cum se probează

`tsc` și build-ul nu văd cum arată un ecran. O schimbare de interfață se rulează prin `npm run e2e`
(`frontend/e2e/README.md`; proba 18 e a panoului), iar capturile din `frontend/e2e/shots/` se **deschid și se
privesc**, la 1440px, 1280px și 375px. `node e2e/shots-cantar.mjs` face capturi ale tuturor ecranelor, la ambele
lățimi, doar pentru privit. Cu IBM Plex textul e mai lat decât cu Nunito: un tabel care încăpea poate să nu mai
încapă — Generare a fost măsurat la 1440px (1114/1114) după ce „Editează” a devenit creion și „Adaugă cantitatea”
a devenit „Cântar”.
