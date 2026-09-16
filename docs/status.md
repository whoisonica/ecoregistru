# Stadiu — EcoRegistru

Jurnalul feliilor livrate, în ordinea în care au fost construite. Fiecare intrare marcată ✅
rulează local și are testele verzi.

> 📚 **Jurnalul de dinainte de 15.09.2026** (feliile din 22.08–14.09, cu G-1…G-10, P0, 11-bis, P1.10–P1.12, P3.1 și
> auditul QA) e în [`istoric/status-pana-la-14.09.2026.md`](istoric/status-pana-la-14.09.2026.md). Aici rămân doar
> intrările noi; o intrare nouă se scrie tot în capul acestui fișier.


> **16.09.2026, 15:12 și 15:14 — ✅ pe producție: aplicația mobilă M1c („A venit controlul”, dosarul prin partajare, push Expo).**
> `ecoregistru-api` **v97** (`7ebbcbb`, **migrarea `V56`**, schema 55 → 56), `ecoregistru-app` **v80** (`75a1ae5`); monorepo
> `main` = `origin/main` = `deploy/heroku-split` = `23cd96c`. Pe producție: `Migrating schema "public" to version "56 - device
> session push token"`, `now at version v56`, `Started EcoRegistruApplication` (12:12:42 UTC); `/actuator/health` 200,
> `PUT /api/v1/auth/devices/{id}/push-token` 401 fără token. Garda de conținut curată (backend `^^`, frontend `^`).
> **`PUSH_ENABLED` nesetat pe api** → push-ul rămâne oprit până când Expo e în registrul art. 30 și în DPA. Pe web intră doar
> refactorul `lib/readiness.ts` (fără efect vizibil). `V54` a rămas nefolosit (M1c a fost renumerotată după D2.2).
> Suita backend **826 de teste, 102 clase, 0 eșecuri**; `tsc` curat în `frontend` și `mobile`. **Următoarea migrare liberă: `V57`.**

> **16.09.2026, 13:41 — ✅ pe producție: depozitul D2.2, șoferii extinși (după D2.1 flota, 13:20, api v95 / app v78, `V53`).**
> `ecoregistru-api` **v96** (`704b817`, **migrarea `V55`**, schema 53 → 55), `ecoregistru-app` **v79** (`0062009`); monorepo
> `main` = `origin/main` = `deploy/heroku-split` = `429d80f`. Pe producție: `Migrating schema "public" to version "55 - driver
> depot attestation"`, `now at version v55`, `Started EcoRegistruApplication` (10:42:06 UTC); bundle-ul servit are
> „Din lista de șoferi”. Garda de conținut curată la `^` pe ambele repo-uri split.
> Ce intră: la șoferii noștri depozitul implicit și atestatul (CPC/ADR) cu alertă pe e-mail la 30 de zile
> (`DriverAttestationAlertScheduler`, 07:25); la cântar șoferul recunoscut din listă (potrivire unică pe nume) leagă fișa și
> pune mașina lui într-o rubrică goală, altfel rămâne ocazional; atestatul expirat se vede. **Reparat în aceeași migrare:**
> `weighing_operations.driver_id` n-avea `ON DELETE SET NULL`, deci fișa unui șofer folosit la cântar nu se putea șterge.
> Suita backend **808 de teste, 99 de clase, 0 eșecuri**; `DriverFleetIT` 5/5 cu proba negativă pe 5 reguli; e2e **21** 13/13.


> **16.09.2026, 12:37 și 12:42 — ✅ pe producție: depozitul D1.9–D1.15 și aplicația mobilă M1b.**
> `ecoregistru-api` **v94** (`3ef32ec`, **migrările `V51` și `V52`**, schema 50 → 52), `ecoregistru-app` **v76** (`b80ab14`,
> depozitul) și **v77** (`7733370`, textele M1b); monorepo `main` = `origin/main` = `deploy/heroku-split` = `35ef8a5`.
> Pe producție: `Migrating schema "public" to version "51 - depot retentions"`, apoi `"52 - weighing operation anexa3"`,
> `Successfully applied 2 migrations […] now at version v52` și `Started EcoRegistruApplication` (09:37:55 UTC).
> Garda de conținut curată pe ambele repo-uri split. Intră deodată blocurile de mai jos marcate „local, nedeployat” din
> 16.09: reținerile la sursă și raportul lor (D1.9, D1.10), ecranul „Cântar” cu reparațiile de QA (D1.15), originea pe
> art. 48 (D1.12), registrul intrărilor și ieșirilor (D1.14), Anexa 3 și avizul pe operațiune (D1.13) și borderoul de
> achiziție cu plafonul de numerar (D1.11). Suita backend după D1.11: **796 de teste, 97 de clase, 0 eșecuri**.
>
> **M1b — „Pozează avizul”** (`mobile/`, fără backend): camera sau galeria, recunoașterea textului pe telefon (Vision pe
> iOS, ML Kit pe Android, cu modelul legat în aplicație), parserul avizului (`mobile/src/aviz/parse.ts`, 18 teste pe avize
> inventate, `npm run test:aviz`), confirmarea câmp cu câmp, codul R/D obligatoriu (G3), partenerul după CUI (G4) și coada
> offline pe SQLite (`mobile/src/outbox.ts`: un `clientGeneratedId` dat o dată, poza după ce predarea are id, reîncercare la
> deschidere, la revenirea rețelei și la 30 s). Probat pe iPhone 17 / iOS 26.5 și Android 16: predarea salvată fără
> legătură, telefonul repornit, trimisă singură, **o singură dată** în bază. App v77 atinge numai `strings.mobile`, pe care
> Rollup îl taie din bundle-ul web — nu schimbă nimic în browser. Rămân deschise: urcarea pozei pe producție merge în
> Cloudinary (SUA) până la B2; o poză urcată cu răspunsul pierdut poate ieși de două ori ca atașament; Sentry pe mobil nu e pus.
>
> Următoarea migrare liberă: **`V53`**.

> **16.09.2026, 11:11 — ✅ pe producție: G1, sesiunea pe dispozitiv, și aplicația mobilă legată la serverul real.**
> `ecoregistru-api` **v93** (`c91687c`, **migrare `V50`**, schema 48 → 50), `ecoregistru-app` **v74** (`2e88413`);
> monorepo `9700a30`, `origin/main` + `deploy/heroku-split` sincronizate. Garda de conținut exactă înainte și după
> cherry-pick, pe amândouă repo-urile split (backend `^` = 1 commit; frontend `^^` = 2, fiindcă `strings.ts` fusese
> atins și de M0). Pe producție: `Migrating schema "public" to version "50 - device sessions"`, `Successfully applied
> 1 migration […] now at version v50`, `Started EcoRegistruApplication` (08:11:15 UTC).
>
> ✅ *Rezolvat înainte de deploy (`2904d41`: `V49` → `V51`); rămâne ca regulă.* ⚠️ **`V49` (depozit, D1.9/D1.10) e scrisă pe `feat/depozit-colector` dar NU e deployată, iar `V50` a intrat înaintea
> ei.** Flyway rulează cu `out-of-order` implicit `false`, deci la următorul deploy al depozitului migrarea `V49` va fi
> **refuzată** și dyno-ul nu va porni. Reparația e într-o linie: `V49__depot_retentions.sql` se renumerotează în `V51`
> înainte de deployul lui. Nu e o alegere, e regula lui Flyway: numerele se dau în ordinea în care ajung pe dyno.
>
> **Probat pe serverul de producție, nu doar local** (contul de platformă al proprietarului, dat pentru probă):
> `/auth/refresh` rotește tokenul și îl refuză pe cel refolosit (`device.session.invalid`); tokenul nou deschide API-ul;
> `/auth/devices` și `DELETE /auth/devices/{id}` cer sesiune (401 fără ea) și răspund 200/204 cu ea; `/auth/logout` e 200
> și pe un token inexistent; loginul **cu** `deviceName` nu strică nimic pentru web. Plafonul de 10 dispozitive pe cont
> s-a văzut lucrând. Cele 10 sesiuni de probă au fost **scoase la final**, contul a rămas curat.
>
> **Aplicația mobilă se leagă din prima la Heroku** (`mobile/src/api.ts`: producția e valoarea implicită,
> `EXPO_PUBLIC_API_URL` în `mobile/.env.local` o bate pentru lucrul local). Fluxurile Maestro rulate **pe datele reale
> de producție**, verzi pe iPhone 17 / iOS 26.5 **și** pe Android 16: firmă nealeasă → afișajul cere alegerea, nu arată
> un zero; comutatorul listează firmele contului cu CUI și tip; o firmă **`GENERATOR` pură** — drumul pe care baza demo
> (`BOTH`) nu-l acoperea — arată „PREDAT”, fără comutator de ecrane; lista lunii, totalurile, pubelele și „Dispozitive
> conectate” cu „telefonul ăsta”. Un cod fără pubelă (`15 01 03`, lemn) rămâne **fără pătrățel**, nu cu unul gol.
> Reparat din capturi: „Generare” scria „ÎNREGISTRAT” peste exact cifra pe care Acasă o numea „PREDAT” — eticheta
> afișajului urmează acum **filtrul ecranului** (`lcdLabelIn`/`Out`/neutru).
>
> 📌 **`strings.mobile` nu ajunge în bundle-ul web** — Rollup îl taie ca proprietate nefolosită (probat și local, și pe
> bundle-ul servit, cu un text pur ASCII). Deci app **v74 nu schimbă nimic din ce rulează browserul**; releaseul există
> ca repo-ul split să rămână sincronizat. La o verificare de conținut pe frontend nu se caută texte de mobil.
> 📌 **Maestro potrivește textul întreg al unui nod, nu o bucată** — un rând „Firmă · CUI · tip” nu se prinde cu numele
> firmei, ci cu `.*Nume.*`.

