# Probe de interfaţă

Şase suite, 99 de verificări, care deschid aplicaţia într-un Chrome adevărat şi apasă pe ea.

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

Se conduce **Chrome-ul deja instalat**, deci `playwright-core` nu descarcă niciun browser — de asta
e `playwright-core`, nu `playwright`. Se alege prin `E2E_CHANNEL`, implicit `chrome`:

```bash
E2E_CHANNEL=msedge npm run e2e     # Edge e tot Chromium, merge la fel
```

**Pe o maşină fără niciun browser din familia Chromium**, `E2E_CHANNEL` gol foloseşte Chromium-ul
lui Playwright, din cache. Cere o descărcare, o singură dată — ceea ce e chiar lucrul pe care
`playwright-core` îl evită, deci e ieşirea de rezervă, nu drumul obişnuit:

```bash
node node_modules/playwright-core/cli.js install chromium
E2E_CHANNEL= npm run e2e
```

Probat pe 07.09.2026 pe un Mac care avea doar Safari: până atunci suita nu putea porni deloc acolo,
fiindcă `channel` era scris în `lib.mjs`. Dacă `npm run e2e` cade cu `Cannot find package
'playwright-core'`, lipseşte doar `npm install` — dependenţa e în manifest, dar un checkout de
ramură nu atinge `node_modules`.

---

## Ce acoperă

| Suita | Ce probează |
|---|---|
| `1-ecrane.mjs` | Cele douăsprezece ecrane se deschid, au titlu şi tabel, fără erori de consolă, excepţii sau răspunsuri HTTP ≥ 400. Trei roluri: admin, administrator de platformă, public. |
| `2-tabele.mjs` | Coloana de acţiuni rămâne vizibilă şi după ce tabelul se derulează la capăt pe orizontală. La 1280px, Mişcări depăşeşte lăţimea cu ~200px. |
| `3-interactiuni.mjs` | Căutarea restrânge şi **toate** rezultatele se potrivesc; `deseuri` găseşte cât `deșeuri`; Escape o goleşte; golul din căutare are alt mesaj decât golul din lipsă de date. Sortarea schimbă `aria-sort` **şi** ordinea rândurilor. Paginarea. Filtrele în adresă, inclusiv după navigare. Ctrl+K, `/`, `N`. |
| `4-formular.mjs` | Cele opt secţiuni, lăţimea dialogului, banda de efect. Trimiterea unui formular gol marchează rubricile şi le leagă de mesaje prin `aria-describedby`. Duplicarea aduce codul, pune data de azi, goleşte documentul. Confirmarea de ştergere numeşte rândul — şi **anulează**, nu şterge. |
| `5-telefon.mjs` | La 375px: nicio pagină nu se derulează lateral — **inclusiv `/cerere-cont`, probat înainte de autentificare**, fiindcă aşa îl vede prospectul —, banda de trei paşi se stivuieşte, sertarul se deschide din buton şi se închide la navigare şi cu Escape, dialogul urcă de la marginea de jos, grilele de formular sunt pe o coloană. |
| `6-cerere-si-rapoarte.mjs` | Formularul public: marcajele de obligatoriu, erorile pe rubrici cu derulare la prima greşită, CUI-ul respins cu forma cerută, lista de coduri R/D pliată, pagina de mulţumire cu emailul şi termenul. Inboxul: dialogul citeşte toate rubricile, inclusiv textul liber, iar cele necompletate se **arată** goale. Drumul cap-coadă de la blocajul roşu de pe Panou, prin registrul filtrat, până la mişcarea deschisă. ⚠️ **Scrie o cerere** în baza de dev la fiecare rulare, cu CUI unic; nu curăţă după ea, fiindcă aprobarea ar crea o firmă şi firmele nu se şterg. |

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

**Pune o gardă care spune că proba a avut ce verifica.** O probă care trece pe zero rânduri nu
probează nimic, dar arată la fel cu una care trece. Vezi mai jos.

---

## Stările „lipsă" — datoria care bloca probele, plătită

*Secţiunea asta descria, până pe 07.09.2026 seara, o gaură. O păstrăm cu istoria ei, fiindcă
motivul rămâne valabil pentru orice regulă nouă scrisă în jurul unei valori care poate lipsi.*

**Cum era.** Zero din cele 34 de mişcări ale seed-ului n-aveau cantitate, zero din cei 5 parteneri
n-aveau dată de autorizaţie, nicio ieşire nu era fără cod R/D. Or tocmai pentru stările astea au
ecranele reguli proprii: „De cântărit" şi autorizaţia fără dată trebuie să stea **la coada**
sortării, în ambele sensuri.

Regula s-a stricat şi s-a reparat în aceeaşi zi — `useTableView` aplica direcţia cu `reverse()`,
deci a doua apăsare pe coloană le aducea tocmai la vârf — şi **nicio probă n-a putut-o prinde**,
fiindcă n-avea pe ce. Verificarea scrisă atunci a fost **scoasă**: garda ei raporta „0 rânduri fără
cantitate", adică trecea pe gol.

**Cum e acum.** `DevDataSeeder` are toate trei stările — o ieşire `UNCLASSIFIED_OUT` (badge roşu), o
predare cu `weighed_at_unloading` şi `quantity` null (badge galben) şi un partener fără dată de
expirare. Aşezate pe plastic la Turda dinadins: seria de hârtie de la Cluj
(40→30→50→50→30→50.5) e vitrina stocului cumulativ şi rămâne neatinsă. `ApplicationBootIT` le
numără **pe nume** — un total care creşte nu spune că stările există.

⚠️ **Baza de dev nu se re-seedează singură.** Seeder-ul rulează doar pe bază goală, deci o bază
făcută înainte de 07.09 n-are rândurile astea, iar probele care se sprijină pe ele trec pe gol fără
să pară. Le adaugi cu `TRUNCATE` pe tabelele demo şi repornire, sau cu un `INSERT` aditiv.

**Ce rămâne cu adevărat neprobabil aici:** ce se **tipăreşte**. Un PDF randat nu se citeşte din DOM
— pentru feliile care ating documente oficiale, regula rămâne cea din `todo-ui-ux.md`: randează
pagina şi uită-te la ea.

---

## Şi ce nu prinde nici DOM-ul

Trei defecte din runda de 07.09 s-au văzut **numai** deschizând captura, cu toate verificările
verzi: rândul TOTAL AN dispărut, steluţa de lângă cod, şi coloana de acţiuni a inboxului de cereri,
unde al treilea buton le rupea pe toate pe câte două rânduri. O probă poate măsura că nimic nu
depăşeşte pe orizontală; nu poate spune că arată prost.

`shots/` (gitignored) există exact pentru asta. **Uită-te la ele** când adaugi ceva vizibil, nu doar
când cade o probă.
