# Probe de interfaţă

Paisprezece suite, care deschid aplicaţia într-un Chrome adevărat şi apasă pe ea: **303 verificări
rulate** (probele 1–12) plus **25 scrise şi nerulate încă** — proba 13 (drumul aprobării, 13
verificări) şi proba 14 (codul-oglindă, 12), amândouă scrise pe 11.09.2026 în sesiuni fără server
pornit. Rândul ăsta se schimbă la prima rulare verde, nu înainte.

⬜ **Şi una lipseşte cu totul: buletinele de analiză (G-7, 11.09.2026).** Felia s-a livrat cu 11
teste de backend şi **fără probă de ecran** — ecranul din **Setări → Buletine de analiză** n-a fost
deschis niciodată de o suită. Ce ar avea de verificat, şi e scris aici ca să nu se piardă: că
încărcarea cere **toate patru** rubricile (cod, dată, laborator, fişier) şi le marchează pe cele
lipsă; că **o dată în viitor** e refuzată cu mesajul ei; că al doilea buletin pe acelaşi cod apare ca
**istoric**, nu îl înlocuieşte pe primul; şi — cea care leagă felia de G-4 — că după încărcarea unui
buletin pe un cod-oglindă **badge-ul „Cod-oglindă" se stinge** pe o mişcare a acelui cod care **n-are
niciun ataşament**. Ultima e cea care contează: e singura care probează mutarea sursei, iar backendul
o ţine deja (`MirrorWasteCodeIT`), deci proba de ecran verifică drumul, nu regula.

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
cd backend && SPRING_PROFILES_ACTIVE=dev DEMO_PASSWORD=<ceva> ./gradlew bootRun

# în altul
cd frontend && npm run dev