> **16.09.2026, ~11:00 — ✅ pe producție din 11:11 (api v93, app v75): aplicația mobilă, felia M1a — o zi obișnuită.**
> Ramura rebazată peste `origin/main` (`f21db62`); `tsc` și `npm run build` în `frontend` **curate după
> `strings.mobile`** — golul rămas din M0 e închis.
>
> **Backend, G1 — sesiunea pe dispozitiv** (`V50`, tabel nou `device_sessions`; `V49` rămâne rezervat depozitului).
> Tokenul de acces rămâne ce era: opt ore, semnat, necitit din bază. Nou e ce se întâmplă după: un login care **spune
> cum îl cheamă telefonul** (`deviceName`) primește și un token de reîmprospătare, schimbat la `POST /auth/refresh` pe
> unul nou plus încă opt ore. Rândul din bază ține **SHA-256** peste token, nu tokenul; **se rotește la fiecare
> folosire** (cel vechi nu mai deschide nimic); moare la ieșire, la 60 de zile de nefolosire, la schimbarea parolei și
> la dezactivarea contului. `GET /auth/devices` e „Dispozitive conectate”, `DELETE /auth/devices/{id}` scoate un
> telefon — singura scriere gatuită cu `isAuthenticated()`, fiindcă e despre sesiunea celui care cere, nu despre datele
> unei firme (și `CLIENT_VIEWER` are telefon). **Webul nu trimite `deviceName`, deci nu primește nimic nou.**
> `DeviceSessionIT`, **17 teste**, fiecare regulă cu proba negativă rulată (opt reguli scoase pe rând, testul cade de
> fiecare dată). Suita **755 de teste, 92 de clase, 0 eșecuri, 3 sărite** (era 738/91).
> Două capcane prinse de teste, nu de citit: (1) `revoke` pe drumul de refuz se scria și se **anula**, fiindcă refuzul
> e o excepție — `noRollbackFor` pe `rotate` **și** `@Transactional` scos de pe `AuthenticationService.refresh`, altfel
> tranzacția din afară hotăra ea; (2) `GET /deadlines` cere `year` — §3 din `todo-mobil.md` spunea altceva.
>
> **Telefonul.** Comutator de firmă (`X-Tenant-Id`) pentru consultant și platformă, cu golirea cache-ului la schimbare;
> „Dispozitive conectate” cu eticheta „telefonul ăsta”; mișcările lunii cu totalurile serverului deasupra și rândurile
> lipsă în rândul de alertă („1 cântărire la destinatar”, „N fără cod R/D”); termenele anului, restanțele întâi; pubela
> pe codul de deșeu. Bara de jos rămâne cea din prototip — **cinci locuri, cu „Control” la locul lui**: ecranele de
> mișcări ale unei firme „generator și colector” sunt trei, iar băgate în bară ar fi scos afară tocmai ecranul pentru
> care se ia telefonul când vine Garda, deci stau pe un comutator în capul ecranului. Regula care le alege e
> `@/lib/movementScreens` de pe web, importată, nu rescrisă — la fel `binColor.ts`.
> **Proba:** două fluxuri Maestro (`m1a-luna-termene`, `m0-login-acasa`) verzi pe **iPhone 17 / iOS 26.5** și pe
> **Android 16**, capturile privite. G1 probat pe telefon cu `m1a-reimprospatare`: `token_version` mărit din bază omoară
> tokenul de acces fără să atingă sesiunea de dispozitiv, iar aplicația își ia singură unul nou și nu scoate pe nimeni
> din cont — cu **control pozitiv**, sesiunea revocată duce la ecranul de login, deci verdele nu e fals.
> Backendul atins, deci felia are migrare: la deploy se ia numărul liber atunci.
>

> **16.09.2026 — ✅ pe producție din 12:37 (api v94, app v76): depozitul D1.11, borderoul de achiziție de la persoane fizice.**
> În formularul unei intrări **finalizate** de la o persoană fizică apare **„Borderou”** (`GET /weighing-operations/{id}/borderou`),
> după modelul din anexa la OUG 31/2011, rubrică cu rubrică: operatorul (denumire, adresă și punct de lucru, Reg. Com.,
> CUI/CIF, autorizația de mediu), deținătorul, tabelul (0)–(4) cu TOTAL, plata (chitanța, viramentul în 3 zile lucrătoare,
> sau fraza întreagă cu ambele variante când felul plății nu e ales), reținerile, gestionarul primitor. Numărul se dă la
> prima tipărire și se păstrează (regim intern, alin. (1^3); coloana e din V46, **fără migrare nouă**). **Cotele în
> vigoare** (10% impozit, doar pe metal; 2% AFM), cu sumele păstrate la finalizare — nu „16% și 3%” din model (C2). La
> **metal**: act, CNP, domiciliu și declarația de gospodărie proprie; **fără metal**: doar numele (C3, Legea 190/2018
> art. 4). Îl tipărește cine scrie **și** vede prețurile. **Plafonul de numerar** (Legea 70/2015 art. 4): în formular apare
> un avertisment când plățile în numerar către aceeași persoană în aceeași zi (după rețineri, fără anulate și viramente)
> trec de 10.000 lei (`GET /{id}/cash-check`); suma se arată doar cui vede prețurile. Pe hârtie nu se scrie nimic despre asta.
> **Proba:** `BorderouIT` 7/7 pe fraze întregi din model, cu proba negativă pe zece reguli (identitate doar la metal,
> declarație doar la metal, suma plătită după rețineri, prețurile, doar finalizate, doar PF, numărul păstrat, plafonul,
> suma ascunsă, fraza fără fel de plată), fiecare scoasă pică exact testul ei. `RegisterSelectionInventoryTest`: borderoul
> intră în excepția numită a documentelor pe o singură operațiune. Probat în Chrome ca admin și operator, la 1440 și 375:
> butonul, PDF-ul citit, avertismentul cu 10.560 lei. Suita **796/97** (singura cădere, inventarul, reparată și rerulată); e2e 19 verde.

> **16.09.2026 — ✅ pe producție din 12:37 (api v94, app v76): depozitul D1.13, documentele de transport pe operațiune.**
> O ieșire cu douăsprezece sortimente e un camion, deci **un singur formular** (HG 1061/2008 art. 20 alin. (4)). În
> formularul unei ieșiri salvate și neanulate de pe „Cântar” apar **„Anexa 3”** și **„Aviz”**
> (`GET /weighing-operations/{id}/anexa3` și `/aviz`), cu toate liniile: pe Anexa 3 fiecare deșeu cu codul lui și
> cantitatea legată prin cod, pe aviz câte o poziție pe linie. Anexa 3 ia **doar liniile nepericuloase** (una cu toate
> liniile periculoase e refuzată cu trimitere la anexa 2); avizul le ia pe toate. Numărul Anexei 3 se dă la prima
> tipărire și se păstrează pe capul operațiunii (**migrarea `V52`**), din **aceeași serie** cu mișcările: maximul din
> amândouă + 1, sub lacăt consultativ (`Anexa3Numbering`, folosit acum și de mișcare). „Nr. comandă / aviz” de pe
> operațiune e numărul avizului și rubrica „Observaţii”. **Refuzuri:** intrarea (formularul îl face expeditorul; PF n-are
> deloc, AX), operațiunea anulată, și un rând de cântar tipărit prin `/movements/{id}/anexa3|aviz` — ar fi dat un număr
> pe sortiment pentru un singur camion. Ce nu ține operațiunea (transportator ales, data descărcării, bifele „Destinat:”,
> volumul) iese gol, de completat de mână. **Anexa 2 nu e pe operațiune:** formularul are un singur tip de deșeu și un
> număr dat de agenție pe expediție.
> **Proba:** `WeighingDocumentIT` 8/8, cu proba negativă pe opt reguli (filtrul de periculoase, toate liniile, seria
> comună, numărul păstrat, doar ieșiri, anulata, rândul de cântar, numărul avizului), fiecare scoasă pică exact testul
> ei; `Anexa3FormIT` 20/20 neschimbat (formularul unei mișcări obișnuite iese la fel). `RegisterSelectionInventoryTest`
> a căzut corect (generatoarele primesc acum o listă de mișcări) și are o excepție numită, cu motivul, pentru cele
> două documente de transport. Probat în Chrome pe o ieșire cu două sortimente: PDF-urile deschise și citite, subsolul
> la 1440 și 375 (pe telefon cele două documente stau pe un rând). Suita **789/96, 0 eșecuri**; e2e 19 verde.

