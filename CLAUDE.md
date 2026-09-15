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
  `lib/movementScreens.ts`, `App.tsx`): proprietarul nu vrea să explice ce există deja. Tasta N e acțiunea principală
  a ecranului curent; cifrele, I, E, `/`, `[` și Ctrl K sunt legate în `Layout.tsx`.
- Textele de ecran stau în `frontend/src/lib/strings.ts`, în română, pe înțelesul unui client care nu e specialist de
  mediu. Temeiul legal merge în explicație, nu în etichetă. Documentele tipărite își păstrează numele din acte.
- Ordinea rubricilor din formularul de mișcare nu se schimbă. Nicio cifră nu se precompletează pe un formular oficial.
  Un indicator care n-a putut încărca arată „?”, nu „0”.
- O schimbare de ecran se probează în browser: `npm run e2e` (`frontend/e2e/README.md`), cu capturile privite la
  1440px și 375px; `node e2e/shots-cantar.mjs` face capturile tuturor ecranelor. Un tabel se măsoară (`scrollWidth`
  față de `clientWidth`) după orice schimbare de font sau de coloane.
- Backend: cifrele de teste se citesc din `backend/build/test-results/test/*.xml`, după `./gradlew cleanTest test`.
- Cod, comentarii tehnice și commituri în engleză sau română, ca fișierul atins; interfața în română.
- Notele interne (starea la zi, deploy, todo) sunt în repo-ul privat `ecoregistru-docs`, la `docs/prompt-continuare.md`.
