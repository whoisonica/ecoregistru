# Surse oficiale — extrase verbatim

> **Ce e documentul ăsta:** citatele din actele normative pe care se construiește codul, copiate
> cuvânt cu cuvânt din sursa primară. Nu conține interpretări sau rezumate. Când codul are nevoie
> să știe „ce cere legea", răspunsul se ia de aici, nu din memorie și nu din `legislatie.md`.
>
> **Regula:** nimic nu intră aici fără link către sursa primară și data accesării. Sursele secundare
> (lege5, lege6, bloguri de specialitate, PDF-uri republicate) **nu** sunt sursă primară — vezi
> anexa de la final pentru de ce contează asta concret.
>
> Ultima verificare: **22.08.2026**, integral pe surse primare.
> Completat pe **23.08.2026** cu consecința pe model a coloanelor din cap. 3 și 4 (§1.2).

---

## 1. HG 856/2002 — evidența gestiunii deșeurilor

Sursă: [legislatie.just.ro/Public/DetaliiDocumentAfis/38294](https://legislatie.just.ro/Public/DetaliiDocumentAfis/38294)
(consolidare 26.03.2025, include modificările prin HG 210/2007), accesat 22.08.2026.

### 1.1 Cine ține evidența și pentru ce

**Art. 1 alin. (1):**

> Agenţii economici care generează deşeuri au obligaţia să ţină o evidenţă a gestiunii acestora,
> în conformitate cu modelul prevăzut în anexa nr. 1, pentru fiecare tip de deşeu.

**Art. 2 alin. (1):**

> Agenţii economici autorizaţi sa desfăşoare activităţi de colectare, transport, depozitare
> temporară, valorificare şi eliminare a deşeurilor sunt obligaţi sa ţină evidenta gestiunii
> deşeurilor conform art. 1 alin. (1) **numai pentru deşeurile generate în cadrul activităţilor
> proprii**.

**Art. 2 alin. (2):**

> Evidenta gestiunii deşeurilor colectate, transportate, depozitate temporar, valorificate şi
> eliminate se raportează de către agenţii economici autorizaţi, menţionaţi la alin. (1), la
> solicitarea autorităţilor publice teritoriale pentru protecţia mediului [...]

> **Consecință pe modelul de date.** Art. 2(1) e restrictiv, nu permisiv. Marfa preluată de la terți
> NU intră în Anexa 1 — nici la „generat", nici în Cap. 3/4 (acelea privesc valorificarea/eliminarea
> deșeului propriu). Art. 2(2) confirmă că evidența mărfii colectate se ține și se raportează
> **separat**, fără să impună modelul Anexa 1. Sunt două fluxuri, două evidențe, două formate.

**Art. 3 alin. (3)** — atenție, se referă la **autorități**, nu la firmă:

> Datele centralizate anual, referitoare la gestionarea deşeurilor, se păstrează de către
> autorităţile publice teritoriale de protecţie a mediului într-un registru de evidenţă o perioadă
> de minimum 3 ani.

Termenul de păstrare **al operatorului economic** e la OUG 92/2021 art. 48 alin. (5) — vezi §2.1.

### 1.2 ANEXA Nr. 1 — modelul fișei (reprodusă în facsimil în act)

Câmpuri de identificare, în capul fișei:

```
EVIDENŢA GESTIUNII DEŞEURILOR
Agentul economic   . . . . . . . . . . . . . .
Anul               . . . . . . . . . . . . . .
Tipul de deşeu     . . . . . . . .  cod . . . . . (conform codificării din anexa nr. 2)
Starea fizică      . . . . . . . . . . . . . .
Unitatea de măsură . . . . . . . . . . . . . .
```

> Nici „Starea fizică", nici „Unitatea de măsură" nu au listă închisă de valori în act. Sunt câmpuri
> libere. (Închide A3 și A4 cu răspuns negativ: legea nu impune nimic.)

**CAPITOLUL 1 — Generarea deşeurilor**

| Nr. | Luna | Generate | din care: valorificată | eliminată final | rămasă în stoc |
|---|---|---|---|---|---|

12 rânduri (Ianuarie … Decembrie) + rând **TOTAL AN**.

> **Nu există coloană de „predare".** Predarea către un colector autorizat se raportează în coloana
> „valorificată" sau „eliminată final", cu operatorul trecut în Cap. 3 / Cap. 4. Formula stocului
> rezultă direct: `stoc = stoc_anterior + generat − valorificat − eliminat`.

**CAPITOLUL 2 — Stocarea provizorie, tratarea şi transportul deşeurilor**

| Nr. crt. | Luna | Secţia | Stocare: Cantitatea | Stocare: Tipul¹ | Tratare: Cantitatea | Tratare: Modul² | Tratare: Scopul³ | Transport: Mijlocul⁴ | Transport: Destinaţia⁵ |
|---|---|---|---|---|---|---|---|---|---|

12 rânduri + rând **TOTAL**.

Notele oficiale — **nomenclatoare închise**, verbatim:

```
1) Tipul de stocare:
   RM - recipient metalic            RP - recipient de plastic
   BZ - bazin decantor               CT - container transportabil
   CF - container fix                S  - saci
   PD - platformă de deshidratare    VN - în vrac, neacoperit
   VA - în vrac, incintă acoperită   RL - recipient din lemn
   A  - altele

2) Modul de tratare:
   TM  - tratare mecanică            TC - tratare chimică
   TMC - tratare mecano-chimică      TB - tratare biochimică
   D   - deshidratare                TT - tratare termică
   A   - altele

3) Scopul tratării:
   V - pentru valorificare           E - în vederea eliminării

4) Mijlocul de transport:
   AS - autospeciale                 AN - auto nespecial
   H  - transport hidraulic          CF - cale ferată
   A  - altele

5) Destinaţia:
   DO - depozitul de gunoi al oraşului/comunei
   HP - halda proprie
   HC - halda industrială comună
   I  - incinerarea în scopul eliminării
   Vr - valorificare prin agenţi economici autorizaţi
   P  - utilizare materială sau energetică în propria întreprindere
   Ve - valorificare energetică prin agenţi economici autorizaţi
   A  - altele
```

> Atenție la coliziunile de abrevieri: `CF` înseamnă *container fix* la nota 1 și *cale ferată* la
> nota 4; `A` înseamnă *altele* la patru note diferite; `D` e *deshidratare* la nota 2, dar prefix de
> cod de eliminare în altă parte. Sunt cinci enum-uri distincte, nu unul comun.

**CAPITOLUL 3 — Valorificarea deşeurilor**

| Nr. | Luna | Cantitatea de deşeu valorificată | Operaţia de valorificare | Agentul economic care efectuează operaţia de valorificare |
|---|---|---|---|---|

12 rânduri + **TOTAL AN**.

**CAPITOLUL 4 — Eliminarea deşeurilor**

| Nr. | Luna | Cantitatea de deşeu eliminată | Operaţia de eliminare | Agentul economic care efectuează operaţia de eliminare |
|---|---|---|---|---|

12 rânduri + **TOTAL AN**.

> **Consecință pe modelul de date.** Cap. 3 și cap. 4 nu cer doar cantitatea: cer și *operaţia*
> (un cod din anexa nr. 3, respectiv nr. 7 la OUG 92/2021) și *agentul economic care o efectuează*.
> Deci **orice cantitate care iese de pe amplasament poartă un cod R/D**, inclusiv una predată unui
> colector — altfel rândul din cap. 3/4 nu se poate completa. Codul decide și coloana din cap. 1:
> R → „valorificată", D → „eliminată final". Litera V/E de la cap. 2 nota 3 se **derivă** din el; a o
> cere separat ar fi strict mai puțină informație decât cere formularul.
>
> ⚠️ **Ce nu tranșează sursa:** ce cod se trece când predarea se face către un colector care doar
> stochează și duce mai departe — R13 („stocarea înaintea oricărei operaţiuni R1-R12"), sau
> operaţiunea finală făcută de altcineva? Și cine e „agentul economic care efectuează operaţia":
> colectorul, sau reciclatorul final? Întrebare deschisă către specialistă
> (`docs/intrebari-specialist.md` §3). În cod: se cere codul, nu se propune niciunul implicit.

> **Referința legală din facsimil e depășită.** Actul spune „conform Anexei IIB din legea 426/2001"
> (Cap. 3) și „conform Anexei IIA din Legea 426/2001" (Cap. 4). Legea 426/2001 e abrogată. Șablonul
> de lucru al specialistei scrie „conform Anexei 3 din Legea 211/2011" — și aceea e abrogată.
> **Referința corectă de pus în export: OUG 92/2021, anexa nr. 3 (valorificare) și anexa nr. 7
> (eliminare)** — vezi §2.2 și §2.3.

---

### 1.3 Ce spune practica peste ce spune actul (corpus de 10 fişe completate, 23.08.2026)

Actul reproduce formularul; **cum se completează** l-am citit din zece Anexe 1 completate cu cifre
reale, primite de la specialistă (Cluj, Timişoara, Bragadiru, Oradea — 2022–2024, plus Cluj 2025 ca
PDF). Fişierele sunt gitignored, deci **niciun test nu le poate citi**: regula extrasă din ele se
scrie în cod ca un comentariu care spune pe câte fişiere se sprijină.

1. **Litera „E” din cap. 2 nota 3 nu se mai scrie.** Nota defineşte `V - pentru valorificare` şi
   `E - în vederea eliminării`, dar pe cele zece fişe `E` apare **o singură dată** (Cluj 2022, codul
   19 12 12) şi acelaşi client a pus liniuţă în 2023 şi 2024. Pe toate fişele de valorificare scrie
   `V` pe toate cele 12 rânduri; pe cele de eliminare, liniuţă. Ce identifică eliminarea e **codul D
   din cap. 4**, lângă operator. → `TreatmentPurpose` are un singur membru.
2. **Coloana „Secţia” din cap. 2 e constantă pe cele 12 luni** ale unei foi („birouri”, „productie”).
   E o proprietate a sursei, nu a lunii. → entitatea `InternalGenerator`, sub punctul de lucru.
3. **Fişa are exact 12 rânduri per capitol, plus TOTAL AN**, chiar şi în lunile fără mişcări.
4. **Antetul cap. 3 şi 4 trimite încă la Legea 211/2011**, abrogată de OUG 92/2021. Numerele anexelor
   sunt identice în noul act (3 = valorificare, 2 = eliminare), deci corectura ar fi doar numele
   actului — 🟠 **decizie deschisă**, e a specialistei.
5. **Unitatea e kg** pe toate fişele, şi antetul o declară explicit („Unitatea de măsură: kg”).

---

## 2. OUG 92/2021 privind regimul deșeurilor — legea-cadru

Sursă: [legislatie.just.ro/Public/DetaliiDocument/245846](https://legislatie.just.ro/Public/DetaliiDocument/245846);
text integral folosit pentru anexe:
[PDF Monitorul Oficial](https://ecoteca.ro/wp-content/uploads/2022/01/92_OG_2021_RegimulDeseurilor_10ian2022.pdf).
Accesat 22.08.2026.

### 2.1 ARTICOLUL 48 — Păstrarea evidenţei

**Alin. (1)** — cine, ce, până când:

> Producătorii de deşeuri nepericuloase, unităţile şi întreprinderile prevăzute la art. 34,
> producătorii de deşeuri periculoase şi unităţile şi întreprinderile care colectează sau transportă
> deşeuri periculoase, nepericuloase cu titlu profesional sau acţionează în calitate de comercianţi
> şi de brokeri de deşeuri periculoase şi nepericuloase ţin o **evidenţă cronologică lunară
> tabelară** şi o pun la dispoziţia agenţiei judeţene pentru protecţia mediului în format letric, la
> cerere, şi electronic în sistemul pus la dispoziţie de APM **până la 15 martie anul următor
> raportării**, precum şi la cerere autorităţilor competente de control, după:
>
> a) codul deşeului potrivit art. 7 alin. (1), **cantitatea în tone**, natura şi originea deşeurilor
> generate, precum şi cantitatea de produse şi materiale care rezultă din pregătirea pentru
> reutilizare, din reciclare sau din alte operaţiuni de valorificare, eliminare;
> b) destinaţia, frecvenţa colectării, modul de transport şi metoda de tratare prevăzută pentru
> deşeuri, atunci când este relevant; şi
> c) **cantitatea de deşeuri în tone** încredinţată spre eliminare.