> **16.09.2026 — ✅ pe producție din 12:37 (api v94, app v76): depozitul D1.14, registrul intrărilor și ieșirilor.**
> Documentul de lucru pe care depozitele îl scot azi din programul de cântar, acum din aplicație: butonul **„Registrul
> lunii”** de pe ecranul „Cântar” descarcă `registru-intrari-iesiri-AAAA-LL.xlsx` (`GET /api/v1/weighing-operations/registru
> ?year&month`, fără lună = anul). O linie pe sortiment, cu capul operațiunii repetat; amândouă direcțiile și **toate
> stările**, cu starea scrisă, cine a anulat și motivul — un registru din care lipsesc anulările nu arată ce s-a anulat.
> Ordinea cântarului: zi, intrările înaintea ieșirilor, numărul. Coloanele urmează exportul unui depozit funcțional, cu
> trei abateri: **fără oră** (decizia proprietarului), „Sortiment” în locul unui ID intern, iar **prețul și valoarea doar
> pentru cine vede prețurile** (D1.8). Persoana fizică apare cu numele, niciodată cu CNP-ul sau actul.
> **Proba:** `DepotRegisterIT` 4/4 pe rânduri întregi (nume inventate), cu proba negativă pe patru reguli (ordinea,
> prețurile ascunse, începutul perioadei, rolul PF), fiecare scoasă pică exact testele ei; e2e **19** verde, cu
> descărcarea nouă; capturile privite la 1440 și 375. Suita **781/95, 0 eșecuri**.

> **16.09.2026 — ✅ pe producție din 12:37 (api v94, app v76): depozitul D1.12, originea în evidența cronologică art. 48.**
> Art. 48 alin. (1) lit. a) cere „natura şi originea” deșeurilor; exportul avea partenerul, dar nu și de la cine vine
> deșeul, iar o preluare de la o persoană fizică ieșea cu rubrica de partener goală și nimic altceva. Coloana nouă
> **„Originea”** (după „CUI partener”, în xlsx și în PDF) tipărește, numai la preluări, cuvântul notei 2 din Anexa 3 la
> Ordinul 794/2012: la o linie de cântar, originea fixată pe operațiune la intrare (persoana fizică e mereu
> „populaţie”), altfel alegerea de pe mișcare, apoi fișa partenerului. Nimeni n-a clasificat → celulă goală, nu ghicită.
> La ieșiri rămâne goală. Nota de sub tabel nu mai spune „aplicația nu o ține”. **Proba:** `Art48RegisterIT` 6/6, cu
> proba negativă pe trei reguli (doar la preluare, operațiunea înaintea fișei, fișa partenerului ca rezervă), fiecare
> scoasă pică exact testul ei. Ramura rebazată întâi peste `main` `1bfa416` (mobil M1a), iar **`V49` renumerotată
> `V51`**; suita după rebazare **776/94, 0 eșecuri**.

> **16.09.2026 — ✅ pe producție din 12:37 (api v94, app v76): depozitul D1.9, D1.10 și D1.15 — reținerile la sursă și ecranul „Cântar”.**
> Migrarea **`V51`** (scrisă ca `V49`, renumerotată pe 16.09 fiindcă `V50` a mobilului a ajuns întâi pe producție) adaugă bazele de calcul lângă sumele reținute, cu constrângere: o sumă reținută fără baza ei ar fi o
> cifră fără document. La finalizarea unei **intrări** se calculează și se păstrează **2% la Fondul pentru mediu** din toată
> valoarea (OUG 196/2005 art. 9 alin. (1) lit. a), și de la persoane fizice, și la hârtie sau plastic) și **10% impozit pe
> venit** numai pe liniile de metal cumpărate de la o persoană fizică (Codul fiscal art. 114 alin. (2) lit. m²), art. 115
> alin. (1) lit. a)); amândouă pe valoarea brută, independent una de alta, rotunjite o singură dată pe total. Cotele sunt
> fixe prin lege, deci stau în cod cu trimiterea la articol — o cotă schimbată mâine nu rescrie declarația de luna trecută.
> `GET /api/v1/weighing-operations/retentions?year&month` dă raportul lunar (AFM și D100, 25 a lunii următoare) sau anual,
> cu beneficiarii pentru D205 (ultima zi a lui februarie); îl vede cine administrează firma **și** vede prețurile.
> Ecranul **„Cântar”** (`/cantar`, doar la firmele cu registrul art. 48) arată operațiunile pe direcții și pe lună, cu banda
> reținerilor deasupra; formularul ține capul, liniile de cântar (tara unei linii = brutul celei dinainte), plata și
> declarația de gospodărie proprie, cerută la finalizare când persoana fizică aduce metal (OUG 31/2011 art. 1 alin. (1^1)).
> Un rând de registru venit de la cântar nu se mai editează din „Intrări”/„Ieșiri”: duce la operațiunea lui (BUG-018).
> Suita **757 de teste, 92 de clase, 0 eșecuri, 3 sărite**; `DepotRetentionIT` 15/15 cu proba negativă pe cinci reguli de
> excludere; e2e: proba **19** nouă, verde, plus 1–5, 15, 18 verzi (9, 10, 11 cad pe date, ca pe `origin/main`).
> **Trecerea de QA de după** (tot 16.09) a găsit și reparat: (1) un **refuz la finalizare urmat de a doua apăsare crea o a doua
> operațiune**, cu încă un număr consumat — formularul ține acum minte operațiunea creată; (2) „Finalizează” pe un formular
> fără linii crea o ciornă numerotată înainte de refuz — acum se oprește în ecran; (3) o linie cu un sortiment scos între timp
> din catalog **se pierdea tăcut la salvare** — sortimentul liniei rămâne de ales; (4) **reținerile apăreau în clar în jurnalul
> de audit**, deși sunt bani din prețuri (baza AFM e chiar valoarea intrării) — se redactează ca `unitPrice`; (5) declarația de
> gospodărie proprie se golește pe o operațiune fără persoană fizică; (6) operatorul nu mai cere raportul reținerilor (un 403
> la fiecare deschidere); (7) liniile listei aduc codul și sortimentul în aceeași interogare (3 în loc de 5, constant la 3 ca
> și la 15 operațiuni — `DepotListPerformanceIT`). Suita **759 de teste, 93 de clase, 0 eșecuri, 3 sărite**; e2e comparată
> probă cu probă cu `main` pe aceeași bază: identică, plus 19.

