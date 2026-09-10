# Stadiu — EcoRegistru

Jurnalul feliilor livrate, în ordinea în care au fost construite. Fiecare intrare marcată ✅
rulează local și are testele verzi.

> **Unde suntem — 10.09.2026.** 288 de teste verzi (0 eșecuri) și **11 probe de interfață, 281 de
> verificări**. Migrări până la **`V34`**, următoarea liberă e **`V35`**. În producție:
> `ecoregistru-api` la **v46**, `ecoregistru-app` la **v41**, cu **`V34` migrat acolo**.
> **P0 e închis, toate opt.** **11-bis și 11-ter sunt livrate și pe dyno**, cu probele lângă ele:
> atașamentele nu mai stau la un URL public — nesemnat dă `401`, semnat dă `200`, iar sub prefixul
> `ecoregistru` nu mai există niciun obiect public; atașamentul urcă și **se deschide în tab**.
> Colectorul de erori e aprins pe amândouă capetele.
> ✅ **`V34` (P1.12 — utilizatorii firmei) e deployată**, cu `V34` migrată pe dyno. Repo-ul și
> producția sunt din nou la același conținut.
> ⬜ **Rămâne de mers pe ecran cu sesiune reală** — drumul din `todo-lansare.md`, punctul 12.
> **Ce ține lansarea pe loc de-acum e P1, adică juridic** — SRL, DPA, termeni. Nu se rezolvă la
> tastatură.
>
> **Adăugat 09.09.2026, târziu de tot.** Trei lucruri pe care nu le-a găsit niciun compilator:
> deployul lui 11-bis; un **commit pierdut** la deployul dinainte (procedura de subtree lua un
> singur commit când erau două în așteptare — acum se verifică pe conținut, nu pe hash-uri); și
> `noopener`, care făcea ca **fiecare atașament să se descarce** în loc să se deschidă. Ultimul a
> fost găsit de utilizator, deschizând fișierul, după ce toate probele automate spuseseră „gata".
> Plus limita de mărime, care era afișată la 15 MB peste un zid real de 10.
> Secțiunea „11-bis pe producție, limita adevărată și tabul care descărca".
>
> **Adăugat 09.09.2026, seara — perimetrul de producție.** Prima felie care nu atinge nicio funcție
> a produsului: șase din cele opt puncte P0 ale lansării — conturile demo scoase din producție (fără
> `platform@`, vezi mai jos), CORS pe o singură origine, frână pe cele trei uși publice, sesiuni de
> 8 ore care se pot revoca (`V32`), Cloudinary setat, colector de erori, și CI care rulează suitele
> la fiecare push. 🔴 **Rămân deschise `platform@ecoregistru.ro` pe producție** (blocat pe o
> permisiune, comenzile sunt scrise) **și proba de restaurare a backupului.** Secțiunea
> „Perimetrul de producție".
>
> **Adăugat 09.09.2026, noaptea — partea a doua.** Cele șase de mai sus erau bifate **în repo, nu pe
> dyno**: commitul stătea nedeployat, deci producția rula încă cu CORS pe `*`, fără frână și cu
> tokenuri de 30 de zile. Deployat (api **v41**, app **v36**, `V32` migrat) și **probat pe
> producție**: origine străină fără antet CORS, preflight străin `403`, prima `429` la încercarea 61.
> **P0.7 e închis** — CI verde pe `main` și roșu la un test stricat dinadins. Baza n-avea **niciun
> backup logic**: capturat `b001` și pus program zilnic la 03:00. Secțiunea „Perimetrul de producție,
> partea a doua".
>
> *(Blocul de mai jos, până la linia despre jurnal, s-a scris pe 02.09.2026 și e păstrat pentru
> continuitate; cifrele lui sunt cele de atunci.)*
>
> **Livrat:** fundația · nomenclatorul LED (842 coduri) · motorul de evidență cu stoc cumulativ ·
> termene, alerte și dosar de control · modulul de generatori complet (G1–G8) cu cele patru
> documente oficiale — fișa de evidență (HG 856/2002), declarația anuală, Anexa 3 (HG 1061/2008) și
> Anexa 1 Ambalaje (Ordinul 794/2012, `.xls`) · cadențele AFM · transportatori cu șoferi.
>
> **Urmează:** modulul de depozit — ecrane `Reception`/`Delivery`, registru art. 48, borderou de
> achiziție la metale, profil de groapă (Etapele 8–11), plus TODO-ul Anexa 3 Ambalaje.
> 🔴 **Cere întâi modelul de registru art. 48** — vezi întrebarea AD la „Blocaje rămase".
>
> **Deschis:** **trei** întrebări către specialistă — 🔴 **AD** (n-avem niciun model de registru
> art. 48; blochează exportul din Etapa 8), **C** (cine se scrie la tratarea proprie) și **W**
> (generarea dedusă fără stoc). Din unsprezece vechi: patru închise pe corpus (**V**, **X**, **P**,
> **I**), trei pe textul actelor (**Q**, **Y**, **AA**), iar **AB** și **AC** au devenit alegeri de
> produs, decise. Blocajele de infrastructură sunt la finalul documentului.
>
> **Adăugat 07.09.2026 — interfața.** Ramura `ui-ux-modernizare` (31 de commituri, împinsă pe
> `origin`) duce cele șaisprezece puncte de UI/UX, șapte defecte găsite probând aplicația în
> browser, suita care le-a găsit (`frontend/e2e/`, **58 de verificări verzi**) și încă trei defecte
> găsite recitind ramura — de data asta în primitive, deci pe toate ecranele deodată — plus al
> patrulea, găsit de utilizator: căutarea cerea diacritice, deci `miscari` nu găsea nimic.
> **Backendul n-a fost atins**, deci cifrele de mai sus rămân valabile. ⚠️ **Nu e deployată:**
> merge-ul în `main` și push-ul pe repo-urile split se fac după ce interfața e privită cu ochiul.
>
> **Adăugat 07.09.2026 — materialul privat.** Ce ținea `.gitignore` afară nu era salvat nicăieri.
> E acum în `whoisonica/ecoregistru-docs`, repo **privat**. Vezi „Ce nu se commite" din
> `prompt-continuare.md`.
>
> **Adăugat 07.09.2026 — auditul de interfață.** O citire a frontendului ecran cu ecran a scos
> **zece defecte**, dintre care unul în backend și cel mai scump din toate: documentele oficiale se
> puteau tipări dintr-un cache rămas în urmă. Toate zece sunt reparate și probate — secțiunea
> „Auditul de interfață" de mai jos. **Testele sunt acum 228** (de la 224), migrările rămân la
> `V31`. Ce a ieșit din aceeași citire și **nu** e un defect — îmbunătățirile de UI/UX, pe ecrane —
> e în `docs/todo-ui-ux.md`, cu ordinea de atacat și cu lista lucrurilor care par greșite și sunt
> dinadins așa.
>
> **Adăugat 07.09.2026, noaptea — filtrul de lună, datele firmei, reactivarea.** Şase felii din
> `docs/todo-ui-ux.md`, în ordinea de acolo. Una repară ceva **rupt**: `<input type="month">` nu
> există în Safari şi Firefox, deci filtrul principal de pe Mişcări era câmp text liber pe Mac. Cu
> el, ecranul porneşte pe luna curentă (nu mai aduce toate mişcările firmei) şi capătă treapta „tot
> anul" — care a cerut şi backend, fiindcă `year` fără `month` se ignora în tăcere. Restul erau
> lucruri care **lipseau**: garda de la închiderea formularului de mişcare, datele firmei în
> „Setări" (în citire), plasa de sub excepţiile de randare, reactivarea a ce s-a dezactivat, şi
> formularul de firmă lărgit. **Testele sunt acum 239** (de la 230), migrările rămân la `V31`;
> suita de interfaţă e la **7 probe, 127 de verificări**. Detaliile şi motivele: secţiunea
> „Filtrul de lună, datele firmei şi dezactivarea care se poate lua înapoi". ⚠️ Tot nedeployat.
>
> **Adăugat 08.09.2026, după-amiaza — cinci felii de interfaţă, patru dintre ele afirmaţii
> corectate.** Provenienţa ambalajelor nu se mai cere unui generator pur (Anexa 3 Ambalaje n-o
> depune niciodată); grila de 66 de celule spune ce a salvat şi ce nu — şi, scriind starea, a ieşit
> o **pierdere tăcută de date** veche de la `V22`: ciorna rândului se ştergea întreagă la răspuns,
> deci ce se tasta cât zbura salvarea se pierdea; panoul numără **coduri cu stoc** în loc să adune
> kilograme peste coduri diferite; termenele spun câte zile mai sunt şi duc la documentul care le
> stinge, pe anul raportat; iar actul de identitate al şoferilor are, în sfârşit, o notă de
> retenţie, în amândouă locurile unde se tastează. **Backendul n-a fost atins** — 239 de teste,
> migrări tot până la `V31`. Suita de interfaţă e la **9 probe, 200 de verificări**. Detaliile şi
> motivele: secţiunea „Rubrica cerută cui i se aplică, grila care spune că a salvat, stocul pe
> coduri". ✅ **În producţie**: `ecoregistru-app` **v31**, `ecoregistru-api` neschimbat la v37,
> schema tot la 31.
>
> **Adăugat 08.09.2026, seara — propoziţia care lipsea, şi „1 linii".** Panoul spunea *starea* în
> cinci locuri şi lăsa clientul s-o sintetizeze; are de-acum o bandă care numeşte **un singur** lucru
> de făcut, ales după cât costă dacă rămâne nefăcut, cu drumul către el — şi care **tace** până vin
> toate sursele, ca să nu scrie „Eşti la zi" pe jumătate de răspuns. Antetul de pe Evidenţe a trecut
> de la cinci butoane la trei şi un meniu: exporturile generice scriu pe ele „rezumat neoficial", nu
> sunt acelaşi fel de lucru cu documentele care se depun. 🔴 **Şi, uitându-mă la captură, o greşeală
> de limbă veche pe toate ecranele:** „1 linii", „pe 1 mişcări" — româna cere trei forme, iar de la
> 20 în sus „de linii". `countOf` în `lib/utils`; backendul o reparase deja pe 06.09, la mail.
> **Backendul n-a fost atins** — migrări tot până la `V31`. Suita: **10 probe, 228 de verificări**.
> Detaliile: secţiunea „Propoziţia care lipsea de pe Panou". ✅ **În producţie**:
> `ecoregistru-app` **v32**, `ecoregistru-api` neschimbat la v37.
>
> **Adăugat 08.09.2026, seara târziu — sugestia care nu mai mută covorul, şi badge-ul care duce
> undeva.** Sugestia de duplicat de la Parteneri comuta pe tăcute la fişa existentă, aruncând tot ce
> completasei; acum se **spune** că s-a comutat, cu drum înapoi, iar pe un formular început se
> întreabă întâi. Badge-ul „Autorizaţie expirată" de pe o predare cerea, în chiar textul lui, să
> actualizezi fişa partenerului — şi nu ducea nicăieri; duce acum, prin `?partener=`, şi a ieşit din
> `Tooltip`, care nu poate înveli un link. 🔴 A treia tranşă de datorie de seed, iar prima ei
> variantă a **rupt proba 9** ducând stocul unui cod la zero. **Suita: 10 probe, 245 de verificări.**
> ✅ **În producţie**: `ecoregistru-app` **v33**. Detaliile: secţiunea „Sugestia care nu mai mută
> covorul".
>
> **Adăugat 09.09.2026 — numeralul până la capăt, şirurile din primitive şi seed-ul mutat acasă.**
> Restul listei din `docs/todo-ui-ux.md`: patru puncte, dintre care unul n-a fost construit, ci
> **mutat**. Optsprezece şiruri trec acum prin `withCount`, iar din felie a ieşit **convenţia**
> `{count}` (grup nominal acordat) vs `{n}` (cifră goală), care face regula găsibilă. 🔴 **Şi
> captura a scos al doilea fel de dezacord**, cel de după substantiv: „1 mişcare — cantitatea
> **lor** lipseşte", „1 fişier **n-au** urcat" — verbul şi pronumele se acordă şi ele. Şirurile
> hardcodate au ieşit din primitive (`combobox.tsx`, plus una găsită pe deasupra în `PackagingPage`);
> **primitivele n-au acum niciun literal românesc**. Seed-ul a treia tranşă a intrat în
> `DevDataSeeder` — 37 de mişcări, nu 36 —, iar mutarea a scos **o bombă cu ceas**: o dată fixă şi
> una relativă care trebuiau să rămână în aceeaşi ordine s-ar fi inversat pe 19.09.2026, stingând
> badge-ul singure. 🔒 Ştergerea datelor unui şofer a plecat de pe lista de ecrane pe lista de
> **întrebări** (**AO**): e o decizie, nu o felie. **Backendul a fost atins** (`DevDataSeeder`,
> `ApplicationBootIT`), dar nu schema — **239 de teste**, migrări tot până la `V31`. Suita:
> **11 probe, 273 de verificări**. Detaliile: secţiunea „Numeralul până la capăt". ✅ **În
> producţie**: `ecoregistru-api` **v38**, `ecoregistru-app` **v34**, schema tot la 31.
>
> **Adăugat 09.09.2026, după-amiaza — verdele fals de pe Panou, şi celelalte şapte.** Nu o felie de
> funcţii, ci **o probă cap-coadă** peste tot ce s-a livrat în 07–09.09, cu suita şi testele deja
> verzi. Au ieşit **opt defecte**, niciunul găsit de compilator. 🔴 Cel mai scump: Panoul citea
> numai `isLoading`, deci o cerere **căzută** arăta ca un răspuns gol şi ecranul scria „Eşti la zi"
> peste ea, fără niciun mesaj de eroare — probat pe firma demo, cu 9 termene depăşite şi o linie fără
> cod R/D, cu sursele răspunzând 500. Există acum a treia stare, „nu ştiu". Cauza lui în producţie:
> `PLATFORM_ADMIN` fără firmă aleasă cerea date fără `X-Tenant-Id` şi primea opt `400` la fiecare
> autentificare; ecranele de firmă stau acum închise până se alege una. Restul: „expiră în **0 de
> zile**" şi „**0 de linii**" — aceeaşi gardă de zero pe care feliile de dimineaţă o puseseră pe
> ecranele vecine şi nu pe astea două; „Adaugă cantitatea" rupt pe două rânduri, **al treilea caz**
> al aceluiaşi defect, reparat de data asta în primitivă; `/actuator/health` care întorcea `DOWN`
> fiindcă indicatorul de mail deschidea un SMTP real la fiecare sondă; şi lipsa unui favicon, care
> făcea **proba 6 să cadă** de fiecare dată. Al optulea: „Eşti la zi" pe un cont pe care nu s-a scris
> încă nimic — îl lăsasem ca **decizie**, dar cei doi paşi erau deja scrişi în cod ca dependenţe
> (fără punct de lucru nu se poate înregistra nimic, fără mişcări nu e nimic de raportat), deci n-avea
> ce ghici. ⚠️ **De trei ori, acelaşi defect la trei adâncimi** — bandă, casetă, liste —, şi de
> fiecare dată văzut pe captura **reparaţiei**, nu a feliei: regula 5 se aplică şi reparaţiilor.
> **Backendul atins numai la configuraţie** — 239 de teste, schema tot la `V31`; plus prima probă a
> seeder-ului **pe bază curată** (31 de migrări, 37 de mişcări, toate patru stările, expirarea fixă).
> Suita: **11 probe, 273 de verificări, toate verzi**. Detaliile şi motivele: secţiunea „Verdele fals
> de pe Panou".
>
> *Jurnalul de mai jos e cronologic și **nu se rescrie**: o intrare descrie ce era adevărat în ziua
> ei. Când o cifră din el diferă de blocul ăsta, blocul ăsta are dreptate.*

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

> ✅ **Corectat pe 02.09.2026 — sunt două firme, nu una.** Vezi secțiunea „Corpusul, recitit integral"
> de la finalul jurnalului: `deseuri generate_Cluj_2025_Iuhos Lorena.pdf` e al firmei **Panemar Jr.**
> (CAEN 1071, brutărie), nu al lui Hamburger. Restul paragrafului de mai sus rămâne adevărat, iar
> observația despre CAEN tot nu spune nimic — dar „o singură practică" nu mai e exact: **contrastul
> dintre un reciclator și o brutărie** e chiar ce a închis întrebarea V.

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

> ✅ **Livrată pe 06.09.2026** (`V31`). Ce urmează mai jos e analiza din 25.08 care a stat la baza
> ei, păstrată fiindcă fiecare rând s-a dovedit corect — inclusiv bănuiala că împărțirea
> reciclat/valorificat se citește din codul R. Ce a adăugat construcția: proveniența e **sub-rând
> sub fiecare material**, nu o coloană; documentul e **unul per punct de lucru**, nu o pagină per
> punct de lucru; și care tabel se aplică vine din **profilul firmei**. Deciziile 42–46 din
> `prompt-continuare.md`.

**Neînceput** *(la data scrierii)*. Structura reală, din act, nu din `.ods`-ul primit:

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

## Anexa 3, redesenată 1 la 1 după modelul Hamburger (02.09.2026)

**Cererea:** „scoate din antet «Exemplarul 2 din 3 — destinatar (colector)» [...] cumva să fie 1 la 1
cu ce vezi tu aici" — cu modelul ștampilat de la Hamburger Recycling Romania alături
(`documente oficiale/anexa 3 hamburger reciclying.pdf`, seria HRR-BH, două formulare).

Am pus modelul lângă ce tipăream și am mers rubrică cu rubrică. Fiecare diferență găsită era **ceva
pus de noi în plus**, nu ceva lipsă — util de știut, fiindcă antetul era locul unde adăugasem cel mai
mult.

### Antetul: trei rânduri devenite unul

Tipăream un titlu mare („FORMULAR DE ÎNCĂRCARE-DESCĂRCARE DEŞEURI NEPERICULOASE"), un subtitlu cu
temeiul legal („Anexa 3 la HG 1061/2008") și o casetă cu chenar în care stăteau seria și eticheta
exemplarului. Modelul are **un singur rând, fără chenar**: `ANEXA 3` în stânga, iar în dreapta
`Serie şi număr: HRR-BH 20 Nr: 169924 / 12 . 08 . 2026`. Atât. Numele actului nu se pierde: îl spune
al doilea rând de subsol, exact ca pe model.

### Exemplarele nu se mai numesc

Scriam „Exemplarul 1 din 3 — expeditor (generator)" ș.a.m.d. în antetul fiecărei pagini. **Niciun
model n-are așa ceva**, și motivul e fizic: pe hârtie cele trei exemplare sunt un carnet cu indigo —
aceeași filă de trei ori, sortată după semnare, nu înainte.

Cele **trei pagini rămân** (HG 1061/2008 art. 20 alin. (2) cere trei exemplare, iar specialista le-a
numit părțile pe 24.08). Ce s-a mutat e doar eticheta: cine ia care exemplar scrie acum în hintul de
pe ecran, unde nu costă nimic, în loc să fie tipărit pe un formular oficial.

### Restul diferențelor, rubrică cu rubrică

| Ce | Aveam | Modelul, deci acum |
|---|---|---|
| Prima casetă a fiecărei coloane | conținut amestecat cu titlul (`Data\nÎncărcare`) | **doar titlul coloanei** — așa desenează hârtia rândul de sus |
| Delegatul | trei valori una sub alta | etichetele modelului: `Nume si prenume:` și `Nr.inmatr.mij.trans:` |
| Codul deșeului | `cod 20 01 01` | `Cod: 20 01 01` |
| Bifele „Destinat:" | `Colectării   [X]` | `colectării   \|X\|` — notația și minusculele modelului |
| „Descriere" și „Destinat:" | casete separate | aceeași casetă, bifele în următoarea |
| Rubrica punctului de lucru | `Date privind punctul de lucru *) unde se efectuează` | `Date privind punctul de lucru unde se efectuează *)`, și **repetată** a doua oară între cele două blocuri de semnătură, ca pe model |
| Expirarea autorizației | `Data la care expiră autorizaţia` | `... autorizaţia de mediu` |
| DESCĂRCAREA | nume, CUI, apoi locul descărcării | **locul descărcării întâi**, apoi `Date de identificare destinatar` cu sediul social — două adrese diferite: unde a ajuns camionul, apoi a cui e |
| Coloana 6 | `Obs` | `Observaţii` |
| Subsolul | doar nota cu asterisc | plus `Publicat în Monitorul Oficial cu numărul 672 din data de 30 septembrie 2008` |
| Înălțimea tabelului | ~60% din pagină | **umple pagina**: liniile coloanelor coboară până jos, iar ultima rubrică a fiecărei coloane se termină într-o casetă goală — acolo se semnează și se scrie de mână cantitatea |

### Ce **nu** s-a luat din model, și de ce

- **Datele de identificare ale transportatorului rămân complete** (nume, adresă, CUI, Reg. Com.).
  Modelul scrie doar numele fiindcă e carnetul lui Hamburger, pre-tipărit cu datele lui în altă
  parte; rubrica se cheamă „Date de identificare", iar a scoate CUI-ul de pe un document de transport
  ar fi înrăutățit formularul, nu l-ar fi apropiat de model.
- **Codul se scrie cu spații** (`15 01 01`), nu cu puncte ca la ei: așa îl scrie Lista Europeană.
- **Unitatea rămâne alegerea firmei** (`kg` / `tone`, `mc`) — decizia 16, nemodificată. Modelul e în
  tone fiindcă așa a ales Hamburger.
- „Semnătura şi **stampila**", fără diacritică, e greșeala lor de tastare; noi scriem „ştampila".

### Verificat pe hârtie

PDF-ul randat și comparat vizual cu modelul, la 120 dpi, pagină lângă pagină. Testul
`theFormPrintsThreeIdenticalCopies` ține de-acum trei lucruri: trei pagini, niciuna cu cuvântul
„Exemplarul", și **pagina 1 identică cu pagina 3** — dacă mâine cineva pune la loc o etichetă pe
exemplare, testul cade.

⚠️ Asertările din testul ăsta sunt pe text **ASCII**: `PdfTextExtractor` decodează pagina Cp1250
înapoi prin Latin-1, deci „ă" iese „ª" la citire, deși pe pagina tipărită e corect. Nu e nimic de
reparat în formular — e și motivul pentru care asertările vechi erau tot ASCII.

**175 de teste verzi.**

---

## Operațiunea s-a mutat sub transport, iar la generator rămâne „Generare" (02.09.2026)

**Cererea, din conversația cu specialista de pe 25.08.2026**, cuvânt cu cuvânt:

> „Dar asta nu trebuie pusă aici, ci în alt tab. Că dacă alegi Valorificare rămân necompletate
> restul. [...] Aici, la operațiune, aici trebuie să rămână Generator. Și după, mai jos, trebuie pus
> în tab cu Valorificare/Eliminare, unde poți să le selectezi. [...] După ce alegi la Transport spre
> Valorificare să apară următoarele taburi cu codurile de valorificare. Sau cu codurile de eliminare.
> În funcție de cum o să fie transportul."

Și, în aceeași conversație, ce **nu** se face: *„nu o să-și pună stocuri generatorii [...] doar la
colectori trebuie"* — deci nicio validare de stoc la ieșire. Rămâne cum era.

### Ce s-a schimbat pe ecran

Formularul de mișcare are de-acum două jumătăți, în ordinea în care se întâmplă lucrurile:

1. **Sus, „Operațiune" = de unde vine deșeul.** Pe un cont de generator, select-ul are o singură
   opțiune, „Generare", cu o linie sub el care spune unde se alege restul. Un cont care poate prelua
   de la terți păstrează și `Preluare`, și ieșirea directă.
2. **Sub transport, „Ce se întâmplă cu deșeul"** — trei opțiuni, fiecare cu efectul ei scris dedesubt,
   ca la blocul de proveniență: *Rămâne în stoc* (implicit) · *Transport spre valorificare* ·
   *Transport spre eliminare*. Codul R/D apare acolo, în aceeași casetă, și numai familia potrivită.

Anexa 3 apare mai departe după aceleași reguli (ieșire + partener ales, decizia 25); ce s-a schimbat
e că acum se ajunge la ea pe drumul pe care îl descrie ea: transport → ce se întâmplă → destinatar.

### De ce o singură mișcare, și nu două

„Generare + transport spre valorificare" pleacă pe server ca **o** mișcare `RECOVERED` cu cod R.
N-a fost nevoie de nimic nou în model, fiindcă motorul face deja jumătatea cealaltă: **generarea se
deduce din ieșire** (decizia 17, `V24`), tocmai din observația ei — *„cum poți să valorifici ceva ce
nu este generat?"*. Deci aceeași mișcare iese pe fișă **generat 100 · valorificat 100 · stoc 0**,
exact ca proba de pe producție din 25.08. Zero migrări, zero schimbări de backend.

Varianta cu două mișcări legate (o intrare `GENERATED` + o ieșire) ar fi cerut o migrare, ar fi
atins editarea, ștergerea și registrul de predări, și ar fi dublat rândurile din listă — pentru
aceleași cifre pe formular.

### Nodul: ce se întâmplă la redeschiderea unei mișcări vechi

O ieșire salvată înainte se citește înapoi în cele două jumătăți, dar **nu la fel pentru toate**:

| Mișcarea în bază | Se redeschide ca | De ce |
|---|---|---|
| `RECOVERED`/`DISPOSED`, registru **Anexa 1** | Generare + „spre valorificare/eliminare" | E deșeul firmei; motorul îi deducea oricum generarea |
| `RECOVERED`/`DISPOSED`, registru **art. 48** | Ieșire directă, ca până acum | Marfa preluată **nu** e generată de noi; a o rescrie ca generare i-ar muta tăcut cantitatea pe alt formular |
| `UNCLASSIFIED_OUT` (linie veche fără cod) | Ieșire neclasificată, cu roșu | Se completează alegând mai jos ce s-a întâmplat — exact drumul nou |

Din tabelul ăsta iese și regula pentru **proveniență** (decizia 23): întrebarea „de unde vine deșeul"
se pune numai la ieșirea directă, unde e ambiguă. Când ieșirea vine din blocul de sub transport,
răspunsul e deja dat sus — „Generare" înseamnă chiar deșeul firmei — deci se trimite `ANEXA_1`
explicit, fiindcă backendul cere registrul la orice ieșire de pe un cont care ține și art. 48.
Nu se ghicește nimic; e chiar clicul omului.

### Ce **era** stricat, găsit în drum

Blocul de radio-uri „Proveniența deșeului" și hintul de la `Preluare` fuseseră lipite, în `a50bb17`
(25.08), **înăuntrul `<select>`-ului de operațiune**. Browserul aruncă din `<select>` orice nu e
`<option>`, deci întrebarea nu se vedea niciodată — iar validarea o cerea. Pe un cont de colector,
**nicio valorificare nu se putea salva**: mesajul „Alege proveniența" apărea pentru un câmp care nu
exista pe ecran. Nu era vizibil în teste, fiindcă e o regulă de HTML, nu de TypeScript.

### Verificat în aplicație, nu doar la compilare

Dev server local, cu proxy-ul întors spre API-ul de producție, pe `Demo Reciclare SRL` (cont care
ține și art. 48). Nimic salvat — doar parcurs:

1. **Mișcare nouă, Operațiune = Generare** → blocul „Ce se întâmplă cu deșeul" apare imediat sub
   „Transport — mijlocul / destinația", cu *Rămâne în stoc* bifat.
2. **Transport spre valorificare** → apare „Cod operațiune (R/D) *", cu **doar R1–R13** în listă.
   Proveniența **nu** se întreabă: răspunsul e deja dat sus.
3. **Operațiune = Valorificare** (ieșirea directă) → radio-urile „Proveniența deșeului" **se văd**
   — ăsta e defectul reparat — iar blocul de jos rămâne doar cu codul.
4. **Editez `15 01 02` Valorificare(R3) din 16.07** → se redeschide ca **Generare** +
   *Transport spre valorificare*, cu **R3** preselectat. Dus-întors curat.
5. **Editez o linie veche „Fără cod R/D"** → „Ieșire neclasificată" cu hintul roșu sus, iar blocul
   de jos e chiar drumul prin care se completează.

Rămân neprobate live două lucruri, din lipsă de date: select-ul cu **o singură opțiune** pe un cont
de generator pur (tenantul demo e `BOTH`) și redeschiderea unei ieșiri pe **art. 48** (demo n-are
niciuna) — ambele sunt o ramură de-o linie, dar merită văzute pe contul specialistei.

### Deployat și verificat pe producție (02.09.2026, ora 13:50)

`ecoregistru-app` la **v26** (`0182ef4`); backendul n-a fost atins, deci `ecoregistru-api` rămâne
unde era. Proba, pe **contul specialistei** (`Ecodoc SRL`, generator pur) — nimic salvat:

- „Operațiune" are **o singură opțiune, „Generare"**, cu hintul care spune unde se alege restul.
  Ăsta e cazul pe care tenantul demo nu-l putea arăta.
- *Transport spre valorificare* → „Cod operațiune (R/D)" cu R1–R13; partener `Hamburger Recycling`
  → **Anexa 3 apare**, sub el.
- Mișcarea ei reală din 25.08 (`15 01 03`, Valorificare R3, 1000 kg) se **redeschide** ca
  Generare + *Transport spre valorificare*, cu R3 preselectat.

### Stare

Frontend curat: `tsc -b` și `vite build` trec. Backendul nu s-a atins — contractul era deja bun
(`resolveRegister` acceptă `ANEXA_1` pe o ieșire), deci cele **175 de teste** rămân cum erau.

---

## Corpusul, recitit integral — patru întrebări închise, zero cod de schimbat (02.09.2026)

Toate cele 20 de fișiere din `documente oficiale/` citite rubrică cu rubrică, nu pe sărite: 13
fișiere de evidență (409 rânduri de lună în cap. 2, 382 în cap. 1, 610 în cap. 3/4), cele două
declarații de ambalaje, șablonul Anexei 3 Ambalaje și cele două modele de Anexa 3. Trei formate care
nu se citiseră până acum — `.xls` (Oradea, prin `xlrd`), `.ods` (dezarhivat, `content.xml`) și
`.docx`.

### Întâi: corpusul e al **a două** firme

`deseuri generate_Cluj_2025_Iuhos Lorena.pdf` **nu e Hamburger** — e **Panemar Jr.**, CUI
RO 17022001, **CAEN 1071 (brutărie)**, `office1@panemar.ro`, întocmit de altcineva. Documentul e
real, nu ieșit din aplicația noastră (producer `iLovePDF`, iulie 2025). Restul corpusului rămâne
Hamburger Recycling Romania, inclusiv fișierele Oradea și modelul „Anexa 3 CARTON".

Contează, fiindcă **una tratează deșeul și cealaltă doar îl predă** — iar diferența dintre ele
răspunde la o întrebare pe care o singură firmă n-o putea răspunde.

### Ce s-a închis

| Întrebare | Răspunsul din corpus |
|---|---|
| **V** — „Tratare: Cant." la un client care doar predă | **0.** Panemar scrie `0.000` cu Modul `-` pe toate cele 5 coduri; Hamburger scrie cantitatea, cu Modul `TM`, fiindcă chiar balotează. Regula e „ce a tratat firma însăși", citită din Modul. Din 409 rânduri: 287 cu Tratare = Stocare, 60 goale, 50 cu Stocare 0 și Tratare > 0 — **niciunul invers**. |
| **X** — sunt „Birouri" și „Producție" secțiile potrivite | **Da.** `birouri` 336 · `productie` (trei ortografii) 56 · **`birouri+productie` 15** · `personal` 2. Cele două implicite acoperă 407 din 409, iar tipărirea amândurora când nu s-a ales una **există deja în corpus**, scrisă combinat într-o celulă. |
| **P** — unde stă titlul | **Doar pe centralizator** (`D12`/`A12`); varianta Bragadiru scrie „CENTRALIZATOR". Pe cele 33 de foi per cod de deșeu, antetul începe direct cu „Agentul economic:" — niciun titlu, în niciun fișier. **✅ Implementat** — vezi mai jos. |
| **I** — cine dă numărul de înregistrare | **Autoritatea, la depunere.** Hamburger Cluj 2024 → `Nr inreg: 23/11.02.2025`; Panemar 2025 → `Nr inreg: 25/11.02.2025`. Două firme diferite, numere apropiate, **aceeași zi**, aceeași agenție județeană, cu o lună înainte de 15 martie. E registrul de intrare al agenției. |

### Ce s-a nuanțat

- **AC** — declarația HRR 2021 raportează **numai Oțel, 5.192 kg**, cifră absentă din toate fișele
  lor de gestiune; invers, `15 01 02` din fișe (5,5 kg) lipsește din declarație, iar **Tabelul 2 e
  complet gol**. Cele două documente raportează mulțimi disjuncte. Notele 1) și 4) explică de ce:
  Tabelul 1 e despre marfă pusă pe piață, nu despre deșeu. Deci bifa din `V27` e justificată — dar
  premisa lui `V26`, că *ambele* tabele se însumează din mișcări, ține **doar pentru Tabelul 2**.
- **AA** — „Proveniența" nu e un câmp, ci **o axă a tabelului**: fiecare material se sparge în trei
  sub-rânduri fixe (`populatie`, `colectori`, `generatori persoane juridice`), fără „comerciant".
  Șablonul e însă cel modificat local, deci pentru numărul valorilor rămâne actul autoritatea.
- **W** — cele 7 luni din corpus cu ieșire > generat sunt **toate** absorbite de stocul reportat.
  Timișoara 2022 `19 12 12`: 48.755 + 200.008 − 225.430 = **23.333**, exact formula noastră.
  Confirmă precedența din `V24`; cazul fără stoc nu apare, deci întrebarea rămâne.
- **C** — în 610 rânduri de cap. 3/4 apar 18 operatori, **niciodată firma raportoare**. Nu există
  niciun exemplu de tratare proprie, deci corpusul nu spune ce se scrie acolo.

### Auditul de cod: cinci verificări, zero defecte

Fișierele Oradea au o foaie `simboluri` — **legenda celor cinci nomenclatoare, notele 1–5 din
HG 856/2002**. Deci le avem din două surse independente: actul și un fișier de lucru al
specialistei. Comparate valoare cu valoare cu enum-urile din cod:

| Nomenclator | Act | Cod |
|---|---|---|
| `StorageType` | RM RP BZ CT CF S PD VN VA RL A | identic |
| `TreatmentMethod` | TM TC TMC TB TT D A | identic |
| `TransportMeans` | AS AN H CF A | identic |
| `WasteDestination` | DO HP HC I Vr P Ve A | identic |
| `TreatmentPurpose` | V E | **doar `V`** — abatere deliberată, vezi mai jos |

Restul verificărilor, toate curate: cele două coloane „Cant." din cap. 2 (`treatedHere` = ieșirile
fără partener — exact regula pe care o arată contrastul Panemar/Hamburger), rândurile per operator
din Tabelul 2 (`PackagingDeclarationBuilder`, cu denumire + adresă + CUI, cum cer rubricile `D7`/`E7`),
rândurile de subtotal „Total plastic" / „Total metal" / „TOTAL:", și secțiile implicite din `V25`.

**`TreatmentPurpose` fără `E` nu mai e o presupunere, e o constatare.** Cele 11 rânduri cu `E` din
corpus sunt toate într-o singură foaie (Cluj 2022, `19 12 12`) și sunt **greșite în fișierul lor**:
cap. 1 al aceleiași foi arată cantitatea la *valorificat* (6 · 20,48 · 4,42 · 9,26 t), cu *eliminat*
zero pe toate lunile. Au scris „în vederea eliminării" peste o valorificare. Fișierele Oradea, care
**chiar elimină** (`20 03 01` → `D1`, prin RER Ecologic Service), lasă celula **goală**. Numărătoarea
completă: `V` 342 · liniuță 188 · gol 25 · `E` 11.

### Ce s-a schimbat în cod: titlul iese de pe fișă (**P**)

Trei din cele patru întrebări închise confirmau comportamentul existent. A patra nu: tipăream
„Evidența gestiunii deșeurilor generate «an»" în capul **fiecărei** fișe per cod de deșeu, iar
**niciunul** din cele 33 de modele nu face asta — ele încep direct la „Agentul economic:", iar
titlul stă pe centralizator, unde `AnnualDeclarationGenerator` îl tipărea deja.

Motivul pentru care deviasem era scris în javadoc și **nu mai stă în picioare**: „un PDF de 20 de
pagini fără titlu e greu de dat unui inspector" — dar dosarul de control numește deja fișierul
`evidenta-gestiunii-deseurilor-{an}.pdf` și îl descrie în README-ul lui. Deci argumentul nu cumpăra
nimic și costa o abatere de la fiecare model pe care îl avem.

- `Anexa1FormGenerator.addSheet` nu mai adaugă titlul; metoda `documentTitle()` a fost ștearsă.
- Testul **n-a fost șters, ci inversat** (`thePerCodeSheetCarriesNoDocumentTitle`): aceeași regulă,
  întoarsă în direcția bună — pagina **nu** conține titlul, dar conține „Agentul economic:" și anul.
- **Verificat pe hârtie**, nu doar la teste: PDF randat și privit. Pagina se deschide la „Agentul
  economic:", fără spațiu rămas, cele patru capitole intacte, nota cu legenda la subsol, iar
  antetele cap. 3/4 tipăresc **OUG 92/2021**.

Nicio migrare. **175 de teste verzi.**

### Ce nu s-a schimbat, și de ce

Restul auditului n-a găsit nimic de reparat — nomenclatoarele, cele două coloane „Cant.", rândurile
per operator din Tabelul 2, subtotalurile, secțiile. **Și antetul cap. 3/4 a încetat să fie o
întrebare:** modelele trimit la Legea 211/2011, abrogată de OUG 92/2021, dar un act abrogat nu
devine autoritate fiindcă apare într-un șablon vechi. Regula, de aplicat la tot corpusul: **ce e
depășit în model nu se copiază** — corpusul spune cum se completează un formular, nu ce lege e în
vigoare.

Detaliile verbatim, cu citatele din note: `surse-oficiale.md` §1.2, §1.3 (punctele 4 și 6–10) și §5.3.

### Trei întrebări închise pe textul actelor, nu pe practică (02.09.2026)

Reverificare direct pe **Portalul Legislativ**, fiindcă o parte din citate veneau de la gazde
secundare (PDF Lege5 găzduit de FEPRA). Textul e identic; sursa e acum primă.

| Întrebare | Ce spune actul |
|---|---|
| **Q** — 15 martie, una sau două depuneri | **Una.** HG 856/2002 (consolidare 19.03.2007) nu impune niciun termen: cere să **ții** evidența (art. 1) și să o dai **la cerere** (art. 2 alin. (2), art. 3). Singura depunere cu dată fixă e OUG 92/2021 art. 48 alin. (1). |
| **Y** — împreună sau separat | **Nu pot fi împreună.** Notificarea la AFM pe 25 ianuarie (art. 3), raportarea la agenția județeană pe 25 februarie (art. 1 + art. 6). Alt destinatar, altă lună. |
| **AA** — valorile „Provenienței" | **Patru**, verbatim din anexa 3 nota 2: „populație", „generator persoană juridică", „colector", „comerciant". Șablonul primit, cu trei sub-rânduri, e modificat local. |

Pe deasupra, două reclasificări: **AB** nu mai e întrebare pentru specialistă (art. 1 alin. (1) a
răspuns — anexa 1 o depun doar cei cu obiective individuale; rămâne alegerea noastră dacă întrebăm
asta în cererea de cont), iar la **AC** criteriul e în act (notele 1 și 4 + art. 8 alin. (2):
ambalaj pus pe piața națională, exportul exclus) — deschisă rămâne doar forma, nu regula.

**Rămân două: C și W.** Forma lui **AC** a fost decisă de utilizator în aceeași zi — *„la AC lasă
momentan bifa"* — iar bifa e confirmată pe producție: `V27` aplicată, coloana `packaging_on_market`
prezentă pe `waste_movements`. Zero cod schimbat: toate confirmă ce face aplicația.

---

### Commis, pushat și deployat (02.09.2026, ora 16:43)

| Unde | Ce | Stare |
|---|---|---|
| `origin/main` și `origin/deploy/heroku-split` | `54ec88d` | ✅ |
| `newrepo/main` | `889f6a7` | ✅ `ecoregistru-api` la **v32**, pornire 9,66 s, fără migrare nouă |
| `ferepo/main` | `0182ef4`, neschimbat | ✅ frontendul n-a fost atins — `tmp-frontend` a ieșit identic cu `ferepo/main`, deci n-a fost nimic de trimis. E cazul normal, nu o eroare |

---

## Reparaţiile auditului care nu depindeau de nimeni (02.09.2026)

Şase din cele 14 puncte s-au închis în aceeaşi zi — toate cele la care **actul răspunde singur**,
deci nu era nimic de întrebat. **177 de teste verzi** (de la 175). Migrări: **niciuna** — totul e
afişare, format de fişier sau text.

| Pct. | Ce s-a schimbat | Unde |
|---|---|---|
| 1 | Cap. 4 al fişei trimite la **anexa nr. 7**, nu nr. 2 | `Anexa1FormGenerator` |
| 2 | Codurile periculoase se tipăresc **cu asterisc**, pe fişă şi pe declaraţia anuală | `WasteCodeLabel` (nou), `Anexa1Sheet`, `AnnualDeclaration.Row` |
| 4 | Ambalajele se descarcă **`.xls` real** (BIFF8), nu `.xlsx` | `PackagingDeclarationXlsGenerator` (redenumit), `ExportFormat.XLS` |
| 11 | Mesajul dosarului: 5 ani, iar art. 48(5) e prag, nu plafon | `ErrorMessageEnum` |
| 12 | Nota 2 a fişei: `D` înaintea lui `TT`, ca în act | `Anexa1FormGenerator` |
| 13 | `ReportType` din frontend are toate cele cinci valori | `types.ts` |

**Cum s-a pus asteriscul, şi unde nu.** `WasteCodeLabel.official(cod, periculos)` e singurul loc
care ştie regula, e idempotent şi lasă un cod gol neatins. Denumirile **nu** se ating: actul scrie
referinţele încrucişate din ele curat, iar seed-ul le are verbatim din EUR-Lex. Anexa 3 n-a avut
nevoie de nimic — refuză oricum codurile periculoase (`ANEXA3_HAZARDOUS_NOT_ALLOWED`), deci n-are
cum să tipărească vreunul.

⚠️ **Coliziunea celor două `(*)` a rămas, deliberat.** Pe declaraţia anuală, codul poartă acum `*`
iar stocul poate purta `(*)` cu alt înţeles. Sunt în coloane diferite şi al doilea e în paranteze,
iar nota de subsol a fost rescrisă ca să spună explicit care e care şi să numească şi celălalt
înţeles. **Nu s-a schimbat al doilea marcaj** — asta e întrebarea **AE**, şi n-are rost ghicit.

**Două teste noi în `Anexa1FormIT`:** `chaptersThreeAndFourCiteTheAnnexesInForce` (cere anexele 3 şi
7, respinge „anexei nr. 2" şi „211/2011"; normalizează spaţiile, fiindcă antetul se rupe pe rânduri
în coloana îngustă) şi `aHazardousCodePrintsItsAsteriskAndAPlainOneDoesNot` — a doua jumătate a
numelui e partea care contează. `PackagingDeclarationIT` citeşte acum fişierul cu `HSSFWorkbook`,
deci o întoarcere la OOXML cade la parsare, nu trei rubrici mai jos.

✅ **Verificat pe documentele randate, nu doar la teste** (regula 5): fişa scrie
`Cod deşeu: 16 06 01*` şi încape în continuare pe o pagină · cap. 3 „conform anexei nr. 3", cap. 4
„conform anexei nr. 7" · declaraţia anuală scrie `16 06 01*` şi `13 02 08*`, iar `20 01 01` rămâne
curat · fişierul de ambalaje începe cu `d0 cf 11 e0 a1 b1 1a e1` — OLE2/BIFF8, `.xls` adevărat — şi
se serveşte cu `application/vnd.ms-excel`, sub numele `anexa1-ambalaje-2026.xls`.

**Un lucru prins de compilator, care merită ţinut minte:** `ExportFormat` e folosit într-un `switch`
exhaustiv în `GenericEvidenceExporter`, deci adăugarea lui `XLS` a rupt compilarea până i s-a dat un
răspuns explicit. Exportul generic e un rezumat **neoficial**, pe care niciun act nu-l constrânge,
deci rămâne pe `.xlsx` şi întoarce 400 dacă i se cere `.xls` — nu tăcere şi nici alt format decât
cel cerut.

### Commis, pushat şi deployat (04.09.2026, ora 01:25)

Commit `dee10f4` pe `main`, split-uri `08089bb` (backend) şi `274cabd` (frontend), procedura
obişnuită, fără `--force` şi fără conflicte. **`ecoregistru-api` la v33** (`08089bb`), pornit în
8,1 s, `Current version of schema "public": 28` — **nicio migrare, cum trebuia.**

✅ **Proba pe producţie, pe documentele descărcate de pe dyno**, nu pe cele locale:

| Ce | Rezultat |
|---|---|
| `anexa1-ambalaje-2026.xls` | `Content-Type: application/vnd.ms-excel`, magic `d0 cf 11 e0 a1 b1 1a e1` — **BIFF8 real** |
| Fişa, 8 pagini | `13 02 08*` şi `16 06 01*` **cu** asterisc; `15 01 01`, `15 01 02`, `15 01 07`, `20 01 01`, `20 01 40`, `20 03 01` **fără** |
| Fişa, cap. 3 şi 4 | „conform anexei nr. 3" ✅ · „conform anexei nr. **7**" ✅ · „anexei nr. 2" **absent** ✅ |
| Declaraţia anuală, 3 pagini | aceleaşi două coduri cu asterisc, restul curate |

⚠️ **Am apăsat „Regenerează" pe tenantul demo de pe producţie** ca să am ce descărca (96 de linii de
evidenţă, 2026). E date derivate din mişcări şi idempotent — dosarul de control o face oricum înainte
să împacheteze — şi e tenantul de test, nu un client. Dar e o scriere în baza de producţie, deci se
scrie aici.

### Tranziţia de cache, prinsă la timp (04.09.2026)

Schimbarea formatului a lăsat o gaură de un deploy, găsită **după** push: un browser care încă
rulează bundle-ul vechi cere `format=xlsx`, iar cererea cădea pe ramura PDF. Rezultatul era un
**PDF numit `.xlsx`, cu content type OOXML** — un fişier stricat, dintr-un buton care arăta bine.

Endpointul are un singur fişier de calcul de dat, cel pe care îl numeşte art. 6, deci acum
`xlsx` e primit şi **răspuns cu `.xls`**, cu numele şi content type-ul corecte. Tranziţia ţine un
singur deploy, dar un fişier greşit e mai rău decât unul vechi — şi nu cere nimănui să dea
hard-refresh.

Testul `aStaleClientAskingForXlsxStillGetsTheXls` verifică toate trei: content type-ul, numele care
se termină în `.xls` şi nu în `.xlsx`, şi primii patru octeţi `d0 cf 11 e0` — ca să nu treacă nici un
PDF deghizat. **178 de teste verzi.** Commit `d38d4b8`, split `7b0124e`.

✅ **Verificat pe producţie la 01:35**, cerând endpointului exact ce ar cere un client vechi
(`format=xlsx`): răspunde `Content-Type: application/vnd.ms-excel`, nume
`anexa1-ambalaje-2026.xls`, magic `d0 cf 11 e0` — deci `.xls` real, nu PDF-ul de dinainte.

**Releaseuri:** `ecoregistru-api` **v34** (`7b0124e`) · `ecoregistru-app` **v27** (`274cabd`).
⚠️ Build-ul de frontend a stat `pending` **zece minute** în coada Heroku, deşi cel de backend din
acelaşi push a trecut în 12 secunde. A intrat singur. Nu e o eroare de-a noastră şi nu e nimic de
reparat — dar merită ştiut, ca să nu creadă cineva că push-ul n-a plecat şi să forţeze un al doilea.

---

## Audit de conformitate legală — 14 puncte, două abateri confirmate (02.09.2026)

Documentul complet, cu temeiul fiecărui punct: **`docs/audit-conformitate.md`** (gitignored — enumeră
abateri nereparate într-un repo public; după ce se repară, concluziile intră aici ca istoric).
Întrebările ieşite din el: **`docs/intrebari-specialist.md`**, literele **AE–AN**.

**Metoda, şi de ce contează.** Până acum documentarea a mers într-o direcţie: *citim actul, apoi
construim*. Auditul a mers invers — *citim ce tipăreşte aplicaţia, apoi căutăm rubrica în act*. E
altă operaţie şi găseşte alte lucruri: cele două abateri de mai jos au trecut prin toate rundele de
documentare tocmai fiindcă nimeni nu s-a uitat la ele **dinspre hârtie înapoi spre lege**.

Baza: 175 de teste verzi la momentul auditului, arbore curat la `4193a2c`. **Niciunul dintre cele 14
puncte nu e prins de un test** — testele verifică ce am scris noi, nu ce cere actul.

### Cele două abateri confirmate

**1. Cap. 4 al fişei trimitea la anexa greşită.** `Anexa1FormGenerator` tipărea „Operaţia de
eliminare, conform **anexei nr. 2** din OUG 92/2021". Anexa nr. 2 e „EXEMPLE de instrumente
economice"; operaţiunile D1–D15 sunt în **anexa nr. 7**. Cap. 3 (anexa nr. 3, valorificare) era
corect, şi de aici a venit şi greşeala: javadoc-ul presupunea că OUG 92/2021 a păstrat numerotarea
din Legea 211/2011, unde eliminarea era anexa 2. A păstrat-o doar pentru valorificare.

Verificat pe textul din Monitorul Oficial (p. 58 şi 69) şi confirmat a doua oară de definiţiile din
anexa nr. 1 — pct. 17 („Anexa nr. 7 stabileşte o listă a operaţiunilor de eliminare") şi pct. 37
(„Anexa nr. 3 [...] operaţiunilor de valorificare"). Tabelul complet al anexelor e în
`surse-oficiale.md` §2.3. **Nu e întrebare pentru specialistă: actul o spune de două ori.**

**2. Codurile periculoase se tipăreau fără asterisc.** HG 856/2002 **art. 4 alin. (3)**: „Deşeurile
periculoase prevăzute în anexa nr. 2 sunt marcate cu un asterisc (*)" — iar antetul fişei trimite
chiar la „codificarea din anexa nr. 2". `waste_codes.csv` ţine deliberat codul fără asterisc, cu
periculozitatea ca boolean separat (corect pentru stocare), dar **niciun generator nu-l punea înapoi
la tipărire**: nici fişa, nici declaraţia anuală, nici Anexa 3. 408 coduri din 842.

**Unde stă asteriscul, verificat pe anexa 2:** **numai în coloana de cod**. În denumiri, actul scrie
referinţele încrucişate curat — `19 12 12` e „…altele decât cele specificate la **19 12 11**", iar
`20 01 36` trimite la „20 01 21, 20 01 23 şi 20 01 35", toate periculoase, niciuna cu asterisc.
Deci **denumirile din `waste_codes.csv` sunt deja corecte** şi nu se ating; reparaţia atinge strict
codul.

**Ce spune corpusul — şi ce nu poate spune.** Prima citire a concluzionat că n-are niciun cod
periculos; **e adevărat doar pentru codurile raportate**. Scanat programatic la audit, `19 12 11*`
apare în **patru** fişiere (Bragadiru 2022 şi 2024, Cluj 2022, Timişoara 2022) — nu ca rând raportat,
ci **înăuntrul denumirii** lui `19 12 12`, şi acolo ei îl scriu **cu** asterisc, deşi actul nu-l pune.
Deci practica lor foloseşte marcajul mai des decât legea, nu mai rar. Sprijină reparaţia, fără s-o
închidă: niciunul din cele 33 de rânduri raportate nu e pe un cod periculos, deci n-avem niciun
antet completat cu unul — de aici întrebarea **AE**.

⚠️ **Lecţia de metodă, mai valoroasă decât punctul în sine.** „Corpusul nu conţine X" e o afirmaţie
care se **verifică**, nu se presupune din ce scrie în documentaţia noastră despre corpus. Prima
formulare a acestui paragraf a fost greşită fiindcă a repetat o listă de coduri din alt document în
loc să scaneze fişierele. Scanarea a durat un minut şi a schimbat concluzia în bine.

⚠️ **Coliziune de marcaj, de rezolvat odată cu reparaţia:** declaraţia anuală foloseşte deja `(*)`
pe coloana de stoc cu alt înţeles — ieşiri fără cod R/D. Cele două pot ajunge pe acelaşi rând.
Comentariul din `AnnualDeclarationGenerator` arăta că ştiam de ambiguitate; acum devine reală.
Întrebarea **AE**.

### Cinci cerinţe legale neacoperite

| # | Ce lipseşte | Temei | Întrebarea |
|---|---|---|---|
| 3 | **Termenul de 25 februarie** nu se generează. Construim documentul, README-ul dosarului îl numeşte, dar nu pleacă nicio alertă. `AFM_ANNUAL` pe 25 ianuarie e altceva — alt destinatar, alt temei | Ordin 794/2012 art. 6 | **AM** |
| 4 | **Fişierul de ambalaje e `.xlsx`, nu `.xls`.** `XSSFWorkbook` produce OOXML. Documentaţia promitea deja lucrul corect; doar codul nu-l făcea | Ordin 794/2012 art. 6 | **AG** |
| 5 | **Nimeni nu verifică autorizaţia partenerului la data predării.** O tipărim pe Anexa 3, dar `expiringSoon` se calculează faţă de *azi* şi e doar un badge | OUG 92/2021 art. 23(1) + art. 24(1) | **AH** |
| 6 | **Deşeurile periculoase n-au document de transport.** Blocăm corect Anexa 3, dar nu punem nimic în loc — anexa 2 la HG 1061/2008, plus aprobarea prealabilă din anexa 1 | HG 1061/2008 | **AI** 🔴 |
| 7 | **Art. 48 cere tone**, toate documentele sunt în kg. Kilogramele sunt corecte pe fişă; lipseşte cifra pentru depunerea de pe 15 martie, generată azi pentru orice generator | OUG 92/2021 art. 48(1) lit. a) şi c) | **AF** |

Plus două obligaţii pe care nu le ţinem nicăieri: **persoana desemnată** cu gestiunea deşeurilor
(art. 23 alin. (4)–(5) — şi consultantul de mediu *este* „terţa persoană" de acolo, deci rubrica s-ar
completa singură pentru tot portofoliul specialistei — întrebarea **AK**) şi **buletinele de
analiză** pentru codurile periculoase (art. 48 alin. (2), întrebarea **AL**).

### Cinci mărunte

Notele Tabelului 1 din declaraţia de ambalaje citează **HG 621/2005** (abrogată în 2015 de Legea
249/2015) şi **HG 937/2010** (abrogată în 2016) — inconsecvent, fiindcă nota 2 a Tabelului 2 fusese
deja actualizată deliberat (**AJ**) · mesajul dosarului spune „cel mult 3 ani" unde `MAX_YEARS = 5`,
şi prezintă art. 48(5) ca plafon când e prag · nota 2 a fişei inversează `D` şi `TT` faţă de act ·
`ReportType` din frontend a rămas la trei valori din cinci · cap. 2 poate tipări `Modul: TM` lângă
`Cant.: 0.000`, fiindcă modul se ia din toate mişcările iar cantitatea doar din tratarea proprie
(**AN**).

### Ce s-a verificat şi e corect

Nu se atinge, şi fiecare rând a fost citit înapoi în act în sesiunea asta: cele **842 de coduri** ·
cele **cinci nomenclatoare închise** din cap. 2, valoare cu valoare · **formula stocului** · structura
fişei · referinţa cap. 3 · **un singur termen pe 15 martie** · **cele trei cadenţe AFM** · Anexa 3 ca
formular nepericulos cu 3 exemplare · kilogramele la ambalaje · depunerea pe sediul social · foile
protejate · dosarul dimensionat pe 3 ani · filtrul care ţine marfa preluată în afara Anexei 1.

**Toate actele sunt în vigoare şi nemodificate**, reverificate pe 02.09.2026: HG 856/2002 (ultima
consolidare 19.03.2007), OUG 92/2021 art. 48 cu termenul de 15 martie neschimbat, Ordinul 794/2012 cu
25 februarie, HG 1061/2008.

---

## Șase puncte de audit deblocate prin decizie, nu prin răspuns (04.09.2026)

**Întrebarea care a pornit sesiunea:** *„putem progresa și fără Andreea într-o anumită direcție?"*

Răspunsul, după recitirea celor patru documente: **da, și blocajul era mai mic decât arăta
documentația.** Din cele opt puncte marcate „blocate pe specialistă", **unul singur** era blocat pe
ceva ce numai ea poate trimite — **AI**, modelul completat de anexa 2 la HG 1061/2008. Plus **AD**,
din afara auditului. Restul de șase erau blocate pe o **decizie**, iar decizia era a utilizatorului.

Le-a luat pe toate șase într-o singură rundă. Ce urmează e ce s-a construit din ele.

### Distincția care a deblocat lista: „nu știm ce cere ea" vs. „nu ne-am hotărât noi"

Documentația le amestecase sub aceeași etichetă. Triajul nou:

| Fel | Exemplu | Ce se face |
|---|---|---|
| Lipsește un **document** pe care numai ea îl are | AD (registru art. 48), AI (anexa 2 periculoase) | se așteaptă — altfel inventăm un format oficial |
| Lipsește o **decizie de produs** | AH (avertisment vs. refuz), AM (vrem alerta?), AJ (politica notelor) | o ia utilizatorul |
| Actul **răspunde deja**, iar întrebarea confirma doar practica | AE, AN, AF | se citește actul și corpusul |

Mailul care i se trimite s-a scurtat de la zece întrebări la **două cereri**: un registru art. 48
completat și un formular anexa 2 completat.

### AH — autorizația expirată la data predării: avertisment, și numai pe ecran

`renderAnexa3` avea două refuzuri, amândouă spunând „ăsta e documentul greșit" — fără partener n-ai
destinatar, cod periculos merge pe anexa 2. **O autorizație expirată nu face Anexa 3 documentul
greșit:** predarea chiar a avut loc. Un refuz ar fi însemnat că aplicația refuză să documenteze
realitatea, și ar fi blocat reconstituirea unui dosar vechi ai cărui parteneri au expirat între timp.

`WasteMovementResponse` poartă de-acum `recipientAuthorizationExpired` + data, calculate în mapper:

- **numai pe ieșiri** — o intrare sau o generare n-are destinatar de autorizat;
- **numai cu expirare completată** — un câmp gol înseamnă „nu știm", iar regula de lucru 1 interzice
  transformarea unei lipse în acuzație;
- **strict înainte de data mișcării** — o autorizație valabilă *în* ziua expirării e valabilă, deci
  comparația e `expiry.isBefore(date)`. Ambiguitatea cade în favoarea clientului.

Pe ecran, badge **galben** în coloana de partener — nu roșu. Roșul e al lui `UNCLASSIFIED_OUT`, unde
rândul chiar e de nedepus; aici rândul e corect, dar lipsește o condiție de legalitate a predării, pe
care clientul o poate lămuri. E aceeași familie cu „De cântărit".

⚠️ **Și nu se tipărește pe formular.** Hârtia ajunge la destinatar și, la control, la inspector — o
notă a noastră în marginea ei ar fi propria noastră acuzație pusă în dosarul clientului. Pe ecran e
informație; pe hârtie ar fi probă împotriva celui pentru care am construit-o. Motivul e scris în
javadoc-ul lui `renderAnexa3`, ca următorul să nu-l „repare" invers.

Teste: `Anexa3FormIT` — flagul apare, **formularul se tipărește oricum**, iar ziua expirării și
câmpul gol nu declanșează nimic.

### AM — 25 februarie există, 25 ianuarie numește două documente

`ReportType.PACKAGING_ANNUAL`, generat de `DeadlineService.packagingDeadline`. Aplicația construia
documentul de la `V22`, README-ul dosarului chiar numea termenul — dar nu pleca **nicio** alertă
pentru singura depunere pentru care fusese construit tot modulul de ambalaje.

**Cine îl primește:** doar o firmă al cărei profil e **răspuns** și pune ambalaje pe piață. Un profil
gol nu primește nimic — **deliberat invers față de regula ecranelor** (decizia 6: profil gol nu
restrânge nimic). O alertă e o *afirmație*, un ecran e doar o *ofertă*: o alertă lipsă e mai tăcută
decât una falsă, iar cea falsă e exact ce a costat o migrare la `V21`. Comentariul e în cod, ca
inconsecvența aparentă să nu fie „reparată" de cineva care vede doar una din cele două reguli.

**25 ianuarie n-a primit un rând nou.** Notificarea din Ordinul 794/2012 art. 3 și contribuția din
OUG 196/2005 art. 11 alin. (2) cad în aceeași zi, la **același destinatar** (AFM). Două rânduri ar fi
pus două alerte în aceeași zi la aceeași adresă — zgomotul pe care `V21` l-a stins. Un rând, o
etichetă care numește ambele documente.

Fără migrare: `report_type` e stocat ca text. Teste: producător da, comerciant nu, profil gol nu.

### AJ — notele actualizate în toate cele cinci locuri, și o corectură a auditului

Politica era luată de două ori și aplicată în trei locuri din cinci. Acum e aplicată peste tot:
nota 1 → **Legea nr. 249/2015**, nota 3 → **Regulamentul (CE) nr. 1272/2008**, în PDF și în `.xls`.

🔍 **Ce a ieșit la verificare, și e mai valoros decât reparația.** Auditul propusese pentru nota 3
„Regulamentul CLP / **HG 539/2016**". Citit pe Portalul Legislativ, titlul lui HG 539/2016 e:

> HOTĂRÂRE nr. 539 din 27 iulie 2016 **pentru abrogarea** Hotărârii Guvernului nr. 1.408/2008 [...]
> **şi a Hotărârii Guvernului nr. 937/2010** [...]

E un **act pur de abrogare, fără conținut propriu**. O notă care ar trimite acolo ar duce cititorul
la o pagină care nu spune nimic despre etichetare. Regula de fond e Regulamentul CLP, direct
aplicabil — numit chiar în preambulul acelei hotărâri ca motiv al abrogării.

**Lecția, care depășește nota:** *„actul X a fost abrogat de Y" nu înseamnă „scrie Y în loc de X".*
Actul abrogator poate fi doar un certificat de deces. Succesorul se caută în **conținut**, nu în
istoricul abrogărilor. Detaliul, cu link: `surse-oficiale.md` §5.

Testul `PackagingDeclarationIT.theFootnotesCiteTheActsInForce` asertează și **absențele** —
`621/2005`, `937/2010` și `539/2016` — fiindcă modul de eșec e revenirea către model: cineva citește
„verbatim din model" într-un javadoc și pune HG 621/2005 la loc.

### AN — `Modul` și `Scopul` tac odată cu cantitatea

`Anexa1SheetBuilder` calcula cantitatea tratată doar din tratarea proprie (`isExit() && partner ==
null`), corect de la decizia 18 — dar lua `Modul` și `Scopul` din **toate** mișcările lunii. Deci o
predare pe care clientul completase și un mod de tratare tipărea `Modul: TM` lângă `Cant.: 0.000`: o
tratare declarată fără cantitate, o rubrică ce se contrazice singură pe un formular depus.

Cele trei rubrici descriu **același eveniment**, deci vin acum din aceeași mulțime. Corpusul decide
forma rubricii (regula de lucru 3): Panemar — brutărie, doar predă — scrie `0.000` cu Modul `-`;
Hamburger scrie amândouă, fiindcă chiar balotează.

### AF — cifra în tone există, dar nu pe hârtie

OUG 92/2021 art. 48 alin. (1) scrie „cantitatea **în tone**" de două ori. Evidența noastră e integral
în kg — și e **corectă** așa: HG 856/2002 lasă unitatea câmp liber, iar toate cele 33 de foi primite
sunt în kilograme. Una e ce **ții**, alta ce **depui**; până acum clientul făcea conversia de mână,
cod cu cod, în ziua depunerii.

Ce s-a construit: `frontend/src/lib/units.ts` (un singur loc pentru factorul 1000) și, pe ecranul
Evidențe, un panou **„Pentru depunerea din 15 martie — totalul anului, în tone"**, per cod de deșeu,
calculat din rândurile anului.

**Ce NU s-a atins:** niciun formular tipărit. Fișa și declarația rămân în kg. Un document nou
„fișă de depunere SIM" ar fi fost un format oficial inventat. Afișarea e nedistructivă indiferent de
răspunsul la **AF**: arată ambele unități, nu înlocuiește una cu alta.

### AK — persoana desemnată, minimul care se poate construi (`V29`)

OUG 92/2021 art. 23 alin. (4)–(5). Patru coloane nullable pe `companies`: nume, calitate, angajat
propriu vs. terță persoană delegată, certificat de instruire ca text liber.

**Nu e `contact_name`/`contact_role`,** și confuzia era ușoară: acelea sunt blocul de semnătură al
declarației anuale („Întocmit"/„Funcția"), adică cine a redactat documentul. Persoana desemnată e
desemnată prin decizie internă, poate fi din afară, și poartă un certificat pe care blocul de
semnătură nu-l poartă.

**De ce fără să așteptăm răspunsul:** obligația e a legii, nu a practicii. Ce rămâne întrebat e dacă
inspectorul cere certificatul și în ce formă; dacă răspunsul e „nu cere nimeni", costul greșelii e
**patru coloane nefolosite**, nu o felie de aruncat.

În dosarul de control, blocul se tipărește — **iar când lipsește, o spune cu voce tare.** Și asta e
singura diferență față de `marketRoleNote`, care tace la profil gol: rolul de piață e o *proprietate*
a firmei, deci necunoscută înseamnă că nu putem conchide nimic. Persoana desemnată e o *obligație* a
oricui are autorizație de mediu, deci absența ei **este** constatarea. Mai bine o citește clientul
în README decât s-o audă de la inspector.

**Un lucru prins la verificarea pe document, nu la teste:** primul mesaj scria „Completeaz-o în
Setări → profilul firmei". Profilul firmei se editează însă din ecranul **Clienți**, care e
`PLATFORM_ONLY` — deci un ADMIN de client ar fi fost trimis într-un ecran pe care nu-l are. Mesajul
spune acum pe cine să întrebe.

### Verificat pe hârtie, nu doar în teste

Regula de lucru 5, cu un test temporar care a scos documentele pe disc și PyMuPDF pentru randare
(poppler tot nu e instalat). Ce s-a văzut, pe fișa `15 01 01`, luna Septembrie:

- cap. 2 — **Stocare: `250.000` / `CT`** rămâne, fiindcă stocarea chiar s-a întâmplat aici; **Tratare:
  `0.000`, Modul gol, Scopul gol**. `TM`-ul care se tipărea singur a dispărut. Exact reparația AN;
- cap. 3 — `R3` cu operatorul numit; cap. 4 trimite la **anexa nr. 7**, deci reparația din 02.09 ține;
- ambalaje PDF **și** `.xls` — notele scriu `Legea nr. 249/2015` și `Regulamentul (CE) nr. 1272/2008`;
  `.xls`-ul începe cu `d0 cf 11 e0`, deci e BIFF8 adevărat;
- README-ul dosarului — blocul persoanei desemnate, cu textul de „necompletată".

Diacriticele au trecut prin Cp1250 pe toate.

### Igienă găsită în drum

`Anexa1SheetBuilder.java` conținea un **byte NUL brut** într-un literal de String, folosit ca
separator de cheie. Fișierul ieșea „binar" la `grep` și la unelte. Înlocuit cu escape-ul `\0` —
aceeași constantă compilată, fișier text. Plus un import dublu de `java.util.List` în
`DeadlineService`.

### Stare

**186 de teste verzi** (de la 178: opt noi), 0 eșecuri, 0 erori, 0 sărite. O migrare, **`V29`**;
următoarea liberă e `V30`. Frontendul trece `tsc --noEmit` și se build-uiește.