**Alin. (4)** — termenul **autorității**:

> APM păstrează pentru scopuri statistice, cel puţin 5 ani, evidenţele prevăzute la alin. (1).

**Alin. (5)** — termenul **operatorului economic**:

> Operatorii economici prevăzuţi la alin. (1) sunt obligaţi să păstreze evidenţa gestiunii deşeurilor
> **cel puţin 3 ani**, cu excepţia operatorilor economici care desfăşoară activităţi de transport,
> care trebuie să păstreze evidenţa **timp de cel puţin 12 luni**.

**Alin. (6):**

> La cererea autorităţilor competente sau a unui deţinător anterior, operatorii economici prevăzuţi
> la alin. (1) trebuie să furnizeze documentele justificative conform cărora operaţiunile de
> gestionare au fost efectuate.

> **Trei consecințe.**
> 1. Dosarul de control se dimensionează la **3 ani** (12 luni la transportatori). Închide D1.
> 2. **15 martie e termen legal**, nu cutumă ANPM. Închide C3.
> 3. Evidența lunară se ține în kg (practica fișei Anexa 1), dar **raportarea de la art. 48 e în
>    tone**. Conversia trebuie să existe într-un singur loc în cod, nu presărată prin export.

### 2.2 ANEXA Nr. 3 — OPERAŢIUNI DE VALORIFICARE

```
R1  Întrebuinţarea în principal drept combustibil sau ca altă sursă de energie
R2  Valorificarea/Regenerarea solvenţilor
R3  Reciclarea/Recuperarea substanţelor organice care nu sunt utilizate ca solvenţi
    (inclusiv compostarea şi alte procese de transformare biologică)
R4  Reciclarea/Recuperarea metalelor şi compuşilor metalici
R5  Reciclarea/Recuperarea altor materiale anorganice
R6  Regenerarea acizilor sau a bazelor
R7  Valorificarea componenţilor utilizaţi pentru reducerea poluării
R8  Valorificarea componentelor catalizatorilor
R9  Rerafinarea uleiului uzat sau alte reutilizări ale uleiului uzat
R10 Tratarea terenurilor având drept rezultat beneficii pentru agricultură sau ecologie
R11 Utilizarea deşeurilor obţinute din oricare dintre operaţiunile numerotate de la R 1 la R 10
R12 Schimbul de deşeuri în vederea expunerii la oricare dintre operaţiunile numerotate
    de la R 1 la R 11
R13 Stocarea deşeurilor înaintea oricărei operaţiuni numerotate de la R 1 la R 12
    (excluzând stocarea temporară, înaintea colectării, la situl unde a fost generat deşeul)
```

### 2.3 ANEXA Nr. 7 — OPERAŢIUNILE DE ELIMINARE