> **15.09.2026, 23:54 — ✅ pe producție: două reparații găsite la analiza codului.** `ecoregistru-api` **v92**
> (`7e09df0`, fără migrare, schema `V48`); frontendul e neschimbat (app v73); monorepo `c0d4584`. (1) Linia unei operațiuni
> de cântar nu se mai modifică, nu se mai cântărește și nu se mai șterge prin `/api/v1/movements/{id}`
> (`weighing.line.edited.through.operation`). Până acum un operator putea atinge o linie finalizată fără aprobare și fără
> motiv. Tot aici, lista de mișcări arată acum aceleași rânduri pe care le adună totalurile (filtrul de operațiune e în
> `buildFilter`). (2) `AnafClient` nu mai ține firele de cerere în coadă după un apel lent: lacătul așteaptă cel mult 5 s,
> apoi răspunde 503. `WeighingOperationStatusIT` +2, `AnafClientTest` +1, fiecare probat negativ; suita **738 de teste,
> 91 de clase, 0 eșecuri, 3 sărite**. Garda backend e exactă înainte și după cherry-pick. Pe producție: `Schema "public" is up
> to date` și `Started EcoRegistruApplication` pe v92 (20:54:31 UTC), `health` `UP`.
>
> **15.09.2026, ~23:15 — ✅ (în `main` din 16.09, odată cu M1a): aplicația mobilă, felia M0 — scheletul.**
> `mobile/` nou: **Expo SDK 57** (React Native 0.86, React 19.2, TypeScript), Expo Router, TanStack Query, development build
> (nu Expo Go). Tokenii de aspect în `mobile/src/theme.ts`, după prototipul aprobat (antet grafit, afișaj LCD cu segmentele
> stinse, bară de jos translucidă), IBM Plex Sans + Mono legate în aplicație (OFL). Sesiunea stă în Keychain / Keystore
> (`expo-secure-store`); un 401 pe o cerere autentificată scoate omul din cont, ca pe web.
> **Ecrane:** login; Acasă cu kilogramele lunii din `GET /movements/summary` și săgeți între luni; bara de jos
> (Acasă, Generare, „+”, Termene, Control), cu „+” ascuns la `CLIENT_VIEWER`; celelalte patru ecrane sunt locuri goale
> până la M1. Afișajul scrie „ÎNREGISTRAT”, nu „PREDAT”, fiindcă sumarul numără ambele sensuri; un indicator neîncărcat
> arată „?”.
> **Textele și tipurile se importă direct din `frontend/src/lib`** (`@web/strings`, `@web/types`, Metro `watchFolders`);
> textele noi stau în `strings.mobile`. Singurul import din `types.ts` (`import("@/auth/AuthContext").Role`, doar de tip)
> e trimis de `mobile/tsconfig.json` la `mobile/src/auth.ts`.
> **Proba:** `mobile/maestro/m0-login-acasa.yaml` — login `admin@demo.ro` pe backendul local, două luni înapoi, verifică
> „7 mișcări în lună” și „790,5” (aceeași cifră ca endpointul întrebat direct) — trece pe **iPhone 17 / iOS 26.5** și pe
> **Android 16** (emulator), capturile privite. Din ele s-au reparat segmentele stinse (se rupeau pe două rânduri și nu stăteau
> sub cifre) și locul săgeților. `tsc` în `mobile/` curat; **`frontend/` n-a fost reconstruit după `strings.mobile`** — de rulat
> înainte de merge. Backendul nu e atins, deci subtree-urile de deploy nu iau nimic din felia asta.
>
> **15.09.2026, 23:02 — ✅ pe producție: direcția interfeței „Cântar” (C1–C4).**
> `ecoregistru-api` **v91** (`0d7a8f5`, fără migrare, schema `V48`), `ecoregistru-app` **v73** (`24a5e2d`); monorepo `dc6f555`
> (`feat/ui-cantar` `823e366` + status), `origin/main` + `deploy/heroku-split` sincronizate. Garda exactă pe amândouă
> repo-urile split, înainte și după cherry-pick. Pe producție: `Schema "public" is up to date` și `Started
> EcoRegistruApplication` pe v91 (20:02:39 UTC), `health` `UP`; bundle-ul servit are „Deșeuri proprii”, „Intrări de deșeuri”,
> „Caută oriunde”, `movements/totals` și `data-bin`; CSS-ul are `IBM Plex Sans`, `/fonts/plex-sans-400-latin.woff2`
> răspunde `200 font/woff2`; `/api/v1/movements/totals` fără token `401`.
> Panou grafit cu taste și afișajul lunii, pagina albă, IBM Plex Sans + Mono servite din `frontend/public/fonts`
> (Nunito scos), stări ca LED, tabel ca pe bon, pubela pe codul de deșeu (`lib/binColor.ts`, listă explicită).
> **Intrări și Ieșiri** sunt ecrane separate (`/intrari`, `/iesiri`; `/intrari-iesiri` → `/intrari`), cu patru totaluri
> socotite de server peste toate rândurile filtrului: `direction=IN|OUT` pe `GET /api/v1/movements` și
> `GET /api/v1/movements/totals` (Criteria peste același filtru ca lista). Tastele: cifrele 1–9, 0 pe meniu, N / I / E
> pentru adăugare, `/`, `[` (strânge panoul), Ctrl K; pe telefon bandă grafit sus și bară de taburi jos. Import din Excel
> a ieșit din meniu (buton în Setări + paletă); Abonament e un rând jos în panou. Regula: `docs/stil-interfata.md` și
> `CLAUDE.md` rescrise. Suita **735 de teste, 91 de clase, 0 eșecuri, 3 sărite** (`MovementPagingIT` +2); e2e
> **15 din 18** pe `eco_e2e_stil` — 9, 10, 11 cad pe date, identic cu `origin/main` pe aceeași bază; proba **18** e a
> panoului. Măsurat: Generare 1114/1114 la 1440px după ce „Editează” a devenit creion și „Adaugă cantitatea” → „Cântar”
> (IBM Plex e mai lat decât Nunito: 1301px înainte). Abateri deliberate de la machetă: „Reciclat” → „Valorificat”;
> Evidența cronologică = un buton cu meniu; Abonament fără bara perioadei (API-ul n-are „plătit până la”).
>
> **15.09.2026, 21:12 — ✅ pe producție: stilul interfeței „Prietenos” (felia 1) și căutarea firmei după CUI la ANAF.**
> `ecoregistru-api` **v90** (`a8e3a34`, fără migrare, schema `V48`), `ecoregistru-app` **v72** (`50acba7`); monorepo `3da95dc`,
> `origin/main` + `deploy/heroku-split` sincronizate. Garda exactă pe amândouă repo-urile split, înainte și după cherry-pick.
> Pe producție: `Schema "public" is up to date` și `Started EcoRegistruApplication` pe v90, `health` `UP`; bundle-ul servit
> are „Completează din ANAF” și „Poate vinde metal”, CSS-ul are `Nunito`, `/fonts/nunito-latin-ext.woff2` răspunde `200
> font/woff2`, iar `/api/v1/company-lookup/…` fără token `401`.
> Ramura `feat/stil-prietenos`, peste `aaa0677` (D1.8). Suita **733 de teste, 91 de clase, 0 eșecuri, 3 sărite**; e2e pe o
> bază nouă: **14 din 17** trec, iar 9, 10 și 11 cad pe date lipsă (firma generator de probă, termenele depășite) și cad
> **identic pe `origin/main`** rulat pe aceeași bază.
>
> **Stilul**, decis de proprietar din trei variante desenate, e scris ca regulă în `docs/stil-interfata.md`, iar
> `CLAUDE.md` îl cere oricărei sesiuni. Nunito servit din `frontend/public/fonts` (OFL), nu de pe Google Fonts, ca IP-ul
> clientului să nu plece la un terț; fondul verde pal, griurile cu tentă verde, textul la 15/13px; butoane pastilă,
> câmpuri `rounded-xl` cu chenar de 2px și halou la focus, carduri și tabele `rounded-2xl`, dialoguri `rounded-3xl`,
> titlurile de secțiune citite, nu majuscule; bara laterală pe fundalul paginii. Primitive noi pentru feliile
> următoare și pentru D1.15: `ChoiceCards`, `PillGroup`, `Switch`, `Stepper`. „Cine vede prețurile” (D1.8) e pe carduri,
> nu într-o listă derulantă care se tăia pe telefon. Reparate din verificarea D1.7b/D1.8: pe tabul „Persoane fizice”
> antetul vorbea despre firme; „ⓘ” de pe persoanele cu operațiuni era un `title`, necitibil pe telefon și din tastatură
> (acum `Tooltip`); badge-ul spune „Poate vinde metal”.
> **Găsit de proba 8, nu de `tsc`:** cu textul de 15px și celulele `px-4`, Mișcări ieșea din 1440px cu 63px, iar
> „Vezi atașamentele” intra sub coloana de acțiuni fixată. Tabelele au rămas la 14px, celulele la `px-3`, bara laterală
> la `w-60`; măsurat după: `scrollWidth = clientWidth = 1134`. Tot din capturi: la 900px înălțime „Abonament” ieșea din
> meniu (intrările înapoi la `py-2`).
>
> **ANAF:** „Completează din ANAF” în formularul de partener aduce denumirea, adresa și numărul de la Registrul
> Comerțului din serviciul public v9 și completează **numai rubricile goale**; contribuabilul inactiv e semnalat.
> `GET /api/v1/company-lookup/{cui}`, doar autentificat; cererile rărite la una pe secundă, cum cere documentația ANAF;
> firma găsită ținută o zi, CUI-ul negăsit nu; ANAF căzut → `503 anaf.unavailable`, fără Sentry. Răspunsul live
> (15.09.2026, cu CUI-ul ONSIA) n-are `cod`/`message`, deși documentația le are — nimic nu depinde de ele. Probe:
> `AnafClientTest` 7, `CompanyLookupIT` 5; proba negativă pe memorie și pe 503 pică exact testele lor.
>
> **15.09.2026, 20:27 — ✅ pe producție: depozitul D1.8, prețurile și cine le vede.**
> `ecoregistru-api` **v89** (`aae5fd1`, fără migrare), `ecoregistru-app` **v71** (`8a2cacc`); monorepo `8b28a3d`. Garda exactă pe
> amândouă repo-urile split. Pe producție: `Schema "public" is up to date` și `Started EcoRegistruApplication` pe v89
> (17:27:30 UTC), dyno `up`; bundle-ul servit are `price-visibility`.
>
> Fără migrare: `companies.price_visibility` e din V46, deci schema rămâne `V48` și prima liberă `V49`.
> Setarea firmei, `COMPANY` / `NO_CONSULTANT` / `ADMIN_ONLY`, o schimbă **doar adminul firmei**
> (`PUT /api/v1/companies/current/price-visibility`, secțiunea „Prețuri” din Setări). Platforma e tratată ca un consultant
> (deciziile proprietarului). Cine nu vede prețul primește operațiunile fără `unitPrice`/`totalValue`, iar formularul
> trimis de el nu schimbă prețul salvat: se păstrează pe sortiment și în ordine, iar valoarea urmează cantitatea nouă.
> Jurnalul de audit redactează prețul. Probe: `PriceVisibilityIT` 6/6 (3 setări × 5 roluri, editarea fără drept,
> HTTP, audit); proba negativă pe 7 reguli, 6 pică exact testele lor, iar `@PreAuthorize` scos nu pică nimic (serviciul
> refuză la fel); `EndpointGuardInventoryIT` a prins pragul nou. Suita **721 de teste, 89 de clase, 0 eșecuri, 3 sărite**.
>
> **15.09.2026, 18:03 — ✅ pe producție: depozitul D1.7b, tabul „Persoane fizice”.**
> `ecoregistru-api` **v88** (`4731671`, fără migrare, schema `V48`), `ecoregistru-app` **v70** (`3b03e73`); monorepo `bd701d7`.
> Garda exactă pe amândouă repo-urile split (frontendul a luat și `1fdf36b`, README-ul e2e rămas pe drum). Pe producție:
> `Schema "public" is up to date` și `Started EcoRegistruApplication` pe v88, dyno `up`; bundle-ul servit are
> „Persoane fizice” și `natural-persons`.
>
> Pe Parteneri, la firmele cu art. 48, tab „Firme / Persoane fizice” (`?tab=persoane-fizice`), fără migrare.
> `/api/v1/natural-persons`: lista o citește toată firma, cu CNP-ul redus la ultimele 4 cifre și `metalReady`; fișa
> întreagă (`GET /{id}`) doar cine scrie; scrie și OPERATOR; CNP valid (cifra de control) și unic pe firmă; ștergerea
> definitivă doar pentru o fișă dezactivată și fără operațiuni, fiindcă borderoul se păstrează 10 ani (Legea 82/1991
> art. 25). Probe: `NaturalPersonRegistryIT` 8/8, proba negativă pe 7 reguli (fiecare pică exact testele ei), suita
> **715 teste, 88 de clase, 0 eșecuri, 3 sărite**; e2e `17-persoane-fizice.mjs` **13/13** pe o bază proaspăt seedată.
>
> **15.09.2026, 17:11 — ✅ pe producție: depozitul D1.6 (sortimente) + D1.7 (persoane fizice), blocantele legale citite.**
> `ecoregistru-api` **v87** (`48ee383`, fără migrare, schema `V48`), `ecoregistru-app` **v69** (`65bd192`); monorepo `9c873eb`
> (3 commituri rebazate peste `b3695fc`, BUG-017), `origin/main` + `deploy/heroku-split` sincronizate. Garda exactă pe
> amândouă repo-urile split (backend: `.gitignore` + newline-ul din `SecurityConfiguration.java`; frontend: `.gitignore`
> + `vite.config.js`/`.d.ts`).
>
> Ce a intrat:
> - **D1.6:** catalogul de sortimente pe `/api/v1/waste-articles`, scris de ADMIN, CONSULTANT și **OPERATOR** (decizia
>   proprietarului, „lasă userii să își customizeze sortimentele”); bifa „metal” propusă din cod (`MetalWasteCodes`,
>   `WasteCodeResponse.metalSuggested`); sortimentul „interzis de la PF” refuzat la intrarea unei persoane fizice
>   (OUG 31/2011 art. 1 alin. (1)); secțiunea „Sortimente” în Setări, doar la firmele cu art. 48.
> - **D1.7:** la metal de la o PF se cer CNP valid, act și domiciliu, la cântărire și iar la finalizare
>   (OUG 31/2011 art. 1 alin. (1^2)); persoanele fără operațiune de 10 ani întregi se anonimizează
>   (`NaturalPersonRetentionScheduler`, Legea 82/1991 art. 25); CNP-ul și actul rămân redactate în jurnal.
> - **Blocantele depozitului** citite pe sursă primară (`surse-oficiale.md` §18): C1 (10% din brut), C2, AX, AY, AZ
>   decise pe text. **C3 corectat la reverificare:** Ordinul 701/2024 art. 18 alin. (3) exceptează municipalele,
>   deci borderoul la hârtie/plastic de la PF trece la specialistă (BA).
>
> Probe: suita **707 teste, 87 de clase, 0 eșecuri, 3 sărite** (din XML, după rebase); `WasteArticleIT` 8/8,
> `NaturalPersonIT` 8/8; proba negativă pe 2 + 4 reguli, fiecare pică exact testele ei; proba de ecran pe aplicația
> pornită local, ca ADMIN și ca OPERATOR, **11/11** de fiecare dată (conturile de probă șterse). Pe producție:
> releaseurile cu hash-urile de mai sus, `Schema "public" is up to date` și `Started EcoRegistruApplication` pe v87
> (14:11:40 UTC, dyno `up`), bundle-ul servit are
> `waste-articles`, „Adaugă sortiment” și `metalSuggested`. Proba e2e 7 așteaptă acum 7 secțiuni în Setări.
>
> **15.09.2026, ~17:00 — ✅ pe producție: BUG-017, un DoS de disponibilitate pe importul Excel.**
> `ecoregistru-api` **v86** (`edc9d533`, fără migrare, schema `V48`), monorepo `b3695fc` pe `main` și
> `deploy/heroku-split`; frontend neatins (app rămâne v68). Găsit la o rundă QA amănunțită pe suprafața
> de **după** închiderea auditului: `ExcelImportService.open()` construia tot registrul XSSF în memorie
> **înainte** de `MAX_ROWS`, deci un `.xlsx` valid de 40.000 de rânduri (**1,3 MB comprimat / 16 MB
> despachetat**, sub plasa de 12 MB) sufoca heap-ul de 300 MB al dyno-ului — declanșabil de un `OPERATOR`,
> 5 în paralel dobora procesul pentru **toți clienții** (măsurat: 25/30 cereri normale picau, 42 OOM).
> Reparat cu o gardă care despachetează în flux și se oprește la prima intrare peste 8 MB
> (`guardInflatedSize`, doar `java.util.zip`). `ExcelImportIT` +1, **probat negativ**; re-probat pe jar
> de producție la `-Xmx300m`: bombă → **400 în ~10 ms, 0 OOM**, 30/30 cereri normale supraviețuiesc.
> Suita **691/85, 0 eșecuri, 3 sărite** (din XML). Verificat pe producție (extensia Chrome, sesiune de
> consultant): ecranul „Import din Excel" curat, `/import/verificare` cu șablonul → **200, 0 rânduri, 0
> erori, nimic scris**; consola fără erori; api + app 200. Detalii: `ecoregistru-docs` `QA-BUGS.md`, BUG-017.
>
> **15.09.2026, 15:00 — ✅ pe producție: fixul contului dezactivat, juridicul v2, textul pentru contul inactiv.**
> `ecoregistru-api` **v85** (`699d832`, fără migrare, schema `V48`), `ecoregistru-app` **v68** (`450909d`); monorepo `82d3c51`.
> api v84 / app v67 (`1558ede` / `8b75c7f`, push-urile proprietarului) au adus `dc063f5`; app v68 juridicul v2 (`15b9b8c`);
> api v85 fixul de reset (`82d3c51`, rebazat peste `dc063f5`, conflictul din `ErrorMessageEnum` păstrează ambele texte).
> Suita **690/85, 0 eșecuri, 3 sărite** (din XML, după rebase); garda exactă pe amândouă repo-urile split.
> Verificat: `Started EcoRegistruApplication` pe v85; bundle-ul servit are „Salvând parola”, „Oprirea abonamentului” și
> autorizarea de debitare; **proba 12 pe producție, 20/20 pe partea publică** (partea cu login cere `E2E_PASSWORD`).
> ✅ **Fixul probat pe producție 15.09 ~15:10:** `cmpunkro0+whoperator@gmail.com` (`c8e94cce…`, `enabled = f`, `deactivated_at` 12:01:03 UTC, confirmat în bază) → `request-reset-password` 200 și **niciun** `Sent 'mail/forgot_password'`; controlul pozitiv, consultantul activ, a primit mailul la 15:08 și a intrat (reset 200, login 200). În bază, niciun rând `enabled` + `deactivated_at`.
> Curățenia datelor de probă se face în altă sesiune. Cele două intrări de mai jos rămân ca istoric; marcajele lor „nedeployat”
> sunt închise de aceasta.
>
> **15.09.2026, ~15:00 — 🔴 un cont dezactivat se putea reactiva singur prin „Parolă uitată” (găsit la citirea codului,
> la evaluarea generală; neprobat pe producție).** `resetPassword` punea `enabled = true` fără să se uite la `deactivatedAt`,
> iar `requestPasswordReset` trimitea linkul și conturilor dezactivate. Un coleg dezactivat de admin sau de cabinet își
> punea parola și intra, iar rândul ajungea în starea `enabled` + `deactivated_at`, pe care `V34` o declara imposibilă.
> Reparat în `AuthenticationService`, ramura `fix/deactivated-reset` peste `feat/branding-cabinet`, fără migrare, doar backend:
> - un cont dezactivat nu primește link (tot 200 tăcut);
> - un link emis înainte de dezactivare (invitația de 7 zile) e refuzat cu `account.deactivated`;
> - `login` refuză și rândurile rămase în starea ruptă.
> Trei teste noi în `CompanyUsersIT` (22/22). Proba negativă (`scratchpad/negproof_reset.py`): fiecare gardă scoasă
> pe rând → cade exact testul ei, 1 din 22. Suita completă **690 de teste, 85 de clase, 0 eșecuri, 3 sărite** (din XML).
> Ecranul afișează deja mesajul serverului. ⬜ Nedeployat; ⬜ de verificat pe producție dacă există rânduri cu
> `enabled = true` și `deactivated_at` setat (după deploy nu mai intră, dar parola aleasă rămâne).
> **15.09.2026, seara — 🔄 termenii și politica, v2, în cod (necommis, nedeployat):** textele din
> `frontend/src/lib/legal.ts` urmează setul juridic v2 din repo-ul privat. Termenii au **17 capitole**
> (cap. 11 plata rescris, cap. 12 nou „Oprirea abonamentului și rambursări”, cap. 14 „Datele după încetare”);
> politica numește furnizorii de facturare și de plăți și toate cheile din stocarea locală. Pagina de
> invitație (`ResetPasswordPage`) are rândul „Salvând parola, confirmi că ai citit…” (`legal.setPasswordNotice`),
> iar textul de sub „Card” din `/abonament` descrie autorizarea de debitare. `tsc` și `vite build` curate;
> proba de ecran 12 numără acum 17 capitole și n-a fost rulată. ⬜ La deploy, `LEGAL_DATE` pe ziua deployului.
>
> **15.09.2026, ~14:30 — „primele 3 la 100%”: două defecte de producție reparate, probele de ecran și de ops făcute.**
> Ramura `feat/branding-cabinet` peste `main` `3feb80f`, **2 commituri, fără migrare, doar backend**; suita completă
> **687 de teste în 85 de clase, 0 eșecuri, 3 sărite** (din XML). Ramura de deploy `deploy-be-1509` peste `newrepo/main`
> `6784d9f`, garda pe divergența stabilă (`.gitignore` + `SecurityConfiguration.java`). ⬜ Push de la proprietar.
> - **Schedulerele fără tranzacție (găsit în logurile api v81).** Metoda `@Scheduled` chema o metodă `@Transactional` din
>   aceeași clasă, deci proxy-ul era ocolit. `DriverDataRetentionScheduler` cădea în fiecare noapte cu
>   `TransactionRequiredException` (ștergerea datelor șoferilor, AO, nu rula), iar la `DeadlineAlertScheduler` și
>   `PartnerAuthorizationAlertScheduler` fanioanele se puneau pe entități detașate — același mementou ar fi plecat zilnic.
>   Testele treceau fiindcă chemau direct metoda tranzacțională. Reparat cu `@Transactional` pe intrarea programată (și la
>   `ConsultantDigestScheduler`, `readOnly`). `ScheduledTransactionBoundaryTest` scanează toate componentele și cade pe un
>   `@Scheduled` fără tranzacție într-o clasă cu tranzacții; `DeadlineAlertSchedulerIT` și `DriverDataRetentionIT` au câte
>   un test prin intrarea reală. Proba negativă: fără reparație cad exact cele 3.
> - **Mailul de invitație.** Invitația (utilizator de firmă, consultant, retrimitere) pleca drept „Resetare parolă”, cu
>   „Dacă nu tu ai făcut cererea, ignoră” și 30 de minute. Acum `EmailService.sendInviteEmail` + `mail/invite`, subiect
>   „Invitație în WasteHouse — {firmă/cabinet}”, cod valabil `INVITE_TTL_DAYS = 7`; resetarea rămâne 30 de minute, pagina
>   și endpointul sunt aceleași. `ConsultancyOverviewIT` citește expirarea din bază și randează șablonul. Proba negativă:
>   exact cele 4 teste de invitație cad. Capcana: `<span th:text>7</span> zile` nu conține „7 zile”.
> - **Probe pe producție (api v82, sesiunea administratorului de platformă, cu extensia Chrome):**
>   - `POST /api/v1/platform/sentry-probe` → 500 generic; issue `JAVA-SPRING-BOOT-3` în Sentry. Veriga din P0.6 e închisă.
>   - invitația unui consultant de probă a ajuns în Inbox pe Gmail, de la `contact@wastehouse.ro` — SMTP-ul merge după
>     rotirea secretelor;
>   - importul pe Demo: `verificare` 200 fără erori, `import` 2 mișcări noi, același fișier din nou 0 noi / 2 existente;
>   - R14: zero în ultimele ~1500 de linii de log; ANMAP tot fără lista art. 34¹ (P3.9).
> - ⬜ **Rămase:** contul de consultant de probă (`Cabinet Proba WH SRL`) are nevoie de parolă, pusă de proprietar, apoi
>   `/cabinet` și antetul P2.14 văzute pe ecran; cabinetul, consultantul și cele 2 mișcări „Probă import 15.09” de pe Demo
>   se șterg după verificări. `todo-lansare.md` din repo-ul de docs nu e adus la zi din sesiunea asta (izolare).

