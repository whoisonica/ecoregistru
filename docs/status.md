# Stadiu — EcoRegistru

Jurnalul feliilor livrate, în ordinea în care au fost construite. Fiecare intrare marcată ✅
rulează local și are testele verzi.

> 📚 **Jurnalul de dinainte de 15.09.2026** (feliile din 22.08–14.09, cu G-1…G-10, P0, 11-bis, P1.10–P1.12, P3.1 și
> auditul QA) e în [`istoric/status-pana-la-14.09.2026.md`](istoric/status-pana-la-14.09.2026.md). Intrările din **15–16.09.2026** (depozitul F1–F2,
> mobilul M0–M1c, abonamentele F1–F4, „Cântar”, V58–V62, BUG-017…023) sunt în
> [`istoric/status-15-16.09.2026.md`](istoric/status-15-16.09.2026.md). Aici rămân doar
> intrările noi; o intrare nouă se scrie tot în capul acestui fișier.
>
> ⚠️ **E un jurnal: fiecare intrare spune ce era adevărat în ziua ei.** Un „⬜”, un „nedeployat” sau o cifră de teste dintr-o intrare veche
> nu descriu starea de azi — starea de azi e intrarea cea mai de sus. Pe 19.09.2026 antetele intrărilor au fost comparate, una câte una,
> cu `heroku releases`; unde scriau altceva decât Heroku (patru intrări din 17–18.09), au fost corectate pe loc, cu mențiunea a ce scria înainte.


> **20.09.2026, noaptea — ✅ cele cinci observații de pe producție, BUG-053 și G06** (nedeployat la scrierea intrării; fără migrații, schema rămâne **V63**, liberă **V64**). Măsurat pe rulare curată: backend **1057 de teste / 132 de clase, 0 căzute, 5 sărite**; `npm test` **66/66**; `tsc` curat; `npm run build` curat, bundle **997 kB** (gzip 284).
>
> **Cele cinci observații.** „Totalul anului" și Acasă nu se reîmprospătau după salvare — `useMovements` invalida doar `["movements"]`, nu și `["evidences"]`; reparat **în `invalidateAll`**, funcția prin care trec toate mutațiile, nu la apelanți. Codurile periculoase primesc `*` în „Totalul anului" (documentele îl aveau deja corect). Acasă spune „și încă N … kg", ca lista să se potrivească cu graficul — rândul stă **în afara `<ul>`**, ca numărătoarea din proba 39 să rămână cea cerută. Jurnalul de audit scria „Cantitate: 400.000 → 400" la orice salvare: `AuditInterceptor` compară valorile ca **șiruri**, iar `BigDecimal.toString()` păstrează scara (baza dă `NUMERIC(…,3)`, formularul trimite scara lui) — normalizat cu `stripTrailingZeros().toPlainString()` **în formatorul comun**, deci pentru toate cele 13 tipuri auditate; probă nouă `audit/AuditInterceptorFormatTest`. A cincea — `?luna=2026` pe Mișcări — **nu s-a reprodus**: fiecare treaptă acceptă anul (`isMonthValue` cu `YEAR_PATTERN`, `allowWholeYear` pe ambele filtre, `if (m) f.month`, iar serverul face fereastra 1 ian–31 dec), cu probă verde `MovementListFilterIT.yearAloneMeansTheWholeYear`; nota pornea de la o presupunere.
>
> **BUG-053 — avizul trece pe `CAN_WRITE`** (decizia proprietarului). Legea cere CNP-ul pe document, dar listele îl maschează pentru „Vizualizare": cât timp PDF-ul rămânea deschis, mascarea era decorativă. Nu se pierde nimic — avizul îl tipărește cel care predă deșeul, adică cel care scrie mișcarea. Proba `@Disabled` e reactivată și cere 403. **Pe același drum:** butoanele de Anexa 3 **și** aviz se vedeau la vizualizator și dădeau 403 la clic; `canPrintAnexa3`/`canPrintAviz` primesc `canWrite` ca **parametru obligatoriu**, ca un ecran nou să nu-l uite. **Nu mai există niciun test dezactivat pe suprafața generatorului.**
>
> **G06 — zecimalele.** Cele patru scrieri ale aceleiași cantități nu erau patru scăpări, ci **patru modele**: `Anexa1FormGenerator` cere punct „exactly as every filled sheet prints them", `Anexa2FormGenerator` cere virgulă fiindcă acolo totul e în tone. A le uniformiza ar fi deviat fișa de la cele 33 de foi completate și ar fi redeschis validarea specialistei. S-a păstrat **invariantul**, nu separatorul: *trei zecimale întotdeauna*, deci virgula e mereu ultima și punctul mereu la mii — „35.125" nu mai poate fi citit ca „35,125". Formularele cu model îl respectau deja; s-au aliniat doar suprafețele **fără** model: `formatKg` (ecran) și `GenericEvidenceExporter` (evidența cronologică — art. 48 n-are formular tipărit). Tot atunci s-au **desființat trei formatoare duplicate** de kilograme (`AnnualTotals`, `packagingFormat` — „totalul" din G06 avea propriul `Intl.NumberFormat`, deci schimbarea din `lib/units` singură nu l-ar fi atins). Rămân cu rotunjire la kilogram întreg, dinadins, banda de totaluri și panoul: sunt rezumate, nu cifre de transcris.
>
> **Destinația deșeului, întreagă și pe tabere** (20.09.2026, proprietarul). Ecranul oferea patru valori din nota 5 — `DO`, `I`, `Vr`, `A` — fiindcă atât se dăduseră ca **exemplu** pe 16.09, iar exemplul rămăsese în cod ca filtru: cine valorifică în propria întreprindere (`P`), are haldă proprie (`HP`) sau dă la valorificare energetică (`Ve`) n-avea ce alege, deși enumul le avea și nota se tipărește verbatim. Acum se oferă **toate opt**, grupate după operațiune: la un cod **R** — `Vr`, `P`, `Ve`, `A`; la un cod **D** — `DO`, `HP`, `HC`, `I`, `A`; până se alege soarta deșeului, toate. Gruparea e **textul notei 5**, nu o deducție: `I` scrie „Incinerarea în scopul **eliminării**", `Vr`/`Ve` scriu „**Valorificare** …"; `A` stă în amândouă. Schimbarea operațiunii golește o destinație rămasă din tabăra cealaltă, iar o mișcare veche se deschide cu valoarea ei neatinsă. Regula stă în `movementRules.destinationsFor`, cu probă proprie (reuniunea taberelor acoperă toate cele opt coduri). Serverul **nu** verifică încă potrivirea — ar atinge rândurile deja salvate. Închide jumătatea a doua a întrebării **BD**.
>
> **20.09.2026, 00:35 — ✅ PE PRODUCȚIE (`ecoregistru-api` v130, `ecoregistru-app` v129; monorepo `7c0673b`): reparațiile din QA-ul de lansare a generatorului, BUG-031…066** (fără BUG-053, care aștepta decizia). Fără migrații: schema **V63**, liberă **V64**. Backend 1053/6 sărite/0, `npm test` 65, e2e 42/42. Detaliile, în repo-ul privat de documentație (`qa/`).