# în al treilea — aceeaşi valoare ca DEMO_PASSWORD
cd frontend && E2E_PASSWORD=<ceva> npm run e2e
```

⚠️ **Parola conturilor demo nu mai e scrisă în repo** (09.09.2026): stătea în `lib.mjs` şi în
`README.md`-ul public, iar aceleaşi conturi există şi în baza de **producţie** — deci repo-ul dădea
oricui un login de `PLATFORM_ADMIN`. Vine acum din mediu, în amândouă terminalele. Fără
`E2E_PASSWORD`, suita **refuză să pornească** şi scrie ce să setezi, în loc să cadă pe zece
verificări de autentificare.

Pe o bază **deja seedată** parola nu se schimbă retroactiv: `DevDataSeeder` sare când găseşte date,
deci `E2E_PASSWORD` e cea cu care s-au creat conturile atunci. Pe una nouă, dacă `DEMO_PASSWORD` nu
e setată, seeder-ul generează una şi o scrie în log — atunci copiaz-o de acolo.

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
| `7-firma-si-reactivare.mjs` | „Datele firmei" din Setări: cele cinci grupe, rubricile care se tipăresc pe documente, golurile spuse ca goluri, şi cuprinsul paginii cu ţinta fiecărei intrări. Drumul întreg al reactivării: creează un şofer, îl dezactivează prin confirmare, verifică faptul că **iese din listă** şi că filtrul de stare abia acum apare, îl regăseşte prin „Inactive", îl reactivează. Garda formularului de mişcare: neatins se închide direct, atins întreabă, iar Escape peste întrebare închide **doar** întrebarea, cu ce s-a scris neatins. ⚠️ **Lasă în urmă un şofer dezactivat** la fiecare rulare. |
| `8-atasamente-partener-paleta.mjs` | Coloana „📎" e un buton care deschide lista fişierelor, fiecare cu numele lui şi cu `target="_blank"`, fără buton de ştergere — ştergerea rămâne în formular. Formularul de partener: cele cinci secţiuni titrate, lăţimea `xl`, şi **ordinea rubricilor** (CUI-ul lângă denumire, nu după camioane). Cuprinsul de pe Ambalaje: cele patru ţinte există, bara stă lipită şi **nimic nu trece peste ea** (măsurat cu `elementFromPoint` în trei puncte), clicul duce sub bară la 80px, ultima secţiune duce la fundul paginii cu marcajul pe ea, iar la 375px pagina nu se derulează lateral. Paleta: „fisa" → Evidenţe, „anexa 1" → **amândouă** documentele **şi nimic altceva**, „predare" → acţiunea de adăugare, care duce pe Mişcări cu formularul deschis şi cu `?nou=1` consumat. Plus verificarea că nicio comandă nu recalculează un dosar. |
| `9-restrangeri-si-semne.mjs` | Provenienţa ambalajelor se cere **numai** pe un cont care preia de la terţi — probat pe amândouă tipurile, comutând tenantul ca administrator de platformă, fiindcă o restrângere care ascunde tuturor e la fel de greşită ca una care nu ascunde nimănui. Grila de suprascriere de pe Ambalaje: „nesalvat" cât s-a tastat, „salvat" după ieşirea din celulă, cifra regăsită după reîncărcare, apoi **golită la loc** ca proba să nu lase nimic în urmă. Panoul numără coduri cu stoc, nu kilograme adunate peste coduri, şi numeşte primele trei. Termenele: zilele rămase pe fiecare rând, depăşirea scrisă ca depăşire, linkul către documentul care stinge termenul — **pe anul raportat**, nu pe anul termenului — şi absenţa lui la contribuţiile AFM, pentru care nu tipărim nimic; plus înălţimea butonului de acţiune, fiindcă a şasea coloană l-a rupt pe două rânduri prima oară. Nota de retenţie a actului de identitate, în amândouă locurile unde se tastează. |
| `10-panou-actiunea-urmatoare.mjs` | Banda „Următoarea acţiune" din capul Panoului: **tace** până vin toate cele trei surse (un „Eşti la zi" peste `partners` neîncărcat ar fi un verde fals), alege cel mai scump lucru deschis dintre cele cinci, şi **duce chiar unde numeşte** — pe anul **raportat** la termene, nu pe anul termenului. Nu ţine locul casetei de blocaje: amândouă trebuie să rămână. Plus badge-ul „Autorizaţie expirată" de pe Mişcări, care duce la fişa partenerului prin `?partener=`, consumat la deschidere. |
| `11-numeralul.mjs` | Numeralul românesc, pe **toate** ecranele deodată, şi singura probă care nu caută un text anume: citeşte tot ce scrie pe ecran, culege perechile «număr + substantiv cunoscut» şi verifică forma fiecăreia (`1 linie` · `2 linii` · `20 de linii`). Un ecran adăugat mâine intră singur sub regulă; un substantiv nou se trece în lista `SUBSTANTIVE` şi de-atunci e păzit peste tot. Regula e scrisă a doua oară în probă, dinadins — dacă ar chema `countOf`, amândouă ar greşi la fel şi n-ar mai fi o probă. Garda: fiecare ecran despre care se ştie că numără ceva trebuie să întoarcă cel puţin o pereche. |
| `15-jurnal-audit.mjs` | ⬜ **Scrisă, nerulată.** Jurnalul de audit (P1.11): face o modificare adevărată prin API — creează o mişcare, îi schimbă cantitatea, o şterge — şi cere apoi jurnalului s-o povestească: cele trei fapte, cea mai nouă prima, ştergerea scrisă **ca ştergere**, modificarea cu numele rubricii şi cu amândouă valorile, `updatedAt`/`version` absente, autorul şi eticheta pe fiecare rând. Apoi ecranul din Setări: secţiunea, cuprinsul, faptele în româneşte (**şi niciun `DELETE`/`WasteMovement` scăpat pe ecran**), termenul de păstrare. La final, uşa: un operator primeşte **403**, nu un tabel gol, şi nu vede nici secţiunea. ⚠️ **Lasă în urmă o mişcare ştearsă** la fiecare rulare, pe anul 2033 — ales ca să nu atingă nimic din ce citesc celelalte probe. |
| `12-pagini-legale.mjs` | Termenii şi politica de confidenţialitate se deschid **fără cont**, au datele şi rubricile firmei, iar subsolul duce la ele de pe fiecare pagină publică şi din bara laterală. |
| `13-drumul-aprobarii.mjs` | Cei nouă paşi ai aprobării din HG 1061/2008 apar **numai** peste pragul de 1 t/an, fiecare cu autorul lui, iar pasul 9 numeşte **ISU-ul** şi nu începe cu „Tu" — două verificări scrise în sens contrar dinadins, fiindcă un text alunecat înapoi la varianta greşită ar trece una scrisă doar pe „conţine 48 de ore". |
| `14-codul-oglinda.mjs` | Badge-ul „Cod-oglindă" apare pe mişcarea fără atașament şi **nu** pe un cod obişnuit, stă în celula codului (se pune la îndoială încadrarea, nu predarea), iar motivul citează art. 8 alin. (2), numeşte perechea periculoasă şi spune că mişcarea rămâne înregistrată. ⚠️ **Îşi scrie singură cele două mişcări şi le şterge la sfârşit** — nu se sprijină pe seed. |

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

**Pe 08.09 s-a mai adăugat una:** două atașamente pe chiar ieșirea fără cod R/D — rândul după care
întreabă inspectorul, iar avizul lui e hârtia pe care o cere. Coloana „📎" avea zero rânduri din 36.
⚠️ Nu s-a **urcat** nimic: `CLOUDINARY_URL` nu e setat nici local, nici pe dyno, deci în dev nu
există cale de a crea un ataşament prin aplicaţie. URL-urile arată către cloud-ul public `demo` al
Cloudinary — se deschid, dar nu sunt documentele firmei.

**Pe 08.09, seara, a treia:** o predare către un partener a cărui autorizaţie **expirase înainte de
data predării** — starea din decizia 36, pe care badge-ul „Autorizaţie expirată" o semnalează şi din
care duce acum la fişa partenerului. Zero rânduri din 37 o aveau, deci proba drumului s-ar fi făcut
pe gol.

✅ **Mutată în `DevDataSeeder` pe 09.09.2026**, de unde trăia ca `INSERT` aditiv chiar în fişierul
ăsta. Acum e a patra stare numărată **pe nume** de `ApplicationBootIT`, ca celelalte trei, iar
seed-ul demo are 37 de mişcări, nu 36. Cine face o bază proaspătă o primeşte din prima; nu mai e
nimic de lipit cu mâna.

⚠️ **Cantitatea e 1 kg dinadins.** Prima variantă punea 120 şi a dus stocul unui cod fix la zero,
deci dala „Coduri cu stoc" a trecut de la 4 la 3 şi **proba 9 a căzut** — cea care fixează chiar
cifra aia. Un rând de seed adăugat pentru o probă nu trebuie să mişte datele pe care se sprijină
alta; când o face, se vede — dar numai dacă suita se rulează **întreagă**.

⚠️ **Şi mutarea a scos la iveală o bombă cu ceas.** Rândul are dată fixă (20.08.2026), dar
partenerul avea expirarea scrisă **relativ**, `now().minusDays(30)`. Amândouă erau adevărate în ziua
în care s-a scris `INSERT`-ul, şi pe 19.09.2026 a doua ar fi încetat să fie — expirarea ar fi
depăşit data predării, badge-ul s-ar fi stins şi proba ar fi trecut degeaba, fără ca nimeni să
atingă nimic. Expirarea partenerului e acum tot o dată fixă (15.07.2026). **O dată relativă şi una
fixă care trebuie să rămână în aceeaşi ordine sunt un defect care se aprinde singur, într-o zi
anume.**

⚠️ **Baza de dev nu se re-seedează singură.** Seeder-ul rulează doar pe bază goală, deci o bază
făcută înainte de 07.09 n-are rândurile astea, iar probele care se sprijină pe ele trec pe gol fără
să pară. Le adaugi cu `TRUNCATE` pe tabelele demo şi repornire, sau cu un `INSERT` aditiv. Pentru
ataşamente, aditiv arată aşa:

```sql
insert into attachments (id, movement_id, url, public_id, file_name, content_type, created_at)
select gen_random_uuid(), m.id,
       'https://res.cloudinary.com/demo/image/upload/sample.jpg', 'demo/sample',
       'aviz-369.jpg', 'image/jpeg', now()