> **15.09.2026, 13:54 — ✅ semnul WasteHouse („Bucla-casă”) peste tot, pe producție:** `ecoregistru-api` **v82**
> (`6784d9f`), `ecoregistru-app` **v66** (`73360dd`), monorepo `0b2bcf4`. **683 de teste, 84 de clase, 0 eșecuri, 3 sărite.**
> Fără migrare (schema `V48`). Alb pe `#047857`, pătrat rotunjit; înlocuiește săgeata diagonală din aplicație și bucla
> rotundă de pe landing. `BrandName`, favicon, `apple-touch-icon` și `frontend/public/brand/wastehouse-mark.png`, cu
> care toate cele 6 mailuri au acum antet cu logo (mailurile nu citesc SVG). **Logoul e închis — nu mai e muncă pe el.**
> Landingul (semn + footer pentru Netopia) îl urcă proprietarul pe cPanel; logoul în FGO și Netopia, tot proprietarul.
>
> **15.09.2026, 13:34 — ✅ pe producție, împreună cu P2.14 (mai jos):** `ecoregistru-api` **v81** (`63dc856`,
> „now at version v48”), `ecoregistru-app` **v65** (`8476ff1`), monorepo `45b157f` pe `main` și
> `deploy/heroku-split`. **Suita combinată: 683 de teste în 84 de clase, 0 eșecuri, 3 sărite.** Schema **`V48`**,
> prima liberă **`V49`**. ⬜ Rămân: `sentry-probe` apăsat o dată pe producție; antetul văzut pe ecran cu un cont de consultant.
>
> **15.09.2026, după-amiaza — generator, conformitate, securitate: ramura `feat/generator-100`.**
> ✅ **Pe producție (api v81, app v65).** Rebazată pe `375a434` (V47), **fără migrare**. Suita completă după rebase: **676 de teste în
> 83 de clase, 0 eșecuri, 3 sărite** (din XML); `tsc` și `vite build` curate; **„✓ 16 probe, toate trec"**.
> - **Sentry, veriga nevăzută din P0.6:** `POST /api/v1/platform/sentry-probe`, numai `PLATFORM_ADMIN`, aruncă o
>   excepție care trece prin `AdviceController.handleUnexpected` (`SentryProbeIT`: 500 generic + `captureException`;
>   adminul firmei ia 403 și nu raportează nimic). ⬜ Se apasă o dată pe producție.
> - **R3:** dosarul de control se scrie direct în răspuns (`AuditFileService.write`); antetele zip pleacă abia cu
>   primul octet, ca un refuz să rămână JSON. Proba negativă: antetele puse devreme → cade testul de 6 ani.
> - **R1:** `WasteRegister.select` e singurul loc unde o listă de mișcări se taie pe registru;
>   `RegisterSelectionInventoryTest` citește sursa și cade pe un cititor care îl ocolește (probat negativ).
> - **P2.15 — importul din Excel:** `GET /api/v1/import/sablon`, `POST /api/v1/import/verificare` (rulează și
>   întoarce înapoi), `POST /api/v1/import` (totul sau nimic). Rândurile trec prin `PartnerService.create` și
>   `WasteMovementService.create`; partenerii după CUI/denumire numai în firmă; același fișier nu dublează
>   mișcările. Ecran `/import`. `ExcelImportIT` 8/8, proba negativă pe trei reguli → 5 cad; proba de ecran 16.
> - **P3.6:** `AuditChecklistTemplate`/`AuditResult` șterse; `Reception`/`Delivery` marcate nefolosite.
> - **UI:** `PageHeader` păstrează 22rem titlului — pe Evidențe se strângea pe trei rânduri (cădea și pe `main`).
>   Probele 6, 7, 9 aduse la zi; 9/10/11 cer pe o bază nouă o firmă „Proba Automata…” și termenele regenerate.

