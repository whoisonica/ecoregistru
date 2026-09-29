# Stadiu — EcoRegistru

Jurnalul feliilor livrate, în ordinea în care au fost construite. Fiecare intrare marcată ✅
rulează local și are testele verzi.

> 📚 **Jurnalul de dinainte de 15.09.2026** (feliile din 22.08–14.09, cu G-1…G-10, P0, 11-bis, P1.10–P1.12, P3.1 și
> auditul QA) e în [`istoric/status-pana-la-14.09.2026.md`](istoric/status-pana-la-14.09.2026.md). Intrările din **15–16.09.2026** (depozitul F1–F2,
> mobilul M0–M1c, abonamentele F1–F4, „Cântar”, V58–V62, BUG-017…023) sunt în
> [`istoric/status-15-16.09.2026.md`](istoric/status-15-16.09.2026.md), iar cele din **17–22.09.2026** (generatorul: QA-ul de
> lansare, BUG-024…066, securitatea R1–R3, juridicul v2.1, abonamentele F-A…F-E, „Generare”, „Acasă”, panoul) în
> [`istoric/status-17-22.09.2026.md`](istoric/status-17-22.09.2026.md) (mutate pe 28.09.2026). Aici rămân doar
> intrările de la 26.09 încolo; o intrare nouă se scrie tot în capul acestui fișier.
>
> ⚠️ **E un jurnal: fiecare intrare spune ce era adevărat în ziua ei.** Un „⬜”, un „nedeployat” sau o cifră de teste dintr-o intrare veche
> nu descriu starea de azi — starea de azi e intrarea cea mai de sus. Pe 19.09.2026 antetele intrărilor au fost comparate, una câte una,
> cu `heroku releases`; unde scriau altceva decât Heroku (patru intrări din 17–18.09), au fost corectate pe loc, cu mențiunea a ce scria înainte.

> **29.09.2026, 13:06 — ✅ PE PRODUCȚIE: `ecoregistru-api` **v141** (`260d009`), `ecoregistru-app` **v143** (`6ff6c00`), din `main` `df8dd15` — „cabinet” devine „consultant” peste tot.**
> Vocabularul proprietarului: „consultant” la etichete (grupul din meniu, tabul „Consultanți”, „Plătește consultantul”, „Abonament de consultant”, „Pornirea contului de consultant”),
> „firmă de consultanță” la entitate. Panoul pe `/consultant`; `/cabinet` rămâne `<Navigate>` pentru mailurile de rezumat deja trimise, iar mailul nou trimite la `/consultant`.
> Erori, mail, `legal.ts` (aliniat cu `juridic/politica-confidentialitate.md`), comentarii, teste, seed-ul demo și e2e 30/37 urmează același vocabular; migrațiile Flyway neatinse
> („Successfully validated 77 migrations”), fără migrare nouă, liberă tot `V81`. Suita 1334/0, web 88/88, CI verde pe `df8dd15`; garda: câte un commit pe fiecare parte.
> `Started EcoRegistruApplication in 14.062 seconds`, `/actuator/health` 200; bundle-ul servit (`index-CxKt9jyy.js`) are „Firme de consultanță”, „Plătește consultantul”,
> „Abonament de consultant”, „Echipa firmei de consultanță” și `"/consultant"`; singurul „cabinet” rămas e ruta de redirecționare. Landingul (tab „Pentru consultanți”,
> `video/consultant.mp4` + poster) urcat prin cPanel, verificat cu `curl` + `cmp`; `video/cabinet.mp4` rămâne pe server (linkul din mailul trimis prospectului).
> Firma demo de pe producție păstrează numele vechi (decizia proprietarului).

> **29.09.2026, 02:20 — ✅ PE PRODUCȚIE: `ecoregistru-api` **v140** (`7cc7dbb`), `ecoregistru-app` **v142** (`2885e93`), din `main` `bf9413d` — concluziile specialistei despre generator.**
> Fără migrare („Schema "public" is up to date”), liberă tot `V81`; `Started EcoRegistruApplication in 17.098 seconds`, `/actuator/health` 200; bundle-ul servit
> (`index-DDt_sp8a.js`) conține „Registrul Anexa 3 (transport)”, `registru-anexa3` și „Alege întâi valorificare sau eliminare.”; `deploy-split.sh both --ref origin/main`
> → „nimic de deployat” pe ambele. CI verde pe `bf9413d`. Deciziile 87–90 din `decizii-model.md`. **Anexa 3 Ambalaje numai la colector**: `PackagingService.anexa3` refuză contul fără registru art. 48
> (`anexa3.packaging.collectors.only`), `PackagingAnexa3` fără `exitsOnly`, dosarul nu-i mai pune generatorului foaia (`AuditFileContents.anexa3Applies`),
> pe web meniul și rândul din „Ce intră în arhivă” apar doar la firma care colectează. **„Registrul Anexa 3 (transport)”** (nou, numele ales de proprietar): `Anexa3Register` +
> `Anexa3RegisterBuilder` + `Anexa3RegisterGenerator` (PDF A4 orizontal: nr. crt., data, seria și nr., cantitate kg, cod, denumire, predat către, CUI,
> cod R/D) + `Anexa3RegisterService`, `GET /api/v1/evidences/registru-anexa3?year&workPointId`; meniul „Registrul Anexa 3 (transport)” pe „Generare” și „Ieșiri”, și în dosarul de control
> (`rapoarte/registru-anexa3-{an}.pdf`, numai când anul are formulare emise; `YearContents.anexa3Forms`, rândul din „Ce intră în arhivă”).
> Un rând = o predare cu număr alocat, în ordinea numerelor. Niciun alt document tipărit schimbat (cerința proprietarului).
> **Formularul de pe „Generare”** întreabă întâi valorificare/eliminare → cod R/D → destinație → cine preia → transportul (decizia 89, numai afișarea;
> „Ieșiri” păstrează ordinea veche). **Persoana desemnată** se completează la „Client nou” (numele obligatoriu), nu mai e în „Primii pași” (decizia 90).
> Probe: backend **1334 / 163 de clase, 0 căzute, 4 sărite** (`cleanTest test` pe `bf9413d`, 29.09 02:35); web `tsc` curat, lint 0 erori (29 avertismente), `npm test` 88/88; `vite build`; e2e 23, 34, 38 verzi pe o stivă proprie (sesiunea din noapte), probele 4, 24 și 31 aduse la zi pentru deciziile 89–90 (CI verde pe `bf9413d`). Noi: `Anexa3RegisterIT` (5), `AuditFileIT.theDossierCarriesTheAnexa3RegisterWhenFormsWereIssued`; `RegisterSelectionInventoryTest` exceptează builderul nou cu motiv.

