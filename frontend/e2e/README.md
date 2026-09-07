# Probe de interfaţă

Cinci suite care deschid aplicaţia într-un Chrome adevărat şi apasă pe ea.

**De ce există.** Pe 07.09.2026, după şaisprezece felii de UI/UX toate „verzi" — `tsc --noEmit`
curat, `vite build` verde, cod recitit — prima rulare adevărată a scos **şapte defecte**. Patru
cereau apăsarea unui buton, două o măsurătoare de geometrie, unul o privire pe o captură.
Compilatorul spune că programul e consistent cu el însuşi, nu că face ce trebuie.

Cel mai instructiv dintre cele şapte: trei rubrici din şapte n-aveau marcaj de eroare, fiindcă
scriptul care le lega se oprise înainte să scrie. `validate()` le seta, dar nu le afişa nimeni, iar
bannerul spunea „verifică rubricile marcate mai jos" **fără să marcheze nimic**. Nimic din asta nu
se vedea din cod.

---

## Cum se rulează

Trebuie două lucruri pornite: **backendul** (profilul `dev`, cu datele demo seedate) şi **serverul
de dev**. Probele scriu în baza locală, nu în producţie.

```bash
# într-un terminal
cd backend && SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun

# în altul
cd frontend && npm run dev

# în al treilea
cd frontend && npm run e2e
```

O singură suită:

```bash
npm run e2e -- formular      # rulează doar 4-formular.mjs
```

Împotriva altei adrese:

```bash
E2E_BASE=http://localhost:4173 npm run e2e
```

### Browserul

Se conduce **Chrome-ul deja instalat** (`channel: "chrome"`), deci `playwright-core` nu descarcă
niciun browser — de asta e `playwright-core`, nu `playwright`. Dacă pe maşina ta nu e Chrome, pune
`channel: "msedge"` în `lib.mjs`; Edge e tot Chromium şi merge la fel.

---

## Ce acoperă

| Suita | Ce probează |
|---|---|
| `1-ecrane.mjs` | Cele douăsprezece ecrane se deschid, au titlu şi tabel, fără erori de consolă, excepţii sau răspunsuri HTTP ≥ 400. Trei roluri: admin, administrator de platformă, public. |
| `2-tabele.mjs` | Coloana de acţiuni rămâne vizibilă şi după ce tabelul se derulează la capăt pe orizontală. La 1280px, Mişcări depăşeşte lăţimea cu ~200px. |
| `3-interactiuni.mjs` | Căutarea restrânge şi **toate** rezultatele se potrivesc; Escape o goleşte; golul din căutare are alt mesaj decât golul din lipsă de date. Sortarea schimbă `aria-sort` **şi** ordinea rândurilor. Paginarea. Filtrele în adresă, inclusiv după navigare. Ctrl+K, `/`, `N`. |
| `4-formular.mjs` | Cele opt secţiuni, lăţimea dialogului, banda de efect. Trimiterea unui formular gol marchează rubricile şi le leagă de mesaje prin `aria-describedby`. Duplicarea aduce codul, pune data de azi, goleşte documentul. Confirmarea de ştergere numeşte rândul — şi **anulează**, nu şterge. |
| `5-telefon.mjs` | La 375px: nicio pagină nu se derulează lateral, sertarul se deschide din buton şi se închide la navigare şi cu Escape, dialogul urcă de la marginea de jos, grilele de formular sunt pe o coloană. |

Capturile intră în `shots/` (gitignored). Sunt utile când o probă cade: se vede ce vedea ea.

---

## De ştiut când scrii una nouă

**Playwright derulează singur înainte de clic**, după geometria proprie, şi ignoră `scroll-margin`.
Pe un tabel cu antet lipicios asta duce rândul chiar sub antet şi raportează un clic interceptat —
artefact al probei, nu defect al aplicaţiei. Foloseşte `clickAt()` din `lib.mjs`: derulează cu
`block: "center"`, **verifică cu `elementFromPoint` că ţinta chiar e deasupra** (verificarea care
nu trebuie pierdută — ea a prins meniul acoperit de celulele fixate) şi apasă cu mausul acolo.

**Filtrul de zgomot** din `lib.mjs` ţine afară avertismentele React Router despre v7. Când adaugi
ceva acolo, scrie de ce: un filtru fără motiv ascunde exact defectul următor.

**Pragul de căutare e 10 rânduri.** Pe un tabel cu mai puţine, caseta nu se randează — nu e o
scăpare, e regula din `TableToolbar`. Proba de pe Parteneri (5 rânduri) o verifică explicit.