>
> **15.09.2026, după-amiaza — P2.14: antetul cabinetului pe rapoartele neoficiale.**
> ✅ **Pe producție (api v81 cu `V48` migrată, app v65).** Construit în `feat/branding-cabinet` (din `e1470a8`),
> rebazat peste plăți și deployat după `V47`.
> - **`consultancy_branding`** (tabel separat, un rând pe cabinet): logo PNG/JPG ≤ 500 KB în bază (nu pe
>   Cloudinary, care e în SUA) și un rând de contact ≤ 200 de caractere.
> - **Endpointuri, numai `CONSULTANT`:** `GET/PUT /api/v1/consultancy/branding`, `POST/DELETE/GET …/logo`.
>   Tipul imaginii se citește din octeți și se decodează, nu se crede numele fișierului.
> - **Pe hârtie:** `ReportBranding.addPdfHeader` / `addXlsxHeader` — logo, „Pregătit de {cabinet}”, rândul —
>   pe rezumatul evidenței (`/evidences/export`, PDF și xlsx), `autorizatii-parteneri.pdf` și `README.txt` din
>   dosar. Antetul e al **firmei** (`ConsultancyBrandingRepository.findForCompany`), nu al celui care descarcă.
>   **Formularele oficiale nu îl primesc.** Un antet gol nu tipărește nimic.
> - **Frontend:** Clienți → „Antetul cabinetului pe rapoarte” (numai consultant), cu previzualizarea benzii.
> - **Probe:** `ConsultancyBrandingIT` 7/7, citit de pe PDF, xlsx și README; vecinii `AuditFileIT` 19,
>   `EvidenceExportIT` 5, `EndpointGuardInventoryIT` 4, `ConsultancyOverviewIT` 8. `tsc` curat, `vite build` verde.
>   Proba negativă pe 5 reguli (controller, antet gol, legătura cu cabinetul firmei, tipul din octeți, README):
>   fiecare a picat testele ei; controlul pe codul întreg 7/7. **Suita completă: 639 de teste în 75 de clase,
>   0 eșecuri, 1 sărit** (din XML).
>
> **15.09.2026, ~13:50 — 💳 Abonamente F3 (plata cu cardul prin Netopia) și F4 (doar-citire, mementouri, oprire).**
> ✅ **Pe producție:** `ecoregistru-api` **v80** (`2242e2a`, „now at version v47”), `ecoregistru-app` **v64**
> (`ac95170`), monorepo `375a434`. **Suita completă: 665 de teste în 80 de clase, 0 eșecuri, 3 sărite** (probele
> live), din XML; `tsc` și `vite build` curate. Pe producție, fără sesiune: `/billing/access` → 401, IPN nesemnat → 400.
> - **Factura întâi, și la card:** rularea zilnică emite factura în FGO ca la transfer; cardul plătește o factură
>   existentă, pe pagina Netopia. Notificarea (IPN) se păstrează unică pe (ntpID, stare) în aceeași tranzacție care
>   plătește factura; încasarea se trece în FGO (`factura/incasare`) și se reîncearcă dacă FGO e căzut.
> - **Cardul salvat** (când Netopia dă token): debitare în zilele 0, 3, 6, 10 după emitere, mail la fiecare refuz.
> - **`V47`:** metoda de plată și cardul mascat pe abonament, `ends_on`; pe factură `paid_by`, `fgo_collected_at` și
>   mementourile; tabelele `card_payments` și `payment_notifications`.
> - **Doar-citirea** (15 zile după scadență, sau abonament oprit): orice scriere → 403 `subscription.read_only`;
>   GET-urile (toate documentele), plata, autentificarea și platforma rămân libere. **Oprită din
>   `APP_BILLING_READ_ONLY_ENABLED`** până la contract. Mementouri la scadență +1, +8, +15.
> - **Oprirea abonamentului** cu preaviz de o lună, din dialogul Abonament; bannerul de abonament pe orice ecran;
>   butoanele de scriere urmează rolul și doar-citirea.
> - **Probe:** `CardPaymentIT` 8, `BillingReadOnlyIT` 8, `SubscriptionStatusRulesTest` 7, `NetopiaClientTest` 5,
>   `SubscriptionEndsOnTest` 2. Proba negativă pe 6 reguli scoase deodată → exact 6 teste. Live: sandboxul Netopia
>   dă pagina de plată pentru cererea noastră; FGO de test acceptă încasarea (99/99).
> - ⬜ **Cardul nu e pornit pe producție:** lipsește cheia cu care Netopia semnează notificările (RSA 2048) și
>   tokenizarea pe POS. Debitarea cu cardul salvat e neprobată pe Netopia.
>
> **15.09.2026, după-amiaza — P2.13 felia 2: „Firmele mele” și rezumatul zilnic al consultanților.**
> ✅ **Pe producție din 15.09.2026, 12:25:** `ecoregistru-api` **v79** (`6a826d2`, fără migrare, schema `V46`),
> `ecoregistru-app` **v63** (`d48889e`), monorepo `d2f4639`. Bundle-ul servit conține `consultancy/overview`.
> Construită în `feat/cabinet-panou`, pusă peste billing `7561b16`. **Suita completă după rebase:
> 632 de teste în 74 de clase, 0 eșecuri, 1 sărit** (din XML), `tsc` curat, `vite build` verde.
> - **`GET /api/v1/consultancy/overview`** (numai `CONSULTANT`, fără firmă aleasă): pe fiecare firmă activă a
>   cabinetului — termene depășite (din anul trecut încoace) și următorul, `deadlinesGenerated`, liniile fără
>   cod R/D și cele de cântărit (`EvidenceCalculator.blockers`, aceeași prospețime ca Panoul), codurile-oglindă
>   fără document, partenerii activi cu autorizația/viza în 60 de zile. Ordonat: depășite, apoi cu probleme,
>   apoi după termen. Fără tranzacție peste buclă, ca lacătul evidenței să nu fie ținut peste toate firmele.
> - **`ConsultantDigestScheduler`** (07:30, Europe/Bucharest): un mail pe zi pe consultant activ, cu termenele
>   nefinalizate din 7 zile ale firmelor active; nimic când nu e nimic; fără fanioane (mâine are aceleași termene).
>   Șablonul `mail/consultant_digest`, cu link spre `/cabinet`.
> - **Echipa cabinetului:** `POST /consultancy/users/{id}/resend-invite`, `DELETE /consultancy/users/{id}/invitation`.
> - **Frontend:** `/cabinet` „Firmele mele” (meniu numai la consultant); clic pe celulă = comută firma și
>   deschide Termene / Evidențe / Parteneri; „Alege o firmă” trimite consultantul aici.
> - **Probe:** `ConsultancyOverviewIT` 8, `ConsultantDigestIT` 3. Proba negativă pe 8 reguli (inactive, finalizate,
>   invitații, șterse, parteneri inactivi, retrimitere la activ) — fiecare a picat testul ei.
> - ⬜ Pe ecran nevăzută: Demo n-are cont de consultant.
>
> **Unde suntem — 15.09.2026, 01:52.** 💳 **Abonamentele F2 (facturi FGO, transfer) sunt pe producție,
> legate de contul de TEST FGO.**
> ✅ Monorepo `d41f99c`, `api` **v71** (`14c2af3`, „now at version v44”, pornire 11 s), config **v72**
> (cheile FGO de test), `app` **v60** (`270fa5a`). Bundle-ul servit are „Date de facturare” și „Emite
> facturile scadente acum”. Garda de conținut a ieșit exact pe divergența stabilă. **Suita completă:
> 557 de teste în 64 de clase, 0 eșecuri** (din XML), `tsc` curat.
> - **`V44__subscription_invoices.sql`:** datele de facturare pe `subscriptions` (email, județ,
>   localitate, adresă) și `subscription_invoices`, cu UNIQUE (`subscription_id`, `period_start`) și
>   stările DRAFT → ISSUED → PAID.
> - **`FgoClient`** (API FGO v7):
>   - `emitere` și `getstatus`, JSON, hash SHA-1 cu majuscule, o cerere pe secundă;
>   - la un `IdExtern` repetat, FGO răspunde `Success:false` cu factura existentă, tratată drept emisă.
> - **`BillingRunService`** + **`BillingScheduler`** (06:30, Europe/Bucharest) și
>   `POST /api/v1/subscriptions/billing/run`: rezervă rândul → emite cu `IdExtern` = id-ul rândului →
>   citește plățile → PAST_DUE / ACTIVE. Fără chei FGO nu face nimic.
> - **Garda de DELETE** lipsă din F1 e pusă (`subscription.has.invoices`).
> - **Frontend:** dialogul „Abonament” are datele de facturare (județele din nomenclatorul FGO),
>   lista facturilor cu PDF și butonul de emitere.
> - **Probe:**
>   - `FgoClientTest` 6, `BillingPeriodTest` 3, `BillingRunIT` 7;
>   - proba negativă pe fiecare regulă separat: adresa lipsă și DELETE au picat exact testul lor;
>     fără verificarea de perioadă cad 5, fiindcă UNIQUE-ul oprește rularea;
>   - `FgoLiveTest` pe api-testuat, cu codul nostru: emitere, getstatus și `IdExtern` repetat, fără
>     dublură. Încasarea pusă manual în FGO a fost citită corect (389/389).
> - ⚠️ Facturile emise din producție sunt **de test** până la cheia reală. Înainte de ea se șterg din
>   `subscription_invoices`.
> - Pe ecran, logat, **nu** a fost verificat de sesiune; testul pe Demo e al proprietarului.
>
> **15.09.2026, 01:03.** 💳 **Abonamentele F1 și „Evidența cronologică” sunt pe producție.**
> ✅ Monorepo `e0442b1`, `api` **v70** (`d245632`, „now at version v43”, pornire 13 s, nicio eroare în
> loguri), `app` **v58** (`6be31ee`). Bundle-ul servit are „Evidența cronologică” și „Creează abonamentul”.
> `/subscriptions/founders` și `/evidences/registru-cronologic` fără token → 401. Garda de conținut a
> ieșit exact pe divergența stabilă; `deploy/heroku-split` sincronizat. **Suita completă: 542 de teste
> în 61 de clase, 0 eșecuri** (din XML), `tsc` curat. Pe ecran, logat, **nu** a fost verificat.
>
> **Abonamentele, felia F1** (planul e în repo-ul privat):
> - `V43__subscriptions.sql`: numai tabela `subscriptions`. Firma **sau** cabinetul (CHECK), pachetul,
>   starea, prețurile copiate din grilă la creare, fondator, data de start. Facturile și cardul vin în
>   migrările feliilor următoare.
> - `BillingCalculator`, funcție pură: liniile și totalul unei perioade. Perioada începe în ziua de start
>   și ține până în ziua dinaintea ei din luna următoare, la preț întreg, numărată de la
>   `startedAt.plusMonths(n)`. O firmă adăugată în cabinet se plătește din perioada următoare.
> - `/api/v1/subscriptions/{company|consultancy}/{id}` (GET, PUT, DELETE) și `/founders`, `PLATFORM_ONLY`.
> - Gărzi împotriva plății duble: firmă de cabinet fără abonament propriu
>   (`subscription.company.in.consultancy`), iar o firmă cu abonament propriu nu se mută într-un cabinet
>   (`company.has.own.subscription`).
> - Ecran: **Clienți** → butonul „Abonament” pe rândul unei firme directe și al unui cabinet
>   (`SubscriptionDialog`). Încă nu există o listă a tuturor abonamentelor și nu se emite nicio factură.
> - Probe: `BillingCalculatorTest` 10, `SubscriptionIT` 8. Proba negativă a scos trei reguli și au căzut
>   exact cele trei teste ale lor.
> - ⚠️ DELETE pe abonament n-are încă gardă; o primește odată cu facturile.

