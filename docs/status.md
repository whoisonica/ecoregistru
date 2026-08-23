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
  (cod complet, degradează grațios).
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

---

## Ce urmează — plan revizuit (22.08.2026)

Ordinea e dictată de **risc de rework**, nu de valoare vizibilă. Exportul oficial e ultimul lucru
construit, deși e singurul pe care îl vede clientul: nimic construit peste o formulă de stoc greșită
nu se salvează.

| # | Etapă | Depinde de | Mărime |
|---|---|---|---|
| 0 | ✅ Documentare legislativă (inclusiv runda „depozite", 22.08) | — | **GATA** |
| 1 | ✅ **Nomenclator LED** — 842 coduri din Decizia 2014/955/UE | — | **GATA** |
| 2 | ✅ **Model: operațiuni + stoc + cele trei evidențe** — reparația critică | 1 | **GATA** |
| 3 | Cap. 2 ca profil (5 nomenclatoare + `Secția`) | 2 | M |
| 4 | **Export oficial Anexa 1** (4 capitole) | 1, 2, 3 | L |
| 5 | Centralizator anual + conversie kg→tone (art. 48) | 4 | S |
| 6 | Dosar de control dimensionat la 3 ani | 4 | S |
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
- ✅ **„nu e interesată de preluare de la terți (pentru modulul generat)”** — deja: un cont de tip
  generator nu primește deloc operațiunea de preluare.
- 📎 **„Anexa 1 e strict pentru generatorii de deșeuri de ambalaj (producători/importatori)”** și
  **„SIM se bazează pe documentele pe care le avem”** — încadrează modulul de ambalaje, care rămâne
  după modulul de generatori.

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
| G5 | 🔜 **Fișa oficială Anexa 1** — antet + cele 4 capitole, o pagină per cod, ca în exemplele completate | G4 | L |
| G6 | **Declarația anuală** (foaia `raportare deseuri generate`): un rând per cod, stoc → generat → valorificat → eliminat → stoc | G5 | M |

**Ce a rămas deschis după Etapa 2, în ordinea în care doare:**

- 🟠 **Codul de operațiune la predarea către un colector** (întrebarea 3 din
  `intrebari-specialist.md`) — nerezolvat, și acum se **vede**: cele 13 predări vechi fără cod sunt
  marcate `incomplete` și nu intră în nicio coloană. Codul nu propune niciun implicit.
- 🟠 **Predările de marfă preluată sunt semnalate, nu mutate.** `resaleSuspected` arată liniile
  suspecte (stoc negativ pe pereche, la demo); mutarea reală e Etapa 8, într-o singură migrare.
- 🟡 **`total_collected` a rămas în schemă**, cu default 0 și nescris de motor. Se șterge tot în
  Etapa 8, împreună cu mișcările `COLLECTED` pe care le descria.

**Ce a rămas neclasificat după `V5`, și de ce nu ghicim:**

1. **Ieșirile vechi n-au cod R/D.** Pe baza de dev sunt 13 predări fără cod — nu pot fi clasificate
   retroactiv, fiindcă a inventa o operațiune ar pune o cifră născocită pe un formular oficial.
   Contractul pentru 2b: cantitatea **se scade din stoc** (a plecat fizic), dar nu intră în niciuna
   dintre cele două coloane oficiale, iar linia se marchează **incompletă**. Astfel Anexa 1 nu „se
   închide" tăcut pe date lipsă — se vede că e ceva de completat. Editarea unei astfel de mișcări
   cere de-acum codul, deci completarea se face natural, prin ecranul care există.
2. **Predările de marfă preluată au rămas în `ANEXA_1`.** Backfill-ul poate clasifica preluarea în
   sine (`COLLECTED`), dar o predare care dă mai departe marfă colectată arată identic cu predarea
   de deșeu propriu. Nu există selector de registru în UI, și **nici nu se adaugă unul acum**: după
   Etapa 8, fluxul art. 48 se înregistrează ca `Reception`/`Delivery`, iar `waste_movements` rămâne
   Anexa 1 curat. `register = ART_48` pe o mișcare e o stare **tranzitorie**, pentru liniile vechi,
   pe care migrarea din Etapa 8 le mută. Până atunci, 2b le semnalează (o predare în `ANEXA_1` la o
   pereche punct-de-lucru/cod care are și preluări e suspectă) — semnalează, nu rescrie.

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

- 🔴 **Capturi din chestionarele SIM** (PRODDES / COL-TRAT) — în spatele login-ului pe
  `raportare.anpm.ro`, neobtenabile din documente publice. Singura întrebare care mai are rost să fie
  pusă specialistei.
- 🟠 **Cuantumul contribuției pentru economia circulară** (OUG 196/2005, anexa nr. 2) — Portalul
  Legislativ trunchiază anexele pe versiunile consolidate. Blochează doar profilul de groapă.
- 🟠 **Termenul de păstrare al borderoului de achiziție** — nu e în OUG 31/2011; intră sub Legea
  contabilității, de verificat separat.
- 🟡 SMTP (email real), Cloudinary (upload real).

**Închis pe 22.08.2026:** ✅ *Unitatea din Anexa 3 la Ordinul 794/2012* — actul scrie `[kilograme]`
la toate cele cinci anexe. Fișierul în tone al specialistei e șablon modificat local. Modulul de
ambalaje nu mai e blocat pe unitate.

Model de produs: Faza 1 = „pregătim, nu transmitem" — ținem evidența și generăm ce trebuie raportat
(SIM/AFM), clientul încarcă în portalul oficial (portalurile n-au API public de transmitere de la terți).
