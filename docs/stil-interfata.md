# Stilul interfeței — direcția „Cântar”

*Decis de proprietar pe 15.09.2026, noaptea, din trei direcții desenate (Ghișeu · **Cântar** · Dosar), după ce
stilul „Prietenos” din aceeași zi a fost respins („arată ca orice SaaS făcut cu AI”). Documentul ăsta e regula
pentru orice ecran sau formular nou și pentru orice ecran vechi care se atinge. Dacă un ecran nou nu arată așa,
e un defect, nu o preferință.*

---

## De unde vine

Pornește de la cântarul de la colector și de la bonul lui: **panou grafit** cu taste (afișajul LCD al lunii, din
prima formă, a fost scos din panou pe 18.09.2026 — negrul lui a rămas doar pe plăcuța firmei; banda de sus de pe telefon e grafit, ca panoul, cu cifrele în culorile afișajului), pagina
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
| Negrul de afișaj | `bg-lcd`, `text-lcd-digit/-unit/-warn/-bad` | `#0D1210` — plăcuța firmei (`CompanyLabel`); banda de telefon e `bg-panel` și ia de aici doar `text-lcd-digit/-warn/-bad`; `#7CF2A9` linia intrării active, galben `#FFB020` și roșu `#FF6B5E` la indicatori |
| Stări | `Badge` (LED) · `text-state-ok/-warn/-bad-text` | **pătrățel de 8px + cuvânt**, nu pastilă colorată. Sensul nu se schimbă: verde = gata; **galben = o așteptare legitimă** (cântarul); **roșu = nu se poate depune așa** |
| Pubela | `BinSwatch` + `lib/binColor.ts` | pătrățel de 10×12px înaintea codului de deșeu: albastru hârtie, galben plastic/metal-ambalaje, verde sticlă, maro bio, gri rezidual, roșu periculoase, gri-metal fier vechi. **Listă explicită cod → pubelă, nu prefix ghicit**; codul necunoscut n-are culoare. Galbenul pubelei nu înseamnă „așteaptă” |
| Colțuri | scara `borderRadius` (înlocuită, nu extinsă) | 4–8px peste tot: `rounded-md` 5px butoane și câmpuri, `rounded-lg` 6px cutii; `rounded-xl/2xl/3xl` scrise în ecrane cad tot pe 6–8px. **Fără pastile.** `rounded-full` doar pentru avatar și rotița de încărcare |
| Umbre | `shadow-card` = **niciuna**; `shadow-popover` | umbra există doar pe ce plutește cu adevărat: popover, meniu, dialog |
| Tasta | `.key` + `.key-dark` / `.key-light` (`index.css`) | chenar de 1px, „grosime” de 3px jos, mono 10,5px. Butoanele o arată prin `hotkey="N"` |
| Eticheta mică | `.eyebrow` | mono 11px, majuscule, spațiere 0,07em — capul de tabel, eticheta unui total |

**Fără temă întunecată.** Panoul e grafit, pagina e albă — o singură lume de culori, cerută explicit.

---

## Panoul — bara din stânga (262px; 64px strâns cu `[`)

*Forma „E2" din machetele „Meniul după fuziuni" (proprietarul a ales E3 pe 18.09.2026, apoi a scos pe localhost
„+"-urile de pe rânduri: „arată urâțel"), după ce „Evidențe" și
„Ambalaje" au intrat ca taburi în „Generare": generatorul rămăsese cu șase intrări într-un panou desenat pentru
unsprezece, iar până la primul rând de meniu stăteau cinci blocuri.*

`components/panel/*`, orchestrate de `Layout.tsx`. De sus în jos:

1. Logo, **căutarea ca iconiță** (paleta, Ctrl K) și tasta `[` (strânge la o șină de iconițe; ținută în
   `localStorage`, cu try/catch).
2. **Firma, pe plăcuța ei** (`CompanyLabel`, fond `bg-lcd`): nume, `CUI · tip` în mono. La consultant și platformă e
   buton → popover cu căutare, grupat „Cer atenție” (cu motivul) și „În regulă · N”.