> **28.09.2026, 18:36 — ✅ PE PRODUCȚIE: `ecoregistru-api` **v139** (`b0f0836`), `ecoregistru-app` **v141** (`2c7fba1`), din `main` `c76866d`.**
> Backup `b025` înainte. Pe dyno: „Successfully applied 1 migration to schema "public", now at version v80”, `Started EcoRegistruApplication
> in 18.573 seconds`, `/actuator/health` 200; bundle-ul servit conține „Operator de cântar”, `confirmPastPeriod` și `SCALE_OPERATOR`. Deployul:
> `scripts/deploy-split.sh both --ref c76866d --push` (CI verde, garda curată pe ambele, câte un commit cu merge-uri), rulat de proprietar.
> Pleacă astfel tot ce e în intrarea de mai jos (A1–A7, `V80`, D2, PF la 5 ani, operatorul de cântar). Liberă **`V81`**.

> **28.09.2026, ~17:30 — `main` la zi (cerut de proprietar), fără release Heroku: producția rămâne api **v138** / app **v140** (`V78`).**
> Mergeuite prin `integrare/2809`: `fix/depozit-decizii` (`94173db` D2 — luna încheiată cere a doua confirmare; `8fd9e5c` PF anonimizate la
> 5 ani de la 1 iulie), `feat/operator-cantar` (intrarea de mai jos) și `fix/generator-a` (A1–A7 din `ecoregistru-docs/docs/todo-reparatii-2809.md`:
> punctul de lucru folosit nu se mai șterge cu 500, limita de 1.000.000 t și la „Se cântărește la descărcare”, mail la platformă pentru fiecare
> cerere de cont, avizul și retipărirea Anexei 3 în doar-citire, atașamentele șterse din Cloudinary după commit + 503, 50 de invitații/oră,
> proba de diacritice a evidenței), peste `17af915` (**`V80`**, lista deșeurilor pe ediții, Decizia (UE) 2025/934 de la 9.11.2026) și
> reparațiile B ale mobilului (`d61b4b6`). Plus curățenia md-urilor din 28.09 (intrările 17–22.09 mutate în `istoric/status-17-22.09.2026.md`).
> Singurul conflict: `WeighingOperationStatusIT` (proba D2 și a operatorului de cântar în același loc) — păstrate amândouă, proba HTTP pe
> ziua de azi. Probe pe rezultat: backend **1328 / 162 de clase, 0 căzute, 4 sărite**; web `tsc` curat, lint 0 erori, `npm test` 86/86, `vite build` verde; mobil `tsc` + 96/96.
> ⚠️ `V80` e în `origin/main`: **`V79` nu se mai folosește, F6b ia `V81`**. La următorul `deploy-split.sh` pleacă toate cele de mai sus.

> **28.09.2026, seara — ✅ operatorul de cântar (ramura `feat/operator-cantar`, nemergeuită, nedeployată; fără migrare, liberă tot `V81`).**
> Spec `ecoregistru-docs/docs/specs/2026-09-28-operator-cantar-design.md`, deciziile proprietarului din aceeași zi.
>
> - **Rol nou `SCALE_OPERATOR`** („Operator de cântar”), ales la invitație doar la o firmă cu depozit (`inviteRoles`). `app_users.role`
>   e `VARCHAR(20)` fără CHECK, deci fără migrare; contul demo `cantar@demo.ro` în `DevDataSeeder`.
> - **Cântărește** (creare, antet, linii, balotare, borderou, NIR, verificarea numerarului, Anexa 3): `CAN_WEIGH` = platformă, consultant,
>   admin, operator de cântar. **„Operator”-ul nu mai cântărește** (403), iar pe web dialogul operațiunii i se deschide doar în citire.
>   Finalizarea, anularea și recepția transferului rămân la cine aprobă.
> - **Cântarele** (listă, fișă, evenimente, dovada BRML, documente): le văd și le scriu doar operatorul de cântar și cine aprobă, pe
>   controller și în `ScaleService` (și la citire). Riscul știut: își poate trece singur un „Admis”; rămâne în jurnal. „Operator” și
>   „Vizualizare” nu mai au cardul „Cântare”, iar `/setari/cantare` îi trimite înapoi.
> - **Restul**: persoanele fizice (scrie, CNP întreg) și CNP-ul șoferului întreg; restrângerea pe depozite (`DepotAccess.RESTRICTABLE`);
>   prețurile ca operatorul, iar la „Doar adminul” totalul de plată trece de la operator la el. Partea de generator o citește (`canWrite`
>   fără el). Mailul de cântar merge la admin și la el; termenele, autorizațiile partenerilor și atestatele nu-i mai vin.
>   Mobilul neatins: cu rolul nou îl vede în citire.
> - **Probe:** backend **1316 / 161 de clase, 0 căzute, 4 sărite**; fiecare regulă nouă scoasă din cod a căzut exact pe testele ei
>   (două serii, 9 + 10 căderi, plus garda de citire din `ScaleService` și `WRITERS`). Web: `tsc` curat, lint 0 erori, `npm test` 81/81;
>   e2e **44, 45, 54** verzi pe o bază nouă (`cantar@demo.ro`), trecute pe operatorul de cântar.
>   Suita `run.mjs` **43/43** pe aceeași stivă (29, 30, 33, 36, 37 cer `E2E_DB` = baza stivei; 2 și 38 au căzut o dată, verzi la reluare).
>   Negativa pe web: cu `OPERATOR` în `canWeigh`, proba 44 cade exact pe cele patru verificări ale operatorului de birou.