> 📜 **15.09.2026, noaptea — AD închisă în act și „Evidența cronologică” construită** (✅ pe producție la 01:03, mai sus).
> Art. 48 alin. (1) prescrie conținutul (lit. a–c) și „cronologic lunar, tabelar”, nu un formular. Alin. (3)
> trimite la o procedură prin ordin, pe care n-am găsit-o. Alin. (7) + AG: datele se tastează în SIM
> („Colectare/Tratare”). Citatele sunt în `surse-oficiale.md` §2.1-bis. Pe baza lor:
> `Art48RegisterBuilder` / `Art48RegisterGenerator` / `Art48RegisterService` și
> `GET /evidences/registru-cronologic?year&workPointId&format=xlsx|pdf`. Documentul are patru tabele:
> - cronologic, în kg și t;
> - Cap. 1 pe cod, în t, cu stocul inițial calculat din anii anteriori;
> - Cap. 2 A pe destinatar, cu cod R;
> - Cap. 2 B pe destinatar, cu cod D.
>
> Generatorul primește `art48.register.collectors.only`. Pe ecranul „Intrări și ieșiri” sunt două butoane,
> pe anul din filtru. Fără migrare.
> Probe: `Art48RegisterIT` 5/5, pe rânduri întregi. Suita completă are **541 de teste în 61 de clase,
> 0 eșecuri** (din XML), cu tot cu felia de abonamente din sesiunea paralelă. Probe negative: filtrul de
> registru, stocul inițial și refuzul, scoase pe rând din cod, au doborât fiecare exact testele lor.
> `tsc` e curat. Pe ecran **nu** a fost verificat.

