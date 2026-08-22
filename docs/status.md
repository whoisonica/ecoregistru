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
  și frontend (`ecoregistru-app`) rulează pe dyno Basic. **Neîmpins pe `origin/main`.**
- ✅ **ETAPA 0 — Documentare legislativă (2026-08-22):** verificare integrală pe surse primare
  (Portal Legislativ, EUR-Lex, sgglegis.gov.ro). Nou: `docs/surse-oficiale.md` (citate verbatim cu
  link + dată). Corectat `docs/legislatie.md` (6 corecții, inclusiv **formula stocului**, care era
  greșită și în doc, și în cod). Actualizat jurnalul de răspunsuri + documentul trimis specialistei.
  **8 din 9 blocante 🔴 închise.** Zero cod atins.

---

## Ce urmează — plan revizuit (22.08.2026)

Ordinea e dictată de **risc de rework**, nu de valoare vizibilă. Exportul oficial e ultimul lucru
construit, deși e singurul pe care îl vede clientul: nimic construit peste o formulă de stoc greșită
nu se salvează.

| # | Etapă | Depinde de | Mărime |
|---|---|---|---|
| 0 | ✅ Documentare legislativă (inclusiv runda „depozite", 22.08) | — | **GATA** |
| 1 | 🔜 **Nomenclator LED** — 842 coduri din Decizia 2014/955/UE | — | S |
| 2 | 🔴 **Model: operațiuni + stoc + cele trei evidențe** — reparația critică | 1 | M–L |
| 3 | Cap. 2 ca profil (5 nomenclatoare + `Secția`) | 2 | M |
| 4 | **Export oficial Anexa 1** (4 capitole) | 1, 2, 3 | L |
| 5 | Centralizator anual + conversie kg→tone (art. 48) | 4 | S |
| 6 | Dosar de control dimensionat la 3 ani | 4 | S |
| 7 | 🟠 **Obligațiile AFM ca set de contribuții + trei cadențe** — reparație în Termene | — | M |
| 8 | **Modul depozit — ecrane** (Recepții/Livrări, registru art. 48, formulare HG 1061, ceas SIATD) | 2 | L |
| 9 | Borderou de achiziție la metale (OUG 31/2011) + regim GDPR pentru CNP | 8 | M |
| 10 | Profil groapă (registru recepție HG 349 art. 15, raportare semestrială, alertă 12h) | 8 + cuantumul din anexa 2 | M |
| 11 | Modul ambalaje (Ordin 794/2012 anexa 3, **în kg**) | 8 | M |

**Etapa 2 e cea critică.** `EvidenceCalculator` calculează azi stocul greșit: scade `handedOver`,
`recovered` **și** `disposed`, deși fișa oficială (HG 856 Anexa 1, Cap. 1) nu are coloană de predare.
În plus, `COLLECTED` e adunat în aceeași linie cu `GENERATED`, ceea ce **art. 2(1) din HG 856
interzice** — marfa preluată de la terți nu intră în Anexa 1. Detalii: `surse-oficiale.md` §1.1 și
`legislatie.md` §3. Etapa 2 nu doar șterge `COLLECTED`, ci îl **mută**: modelul trebuie să separe
cele **trei** evidențe (Anexa 1 · registrul art. 48 · registrul de recepție al depozitului).

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
