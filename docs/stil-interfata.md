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
   Ambalaje · Termene · Dosar de control · Parteneri · Setări; grupul **Cabinet** (F, C) la consultant și
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
| `SectionNav` | Cuprinsul unei pagini lungi, ca taste; secțiunea curentă e grafit (Ambalaje). **Nu** în Setări: acolo pagina de start are carduri pe grupuri, cu starea pe scurt, iar fiecare secțiune stă pe `/setari/<id>` (17.09.2026) |
| `Menu`, `MenuItem` | Un buton cu listă (descărcări, acțiunile rândului) |
| `Tooltip` | Orice explicație la cerere, pe grafit. **Niciodată `title=`**; și niciodată în jurul unui `Button` — bula își randează propriul buton |
| `Toast` | Mesaje scurte, pe grafit, ca de la aparat |

Select-ul nativ rămâne pentru liste lungi (parteneri, puncte de lucru, coduri R/D din profil). **Sub șapte opțiuni,
într-un formular nou, se folosesc `ChoiceCards` sau `PillGroup`.**

---

## Paginile de dinaintea contului — direcția „Poster”, în paleta landingului

*Aleasă de proprietar pe 16.09.2026 din trei machete (Aurora · **Poster** · Editorial), după ce trei variante în
stilul panoului (LCD, taste, bon) fuseseră respinse: pe login și pe cerere nu se pune „aparatul”. Coloana verde
plină din machetă a fost și ea respinsă în aceeași seară („prea deranjantă”; „culorile din landing îmi plac”).*

`components/PublicShell.tsx`, folosit de login și de cererea de cont. **Stânga, pe hârtie deschisă**
(`surface-muted`, linie subțire spre dreapta): semnul, rândul mic verde cu linie de pe landing
(`strings.publicKicker`), un titlu mare negru (700, spațiere −0,03em; 56px pe login, 44px pe cerere) cu **ultima
frază în verdele adânc al mărcii** (`text-mark`, ca „Fără să le mai ții minte.” de pe landing), o propoziție, apoi
ce vrea pagina să arate — pe login trei tile-uri albe cu ecrane care chiar există (Anexa 1, 15 martie, Dosarul de
control), pe cerere cuprinsul formularului (`PosterStep`, un `ol`) și trei „fapte” cu punct verde (`PosterFact`).
**Dreapta, alb**: formularul, cu rubrici de 48px (`h-12`), titlul de 32px, linkul spre cealaltă pagină în colț și
subsolul juridic o singură dată. **Butoanele principale și linkurile de aici sunt în `mark`** (`publicButtonClass`),
verdele landingului `#047857`, nu `brand`-ul aplicației: cine vine de pe wastehouse.ro nu simte că a schimbat
site-ul. Tokenul `mark` (`DEFAULT` · `hover` · `soft`) stă în `tailwind.config.js` și e doar pentru paginile publice.
Pe telefon coloana din stânga devine o bandă în capul paginii; loginul își ascunde tile-urile, cererea își ține pașii.

**Cererea e în patru pași, aproape totul obligatoriu** (proprietarul, 16.09.2026, seara: „nu e prea lung?” la 22 de
rubrici pe o pagină; apoi, la blocul pliat cu opționale, „hai să obligăm omul să își facă și punct de lucru și tot ce
e sub «Detalii care ne scutesc de un telefon»”): „Detalii despre compania ta” (CUI, denumire, tipul ca `ChoiceCards`,
adresa sediului, CAEN) · „Punctul de lucru” (denumire, adresă, autorizația de mediu cu nr. și expirare — la generatorul pur cu bifa
„Activitatea noastră nu are nevoie de autorizație de mediu”, fiindcă Anexa 1 la Ordinul 1798/2007 o cere doar
activităților cu impact; bifa pleacă drept propoziție în observații; la colectori și transportul) · „Datele tale de contact” (nume, telefon, email, funcția) · „Deșeurile companiei tale” (etichetele
bifabile + rând liber, opționale; tipul de generator obligatoriu la cine generează; codurile R/D cu „Nu știu” ca
răspuns valid; observațiile libere). Fiecare „Continuă” își verifică pasul; trimiterea întoarce omul la pasul rubricii
greșite. Pașii din stânga sunt cuprinsul formularului: cel curent aprins, sub cei trecuți ce s-a completat. Titlurile
sunt prietenoase, nu întrebări („Cine sunteți?” a fost respins).

**Parola uitată și alegerea parolei** (17.09.2026) stau în același cadru, cu `split="half"` ca loginul: titlul din stânga
nu repetă titlul formularului („Parola se schimbă, evidența rămâne.” · „Încă un pas și intri în evidență.”), dedesubt trei
„fapte” (`PosterFacts`, ascunse pe telefon, ca tile-urile loginului). Starea de după trimitere înlocuiește formularul în
dreapta, cu pictograma în `mark` și titlul de 32px. Eroarea formularelor publice e `PublicError` (chenar `state-bad` cu
pătrățel), aceeași pe login și pe cele două pagini.

## Formularele din aplicație după cererea de cont (17.09.2026)

Proprietarul a cerut „fă 1 și 2, dar hai să le gândim frumos; nu schimba informația”. Ce s-a luat de la cererea de cont
e **felul de a întreba**, nu paleta paginilor publice: înăuntru rămâne „Cântar” (alb, `brand`, colțuri mici).