```
D1  Depozitarea în sau pe sol (de exemplu, depozite de deşeuri etc.)
D2  Tratarea solului (de exemplu, biodegradarea deşeurilor lichide sau nămoloase în sol etc.)
D3  Injectarea în adâncime (de exemplu, injectarea deşeurilor care pot fi pompate în puţuri,
    saline sau depozite geologice naturale etc.)
D4  Acumulare la suprafaţă (de exemplu, depunerea de deşeuri lichide sau nămoloase în bazine,
    iazuri sau lagune etc.)
D5  Depozite special construite (de exemplu, depunerea în compartimente separate etanşe care sunt
    acoperite şi izolate unele faţă de celelalte şi faţă de mediul înconjurător etc.)
D6  Evacuarea într-o masă de apă, cu excepţia mărilor/oceanelor
D7  Evacuarea în mări/oceane, inclusiv eliminarea în subsolul marin
D8  Tratarea biologică nemenţionată în altă parte în prezenta anexă, care generează compuşi sau
    mixturi finale eliminate prin intermediul unuia dintre procedeele numerotate de la D1 la D12
D9  Tratarea fizico-chimică nemenţionată în altă parte în prezenta anexă, care generează compuşi
    sau mixturi finale eliminate prin intermediul unuia dintre procedeele numerotate de la D1 la
    D12 (de exemplu, evaporare, uscare, calcinare etc.)
D10 Incinerarea pe sol
D11 Incinerarea pe mare       [operaţiune interzisă de legislaţia UE şi de convenţii internaţionale]
D12 Stocarea permanentă (de exemplu, plasarea de recipiente într-o mină etc.)
D13 Amestecarea anterioară oricărei operaţiuni numerotate de la D1 la D12
D14 Reambalarea anterioară oricărei operaţiuni numerotate de la D1 la D13
D15 Stocarea înaintea oricărei operaţiuni numerotate de la D1 la D14 (excluzând stocarea
    temporară, înaintea colectării, în zona de generare a deşeurilor)
```

**13 coduri R + 15 coduri D.** `WasteOperationCode` din cod e corect și complet.

> **Nuanța D1 / D5 NU e tranșată de sursa primară.** Specialista a spus că deșeul menajer dus la
> depozit se codifică **D5**, nu D1, pentru că depozitele conforme au celule etanșe — iar textul lui
> D5 („compartimente separate etanşe [...] acoperite şi izolate") descrie exact asta. **Dar exemplul
> dat la D1 este literalmente „depozite de deşeuri".** E o judecată de încadrare, nu un fapt
> verificabil. Nu se propune ca valoare implicită în formular fără confirmare explicită. Rămâne
> întrebare deschisă.

---

## 3. Nomenclatorul codurilor de deșeuri

### 3.1 Sursa care se folosește: Decizia 2014/955/UE

Sursă: [EUR-Lex CELEX 32014D0955, versiunea RO](https://eur-lex.europa.eu/legal-content/RO/TXT/?uri=CELEX%3A32014D0955),
accesat 22.08.2026. Modifică Decizia 2000/532/CE; **se aplică de la 1 iunie 2015**.

Extras din structura de tabel a Jurnalului Oficial și validat programatic:

```
CODURI: 842 | unice: 842 | periculoase: 408
duplicate:                 niciunul
cod sub subcapitol greşit: niciunul
cod sub capitol greşit:    niciunul
format invalid:            niciunul
```

Distribuția pe capitole — folosită ca amprentă de regresie la orice reîncărcare:

```
01:24  02:38  03:19  04:21  05:24  06:48  07:78  08:38  09:13  10:173
11:27  12:23  13:34  14:5   15:12  16:72  17:38  18:16  19:99  20:40
```

**În cod (din 22.08.2026, Etapa 1):** nomenclatorul se regenerează cu
`python scripts/generate_waste_codes.py`, care descarcă HTML-ul de mai sus, îl parsează și refuză
să scrie dacă amprenta nu se reproduce exact. Rezultatul e
`backend/src/main/resources/seed/waste_codes.csv` (cu titlurile de capitol și subcapitol păstrate ca
structură), încărcat de migrarea `V4__reseed_waste_codes`. Cele cinci validări de mai sus rulează și
ca test Java — `WasteCodeSeedTest` — nu doar în script.

### 3.2 De ce lista 2014 și nu Anexa 2 din HG 856/2002

Anexa 2 la HG 856/2002 e versiunea 2002/2007, netranspusă după Decizia 2014/955/UE. Argumentul
decisiv nu e vechimea, ci practica legiuitorului: **proiectul de HG TRACE-DM (mai 2026), art. 3
lit. a) și art. 5 lit. d), definește codul deșeului explicit „conform Deciziei Comisiei
2014/955/UE"** (vezi §7). Legiuitorul român de azi scrie pe lista 2014.

### 3.3 Reconcilierea celor două liste

Anexa 2 la HG 856/2002 conține **839** coduri. Diferențele față de lista 2014:

| | Cod | Denumire |
|---|---|---|
| **+** (doar în LED 2014) | `01 03 10*` | nămoluri roșii rezultate din producerea aluminei, care conțin substanțe periculoase |
| **+** | `13 03 06*` | uleiuri minerale clorurate izolante și de transmitere a căldurii, altele decât cele specificate la 13 03 01 |
| **+** | `16 03 07*` | mercur metalic |
| **+** | `19 03 08*` | mercur parțial stabilizat |
| **−** (doar în HG 856) | `13 03 05*` | aceeași denumire ca `13 03 06*` — **renumerotat**, nu eliminat |

`839 + 4 − 1 = 842`. Se închide exact.

**Zero divergențe pe marcajul de periculozitate** pe cele 838 de coduri comune. Cele două liste,
obținute din surse independente, se validează reciproc.

