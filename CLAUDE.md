# Reguli pentru sesiunile de lucru pe WasteHouse

- **Interfața urmează stilul „Prietenos”: [`docs/stil-interfata.md`](docs/stil-interfata.md).** Orice ecran sau
  formular nou folosește primitivele din `frontend/src/components/ui` și tokenii din `frontend/tailwind.config.js` și
  `frontend/src/index.css`. Nu se scriu culori (`gray-*`, `bg-white`), colțuri sau umbre de mână. Sub șapte opțiuni,
  într-un formular nou: `ChoiceCards` sau `PillGroup`, nu `Select`. Explicațiile: `Tooltip`, niciodată `title=`.
- **Fără temă întunecată.**
- Textele de ecran stau în `frontend/src/lib/strings.ts`, în română, pe înțelesul unui client care nu e specialist de
  mediu. Temeiul legal merge în explicație, nu în etichetă. Documentele tipărite își păstrează numele din acte.
- Ordinea rubricilor din formularul de mișcare nu se schimbă. Nicio cifră nu se precompletează pe un formular oficial.
- O schimbare de ecran se probează în browser: `npm run e2e` (`frontend/e2e/README.md`), cu capturile privite.
- Backend: cifrele de teste se citesc din `backend/build/test-results/test/*.xml`, după `./gradlew cleanTest test`.
- Cod, comentarii tehnice și commituri în engleză sau română, ca fișierul atins; interfața în română.
- Notele interne (starea la zi, deploy, todo) sunt în repo-ul privat `ecoregistru-docs`, la `docs/prompt-continuare.md`.