**Ce rămâne blocat, și acum e o listă scurtă:** **AD** (registru art. 48) și **AI** (anexa 2,
transport periculos) — amândouă cer un formular completat. **AE** s-a închis fără cod: coliziunea
celor două marcaje `(*)` era deja rezolvată în `AnnualDeclarationGenerator` prin coloană, paranteze
și o notă de subsol care le numește pe amândouă. **AG** era închisă de reparația din 02.09.

### Commis, pushat şi deployat (04.09.2026, ora 12:40)

Două commit-uri pe monorepo: `be91df2` (codul) şi `ee7efe5` (documentaţia). Split-urile s-au tăiat
şi s-au cherry-pickat după procedura obişnuită, fără `--force` şi fără conflicte — un singur commit
nou de fiecare parte, fiindcă commit-ul de documentaţie nu atinge nici `backend/`, nici `frontend/`.

| | Hash | Release |
|---|---|---|
| `origin/main` = `origin/deploy/heroku-split` | `ee7efe5` | — |
| `newrepo/main` → `ecoregistru-api` | `f79e54d` | **v35** |
| `ferepo/main` → `ecoregistru-app` | `d1a1a12` | **v28** |

**Migrarea a rulat**, verificat în logurile dyno-ului:

```
Current version of schema "public": 28
Migrating schema "public" to version "29 - designated waste manager"
Successfully applied 1 migration to schema "public", now at version v29
Started EcoRegistruApplication in 13.987 seconds
```

**Proba pe producţie, nu doar la teste.** Autentificat pe `admin@demo.ro` şi regenerate termenele
pentru 2027: ies **13** — douăsprezece AFM lunare plus 15 martie. **`PACKAGING_ANNUAL` nu apare**,
şi asta e răspunsul corect: `marketRoles` al tenantului demo e `[]`, iar decizia 37 spune că o
alertă tace la un profil fără răspuns. Dacă ar fi apărut, ar fi fost defectul. `companies/current`
întoarce şi câmpurile noi de persoană desemnată, goale.

**Frontendul, verificat pe bundle-ul servit.** Build-ul a stat din nou în coada Heroku — cel de
backend din acelaşi push a intrat în secunde, ăsta a venit după opt minute. A intrat singur, ca pe
02.09; nu e nimic de reparat, doar de ştiut. Bundle-ul de pe dyno (`/assets/index-DYXCxcIV.js`)
conţine toate cele patru şiruri noi: „Autorizaţie expirată", „25 februarie", „Persoana desemnată" şi
„în tone" — deci ecranele chiar au ajuns acolo, nu doar commit-ul.

### Şi cazul pozitiv, probat pe producţie (04.09.2026, ora 13:05)

Prima rundă probase doar cazul negativ. La cererea utilizatorului s-a făcut şi cealaltă jumătate,
pe ani neatinşi (2028, 2029) ca să nu se amestece cu termenele deja generate:

| | Ce s-a pus pe profil | Ce a ieşit |
|---|---|---|
| 1 | `marketRoles: []` | `AFM_MONTHLY`, `SIM_ANNUAL` — **niciun** `PACKAGING_ANNUAL` |
| 2 | `["PRODUCER"]` | `PACKAGING_ANNUAL` pe **`2028-02-25`** |
| 3 | `["TRADER"]` | `AFM_MONTHLY`, `SIM_ANNUAL` — **niciun** `PACKAGING_ANNUAL` |
| 4 | persoana desemnată completată | dosarul de control tipăreşte blocul: Nume, Calitate, „Delegată unei terţe persoane", Instruire |

Deci regula ţine capăt la capăt pe dyno, nu doar în teste: **profil gol tace, producătorul primeşte
termenul, comerciantul nu.**

⚠️ **Cum s-a făcut, fiindcă PUT-ul pe firmă e o capcană.** `applyProfile` lasă în pace seturile
când vin `null`, dar scrie **necondiţionat** scalarele — `caenCode`, `contactRole`, `anexa3Series`,
cele patru de persoană desemnată. Un PUT parţial le-ar fi golit tăcut. Proba a citit starea
completă înainte, a trimis de fiecare dată payload-ul întreg cu o singură valoare schimbată, şi a
restaurat instantaneul în `finally`. Verificat după: **identic cu starea iniţială**.

🧹 **Ce a rămas în urmă, şi de ce nu se poate curăţa.** Generarea de termene e **aditivă prin
design** — nu şterge niciodată, ca să supravieţuiască starea de „finalizat" la o regenerare — iar
`DeadlineController` n-are endpoint de ştergere. Deci pe tenantul demo au rămas termenele pentru
**2027, 2028 şi 2029**, printre care un **`PACKAGING_ANNUAL` pe `2028-02-25`**, creat cât profilul
era `PRODUCER`.

**Nu e un defect al filtrului.** Dacă o sesiune viitoare vede rândul acela pe un tenant al cărui
`marketRoles` e `[]` şi crede că gating-ul s-a stricat: nu s-a stricat, rândul e mai vechi decât
profilul de azi. Sunt ani care n-au venit încă, pe un tenant de test.



## Depozitul amânat, două felii deblocate în loc (06.09.2026)

Sesiunea a început cu întrebarea „unde suntem". Prima alegere a fost **Etapa 8 fără export**, iar
utilizatorul a revenit după ce am citit schema: *„hai să lăsăm partea de depozit deoparte până am
meeting cu Andreea"*. Decizia e bună şi merită scrisă cu motivul, fiindcă e uşor de reluat greşit:
pentru fişă, Anexa 1 Ambalaje şi Anexa 3 avem **model completat** în corpus, deci forma se poate
copia; pentru registrul art. 48 **nu avem niciunul**, iar actul descrie conţinutul, nu forma. Deci
Etapa 8 nu e „mai grea", e **de alt fel**: e singura felie unde am inventa un format oficial.

Ce s-a livrat în loc — două felii care nu depind de nimeni.

### Trei corecturi de stare, făcute înainte de orice cod