> **Compatibilitate:** un client cu documente vechi poate avea hârtii pe `13 03 05*`. Dacă apare
> cazul, se tratează ca alias istoric către `13 03 06*` — nu se reintroduce în nomenclator.

---

## 4. HG 1061/2008 — transportul deșeurilor pe teritoriul României

Sursă: [legislatie.just.ro/Public/DetaliiDocument/97706](https://legislatie.just.ro/Public/DetaliiDocument/97706),
consolidare 23.01.2026. **În vigoare.** Accesat 22.08.2026.

Formularul de încărcare-descărcare deșeuri nepericuloase (anexa nr. 3) — câmpuri:

```
Serie şi număr
Date de identificare expeditor    (+ autorizaţie de mediu)
Date de identificare destinatar   (+ autorizaţie de mediu)
Date de identificare transportator
Data | Caracteristici deşeuri: cod, descriere | Cantitate: tone, mc
Date privind punctul de lucru | Observaţii | Semnătura: încărcare, descărcare
```

**Art. 20 alin. (2):** formularul se completează de expeditor în **3 exemplare** — unul rămâne la
expeditor, unul la transportator, unul se transmite destinatarului prin transportator.

> ✅ **Structurat pe 23.08.2026 (Etapa G3).** Observația de mai sus s-a rezolvat: rubricile
> formularului sunt acum câmpuri pe `WasteMovement` (`unloadDate`, `transportPartner`, `driverName`,
> `driverIdentification`, `vehicleRegistration`, `transportDestinations`, `anexa3Series` +
> `anexa3Number`, `volumeM3`), iar `Anexa3FormGenerator` tipărește documentul.
> `documentReference` a rămas ce era de fapt: numărul avizului, care merge în coloana „Observaţii”.

**Rubricile exacte, citite din două modele completate** (`documente oficiale/ANEXA 3 model_CARTON.docx`
și `Anexa 3_model.pdf`, seria HMB 180) — actul reproduce formularul în facsimil, iar modelele arată
cum se completează în practică:

```
Serie şi număr
┌ Date de identificare transportator: denumire, adresă, CIF, Reg. Com.
├ Date de identificare delegat şi nr. de înmatriculare mijloc de transport
├ Licenţa de transport mărfuri nepericuloase nr.
└ Data la care expiră licenţa · Semnătura
Data: Încărcare | Descărcare
Caracteristici deşeuri: categorii/cod · Descriere · Destinat: colectării |_| stocării temporare |_|
                                                       tratării |_| valorificării |_| eliminării |_|
Cantitate: kg (sau tone) · mc
┌ ÎNCĂRCAREA — date de identificare expeditor (+ autorizaţie de mediu, expirare, semnătură+ştampilă)
└ DESCĂRCAREA — date de identificare destinatar (+ autorizaţie de mediu, expirare, semnătură+ştampilă)
Observaţii: aviz
*) Se va completa numai în cazul în care încărcarea/descărcarea are loc la un punct de lucru care
   nu reprezintă sediul social.
```

**Trei lucruri pe care le-au lămurit modelele completate, nu actul:**

1. **Cantitatea e scrisă de mână.** Pe modelul HMB 180 apare „1,02”, completat ulterior — expeditorul
   n-avea cântar. De aici steagul `weighedAtUnloading`: rubrica se tipărește goală și se completează
   la descărcare, de destinatar.
2. **„Destinat:” admite mai multe bife.** Pe același model sunt bifate două: *Colectării* și
   *Valorificării*. De aceea e un set, nu o alegere unică.
3. **Destinatarul e scris cu punctul de lucru, nu cu sediul** („P.L. ILFOV, Şos. de Centura nr. 2-8,
   Bragadiru”). De aceea partenerul are două adrese.

**Art. 20 alin. (2): formularul se completează de expeditor în 3 exemplare** — unul rămâne la
expeditor, unul la transportator, unul ajunge la destinatar prin transportator. Generatorul produce
**un** formular; cele trei exemplare se obţin tipărind documentul de trei ori, fiindcă sunt copii
identice care se semnează separat, nu variante diferite ale aceluiaşi document.

⚠️ **A nu se confunda cu „cele 4 tabele”.** Sunt două documente diferite, iar confuzia s-a şi
produs pe 23.08:
- **Anexa 3 la HG 1061/2008** = formularul de transport de aici, un tabel, per transport. Modele:
  `documente oficiale/Anexa 3_model.pdf` şi `ANEXA 3 model_CARTON.docx`.
- **„Cele 4 tabele”** = cele patru capitole ale fişei **Anexa 1 la HG 856/2002**, per cod de deşeu
  şi per an. Model: `documente oficiale/deseuri generate_Cluj_2025_Iuhos Lorena.pdf`.

---

## 5. Ordinul MMP 794/2012 — raportarea ambalajelor

Sursă: [legislatie.just.ro/Public/DetaliiDocument/135672](https://legislatie.just.ro/Public/DetaliiDocument/135672),
accesat 22.08.2026. **În vigoare.** Art. 14 abrogă Ordinul 927/2005.

Actul din care provin cele două fișiere primite de la specialistă:

| Anexă | Cine raportează | Fișierul primit |
|---|---|---|
| Anexa 1 | producători şi importatori de ambalaje de desfacere, de produse ambalate, supraambalatori | `RAPORTARE AMBALAJE _anexa 1.xlsx` |
| Anexa 2A / 2B | operatori economici autorizaţi care au preluat obligaţiile | — |
| Anexa 3 | colectori, reciclatori, valorificatori şi comercianţi de deşeuri de ambalaje | `RAPORTARE DESEURI DE AMBALAJ COLECTATE ANUAL.ods` |
| Anexa 4 | autorităţi ale administraţiei publice locale | — |

**Termen: 25 februarie** al fiecărui an, pentru anul precedent. Se depune la APM judeţeană / ANPM.

> ✅ **REZOLVAT 22.08.2026 — unitatea este KILOGRAME.** Textul oficial al ordinului tipărește
> `[kilograme]` în antetul tabelului, **la toate cele cinci anexe** (1, 2A, 2B, 3, 4), fără excepție.
> Verificat pe Portalul Legislativ, atât pe `DetaliiDocument/135672`, cât și pe
> `DetaliiDocumentAfis/135672`.
>
> Deci fișierul primit de la specialistă, care folosește **tone** cu 3 zecimale (`0.000`), e un
> **șablon modificat local**, nu forma oficială. Ordinul n-a fost amendat pe unitate.
>
> **Consecință:** modulul de ambalaje se deblochează. Aplicația ține evidența în kg oricum, deci
> raportarea Anexei 3 nu are nevoie de conversie — spre deosebire de raportarea de la art. 48
> OUG 92/2021, care e în tone. Sunt două unități diferite, în două rapoarte diferite; conversia
> kg→tone rămâne necesară **doar** pentru art. 48.
>
> ⚠️ Merită totuși o confirmare de o linie de la specialistă: dacă APM-ul județean îi acceptă în
> practică șablonul în tone, generăm ce cere actul (kg) și o avertizăm despre diferență.

---

## 6. Ordinul MMAP 701/2024 — SIATD

Sursă: [legislatie.just.ro/Public/DetaliiDocument/281612](https://legislatie.just.ro/Public/DetaliiDocument/281612),
Monitorul Oficial nr. 331 din 10.04.2024. Accesat 22.08.2026.

- **Cine:** art. 2 enumeră 15 categorii (lit. a–o) de operatori — OIREP, colectare, sortare, tratare,
  reciclare, brokeraj, salubrizare, UAT-uri. **Generatorii mici nu intră.** (Închide E1.)
- **Fluxuri acoperite:** ambalaje, anvelope, DEEE, baterii şi acumulatori portabili, deşeuri
  municipale.
- **Termene de confirmare după recepţie (art. 18):**
  `deşeuri municipale 3 zile · ambalaje 5 zile · anvelope 5 zile · DEEE şi baterii 15 zile`
- **Date per tranzacţie (art. 18 alin. 2):** identificarea contractorului, cantităţi şi tipuri,
  documente însoţitoare (formular de încărcare-descărcare, factură), **coordonate GPS ale punctului
  de lucru**, **fotografii (2–4 unghiuri la ambalaje)**.
- **Sancţiune:** suspendarea accesului.

---

## 7. Proiect HG — sistemul TRACE-DM (NEADOPTAT)

Sursă: [sgglegis.gov.ro — proiect HG, mai 2026](https://sgglegis.gov.ro/legislativ/docs/2026/05/s954rx0c2hmqfj8_ndb3.pdf).
Consultare publică MMAP, mai 2026. Accesat 22.08.2026.

> **Stare: PROIECT.** Nu se codează nimic pe baza lui. E consemnat aici pentru că definește câmpurile
> unei viitoare entități `Reception` și pentru că e argumentul comercial al modulului de depozit.

Sistem centralizat administrat de **AFM**, pentru trasabilitatea deșeurilor municipale reciclabile
predate de **persoane fizice** către operatori autorizați. Se instituie în **180 de zile**,
interconectat cu SIM.

**Art. 5 — date înregistrate per tranzacție:**

```
a) operator: denumire, CUI, nr. autorizaţie de mediu, adresa punctului de colectare
b) persoană fizică: nume şi prenume, CNP, seria şi numărul actului de identitate, domiciliu
c) data şi ora tranzacţiei
d) codul deşeului, conform Deciziei Comisiei 2014/955/UE
e) cantitatea predată, exprimată în kilograme
f) valoarea de achiziţie şi modalitatea de plată
g) declaraţia pe propria răspundere privind provenienţa deşeurilor din gospodăria proprie
h) tipul activităţii generatoare de deşeuri
```

**Art. 6 — praguri de înregistrare extinsă** (cumulat pe an calendaristic şi pe persoană fizică):
`hârtie şi carton 500 kg · materiale plastice 200 kg · sticlă 200 kg · metal 500 kg ·
deşeuri reciclabile din construcţii şi demolări 750 kg`

**Art. 8 — obligațiile operatorului:** verificarea identității pe act valabil; **înregistrare în
maximum 24 de ore** de la tranzacție; solicitarea și consemnarea declarației; refuzul preluării dacă
persoana refuză actul sau declarația; **păstrarea evidenţelor cel puţin 3 ani**.

**Art. 9 alin. (2):** borderoul de achiziţie cerut de OUG 31/2011 la metale **se întocmeşte şi se
generează în format electronic prin sistemul TRACE-DM**.

**Art. 13 — sancțiuni:** 20.000–40.000 lei pentru neînregistrare / date eronate / preluare nelegală;
10.000–20.000 lei pentru lipsa declarației; complementar, suspendarea dreptului de utilizare 15–30
de zile şi reanalizarea autorizaţiei de mediu.

---

## 8. HG 349/2005 — depozitarea deșeurilor (profilul „groapă")

Sursă: [legislatie.just.ro/Public/DetaliiDocument/61498](https://legislatie.just.ro/Public/DetaliiDocument/61498),
accesat 22.08.2026. Transpune Directiva 1999/31/CE. Abrogă HG 162/2002.

### 8.1 Art. 15 — procedura de recepție în depozit

**Alin. (1)** — operatorii depozitelor sunt obligați să respecte, la primirea deșeurilor, următoarele
proceduri de recepție:

> a) verificarea documentaţiei privind cantităţile şi caracteristicile deşeurilor, originea şi natura
> lor, inclusiv buletine de analiză pentru deşeurile industriale, iar pentru deşeurile municipale,
> când există suspiciuni, precum şi date privind identitatea producătorului sau a deţinătorului
> deşeurilor;
>
> b) inspecţia vizuală a deşeurilor la intrare şi la punctul de depozitare şi, după caz, verificarea
> conformităţii cu descrierea prezentată în documentaţia înaintată de deţinător, conform procedurii
> stabilite la pct. 3.1 nivelul 3 din anexa nr. 3;
>
> c) păstrarea, cel puţin o lună, a probelor reprezentative prelevate pentru verificările impuse
> conform prevederilor cuprinse la pct. 3.1 nivelul 1 sau nivelul 2 din anexa nr. 3, precum şi
> înregistrarea rezultatelor determinărilor;
>
> d) **păstrarea unui registru cu înregistrările privind cantităţile, caracteristicile deşeurilor
> depozitate, originea şi natura, data livrării, identitatea producătorului, a deţinătorului sau,
> după caz, a colectorului** — în cazul deşeurilor municipale, iar în cazul deşeurilor periculoase,
> a **localizării precise a acestora în depozit**.

> **Consecință pe model.** Lit. d) e o a treia evidență, distinctă și de Anexa 1, și de registrul
> art. 48: un **registru de recepție al depozitului**, cu un câmp pe care nu-l are nimeni altcineva —
> *localizarea precisă în depozit* pentru deșeurile periculoase. Lit. a) și c) adaugă **buletine de
> analiză** și **probe păstrate o lună** — atașamente cu termen, nu doar fișiere.