> **28.09.2026, 13:06 — starea de azi: totul de după 27.09, 14:19 e în `origin/main` (= `origin/deploy/heroku-split` = `7fb198c`),
> NEDEPLOYAT. Producția e tot `ecoregistru-api` **v137** (`4d37338`, V74) și `ecoregistru-app` **v139** (`fcf0c9f`)** — verificat cu
> `heroku releases`. Între `63fb3ac` (ultimul deployat) și `7fb198c` stau 49 de commituri:
>
> - **mobilul:** sesiunea telefonului, **`V75`** (`960cef1`), și evaluarea din 27.09 (`70e4308`); valul A și paleta „Hârtie și smarald”
>   (`935496d`, `40988a7`, merge `e6772fc`); valul B, F4–F10 (intrarea de mai jos). Telefoanele primesc codul la un build nou;
> - **depozitul:** cele șase defecte (`ec72b11`), **D1.17a** borderoul și NIR-ul PF (**`V76`**), defectele mici (`aaf5722`, `0c7cfa4`),
>   **F5** balotarea (**`V77`**), **D4.7** rapoartele fixe (fără migrare), **F6a** termenele SIATD (**`V78`**);
> - **CI:** `07b24fb` (deployul nu pleacă peste roșu) și probele 6/9/10/26 (`c72beb3`) — nu ajung pe Heroku.
>
> Migrările **`V75`–`V78`** pleacă deodată la următorul deploy al backendului: numai coloane, indecși și CHECK-uri noi, nimic șters;
> `V76` numerotează retroactiv intrările PF deja finalizate (și cele anulate după finalizare): borderoul, după cel mai mare număr existent al
> firmei, la cele cu linii plătite, și NIR-ul, de la 1, la cele cu linii la 0 lei. Liberă **`V79`**.
> **Local, neîmpins** (ramura `feat/depozit-d23`): `e11a527` — `scripts/deploy-split.sh` duce și commituri de merge. `main` a primit
> ramurile depozitului și mobilului prin merge (27–28.09), iar cherry-pick-ul se oprea la primul merge; când sunt merge-uri între
> producție și split, scriptul trimite acum **un singur commit** cu exact conținutul split-ului (păstrând fișierele divergenței stabile),
> verificat de aceeași gardă. Fără merge-uri, cherry-pick ca înainte.

> **28.09.2026, noaptea — în `main` (monorepo `a9ca693` … `bb7b112`), fără release Heroku: aplicația mobilă, valul B aproape închis — F4, F6, F7, F5, F10, F9.**
> Webul nu se schimbă vizibil (doar texte `strings.mobile.*`, tăiate din bundle-ul web; un singur text scos, `monthMovements`, nefolosit pe web); backendul neatins.
> **F4** predarea pe trei pași + ciorna (`a9ca693`, ff din `feat/mobil-val-b`). **F6** „Generare” ca registru al lunii (`892194e`): lunile de răsfoit cu
> kg, compoziția pe coduri, cipurile și căutarea pe server, lista pe zile. **F7** (`145dff0`): „Repetă predarea” (predare nouă, originalul neatins) și
> „Adaugă poză” pe predarea deschisă (cheia V67 ținută până urcă). **F5** (`c1983f3`, decizia D9): „Bifează” pe Termene, cu numărul de înregistrare,
> în foaia nativă de jos. **F10** (`5cb18cc`): „ce înseamnă” sub fiecare verificare a controlului + modul inspector pe tot ecranul. **F9** (`bb7b112`):
> „În calendar” = formularul de eveniment al sistemului, fără nicio permisiune nouă (`test:store` o păzește); `expo-calendar` = modul nativ nou →
> telefoanele îl primesc abia la un build nou.
> **Probe:** `tsc` curat (mobil + web), `npm test` mobil **87/87**, lint web 0 erori, `vite build` verde; fiecare regulă nouă de excludere probată
> negativ; Maestro `f6-registru`, `f7-repeta-poza`, `f5-bifeaza`, `f10-inspector` verzi pe iPhone 17 și Pixel_WH, `f9-calendar` verde pe Android.
> CI verde pe `5cb18cc` (backend, frontend, mobile, e2e). Detaliile: `ecoregistru-docs/docs/todo-mobil.md` §29.


> **28.09.2026 — ✅ F6a, termenele SIATD pe recepții (ramura `feat/depozit-d23`, migrarea **`V78`**, liberă **`V79`**).**
> Spec `ecoregistru-docs/docs/specs/2026-09-28-f6-siatd-design.md`, plan `…-f6a-siatd-termen-plan.md`.
>
> - **Setări → SIATD** (doar adminul): cinci module (municipale 3 zile, ambalaje 5, anvelope 5, DEEE 15, baterii portabile 15), fiecare cu
>   „Înrolat din”; la un depozit cu persoane fizice, deasupra modulului municipal, obligația din OUG 196/2005 art. 10 alin. (12) și amenda.
> - **Termenul nu se stochează:** `SiatdDeadlines` îl calculează la citire din data recepției (sau a înrolării, dacă e mai târziu), codurile
>   liniilor (`SiatdFlow`, fără `16 06 01*`) și modulele bifate; recepție pe mai multe module → termenul cel mai scurt. `SiatdCalendar`
>   numără ca în Codul civil (art. 2.553–2.554), cu sărbătorile din Codul muncii art. 139, citite pe 28.09.2026 (`surse-oficiale.md` §6.1),
>   și Paștele ortodox (Meeus). Mementoul = o zi mai devreme.
> - **Cântar → SIATD**: De confirmat · Confirmate (30 de zile) · Ratate, zece pe pagină; confirmarea (aprobatorii) cu codul SIATD pe una,
>   sau „Confirmă selectate” fără cod; anularea confirmării. Banda pe rândul reținerilor (singură la operator și pe Balotare), rândul din
>   dialogul recepției, mailul de 07:05 către administratori și consultanții firmei de consultanță, fiecare firmă în tranzacția ei.
> - **Probe:** `SiatdCalendarTest` 7, `SiatdFlowTest` 6, `SiatdDeadlinesTest` 9, `SiatdSettingsIT` 4, `SiatdServiceIT` 14,
>   `SiatdAlertSchedulerIT` 6; e2e **56** + 19 (zece taburi) + 55 pe stiva 8098/5198, baza `eco_e2e_f6a`. **Negativa pe 10 reguli, toate
>   prinse:** `+1` din termen și sărbătorile mobile (`SiatdCalendarTest`), excluderea `16 06 01*` (`SiatdFlowTest`), max(recepție, înrolare)
>   și filtrul pe modulele bifate (`SiatdDeadlinesTest`), aprobatorul, `DepotAccess` pe listă și „cod doar la una” (`SiatdServiceIT`),
>   `try/catch` pe firmă (`SiatdAlertSchedulerIT`), butoanele ascunse operatorului (e2e 56).
> - **Recenzia finală** (recenzent nou): 0 critice; reparate cu test RED→GREEN — consultanții primeau nimic (țin de o firmă de consultanță, nu de o firmă client), o citire
>   căzută otrăvea tranzacția comună a schedulerului, interogarea încărca și modulele nebifate, kg numărau și liniile fără modul, dialogul nu
>   se deschidea din SIATD când tabul rămas era Balotare. **Rămâne la proprietar:** bifarea unui modul cu „Înrolat din” în trecut face tot
>   istoricul „Ratate” (alin. (10) literal).