> **Unde eram — 15.09.2026, noaptea.** 👩‍🔬 **Cererile specialistei din 15.09, în cod** (`V42`):
> - **Licența de transport numai peste 3,5 t:** la parteneri, sub „Transportator”, bifa „Transportă cu
>   vehicule peste 3,5 tone” (`partners.heavy_vehicles`). Licența apare și se păstrează numai bifat;
>   nebifat, serverul o golește (`PartnerService.applyLicence`). Partenerii care aveau licență pornesc
>   bifați.
> - **Data încărcării pe Anexa 3** (`waste_movements.load_date`). Goală, se tipărește data mișcării.
>   Descărcarea nu poate fi înaintea ei.
> - **Avizul de însoțire a mărfii** (`AvizGenerator`, `GET /movements/{id}/aviz`), după modelul primit.
>   Are șase secțiuni. Numărul avizului e referința documentului de pe mișcare, iar aplicația nu alocă
>   serii. E o citire, deci nu cere `CAN_WRITE`, și merge pe orice predare, periculoasă inclusiv.
> - **CNP-ul șoferului**, rubrică proprie (`drivers.cnp`, `waste_movements.driver_cnp`), verificată cu
>   cifra de control (`@ValidCnp`), tipărită pe aviz („nume | CNP | CI”). Are aceleași garanții ca actul
>   de identitate: `REDACTED_FIELDS` în jurnalul de audit, ștergerea automată după trei ani și ștergerea
>   odată cu fișa șoferului.
> - **PDF-urile se deschid în tab, nu se descarcă** (`openPdfInTab`): Anexa 3, Anexa 2, avizul, cele
>   două evidențe, rezumatul și anexele de ambalaje în PDF. `.xls` și dosarul rămân descărcări.
> - **„Declarația anuală” se numește „Evidența gestiunii deșeurilor centralizată”**
>   (`evidenta-centralizata-AAAA.pdf`). Titlul tipărit și adresa API rămân.
> - **Dosarul primește Anexa 1 Ambalaje** (`.xls` + PDF) la firma care pune ambalaje pe piață.
> - **Anexa 3 transport: o singură pagină**, nu trei.
>
> Probe: **518 teste, 58 de clase, 0 eșecuri** (din XML). Probe negative pentru cele trei reguli de
> excludere noi: licența fără bifă, Anexa 1 Ambalaje la comerciant și CNP-ul în jurnal. Scoasă din cod,
> fiecare regulă a doborât exact testul ei. `V42` s-a aplicat pe o bază locală acumulată (V39 → V42). O
> probă Playwright pe aplicația pornită a trecut **16 din 16**, fără erori în consolă: avizul, Anexa 3 și
> evidența centralizată se deschid ca `blob:` într-un tab, bifa ascunde licența, CNP-ul apare în cele
> trei formulare, iar dosarul numește Anexa 1 Ambalaje. Suita de ecran 1–15 **nu** a fost rulată.
> ✅ **Deployat 15.09.2026, 00:01:** monorepo `fd845ad`, `api` **v69** (`7f15c4c`, „now at version v42”,
> pornire 9,2 s, nicio eroare în loguri), `app` **v57** (`302c769`). Bundle-ul servit are „Aviz de
> însoțire”, „Evidența gestiunii deșeurilor centralizată” și bifa de 3,5 t; „Declarația anuală” apare
> de zero ori. `/movements/{id}/aviz` fără token → 401.
> 🌐 **Pagina publică `wastehouse.ro` e live și indexabilă din 15.09.2026, ~00:40**, cu prețurile
> publicate (grila și raționamentul stau în repo-ul privat, `monetizare.md`). Rescrisă în aceeași
> noapte: alb + un singur verde, Inter, vinde pe probleme, cu bucăți de interfață desenate în HTML după
> ecranele reale și textele din `strings.ts` — nu capturi, ca să nu poarte date ale vreunui client.
> Sursa e `../wastehouse-landing/index.html`, în afara oricărui repo.