### 8.2 Art. 20 — raportarea către autoritatea de mediu

Operatorul depozitului raportează **autorităţii competente pentru protecţia mediului**:

> a) **semestrial**, datele înregistrate în urma monitorizării, pentru a demonstra conformitatea cu
> prevederile din autorizaţia/autorizaţia integrată de mediu, precum şi stadiul îndeplinirii măsurilor
> din programul pentru conformare, dacă este cazul;
>
> b) **în maximum 12 ore de la constatare**, orice efecte ecologice negative semnificative constatate
> prin programul de monitorizare.

> **Consecință pe modulul Termene.** Apar două tipuri noi de termen, niciunul acoperit azi:
> un **termen semestrial** recurent și un **termen de 12 ore** declanșat de eveniment (nu de calendar).
> Al doilea nu e un termen programabil, ci o alertă de incident — alt mecanism.

---

## 9. OUG 31/2011 — borderoul de achiziție la metale

Sursă: [legislatie.just.ro/Public/DetaliiDocument/127186](https://legislatie.just.ro/Public/DetaliiDocument/127186),
accesat 22.08.2026. Modificată prin Legea 38/2014.

**Art. 1 alin. (1)** interzice achiziţionarea de la persoane fizice a unor categorii enumerate de
metale feroase şi neferoase (componente de cale ferată, utilităţi, semnalizare, ţiţei/gaze etc.).

**Art. 1 alin. (1^1)** — excepția care contează comercial:

> Metalele feroase şi neferoase şi aliajele acestora, altele decât cele prevăzute la alin. (1) [...]
> pot face obiectul operaţiunilor de comerţ numai în condiţiile în care acestea **provin din
> gospodăriile proprii**.

**Art. 1 alin. (1^3):**

> Borderoul de achiziţie [...] constituie **document de evidenţă financiar-contabilă, cu regim intern
> de numerotare**.

**Câmpurile borderoului (anexa la ordonanță):** date de identificare ale operatorului (denumire, formă
juridică, adresă, reg. comerțului, CUI/CIF, **autorizaţie de mediu**); date de identificare ale
persoanei fizice (nume, **serie și număr act de identitate, CNP**, domiciliu); **codul deşeului**;
cantitatea în **kg**; preţul unitar şi valoarea; contul de virament / documentul de plată;
**declaraţia deţinătorului** că deșeul provine din gospodăria proprie.

**Sancțiuni:** 100.000–150.000 lei pentru încălcarea alin. (1) și (1^1); 30.000–50.000 lei pentru
încălcarea regulilor de plată; la repetare, limitele cresc cu 50%.

> **Două note pentru cod.**
> 1. Borderoul cere codul deşeului **„conform HG 856/2002"**, adică lista 2002/2007 — nu Decizia
>    2014/955/UE, pe care ne bazăm nomenclatorul. Practic diferența e neglijabilă la metale (cele 4
>    coduri divergente sunt la nămoluri roșii, uleiuri izolante și mercur), dar dacă vreodată tipărim
>    un borderou, codul trebuie luat din nomenclator **fără** să pretindem că e „conform 2014/955".
> 2. Borderoul are **CNP și serie/număr de act de identitate**. Ca volum de date personale sensibile
>    cere tratament GDPR distinct de restul aplicației (retenție, acces, log). Nu e o simplă tabelă
>    în plus.
>
> **Termenul de păstrare nu e în ordonanță.** Ca document financiar-contabil intră sub regimul
> general al Legii contabilității — **de verificat separat**, nu se presupune.

---

## 10. OUG 196/2005 — Fondul pentru mediu: cine, cât, cât de des

Sursă: [legislatie.just.ro/Public/DetaliiDocumentAfis/258980](https://legislatie.just.ro/Public/DetaliiDocumentAfis/258980),
versiune consolidată, accesat 22.08.2026.

### 10.1 Art. 9 alin. (1) lit. a) — contribuția de 2%, reținută la sursă

> o contribuţie de **2% din veniturile realizate din vânzarea deşeurilor**, obţinute de către
> deţinătorul deşeurilor, persoană fizică sau juridică. Sumele **se reţin la sursă de către operatorii
> economici care desfăşoară activităţi de colectare şi/sau valorificare a deşeurilor**, care au
> obligaţia să le vireze la Fondul pentru mediu.

> **Cel mai important lucru găsit pentru modulul de depozit.** Contribuția o *datorează* vânzătorul,
> dar o **reține și o virează colectorul** — adică exact clientul-depozit. Deci un centru de colectare
> are o obligație AFM **lunară**, structural, prin simplul fapt că cumpără deșeu. Nu e opțională și nu
> depinde de ambalaje. Calculul se face pe fiecare achiziție → cade direct pe entitatea `Reception`,
> care oricum are prețul.

### 10.2 Art. 9 alin. (1) lit. c) — contribuția pentru economia circulară

> contribuţia pentru economia circulară încasată de la **proprietarii sau, după caz, administratorii
> de depozite** pentru deşeurile municipale, deşeuri din construcţii şi desfiinţări, destinate a fi
> eliminate prin depozitare, **în cuantumul prevăzut în anexa nr. 2**.

Alte litere relevante: **d)** 2 lei/kg la ambalaje (ținte nerealizate); **i)** 2 lei/kg anvelopă;
**p)** 50 lei/tonă la UAT-uri care ratează ținta de reducere.