> **28.09.2026, 00:14 — ✅ D4.7, cele unsprezece rapoarte fixe ale depozitului (`6eac126`, `ac6d7c7`; fără migrare).** *(Intrare scrisă
> pe 28.09.2026 la 13:06, din git: felia n-avea una.)*
> `GET /api/v1/depot-reports/{slug}?from&to[&workPointId][&articleId][&partnerId][&format]`, cel mult 366 de zile: registrul de
> intrări-ieșiri, jurnalul de cântar, documentele emise (cu golurile din serie), operațiunile anulate, fișa de stoc pe sortiment,
> transferurile în tranzit și diferențele, 2% AFM, D100/D205, plățile în numerar către PF peste plafon (Legea 70/2015 art. 4),
> borderourile PF cu situația pe persoană, fișa partenerului. Un model (`DepotReport`), două randări: xlsx la toate, PDF semnabil la fișa
> de stoc, AFM, impozit și PF. `DepotAccess` taie depozitele; banii cer aprobare și prețuri vizibile, CNP-urile aprobare; banii sunt pe
> toată firma. **Ecran:** „Cântar” are deasupra „Operațiuni · Rapoarte” (`CANTAR_TABS`, și în panou), `/cantar?tab=rapoarte`, perioada
> cu scurtături, rapoartele în patru grupe; cardurile fără drept nu apar. Descrierea Cântarului și banda reținerilor, pe câte un rând.
> **Probe:** `DepotReportIT` 13, negativă pe 24 de reguli, suita **1259/154**; e2e **55** + 19 (nouă taburi). ⚠️ Tabul „Operațiuni”
> tot derulează la 1440×900 (54 px la commit, de decis cu proprietarul).

> **27.09.2026, 23:09 — ✅ F5, balotarea (`83f0c9f`, `3f04bc1`, **`V77`**).** *(Intrare scrisă pe 28.09.2026 din git.)*
> Deciziile proprietarului: operatorul scrie **doar câți baloți**; kilogramele vin din greutatea standard a sortimentului balotat,
> fără cântărire pe balot (pierderea iese la vânzare sau la inventar); fișa se salvează finalizată. Numai balotarea — sortarea, separat.
> **`V77`**: sortimentul balotat știe din ce vrac se face (`source_article_id`) și cât cântărește un balot; operațiunea `PROCESSING`
> ține numărul de baloți și greutatea din ziua ei. Fișa scrie două linii (`PROCESSING_INPUT` / `OUTPUT`, **R12**, tratare mecanică):
> stocul le mută, vechimea și limita de ieșiri le ignoră, limita „tratat” le compară; registrul art. 48 are rândurile și coloanele de
> tratare, cu nota TRAT cap. 8; listele și totalurile de pe poartă nu le văd. Fără R12 în profil: avertisment, nu refuz.
> **Ecran:** tabul „Balotare” pe Cântar (tasta N, anularea cu motiv la cine aprobă); Setări → Sortimente: „Se face din” și greutatea
> unui balot. **Probe:** `BalingIT` 13, `Art48TreatmentTotalsTest` 1, negativă pe 26 de reguli, suita **1246/153**; e2e **54** + 19.

> **27.09.2026, 22:20 — ✅ defectele mici ale depozitului (`aaf5722`, `0c7cfa4`, `4df7c81`; fără migrare).** *(Intrare scrisă pe
> 28.09.2026 din git.)* **Anexa 3 tipărită nu se mai schimbă:** D1.13 rămâne (se scoate și pe o operațiune în lucru, pleacă cu
> camionul), dar după ce are număr capul și liniile tipărite (cod + cantitate) sunt blocate (`WEIGHING_ANEXA3_ISSUED`); prețul, plata și
> chitanța, nu. Pe avizul de transfer „Serie / număr aviz” e doar nr. comandă / aviz. **Zece rânduri** pe tabelele depozitului și pe
> Setările lor (fără derulare la 1440×900), inventarul paginat, garda la închidere pe dialogul de cântar și pe inventar (și la schimbarea
> pasului), destinația dezactivată a unui transfer scrisă cu „(dezactivat)”. Suita **1232/151**; e2e **53**, negativă pe fiecare.

> **27.09.2026, 18:32–19:59 — ✅ D1.17a, borderoul implicit și NIR-ul la persoana fizică (`b1e9f12` … `c4f2d7c`, **`V76`**).** *(Intrare
> scrisă pe 28.09.2026 din git.)* La finalizarea unei intrări de la o PF, liniile plătite primesc **borderoul** (14-4-13, numărul dat
> acum la finalizare, nu la prima tipărire; doar liniile plătite; banda „ANULAT” pe o operațiune anulată după finalizare), iar cele
> preluate gratuit (preț 0) **NIR-ul 14-3-1A** (OMFP 2634/2015 anexa 2, fără sume, CNP doar la metal; `GET /weighing-operations/{id}/nir`).
> O linie fără preț oprește finalizarea (`weighing.pf.price.required`; cine aprobă fără să vadă prețurile primește
> `weighing.pf.price.by.admin`). **`V76`**: `reception_note_number` + numerotarea retroactivă a intrărilor PF finalizate (borderoul în continuarea seriei, NIR-ul de la 1). Ecran: „Borderou
> nr. …” / „NIR nr. …”, „0 = gratuit, se face NIR” sub linii. Suita **1231/151**, negativă 7/7; e2e **52**.

> **27.09.2026, seara — în `main` (merge `e6772fc`), fără release Heroku: aplicația mobilă, valul A și paleta „Hârtie și smarald”.**
> *(Intrare scrisă pe 28.09.2026 din git.)* **Valul A** (`935496d`): bara de jos pe `Tabs` din Expo Router, capul pe hârtie în locul
> blocului grafit, Acasă-afiș, Profil (firma, dispozitivele, ieșirea), bonul de după „Salvează” (`app/gata.tsx`), selectorul nativ de dată
> și cantitatea mare, haptics. **Paleta** (`40988a7`): hârtie caldă, verdele semnului ca unic accent, stările ca puncte; Acasă în patru
> blocuri. Webul nu se schimbă vizibil. `npm test` mobil 64/64, Maestro verde pe iPhone 17; detaliile în `ecoregistru-docs/docs/todo-mobil.md`.

