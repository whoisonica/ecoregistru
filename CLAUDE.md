# Reguli pentru sesiunile de lucru pe WasteHouse

- **Interfața urmează direcția „Cântar”: [`docs/stil-interfata.md`](docs/stil-interfata.md).** Panou grafit cu taste,
  pagină albă, IBM Plex Sans + Mono, colțuri mici, nicio umbră, stări ca LED (pătrățel + cuvânt), pubela pe codul de
  deșeu. Orice ecran sau formular nou folosește primitivele din `frontend/src/components/ui` și tokenii din
  `frontend/tailwind.config.js` și `frontend/src/index.css`. Nu se scriu culori (`gray-*`, `bg-white`), colțuri sau
  umbre de mână. Fără pastile (`rounded-full`) în afara avatarului. Sub șapte opțiuni, într-un formular nou:
  `ChoiceCards` sau `PillGroup`, nu `Select`. Explicațiile: `Tooltip`, niciodată `title=`, și niciodată în jurul
  unui `Button`.
- **Fără temă întunecată.** Panoul e grafit, pagina e albă: o singură lume de culori.
- **Meniul și tastele se citesc din cod înainte de a propune ecrane** (`frontend/src/lib/navItems.ts`,
  `lib/movementScreens.ts`, `lib/screenTabs.ts`, `App.tsx`): proprietarul nu vrea să explice ce există deja. Un tab nou
  se scrie în `screenTabs.ts`: de acolo îl citesc și pagina, și panoul. În panou nu se pun la loc afișajul lunii,
  tastele mari de adăugare, rândul „Caută oriunde" sau „+" pe rânduri (scoase pe 18.09.2026). Tasta N e acțiunea principală
  a ecranului curent; cifrele, S (Setări, când meniul trece de zece intrări), I, E, `/`, `[` și Ctrl K sunt legate în
  `Layout.tsx`.
- Textele de ecran stau în `frontend/src/lib/strings.ts`, în română, pe înțelesul unui client care nu e specialist de
  mediu. Temeiul legal merge în explicație, nu în etichetă. Documentele tipărite își păstrează numele din acte.
- Ordinea rubricilor din formularul de mișcare nu se schimbă fără o cerere a specialistei sau a proprietarului. Excepția de pe
  29.09.2026 (Andreea, decizia 89): pe „Generare” valorificare/eliminare → cod R/D → destinație → cine preia → transportul;
  „Ieșiri” păstrează ordinea veche până se aliniază și ea (de făcut). Nicio cifră nu se precompletează pe un formular oficial.
  Un indicator care n-a putut încărca arată „?”, nu „0”.
- O schimbare de ecran se probează în browser: `npm run e2e` (`frontend/e2e/README.md`), cu capturile privite la
  1440px și 375px; `node e2e/shots-cantar.mjs` face capturile tuturor ecranelor. Un tabel se măsoară (`scrollWidth`
  față de `clientWidth`) după orice schimbare de font sau de coloane; derularea verticală, pe `main#continut`, nu pe
  document. `npm run e2e` și CI-ul rulează doar probele din `e2e/run.mjs` (1–43 și 57); ale depozitului (44–56) se pornesc cu
  mâna, `node e2e/<fișier>`.
- Migrările Flyway: următorul număr = cel mai mare din `backend/src/main/resources/db/migration` + 1 (`ls | sort -V`).
  Două ramuri construite în paralel nu iau același număr (27.09.2026: `V74` depozitul, `V75` telefonul).
- Deployul pe Heroku: `scripts/deploy-split.sh backend|frontend|both [--ref <commit>] [--push]`, după ce `main` și
  `deploy/heroku-split` sunt împinse; cu `--push` refuză dacă CI-ul lui `--ref` nu e verde.
- Backend: cifrele de teste se citesc din `backend/build/test-results/test/*.xml`, după `./gradlew cleanTest test`.
- Cod, comentarii tehnice și commituri în engleză sau română, ca fișierul atins; interfața în română.
- Notele interne (starea la zi, deploy, todo) sunt în repo-ul privat `ecoregistru-docs`, la `docs/prompt-continuare.md`.