> ⚠️ **Cuantumul din anexa nr. 2 nu s-a putut extrage** — Portalul Legislativ trunchiază anexele pe
> versiunile consolidate. E singurul număr care lipsește. De obținut înainte de orice cod pe profilul
> de groapă.

### 10.3 Art. 11 — trei cadențe, nu una

> **Alin. (1):** Sumele prevăzute la art. 9 alin. (1) **lit. a), b), e), f) şi s)** se declară şi se
> plătesc **lunar** [...] până la data de **25 inclusiv a lunii următoare**.
>
> **Alin. (1^1):** Sumele prevăzute la art. 9 alin. (1) **lit. c)** se declară şi se plătesc
> **trimestrial** [...] până la data de **25, inclusiv, a lunii următoare trimestrului**.
>
> **Alin. (2):** Sumele prevăzute la art. 9 alin. (1) **lit. d), i), j), p), v), w) şi x)** se declară
> şi se plătesc **anual** [...] până la data de **25 ianuarie inclusiv a anului următor**.

> **Consecință pe modulul Termene — gap confirmat.** Azi generăm un singur termen AFM, lunar pe 25,
> pentru orice firmă cu `afmObligation`. Realitatea are **trei cadențe**, după *care* contribuție o
> datorează firma: lunar (inclusiv contribuția de 2% a colectorului), trimestrial (economia circulară
> — depozitele), anual pe 25 ianuarie (ambalaje, anvelope, UAT). Flagul boolean `afmObligation` e prea
> sărac: are nevoie să devină un **set de contribuții datorate**, fiecare cu cadența ei.

---

## Anexă — de ce „sursă primară" nu e pedanterie

PDF-ul HG 856/2002 primit de la specialistă provine de pe **lege6.ro**. Comparat cu textul de pe
Portalul Legislativ, conține **patru erori de transcriere** în Anexa 2, toate confirmate:

| Ce zice lege6 | Ce zice actul oficial |
|---|---|
| `10 01 13*` „deşeuri de degresare", plasat în subcapitolul `11 01` | **`11 01 13*`** |
| `11 01 10*` „răşini schimbătoare de ioni saturate sau epuizate" (cheie duplicată) | **`11 01 16*`** |
| antet de subcapitol `11 08 deşeuri de la procesele de galvanizare la cald` | **`11 05`** |
| blocurile `07 02`, `07 03`, `07 04` intercalate în capitolul 06 | cap. 06 curge contiguu până la `06 09`; cap. 07 începe separat |

Un parser care atribuie părintele după „ultimul subcapitol văzut" ar fi greșit sistematic **44 de
coduri**. De aceea: seed-ul de coduri nu se face niciodată dintr-o sursă secundară, iar validările
structurale rulează **ca test**, nu o singură dată de mână.