**Partenerul, pe trei pași** (`PartnersPage.tsx`, `FormStepRail` din `components/ui/form-steps.tsx`): „Despre partener”
(CUI-ul întâi, cu ANAF, apoi denumirea cu sugestia de duplicat, adresa, Registrul Comerțului, punctele de lucru) · „Ce
face pentru tine” (tipul pe carduri; „Generator” nu apare la un cont de generator pur, care doar predă, decât pe un partener care îl are deja — „Doar le transportă” e tipul gol și pune singur bifa de transportator —, cine pe cine
facturează ca două comutatoare, proveniența ambalajelor ca taste, cu întrebarea „Cine duce deșeul de la tine la el?” pe două carduri imediat sub tip, cu mașinile și șoferii lui dedesubt) · „Autorizația de
mediu” (sus numărul — obligatoriu la colector/valorificator — și „Viza e valabilă până la”; emiterea, decizia de viză
și bifa „Autorizație integrată veche, cu termen?” sub „Detalii de pe hârtii”, deschis singur la editare când are ceva;
temeiul sub „De ce?”; în listă, „Viză necompletată” galben — întrebarea BB). În stânga dialogului stă cuprinsul: cifra
în mono, pasul curent cu linia grafit, sub pașii completați ce s-a scris, pătrățel roșu pe pasul cu greșeala. Pe telefon,
`Stepper` sus, cu nume scurte. La adăugare „Continuă” își verifică pasul; la editare Salvează stă pe orice pas, iar linkul
„Autorizație expirată” deschide direct pasul 3. **Pașii sunt toți montați** (`hidden`), deci id-urile rubricilor și
valorile nu se pierd între pași; regulile și `PartnerInput` sunt cele de dinainte.

**„Adaugă deșeuri” rămâne pe o pagină** (se completează des; pașii ar fi doar clicuri în plus), cu **aceeași ordine a
rubricilor** și același `validate`/`buildInput`: titlurile de secțiune întreabă („Ce deșeu și când”, „Cât a fost”, „Cum
pleacă și unde ajunge”, „Cine îl preia”); sub șapte opțiuni, taste (`PillGroup`: unitatea, operațiunea, starea fizică,
mijlocul de transport, destinația, unitatea de pe Anexa 3, caseta „Destinat:”); „Unde pleacă deșeul” pe carduri; „Nu am
cântar” ca comutator. Listele lungi rămân `Select` (depozitarea, tratarea, codul R/D, partenerii, șoferii). În dreapta,
**bonul mișcării**: codul cu pubela, cantitatea, unde pleacă, către cine, documentul, apoi „Ce face mișcarea asta” — pe
ecran îngust rămân doar efectele, deasupra. **„La fel ca data trecută”** (regula 4) apare pe o mișcare nouă după ce se
alege codul, dacă există o mișcare cu același cod pe același ecran; la clic pune alegerile ei (ca „Duplică”), niciodată
cantitatea, data, documentul sau numărul Anexei 2.

**După salvare** (regula 12, 17.09.2026, `MovementSavedDialog.tsx`): o mișcare **nouă** nu mai închide doar dialogul cu un mesaj.
Apare „Predarea e în evidență” (Intrarea / Ieșirea), cu bonul citit din răspunsul serverului și, sub „Ce urmează”, exact
documentele pe care rândul le poate tipări — aceleași reguli ca meniul „⋯” (`canPrintAnexa3`, `canPrintAviz`,
`canPrintAnexa2`) — plus, la o predare fără cântar, unde se scrie cantitatea când vine bonul. „Încă una la fel” pornește
formularul cu alegerile ei (`sameAs`), fără cantitate, volum, cântărire, notițe, dată sau document; „Gata” închide. **Editarea**
păstrează mesajul scurt. Formularul deschis prin `?nou=1` (butonul „+” de pe telefon, panoul, „Primii pași”) ia punctul de
lucru implicit și când lista sosește după deschidere.

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

**Generare are două taburi** (18.09.2026, proprietarul: „și Generare și Evidența e la fel?"): **Mișcări** (lista de mai
sus, tabul implicit) și **Totalul anului** — un rând pe cod de deșeu, cu generat, valorificat, eliminat, ce a rămas în
stoc și starea lui („Gata" · „N de cântărit" · „N kg fără cod R/D"), plus cele două documente oficiale dedesubt. Tabul
stă în adresă (`?tab=total`), ca la Termene și Clienți. Ecranul **„Evidențe" a fost scos**: `EvidenceCalculator` agregă
exact mișcările registrului `ANEXA_1`, adică exact rândurile ecranului de generare — două intrări în meniu pentru
același registru, cu aceleași filtre și aceleași butoane de document. `/evidente` e redirect (`EvidencesRedirect`):
`?problema=cod-rd` duce pe lista anului cu filtrul pus, restul pe tabul totalului. Registrul art. 48 nu se atinge:
rămâne pe Intrări / Ieșiri, cu „Evidența cronologică" în meniul lui.

**Dosarul de control e locul hârtiilor.** Lista „Documentele anului" (`GET /api/v1/audit-file/contents`) are de acum
un buton pe fiecare rând: fișa, centralizata, Anexa 1 și Anexa 3 Ambalaje (`.xls` / PDF), plus rezumatele neoficiale
sub linie. Ce se naște numai înăuntrul arhivei — lista autorizațiilor, atașamentele — scrie „în arhivă"; ce nu se
aplică anului scrie „—", niciodată „în arhivă".

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
