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

**Etapa 2 e livrată integral (2a–2d, 23.08.2026).** Următoarea migrare liberă e **`V7`**
(`V5` = seam-ul de registru, `V6` = modelul de stoc).

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