1. **„Cele două coloane moarte" din `prompt-continuare.md` nu mai există.** `total_collected` a fost
   scoasă în `V18`, iar `total_handed_over` **e scrisă** de `EvidenceCalculator` şi are teste care o
   ţin. Chiar comentariul lui `V18` spunea asta („Documentaţia internă le enumera pe amândouă ca
   «rămase în schemă nescrise»; era adevărat doar despre asta de aici"), dar rândul din handoff n-a
   fost corectat atunci. Acum e.
2. **`CLOUDINARY_URL` chiar lipseşte**, verificat pe dyno cu `heroku config -a ecoregistru-api`:
   sunt opt variabile, niciuna Cloudinary. Ataşamentele de pe mişcări nu urcă în producţie. Rămâne
   deschisă — cere contul, nu cod.
3. **Producţia are 4 mişcări pe registrul art. 48** (3 `COLLECTED` + 1 `RECOVERED`) şi tabelele
   `receptions`/`deliveries` sunt goale, cum le-a lăsat `V5`. Citit ca să ştiu ce mută Etapa 8.

### Felia 1 — alerta de expirare a autorizaţiei partenerului (`V30`)

Ultimul slice deschis din FAZA TERMENE. `MailType.PARTNER_AUTHORIZATION_EXPIRING` exista în enum
**şi nu era folosit nicăieri**: felia fusese numită, nu construită.

**Temeiul.** OUG 92/2021 art. 23 alin. (1) — predarea e legală numai către un operator **autorizat**;
alin. (2) — predarea nu descarcă de răspundere. Deci expunerea e a clientului, nu a partenerului, şi
mailul o spune în atâtea cuvinte, altfel se citeşte ca „are partenerul o problemă".

**Nu e o dublare a deciziei 36.** Cele două rezolvă lucruri diferite: decizia 36 compară expirarea
cu **data mişcării** şi arată un badge galben — semnalează **după**, când predarea s-a întâmplat
deja. Asta scrie un mail cu 60 de zile **înainte**, cât mai e timp de reînnoit sau de ales altcineva.
Prima e o constatare, a doua e o şansă.

**Fereastra e numai înainte, `[azi, azi+60]`.** O autorizaţie deja expirată nu intră pe mail:
un mail despre una expirată acum doi ani nu previne nimic, iar la prima rulare ar fi plecat câte
unul pe fiecare partener vechi deodată. E deja acoperită în alte două locuri — badge-ul din ecranul
Parteneri (acelaşi prag, dar calculat cu `<=`, deci prinde şi trecutul) şi avertismentul de la
predare. Pe producţie, regula asta înseamnă **un singur partener** care ar primi mail azi
(expirare 24.09.2026), nu trei.

**Coloana ţine o dată, nu un boolean**, şi ăsta e miezul feliei. Cheia de deduplicare e chiar
expirarea pentru care s-a scris ultima oară. Un boolean ar fi cerut ca cineva să-l stingă manual la
reînnoire — şi nimeni n-ar fi făcut-o, deci al doilea termen ar fi trecut tăcut. Cu o dată,
reînnoirea **rearmează singură** alerta, şi nicio linie din `PartnerService` nu trebuie să ştie de
coloană. Există test: `renewingTheAuthorizationReArmsTheAlert`.

**Clasă proprie, nu o metodă pe schedulerul de termene.** Un termen de raportare şi o autorizaţie de
partener au comun doar faptul că au o dată. Ferestre diferite, deduplicare diferită, moduri de eşec
diferite — iar numele `DeadlineAlertScheduler` ar fi încetat să fie adevărat. Sunt două joburi care
se nimeresc dimineaţa aceleiaşi zile.

**Şi mailul s-a verificat pe artefact, nu doar din aserţiuni** (regula de lucru 5, aplicată unui
e-mail în loc de un PDF). `PartnerAuthorizationMailIT` porneşte serviciul real, `EmailService` real
şi Thymeleaf real, cu numai SMTP-ul mockat, şi citeşte HTML-ul din `MimeMessage`. Un şablon care ar
citi `${partnerNume}` în loc de `partnerName` randează o celulă goală şi trece toate celelalte teste
din proiect. Două lucruri prinse aşa, pe care nicio aserţiune nu le-ar fi semnalat:

- **diacriticele erau cu sedilă** (`ţ`, `ş`) în tot şablonul, iar restul textelor din aplicaţie sunt
  cu virgulă (`ț`, `ș`). Corectat. *(Sedila rămâne corectă în PDF-uri — acolo e Cp1250, decizia 8.)*
- **„expiră în 60 zile" e agramatical.** Româna cere „de" de la 20 în sus. `whenExpiry` are acum
  pragul, cu test (`theWordingAgreesWithSmallNumbers`), iar `when()` de la termene rămâne separată
  fiindcă fereastra ei e de şapte zile şi nu atinge niciodată pragul.

Două capcane de mediu, notate ca să nu se redescopere: mock-ul de `JavaMailSender` lasă fără bean
health-check-ul de mail din actuator şi pică tot contextul cu „Beans must not be empty"
(`management.health.mail.enabled=false` în test), iar `MimeMessage` nu-şi scrie anteturile
`Content-Type` până la `saveChanges()`, deci parcurgerea MIME găseşte peste tot `text/plain`.

**14 teste noi**, 10 pe scheduler şi 4 pe mailul randat.

### Felia 2 — Anexa 3 Ambalaje (`V31`)

Raportul anual al colectorilor, comercianţilor, reciclatorilor şi valorificatorilor de deşeuri de
ambalaje (Ordinul 794/2012, anexa nr. 3). Termen 25 februarie, ca şi anexa 1. **Celălalt capăt al
lanţului**: anexa 1 raportează ce a pus firma pe piaţă, anexa 3 ce a **preluat de la terţi** şi ce a
făcut cu marfa.

S-a putut construi fără să aşteptăm nimic fiindcă avem **modelul completat**
(`documente oficiale/RAPORTARE DESEURI DE AMBALAJ COLECTATE ANUAL.ods`, care e tabelul 1 gol) plus
textul articolelor. Citit din `.ods` direct din `content.xml`, fiindcă `odfpy` nu e instalat aici.

**Ce a arătat modelul şi documentaţia nu descria:** provenienţa e **sub-rând sub fiecare material**,
nu o coloană cu o valoare. Hârtie-carton urmată de populaţie / colectori / generatori persoane
juridice, apoi „total hârtie-carton".

#### Deciziile, fiecare cu sursa

| | Ce | De unde |
|---|---|---|
| **Care tabel** | din profilul firmei, `PackagingOperatorRole` — colector/comerciant → tabelul 1, reciclator/valorificator → tabelul 2 | art. 4 alin. (1): „tabelul 1 sau, **după caz**, tabelul 2"; alegerea utilizatorului, dintre trei variante |
| **Nul = nu se tipăreşte nimic** | ecranul spune ce e de completat, documentul refuză | decizia 37 aplicată unui document: *un ecran e o ofertă, un document e o afirmaţie*. Un antet „Colectori/Comercianţi" pe un formular ar afirma calitatea juridică a clientului în locul lui |
| **Provenienţa stă pe partener** | `partners.packaging_origin`, cu suprascriere pe mişcare | nota 2 descrie **sursa**, nu transportul. Închide jumătatea lui **AA** care era a noastră |
| **Suprascrierea pe mişcare nu e un moft** | `waste_movements.packaging_origin` | „populaţia" n-are CUI, n-are autorizaţie şi nu e partener. **Fără coloană, rândul „populaţie" al formularului n-ar putea fi completat deloc** — iar un centru de colectare cumpără de la populaţie zilnic |
| **Reciclare = R3, R4, R5** | restul codurilor R = valorificare prin alte metode; codurile D = niciuna | **se citeşte din act**: OUG 92/2021 anexa nr. 3 numeşte „Reciclarea/Recuperarea" exact trei operaţiuni. R1 e „întrebuinţarea în principal drept combustibil", pe care directiva-cadru o exclude pe nume |
| **Un document per punct de lucru** | nu o pagină per punct de lucru, cum face declaraţia anuală | art. 4 alin. (4) „pentru fiecare punct de lucru în parte" + alin. (3) trimite la agenţia din raza lui: două puncte în două judeţe = două depuneri, la doi destinatari. Un fişier unic n-ar putea fi depus |
| **Kilograme, nu tone** | deşi modelul scrie „(tone)" în antet | art. 8 alin. (1) lit. a). Modelul e şablon modificat local, ca şi cel de anexa 1 — regula de lucru 2 |
| **Aluminiu şi Oţel separat** | deşi modelul are un singur rând „metal /aluminiu" | acelaşi motiv, aceleaşi rânduri ca la anexa 1 |
| **`.xls` protejat + PDF** | ca la anexa 1 | art. 6 e despre „datele de raportare" în general, nu doar despre anexa 1 |

#### Două defecte prinse uitându-mă la document, nu din teste

Regula de lucru 5, încă o dată, şi încă o dată a meritat. Cele 16 teste treceau toate.

1. **Totaluri duplicate.** Ieşea „total pet — 980" şi imediat sub el „total plastic — 980". În model,
   materialele care intră într-o grupă (PET şi alte plastice → plastic; aluminiu şi oţel → metal)
   **nu** au total propriu; îl are doar grupa. Total propriu au doar Sticla, Hârtia carton, Lemnul şi
   Altele. Aceeaşi cifră sub două etichete se citeşte, pe un formular depus, ca **două cantităţi
   diferite**. Reparat cu `PackagingMaterial.isSummedIntoAGroup()`, cu două teste.
2. **O aliniere care minte.** Cele două jumătăţi ale tabelului au cardinalităţi diferite — trei
   provenienţe faţă de doi operatori — iar împerecherea rând cu rând punea „22.000 kg → Reciclator
   SA" pe acelaşi rând cu „populaţie". Pe hârtie rubrica materialului e o **casetă**, nu un rând; la
   noi bordura face rândul să pară o afirmaţie. Acum jumătăţile sunt **stivuite**: un rând poartă una
   sau alta, niciodată o coincidenţă. Test: `noRowPairsAProvenanceWithAnOperatorFigure`.

#### Şi o pierdere tăcută de date, prinsă înainte să existe

Răspunsul întorcea provenienţa **rezolvată** (mişcare sau partener). La redeschiderea unei mişcări
care moştenea răspunsul partenerului, select-ul ar fi arătat gol şi salvarea l-ar fi şters; invers,
o suprascriere proprie ar fi fost transformată tăcut în moştenire. Rezolvat exact ca la material:
`packagingOrigin` e ce a scris clientul **pe mişcare**, `effectivePackagingOrigin` e ce va tipări
formularul.

**19 teste noi.** Ecranul: secţiune nouă în tabul Ambalaje, cu selectorul de punct de lucru, tabelul
aplicabil, semnalele pentru ce n-a intrat, şi cele două descărcări.

### Stare

**224 de teste verzi** (de la 186: 38 noi), 0 eşecuri, 0 erori, 0 sărite. Migrări până la **`V31`**;
următoarea liberă e **`V32`**. Frontendul trece `tsc --noEmit` şi se build-uieşte.

**Commis, pushat şi deployat** — vezi secţiunea de mai jos.

### Auditul cerut după livrare — cinci defecte, toate în ce nu se vedea (06.09.2026)

Cele două felii erau „gata": 221 de teste verzi, frontendul build-uit. La cererea utilizatorului
(*„verifică bine ce ai lucrat [...] să nu fi lăsat inconsistenţe sau buguri"*) am făcut o trecere
sistematică. **Cinci defecte, niciunul prins de teste** — şi toate în acelaşi loc: în ce nu se
vedea. Patru din cinci au ieşit **citind documentul randat**, a cincea scriind un test care să
demonstreze o bănuială.

#### 1. XLS: a doua cifră a tabelului 2 era text, nu număr

Cele două tabele împărţeau un singur scriitor de rânduri, care primea toate celulele ca `String[]`.
Pe tabelul 1 asta e inofensiv — coloana 6 chiar e text, numele operatorului. Pe tabelul 2 coloana 6
e **„cantitatea valorificată prin alte metode"**, deci ieşea o celulă text pe care Excel n-o
adună, şi pe care depunătorul n-o poate verifica. În plus, acumulatorul de totaluri aduna doar
prima cifră, deci coloana lipsea şi din „total <material>" şi din „TOTAL ambalaje".
**Reparat** scriind cele două tabele separat, ca în generatorul PDF. Test:
`bothColumnsOfTabelul2AreNumbersAndBothCountInTheTotals`.

#### 2. O preluare necântărită dispărea fără urmă

Constructorul filtra `quantity != null` din capul locului. O mişcare care aşteaptă cântarul —
caz legitim, `quantity` e nullable prin decizia 5 — nu apărea nici în tabel, nici în lista „nu
intră în tabel". Adică exact ce interzice regula casei: **o lipsă trebuie să se vadă ca lipsă.**
**Reparat:** cantitatea lipsă e acum al treilea motiv de raportare, lângă material şi provenienţă,
şi pe intrări, şi pe ieşiri. Se numără şi pe ecran. Test:
`aTakeoverStillAwaitingItsWeightIsShownRatherThanDropped`.

#### 3. 🔴 PDF: reciclarea şi valorificarea, adunate în coloana greşită

Cel mai grav. Pe tabelul 2, rândurile de total ale PDF-ului puneau **suma amândurora** în coloana
„cantitatea reciclată" şi lăsau vecina goală: 22.000 kg reciclaţi + 2.100 valorificaţi altfel se
tipăreau ca **24.100 kg reciclaţi**. Pe formularul ăsta aia e chiar cifra cu consecinţe legale —
obiectivele de reciclare. XLS-ul era corect, deci **cele două documente ale aceleiaşi depuneri
spuneau lucruri diferite**.

**Cauza e structurală, nu o scăpare:** fiecare generator îşi calcula singur totalurile. Reparaţia nu
e o corectură într-un loc, ci mutarea calculului în document — `PackagingAnexa3.Totals`, cu
`totalsFor`, `totalsOver` şi `grandTotals` —, iar cele două generatoare doar tipăresc ce li se dă.
O sursă, două imprimante; clasa asta de bug nu se mai poate întoarce. Teste:
`aSummedLineKeepsRecyclingApartFromOtherRecovery` şi
`onTabelul1EverythingThatLeftCountsInOneColumn`.

#### 4. Totalul nu însuma coloana „din care periculoase"

Rândul de detaliu arăta 120 kg periculoase la Oţel, iar „total metal" lăsa celula goală. Nu e
fidelitate faţă de model, e **document care se contrazice singur**: aceeaşi coloană, totalizată
într-un rând şi nu în celălalt. Reparat în amândouă. Test: `theTotalRowsSumTheHazardousColumnToo`.

#### 5. Se putea descărca formularul fără punct de lucru

Selectorul avea „Toate punctele de lucru", util pe ecran — dar descărcarea din starea aia producea
un formular cu rubrica „Punct de lucru" **goală**, adică nedepunibil: art. 4 alin. (4) cere
raportarea per punct de lucru, iar alin. (3) o trimite la agenţia din raza lui. Acum, cu un singur
punct de lucru se selectează singur, iar pe „Toate" butoanele sunt inactive şi ecranul spune de ce.

#### Şi trei lucruri verificate care **nu** erau defecte

- **Firmele dezactivate n-ar primi mailuri greşite:** `CompanyRequest` n-are câmpul `active` şi
  `CompanyService` scrie doar `active(true)`, deci o firmă nu poate fi dezactivată prin API.
  Schedulerul nou se poartă oricum identic cu cel de termene.
- **Suprascrierea provenienţei nu se pierde la reeditare** — separarea
  `packagingOrigin` / `effectivePackagingOrigin` era deja făcută exact pentru asta.
- **Fereastra alertei e inclusivă la ambele capete** (`Between`), cu teste pe ziua 60 şi ziua 61.

#### Igienă găsită în drum

`V31` crea un index parţial pe `(company_id, packaging_origin)`, cu un comentariu care spunea că
„adaugă capătul pe cod" — fals de două ori: indexul nu conţine codul, iar documentul citeşte
mişcările prin `findAllByCompany_IdAndDeletedFalseAndDateBetween`, acoperit deja de indexul din
`V5`, şi filtrează în memorie. **Un index pe care nu-l foloseşte nicio interogare e cost de scriere
fără cititor** — scos, cu motivul în migrare. Plus un `nz()` rămas mort în amândouă generatoarele
după mutarea totalurilor, şi un import nefolosit.

**224 de teste verzi** după audit — 219 înainte, plus cele **cinci** scrise ca să ţină pe loc fiecare defect găsit. 0 eşecuri, 0 erori, 0 sărite.

> **Ce merită reţinut din runda asta.** Toate cele patru defecte de output au trecut de teste
> verzi şi au picat la prima citire a documentului randat. Regula de lucru 5 nu e o formalitate de
> final, e singura care prinde clasa asta. Şi încă ceva: **două generatoare pentru acelaşi
> document sunt două ocazii să difere** — dacă amândouă calculează, vor diverge; dacă amândouă
> citesc, nu pot.

### Commis, pushat şi deployat (06.09.2026, ora 15:01)

Două commit-uri pe monorepo: `b367b57` (codul, ambele felii) şi `4cac65a` (documentaţia). Un singur
commit de cod fiindcă cele două felii ating aceeaşi entitate — `Partner` — iar o despărţire ar fi
produs un commit care nu compilează; e scris ca atare în mesaj.

Split-urile s-au tăiat şi s-au cherry-pickat după procedura obişnuită, fără `--force` şi fără
conflicte, câte un commit nou de fiecare parte. `git reset --hard` din procedură **n-a fost
necesar**: cele două branch-uri locale erau deja exact la capetele remote-urilor, aşa că s-a sărit —
merită verificat întâi cu `git rev-parse`, e mai rapid şi mai puţin brutal decât resetul.

| | Hash | Release |
|---|---|---|
| `origin/main` = `origin/deploy/heroku-split` | `4cac65a` | — |
| `newrepo/main` → `ecoregistru-api` | `6e16562` | **v36** |
| `ferepo/main` → `ecoregistru-app` | `e595e19` | **v29** |

**Amândouă migrările au rulat**, verificat în logurile dyno-ului:

```
Current version of schema "public": 29
Migrating schema "public" to version "30 - partner authorization alert"
Migrating schema "public" to version "31 - packaging anexa3"
Successfully applied 2 migrations to schema "public", now at version v31
Started EcoRegistruApplication in 8.035 seconds
```

**Verificat şi în baza de producţie**, nu doar în loguri: cele patru coloane există
(`partners.authorization_warning_sent_for` ca `date`, `partners.packaging_origin`,
`companies.packaging_operator_role`, `waste_movements.packaging_origin`), iar `flyway_schema_history`
are `30` şi `31` cu `success = true`.

**Frontendul, verificat pe bundle-ul servit**, nu pe build: `/assets/index-CvqdoP90.js` de pe dyno
conţine toate şirurile noi — „Anexa 3. Deşeuri de ambalaje preluate", „Provenienţa", „populaţie",
„Reciclator" şi citatul din art. 4 alin. (1). Deci ecranul chiar a ajuns acolo. *(De data asta
build-ul de frontend a intrat înaintea celui de backend, invers decât ultimele două dăţi.)*

**Şi proba alertei, pe datele reale.** Interogat pe producţie cine ar primi mail la următoarea
rulare de 07:15: **exact un partener**, `Hamburger`, cu autorizaţia expirând pe 24.09.2026. Cei doi
parteneri cu autorizaţii deja expirate (12.06 şi 26.08) **nu** apar — adică fereastra „numai
înainte" se poartă pe date reale exact cum spune migrarea, şi prima rulare nu inundă pe nimeni.

⚠️ **Ce se va vedea în loguri, şi nu e defect.** Partenerul acela e al lui `Demo Reciclare SRL`, ai
cărui utilizatori sunt toţi pe `@demo.ro` — domeniu care nu e al nostru, deci Gmail îi va respinge.
Schedulerul prinde excepţia, o loghează, **lasă flagul nescris** şi reîncearcă mâine. Deci va apărea
un `Failed to send authorization warning` pe zi până pe 24.09, când partenerul iese din fereastra de
60 de zile şi zgomotul se opreşte singur. E chiar comportamentul proiectat — o livrare eşuată nu se
marchează ca trimisă — şi totodată o consecinţă a conturilor de demo lăsate deliberat în producţie.

### Agenda meetingului cu Andreea, pregătită

`docs/intrebari-specialist.md` (gitignored) are acum, în cap, o **agendă pe cinci puncte, ordonată
după ce se pierde dacă meetingul se termină devreme**: cele două documente de obţinut (**AD**,
**AI**), practica inspectorului (**AL**), cele două întrebări vechi (**C**, **W**) — scrise complet
în fişier, unde lipseau —, cele două lucruri de confirmat pe Anexa 3 Ambalaje, şi confirmările de
pus doar dacă mai e timp. Plus ce ducem noi: zece întrebări au devenit trei, şi de ce.


### Modernizarea interfeţei — şaisprezece puncte, pe felii (07.09.2026)

La cererea utilizatorului (*„scanează aplicaţia şi spune-mi ce improvement de UI/UX am putea
face"*, apoi *„să le faci treptat pe toate şi să verifici constant [...] să nu strici nimic sau să
bagi buguri sau inconsistenţe"*), o trecere completă peste frontend. **Fără temă întunecată**, cerut
explicit. Ramura: `ui-ux-modernizare`; `main` neatins.

Scanarea a găsit șaisprezece lucruri. Cele care cântăreau, în ordinea în care s-au reparat:

1. **Aplicaţia nu era responsive.** Patru breakpointuri în opt mii de linii, toate pe Panou.
   Sidebarul era `w-60` fix, fără cale de a-l ascunde — iar omul care înregistrează o predare la
   cântar, în depozit, e exact utilizatorul de telefon. Acum: sertar sub `lg`, 24 de grile de
   formular stivuite, `PageHeader` în loc de antetul scris de mână în opt pagini.
2. **Formularul de mişcare stătea în 512px** cu treizeci de rubrici şi rânduri de câte trei
   coloane. Acum e `xl`, cu opt secţiuni titrate. **Ordinea rubricilor nu s-a schimbat** — e cea
   stabilită cu specialista; blocurile s-au mutat prin poziţie, nu rescrise.
3. **Eroarea de validare nu spunea care rubrică.** `validate()` întorcea un şir pus în capul unui
   formular care se derulează pe câteva ecrane. Acum întoarce o hartă rubrică → mesaj, cu
   `aria-describedby` şi derulare la prima greşeală. Regulile sunt neatinse.
4. **Cele cinci `window.confirm`** nu puteau spune *ce* rând se şterge. Textele s-au rescris ca să
   spună urmarea — verificată în cod, nu presupusă: `usePartners` şi `useInternalGenerators` spun
   amândouă că dezactivarea e într-un singur sens, deci niciun mesaj nu promite că se poate desface.
5. **Tabelele nu scalau.** Unsprezece tabele, niciunul cu căutare, sortare sau paginare.
6. **Duplicarea mişcării** lipsea: treizeci de predări pe lună însemnau treizeci de deschideri ale
   unui formular din care aceleaşi douăzeci şi opt de rubrici se rescriau de fiecare dată.

Restul: schelete şi stări goale unificate în toate tabelele · filtrele în bara de adrese
(`?an=`, `?luna=`, `?punct=`, `?rol=`, `?vedere=`) · panoul care răspunde la „sunt în regulă?" ·
navigaţia grupată şi contul ca meniu · progres pe urcarea atașamentelor · paletă de comenzi
(Ctrl+K), `/` şi `N` · tooltip-uri în locul lui `title` · 266 de clase `gray-*` mutate pe tokens.

**Două afirmaţii scrise cu grijă ca să nu mintă.** Suma lunii de pe panou e „cantitate
înregistrată", nu „generat" — o ieşire nu e o generare, iar generarea o deduce motorul din ieşiri
(decizia 17). Stocul spune „la ultima lună calculată", fiindcă vine din evidenţa care poate fi în
urma mişcărilor.

**Verificare:** `tsc --noEmit` curat şi `vite build` verde **după fiecare felie**, nu doar la final.
**Backendul n-a fost atins**, deci cele 224 de teste rămân cum erau.

#### Auditul de după — inconsistenţa cea mai mare era a mea

La a doua cerere (*„verifică tot şi continuă, dar să fie totul pus la punct"*) am făcut o trecere
sistematică peste ce livrasem. A ieşit exact defectul pe care îl reproşasem codului: **unsprezece
tabele şi un singur tabel cu bară de căutare.** Mişcări fusese primul refăcut, restul rămăseseră.

Regula e acum una singură, scrisă într-un loc: fiecare tabel primeşte `TableToolbar` şi
`TablePagination`; bara apare de la zece rânduri în sus şi rămâne cât timp se caută ceva; antetul
lipicios e implicit peste tot. Pe un tabel cu patru puncte de lucru nu apare nimic în plus.

Restul auditului:

- 14 şiruri şi componenta `InfoHint`, adăugate speculativ, pe care nu le folosea nimeni
- `padded` pe `Card`, `label` pe `RowActions`, `setPageSize` pe vedere — API neatins de niciun apelant
- `bg-white` scăpase de migrarea pe tokens (mapa acoperea doar `bg-gray-*`): unsprezece locuri
- `Card` se folosea doar pe Panou, deşi Dosarul de control şi cele trei pagini de autentificare
  aveau aceeaşi cutie scrisă de mână
- nouă linkuri `text-blue-600` într-un produs al cărui brand e emerald
- **formularul de autentificare n-avea etichete legate** (`<label>` fără `htmlFor`): nici clicul nu
  focaliza, nici cititorul de ecran nu ştia ce se cere
- **cele 66 de câmpuri din grila de suprascriere a Anexei 1 Ambalaje n-aveau nume**: capul de tabel
  se vede, dar nu se aude
- indentarea blocului „lunar" din Evidenţe, greşită încă dinainte — derivă de la 56% la 7%

**Două greşeli proprii, reparate în aceeaşi sesiune.** Am rulat Prettier pe fişiere editate;
proiectul nu-l are configurat, iar implicitul de 80 de coloane a reformatat două fişiere întregi
contra stilului de ~100 al casei. Am revenit la commit şi am reaplicat prin scripturi — build-ul de
după a ieşit cu hash identic. A doua: scriptul de tokens a rescris şi numele de clase **din
comentarii**, care descriau codul vechi; le-am pus la loc.

**La momentul ăsta nu se deschisese niciun ecran:** extensia Chrome nu se conecta, deci verificarea
era doar compilator, build şi citirea codului. Ce a ieşit când s-au deschis, în secţiunea
următoare.


#### Proba pe browser — şapte defecte pe care compilatorul nu le vedea (07.09.2026)

La a treia cerere (*„deschide şi testează tot, nu punem nimic în prod fără să fie 100% validat"*),
prima probă adevărată: **backend local** cu profilul `dev` şi datele demo (Postgres 17 era deja
pornit), **Chrome condus prin Playwright** — extensia nu se conecta, dar `playwright-core` conduce
Chrome-ul instalat, fără să descarce nimic. Nu s-a atins producţia: totul pe `localhost:8080`.

55 de verificări: cele douăsprezece ecrane deschise şi fotografiate, interacţiunile apăsate una
câte una, formularul de mişcare umblat, ecranul de 375px măsurat. **Şapte defecte, niciunul vizibil
din cod** — `tsc` şi `vite build` treceau pe toate:

1. **Căutarea „15 01 02" întorcea coduri 15 01 07.** Potrivirea era pe subşir oriunde în textul
   rândului, iar „02" se găseşte în „2026" din dată. Reparat în două trepte — prima a scăzut
   numărul de intruşi de la doi la unul, fiindcă rămânea mişcarea din **02**.06. Fondul: un cod de
   deşeu e o **expresie**, nu trei cuvinte. Acum se caută întâi expresia întreagă.
2. **Primul clic pe coloana sortată implicit părea că nu face nimic.** Ciclul avea trei stări, iar
   pe Mişcări (implicit: dată descrescător) primul clic ducea la „deloc" — unde ordinea de la
   server e tot descrescătoare după dată. Măsurat: `aria-sort` trecea pe `none`, primul rând
   rămânea acelaşi. Acum două stări.
3. **Paleta evidenţia „Mişcări" când tastai „evid"**, fiindcă grupul lui Mişcări e „Evidenţă" şi
   intra în textul căutat. Apăsai Enter aşteptând Evidenţe şi rămâneai unde erai.
4. **Trei rubrici din şapte n-aveau marcaj de eroare** — codul de deşeu, punctul de lucru, data.
   Scriptul care le lega se oprise înainte să scrie; `validate()` le seta, dar nu le afişa nimeni.
   Bannerul spunea „verifică rubricile marcate mai jos" **fără să marcheze nimic**. Ăsta e felul de
   defect pe care numai apăsarea butonului îl scoate la iveală.
5. **„Şterge" din meniul de rând nu se putea apăsa.** `position: sticky` face un context de
   stivuire propriu, deci meniul rămânea prins în celula lui, iar celulele fixate ale rândurilor de
   dedesubt se desenau peste el.
6. **Coloana de acţiuni ieşea din ecran.** Măsurat la 1280px: Mişcări depăşeşte cu 202px, Ambalaje
   cu 213, Parteneri cu 122. Nu se vedea nici măcar că *există* o coloană de acţiuni. Fixată la
   marginea din dreapta, în toate cele nouă tabele care au una.
7. **Panoul se derula lateral cu 110px pe telefon.** O grilă cu o singură coloană foloseşte o pistă
   `auto`, dimensionată după conţinutul cel mai lat — iar `truncate` nu micşorează lăţimea maximă a
   textului. 27 de grile au primit `grid-cols-1`, adică `minmax(0, 1fr)`.

Plus rândurile de tabel cu `scroll-mt-12`, ca unul adus la vedere să nu ajungă sub antetul lipicios
— browserul derulează singur când focusul cade pe un rând nevăzut, adică la navigarea cu Tab.

**Ce spune asta despre feliile de dinainte.** Erau toate „verificate": `tsc --noEmit` curat, build
verde, cod recitit. Niciuna dintre cele şapte n-a ieşit aşa. Patru cereau apăsarea unui buton, două
o măsurătoare de geometrie, una o privire pe o captură. Compilatorul spune că programul e
consistent cu el însuşi, nu că face ce trebuie.

**Suita a rămas în afara repo-ului**, în directorul temporar al sesiunii: patru fişiere `.mjs` şi
`playwright-core`. Adăugarea unui cadru de teste în proiect e o decizie proprie, nu una de luat din
mers. *(Decisă în aceeaşi zi, câteva ore mai târziu: suita e acum `frontend/e2e/`.)*

#### Recitirea ramurii — trei defecte, toate în primitive (07.09.2026)

Ramura recitită de la capăt, cu `tsc`, `vite build` şi suita rulate pe ea. Cele şapte de mai sus
erau fiecare într-un ecran; **astea trei sunt în primitivele pe care le folosesc toate**, deci
ajungeau pe toate ecranele deodată.

1. **`Dialog` îşi fura singur focusul.** Ascultătorul de taste avea `[open, onClose, busy]` în
   dependenţe, iar cleanup-ul aceluiaşi efect e cel care dă focusul înapoi elementului de dinaintea
   deschiderii. Cum `onClose` e scris inline la 11 din 13 apelanţi şi `busy` comută la fiecare
   salvare, cleanup-ul rula **cu dialogul încă deschis**: apăsai Salvează şi focusul sărea pe
   butonul din spatele lui. Pe o salvare respinsă de server formularul rămânea deschis cu focusul
   afară, iar capcana de Tab nu-l mai aducea înapoi — ea prinde doar Shift+Tab din exterior.
   Reparat rupând efectul în două: scrollul blocat şi focusul redat atârnă acum numai de `open`,
   iar handlerul citeşte `onClose`/`busy` dintr-un ref, ca în `useHotkey`.
2. **Escape în comboboxul de cod închidea tot formularul.** Comboboxul apela `preventDefault()`,
   care nu opreşte propagarea, iar `Dialog` ascultă Escape pe `document`. Deci manevra adăugată
   dinadins la felia de tastatură nu funcţiona exact în singurul loc unde contează: apăsai Escape
   ca să scapi de lista de coduri şi pierdeai tot ce scrisesei în cele opt secţiuni. Reparat cu
   `stopPropagation()`, plus o gardă pe `defaultPrevented` în `Dialog` pentru straturile viitoare.
   **Proba a fost scrisă întâi şi verificată că pică fără reparaţie** — `{lista: false, dialog:
   false}` înainte, `{lista: false, dialog: true}` după.
3. **Sortarea descrescătoare aducea la vârf exact rândurile puse dinadins la coadă.** Trei
   comparatoare scriau `return 1` pentru rândul fără valoare — cantitatea „De cântărit" pe Mişcări
   şi în registrul de predări, autorizaţia fără dată pe Parteneri — iar `useTableView` aplica
   direcţia cu `reverse()` peste rezultatul lor. Deci a doua apăsare pe „Cantitate" scotea în capul
   listei toate mişcările necântărite, cu trei comentarii în cod care scriau că stau la coadă în
   ambele sensuri. Reparat cu `missingLast()` în hook, iar direcţia se aplică negând comparatorul:
   `reverse()` inversa în plus şi ordinea rândurilor egale între ele, adică ordinea de la server.

**De ce n-a prins suita al treilea.** Fiindcă n-are pe ce: seed-ul de demo are **zero** din 34 de
mişcări fără cantitate şi **zero** din 5 parteneri fără dată de autorizaţie. Verificarea scrisă
pentru regulă a fost scoasă, nu lăsată să treacă pe gol — garda ei a raportat „0 rânduri fără
cantitate". 🟡 **De completat seed-ul** cu stările pentru care ecranele au reguli proprii (o mişcare
cu `weighed_at_unloading`, un partener fără expirare, o ieşire fără cod R/D); atinge backendul, deci
n-a intrat în ramura de interfaţă.

**Două lucruri ţineau de unealtă, nu de aplicaţie.** `channel: "chrome"` era scris în `lib.mjs`, iar
maşina pe care s-a făcut recitirea n-are niciun browser din familia Chromium — doar Safari. Suita nu
putea porni deloc acolo. Acum browserul se alege prin `E2E_CHANNEL`, implicit tot `chrome`, cu
Chromium-ul lui Playwright ca ieşire de rezervă. Iar `playwright-core` era în manifest dar nu în
`node_modules`: un checkout de ramură nu atinge dependenţele.

**Suită: 58 de verificări, toate verzi** (55 înainte). `tsc --noEmit` curat, `vite build` verde.

#### Căutarea cerea diacritice — al patrulea defect, găsit de utilizator (07.09.2026)

*„la search dacă scriu miscari nu găsește, că nu are simbolul special."* Şi nu erau rezultate
parţiale: era **zero**, cu ecranul spunând „Niciun rezultat" pentru un cuvânt care se vede în tabel.
Măsurat pe browser cu datele demo: `deseuri` → 0 din 5 rânduri, `hartie` → 0 din 13, `sticla` → 0
din 2, iar în paletă `miscari` şi `evidente` → nimic.

Aplicaţia se foloseşte toată ziua, la introdus date, iar cine scrie repede nu pune diacritice. Deci
căutarea — lucrul adăugat tocmai ca tabelele să scaleze — nu funcţiona pentru felul obişnuit de a
tasta.

`fold()` în `lib/utils`: `NFD` desface litera în literă + semn, `\p{Diacritic}` scoate semnul. Se
aplică **şi** pe textul căutat, **şi** pe ce s-a tastat.

**Trei locuri, nu două.** Al treilea e cel cu urmări asupra datelor, nu doar asupra răbdării:
sugestiile de nume din Parteneri există ca să nu se creeze un partener de două ori. Cine tasta
„deseuri" nu vedea „Transport Deşeuri SRL", deci îl adăuga încă o dată — iar duplicatul rămâne în
nomenclator şi pe documentele tipărite. Celelalte două: `useTableView` (ambele treceri de potrivire)
şi `CommandPalette` (inclusiv scorul, care pe text nepliat ar fi căzut tot pe ultima treaptă şi ar
fi stricat ordonarea în tăcere).

**Pliază în plus cedila peste virgulă** — `deşeuri` şi `deșeuri` devin acelaşi lucru — iar codul şi
datele proiectului le amestecă pe amândouă, cum s-a văzut la auditul de diacritice din aceeaşi zi.

⚠️ **O măsurătoare a mea a fost greşită pe drum, şi merită reţinut de ce.** Prima rulare a raportat
„hârtie → 12", iar după reparaţie 13 — şi era gata să scriu că plierea a găsit un rând în plus.
Măsurat însă comportamentul vechi ca lumea, cu reparaţia scoasă: era 13 şi înainte. Cei 12 erau un
artefact al scriptului de probă, nu un rând. **Plierea nu descoperă rânduri noi**; face doar ca
scrierea fără diacritice să dea acelaşi răspuns.

Proba e în `3-interactiuni.mjs`, verificată că pică fără reparaţie (`0 vs 5`, `0 vs 13`).
**Suită: 60 de verificări verzi.**

---

## Auditul de interfaţă — zece defecte reparate, unul în backend (07.09.2026)

La cererea utilizatorului (*„citeşte toate md files şi după analizează aplicaţia pe fiecare ecran
dacă există buguri sau probleme"*), o citire a întregului frontend ecran cu ecran, apoi
*„fixează toate bugurile"*. Ce a ieşit e mai jos, în ordinea gravităţii. **Îmbunătăţirile de
UI/UX** care au ieşit din aceeaşi citire n-au intrat aici: sunt în `docs/todo-ui-ux.md`, pe ecrane,
cu ordinea de atacat.

### 🔴 Documentele oficiale se puteau tipări dintr-un cache vechi

Singurul defect din backend, şi cel mai scump: `EvidenceCalculator.list()` reconstruia evidenţa
**doar când anul era gol**. Cache-ul populat şi rămas în urmă trecea neatins — iar prin `list()`
trec toate: ecranul, panoul, exportul, şi cele **două documente oficiale**. Deci un client care
înregistra trei predări şi apăsa „Evidenţa gestiunii deşeurilor" primea un PDF fără ele, care arată
perfect valid şi se depune la agenţie. În acelaşi timp, panoul scria verde „nimic nu blochează
depunerea" peste o ieşire fără cod R/D înregistrată cu cinci minute înainte. Singura apărare era
bannerul galben permanent care ruga clientul să ţină minte să apese un buton.

Dosarul de control făcea deja ce trebuie, şi scria de ce (`AuditFileService` regenerează înainte să
împacheteze — decizia 20). Deci nu era o decizie nouă de produs, era **o inconsecvenţă**: două
drumuri către acelaşi PDF, din care numai unul recalcula.

Predicatul e acum „ce s-a schimbat de la ultima scriere": `max(updatedAt)` al mişcărilor faţă de
`min(generatedAt)` al liniilor. Trei lucruri merită reţinute din cum a ieşit:

1. **Interogarea mişcărilor n-are filtru `deletedFalse`.** O ştergere invalidează cache-ul exact ca
   o editare, iar ştergerea e soft: rândul rămâne, cu `updatedAt` împins.
2. **Reconstrucţia nu porneşte de la anul cerut.** Stocul se reportează, deci o corecţie pe o
   mişcare din 2024 lasă **deschiderea lui 2026** greşită, iar a reconstrui 2026 singur l-ar
   reconstrui pe aceeaşi cifră greşită. Porneşte de la cel mai vechi an cu linii mai vechi decât
   schimbarea şi merge până la cel cerut. Se **opreşte** acolo, dinadins: anii de după răspund la
   aceeaşi întrebare când îi deschide cineva. Aia e şi deosebirea de butonul „Regenerează", care
   cascadează înainte — apăsarea lui e o afirmaţie despre tot dosarul, citirea unui an e o întrebare
   despre anul ăla.
3. **`anexa1()` şi `annualDeclaration()` nu mai sunt `readOnly`.** Cheamă `list()` prin
   auto-invocare, deci rebuild-ul rula în tranzacţia lor; sub `readOnly = true` Hibernate trece pe
   `FlushMode.MANUAL` şi liniile reconstruite s-ar fi pierdut la commit — documentul ar fi tipărit
   cifra veche oricum, şi tăcut.

**Probat pe date reale, nu doar la teste:** 777 kg înregistrate fără să se apese „Regenerează" →
ecranul urcă cu 777, fişa Anexa 1 trece de la 35.258 la 39.825 de octeţi; ştergerea mişcării le
scoate înapoi. **228 de teste verzi** (de la 224), patru noi în `EvidenceCalculatorIT`: mişcarea
nouă, ştergerea, corecţia pe un an anterior, şi garda că o citire a unui an neschimbat **nu**
rescrie nimic.

### 🟠 Nouă defecte de interfaţă

| Ce | Unde era |
|---|---|
| **Formularul de firmă nouă moştenea şapte rubrici** de la firma editată înainte — între ele **persoana desemnată**, care se tipăreşte în dosarul de control, şi calitatea care decide ce tabel din Anexa 3 Ambalaje se tipăreşte | `ClientsPage` avea două funcţii de umplere ţinute sincronizate cu mâna, iar cea de adăugare rămăsese în urmă. Acum e una singură, cu `null` = firmă nouă |
| **A doua încercare după un ataşament căzut crea o mişcare duplicat** | `clientGeneratedId` se genera în `buildInput()`, deci alt UUID la fiecare apăsare. Acum e un `useRef`, stabil cât trăieşte dialogul — la ce serveşte cheia |
| **Duplicarea păstra data de descărcare veche** | `initial?.unloadDate` în loc de `editing?.` — deci Anexa 3 ieşea cu încărcarea azi şi descărcarea acum şase luni |
| **Mesajele backendului nu ajungeau la niciuna din cele opt descărcări** | `responseType: "blob"` se aplică şi răspunsului de eroare, deci `apiErrorMessage` citea un `Blob`. „Anexa 3 e formularul pentru nepericuloase…" se afişa ca „Anexa 3 nu a putut fi generată" |
| **`window.prompt` la respingerea unei cereri; aprobarea nu întreba nimic** | Ultimul dialog nativ rămas după ce cele cinci `window.confirm` fuseseră înlocuite. Iar aprobarea creează un tenant real, ireversibil |
| **Ataşamentele nu se puteau adăuga de la tastatură** | `<div onClick>` peste un `<input type="file">` cu `display:none` — singura zonă rămasă aşa după runda de accesibilitate |
| **O adresă greşită dădea ecran alb** | `<Routes>` fără rută `*` nu randează nimic. Plus: login-ul uita unde voiai să ajungi |
| **„Export PDF" învârtea rotiţa pe documentul oficial** | Amândouă foloseau `exporting === "pdf"`. Butoanele anuale se dezactivau şi după filtrul de lună, deşi documentele acoperă anul |
| **Trei formate de dată în acelaşi produs**, şi o coloană care se sorta după altă valoare decât cea afişată | `formatDate` era copiată în patru ecrane şi lipsea din trei; registrul de predări arăta descărcarea şi sorta după încărcare |

Plus trei mărunte din aceeaşi citire: panoul aduna `1000 kg + 1 t = 1001` (mişcarea îşi poartă
unitatea, iar conversia exista doar în motorul de evidenţă); erorile din toast dispăreau în patru
secunde, adică exact mesajele care de-acum poartă o propoziţie utilă; şi secţiunea Anexa 3 Ambalaje
oferea puncte de lucru dezactivate, cu filtrul în afara adresei şi butoanele fără rotiţă.

⚠️ **Bannerul galben permanent de pe Evidenţe a fost schimbat**, şi nu din estetică: „Evidenţa nu se
actualizează singură" devenise o afirmaţie falsă. Un avertisment care e mereu acolo devine tapet în
trei zile — şi atunci nu mai apără nimic exact în ziua în care ar fi trebuit.

### Cum s-a verificat

`tsc --noEmit` curat, `vite build` verde, **suita e2e trece** (rulată cu Chromium-ul lui Playwright:
maşina n-are niciun browser Chromium instalat). Peste ea, **22 de verificări scrise anume pentru
reparaţiile astea**, pe browser adevărat cu datele demo — suita nu le acoperă pe niciuna. Cea mai
instructivă: toastul de la dosarul de control arată acum *„Dosarul se poate genera pentru cel mult 5
ani"*, adică **mesajul corectat la punctul 11 al auditului de conformitate**, pe care până azi nu-l
citise nimeni niciodată.

*(Verificările punctuale n-au intrat în repo: sunt scrise pentru o reparaţie anume, nu pentru o
regulă care trebuie ţinută. Ce merită păstrat din ele a intrat ca test în backend.)*


## Primul contact cu produsul, şi rapoartele care duc undeva (07.09.2026, seara)

Prioritatea 1 şi 2 din lista de îmbunătăţiri de interfaţă, plus un defect de concurenţă găsit în
timpul probelor — singurul din runda asta care atinge date, nu ecrane.

### 1. Formularul public `/cerere-cont`

Singura pagină pe care o vede cineva **înainte** să fie client, şi singura pe care modernizarea din
07.09 n-o atinsese deloc.

- **Cele trei rubrici obligatorii se marchează**, cu asterisc plus un cuvânt citit doar de cititorul
  de ecran — „*" rostit „stea" nu spune nimic. Marcajul a intrat în `Label` ca proprietate, deci e
  disponibil pentru orice formular, nu scris pe loc aici.
- **Erorile stau pe rubrici, nu într-un banner**, cu derulare la prima greşită. E fix defectul nr. 4
  din proba pe browser („bannerul marca nimic"), rămas pe pagina asta după ce s-a reparat pe
  formularul de mişcare: `FieldError` + `invalidProps` + cârligul `data-invalid` existau deja.
- **CUI-ul se verifică aici**, cu aceeaşi formă pe care o cere `CompanyService` la creare. Înainte,
  un CUI stricat trecea de formular şi cădea abia la aprobare, în mâinile altcuiva.
- **Pagina are identitate** — cele două rânduri de brand ale cardului de login — şi un rând de trei
  paşi în cap, care răspunde la „merită să încep?" **înainte** de prima rubrică, nu după şase
  secţiuni.
- **Cele 28 de bife R/D s-au pliat**, în spatele unei alegeri a cărei primă opţiune e „Nu știu — le
  stabilim împreună". Setul gol era deja răspunsul „nu s-a răspuns" (decizia 6); ce s-a schimbat e
  că formularul o spune. Trecerea pe „nu ştiu" **goleşte** ce s-a bifat, altfel ar rămâne în urmă
  răspunsuri pe care omul crede că le-a retras.
- **Pagina de mulţumire spune ce urmează**, cu emailul numit şi cu termenul de 1–2 zile lucrătoare.
  „Am primit cererea" răspunde la ce s-a întâmplat; omul tocmai a dat datele firmei lui unui site pe
  care nu-l cunoaşte.
- 🔒 **Honeypot** pe singurul endpoint public de scriere. Rubrica e ascunsă de ochi, de Tab şi de
  `aria-hidden`, deci un om n-o poate completa nici din greşeală. Backendul **nu scrie nimic şi
  răspunde tot 202**: un refuz vizibil i-ar spune botului ce câmp să evite data viitoare. Un test
  ţine ambele jumătăţi, inclusiv că gol nu se citeşte ca verdict.

### 2. Inboxul de cereri — jumătatea care nu se citea

Tabelul arăta şapte coloane dintr-un formular cu douăzeci de rubrici. **Opt câmpuri completate nu se
citeau de nicăieri**, între ele `notes` — chiar rubrica de text liber în care omul scrie ce nu încape
în celelalte —, deşi exact cine creează firma din cerere are nevoie de ele.

- **„Vezi cererea"**, pe orice rând indiferent de stare: cererea întreagă, în ordinea secţiunilor pe
  care le-a parcurs clientul. O rubrică necompletată **se arată goală**, nu se sare — cine creează
  firma trebuie să vadă că lipseşte, nu să deducă asta. O secţiune fără niciun răspuns se pliază la
  un rând, fiindcă acolo absenţa e chiar informaţia.
- **„Vezi firma creată"** pe rândurile aprobate. Rândul devenea „Cont creat" şi nu ducea nicăieri,
  deşi firma stă în tabelul de deasupra — putea fi al treizecilea rând sau pe altă pagină a listei.

### 3. Badge-ul roşu nu mai e fund de sac

„Fără cod R/D" e informaţia cea mai importantă din aplicaţie şi se vedea pe trei ecrane din care nu
se putea face nimic.

- **`/miscari?luna=…&miscare=…`** deschide mişcarea cerută direct în formularul de editare.
  Parametrul se **consumă** la deschidere: lăsat în adresă, un refresh ar redeschide dialogul peste
  ce lucrezi. ⚠️ Linkul poartă luna lui **`date`**, nu a datei afişate în registru — ecranul Mişcări
  filtrează pe `date`, iar registrul arată `unloadDate` când o are, deci o predare din 31 martie
  descărcată pe 2 aprilie s-ar căuta în aprilie şi n-ar fi găsită.
- **Registrul de predări** primeşte „Completează codul" pe rândul roşu — şi numai pe el. Cel amber
  („De cântărit") e o aşteptare legitimă, n-are ce repara nimeni azi.
- **Filtrul `?problema=cod-rd`** pe registrul de predări, cu bannerul care spune că e pus şi butonul
  care îl scoate. Un filtru venit din altă parte trebuie să se vadă, altfel tabelul pare gol pe
  nedrept şi omul caută rânduri care există.
- **Panoul duce acum unde promitea.** Linkul blocajului roşu mergea la vederea lunară, unde rândul e
  un **agregat** pe (punct de lucru, cod, lună) şi nu se poate deschide nicio mişcare. Duce la
  registrul filtrat, unde un rând **este** o mişcare.
- **Vederea lunară** face acelaşi drum prin badge-ul ei, restrâns la luna şi punctul de lucru ale
  rândului. **Registrul de ambalaje** primeşte coloană de acţiuni, cu condiţia ţinută dinadins
  identică cu cea care colorează rândul: dacă tabelul semnalează ceva se poate apăsa, dacă nu, nu
  apare niciun buton care să sugereze că ar fi.
- `LinkButton` e primitivă nouă: o navigare scrisă ca `<button onClick={navigate}>` nu se deschide
  în filă nouă şi nu spune cititorului de ecran că duce altundeva. Rapoartele sunt exact cazul în
  care omul vrea a doua filă — repară acolo, se întoarce la listă aici.

### 4. 🔴 Un defect de concurenţă în decizia 50, găsit de probe

Nu era pe nicio listă. A ieşit fiindcă probele au făcut cache-ul de evidenţă să rămână în urmă —
starea pe care decizia 50 o repară — iar apoi două ecrane au cerut acelaşi an deodată:

```
[HTTP 409] GET /api/v1/evidences?year=2026
Concurrent update conflict: Row was updated or deleted by another transaction
  : [MonthlyEvidence#7a497ff4-…]
```

**Ce se întâmplase.** Decizia 50 a făcut din citire o **scriere**: `list()` reconstruieşte anul când
mişcările s-au mişcat de la ultima scriere. Reparaţia era corectă — se putea depune o fişă fără
mişcările de ieri — dar a deschis o cursă pe care varianta veche aproape n-o avea, fiindcă
reconstruia doar când anul era **gol**. `deleteByCompany_IdAndYear` încarcă rândurile înainte să le
şteargă, deci a doua citire ştergea rânduri pe care prima le înlocuise deja.

**Nu e teoretic:** Panoul şi ecranul Evidenţe cer amândouă anul curent. Două taburi, sau o navigare
rapidă, sunt destul. Nu se pierde nimic din date — tranzacţia cade întreagă — dar clientul vede un
raport care nu se deschide.

**Reparaţia**, fără migrare: o secţiune critică per (firmă, an), cu **lacăt consultativ** Postgres
(`pg_advisory_xact_lock`) şi **verificare dublă**. Nu un lacăt pe rând — nu există rând care să
însemne „anul din cache", fiindcă reconstrucţia le şterge pe toate; nu unul pe firmă — ar serializa
ani fără legătură şi ar bloca editările simple de firmă. Prima verificare, fără lacăt, ţine drumul
obişnuit (cache la zi, cazul de aproape fiecare dată) la două agregate; a doua, sub lacăt, face ca
aşteptarea la uşă să nu coste şi o reconstrucţie în plus. Postgres eliberează lacătul la commit sau
rollback, deci o reconstrucţie care aruncă nu-l poate lăsa ţinut. Acelaşi lacăt s-a pus şi pe
`regenerateYear`: butonul şi citirea rescriu aceleaşi rânduri, deci sunt aceeaşi secţiune critică.

**Proba a fost scrisă întâi şi verificată că pică** — patru cititori pe un an învechit, cu aceeaşi
excepţie ca în producţie. Nu doi: cu doi, fereastra se poate rata pe o maşină rapidă.

### 5. Datoria care bloca probele, plătită

Seed-ul demo n-avea **niciun** rând în stările pentru care ecranele au reguli proprii — zero din 34
de mişcări fără cantitate, zero ieşiri fără cod R/D, zero parteneri fără dată de autorizaţie. De asta
al treilea defect din primitivele reparate pe 07.09 a scăpat şi de suită: verificarea scrisă pentru
el trecea pe un tabel care n-avea rândul, şi a fost **scoasă** în loc să fie lăsată să treacă pe gol.

`DevDataSeeder` are acum toate trei, aşezate pe plastic la Turda dinadins — seria de hârtie de la
Cluj (40→30→50→50→30→50.5) e vitrina stocului cumulativ şi rămâne neatinsă. `ApplicationBootIT` le
numără **pe nume**, nu doar în total: un total care creşte nu spune că stările există.

### 6. Ce a ieşit uitându-mă la capturi, cu toate verificările verzi

Suita trecea, `tsc` era curat. Deschiderea capturilor a scos două lucruri:

- **Coloana de acţiuni a inboxului de cereri se rupea pe două rânduri.** Al treilea buton
  („Vezi cererea", adăugat mai sus) le frângea pe toate — „Vezi / cererea", „Creează / contul" —,
  rândurile creşteau în înălţime şi etichetele se citeau greu. `whitespace-nowrap` pe toate trei;
  coloana e `sticky="right"`, deci poate fi mai lată fără să iasă din îndemână.
- ⚠️ **Prima reparaţie a fost mai proastă decât defectul.** Am încercat o pictogramă învelită în
  `Tooltip` — care îşi randează **propriul `<button>`**, deci ieşea buton în buton, HTML invalid.
  React o spunea în consolă, iar suita citeşte consola, deci a picat pe loc. Nota a rămas în cod, ca
  să nu se reîncerce. **`Tooltip` nu poate înveli nimic interactiv**, atât.

Şi o gaură în ce se proba, nu în cod: **suita de ecran îngust umbla doar pe cele patru ecrane de
după autentificare**, deci `/cerere-cont` — rescris cap-coadă chiar azi — n-a fost privit niciodată
la 375px. Intră acum în ea, cu verificarea că banda de trei paşi (`sm:grid-cols-3`) chiar se
stivuieşte pe telefon.

E a treia oară când regula de lucru 5 („randează şi uită-te") prinde ce testele nu pot — vezi şi
rândul TOTAL AN dispărut, şi steluţa de lângă cod de la G6. De data asta nu era un document
tipărit, ci un ecran: **regula e mai largă decât credeam când s-a scris.**

### Cifrele

- **230 de teste verzi** (de la 228): honeypot-ul, concurenţa evidenţei, cele două stări din seed.
  Migrări tot până la **`V31`** — reparaţia de concurenţă n-a cerut niciuna, şi ăsta era criteriul.
- **Suita de interfaţă: 6 probe, 99 de verificări** (de la 5 şi 60). Proba nouă,
  `6-cerere-si-rapoarte.mjs`, acoperă formularul public, inboxul şi drumul cap-coadă de la blocajul
  de pe Panou până la mişcarea deschisă. ⚠️ **Scrie o cerere în baza de dev la fiecare rulare** şi
  nu curăţă după ea — aprobarea ar crea o firmă, iar firmele nu se şterg.
- `tsc --noEmit` curat, `vite build` verde.
- ⚠️ **Tot nedeployat**, ca toată ramura `ui-ux-modernizare`.

### Ce s-a găsit pe drum şi n-a fost reparat

**`MovementsPage.tsx:360` foloseşte `<input type="month">`, pe care Safari nu-l implementează.** Pe
Mac devine câmp text liber: filtrul principal al celui mai folosit ecran n-are selector, n-are
validare, şi cere tastat `2026-06` exact. E notat la Prioritatea 3 în `todo-ui-ux.md`, şi acolo e
prea jos — nu e cizelare, e o funcţie ruptă pe o familie întreagă de browsere, chiar pe maşina de
dezvoltare. Recomandarea, scrisă şi în lista de îmbunătăţiri: se ia împreună cu al doilea punct de
la Mişcări (fără filtru de lună se aduc **toate** mişcările, oricâte), fiindcă e aceeaşi reparaţie
pe aceleaşi trei linii.

## Filtrul de lună, datele firmei şi dezactivarea care se poate lua înapoi (07.09.2026, noaptea)

Trei felii cerute în ordinea din `todo-ui-ux.md`, după ce lista fusese rearanjată: felia „următoare"
(filtrul de lună), Prioritatea 2 (garda de formular + datele firmei) şi ce urma după ea (plasa de
sub excepţii, reactivarea, formularul de firmă). **Prima e singura care repară ceva rupt; restul
sunt lucruri care lipseau.**

### 1. Filtrul de lună de pe Mişcări — o funcţie ruptă pe Safari şi Firefox

`<input type="month">` **nu există în Safari şi Firefox**: degenerează în câmp text liber. Adică pe
Mac — chiar maşina de dezvoltare — filtrul principal al celui mai folosit ecran n-avea selector,
n-avea validare şi cerea tastat `2026-06` exact, fără să spună asta nicăieri.

În loc: `MonthInput`, două `<select>` native (luna, apoi anul — cum se citeşte în româneşte).
Native dinadins: tastatura, cititorul de ecran şi selectorul de pe telefon vin gata făcute, ceea ce
un calendar scris de mână ar fi trebuit să refacă.

Odată cu el, **a doua jumătate a aceleiaşi reparaţii**: ecranul porneşte pe **luna curentă**. Fără
lună se aduceau toate mişcările firmei, oricâte — la doi ani × 30 de predări pe lună sunt ~700 de
rânduri la fiecare deschidere, iar `useTableView` paginează abia **după** ce au venit, deci
paginarea nu apăra nimic. Luna implicită nu se scrie în adresă, deci `/miscari` rămâne un link
curat care înseamnă „luna asta", iar `?luna=2026-03` continuă să însemne o lună anume — linkurile
vechi din rapoarte deschid exact ce deschideau.

**Treapta de mijloc, şi de ce a cerut backend.** „Tot anul" (`?luna=2026`) există fiindcă bara de
căutare a tabelului caută în ce s-a **adus**: fără ea, o predare de acum trei luni s-ar găsi numai
nimerindu-i luna din prima. Numai că `year` fără `month` nu însemna nimic în backend — se cerea
`?year=2026` şi veneau înapoi toate mişcările, din toţi anii. **O filtrare ignorată în tăcere e mai
rea decât una respinsă**, aşa că `WasteMovementService.list` o interpretează acum: an fără lună =
anul întreg. Trei teste noi (`MovementListFilterIT`), fără migrare.

Şi ieşirea din luna goală: pe o lună fără rânduri, tabelul spune **„Nicio mişcare în Februarie
2019"** şi oferă butonul „Vezi tot anul 2019". Fără el, un ecran care porneşte pe luna curentă ar
arăta „nicio mişcare" unui client care are şapte sute, iar nimic de pe ecran n-ar spune că vina e a
filtrului, nu a datelor.

### 2. Garda de la închiderea formularului de mişcare

Escape sau un clic pe fundal ştergeau treizeci de rubrici din opt secţiuni, fără o vorbă. E
jumătatea cealaltă a defectului reparat la Escape-ul din combobox: acolo se pierdea tot fiindcă
tasta trecea prin listă până la dialog, aici se pierdea fiindcă dialogul făcea exact ce i se cerea.

Formularul „atins" se marchează din `onChange`-ul **formularului**, nu din cele treizeci de
`setState`: evenimentul urcă din orice rubrică nativă, deci o rubrică adăugată mâine intră singură
sub gardă. Cele două căi care nu trec prin el — alegerea unui cod din listă şi fişierele lăsate cu
mausul peste zonă — marchează pe faţă.

**Şi o reparaţie în primitivă, cerută de asta:** `Dialog` ţine acum un **teanc** al dialogurilor
deschise, iar la Escape şi la capcana de Tab răspunde doar cel de deasupra. Fără el, întrebarea
„închizi fără să salvezi?" stând peste formular s-ar fi închis odată cu formularul la o singură
apăsare de Escape — adică exact paguba de care întreabă. `confirm-dialog.tsx` ocolea până acum
aceeaşi problemă închizându-se înainte de a lansa acţiunea.

### 3. Datele firmei, în Setări

Un ADMIN de firmă nu-şi vedea nicăieri CAEN-ul, autorizaţia de mediu, persoana desemnată sau seria
Anexei 3 — toate se editează **exclusiv** din „Clienţi", care e ecran de `PLATFORM_ADMIN`. Intra în
„Setări", singurul loc unde s-ar fi uitat, şi găsea puncte de lucru, secţii şi şoferi.

`CompanyDetailsSection`, **numai citire**: cine poate schimba rubricile rămâne cine era; ce se
schimbă e că se **văd** — inclusiv golurile, fiindcă un CAEN necompletat se tipăreşte gol pe
declaraţia anuală, iar asta se află mai bine aici decât din documentul depus. Nota de subsol nu
trimite la o adresă de e-mail: aplicaţia n-are nicăieri una, iar una inventată aici ar fi prima care
se dovedeşte falsă — trimite la consultantul care a deschis contul.

Etichetele rubricilor şi titlurile grupelor se citesc din `strings.clients`, adică din ecranul unde
se **editează** aceleaşi rubrici: două nume pentru „Nr. Registrul Comerţului" ar fi două nume pentru
acelaşi lucru. Excepţie fac două, care în formular sunt etichete de bifă („Datorează ceva la AFM,
dar…") şi aici trebuie să stea singure deasupra unui răspuns.

Plus `SectionNav`, cuprinsul lipicios al paginii — patru secţiuni, trei dintre ele tabele cu
paginare. **Trei defecte ale lui s-au văzut abia pe captură**, cu toate verificările de DOM verzi:
`overflow-x-auto` pe `<nav>` decupa şi pe verticală, deci banda care acoperă căptuşeala paginii nu
se vedea şi pe sub bară trecea o dungă de tabel; coloana de acţiuni a tabelelor e şi ea lipită
(`sticky right-0 z-10`) şi vine **după** bară în DOM, deci la z egal acoperea jumătatea din dreapta;
iar prima variantă marca secţiunea curentă cu `IntersectionObserver`, care la capătul de jos al
paginii nu vede niciodată ultima secţiune — clicul pe „Şoferii noştri" ducea acolo lăsând marcajul
pe „Puncte de lucru". Regula 5, încă o dată: randează şi uită-te la el.

### 4. Plasa de sub excepţii

Orice excepţie de randare demonta tot arborele şi lăsa un `<div id="root">` gol: ecran alb, fără
meniu, fără mesaj, fără drum înapoi, pe **orice** ecran, pentru un câmp null pe care nu-l aştepta
nimeni. Cel mai ieftin defect de reparat şi cel mai scump de trăit — omul n-are ce povesti la
telefon în afară de „s-a albit".

`ErrorBoundary` stă în două locuri, fiindcă apără de două lucruri: în jurul paginii, **sub**
`Layout`, unde meniul rămâne viu şi se poate merge în altă parte fără reîncărcare (cheia e adresa,
deci plecarea de pe ecranul căzut şterge mesajul); şi în jurul aplicaţiei întregi, pentru ce cade în
`Layout` sau în context.

### 5. Dezactivarea se poate lua înapoi

Prima greşeală era definitivă la **punct de lucru, partener, secţie şi şofer**. Dezactivarea nu
şterge niciun rând — dinadins, fiindcă mişcările vechi îl citează — deci n-avea de ce să fie
ireversibilă; pur şi simplu nu exista drumul înapoi.

`POST /{id}/reactivate` pe toate patru (ca `/{id}/reopen` de la termene: e o faptă, nu o resursă),
cu aceleaşi verificări de tenant şi aceleaşi reguli de rol ca dezactivarea. Nicio unicitate nu se
poate strica: singura care există — numele secţiei într-un punct de lucru — numără şi rândurile
inactive, deci un nume liber azi n-a fost al nimănui. Şase teste (`ReactivationIT`), pe toate patru
resursele, fiindcă **simetria e chiar lucrul care se poate strica**: fiecare are propriul controller
şi propriul serviciu, iar una uitată ar arăta pe ecran ca un buton care nu face nimic.

Pe ecran: butonul „Reactivează" pe rândul inactiv, şi **filtrul activ/inactiv** cerut de mult —
după un an de folosire, „Parteneri" e un cimitir prin care se caută. Porneşte pe **Active**, cu
numărul celor scoase chiar în opţiune, şi **nu apare deloc** cât timp n-a fost dezactivat nimic: pe
un cont nou ar fi un comutator între „tot" şi „tot".

### 6. Formularul de firmă

Al doilea ca mărime din aplicaţie — ~25 de rubrici, inclusiv profilul şi blocul persoanei desemnate
— şi singurul rămas la 512px după modernizare. Acum `xl`, cu cinci secţiuni titrate: aceleaşi
grupe, în aceeaşi ordine, ca vederea în citire din „Setări". Rubricile nu s-au schimbat, doar
aşezarea.

### Cifre

- **239 de teste verzi** (de la 230): `ReactivationIT` (6) şi `MovementListFilterIT` (3). Migrări
  tot până la **`V31`**, următoarea liberă **`V32`** — nicio felie n-a cerut una.
- **Suita de interfaţă: 7 probe, 127 de verificări** (de la 6 şi 99). Proba nouă,
  `7-firma-si-reactivare.mjs`, face drumul întreg al reactivării (creează un şofer, îl dezactivează,
  îl regăseşte prin filtru, îl reactivează) şi probează garda formularului, inclusiv că Escape peste
  întrebare închide **doar** întrebarea. ⚠️ Lasă în urmă un şofer dezactivat de probă.
- `tsc --noEmit` curat, `vite build` verde.
- ⚠️ **Tot nedeployat**, ca toată ramura `ui-ux-modernizare`.

## Atașamentele, partenerul, cuprinsul și paleta (08.09.2026)

Cele patru felii din capul secțiunii „Ordinea recomandată" a lui `todo-ui-ux.md`, luate în ordine.
Niciuna nu repară ceva rupt — sunt lucruri care lipseau — **dar a cincea a ieșit din ele**: proba
scrisă pentru paletă a scos un defect de reconciliere care ascundea rânduri moarte în listă.

### 1. Atașamentele se văd fără să deschizi editarea

Coloana arăta „📎 2" și atât. Ca să afli *ce* document e acolo trebuia deschis formularul de
editare, cu treizeci de rubrici — iar un **VIEWER nu-l poate deschide deloc**, fiindcă butonul
„Editează" stă sub `canWrite`. Adică pe rolul care există tocmai ca să citească, avizul urcat lângă
o predare era o cifră, nu un document.

Cifra e de-acum un buton care deschide o vedere cu numele fiecărui fișier și un link către el.
**Numai citire, dinadins:** ștergerea rămâne în formular, lângă urcare, unde e și confirmarea și
regula de rol. Un coș de gunoi într-o vedere deschisă de oriunde ar fi cea mai ușoară apăsare
greșită din ecran.

Două amănunte care se pot strica la următoarea atingere: dialogul ține **`id`-ul** mișcării, nu
obiectul — lista se reîmprospătează sub el, iar un instantaneu ar arăta fișierul care tocmai a
plecat; și nu e `Tooltip`, fiindcă bula își randează propriul `<button>` și n-ar putea înveli
linkuri (defectul din 07.09, seara).

### 2. Formularul de partener, pe cinci secțiuni

Era o coloană de cincisprezece blocuri într-un dialog de 512px, deși are secțiuni evidente. Acum
`xl`, cu **Identificare · Ce face partenerul · Transport · Autorizația de mediu · Date pentru
Anexa 3** — `FormSection` era deja folosit în trei locuri, deci n-a fost nimic de inventat.

**Ordinea s-a schimbat într-un singur loc, și merită spus de ce:** CUI-ul stătea între bifa de
transportator și autorizație, adică o identificare ruptă în două de o întrebare despre camioane. A
urcat lângă denumire. Restul rubricilor sunt exact unde erau.

Trei lucruri mărunte, luate în aceeași felie fiindcă erau în același ecran:
- comentariul „Transportatorul e o bifă, nu un tip" stătea deasupra blocului de **ambalaje** — se
  desprinsese de codul lui la o mutare anterioară. A fost pus înapoi peste bifă.
- `typeNoneHint` spunea „se poate alege numai cu bifa **de mai sus**", iar bifa era dedesubt de
  când s-a livrat `V28`. Acum numește rubrica, nu direcția — un text care spune „mai sus" se strică
  la fiecare rearanjare.
- rândurile de șofer și de punct de lucru erau `flex` fix: trei câmpuri și un buton strânse la ~70px
  pe telefon. Se stivuiesc sub `sm`.

### 3. Cuprinsul de pe Ambalaje

Patru tabele mari unul sub altul plus grila de 66 de celule: cea mai lungă pagină din aplicație, și
singura fără nimic care să te ducă între ele. `SectionNav` fusese scris **ca primitivă chiar pentru
ea** pe 07.09 și era folosit doar în „Setări".

Bara stă deasupra casetei chihlimbarii cu ce blochează declarația, nu sub ea: caseta apare și
dispare după cum e completată luna, iar un cuprins care sare cu ea ar fi altă bară la fiecare
deschidere.

Cele trei defecte de lipire ale primitivei (banda decupată, coloana de acțiuni care o acoperea,
poziția numărată de la marginea conținutului) nu s-au repetat — dar proba le măsoară acum și aici,
cu `elementFromPoint` în trei puncte pe bară, fiindcă toate trei au trecut de verificările de DOM
prima oară.

### 4. Ctrl+K găsește documente, și pornește ceva

`Command.keywords` era declarat, **citit la potrivire**, și niciodată completat de nimeni: o funcție
întreagă, verde la compilare, moartă la rulare. Tastai „fișa" sau „anexa 1" și paleta nu găsea
nimic, deși propriul docstring promitea „unde vreau să ajung, ce vreau să încep".

Cuvintele stau în bara laterală, lângă ecranul pe care îl descriu — nu într-o a doua listă a
paletei, care ar fi ajuns să nu mai spună același lucru. Sunt numele **documentelor** și vorbele
clientului, nu sinonime: „fișa", „anexa 1", „HG 856/2002", „25 februarie", „inspector".

⚠️ **„Anexa 1" duce la două ecrane, dinadins.** Numele scurt înseamnă chiar două documente
(decizia 12): declarația de ambalaje la Ambalaje, fișa din HG 856/2002 la Evidențe. A alege unul
ca „adevăratul" ar ascunde celălalt document exact de cine îl caută pe nume.

Și jumătatea cealaltă a promisiunii: două comenzi care **încep** ceva — „Adaugă mișcare", „Adaugă
partener" — fiecare ducând pe ecranul ei cu formularul deschis, prin `?nou=1` care se consumă la
deschidere, ca `?miscare=`.

**„Regenerează" nu e printre ele, și n-a fost o scăpare.** Butonul de pe Evidențe e o cerere
explicită de recalculare pe un an anume (decizia 51), iar dintr-o paletă nu se vede pe care an ar
cădea. O comandă care rescrie tăcut liniile unui dosar e exact genul de ghicit pe care ecranul ăsta
nu-l face. Proba verifică și asta: `check("nicio comandă nu recalculează un dosar din paletă")`.

### 🔴 Al cincilea: rânduri moarte în paletă, găsite de propria probă

Prima rulare a probei a raportat pentru „anexa" rezultate care **n-au cuvântul nicăieri** — „Dosar
de control", „Parteneri". Citit repede, arăta ca o potrivire prea largă: cuvintele-cheie noi ar fi
fost de vină. Nu erau.

Sortarea pe scor amestecă grupurile între ele. Randarea deschide un `<div>` nou la fiecare
schimbare de grup, iar cheia lui era **numele grupului** — deci la „anexa" ieșeau grupurile
`Evidență · Raportare · Evidență`, adică **două surori cu aceeași cheie**. React nu mai putea
reconcilia, și în listă rămâneau rânduri din randarea dinainte, cu `data-index` duplicat. Se putea
apăsa pe ele.

Reparat în două locuri, fiindcă sunt două probleme:
- **grupurile rămân întregi**, în ordinea celui mai bun membru al fiecăruia (`Map`, care ține
  ordinea inserării, peste lista deja sortată). Primul rând al listei rămâne cea mai bună potrivire
  — proprietatea de care atârnă Enter — fără ca antetul unui grup să apară de două ori;
- cheia `<div>`-ului e `id`-ul primei comenzi, unic prin construcție, ca apărare dacă vreodată
  ordonarea rupe iar grupurile.

⚠️ Defectul e **mai vechi decât felia de azi** — exista de când s-a scris scorul, pe 07.09. Până
acum nu se vedea fiindcă fără cuvinte-cheie potrivirile cădeau aproape mereu în același grup.
Cuvintele n-au stricat nimic: au făcut ca un drum rar să devină cel obișnuit.

**Cum s-a prins, și de ce merită reținut:** nu din cod, și nici din captură — pe captură arăta doar
ca „prea multe rezultate". S-a prins **numărând**: verificarea nu era „găsește Ambalaje și
Evidențe" (care trecea, din motivul greșit), ci `check("și nimic altceva", rezultate.length === 2)`.
O probă care întreabă doar dacă ce trebuie e acolo nu poate spune că mai e și altceva.

### Datoria de seed, a doua tranșă

Coloana „📎" avea **zero rânduri din 36**, deci vederea de la punctul 1 s-ar fi probat pe gol —
exact felul de gol în care s-a ascuns al treilea defect din primitive pe 07.09. `DevDataSeeder` are
acum două atașamente pe **ieșirea fără cod R/D**: e rândul pe care îl deschide inspectorul, iar
avizul lui e chiar hârtia după care întreabă. Două, nu unul, ca să se vadă dacă lista chiar le
enumeră. `ApplicationBootIT` le numără pe nume, ca pe celelalte trei stări.

⚠️ **Nu s-a urcat nimic.** `CLOUDINARY_URL` nu e setat nici local, nici pe dyno, deci în dev nu
există cale de a crea un atașament prin aplicație. URL-urile arată către cloud-ul public `demo` al
Cloudinary — se deschid, dar nu sunt documentele firmei. Şi, ca la celelalte, **baza de dev nu se
re-seedează singură**: pe o bază veche rândurile se adaugă cu un `INSERT` aditiv, scris în
`frontend/e2e/README.md`.

### Starea

- **239 de teste verzi**, neschimbat ca număr: verificarea de atașamente a intrat în metoda
  existentă din `ApplicationBootIT`. Migrări tot până la **`V31`**, următoarea liberă **`V32`** —
  nicio felie n-a cerut una, nici seed-ul.
- **Suita de interfaţă: 8 probe, 161 de verificări** (de la 7 şi 127). Proba nouă,
  `8-atasamente-partener-paleta.mjs`, acoperă toate cele patru felii şi e cea care a găsit defectul
  paletei.
- `tsc --noEmit` curat, `vite build` verde.

### Ramura a intrat în `main` (08.09.2026)

`ui-ux-modernizare` a stat afară din `main` de pe 07.09, cât timp aştepta să fie **privită cu ochiul
de cineva**. Decizia utilizatorului din 08.09 e că poate intra direct, aşa că cele 41 de commituri
s-au mers cu `--no-ff` şi s-au împins.

- `main` == `origin/main` == `origin/deploy/heroku-split` la **`4c5b8dc`** (merge commit).
  Ultimele două de pe ramură: `8fb2988` codul, `dcfe439` documentaţia.
- Verificat **după merge**, pe `main`, nu doar pe ramură: 239 de teste verzi, `tsc --noEmit` curat,
  `vite build` verde.
### Şi a ajuns în producţie (08.09.2026, ora 12:08)

Modernizarea interfeţei a stat nedeployată de pe 07.09. Cu merge-ul făcut, procedura de split a
rulat cap-coadă, prima oară pe maşina asta:

- `newrepo` şi `ferepo` **nu existau ca remote-uri** — s-au adăugat. Procedura din handoff
  presupunea că sunt acolo; acum sunt, deci data viitoare rulează ca scrisă.
- `git subtree split` pe fiecare prefix, apoi cherry-pick peste capul fiecărui remote: **5 commituri
  pe backend** (`99b8227..`) şi **31 pe frontend** (`ca3cf87..`). **Niciun conflict**, deşi
  handoff-ul avertiza — vezi mai jos de ce.
- `newrepo/main` la **`b5dd344`**, `ferepo/main` la **`2fac378`**.
- **`ecoregistru-api` v36 → v37**, **`ecoregistru-app` v29 → v30**, amândouă verificate în
  `heroku releases` cu hash-ul care trebuie.
- **Schema rămâne la 31**: `Current version of schema "public": 31`, nicio migrare de rulat, iar
  aplicaţia a pornit în 12,1 secunde fără erori.
- Verificat pe conţinut, nu pe hash: bundle-ul servit de producţie **conţine** şirurile feliilor de
  azi — „Vezi ataşamentele", „Atașamentele mișcării", „Ce face partenerul", „Date pentru Anexa 3" —
  şi ale celor de ieri („Tot anul"). `/` întoarce 200, iar API-ul răspunde.

⚠️ **Hash-ul bundle-ului din producţie nu se potriveşte cu cel construit local**, şi nu e un semn
rău: monorepo-ul are **`vite.config.js` şi `vite.config.d.ts` urmărite** — artefacte compilate din
`vite.config.ts` — iar repo-ul de frontend are doar `.ts`-ul. Vite caută `.js` **înaintea** lui
`.ts`, deci local se citeşte artefactul, în producţie sursa. Azi au acelaşi conţinut, deci nu
schimbă nimic; **dar o modificare viitoare în `vite.config.ts` n-ar avea efect local** până se
recompilează artefactul. E acelaşi tip de capcană ca `tsconfig.node.tsbuildinfo`, care a fost scos
din urmărire pe 24.08 exact din motivul ăsta. Aici mai explică şi de ce cele două repo-uri diferă cu
`.gitignore` + cele două fişiere: **divergenţa e veche şi stabilă**, nu ceva ce s-a stricat azi, şi
tocmai fiindcă niciun commit din interval nu le atinge, cherry-pick-ul n-a avut conflicte.

**Două verificări scrise greşit, corectate după măsurătoare** — se scriu aici fiindcă amândouă
păreau defecte ale aplicaţiei: (a) clicul pe „Tabelul 2" nu duce titlul sus, fiindcă ultimele două
secţiuni intră amândouă în ultimul ecran şi pagina se termină înaintea lor — e chiar ramura „la
fund" a primitivei, scrisă pe 07.09 după o captură; proba se face pe „Tabelul 1". (b) Cifra din
capul unei probe nu spune nimic dacă tabelul n-are rândul — vezi datoria de seed de mai sus.

## Rubrica cerută cui i se aplică, grila care spune că a salvat, stocul pe coduri (08.09.2026, după-amiaza)

Cele cinci felii din capul secţiunii „Ordinea recomandată" a lui `todo-ui-ux.md`, luate în ordine.
Niciuna nu repară ceva rupt — dar patru din cinci schimbă o **afirmaţie** pe care ecranul o făcea
fără să aibă cum s-o susţină, iar una singură (grila) a scos la iveală şi o pierdere tăcută de date.

### 1. Provenienţa ambalajelor se cere numai cui i se aplică

Rubrica de pe partener alimentează Anexa 3 Ambalaje (Ordinul 794/2012, art. 4), pe care o depun
colectorii, comercianţii, reciclatorii şi valorificatorii. Un **generator pur** n-o depune
niciodată, deci întrebarea „ce e partenerul ăsta faţă de ambalajele pe care ţi le aduce" n-avea
pentru el niciun răspuns — şi totuşi i se punea, pe formularul pe care îl deschide cel mai des.

Se restrânge după **tipul contului**, exact ca provenienţa deşeului de pe mişcare (decizia 23).
⚠️ **Şi nu contrazice decizia 6** („profil gol = fără restricţie"): nu restrângem pe un profil
neîntrebat, ci pe un răspuns care nu poate lipsi — tipul firmei se alege la deschiderea contului.
Un partener care **are** deja o provenienţă scrisă o vede oricum, ca să nu dispară tăcut o valoare
scrisă cândva.

### 2. Grila de 66 de celule spune ce a salvat — şi nu mai pierde ce s-a tastat între timp

Se salvează singură, un rând odată, la ieşirea din celulă. Până azi se vedeau **numai erorile**: o
salvare reuşită nu spunea nimic, deci cine completa şaizeci şi şase de cifre n-avea de unde şti
câte au ajuns. Rândul are de-acum trei stări — „nesalvat", „se salvează…", „salvat" — plus o
numărătoare sub grilă pentru rândul derulat afară din ochi.

🔴 **Şi, scriind starea, a ieşit o pierdere tăcută de date.** `onSuccess` ştergea **ciorna rândului
întreg**, nu ce trimisese: cine tasta într-o a doua celulă cât zbura salvarea primea, la
reîmprospătarea de după răspuns, cifra veche a serverului peste ce tocmai scrisese. Acum se şterge
din ciornă numai ce s-a trimis **şi n-a fost tastat între timp** — restul rămâne „nesalvat" şi
pleacă la ieşirea din celula lui. Defectul era vechi de la `V22`; l-a scos la iveală chiar
întrebarea „ce are rândul ăsta de spus despre el însuşi".

Starea e **per material**, nu pe mutaţie: `saveMut.isPending` e unul singur pentru toată grila şi
ar fi aprins toate cele opt rânduri la fiecare salvare.

### 3. Panoul numără coduri, nu adună kilograme peste ele

„Stoc la ultima lună calculată" era suma închiderilor peste **toate** codurile — hârtie plus ulei
uzat plus menajer, o cifră care nu există fizic nicăieri. Mai rău: un stoc **negativ** pe un cod —
ieşiri neacoperite, exact ce nu se poate depune — se scădea din pozitivele celorlalte şi dispărea
din ochi.

Dala numără de-acum **codurile cu stoc** şi le numeşte pe primele trei, cu kilogramele fiecăruia.
Negativele stau primele şi sunt roşii, oricât de mici — ordonarea doar după mărime ar fi ascuns
un −1 exact când e singurul lucru de văzut. Stocul unei perechi (punct de lucru, cod) e închiderea
**ultimei ei luni calculate**, nu a ultimei luni din tot setul: un maxim luat peste tot ar sări
perechea care se termină mai devreme.

### 4. Termenele spun câte zile mai sunt, şi duc la documentul care le stinge

Panoul socotea zilele de mult (`daysUntil`); tabelul lăsa clientul s-o facă în cap. Acum fiecare
rând nefinalizat o spune, iar depăşirea se scrie ca depăşire („depăşit de 177 zile", roşu), nu ca
aşteptare.

Şi termenul duce la document: 15 martie → **Evidenţe**, 25 februarie → **Ambalaje**, amândouă pe
**anul raportat**, adică anul precedent celui în care se depune (OUG 92/2021 art. 48 alin. (1);
Ordinul 794/2012 art. 6, „pentru anul anterior"). A duce la anul termenului ar fi deschis un dosar
gol chiar în ziua depunerii.
⚠️ **Contribuţiile AFM n-au link, dinadins.** Sunt bani declaraţi în aplicaţia AFM, iar noi nu
tipărim niciun formular pentru ele (`legislatie.md` §5.B). Un link către un document care nu există
ar fi fost chiar promisiunea goală reparată pe 07.09 la badge-ul roşu, pe dos.

🔴 **Coloana nouă a rupt butonul de acţiune pe două rânduri** — „Marchează / finalizat" — şi asta
s-a văzut **numai pe captură**, cu toate verificările verzi. Acelaşi defect ca la inboxul de cereri,
pe 07.09. Reparat cu `whitespace-nowrap` pe celula de acţiuni şi pe badge-ul de status; proba
măsoară de-acum **înălţimea butonului**, fiindcă asta e proprietatea care se strică.

### 5. Actul de identitate al şoferilor spune de ce e ţinut

Singurul dat personal al cuiva din afara firmei pe care aplicaţia îl ţine — şi singurul care se
**tipăreşte** (Anexa 3, „Date de identificare delegat"). `plan-executie.md` prevedea regim GDPR abia
la Etapa 10, pentru CNP-ul de pe borderoul de metale, dar câmpul e în producţie de la `V28`.

Nota de retenţie stă în **amândouă** locurile unde se tastează — „Şoferii noştri" din Setări şi fişa
transportatorului — şi spune trei lucruri, toate verificabile: **de ce** se ţine (rubrica de pe
formular), **cât** (cât se păstrează evidenţa: cel puţin 3 ani, 12 luni la transportatori, OUG
92/2021 art. 48 alin. (5)) şi **ce nu face dezactivarea** (mişcările păstrează instantaneul —
decizia 30). Indicaţia de sub câmp nu mai oferă CNP-ul ca variantă la fel de bună: rubrica cere
„date de identificare", fără să numească nimic, iar noi cerem cât mai puţin, fiindcă hârtia ajunge
la destinatar.
⚠️ **Nu e o ştergere.** Un drum de ştergere adevărat (scoaterea unui şofer din rândurile vechi) ar
rescrie documente deja tipărite — deci rămâne o decizie, nu o felie de interfaţă.

### Starea

- **Backendul n-a fost atins**: 239 de teste verzi, migrări tot până la **`V31`**, următoarea liberă
  **`V32`**. Nicio felie n-a cerut una.
- **Suita de interfaţă: 9 probe, 200 de verificări** (de la 8 şi 161). Proba nouă e
  `frontend/e2e/9-restrangeri-si-semne.mjs`. **Nu lasă nimic în urmă**: cifra scrisă în grilă e
  ştearsă la loc de aceeaşi probă, iar golirea rândului chiar şterge suprascrierea (backendul o
  spune explicit în `saveMarketEntry`).
- `tsc --noEmit` curat, `vite build` verde.
### ✅ În producţie (08.09.2026, ora 13:07)

| Unde | La ce |
|---|---|
| `main` == `origin/main` == `origin/deploy/heroku-split` | **`5b54e5c`** (`313e862` codul, `5b54e5c` documentaţia) |
| `ferepo/main` (frontend) | **`e27c220`** |
| `newrepo/main` (backend) | `b5dd344`, **neatins** |
| `ecoregistru-app` | **v31** (de la v30) |
| `ecoregistru-api` | **v37**, neschimbat |

- **Backendul n-a avut ce trimite**, şi asta e cazul normal descris în handoff: `tmp-backend` a ieşit
  cu acelaşi commit în vârf ca `newrepo/main`, fiindcă niciun fişier din `backend/` nu s-a atins.
  Schema rămâne la **31** în loguri; nicio migrare de rulat.
- Cherry-pick fără conflict, cu aceeaşi explicaţie ca pe 08.09 dimineaţa: cele trei fişiere prin
  care repo-ul de frontend diferă stabil de monorepo (`.gitignore`, `vite.config.js`,
  `vite.config.d.ts`) n-au fost atinse de niciun commit.
- Verificat **pe conţinut, nu pe hash** — bundle-ul servit de producţie conţine şirurile feliilor de
  azi („Coduri cu stoc", „kilogramele stau pe cod", „Deschide evidenţa pe", „Se salvează singur, un
  rând odată", „Date personale: se ţin doar pentru rubrica") **şi nu mai conţine** ce au înlocuit
  ele („Stoc la zi", „kilograme, la ultima lună calculată").
- *(Build-ul de frontend a intrat în ~1 minut de data asta; cozile de 8–10 minute din 02.09 şi 04.09
  rămân posibile, nu obligatorii.)*

⚠️ **O probă poate trece din motivul greşit, a doua oară în două zile.** Prima rulare a probei de
restrângere a raportat „provenienţa NU se cere unui generator pur" — verde, dar fiindcă
administratorul de platformă **nu alesese nicio firmă**, deci rubrica lipsea din alt motiv decât cel
probat. Garda care aşteaptă lista de firme a fost scrisă abia după ce s-a văzut asta. Aceeaşi lecţie
ca la paletă pe 08.09: verifică **şi** cazul pozitiv, pe aceeaşi probă.

## Propoziţia care lipsea de pe Panou, şi cele cinci butoane de pe Evidenţe (08.09.2026, seara)

Primele două felii din „Ordinea recomandată" a lui `todo-ui-ux.md`, luate în ordine. Niciuna nu
repară ceva rupt — dar amândouă schimbă **ce spune ecranul despre el însuşi**, iar prima a scos la
iveală o greşeală de limbă veche de pe toate ecranele.

### 1. Panoul spune ce e de făcut, nu doar cum stau lucrurile

Panoul răspundea la „sunt în regulă?" în **cinci locuri deodată** — trei dale, caseta de blocaje şi
două liste — şi lăsa clientul să tragă singur concluzia. Banda nouă din capul paginii numeşte **un
singur** lucru, cel mai scump dintre cele deschise, cu drumul către el.

**Nu aduce nicio cifră nouă pe ecran**, şi ăsta e chiar conţinutul feliei: fiecare ramură citeşte
exact numărul pe care ecranul îl arată deja mai jos. Ce adaugă e **ordinea** — ce se ia întâi:

| # | Când | De ce în ordinea asta |
|---|---|---|
| 1 | un termen **depăşit** | curge deja; nimic din ce e mai jos nu costă mai mult |
| 2 | ieşiri **fără cod R/D** | blochează depunerea următoare (decizia 13) |
| 3 | un termen în **≤ 30 de zile** | se mai poate prinde, şi duce la documentul care îl stinge |
| 4 | o **autorizaţie** de partener pe terminate | se mai poate reînnoi (decizia 41) |
| 5 | linii care **aşteaptă cântarul** | aşteptare legitimă, nu greşeală — deci ultima |
| — | nimic din ce e mai sus | „Eşti la zi" |

⚠️ **Banda tace până vin toate trei sursele.** Fără garda asta, un `partners` întârziat ar scrie
„Eşti la zi" o clipă, peste o autorizaţie care expiră — o afirmaţie falsă făcută pe jumătate de
răspuns, exact clasa de defect pentru care s-a reparat citirea evidenţei pe 07.09.

⚠️ **Şi dublează dinadins caseta de blocaje de dedesubt.** Una spune ce se face **acum**, cealaltă
tot ce e de lămurit. Dacă vreodată una o înlocuieşte pe cealaltă, felia s-a înţeles greşit — proba
verifică explicit că amândouă sunt pe ecran.

**Legătura termen → document nu s-a scris a doua oară.** `daysUntil` era deja copiată identic pe
Panou şi pe Termene, cu acelaşi javadoc; banda ar fi cerut a treia copie, iar `documentFor` — care
decide **anul raportat** (decizia 59) — a doua. Amândouă s-au mutat în `frontend/src/lib/deadlines.ts`.
De acolo a ieşit şi o îmbunătăţire pe care n-o căutam: lista de termene de pe Panou avea şirul ei
propriu, care spunea numai zilele rămase şi **tăcea pe cele depăşite**; foloseşte acum aceeaşi
formulare ca ecranul Termene, deci un termen trecut scrie „depăşit de 226 de zile".

**Şi numărul de mişcări s-a întors lângă kilograme.** Era cifra principală a dalei până pe 08.09,
când dala a trecut pe kilograme; el răspunde la altceva — *se ţine evidenţa la zi?* — deci stă în
subtitlu, unde nu concurează cu cantitatea.

### 2. 🔴 „1 linii" — greşeala de limbă găsită uitându-mă la captură

Cu banda gata şi cele 15 verificări verzi, captura arăta **„1 linii cu ieşiri fără cod R/D"**,
„1 linii care aşteaptă cântarul" şi — chiar şirul scris în felia asta — „pe 1 mişcări din luna
aceasta". Niciun test n-avea cum s-o vadă: toate şirurile aveau `{n}` şi pluralul lipit de el.

Româna are **trei** forme acolo unde engleza are două: `1 linie` · `2 linii` · **`20 de linii`**.
„de" intră de la 20 în sus şi se întoarce la fiecare sută — `101 linii`, dar `120 de linii` —, deci
regula se citeşte pe **ultimele două cifre**, nu pe număr. `countOf(n, "linie", "linii")` în
`lib/utils` e singurul loc care o ştie.

⚠️ **Backendul rezolvase deja aceeaşi problemă**, pe 06.09, la mailul de expirare a autorizaţiei
(„expiră în 60 zile" → `theWordingAgreesWithSmallNumbers`). Interfaţa rămăsese în urmă un trimestru,
pe ecranul cel mai des deschis din aplicaţie. **Când o regulă de limbă se repară pe un canal, merită
căutată pe celelalte** — un mail şi un ecran scriu pentru acelaşi om.

Aplicat pe tot Panoul şi pe zilele rămase (deci şi pe Termene, prin `daysLabel`). Restul ecranelor
n-au fost atinse: e o trecere proprie, nu o notă de subsol a acesteia.

**Două probe fixau formularea veche şi au căzut, cum trebuia:** proba 6 aştepta „linii" acolo unde
un singur rând scrie acum „linie", iar proba 9 aştepta „depăşit de N zile" unde de la 20 în sus se
scrie „de N zile". Regexurile s-au lărgit pe cele trei forme — probele erau învechite, nu aplicaţia.

### 3. Evidenţe: cinci butoane la fel de vizibile, devenite trei şi un meniu

Antetul avea **Evidenţa gestiunii deşeurilor · Declaraţia anuală · Export Excel · Export PDF ·
Regenerează**, toate cu aceeaşi greutate vizuală — şi strângeau titlul paginii pe trei rânduri ca
să le facă loc. Dar nu sunt acelaşi fel de lucru: pe primele două scrie ce se **depune la agenţie**,
pe celelalte două scrie chiar în fişier „rezumat generic (**neoficial**)".

Cele două exporturi generice au intrat într-un meniu numit „Alte descărcări", cu nota care spune pe
faţă ce sunt („Rezumat neoficial al evidenţei — pentru lucru, nu pentru depunere") şi cu numele
schimbat din „Export Excel/PDF" în **„Rezumat Excel/PDF"**. Rămân la îndemâna oricui, viewer
inclusiv: sunt o citire, nu o scriere. „Regenerează" rămâne afară, gated pe rol — e o cerere
explicită de recalculare pe un an anume (decizia 51), nu o descărcare.

**Meniul nu s-a scris a doua oară.** Comportamentul exista în `RowActions` din `table-toolbar.tsx`
— Escape, clic în afară, `role="menu"` — scris pentru meniul de rând de pe Mişcări. S-a mutat în
`components/ui/menu.tsx` ca `Menu`/`MenuItem`, cu declanşator opţional cu etichetă; `RowActions` şi
`RowAction` rămân exportate ca **învelişuri**, deci niciun apelant existent nu s-a schimbat. Un al
doilea meniu ar fi însemnat două comportamente de Escape care trebuie să rămână identice — adică
două ocazii să difere, aceeaşi lecţie ca la totalurile Anexei 3 Ambalaje (decizia 47).

`Menu` are şi o gardă care nu era în original: **un meniu dezactivat se şi închide**, fiindcă
butonul se poate dezactiva (o descărcare porneşte) cât timp caseta e deschisă sub degetul cuiva.

### Ce ţin probele

**Proba nouă `frontend/e2e/10-panou-actiunea-urmatoare.mjs`, 28 de verificări.** Ce merită reţinut
din ea, fiindcă e a treia oară când aceeaşi capcană se închide altfel:

- **banda e probată pe două ramuri, nu pe una.** Toate verificările ar fi trecut la fel de bine dacă
  banda ar fi scris mereu acelaşi lucru: pe tenantul demo ramura de sus e adevărată. Proba comută pe
  o firmă fără termene generate şi cere ca propoziţia **şi tonul** să se schimbe. Aceeaşi lecţie ca
  la paletă (08.09, dimineaţa) şi ca la restrângerea de provenienţă (08.09, după-amiaza);
- **descărcarea chiar pleacă**, nu doar meniul arată bine — pentru asta a trebuit
  `acceptDownloads: true` în `lib.mjs`: fără el, `waitForEvent("download")` nu se declanşează
  niciodată, iar verificarea ar fi trecut pe „nu s-a întâmplat nimic", adică din motivul greşit;
- **ce era mai jos n-a dispărut** — caseta de stare, cele două dale, cele două liste;
- geometria la 375px, şi înălţimea etichetei de link, fiindcă exact aia se rupe pe două rânduri.

### Starea

- **Backendul n-a fost atins**: migrări tot până la **`V31`**, următoarea liberă **`V32`**. Nicio
  felie n-a cerut una.
- **Suita de interfaţă: 10 probe, 228 de verificări** (de la 9 şi 200).
- `tsc --noEmit` curat, `vite build` verde.
- ✅ **Mers în `main` şi deployat** (08.09.2026, ora 13:45). `main` == `origin/main` ==
  `origin/deploy/heroku-split` la **`584fe06`**; `ferepo/main` la **`c9d8208`**; `ecoregistru-app`
  **v32**. **`newrepo/main` rămâne la `b5dd344`** — backendul n-a fost atins, deci `tmp-backend` a
  ieşit cu acelaşi vârf ca remote-ul şi n-a avut ce trimite; `ecoregistru-api` rămâne **v37**, schema
  **31**, nicio migrare. *(A treia oară la rând când `git reset --hard` din procedură e de prisos:
  `split-frontend` era deja la `ferepo/main`. Verifică cu `git rev-parse` înainte să resetezi.)*
  Verificat **pe conţinut, nu pe hash**: bundle-ul din producţie conţine „Următoarea acţiune",
  „Alte descărcări", „Rezumat Excel" şi „Eşti la zi", şi **nu mai conţine** „Export Excel" /
  „Export PDF".
  ⚠️ **Şi o capcană de verificare, notată fiindcă a minţit o dată:** prima probă pe bundle a citit
  fişierul de 596 KB într-o variabilă de shell şi l-a trecut prin `echo | grep` — toate cele patru
  şiruri au ieşit „lipsă", inclusiv unele care erau acolo. **Descarcă în fişier şi caută în fişier.**

### 📋 Ce urmează

`docs/todo-ui-ux.md`, „Ordinea recomandată", de la punctul 3: sugestia de duplicat de la Parteneri
care mută covorul · badge-ul „Autorizaţie expirată" fără link · şirurile hardcodate din
`combobox.tsx` · 🔒 ştergerea datelor unui şofer, care e o **decizie**, nu o felie.

🟡 **Şi o restanţă ieşită din felia 2:** numeralul corect s-a aplicat pe Panou şi pe zilele rămase,
dar restul ecranelor scriu în continuare „{n} zile" fără acordul de la 20 în sus. `countOf` există;
e o trecere de o oră peste `strings.ts`, de făcut într-o felie proprie ca să se vadă ce s-a schimbat.

---

## Sugestia care nu mai mută covorul, şi badge-ul care duce undeva (08.09.2026, seara târziu)

Feliile 3 şi 4 din „Ordinea recomandată". Amândouă sunt despre acelaşi lucru, pe două ecrane: un
element care **face** ceva fără să spună, sau care **spune** ceva fără să ducă undeva.

### 1. Sugestia de duplicat comuta pe tăcute

Apăsai pe firma sugerată şi acelaşi dialog devenea „Editează partener": tot ce completasei
dispărea, iar singurul semn era **titlul** — pe care nu se uită nimeni când tocmai a apăsat pe altă
parte a ecranului.

**Comutarea rămâne fapta bună.** Sugestia există tocmai ca să nu se creeze un partener de două ori
(un duplicat rămâne în nomenclator şi pe documentele tipărite), iar dacă firma chiar există, fişa ei
e unde vrei să ajungi. Ce lipsea era să **se vadă** că s-a întâmplat, şi drumul înapoi.

Două tratamente, după cât ai apucat să scrii — aceeaşi regulă ca la garda de pe formularul de
mişcare (07.09), care întreabă doar pe un formular atins:

| Ce ai completat | Ce se întâmplă |
|---|---|
| doar numele | se comută **pe loc**, iar o bandă în capul formularului spune „Editezi un partener care există deja", de unde ai venit, şi oferă „Înapoi la adăugare" |
| şi alte rubrici | se **întreabă întâi**, cu ce se pierde scris în întrebare |

„Înapoi la adăugare" pune **numele tastat la loc** — restul rubricilor s-au pierdut oricum la
comutare, iar numele e chiar ce te-a adus acolo.

`formHasMoreThanName` se citeşte din starea care există deja, fără să se mai adauge un „touched":
rubricile numărate sunt exact cele pe care le-ar arunca o comutare. Tipul şi rolurile **nu intră** —
au implicit la deschidere, deci n-ar deosebi nimic.

⚠️ **Escape peste întrebare închide doar întrebarea**, nu şi formularul de sub ea — teancul din
`Dialog`, scris pe 07.09 exact pentru cazul ăsta. Există verificare.

### 2. Badge-ul „Autorizaţie expirată" era al doilea fund de sac

Badge-ul galben de pe o predare (decizia 36) spunea, în chiar textul lui, *„verifică dacă partenerul
are o autorizaţie reînnoită şi actualizeaz-o în fişa lui"* — şi **nu ducea nicăieri**. Acelaşi defect
ca badge-ul roşu reparat pe 07.09, pe alt badge; îl aveam scris de două zile în listă şi nu-l
văzusem ca fiind acelaşi.

`/parteneri?partener=<id>` deschide fişa cerută, cu parametrul **consumat la deschidere** ca
`?miscare=` pe Mişcări — lăsat în adresă, un refresh ar redeschide dialogul peste ce lucrezi. Dacă
partenerul nu mai e printre rândurile aduse (dezactivat, sau link vechi) se **spune**, nu se
deschide un formular gol.

⚠️ **Badge-ul nu mai e învelit în `Tooltip`, şi n-avea cum să fie.** Bula îşi randează **propriul
`<button>`**, deci nu poate înveli nimic interactiv — defectul din 07.09, seara, notat atunci în
cod ca să nu se reîncerce. Deci data expirării a **urcat în badge**, unde se citeşte pe ecran în loc
să stea în spatele unui hover (mai bine şi pe telefon), iar motivul întreg — cele două articole şi
ce e de făcut — a rămas în `aria-label`, pentru cine citeşte cu tastatura. Pe o mişcare fără
partener legat, badge-ul rămâne cu tooltip: n-are unde duce.

### 3. Datoria de seed, a treia tranşă — şi un rând care a rupt altă probă

**Zero rânduri din 37** aveau o predare către un partener cu autorizaţia expirată **la acea dată**,
deci proba drumului s-ar fi făcut pe gol. `INSERT`-ul aditiv e în `frontend/e2e/README.md`, ca cel
de ataşamente din 08.09 dimineaţa.

🔴 **Şi prima variantă a lui a căzut proba 9.** Rândul avea 120 kg, ceea ce a dus stocul unui cod
**fix la zero** — deci dala „Coduri cu stoc" a trecut de la 4 la 3, iar proba care fixează chiar
cifra aia a picat. Cantitatea e de-acum 1 kg. **Lecţia**: un rând de seed adăugat pentru o probă nu
trebuie să mişte datele pe care se sprijină alta — şi se vede că a mişcat **numai dacă suita se
rulează întreagă**, nu doar proba la care lucrezi.

🟡 **Nu e în `DevDataSeeder`**, ca celelalte două stări: seeder-ul e backend, iar felia a fost de
interfaţă. Rămâne restanţă, scrisă în `e2e/README.md`.

### Starea

- **Backendul n-a fost atins**: migrări tot până la **`V31`**, următoarea liberă **`V32`**.
- **Suita: 10 probe, 245 de verificări** (de la 228).
- `tsc --noEmit` curat, `vite build` verde.
- ✅ **În producţie** (08.09.2026, ora 16:47). `main` == `origin/main` == `origin/deploy/heroku-split`
  la **`b6ee36c`**; `ferepo/main` la **`970fb52`**; `ecoregistru-app` **v33**. `newrepo` neatins,
  `ecoregistru-api` rămâne **v37**, schema **31**. Verificat pe conţinut: bundle-ul din producţie
  conţine „Editezi un partener care există deja", „Înapoi la adăugare", „Deschizi fişa partenerului
  existent?" şi `partener=`.

### 📋 Ce urmează

`docs/todo-ui-ux.md`: şirurile hardcodate din `combobox.tsx` · 🔒 ştergerea datelor unui şofer (o
**decizie**, nu o felie) · 🟡 numeralul pe restul ecranelor · 🟡 mutarea seed-ului în `DevDataSeeder`.

---

## Numeralul până la capăt, șirurile din primitive și seed-ul mutat acasă (09.09.2026)

Restul listei din `todo-ui-ux.md`: patru puncte, dintre care unul n-a fost construit, ci **mutat**.
Niciunul nu adaugă o funcție — toate patru sting datorii scrise cu mâna noastră, în zile diferite.

### 1. Numeralul, pe restul ecranelor — și al doilea fel de dezacord

Pe 08.09 s-a reparat „1 linii" **pe Panou și pe zilele rămase**, cu `countOf` din `lib/utils`.
Restul ecranelor rămăsese, notat ca „o trecere de o oră". A ieșit mai mult decât o trecere.

**Ce s-a schimbat.** Optsprezece șiruri de pe Ambalaje, Evidențe, Termene, Mișcări, Setări și
cererea de cont trec acum prin **`withCount(template, n, singular, plural)`**, un înveliș subțire
peste `countOf` care așază numeralul acordat într-un `{count}`. Din el a ieșit și **convenția care
lipsea**, scrisă în javadoc:

> **`{count}` e un grup nominal acordat** („3 mișcări"), construit de `countOf`. **`{n}` e o cifră
> goală** — un ordinal („fișierul 2 din 5"), un număr între paranteze („Inactive (4)") —, acolo
> unde nu urmează niciun substantiv de acordat.

Convenția e ce face regula găsibilă: cine caută `{count}` găsește **toate** locurile în care regula
chiar se aplică, fără să citească fiecare șir. Două perechi de șiruri „unul / mai multe"
(`operationCodesSelectedOne`, `overrideUnsavedRow`) au dispărut: acopereau corect 1 și 2, dar
scriau „20 rânduri" fără „de" — exact treapta pe care o pereche fixă n-o poate prinde. Un șir mort
a ieșit și el (`statusBlocked`, nefolosit de nicăieri).

Trei locuri au cerut mai mult decât o înlocuire:

- **Dala de termene de pe Panou** scria „{n} depășite" — aici trebuie să se acorde și
  **adjectivul**, nu doar substantivul, deci toată sintagma a intrat în perechea dată lui `countOf`
  („1 termen depășit" · „20 de termene depășite"), iar șirul a rămas un slot gol.
- **„Următorul termen: X, în {days} zile"** socotea zilele a treia oară, cu mâna. Cheamă acum
  `daysLabel`, care le scrie o singură dată pentru toată aplicația — deci a primit pe gratis și
  „azi"/„mâine" în cuvinte, în locul lui „în 0 zile", care era adevărat și se citea ca nimic.
- **Generarea termenelor** raporta „Termene generate: 1 noi". Zero are acum șir propriu: „0 de
  termene noi" e corect gramatical și se citește ca o eroare.

🔴 **Și captura a scos al doilea fel de dezacord, cel de după substantiv.** Cu toate numărătorile
reparate, cu `tsc` curat și cu proba nouă verde, ecranul de Ambalaje scria **„1 mișcare încă de
cântărit — cantitatea LOR lipsește"**, iar cel de Mișcări „1 fișier **N-AU** urcat". Substantivul
se acordase; verbul și pronumele din jurul lui, nu.

Două tratamente, după unde încape acordul:

| Unde | Ce s-a făcut |
|---|---|
| verbul stă lipit de numeral | intră **în perechea** dată lui `countOf`: „1 fișier n-a urcat" · „2 fișiere n-au urcat" |
| pronumele e departe, în altă propoziție | propoziția se **rescrie fără el**: „cantitatea lor lipsește" → „cantitatea lipsește" |

**Vezi decizia 66.** Și reține forma pe care o ia lecția a doua oară: pe 08.09 „randează și uită-te
la el" a prins substantivul; pe 09.09, cu substantivul reparat, aceeași privire a prins cuvântul de
alături. **O regulă de limbă nu se termină la cuvântul pe care l-ai reparat.**

### 2. Proba 11 — prima care nu caută un text anume

Celelalte zece probe întreabă dacă un anume text e pe ecran. Aici n-are cum să meargă: un șir scris
mâine ar trece pe lângă orice listă fixă — exact cum au trecut cele de pe Ambalaje pe lângă felia
din 08.09. Deci `11-numeralul.mjs` **citește tot ce scrie pe ecran**, culege perechile «număr +
substantiv cunoscut» și verifică forma fiecăreia, pe opt ecrane. Un ecran adăugat mâine intră singur
sub regulă; un substantiv nou se trece în lista `SUBSTANTIVE` și de-atunci e păzit peste tot.

Trei lucruri de reținut din felul în care e scrisă:

- **Regula e scrisă a doua oară în probă, dinadins.** Dacă ar chema `countOf`, amândouă ar greși la
  fel și n-ar mai fi o probă — ar fi o oglindă.
- **Garda e pe ecran, nu pe total.** Prima variantă trecea verde cu „0 numărători citite" pe patru
  ecrane din opt: căuta Evidențele și Ambalajele pe `an=2025`, unde datele demo nu sunt, și
  Mișcările pe luna curentă, care e goală. Regula 9 din `todo-ui-ux.md`, a treia oară: **o
  verificare poate trece fiindcă premisa ei nu s-a întâmplat.** Acum fiecare ecran despre care se
  știe că numără ceva trebuie să întoarcă cel puțin o pereche, iar anul se citește din ceas, nu se
  scrie de mână — altfel proba ar începe să treacă pe gol la 1 ianuarie.
- **Euristica de acord se probează pe ea însăși.** Cele trei verificări de la început îi dau chiar
  propoziția găsită pe captură („1 mișcare … cantitatea lor lipsește"), forma reparată și un plural
  adevărat. Fără ele, o euristică prea îngustă ar raporta „nicio problemă" și n-am ști de ce.

### 3. Șirurile hardcodate au ieșit din primitive

Cele cinci din `combobox.tsx` („Selectează…", „Caută…", „Niciun rezultat.", „Se caută…", „Șterge
selecția") sunt în `strings.common`. **Nu s-au topit** peste `searchPlaceholder` / `noResults` de
acolo, deși seamănă: alea sunt ale barei de căutare a unui **tabel**, care caută în rândurile deja
aduse, iar astea ale unei liste care caută **la server**. Un singur șir pentru amândouă ar fi legat
două ecrane care n-au de ce să se miște împreună.

O trecere peste tot frontendul, cu diacriticele drept cârlig, a mai găsit unul: destinatarul Anexei
3 din `PackagingPage` („agenția județeană pentru protecția mediului din raza punctului de lucru" /
„ANPM"), scris în pagină. A ieșit și el. **Primitivele n-au acum niciun literal românesc** — singurul
rămas în tot frontendul e un mesaj de consolă din `ErrorBoundary`, care nu se vede pe ecran.

### 4. Seed-ul mutat acasă — și bomba cu ceas de sub el

Predarea către un partener cu autorizația expirată **înainte** de data predării trăia din 08.09 ca
`INSERT` aditiv în `frontend/e2e/README.md`: exista pe discul unei mașini, nu pe o bază proaspătă. E
acum în `DevDataSeeder`, a patra stare numărată **pe nume** de `ApplicationBootIT`. Seed-ul demo are
37 de mișcări, nu 36.

Rândul stă pe hârtia de la Cluj și e de **1 kg** — nu pe plasticul de la Turda, lângă celelalte două
stări, fiindcă acolo ar muta chiar cifrele negative pe care e verificată dala de stoc; și nu de
120 kg, fiindcă prima variantă a dus stocul unui cod fix la zero și a căzut proba 9.

🔴 **Mutarea a scos la iveală o bombă cu ceas.** Rândul are dată **fixă** (20.08.2026), dar
partenerul avea expirarea scrisă **relativ**: `LocalDate.now().minusDays(30)`. Amândouă erau
adevărate în ziua în care s-a scris `INSERT`-ul. Pe **19.09.2026** a doua ar fi încetat să fie —
expirarea ar fi trecut peste data predării, badge-ul „Autorizație expirată" s-ar fi stins, iar proba
drumului ar fi trecut degaba, fără ca nimeni să atingă nimic și fără niciun mesaj care să spună de
ce. Expirarea partenerului e acum tot o dată fixă (15.07.2026).

**Lecția, scrisă ca regulă:** o dată relativă și una fixă care trebuie să rămână în aceeași ordine
sunt un defect care se aprinde singur, într-o zi anume. Ori amândouă relative, ori amândouă fixe —
o afirmație care leagă două date n-are voie să depindă de ziua în care se citește. **Vezi decizia 67.**

⚠️ Prima variantă a verificării din `ApplicationBootIT` a aruncat `LazyInitializationException`:
citea `m.getPartner().getAuthorizationExpiry()` pe un proxy leneș, în afara unei tranzacții.
Partenerul se citește acum întreg din repository; din proxy se ia doar id-ul, care nu încarcă nimic.

### 5. Ștergerea datelor unui șofer a plecat de pe lista de ecrane

Rămăsese deschisă din 08.09, după nota de retenție. Nu e o felie de interfață și n-are ce căuta pe o
listă de ecrane: e o **decizie**, între două obligații care se bat cap în cap — evidența trebuie să
rămână cum a fost depusă, iar datele nu se țin peste termenul de păstrare. A trecut pe lista de
întrebări ca **AO**, cu întrebarea adevărată scrisă pe față: nu „adăugăm un buton de ștergere", ci
**ce se șterge** — fișa șoferului (și mișcările rămân cu instantaneul, decizia 30), sau și
instantaneele, caz în care un document deja tipărit nu se mai poate reproduce identic.

Dacă răspunsul specialistei e „nimeni n-a cerut vreodată", **asta** e decizia — scrisă, nu o
restanță care se plimbă din listă în listă.

### Starea

- **Backendul a fost atins** — `DevDataSeeder` și `ApplicationBootIT` —, dar nu schema: **239 de
  teste verzi**, 0 eșecuri. Migrări tot până la **`V31`**, următoarea liberă **`V32`**.
- **Suita de interfață: 11 probe, 273 de verificări** (de la 10 și 245). Proba nouă e
  `frontend/e2e/11-numeralul.mjs`, 26 de verificări.
- `tsc --noEmit` curat, `vite build` verde.
- ✅ **În producție** (09.09.2026, ora 11:21). `main` == `origin/main` == `origin/deploy/heroku-split`
  la **`5ff8ef4`**; `newrepo/main` la **`b18c64e`**, `ferepo/main` la **`19cc534`**;
  `ecoregistru-api` **v38**, `ecoregistru-app` **v34**. **Backendul a intrat de data asta** — prima
  oară după patru felii de interfață —, dar fără migrare: Flyway a validat 31 și a rămas la 31.
  Verificat **pe conținut, nu pe hash**, cu bundle-ul descărcat **în fișier** (capcana din 08.09:
  596 KB citite într-o variabilă de shell și trecute prin `echo | grep` ies toate „lipsă"). Sunt
  acolo „Șterge selecția", „Se caută…", „fără cod R/D la ieșire", „cantitatea lipsește din ambele
  tabele", „Ce a rămas e tot în listă" și „agenția județeană pentru protecția mediului"; **și nu mai
  sunt** „cantitatea lor lipsește", „cu ieșiri fără cod R/D", „Termene generate:", „o operațiune
  aleasă" și „Un rând are cifre nesalvate".
  *(`git reset --hard` a fost din nou de prisos: amândouă ramurile split erau deja la capetele
  remote. `git rev-parse` înainte de reset, a patra oară la rând.)*

### 📋 Ce urmează

`docs/todo-ui-ux.md` a rămas cu ce **nu** e cosmetic: vederea cross-tenant pentru `PLATFORM_ADMIN`
(singura care aduce bani — `monetizare.md` o numește diferențiatorul canalului de consultanți) ·
greutatea arhivei din Dosarul de control · „Arată parola" la resetare · 🟡 tabelele care aduc tot și
paginează în client.

Iar în afara interfeței, neschimbat: 🔵 **modulul de depozit (Etapele 8–11) stă pe pauză până la
meetingul cu Andreea** — `docs/intrebari-specialist.md`, întrebările **AD** și **AI**.

---

## Verdele fals de pe Panou, şi celelalte şapte găsite probând aplicaţia (09.09.2026, după-amiaza)

Nu e o felie de funcţii: e **o probă cap-coadă cerută de utilizator** peste feliile de interfaţă
livrate în 07–09.09, plus reparaţiile a ce a ieşit din ea. Suita şi testele erau verzi înainte să
încep — 10 din 11 probe, 239 de teste, `tsc` curat. Cele opt de mai jos au ieşit din altceva:
**deschizând aplicaţia, apăsând butoanele, şi făcând cererile să cadă**.

Trei dintre ele sunt acelaşi defect la trei adâncimi: o afirmaţie scrisă peste date care nu există.
Şi de trei ori s-a văzut **numai pe captura reparaţiei**, nu pe cea a feliei.

### 1. 🔴 „Eşti la zi" scris peste date care n-au venit

Cel mai scump. Panoul citea numai `isLoading`. O cerere **căzută** iese din `isLoading` cu `data`
nedefinit, iar `?? []` o făcea să arate identic cu un răspuns gol — deci banda scria „Eşti la zi",
caseta scria „Nimic nu blochează documentele", iar cele două liste de jos „Niciun termen deschis" şi
„Nicio autorizaţie aproape de expirare". **Fără niciun mesaj de eroare pe ecran.**

Probat pe firma demo, care are 9 termene depăşite şi o linie fără cod R/D, cu `/evidences`,
`/deadlines` şi `/partners` răspunzând 500:

```
banda:  „Ești la zi — Niciun termen deschis apropiat, nicio linie de lămurit…"
starea: „Nimic nu blochează documentele"
vreun mesaj de eroare pe ecran? false
```

Garda `nextActionLoading`, scrisă pe 08.09 chiar pentru asta, acoperea **numai cererile în zbor** —
comentariul ei spune „«Eşti la zi» peste un `partners` neîncărcat ar fi un verde fals", şi avea
dreptate despre cazul greşit. Există acum a treia stare, `unknown`: gri, „Nu am putut verifica
starea", cu Reîncarcă. Dalele arată „—" în loc de `0`, iar culoarea stării le cade odată cu cifra:
un `0` verde pentru „n-am putut citi" e aceeaşi minciună, doar mai scurtă.

⚠️ **Prima reparaţie a fost incompletă, şi s-a văzut tot pe captură.** Cu banda şi caseta reparate,
cele două liste de jos scriau în continuare „Niciun termen deschis pentru anul curent". Acelaşi fals,
cu litere mai mici. **Regula 5 încă o dată: reparaţia se randează şi se priveşte, ca şi felia.**

### 2. Administratorul de platformă cerea date fără să aibă firmă

Cauza defectului 1 în producţie, nu într-o probă: `PLATFORM_ADMIN` aterizează după login **fără
firmă aleasă**, ecranele se randau oricum, cele patru cereri plecau fără `X-Tenant-Id` şi primeau
`400` — de două ori fiecare, că `retry: 1`. Opt cereri roşii în consolă la fiecare autentificare, şi
peste ele verdele de mai sus.

`RequireTenant` (în `ProtectedRoute.tsx`) ţine ecranele de firmă închise cât timp nu s-a ales una şi
spune de unde se alege. **Clienţi** nu trece pe acolo, dinadins: e chiar ecranul din care se aleg.
Zero cereri de eroare acum, pe toate ecranele.

### 3. „expiră în **0 de zile**"

`countOf` chemat cu `0` pe badge-ul de autorizaţie, în ziua expirării. E **exact** greşeala pe care
felia din 09.09 o reparase cu o casetă mai sus, pe `statDeadlinesNext` — comentariul de acolo o
numeşte pe litere: „mai rău, «în 0 zile» chiar în ziua termenului". Lista de parteneri n-a intrat pe
`daysLabel`, deci a păstrat-o, în forma mai proastă („0 **de** zile"). Scrie „expiră azi" /
„expiră mâine", ca restul aplicaţiei.

🔴 **Şi spune ceva despre proba 11:** ea verifică *forma* numeralului, iar „0 de zile" e forma
corectă pentru 0. O probă care păzeşte o regulă nu păzeşte şi domeniul pe care regula se aplică —
javadocul lui `countOf` scrie „se cheamă doar cu `n >= 1`", iar asta nu o verifică nimeni.

### 4. „Evidenţă regenerată: **0 de linii**"

Aceeaşi zi, acelaşi commit, ecranul vecin: **Termenele** au primit garda de zero (`generatedNone`),
**Evidenţele** nu. Se vede pe orice firmă fără mişcări, adică la primul contact al oricărui client
nou. Are acum `regeneratedNone`, care spune ce s-a întâmplat: nu există mişcări în anul ăla.

### 5. „Adaugă cantitatea", rupt pe două rânduri

`scrollHeight 36` într-un `clientHeight 32`, cu pictograma rămasă lângă primul rând. **Al treilea caz
al aceluiaşi defect** — după coloana de acţiuni din Cereri (07.09) şi cea din Termene (08.09),
reparate amândouă **local**. Găsit tot pe captură, cu toate verificările de DOM verzi.

`whitespace-nowrap` a intrat de data asta în `buttonVariants`, nu la apelant: toate măsurile
primitivei sunt înălţimi fixe (`h-8`, `h-10`, `h-12`), deci o etichetă care se rupe nu măreşte
butonul, îi iese din cutie. Al patrulea caz nu mai are de unde veni. Verificat la 1440px şi 375px că
niciun buton nu iese din celula lui şi că pagina tot nu derulează lateral.

### 6. `/actuator/health` întorcea `DOWN`

```
{"status":"DOWN"}
jakarta.mail.AuthenticationFailedException: failed to connect, no password specified?
```

Indicatorul de mail al lui Actuator deschide o conexiune SMTP **reală** la fiecare cerere. În dev,
fără parolă, cade — deci health-ul raporta o aplicaţie perfect sănătoasă ca fiind căzută, iar
README-ul îl dă drept locul unde se verifică dacă merge. În producţie era mai mult decât zgomot:
fiecare sondă deschidea un SMTP cu timeout-urile de 10s din `application.yml`, iar o indisponibilitate
la furnizorul de mail ar fi raportat tot dyno-ul ca fiind jos. `management.health.mail.enabled: false`
— mailul e o **funcţie** a aplicaţiei, nu condiţia ca ea să răspundă la cereri.

### 7. Aplicaţia n-avea favicon — şi de-asta cădea proba 6

`index.html` n-avea `<link rel="icon">` şi nu există `public/`, deci Chrome cerea `/favicon.ico`,
primea 404 şi scria eroarea în consolă. Prima navigare a probei 6 e `/cerere-cont` — singura suită
care **nu** începe pe `/login` —, deci 404-ul intra în `page.problems` şi făcea proba roşie.
**De două ori din două**, nu intermitent: „11 probe, toate verzi" nu se reproducea pe o maşină curată.

Iconiţa e inline, ca `data:` URI: un `.ico` ar fi fost primul fişier binar din repo. Şi, dincolo de
probă, tab-ul era gol pentru prospectul care deschide `/cerere-cont` — chiar pagina pe care felia din
07.09 i-a dat identitate.

### Mărunţişuri, din acelaşi drum

- **„Rezumat PDF" n-avea nota** care spune că nu se depune; „Rezumat Excel", de deasupra lui, o avea.
  Un avertisment pus o singură dată păzeşte un rând.
- **`Menu` nu marca Escape ca tratat.** `Dialog` citeşte `defaultPrevented` tocmai ca un strat
  dinăuntru să poată închide numai pe el (aşa face lista comboboxului). Azi nu există meniu în
  dialog, dar meniul e o primitivă: cine îl pune acolo mâine n-are de unde şti că trebuie reparat
  întâi.

### 8. „Eşti la zi" pe un cont pe care nu s-a scris încă nimic

Îl lăsasem deoparte ca **decizie**, nu felie — „care e primul pas" părea să ţină de fluxul de
deschidere a contului, care e al consultantului. Greşit: **cei doi paşi sunt scrişi deja în cod**, ca
dependenţe, nu ca preferinţe. O mişcare se înregistrează **pe** un punct de lucru — chiar formularul
o spune, în `noWorkPointHint` —, iar evidenţa, fişa şi declaraţiile se calculează **din** mişcări.
Deci nu e nimic de ghicit; era doar nescris pe ecranul care întreabă „ce fac acum?".

Banda are un ton nou, `start`, în culoarea mărcii şi nu în verde: o bifă verde pe un cont pe care nu
s-a scris nimic îi spune omului că a terminat. Fără punct de lucru → „Adaugă primul punct de lucru"
(Setări); cu punct de lucru dar fără nimic înregistrat → „Înregistrează prima mişcare".

Condiţia a doua e o **conjuncţie de trei**, dinadins — zero mişcări luna asta **şi** zero linii de
evidenţă pe an **şi** zero parteneri. O firmă care lucrează are parteneri şi într-o lună goală, iar
una cu date numai din anii trecuţi îi are cu atât mai mult; conjuncţia e ce ţine propoziţia adevărată
pe un cont vechi şi liniştit. Probat pe patru firme: cele două goale o primesc, „Reciclare Verde" nu
(are termene depăşite, care câştigă, cum trebuie), iar firma demo e neatinsă.

⚠️ **Şi captura a arătat, iar, un strat mai jos.** Cu banda gata, caseta de dedesubt scria pe un an
fără nicio linie: „Nimic nu blochează documentele — Fişa de evidenţă şi declaraţia se pot tipări aşa
cum sunt." Adevărat şi nefolositor: se pot tipări **goale**. Zero blocaje şi zero de raportat sunt
două lucruri diferite, iar acum scrie care dintre ele e. **A treia oară în aceeaşi zi când reparaţia
cerea privită captura ei, nu doar a feliei.**

### Seed-ul, probat în sfârşit pe o bază curată

`DevDataSeeder` sare când găseşte date, iar baza de dev de pe maşină avea seed-ul vechi plus
`INSERT`-urile aditive (38–39 de mişcări, nu 37) — deci felia de seed din 09.09 n-avusese niciodată
o probă cap-coadă, doar `ApplicationBootIT`. S-a făcut pe o bază nouă (`ecoregistru_seedtest`,
ştearsă după), cu backendul pornit pe `:8081`:

```
Successfully applied 31 migrations to schema "public", now at version v31
Seeded 37 sample movements.
Seeded 2 demo attachments on the movement without an R/D code.
```

Toate patru stările sunt acolo, numărate prin API: o ieşire fără cod R/D, una care aşteaptă cântarul,
una cu două ataşamente, şi predarea din **20.08.2026** către un partener cu autorizaţia expirată la
**15.07.2026**. Amândouă datele fixe, în ordinea cerută — **bomba cu ceas din 19.09 chiar e
dezamorsată**, nu doar rescrisă.

### Cifre

- **Backendul atins numai la configuraţie** (`application.yml`), nu la cod şi nu la schemă:
  **239 de teste verzi** (rulate cu `cleanTest test`, 0 eşecuri, 0 erori, 0 sărite), migrări tot până
  la **`V31`**, următoarea liberă **`V32`**. Plus o probă de seed pe bază curată, pe `:8081`.
- **Suita de interfaţă: 11 probe, 273 de verificări, toate verzi** — inclusiv proba 6, care cădea
  înainte de reparaţia 7.
- `tsc --noEmit` curat, `vite build` verde.
- Documentele s-au probat pe rând, descărcate şi deschise: fişa Anexa 1 (randată şi privită —
  `16 06 01*` cu asterisc, patru capitole, TOTAL AN, legenda), declaraţia anuală, Anexa 3 de
  transport, Anexa 1 Ambalaje `.xls` şi `.pdf`, cele două rezumate, dosarul ZIP.

### ✅ În producţie (09.09.2026, ora 12:41)

`main` == `origin/main` == `origin/deploy/heroku-split` la **`c2b7276`**; `newrepo/main` la
**`d0db491`**, `ferepo/main` la **`7bb19a0`**; `ecoregistru-api` **v39**, `ecoregistru-app` **v35**.
**Backendul a intrat**, dar numai cu `application.yml` — Flyway a scris „Current version of schema
«public»: 31 · No migration necessary".

Verificat **pe conţinut, cu bundle-ul descărcat în fişier**, nu într-o variabilă de shell (capcana
din 08.09): toate cele opt şiruri noi sunt acolo — „Nu am putut verifica starea", „Nu am putut citi
evidenţa", „Alege o firmă ca să vezi ecranul", „expiră azi", „Nimic de regenerat pentru",
„Înregistrează prima mişcare", „Adaugă primul punct de lucru", „Nu e nimic de verificat pe" — iar
„expiră în" a rămas, cum trebuie: de la 2 zile în sus e tot forma folosită.

Şi cele două verificări care erau chiar defectele:

```
/actuator/health  →  {"status":"UP"}      (era DOWN)
index.html        →  <link rel="icon">     prezent
```

### Parola conturilor demo a ieşit din repo

Cerut de utilizator după proba pe producţie, şi e a doua jumătate a aceleiaşi poveşti.

`README.md` — repo **public** — avea un tabel „Demo accounts" cu cele patru adrese **şi cu parola
scrisă în titlul secţiunii**. Nota „dev profile" descrie **intenţia**, nu realitatea: `status.md` ştia
deja, de pe 24.08, că aceleaşi conturi sunt în baza de **producţie** („De şters sau dezactivat"), iar
decizia 14 din notele private spune că utilizatorul a hotărât să nu le şteargă cât timp nu există
clienţi reali. Ce n-a fost pus cap la cap până acum e că tabelul din repo **completa** situaţia aia:
vectorul scris în notă era resetarea de parolă prin domeniul `demo.ro`, care nu e al nostru — dar
`POST /auth/login` cu parola tipărită în README întorcea **200** direct, iar
`platform@ecoregistru.ro` e `PLATFORM_ADMIN`, adică toate firmele.

Ce s-a schimbat, ca un `grep` după ea să iasă gol pe amândouă repo-urile:

| Unde | Ce era | Ce e |
|---|---|---|
| `DevDataSeeder` | un `static final String DEMO_PASSWORD` cu parola scrisă | `@Value("${app.demo-password:}")`; nesetată → una aleatoare pe pornire, scrisă în log |
| `application.yml` | — | `app.demo-password: ${DEMO_PASSWORD:}`, gol dinadins |
| `frontend/e2e/lib.mjs` | patru perechi cu parola scrisă | `process.env.E2E_PASSWORD`, şi **refuză să pornească** fără ea |
| `README.md` (public) | tabelul cu adrese + parola | rolurile, plus cum se setează `DEMO_PASSWORD` |
| `RegisterSeamIT`, `TenantIsolationIT` | aceeaşi literală, la `passwordEncoder.encode(...)` | `encode(UUID.randomUUID().toString())` — nimeni nu se autentifica cu ea, e umplutură pentru o coloană `NOT NULL` |

**Adresele au rămas** în seeder şi în teste, dinadins: sunt identităţile pe care `ApplicationBootIT`
le numără pe nume, iar o adresă fără parolă nu e o credenţială. Ce se commite de-acum e **cine**, nu
**cum intri**.

Probat pe două baze curate: fără `DEMO_PASSWORD`, seeder-ul scrie „parola conturilor demo pe pornirea
asta: …", login-ul cu ea întoarce 200 şi cel cu vechea parolă **400**; cu `DEMO_PASSWORD` setată,
avertismentul nu mai apare şi merge valoarea dată. Suita: 11 probe, 273 de verificări, verzi cu
`E2E_PASSWORD`.

⚠️ **Şi jurnalul ăsta a pus-o înapoi de patru ori, la prima scriere** — citând-o, ca să explice ce
s-a scos. `git status` era curat şi commitul plecase; a prins-o abia `grep`-ul de verificare rulat
**după** push, pe amândouă repo-urile. Un secret scos din cod se întoarce cel mai uşor prin
documentul care povesteşte cum a fost scos. **Verificarea se face pe arborele întreg, nu pe fişierele
pe care crezi că le-ai atins.**

🔴 **Şi ce nu repară asta.** Istoricul git păstrează parola, repo-ul e public şi poate fi deja
clonat, iar conturile **rămân valabile în producţie**. Curăţarea fişierelor opreşte următorul cititor,
nu pe cel de ieri. Reparaţia adevărată e rotirea sau dezactivarea conturilor, şi ea n-a fost făcută —
rămâne pe lista de blocaje, unde stă din 24.08.

⚠️ **Şi o capcană de mediu, nu de cod:** după procedura de deploy (`git checkout` / `reset --hard` pe
subtree-uri) serverul Vite pornit dinainte rămâne cu graful vechi şi întoarce `500` pe `main.tsx`
(„Failed to resolve import"). Toate cele 11 probe au căzut la login din motivul ăsta, nu din cod.
**După un deploy, reporneşte `npm run dev` înainte să rulezi suita.**

### 📋 Ce urmează

Neschimbat faţă de felia de dimineaţă: `docs/todo-ui-ux.md` — vederea cross-tenant pentru
`PLATFORM_ADMIN` · greutatea arhivei din Dosarul de control · „Arată parola" la resetare ·
🟡 tabelele care aduc tot şi paginează în client. **Cele două lucruri pe care le lăsasem deschise —
Panoul pe un cont gol şi proba seeder-ului pe bază curată — s-au făcut amândouă**, deci lista rămâne
exact cea de dimineaţă.

---

## Perimetrul de producție — șase din opt puncte P0 (09.09.2026, seara)

Prima felie care nu atinge nicio funcție a produsului. Vine din `docs/todo-lansare.md`, scris în
aceeași zi după o citire completă a codului, și din constatarea lui: **produsul e ~85% gata și nu el
ține lansarea pe loc**. Ce ține e perimetrul din jur — securitatea producției, juridicul, canalul —
adică exact partea în care nu intrase până acum niciun procent din disciplina care se vede peste tot
în rest.

Lista P0 are opt puncte. **Șase sunt încheiate aici**, două rămân pe o permisiune pe care n-o am
(vezi la final). Trei dintre cele șase se reparau pe dyno, nu în cod — și tocmai de asta fiecare are
scrisă și proba, fiindcă un `heroku config:set` nu lasă urmă în git.

### 1. Conturile demo nu mai intră în producție (P0.1, parțial)

Faptul, verificat cu `curl` înainte de orice atingere, nu dedus:

```
platform@ecoregistru.ro        200
admin@demo.ro                  200
operator@demo.ro               200
viewer@demo.ro                 200
```

Patru conturi, cu parola tipărită în `README.md` până pe 09.09 (`5a11865`) și rămasă în istoricul
unui repo **public**. *(Parola nu se mai scrie aici: paragraful ăsta o retipărea în clar, în repo-ul
public, chiar lângă propoziția care spune că unul dintre conturi e `PLATFORM_ADMIN` și încă activ —
adică o făcea mai ușor de găsit decât în istoric. Scoasă pe 09.09.2026, noaptea. Istoricul git o
păstrează, deci **reparația rămâne dezactivarea contului**, nu ștergerea din text.)* Nu era o notă teoretică într-un document: era un login funcțional
către un sistem viu, publicat, iar `platform@ecoregistru.ro` e `PLATFORM_ADMIN`, adică **toate
firmele**, nu doar tenantul demo.

Cele trei conturi de tenant sunt dezactivate. După:

```
platform@ecoregistru.ro        200      ← rămas deschis, vezi „Ce n-am putut face"
admin@demo.ro                  400
operator@demo.ro               400
viewer@demo.ro                 400
```

🔴 **Ce s-a aflat în aceeași trecere, și e mai important decât punctul în sine:** cum au ajuns acolo.
`DevDataSeeder` e `@Profile("dev")` și `SPRING_PROFILES_ACTIVE` e **gol** pe dyno — deci n-au venit
prin seeder, ci pe alt drum (cel mai probabil un dump încărcat cândva). Drumul ăla n-a fost găsit,
deci nu se poate spune că e închis.

### 2. CORS pe o origine, nu pe tot internetul (P0.2)

`Access-Control-Allow-Origin: *` pe un API care se autentifică prin `Authorization: Bearer`, deschis
către orice pagină de pe internet. Comentariul din chiar fișierul ăla o spunea de la prima zi:
*„Tighten Access-Control-Allow-Origin before opening the product publicly."*

Lista vine din `app.cors.allowed-origins`, iar valoarea implicită e **chiar `FRONTEND_BASE_URL`** —
deci producția n-a avut nevoie de nicio variabilă nouă, ceea ce era jumătate din motivul pentru care
punctul stătea nefăcut. Profilul `dev` adaugă cele două scrieri ale serverului Vite; `localhost` și
`127.0.0.1` sunt origini diferite pentru browser.

Trei lucruri care par detalii și nu sunt:

* **`Vary: Origin` pe fiecare răspuns, inclusiv pe refuzuri.** Fără el, un cache partajat poate
  servi răspunsul originii permise — cu tot cu antet — unei pagini de altundeva.
* **O cerere fără `Origin` trece neatinsă.** curl, sonda de health, un apel server-la-server nu sunt
  cereri CORS și n-au voie să devină: a le răspunde cu antete CORS e felul în care o „reparație"
  redeschide ușa în tăcere.
* **Un preflight de la o origine refuzată primește `403`, nu `200`.** Browserul l-ar bloca oricum,
  dar un 200 e o minciună pe care n-o citește nimeni — iar în log-uri se vede refuzul.

Probat pe backendul pornit, nu doar în teste:

```
Origin: http://localhost:5174    → Access-Control-Allow-Origin: http://localhost:5174 · Vary: Origin
Origin: https://exemplu-strain.ro → (niciun Access-Control-Allow-Origin) · Vary: Origin
preflight strain: 403 · preflight propriu: 200
```

⚠️ **Și normalizarea care nu e cosmetică:** o valoare configurată cu slash la final
(`https://app.exemplu.ro/`) nu s-ar mai fi potrivit niciodată cu antetul `Origin`, care n-are unul.
Ar fi ieșit ca „aplicația e căzută", pe un antet la care nu se uită nimeni. Se taie la citire, și
`CorsOriginListTest` o fixează — împreună cu potrivirea **exactă**: `https://ecoregistru.ro` nu
acceptă `https://ecoregistru.ro.atacator.com`.

### 3. Frână pe cele trei uși publice (P0.3)

Căutat în tot backendul înainte: **nu exista nimic** — nici bucket4j, nici resilience4j, nici o
numărătoare de încercări eșuate, nici lockout. Brute force pe `/auth/login` era gratuit, cu parola
minimă de 8 caractere și nicio altă regulă.

Cinci cote, în memorie (un dyno; Redis n-ar aduce nimic acum, iar un limitator care cere Redis ca să
pornească e un limitator care se stinge):

| Ușa | Pe IP | Pe email |
|---|---|---|
| `POST /auth/login` | 60 / 5 min | **10 eșuate** / 15 min |
| `POST /auth/request-reset-password` | 10 / oră | 3 / oră |
| `POST /account-requests` | 10 / oră | — |

Două decizii poartă tot punctul:

* **Pe email se numără doar încercările care au eșuat**, iar la o autentificare reușită jetonul se
  dă înapoi. Altfel s-ar fi blocat exact omul care își știe parola: un birou care intră luni
  dimineața, sau suita e2e, care se autentifică de vreo zece ori pe rulare ca același utilizator.
  Verificarea vine **înaintea** consumului, ca o cotă epuizată să nu se poată deschide ghicind
  corect a unsprezecea oară.
* **Adresa se citește din ultimul salt din `X-Forwarded-For`, nu din primul.** Reflexul e primul, și
  pe Heroku e greșit: routerul **adaugă** adresa de la care se conectează, deci primul salt e scris
  de client. Cu primul, oricine își trimitea propriul antet și primea o găleată nouă la fiecare
  cerere — adică un limitator care arată că funcționează și nu limitează nimic. `getRemoteAddr()` ar
  fi fost și mai rău: routerul Heroku e aceeași mână de adrese pentru tot internetul.

Probat pe backendul pornit — a 61-a cerere de pe aceeași adresă:

```
prima 429 la încercarea 61
HTTP/1.1 429 · Retry-After: 299
{"error-type":"too-many-requests","error-code":"too.many.requests",
 "error-message":"Prea multe încercări. Te rugăm să încerci din nou peste câteva minute."}
```

Frontendul n-a avut nevoie de nicio linie: plicul de eroare e cel obișnuit, iar cele trei pagini
publice trec deja prin `apiErrorMessage`. Un `429` nu e `401`, deci nici interceptorul de sesiune
expirată nu se aprinde pe el.

### 4. Sesiuni de o zi de lucru, care se pot închide (P0.4, `V32`)

`TOKEN_VALIDITY_MS` era **30 de zile**, fără refresh, fără listă de revocare, cu tokenul în
`localStorage`. Consecința, în propoziția pe care o pune un client înainte să semneze — *ce se
întâmplă când pleacă un angajat?* — era: **nimic, încă o lună.**

Trei schimbări, și fiecare acoperă ce nu poate cealaltă:

* **8 ore** în loc de 30 de zile. O sesiune trăiește cât o zi de lucru; frontendul duce deja un 401
  la login cu „sesiunea a expirat" **și** cu adresa de unde s-a căzut, deci costul e o autentificare.
* **`enabled` se verifică la fiecare cerere.** Asta închide pe loc sesiunea unui cont dezactivat, și
  e chiar proba din TODO: autentificare → dezactivare → aceeași cerere. Înainte răspundea `200`, încă
  douăzeci și nouă de zile.
* **`token_version` (`V32`)** închide ce `enabled` nu poate: o **schimbare de parolă**. Contul rămâne
  activ, deci nimic din rândul lui nu s-ar fi schimbat — iar tokenul emis cu parola veche ar fi rămas
  valabil lângă cea nouă, inclusiv la cel de la care tocmai ți-ai luat contul înapoi. Resetarea
  incrementează contorul; tokenul poartă valoarea de la emitere, în claimul `tv`.

⚠️ **Și capcana migrării, evitată dinadins.** Tokenurile emise **înainte** de `V32` n-au deloc
claimul `tv`. Sunt citite ca versiunea 0 — valoarea la care s-a migrat fiecare rând — deci nu s-a
deconectat nimeni la ora la care s-a întâmplat să ruleze Flyway. Un `DEFAULT 1` ar fi făcut exact
asta, tăcut. `SessionRevocationIT` fixează și cazul ăsta, nu doar cele două reparații.

### 5. Cloudinary, setat (P0.5)

`CLOUDINARY_URL` era gol pe dyno — verificat pe 02.09 și încă gol azi — deci **atașamentele nu urcau
în producție**, iar ecranul Mișcări le oferea, cu drag-drop cu tot. Primul client care încarcă un
aviz și nu-l regăsește în dosarul de control nu raportează un defect, ci pierde încrederea.
Variabila e setată (`ecoregistru-api` v40). ⚠️ **Proba adevărată — un fișier urcat pe producție și
regăsit în `atasamente/` din arhivă — rămâne de făcut**, și până atunci punctul nu se bifează.

### 6. Colector de erori (P0.6)

`ErrorBoundary.tsx` o scria singur: *„Nu există colector de erori în producție (nici Sentry, nici
altceva). Consola e tot ce avem."* Adevărat cât timp singurul utilizator era cel care a scris-o. Cu
clienți, consola e a lor: un defect se află doar dacă cineva sună, și cei mai mulți nu sună.

Sentry pe amândouă capetele, **inert fără DSN** — fără `SENTRY_DSN` / `VITE_SENTRY_DSN` nu se
inițializează nimic, deci dev-ul, testele și un deploy fără variabilă se comportă identic cu
înainte. Se aprinde punând variabila.

Ce s-a ales dinadins, și motivul:

* **Nu rezolvatorul automat al lui Sentry, ci o linie în `AdviceController.handleUnexpected`.**
  Rezolvatorul raportează **fiecare** excepție care iese dintr-un controller — iar aici ies și cele
  normale: un CUI care nu există, un formular greșit, o mișcare care încalcă o regulă. Într-o
  săptămână, 400-urile unui client care greșește un CUI ar fi înecat singurul lucru pentru care
  există colectorul. Se raportează exact ramura care înseamnă „nu ne așteptam la asta".
* **Fără `send-default-pii`, fără tracing, fără Session Replay, cu parametrii adresei tăiați.**
  Aplicația e multi-tenant și duce date de client; un raport de defect cu un formular de mișcare în
  el ar scoate din firmă exact ce apărăm. Replay-ul ar filma ecranul unui client și l-ar trimite la
  un terț.

### 7. CI care rulează ce aveam deja (P0.7)

260 de teste de backend și 273 de verificări de interfață, și **nimic nu le pornea la push**: toată
plasa de siguranță a proiectului atârna de disciplina de a o rula cu mâna.
`.github/workflows/ci.yml` rulează `./gradlew test` pe un job și `tsc --noEmit` + `vite build` pe
celălalt, la fiecare push și pe fiecare PR.

🔴 **Iar premisa scrisă în TODO era greșită pe jumătate, și adevărul e mai neplăcut.** Nota spunea că
`build.gradle` declară doar `embedded-postgres-binaries-windows-amd64`, deci suita „nu pornește pe un
runner Linux". Ba pornea: `embedded-postgres:2.0.7` aduce **transitiv** binarele pentru linux-amd64,
linux-alpine și darwin-amd64. Numai că le aduce la **14.10.1**, în timp ce declarația explicită
ridica Windows la **15.6.0** — deci suita ar fi fost verde pe Postgres 14 în CI și pe Postgres 15 pe
mașina care a scris codul. Diferența aia se vede o singură dată, la o migrare, în cea mai proastă
săptămână. Toate patru platformele sunt acum fixate la 15.6.0, plus `darwin-arm64v8`, cu care mașina
de dezvoltare nu mai trece prin Rosetta. Suita rulează verde pe Postgres 15.

Suita e2e **nu** intră în CI, dinadins: conduce un Chrome adevărat peste un backend și un frontend
pornite, ~7 minute, și cere `E2E_PASSWORD` plus o bază. Rămâne comanda de dinaintea deployului.

### Cum s-a probat, și ce a ieșit din felul în care s-a probat

**260 de teste** (de la 239): `CorsIT` (5), `CorsOriginListTest` (5), `RateLimitIT` (7),
`SessionRevocationIT` (4). Migrări până la **`V32`**; următoarea liberă **`V33`**.

**Suita de interfață: 11 probe, 273 de verificări, toate verzi** — și de data asta rulată
**cross-origin de-adevăratelea**: frontendul pe `:5174` chemând direct API-ul pe `:8081`, fără proxy.
Setarea obișnuită (Vite proxy pe `/api`) e same-origin, deci **n-ar fi atins deloc** codul CORS. Ce
era gândit ca o ocolire a două servere deja pornite a ieșit o probă mai bună decât cea normală.

⚠️ **Prima rulare a căzut pe 6 din 11 probe, și niciuna nu era o regresie.** Pornisem pe o bază
**curată**, iar suita se sprijină pe stare **acumulată**: `6-cerere-si-rapoarte.mjs` scrie o cerere
la fiecare rulare și nu curăță după ea (aprobarea creează o firmă, iar firmele nu se șterg), deci
baza de dev are patru firme și cincisprezece termene acolo unde una proaspătă are una și zero.
Verificările au spus-o singure — „există o a doua firmă pe care să se probeze cealaltă ramură —
(niciuna)", „tenantul demo chiar are termene depășite — 0 depășite". **Gardele de premisă scrise
pentru regula 9 au funcționat exact invers decât fuseseră gândite** și au arătat că nu premisa
codului lipsea, ci a probei. Aceeași suită pe baza de dev: 273 din 273.

**`V32` e probată în amândouă felurile** — pe o bază goală (31 de migrări + a 32-a, seed complet) și
**pe una populată**, cu 5 utilizatori și 39 de mișcări: „Current version of schema «public»: 31 →
Migrating to version 32 → Successfully applied 1 migration".

`tsc --noEmit` curat, `vite build` verde (610 kB, de la 600 — cele 10 kB sunt SDK-ul Sentry).

### Ce n-am putut face, și de ce

🔴 **`platform@ecoregistru.ro` se autentifică în continuare pe producție cu parola din istoricul
public.** E jumătatea scumpă a lui P0.1, fiindcă e `PLATFORM_ADMIN`: toate firmele. Cele două căi de
închidere — scrierea unui hash de parolă nou și promovarea contului real al proprietarului la
`PLATFORM_ADMIN` — au fost **amândouă blocate** de clasificatorul de securitate al uneltei, corect
în principiu: sunt scrieri de credențiale și o escaladare de privilegii pe o bază de producție.
Comenzile sunt scrise, se rulează cu mâna. **Până atunci, punctul e deschis, iar `JWT_SECRET` nu s-a
rotit** — și n-are rost să se rotească înainte, fiindcă rotirea e a doua jumătate a aceleiași
reparații.

⬜ **P0.8, proba de restaurare a backupului**, nu s-a atins. Nicăieri în documente nu apare o
restaurare făcută vreodată, iar trei ani e chiar termenul pe care legea îl cere păstrat
(OUG 92/2021 art. 48 alin. (5)).


---

## Perimetrul de producție, partea a doua — deployul și probele pe viu (09.09.2026, noaptea)

Secțiunea de deasupra spune „șase din opt puncte P0". Era adevărat **în repo**. Prima constatare a
serii e că nu era adevărat **pe dyno**: commitul cu toate cele șase (`474347e`) stătea nedeployat,
iar producția rula în continuare codul de dimineață. Verificat, nu dedus — `flyway_schema_history`
din backup avea **31 de rânduri**, deci schema era la `V31`, nu la `V32`.

Adică, până la deployul de mai jos, producția avea încă: **CORS pe `*`**, **nicio frână** pe ușile
publice și **tokenuri de 30 de zile fără revocare**. Bifele erau bifele codului, nu ale sistemului
viu. E fix genul de decalaj pentru care `todo-lansare.md` cere „cum se probează" la fiecare punct.

### Deployul

Procedura din `prompt-continuare.md`, rulată ca scrisă: push pe monorepo, `git subtree split`,
cherry-pick peste capul fiecărui remote. Zero conflicte.

| | Înainte | După | Commit |
|---|---|---|---|
| `ecoregistru-api` | v40 | **v41** | `c9401e0` |
| `ecoregistru-app` | v35 | **v36** | `a26520a` |

Migrarea, din `heroku logs`:

```
Migrating schema "public" to version "32 - token version"
Successfully applied 1 migration to schema "public", now at version v32 (execution time 00:00.026s)
```

### Ce s-a probat pe producție, după deploy

**CORS (P0.2).** Nu pe backendul local, ci pe dyno:

```
origine străină   → 401, fără niciun Access-Control-Allow-Origin, cu Vary: Origin
originea proprie  → 401, Access-Control-Allow-Origin: https://ecoregistru-app-58d0aa109c07.herokuapp.com
preflight străin  → 403
preflight propriu → 200
cerere fără Origin → 401 (neatinsă)
```

`FRONTEND_BASE_URL` era deja exact URL-ul real al frontendului, deci deployul n-a avut nevoie de
nicio variabilă nouă — verificat **înainte** de push, fiindcă o nepotrivire acolo ar fi însemnat
frontendul închis afară din propriul API, cu un antet la care nu se uită nimeni.

**Frâna (P0.3).** Șaptezeci de încercări de login pe producție, cu adrese inexistente:

```
prima 429 la încercarea 61
Retry-After: 262
```

Exact limita configurată (60/5min pe IP) — și, mai important, dovada că numărătoarea pe IP chiar
funcționează **prin routerul Heroku**, unde `X-Forwarded-For` e scris parțial de client. Testul de
integrare nu putea să arate asta; numai dyno-ul putea.

### CI, probat în ambele sensuri (P0.7 — închis)

Punctul cerea două lucruri: verde pe `main`, **roșu la un test stricat dinadins**. Amândouă s-au
văzut, fiindcă workflow-ul pornește pe `branches: ["**"]`:

| Rulare | Ramură | Verdict |
|---|---|---|
| `34385650992` | `main` | ✅ backend 260 de teste (2m37s) · frontend `tsc` + build (26s) |
| `34386646421` | `proba-ci-rosu` | ❌ **backend picat**, frontend verde |

Ramura de probă purta o singură aserțiune întoarsă pe dos în `CorsOriginListTest` și **a fost
ștearsă** după verdict, local și pe `origin`. Faptul că a picat **doar** jobul de backend, nu
amândouă, e partea care spune că workflow-ul chiar rulează ce trebuie, nu că ar fi roșu din alt
motiv.

⚠️ **Datorie mică, semnalată de rulare:** `actions/checkout@v4`, `actions/setup-node@v4` și
`actions/setup-java@v4` sunt pe Node 20, depreciat — GitHub le forțează deja pe Node 24 și avertizează.
Nu strică nimic azi; se ridică la `v5` când se atinge fișierul.

### Backup (P0.8 — aproape închis)

Prima comandă a serii a fost și cea mai neplăcută:

```
=== Backups
No backups. Capture one with heroku pg:backups:capture
```

**Zero backupuri logice, niciodată.** Nuanța care salvează situația: `pg:info` arată
`Continuous Protection: On`, deci datele nu erau neprotejate — Heroku ține copii fizice. Dar
`Rollback: Unsupported` pe `essential-0`, nicio copie pe care s-o ținem noi, și nicio restaurare
făcută vreodată. „Protejat de furnizor" și „probat de noi" nu sunt același lucru, iar punctul cerea
al doilea.

Făcut:

- **`b001` capturat** — 135.78 KB, prima copie logică din istoria bazei.
- **Program zilnic**: `daily at 3:00 Europe/Bucharest`. Nu era cerut de P0.8, dar un punct despre
  backupuri care se închide fără ca al doilea backup să vină singur se redeschide de la sine.
- **Descărcat și citit.** Cele 27 de tabele sunt acolo, cu date:

| Tabelă | Rânduri |
|---|---|
| `waste_codes` | 842 |
| `monthly_evidences` | 180 |
| `waste_movements` | **55** |
| `reporting_deadlines` | 54 |
| `flyway_schema_history` | 31 |
| `partners` | 11 |
| `app_users` | 8 |
| `work_points` | 7 |
| `companies` | 6 |

⚠️ **Capcană de versiune, găsită aici:** producția e pe **Postgres 18.3**, mașina de dezvoltare avea
doar 16, iar `pg_restore` 16 refuză un dump de 18 — `unsupported version (1.16) in file header`.
S-a instalat `postgresql@18` (keg-only, nu atinge cel existent). Merită ținut minte și pentru altceva:
**testele rulează pe Postgres 15** (zonky, fixat în `build.gradle`), CI la fel — deci suita verde nu
spune nimic despre 18. Până acum n-a contat; într-o zi va conta.

Ce a rămas: **restaurarea propriu-zisă**. Clusterul temporar e pornit, baza-țintă e creată, dumpul e
descărcat — dar `pg_restore` e blocat de clasificatorul uneltei, ca și `heroku pg:psql`. **O singură
comandă**, rulată cu mâna, și punctul se închide cu numărul de mișcări comparat: 55.

### Cloudinary — jumătatea care se putea proba fără login (P0.5)

Credențialele sunt **valide**: `ping` întoarce `200`, contul e pe plan Free, cloud `ojituo63`.
Asta exclude ipoteza cea mai probabilă de eșec — un `CLOUDINARY_URL` scris greșit, care ar fi tăcut
exact ca un buton care înghite fișierul.

Dar listarea resurselor sub prefixul `ecoregistru` întoarce **zero obiecte**, pe toate cele trei
tipuri. Deci **nu s-a urcat niciodată nimic**, nici din producție, nici din dev. Proba cerută de
punct — fișier urcat, pagina reîncărcată, dosarul de control descărcat, fișierul găsit în
`atasamente/` — rămâne de făcut prin interfață, cu un cont care poate intra. Adică **după P0.1**.

### Colectorul de erori, aprins (P0.6)

Codul era deployat de la v41, dar inert: fără DSN nu se inițializează nimic. Puse amândouă —
`SENTRY_DSN` pe `ecoregistru-api` (**v42**) și `VITE_SENTRY_DSN` pe `ecoregistru-app` (v37).

⚠️ **Frontendul a avut nevoie de un rebuild, nu de un restart.** `VITE_*` se coace **în build**, deci
`config:set` singur ar fi lăsat bundle-ul vechi în producție, cu variabila setată și colectorul mort
— exact tipul de reparație care arată făcută și nu e. Un commit gol pe repo-ul split a forțat
reconstrucția (**v38**), iar proba e că **cheia publică a DSN-ului se găsește chiar în
`/assets/index-*.js` servit de producție**.

**Ingestia e probată, nu doar configurată:** câte un eveniment marcat („P0.6 — proba de ingestie…
se poate șterge") trimis prin API-ul de envelope în fiecare proiect, **`HTTP 200`** de la amândouă.

⚠️ Ce încă **nu** s-a văzut: un **500 adevărat**, ridicat de aplicație și trecut prin
`AdviceController.handleUnexpected`. Lanțul e complet pe hârtie — cod deployat, DSN valid, ingestie
confirmată — dar veriga dintre o excepție reală și raport rămâne neprobată până la prima.

⚠️ Regiunea de ingestie e **UE** (`de.sentry.io`), potrivit pentru un produs de conformitate din
România. De scris în politica de confidențialitate: Sentry e **subîmputernicit**, chiar dacă nu
primește date personale — fără `sendDefaultPii`, fără Session Replay, cu parametrii adresei tăiați.

### Unde a ajuns P0

| Punct | Stare la începutul serii | Acum |
|---|---|---|
| P0.1 conturi + `JWT_SECRET` | 🟡 pe jumătate | 🟡 neschimbat — blocat pe permisiune |
| P0.2 CORS | ✅ în cod | ✅ **și pe producție, probat** |
| P0.3 frână | ✅ în cod | ✅ **și pe producție, probat** |
| P0.4 sesiuni | ✅ în cod | ✅ **`V32` migrat pe producție** |
| P0.5 Cloudinary | 🟡 setat, neprobat | 🟡 credențiale valide; upload-ul lipsește |
| P0.6 Sentry | 🟡 fără DSN | ✅ **aprins pe amândouă capetele, ingestie probată** |
| P0.7 CI | 🟡 nevăzut rulând | ✅ **închis — verde pe `main`, roșu la test stricat** |
| P0.8 backup | ⬜ neatins | 🟡 `b001` + program zilnic; restaurarea lipsește |

**Trei puncte închise în plus (P0.6, P0.7 și, pe fond, P0.2–P0.4, care abia acum sunt adevărate în
producție).** Ce a rămas: **P0.1** — singura gaură deschisă — plus două comenzi blocate de
clasificator (P0.8) și proba de atașament, care oricum așteaptă P0.1. Niciuna nu e cod.

---

## Perimetrul de producție, partea a treia — P0 închis (09.09.2026, noaptea târziu)

*A treia trecere prin aceeași listă, și ultima: din cele opt puncte P0 nu mai rămâne niciunul
deschis. Două s-au închis cu comenzi, al treilea cu o probă prin interfață.*

### Restaurarea care lipsea de la backup (P0.8)

Punctul cerea, de la început, ceva ce nimeni nu făcuse niciodată: **nu o copie, ci o restaurare**.
Copia exista de dimineață (`b001`, plus programul zilnic la 03:00), dar „Heroku ține copii fizice"
și „noi am probat că se pot citi" nu sunt același lucru — iar punctul îl cerea pe al doilea.

S-a făcut local, nu într-un al doilea addon: `pg_restore` din dumpul descărcat, într-un cluster
Postgres 18 temporar. **A trecut cu cod 0, fără o eroare**, iar proba a ieșit exact: **55
`waste_movements`**, cifra producției. Și restul se potrivește — 842 `waste_codes`,
180 `monthly_evidences`, 54 `reporting_deadlines`, 11 `partners`, 6 `companies`.

Varianta locală s-a dovedit **proba mai bună**, nu compromisul mai ieftin: trece prin fișierul
descărcat, deci verifică și că dumpul chiar e citibil **în afara Heroku** — exact scenariul pentru
care există un backup. Restaurarea într-o a doua bază Heroku n-ar fi atins asta.

### Contul publicat, închis în ordinea care contează (P0.1)

Singura gaură rămasă din 24.08: un `PLATFORM_ADMIN` de producție a cărui parolă stătea în istoricul
unui repo public. Reparația are patru pași, și **ordinea lor e tot ce contează** — invers, producția
rămâne fără niciun administrator de platformă:

1. contul real devine `PLATFORM_ADMIN`, cu `company_id=null` (rolul e global; firma se alege prin
   comutator);
2. **login cu el, văzut mergând** — pasul care nu se sare, fiindcă el e plasa;
3. abia acum contul publicat se închide (`enabled=false`);
4. `JWT_SECRET` rotit — **v43**, dyno repornit și `up`.

Pasul 4 nu e opțional și nu e același lucru cu pasul 3: `V32` a scurtat tokenurile la 8 ore, dar
cele emise **înainte** poartă în ele termenul vechi de 30 de zile. Numai rotirea secretului le rupe
semnătura; închiderea contului nu atinge o sesiune deja deschisă.

Parola noului administrator **nu a fost scrisă de nimeni altcineva decât de proprietarul contului**,
prin fluxul de „Parolă uitată". Se vede în date: `token_version=1` pe contul lui, singurul din tabel
cu valoare nenulă, fiindcă `resetPassword` îl incrementează. Asta închide punctul în spiritul lui —
o gaură deschisă de o parolă cunoscută nu se repară cu altă parolă cunoscută.

Starea finală a tabelului: **două conturi active** din opt — administratorul real și un `OPERATOR`.
Toate celelalte șase (demo, smoke, brutărie) sunt `enabled=false`.

✅ **Probele au ieșit (09.09.2026, târziu de tot).** Amândouă rulate cu mâna, fiindcă amândouă
ating producția: `POST /auth/login` cu contul publicat și parola din istoricul git întoarce acum
**`400`** — dădea `200` la 21:30 — iar `GET /companies` cu un token păstrat dinainte de rotire
întoarce **`401`**, unde dădea `200`. Punctul e închis cu probă, nu cu intenție.

⚠️ **Dar a doua probă nu izolează rotirea, și merită spus înainte de a ne bizui pe ea.** Tokenul
păstrat e al contului publicat, iar contul acela e acum și `enabled=false`; din `V32`, `enabled` se
verifică la **fiecare** cerere, deci `401` ar fi ieșit și fără nicio rotire de secret. Ce arată
proba e că **sesiunea publicată e moartă** — exact ce contează operațional. Ce nu arată e că
`JWT_SECRET`-ul nou chiar s-a aplicat. Proba curată a rotirii e alta și e gratis: **administratorul
real, rămas activ, a fost dat afară din browser** de v43 — dacă aplicația îi cere să se
re-autentifice, aia e semnătura ruptă, nu `enabled`.

⚠️ **Ce rămâne deschis ca întrebare, nu ca gaură:** drumul pe care conturile demo au ajuns în
producție tot nu s-a găsit. `DevDataSeeder` e `@Profile("dev")` și `SPRING_PROFILES_ACTIVE` e gol pe
dyno, deci n-a fost seeder-ul — cel mai probabil un dump încărcat cândva. Conturile sunt închise;
drumul, dacă e deschis, nu e.

### Atașamentele urcă — și de ce tot nu se văd (P0.5)

Cu login-ul deblocat, proba cerută de punct s-a putut face în sfârșit prin interfață. **Upload-ul
merge:** un PDF pus pe o mișcare a ajuns la `.../ecoregistru/movements/<uuid>/<...>.pdf`. Contul care
întorcea **zero** obiecte pe toate cele trei tipuri de resursă are acum conținut — butonul nu mai
înghite fișierul.

Fișierul însă nu se deschidea: **`401`**. Nu e defect în codul nostru, ci restricția implicită de
cont a lui Cloudinary — livrarea de PDF și ZIP e **oprită din fabrică**, și se aprinde dintr-un click
în Console → Settings → Security. Diagnosticul exclude celelalte cauze: același URL cerut ca
`/raw/upload/` dă **404**, deci fișierul chiar e stocat ca `image`. Motivul e `resource_type: auto`,
care clasifică PDF-urile ca imagini — de aceea intră sub restricția aceea și nu sub alta.

✅ **Bifa a fost pusă, și livrarea e probată.** Ambele fișiere de sub `ecoregistru/movements/`
întorc acum **`200`**, `application/pdf`, 7204 octeți — și ce vine înapoi **chiar e un PDF**, nu o
pagină de eroare servită cu cod 200: `PDF document, version 1.5, 3 pages`. Cu asta, punctul e închis
cap-coadă, și odată cu el **toate cele opt** din P0.

⚠️ **Reținut pentru altă dată:** setarea de livrare PDF e **la nivel de cont și trece peste**
controlul de acces al fișierului. Deci ea **nu** se poate ocoli făcând assetul `authenticated` cu URL
semnat — verificat înainte de a construi pe ipoteza contrară, care era comodă și greșită. Cele două
straturi sunt independente.

Merită reținut ca formă de eșec: un `401` de la un CDN arată exact ca o problemă de credențiale, dar
credențialele erau bune de la 02.09 (`ping` → `200`). Cauza era o bifă de cont, la două niveluri
distanță de codul nostru.

**Legat de asta, o felie de luat înainte de primul client cu atașamente reale:** livrarea trece de la
URL public la **URL semnat, cu expirare**, generat de backend la cerere. Motivele și proba sunt în
lista de lansare, punctul 11-bis.

🔴 **Și proba aceea nu mai e o deducție din citit cod.** Comanda care a arătat că livrarea merge a
fost un **`curl` gol** — fără sesiune, fără `Authorization`, fără cookie — și a descărcat un
**document de producție**. Aceeași comandă e, cuvânt cu cuvânt, demonstrația găurii: `AttachmentResponse.url`
e `secure_url` de la Cloudinary, deci fiecare atașament e un link public, fără verificare de tenant.
Merită spus limpede, fiindcă e felul în care punctul ăsta s-a purtat de două ori: **proba cerută de un
punct a scos la iveală ceva mai mare decât punctul.**

### Unde a ajuns lista

| Punct | Dimineață | Seară | **Noaptea târziu** |
|---|---|---|---|
| P0.1 conturi + `JWT_SECRET` | 🔴 deschis | 🟡 pe jumătate | ✅ **închis — cont dezactivat, secret rotit (v43), probe `400`/`401`** |
| P0.5 Cloudinary | 🟡 credențiale valide | 🟡 upload neprobat | ✅ **upload și livrare probate — `200`, PDF valid** |
| P0.8 backup | ⬜ neatins | 🟡 fără restaurare | ✅ **restaurat, 55 de mișcări** |

**P0 e închis, toate opt, fiecare cu probă.** Ce ține acum lansarea pe loc nu mai e nici dyno, nici
funcție de produs: e **P1, adică juridic** — SRL, contract-cadru, DPA, termeni, politică de
confidențialitate. Prima oară de la 24.08 când lista nu mai are un punct tehnic în față.

Cu o singură excepție, și e una pe care tot P0 a produs-o: **11-bis**, atașamentele mutate pe URL
semnat. E cod, e înaintea juridicului în ordine, și e acolo fiindcă un consultant întreabă de ea
**înainte** să semneze DPA-ul, nu după.

---

## 11-bis — atașamentele nu mai stau la un URL public (09.09.2026, târziu de tot)

*Prima felie de cod de după închiderea lui P0, și una pe care P0 a produs-o: proba de livrare a lui
P0.5 a fost un `curl` gol care a descărcat un document de producție. Punctul era scris în lista de
lansare ca observație din citit cod; s-a închis ca demonstrație.*

### Ce era

`CloudinaryStorageService.upload` lua `secure_url`, îl scria în `Attachment`, iar
`AttachmentResponse.url` îl trimitea clientului, care îl punea într-un `<a href>`. Deci fiecare
atașament era **un link public**: fără autentificare, fără sesiune, **fără nicio verificare de
tenant**, valabil pe veci. Singura apărare era că adresa e greu de ghicit — iar aplicația e
multi-tenant și prin ea trec avize, contracte și acte de identitate de șofer; la Etapa 9 intră
**CNP**, pe borderoul de achiziție la metale.

### Ce e acum

Fișierele urcă `type=authenticated`, pe care Cloudinary refuză să-l livreze fără semnătură, iar
conținutul se citește printr-un endpoint al nostru —
`GET /api/v1/movements/{id}/attachments/{aid}/continut` — care trece prin aceeași verificare de
tenant ca orice altă citire. `AttachmentResponse` **nu mai are câmpul `url`**, iar frontendul cere
fișierul cu sesiunea omului și îl deschide dintr-un `blob:`.

Adresa semnată se construiește pe server și **se folosește tot acolo**. Nu pleacă spre client, și
motivul e capcana în jurul căreia e construită felia: pentru un asset `authenticated` **URL-ul
semnat *este* acreditarea**, și nu expiră. Dacă l-am fi stocat și trimis mai departe — varianta
comodă, fiindcă Cloudinary îl întoarce gata semnat la upload — am fi refăcut exact gaura pe care o
înlocuim, cu un `s--…--` în plus.

### Ce s-a măsurat înainte de a se scrie

Trei lucruri, toate pe contul real, fiindcă toate trei ar fi trecut de compilator și ar fi picat în
producție:

| Ce | Rezultat |
|---|---|
| același asset `authenticated`, cerut nesemnat | **401** |
| același asset, cerut semnat | **200** |
| `/v1/` pe care îl generează SDK-ul, în loc de versiunea reală | **200** |

Al treilea a ieșit dintr-un test picat, nu dintr-o bănuială: SDK-ul forțează un `/v1/` pe orice
public id cu slash și adaugă un parametru `?_a=` de analytics. Amândouă sunt inofensive — Cloudinary
tratează versiunea drept cache-buster, nu drept parte din căutare, iar semnătura se calculează doar
peste public id și format. Dar „inofensiv" era o deducție, iar un URL care doar *pare* corect nu se
deosebește de unul corect până când Cloudinary răspunde 401, și atunci e un fișier pe care clientul
nu-l poate deschide.

⚠️ **Ce nu rezolvă felia asta:** restricția de livrare PDF de la P0.5 e o setare **de cont** și trece
peste controlul de acces al fișierului. Un asset `authenticated` e blocat de ea exact ca unul public,
deci bifa aia rămâne pusă. Verificat înainte de a construi pe ipoteza contrară.

### Migrarea, și rândurile vechi

`V33` adaugă `resource_type`, `delivery_type` și `format`, toate nullable — aditivă, ca toate
celelalte. **NULL e semnalul, nu o scăpare:** înseamnă „asset dinainte de 11-bis", iar codul cade
înapoi pe `url` pentru el.

⚠️ **Cele două fișiere de pe producție rămân public livrabile.** Sunt probele lui P0.5, duplicate ale
aceluiași PDF, și stau la `type=upload`; assetul e la Cloudinary, deci nicio migrare nu-l poate muta.
Se sting urcându-le din nou sau ștergându-le din Console — de făcut cu mâna, e producție.

### Dosarul de control

`AuditFileService` semnează acum URL-ul pe loc și îl aruncă. Nu mai apare în `atasamente/index.txt`,
unde stătea ca ajutor pentru cititor când o descărcare eșua — adică **un link public către documentul
unui client, într-o arhivă pe care clientul o trimite unui inspector**. Arhiva poartă fișierul sau
spune că lipsește; nu mai poartă și drumul spre el.

### Proba

- **267 de teste verzi** (erau 260): 4 în `AttachmentAccessIT` — fără sesiune `401`, alt tenant `404`,
  proprietarul primește octeții, iar răspunsul mișcării **nu conține niciun `http`** — și 3 în
  `CloudinaryStorageServiceTest`, care fixează `type=authenticated` la upload și forma URL-ului semnat.
- **Suita 8 de interfață, verde**, cu două verificări noi: niciun link în dialogul de atașamente și
  nicio adresă de Cloudinary în tot HTML-ul lui. Cea veche cerea exact pe dos (`<a href>` cu
  `target=_blank`) — a fost rescrisă, fiindcă acela era contractul greșit.
- **Pe aplicația pornită:** același atașament, `401` fără sesiune și `200 · image/jpeg · 109669
  octeți` cu ea — JPEG adevărat, nu o pagină de eroare.

---

## 11-bis pe producție, limita adevărată și tabul care descărca (09.09.2026, târziu de tot)

*Trei lucruri într-o felie, și niciunul n-a fost găsit de compilator. Primul e un deploy; al doilea
e un commit pierdut la deployul dinainte; al treilea e un defect pe care l-a găsit **utilizatorul,
deschizând fișierul** — după ce toate probele automate spuseseră că 11-bis e închis.*

### 11-bis e pe dyno, nu doar în `main`

`ecoregistru-api` **v43 → v44** (`d3e7fda`), `ecoregistru-app` **v38 → v39** (`d89a279`). `V33` a
migrat acolo: *„Successfully applied 1 migration to schema «public», now at version v33"*.

Probele, luate pe producție după deploy — și dintre ele contează **ultima**:

| Ce s-a cerut | Răspuns |
|---|---|
| `/movements/{id}/attachments/{aid}/continut`, fără sesiune | **401** |
| același, cu `Authorization` inventat | **401** |
| URL brut Cloudinary, nesemnat (cu și fără versiune) | **401** |
| același, cu semnătura stricată | **401** |
| același `public_id` pe ruta publică `/upload` | **404** |
| **URL-ul semnat corect** | **200 · application/pdf** |

Rândul de jos e cel care face proba să însemne ceva: fișierul **este** livrabil, deci cele patru
`401` sunt despre semnătură, nu despre un fișier lipsă. Aceeași comandă care pe 09.09 dimineața a
descărcat un document de producție întoarce acum `401`.

Cele două fișiere de probă rămase `type=upload` **au fost șterse** de proprietar, din Consolă.
Verificat prin Admin API, pe toate cele patru combinații (`image`/`raw` × `upload`/`authenticated`):
sub prefixul `ecoregistru` nu mai există **niciun** obiect public. Fișierul urcat după deploy apare
la `image/authenticated`, iar `image/upload` a rămas **0**.

### Procedura de deploy pierduse un commit, în tăcere

`newrepo/main` și `ferepo/main` nu conțineau `5a11865` — commitul care scotea parola conturilor demo
din cod. Nu se vedea de nicăieri: push-ul mersese, releaseul ieșise, iar probele de pe dyno
trecuseră, fiindcă niciuna nu se uita la fișierul lipsă. Chiar și verificarea scrisă în procedură —
`heroku releases` arată hash-ul așteptat — spunea „da": hash-ul *era* corect, doar conținutul era
incomplet.

Cauza e chiar linia din procedură: `git cherry-pick <tmp-backend>` ia **un singur** commit, vârful.
Când sunt două în așteptare — și pe 09.09 seara erau două — al doilea dispare fără niciun semn.

Reparat în deployul ăsta, cu cherry-pick de două commituri pe fiecare parte. Și, mai important,
procedura are acum o verificare **înainte de push**, care nu se uită la commituri, ci la conținut:

```bash
git diff --stat split-backend  tmp-backend    # doar .gitignore
git diff --stat split-frontend tmp-frontend   # .gitignore + vite.config.js/.d.ts
```

Divergența dintre monorepo și repo-urile de deploy e **stabilă și cunoscută**; orice fișier în plus
în stat înseamnă un commit rămas pe drum. Un commit se poate pierde; o diferență de conținut, nu.

### Limita de mărime: cifra afișată era peste zidul adevărat

Trei etaje, și cel mai strâns era ultimul, iar cel care vorbea cu omul era cel mai larg:

| Etaj | Era | Ce făcea |
|---|---|---|
| `file-dropzone.tsx` | 15 MB | singurul gardian — și rulează în browser |
| `application.yml` (multipart) | 25 MB | nu oprea nimic din ce trecea de dropzone |
| Cloudinary, plan Free | **10 MB / asset** | zidul adevărat, și nimeni nu-l știa |

Un PDF de 12 MB trecea de dropzone, trecea de backend și cădea **la furnizor**, unde nu-l prindea
niciun handler — deci omul primea o eroare care nu spunea nimic. Iar pe API direct, fără browser,
treceau 25 MB: verificarea din dropzone e o curtoazie, nu o pază.

Acum: pragul e **10 MB, verificat pe server** în `WasteMovementService`, **înainte** de upload — un
`400` dat după ar fi lăsat fișierul urcat și rândul nescris. Dropzone-ul afișează aceeași cifră,
multipart-ul a coborât la 12 MB ca plasă *sub* care stă verificarea noastră, nu peste ea, iar
`MaxUploadSizeExceededException` are handler: `400` cu mesaj, în loc de un 500 raportat la Sentry ca
defect când e o cerere greșită.

### `noopener` făcea ca fiecare atașament să se descarce

Găsit de utilizator, deschizând fișierul din aplicație: în loc să se deschidă în tab, se descărca.
Toate probele automate ale lui 11-bis trecuseră — fiindcă niciuna nu privea ce face browserul cu
octeții după ce sosesc.

```js
const tab = window.open("", "_blank", "noopener,noreferrer");
```

Specificația HTML spune că, atunci când `noopener` e prezent, `window.open` întoarce **`null`** — n-ai
cum să primești un mâner către o fereastră de care tocmai te-ai lepădat. Deci `tab` era `null` de
fiecare dată, ramura `if (tab)` era cod mort, și fiecare atașament ajungea pe calea de rezervă,
`saveBlob`. Verificat în Chrome, nu dedus din act: cu `noopener` → `null`, fără → obiect.

`tsc` n-avea ce să obiecteze: tipul lui `window.open` chiar include `null`, iar codul trata `null`
— corect, doar că pentru cazul greșit. Ce voia `noopener` să apere (o pagină străină care citește
`window.opener`) nu se aplică unei adrese `blob:` din propria origine; e tăiat oricum, explicit, cu
`tab.opener = null`.

### Proba

- **269 de teste verzi** (erau 267): două noi în `AttachmentAccessIT` — un fișier peste prag
  întoarce `400` **și storage-ul nu e chemat deloc**, iar unul exact la prag trece. Al doilea nu e
  decor: fără el, un prag pus din greșeală la zero ar fi trecut testul de deasupra.
- `tsc --noEmit` curat, `vite build` verde (611,84 kB, de la 610).
- Pe producție, după deploy: tabelul de `401`/`200` de mai sus.
- ✅ **Proba omului, cu sesiune reală, pe producție** (09.09.2026, după v45/v40): un atașament urcat
  din aplicație **s-a deschis în tab**, iar adresa din bară era
  `blob:https://ecoregistru-app-58d0aa109c07.herokuapp.com/…` — adică exact ramura
  `tab.location.href = url` din `useAttachmentOpen`. Calea de rezervă n-ar fi lăsat nicio adresă în
  bară, ci un fișier în Descărcări; deci rândul ăsta e proba care închide defectul cu `noopener`
  acolo unde contează. Odată cu el, drumul întreg — **urcare → stocare `authenticated` → citire prin
  endpointul nostru → deschidere în browser** — e probat cap-coadă pe producție, nu doar pe stiva
  locală. Era singurul lucru pe care nici compilatorul, nici testele, nici suita de interfață nu
  aveau cum să-l vadă.
- **Deployat:** 11-bis la `ecoregistru-api` **v44** / `ecoregistru-app` **v39** (cu `V33` migrat);
  limita de mărime și reparația lui `noopener` la **v45** / **v40** (fără migrare).
- ✅ **Zidul din browser, probat pe producție** (09.09.2026): un PDF de peste 10 MB — `schite
  meeting andreea 23august 2026.pdf` — **nu s-a pus la coadă deloc**, iar mesajul a fost exact cel
  din `strings.ts`: „Prea mari, peste 10 MB: …". Deci `MAX_FILE_MB` oprește fișierul **înainte** de
  orice octet urcat, nu la furnizor, cum se întâmpla la 15 MB.
- ⚠️ **Zidul de pe server n-a fost atins, și nu poate fi atins din interfață.** Cele două praguri
  sunt egale (10 MB), deci dropzone-ul refuză fișierul înaintea oricărei cereri — `MAX_ATTACHMENT_BYTES`
  din `WasteMovementService` nu vede niciodată un fișier prea mare venit prin ecran. **Singurul drum
  către el e un `curl` cu tokenul de sesiune**, adică exact calea de care pragul de server există în
  primul rând (cel din browser e curtoazie: un client care nu e browserul nostru nu-l vede). Așteptat
  acolo: `400` cu „Fișierul e prea mare. Cel mult 10 MB per fișier.", storage-ul nechemat — ce
  probează azi cele două teste din `AttachmentAccessIT`, dar pe stiva de test, nu pe dyno.
- ⚠️ **Suita de interfață n-a fost rulată** pe felia asta — cere stiva pornită și o bază locală.
  Suita 8 nu deschide atașamentul (verifică doar că dialogul n-are linkuri), deci nicio verificare
  existentă nu acoperea defectul cu `noopener` și niciuna nu se strică. **De rulat la următoarea
  atingere de ecran**, cu o verificare nouă care chiar apasă butonul de deschidere.

---

## P1.12 — o firmă își administrează utilizatorii (10.09.2026)

**Ce era.** `CompanyController` avea `POST /{id}/users` — invitație, platform-only, și atât. Nu se
listau, nu se retrimitea invitația, nu se dezactiva nimeni. O firmă cu trei angajați lovește asta în
ziua întâi, iar singurul drum era să ne sune.

**Ce e acum.** Controller nou, `/api/v1/users`, **tenant-scoped** — spre deosebire de
`CompanyController`, care e global prin construcție. Nu există id de firmă în nicio cale: firma e cea
din sesiune, deci un `ADMIN` de client ajunge exact la colegii lui și la nimeni altcineva.
Administratorul de platformă ajunge la același ecran prin comutatorul de firme, deci e **o singură
implementare**, nu o copie de client a uneia de personal. Ecranul e o secțiune în **Setări**, lângă
punctele de lucru și șoferi — sunt același fel de lucru: date pe care firma și le ține singură.

### Cele trei stări, și de ce a fost nevoie de o migrare

`enabled = false` purta **două înțelesuri** pe aceeași coloană: „invitat, n-a apăsat încă linkul și
n-are parolă" și „dezactivat de administrator". Câtă vreme singura operație era invitația,
ambiguitatea nu se vedea nicăieri. Ecranul o scoate la iveală de două ori:

* **lista** n-ar fi avut ce scrie în dreptul rândului — două stări opuse, aceeași valoare în bază;
* **„Reactivează"** pe un invitat i-ar fi pus `enabled = true` peste parola aleatoare pe care
  `inviteUser` o generează. Un rând care scrie **Activ** și nu se poate autentifica niciodată, fără
  ca nimic de pe ecran să spună de ce.

`V34` adaugă `deactivated_at` — o dată, nu un boolean, fiindcă răspunde și la „de când", care e prima
întrebare pusă când pleacă un angajat. Starea se citește din pereche, într-un singur loc
(`CompanyUserResponse.from`), deci nu e o a treia coloană care poate să nu fie de acord cu primele două.

### 🔴 Ce a scos la iveală proba pe serverul viu — încă o dată, altul decât ce vedeau testele

Prima versiune lăsa un invitat să fie **dezactivat**. Cele 17 teste treceau, `tsc` era curat,
`vite build` verde. Dar drumul înapoi construia **exact rândul stricat pe care `V34` există ca să-l
prevină**, mutat cu un pas mai încolo: invitat → dezactivat → reactivat = Activ, peste o parolă pe
care n-a văzut-o nimeni.

Reparat **prin construcție, nu cu încă o verificare**: o invitație nu se dezactivează, se **anulează**.
`DELETE /users/{id}/invitation` chiar șterge rândul — singurul loc din aplicație unde se șterge ceva,
și se poate **doar** aici, fiindcă un cont în care nu s-a intrat niciodată nu e `created_by` la nicio
mișcare, evidență sau atașament. Ce câștigi e adresa liberă înapoi, adică fix ce vrei după o greșeală
de tastare, fiindcă `inviteUser` refuză un email care există deja. Cu asta, **„Dezactivat" se atinge
numai din „Activ"**, deci oricine e acolo are parolă — invariantul e adevărat prin drumuri, nu prin
grijă.

Modelul e cel scris pe 09.09 și repetat acum: *o felie se poate proba corect și complet pe stratul pe
care l-ai ales, și să fie greșită pe cel la care nu te-ai uitat.* Aici stratul nevăzut n-a fost
browserul, ci **spațiul stărilor**.

### Gardele — ce poate o firmă să-și facă singură, și nu poate

| Ce | Cum e oprit |
|---|---|
| Îți dezactivezi sau îți retrogradezi **propriul** cont | refuzat. Sesiunea moare la cererea următoare, deci greșeala n-ar fi reparabilă din aplicație — ar trebui să ne sune cineva |
| Dezactivezi sau retrogradezi **ultimul `ADMIN` activ** | numărat, nu sperat. Firma ar rămâne fără nimeni care să invite, să promoveze sau să reactiveze |
| Un admin **invitat**, care n-a pus parola, ținut drept acoperire | nu contează: numărătoarea e a celor `enabled`. Nu poate lăsa pe nimeni înapoi |
| `PLATFORM_ADMIN` atins dintr-un tenant | **structural imposibil**: are `company = null`, iar toate căutările sunt scoped pe firmă. Nu e o verificare care se poate uita |
| `PLATFORM_ADMIN` acordat | refuzat pe intrare, aceeași regulă pe care invitația o are de la început |
| Utilizatorul altei firme | **404**, nu 403 — răspunsul nu confirmă că id-ul e real în altă parte |

**Dezactivarea incrementează contorul de sesiuni**, deși `enabled` singur ar închide sesiunea (P0.4 îl
verifică la fiecare cerere). Motivul e **drumul înapoi**, nu cel înainte: tokenurile trăiesc 30 de
zile, deci cineva dezactivat luni și reactivat miercuri ar regăsi valabil fiecare token emis înainte
de luni — inclusiv cel de pe laptopul care a fost motivul dezactivării. Reactivarea dă înapoi contul,
nu sesiunile vechi.

**Retrimiterea invitației** refolosește fluxul de resetare, ca invitația inițială: stinge linkul
neconfirmat rămas (două linkuri vii pentru un cont e cu unul mai mult decât trebuie), face unul nou,
trimite mailul. E numărată la aceeași frână ca resetarea de sine (`RESET_PER_EMAIL`, trei pe oră) —
căsuța protejată e a invitatului, și nu-i pasă că cererea a venit de la un administrator autentificat.
Se retrimite **doar** unui cont care n-a intrat niciodată: pe unul cu parolă, ar fi însemnat un
administrator care poate emite un link de resetare pentru contul viu al unui coleg, adică altă
funcționalitate, și mai proastă.

### Ce a ieșit la privitul ecranului, nu din DOM

Pe **375px**, coloana de acțiuni — lipită la dreapta — măsura **352px într-un container de 341**,
deci acoperea complet emailul, numele, rolul și starea: un tabel din care se vedeau numai butoane
roșii, fără să știi al cui e rândul. Cauza: „Retrimite invitația" și „Anulează invitația" una lângă
alta. Etichetele de pe rând s-au scurtat la „Retrimite" / „Anulează" (textul întreg rămâne în
`aria-label` și în confirmare) → **241px**, în linie cu celelalte secțiuni (puncte de lucru 265,
șoferi 85). ⚠️ Prima captură arăta același defect **și** ca artefact: `scrollIntoViewIfNeeded()`
derulase containerul orizontal ca să aducă în cadru coloana lipită. Măsurat cu `scrollLeft`, ca să nu
se repare fantoma în loc de defect.

### Probe

* **288 de teste** (erau 269) — `CompanyUsersIT`, 19 verificări, fiecare gardă cu testul ei.
  ⚠️ Prima versiune a clasei **se otrăvea singură**: un test lăsa în urmă un al doilea `ADMIN` activ
  pe firma demo, ceea ce dezarma garda de „ultim administrator" în testele următoare — care atunci
  chiar dezactivau `admin@demo.ro`, și tot restul clasei ieșea 401/403 din motive care n-aveau
  legătură cu ce testau. Reparat dându-le **firmă proprie**, nu curățenie după.
* `tsc --noEmit` curat, `vite build` verde (621,94 kB).
* **Suita de interfață verde**, cu **7 verificări noi** care chiar apasă butoanele: invitat →
  „În așteptare" cu exact cele două acțiuni potrivite și **fără** „Dezactivează" → rândul propriu
  marcat „(tu)" și fără butoane → „Anulează" întreabă → rândul dispare. Regula de pe 09.09, aplicată:
  ce nu apasă un buton n-a probat butonul.
  ⚠️ Suita 7 mai avea o **bombă cu ceas** care a explodat acum: lasă în urmă câte un șofer inactiv la
  fiecare rulare, iar la a 26-a rândul căutat a trecut de pagina de 25 și proba a căzut cu ecranul
  neschimbat. Verificarea se restrânge acum prin căutare, deci nu mai depinde de vechimea bazei.
* Pe serverul local pornit: drumul întreg prin `curl` — invitație → `PENDING_INVITE`, retrimitere
  `204`, dezactivarea unui invitat `400 user.still.pending`, anulare `204`, adresa liberă din nou
  `200`, autodezactivare `400 user.cannot.manage.self`, operator pe listă `403`.

### Deploy — 10.09.2026, 11:47

`ecoregistru-api` **v46** (`6c6011f`), `ecoregistru-app` **v41** (`4b990ad`). Flyway pe dyno:
*„Migrating schema public to version 34 - user deactivation"*, aplicată în 13 ms, iar aplicația a
pornit după ea (`State changed from starting to up`). `GET /api/v1/users` fără token dă **401**,
deci ruta există și e închisă.

⚠️ **Garda de conținut de la pasul 3 a trecut curat de două ori** — backend doar `.gitignore`,
frontend `.gitignore` + cele două artefacte vite, adică exact divergențele stabile. Deci nu era
niciun commit rămas pe drum de la deployul anterior.

**Și încă un pas peste hash:** bundle-ul servit de producție (`index-C0OdtWkd.js`) a fost descărcat
și căutat în el — conține „Utilizatorii firmei", „Retrimite invitația" și `resend-invite`. Lecția
din 09.09 spune că un hash corect nu garantează conținut complet; singurul lucru care garantează
e conținutul.

⬜ **Ce nu s-a probat încă:** drumul pe ecran cu sesiune reală de `ADMIN` — invitație →
retrimitere → parolă → dezactivare dintr-o altă sesiune. E scris pas cu pas în `todo-lansare.md`,
la punctul 12. Până atunci, felia e **deployată**, nu **probată pe producție** — regula 2 se aplică
la fel și când tot ce e automat e verde.

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
| 6 | ✅ **Dosar de control pe mai mulți ani** — livrat 24.08.2026 (folder per an, avertisment pe anul fără evidență). Plaja a crescut la **`years=1..5`** pe 25.08, la cererea specialistei: 3 ani e ce cere inspecția (OUG 92/2021 art. 48(5)), 5 e marja ei | 4 | **GATA** |
| 7 | ✅ **Obligațiile AFM ca set de contribuții + trei cadențe** — livrat 25.08.2026 (`V21`, Grupul 5) | — | **GATA** |
| 8 | 🔜 **Modul depozit — ecrane** (Recepții/Livrări, registru art. 48, formulare HG 1061, ceas SIATD) — **următorul** | 2 | L |
| 9 | Borderou de achiziție la metale (OUG 31/2011) + regim GDPR pentru CNP | 8 | M |
| 10 | Profil groapă (registru recepție HG 349 art. 15, raportare semestrială, alertă 12h) | 8 + cuantumul din anexa 2 | M |
| 11 | ✅ Modul ambalaje (Ordin 794/2012, **în kg**) — **complet**: Anexa 1 Ambalaje în `.xls` (`V22`, `V26`, `V27`, 25.08) și **Anexa 3 Ambalaje** (`V31`, 06.09) | 8 | **GATA** |

**Restanțe mici (S, se pot lua oricând, nu blochează nimic):**

- ✅ ~~**Căutarea de coduri e sensibilă la diacritice.**~~ **Rezolvat pe 24.08** (`V17`): cine
  tastează „deseuri" găsește „deșeuri". *(Rândul rămâne corectat, nu șters — descria un defect
  real timp de două zile.)*
- **`total_collected` și `total_handed_over` au rămas în schemă nescrise.** Se curăță în Etapa 8,
  odată cu mișcările `COLLECTED`.
- **Alerte de expirare a autorizației de partener (<60 zile)** — ultimul slice al fazei TERMENE.
  Cere o coloană de dedup pe `partners`, altfel trimite e-mail zilnic.

**Etapa 2 e livrată integral (2a–2d, 23.08.2026); G1, G2 și G3 sunt livrate peste ea.** Următoarea
migrare liberă era atunci **`V13`** (`V5` = seam-ul de registru, `V6` = modelul de stoc, `V7` =
modulul de generatori, `V8` = profilul de cont, `V9` = cererile de cont, `V10` = Anexa 3, `V11` =
tipul de partener, `V12` = transportul din cap. 2). ⚠️ **Azi (02.09.2026) următoarea liberă e
`V29`** — numărul de mai sus e de pe 23.08 și e păstrat ca instantaneu.

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
  ✅ **Închis pe 02.09.2026: nu era o decizie de luat.** Un act abrogat nu devine autoritate fiindcă
  apare într-un model — șablonul lor e vechi, atât. Tipărim OUG 92/2021 și rămâne așa.
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

- ✅ ~~**Două abateri confirmate**~~ — **reparate în aceeaşi zi**, împreună cu alte patru puncte la
  care actul răspundea singur. Vezi secţiunea „Reparaţiile auditului care nu depindeau de nimeni".
  Rămân **opt** puncte, toate blocate pe răspunsurile **AE–AN**.
- 🔴 **N-avem niciun model de formular anexa 2 la HG 1061/2008** — transportul deşeurilor
  **periculoase** (întrebarea **AI**). E acelaşi tip de blocaj ca AD, din acelaşi motiv: refuzăm să
  inventăm un formular oficial. Deosebirea e că aici nu blochează o etapă viitoare, ci lasă o gaură
  în modulul **deja livrat** — un generator cu ulei uzat sau tuburi fluorescente n-are ce tipări.
- 🔴 **N-avem niciun model de registru art. 48** (întrebarea **AD**, deschisă 02.09.2026). Tot
  corpusul primit e despre fișa de gestiune, declarația anuală, Anexa 3 și ambalaje. Despre evidența
  cronologică lunară a mărfii preluate de la terți — nimic.
  **De ce contează:** OUG 92/2021 art. 48 alin. (1) descrie **conținutul** (lit. a–c) și spune că se
  depune „în sistemul pus la dispoziție de APM", dar **n-are anexă cu facsimil**, cum are HG 856/2002
  pentru fișă. Formatul nu se poate citi din act. Ecranele Etapei 8 se pot construi și fără model —
  schema există din `V5` — dar **exportul ar fi ghicit**, iar asta contrazice regula pe care stă tot
  proiectul: nu inventează formate oficiale. De cerut **înainte** de Etapa 8, nu în timpul ei.

*Verificate pe dyno la 02.09.2026: `CLOUDINARY_URL` și `SPRING_PROFILES_ACTIVE` continuă să fie
nesetate pe `ecoregistru-api`; cele cinci variabile `MAIL_*` sunt setate.*

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
- ✅ **Conturile de demo din baza de producție — închise (09.09.2026, noaptea târziu).** Toate
  conturile demo sunt `enabled=false`, inclusiv `platform@ecoregistru.ro`, care era `PLATFORM_ADMIN`;
  administrarea de platformă a trecut pe un cont real, cu parolă pusă de proprietar prin fluxul de
  resetare. **`JWT_SECRET` a fost rotit** (v43) — pasul fără de care închiderea conturilor n-ar fi
  atins sesiunile deja deschise, fiindcă tokenurile emise înainte de `V32` poartă termenul vechi.
  Detaliile şi ordinea paşilor: secţiunea „Perimetrul de producţie, partea a treia".
  ⚠️ **Ce rămâne de aflat:** *cum* au ajuns acolo. `DevDataSeeder` e `@Profile("dev")` şi
  `SPRING_PROFILES_ACTIVE` e gol pe dyno — deci n-a fost seeder-ul, cel mai probabil un dump încărcat
  cândva. Conturile sunt închise; drumul, dacă a rămas deschis, nu e.
- ✅ **Cloudinary (upload real) — probat pe 09.09.2026, noaptea târziu.** `CLOUDINARY_URL` setat
  (v40), credențiale valide (`ping` → `200`), iar acum și **un fișier real urcat prin interfață**, sub
  `ecoregistru/movements/` — contul care n-avea niciun obiect are conținut. 🟡 Fișierul nu se
  **deschide** încă (`401`): e restricția implicită de cont „Allow delivery of PDF and ZIP files",
  un click în Console → Settings → Security, nu o problemă de cod. Vezi „partea a treia".

**Închis pe 22.08.2026:** ✅ *Unitatea din Anexa 3 la Ordinul 794/2012* — actul scrie `[kilograme]`
la toate cele cinci anexe. Fișierul în tone al specialistei e șablon modificat local. Modulul de
ambalaje nu mai e blocat pe unitate.

Model de produs: Faza 1 = „pregătim, nu transmitem" — ținem evidența și generăm ce trebuie raportat
(SIM/AFM), clientul încarcă în portalul oficial (portalurile n-au API public de transmitere de la terți).