> **27.09.2026, seara — ✅ local (ramura `feat/depozit-d23`, fără migrare, liberă tot **V75**): cele șase defecte de depozit din evaluarea
> de la 15:00, reparate.** *(28.09.2026: „liberă tot V75” era adevărat doar pe ramură; în `main`, `V75` era deja al sesiunii telefonului,
> `960cef1`, de la 14:13.)* Suita **1204/149** verde; probă negativă pe **15 reguli**, fiecare pică exact testul ei.
>
> 1. **Transferul se scrie doar din depozitul de plecare** (`WeighingOperationService.requireOwnOperation`): operatorul din B vede transferul în lucru,
>    dar capul, liniile, plecarea și anularea îi dau 400 `transfer.edited.at.source`; numărul de Anexa 3 îl alocă doar A (`WeighingDocumentService`),
>    B retipărește ce a tipărit A. Pe ecran formularul e doar de citit pentru B.
> 2. **Un transfer recepționat nu se mai anulează** (`transfer.received.not.cancellable`): e în stocul lui B și în registrul formularelor primite.
>    Plecat și nerecepționat se anulează în continuare. Butonul „Anulează” lipsește pe un transfer recepționat.
> 3. **Cântarele, verificările și dovezile le scrie doar cine aprobă** (admin, consultant, platforma) — `ScaleController` + `ScaleService.requireManager`.
>    Un operator care își trecea singur un „Admis” ocolea confirmarea cu motiv la finalizare. Setări → Cântare îi arată operatorului lista fără butoane.
> 4. **Registrul art. 48 al unui depozit anume** trece prin `DepotAccess` (404 pe depozitul altuia); cel pe firmă rămâne al firmei (granița D2.4).
> 5. **`StockPeriodLock`:** pe un depozit cu inventar aprobat sau cu notă de preluare confirmată, nimic datat înaintea datei lor nu se mai creează,
>    mută, finalizează, anulează, recepționează sau șterge — nici pe cântar, nici în „Intrări/Ieșiri” scrise de mână (art. 48, fără generare).
>    400 `stock.period.closed`. Din ziua de referință încolo se lucrează normal (pct. 9).
> 6. **Formularul de cântar trimite prețul cum e în formular** (`WeighingOperationDialog`): înainte, o salvare apăsată cât firma nu se încărcase
>    trimitea `unitPrice: null` și serverul ștergea prețurile adminului. Cine nu vede prețurile nu le poate schimba (serverul le păstrează).
>
> Teste noi: `TransferIT` +2, `InventoryIT` +2, `DepotAccessIT` +1 (și testul cântarelor rescris: operatorul nu scrie nici în depozitul lui).
> e2e 47: curățenia nu mai anulează transferul recepționat, ci verifică refuzul.

> **27.09.2026, 14:13 — în `main`, fără release Heroku (nu era în `63fb3ac`, deployat la 14:19): sesiunea telefonului (`960cef1`,
> **`V75`**) și evaluarea mobilului (`70e4308`).** *(Intrare scrisă pe 28.09.2026 din git.)* **`V75`** `device_sessions.previous_token_hash`:
> un răspuns de `/auth/refresh` pierdut pe semnal slab nu mai scoate omul din cont — tokenul vechi mai deschide numai cât cel nou n-a fost
> folosit. Tokenul de acces al telefonului poartă `sid`, iar `JwtAuthenticationFilter` îl refuză când sesiunea nu mai e vie (scos din
> Dispozitive, ieșit din cont, parolă schimbată); tokenurile webului n-au `sid`. `DeviceSessionIT` 23/23, cinci negative. Pe telefon
> (`70e4308`): corectura nu mai șterge greutatea sau bifa de ambalaj, „2.5” t nu mai pleacă 25 t, timp limită pe cereri, coada cu eroarea
> pe rând; `npm test` mobil 61. **`V75`, nu `V74`:** `V74` era al depozitului, construit în paralel.

> **27.09.2026, 14:19 — ✅ PE PRODUCȚIE (`ecoregistru-api` **v137**, `4d37338`, V74 aplicată la 14:13; `ecoregistru-app` **v139**, `fcf0c9f`; monorepo `main` = `deploy/heroku-split` = `63fb3ac`). D3.5 inventarul depozitului și nota de preluare a soldurilor** (`2ce260e` … `63fb3ac`; suita **1199/149** după recenzia finală; liberă **V75**;
> spec `ecoregistru-docs/docs/specs/2026-09-27-d35-inventar-design.md`, temeiul în `ecoregistru-docs/reports/Inventarul depozitului în lege.md`).
>
> **V74** `stock_openings` + `inventories` (+ comisie, linii), `waste_movements.stock_opening_id / inventory_id` cu CHECK. Trei operațiuni noi,
> `OPENING_BALANCE`, `INVENTORY_SURPLUS`, `INVENTORY_SHORTAGE`: **excluse implicit** din orice cititor de registre (`NOT_STOCK_ONLY` în JPQL,
> filtrul din `MovementQueryService`), **incluse** în stoc, FIFO, soldul art. 48 și Cap. 1 („Sold preluat” în coloana de început, „Plus/Minus
> la inventar” coloane proprii, nota „Evidența în aplicație începe la …”). `/movements` refuză editarea lor (400, ca liniile de cântar).
> **Nota de preluare** (`/api/v1/stock-openings`): stocul de dinaintea aplicației, din fișele de magazie / contabilitate, la o dată de tăiere
> ≤ prima mișcare; confirmată o singură dată pe depozit. **Nu e inventar** (Legea 82 art. 7, Normele 2861 pct. 2–3, 35; OMFP 2634 anexa 1
> pct. 58, 61). **Inventarul** (`/api/v1/inventories`): decizia (președinte, mod ≠ metodă), declarația gestionarului cu cele 7 întrebări,
> scripticul la începutul zilei de început, faptic cu metodă, explicație la fiecare diferență, natura lipsei, PV, închidere → aprobare (scrie
> ajustările datate la început) → definitiv; redeschidere și recalcularea scripticului când o operațiune retroactivă îl schimbă; operațiunile
> nu se blochează (pct. 9). PDF-uri: decizie, declarație, listă 14-3-12, PV, nota de preluare — semnături pe fiecare pagină, „Generat cu
> WasteHouse, versiunea <data build-ului>” (`springBoot { buildInfo() }`). Ecran: tabul „Inventar” pe „Cântar”.
>
> **Probe.** Suita **1194/149 verde** (`cleanTest test`), `InventoryIT` 21, `StockOpeningIT` 9, `InventoryDocumentsIT` 8,
> `InventoryInvariantIT` 4, `InventorySchemaIT` 9, `Art48RegisterIT` 8; negativă pe 27 de reguli, fiecare pică exact testul ei.
> Frontend: tsc, eslint (0 noi), 68/68. e2e **51** verde pe `eco_e2e_inventar` (jar 8091 + Vite 5191); a prins decizia goală la redeschidere.
> PDF-urile privite randate (Quick Look): pe lista landscape ștampila folosea lățimea nerotită — reparat (`8fad2fb`).
> **Recenzia finală** (recenzent separat) a prins dubla numărare notă + inventar în aceeași zi, inventarul datat înaintea unuia aprobat, lipsa
> numărată ca ieșire pe an și exportul DPA fără liniile noi — toate reparate cu testul întâi (`eecac80`); 10 observații mici rămase în ledger.
> Odată cu frontendul a plecat și partea web din M1f (`2538cbc`, doar texte), care era în `main`.
> **Găsit în cercetare, nereparat (decizia proprietarului):** păstrarea documentelor contabile e 5 ani de la 1 iulie (Legea 36/2023), iar
> `NaturalPersonRetentionScheduler` anonimizează la 10 ani.

