# Stadiu — EcoRegistru

Jurnalul feliilor livrate, în ordinea în care au fost construite. Fiecare intrare marcată ✅
rulează local și are testele verzi.

- ✅ **B0 — Fundația:** auth JWT (login, verificare email, resend, reset parolă), multi-tenancy
  (`company_id` pe fiecare tabelă + `TenantContext` + `TenantFilter`, izolare la nivel de request;
  PLATFORM_ADMIN comută tenantul via `X-Tenant-Id`), roluri `PLATFORM_ADMIN`/`ADMIN`/`OPERATOR`/`CLIENT_VIEWER`,
  schema completă Faza 1 (Flyway), seed nomenclator coduri din CSV, envelope de erori, OpenAPI, seed tenant demo.
- ✅ **B1-core:** CRUD `WasteMovement` (+atașamente Cloudinary, idempotent, soft-delete, `?since=`),
  `Partner` CRUD, `WorkPoint` CRUD, căutare `WasteCode`.
- ✅ **Faza M:** stare fizică + cod operație R/D (validat) pe mișcări, flag obligație AFM pe firmă,
  stoc cumulativ pe evidența lunară (Flyway `V3`).
- ✅ **UI-1 / U0:** infra frontend (toast, primitive UI, hooks TanStack Query, tipuri) +
  ecran **Setări / Puncte de lucru** (CRUD).
- ✅ **UI-1 / U1:** ecranul **Mișcări** — tabel + filtre (lună / punct de lucru) + dialog de
  adăugare rapidă/editare cu combobox de căutare coduri, validare R/D condiționată, atașamente
  (drag-drop) și ștergere soft. Plus primitive noi (`select`, `date-input`, `combobox`, `file-dropzone`).
- ✅ **UI-1 / U2:** ecranul **Parteneri** — tabel + CRUD + badge expirare autorizație + dezactivare soft.
- ✅ **EVID / E1 (backend):** motor de evidență — `EvidenceCalculator` agregă mișcările în linii
  lunare per (punct de lucru, cod) cu **stoc cumulativ**; `GET /api/v1/evidences`,
  `POST /api/v1/evidences/regenerate?year=`. Test de corectitudine verde.
- ✅ **EVID / E3 (frontend):** ecranul **Evidențe** (`/evidente`) — filtre an / lună / punct de lucru,
  tabel cu totaluri pe operațiune + stoc cumulativ (roșu când e negativ), buton **Regenerează**
  (gated pe rol), empty-state per an.
- ✅ **EVID / E2-generic (export):** descărcare „tabel generic" (rezumat neoficial) din evidență —
  `GET /api/v1/evidences/export?year=&month=&workPointId=&format=xlsx|pdf` (Apache POI pentru `.xlsx`,
  OpenPDF pentru `.pdf`), cantități în KG, antet „rezumat generic (neoficial)". Citire pentru orice
  membru al firmei (inclusiv `CLIENT_VIEWER`). Test `EvidenceExportIT` verde.
  ⛔ Formatul oficial Anexa 1 rămâne blocat pe expert — vezi nota de reglementare din README.
- ✅ **UI-1 / U4 (header + selector tenant):** firma curentă în header; `PLATFORM_ADMIN` comută tenantul
  printr-un selector. Endpoint nou `GET /api/v1/companies` (doar PLATFORM_ADMIN) + `tenantName` în login.
- ✅ **FAZA TERMENE (calendar + alerte):** ecran **Termene** (`/termene`) — auto-generare SIM anual
  (15 martie) + AFM lunar (doar firme cu obligație AFM), marcare finalizat / redeschide; scheduler zilnic
  cross-tenant cu alerte email **T-7 / T-1** (dedup pe firmă). ⚠️ Trimiterea reală de email = blocată pe SMTP
  (cod complet, degradează grațios). *Deblocat pe 24.08 — vezi felia „Mail real".*
- ✅ **FAZA DOSAR (dosar de control):** `GET /api/v1/audit-file?year=` → **ZIP** cu evidența (xlsx+pdf),
  PDF autorizații parteneri și atașamentele mișcărilor; ecran **Dosar de control** (`/dosar-control`).
- ✅ **FAZA DASH (panou):** ecranul `/` — stat tiles (mișcări luna curentă, termene deschise/depășite,
  autorizații care expiră) + liste (termene următoare, autorizații aproape expirate).
- ✅ **FAZA CLIENȚI (management firme + invitații):** `PLATFORM_ADMIN` creează/editează firme
  (`POST`/`PUT /api/v1/companies`, CUI validat + unic) și **invită utilizatori** pe o firmă
  (`POST /api/v1/companies/{id}/users` — user creat inactiv + email de setare parolă, refolosind fluxul
  reset-parolă). Ecran **Clienți** (`/clienti`), vizibil doar pentru PLATFORM_ADMIN. `CompanyManagementIT` verde.
- ✅ **Deploy Heroku (2026-08-22, `0fd09ae`, branch `deploy/heroku-split`):** backend (`ecoregistru-api`)
  și frontend (`ecoregistru-app`) rulează pe dyno Basic. Auto-deploy din **repo-urile split**
  (`whoisonica/ecoregistru-backend` ← remote `newrepo`, `whoisonica/ecoregistru-frontend` ← `ferepo`);
  push-ul pe `origin` (monorepo) **nu deployează nimic**. Ambele repo-uri au commit-uri proprii, deci
  actualizarea lor se face cu `git subtree split` + **cherry-pick** peste capul remote-ului, niciodată force.
- ✅ **ETAPA 0 — Documentare legislativă (2026-08-22):** verificare integrală pe surse primare
  (Portal Legislativ, EUR-Lex, sgglegis.gov.ro). Nou: `docs/surse-oficiale.md` (citate verbatim cu
  link + dată). Corectat `docs/legislatie.md` (6 corecții, inclusiv **formula stocului**, care era
  greșită și în doc, și în cod). Actualizat jurnalul de răspunsuri + documentul trimis specialistei.
  **8 din 9 blocante 🔴 închise.** Zero cod atins.
- ✅ **ETAPA 1 — Nomenclator LED (2026-08-22):** cele **842 de coduri** ale Listei Europene a
  Deșeurilor, extrase din Decizia 2014/955/UE (EUR-Lex, versiunea RO) de un script comis în repo
  (`scripts/generate_waste_codes.py`), nu copiate de mână. Seed-ul
  `backend/src/main/resources/seed/waste_codes.csv` păstrează titlurile de capitol și subcapitol ca
  structură, iar `V4__reseed_waste_codes` îl reîncarcă peste cele 10 coduri-paravan ale lui `V2`
  (`ON CONFLICT DO UPDATE`; denumirile oficiale conțin virgule, deci linia se taie la prima și la
  ultima virgulă). Cele cinci validări rulează ca test (`WasteCodeSeedTest`): unicitate, format de
  6 cifre, capitol și subcapitol corecte pentru fiecare cod, plus **amprenta pe capitole**
  (842 coduri, 408 periculoase). `ApplicationBootIT` verifică reîncărcarea în DB. Suită verde.
  **Livrat în producție pe 23.08.2026** (`ecoregistru-api` release v8): Flyway a aplicat `V4` pe baza
  Heroku în 56 ms, aplicația a repornit curat, iar codurile se văd în combobox-ul de pe Mișcări.
  Commit-uri: `53f5e8c` (seed), `4efff97` (docs), `491a241` (taskul Gradle `stage`, adus înapoi din
  repo-ul de deploy — lipsea din monorepo și ar fi rupt primul subtree split proaspăt).
- ✅ **ETAPA 2a — Seam-ul de registru (2026-08-23):** cele două evidențe care azi împart o tabelă
  au fost separate logic, fără să se șteargă sau să se mute vreo linie. `WasteMovement` primește
  `register` (`ANEXA_1` / `ART_48`). Trei reguli, validate în service și acoperite de test: preluarea
  de la terți nu ajunge niciodată în Anexa 1 (art. 2 alin. (1)), deșeul propriu nu iese niciodată din
  ea (art. 1 alin. (1)), iar **orice ieșire de pe amplasament poartă un cod R/D** — fișa nu are
  coloană „predare", iar cap. 3 și 4 raportează cantitatea alături de „Operaţia de valorificare"/„de
  eliminare" și de operatorul care o face. Litera V/E din cap. 2 nota 3 se **derivă** din cod
  (`WasteOperationCode.treatmentPurpose()`), nu se stochează. `CompanyType` a devenit
  comutator real (`keepsArt48Register()`): o firmă doar-generator nu poate scrie în registrul art. 48.
  Entitățile `Reception` / `Delivery` există ca schemă (recepția = document primar, cu preț pentru
  contribuția AFM de 2%), **fără ecrane** — alea sunt Etapa 8, când se mută și mișcările `COLLECTED`,
  o singură dată. Migrarea **`V5`** e aditivă: nimic șters, backfill `COLLECTED → ART_48`, iar o firmă
  marcată „generator" care avea deja preluări e lărgită la `BOTH` ca să nu-i blocheze liniile.
  Verificată prin rulare într-o tranzacție cu rollback pe baza de dev, pe 35 de mișcări reale.
  Suită verde (63 de teste; `RegisterSeamIT` 9/9).
  ⚠️ **O restanță de clasificare pe care nicio migrare nu o poate ghici** (predările de marfă
  preluată) și **o întrebare deschisă** către specialistă (ce cod se trece la predarea către un
  colector) — detalii mai jos.