from waste_movements m where m.operation = 'UNCLASSIFIED_OUT';
```

**⚠️ Lista de mişcări vine paginată de pe 12.09.2026 (P3.1).** Cine cere `/api/v1/movements` dintr-o
probă ia rândurile din **`content`**, nu din rădăcină, şi pune `size=` când caută prin tot seed-ul —
implicitul e 25. Scris greşit, nu cade nimic: `list.length` e `undefined`, condiţia e falsă, şi bucata
se sare în tăcere. Exact aşa a stat proba 6 până a fost prinsă. Iar căutarea şi sortarea sunt acum
cereri, nu filtrări locale — aşteptările de după o tastare în `[data-table-search]` ţin cont de cele
250 ms de întârziere plus drumul până la bază.

**Ce rămâne cu adevărat neprobabil aici:** ce se **tipăreşte**. Un PDF randat nu se citeşte din DOM
— pentru feliile care ating documente oficiale, regula rămâne cea din `todo-ui-ux.md`: randează
pagina şi uită-te la ea.

---

## Şi ce nu prinde nici DOM-ul

Trei defecte din runda de 07.09 s-au văzut **numai** deschizând captura, cu toate verificările
verzi: rândul TOTAL AN dispărut, steluţa de lângă cod, şi coloana de acţiuni a inboxului de cereri,
unde al treilea buton le rupea pe toate pe câte două rânduri. O probă poate măsura că nimic nu
depăşeşte pe orizontală; nu poate spune că arată prost.

**Şi unul pe 08.09**, de alt fel: paleta arăta la căutarea „anexa" rezultate care n-aveau cuvântul
nicăieri — „Dosar de control", „Parteneri". Părea o potrivire prea largă; erau **rânduri moarte**.
Sortarea pe scor rupe grupurile între ele, randarea deschide un `<div>` la fiecare schimbare de
grup, iar cheia lui era **numele grupului** — două surori cu aceeaşi cheie, deci reconcilierea lăsa
în listă rânduri din randarea dinainte, cu `data-index` duplicat. Nu se vedea din cod, şi din
captură se vedea doar ca „prea multe rezultate". S-a prins numărând: `check("şi nimic altceva")`.

**Încă trei, pe 07.09 târziu**, toate pe cuprinsul lipicios al paginii de Setări, toate cu
verificările de DOM verzi: `overflow-x-auto` pe `<nav>` decupa şi pe verticală, deci banda care
acoperă căptuşeala paginii nu se vedea şi pe sub bară trecea o dungă de tabel; coloana de acţiuni a
tabelelor e şi ea `sticky z-10` şi vine **după** bară în DOM, deci la z egal acoperea jumătatea din
dreapta; iar poziţia de lipire se numără de la marginea **conţinutului** zonei care se derulează,
deci pe telefon bara stătea la 128px, nu sub antet. Niciuna nu e o proprietate pe care ştiai s-o
măsori dinainte — se văd dintr-o privire şi din nimic altceva.

`shots/` (gitignored) există exact pentru asta. **Uită-te la ele** când adaugi ceva vizibil, nu doar
când cade o probă.