> **27.09.2026 — în `main` (monorepo `2888e7d`), fără release Heroku: aplicația mobilă nu mai scoate omul din cont la o reîmprospătare căzută și golește cache-ul la ieșire.** Webul nu se schimbă; telefoanele primesc codul la un build nou.
>
> **Reîmprospătarea.** Fără semnal sau cu serverul ocupat (5xx, 429), `/auth/refresh` nu mai golește sesiunea: eroarea urcă la ecran („Nu am putut încărca luna”) și la coadă (reîncercare). Numai refuzul serverului (400 `device.session.invalid`) scoate din cont (`mobile/src/refresh.ts`, `npm run test:refresh`, și în CI). **Cache-ul.** La ieșire, `queryClient.clear()`: `["devices"]` și `["companies"]` nu poartă omul în cheie, iar al doilea om de pe același telefon vedea telefoanele primului. Probat pe iOS cu un proxy care dă 503 numai pe `/auth/refresh`, cu negativă și control pozitiv; `cache-deconectare.yaml` pica pe codul vechi. Tot verde și pe Android 16 (27.09, după-amiaza), cu control pozitiv. iPhone-ul proprietarului reinstalat cu ea (27.09 12:36; profilul expiră 03.10).
>
> **27.09.2026 — configurația de magazin a telefonului (`f272268`, fără release Heroku):** fără cererea de Face ID în engleză (biometria nu e folosită), Release-ul Android fără `SYSTEM_ALERT_WINDOW`/`USE_BIOMETRIC`/`USE_FINGERPRINT`, iar `PrivacyInfo` declară datele trimise (e-mail, cont, poze, mișcări, tokenul telefonului; erorile nelegate). `npm run test:store`, și în CI.
>
> **27.09.2026 — CI: `main` roșu din 20.09 (ultima rulare verde `f09f52f`), reparat.** Proba **26** aștepta „33 kg”, dar bonul scrie din 20.09 „33,000 kg” (G06, trei zecimale dinadins) → regex-ul probei. Probele **6, 9, 10** cădeau după calendar din 26.09: `DevDataSeeder` punea AFM-uri nebifate până ieri, iar `MissedDeadlinePolicy` arată ca depășit orice ratat după 17.09 → istoria seederului se oprește acum la `shownFrom()`. Suita e2e **43/43** pe bază nouă, local. **CI:** un commit numai cu md-uri nu mai pornește rularea (`paths-ignore`), `deploy/heroku-split` nu mai rulează a doua oară același SHA, e2e-ul nu mai așteaptă backendul (~30 → ~16 min). **`scripts/deploy-split.sh --push` refuză dacă CI-ul lui `--ref` nu e verde** (coboară peste commiturile numai cu md-uri; `--fara-ci "<motiv>"` pentru urgențe) — zece deployuri plecaseră peste roșu între 20 și 27.09.

> **26.09.2026, seara — în `main` (monorepo `bc83873`, `2538cbc`), fără release Heroku: M1f — corectura predării de pe telefon; fluxurile M1b verzi din nou.** Webul nu se schimbă (doar texte `strings.mobile`, tăiate din bundle-ul web), deci nimic de deployat; telefoanele primesc codul la un build nou.
>
> **Corectura** (`2538cbc`). Pe predarea deschisă, „Corectează” (numai predările proprii de pe Anexa 1, fără rândurile din cântar, numai cine scrie) duce la formularul de predare completat din `GET /movements/{id}`; salvarea e `PUT`, direct, numai cu semnal — nu intră în coada offline. `PUT` înlocuiește predarea întreagă, iar telefonul n-are toate rubricile webului, deci cererea pornește de la predarea de pe server și pune peste ea doar ce e pe ecran (`mobile/src/movementEdit.ts`): notele, volumul, tratarea, Anexa 2 rămân. Anul deja declarat: aceeași întrebare ca pe web.
>
> **Probe.** `npm run test:edit` (4, și în CI); `maestro/m1f-corectura.yaml` verde pe iPhone 17 / iOS 26.5 și Android 16 (777 → 778 kg; în bază nota și volumul rămase; „Vizualizare” fără buton). **Negativă:** fără `editBody`, notele și volumul se golesc și fluxul pică pe notă. `m1e-transport` verde după schimbarea salvării. `tsc` curat pe `mobile/` și `frontend/`.
>
> **Fluxurile M1b** (`bc83873`). `m1b-aviz` verde pe iOS și Android (avizul de probă inventat `aviz-partener.png` lipsea din repo; fluxul alege cea mai nouă poză; „Corect” centrat, altfel îl acoperea rotița clientului de dezvoltare). `m1b-offline` verde cap-coadă pe Android: rubricile BUG-023 completate, iar legătura se taie și cu `adb reverse --remove` — modul avion singur nu taie tunelul adb.