- ✅ **ETAPA 2b–2d — Formula de stoc a Anexei 1 (2026-08-23):** `EvidenceCalculator` calculează
  acum identitatea pe care o cere fișa, nu una inventată:
  `stoc = stoc_anterior + generat − valorificat − eliminat − ieșiri neclasificate`. Ce s-a schimbat,
  punct cu punct:
  - **intră doar registrul `ANEXA_1`.** Marfa preluată de la terți nu mai ridică stocul propriu
    (HG 856 art. 2 alin. (1)); `totalCollected` iese din răspuns, din export și din ecran.
  - **predarea nu mai e o coloană separată.** Fișa n-are „predat", deci fiecare predare aterizează în
    „valorificată" sau „eliminată final" după familia codului R/D al destinatarului. Cantitatea
    predată rămâne ca **memo** („din care predat"), niciodată ca termen al stocului — o ieșire
    fizică, o singură scădere.
  - **12 rânduri pe an**, chiar și în lunile fără mișcări: formularul e un tabel de 12 rânduri și
    stocul trebuie să se citească pe fiecare linie.
  - **perechile cu stoc reportat și zero mișcări nu mai dispar** din raport: December-ul anului
    anterior le ține în viață.
  - **regenerarea cascadează.** Stocul e cumulativ între ani, deci o corecție pe 2025 reconstruiește
    și 2026 (`cascadedYears` în răspuns, mesaj dedicat în UI). Înainte, anii următori rămâneau greșiți.
  - **ieșirile fără cod R/D** (predările vechi) se scad din stoc, dar nu intră în nicio coloană
    oficială: `totalUnclassifiedOut` + `incomplete` pe linie. Nu se ghicește o operațiune ca să se
    închidă fișa.
  - **predările suspecte de marfă preluată** (aceeași pereche punct-de-lucru/cod are și activitate
    art. 48) se marchează `resaleSuspected` — semnal, nu rescriere; Etapa 8 le mută.
    ⚠️ *Scos pe 24.08.2026 (G8), cu tot cu coloană: specialista a închis subiectul preluării de la
    terți pentru modulul de generatori. Separarea registrelor rămâne.*
  Migrarea **`V6`** e aditivă (`total_unclassified_out`, `resale_suspected`, default pe
  `total_collected`) și **golește cache-ul** `monthly_evidences`: liniile vechi arătau o Anexă 1 pe
  care legea n-o recunoaște, iar tabela e prin contract regenerabilă din mișcări.
  Suită verde: **73 de teste** (63 înainte), din care `EvidenceCalculatorIT` rescris (8 teste) și
  `Anexa1FormConformanceIT` nou (5 teste, Etapa 2c). Verificat și pe Postgres-ul de dev: `V6` aplicată
  în 30 ms, regenerarea a produs 84 de linii pentru 2026, cu 13 linii `incomplete` (exact predările
  vechi fără cod) și 1 linie `resaleSuspected` — sticla predată din marfă preluată, care iese acum
  cu stoc negativ vizibil în loc să fie compensată tăcut din deșeul propriu.
  ⚠️ **Șablonul specialistei e gol.** `documente oficiale/RAPORTARE DESEURI GENERATE.xlsx` (foile
  `20 03 01`, `20 01 01`, `15 01 02`) nu conține nicio cifră — toate celulele de cantitate sunt
  goale și fiecare TOTAL AN e 0. Deci Etapa 2c n-a putut „reproduce cifrele"; ce **poartă** fișierul
  sunt formulele ei, și pe alea le fixează testul: `C26=SUM(C14:C25)` (TOTAL AN = suma celor 12
  rânduri), `F26=C26-D26` (stoc = generat − tratat) și linia de antet „Stoc: 0 kg". Două observații
  colaterale: coloana `Secția` din cap. 2 e **constantă pe 12 luni** („birouri", „productie") — exact
  ipoteza de profil implicit a Etapei 3 —, iar antetul cap. 3/4 al șablonului încă trimite la
  **Legea 211/2011**, abrogată de OUG 92/2021. Fișierul e gitignored, deci niciun test nu-l citește.

- ✅ **ETAPA G1 — fundația modulului de generatori (2026-08-23, după meeting-ul de 2 ore cu
  specialista):** s-a decis ca **modulul de generatori** să fie construit primul, „de la început”:
  cont → firmă cu adresă → puncte de lucru → generatori interni → parteneri → mișcări. Ce s-a
  schimbat efectiv, punct cu punct:
  - **Registru închis.** `POST /api/v1/auth/register` **a fost șters**, împreună cu
    `RegisterRequest` și flagul `app.registration-enabled`. Nu mai există înregistrare liberă nici
    măcar dezactivată: un cont există fiindcă supportul a creat firma și a invitat utilizatorul, pe
    baza formularului completat de client (`POST /api/v1/companies` +
    `POST /api/v1/companies/{id}/users`, ambele PLATFORM_ADMIN). Un endpoint dezactivat ar fi rămas
    la un flag distanță de a fi deschis.
  - **Partenerii au un rol comercial**, separat de ce sunt autorizați să facă (`PartnerType`):
    **client** (îi predai deșeu și îi facturezi tu) și **furnizor** (îți prestează serviciul și îți
    facturează el). Două flaguri, nu un enum, fiindcă **același partener e des amândouă** — îi vinzi
    cartonul și cumperi de la el ridicarea menajerului. Serviciul refuză un partener fără niciun rol.
    **`V7` nu ghicește** rolul partenerilor existenți: direcția facturii nu se poate deduce din nimic
    din ce stocăm, deci rămân „rol nestabilit”, iar editarea îi completează — exact tratamentul pe
    care `V5` l-a dat predărilor fără cod R/D.
  - **Culorile cerute la meeting:** verde = client (banii intră), chihlimbar = furnizor (banii ies),
    gri = rol nestabilit. Un chip per rol, deci „ambele” se citește ca ambele, nu ca o a treia
    categorie. Plus filtru pe rol în ecranul Parteneri, inclusiv pe „rol nestabilit”.
  - **Generator intern** — al treilea nivel de locație, sub punctul de lucru: birouri, producție,
    cantină. Singurul fără adresă proprie (stă în adresa punctului de lucru), fiindcă e exact ce
    tipărește **cap. 2 din Anexa 1 în coloana „Secţia”**. Confirmat de toate fișierele completate
    primite: valoarea e **constantă pe cele 12 luni** ale unei foi. Entitate + CRUD + ecran în
    Setări; mișcarea poartă opțional secția din care a venit deșeul, și refuză o secție a altui
    punct de lucru. Nu se poate muta între puncte de lucru: ar rescrie coloana „Secţia” de pe fișele
    deja tipărite.
  - **„Predare” nu mai e operațiune.** `WasteOperation.HANDED_OVER` **a dispărut**. Anexa 1 cap. 1
    are patru coloane de cantitate și niciuna nu e „predat”, iar cap. 3 / cap. 4 raportează
    cantitatea împreună cu operaţia R/D **și** cu „agentul economic care efectuează operaţia”. Deci
    predarea la un reciclator e o **valorificare făcută de partenerul acela**, iar predarea la o
    groapă o **eliminare făcută de el**: partenerul spune că a fost predare, codul spune ce se
    întâmplă cu deșeul. Partenerul devine opțional peste tot (gol = ai făcut-o tu, pe amplasament).
    `V7` convertește după codul R/D, deci nu ghicește nimic: cod R → `RECOVERED`, cod D → `DISPOSED`.
  - **Rândurile vechi fără cod R/D** nu pot fi clasificate retroactiv, deci primesc o stare proprie,
    `UNCLASSIFIED_OUT` („ieșire neclasificată”) — exact ce raporta deja `V6`: cantitatea iese din
    stoc, nu intră în nicio coloană oficială, iar linia e `incomplete`. **Nu se poate alege** din
    formular; apare doar la editarea unei linii vechi, cu explicația a ce trebuie completat.
  - **Operațiunile disponibile depind de tipul contului** (`CompanyType.allowedOperations()`), și
    în ecran, și în service. Singura care variază e **preluarea de la terți**: un generator pur nu
    are registru art. 48, deci n-are ce prelua. `GENERATED` rămâne la toate tipurile, deliberat —
    art. 2 alin. (1) obligă și un colector să țină Anexa 1 pentru deșeul din activitatea proprie
    (refuzul de la sortare inclusiv). Ecranul află tipul firmei din endpointul nou
    `GET /api/v1/companies/current`, primul din `CompanyController` care nu e platform-only.
  - **Litera „E” din cap. 2 nota 3 nu se mai scrie.** Nota definește `V - pentru valorificare` și
    `E - în vederea eliminării`, dar practica a renunțat la a doua, iar cele **zece Anexe 1
    completate** primite o confirmă aproape unanim: pe toate fișele de valorificare (Cluj,
    Timișoara, Bragadiru, Oradea) scrie `V` pe toate cele 12 rânduri, iar pe fișele de eliminare
    (20 03 01, 19 12 12) scrie liniuță. `E` apare **o singură dată în tot corpusul** — Cluj 2022,
    codul 19 12 12 — iar același client a pus liniuță în 2023 și 2024. Deci `TreatmentPurpose` are
    un singur membru, iar `WasteOperationCode.treatmentPurpose()` întoarce `null` pentru familia D:
    celula rămâne goală, exact ca pe formularele completate. Eliminarea e identificată de codul D
    din cap. 4, lângă operator.
  Migrarea **`V7`** e aditivă (roluri de partener, tabela `internal_generators`,
  `waste_movements.internal_generator_id`), plus conversia predărilor și golirea cache-ului
  `monthly_evidences` — memo-ul „din care predat” înseamnă acum „partea din valorificat + eliminat
  pe care a făcut-o un partener”, cu care nicio linie veche nu fusese calculată.
  Suită verde: **84 de teste** (73 înainte), din care `GeneratorModuleIT` nou (8 teste) și
  `RegisterSeamIT` rescris pe modelul fără predare. Frontend-ul compilează.

- ✅ **ETAPA G2 — profilul de cont și formularul din care se naște (2026-08-23):** clientul
  completează un formular cu întrebări punctuale, supportul creează contul din el, iar de-atunci
  ecranele oferă **doar ce îi trebuie tipului lui de activitate**. Trei piese:
  - **Formularul de cerere** (`/cerere-cont`, public) — singura scriere publică din aplicație și
    singura intrare într-un registru închis. Creează o **cerere**, niciodată un cont: fără user,
    fără sesiune, și nu întoarce nimic despre ce a scris, ca să nu poată fi folosit ca sondă pentru
    firmele existente. Doar patru câmpuri sunt obligatorii — denumire, CUI, tipul activității și un
    email de răspuns — fiindcă un formular care refuză să fie trimis e un formular pe care nu-l
    trimite nimeni. Întrebările sunt fix profilul firmei, în ordinea în care un client le poate
    răspunde: cine ești → unde lucrezi (adresă sediu **și** adresă punct de lucru, separat, fiindcă
    evidența se ține pe punct de lucru) → pe cine sunăm → autorizația → *doar dacă preiei de la
    terți:* cu ce transporți + licența → ce se întâmplă cu deșeul.
  - **Aprobarea** (`POST /api/v1/account-requests/{id}/approve`, PLATFORM_ADMIN) copiază
    răspunsurile pe o firmă reală, profil inclus, și creează punctul de lucru pe care l-a numit
    formularul. **Nu invită pe nimeni**: crearea contului și darea accesului rămân două acte
    separate. Cererea nu se șterge niciodată — e urma de hârtie din spatele profilului, adică
    răspunsul la „de ce vede clientul ăsta doar cinci coduri?”. Lista cererilor apare în ecranul
    **Clienți**, sub firme.
  - **Profilul restrânge ce se vede.** `Company` primește operațiunile R/D declarate, codurile de
    deșeu din autorizație și, pentru colectori, cu ce transportă + licența de transport mărfuri
    (aceleași câmpuri pe care le tipărește Anexa 3 pe partea transportatorului). Ecranul de mișcări
    oferă doar codurile din profil, iar serviciul le și impune. **Profil gol = fără restricție**,
    deliberat: conturile existente n-au completat formularul, iar a restrânge pe un răspuns gol
    le-ar ascunde opțiuni pe care le folosesc azi. Codul mișcării editate rămâne mereu în listă, ca
    o linie veche să nu-și piardă tăcut operațiunea la salvare.
  - **Cap. 2 al Anexei 1 apare sub codul de deșeu**, cum s-a cerut: **Stocare — tipul** (nota 1: RM,
    RP, BZ, CT, CF, S, PD, VN, VA, RL, A) și **Tratare — ce se face** (nota 2: TM, TC, TMC, TB, TT,
    D, A), ambele verbatim din formular, ambele opționale. A treia coloană a capitolului, „Scopul”,
    **nu se stochează**: se derivă din codul R/D și e doar `V`. Atenție la coliziunea de abreviere
    pe care o face chiar formularul: `D` din nota 2 e **deshidratare**, nu un cod de eliminare — de
    aceea sunt tipuri diferite (`TreatmentMethod` vs. `WasteOperationCode`).
  Migrări: **`V8`** (profilul firmei + cele două nomenclatoare pe mișcare) și **`V9`**
  (`account_requests` + codurile declarate). Ambele aditive.
  Suită verde: **92 de teste** (84 după G1), din care `AccountRequestIT` nou (4 teste) și patru
  teste noi în `GeneratorModuleIT` pentru restrângerea după profil. Frontend-ul compilează.

- ✅ **ETAPA G3 — Anexa 3, dovada predării (2026-08-23):** după ce mișcarea e înregistrată, se
  generează **formularul de încărcare-descărcare deșeuri nepericuloase** (Anexa 3 la HG 1061/2008)
  ca PDF, din `GET /api/v1/movements/{id}/anexa3`. Layoutul urmează rubrică cu rubrică modelul
  completat primit de la specialistă (seria HMB 180): transportatorul și delegatul în stânga, cele
  două date lângă, deșeul și bifele „Destinat:” la mijloc, cantitatea, apoi expeditorul și
  destinatarul în dreapta, observațiile la final.
  - **Cantitatea poate lipsi, declarat.** Pe modelul primit cifra — „1,02” — e **scrisă de mână**,
    după cântărire. Un magazin de cartier n-are cântar: predă deșeul, iar colectorul îl cântărește
    la depozit. Bifa **„Se cântărește la descărcare”** face câmpul de cantitate inactiv (gri),
    mișcarea se salvează fără cantitate, iar formularul se tipărește cu rubrica goală și o linie
    care spune de ce. **Nici zero, nici estimare** — ar fi o cifră inventată și pe un document
    legal, și în stocul din Anexa 1. Bifa cere un destinatar: cineva trebuie să facă cântărirea.
  - **Linia lunară devine provizorie**, nu tăcută: `awaitingWeighing` marchează luna în care o
    ieșire încă așteaptă cântarul, iar `incomplete` o include. Se completează editând mișcarea când
    vine cântarul.
  - **Volumul în mc** e singura măsură a celui fără cântar, și e o rubrică pe care formularul o are
    („17 mc” pe model). Nu ține loc de kilograme: Anexa 1 se ține în kg.
  - **Două refuzuri, ambele legale.** Titlul formularului spune *nepericuloase*, deci un cod
    periculos e refuzat cu mesajul care trimite la formularul de expediție din anexa 2 (neimplementat),
    nu tipărit pe documentul greșit. Și formularul descrie o predare, deci cere un destinatar.
  - **„Destinat:” e multiplu**, nu unic: pe model sunt bifate două, „Colectării” și „Valorificării”.
  - **Seria și numărul** se alocă la prima generare și se păstrează, deci retipărirea dă același
    document (index unic pe firmă). Seria e configurabilă — multe firme au carnete pre-tipărite cu
    seria lor.
  - **Diacriticele** se randează prin **Cp1250**: Cp1252 n-are ă/ş/ţ și le-ar fi șters de pe un
    formular oficial. Formele cu virgulă se pliază pe cele cu sedilă, care sunt și cele folosite de
    textul legal.
  Migrarea **`V10`** e aditivă, cu o singură relaxare: `waste_movements.quantity` devine nullable.
  Suită verde: **99 de teste** (92 după G2), din care `Anexa3FormIT` nou (7 teste).

- ✅ **G3b — foaia 1 și 2 din schițe, închise (2026-08-23):** patru lucruri, toate din notițe.
  - **Adresele erau deja acolo:** adresa firmei se editează în *Clienți*, adresa punctului de lucru
    în *Setări*, iar formularul de cerere le cere pe amândouă separat — evidența se ține pe punct
    de lucru, nu pe firmă, iar cele două adrese sunt des diferite.
  - **Anexa 3 se tipărește acum cu casetele separate**, ca în modele: fiecare rubrică în chenarul
    ei — patru pe coloana transportatorului (transportator · delegat + nr. auto · licență · data
    expirării + semnătura), cinci pe coloana părților, două pe date. Înainte erau șase coloane cu
    text îngrămădit; acum pagina se citește și se semnează rubrică cu rubrică, ca originalul.
  - **„Predare” a ieșit și din evidență.** Coloana memo „din care predat” a dispărut de pe ecran și
    din exportul generic. Nu doar fiindcă fișa n-o are: erau **aceleași kilograme afișate a doua
    oară**, în interiorul coloanelor de valorificat/eliminat, iar cine citea tabelul n-avea cum
    să-și dea seama. Cine a făcut operațiunea se vede în registrul de predări, pe mișcare.
  - **Transportatorul a ieșit dintre tipurile de partener** (vezi mai jos), iar partenerul a primit
    adresa punctului de lucru.
  Migrarea **`V11`**. Suită verde: **99 de teste**.

- ✅ **ETAPA G4 — ultimele două nomenclatoare din cap. 2 (2026-08-23):** „Transport: Mijlocul”
  (nota 4 — AS/AN/H/CF/A) și „Transport: Destinaţia” (nota 5 — DO/HP/HC/I/Vr/P/Ve/A), verbatim din
  formular, pe mișcare și în ecran, lângă stocare și tratare. Cu ele, **capitolul 2 al Anexei 1 are
  toate coloanele**; până acum jumătate s-ar fi tipărit goale. Migrarea **`V12`**, aditivă.
  ⚠️ **Omonimie de evitat:** `waste_movement_transport_destinations` (`V10`) e caseta „Destinat:” de
  pe **Anexa 3** — ce se face cu transportul acela, cu mai multe bife posibile. `waste_destination`
  (`V12`) e „Destinaţia” din **cap. 2 al Anexei 1** — unde ajunge deșeul, o singură valoare. Două
  rubrici din două formulare diferite, de aceea sunt două coloane și două enum-uri
  (`TransportDestination` vs. `WasteDestination`).

- ✅ **ETAPA G5 — fişa oficială Anexa 1 (2026-08-24):** documentul spre care lucra tot modulul.
  `GET /api/v1/evidences/anexa1?year=&workPointId=` întoarce un PDF cu **o pagină per cod de deşeu**
  (pe punct de lucru, fiindcă evidenţa se ţine pe punct de lucru): antet de identificare, apoi
  **cele patru capitole**, fiecare cu 12 rânduri şi TOTAL AN, plus cele cinci note verbatim.
  Buton **„Fişa Anexa 1”** în ecranul Evidenţe.
  - **Cap. 1 vine din motorul de evidenţă**, nu recalculat: identitatea stocului are o singură
    implementare şi aia rămâne. Cap. 2–4 se citesc din mişcări, fiindcă au nevoie de atribute pe
    care cache-ul lunar nu le poartă (secţia, recipientul, tratarea, operaţia, operatorul).
  - **Cap. 2 numără la „Tratare” doar ce am făcut noi**, pe amplasamentul propriu. O valorificare
    făcută de partener se tratează la el, deci apare în cap. 3 — exact cum arată modelul completat,
    cu 0.000 la tratare şi cantitatea întreagă la valorificare.
  - **O lună cu mai multe predări diferite** listează valorile distincte („R3, R13”; două nume de
    operator). Fişa are exact 12 rânduri; a alege una şi a le pierde pe celelalte ar pune pe un
    formular oficial o cifră pe care n-a înregistrat-o nimeni.
  - ⚠️ **O abatere deliberată de la modele:** ele scriu „conform Anexei 3 / Anexei 2 din Legea
    211/2011”, act **abrogat** de OUG 92/2021. Anexele noului act au aceleaşi numere şi aceleaşi
    liste de operaţiuni, deci referinţa e actualizată, nu reprodusă — un act abrogat tipărit pe un
    formular depus la autoritate e exact genul de detaliu pe care îl vede un control.
    🟠 **De confirmat cu specialista.**
  Suită verde: **103 teste** (99 înainte), din care `Anexa1FormIT` nou.

- 📎 **Exemplele completate au sosit (2026-08-23).** `documente oficiale/` are acum **zece Anexe 1
  cu cifre reale** (Cluj 2022–2024, Timișoara 2022–2024, Bragadiru 2022–2024, Oradea 2022–2024) și
  modelul **Anexa 3 — dovada predării** (formularul de încărcare-descărcare deșeuri nepericuloase,
  HG 1061/2008: expeditor, destinatar, cod deșeu, cantitate în kg, aviz, șofer, nr. auto,
  autorizație de mediu, bifa colectare/stocare/tratare/valorificare/eliminare). Asta **închide
  restanța „șablonul specialistei e gol”** din Etapa 2c: acum există cifre de reprodus, nu doar
  formule. Fiecare fișier poartă și foaia `raportare deseuri generate` — **declarația anuală**: un
  rând per cod, cu `stoc iniţial → generat → valorificat → eliminat → stoc final` plus prin cine.
  Fișierele rămân gitignored (sunt ale clientului), deci testele nu le citesc; ce s-a extras din ele
  a intrat în cod ca regulă comentată, cu numărul de fișiere care o susțin.

- 📎 **Runda veche de întrebări (1–5) a primit răspuns (2026-08-24).** Detalii în
  `docs/raspunsuri-specialist.md`, R19–R26. **Zero cod atins** — toate cinci confirmă ce face deja
  aplicația. Pe scurt:
  - **Întrebarea 3 e închisă:** „la înregistrare, codurile alese de client". Nu există regulă de
    codat la predarea către un colector — alege omul care înregistrează. Cerem codul R/D și nu
    propunem niciun implicit, exact ce face `WasteMovementService` azi. Cele 13 predări vechi fără
    cod rămân `incomplete`, nu se migrează.
  - **Întrebarea 4 e închisă:** „evidența pentru colector" — colectorul ține registrul cronologic
    art. 48 pentru marfa care trece prin el, nu o fișă per cod preluat. Confirmă seam-ul din
    Etapa 2a, iar `GENERATED` rămâne disponibil la toate tipurile de cont, pentru deșeul propriu.
  - **Întrebarea 2 e închisă:** ambalajele se raportează în **kilograme**. Foaia scrie literal
    `[kilograme]`. Fișierul în tone e șablon modificat local.
  - **Întrebarea 1 coboară la 🟡:** SIM se completează din documentele pe care le ținem deja, deci
    nu lipsește niciun câmp din model; rămâne nevăzut doar layoutul.
  - **Obligația AFM** (întrebarea B, fără răspuns din 22.08): **doar producătorii/importatorii de
    ambalaje.** Restrânge Etapa 7 — un generator obișnuit nu primește niciun termen AFM.
  - ⚠️ **Omonimie nouă, a treia:** „Anexa 1" înseamnă **două documente**. Fișa de gestiune
    (HG 856/2002 anexa 1, patru capitole × 12 luni, o pagină per cod — ce tipărește G5) și
    declarația de ambalaje (Ordinul 794/2012 anexa 1, „Producători şi importatori de ambalaje de
    desfacere, **de produse ambalate**, supraambalatori" — adică oricine pune pe piață marfă
    ambalată, nu fabricanții de ambalaje; tabele pe materiale — modulul de ambalaje, nescris).
    Fraza „Anexa 1 e strict pentru
    producători/importatori" e despre a doua. Că nu e despre prima se vede pe foile fișierelor
    primite: `20 01 01`, `20 03 01`, `19 12 12`, `20 01 36` — patru coduri care nu sunt ambalaje,
    fiecare cu fișa lui.

---


---

## G7 — Dosarul de control pe structura Andreei, și „ce tip de generator" (24.08.2026)

Din schițele meeting-ului, pagina 4, propoziție cu propoziție. E singura pagină din cele șapte care
cerea patru lucruri deodată, iar trei dintre ele s-au dovedit una singură.

### 1. „Când dă print la dosar control să respecte structura de 4 tabele pe care o am de la Andreea (la generator) + Anexa 1 trebuie să arate ca tabelul de la Andreea"

Dosarul conține de-acum **`anexa1-<an>.pdf`** — fișa oficială, cele patru capitole, o pagină per cod
de deșeu, exact ce tipărea deja ecranul Evidențe. Arhiva o pune **prima**, iar `README.txt` din ea o
numește ca document reglementat și îi scrie termenul. Restul pachetului (xlsx/pdf de lucru,
autorizațiile partenerilor, atașamentele) rămâne neschimbat, dar nota de subsol s-a corectat: nu mai
scrie că dosarul „NU înlocuiește Anexa 1", fiindcă acum chiar o conține.

„Cele 4 tabele" **sunt capitolele fișei**, nu patru exemplare de Anexa 3 — confuzia din 23.08, care
a costat o felie revertită. Pagina 4 o spune singură: „+ Anexa 1", în aceeași casetă.

### 2. Titlul documentului: „Evidenţa gestiunii deşeurilor generate «an»"

Se tipărește centrat, deasupra antetului de identificare. **Cu anul, nu cu luna** — întrebarea era
deschisă („Luna_An sau pe tot anul?") și au răspuns fișierele: șase din cele primite scriu exact
`Evidenta gestiunii deseurilor generate 2022` / `2023` / `2024` (Cluj și Timișoara), iar șablonul gol
îl lasă `20..`. Sub titlu, foaia are 12 rânduri și un TOTAL AN, iar rubrica din antet e „Anul", nu
„Luna". Verificat pe PDF-ul randat, nu doar în cod: fișa încape în continuare pe o singură pagină.

### 3. „Astea se calculează din ieșiri"

Confirmat, era deja așa: coloanele „valorificată" și „eliminată final" din cap. 1 vin din mișcările
de ieșire, după codul R/D, iar cap. 3 și 4 le repetă cu operația și operatorul. Nimic de schimbat.

### 4. „Anexa 1 termen 15 martie"

Există deja un termen pe 15 martie, generat pentru toate firmele, cu temei legal scris
(OUG 92/2021 art. 48 alin. (1)). Ce s-a schimbat e **cum se citește**: se numea „Raportarea SIM
(anual) — ANPM", adică numea canalul, și clientul rămânea să ghicească ce are de pregătit. Acum se
numește **„Anexa 1 — evidența gestiunii deșeurilor generate (anual, 15 martie)"**, în ecran și în
email. Nu s-a adăugat un al doilea termen pe aceeași zi: e o singură depunere.

### 5. „Ce tip generator (imp/prod/comercial) — comercialul nu are deșeuri proprii"

Întrebare nouă în chestionar și pe firmă: **`MarketRole`** = `PRODUCER` / `IMPORTER` / `TRADER`,
bifabile împreună (o firmă poate fi și producător, și importator). Migrarea `V13`, două tabele de
legătură cu aceeași formă ca restul profilului, ca aprobarea unei cereri să rămână o copiere.

**Cum se cheamă de fapt „comercial".** Legea 249/2015, anexa nr. 1, enumeră trioul verbatim:
„furnizorii de materiale de ambalare, producătorii de ambalaje şi produse ambalate, **importatorii,
comercianţii, distribuitorii**". Deci termenul e **comerciant**. Citatul, cu link și dată, în
`surse-oficiale.md` §11.

**Ce decide bifa — și, mai important, ce nu decide.** Decide declarația de ambalaje (Ordinul
794/2012, anexa 1, termen 25 februarie) și contribuția pe ambalaje la AFM: comerciantul vinde marfă
ambalată de altcineva, deci nu el a pus ambalajul pe piață. **Nu** decide fișa de gestiune din
HG 856/2002 — alt document cu același nume —, pe care o ține oricine generează deșeu, art. 1
alin. (1). Un comerciant cu tomberon de carton în curte o ține ca oricine altcineva. Există un test
care ține regula asta pe loc (`aTraderStillKeepsTheSheet`), scris tocmai ca să nu „ajute" cineva mai
târziu ascunzând fișa.

Unde se vede răspunsul: în formularul public de cerere de cont, în profilul firmei de la **Clienți**,
în lista de cereri pe care o citește suportul, și în `README.txt` din dosarul de control, care scrie
ce decurge din el. Nebifat înseamnă „nu s-a răspuns", nu „niciuna" — nimic nu se restrânge.

**Ce se închide cu asta:** întrebarea **K** (despre care „Anexa 1" vorbea specialista — despre cea
de ambalaje) și partea de model a întrebării **M** (calitatea de producător/importator nu se
întreba nicăieri; acum se întreabă). Rămâne deschisă întrebarea **L**, deci Etapa 7 rămâne blocată:
nu am legat încă bifa de generarea termenului AFM, fiindcă nu știm dacă răspunsul acoperă și
celelalte două contribuții, iar a stinge o alertă pe o presupunere e mai rău decât a lăsa una
zgomotoasă.

### Ce rămâne interpretarea noastră, şi e trimis la validare

Toată felia s-a construit dintr-o pagină de notițe scrise de mână. Trei locuri unde am **ales**, nu
am citit — runda 3 de întrebări, `intrebari-specialist.md`. **Prima a primit răspuns în aceeași
zi și confirmă alegerea noastră; celelalte două rămân deschise:**

| # | Ce am presupus | Ce se strică dacă greșim |
|---|---|---|
| **O** ✅ | „Comercial nu are deșeuri proprii" e despre **ambalaje**; fișa HG 856 rămâne obligatorie și pentru un comerciant | — **confirmat 24.08**: „se referă la deșeuri de ambalaj" (R31). Zero cod schimbat |
| **P** 🟡 | Titlul se pune pe **fiecare fișă**. În fișierele ei stă pe foaia de centralizare, iar foile per cod încep direct cu „Agentul economic:" | abatere de la un model pe care autoritatea l-a acceptat; se mută într-o linie |
| **Q** 🟠 | 15 martie e **o singură** depunere (fișa = ce se încarcă în SIM), deci un singur termen | dacă sunt două obligații distincte, clientul vede un termen în loc de două și nu le poate bifa separat |

Ce **nu** e presupunere, fiindcă e citit din fișierele primite sau din textul actelor: textul exact al
titlului și faptul că poartă anul (șase fișiere), termenul de 15 martie (OUG 92/2021 art. 48(1)), și
numele oficial „comerciant" (Legea 249/2015, anexa nr. 1).

**Migrare:** `V13`. **Suită: 107 teste verzi** (103 înainte).

## G8 — Ieșirea fără cod R/D e roșie, iar „de verificat" dispare (24.08.2026)

Două cereri dintr-o propoziție, amândouă despre ce se vede pe ecran când datele nu sunt complete.

### 1. Ieșirile vechi fără cod R/D — cu roșu

Cele 13 predări dinaintea codului obligatoriu erau marcate **galben**, „Incomplet", alături de
„De cântărit". Dar cele două stări nu sunt la fel de grave: o cantitate care așteaptă cântarul
destinatarului e **normală** (decizia 5 — magazinul n-are cântar, colectorul cântărește la
descărcare), pe când o cantitate ieșită fără cod R/D e **greșită**: a plecat din stoc și nu intră
nici la „Valorificat", nici la „Eliminat", deci Anexa 1 nu se poate depune cu ea așa.

Roșu deci pentru a doua, galben rămâne pentru prima, în toate cele trei locuri unde apărea:

| Ecran | Înainte | Acum |
|---|---|---|
| **Evidențe → Anexa 1 lunar** | badge galben „Incomplet" + coloana `text-amber-700` | badge roșu **„Fără cod R/D"** + coloana `text-red-600` |
| **Evidențe → Predări** | badge galben „Incomplet" în coloana Operațiune | badge roșu „Fără cod R/D" |
| **Mișcări** | textul „Ieșire neclasificată", gri ca oricare altul | badge roșu în locul lui; caseta din formularul de editare, roșie |

**Badge-ul nu mai atârnă de `incomplete`, ci de `totalUnclassifiedOut > 0`.** Câmpul `incomplete` din
răspuns e `unclassifiedOut > 0 || awaitingWeighing` — adevărat și pentru o linie care doar așteaptă
cântarul. Legat de el, badge-ul roșu ar fi mințit pe jumătate din cazuri. Backend-ul rămâne
neschimbat: `incomplete` înseamnă în continuare „linia nu se poate raporta ca atare", ceea ce e
corect pentru amândouă stările.

### 2. `resaleSuspected` — scos, cu tot cu coloană

Semnalul „De verificat" marca liniile unde aceeași pereche punct-de-lucru/cod avea și activitate
art. 48: o predare de deșeu propriu și una care dă mai departe marfă preluată arată identic, deci
linia era semnalată în loc să fie reclasificată.

Răspunsul specialistei (întrebarea 4, 23.08): **„nu e interesată de preluare de la terți (pentru
modulul generat)".** Iar în cod steagul era oricum inert pentru publicul modulului: un generator pur
nu poate înregistra `COLLECTED` — `CompanyType.allowedOperations()` nu i-o oferă —, deci n-avea cum
să aibă linii art. 48 pe care să le compare. Rămânea o promisiune de verificare pe care nimeni n-o
cerea.

**Ce NU s-a atins: separarea celor două registre.** Marfa preluată de la terți rămâne în afara
Anexei 1 (HG 856/2002 art. 2 alin. (1)) — filtrul `register = ANEXA_1` din `EvidenceCalculator` e pe
loc, iar testul care îl ține s-a păstrat, redus la invariantul care contează
(`EvidenceCalculatorIT.takeoverStaysOutOfAnexa1`). S-a pierdut avertismentul, nu regula.

**Migrare:** `V14` (`DROP COLUMN resale_suspected`; următoarea liberă e `V15`).
**Suită: 107 teste verzi**, aceleași — nu s-a adăugat comportament, s-a scos unul.

## Mail real — linkul din email are unde ateriza (24.08.2026)

Ca să se poată genera conturi și testa pe bune. Trei lucruri, dintre care două erau defecte,
nu lipsă.

**1. STARTTLS nu era pornit.** `application.yml` avea `mail.starttls.enabled` — cheia greșită
(JavaMail citește `enable`) și în locul greșit (soră cu `smtp`, nu în el). Rezulta
`mail.starttls.enabled`, o proprietate pe care n-o citește nimeni, deci portul 587 rămânea în clar
și **orice** provider ar fi refuzat autentificarea. Nu s-a văzut niciodată, fiindcă mailul n-a fost
pornit în producție: se citea ca „blocat pe SMTP", nu ca defect. Acum `smtp.starttls.enable: true`
plus `required: true` — dacă serverul nu urcă conexiunea, eșuăm, nu trimitem parola pe socket
deschis. Plus timeout-uri de 10s (connection/read/write): altfel un SMTP care atârnă ține un fir de
request până cedează socket-ul, mult peste cele 30s ale routerului Heroku.

**2. Linkul din mail ducea în gol.** `EmailService` construiește
`FRONTEND_BASE_URL + "/reseteaza-parola?code=..."`, dar ruta **nu exista** în `App.tsx`. Deci chiar
cu SMTP funcțional, invitatul ajungea pe o pagină albă și nu putea intra niciodată. Adăugate:

- **`/reseteaza-parola`** — codul din query, două câmpuri de parolă, `POST /auth/reset-password`.
  E și pagina invitației, nu doar a resetării: `inviteUser` creează contul **dezactivat** cu o parolă
  inutilizabilă, iar `resetPassword` e cel care face `enabled = true`. Fără ecranul ăsta, fluxul de
  invitație n-avea capăt.
- **`/parola-uitata`** — cere un link nou. Nu e doar comoditate: codul trăiește **30 de minute**
  (`CODE_TTL_MINUTES`), deci un client care deschide mailul a doua zi ar fi trebuit reinvitat manual.
  Confirmarea e aceeași și când adresa n-are cont — backend-ul e no-op tăcut ca să nu spună cine e
  înregistrat, iar ecranul n-are voie să spună în locul lui.
- Linkul „Ai uitat parola?" de pe login, care exista ca text în `strings.ts` din prima zi și nu
  ducea nicăieri.

`serve -s dist` are fallback de SPA, deci linkurile adânci din mail se încarcă direct — verificat pe
dyno, nu presupus.

**3. Config de mail în producție — pornit.** Gmail cu App Password, pe `ecoregistru-api`:
`MAIL_HOST=smtp.gmail.com`, `MAIL_PORT=587`, `MAIL_USERNAME` = `MAIL_FROM` = adresa de Gmail.
Cele două trebuie să fie **identice**: Gmail rescrie un From pe care nu-l controlezi, iar
`contact@ecoregistru.ro` n-are încă domeniul în mână. App Password-ul nu expiră — se revocă manual.

**Verificat pe dyno**, nu presupus: `POST /auth/request-reset-password` → `EmailService : Sent
'mail/forgot_password' email to ...` în log, mailul ajuns. Când vine domeniul, mutarea pe Zoho e
doar schimbarea celor trei variabile; default-ul din `application.yml` e deja `smtp.zoho.eu`.

Rămas deschis: **`/verifica-email`** e la fel de fără rută, dar fluxul lui e orfan — se declanșează
doar prin `POST /auth/resend-verification-email`, pe care niciun ecran nu-l apasă, iar conturile
invitate se activează prin resetare. De construit când există un motiv, nu acum.

## Probă de acceptanță cap-coadă, pe producție (24.08.2026)

Prima parcurgere completă a fluxului de generator pe dyno-ul de producție, nu pe date de dev.
Firma de probă: **Ardeal Reciclare SRL** (CUI RO41982307), tip „Generator și colector”, cu punct de
lucru la Florești, o secție, doi parteneri și **șase mișcări** în august 2026. Rezultatul e strâns
într-un document cu capturi — `docs/EcoRegistru-de-la-cerere-la-dosar.pdf`, 30 de pagini, netracked.

⚠️ **Datele astea sunt în baza de producție și rămân acolo.** O sesiune viitoare care se uită la
`companies` va găsi Ardeal Reciclare lângă conturile de demo — e firmă de test, nu client.

### Ce s-a confirmat că merge

| Verificare | Rezultat |
|---|---|
| Formularul public creează o cerere, nu un cont | ✅ |
| Profilul trece din cerere în firmă, punctul de lucru se creează singur | ✅ |
| Invitația pleacă pe email și activează contul | ✅ mail livrat, confirmat în log |
| Rolul comercial acceptă client + furnizor deodată | ✅ |
| Orice ieșire cere cod R/D, fără implicit | ✅ |
| Mișcarea fără cântar rămâne provizorie („De cântărit”) | ✅ volumul înlocuiește cantitatea |
| Anexa 3 se tipărește din mișcare, cu `X` unde s-a bifat | ✅ |
| **Preluarea de la terți NU intră în Anexa 1** | ✅ `15 01 01` arată 400 generat, nu 600 |
| Stocul cumulativ | ✅ 400 − 300 = 100 kg carton; 150 − 150 = 0 menajer |
| Un singur termen pe 15 martie, numit după document | ✅ |
| Dosarul de control conține fișa Anexa 1 | ✅ |

Verificarea din mijloc e cea care contează: cele 200 kg preluate de la un generator terț pe 18 august
n-au urcat stocul din fișă. Art. 2 alin. (1) e respectat pe date reale, nu doar în teste.

### Ce a ieșit prost — de reparat

1. 🟠 **Sesiunea expiră fără niciun mesaj.** În mijlocul probei am fost aruncat la `/login`, cu
   formularul completat pierdut. Nu există notificare de expirare, deci arată ca o deconectare
   inexplicabilă. Pe cererea de cont, care are șase secțiuni, înseamnă muncă refăcută de la zero.
2. 🟡 **Selectorul „Generator intern (Secția)” nu apare la prima deschidere** a formularului de
   mișcare, imediat după ce secția tocmai a fost creată în Setări. Prima mișcare s-a salvat fără
   secție. La a doua deschidere e acolo. E cache-ul listei, nu pierdere de date.
3. 🟡 **Caseta „Destinat:” de pe Anexa 3 se uită ușor.** Două din trei Anexe 3 tipărite au ieșit cu
   toate cele cinci căsuțe goale, deși codul `D5`, respectiv `R13`, era completat alături. Codul pune
   `X` doar unde s-a bifat — nu deduce nimic. Propunerea era prebifarea din familia codului
   (`R` → Valorificării, `D` → Eliminării). ⚠️ **Documentul primit în aceeași zi o contrazice**
   (`anexa 3 hamburger reciclying.pdf`, R32): la o predare către un colector, caseta bifată —
   pretipărită de colector — e **`colectării`**, singură, iar formularul n-are deloc rubrică de cod
   R/D. Caseta spune ce face **destinatarul**, nu ce cod a ales expeditorul. Rămâne blocată pe
   **întrebarea R**, acum reformulată; necunoscuta e tot `R13`.

### Ce nu s-a putut acoperi

Declarația anuală (G6), cele trei cadențe AFM (Etapa 7), modulul de depozit (Etapele 8–11) și
confirmarea de email — `/verifica-email` n-are rută, iar fluxul lui e orfan: niciun ecran nu-l
declanșează, iar conturile invitate se activează prin alegerea parolei.

## G6 — Declaraţia anuală (centralizatorul) (24.08.2026)

Ultima felie neconstruită a modulului de generatori. Foaia `raportare deseuri generate` din
fişierele primite: **un rând per cod de deşeu**, cu stoc iniţial → generat → valorificat → eliminat
→ stoc final, plus „prin cine", şi **o pagină per punct de lucru**. Datele existau deja toate; ce a
adus felia e împachetarea lor în forma pe care o depune clientul.

**Corpusul are două layouturi ale aceleiaşi foi**, şi alegerea dintre ele nu e cosmetică:

| Layout | Unde | Antet |
|---|---|---|
| **Complet** — 11 rânduri de identificare, apoi titlul „Evidenţa gestiunii deşeurilor generate «an»" | Cluj şi Timişoara 2022–2024 (6 fişiere) **şi şablonul gol** | denumire · judeţ+localitate · adresă · tel/fax/email · CUI · autorizaţie de mediu · **cod CAEN** · anul · punct de lucru · u.m. „kg" |
| **Scurt** — trei rânduri şi titlul „CENTRALIZATOR" | Bragadiru 2022–2024 (3 fişiere) | agentul economic · punct de lucru · anul |

**Tipărim layoutul complet**, fiindcă e cel al şablonului gol pe care specialista l-a trimis ca
model de completat, şi singurul care identifică firma destul cât să stea singur odată desprins din
workbook. Ambele au aceleaşi nouă coloane şi acelaşi bloc de semnătură („Intocmit / Functia /
Telefon / Email").

### Ce a cerut o migrare, şi ce nu

Tot antetul exista deja pe `Company` sau pe `WorkPoint`, **în afară de două rubrici**. Migrarea
**`V15`** le adaugă, aditiv şi nullable:

- **`caen_code`** — „COD CAEN 4677". Nu se derivă din nimic: CUI-ul nu-l conţine, iar tipul de cont
  (generator / colector) e clasificarea noastră, nu a INS. Necompletat, rubrica **rămâne goală** —
  nu se pune o cifră ghicită pe un formular depus la APM.
- **`contact_role`** — „Functia:" din blocul de semnătură. `contact_name`, `contact_phone` şi
  `contact_email` existau din `V1`; funcţia lipsea. În corpus e text liber („Manager Mediu",
  „Area Manager"), nu nomenclator.

Ambele se completează în **Clienţi → editează firma**. Formularul public de cerere de cont **nu**
le întreabă — ar fi însemnat lărgirea feliei în încă un ecran; se pot muta acolo oricând.

### Trei abateri de la model, toate deliberate

1. **Data din capul coloanei de stoc.** Modelele scriu „stoc la 01.01.«an»" şi îl copiază de la an
   la an fără să-l actualizeze: fişa Cluj 2024 zice `01.01.2023`, cea Bragadiru 2024 zice
   `01.01.2020`. Noi tipărim anul declarat, fiindcă cifra de dedesubt e chiar stocul lui de
   deschidere. E o scăpare de transcriere în workbook-uri, nu o alegere de model — deci se
   corectează, nu se reproduce.
2. **Fără rând TOTAL.** Îl construisem, şi a fost scos la verificarea pe hârtie: **niciun model din
   corpus nu are aşa ceva**, iar suma ar aduna kilograme de hârtie cu kilograme de menajer — o cifră
   pe care n-o cere nimeni şi n-o poate folosi nimeni.
3. **Marcajul `(*)` stă pe stoc, nu pe cod.** Un rând cu ieşiri fără cod R/D nu se închide aritmetic
   (cantitatea s-a scăzut din stoc dar nu intră în nicio coloană oficială), deci e marcat şi explicat
   sub tabel. Marcajul **nu poate sta lângă codul de deşeu**: în Lista Europeană steluţa de după cod
   e chiar ce face codul periculos, iar „02 02 02 *" s-ar citi ca alt deşeu. Modelele n-au nici
   marcaj, nici notă — cine completează de mână scrie codul odată cu linia şi n-are cum să aibă
   rândul ăsta.

### Ce ţin testele (`AnnualDeclarationIT`, 9 teste)

O pagină per punct de lucru · rândul se închide (`stoc final = stoc iniţial + generat − valorificat
− eliminat`) · **stocul de deschidere e identic cu cel din antetul fişei** — cele două documente se
citesc alături, iar un client care găseşte două stocuri diferite nu mai are încredere în niciunul ·
„valorificat prin" poartă codul **şi** operatorul, amândoi când anul a avut doi („R3 - Colector SRL;
R13 - Reciclator SRL") · preluarea de la terţi rămâne pe dinafară (art. 2 alin. (1)) · ieşirea fără
cod e semnalată, nu absorbită.

Corpusul e gitignored, deci niciun test nu-l citeşte: regula extrasă din el e scrisă ca fixture, cu
numărul de fişiere care o sprijină notat în comentariu.

⚠️ **Pe câte fişiere se sprijină, exact.** Toate afirmaţiile de mai sus („nouă fişiere", „niciun
model n-are rând TOTAL", „două layouturi") sunt verificate pe cele **nouă `.xlsx` + şablonul gol**.
Cele **trei fişiere Oradea (2022–2024) sunt `.xls` vechi şi NU au fost citite**: pe maşina asta nu e
instalat `xlrd`, iar `openpyxl` nu deschide formatul. Deci sunt trei fişiere din corpus pe care
nimeni nu s-a uitat la felia asta. Dacă vreunul are rând de total sau un al treilea layout, concluzia
se schimbă — de-aia întrebarea **T** către specialistă întreabă direct, în loc să se sprijine doar pe
absenţă. Cine reia subiectul: `pip install xlrd` şi o verificare de zece minute închide golul.

**Unde se vede:** **Evidenţe → „Declaraţia anuală"**, şi în arhiva din **Dosar de control**
(`declaratie-anuala-«an».pdf`, imediat după fişa Anexa 1). Verificat pe PDF randat, nu doar pe
aserţiuni — aşa au ieşit la iveală rândul TOTAL şi steluţa.

### Restanţa feliei, închisă în aceeaşi zi: formularul public întreabă cele două rubrici

`V15` le adăugase pe `companies`, unde le citeşte generatorul de PDF — dar acolo le completa doar
administratorul platformei, din ce afla pe telefon. **Clientul, care le ştie, n-avea unde să le
scrie.** Migrarea **`V16`** le mută în locul care le e firesc: cererea de cont. Se cer o dată, la
intrare, iar aprobarea rămâne o **copiere**, nu o traducere — acelaşi tratament ca `marketRoles` în
`V13`. Ambele opţionale: un formular care refuză să plece e un formular pe care nu-l trimite nimeni.

Se văd şi în inbox-ul de cereri din **Clienţi** (CAEN sub tipul de firmă, funcţia lângă numele
persoanei), ca aprobarea să nu fie pe încredere oarbă.

⚠️ **Formularea celor două întrebări e a noastră, nu a specialistei** — de-aia e scrisă ca
întrebare, nu doar ca cod: **întrebarea S** din `intrebari-specialist.md`. Două necunoscute reale:

| Ce nu ştim | De ce contează |
|---|---|
| **Care CAEN** — cel principal al firmei, sau al activităţii de pe amplasamentul care generează deşeul? | Declaraţia se depune **per punct de lucru**. În fişierele primite acelaşi `4677` apare pe toate trei punctele, ceea ce sugerează codul firmei — dar e o singură observaţie, pe o singură firmă. Dacă e per amplasament, câmpul se mută de pe `companies` pe `work_points`. |
| **Cine e „Întocmit"** — cine ţine evidenţa, sau cine semnează ca reprezentant legal? | La un magazin mic e aceeaşi persoană; la o firmă cu departament de mediu, nu. |

Până la răspuns, eticheta spune ce ştim şi nu presupune nimic în plus („Dacă nu eşti sigur care e,
lasă gol"), iar rubrica necompletată se tipăreşte goală. A doua întrebare deschisă de felie e
**T**: lipsa rândului TOTAL din toate cele nouă fişiere e chiar răspunsul, sau vrea vreo autoritate
un total?

**Migrări:** `V15` (declaraţia) + `V16` (cererea de cont). Următoarea liberă e **`V17`**.
**Suită: 117 teste verzi** (107 înainte).

### Livrat în producţie pe 24.08.2026, ora 16:13

`ecoregistru-api` **v21** (`aef7651`) · `ecoregistru-app` **v15** (`e6ecb68`). Cherry-pick curat pe
ambele repo-uri split, fără `--force`; conflictul cunoscut cu `tsconfig.node.tsbuildinfo` n-a apărut
(commit-ul nu atinge fişierul). Flyway a aplicat `V15` şi `V16` pe baza Heroku în **23 ms**, iar
aplicaţia a pornit în 8,4 s, fără erori.

**Probă pe dyno, nu presupunere:** login şi
`GET /api/v1/evidences/declaratie-anuala?year=2026` → `200 application/pdf`, **3 pagini** (cele trei
puncte de lucru ale tenantului demo). Două lucruri de pe hârtia aceea confirmă regulile pe date
reale, nu doar în teste:

- **„Cod CAEN:" e gol.** Firma demo n-a răspuns niciodată la întrebare, deci rubrica rămâne goală
  în loc să fie completată cu ceva plauzibil.
- **Două rânduri poartă `(*)`:** `15 01 07` cu stoc **−450 kg** şi `16 06 01` cu 0. Sunt predările
  vechi, dinainte ca aplicaţia să ceară codul R/D — cantitatea a plecat din stoc şi nu intră în
  nicio coloană oficială. Restanţa de clasificare din Etapa 2, acum **vizibilă pe formular**.

## Restanțele probei de acceptanță, Etapa 6 și igiena (24.08.2026)

Sesiune de reparații, nu de felii noi: cele două restanțe deblocate din proba de acceptanță,
dosarul de control dimensionat la termenul legal, și cele patru restanțe de igienă care se
strânseseră. **130 de teste verzi** (117 înainte). Migrări noi: **`V17`**, **`V18`**; următoarea
liberă e **`V19`**.

Commit-urile, în ordine: `c5d7b9c` (sesiunea), `9762bb0` (dosarul pe 3 ani), `b3e2c65` (igiena),
`de3447f` (documentația), `340629e` (lista de secții).

### 1. Sesiunea nu mai expiră mut — și cauza era în backend, nu în interfață

Restanța (a) părea o problemă de frontend: interceptorul de 401 golea tokenul și făcea
`window.location = "/login"` fără niciun cuvânt. Pusă sub probă, s-a văzut că **interceptorul nici
nu se declanșa**: la un token expirat sau stricat, backendul răspundea **403**, nu 401.

Motivul: `SecurityConfiguration` n-avea `authenticationEntryPoint`, deci Spring Security cădea pe
`Http403ForbiddenEntryPoint`. Pe deasupra, `JwtAuthenticationFilter` chema `extractEmail` fără
`try/catch`, iar JJWT aruncă pe un token expirat — o excepție care iese dintr-un filtru e un 500.

Reparat pe ambele capete:

- **`RestAuthenticationEntryPoint`** răspunde **401** cu plicul obișnuit de erori
  (`error-code: session.expired`). **403 rămâne ce a fost**: `AccessDeniedException`, adică un
  utilizator autentificat care întinde mâna peste rolul lui. Sunt două răspunsuri la două
  întrebări diferite, iar clientul chiar are nevoie să le deosebească.
- **`JwtAuthenticationFilter`** prinde `JwtException` și lasă cererea neautentificată. Log pe
  `debug`, nu `warn`: așa arată un tab lăsat deschis o lună, nu un atac.
- **Trei teste fixau vechiul 403** pentru cereri fără token (`CompaniesControllerIT`,
  `TenantIsolationIT`, `AccountRequestIT`). Toate trei ziceau ce se întâmpla, nu ce trebuia să se
  întâmple — actualizate la 401, cu motivul scris lângă ele.
- **`SessionExpiryIT`** (4 teste) ține de acum contractul: fără token, token stricat, token expirat
  cu semnătură bună → toate 401 cu `session.expired`; token valid → 200.

Pe frontend:

- Interceptorul deosebește acum **cine a pățit-o**: un 401 pe o cerere care **n-avea** token e o
  parolă greșită la login sau un link de resetare expirat — pagina își arată singură eroarea. Doar
  un 401 pe o cerere autentificată închide sesiunea.
- **Paginile publice nu mai sunt evacuate.** `/cerere-cont`, `/parola-uitata`, `/reseteaza-parola`
  și `/login` rămân pe loc: exact accidentul din probă, unde o sesiune expirată într-un tab a luat
  cu ea formularul de șase secțiuni din altul.
- Motivul călătorește ca **parametru în URL** (`/login?expirat=1`), nu în `sessionStorage`. Prima
  variantă folosea un flag „consumat" la prima citire — și n-a mers: sub `StrictMode`, React
  invocă inițializatorul lui `useState` de două ori, deci flagul era consumat înainte de randare.
  Un parametru se citește acolo unde se afișează, dispare la următoarea navigare și nu poartă
  nimic personal.
- **`useFormDraft`** ține formularul de cerere în browser (debounce 400 ms, versionat, expiră în 7
  zile), îl pune la loc **vizibil**, cu buton de aruncat. Nu salvează un formular neatins și nu
  anunță o restaurare goală — prima variantă făcea amândouă, s-a văzut la probă.
  Ciorna se șterge la trimiterea reușită. Nimic nu pleacă din browser până la trimitere.
- Cheile de sesiune stau acum într-un singur loc (`tokenStore` / `tenantStore` / `userStore` +
  `clearSession()`), nu jumătate în `api.ts` și jumătate în `AuthContext`.

### 2. Lista de secții se reîmprospătează la prima deschidere

Restanța (b). Mutațiile invalidau deja cheia, dar `invalidateQueries` reîmprospătează implicit
**doar interogările montate în acel moment** — pe celelalte le marchează învechite. Lista de secții
din formularul de mișcare e tocmai una dintre „celelalte": trăiește pe altă rută. De aici
„apare abia la a doua deschidere".

`refetchType: "all"`, iar rezultatul se așteaptă (`await`), deci dialogul din Setări stă pe ecran
până când datele sunt reale.

### 3. Etapa 6 — dosarul de control dimensionat la 3 ani

**OUG 92/2021, art. 48 alin. (5):** operatorul păstrează evidența **cel puțin 3 ani** (12 luni la
transportatori). Atât poate cere un control, deci atât oferă arhiva.

- `GET /api/v1/audit-file?year=&years=` — `years` implicit **1**, plafonat la **3**. Peste, 400 cu
  `audit.file.years.unsupported`.
- **Un an rămâne exact cum era** (fișiere la rădăcină, `dosar-control-2026.zip`). Mai mulți ani
  intră fiecare în folderul lui (`2024/`, `2025/`, `2026/`), fiindcă numele de fișiere se repetă;
  arhiva se cheamă `dosar-control-2024-2026.zip`.
- **Autorizațiile partenerilor rămân o singură dată, la rădăcină.** Statusul lor („expiră în 30 de
  zile") se citește față de ziua de azi, nu față de un an de raportare — trei copii ar fi aceeași
  pagină cu o dată care nu se potrivește niciuneia.
- **Un an fără linii de evidență e numit ca atare în `README.txt`**, cu ce are omul de făcut
  („deschide Evidențe, alege anul, apasă Regenerează"). Altfel dosarul ar preda o fișă oficială
  goală care arată ca date pierdute.
- `AuditFileIT` are 4 teste noi (9 în total): structura pe foldere, antetul cu termenul de
  păstrare, avertismentul pe anul gol, și refuzul peste 3 ani.
- În interfață: selectorul **Perioada** („Doar anul ales" / „Ultimii 2 ani" / „Ultimii 3 ani (cât
  cere un control)"), cu temeiul legal scris dedesubt.

### 4. Cele patru restanțe de igienă

- **Căutarea de coduri nu mai depinde de diacritice** (`V17`). Cele 842 de denumiri din Lista
  Europeană sunt scrise cu diacritice, iar căutarea compara literal: cine tastează „deseuri" —
  adică oricine, la o tastatură fără layout românesc — nu găsea **nimic**. Acum `waste_codes` are o
  coloană **generată** (`search_text`) cu forma pliată a codului și denumirii, iar
  `Diacritics.fold` pliază la fel textul căutat. Generată, nu întreținută de mână: se recalculează
  singură la orice reîncărcare viitoare a nomenclatorului. Nu `unaccent`, fiindcă acela cere
  `CREATE EXTENSION` și nu e immutable. Ambele jumătăți acoperă și ș/ț cu virgulă, și ş/ţ cu
  sedilă — fișierele oficiale le amestecă. `WasteCodeSearchIT`, 5 teste.
- **`total_collected` a ieșit din schemă** (`V18`). Nemapată de entitate de la `V6`, deci fiecare
  rând a primit 0 din default și nimeni nu l-a citit. Precedentul e `V14`. **`total_handed_over`
  rămâne** — documentația internă le enumera pe amândouă ca „rămase în schemă nescrise", dar aia e
  scrisă de `EvidenceCalculator`, e memo-ul „din care predat" din răspunsul API și are teste care o
  fixează; a fost scoasă din ecran la G3b, nu din model.
- **`frontend/tsconfig.node.tsbuildinfo` nu mai e tracked.** Artefact de build care dădea conflict
  modify/delete la fiecare cherry-pick către repo-ul de frontend, adică la fiecare deploy.
- **Fluxul de confirmare a emailului, scos.** `/verifica-email` n-a avut niciodată rută, iar
  `POST /auth/verify-email` și `/auth/resend-verification-email` nu erau chemate de niciun ecran.
  Venea din șablonul de la care a pornit proiectul, unde omul se înregistra singur. Aici registrul
  e închis: contul se creează dezactivat printr-o invitație, iar alegerea parolei prin
  `/reset-password` e ce îl activează — deci confirmarea n-avea ce confirma. Scoase: cele două
  endpointuri, metodele din `AuthenticationService`/`EmailService`, cele două DTO-uri și șablonul
  `verify_email.html`. `/parola-uitata` merge și pentru un cont dezactivat, deci nimeni n-a pierdut
  o cale de intrare.

### Ce s-a verificat pe viu, nu doar în teste

Backend pornit local pe Postgres real, frontend pe Vite, parcurs în browser:

| Verificare | Rezultat |
|---|---|
| `V17` și `V18` aplicate pe baza de dev | ✅ „now at version v18", 310 ms |
| Căutare „deseuri" fără diacritice | ✅ 50 de rezultate (înainte: zero) |
| „ambalaje de hartie" găsește „ambalaje de hârtie și carton" | ✅ |
| Dosar pe 3 ani: foldere, nume de arhivă, autorizații o dată | ✅ `dosar-control-2024-2026.zip` |
| `years=4` | ✅ 400, cu mesajul care spune de ce |
| Fișa Anexa 1 din dosarul multianual, **randată și privită** | ✅ 7 pagini, patru capitole, antet corect |
| Parolă greșită la login | ✅ „Email sau parolă incorecte", **nu** „sesiunea a expirat" |
| Sesiune expirată în timpul lucrului | ✅ `/login?expirat=1`, cu mesajul galben |
| Sesiune expirată **în timp ce completezi formularul public** | ✅ rămâi pe formular, textul tastat rămâne |
| Ciornă restaurată după reîncărcarea paginii | ✅ cu anunț și buton de ștergere |
| Formular neatins | ✅ nu salvează nimic |
| Secție nou creată, la **prima** deschidere a formularului de mișcare | ✅ apare |

Două lucruri au ieșit prost la probă și au fost reparate în aceeași trecere: ciorna goală
salvată după „Șterge", și butonul de descărcare strâns de al treilea control de pe rând.

### Livrat în producție pe 24.08.2026, ora 17:49

`ecoregistru-api` la **v22** (`62cbbce`), `ecoregistru-app` la **v16** (`0d38a7e`). `V17` și `V18`
aplicate pe baza de producție în **48 ms**, aplicația pornită în **8,9 s**. La cherry-pick-ul către
repo-ul de frontend, commit-ul de igienă a ieșit gol — partea lui de frontend era doar ștergerea
`tsconfig.node.tsbuildinfo`, fișier deja absent acolo. `--skip`, nu `--force`: exact conflictul
cunoscut, de acum stins la sursă, fiindcă fișierul nu mai e tracked în monorepo.

Probe pe dyno, nu presupuneri:

| Verificare | Rezultat |
|---|---|
| `GET /api/v1/work-points` fără token | ✅ **401**, `error-code: session.expired` (înainte: 403) |
| `GET /api/v1/waste-codes?q=deseuri` | ✅ 50 de rezultate |
| `GET /api/v1/audit-file?year=2026&years=3` | ✅ `dosar-control-2024-2026.zip`, folder per an, autorizațiile o dată |
| Fișa Anexa 1 din arhiva de producție, anul 2026 | ✅ 40 KB, cu conținut (2024 și 2025 goale, numite ca atare în README) |
| `years=4` | ✅ 400 |

## Runda de răspunsuri de seară, și unitatea de pe Anexa 3 (24.08.2026)

Cinci răspunsuri primite după deploy, plus o felie mică ieșită din unul dintre ele.
**137 de teste verzi** (130 înainte). Migrare nouă: **`V19`**; următoarea liberă e **`V20`**.

### Ce s-a închis

| Întrebarea | Răspunsul | Cod schimbat |
|---|---|---|
| **1 / H** — chestionarele SIM | se completează cu **datele din anexe**, deci nu lipsește niciun câmp din model | niciunul |
| **2** — unitatea la ambalaje | **kilograme**, ca în act (a treia confirmare, acum pe un fișier completat) | niciunul |
| **5** — o fișă completată | a sosit și declarația de ambalaje completată, care era chiar „anexa" la care se referea | niciunul |
| **menajer D5/D1** | **D5 peste tot**, D1 n-ar mai trebui să existe | niciunul — `D1` nu se propune nicăieri de pe 20.08 |
| **O** — „comercial nu are deșeuri proprii" | despre **ambalaje**; fișa rămâne a tuturor | niciunul |

Documentul de întrebări a fost **restructurat**, fiindcă devenise un labirint: sus, lista completă a
celor șaptesprezece întrebări deschise, grupate pe document (fișa Anexa 1 · declarația anuală ·
Anexa 3 · obligații), fiecare cu ce face aplicația până la răspuns. Dedesubt, un tabel scurt cu ce
s-a închis. Textul lung al fiecărei întrebări a trecut în **arhivă**, nimic șters.

### ⚠️ Corpusul de referință e al unei singure firme

Declarația de ambalaje completată e a **Hamburger Recycling Romania**, adresa de e-mail din antetul
ei e chiar a specialistei, Anexa 3 primită azi e a aceleiași firme, iar punctele de lucru din
corpusul de fișe — Cluj, Timișoara, Bragadiru, Oradea — se potrivesc cu ale ei.

Deci **cele zece fișe sunt zece fișiere ale aceleiași firme, nu zece firme.** Regulile scoase din
ele rămân valabile și verificate, dar comentariile din cod care spun „pe câte fișiere se sprijină"
trebuie citite așa: zece fișiere, o practică. Consecința imediată e la **întrebarea S**: observația
noastră că `4677` apare identic pe toate punctele de lucru **nu** mai sugerează nimic despre
firmă-vs-amplasament — e aceeași firmă. Întrebarea rămâne exact la fel de deschisă.

### Felia: unitatea de pe Anexa 3 se alege pe firmă (`V19`)

Recitind actul pentru răspunsul la întrebarea 2, a ieșit o distincție pe care o amestecasem:

- **raportarea de ambalaje** (Ordinul 794/2012) scrie `[kilograme]`, verbatim, la toate cele cinci
  anexe;
- **formularul de transport** (HG 1061/2008, anexa 3) are la „Cantitate" rubricile **tone** și
  **mc** — verificat pe Portalul Legislativ, nu doar pe modele.

Două din cele trei modele completate îi dau dreptate actului, inclusiv cel ștampilat de la
Hamburger, unde 76 de kilograme se scriu `0,076`. Al treilea (`ANEXA 3 model_CARTON.docx`) scrie KG
și pare adaptat local — exact tiparul fișierului de ambalaje în tone, despre care se lămurise deja
că nu e forma oficială.

**Nu alegem noi în locul clientului.** `Company.anexa3Unit` (`V19`, nullable): necompletat înseamnă
„ca în mișcare", adică fix comportamentul de până acum, deci **niciun cont existent nu se schimbă**.
Firma poate forța kg sau tone din **Clienți → editează firma**, iar cantitatea se convertește la
tipărire prin mutarea virgulei — exact, fără rotunjire. Cifra și unitatea de pe hârtie sunt
întotdeauna de acord; o eroare de 1000× pe un formular care pleacă la transportator e exact ce nu
vrem.

`Anexa3UnitTest` (7 teste) fixează conversia, fiindcă e singurul loc unde o greșeală tăcută ar
înmulți o cifră oficială cu o mie. Întrebarea **A3.4** rămâne la specialistă, dar s-a schimbat: nu
mai e „kg sau tone", ci „contează la control că scrie kg lângă cifră?".


## Corpusul citit rând cu rând — trei întrebări închise fără specialistă (24.08.2026)

> **Unde stau întrebările, de pe 24.08.2026 seara:** în documentul Word
> `docs/EcoRegistru - intrebari specialist 24.08.2026.docx`, ţinut şi pe Desktop. Fişierele
> `docs/intrebari-specialist.md` şi `docs/raspunsuri-specialist.md` au fost **şterse deliberat**
> de utilizator: markdown-ul nu se deschidea uşor la client, iar două surse pentru aceeaşi listă
> se dezacordau. Ce era esenţial din jurnalul de răspunsuri e rezumat mai jos şi în secţiunile
> zilei; restul a fost provenienţă.

Întrebarea utilizatorului a fost bună: dacă citim tot ce e în `documente oficiale/` riguros, nu
găsim singuri o parte din răspunsuri? Ba da. Nouă fişiere, 33 de foi, **336 de luni completate**,
citite rubrică cu rubrică, cu întrebările deschise în mână.

| Întrebarea | Ce spune corpusul | Consecinţa |
|---|---|---|
| **D** — „Stocare: Cant." | = cantitatea **generată** în lună, 336 din 336. Decisiv pe foaia `19 12 12`, unde stocul trece de 50 t iar stocarea rămâne 1.827 kg | ✅ închisă; aplicaţia făcea deja aşa |
| **E** — „Rămasă în stoc" pe TOTAL AN | **stocul din decembrie**: 28 din 33 de foi. Singura foaie unde cele două citiri diferă scrie decembrie. **Niciuna nu lasă celula goală** | ✅ închisă; **noi o lăsam goală** — reparat |
| **C** — agentul economic | mereu **partenerul direct** (Hamburger Hungaria, Retim, SALSERV…) | ✅ jumătate închisă; rămâne doar cazul tratării proprii, care în corpus nu apare |
| **B** — mai multe predări într-o lună | 345 de celule completate, **niciuna** cu două valori | 🟠 rămâne deschisă, dar acum ştim că ce tipărim noi n-are precedent |

### Ce am reparat imediat

Celula „rămasă în stoc" de pe rândul TOTAL AN al capitolului 1 **nu mai iese goală**: tipăreşte
stocul din decembrie, care e stocul anului. Verificat pe hârtie, nu doar în cod — fişa demo arată
acum `-450.000` pe TOTAL AN, exact valoarea din decembrie.

### Trei lucruri pe care nu le căutam

1. ~~**`D5` nu apare nicăieri în corpus.**~~ ✅ **Închis în aceeaşi seară:** „D5 este codul bun
   100%". Ce e în corpus — `D1` la Bragadiru, `D15` la Timişoara — e practică veche, corectată,
   nu o regulă concurentă. Nimic de schimbat în cod: aplicaţia nu propune niciun cod de
   eliminare, iar `D5` e ce folosim în datele de exemplu de pe 20.08. Un client cu fişe vechi pe
   `D1` n-are de corectat retroactiv nimic — sunt documente deja depuse; codul se schimbă de la
   înregistrările noi înainte.
2. **`R13` nu apare nicăieri; corpusul foloseşte `R12`** — şi asta **rămâne deschis**: confirmarea lui `D5` priveşte eliminarea, nu valorificarea.
   intermediar. Noi pusesem R13 în datele de exemplu, marcat explicit ca alegerea noastră —
   alegerea nu se potriveşte cu practica lor.
3. **⚠️ Un comentariu din codul nostru era fals.** `Anexa1SheetBuilder` scria că modelele completate
   arată 0 la „Tratare: Cant." atunci când valorificarea o face partenerul. Nu arată: toate cele 336
   de luni au acolo cantitatea lunii. **Comportamentul nu s-a schimbat** — corpusul e al unei firme
   care chiar sortează şi balotează pe amplasament, deci scrie ce face ea, iar un client care doar
   predă nu tratează nimic. Dar comentariul a fost corectat, fiindcă sprijinea o regulă cu o dovadă
   inexistentă. Devine întrebarea **U**.

*Cele trei fişiere Oradea sunt `.xls` vechi şi tot n-au putut fi deschise (`xlrd` nu e instalat),
deci toate cifrele de mai sus sunt din cele nouă `.xlsx`.*


## Toate răspunsurile, în scris (24.08.2026, seara târziu)

Documentul Word s-a întors completat. Din cele şaptesprezece întrebări, **treisprezece au primit
răspuns**, patru au rămas fără (C, I, Q, şi partea de amplasare a titlului din P). Răspunsurile au
adus şi **nouă cereri noi**, care n-au fost întrebări — notate ca atare, ca să nu pară că le-a cerut
cineva de două ori.

### 1. Confirmă ce face aplicaţia — zero cod de schimbat

| Întrebarea | Răspunsul, pe scurt |
|---|---|
| **A** — antetul cap. 3/4 | „e corect cum ai pus tu cu OUG nou". Deci **OUG 92/2021**, nu actul abrogat. Abaterea de la modelele primite e acum decizie luată, nu presupunere |
| **U** — „Tratare: Cant." la o firmă care doar predă | „ai dreptate" — **0**, nu cantitatea lunii. Comentariul fals din `Anexa1SheetBuilder`, prins la recitirea corpusului, avea totuşi concluzia bună |
| **T** — rând de TOTAL pe centralizator | „nu, e ok cum ai făcut tu". Lipsa lui din toate cele nouă fişiere **era** răspunsul |
| **M** — formularea celor trei bife | „sunt foarte clare" |
| **J** — ce lipseşte din formularul de cerere de cont | „este foarte ok tot ce am pus noi în chestionar" — şi a completat un cont nou prin el, de verificat în producţie |

### 2. Schimbă comportamentul

- **B — o lună cu mai multe predări diferite: „trebuie un rând nou pentru fiecare chestie nouă
  pentru luna respectivă."** Contrazice ce facem: azi înghesuim valorile distincte într-o celulă
  („R3, R12"). Fişa capătă deci **mai multe rânduri pentru aceeaşi lună** la cap. 3 şi 4 — corpusul
  n-avea precedent fiindcă la ei nu s-a nimerit, nu fiindcă aşa se scrie. E singurul răspuns care
  strică o regulă deja tipărită pe hârtie.
- **S — antetul declaraţiei anuale: „cod CAEN completat de utilizator, semnează cine a întocmit."**
  Deci CAEN rămâne **pe firmă**, cerut de la client (nu se mută pe punctul de lucru), iar „Întocmit"
  e **cine ţine evidenţa** — aceeaşi persoană semnează. Se schimbă eticheta, nu câmpurile.
- **A3.1 — „când pleacă la colector se pot bifa valorificării şi colectării, dacă se poate
  valorifica. Iar când pleacă la valorificator, doar valorificării."** Prebifarea redevine posibilă,
  dar **nu după codul R/D** (aia era greşeala pe care documentul Hamburger a arătat-o), ci după
  **ce este destinatarul**. De aceea atârnă de tipul de partener nou (vezi §4).
- **A3.3 — „în 3 exemplare, pentru generator, colector şi transportator."** Cele trei ale art. 20
  alin. (2), cu destinatarul fiecărui exemplar numit pe el. Al patrulea din discuţie nu există.
- **A3.4 — „e bine să poată selecta la introducerea mişcării."** Unitatea nu mai e doar alegere de
  firmă (`V19`): se alege şi **pe mişcare**, cu setarea firmei ca implicit.
- **L — „2% pe orice deşeu, păstrăm alerta. De obicei plăteşte colectorul şi se reflectă în factură.
  Datorează 2%; dacă deşeul nu e din sursă gospodărie proprie, încă 10%."** Contribuţia de 2%
  (OUG 196/2005 art. 9 alin. (1) lit. a) **nu se stinge** — nu ţine de ambalaje, ci de orice vânzare
  de deşeu, şi o reţine colectorul. **Etapa 7 se deblochează**: setul de contribuţii cu trei cadenţe
  rămâne exact cum e documentat, iar alerta lunară a colectorului rămâne pe loc.
  ⚠️ **Cei 10% nu sunt AFM.** Citirea noastră: impozit pe venit reţinut la sursă de la persoana
  fizică ce vinde deşeu care nu provine din gospodăria proprie — Codul fiscal, nu OUG 196/2005.
  **Neverificat pe sursă primară**, deci nu se codează nimic pe el; intră în `surse-oficiale.md`
  abia după ce se citeşte articolul.
- **N — „cartonul din magazine este 15 01 01"**, iar raportarea lui se face în declaraţia de
  ambalaje. Nu propunem coduri în formular, deci nu se schimbă nicio validare — dar modulul de
  ambalaje ştie de-acum ce coduri îl privesc.

### 3. Corectarea de nume, cerută explicit

> „Anexa 1 de la noi ar trebui să se numească **Evidenţa gestiunii deşeurilor generate**, şi
> **Anexa 1 Ambalaje** [să fie] deşeurile de ambalaje puse pe piaţa naţională. [...] Anexa 1
> ambalaje este pentru fondul de mediu, declaraţie AFM. Te rog să corectezi confuzia."

Aplicaţia numeşte azi „Fişa Anexa 1" documentul din HG 856/2002 — corect faţă de act, dar în vorbirea
clientului „Anexa 1" înseamnă **cealaltă** anexă, cea de ambalaje. Numele care circulă câştigă:

| Ce e | Cum se numeşte de-acum în aplicaţie | Act |
|---|---|---|
| Fişa cu patru capitole × 12 luni, o pagină per cod | **Evidenţa gestiunii deşeurilor generate** | HG 856/2002, anexa 1 |
| Tabelele pe materiale, în kg | **Anexa 1 Ambalaje** | Ordinul 794/2012, anexa 1 |

Se schimbă butonul din Evidenţe, numele fişierului, `README.txt`-ul din dosarul de control şi
eticheta termenului de 15 martie. **Distincţia din documentaţie rămâne exact cum era** — sunt tot
două documente diferite; se schimbă doar care dintre ele poartă numele scurt.

⚠️ **Un lucru de reconfirmat, într-o linie.** Specialista spune că Anexa 1 Ambalaje „este pentru
fondul de mediu, declaraţie AFM". Ordinul 794/2012, citit verbatim (`surse-oficiale.md` §5), cere
raportarea **la APM judeţeană / ANPM**, până pe **25 februarie**. Cele două nu se exclud — cifrele
din tabele sunt şi baza contribuţiei pe ambalaje datorate AFM (art. 9 alin. (1) lit. d, anuală, pe
25 ianuarie) —, dar ar fi **două depuneri, la două autorităţi, la două date**. Până la confirmare,
termenele se generează pe ce scrie în acte.

### 4. Cereri noi, ieşite din răspunsuri

Niciuna n-a fost întrebare; toate nouă sunt din marginile documentului.

| # | Cererea, în cuvintele ei | Ce înseamnă în cod |
|---|---|---|
| 1 | „vom face un tab nou numit **Ambalaje** [...] să se completeze automat şi corect cum e acolo, pentru ambele sheeturi" | **Modul nou** — Anexa 1 Ambalaje, cele două tabele din §5 |
| 2 | „un nou tip la parteneri: **Valorificator**" | `PartnerType` capătă un al treilea membru — şi de el atârnă prebifarea de la A3.1 |
| 3 | „să poţi să adaugi mai multe puncte/parteneri" | partenerul are azi **o** adresă de punct de lucru; devin mai multe |
| 4 | „descărcarea din Anexa 3 să se poată face din mişcări pentru punctele de lucru dorite" | filtrare pe punct de lucru la generarea Anexei 3 |
| 5 | „când adaugi partener şi scrii, să apară din db ce clienţi sunt după primele 2 litere" | autocomplete pe parteneri, prag de 2 caractere |
| 6 | „notează în todo lookup după CUI, integrare cu ANAF" | **TODO**, nu felie: completarea firmei din CUI. Utilizatorul are o aplicaţie în `Work/` cu ceva asemănător |
| 7 | „buton **actualizează cantitatea**" după ce vine cântarul de la colector | azi se completează editând mişcarea; se cere o acţiune dedicată, vizibilă pe rândurile „De cântărit". Motivul e explicit: „ne încurcă la rapoarte şi la anexe lipsa cantităţii" |
| 8 | „să fie păstrate **5 ani** documentele din dosar, sunt 3 în lege dar de safety" | `MAX_YEARS` din `AuditFileService`: 3 → 5 |
| 9 | la Anexa 3, „primul element **data încărcării**, după **data descărcării**" | ordinea celor două date în formularul de mişcare |

### 5. Ce arată fişierele de ambalaje, citite rubrică cu rubrică

`RAPORTARE AMBALAJE 2021_anexa 1_ HRR.xlsx` (completat) şi `RAPORTARE AMBALAJE _anexa 1.xlsx`
(gol) au **aceeaşi structură**: două foi, `Tabelul nr. 1` şi `Tabelul nr. 2`, ambele în
**`[kilograme]`** — exact ce spune actul, deci fişierele astea nu sunt şablon modificat local.

- **Antetul** (7 rânduri, pe foaia 1): denumire · judeţ şi localitate · adresă · tel/fax/e-mail ·
  **cod CAEN pentru activitatea aferentă raportării** · CUI · anul. Le avem pe toate.
- **Tabelul 1 — ambalaje introduse pe piaţa naţională.** Rânduri: Sticlă · PET · Alte plastice ·
  Total plastic · Hârtie carton · Aluminiu · Oţel · Total metal · Lemn · Altele · TOTAL. Coloanele
  1–7: ambalaje de desfacere fabricate/importate · total (col. 3+5) · primare (total, din care
  reutilizabil) · secundare şi de transport (total, din care reutilizabil) · cu conţinut periculos.
  ⚠️ **Datele astea nu există în aplicaţie şi nu se pot deduce din mişcări** — sunt despre marfa
  pusă pe piaţă, nu despre deşeu. Se introduc de client. La HRR e completat un singur rând
  (Oţel, 5192 kg), restul foii e gol.
- **Tabelul 2 — deşeuri de ambalaje gestionate.** Material · cantitate · **operatorul** (denumire +
  adresă punct de lucru + CUI) · **operaţiunea** la care l-a supus. Nota 1 cere „câte o rubrică
  distinctă pentru fiecare dintre operatorii care au preluat" — deci **un rând per operator**,
  aceeaşi regulă ca răspunsul **B**. **Asta se completează singură din ce avem deja**: predările pe
  coduri `15 01 xx`, cu partenerul şi codul R.
- Blocul de semnătură: „Numele şi prenumele" + **„Funcţia: DIRECTOR"** — la HRR semnează directorul,
  nu cine ţine evidenţa. Deci răspunsul de la **S** („semnează cine a întocmit") priveşte
  centralizatorul, nu şi documentul ăsta.
- ⚠️ Nota 2 a tabelului 2 trimite tot la **Legea 211/2011**, abrogată — aceeaşi situaţie ca la
  cap. 3/4 ale fişei, unde răspunsul **A** ne-a dat voie să tipărim actul în vigoare.

### 6. Ce a rămas fără răspuns

| # | Ce | Ce facem până atunci |
|---|---|---|
| **C** | cine se scrie la „agentul economic care efectuează operaţia" când firma îşi tratează singură deşeul | scriem „în activitatea proprie" |
| **I** | declaraţia anuală: se depune odată cu fişele sau separat, ce termen, cine dă numărul de înregistrare | o generăm la cerere, fără număr |
| **Q** | 15 martie: o depunere sau două (fişa la APM **şi** chestionarul SIM) | un singur termen, numit după document |
| **P** | *partea de amplasare:* titlul pe fiecare fişă sau doar pe centralizator — răspunsul primit a fost despre **nume**, nu despre unde stă | îl tipărim pe amândouă |


## Ce s-a construit din răspunsuri (25.08.2026)

Cinci grupuri, în ordinea în care contau. **152 de teste verzi** (137 înainte). Migrări noi:
**`V20`**–**`V23`**; următoarea liberă e **`V24`**.

### Grupul 1 — rândul per predare, şi redenumirea

**Răspunsul B, în cod.** Capitolele 3 şi 4 ale fişei au de-acum **un rând per predare distinctă**,
nu valori înghesuite într-o celulă. Concret:

- gruparea se face pe (cod R/D, operator), în ordinea mişcărilor, iar două predări identice rămân
  un singur rând — regula e „un rând per rubrică nouă", nu „un rând per mişcare", altfel un client
  cu patru ridicări pe lună de la acelaşi colector ar depune o fişă pe care n-o citeşte nimeni;
- **„Nr. crt." curge** (1, 2, 3…) în loc să repete numărul lunii, iar luna se scrie pe fiecare rând
  al ei, ca un rând citit singur să nu aterizeze în luna greşită;
- **capitolele 1 şi 2 rămân pe 12 rânduri.** Cap. 1 e registrul de stoc — soldul trebuie citit lună
  de lună —, iar „Stocare: Cant." din cap. 2 e cantitatea lunii, care nu se poate împărţi pe secţii
  după nicio regulă pe care ne-a dat-o cineva. Suma rândurilor unei luni din cap. 3 e egală cu
  cifra ei din cap. 1, şi există test pe asta;
- un grup în care **nicio** mişcare n-a fost cântărită tipăreşte celula de cantitate **goală**, nu
  zero, dar îşi păstrează rândul: predarea a avut loc, doar cifra lipseşte.

⚠️ **Verificat pe hârtie, şi bine că a fost.** Fişa cu rânduri în plus **curgea pe a doua pagină** —
o singură linie orfană din notele de subsol. S-au strâns fontul notelor (5 → 4,4), interliniajul şi
paddingul celulelor; acum o foaie cu trei rânduri în plus la cap. 3 încape lejer, cu spaţiu rămas.
Exact tiparul pe care regula „randează PDF-ul şi uită-te la el" l-a mai prins o dată.

**Redenumirea, cerută explicit.** „Fişa Anexa 1" se numeşte peste tot **„Evidenţa gestiunii
deşeurilor generate"** — butonul din Evidenţe, textele din Mişcări, fişierul descărcat
(`evidenta-gestiunii-deseurilor-2026.pdf`), intrarea şi nota din `README.txt`-ul dosarului, şi
eticheta termenului de 15 martie. Numele scurt **„Anexa 1"** e liber acum pentru ambalaje, iar
acolo unde chiar despre ambalaje e vorba scrie **„Anexa 1 Ambalaje"**. Comentariile care citează
actul („HG 856/2002, anexa 1") au rămas — acolo e numele corect.

### Grupul 2 — mărunţişurile care se văd

| Ce | Unde | Din ce răspuns |
|---|---|---|
| **Butonul „Adaugă cantitatea"** pe rândurile „De cântărit", cu dialog de o cifră | Mişcări | cererea Andreei: „ne încurcă la rapoarte şi la anexe lipsa cantităţii" |
| **Dosarul merge până la 5 ani** (`MAX_YEARS` 3 → 5) | Dosar de control | „sunt 3 în lege dar de safety" |
| **Unitatea Anexei 3 se alege pe mişcare**, nu doar pe firmă (`V20`) | Mişcări | A3.4 |
| **Anexa 3 iese în 3 exemplare numite** — expeditor (generator) · destinatar (colector) · transportator | Anexa 3 | A3.3 |
| **Data încărcării stă prima**, apoi descărcarea | Mişcări, secţiunea Anexa 3 | cererea din 24.08 |
| **Sugestii de partener după două litere** | Parteneri | „să apară din db ce clienţi sunt după primele 2 litere" |

Două lucruri de reţinut despre butonul de cantitate. Are **endpoint propriu**
(`POST /movements/{id}/weight`), nu trece prin editarea mişcării, fiindcă formularul face câmpul
gri cât timp e bifat „se cântăreşte la descărcare" — singura cale de dinainte era să debifezi, adică
să ştergi tocmai informaţia că destinatarul a cântărit. Şi **bifa rămâne pusă** după completare: aşa
*a fost* cântărită marfa. Linia lunară iese din „provizoriu" fiindcă motorul citeşte cantitatea, nu
bifa. Editarea unei cantităţi deja existente e refuzată acolo — aia e o editare şi se face din
formular, unde se vede toată mişcarea.

**Data încărcării** nu e câmp nou: e data mişcării, arătată în secţiunea Anexei 3 ca rubrică gri, cu
o notă că e aceeaşi. Două câmpuri pentru aceeaşi dată ar fi două date care se pot contrazice pe un
formular semnat.

### Grupul 3 — avertismentul înainte de generare (cerut în timpul lucrului)

> „vreau când generezi doc să te atenţioneze că ai mişcări nenotate ca şi cantitate dacă ai bifat
> chestia cu cântărire la descărcare" — şi, imediat după: **„doar unde impactează acea mişcare"**.

Aşa e făcut. Înainte de **Evidenţa gestiunii deşeurilor**, de **Declaraţia anuală** şi de
**descărcarea dosarului**, aplicaţia numără liniile care aşteaptă cântarul **din exact ce intră în
documentul cerut** — anul şi punctul de lucru pentru primele două, anii cuprinşi în arhivă pentru
al treilea. Dacă sunt, arată un dialog care le **listează** (cod, luna, punctul de lucru) şi lasă
alegerea: „Generează oricum" sau „Renunţ, completez întâi". Nu blochează: o ciornă de lucru e utilă
şi incompletă, iar cifra poate să chiar nu existe încă.

O mişcare care aşteaptă cântarul într-un alt punct de lucru **nu** apare pe documentul altcuiva.

### Grupul 4 — tipul „Valorificator" şi prebifarea casetei „Destinat:"

`PartnerType` are un al treilea membru, **`RECOVERER`** („Valorificator"), şi nu e cosmetic: de el
atârnă răspunsul **A3.1**. La alegerea partenerului pe o mişcare de valorificare se prebifează

- **colector** → *Colectării* **+** *Valorificării*
- **valorificator** → doar *Valorificării*

exact cum a spus. **Nu după codul R/D** — aia era greşeala pe care documentul Hamburger a
arătat-o: acolo marfa pleacă sub `15 01 01` la un colector şi caseta pretipărită e *colectării*,
fiindcă rubrica spune ce face **destinatarul**, nu ce cod a ales expeditorul.

Trei garduri în jurul prebifării: se pune numai peste o rubrică **neatinsă**, sub ea scrie că bifele
sunt puse automat, iar din clipa în care omul umblă la ele redevin ale lui. **Eliminarea nu se
prebifează** — n-a fost întrebată.

### Grupul 5 — Etapa 7, cadenţele AFM (`V21`)

Deblocată de răspunsul **L**: „2% pe orice deşeu, păstrăm alerta. De obicei plăteşte colectorul şi
se reflectă în factură." Deci cei 2% **nu ţin de ambalaje** şi nu se sting.

`Company.afmObligation`, un boolean care producea un termen lunar pentru oricine, devine un **set de
contribuţii**, fiecare cu ritmul din OUG 196/2005 art. 11:

| Contribuţie | Cadenţă | Cine |
|---|---|---|
| 2% reţinut la sursă (art. 9 lit. a) | **lunar**, pe 25 | colectorul care cumpără deşeu |
| Economia circulară (lit. c) | **trimestrial**, pe 25 după trimestru | depozitele |
| Ambalaje (lit. d) | **anual**, pe **25 ianuarie** | cine pune produse ambalate pe piaţă |

Cu asta dispar cele **11 alerte greşite pe an** primite de o firmă cu obligaţie doar anuală — cel
mai vechi output greşit din aplicaţie.

**Ce nu se ghiceşte.** Migrarea completează doar ce se poate deriva: un colector cu obligaţia
pornită primeşte 2%, o firmă care pune ambalaje pe piaţă primeşte contribuţia pe ambalaje. Restul
rămân cu setul **gol** — şi tocmai de aceea flagul vechi **nu s-a şters**: cât timp setul e gol,
firma primeşte exact termenul lunar de dinainte. A stinge o alertă pe o presupunere e mai rău decât
a lăsa una gălăgioasă; calea veche se stinge singură, pe măsură ce conturile se completează.

⚠️ **Cei 10% din răspuns** („dacă deşeul nu e din sursă gospodărie proprie, încă 10%") **nu sunt în
cod**. Citirea noastră e că e impozit pe venit reţinut la sursă de la persoana fizică — Codul fiscal,
nu OUG 196/2005 — dar n-a fost verificat pe sursă primară, deci nu s-a codat nimic pe el.

### Grupul 6 — modulul Ambalaje: Anexa 1 Ambalaje (`V22`)

Tab nou, **Ambalaje**, şi documentul pe care îl cere: Ordinul 794/2012, anexa nr. 1, cele două
tabele, în kilograme, după modelul completat de la Hamburger Recycling.

**Cele două tabele au proprietari diferiţi, şi ecranul o spune:**

- **Tabelul 2 — deşeuri de ambalaje gestionate: se completează singur.** Iese din predările deja
  înregistrate pe coduri **`15 01 xx`**, cu partenerul, adresa punctului lui de lucru, CUI-ul şi
  codul R/D. **Un rând per operator**, cum cere nota 1 a formularului — aceeaşi regulă pe care a
  dat-o şi pentru fişă în răspunsul B, scrisă de data asta chiar în act.
- **Tabelul 1 — ambalaje introduse pe piaţa naţională: se scrie de client.** Nu se poate deduce din
  **nimic** din ce ţinem: e despre marfa vândută, nu despre deşeu. Grilă de 8 materiale × 6 cifre,
  salvată rând cu rând la ieşirea din câmp. „Total (col. 3+5)", „Total plastic", „Total metal" şi
  „TOTAL" **nu se stochează** — sunt sume, făcute la tipărire.

**Ce nu ghiceşte, şi o spune pe hârtie.** `15 01 04` e „ambalaje metalice": aluminiul şi oţelul au
acelaşi cod, iar formularul are rând pentru fiecare. Cantitatea intră la **„Altele"**, iar sub tabel
se tipăreşte o linie care **numeşte codurile** ajunse acolo şi de ce. La fel, `15 01 02` intră la
„Alte plastice", fiindcă PET-ul nu se distinge din cod. O rubrică fără răspuns se tipăreşte
**goală**, nu 0.000: pe formularul ăsta „nimic" şi „n-am răspuns" sunt două afirmaţii diferite, şi
amândouă sunt ale clientului.

Distincţia care contează comercial, din răspunsul **N**: cartonul dintr-un magazin e **`15 01 01`**
şi intră aici; acelaşi carton înregistrat pe `20 01 01` intră doar în evidenţa gestiunii. Codul
ales la înregistrare decide, iar aplicaţia nu propune niciunul. Există test pe asta.

**Verificat pe hârtie:** PDF randat şi comparat cu modelul HRR — antetul de 7 rânduri, rândul de
numere 0–7, subtotalurile la locul lor, TOTAL-ul care se închide (5.192 + 3.400 = 8.592), nota
despre codurile nerezolvate, blocul de semnătură. O rubrică de reparat s-a văzut tot acolo:
„Judeţ şi localitate" tipărea a doua oară adresa, fiindcă ţinem o singură adresă liberă. Acum rămâne
goală.

### Grupul 7 — puncte de lucru pe partener (`V23`)

Cererea „să poţi să adaugi mai multe puncte" avea două citiri; utilizatorul a ales-o pe cea care se
potriveşte cu Anexa 3: **un partener are mai multe puncte de lucru**, iar mişcarea spune la care a
ajuns marfa.

- **`partner_work_points`**, cu nume opţional şi adresă. Adresa unică de dinainte
  (`partners.work_point_address`, `V11`) devine primul punct, numit „Punct de lucru"; coloana
  rămâne, necitită şi nescrisă, până o scoate o migrare viitoare — precedentul e `total_collected`.
- **Mişcarea poartă `partner_work_point_id`.** E o proprietate a **transportului**, nu a
  partenerului: acelaşi colector primeşte marfa când într-un depozit, când în altul. Un punct de
  lucru al altui partener e refuzat — o Anexă 3 care numeşte o firmă şi depozitul alteia nu se poate
  urmări înapoi.
- **Ce tipăreşte Anexa 3 la destinatar**, de la specific la general: punctul ales pe mişcare → dacă
  partenerul are exact unul, acela → altfel sediul. Un partener cu trei depozite şi nicio alegere
  primeşte **sediul**, nu un depozit ales de noi: pe o hârtie care pleacă cu camionul, depozitul
  greşit e mai rău decât niciunul. Aceeaşi regulă la coloana „Denumirea, adresă punct de lucru" din
  tabelul 2 al declaraţiei de ambalaje.
- În ecrane: listă cu adăugare/ştergere în formularul de partener, iar la mişcare selectorul apare
  **doar dacă partenerul are mai multe** — cu unul singur n-are ce alege nimeni.
- Salvarea înlocuiește lista cu ce e pe ecran, dar un rând **îşi păstrează id-ul**, deci o mişcare
  care îl arată deja pe o Anexă 3 tipărită continuă să numească acelaşi loc. Lista lipsă din cerere
  (`null`) nu şterge nimic — aceeaşi regulă ca la profilul firmei.

**Suită: 152 de teste verzi.** Migrare: `V23`; următoarea liberă e **`V24`**.

## Fişele scoase din contul specialistei, reparate (25.08.2026)

Specialista şi-a făcut singură cont prin formularul public, a înregistrat două mişcări şi
şi-a tipărit documentele. Ce a ieşit nu se putea depune nicăieri.
Patru observaţii ale ei, şi toate patru veneau **din aceeaşi cauză**.

### Ce era în contul ei, exact

Citit în baza de producţie (read-only), nu presupus:

| Data | Cod | Cantitate | Operaţie | Partener |
|---|---|---|---|---|
| 24.08.2026 | `15 01 01` | 100 kg | R3 | un colector |
| 24.08.2026 | `15 01 02` | 50 kg | R12 | alt colector |

Zero mişcări de generare. Zero secţii definite. Şi linia de evidenţă rezultată:

```
Generate 0.000 | valorificată 100.000 | eliminată 0.000 | rămasă în stoc −100.000
```

### 1. „Cum poţi să valorifici ceva ce nu este generat?" (`V24`)

Are dreptate, şi o spune chiar antetul formularului: cap. 1 e „Cantitatea de deşeuri **Generate**",
iar sub el **„din care:** valorificată | eliminată final | rămasă în stoc". Coloanele 2–4 sunt părţi
din coloana 1 — nu pot fi mai mari decât ea, iar un stoc negativ nu există pe hârtie.

Motorul deduce de-acum generarea din ieşiri, lună cu lună:

```
ieşiri    = valorificat + eliminat + ieşiri fără cod R/D
acoperire = stoc la începutul lunii + generat înregistrat
dedus     = max(0, ieşiri − acoperire)
```

„Generate" tipărit = înregistrat + dedus. **Când clientul chiar înregistrează generarea, sau când
stocul reportat acoperă ieşirea, dedusul e zero şi nu se schimbă nimic** — există test pe asta, ca
să nu se numere o cantitate de două ori.

Nu e o cifră inventată: e cea pe care omul a scris-o la ieşire, recunoscută în coloana din care nu
avea cum să nu vină. Partea dedusă se ţine separat (`implied_generated`), ca să se poată spune
oricând cât din „Generate" a scris omul şi cât a rezultat din predări. Cache-ul se goleşte, ca la
`V6`: `total_generated` şi-a schimbat înţelesul.

**Fişa ei, după:** August — Generate 100.000, valorificată 100.000, **rămasă în stoc 0.000**;
TOTAL AN la fel. Declaraţia anuală: Generat 100 / Valorificat 100 / **Stoc 0.000**, cu operatorul şi codul R
la „Valorificat prin". Verificate pe PDF randat, pe datele ei reconstruite.

### 2. Cele două coloane „Cant." din cap. 2 — şi o corectură a noastră în aceeaşi zi

Cerinţa a venit aşa: „stocarea trebuie să apară şi la cantitatea 1 şi la cantitatea 2". Am citit-o
ca „aceeaşi cifră în ambele coloane" şi am pus cantitatea lunii şi la „Tratare: Cant.". Câteva
minute mai târziu, uitându-se la rezultat: **„de ce ai la tratare cantitatea 2 ceva?"** — deci nu
aia era.

Aşa că regula rămâne cea din **răspunsul U** (24.08), iar cele două coloane răspund la două
întrebări diferite:

- **„Stocare: Cant." = ce a produs luna.** Ieşea 0 în contul ei, dar din cauza generării, nu a
  coloanei: odată ce generarea se deduce din ieşiri, coloana poartă cifra reală. Asta era, de fapt,
  toată cererea.
- **„Tratare: Cant." = doar ce a tratat firma cu mijloace proprii.** O valorificare făcută de
  partener se tratează la el şi apare în cap. 3, deci un client care doar predă cartonul are aici
  **0**. A tipări cantitatea ar declara o operaţiune care n-a avut loc.

Lecţia, notată fiindcă a costat un drum dus-întors: o cerinţă formulată pe rezultat („nu apare
nimic") nu spune de la sine **care** e cauza. Ce lipsea era generarea; coloana a doua era corectă.

### 3. Secţia: „Birouri + producţie", predefinit

Două lucruri, fiindcă erau două cauze:

- **Fiecare punct de lucru nou porneşte cu două secţii**, „Birouri" şi „Producţie" — verbatim ce
  scrie în coloana „Secţia" din fişele ei („birouri", „productie"). Firma ei avea **zero** secţii,
  deci nimeni n-avea ce alege.
- **Cap. 2 nu mai lasă coloana goală**: când mişcările lunii nu numesc nicio secţie, se tipăresc
  secţiile punctului de lucru („Birouri, Producţie"). E provenienţa deşeului de pe amplasament, nu
  o presupunere despre un transport anume. Un punct de lucru fără secţii definite tipăreşte în
  continuare gol — nu se inventează din nimic.

### 4. Dosarul de control „cu documente aiurea, fără date"

Două cauze, amândouă închise. Prima e chiar (1): fişa din arhivă avea aceleaşi zerouri. A doua e a
noastră şi era mai urâtă: **dosarul citea cache-ul lunar fără să-l reconstruiască**, deci un client
care n-a apăsat niciodată „Regenerează" primea un pachet de formulare oficiale goale — care arată
exact ca date pierdute. De-acum arhiva **regenerează evidenţa fiecărui an pe care îl acoperă**
înainte să împacheteze. Idempotent: fără nimic nou de adunat, ies aceleaşi linii.

Verificat pe arhiva ei reconstruită: `evidenta-gestiunii-deseurilor-2026.pdf` conţine 100.000 şi
nu mai conţine −100.

### Şi o plasă sub toate: cache-ul gol se reface singur

Liniile lunare sunt date derivate, deci un cache gol nu e o veste pe care s-o poarte clientul:
înseamnă că nimeni n-a apăsat încă „Regenerează", sau că o migrare l-a golit fiindcă s-a schimbat
înţelesul unei coloane — exact ce face `V24`. Până acum, ecranul ieşea gol şi fişa după el, ceea ce
arată leit a evidenţă pierdută. De-acum, un an care **are mişcări dar n-are linii** se
reconstruieşte la prima citire. Restul citirilor rămân citiri.

**Migrare:** `V24`. **Suită: 154 de teste verzi.**

### Ce s-a mai văzut în contul ei, şi nu e defect

- **Tabul Ambalaje îi merge din prima**: ambele coduri sunt `15 01 xx`, deci tabelul 2 al Anexei 1
  Ambalaje se completează singur, cu operatorii şi codurile R.
- **Autorizaţia de mediu lipseşte** de pe firmă, deci rubrica ei iese goală pe declaraţia anuală.
  Nu e bug — n-a completat-o nimeni.


## Ambalajele se declară din mişcări (25.08.2026)

**Reclamaţia, în cuvintele utilizatorului:** *„cred că Andreea voia să se vadă automat în ambalaje
mişcările care sunt pentru asta, nu să fie XLS-ul cu 2 sheeturi de completat în UI. XLS-ul să se
genereze în funcţie de ce mişcări sunt în ambalaje, tabul ambalaje să centralizeze cumva tot ce ţine
de ambalaje, mişcări. Să poţi genera Anexa 1 Ambalaje."*

Şi avea dreptate pe fond. Tabul livrat pe 25.08 dimineaţa (`V22`, grupul 6 de mai sus) **reproducea
fişierul**: o grilă de şaizeci şi şase de celule pentru tabelul 1, tabelul 2 calculat — iar
mişcările pe `15 01 xx`, cele din care iese tot, **nu se vedeau nicăieri** în ecran.

### Ce era greşit în premisa lui `V22`

`V22` pornea de la o propoziţie care părea de nezdruncinat: *tabelul 1 e despre marfa pusă pe piaţă,
nu despre deşeu, deci nu se poate deduce din nimic din ce ţine aplicaţia.* Legal, propoziţia e
adevărată. Ca flux, e falsă — şi utilizatorul a arătat de ce, într-o frază:

> „Omul, când reciclează acea cantitate de ambalaj pusă pe piaţă, o să adauge mişcare, ca să o poată
> scoate şi să apară în gestiune şi în rapoarte."

Kilogramele **trec oricum printr-o mişcare**: aşa ies din stoc şi aşa ajung în evidenţă. Ce lipsea
nu era cifra, ci **felul ambalajului** — singura rubrică a tabelului 1 pe care mişcarea n-o purta.
Deci nu un registru paralel, ci trei rubrici în plus pe mişcare.

### Ce s-a construit (`V26`)

| # | Ce | Unde |
|---|---|---|
| 1 | **Mişcarea poartă ambalajul**: materialul, felul (desfacere / primar / secundar-transport), bifa „reutilizabil", bifa „conţinut periculos" | `V26`, `PackagingCategory`, blocul din formularul de mişcare — se arată **doar** pe coduri `15 01 xx` |
| 2 | **Tabelul 1 se însumează din mişcări**: materialul dă rândul, felul dă coloana | `PackagingDeclarationBuilder.marketRows` |
| 3 | **Tabul e registrul**: mişcările de ambalaje ale anului, cu ce le lipseşte scris pe fiecare rând, plus o bandă de semnale deasupra | `PackagingPage`, `GET /api/v1/packaging/movements` |
| 4 | **Descărcarea e `.xls`, cu două foi** — `Tabelul nr. 1` şi `Tabelul nr. 2`, la aceleaşi adrese de celulă ca modelul | `PackagingDeclarationXlsxGenerator` |
| 5 | Grila rămâne, pliată, ca **suprascriere pe material** | `PUT /api/v1/packaging/market` |

**Fiecare kilogram se numără o dată.** O firmă care înregistrează şi generarea, şi predarea aceleiaşi
încărcături are două mişcări pentru o singură cantitate. Per cod de deşeu şi an: dacă există
generări înregistrate, ele contează; dacă nu există, ieşirile ţin locul lor. E **exact** substituţia
pe care motorul de evidenţă o face pentru generarea dedusă (`V24`), şi din acelaşi motiv — ieşirea e
dovada că deşeul a existat. Are test (`oneLoadRecordedTwiceIsDeclaredOnce`).

### „Altele" nu mai e găleată

Indicaţia Andreei, relatată de utilizator: rândurile de material sunt **PET + Alte plastice = Total
plastic**, **Aluminiu + Oţel = Total metal**, iar Sticla, Hârtia carton şi Lemnul stau singure —
*„Altele nu cred că trebuie să fie, a zis Andreea"*.

Sumele erau deja aşa. Ce s-a schimbat e **fallback-ul**: până acum, orice cod `15 01` pe care Lista
Europeană nu-l aşeza cădea în „Altele", iar formularul tipărea sub tabel ce coduri au ajuns acolo.
De-acum, cantitatea aia **nu intră în tabel** şi se raportează ca neîncadrată — în ecran, ca semnal
portocaliu pe rândul mişcării, şi pe hârtie, ca linie sub tabelul 1. Rândul „Altele" rămâne pe
formular, fiindcă actul îl are, dar se foloseşte numai dacă îl alege cineva deliberat.

**Asta închide întrebarea Z.** `15 01 04` („ambalaje metalice" — şi aluminiu, şi oţel) şi `15 01 02`
(„ambalaje de materiale plastice" — şi PET, şi navete) se rezolvă acolo unde se ştie răspunsul: la
înregistrarea mişcării. Codul **propune** materialul unde îl decide singur (`15 01 01` → Hârtie
carton, `15 01 02` → Alte plastice, `15 01 03` → Lemn, `15 01 07` → Sticlă) şi tace unde nu.

### Ce a adus textul ordinului

Utilizatorul a trimis [envirocons.ro/ordinul-794-din-2012-varianta-actualizata](https://envirocons.ro/ordinul-794-din-2012-varianta-actualizata/).
Trei lucruri noi, verbatim în `surse-oficiale.md` §5.1:

1. **Art. 6: „Datele de raportare se transmit în format electronic «.xls»".** Formatul e **cerut de
   act**, nu ales de noi — ceea ce ridică exportul XLSX de la comoditate la cerinţă, şi confirmă
   exact ce ceruse utilizatorul. PDF-ul rămâne pentru dosarul de control.
2. **Întrebarea Y se lămureşte pe jumătate: sunt două depuneri, nu una.** Anexa 1 Ambalaje merge la
   **agenţia judeţeană/regională de mediu** pe **25 februarie** (art. 1 + art. 6); notificarea
   „îmi îndeplinesc individual obiectivele" merge la **AFM** pe **25 ianuarie** (art. 3). Deci şi
   specialista, şi actul spun adevărul — despre documente diferite. Cadenţa AFM din `V21` e a
   notificării de la art. 3, nu a declaraţiei.
3. **Art. 8 alin. (1): „se raportează în kilograme"** — a treia confirmare, prima dintr-un articol
   şi nu dintr-un antet de tabel.

### Verificat pe hârtie, nu doar în teste

Randat şi privit, ca la fiecare formular oficial. XLSX-ul cade **celulă cu celulă** peste model:
titlul în `B2`, antetul în `B4:B10`, `[kilograme]` în `I15`, banda numerotată `B19:I19`, materialele
`B20:B30`, notele `B32:B35`; foaia a doua cu `B5/C5/F5`, `C6/D6`, `D7/E7` şi blocul de semnătură.
PDF-ul tipăreşte aceleaşi cifre pe o pagină. Ce s-a prins uitându-mă: scria „**1 mişcări**" pe linia
de avertizare — niciun test nu s-ar fi supărat.

**Migrare:** `V26`. **Ce nu s-a atins:** `packaging_market_entries` rămâne pe loc, cu comentariul
schimbat; rândurile scrise până acum devin suprascrieri, deci nicio cifră existentă nu dispare.

### Commis şi pushat, tot (25.08.2026, ora 12:19)

Toate cele trei repo-uri, plus ambele dyno-uri:

| Unde | Ce | Stare |
|---|---|---|
| `origin/main` şi `origin/deploy/heroku-split` | `0d1c1c7` | ✅ împinse, cele două branch-uri sincronizate |
| `newrepo/main` (`ecoregistru-backend`) | `58e1025` | ✅ `ecoregistru-api` la **v26** |
| `ferepo/main` (`ecoregistru-frontend`) | `81ea3e5` | ✅ `ecoregistru-app` la **v19** |

`V26` aplicată pe producţie în **19 ms**, aplicaţia pornită în **9,75 s**. Niciun conflict la
cherry-pick, niciun commit gol — commit-ul atinge şi backendul, şi frontendul.

⚠️ **`docs/plan-executie.md` şi `docs/prompt-continuare.md` sunt gitignored**, deci modificările din
ele (rândul R6, întrebările Y şi Z, deciziile 21 şi 22, numărul de teste şi migrarea liberă) există
**doar local**. Nu sunt în niciunul din cele trei repo-uri — aşa e regula, fiindcă monorepo-ul e
public.


## Provenienţa deşeului la ieşire — un defect găsit dintr-o întrebare (25.08.2026)

**Întrebarea utilizatorului**, imediat după ce s-a livrat modulul de ambalaje:

> „Şi dacă de exemplu eu sunt reciclator, adică Hamburger Recycling, şi iau de la un generator deşeu
> de 15 01 01 şi îl valorific şi adaug în mişcare, nu va fi considerată în Anexa 1 Ambalaje, ceea ce
> este greşit? [...] că teoretic nu e generat de mine."

Al doilea gând e criteriul legal exact — HG 856/2002 art. 2 alin. (1): un operator autorizat ţine
Anexa 1 *„numai pentru deşeurile generate în cadrul activităţilor proprii"*. Deci **nu**, marfa
preluată n-are ce căuta acolo. Dar verificând, s-a văzut că regula era aplicată **pe jumătate**.

### Ce era rupt

`resolveRegister` fixa doar cele două capete: `GENERATED` → Anexa 1, `COLLECTED` → art. 48. **Ieşirea
nu era întrebată nimic** şi cădea pe implicitul `ANEXA_1` — iar formularul de mişcare nu trimitea
niciodată câmpul `register`. Deci un reciclator care valorifica marfa preluată o declara ca a lui.

Probat, nu dedus. Scenariul din întrebare, rulat cap-coadă:

```
preiau 1000 kg 15 01 01 de la un magazin   (COLLECTED → art. 48, corect afară)
le valorific R3 la mine                    (RECOVERED → implicit Anexa 1)

→ tabel 1, Hârtie carton, secundar = 1000.000   ← ambalajul altuia, declarat pus pe piaţă de mine
→ tabel 2, rânduri = 0                          ← corect, n-am predat nimănui
```

Şi nu se oprea la ambalaje: aceeaşi mişcare intra şi în *Evidenţa gestiunii deşeurilor generate*,
unde generarea dedusă din `V24` o raporta drept **generată de firmă** — exact ce nu generase.
Defectul e mai vechi decât modulul de ambalaje (vine din implicitul modulului de generatori), dar
`V24` şi tabelul 1 l-au făcut vizibil în cifre.

### Reparaţia: se întreabă, nu se presupune

Alegerea utilizatorului dintre cele două variante. Aceeaşi valorificare, cu acelaşi cod R, poate fi
a deşeului propriu sau a mărfii preluate — **operaţiunea nu spune care**, deci nu se deduce.

- **Backend:** o ieşire (`RECOVERED`/`DISPOSED`) de pe un cont care ţine registrul art. 48
  (`COLLECTOR` sau `BOTH`) **cere** registrul explicit; fără el, 400 cu `movement.register.required`.
  Un generator pur nu vede nimic — n-are ce prelua, deci întrebarea n-ar avea sens.
- **Formular:** blocul „Proveniența deșeului", radio fără preselecţie, **cu efectul scris sub
  fiecare opţiune** — fiindcă alegerea nu schimbă un câmp, ci pe ce formular oficial ajunge cifra:

  | Alegerea | Ce scrie în ecran că se întâmplă |
  |---|---|
  | **Generat în activitatea proprie** | „Intră în Evidenţa gestiunii deşeurilor generate şi, dacă e cod 15 01 xx, în Anexa 1 Ambalaje — tabelul 1 ca ambalaj pus de tine pe piaţă, tabelul 2 dacă l-ai predat cuiva." |
  | **Preluat de la terţi** | „Intră în registrul cronologic art. 48 şi în raportarea colectorilor (Anexa 3 la Ordinul 794/2012, încă neconstruită). NU intră în Anexa 1 şi nici în evidenţa gestiunii — nu e deşeul tău." |

  La `COLLECTED` nu se întreabă nimic, dar se scrie de ce: „intră automat în registrul cronologic
  art. 48, niciodată în Anexa 1".
- **Tabul Ambalaje:** mişcările pe marfă preluată **rămân în registru** — sunt ambalaj, şi omul le
  caută acolo — dar gri, cu eticheta „Preluat de la terţi" şi explicaţia că nu hrănesc niciun tabel.
  Semnalele („de cântărit", „fără cod R/D") le sar: o reparaţie pe ele n-ar schimba nicio cifră.

**Teste noi:** `ThirdPartyPackagingIT`, patru — ieşirea fără provenienţă e refuzată; marfa preluată
stă în afara ambelor tabele dar se vede în registru; deşeul propriu al **aceleiaşi firme** intră
normal (regula separă registrele, nu firmele); preluarea nu poate cere Anexa 1.

**Cincisprezece teste vechi au picat, şi asta a fost informativ.** Tenantul demo e `CompanyType.BOTH`,
deci regula i se aplică — iar testele alea creau ieşiri fără să spună de unde vine deşeul. Toate
descriau deşeu propriu, deci au primit `"register": "ANEXA_1"` explicit; sunt mai oneste aşa. **166
de teste verzi.**

### Anexa 3 la Ordinul 794/2012 — raportul care lipseşte

Ce **nu** s-a construit, şi cine îl datorează. Ordinul are cinci anexe, iar reciclatorul din
întrebare nu depune anexa 1, ci **anexa 3**:

> **Art. 4** — operatorii economici autorizaţi pentru **colectarea, reciclarea şi valorificarea**
> deşeurilor de ambalaje, şi **comercianţii** de deşeuri de ambalaje, raportează agenţiei
> judeţene/regionale de mediu **din raza de activitate**; comercianţii raportează la ANPM.
> Raportarea se face **per punct de lucru**.

Modelul e deja la noi: `documente oficiale/RAPORTARE DESEURI DE AMBALAJ COLECTATE ANUAL.ods`. Citit
pe 25.08, o singură foaie, `RAPORTARE_AMBALAJE`:

- **Antetul** cere ceva ce Anexa 1 nu cerea: **autorizaţia de mediu** (nr. înregistrare / dată /
  valabilitate) şi **punctul de lucru**. Le avem pe amândouă pe firmă.
- **Coloanele:** Material · *Cantitatea colectată* (Total | **din care periculoase**) ·
  **Provenienţa** · *Deşeuri comercializate / trimise la reciclare / valorificare / exportate*
  (cantitate | operatorul economic).
- **Rândurile de material sunt altele decât la anexa 1**: hârtie-carton · PET · alte plastice ·
  *total plastic* · lemn · **metal/aluminiu** (un singur rând, nu aluminiu şi oţel separat) ·
  *total metal* · *TOTAL ambalaje*.
- ⚠️ **„Provenienţa" e o dimensiune pe care mişcarea n-o are**: fiecare material se desface pe trei
  rânduri — **populaţie · colectori · generatori persoane juridice**. Fără ea, raportul nu se poate
  completa deloc.
- ⚠️ Fişierul e în **tone**; actul zice **kilograme** la art. 8 alin. (1). Acelaşi tipar ca la
  anexa 1: şablon modificat local.

Deci felia are o migrare (provenienţa pe mişcare), un ecran şi un generator — nu e o variantă a
celei de azi.

### Întrebarea AA, răspunsă pe 25.08 — şi ce a mai ieşit din actul citit integral

Utilizatorul a cerut răspunsuri, iar textul oficial al ordinului (citit integral, `surse-oficiale.md`
§5.2) le-a confirmat pe toate.

| # | Întrebarea | Răspunsul | Sursa |
|---|---|---|---|
| **1** | Provenienţa: per mişcare sau per partener? | **Pe partener, cu suprascriere pe mişcare** — vezi mai jos. 🟡 rămâne de confirmat cu Andreea | decizia noastră, sprijinită de nota 2 |
| **2** | kg sau tone? | **Kilograme** | utilizatorul + art. 8 alin. (1) lit. a) |
| **3** | „metal/aluminiu" un singur rând? | **Nu** — Aluminiu şi Oţel separat, plus *Total metal*, exact ca la anexa 1. Fişierul `.ods` e şablon modificat local | utilizatorul („aşa arată corect, ca în raportare ambalaje 2021 anexa 1 HRR") + textul anexei 3 |
| **4** | Cele două depuneri sunt separate? | **Da, şi sunt de fapt trei lucruri distincte** | căutat şi verificat pe act |

**Detaliul lui 4**, fiindcă e uşor de confundat:

1. **Anexa 1** → agenţia judeţeană de mediu, din raza **sediului social**, **25 februarie**.
2. **Notificarea de la art. 3** („îmi îndeplinesc individual obiectivele") → **AFM**, **25 ianuarie**.
3. **Contribuţia de 2 lei/kg** → AFM, declarată şi plătită **anual, 25 ianuarie**, şi **numai dacă
   nu ţi-ai atins obiectivele** de valorificare (OUG 196/2005 art. 9 alin. (1) lit. d) + art. 11
   alin. (2)).

Un rezumat de pe internet dădea contribuţia pe ambalaje drept **lunară**. E greşit — art. 11
alin. (2) o pune explicit în grupa anuală, alături de lit. i), j), p), v), w), x). Verificat pe
Portalul Legislativ înainte de a schimba ceva; `V21` rămâne corectă, n-am atins nimic.

**Răspunsul la 1, şi de ce.** Nota 2 a formularului spune „în funcţie de persoanele juridice sau
fizice **de la care provin** deşeurile" — deci provenienţa descrie **sursa**, nu transportul şi nu
încărcătura. Iar sursa e, în aplicaţie, partenerul. Deci: câmp pe **partener**, moştenit automat de
fiecare recepţie de la el, cu posibilitatea de a-l suprascrie pe o mişcare anume. Aşa se răspunde o
dată pentru un furnizor care revine lunar, dar rămâne loc pentru cazul mixt.
Excepţia care cere şi câmpul pe mişcare: **„populaţie"** n-are partener — un centru care primeşte
direct de la oameni n-are pe cine să eticheteze.
⚠️ **Valorile sunt patru, nu trei** — `.ods`-ul primit omitea „comerciant": populaţie · generator
persoană juridică · colector · comerciant.
🟡 **Rămâne de confirmat cu Andreea** cum ţine ea evidenţa în practică — pe furnizor sau pe recepţie.

### 🟡 Ce a deschis actul, şi nu era întrebat

**Anexa 1 nu e a tuturor.** Art. 1 alin. (1) o cere celor care îşi îndeplinesc obiectivele **în mod
individual**; cine şi-a transferat obligaţiile către un OIREP nu o depune (OIREP-ul raportează prin
anexele 2A/2B), iar cine a transferat **parţial** raportează doar cantităţile netransferate. Noi nu
întrebăm pe nimeni dacă a transferat obligaţiile, deci tabul se oferă tuturor. **Nu s-a schimbat
nimic** — a restrânge pe un răspuns pe care nu-l avem ar ascunde ecranul unor firme care chiar
depun. Devine **întrebarea AB**: se întreabă în chestionarul de cerere de cont dacă firma îşi
îndeplineşte obiectivele individual sau a transferat către un OIREP?

### TODO — Anexa 3 Ambalaje (Ordinul 794/2012, anexa nr. 3)

**Neînceput.** Structura reală, din act, nu din `.ods`-ul primit:

- **Două tabele, şi se completează unul singur**, „după caz" (art. 4 alin. (1)):
  **tabelul 1** pentru colectori şi comercianţi, **tabelul 2** pentru reciclatori şi valorificatori.
  Hamburger Recycling, exemplul din întrebarea utilizatorului, completează **tabelul 2**.
- **Antetul** cere în plus faţă de anexa 1: **autorizaţia de mediu** (nr./dată/valabilitate) şi
  punctul de lucru. Le avem pe firmă.
- **Coloanele tabelului 2:** Material · cantitatea preluată (Total | din care periculoase) ·
  **Provenienţa** · cantitatea **reciclată** · cantitatea **valorificată** (numai prin alte metode
  decât reciclarea) · **metoda**.
- **Rândurile de material sunt cele de la anexa 1** — Aluminiu şi Oţel separat.
- **Se depune per punct de lucru** (art. 4 alin. (4)), la agenţia din raza punctului de lucru; un
  comerciant raportează în schimb la ANPM.
- **Ce lipseşte din model:** provenienţa (migrare nouă, pe partener + pe mişcare) şi separarea
  „reciclat" / „valorificat prin alte metode", care s-ar putea deduce din codul R (R3 reciclare vs.
  R1 valorificare energetică) — de confirmat.

Felia are migrare, ecran şi generator propriu; nu e o variantă a celei de azi.

### Trei lucruri mici, din aceeaşi sesiune

1. **„Secţia" a ieşit din formularul de mişcare** (cererea utilizatorului: *„Generator intern
   (Secţia) şterge de tot. Doar în tabelul ăla să apară automat birouri şi producţie. Atât"* — iar
   „tabelul ăla" e, precizat imediat, **cap. 2 Stocare** al Evidenţei gestiunii). Nu se mai alege pe
   mişcare; rubrica se completează singură cu secţiile punctului de lucru, exact ce face decizia 19
   când mişcarea nu numeşte niciuna. **Ce nu s-a atins:** coloana şi entitatea rămân, iar la editarea
   unei mişcări vechi valoarea ei se **păstrează** — o fişă tipărită până acum nu se schimbă. Lista
   de secţii se administrează în continuare în **Setări**, sub punctele de lucru; de acolo vin
   „Birouri" şi „Producţie".

2. **Anexa 3 nu dispăruse, doar tăcea.** Reclamaţia: *„e o problemă la adaugă mişcare, nu mai este
   «generează Anexa 3», doar să completezi Anexa 1 Ambalaje când pun cod de 15 01 01."* Verificat în
   git: condiţia `showAnexa3Section` e neatinsă de sesiunea asta — cere de dinainte un **partener
   ales**, fiindcă Anexa 3 e dovada predării şi n-are ce tipări fără destinatar. Ce se schimbase e că
   blocul de ambalaje apare acum imediat ce codul e `15 01 xx`, deci ocupă locul unde ochiul o
   căuta. Reparaţia e la vizibilitate, nu la logică: când mişcarea e o ieşire şi secţiunea nu apare,
   scrie **de ce**.

3. **Foile XLSX sunt protejate.** Art. 6 cere „format electronic «.xls» **protejat împotriva
   modificării datelor** şi pe suport hârtie". Parola e goală dinadins — protecţia opreşte
   modificarea din greşeală, dar clientul o poate ridica dacă are de corectat ceva. Şi tot de acolo
   se vede că **PDF-ul nu e un moft**: e exemplarul pe hârtie pe care actul îl cere alături de fişier.

## Ce intră în Anexa 1 Ambalaje se bifează, nu se deduce din cod (25.08.2026)

**Reclamaţia, a treia oară pe acelaşi ecran** — şi de data asta cu diagnosticul corect în ea:

> „Anexa 1 ambalaje este pentru producători şi importatori care pun ambalaje pe piaţă. Trebuie cumva
> să bifezi un checkbox. [...] Dacă pun 15 01 01 cred că îmi blochează, pentru că se consideră
> automat ambalaj după cod."

### Întâi, ce **nu** era stricat — verificat în aplicaţia live, nu din citit

Primele două explicaţii pe care le-am dat au fost citite din cod şi n-au convins, pe bună dreptate.
Deci am deschis producţia în browser şi am reprodus fluxul, pas cu pas, pe `Ecodoc SRL`:

1. „Adaugă mişcare" → cod `15 01 01` → blocul **Ambalaje** apare. Corect.
2. Operaţiune = **Generare** → nu există Anexa 3, fiindcă nu există predare. Corect.
3. Operaţiune = **Valorificare** → apare „Cod operaţiune (R/D)" **şi** linia nouă: *„Anexa 3 (dovada
   predării) apare după ce alegi partenerul care preia deşeul — fără destinatar, formularul n-are ce
   tipări."*
4. Partener = **Hamburger Recycling** → **Anexa 3 apare**, sub blocul de ambalaje. Ambele
   secţiuni, în acelaşi formular, în acelaşi timp.

**Deci nimic nu se blochează şi nimic nu dispare.** Condiţia Anexei 3 e neatinsă din 24.08 (cere un
partener, verificat în `git diff`); ce se schimbase e că blocul de ambalaje apare acum imediat ce
codul e `15 01 xx` şi ocupă locul unde ochiul căuta Anexa 3. Explicaţia adăugată mai devreme e live
şi se vede în captură.

Verificat şi al doilea lucru din reclamaţie: **cantităţile intră deja în amândouă**. Aceeaşi mişcare
pe `15 01 01`, registru Anexa 1, hrăneşte şi *Evidenţa gestiunii deşeurilor generate*
(`EvidenceCalculator` filtrează `register == ANEXA_1`) şi *Anexa 1 Ambalaje*. Nu e nimic de reparat
acolo.

### Ce **era** stricat: codul decidea în locul omului

Partea de fond a reclamaţiei era corectă, şi e o eroare de model, nu de ecran. Regula de includere
era: *orice mişcare pe cod `15 01 xx`, din registrul Anexa 1, intră în declaraţie*. Prea larg.

Declaraţia se cheamă „**Producători şi importatori** de ambalaje de desfacere, **de produse
ambalate**, supraambalatori" şi raportează **ambalajul pe care firma l-a introdus pe piaţa
naţională** — nu orice deşeu de ambalaj care trece prin curte. Un magazin care aruncă cutiile în
care i-a venit marfa generează deşeu pe `15 01 01`, dar ambalajul acela l-a pus pe piaţă furnizorul
lui. **Aceleaşi kilograme, două documente diferite**: fişa de evidenţă — mereu; Anexa 1 Ambalaje —
numai dacă el le-a introdus.

**Reparaţia (`V27`):** o bifă pe mişcare, „**Ambalaj pus de noi pe piaţa naţională**", în capul
blocului de ambalaje. Nebifat, restul rubricilor (material, fel) se ascund — n-au sens dacă mişcarea
nu ajunge în tabel — iar cantitatea rămâne în evidenţa gestiunii ca oricare alta.

| Valoare | Ce înseamnă | Ce face |
|---|---|---|
| **bifat** | firma a introdus ambalajul pe piaţă | intră în ambele tabele ale Anexei 1 Ambalaje |
| **nebifat** | l-a pus altcineva pe piaţă | rămâne **doar** în evidenţa gestiunii; în tab apare gri, „Nu — nu l-am pus noi pe piaţă" |
| **null** | mişcare de dinaintea întrebării | se poartă **ca înainte** (intră), ca să nu se schimbe de la sine o cifră deja tipărită; în tab apare portocaliu, „Din cod, neconfirmat" |

Implicitul pe o mişcare **nouă** e **nebifat**: întrebarea e „ai pus **tu** ambalajul pe piaţă?", iar
un „da" presupus e chiar ce se reclama.

**Unde a ajuns bifa şi unde nu.** Prima variantă a fost în **profilul firmei** — o întrebare la
nivel de companie („îţi îndeplineşti individual obiectivele sau le-ai transferat unui OIREP?", art. 1
alin. (1)). A fost **revertită la cererea utilizatorului**: *„eu cred că atunci când înregistrezi
mişcarea să fie acel checkbox, nu în profil"*. Are dreptate practic — răspunsul diferă de la o
mişcare la alta, iar o firmă poate pune pe piaţă un ambalaj şi arunca altul primit. Întrebarea de
profil rămâne notată ca **AB**, nescrisă.

**Teste:** `packagingSomebodyElsePutOnTheMarketStaysOutOfTheDeclaration` — 300 kg bifate şi 900 kg
nebifate pe acelaşi cod dau **300** în tabelul 1, o singură linie în tabelul 2, iar rândul de 900
apare în registrul tabului cu `countsForAnexa1Packaging: false`. **167 de teste verzi.**

### 🟡 Întrebarea AC — deja trimisă Andreei

Utilizatorul a trimis-o în timp ce se construia, deci regula de mai sus e **implementată dar
neconfirmată**:

> „Salut Andreea, am făcut tabul Ambalaje. Bazat pe ce regulă să se afişeze în tabul Ambalaje şi în
> «Anexa 1 Ambalaje» mişcările care se includ în această anexă? Să fac un checkbox «Deşeuri de
> ambalaj puse pe piaţa naţională (Anexa 1 Deşeuri)» pentru codurile de ambalaj?"

Până la răspuns rămâne bifa. Dacă Andreea spune că regula e alta — de pildă că se ia după rolul
firmei, nu după mişcare — se schimbă un singur filtru în `PackagingDeclarationBuilder`, plus
implicitul bifei. Coloana rămâne oricum utilă.

### Commis şi pushat, tot (25.08.2026, ora 15:24)

| Unde | Ce | Stare |
|---|---|---|
| `origin/main` şi `origin/deploy/heroku-split` | `228434b` | ✅ |
| `newrepo/main` | `8f1ebd3` | ✅ `ecoregistru-api` la **v29**, `V27` aplicată, pornire 8,36 s |
| `ferepo/main` | `2a77e3b` | ✅ `ecoregistru-app` la **v22** |

### Commis şi pushat (25.08.2026, ora 13:17)

| Unde | Ce | Stare |
|---|---|---|
| `origin/main` şi `origin/deploy/heroku-split` | `19cfa5c` | ✅ |
| `newrepo/main` | `3ad4d97` | ✅ `ecoregistru-api` la **v28**, pornire 9,17 s (fără migrare nouă) |
| `ferepo/main` | `f74869a` | ✅ `ecoregistru-app` la **v21** |

Releaseurile intermediare ale zilei: api `v26` (`58e1025`, cu `V26`) → `v27` (`15fb46e`,
reparaţia registrului) → `v28`; app `v19` → `v20` → `v21`.


## Transportatorul se configurează, iar șoferii nu se mai rescriu (`V28`) (02.09.2026)

**Cererea:**

> „vreau la parteneri să poți configura și transportator și să se vadă pe anexa 3 transport
> transportatorul și să îl poți selecta de acolo și să poți cumva să configurezi și șoferii de acolo
> sau să scrii free text. să facem un tab nou pentru transportator? ideea e că uneori firma care
> colectează și transportă, alteori nu poate să transporte o firmă de transport mai mare."

### Ce exista deja, și de ce n-a fost construit a doua oară

Jumătate din cerere era livrată din `V10` și n-am atins-o:

- `WasteMovement` avea `transportPartner`, `driverName`, `driverIdentification`,
  `vehicleRegistration`;
- formularul de mișcare avea select-ul **Transportator**, cu implicitul `— transportăm noi —`;
- `Anexa3FormGenerator.carrierColumn()` tipărea deja coloana „Date de identificare transportator" —
  nume, adresă, CUI, Reg. Com., licența și data expirării — luate de la partenerul ales, sau de la
  firma noastră când nu e ales niciunul.

Ce lipsea era exact ce se **configurează**: nu puteai marca un partener ca transportator (select-ul
lista toți partenerii activi, nefiltrat), iar cele trei rubrici ale delegatului se scriau de mână la
fiecare transport, deși vin aceiași doi-trei oameni cu aceleași mașini luni de zile.

### Decizia: bifă, nu tab, nu tip

Întrebarea din cerere („să facem un tab nou?") s-a pus înainte de orice cod, cu trei variante puse
pe masă. Răspunsul a fost **bifa**, iar motivul e chiar exemplul din cerere: firma care *și*
colectează, *și* transportă. Cu tab separat sau cu o a patra valoare în `PartnerType`, aceeași firmă
s-ar fi introdus de două ori, cu CUI și adresă de ținut sincronizate manual — problema pe care a
rezolvat-o `V23` la punctele de lucru. Codul luase deja aceeași decizie de două ori: rolul comercial
e două flaguri, nu un enum (`V7`), iar javadoc-ul lui `PartnerType` scrie de la `V10`
*„There is deliberately no CARRIER"*. Rămâne adevărat: hauling-ul e o rubrică a unui transport anume;
bifa spune doar **cine poate** apărea acolo.

### Nodul: `type` a devenit nullable

O firmă de transport pură nu e nici GENERATOR, nici COLLECTOR, nici RECOVERER — nu face nimic cu
deșeul, îl mută. A o trece „Colector" ar fi fost o cifră ghicită pe o rubrică tipărită: coloana „Tip"
apare în dosarul de control, iar prebifarea casetei „Destinat:" de pe Anexa 3 se citește **chiar din
tip** (decizia de la G3b/`V11`). Deci `type` s-a relaxat la nullable, cu înțelesul „doar
transportator".

E a **doua** relaxare de migrare din proiect, după `quantity` (`V10`), și urmează aceeași regulă: se
relaxează doar când alternativa e să ghicim. Constrângerea n-a dispărut, s-a mutat în serviciu — **ori
tip, ori bifa** — și are testul ei (`aPartnerThatIsNeitherIsRefused`).

### Backfill-ul e din răspunsuri deja date, nu din ghicit

`V28` bifează „Transportator" la partenerii care **erau deja folosiți** ca atare: cei care apar pe o
mișcare la `transport_partner_id`, și cei cărora li s-a completat licența de transport. Amândouă sunt
lucruri scrise de om. Restul rămân nebifați.

### Șoferii: un tabel cu `partner_id` **nullable**

Asta e miezul feliei:

| `partner_id` | Cine sunt | Unde se editează |
|---|---|---|
| completat | șoferii transportatorului | fișa partenerului, ca punctele de lucru |
| `NULL` | **șoferii noștri** | Setări, sub generatorii interni |

Rândurile cu `NULL` sunt exact cazul `— transportăm noi —`, care altfel rămânea pe free text pe veci.

**Ce se salvează pe mișcare rămân tot cele trei coloane text din `V10`, nu o cheie străină.**
Alegerea unui șofer precompletează câmpurile, atât. Motivul e că formularul tipărește un
**instantaneu** — actul de identitate de la data aia, mașina de la data aia — iar o mișcare veche
trebuie să tipărească mâine exact ce tipărea ieri, chiar dacă omul și-a schimbat între timp buletinul
sau a plecat de la firmă. Free textul rămâne prima clasă, nu o portiță.

### Trei alegeri de interfață care nu sunt cosmetice

1. **Transportatorii se grupează, nu se filtrează.** Select-ul de pe mișcare pune întâi `<optgroup>`
   „Transportatori", apoi „Alți parteneri". Regula casei e că un răspuns lipsă nu restrânge nimic
   (vezi „profil gol = fără restricție"): dacă nimeni n-a bifat încă pe nimeni, un filtru dur ar goli
   select-ul și ar arăta ca un defect. Sub el scrie care e situația.
2. **Licența și șoferii apar doar la bifat.** Cele două câmpuri de licență se cereau până acum
   tuturor partenerilor, inclusiv unui valorificator care n-a transportat nimic niciodată.
3. **Debifarea „Transportator" NU șterge șoferii.** Bifa se ia jos pentru un sezon — colectorul care
   de obicei transportă, dar iarna asta nu — și o listă de oameni scrisă de mână n-are voie să
   evaporeze pe un checkbox. Are test (`untickingCarrierKeepsTheDrivers`).

### Un singur drum de scriere per fel de șofer

Șoferii unui transportator se scriu **doar** prin formularul partenerului, unde lista se înlocuiește
la salvare. `POST/PUT/DELETE /api/v1/drivers` ating doar șoferii noștri și **refuză** explicit un
șofer care are partener (`driver.belongs.to.partner`). Fără regula asta, un șofer adăugat prin
endpoint ar fi dispărut data viitoare când cineva deschidea și salva partenerul — un bug tăcut, greu
de reprodus. `GET` întoarce tot, fiindcă formularul de mișcare are nevoie de amândouă felurile
într-un apel.

### Verificat pe hârtie, nu doar în teste

Regula 4 a casei: PDF-ul s-a randat și s-a **uitat** cineva la el. Anexa 3 a unei mișcări cu
transportator configurat tipărește în coloana din stânga **Trans Greu SA**, cu CUI-ul, adresa și
Reg. Com. **lui**, licența `LIC 4417/2025` expirând `31.03.2027`, iar dedesubt delegatul
`Ion Popescu` / `CJ 123456` / `CJ 01 ABC` — toate venite din configurare, niciuna scrisă de mână.
Expeditorul rămâne firma noastră. Testul `anexa3PrintsTheChosenCarrierAndHisLicence` lasă PDF-ul în
`backend/build/anexa3-carrier.pdf` după fiecare rulare, ca să poată fi deschis din nou.

### Stare

**175 de teste verzi** (8 noi, în `CarrierAndDriversIT`), migrări până la **`V28`**; următoarea
liberă e **`V29`**.

---

## Ce urmează — plan revizuit (22.08.2026)

Ordinea e dictată de **risc de rework**, nu de valoare vizibilă. Exportul oficial e ultimul lucru
construit, deși e singurul pe care îl vede clientul: nimic construit peste o formulă de stoc greșită
nu se salvează.

> ⚠️ **Tabelul ăsta e de pe 22.08 şi a fost depăşit de feliile G.** Etapele 3, 4 şi 5 s-au livrat
> sub alte nume (G2+G4, G5, G6) după meeting-ul din 23.08. **Sursa de adevăr pentru ce urmează e
> tabelul G de mai jos** plus lista din `plan-executie.md`; ăsta rămâne ca să se vadă de unde am
> plecat. Ce a mai rămas nelivrat din el: **7** (cadenţele AFM) şi
> **8–11** (modulul de depozit).

| # | Etapă | Depinde de | Mărime |
|---|---|---|---|
| 0 | ✅ Documentare legislativă (inclusiv runda „depozite", 22.08) | — | **GATA** |
| 1 | ✅ **Nomenclator LED** — 842 coduri din Decizia 2014/955/UE | — | **GATA** |
| 2 | ✅ **Model: operațiuni + stoc + cele trei evidențe** — reparația critică | 1 | **GATA** |
| 3 | ✅ Cap. 2 ca profil (5 nomenclatoare + `Secția`) — *livrat ca G2 + G4* | 2 | **GATA** |
| 4 | ✅ **Export oficial Anexa 1** (4 capitole) — *livrat ca G5* | 1, 2, 3 | **GATA** |
| 5 | ✅ **Centralizator anual** — *livrat ca G6*. 🔜 Conversia kg→tone rămâne, dar e a registrului art. 48 (Etapa 8), nu a centralizatorului: fişa şi declaraţia sunt în kg | 4 | **GATA** (partea de centralizator) |
| 6 | ✅ **Dosar de control dimensionat la 3 ani** — livrat 24.08.2026 (`years=1..3`, folder per an, avertisment pe anul fără evidență) | 4 | **GATA** |
| 7 | 🟠 **Obligațiile AFM ca set de contribuții + trei cadențe** — reparație în Termene | — | M |
| 8 | **Modul depozit — ecrane** (Recepții/Livrări, registru art. 48, formulare HG 1061, ceas SIATD) | 2 | L |
| 9 | Borderou de achiziție la metale (OUG 31/2011) + regim GDPR pentru CNP | 8 | M |
| 10 | Profil groapă (registru recepție HG 349 art. 15, raportare semestrială, alertă 12h) | 8 + cuantumul din anexa 2 | M |
| 11 | Modul ambalaje (Ordin 794/2012 anexa 3, **în kg**) | 8 | M |

**Restanțe mici (S, se pot lua oricând, nu blochează nimic):**

- **Căutarea de coduri e sensibilă la diacritice.** `WasteCodeRepository.search` face
  `lower(...) like`, iar denumirile din nomenclator au diacritice: cine tastează „deseuri" nu
  găsește nimic. Cu 10 coduri nu conta; de la Etapa 1 încoace, cu 842, se vede. Reparație:
  `unaccent` sau o coloană normalizată, plus un test pe „deseuri" vs. „deșeuri".

**Etapa 2 e livrată integral (2a–2d, 23.08.2026); G1, G2 și G3 sunt livrate peste ea.** Următoarea
migrare liberă e **`V13`** (`V5` = seam-ul de registru, `V6` = modelul de stoc, `V7` = modulul de
generatori, `V8` = profilul de cont, `V9` = cererile de cont, `V10` = Anexa 3, `V11` = tipul de
partener, `V12` = transportul din cap. 2).

### Schițele de la meeting (docs, 23.08.2026) — ce confirmă și ce deschide

Notițele de mână ale specialistei, șapte pagini. Confirmă G1–G3 aproape punct cu punct
(„fără predare”, „doar ce are el nevoie de tipul lui de business”, „sub cod deșeu ⟹ stocare /
tratare”, „după operațiune R1, R2 în funcție de ce s-a ales mai sus”, „Anexa 3 transport … la final
după ce au fost introduse datele”, „10 kg (se cântărește la descărcare)”). Ce **nu** e încă făcut:

- ✅ **„La parteneri ⟹ fără transportator”** — confirmat. `PartnerType` e de-acum
  **`GENERATOR` / `COLLECTOR`**, atât. Transportatorul nu e o categorie de partener, ci o rubrică a
  unui transport anume, iar `V10` îl pusese deja acolo unde îi e locul: pe mișcare
  (`transport_partner_id`), lângă șofer și numărul de înmatriculare, cum cere Anexa 3. `V11` pliază
  `CARRIER` și `BOTH` pe `COLLECTOR` — amândouă descriau un operator care mișcă sau preia deșeu,
  niciunul un generator, deci nu se pierde nimic. Partenerul primește și **adresa punctului de
  lucru**, separat de sediu: pe modelul completat destinatarul e scris cu „P.L. ILFOV, Șos. de
  Centura nr. 2-8”, nu cu sediul social. Autorizația de mediu (număr + expirare) o avea deja.
- ✅ **„Evidență – Tabel: scot generat / adaug cantitate, data când s-o predat; la valorificare să
  apară partenerul și cod V/R/D. Și atât.”** Confirmat că e vorba de **ecran**, nu de formular.
  Tabul **Evidențe** are de-acum două vederi, cu aceleași filtre: **„Predări”** (implicită) e
  registrul cerut — data predării, cod deșeu, cantitate, operațiune (litera + codul R/D), partener,
  punct de lucru — și e rândul din care se tipărește Anexa 3; **„Anexa 1 — lunar”** e agregatul de
  până acum. Vederea lunară **rămâne** fiindcă poartă **stocul cumulativ**, singura cifră pe care
  ochiul n-o poate reface din rânduri și exact cea în jurul căreia e construită fișa. Predările fără
  cod R/D (liniile vechi) apar în registru marcate „Incomplet”, iar cele necântărite cu „De
  cântărit”.
- 🔜 **„Când dă print la dosar control să respecte structura de la tabelele pe care le am de la
  Andreea !!! (la generator) + Anexa 1”** — **asta e G5, felia următoare**, și acum se știe exact ce
  înseamnă. „4 tabele ca în exemple” (mesajul din 23.08) nu erau patru exemplare pe pagină, ci
  **cele patru capitole ale fișei Anexa 1**, arătate în
  `raportare deseuri generate_Bragadiru 2024.xlsx` și în
  `deseuri generate_Cluj_2025_Iuhos Lorena.pdf`: antet (agent economic · an · tip și cod deșeu ·
  stare fizică · u.m. · stoc), apoi **1. GENERAREA** · **2. STOCAREA PROVIZORIE, TRATAREA ŞI
  TRANSPORTUL** · **3. VALORIFICAREA** · **4. ELIMINAREA**, fiecare cu 12 rânduri și TOTAL AN, tot
  pe o singură pagină, câte una per cod de deșeu.
  Ce alimentează fiecare capitol: **cap. 1** din motorul de evidență (are deja generat/valorificat/
  eliminat/stoc), **cap. 2** din secție + stocare + tratare + transport (complet de la G4),
  **cap. 3 și 4** din mișcările de ieșire, cu codul R/D și numele partenerului. Ce rămâne de decis:
  ce se tipărește într-o lună cu mai multe predări diferite — fișa are exact 12 rânduri, iar
  propunerea e să se listeze valorile distincte („R3, R13”), nu să se aleagă una.
  ⚠️ **Antetul cap. 3/4 din exemple trimite la Legea 211/2011, abrogată de OUG 92/2021.** Numerele
  anexelor sunt aceleași (3 = valorificare, 2 = eliminare), deci corectura e doar numele actului —
  dar e o abatere de la model pe un formular oficial și **e decizia specialistei**, nu a noastră.
- ✅ **„D5 peste tot; nu ar trebui să mai fie D1”** — deja aplicat (datele demo și
  `docs/legislatie.md` foloseau D5 din 20.08).
- 📎 **„Anexa 1 e strict pentru generatorii de deșeuri de ambalaj (producători/importatori)”** și
  **„SIM se bazează pe documentele pe care le avem”** — încadrează modulul de ambalaje, care rămâne
  după modulul de generatori.
  ⚠️ **Atenție, e o altă „Anexa 1”.** Lămurit pe 24.08 (R19): fraza se referă la **anexa 1 la
  Ordinul 794/2012**, al cărei titlu trebuie citit până la capăt — „Producători şi importatori de
  ambalaje de desfacere, **de produse ambalate**, supraambalatori de produse ambalate”. Nu e despre
  fabricanții de ambalaje, ci despre **oricine pune pe piață marfă ambalată**, deci despre o
  populație largă. Documentul are tabele pe materiale (PET, hârtie, aluminiu), în kg.
  **Nu** e fișa de gestiune din **HG 856/2002 anexa 1**, cea cu patru capitole × 12 luni pe care o
  generează G5. Dovada e în `documente oficiale/`: `RAPORTARE AMBALAJE _anexa 1.xlsx` și
  `RAPORTARE AMBALAJE 2021_anexa 1_ HRR.xlsx` încep cu exact titlul ăla. Deci fraza **nu restrânge**
  fișa Anexa 1 la ambalaje — foile fișierelor completate sunt pe `20 01 01`, `20 03 01`, `19 12 12`,
  `20 01 36`, iar HG 856 art. 1 alin. (1) obligă orice generator. Nimic de schimbat în cod; totul de
  reținut înainte de a schimba ceva.

Fișierul rămâne **negitignored local, dar necommis**: e o notiță internă scrisă de mână, iar repo-ul
e public.


**Ordinea s-a schimbat la meeting-ul din 23.08.2026:** se construiește întâi **modulul de
generatori**, cap-coadă. Etapele 8–11 (depozit, borderou, groapă, ambalaje) rămân în listă, dar
după ce generatorul e complet. Ce urmează imediat, în ordine:

| # | Felie | Depinde de | Mărime |
|---|---|---|---|
| G1 | ✅ Registru închis · rol comercial de partener · generator intern · operațiuni pe tip de cont | 2 | **GATA** |
| G2 | ✅ Formular de cerere de cont · profil de firmă · cap. 2 (stocare/tratare) sub codul de deșeu | G1 | **GATA** |
| G3 | ✅ **Anexa 3 — dovada predării**, generată din mișcare · cantitate cântărită la descărcare | G2 | **GATA** |
| G4 | ✅ Cap. 2 — ultimele două nomenclatoare (Transport: mijlocul, destinația) | G2 | **GATA** |
| G5 | ✅ **Fișa oficială Anexa 1** — antet + cele 4 capitole, o pagină per cod | G4 | **GATA** |
| G6 | ✅ **Declarația anuală** (foaia `raportare deseuri generate`): un rând per cod, stoc → generat → valorificat → eliminat → stoc, o pagină per punct de lucru | G5 | **GATA** |
| G7 | ✅ **Dosarul de control pe structura Andreei** — fișa Anexa 1 în arhivă · titlul „Evidenţa gestiunii deşeurilor generate «an»" · termenul de 15 martie numit după document · întrebarea „ce tip de generator" | G5 | **GATA** |

**Ce a rămas deschis după Etapa 2, în ordinea în care doare:**

- ✅ **Codul de operațiune la predarea către un colector** (întrebarea 3) — **închisă pe 24.08**
  (R23): „la înregistrare, codurile alese de client". Nu există regulă de codat; alege omul care
  înregistrează mișcarea, iar aplicația i-o cere. Comportamentul de azi e deci confirmat, nu
  schimbat. Cele 13 predări vechi fără cod **rămân** `incomplete` — nu se migrează, fiindcă nu
  există regulă din care să le derivăm.
  🟠 Jumătatea a doua a întrebării rămâne deschisă (**întrebarea C**): cine se scrie la „agentul
  economic care efectuează operaţia" — colectorul căruia i-am predat, sau reciclatorul final.
- ✅ **Predările de marfă preluată nu se mai semnalează** — `resaleSuspected` scos pe 24.08 (G8,
  migrarea `V14`): preluarea de la terți nu interesează modulul de generatori. Mutarea reală a
  fluxului art. 48 în `Reception`/`Delivery` rămâne Etapa 8, ca înainte.
- 🟡 **`total_collected` a rămas în schemă**, cu default 0 și nescris de motor. Se șterge tot în
  Etapa 8, împreună cu mișcările `COLLECTED` pe care le descria.

**Ce a rămas neclasificat după `V5`, și de ce nu ghicim:**

1. **Ieșirile vechi n-au cod R/D.** Pe baza de dev sunt 13 predări fără cod — nu pot fi clasificate
   retroactiv, fiindcă a inventa o operațiune ar pune o cifră născocită pe un formular oficial.
   Contractul pentru 2b: cantitatea **se scade din stoc** (a plecat fizic), dar nu intră în niciuna
   dintre cele două coloane oficiale, iar linia se marchează **incompletă**. Astfel Anexa 1 nu „se
   închide" tăcut pe date lipsă — se vede că e ceva de completat. Editarea unei astfel de mișcări
   cere de-acum codul, deci completarea se face natural, prin ecranul care există.
   **De pe 24.08 (G8) se vede cu roșu**, badge „Fără cod R/D", nu cu galben: nu e o rubrică de
   completat cândva, e o cantitate care lipsește din declarație.
2. **Predările de marfă preluată au rămas în `ANEXA_1`, și rămân netulburate.** Backfill-ul poate
   clasifica preluarea în sine (`COLLECTED`), dar o predare care dă mai departe marfă colectată
   arată identic cu predarea de deșeu propriu. Nu există selector de registru în UI, și **nici nu se
   adaugă unul**: după Etapa 8, fluxul art. 48 se înregistrează ca `Reception`/`Delivery`, iar
   `waste_movements` rămâne Anexa 1 curat. `register = ART_48` pe o mișcare e o stare
   **tranzitorie**, pentru liniile vechi, pe care migrarea din Etapa 8 le mută.
   **Semnalul `resaleSuspected` s-a scos pe 24.08 (G8, `V14`)** — preluarea de la terți nu
   interesează modulul de generatori, iar un generator pur nici nu poate înregistra `COLLECTED`.
   Separarea registrelor rămâne: art. 2 alin. (1) e în continuare în filtrul motorului, cu testul lui.

**Etapa 2 era cea critică; e închisă.** `EvidenceCalculator` scădea `handedOver` **peste**
`recovered`/`disposed` și aduna `COLLECTED` în aceeași linie cu `GENERATED` — două lucruri pe care
fișa oficială (HG 856 Anexa 1, cap. 1) și art. 2(1) le exclud. Ambele sunt reparate, cu testele care
le țin așa. Detalii: `surse-oficiale.md` §1.1–1.2. Din cele trei evidențe, două sunt separate în cod
(Anexa 1 · registrul art. 48); registrul de recepție al depozitului rămâne pentru Etapa 8.

**Etapa 7 e o corectitudine, nu o funcționalitate nouă.** Azi generăm un termen AFM **lunar pe 25**
pentru orice firmă cu `afmObligation = true`. Dar OUG 196/2005 art. 11 are trei cadențe, iar o firmă
care datorează doar contribuția anuală la ambalaje primește de la noi **11 alerte greșite pe an**.
E singurul element din listă care produce output incorect pentru clienții existenți, nu doar lipsă
de funcționalitate — de asta nu depinde de nimic și poate fi luată oricând, chiar înaintea Etapei 2
dacă apare un client afectat.

**Răspunsul din 24.08 o restrânge și mai mult** (R25): „obligația AFM, doar generatorii de deșeuri de
ambalaj — producători/importatorii". Pentru un cont de tip generator, obligația se condiționează deci
de calitatea de producător/importator — cine pune pe piață produse ambalate —, iar un generator obișnuit **nu primește niciun
termen AFM**. Celelalte două contribuții rămân valabile la tipurile de cont fără ecrane: cei 2%
reținuți la sursă de un centru de colectare (lunar, art. 9(1) lit. a) și contribuția pentru economia
circulară a depozitelor (trimestrial). Deci `afmObligation` devine un **set** de contribuții datorate,
fiecare cu ritmul ei, și **niciuna nu se presupune** — exact tratamentul dat rolului de partener în
`V7` și codului R/D în `V5`.

## Decizie de produs (22.08.2026) — aplicația servește și depozitele

Vechea Etapă 7 era marcată „decizie de produs". Decizia s-a luat: **da**, EcoRegistru acoperă și
operatorii de depozit, nu doar generatorii. Toate trei tipurile, cu grade diferite de pregătire
(etapele 8–11 în tabelul de mai sus):

| Tip de client | Ce cere legea în plus față de un generator | Stare documentare |
|---|---|---|
| **Centru de colectare / depozit de reciclabile** | registru art. 48; formulare de încărcare-descărcare; confirmări SIATD 3/5/15 zile; borderou de achiziție la metale (OUG 31/2011); **contribuția AFM de 2% reținută la sursă**, lunar pe 25 | ✅ **integral verificat** pe surse primare — `surse-oficiale.md` §2.1, §4, §6, §9, §10.1 |
| **Stație de sortare / tratare** | idem + cantitățile rezultate din valorificare + chestionar SIM `TRAT` | ✅ obligațiile de evidență; 🔴 doar chestionarul SIM |
| **Depozit de deșeuri (groapă) / eliminare** | HG 349/2005: **registru de recepție** (art. 15(1) lit. d, cu localizarea precisă a periculoaselor), buletine de analiză, probe păstrate o lună, **raportare semestrială** + alertă de 12 ore (art. 20); **contribuția pentru economia circulară**, trimestrial pe 25 | ✅ obligațiile, verificate — `surse-oficiale.md` §8, §10.2; 🟠 lipsește doar **cuantumul** din anexa 2 la OUG 196/2005 |

**Principiul de design** (formulat în `docs/prezentare-specialist.html`): *recepția e documentul primar,
evidența e derivată din ea.* Omul de la depozit înregistrează recepția așa cum o face oricum — furnizor,
material, cântar, preț — și din ea se propagă restul, fără dublă introducere.

⚠️ **Precizarea care evită repetarea bugului actual.** Recepția NU alimentează Anexa 1. Art. 2 alin. (1)
din HG 856 o interzice explicit. Ea alimentează **registrul cronologic de la art. 48 OUG 92/2021** —
un flux paralel, cu alt format și alt destinatar. Anexa 1 a unui depozit conține doar deșeul generat în
activitatea proprie (inclusiv refuzul de la sortare). Formularea „din recepție iese mișcarea, din mișcări
iese evidența lunară" din prezentare e corectă doar cu „evidența lunară" = registrul art. 48.

**Vestea bună:** registrul art. 48 **nu are formular oficial impus.** HG 856 art. 2(2) spune că se
prezintă „la solicitarea autorităţilor", iar OUG 92/2021 art. 48(1) lit. a–c îi dă conținutul (cod,
cantitate în tone, natura și originea, destinația, frecvența colectării, modul de transport, metoda de
tratare, cantitatea încredințată spre eliminare). Deci nu încălcăm regula de aur construindu-l — nu
inventăm un format oficial, ci un registru care poartă conținutul cerut de lege.

**De ce contează SIATD:** generatorii mici nu intră (Ordin 701/2024 art. 2), dar un depozit intră
aproape sigur, și are un ceas de 3/5/15 zile pe fiecare recepție, cu suspendarea accesului ca sancțiune.
Un ecran „ai N recepții neconfirmate, 3 expiră mâine" e o funcționalitate pe care modulul Termene de azi
nu o poate exprima: e declanșată de eveniment, nu de calendar.

**Secvențierea aleasă:** doar *decizia de design* intră în Etapa 2 — entitățile și comutatorul
`CompanyType`, fără ecrane. Ecranele de depozit sunt Etapa 8, după Anexa 1 oficială. Motivul: migrarea
lui `COLLECTED` se face o singură dată, iar exportul oficial se construiește peste un model deja corect.

### Documentare completată pe 22.08.2026 (runda „ce lipsește pentru depozite")

Verificate pe Portalul Legislativ, cu citate verbatim în `surse-oficiale.md` §8–§10:

- **HG 349/2005** — procedura de recepție în depozit și registrul obligatoriu (art. 15), raportarea
  semestrială + alerta de 12 ore (art. 20). Profilul de groapă nu mai e o pată albă.
- **OUG 31/2011** — borderoul de achiziție la metale: câmpuri complete, natura de document
  financiar-contabil, sancțiuni 100.000–150.000 lei. Conține **CNP** → regim GDPR distinct.
- **OUG 196/2005** — contribuțiile la Fondul pentru mediu, cu **trei cadențe** (lunar / trimestrial /
  anual), nu una singură cum presupune codul de azi.
- **Ordinul 794/2012** — unitatea Anexei 3 este **kg**; blocajul de factor 1000 s-a închis.

**Trei consecințe care depășesc modulul de depozit:**

1. 🔴 **Sunt trei evidențe, nu două.** Anexa 1 (deșeu propriu) · registrul cronologic art. 48 (marfa
   tranzacționată) · registrul de recepție al depozitului (HG 349 art. 15(1) lit. d, cu localizarea
   precisă a periculoaselor). Modelul din Etapa 2 trebuie să le țină pe toate trei distincte.
2. 🔴 **`Company.afmObligation` e prea sărac.** Un boolean nu poate exprima trei cadențe. Devine un
   **set de contribuții datorate**, fiecare cu ritmul ei. Afectează generatorul de termene existent,
   nu doar depozitele. Detalii: `surse-oficiale.md` §10.3.
3. 🟢 **Orice centru de colectare e client AFM lunar**, structural — reține 2% la sursă din fiecare
   achiziție (art. 9(1) lit. a). Nu depinde de ambalaje. Calculul cade direct pe `Reception`, care
   oricum are prețul. E și un argument de vânzare: obligația există fie că firma o știe, fie că nu.

---

## Blocaje rămase

- ✅ **Chestionarele SIM — închis pe 24.08.** Se completează cu **datele din anexe**: Anexa 1 la
  producător/importator, evidența la colector. Nu lipsește niciun câmp din model și niciun client
  nu va trebui să completeze retroactiv un an — de asta era roșu. Rămâne nevăzut doar **layoutul**
  ecranului, care ar scuti muncă la exportul SIM (felie târzie oricum); e o înlesnire, nu un blocaj.
- 🟠 **Cuantumul contribuției pentru economia circulară** (OUG 196/2005, anexa nr. 2) — Portalul
  Legislativ trunchiază anexele pe versiunile consolidate. Blochează doar profilul de groapă.
- 🟠 **Termenul de păstrare al borderoului de achiziție** — nu e în OUG 31/2011; intră sub Legea
  contabilității, de verificat separat.
- ✅ **SMTP** — deblocat pe 24.08 și **confirmat pe producție**: STARTTLS reparat, credențiale
  Gmail pe dyno, paginile de resetare/invitație construite, mail chiar livrat.
- 🔴 **Conturile de demo sunt în baza de producție.** `platform@ecoregistru.ro` și `admin@demo.ro`
  răspund la `request-reset-password` pe dyno-ul de producție, deşi `DevDataSeeder` e `@Profile("dev")`
  şi `SPRING_PROFILES_ACTIVE` e gol — au ajuns acolo altfel. Cât timp mailul nu pleca era inofensiv;
  de pe 24.08 nu mai e: **`demo.ro` nu e domeniul nostru**, deci cine îl controlează poate cere o
  resetare și intra în producție ca ADMIN pe tenantul demo. De șters sau dezactivat.
- 🟡 Cloudinary (upload real) — `CLOUDINARY_URL` nesetat pe `ecoregistru-api`, deci atașamentele
  de pe mișcări nu urcă în producție.

**Închis pe 22.08.2026:** ✅ *Unitatea din Anexa 3 la Ordinul 794/2012* — actul scrie `[kilograme]`
la toate cele cinci anexe. Fișierul în tone al specialistei e șablon modificat local. Modulul de
ambalaje nu mai e blocat pe unitate.

Model de produs: Faza 1 = „pregătim, nu transmitem" — ținem evidența și generăm ce trebuie raportat
(SIM/AFM), clientul încarcă în portalul oficial (portalurile n-au API public de transmitere de la terți).