3. **Meniul** (`lib/navItems.ts`): pe fiecare rând **tasta (1–9, 0), numele și indicatorul** („1 de cântărit”,
   „1 fără cod R/D”, „2 depășite”, „2 expiră”). **Fără iconiță cât panoul e lat** — tasta și iconița erau două semne de
   citit până la cuvânt; iconița rămâne pe șina strânsă, unde ține locul numelui. Fiecare ecran de lucru e intrare
   proprie: Acasă · Generare · Intrări · Ieșiri · Cântar · Termene · Dosar de control · Parteneri · Setări; grupul
   **Cabinet** (F, C, B) la consultant și platformă. Import din Excel stă la Setări și în paletă; Abonament jos în panou.
   „Ambalaje" e intrare proprie numai la firma care n-are „Generare" (colectorul pur).
4. **Taburile, sub intrarea deschisă** — Generare (Mișcări · Totalul anului · Ambalaje), Termene (De făcut · Bifate ·
   Trecute), Parteneri (Firme · Persoane fizice, numai cu depozit). **O singură listă, `lib/screenTabs.ts`, citită și de
   pagină, și de panou.** Tabul din meniu schimbă doar `?tab=`, ca `setTab` din pagină: luna, punctul și filtrele
   rămân. Cât taburile sunt desfăcute, indicatorul coboară pe primul: cifrele lui sunt despre lista implicită.
5. Jos: Abonament (doar cine administrează) și contul, cu meniul Termeni · Confidențialitate · Scurtături · Deconectare.