> **26.09.2026, 18:14 — ✅ PE PRODUCȚIE (`ecoregistru-api` **v136**, `e236141`, la 18:11; `ecoregistru-app` **v138**, `2712dbf`, la 18:14; monorepo `ac18b23` … `a53777c`, `main` = `deploy/heroku-split` = `c838a5a`): modulul de depozit — F2 închisă (cântarul, accesul pe depozit, transferurile, registrul formularelor primite) și F3 până la D3.4 (stocul, pragurile, limitele din autorizație, vechimea); adresa SuportSIM corectată.** Migrări noi **`V68`–`V73`** — pe dyno „Successfully applied 6 migrations to schema "public", now at version v73”, `Started EcoRegistruApplication in 11.874 seconds`, `/actuator/health` 200, `/api/v1/stock` 401 fără token (există); bundle-ul servit conține „Formulare primite”, „Praguri și limite” și `suportsim@anmap.gov.ro`, iar `suportsim@anpm.ro` nu mai apare. Următoarea liberă **`V74`**. Deployul: `origin main`, `deploy/heroku-split` și `scripts/deploy-split.sh backend|frontend --ref c838a5a --push`, toate din sesiune; garda curată (numai commiturile de depozit + SuportSIM). Totul e sub `hasDepot` / „Cântar”: o firmă fără depozit nu vede nimic nou, în afară de adresa SuportSIM.
>
> **D2.3 cântarul** (`ac18b23`, V68, V70 în `4136bc6`). Fișa cântarului (serie, clasă, diviziunea „e”, pus în funcțiune, declarat la BRML, stare) și istoricul (verificare ADMIS/RESPINS cu buletinul atașat, reparație, incident); `ScaleLegality`: legal = în uz, verificare ADMIS în termen (sau primul an de la punerea în funcțiune) și declarat la BRML înainte (OG 20/1992 art. 19, 24). Sigilat / scos din uz / al altui depozit = refuz; nelegal = finalizare doar cu motiv (decizia proprietarului), păstrat pe operațiune, în jurnal și pe registrul intern xlsx — **nu** pe aviz sau Anexa 3. Alertă pe mail la 30 de zile. La „Doar administratorul”, operatorul vede totalul de plată, nu prețul pe kg.
>
> **D2.4 accesul pe depozit** (`4136bc6`, V69). `app_users.all_work_points` (implicit toate, decizia din 16.09) + `user_work_points`; `DepotAccess` — restrâns poate fi doar OPERATOR/CLIENT_VIEWER; ce e al altui depozit e 404. Setări → Utilizatori: coloana „Depozite” doar la firma cu mai multe depozite active. Totalurile pe firmă rămân pe firmă.
>
> **D2.5 transferurile** (`c0e3c68`, V71). „Pleacă” → „În tranzit” → „Recepționează” la depozitul B, fiecare linie recântărită; toleranța din clasa celor două cântare (HG 710/2015), peste ea NIR + decizia comisiei; destinația fără autorizație valabilă = refuz (HG 1061/2008 art. 1 alin. (3)); aviz „Fără factură”, Anexa 3 cu cantitatea expeditorului. `WasteOperation.TRANSFERRED_OUT/IN` — pe firmă transferul iese din liste, totaluri, Acasă și art. 48; pe depozit se vede. **D2.6** (V72): registrul formularelor primite, append-only în bază (trigger), număr fără goluri, corectura = rând nou, PDF cu „Pagina X din Y”.
>
> **D3.1–D3.4** (`1981677`, `8a35438`, V73). Stocul pe depozit × sortiment × cod, la zi și la orice dată, cu tranzitul și angajatul (fără tabel de solduri); avertisment de stoc negativ la finalizarea unei ieșiri, fără blocare; praguri min/max; limitele din autorizație tastate ca rânduri (fel, cod, unitate, perioadă, durata maximă) și vechimea stocului FIFO („Peste 1 an / 3 ani / Peste autorizație”). Tabul „Stoc” și dialogul „Praguri și limite” în „Cântar”.
>
> **Generator** (`a53777c`): textul pentru un an încheiat fără generări trimitea la `suportsim@anpm.ro` — `anpm.ro` n-are MX; acum `suportsim@anmap.gov.ro` (din 01.06.2025). Plus `surse-oficiale.md` și `legislatie.md` din cercetarea în lege (HG 349/2005 abrogată, OG 20/1992 în forma în vigoare).
>
> **Găsit pe drum:** tasta N pe `/cantar` pornea și „Adaugă deșeuri” din panou (`OWNS_N`) — reparat.
>
> **Probe** (pe ramură, înainte de rebazare): suita 1141/144, 0 căderi; negative exacte pe fiecare felie (22, 18 + 10, 31 + 10 + 15, 12, 17 reguli); e2e 44–50 (+ negative) și 19/20/21 verzi pe o stivă proprie. **După rebazarea peste `bc83873`:** `tsc` 0 erori (29 de avertismente ESLint, aceleași), `npm test` 68/68, `vite build` verde; suita backend **1141 de teste, 144 de clase, 0 eșecuri, 4 sărite**.

> **26.09.2026, 17:03 — ✅ PE PRODUCȚIE (`ecoregistru-app` **v136**, `d1a478a`, la 16:38 și **v137**, `6def2ba`; `ecoregistru-api` neschimbat, v135; monorepo `a38e83e`, `8b59da5`): M1e — predarea deschisă pe telefon, și rubricile Anexei 3 pe formularul de predare.**
>
> **Predarea deschisă** (`a38e83e`). Rândul din Mișcări duce la `mobile/app/miscare/[id].tsx` (`GET /movements/{id}`): deșeul, rubricile fișei, partenerul, transportul, notele, atașamentele (prin `/continut`, în foaia de partajare) și **Anexa 3** / **Aviz de însoțire** ca PDF în foaia de partajare, după regula de pe web — mutată din `hooks/useAnexa3.ts` în `frontend/src/lib/movementPrint.ts` (cu numele fișierului), ca s-o citească și telefonul. OPERATOR vede butoanele (are `CAN_WRITE`); „Vizualizare” nu.
>
> **Rubricile Anexei 3 pe formularul de predare** (`8b59da5`; proprietarul: „când adaug mișcare nu pot adăuga detalii pt anexa 3 transport”). Cu destinatar ales, formularul de pe telefon are secțiunea de pe web, în ordinea ei: încărcare, descărcare, unitatea tipărită, transportatorul (noi / bifații „Transportator” / destinatarul), delegatul din listă (`/drivers`, ținut offline) sau scris, actul, CNP, mașina, „Destinat:”, plus punctul de lucru al destinatarului când are mai multe. „La fel ca data trecută” le pune și pe ele. CNP-ul greșit și descărcarea înaintea încărcării se refuză pe telefon, nu din coadă (`frontend/src/lib/cnp.ts`, aceeași regulă ca `ValidCnp`).
>
> **Pe web** nu se vede nimic nou: regula mutată (reexportată din `useAnexa3.ts`), `lib/cnp.ts` și texte `strings.mobile.*`.
>
> **Probe.** `maestro/m1e-predare.yaml` și `maestro/m1e-transport.yaml` verzi pe iPhone 17 / iOS 26.5 și Android 16 (backend local): admin cu documente, „Vizualizare” fără; predarea cu transport ajunge pe server cu toate rubricile, iar Anexa 3 tipărește delegatul, actul, mașina și datele. Negative exacte: butoanele forțate pentru toți → pică aserțiunea „Vizualizare”; fără verificarea CNP → pică pasul CNP. `tsc` curat pe `frontend` și `mobile`, lint 0 erori, `vite build` verde. Atașamentele — doar pe server.
>
> **Pe iPhone-ul proprietarului**, reinstalat **pe Wi-Fi** (`xcrun devicectl device install app`), legat de producție.