> **18.09.2026, ~23:55 — documentația, recitită față de cod (fără cod de aplicație, fără deploy; producția rămâne `ecoregistru-api` v129 / `ecoregistru-app` v127, schema V63, liberă V64).**
> Proprietarul: *„citește atent tot codul și toate md files… dacă găsești 2 informații diferite în md-uri caută în cod sau oficial, nu presupune"*.
> **Măsurat, nu copiat:** `./gradlew cleanTest test` → **934 de teste / 113 clase, 0 eșecuri, 3 sărite**, zero avertismente `[removal]` (intrarea de la 16:22 scria „932+2 / 112"; clasele sunt 113, cu `PackagingMovementFilterIT`); `npm test` **61/61**; `npm run build` curat, bundle **993 kB** (gzip 283 kB); **39 de probe** în `e2e/run.mjs` = 39 de fișiere; ce rulează pe dyno-uri e egal cu `main` (`git diff --stat main:frontend ferepo/main` → doar `.gitignore`; backend → `.gitignore` + newline-ul din `SecurityConfiguration.java`).
> **Contradicții închise din cod:** `surse-oficiale.md` (§1.2, §1.3 pct. 1) și capul lui `legislatie.md` spuneau că `TreatmentPurpose` n-are `E` — are `V` și `E` din 16.09.2026; `stil-interfata.md` scria partenerul pe trei pași (sunt **patru**, în `components/partners/PartnerFormDialog.tsx`), „Generare" cu două taburi (sunt **trei**, `GENERATION_TABS`) și „Adaugă deșeuri" sus pe Acasă (scos pe 18.09); `README.md` numea ecranele „Panou" și „Intrări și ieșiri" (sunt **Acasă**, respectiv **Intrări** și **Ieșiri**), descria caseta de blocaje, dalele și listele de pe Acasă (înlocuite de bandă + anul pe luni + deșeurile anului + termene), afișajul LCD al panoului (scos), jurnalul de audit pe „zece tipuri" (sunt **13** în `AuditInterceptor.AUDITED`), seed-ul demo cu „5 parteneri, 34 de mișcări, 6 luni" (sunt **6 / 29 / februarie–august** în `DevDataSeeder`, plus a doua firmă, „Proba Automata SRL"), 37 de probe și 932/112; `frontend/e2e/README.md` descria probele 1, 5, 6, 8, 9 și 10 cum erau înainte de 16–18.09 (cuprinsul „de pe Ambalaje", „fisa → Evidențe", cinci secțiuni la partener, dala de stoc, caseta de blocaje) — rescrise după verificările din fișierele probelor.
> ⚠️ **Nepotrivit în cod, lăsat neatins (cererea a fost pe md-uri):** descrierile din `frontend/e2e/run.mjs` ale probelor 18 („afișajul, tastele") și 28 („Evidențe: …") numesc lucruri scoase pe 18.09; `scripts/deploy-split.sh` mai trece `vite.config.js` / `.d.ts` la divergența permisă a frontendului, deși fișierele nu mai sunt urmărite nicăieri (inofensiv).


> **18.09.2026, 23:40 — ✅ PE PRODUCȚIE (`ecoregistru-app` v127, `e2aa7b3`; monorepo `32ada3a`): tabul „Ambalaje", într-un singur ecran** (numai frontend; api neatins, v129; backendul, migrările și documentele tipărite neatinse, schema **V63**, liberă **V64**).
> Proprietarul: *„vreau ca în ambalaje să facem o refactorizare, nu îmi place cum e aranjată pagina"*, apoi, pe machete („Ambalaje, rearanjat", trei runde): *„să nu meargă pagina în jos, să tot dai scroll"*, *„are ieșiri și aia mi se pare aiurea rău, anexa 3 e ascunsă așa cumva"*, *„perfect arată fără scris așa"*.
> **Ce e acum pe tab:** două butoane lipite, **„Anexa 1 Ambalaje"** și **„Anexa 3 Ambalaje"**, fără text dedesubt — termenul și formatele în meniu, iar la Anexa 3 **punctul de lucru se alege în meniul butonului** (`MenuLabel` nou în `menu.tsx`; meniul se derulează în el peste 70vh); în dreapta, tastele **„Pus pe piață · Predat"** (`?tabel=predat`) — **un singur tabel pe ecran**; semnalele pe **un rând cu linkuri**; „Scrie cifre proprii" face câmpuri **chiar din tabel** (a doua grilă de 66 de câmpuri a dispărut; starea „nesalvat / se salvează / salvat" stă lângă numele materialului); tabelul 1 arată **doar materialele cu cifre**, cu capul pe două niveluri; „Predat" e **paginat la zece**.
> **Ce a plecat:** tabelul Anexei 3 și orice „Ieșiri" la **generator** (erau aceleași predări ca în „Predat"); filtrul „Punct" și butoanele de la mijlocul paginii; cutia galbenă; titlurile „Tabel 1." / „Tabelul 2." și explicațiile de sub ele. La firma care **și colectează** rămâne a treia tastă, „Preluat de la alții" (`Anexa3Section`, fără butoane de descărcare), fiindcă preluările pe proveniență și avertismentul de rol nu se văd altundeva; când profilul n-a spus rolul, butonul „Anexa 3 Ambalaje" e stins, iar motivul e pe tasta aceea.
> **Regula, scrisă în `docs/stil-interfata.md`:** un ecran de lucru nu se derulează la 1440 × 900.
> **Probe:** `tsc`, `npm test` **61/61**; proba **38** cere cele două butoane fără text sub ele, tastele fără „Ieșiri", **un singur tabel**, **pagina nu mai înaltă decât ecranul** (pe `main#continut` și pe document) pe „Pus pe piață" și pe „Predat", alegerea în adresă, și că semnalul e chiar linkul; proba **23** cere la generator butonul Anexei 3 și tastele fără preluări. Drumul de scris probat cu mâna prin browser: cifra tastată se salvează la ieșirea din celulă, apare cu „scris de tine" și în TOTAL, golită revine. **Control pozitiv al măsurătorii:** modul de scris ieșea cu 58px (958/900) până la strângerea rândurilor — măsura chiar vede derularea.
> **Suita e2e completă** pe stivă proprie și bază nouă (`eco_e2e_ambalaje`, 8077/5177): **39 din 39**. La prima rulare au căzut șase: proba **9** pe drept (citea grila veche de suprascriere — adusă la tabelul nou) și **29, 30, 33, 36, 37** din greșeala mea de rulare — își seamănă datele prin `psql`, iar fără `E2E_DB=<baza stivei>` lovesc baza `ecoregistru`; au căzut la primul INSERT, pe cheie străină, deci n-au scris nimic acolo. ⚠️ **Pe o stivă proprie, suita se rulează cu `E2E_DB`.**

> **18.09.2026, 19:00 — ✅ PE PRODUCȚIE (`ecoregistru-app` v126, `3601caf`; monorepo `88216fe`): panoul din stânga, după fuziuni; „Rol" pe linia taburilor la Parteneri; Acasă fără dată și fără buton de adăugare** (numai frontend; backendul, migrările și documentele tipărite neatinse, schema **V63**, liberă **V64**).
> Proprietarul, după ce „Evidențe" și „Ambalaje" au intrat ca taburi în „Generare": *„vreau să vedem niște variante de improve pentru meniul din stânga"*. Din machete („Meniul după fuziuni", două runde) a spus întâi ce nu-i place — *„Adaugă deșeuri și Septembrie generat și Caută oriunde"* —, apoi că varianta E *„e frumoasă"*, a ales **E3** și, pe localhost, a scos „+"-urile de pe rânduri: *„arată urâțel"*.
> **Ce a rămas în panou:** logo · căutarea ca **iconiță** · `[`; firma pe **plăcuța ei** (`bg-lcd`); meniul cu **tastă, nume, indicator** — fără iconiță cât panoul e lat (iconița rămâne pe șina de 64px); **taburile ecranului sub intrarea deschisă** (Generare, Termene, Parteneri la firma cu depozit), cu indicatorul coborât pe primul tab; Abonament și contul jos. Meniul începe la **118px**, nu la ~355.
> **Ce a plecat și nu se pune la loc:** afișajul lunii (`MonthDisplay.tsx` șters), tastele mari de adăugare, rândul „Caută oriunde", orice „+" pe rânduri. Din panou nu se mai adaugă nimic: adăugarea e butonul din antetul ecranului de mișcări și N / I / E; pe telefon, „+" din bara de jos.
> **O singură listă de taburi:** `lib/screenTabs.ts`, citită și de pagini (`PageTabs`), și de `navItems.ts`. Tabul din meniu schimbă doar `?tab=` (cu `replace`), ca `setTab` din pagină: luna, punctul și filtrele rămân.
> **Parteneri:** filtrul „Rol" stă pe **linia taburilor**, lipit la dreapta, cu eticheta mică lângă el, ca luna și anul pe „Generare"; banda de dedesubt a dispărut, iar taburile făcute de mână au devenit `PageTabs`. Firma fără depozit n-are taburi, deci la ea filtrul stă pe linia căutării. **Acasă:** fără ziua și data de deasupra salutului și fără butonul de adăugare din antet (amândouă scoase de proprietar).
> **Probe:** `tsc`, build, `npm test` **61/61**; suita e2e completă pe stivă proprie și bază nouă (`eco_e2e_panou`, 8096/5196): **39 din 39, 845 de verificări, 0 căderi**; probele de dinaintea ultimelor două schimbări (1, 2, 5, 6, 8, 10–19, 24) reluate pe codul final, 0 căderi. **Proba 18** rescrisă: cere că cele trei blocuri au plecat, că în panou nu e niciun link `?nou=1`, că rândurile n-au iconiță decât pe șină, că taburile din meniu sunt aceleași ca în pagină și că tabul din meniu păstrează luna. Proba 39 cere că deasupra salutului nu stă nimic cu an și că antetul n-are buton de adăugare. Rândurile meniului se aleg de acum cu `nav a[data-nav="row"]` (probele 18, 19).
> ⚠️ Banda grafit de sus de pe **telefon** încă arată cifra lunii și alerta — neatinsă, proprietarul a vorbit numai de panou.

> **18.09.2026, 17:01 — ✅ PE PRODUCȚIE (`ecoregistru-app` v125, `bfd8250`): Acasă, varianta A** (numai frontend; backendul, migrările și documentele tipărite neatinse, schema **V63**, liberă **V64**).
> Proprietarul a ales varianta A din cinci machete („Acasă — variante”), apoi, pe localhost, a scos *„Dacă vine controlul azi”* și *„Ultimele predări”*. Au rămas: **salutul zilei** cu firma și punctele de lucru; banda **„Următoarea acțiune”** cu **„+ încă N”** (`useDashboardData` întoarce acum lista `actions`, în aceeași ordine a costului; primul e `nextAction`); **„{an} pe luni”** — kg generate pe lună din evidența anului, cu luna goală dintre prima lună cu date și azi în galben, ca întrebare; **„Deșeurile anului”** — primele cinci coduri cu pubela; **termenele pe 12 coloane de luni** cu **„Adaugă în calendar”** (fișier `.ics` făcut în browser, UID = id-ul termenului, memento cu 3 zile înainte). Au ieșit dalele, caseta „Starea evidenței” și listele de termene/autorizații; explicațiile benzii, scurtate la un rând. Socotelile în `lib/home.ts`, fără React.
> ⚠️ **Capcana prinsă pe localhost:** un `sr-only` (poziționat absolut) într-un card fără `relative` lungea **documentul** cu 103px, iar pagina derula în document, nu în `main` — urcau antetul și panoul, dedesubt rămânea alb. `relative` pe card; proba 39 cere acum `scrollHeight === clientHeight`.
> **Probe:** `tsc`, build, `npm test` **61/61** (`home.test.ts` nou; **negative:** golul numărat de la ianuarie și ziua expirării luată drept expirată → cad exact testele lor); proba e2e **39** nouă (totalul anului și lunile goale socotite a doua oară din API, ordinea codurilor, fișierul `.ics`, „+ încă N” sub bandă, 375px); probele **6** și **10** citesc blocajele din bandă și noua așezare. Suita întreagă pe bază nouă (`eco_e2e_acasa`): **39/39, 835 de verificări, 0 căderi**.

> **18.09.2026, 13:27 — ✅ PE PRODUCȚIE (`ecoregistru-app` v123, `7e6c7cd`; monorepo `a6057c9`): Dosarul de control pe toată lățimea paginii** (numai frontend). *Intrare scrisă pe 19.09.2026, la recitirea md-urilor față de `heroku releases`: releaseul exista, dar nu era notat nicăieri.*
> Dosarul era singurul ecran oprit la `max-w-3xl`, deși antetul paginii și `<main>` n-au limită: la 1440px cardul se strângea în stânga, cu jumătate de ecran gol lângă el. Cardul și nota de sub el n-au acum limită, iar coloana de text a fiecărui rând are `max-w-[70ch]` — altfel explicațiile ajungeau la ~140 de caractere pe rând, iar „Descarcă" plutea departe de documentul lui. Cele trei coloane se citesc ca un tabel: stare · document · acțiune.


> **18.09.2026, 13:03 — ✅ PE PRODUCȚIE (`ecoregistru-app` v122, `27be747`; la scriere intrarea zicea „~14:15, nedeployat" — ora și starea sunt acum cele din `heroku releases`): banda de taburi nu mai derulează — firul gri de după ultimul tab a dispărut** (o clasă; numai frontend, schema **V63**, liberă **V64**).
> Proprietarul, cu mausul pe antet: *„ce e după «Totalul anului», butonul ăla?"* Nu era buton și nu delimita nimic: era **indicatorul de derulare** desenat de macOS peste `[role="tablist"]`.
> **De ce tocmai acolo:** butoanele de tab au `-mb-px`, ca linia tabului ales să acopere chenarul de jos — un pixel de depășire pe verticală. Iar `overflow-x` diferit de `visible` face **și** `overflow-y` să devină `auto` (regulă CSS), deci banda se socotea derulabilă. Cât ținea toată lățimea paginii, firul cădea la marginea din dreapta și nu-l vedea nimeni; de când s-a strâns, ca să încapă filtrele lângă ea, se termină fix după ultimul tab — în mijlocul antetului.
> **Reparația:** `overflow-x-auto` → `flex-wrap` pe `[role="tablist"]`. Taburile se rup pe două rânduri dacă nu încap, în loc să deruleze pe o bară pe care oricum n-o vedea nimeni. **Atinge toate ecranele cu taburi** (Generare, Termene, Clienți, pagina firmei) — pe cele de acum, două până la patru taburi, nu se rupe la nicio lățime (măsurat 1440, 1280, 375).
> **Probe:** `tsc`, build, `npm test` **56/56**; verificare nouă în proba 10: banda cere `overflow: visible` pe **amândouă** axele. Se cere asta, nu absența depășirii — cei 1px rămân, fiindcă `-mb-px` e chiar rostul lor, dar pe o cutie `visible` niciun browser nu desenează indicator.
> **Negativă:** cu `overflow-x-auto` pus la loc cade exact verificarea nouă și arată chiar regula CSS — `{"x":"auto","y":"auto"}`.

> **18.09.2026, 12:20 — ✅ PE PRODUCȚIE (`ecoregistru-app` v121, `186d4b5`; la scriere: „~13:30, nedeployat"): totalul anului pe pagini de zece, iar „Mișcări" primește aceleași filtre pe linia taburilor** (numai frontend; backendul, migrările și generatoarele de documente neatinse, schema **V63**, liberă **V64**).
> Proprietarul: *„să se vadă primele intrări, să nu se lungească pe tot ecranul lista"* și *„în generare/mișcări vreau dropdownurile cu ani să fie ca la Totalul anului"*.
> **(1) Zece coduri pe pagină** (`useTableView` + `TablePagination`, primitivele casei), nu douăzeci și cinci cât e implicit: tabelul stă sub un antet înalt, deci la o firmă cu treizeci de coduri **rândul de total** — cifra pentru care se deschide tabul — ajungea mult sub marginea ecranului.
> ⚠️ **Rândul de total rămâne al anului, nu al paginii:** se socotește din toate codurile, nu din cele vizibile, iar eticheta le numără pe toate. Probat pe **18 coduri** puse anume: 10 rânduri, „1–10 din 18", iar jos „18 coduri de deșeu" cu totalurile anului. Altfel, pe pagina a doua, cifra tastată în SIM ar fi fost alta.
> **(2) „Mișcări" are filtrele pe linia taburilor** — luna, anul, punctul de lucru și „Șterge filtrele" — exact ca „Totalul anului"; banda de dedesubt a dispărut, deci dala de totaluri urcă. **Intrări și Ieșiri își păstrează banda**: n-au taburi pe care să urce filtrele.
> **Probe:** `tsc`, build, `npm test` **56/56**; suita e2e completă pe bază nouă (`eco_e2e_gata`): **37 din 37, 792 de verificări, 0 căderi**. Proba 10 are două verificări noi: cel mult zece rânduri pe pagină și rândul de total care numără **toate** codurile anului.
> ⚠️ **Două rulări au căzut înainte, și nu din cod:** proprietarul se uita pe localhost pe **aceeași stivă** pe care rula suita și a completat, firesc, chiar cele două rânduri pe care `DevDataSeeder` le pune stricate dinadins — „Aviz nr. 369 (fără cod)" și „Aviz nr. 391" (fără cantitate). Alea sunt **premisa** probelor 10 și 28; fără ele au căzut opt verificări din două probe. Jurnalul de audit (`audit_log`) a arătat exact cine și când. Scris în `frontend/e2e/README.md`: nu se rulează suita pe stiva dată cuiva să se uite.

> **18.09.2026, 11:44 — ✅ PE PRODUCȚIE (`ecoregistru-app` v120, `bda38c6`; la scriere: „~12:15, nedeployat"): antetul tabului „Totalul anului" refăcut — filtrele pe linia taburilor, documentele cu numele lor, recalcularea ca acțiune, nu ca document** (numai frontend; backendul, migrările și generatoarele de documente neatinse, schema **V63**, liberă **V64**).
> Șase așezări într-o dimineață, cu proprietarul lângă ecran, pe localhost. Ce a rămas, și de ce:
> **(1) Filtrele urcă pe linia taburilor**, lipite la dreapta, cu eticheta mică lângă ele, nu deasupra: „An" și „Punct de lucru" ocupau o bandă întreagă sub taburi („arată rău acolo"). Slotul e în primitiva `PageTabs` (`right`), ca să-l poată folosi și Termene sau Clienți, și stă **în afara** lui `role="tablist"`, ca săgețile dintre taburi să nu treacă peste el.
> **(2) Documentele, un rând deasupra tabelului**, fiecare cu explicația lui dedesubt (`DocAction`). Butonul poartă **numele documentului** — „Evidența gestiunii deșeurilor", „Evidența centralizată" — nu „Descarcă": două butoane la fel de anonime, unul lângă altul, sunt felul în care cineva depune hârtia greșită. Numele întreg din act rămâne în explicație, în `aria-label` și pe hârtie.
> **(3) „Recalculează acum" a ieșit dintre documente.** E ultimul pe rând și are **`variant="muted"`** — variantă nouă în `button.tsx`: cutia lui, ca să nu plutească, dar fundal stins și chenar subțire, ca să nu pară al treilea document. E **singurul buton de pe ecran care schimbă date** (rescrie evidența, și anii de după, fiindcă stocul se reportează); îmbrăcat ca ele, ar fi fost apăsat pe 15 martie de cineva care crede că scoate un fișier — exact greșeala pentru care ecranul vechi ascunsese cele cinci butoane la fel de vizibile. Logica lui s-a întors în `AnnualTotals`, lângă buton.
> ⚠️ **Ce s-a încercat și s-a aruncat, ca să nu se reia:** documentele **pe rândul filtrelor** — numele din act nu încap; ca o **coloană de 320px în dreapta, lângă tabel** — arăta bine la 1440px, dar fura din lățimea tabelului („arată rău tăiat în dreapta"), iar la `xl` (1280px) tabelul chiar **ieșea din cutie**, 677 într-un 602; coloana **doar lângă introducere**, cu tabelul dedesubt — lăsa un **gol alb de ~230px** în stânga, fiindcă dreapta e de trei ori mai înaltă. Odată cu coloana a ieșit și pragul `rail` din `tailwind.config.js`. Recalcularea a stat pe rând sub tabel („ascuns tare acolo jos"), ca link lângă introducere („parcă e în aer") și ca buton cu chenar între documente (arăta ca un document).
> **Probe:** `tsc`, build și `npm test` **56/56** curate; suita e2e completă pe bază nouă (`eco_e2e_valid`): **37 din 37, 791 de verificări, 0 căderi**. Proba 10 are patru verificări noi: butoanele își poartă **numele**, tabelul vine **după** primul dintre ele în DOM (`compareDocumentPosition`, nu pixeli), iar „Recalculează acum" stă **după** documente și are **alt chenar și alt fundal** decât ele (citite din `getComputedStyle`, nu din clase), plus antetul fără „stoc".
> **Negativă:** cu `variant="outline"` pe „Recalculează acum" — adică îmbrăcat ca documentele — cade **exact** verificarea lui, cu motivul la vedere (`altChenar:false, altFundal:false`); restul probei rămâne verde.
> **Măsurat la șapte lățimi** (1600, 1440, 1399, 1280, 1024, 768, 375): tabelul e **întreg la toate**, `scrollWidth` = `clientWidth` (1274, 1114, 1073, 954, 698, 720, carduri la 375), iar pagina n-are derulare laterală nicăieri. Capturi privite la 1440, 1280, 1024 și 375.

> **18.09.2026, 10:12 — ✅ pe producție: documentele urcă deasupra tabelului, iar coloana „În stoc" iese de pe Generare** (`ecoregistru-app` **v119**, `534528a`; api neatins, rămâne **v128**; monorepo `52ca3a0`; fără migrare, schema **V63**, liberă **V64**). *Aspectul a fost refăcut la 11:0x — vezi intrarea de deasupra.*
> Proprietarul, după ce a văzut tabul nou pe producție: *„ar trebui în Generare/Totalul anului să punem butoanele de evidențele gestiunii sus, nu ascunse jos"* și *„să scoți «în stoc», că oamenii nu au stoc la generatori"*. Amândouă sunt reparații ale feliei de la 02:10, nu funcții noi.
> **(1) Ordinea ecranului:** intro → banda cu ce blochează depunerea → **cele două documente oficiale** → tabelul → „Recalculează acum". Până acum butoanele stăteau **sub** tabel: cu douăzeci de coduri cădeau sub marginea ecranului, iar omul care vine pe 15 martie după hârtie nu le găsea. Banda de blocaje rămâne lipită deasupra lor dinadins — se vede ce lipsește *înainte* de clic, nu abia în dialogul de după.
> **(2) Coloana „În stoc" a ieșit** din tabel, din cardurile de telefon și din rândul de total (șase coloane → **cinci**). Nu e o preferință de ecran, e aceeași decizie ca la **`V58`** (16.09.2026), pe care felia de la 02:10 o călcase fără să bage de seamă: un generator n-are cântar și nu ține stoc — deșeul stă în pubelă până vine colectorul, iar cantitatea se află abia la predare, de pe tichetul lui. De aceea serverul refuză o generare fără ieșire, generarea e cea **dedusă** din predare (`V24`), iar `stoc = stoc_anterior + generat − valorificat − eliminat` iese **zero prin construcție**. O coloană de zerouri nu e o informație. Dala „Coduri cu stoc" ieșise deja de pe panou pe 16.09.
> ⚠️ **„Rămasă în stoc" rămâne unde o cere actul:** pe fișa tipărită (HG 856/2002, anexa 1, cap. 1, care are coloana) și pe centralizator. **Niciun generator de document nu s-a atins** — validarea Andreei din 16.09 nu se redeschide.
> **Ce s-a șters odată cu ea:** socoteala „stocul e al ultimei luni, nu suma lunilor" din `lib/annualTotals.ts` (devenise cod fără cititor) și cele trei teste ale ei; în locul lor, un test care cere ca `closingStock` de pe linii să **nu** ajungă în totaluri — adică regula, nu formula. `npm test` **56** (era 58).
> **Probe:** `tsc` și build curate; `npm test` 56/56; suita e2e completă **37 din 37, 790 de verificări, 0 căderi**, pe stivă proprie și bază nouă (`eco_e2e_docsus`). Proba 10 are două verificări noi: tabelul vine **după** primul „Descarcă" în DOM (`compareDocumentPosition`, nu pixeli: o probă pe coordonate ar fi trecut și cu tabelul gol) și antetul are cinci capete, niciunul cu „stoc".
> **Negativă:** cu versiunea de dinainte pusă la loc din git (`AnnualTotals.tsx`, `annualTotals.ts`, `strings.ts`), cad **exact** cele două verificări noi, nimic altceva.
> **Măsurat** (regula din `CLAUDE.md` după orice schimbare de coloane): tabelul 1114/1114 la 1440px, 698/698 la 1024px, iar la 375px nu există — sunt cardurile; pagina fără derulare laterală la toate trei. Capturi la 1440 și 375 privite.
> **Md-uri:** `stil-interfata.md`, `frontend/e2e/README.md` și două rânduri din README care rămăseseră neadevărate de pe 16.09 — „the year's totals … what is left in stock" și rândul despre dala de stoc de pe panou, care nu mai există în cod.

> **18.09.2026, 09:46 — ✅ pe producție: reparațiile radiografiei și cele patru fișiere-mamut împărțite, la același deploy cu felia „Evidențe în Generare"** (`ecoregistru-api` **v128**, `defe8f8`; `ecoregistru-app` **v118**, `c75deec`; monorepo `c5df621`; fără migrare, schema rămâne **V63**, liberă **V64**).
> Un singur deploy pentru tot ce s-a scris în noaptea de 17 spre 18.09 (01:32–02:31), cerut de proprietar dimineața („pune la zi md-urile și dyno-ul de pe prod"). **Niciun document tipărit și nicio regulă de evidență atinse** — validarea Andreei din 16.09 rămâne în picioare.
> **(1) Reparațiile din `todo-radiografie-1709.md`** (`ec02bce`, singurul commit cu backend în el, și acela numai comentarii și teste): proba **34** nu mai caută constanta „6 parteneri" în dosar — citește lista din `GET /api/v1/partners` și socotește expirat/pe-terminate cu aceleași reguli ca `AuditFileService.status` (asta dobora CI-ul de trei rulări la rând); proba **32** a intrat în `run.mjs`, unde lipsea de la scriere, deci **37 de intrări = 37 de fișiere**; `@MockBean` (deprecat, scos în Boot 4) → `@MockitoBean` în **27** de fișiere de test; comentariile care descriau o stare veche, rescrise (`V2__seed_waste_codes` nu mai trimite la un TODO inexistent, `AuthenticationService` nu mai zice „blocked on SMTP for now", `analysis_bulletins` notată ca tabelă istorică în `export-client.sh`); md-urile publice aduse la adevăr (README, `ci.yml` fără cifre care îmbătrânesc, `frontend/e2e/README.md`).
> **(2) Fișierele-mamut, punctul 3.2 al radiografiei** — patru refactorizări fără nicio schimbare de comportament, fiecare cu suita ei: `PackagingPage` **1.074 → 797** (Anexa 3 în `components/packaging/Anexa3Section.tsx` + `packagingFormat.ts`), `AccountRequestPage` **1.003 → 927** (regulile cererii în `components/account-request/accountRequestRules.ts`, cu teste proprii), `PartnersPage` **1.554 → 483** (fișa în `components/partners/PartnerFormDialog.tsx`, deschisă prin `ref`), `MovementFormDialog` **1.766 → 1.653** (transportatorul, șoferul, vehiculul și destinațiile drumului în `components/movements/TransportFields.tsx`, lângă `Anexa2Fields` și `PackagingFields`).
> ⚠️ Cele 135 de linii mutate în `TransportFields` sunt **identice caracter cu caracter** cu originalul (`diff` pe bloc), iar învelișul e un fragment care nu produce niciun element — grila și aspectul rămân cele dinainte. Celelalte șapte secțiuni ale formularului **nu s-au atins** dinadins: sunt 484 de linii pe ecranul validat de specialistă, iar o greșeală acolo nu se vede în `tsc`, se vede pe un aviz tipărit greșit.
> **Probe înainte de deploy:** backend **932 / 112, 0 eșecuri**, **zero avertismente `[removal]`**; `npm test` **58/58**; `tsc` curat; `npm run build` curat; e2e **37/37** pe bază nouă (rulate de sesiunea care a scris feliile, întâi una câte una cele care ating rubricile mutate — 4, 5, 13, 20, 21, 26, 27); **CI verde pe `main`** la `c5df621` (rularea `35288407823`), prima rulare verde după reparația probei 34.
> **Pe dyno:** garda `deploy-split.sh` curată pe ambele părți (backend 1 commit, frontend 6); „Schema «public» is up to date. No migration necessary", `Started EcoRegistruApplication`, `/actuator/health` **200**, `GET /api/v1/partners` **401** fără token; bundle-ul servit (`index-B1DJFHJ1.js`) conține „Totalul anului" și „Documentele anului", iar cuvântul „Evidențe" **nu mai apare deloc** în el.

> **18.09.2026, 02:10 — ✅ pe producție (deployat 09:46, odată cu felia de mai sus): „Evidențe" fuzionat în „Generare", iar Dosarul de control devine locul hârtiilor** (numai frontend; backendul, migrările și generatoarele de documente neatinse).
> Proprietarul, citind macheta: *„cumva nu e futai că și Generare și Evidența e la fel?"* Are dreptate, și se vede în cod: `EvidenceCalculator` agregă **exact** mișcările registrului `ANEXA_1`
> (`EvidenceCalculator.java:41`), adică exact rândurile listate de `/generare` — două intrări în meniu pentru același registru, cu aceleași trei filtre și aceleași butoane de document. La
> **generatorul pur**, singurul modul care se vinde, `/generare` e chiar singurul lui ecran de mișcări, deci dublura era la clientul care plătește. Decizia (18.09): **Evidențe dispare de tot**,
> meniul rămâne „Generare", documentele se iau din Dosar, iar lista autorizațiilor rămâne numai în arhivă.
> **(1) Generare are două taburi**, ca Termene și Clienți (`PageTabs`, tabul în adresă): **Mișcări** (ecranul de până acum, neatins) și **Totalul anului** — un rând pe cod, cu generat,
> valorificat, eliminat, ce a rămas **în stoc** și starea („Gata" · „N de cântărit" · „N kg fără cod R/D"), rândul total, cele două documente oficiale dedesubt, „Alte descărcări" și
> „Recalculează acum". Componenta nouă: `components/movements/AnnualTotals.tsx`. ⚠️ Stocul **nu** se adună peste luni: `closingStock` e deja cumulativ pe (punct de lucru, cod) și poartă
> anii dinainte, deci se ia al **ultimei** luni cu date, pe fiecare punct, și se adună între puncte. Pe telefon, câte un card pe cod (tabelul are șase coloane).
> **(2) Ecranul „Evidențe" a fost șters** (`EvidencesPage.tsx`, 615 linii, și `HandoverRegister.tsx`, folosit numai de el). `/evidente` e redirect (`EvidencesRedirect`): `?problema=cod-rd`
> duce pe lista anului cu filtrul pus, restul pe tabul totalului; o firmă fără Anexa 1 pleacă pe primul ei ecran. Filtrul „doar ce blochează depunerea" trăiește acum pe lista de mișcări
> (serverul îl știa deja — `WasteMovementController:80`, `missingOperationCode`), cu banda care îl anunță și îl scoate. Cuvintele paletei („fișa", „anexa 1", „evidență") s-au mutat pe Generare.
> **(3) Dosarul de control: buton pe fiecare rând.** Lista „Ce intră în arhivă" devine „Documentele anului": fișa, centralizata, Anexa 1 și Anexa 3 Ambalaje (meniu .xls / PDF), plus
> rezumatele neoficiale sub linie. Ce se naște numai înăuntrul arhivei (autorizațiile, atașamentele) scrie „în arhivă"; ce nu se aplică anului scrie „—", nu „în arhivă" (contrazicea eticheta
> „Nu intră" de lângă el). Zero endpoint-uri noi: toate erau deja folosite de alte ecrane. Lista autorizațiilor rămâne fără buton — decizia proprietarului („lasă în zip").
> **Linkuri repointate:** Acasă (banda și blocajul roșu), panoul consultantului, `documentFor` pe termenul de 15 martie (`/generare?tab=total&luna=…`).
> **Probe:** `tsc` curat, build curat, `npm test` **58** (7 noi: `lib/annualTotals.test.ts` — socoteala pe cod, scoasă din componentă ca să se poată proba).
> **Negativă:** cu stocul adunat peste luni, două teste cad; cu socoteala bună, trec. Asta e cifra pe care ecranul o poate greși în tăcere: pe baza demo
> toate stocurile sunt zero, deci proba de ecran ar fi comparat numai zerouri. Suita e2e completă: **35/37**, iar cele două căderi (8 și 19) erau
> așteptările vechi — „fisa" găsește acum „Generare", iar meniul firmei cu depozit are fix zece intrări, deci Setările sunt iar pe „0", nu pe „S";
> amândouă reparate și re-rulate verzi. Probele 1, 3, 5, 6, 8, 9, 10, 11, 19, 22, 28, 34 rulate una câte una pe stivă proprie (8099/5199, baza
> `eco_e2e_evidente`); proba 10 secțiunea 7 rescrisă pe tabul nou
> (redirectul, cele două taburi, un rând pe cod, **cifrele comparate cod cu cod cu API-ul**, coloana de stare, documentele, meniul, descărcarea), proba 28 mutată pe tab, proba 6 pe lista de
> mișcări (creionul deschide formularul acolo unde stă rândul). Capturi la 1440 și 375 privite.
> ⚠️ În arborele de lucru mai lucra o sesiune paralelă (`components/partners/` netracked, `PartnersPage.tsx` modificat, `tsc` roșu pe ele): commitul feliei ăsteia nu le atinge.

> **17.09.2026, 20:43 — ✅ pe producție: reparațiile din scanarea codului și a md-urilor** (`ecoregistru-api` **v127**, `8fdde53`; `ecoregistru-app` **v117**, `f92ab5f`; monorepo `1b82c51`; fără migrare, schema **V63**, liberă **V64**). Deployul din sesiune a fost refuzat de clasificator; l-a rulat proprietarul cu `!`.
> Proprietarul: „scanează atent codul și toate md files”, apoi „ce ai zis că trebuie fixat, să fixăm neapărat”; depozitul și mobilul lăsate deoparte.
> Scanarea pe `7f16fcc` (= producția): 921 de teste / 110 clase, 0 eșecuri; `npm test` 43; tsc; build. Șase defecte (BUG-025…030 în `QA-BUGS.md`, repo privat):
> **(1) BUG-025** — „Am plătit — verifică acum” (F-E) își punea pauza de 2 minute doar după un răspuns FGO, iar `FgoClient.send` era `synchronized`
> (30 s × 3 la 409 + pauze): cu FGO căzut, clicurile clienților stăteau la coadă și țineau pe loc butoanele platformei. Acum încercarea însăși oprește
> următoarea 2 minute (`fgo.recently.asked`), clientul așteaptă lacătul cel mult 5 s și nu reîncearcă 409; rularea de la 06:30 rămâne neschimbată.
> Fără chei FGO sau pe o factură plătită, pauza nu pornește (proba 36 a prins ordinea). **(2) BUG-026** — **reprodus**: „RO51779887” și „51779887” făceau
> două firme (și două cabinete), la creare și la editare; verificarea compară acum cifrele (`existsByCuiDigits`). **(3) BUG-027** — ANAF v9 răspunde
> **HTTP 404** cu `{"found":[],"notFound":[cui]}` la un CUI necunoscut (verificat cu `curl`), citit ca „ANAF nu răspunde”; acum „ANAF nu are nicio firmă”.
> **(4) BUG-028** — județul de facturare se verifică pe server după nomenclatorul FGO (`util/FgoCounties`, `billing.county.invalid`). **(5) BUG-029** —
> șase `@Scheduled` fără `zone` pe un dyno UTC: mementourile de termene plecau la 10:00; toate nouă pe `Europe/Bucharest` (`SchedulerZoneTest`).
> **(6) BUG-030** — găsit când suita a stat 10 minute: `CloudinaryStorageService.fetch` avea termen doar până la antete, deci un atașament care tăcea la
> jumătate ținea dosarul (și tranzacția) pe loc; acum termen de 20 s pe toată descărcarea. Plus: consultantul nu mai cere `/subscriptions/founders` (403).
> **Probe:** backend **931 / 112, 0 eșecuri** (+10 teste), fiecare regulă nouă scoasă o dată → exact testele ei cad; `npm test` 43, tsc, build;
> e2e **36, 31, 29, 33, 30** verzi pe `eco_e2e_scanfix` (stivă proprie 8097/5197). **Docs:** README (Boot 3.5, 931/112, 37 de probe, kg, fără trimestrial,
> dosarul nou, Clienți pe taburi și pagini, Facturare, Abonament), `legislatie.md` (AFM fără trimestrial), `frontend/e2e/README.md` (proba 37).
> **Pe producție:** garda `deploy-split.sh` curată pe ambele părți (8 commituri backend, 2 frontend); „No migration necessary”, `Started`, health UP,
> `POST /api/v1/billing/invoices/{id}/check-payment` → 401 fără token, `/abonament` 200. ⚠️ Rămâne SQL-ul de dubluri CUI rulat o dată pe producție (BUG-026).

> **17.09.2026, 19:49 — ✅ pe producție: `/abonament` la client (F-E) și cache-ul golit la deconectare** (`ecoregistru-api` **v126**, `8fadcb8`; `ecoregistru-app` **v116**, `53c1791`; monorepo `a5a1bfc` + `e90f759` + `7f16fcc`; fără migrare, schema **V63**).
> **(1) F-E** (`todo-clienti-abonamente.md`, repo privat): sus un bon cu câte un rând pe factură emisă și neplătită, totalul sub linie, „De plată până pe …” /
> „Restantă de N zile” sau „Totul e plătit” cu următoarea factură; transferul cu beneficiarul, CUI-ul, IBAN-ul și banca din contract (`app.billing.payee.*`),
> suma și numerele facturilor, fiecare cu „Copiază” (IBAN-ul fără spații); „Am plătit — verifică acum” (`POST /api/v1/billing/invoices/{id}/check-payment`,
> doar pe facturile contului, fără FGO dacă plata a fost citită în ultimele 2 minute); datele de facturare ținute la zi de client (`PUT /api/v1/billing/details`,
> contract art. 7.5, fără denumire și CUI), cu rândul `Subscription` în jurnalul firmei și mail pe adresa veche. **(2) `7f16fcc`:** `queryClient.clear()` la
> autentificare și deconectare — un consultant care intra după platformă în același tab vedea și putea alege firmele platformei (proba **37**).
> Probe: backend 921 (`BillingSelfServiceIT` 13), `npm test` 43, e2e **36** 29/29 și **37**. CI roșu doar pe proba 34 (date: 6→7 parteneri), deci deployul a
> fost rulat de proprietar.

> **17.09.2026, 19:08 — ✅ pe producție: Termene pe taburi — De făcut · Bifate · Trecute, cu termenele trecute „Calculat”** (`ecoregistru-api` **v125**, `3c2d983`; `ecoregistru-app` **v115**, `6f1718d`; monorepo `6e4a361` + `292e94a`; fără migrare, schema **V63**, liberă **V64**).
> Proprietarul: „termene la fel ca la clienți — sus De făcut, Bifate și nou Trecute, doar 2026 ce a trecut”. **(1)** `PageTabs` pe `/termene`
> (`?tab=bifate|trecute`, ca pe Clienți); „Bifate” păstrează selectorul de an. **(2)** `GET /api/v1/deadlines/past` → `DeadlineService.listPast`: anul
> în curs de la 1 ianuarie până ieri, bifate și nebifate, **fără** filtrul `MissedDeadlinePolicy` (arată și termenele vechi ascunse din „De făcut”);
> pe ecran nebifatele sunt „Nebifat” gri, fără zile, fără „Depășit”. Primul deploy (api v124 / app v114, 18:48) avea doar atât. **(3)** Pe Onsia (creată
> azi) tabul era gol: din 16.09 nu se salvează termene trecute. Varianta aleasă de proprietar: **se socotesc pe loc, nu se salvează**. Regulile calendarului
> au devenit o listă comună (`Rule`: tip, zile, „datorează?”), folosită de `ensureUpcoming` și de `listPast`; ce lipsește din bază vine cu `computed=true`,
> `id=null` → „Calculat”, fără buton, fără mailuri. Se socotesc după profilul **de azi** al firmei. Varianta cu termene salvate la crearea firmei — respinsă
> (contrazice regula din 16.09). Probe: backend **908/109**, `npm test` 37, e2e **35** nouă (demo: 10 „Nebifat”; firmă nouă cu AFM lunar: 9 „Calculat”,
> fără butoane, 0 salvate înainte și după) + 1, 9, 10, 11 verzi pe `eco_e2e_trecute`. **Negative:** badge-ul obișnuit pe Trecute → 1 cădere; butonul pe
> rândurile calculate → 1 cădere. Pe producție, Onsia are tot 1 termen în bază după deploy. Deploy din sesiune cu `deploy-split.sh both --ref HEAD --push`.

> **17.09.2026, 18:24 — ✅ pe producție: pagina firmei, `/clienti/:id`** (`ecoregistru-api` **v123**, `0941597`; `ecoregistru-app` **v113**, `2046680`; monorepo `a75e35e`; fără migrare). *Intrare scrisă pe 19.09.2026, la recitirea md-urilor față de `heroku releases`: felia era pe producție, dar jurnalul ăsta n-o avea — fusese notată numai în notele interne ale sesiunii paralele.*
> Platforma și consultantul deschid o firmă pe **pagina ei**, cu tabul în adresă (`?tab=utilizatori|abonament|istoric`), în locul dialogului `xl` cu ~25 de rubrici: **Profil** — aceeași fișă, în aceeași ordine (`CompanyForm.tsx`), cu cuprins lipicios (`SectionNav`, șapte secțiuni) și caseta „Ce lipsește pentru dosar” (adresa, CAEN, persoana desemnată, CUI de corectat), cu linkuri spre rubrici; **Utilizatori**; **Abonament și facturi** (numai platforma, numai clientul direct: pachetul, prima factură și cea lunară, facturile cu „Verifică plata” și „Oprește”); **Istoric** (jurnalul firmei). Sus: numele, `CUI · RC · tip`, adresa, starea abonamentului; „Invită utilizator” (N), „Intră în cont”, „⋯ → Cabinet”.
> Utilizatorii și jurnalul se cer cu `X-Tenant-Id` **pe cerere**, fără să mute comutatorul de firmă: interceptorul din `api.ts` nu mai calcă un antet pus de cerere (serverul îl verifica deja — consultantul doar pe cabinetul lui). Dialogul firmei și invitația din `ClientsPage` au ieșit; `SubscriptionDialog` a rămas numai pentru cabinete, care n-au pagină. Reparat pe drum: cu o firmă aleasă, tasta N pe `/clienti…` pornea și „Adaugă deșeuri”.
> **Probe:** backend 902/109; `npm test` 37; proba e2e **33** nouă. **Negative:** cu antetul suprascris de interceptor cad trei verificări (lista, invitația ajunsă la altă firmă, istoricul); fără excepția `/clienti` din `Layout`, una.


> **17.09.2026, 18:01 — ✅ pe producție: Dosarul de control — structură nouă, lista autorizațiilor refăcută, „Ce intră în arhivă” din datele reale** (`ecoregistru-api` **v122**, `ef24174`; `ecoregistru-app` **v112**, `c119c25`; monorepo `fe04437`; fără migrare, schema **V63**, liberă **V64**).
> Proprietarul: dosarul „să fie mai frumos și să pară făcut de un profesionist”, „cu mare grijă, dosarul și documentele sunt bune”. **Niciun
> generator de document oficial nu e atins**: diferența e în `AuditFileService` (cum se pun în arhivă), `AuditFileController`, un `count` în
> `WasteMovementRepository`, ecranul `AuditFilePage` și teste; apelurile către generatoare sunt identice cu cele de dinainte.
> **(1) Structura arhivei** (`55dea0a` → `81c1a84`): `00-cuprins.txt` (fostul `README.txt`), `autorizatii-parteneri.pdf`, **`rapoarte/`** (fișa,
> centralizata, Anexa 1 Ambalaje, Anexa 3 Ambalaje pe punct de lucru — aceleași condiții ca înainte) și **`atasamente/`** (+ `index.txt`); pe mai mulți
> ani `AAAA/rapoarte/`, `AAAA/atasamente/`, lista autorizațiilor o dată la rădăcină. **Rezumatul neoficial (`evidenta-AAAA.xlsx/.pdf`) a ieșit din
> dosar** — rămâne pe Evidențe → Alte descărcări. Numerotarea `01-…`/`90-de-lucru/` a fost o etapă intermediară, respinsă de proprietar.
> **(2) `autorizatii-parteneri.pdf`** scria cu Helvetica WinAnsi și **pierdea ă, ș, ț** („Autorizaii”, „Expir în”): acum Cp1250 ca celelalte generatoare,
> A4 orizontal, antetul tabelului repetat, rezumat („6 parteneri · 1 cu autorizația expirată · …”), status colorat (verde valabilă, galben ≤ 60 de
> zile, roșu expirată, gri inactiv, **alb fără dată** — nu pare verificat), coloana **„Coduri de deșeu <perioada>”** (partener sau transportator pe
> mișcările care contează), „Colector, transportator” în Tip, nota despre valabilitate, „pagina X din Y”. Rămâne document de lucru (antetul cabinetului).
> **(3) „Ce intră în arhivă”** (`906eafc`): lista fixă de pe ecran devine citirea reală — `GET /api/v1/audit-file/contents?year&years` (aceleași
> reguli ca arhiva: `anexa3Plan`, rolul de piață, mișcările care contează; nu regenerează nimic). Fiecare document are LED-ul lui — **Intră** /
> **Intră fără date** (an fără mișcări) / **Nu intră** (comerciant; profil fără rol de piață; fără ambalaje în an) / **Lipsește** (anul are ambalaje, dar
> rolul în lanțul ambalajelor nu e ales) — și motivul; pe mai mulți ani, câte un rând pe an; partenerii cu autorizația expirată sau pe expirate.
> Probe: suita **906/109, 0 eșecuri**; `AuditFileIT` compară textul foilor din dosar cu descărcarea lor directă și endpointul cu arhiva citită în
> același moment (demo-ul își schimbă profilul în suita completă — alt test îl face TRADER —, deci nu se presupune profilul); **negative:** fontul
> vechi și foaia pe alt an → 2 căderi, punctele Anexei 3 golite → 1 cădere. E2E **34** nouă (`34-dosar-continut.mjs`) **10/10** la 1440 și 375px pe
> `eco_e2e_dosar` (**negativă:** Anexa 1 mereu „Intră” → 1 cădere), 22 verde. Garda: backend `.gitignore` + `SecurityConfiguration` (newline),
> frontend `.gitignore` + `vite.config.*`. Pe dyno: „Started”, bundle-ul servit are „Ce intră în arhivă”, `/audit-file/contents` → 401 fără token.
> ⚠️ Proba s-a numit întâi 33; numărul l-a luat pagina firmei (tabul paralel), deci e **34**. Pe producție erau deja, de la tabul paralel (17:05–17:12,
> api v120/v121, app v111), `ffec2c9`, `54f7036` și `1eeaff6` — intrarea de mai jos („NEDEPLOYAT”) nu mai e de actualitate.

> **17.09.2026, ~17:30 — ✅ pe producție din 17:05 (`ecoregistru-app` **v111**, `09e9219`, deployat de tabul paralel odată cu adresa de pe factura FGO; intrarea scria „NEDEPLOYAT — proprietarul: «să nu le deployăm»”, dar `heroku releases` și istoricul repo-ului de frontend arată că plecase deja): Parteneri — punctele de lucru și șoferii la vedere** (`1eeaff6`, doar frontend, fără migrare; așteaptă la același deploy cu `ffec2c9`, adresa pe factura FGO, backend).
> Proprietarul: „dacă cineva vrea ulterior să adauge șoferi într-un partener sau puncte de lucru, nu e deloc la vedere și nici intuitiv”. Punctele
> de lucru stăteau la coada pasului 1, șoferii sub cardul „Vine el și îl ia”. Acum: **pasul 4 „Puncte de lucru și șoferi”** (opțional; la pasul 2
> rămâne doar licența de transport; la cine nu transportă, pasul 4 spune de ce și are „Vine el cu mașinile lui — adaugă șoferi”); **în tabel, sub
> nume**, „1 punct de lucru · 2 șoferi” sau „+ Punct de lucru” / „+ Șofer” (șoferii doar la transportatori) — deschid fișa pe pasul 4, cu rând gol
> și cursor; pe rând „Editează” + „⋯” (Puncte de lucru și șoferi, Dezactivează / Reactivează), ca tabelul să încapă. Legătura din „Autorizație
> expirată” deschide tot pasul autorizației. Aspectul aprobat pe localhost.
> Probe: `tsc` curat, `npm test` 37/37; e2e **32** nouă (16/16), 8, 9 (patru pași), 10 verzi; 1440px 1114/1114, 375px fără derulare. Proba 3 cade
> doar pe o bază cu peste 10 parteneri (rulările probei 32 îi lasă în urmă).
> *(Adăugat 18.09.2026: proba 32 a rulat până atunci doar cu mâna — nu era în `frontend/e2e/run.mjs`, deci nici în CI. A intrat în listă
> pe 18.09; pe baza nouă a CI-ului lasă un singur partener în urmă, departe de pragul de 10 al probei 3.)*

> **17.09.2026, 16:43 — ✅ pe producție: Setările pe carduri, fiecare secțiune pe pagina ei** (`ecoregistru-app` **v110**, `ad3a0f2`; api neatins, rămâne **v119** cu F-C al tabului paralel; monorepo `59f2beb`).
> Proprietarul: tabul Setări „nu e deloc intuitiv și nici user friendly”. Pagina lungă (până la zece tabele, cuprins lipicios, butonul din cap
> adăuga un punct de lucru) devine **`/setari`**: carduri pe grupuri — **Firma** (Datele firmei, Puncte de lucru, Generatori interni),
> **Echipa** (Utilizatori, Jurnal de audit; doar cine administrează), **Transport** (Șoferi; Flota la art. 48), **Depozit** (Prețuri, Sortimente;
> doar art. 48) — fiecare cu starea pe scurt (autorizația valabilă / expirată / necompletată, câte active, invitațiile în așteptare). Cardul duce
> la **`/setari/<secțiune>`**, cu „← Setări” și butoanele secțiunii în cardul ei; N adaugă punct de lucru doar pe pagina lui. Legăturile vechi
> (`/setari#soferi`, `?istoric=…#jurnal-audit`) se redirecționează; „Primii pași”, Acasă și „Istoric” trimit direct la paginile noi. Alegerea
> s-a făcut pe machete (artifact „Setări WasteHouse — machete”, variantele A–D; proprietarul a ales C).
> Probe: `tsc` curat, `npm test` 37/37; e2e 1, 5, 7, 9, 11, 15, 16, 20, 21, 22, 24 verzi pe `eco_e2e_clienti` (1, 7, 9, 15, 22 adaptate; 20 și 21
> rămân pe ancore și apără redirectul — **negativă:** fără redirect, 21 cade). Garda `deploy-split.sh frontend`: exact cele două commituri; bundle-ul
> servit are „Autorizație valabilă până la”, `/setari/utilizatori` → 200.

> **17.09.2026, 16:38 — ✅ pe producție: clientul nou, în patru pași** (`ecoregistru-api` **v119**, `7f96522`; `ecoregistru-app` **v109**, `e7a77d6`; monorepo `844d898`; fără migrare). *Intrare scrisă pe 19.09.2026, la recitirea md-urilor față de `heroku releases`: felia era pe producție, dar jurnalul ăsta n-o avea — fusese notată numai în notele interne ale sesiunii paralele.*
> `/clienti/nou` (`NewClientPage.tsx`), din „Client nou” (N) sau din „Creează contul” pe o cerere (`?cerere=`): **Firma** (CUI → ANAF: nume, adresă, RC, CAEN, județ și localitate; CUI-ul cu cifra de control; județul adus la nomenclatorul FGO) · **Ce face** (tipul pe carduri + profilul) · **Abonamentul** (pachetele cu prețul din grilă, `POST /subscriptions/preview`; data de start, fondator, adresa de la pasul 1 sau alta, prima factură calculată; „Salvează fără abonament”) · **Administratorul** (invitația acum sau mai târziu). Consultantul are trei pași, fără abonament.
> Serverul salvează **totul sau nimic**: `POST /companies/onboard` (`ClientOnboardingService`) face firma — sau aprobă cererea cu datele verificate —, abonamentul și invitația într-o singură tranzacție, cu invitația ultima, fiindcă mailul pleacă pe loc. Un CUI deja luat sau un email cu cont desfac și firma.
> **Tot atunci, pe factura FGO:** adresa clientului fără județul și localitatea repetate (`util/BillingAddress`, **api v120**, 17:05) și cratimă simplă în locul lui „&ndash;” în mențiunea perioadei (**api v121**, 17:12).
> **Probe:** backend 898/108 (`ClientOnboardingIT` 5; **negativă:** fără `@Transactional` și fără garda de rol cad exact două teste); `npm test` 37; proba e2e **31** nouă.

> **17.09.2026, 16:01 — ✅ pe producție: Clienți pe taburi, Facturare pe pagini de la server** (`ecoregistru-api` **v118**, `d7129de`; `ecoregistru-app` **v108**, `5eeeb8f`; monorepo `85faec2`; fără migrare). *Intrare scrisă pe 19.09.2026, la recitirea md-urilor față de `heroku releases`: felia era pe producție, dar jurnalul ăsta n-o avea — fusese notată numai în notele interne ale sesiunii paralele.*
> Cerută de proprietar după ce a văzut tabelul Clienți: *„se dă mult scroll”*, iar *„la 2000 de facturi dai scroll până înnebunești”*. **Clienți** are taburi în adresă — Clienți · Cereri de cont (LED galben la cereri noi) · Cabinete; la consultant, Clienți · Echipa · Antetul cabinetului. **Facturare** cere pagina de la server: `GET /subscriptions/invoices?filter=&month=&q=&page=&size=`, cu numărătorile pe filtre din aceleași predicate (`InvoiceSpecifications`); pornește pe „De rezolvat” (căzute + restante), pe toate lunile, fiindcă luna curentă ar ascunde restanțele vechi; pagini de 50. Banii de pe Clienți vin din `GET /subscriptions/invoices/money`.
> **Probe:** backend 892/107 (`InvoicePageIT` 7, cu negative); probele e2e 29 și 30 aduse la zi.


> **17.09.2026, 15:37 — ✅ pe producție: tabelul Clienți ca tablou de lucru (F-B), panoul „Firmele mele” după ultimul termen al anului, scripturile curățeniei și ale demo-ului** (`ecoregistru-api` **v117**, `1c9d53b`, fără migrare, schema rămâne **V63**; `ecoregistru-app` **v107**, `32347ab`; monorepo `99425e8`).
> **(1) F-B** (`072a18e`): `GET /api/v1/companies/overview` (`MULTI_COMPANY`, 5 interogări oricât de lungă e lista; consultantul fără bani) —
> cifrele de sus, filtrele (Cer atenție, Fără abonament, Restanți, Emitere căzută, Fără utilizatori, Cabinete), coloanele Abonament / Ultima
> factură / Utilizatori / Fișa, „Deschide” + „⋯”, tasta **N** pentru „Client nou”; logica în `lib/clients.ts`. Proba e2e **30**.
> **(2) Panoul „Firmele mele”** (`81469ba`): „Termenele anului nu sunt generate” nu mai apare după ultimul termen al anului —
> `deadlinesGenerated` caută în aceeași fereastră ca termenele deschise; test nou în `ConsultancyOverviewIT`, negativa probată.
> **(3) `scripts/`** (`500075f` + `99425e8`): `curatenie-prod.sql` (istoric, cere `-v admin=<email>`), `curatenie-cloudinary.sh`, `numara-randuri.sql`,
> `demo/seed-demo.mjs` + `demo-data.json` (cere `WH_EMAIL`). ⚠️ `500075f` singur anula F-B și panoul; `99425e8` le repune — arborele
> egal cu `81469ba` + `scripts/`.
> Probe înainte de deploy: backend **886/106**, 0 eșecuri; `npm test` 36/36; build verde; CI pe `99425e8` verde. Garda
> `deploy-split.sh`: un singur commit pe fiecare parte. După: „No migration necessary”, `Started EcoRegistruApplication`,
> `/api/v1/companies/overview` → 401 fără token, bundle-ul servit are textele noi.

> **17.09.2026, 14:46 — ✅ pe producție: ecranul „Facturare”, CUI-ul cu cifra de control, „Verifică plata” și „Oprește” (F-A din `todo-clienti-abonamente.md`)** (`ecoregistru-api` **v116**, `50f9794`, **V63** aplicată; `ecoregistru-app` **v106**, `21655ad`; monorepo `ec29c53`).
> **(1) `/facturare`** (numai platforma, grupul Cabinet, tasta **B**): ultima rulare salvată în `billing_runs` (V63), rând cu rând — firma,
> motivul pe înțeles („CUI-ul „…” nu e valid: FGO nu-l acceptă”), facturile emise și plătite, abonamentele care nu încep; toate facturile
> tuturor clienților cu filtrele Toate · Căzute · Emise, neplătite · Restante · Plătite. Butonul de rulare a plecat din dialogul de abonament.
> **(2) „Verifică plata”** pe o factură (`payment_checked_at`, „verificat azi, 10:29”); fără chei FGO → „lipsesc cheile FGO”, fără cerere.
> **(3) „Oprește”** pe factura refuzată de FGO: se șterge; fără alte facturi, abonamentul se șterge, altfel se oprește în ziua dinaintea perioadei.
> **(4) CUI-ul** firmei, al cabinetului și al cererii de cont se verifică cu cifra de control (backend `util/Cui`, frontend `lib/cui.ts`).
> **(5)** Eticheta „Activ” din dialogul de abonament e verde. **(6)** Symlinkul `frontend/node_modules`, intrat în git la `198616e`, scos;
> `.gitignore` îl prinde și ca link — în arborele principal `frontend/node_modules` trebuie refăcut cu `npm ci`.
> **Probe:** backend `cleanTest test` **880/105** verde (negativa pe 5 reguli → exact 6 teste); e2e **29/29** pe bază nouă (`eco_e2e_facturare`),
> proba 29 nouă (facturile scrise cu `psql`, `E2E_DB`), negativă pe tasta B și pe butoanele rândului din rulare; capturi 1440/375.
> **Pe producție:** V63 „Successfully applied 1 migration”, `Started`, health 200; `GET /subscriptions/invoices` și `/billing/runs/last` → 401
> fără token (rutele există); bundle-ul `index-CSM5YjRW.js` are „Rulează facturarea acum”, `/facturare`, cheia CUI și nu mai are „Emite facturile scadente acum”.
> Baza de producție fusese curățată înainte (sesiunea paralelă, backup `b010`): 1 cont `PLATFORM_ADMIN`, 842 coduri, restul gol.

> **17.09.2026, 12:48 — ✅ pe producție: politica de confidențialitate numește Brevo și cyber_Folks, nu Zoho** (`ecoregistru-app` **v103**, `c9147a7`, din `main` `ca5c5fa`; api neatins).
> Tabelul de furnizori din `/confidentialitate` scria „Zoho — trimiterea e-mailurilor”, deși Zoho nu fusese niciodată furnizorul (mailul
> trimis mergea prin cPanel, din 17.09 prin Brevo). Acum: **Brevo** (e-mailurile automate, UE) și **cyber_Folks** (căsuța contact@ și
> site-ul, România). Aceeași corectură în registrul art. 30, DPA Anexa C și PDF-uri (repo-ul privat). **Probe:** tsc, `npm test` 27/27;
> pe producție bundle-ul `index-CQOz911O.js` are textul nou și niciun „Zoho”, pagina `/confidentialitate` arată cele șapte rânduri.

> **17.09.2026, ~13:45 — operare: urmărirea Brevo anonimizată, DMARC lăsat pe `p=none`** (niciun cod; producția api v115 / app v105).
> Mailul de invitație trimis prin Brevo trece SPF, DKIM (`brevo2`, `wastehouse.ro`) și DMARC. În „Show original” s-a văzut că Brevo rescrie
> linkurile (și pe „Alege parola”, prin `sendibt2.com`), pune un pixel de deschidere și antetul `List-Unsubscribe`. Din Brevo s-a putut doar
> **anonimiza** urmărirea (Transactional → Settings → Tracking → „Anonymous email tracking” = Yes). Rescrierea linkurilor și `List-Unsubscribe`
> nu se pot opri pe SMTP (Brevo le ține obligatorii; `list-help` doar pe Enterprise). Linkul rescris merge: deschiderea paginii de parolă nu
> consumă codul. Un destinatar care apasă „Dezabonare” nu mai primește nici resetările; se deblochează din „Blocked or unsubscribed contacts”.
> DMARC rămâne `p=none` până înainte de primul client (nu strică nimic; `quarantine` după un test din webmailul cPanel).
> Tot atunci, din sesiunea paralelă: app **v105** (`f93800d`, cantitatea adăugată după cântărire nu se mai șterge la editare) și api **v113–v115**
> (clientul FGO: `Content-Length`, răspunsul non-JSON în log, 409 reîncercat).

> **17.09.2026, 13:13 — ✅ pe producție (`ecoregistru-app` **v104**, `04fa66b`): Evidențe în kg, butonul fișei pe înțeles, selectul R/D** (monorepo `198616e`; api neatins de sesiunea asta, nicio migrare; documentele tipărite neatinse; bundle-ul `index-CdJshcWs.js` are textele noi).
> **(1) Kg pe Evidențe:** panoul „Pentru depunerea din 15 martie — totalul anului, pe cod” trece din tone în kg (coloanele `[kg]`), ca
> rândul de pe Termene (app v101). Andreea (AF, 14.09): se depune în kg. Pe ecranele generatorului nu mai e nicio cifră în tone, în afară de
> pragul de 1 t al Anexei 2 (colectori). **(2) Butonul fișei:** cu o predare „de cântărit” în an, butonul deschidea un dialog în locul PDF-ului
> și părea stricat. Acum nota stă sus pe pagină („2026: 1 linie fără cantitatea de la destinatar…”, cu „Arată predările”), iar dialogul are
> numele documentului în titlu, „Descarcă oricum” / „Completez întâi” și tokenii „Cântar” în loc de `amber-*` (la fel pe Dosarul de control).
> **(3) Selectul R/D** din „Adaugă deșeuri” arăta „Câmp obligatoriu.” ca rând gol înainte de orice salvare (văzut pe producție): acum „Alege
> codul R…” / „Alege codul D…”. **Probe:** tsc, build, `npm test` 26/26; e2e completă pe bază nouă (`eco_e2e_kgfisa`) 25/28 la prima rulare —
> 11 a prins un acord greșit în nota nouă („1 linie … n-au”, reformulată), 28 aștepta o descărcare în loc de tabul PDF (proba reparată),
> 18 a căzut o dată pe tasta I în timp ce Vite reîncărca modulele editate; rulate din nou: **10, 11, 18, 28 verzi**. Negative: `/1000` pe
> panou → proba 10 cade; fără notă și cu titlul vechi → proba 28 cade de 3 ori. Capturi 1440/375: 0px derulare laterală.
> **Testul formularelor noi pe producție (app v103), pe Demo Reciclare:** partenerul pe 3 pași (rubrica goală marcată la „Continuă”, pașii 2
> și 3, anulat fără salvare); „Adaugă deșeuri” prin `?nou=1` cu punctul de lucru ales, bonul care se umple, o predare de probă (15 01 01, 5 kg,
> R3, Colector Autorizat SA) → „Predarea e în evidență” cu Anexa 3 și avizul → „Încă una la fel” cu codul, R3 și partenerul, fără cantitate →
> „La fel ca data trecută” pe un formular nou → predarea ștearsă din „⋯”. Singurul defect: selectul R/D de la (3).

> **17.09.2026, ~12:40 — ✅ operare: mailul aplicației prin Brevo, monitorizare la 5 minute, CI verde** (`ecoregistru-api` **v112**, doar config; niciun cod de aplicație; `main` `7d774f9`).
> Din scanarea generatorului cerută de proprietar (17.09 dimineața, 9,4/10, ~98%). **(1) CI roșu pe `main` de la `01e9acf`:** proba e2e 9
> căuta cifra de sub 15 martie în tone, ecranul o scrie în kg — proba învechită, nu produsul; acum caută „kg”. Proba 27 (anul declarat)
> nu era în `e2e/run.mjs`, deci CI-ul n-o rula; adăugată. Local, pe bază nouă: 9 și 27 verzi; CI verde (frontend, backend, e2e).
> **(2) Mailul:** pe 17.09 la 07:15 UTC avertismentul de autorizație a căzut cu `Couldn't connect to host mail.wastehouse.ro:587`
> (timeout 10 s, de pe Heroku). Mailurile automate pleacă acum prin **Brevo** (SMTP relay, UE): domeniul autentificat (TXT de verificare,
> DKIM `brevo1`/`brevo2`, `_dmarc` cu `rua`), expeditorul `WasteHouse <contact@wastehouse.ro>` verificat, variabilele `MAIL_*` schimbate
> (api v112). Probat: „Parolă uitată” → Delivered în Brevo, ajuns în Inbox, linkul pe `app.wastehouse.ro`. ⚠️ Blocarea IP-urilor
> neautorizate pentru cheile SMTP (Brevo → Security → Authorized IPs) trebuie să rămână **oprită**: Heroku n-are IP fix și primul
> test a căzut cu `MailAuthenticationException`. Mailul primit pe contact@ rămâne în cPanel (MX neatins). **(3) Monitorizarea:**
> `uptime.yml` (`*/10`) a rulat de 6 ori în 24 h — GitHub amână cronul. UptimeRobot (5 min, mail + aplicația de telefon) pe API
> `/actuator/health`, `app.wastehouse.ro` și `wastehouse.ro`; toate Up. `uptime.yml` rămâne a doua plasă.

> **17.09.2026, ~02:40 — ✅ pe producție: agentul economic la tratarea proprie, nota SIM pentru un an fără deșeuri, persoana desemnată după Legea 17/2023, avertismentul pentru anul declarat** (`ecoregistru-api` **v111**, `214b3ac`; `ecoregistru-app` **v102**, `8740902`; din `main` `63e75d4`; nicio migrare).
> Întrebările AH, BB, AK, AP și C, închise din lege în locul specialistei (temeiul în `ecoregistru-docs/docs/intrebari-specialist.md`).
> **C:** o operație fără partener o face firma („prin mijloace proprii”, OUG 92/2021 art. 23 alin. (1)), deci fișa (cap. 3/4) și evidența
> centralizată scriu denumirea firmei, nu „în activitatea proprie” (HG 856/2002 anexa 1; ghidul SIM, PRODDES tabelul 2a și corelația
> PRODDES005). Schimbă un document tipărit: validarea specialistei se redeschide pentru fișă și evidența centralizată. **AP:** sub 15 martie,
> un an încheiat fără generări trimite la suportsim@anpm.ro (ghidul SIM, p. 8). **AK:** art. 23 alin. (4)–(5) în forma din 12.01.2023 —
> orice generator desemnează persoana, instruirea se cere doar cu autorizație de mediu; README-ul dosarului, explicația din profil și
> `surse-oficiale.md` §2.1b citau forma veche. **Anul declarat:** cu 15 martie bifat pentru anul mișcării, salvarea întreabă („Salvează
> oricum”), ștergerea și „Adaugă cantitatea” avertizează; doar web, doar avertisment. **Probe:** backend 864/104, 0 eșecuri (negative pe C și
> AK); `npm test` 27/27; e2e 27 (negativă: 4 căderi), 4, 23, 26. **Pe producție:** health UP, bundle-ul `index-BwQPIeNf.js` are cele trei texte noi.

> **17.09.2026 — ✅ pe producție: rândul de sub termenul de 15 martie, pe Termene, în kilograme** (`ecoregistru-app` **v101**, `5e95d05`, din `main` `01e9acf`; api neatins). **Pe producție:** bundle-ul `index-CTHomVzv.js` are textul nou.
> Scria „2026: 3 coduri, 1,060 t” — 1.060 kg în tone, cu virgula românească la zecimale, citit de proprietar ca o mie de tone („nu cred că
> e ok”). Tonele veneau din punctul 7 al auditului (04.09, art. 48 „în tone”), premisă închisă pe 14.09 de Andreea (AF: „kg”). Acum
> `formatKg` (`lib/units.ts`, punct la mii, fără zecimale forțate): „2026: 3 coduri, 1.060 kg”. Numai rândul de pe Termene, la cererea
> proprietarului („la aia unde era confuzia”); tabelul în tone de pe Evidențe și pragul de 1 t/an al Anexei 2 rămân. **Probe:** `npm test`
> 25/25 (1 nou), tsc, build.

> **17.09.2026, 01:38 — ✅ pe producție: nota despre OIREP sub termenul de 25 februarie (Anexa 1 Ambalaje), pe Termene** (`ecoregistru-app` **v100**, `4a954a0`, din `main` `b2b8876`).
> Scanarea de conformitate, pct. 1: Ordinul 794/2012 art. 1 alin. (1)–(2) cere Anexa 1 numai celor care își îndeplinesc singuri
> obiectivele; cu un OIREP raportează OIREP-ul, iar profilul nu întreabă asta. Proprietarul a ales să i se spună omului chiar
> acolo unde vede termenul: `noteFor` (`lib/deadlines.ts`) + `strings.deadlines.typeNote`, tipărită sub numele raportării în tabel
> (`max-w-md`) și pe cardul de telefon. Termenul rămâne; doar Anexa 1 Ambalaje are notă. **Probe:** `npm test` 24/24 (1 nou), tsc,
> build. **Pe producție:** bundle-ul servit (`index-DAYZqpcl.js`) conține nota; proprietarul: „apare, e frumos”.

> **17.09.2026, 01:37 — ✅ pe producție: README-ul dosarului nu mai pune „Termen: 25 februarie” pe Anexa 3 Ambalaje a unui generator** (`ecoregistru-api` **v110**, `f4ae0f9`, din `main` `b2b8876`; nicio migrare).
> Din scanarea de conformitate cerută de proprietar (pct. 4): Ordinul 794/2012 art. 4 alin. (1) numește colectorii, comercianții,
> reciclatorii și valorificatorii, nu generatorul (`docs/surse-oficiale.md` §2.11), iar la generator foaia e tipărită la cerere, cu
> ieșirile (16.09). README-ul scria totuși termenul la orice cont — un termen inventat pe hârtia citită de inspector. Acum
> `AuditFileService.readme` scrie la generator „numai cu ieșirile: tipărită la cerere … fără termen”; la colector rămâne cum era.
> Documentul în sine (xls/PDF) e neatins, deci validarea Andreei din 16.09 nu se redeschide. **Probe:** `AuditFileIT` 21/21;
> negativa (fără schimbare) cade exact testul `theDossierCarriesAnexa3PackagingPerWorkPointThatMovedPackaging`. Suita completă nu s-a rulat.

> **17.09.2026, 01:28 — ✅ pe producție: „Ce urmează” după o mișcare nouă** (`ecoregistru-app` **v99**, `7c4b996`, din `main` `5a2766d`;
> **api neatins**, v109, nicio migrare; datele salvate neschimbate). Regula 12 a formularelor, cerută de proprietar după app v98: o mișcare
> **nouă** arată „Predarea e în evidență” (Intrarea / Ieșirea), bonul citit din răspunsul serverului și, sub „Ce urmează”, documentele pe
> care rândul le poate tipări (aceleași reguli ca meniul „⋯”: Anexa 3, avizul, Anexa 2 la colectori), plus unde se scrie cantitatea la o
> predare fără cântar. „Încă una la fel” pornește formularul cu alegerile ei, fără cantitate, volum, cântărire, notițe, dată sau document.
> Editarea păstrează mesajul scurt. **Defect vechi reparat odată cu ea:** formularul deschis prin `?nou=1` (butonul „+” de pe telefon,
> „Adaugă deșeuri” din panou de pe alt ecran, „Primii pași”) pornea înaintea punctelor de lucru și rămânea fără punct, deci salvarea cădea
> pe o rubrică neatinsă. **Probe:** tsc, e2e **26/26** pe `eco_e2e_formulare` (proba **26** nouă; negative: fără `onCreated` cad 5+
> verificări, fără efectul punctului de lucru cade una; proba 18 știe acum de 15 01 01, pe care îl lasă o rulare întreruptă a probei 26).
> **Pe producție:** bundle-ul servit conține textele noi, `app.wastehouse.ro/generare` 200.

> **17.09.2026, 01:11 — ✅ pe producție: partenerul pe trei pași și „Adaugă deșeuri” pe înțeles** (`ecoregistru-app` **v98**, `df7ff45`, din
> `main` `f14f62e`; **api neatins**, v109, nicio migrare; **aceleași rubrici, reguli și date trimise** — doar felul de a întreba). Proprietarul:
> „fă 1 și 2, dar hai să le gândim frumos; nu schimba informația”, cu fonturile curente. **Partenerul** (`PartnersPage.tsx`, `FormStepRail`):
> Despre partener (CUI întâi, ANAF) · Ce face pentru tine (tipul pe carduri — „Generator” ascuns la un cont doar generator —, „Cine duce
> deșeul de la tine la el?” pe două carduri, cine pe cine facturează, proveniența ambalajelor pe taste doar la colectori, cu textul care o
> deosebește de „Calitatea” din Setări) · Autorizația de mediu (numărul și „Viza e valabilă până la” sus; emiterea, decizia de viză și bifa
> „Autorizație integrată veche, cu termen?” sub „Detalii de pe hârtii”; în listă „Viză necompletată” galben — întrebarea **BB** la Andreea,
> după verificarea vizei în OUG 195/2005 art. 16–17, Ordinul 1150/2020 și art. 34¹ OUG 92/2021, lista ANMAP încă inexistentă). **Mișcarea**
> (`MovementFormDialog.tsx`): o pagină, ordinea rubricilor neatinsă, titluri care întreabă, taste sub șapte opțiuni, destinul pe carduri,
> bonul mișcării în dreapta, „La fel ca data trecută” (alegerile ultimei mișcări cu același cod, fără cantitate/dată/document). Documentele
> tipărite nu se schimbă. **Probe:** tsc, build, `npm test` 23/23, e2e **25/25** pe `eco_e2e_formulare` după rebazare (8, 9, 10, 23
> adaptate), drumul datelor prin ecran citit din API (partener cu toate rubricile, mișcare cu „La fel ca data trecută”, editare fără
> schimbări = identic), capturi 1440/375, tabelul Parteneri 1114/1114. **Pe producție:** bundle-ul servit conține textele noi,
> `app.wastehouse.ro/login` 200. Regula: `docs/stil-interfata.md`, „Formularele din aplicație după cererea de cont”.

> **17.09.2026, 01:02 — ✅ pe producție: mesajul de după import și exportul complet al unei firme** (`ecoregistru-app` **v97**, `1f1d46c`,
> din `main` `44d97a3`; **api neatins**, v109, nicio migrare). După un import salvat, mesajul spunea că ambalajele și transportul se
> completează în aplicație, deși fișierul le poate aduce din a doua felie; acum: „Ce n-a fost în fișier (ambalajele, transportul pentru
> Anexa 3, CNP-ul șoferului) se completează în aplicație, pe fiecare mișcare.” **Exportul promis în DPA §10.1:** `scripts/export-client.sh <CUI>`
> (`b312fb8`) scrie câte un CSV pe tabel dintr-o tranzacție read-only (tabelele cu `company_id` descoperite din schemă, plus cele legate prin
> părinte) și descarcă atașamentele și buletinele de pe Cloudinary cu URL semnat ca `CloudinaryStorageService#signedUrl`; fără parole,
> sesiuni, abonamente și plăți. **Probe:** semnătura identică cu SDK-ul cloudinary-core 2.3.0 pe aceeași cheie de test; pe baza locală,
> Demo Reciclare → 28 de tabele, zero rânduri din celelalte trei firme. Nerulat pe producție. **Pe producție:** bundle-ul servit
> (`index-OX1x_Wm4.js`) are textul nou.

> **17.09.2026, 00:31 — ✅ pe producție: BUG-024, firma aleasă ține de tab** (`ecoregistru-app` **v96**, `d167acc`, din `main` `97236c0`;
> **api neatins**, v109, nicio migrare). Firma aleasă de platformă sau de consultant stătea numai în `localStorage`, comun tuturor taburilor.
> Schimbată într-un tab, ea muta cererile celorlalte, deși ecranul lor arăta tot firma veche. S-a prins la primul import de pe producție:
> verificarea Onsia SRL a plecat spre Ardeal Reciclare SRL, fără să se salveze nimic. Acum `tabScopedStore` (`frontend/src/lib/tenantStore.ts`)
> ține firma în `sessionStorage`, iar `localStorage` e doar punctul de pornire al unui tab nou. **Probe:** `npm test` 23/23 (3 noi, cu două
> taburi; negativa fără `sessionStorage` dă 2 eșecuri), tsc, build. **Pe producție, cu două taburi:** B trece pe Demo Reciclare, A rămâne pe
> Onsia și cererile lui pleacă tot cu Onsia.