⚠️ **Ce a plecat și nu se pune la loc** (proprietarul, 18.09.2026: „nu îmi place"): **afișajul lunii** (LCD-ul cu
kilogramele și alerta), **tastele mari de adăugare**, rândul **„Caută oriunde"** și **orice „+" pe rândurile meniului** (încercat și scos în
aceeași seară). Din panou nu se adaugă nimic. Toate trei repetau ceva: cifra
lunii e pe Acasă („{an} pe luni"), iar alerta e banda de acolo; „Adaugă deșeuri" e în antetul ecranului de
mișcări (butonul de pe Acasă a fost scos și el, tot pe 18.09.2026), iar N / I / E merg de oriunde; căutarea e Ctrl K. Ce e de făcut se vede în panou din indicatorii de pe rânduri.

**Telefon:** sus o bandă grafit cu firma, cifra lunii și alerta; jos bara **Acasă · lista principală · + · Termene ·
Mai mult** (`MobileBar`). „+” oferă doar ce e permis tipului de firmă.

**Tastele:** cifrele deschid intrările meniului; **N = acțiunea principală a ecranului curent** (pe ecranele fără
adăugare, N = adaugă deșeuri); I și E deschid formularele de intrare și ieșire de oriunde, **numai la firmele care preiau de la
alții**; S = Setări când meniul trece de zece intrări; F / C / B în grupul Cabinet; `/` sare în căutarea listei; `[` strânge
panoul; Ctrl K deschide paleta (`CommandPalette`). Tastele tac în câmpuri de text (`useHotkey`).

---

## Primitivele — `frontend/src/components/ui`

| Primitivă | Când |
|---|---|
| `Button`, `LinkButton` | Orice acțiune. Colț de 5px, text 600. `default` = pasul principal al ecranului (unul singur, verde), `inbound` = intrarea (albastru), `outline` = celelalte, `ghost` = acțiunile de rând, `muted` = acțiunea care schimbă date lângă butoane de document („Recalculează acum”), `danger` = ce nu se ia înapoi, `danger-ghost` = ștergerea pe rând. `hotkey="N"` arată tasta |
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
| `SectionNav` | Cuprinsul unei pagini lungi, ca taste; secțiunea curentă e grafit (fișa firmei, `/clienti/:id`). **Nu** în Setări: acolo pagina de start are carduri pe grupuri, cu starea pe scurt, iar fiecare secțiune stă pe `/setari/<id>` (17.09.2026). **Nu** într-un tab: un cuprins înăuntrul unui tab e tab în tab — de asta a plecat de pe Ambalaje (18.09.2026) |
| `Menu`, `MenuItem` | Un buton cu listă (descărcări, acțiunile rândului) |
| `Tooltip` | Orice explicație la cerere, pe grafit. **Niciodată `title=`**; și niciodată în jurul unui `Button` — bula își randează propriul buton |
| `Toast` | Mesaje scurte, pe grafit, ca de la aparat |
| `LoadError` | Orice ecran sau bloc care n-a putut încărca: `role="alert"`, text în `state-bad`, „Încearcă din nou” (20.09.2026) |
| `PageTabs` | Taburile unui ecran (din `lib/screenTabs.ts`), cu filtrele pe aceeași linie în slotul `right` |
| `DocAction` | Butonul unui document, cu explicația în `Tooltip`, nu dedesubt |
| `TableToolbar`, `TableSearch`, `TablePagination`, `RowActions` | Căutarea (de la 10 rânduri), paginarea și meniul de rând al unui tabel (`table-toolbar.tsx`) |
| `MonthInput`, `Combobox`, `PasswordInput`, `FileDropzone` | Luna/anul, alegerea dintr-o listă lungă cu căutare, parola cu tăria ei, atașamentele (10 MB) |
| `EmptyState`, `Skeleton`, `TableFallbackRow` | Lista goală spusă în cuvinte; locul ținut cât se încarcă |
| `useConfirm` | Întrebarea de dinaintea unei fapte care nu se ia înapoi (`confirm-dialog.tsx`) |
| `FormStepRail` | Pașii unui formular lung (partenerul în patru pași, clientul nou), `form-steps.tsx` |

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

**Partenerul, pe patru pași** (`components/partners/PartnerFormDialog.tsx`, `FormStepRail` din `components/ui/form-steps.tsx`;
au fost trei până pe 17.09.2026, când punctele de lucru și șoferii au primit pasul lor): „Despre partener”
(CUI-ul întâi, cu ANAF, apoi denumirea cu sugestia de duplicat, adresa, Registrul Comerțului) · „Ce
face pentru tine” (tipul pe carduri; „Generator” nu apare la un cont de generator pur, care doar predă, decât pe un partener care îl are deja — „Doar le transportă” e tipul gol și pune singur bifa de transportator —, cine pe cine
facturează ca două comutatoare, proveniența ambalajelor ca taste, cu întrebarea „Cine duce deșeul de la tine la el?” pe două carduri imediat sub tip, cu licența de transport dedesubt — șoferii lui se scriu la pasul 4) · „Autorizația de
mediu” (sus numărul — obligatoriu la colector/valorificator — și „Viza e valabilă până la”; emiterea, decizia de viză
și bifa „Autorizație integrată veche, cu termen?” sub „Detalii de pe hârtii”, deschis singur la editare când are ceva;
temeiul sub „De ce?”; în listă, „Viză necompletată” galben — întrebarea BB) · „Puncte de lucru și șoferi” (punctele
de lucru ale partenerului și, numai la cine transportă, șoferii lui; din tabel, „+ Punct de lucru” / „+ Șofer” deschid direct pasul ăsta). În stânga dialogului stă cuprinsul: cifra
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
păstrează mesajul scurt. Formularul deschis prin `?nou=1` (butonul „+” de pe telefon, „Primii pași”, tastele N / I / E) ia punctul de
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

**Generare are trei taburi** (`GENERATION_TABS` din `lib/screenTabs.ts`; 18.09.2026, proprietarul: „și Generare și Evidența e la fel?"): **Mișcări** (lista de mai
sus, tabul implicit), **Totalul anului** și **Ambalaje** (mai jos). Totalul anului are un rând pe cod de deșeu, cu generat, valorificat, eliminat și starea lui
(„Gata" · „N de cântărit" · „N kg fără cod R/D"). Tabul stă în adresă (`?tab=total`), ca la Termene și Clienți.
**Filtrele tabului stau pe linia taburilor**, lipite la dreapta, cu eticheta mică lângă ele, nu deasupra (`PageTabs`,
slotul `right`; în afara lui `role="tablist"`).
⚠️ **Banda de taburi nu derulează** — taburile se rup pe două rânduri dacă nu încap. `overflow-x` diferit de `visible`
face și `overflow-y` să devină `auto` (regulă CSS), iar butoanele de tab ies 1px în jos (`-mb-px`, ca subliniera să
acopere chenarul): atât i-a trebuit ca macOS să deseneze un **indicator de derulare** lipit după ultimul tab, luat
drept buton. Cât banda ținea toată lățimea, firul cădea la marginea paginii și nu-l vedea nimeni; de când se termină
după ultimul tab, stă în mijlocul antetului. Liste derulante cu etichetă deasupra ocupau o bandă întreagă sub taburi.
La fel pe toate trei taburile „Generare": luna și punctul de lucru pe „Mișcări", anul și punctul pe „Totalul anului", doar anul pe
„Ambalaje" (punctul de lucru se alege din meniul butonului Anexa 3).
Intrări și Ieșiri n-au taburi, deci își păstrează banda.
**Tabelul totalului arată zece coduri pe pagină**, nu douăzeci și cinci ca restul: stă sub un antet înalt, iar rândul de
total — cifra pentru care se deschide tabul — ajungea sub marginea ecranului. ⚠️ Rândul de total rămâne al **anului**,
nu al paginii.
**Documentele stau sus, deasupra tabelului, niciodată sub el** (proprietarul: „butoanele de evidențele gestiunii sus, nu
ascunse jos"), un rând de butoane cu explicația fiecăruia dedesubt. Pe 15 martie omul vine după hârtie, nu după un
tabel; cu douăzeci de coduri, butoanele de dedesubt cădeau sub marginea ecranului. **Tabelul rămâne întreg pe toată
lățimea** — o coloană de documente lângă el îi fura din lățime și „arăta rău tăiat în dreapta".
**Butonul poartă numele documentului**, nu „Descarcă": „Evidența gestiunii deșeurilor" și „Evidența centralizată". Două
butoane la fel de anonime, unul lângă altul, sunt felul în care cineva depune hârtia greșită. Numele întreg din act
stă în explicația de dedesubt, în `aria-label` și pe hârtie.
⚠️ **Un buton care schimbă date nu se îmbracă la fel cu unul care dă un fișier.** „Recalculează acum" rescrie evidența,
și anii de după (stocul se reportează), deci stă **ultimul** pe rând și poartă **`variant="muted"`** — cutia lui, ca să
nu pară în aer, dar fundal stins și chenar subțire, ca să nu pară al treilea document. Fără chenar deloc plutea; cu
chenarul lor era apăsat de cine credea că scoate o hârtie.
**Fără coloană de stoc**, ca pe tot ecranul „Generare" de la `V58` încoace: generatorul n-are cântar și nu ține stoc,
deșeul stă în pubelă până vine colectorul, iar cantitatea se află abia la predare. Pe Anexa 1 cifra iese zero prin
construcție, iar o coloană de zerouri nu e o informație. „Rămasă în stoc" rămâne unde o cere actul: pe fișa tipărită
(HG 856/2002, anexa 1, cap. 1) și pe centralizator. Ecranul **„Evidențe" a fost scos**: `EvidenceCalculator` agregă
exact mișcările registrului `ANEXA_1`, adică exact rândurile ecranului de generare — două intrări în meniu pentru
același registru, cu aceleași filtre și aceleași butoane de document. `/evidente` e redirect (`EvidencesRedirect`):
`?problema=cod-rd` duce pe lista anului cu filtrul pus, restul pe tabul totalului. Registrul art. 48 nu se atinge:
rămâne pe Intrări / Ieșiri, cu „Evidența cronologică" în meniul lui.

**Și „Ambalaje" a intrat, ca al treilea tab** (18.09.2026, proprietarul: *„să scoatem mișcări din ambalaje și să facem
în generare ambalaje, iar în mișcări niște filtre frumoase"*). Același motiv: ecranul își ținea propriul registru cu
mișcările pe coduri `15 01 xx` — al treilea tabel de mișcări din aplicație, fără căutare și fără sortare la server,
peste aceleași rânduri ca „Generare" (`PackagingDeclarationBuilder` filtrează chiar registrul `ANEXA_1`). Ce a rămas
pe tab e ce nu se găsește altundeva: cele două tabele ale declarației, suprascrierea, Anexa 3 și documentul.
Întrebările registrului au devenit **tastele de deasupra listei de mișcări** — `PillGroup`, nu `Select`: *Toate
mișcările · Ambalaje · Ambalaj pus de noi pe piață · De completat*, scrise în adresă (`?ambalaje=…`) și cerute
serverului (`?packaging=ANY|ON_MARKET|INCOMPLETE`), cu banda de totaluri filtrată la fel.
⚠️ **Ce știe mișcarea despre ambalaj stă sub cod, nu în coloane proprii:** trei coloane noi duceau tabelul la 1415px
într-un 1114 (derulare laterală, la 1440). Și se arată **ori ce lipsește, ori ce e** — nu amândouă.
`/ambalaje` rămâne ecran întreg numai la firma fără „Generare"; la restul e redirect spre `?tab=ambalaje`.

**Un ecran de lucru nu se derulează** (18.09.2026, seara — proprietarul, la prima machetă a tabului „Ambalaje": *„să
nu meargă pagina în jos, să tot dai scroll"*). Ținta e **1440 × 900 fără derulare verticală**, și se măsoară
(`scrollHeight` față de `clientHeight`, pe `main#continut` **și** pe document — proba 38). Cum se ajunge acolo:
- **un singur tabel pe ecran**, ales din taste (`PillGroup`), cu alegerea în adresă — pe „Ambalaje": *Pus pe piață ·
  Predat* (`?tabel=predat`); ce trece de zece rânduri se **paginează**, ca pe „Totalul anului", nu se derulează într-o cutie;
- **documentele sunt butoane lipite, cu numele lor și fără text dedesubt** („Anexa 1 Ambalaje", „Anexa 3 Ambalaje"):
  termenul și formatul stau în meniul butonului (`MenuLabel`, `MenuItem.hint`), iar la Anexa 3 tot acolo se alege
  punctul de lucru — nu într-un filtru la mijlocul paginii;
- **semnalele sunt un rând cu linkuri**, nu o cutie cu titlu și listă: fiecare semnal e chiar linkul spre rândurile lui;
- **editarea se face în același tabel** („Scrie cifre proprii" face câmpuri din celule), nu într-o a doua grilă dedesubt;
- tabelul arată **doar rândurile cu cifre**; restul se numesc pe un rând gri sub el. Capul de tabel pe două niveluri
  („Primare: total · reutilizabile"), nu nume din formular rupte pe trei rânduri.

⚠️ **Tabelul Anexei 3 nu mai e pe tab la generator** (proprietarul: *„nu își are locul acolo, e ascunsă așa cumva"*):
erau aceleași predări ca în „Predat", iar documentul ei stătea la fundul paginii. A rămas butonul. Numai la firma care
**și colectează** apare a treia tastă, „Preluat de la alții" (preluările pe proveniență și avertismentul de rol nu se
văd nicăieri altundeva); tasta aceea are voie să deruleze. Nu se repropun: tasta „Ieșiri", tabele față în față,
explicații sub butoanele de document.

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
- ⚠️ **Explicația de sub un lucru e de un rând** (proprietarul, 18.09.2026: *„textele de sub chestii care explică
  sunt foarte lungi, prost aranjate și par neîngrijite"*). Măsura e lățimea în care cade textul, nu numărul de
  semne: sub un buton dintr-un rând de patru (≈270px) înseamnă **sub 40 de semne**; sub un titlu de secțiune
  (≈600px), sub 90. Etalonul e rândul de documente de pe „Totalul anului": *„Formularul din HG 856/2002, anexa 1."*,
  *„O pagină per punct de lucru."*, *„Pentru lucru, nu pentru depunere."*, *„Se recalculează din ce ai înregistrat."*
  — patru explicații, patru rânduri egale.
- Ce nu încape într-un rând nu se scrie mai mic, se mută: **detaliul de format** în `MenuItem` (`hint`),
  **motivul** în `Tooltip`, **temeiul întreg** în `docs/surse-oficiale.md`. Numele documentului stă pe buton și în
  `aria-label` — nu se repetă în explicația de sub el.
- Explicația spune ceva **nou** față de eticheta de deasupra. Dacă o repetă cu alte cuvinte, se șterge: sub tastele
  de filtrare („Ambalaje", „Ambalaj pus de noi pe piață") n-a mai rămas niciuna.

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