> **26.09.2026, 14:51 — ✅ PE PRODUCȚIE (`ecoregistru-api` **v135**, `47ebe29`; `ecoregistru-app` **v135**, `5ab205c`; monorepo `457b235`, apoi `bf0e471` numai cu un flux Maestro): Sentry pe telefon, toast-ul jos în stânga, `V67` pe dyno.** „Successfully applied 1 migration […] now at version v67”; liberă **`V68`** (a depozitului). Deployul: `main` împins din sesiune, `deploy/heroku-split` și `scripts/deploy-split.sh both --push` de proprietar (clasificatorul le refuză ca „Production Deploy”).
>
> **Sentry pe telefon** (`efb6977`). `mobile/src/monitoring.ts`, ca pe web: numai erori, fără tracing și replay, `sendDefaultPii: false`, fără parametrii din adrese, `environment=mobile`; fără `EXPO_PUBLIC_SENTRY_DSN` nu pornește nimic. Coada de predări raportează **o singură dată** (la prima cădere) o eroare care nu e nici de rețea, nici a serverului — cazul FormData din 16.09, care se reîncerca tăcut la nesfârșit. `SENTRY_DISABLE_AUTO_UPLOAD=true` în `eas.json` până există token pentru hărțile de sursă. **Probe** (receptor local în locul Sentry, nimic trimis în proiectul real): iOS 26.5 și Android 16 — evenimentul pleacă cu `environment=mobile`, `infer_ip: never`, fără email sau token; fără DSN, zero cereri. Coada, pe backend local: eroare injectată de trei ori → 1 eveniment, predarea pe server la a patra încercare; **negativă:** fără garda „prima cădere” → 3 evenimente.
>
> **Toast-ul** (`457b235`, restul din BUG-058). Pe ecran lat stă jos în stânga (`sm:left-4`): în dreapta, o eroare — care stă până o închizi — acoperea „Salvează” din dialogul `2xl` la 1440×900. Măsurat pe aplicația reală (dialogul „Adaugă deșeuri”, refuz de server simulat): toast 16–336 × 799–890, Salvează 1049–1143 × 798–838, fără suprapunere; cu clasa veche, suprapus. Pe telefon neschimbat.
>
> **Pe iPhone-ul proprietarului** (după deploy): Release legat de producție, fără push — `WH_FARA_PUSH=1` în `mobile/app.config.js` (`9861495`) scoate dreptul `aps-environment`, pe care echipa Apple personală (gratuită) nu-l are; expiră în 7 zile. **Următorul pas pe mobil: M1e** (predarea deschisă + Anexa 3 / aviz pe telefon); magazinele (M1d) amânate.
>
> **Teste de mobil.** Avizul de probă inventat `mobile/maestro/fixtures/aviz-necunoscut.{html,png}` (CUI `99900010`, 404 la ANAF) și fluxul `m1b-cui-necunoscut.yaml`, verde pe iOS. `tsc` curat pe `mobile/` și `frontend/`, parserul avizului 18/18, `npm test` fără eșecuri, ESLint curat pe fișierul atins.

> **26.09.2026 — ✅ PE PRODUCȚIE din 14:51, cu intrarea de mai sus (la scriere: „LOCAL, neîmpins, nedeployat”) (monorepo `a42f7ad` … `9a38e25`): aplicația mobilă reluată — rupturile de pe web, taburile, „Scoate”, `V67` idempotența pozei, iconițele.** Producția era atunci api **v134** / app **v134**, schema **`V66`**. Migrare nouă **`V67`** (depozitul ia **`V68`**, convenit între sesiuni). Suita backend: **1085 de teste, 135 de clase, 0 eșecuri, 4 sărite**. `tsc` curat pe `mobile/` și `frontend/`, parserul avizului 18/18. Fluxuri Maestro verzi pe iPhone 17 / iOS 26.5 și Android 16: `m0`, `m1a-luna-termene`, `m1b-aviz`, `m1c-control`, `taburi` (nou), `dispozitive` (nou).
>
> **De ce.** Mobilul stătea pe pauză din 17.09; între timp webul a schimbat fișierele pe care telefonul le importă direct din `frontend/src/lib`, iar CI-ul nu acoperea `mobile/`. La reluare aplicația nu compila (texte `strings.mobile.control*` șterse), nu pornea (`deadlines.ts` trăgea `clsx` prin `utils.ts`, pe care Metro nu-l vede) și orice predare de pe telefon ar fi fost refuzată (BUG-023 cere stare, depozitare, transport, destinație, material/fel de ambalaj și partener autorizat).
>
> **Cod.** `lib/dates.ts` (fără importuri; `utils.ts` reexportă); `mobile/app/predare.tsx` cere rubricile în ordinea webului, cu `destinationsFor`/`suggestedPackagingMaterial` importate din `movementRules.ts`; job CI `mobile` (`tsc` + parserul), **fără** `npm ci` în `frontend/` ca `tsc` să cadă unde cade Metro (probat negativ); taburile de pe web (`lib/screenTabs.ts`) pe un singur rând pe telefon — „Totalul anului”, „Ambalaje”, „De făcut · Bifate · Trecute”; „Scoate” în Dispozitive conectate; **`V67`** `attachments.client_upload_id` + index unic parțial — a doua urcare cu aceeași cheie pe aceeași mișcare întoarce atașamentul existent, fără urcare la Cloudinary; webul nu trimite cheia (`AttachmentAccessIT` +3, proba negativă: fără căutare cade testul ei); iconițele aplicației erau cele din șablonul Expo → bucla-casă pe `#047857`, ca pe web.
