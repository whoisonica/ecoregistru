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
> Completat pe **02.09.2026** cu citirea integrală a corpusului de formulare completate: cele cinci
> nomenclatoare verificate valoare cu valoare faţă de cod (§1.2), şase constatări noi din practică
> (§1.3, punctele 6–10) şi notele tipărite pe declaraţia de ambalaje (§5.3).

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

> ✅ **HG 856/2002 nu impune niciun termen de depunere** — reverificat pe Portalul Legislativ pe
> 02.09.2026 (consolidare 19.03.2007, ultima). Obligaţia e de a **ţine** evidenţa (art. 1) şi de a o
> transmite **la cererea** autorităţii (art. 2 alin. (2), art. 3). Singura depunere cu dată fixă din
> tot dreptul aplicabil e cea din **OUG 92/2021 art. 48 alin. (1) — 15 martie**, electronic, în
> sistemul APM. **Deci pe 15 martie e o singură depunere, nu două: închide întrebarea Q.** Codul
> avea deja un singur `ReportType.SIM_ANNUAL`; nu se adaugă un al doilea.

**Art. 4 alin. (3)** — cum se scrie un cod periculos. *Citat adăugat pe 02.09.2026, la auditul de
conformitate; e temeiul punctului 2 din `audit-conformitate.md`.*

> Deşeurile periculoase prevăzute în anexa nr. 2 sunt marcate cu un **asterisc (*)**.

**Art. 5 alin. (1):**

> Tipurile de deşeuri prevăzute în anexa nr. 2 sunt definite în mod individual printr-un cod complet
> format din **6 cifre**.

> **Consecință pe export.** Antetul fișei cere „Tipul de deşeu … cod … **(conform codificării din
> anexa nr. 2)**" — deci codificarea la care trimite formularul e chiar cea în care asteriscul e
> marcajul de periculozitate. Anexa 2 tipărește 406 coduri cu asterisc; lista 2014 are 408.
> `waste_codes.csv` ține deliberat codul fără asterisc și periculozitatea ca boolean separat, ceea
> ce e corect pentru stocare — dar la **tipărire** asteriscul trebuie pus la loc, altfel un deșeu
> periculos arată pe fișă exact ca unul nepericulos.
>
> ⚠️ Sursa e Portalul Legislativ, nu PDF-ul din `documente oficiale/` — acela vine de pe lege6.ro și
> are erori de transcriere confirmate (vezi `.gitignore`). Ambele spun același lucru aici, dar
> citatul de mai sus e de pe sursa primară.
>
> **Asteriscul stă în coloana de cod, şi numai acolo — verificat pe anexa 2, 02.09.2026.** În
> denumiri, actul scrie referinţele încrucişate **fără** asterisc: `19 12 12` = „alte deşeuri […]
> altele decât cele specificate la **19 12 11**", iar `20 01 36` = „[…] altele decât cele specificate
> la **20 01 21, 20 01 23 şi 20 01 35**" — toate trei periculoase, toate trei scrise curat. Asteriscul
> apare doar în coloana din stânga (`19 13 01*`, `20 01 37*`).
> **Consecinţa pe reparaţie:** se pune asteriscul **numai** pe cod. `waste_codes.csv` are denumirile
> verbatim din EUR-Lex şi ele **sunt deja corecte** — nu se atinge nimic acolo.
>
> 🟡 **Ce spune corpusul, şi cât de departe merge.** Cele patru fişiere din 2022 şi 2024 scriu în
> denumirea lui `19 12 12` „…altele decât cele specificate la **19 12 11\***", **cu** asterisc — deci
> practica specialistei foloseşte marcajul când trimite la un cod periculos. E o înfrumuseţare a lor
> faţă de textul actului, nu invers, deci **nu o copiem în denumiri**. Dar arată că marcajul le e
> familiar şi îl aşteaptă.
> ⚠️ **Ce corpusul nu poate arăta:** niciunul din cele 33 de rânduri raportate nu e pe un cod
> periculos — sunt `20 01 01`, `19 12 12`, `20 03 01`, `20 01 36`, `15 01 xx`. Deci n-avem niciun
> exemplu de antet completat cu un cod periculos. De aici întrebarea **AE**, care e confirmare de
> practică, nu de drept.
>
> **Atenție la coliziune de marcaj:** declarația anuală folosește deja `(*)` pe coloana de stoc cu
> alt înțeles — ieșiri fără cod R/D. Cele două pot ajunge pe același rând, deci legenda trebuie să
> le despartă.

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

> ✅ **Verificat în cod, valoare cu valoare, pe 02.09.2026.** Legenda de mai sus apare şi în corpus,
> pe foaia `simboluri` din cele trei fişiere Oradea — deci o avem din două surse independente, actul
> şi un fişier de lucru al specialistei. Cele cinci enum-uri (`StorageType`, `TreatmentMethod`,
> `TransportMeans`, `WasteDestination`, `TreatmentPurpose`) conţin **exact** valorile de aici, în
> aceeaşi ordine, cu o singură abatere deliberată: `TreatmentPurpose` n-are `E` — motivul e la §1.3,
> punctul 1. Nimic de corectat.

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

### 1.3 Ce spune practica peste ce spune actul (corpus de fişe completate, 23.08.2026, recitit 02.09.2026)

Actul reproduce formularul; **cum se completează** l-am citit din Anexele 1 completate cu cifre
reale, primite de la specialistă (Cluj, Timişoara, Bragadiru, Oradea — 2022–2024, plus Cluj 2025 ca
PDF). Fişierele sunt gitignored, deci **niciun test nu le poate citi**: regula extrasă din ele se
scrie în cod ca un comentariu care spune pe câte fişiere se sprijină.

⚠️ **Corectare, 02.09.2026 — corpusul e al a două firme, nu al uneia.** Până acum scria peste tot
„zece fişe, o singură firmă". Recitirea integrală (13 fişiere, 409 rânduri de lună în cap. 2) arată
că `deseuri generate_Cluj_2025_Iuhos Lorena.pdf` e al unei **alte firme**: `Panemar Jr.`,
CUI RO 17022001, **CAEN 1071 — brutărie**, întocmit de altcineva. Restul e Hamburger Recycling
Romania. Diferenţa contează: Hamburger tratează efectiv (balotează), Panemar doar predă, iar
**contrastul dintre ele răspunde la întrebări** pe care o singură firmă nu le putea răspunde
(vezi punctul 6 de mai jos).

1. **Litera „E” din cap. 2 nota 3 nu se scrie.** Nota defineşte `V - pentru valorificare` şi
   `E - în vederea eliminării`, dar pe toate cele 13 fişiere `E` apare **într-o singură foaie**
   (Cluj 2022, codul 19 12 12), pe 11 rânduri, iar acelaşi client a pus liniuţă în 2023 şi 2024.
   → `TreatmentPurpose` are un singur membru.
   ✅ **Întărit pe 02.09.2026, din două direcţii.** Întâi: cele 11 rânduri cu `E` sunt **greşite în
   fişierul lor** — cap. 1 al aceleiaşi foi arată cantitatea trecută la *valorificat* (6 · 20,48 ·
   4,42 · 9,26 t), cu *eliminat* zero pe toate lunile. Au scris „în vederea eliminării" peste o
   valorificare. Al doilea: fişierele Oradea, care **chiar elimină** (`20 03 01` → `D1`, prin RER
   Ecologic Service), lasă celula **goală**, nu `E`. Deci practica e consecventă: la eliminare,
   liniuţă. Numărătoarea completă a coloanei: `V` 342 · liniuţă 188 · gol 25 · `E` 11.
2. **Coloana „Secţia” din cap. 2 e constantă pe cele 12 luni** ale unei foi („birouri”, „productie”).
   E o proprietate a sursei, nu a lunii. → entitatea `InternalGenerator`, sub punctul de lucru.
   ✅ **Numărat pe 02.09.2026, 409 rânduri:** `birouri` 336 · `productie`/`prod.`/`PRODUCTIE` 56 ·
   **`birouri+productie` 15** · `personal` 2. Cele două implicite ale aplicaţiei acoperă 407 din
   409, iar practica de a le tipări **pe amândouă** când nu s-a ales una există deja în corpus,
   scrisă combinat într-o singură celulă. → închide întrebarea **X**.
3. **Fişa are exact 12 rânduri per capitol, plus TOTAL AN**, chiar şi în lunile fără mişcări.
4. **Antetul cap. 3 şi 4 trimite încă la Legea 211/2011**, abrogată de OUG 92/2021. Numerele anexelor
   sunt identice în noul act (3 = valorificare, 2 = eliminare), deci corectura e doar numele actului.
   ✅ **Închis pe 02.09.2026, şi nu era o întrebare.** Un act abrogat nu devine autoritate fiindcă
   apare într-un model: şablonul lor e pur şi simplu **vechi**, ne-actualizat după 2021. Tipărim
   OUG 92/2021 şi rămâne aşa. **Regula generală, de aplicat la tot corpusul: ce e depăşit în model
   nu se copiază.** Corpusul spune cum se completează un formular, nu ce lege e în vigoare — aia se
   ia de aici, din sursa primară.
5. **Unitatea e kg** pe toate fişele, şi antetul o declară explicit („Unitatea de măsură: kg”).
   ⚠️ Cu o excepţie care e greşeala lor: foaia Timişoara `19 12 12` declară „tone" peste cifre clar
   în kg (stoc iniţial 48.755).

6. ✅ **Cele două coloane „Cant." din cap. 2 — răspuns, 02.09.2026.** „Tratare: Cant." e **numai ce
   a tratat firma însăşi**, şi contrastul dintre cele două firme din corpus o arată fără echivoc:

   | Firma | Stocare Cant. | Tratare Cant. | Modul |
   |---|---|---|---|
   | **Panemar** (brutărie, doar predă), pe toate cele 5 coduri | 53,000 | **0,000** | `-` |
   | **Hamburger** (balotează pe amplasament) | 25 | **25** | `TM` |

   Nu e „0 mereu" şi nici „cantitatea mereu": se citeşte din „Modul". Din 409 rânduri, 287 au
   Tratare = Stocare, 60 o au goală, iar cele 50 rămase au **Stocare 0 şi Tratare > 0** — niciun
   rând invers. Comportamentul aplicaţiei (ieşirile fără partener) e corect. → închide **V**.

7. ✅ **Titlul stă doar pe centralizator.** „Evidenta gestiunii deseurilor generate «an»" apare pe
   foaia de centralizare (`D12`/`A12`); varianta Bragadiru scrie în loc „CENTRALIZATOR". Pe cele 33
   de foi per cod de deşeu antetul începe direct cu „Agentul economic:" — **niciun titlu**, în
   niciun fişier. → materia întrebării **P**.

8. ✅ **Numărul de înregistrare îl dă autoritatea la depunere.** Două fişiere îl poartă sus-dreapta
   pe centralizator: Hamburger Cluj 2024 → `Nr inreg: 23/11.02.2025`, Panemar 2025 →
   `Nr inreg: 25/11.02.2025`. **Două firme diferite, numere apropiate, aceeaşi zi**, aceeaşi agenţie
   judeţeană, cu o lună înainte de termenul de 15 martie. E un registru de intrare al agenţiei, nu
   ceva ce generează depunătorul. → materia întrebării **I**.

9. **Ieşirea care depăşeşte generarea lunii se acoperă din stocul reportat, nu prin deducere.** În
   cele 7 luni din corpus unde se întâmplă, stocul absoarbe diferenţa: Timişoara 2022 `19 12 12`
   face 48.755 + 200.008 − 225.430 = **23.333**, exact formula noastră. Confirmă precedenţa din
   `V24` — întâi stocul, abia apoi generarea dedusă. Cazul fără stoc nu apare, deci **W** rămâne
   deschisă.

10. **Lunile fără operaţie se umplu în patru feluri diferite** — `-`, soft-hyphen (`U+00AD`), `x`,
    `X` — uneori în acelaşi fişier. Nu e o regulă, e obişnuinţa fiecărui completator; noi scriem
    liniuţă şi e în regulă.

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
> 1. Dosarul de control se dimensionează la **3 ani** (12 luni la transportatori). Închide D1. ✅ **Construit pe 24.08.2026** (Etapa 6): `years=1..3`, folder per an.
> 2. **15 martie e termen legal**, nu cutumă ANPM. Închide C3.
> 3. Evidența lunară se ține în kg (practica fișei Anexa 1), dar **raportarea de la art. 48 e în
>    tone**. Conversia trebuie să existe într-un singur loc în cod, nu presărată prin export.
> 4. 🔴 **Articolul descrie conţinutul, nu forma — şi n-are anexă cu facsimil.** Lit. a)–c) spun ce
>    date trebuie să conţină evidenţa cronologică, iar depunerea se face „în sistemul pus la
>    dispoziţie de APM". Spre deosebire de HG 856/2002, care reproduce modelul fişei în anexa nr. 1,
>    **aici nu există un formular de reprodus**. Deci formatul registrului art. 48 nu se poate citi
>    din act: e nevoie de un exemplar completat. Vezi întrebarea **AD** — nu avem niciunul.

**Alin. (2)** — buletinele de analiză. *Citat adăugat 02.09.2026 (audit, pct. 9).*

> Producătorii şi deţinătorii de deşeuri periculoase sunt obligaţi să deţină **buletinele de analiză
> care caracterizează deşeurile periculoase** şi să le transmită, la cerere, autorităţilor competente
> pentru protecţia mediului.

> **Consecință.** Ataşamentele generice de pe mişcare pot ţine fişierul, dar nimic nu leagă un
> buletin de un cod periculos şi nimic nu semnalează absenţa lui în dosarul de control. Pentru un
> client cu coduri periculoase, dosarul e incomplet legal fără să se vadă. Întrebarea **AL**.

### 2.1b ARTICOLUL 23 — cui poţi preda deşeul, şi cine răspunde de el

*Adăugat 02.09.2026, la audit. E temeiul punctelor 5 şi 8 din `audit-conformitate.md`.*

**Alin. (1):**

> Producătorul de deşeuri iniţial sau, după caz, orice deţinător de deşeuri are obligaţia de a
> efectua operaţiunile de tratare în conformitate cu prevederile art. 4 alin. (1) - (3) şi art. 21
> prin mijloace proprii sau prin intermediul unui **operator economic autorizat** care desfăşoară
> activităţi de tratare a deşeurilor sau unui operator public ori privat de colectare a deşeurilor
> [...]

**Alin. (4) şi (5)** — persoana desemnată:

> (4) Pentru îndeplinirea obligaţiilor legale privind gestionarea deşeurilor, titularul unei
> activităţi, pentru care autoritatea competentă pentru protecţia mediului a emis o autorizaţie de
> mediu/autorizaţie integrată de mediu, are obligaţia să **desemneze o persoană** din rândul
> angajaţilor proprii sau să **delege această obligaţie unei terţe persoane**.
>
> (5) Persoanele desemnate, prevăzute la alin. (4), trebuie să fie **instruite** în domeniul
> prevenirii generării de deşeuri şi al managementului deşeurilor, inclusiv în domeniul substanţelor
> periculoase, ca urmare a absolvirii unor programe de perfecţionare şi specializare recunoscute la
> nivel naţional [...]

**Art. 24 alin. (1)** — predarea nu descarcă de răspundere:

> Producătorul sau deţinătorul care transferă deşeuri către una dintre persoanele fizice autorizate
> ori persoanele juridice prevăzute la art. 23 alin. (1) în vederea efectuării unor operaţiuni de
> tratare preliminară operaţiunilor de valorificare sau de eliminare completă **nu este scutit, ca
> regulă generală, de responsabilitatea** pentru realizarea operaţiunilor de valorificare ori de
> eliminare completă.

> **Trei consecințe pe cod.**
> 1. Alin. (1) cere ca destinatarul să fie **autorizat**, iar Anexa 3 chiar tipărește numărul și
>    expirarea autorizației lui. Azi nimic nu compară expirarea cu **data mișcării** — `expiringSoon`
>    se calculează față de *azi* și e doar un badge în lista de Parteneri. Deci putem tipări un
>    formular care documentează o predare către un operator neautorizat la acea dată. Întrebarea
>    **AH**.
> 2. Alin. (4)–(5) sunt o rubrică de dosar pe care n-o ținem nicăieri. `contactName`/`contactRole`
>    sunt blocul de semnătură al declarației, nu persoana desemnată, și nu poartă certificatul de
>    instruire. Notă de produs: **consultantul de mediu e chiar „terța persoană" de la alin. (4)** —
>    rubrica se completează singură pentru portofoliul specialistei. Întrebarea **AK**.
> 3. Art. 24 alin. (1) explică de ce predarea nu închide subiectul: clientul rămâne răspunzător.
>    Argument în plus pentru avertismentul de la pct. 1, nu doar pentru un badge.

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

> ✅ **Numerotarea anexelor, reverificată pe 02.09.2026 la audit**, direct pe textul din Monitorul
> Oficial (extras cu PyMuPDF din PDF-ul citat în capul secțiunii, ca să nu depindem de un rezumat):
>
> | Anexă | Titlu verbatim | Pagina |
> |---|---|---|
> | ANEXA Nr. 1 | DEFINIREA unor termeni în sensul prezentei ordonanţe de urgenţă | 53 |
> | ANEXA Nr. 2 | EXEMPLE de instrumente economice şi alte măsuri [...] | 57 |
> | **ANEXA Nr. 3** | **OPERAŢIUNI DE VALORIFICARE** (R1–R13) | 58 |
> | ANEXA Nr. 4 | PROPRIETĂŢI ale deşeurilor care fac ca acestea să fie periculoase | 59 |
> | **ANEXA Nr. 7** | **OPERAŢIUNILE DE ELIMINARE** (D1–D15) | 69 |
> | ANEXA Nr. 8 | EXEMPLE DE MĂSURI DE PREVENIRE A GENERĂRII DEŞEURILOR | 70 |
>
> Confirmat și de art. 1 din anexa nr. 1: „Anexa nr. **7** stabileşte o listă a operaţiunilor de
> **eliminare**" (pct. 17) și „Anexa nr. **3** stabileşte o listă a operaţiunilor de
> **valorificare**" (pct. 37). Deci actul o spune de două ori, în două locuri.
>
> 🔴 **De aici a ieșit prima abatere confirmată a auditului.** `Anexa1FormGenerator` tipărea pe
> cap. 4 „conform **anexei nr. 2** din OUG 92/2021" — care e lista de instrumente economice.
> Presupunerea din javadoc, că OUG 92/2021 a păstrat numerotarea din Legea 211/2011 (unde eliminarea
> era anexa 2), e adevărată doar pentru valorificare, rămasă la 3. Vezi `audit-conformitate.md`
> pct. 1. **Nu e o întrebare pentru specialistă** — actul o spune de două ori.

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

**Cele trei anexe, reverificate pe 02.09.2026 la audit** — fiindcă aplicaţia trimite clientul la una
dintre ele într-un mesaj de eroare, deci numărul trebuie să fie corect:

| Anexă | Ce e | Îl generăm? |
|---|---|---|
| **nr. 1** | Formular pentru **aprobarea** transportului deşeurilor periculoase | ❌ nu |
| **nr. 2** | Formular de **expediţie/transport deşeuri periculoase** | ❌ nu |
| **nr. 3** | Formular de **încărcare-descărcare deşeuri nepericuloase** | ✅ da |

> ✅ Mesajul `ANEXA3_HAZARDOUS_NOT_ALLOWED` trimite corect la anexa 2 pentru un cod periculos.
> 🟠 **Dar nu punem nimic în loc.** Un generator obişnuit *are* coduri periculoase — un service auto
> are ulei uzat (`13 02 xx*`) şi filtre, o clinică are `18 01 03*`, un birou are tuburi fluorescente
> (`20 01 21*`). Pentru ei ţinem fişa corect şi nu putem tipări niciun document de transport. Şi e
> mai mult decât un formular: transportul periculos cere **aprobare prealabilă** (anexa 1), adică o
> procedură, nu o pagină. Vezi `audit-conformitate.md` pct. 6 şi întrebarea **AI**.

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

> ⚠️ **Anexa 1 de aici NU e Anexa 1 din HG 856/2002.** Două documente, același nume, iar confuzia
> dintre ele e cea mai ieftină cale de a strica modulul de generatori:
>
> | | Fișa de gestiune | Declarația de ambalaje |
> |---|---|---|
> | Act | HG 856/2002, anexa 1 (§1 de mai sus) | **Ordinul 794/2012, anexa 1** (aici) |
> | Cine o ține | orice generator, per cod de deșeu | cine pune pe piață marfă ambalată |
> | Formă | 4 capitole × 12 luni, o pagină per cod | tabele pe materiale, în kg |
> | În aplicație | G5, livrat | modulul de ambalaje, nescris |
>
> Titlul de mai sus se citește **până la capăt**: nu „producători şi importatori *de ambalaje*", ci
> „*de ambalaje de desfacere, de produse ambalate*, supraambalatori de produse ambalate". Deci nu
> fabricanții de ambalaje — **oricine pune pe piață produse ambalate**, punând astfel și ambalajul.
> Categorie largă. Specialista a formulat-o pe 24.08 ca „Anexa 1 e strict pentru generatorii de
> deșeuri de ambalaj, producători/importatorii" — despre **acest** document vorbește (R19).
>
> Aceeași omonimie la „Anexa 3": HG 1061/2008 (dovada predării, generată de aplicație) vs. anexa 3
> de mai sus (raportarea anuală a colectorilor de ambalaje).

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

### 5.1 Articolele, citite verbatim (25.08.2026)

Sursă secundară, cerută de utilizator: [envirocons.ro/ordinul-794-din-2012-varianta-actualizata](https://envirocons.ro/ordinul-794-din-2012-varianta-actualizata/),
accesată 25.08.2026. Confirmă ce știam și adaugă **trei lucruri pe care nu le aveam**.

> **Art. 1** — „Operatorii economici, producători și importatori de ambalaje de desfacere [...] sunt
> obligați să raporteze **agenției județene/regionale pentru protecția mediului** datele cuprinse în
> anexa nr. 1." Raportarea se transmite agenției din raza **sediului social**.
>
> **Art. 3** — „Operatorii economici care își îndeplinesc în mod individual obiectivele [...] sunt
> obligați să comunice aceasta **Administrației Fondului pentru Mediu**, până cel târziu la data de
> **25 ianuarie** a fiecărui an."
>
> **Art. 6** — „Datele de raportare se transmit **în format electronic „.xls"** [...] până cel
> târziu la data de **25 februarie** a fiecărui an."
>
> **Art. 7** — ANPM pune formatul „.xls" la dispoziție pe pagina de internet.
>
> **Art. 8 alin. (1)** — „Cantitățile de ambalaje, respectiv de deșeuri de ambalaje se raportează
> **în kilograme**."

**1. Formatul cerut e `.xls`, scris în act.** Art. 6 nu spune „un document", spune formatul. Asta
transformă exportul XLSX din comoditate în **cerință**: cele două foi, cu structura pe care ANPM o
publică. De aici vine `PackagingDeclarationXlsxGenerator`, iar PDF-ul rămâne pentru dosarul de
control, nu pentru depunere.

**2. Întrebarea Y se lămurește pe jumătate: sunt două depuneri diferite, nu una.**

| Ce | Unde | Când | Temei |
|---|---|---|---|
| **Anexa 1 Ambalaje** (cele două tabele) | agenția **județeană/regională** de mediu, din raza sediului social | **25 februarie** | art. 1 + art. 6 |
| Notificarea „îmi îndeplinesc individual obiectivele" | **AFM** | **25 ianuarie** | art. 3 |

Deci și specialista („Anexa 1 ambalaje este pentru fondul de mediu, declarație AFM"), și actul
(„agenţia judeţeană") spun adevărul, despre **documente diferite**.

✅ **Închis pe 02.09.2026, pe textul de pe Portalul Legislativ.** Întrebarea era dacă în practică se
depun împreună sau separat — dar **nu pot fi depuse împreună**: art. 3 trimite notificarea la **AFM
până pe 25 ianuarie**, iar art. 1 + art. 6 trimit raportarea la **agenţia judeţeană de mediu până pe
25 februarie**. Alt destinatar, altă lună. Practica nu poate contrazice asta, deci nu mai e o
întrebare pentru specialistă. Cadenţa AFM
anuală pe 25 ianuarie era deja în cod din `V21` (`AfmContribution.PACKAGING`) — și acum se vede că
data aia nu e a declarației de ambalaje, ci a notificării de la art. 3.

**3. A treia confirmare a kilogramelor, de data asta dintr-un articol, nu dintr-un antet de tabel.**
Până acum ne sprijineam pe `[kilograme]` tipărit deasupra celor cinci anexe. Art. 8 alin. (1) o
spune ca obligație. Fișierul primit în tone rămâne, definitiv, șablon modificat local.

### 5.2 Textul oficial, integral (25.08.2026)

Sursă: forma consolidată la 04.02.2021, PDF Lege5 găzduit de FEPRA
([fepra.ro/files/legal/ORDIN_794_-_06-02-2012.pdf](https://fepra.ro/files/legal/ORDIN_794_-_06-02-2012.pdf)),
citit integral pe 25.08.2026. Înlocuieşte rezumatul din §5.1, care era corect dar incomplet.
✅ **Reverificat pe 02.09.2026 direct pe Portalul Legislativ**
([legislatie.just.ro/Public/DetaliiDocumentAfis/135672](https://legislatie.just.ro/Public/DetaliiDocumentAfis/135672)),
fiindcă FEPRA e gazdă secundară, nu sursă primară. Art. 1, 3, 4 şi 6 şi notele anexelor sunt
**identice** cu ce aveam. Actul e în vigoare.
**Şase lucruri pe care nu le aveam, şi patru dintre ele ating cod.**

#### 1. ⚠️ Anexa 1 nu e a tuturor — e a celor care îşi îndeplinesc **individual** obiectivele

> **Art. 1 alin. (1)** — „Operatorii economici, producători şi importatori de ambalaje de desfacere,
> producători/importatori de produse ambalate, precum şi cei care supraambalează produse ambalate,
> **care îşi îndeplinesc în mod individual obiectivele** prevăzute la art. 16 alin. (4) din HG nr.
> 621/2005 [...], sunt obligaţi să raporteze agenţiei judeţene/regionale pentru protecţia mediului
> datele cuprinse în anexa nr. 1."
>
> **Alin. (2)** — cine a transferat obligaţiile **doar parţial** raportează pentru cantităţile
> netransferate.

Deci o firmă care şi-a transferat integral obligaţiile către un OIREP **nu depune anexa 1** — OIREP-ul
raportează în locul ei, prin anexele 2A/2B. Nu ştiam asta, şi e chiar populaţia despre care vorbea
specialista când zicea „Anexa 1 e strict pentru...". **Nu restrânge tabul azi** (nu întrebăm pe
nimeni dacă a transferat obligaţiile), dar e o întrebare de pus.

#### 2. Formatul: `.xls` **protejat**, şi **pe hârtie**

> **Art. 6** — „Datele de raportare se transmit în format electronic „.xls" **protejat împotriva
> modificării datelor** şi **pe suport hârtie**, până cel târziu la data de 25 februarie a fiecărui
> an pentru anul anterior celui pentru care se realizează raportarea."

Două consecinţe directe: foile generate sunt **protejate** (parolă goală, ca să poată fi ridicată —
vezi `PackagingDeclarationXlsxGenerator`), iar **PDF-ul nu e un moft**: e exemplarul pe hârtie.
Art. 7: ANPM publică formatul „xls" pe pagina de internet.

#### 3. Regulile de completare — art. 8 alin. (1), pe care ne sprijinim acum în cod

> a) „Cantităţile de ambalaje, respectiv de deşeuri de ambalaje se raportează **în kilograme**."
> b) „Ambalajele din **materiale compozite** se raportează în funcţie de **materialul preponderent**."
> c) „**Achiziţiile intracomunitare** de ambalaje şi produse ambalate **se asimilează importurilor**."
> d) „În coloana «material», rubrica **«altele» va cuprinde numai alte materiale decât cele
> nominalizate în coloana 0**."
> e) „Rubrica «lemn» va cuprinde atât lemnul, cât şi pluta."
>
> **Alin. (2)** — „Datele referitoare la produsele/ambalajele **exportate** şi cele aflate **în
> tranzit** pe teritoriul României **nu se includ** în datele de raportare."

**Litera d) e temeiul legal al indicaţiei Andreei** că „Altele" n-ar trebui folosit. Nu e o preferinţă
de practică: rubrica e rezervată materialelor **nenominalizate**, iar ambalajul metalic (`15 01 04`)
şi cel de plastic (`15 01 02`) sunt nominalizate. Deci fallback-ul vechi, care le arunca acolo, era
contra actului. Litera b) spune şi ce se face cu compozitele (`15 01 05`): merg pe materialul
preponderent — pe care numai clientul îl ştie, deci tot pe mişcare se răspunde.

#### 4. Anexa 3 are **două tabele**, şi reciclatorul îl completează pe al doilea

> **Art. 4 alin. (1)** — colectorii, reciclatorii, valorificatorii, **comercianţii** de deşeuri de
> ambalaje şi operatorii de salubritate autorizaţi pentru colectare „sunt obligaţi să raporteze
> datele prevăzute în anexa nr. 3, **tabelul 1 sau, după caz, tabelul 2**".
> **Alin. (3)** — colectorii/reciclatorii/valorificatorii raportează la agenţia din raza în care îşi
> desfăşoară activitatea; **comercianţii la ANPM**.
> **Alin. (4)** — „Raportarea se realizează **pentru fiecare punct de lucru în parte**."

| | Tabelul 1 — **Colectori/Comercianţi** | Tabelul 2 — **Reciclatori/Valorificatori** |
|---|---|---|
| Cantitatea | colectată: Total \| din care periculoase | preluată: Total \| din care periculoase |
| Provenienţa | da | da |
| Ieşirea | comercializate/trimise la reciclare/valorificare/exportate: cantitate + **operatorul economic** (denumire + CUI; la export şi ţara) | **cantitatea reciclată** \| **cantitatea valorificată** (numai prin alte metode decât reciclarea) \| **metoda** |

⚠️ **Rândurile de material sunt aceleaşi ca la anexa 1**: Sticlă · Pet · Alte plastice · *Total
plastic* · Hârtie carton · **Aluminiu · Oţel** · *Total metal* · Lemn · Altele · *TOTAL*. Fişierul
`.ods` primit, cu un singur rând „metal/aluminiu" şi în **tone**, e — ca şi cel de anexa 1 — **şablon
modificat local**. Utilizatorul a spus exact asta pe 25.08 („aşa arată corect, ca în raportare
ambalaje 2021 anexa 1 HRR"), iar actul îi dă dreptate.

#### 5. „Provenienţa" are **patru** valori, nu trei

> **Nota 2** a ambelor tabele — „Se menţionează, după caz, **«populaţie»**, **«generator persoană
> juridică»**, **«colector»**, **«comerciant»**, în funcţie de persoanele juridice sau fizice **de la
> care provin** deşeurile de ambalaje preluate."

Fişierul `.ods` avea doar trei (îi lipsea „comerciant"). Şi formularea închide jumătate din
întrebarea **AA**: provenienţa e o proprietate a **sursei**, nu a transportului — deci stă natural pe
partener, cu excepţia „populaţiei", care n-are partener.

#### 6. Calendarul complet

| Cine / ce | Unde | Termen | Temei |
|---|---|---|---|
| Anexa 1 — producători/importatori **care îşi îndeplinesc individual obiectivele** | APM judeţeană/regională, din raza **sediului social** | **25 februarie** | art. 1 + art. 6 |
| Anexele 2A/2B — OIREP-uri | **ANPM** | 25 februarie | art. 2 |
| Notificarea „îmi îndeplinesc individual obiectivele" | **AFM** | **25 ianuarie** | art. 3 |
| Anexa 3 — colectori/reciclatori/valorificatori | APM din raza **punctului de lucru**, per punct de lucru | 25 februarie | art. 4 |
| Anexa 3 — **comercianţi** | **ANPM** | 25 februarie | art. 4 alin. (3) |
| Anexa 4 — autorităţi locale | APM | 25 februarie | art. 5 |
| AFM → ANPM: lista contribuabililor pe ambalaje | — | 31 ianuarie | art. 9 |
| APM → ANPM: datele centralizate | — | 1 mai | art. 10 |

**Separat de tot ce e mai sus**, contribuţia pe ambalaje la Fondul pentru mediu: **2 lei/kg**,
datorată **numai** dacă nu ţi-ai atins obiectivele de valorificare, declarată şi plătită **anual,
până pe 25 ianuarie** — OUG 196/2005 art. 9 alin. (1) lit. d) + **art. 11 alin. (2)**: „Sumele
prevăzute la art. 9 alin. (1) **lit. d)**, i), j), p), v), w) şi x)" sunt cele anuale. Verificat pe
Portalul Legislativ pe 25.08.2026, fiindcă un rezumat de pe internet o dădea drept lunară — nu e.
`AfmContribution.PACKAGING` din `V21` rămâne corectă.

---

### 5.3 Ce arată formularele completate (corpus, citit rubrică cu rubrică pe 02.09.2026)

Trei fişiere: declaraţia HRR 2021 completată, şablonul gol de lângă ea, şi şablonul Anexei 3
Ambalaje (`.ods`). Notele tipărite pe formular sunt **text de act**, deci se citează aici.

**1. Tabelul 1 şi Tabelul 2 raportează lucruri diferite — şi la HRR nu se suprapun deloc.**

> **Nota 1)** (col. 1) — „Se raportează **numai ambalajele de desfacere destinate pieţei
> naţionale**, definite prin HG nr. 621/2005 [...]"
>
> **Nota 4)** (col. 2) — „Se raportează **numai ambalajele folosite la ambalarea produselor
> destinate pieţei naţionale** şi se includ şi ambalajele utilizate pentru ambalarea ambalajelor de
> desfacere."
>
> **Antetul Tabelului 2** — „Deşeuri de ambalaje **încredinţate unui operator economic autorizat**",
> cu **nota 1)**: „Se completează **câte o rubrică distinctă pentru fiecare dintre operatorii** care
> au preluat deşeurile de ambalaje din materialul respectiv."

Proba, pe declaraţia lor din 2021: Tabelul 1 raportează **numai Oţel, 5.192 kg** — o cifră care nu
apare în nicio fişă de gestiune a lor. Invers, `15 01 02` din fişe (5,5 kg/an) **nu apare** în
declaraţie. Iar Tabelul 2 e **complet gol**. Deci:

- **Tabelul 1** e despre marfă pusă pe piaţă, nu despre deşeu. Nu e derivabil din mişcări — ceea ce
  justifică bifa din `V27` (întrebarea **AC**) şi suprascrierea pe material din `V26`.
- **Tabelul 2** e chiar despre deşeul predat, cu un rând per operator — **acela** se însumează
  corect din mişcări, şi aşa îl construieşte `PackagingDeclarationBuilder`. ✅ Verificat în cod pe
  02.09: rândurile ies per (material, operator, operaţie), cu denumire, adresa punctului de lucru şi
  CUI, exact cum cer rubricile `D7`/`E7` ale formularului.

⚠️ Nota Tabelului 2 mai cere ceva ce corpusul face necesar: „În cazul în care operaţiunea de
reciclare/valorificare se face **prin export sau transfer intracomunitar**, se va specifica alături
de denumirea operatorului economic, **adresa punctului de lucru şi ţara de destinaţie**." Corpusul e
plin de operatori străini (*Hamburger Hungaria*, *Hamburger Recycling Group GMBH* — 105 rânduri).
Adresa partenerului e text liber, deci ţara **încape** acolo; nu cere schemă nouă, doar ca cineva
să o scrie.

**2. „Provenienţa" — nota actului, verbatim (Portalul Legislativ, 02.09.2026):**

> **Anexa nr. 3, nota 2)** — „Se menţionează, după caz, «populaţie», «generator persoană juridică»,
> «colector», «comerciant»."

✅ **Patru valori, exact cele presupuse** — deci şablonul primit, care are doar trei sub-rânduri
(`populatie`, `colectori`, `generatori persoane juridice`) şi nu-l are pe „comerciant", e într-adevăr
modificat local. **Închide jumătatea de drept a întrebării AA.** Cealaltă jumătate — dacă ţinem
provenienţa pe partener sau pe mişcare — **nu e o întrebare pentru specialistă, e decizia noastră de
model**: actul spune ce valori se scriu în rubrică, nu unde le stocăm noi.

**2b. Forma rubricii în şablon.** În şablonul primit,
fiecare material se sparge în **trei sub-rânduri fixe** — `populatie`, `colectori`,
`generatori persoane juridice` — plus un rând „total <material>". Nu apare `comerciant`.
⚠️ Şablonul e însă cel **modificat local**: scrie „tone" (actul spune kilograme, art. 8 alin. (1)) şi
pune „metal /aluminiu" pe un rând, deşi actul cere Aluminiu şi Oţel separat. Deci pentru **numărul**
valorilor rămâne actul autoritatea, dar pentru **forma** rubricii — trei rânduri sub fiecare
material, nu o coloană — şablonul arată cum se completează. Materia întrebării **AA**.

### 5.4 Ce a găsit auditul de conformitate (02.09.2026)

**1. Formatul cerut e `.xls`, iar noi producem `.xlsx`.** Art. 6 numeşte formatul pe nume, şi ăsta a
fost chiar argumentul pentru care s-a construit generatorul de foaie de calcul. Codul foloseşte însă
`XSSFWorkbook`, care produce **OOXML** — formatul `.xlsx` introdus în 2007 — servit cu extensia
`xlsx` şi content type-ul OOXML. `.xls` (BIFF8) se produce cu `HSSFWorkbook` şi se serveşte cu
`application/vnd.ms-excel`; protecţia foii cu parolă goală merge identic pe HSSF, deci cerinţa din
aceeaşi frază nu se pierde.
Art. 7 spune că **ANPM publică formatul pe pagina proprie**, deci depunerea se face pe şablonul lor
— iar un portal care validează extensia sau semnătura de fişier respinge un `.xlsx`.
🟡 **Cât de strict e în practică e singurul lucru neclar**, şi numai ea ştie: întrebarea **AG**.
Reparaţia se face oricum, fiindcă actul e explicit.

**2. Două note ale Tabelului 1 citează acte abrogate.** Reproduse verbatim din model:

| Nota | Trimite la | Stare |
|---|---|---|
| 1) | HG 621/2005 privind gestionarea ambalajelor | **abrogată la 1 noiembrie 2015**, de Legea 249/2015 |
| 3) | HG 937/2010 (clasificarea/etichetarea preparatelor periculoase) | **abrogată prin HG 539/2016** |

Inconsecvenţa e în acelaşi fişier: nota 2) a Tabelului 2 a fost **actualizată deliberat** de la
Legea 211/2011 la OUG 92/2021, cu un comentariu care explică de ce. Acelaşi lucru s-a făcut şi pe
cap. 3/4 ale fişei Anexa 1. Deci politica „actualizăm referinţele abrogate" e stabilită de două ori
şi aplicată în trei locuri din cinci.
🟡 **De decis o dată, pentru tot** — şi e o întrebare de practică, nu de drept: agenţia compară
notele cu şablonul ei? Întrebarea **AJ**. Ordinul 794/2012 n-a fost actualizat după 2015, deci
notele lui *sunt* textul în vigoare al ordinului, chiar dacă trimit la acte moarte; e exact situaţia
de la antetul cap. 3/4, unde am ales să actualizăm.

✅ **Decis şi aplicat pe 04.09.2026: actualizăm, în toate cele cinci locuri.** Aceeaşi politică pe
care o aplicam deja în trei din cinci. Nota 1 → **Legea nr. 249/2015**, nota 3 → **Regulamentul (CE)
nr. 1272/2008** (CLP).

⚠️ **Şi aici e o corectură a auditului însuşi.** Auditul propusese pentru nota 3 „Regulamentul CLP /
**HG 539/2016**". A doua variantă e greşită, şi se vede numai citind actul. Titlul lui, verificat pe
Portalul Legislativ pe 04.09.2026
([legislatie.just.ro/Public/DetaliiDocument/180604](https://legislatie.just.ro/Public/DetaliiDocument/180604)),
verbatim:

> HOTĂRÂRE nr. 539 din 27 iulie 2016 **pentru abrogarea** Hotărârii Guvernului nr. 1.408/2008 privind
> clasificarea, ambalarea şi etichetarea substanţelor periculoase **şi a Hotărârii Guvernului
> nr. 937/2010** privind clasificarea, ambalarea şi etichetarea la introducerea pe piaţă a
> preparatelor periculoase

Deci e un **act pur de abrogare, fără conţinut propriu**. O notă care ar trimite acolo ar duce
cititorul la o pagină care nu spune nimic despre cum se etichetează un ambalaj. Regula de fond în
vigoare e Regulamentul (CE) nr. 1272/2008, direct aplicabil — şi chiar preambulul lui HG 539/2016 îl
numeşte ca motiv pentru care actele naţionale au devenit redundante.

**Lecţia, care depăşeşte nota asta:** „actul X a fost abrogat de Y" **nu** înseamnă „scrie Y în loc
de X". Actul abrogator poate fi doar un certificat de deces. Succesorul se caută în conţinut, nu în
istoricul abrogărilor. Testul care ţine regula pe loc:
`PackagingDeclarationIT.theFootnotesCiteTheActsInForce` — respinge explicit şi `539/2016`.

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

> **Ce a spus specialista pe 24.08 (R25), și ce rămâne de lămurit.** Întrebată cine are obligație
> AFM, a răspuns: *„doar generatorii de deșeuri de ambalaj — producători/importatorii"*. Adică
> **cine pune pe piață marfă ambalată**, nu fabricanții de ambalaje.
>
> Asta se potrivește peste **lit. d)**, contribuția pe ambalaje, care e **anuală** (alin. (2)) — și
> confirmă că termenul lunar generat azi pentru un generator obișnuit e greșit. Ce **nu** spune
> răspunsul e dacă acoperă și celelalte două cadențe, care aparțin unor tipuri de client fără ecrane
> încă: cei **2%** ai colectorului (lit. a, lunar, §10.1) și **economia circulară** a gropilor
> (lit. c, trimestrial, §10.2). Formularea ei vine din contextul modulului de generatori, unde
> singura relevantă e cea pe ambalaje.
>
> Deci documentarea de aici **nu se modifică** pe baza răspunsului: rămâne cu trei cadențe, iar
> răspunsul restrânge doar cazul generatorului. Reconfirmarea e **întrebarea L** din
> `intrebari-specialist.md`; de ea atârnă forma Etapei 7.

---

## 11. Legea 249/2015 — cine e „producător", „importator" și „comerciant" de ambalaje

Sursă: [legislatie.just.ro/Public/DetaliiDocumentAfis/179664](https://legislatie.just.ro/Public/DetaliiDocumentAfis/179664)
(text consolidat), accesat **24.08.2026**.

Actul-cadru pentru ambalaje. Contează aici pentru un singur lucru: **definește trioul** pe care
specialista îl numește „ce tip de generator" — producător / importator / comercial.

**Anexa nr. 1, „Semnificaţia termenilor specifici":**

> operatori economici - referitor la ambalaje, înseamnă furnizorii de materiale de ambalare,
> producătorii de ambalaje şi produse ambalate, **importatorii, comercianţii, distribuitorii**,
> autorităţile publice şi organizaţiile neguvernamentale

**Art. 17 alin. (1)** — cine raportează:

> Operatorii economici care îşi îndeplinesc responsabilităţile potrivit prevederilor art. 16
> alin. (2) lit. a) [...] au obligaţia să furnizeze anual Ministerului Mediului, Apelor şi Pădurilor
> informaţii privind gestionarea ambalajelor şi a deşeurilor de ambalaje.

> **Consecința pe model (24.08.2026).** Trioul e o proprietate a firmei, nu a deșeului, și decide
> **un singur lucru**: dacă firma depune declarația de ambalaje (§5 de mai sus — Ordinul 794/2012,
> anexa 1, termen 25 februarie) și dacă datorează contribuția pe ambalaje la AFM (§10, art. 9(1)
> lit. d). Un **comerciant** vinde marfă ambalată de altcineva: nu el a introdus ambalajul pe piață.
>
> Ce **nu** decide: fișa de evidență a gestiunii deșeurilor (§1 — HG 856/2002, anexa 1). Aceea se
> ține de oricine generează deșeu, art. 1 alin. (1), oricare ar fi rolul lui comercial. Un
> comerciant cu un tomberon de carton în curte o ține exact ca oricine altcineva.
>
> În cod: `MarketRole` (`PRODUCER` / `IMPORTER` / `TRADER`), set pe `Company` și pe
> `AccountRequest`, migrarea `V13`. Set gol = întrebarea n-a primit răspuns, deci nu se
> concluzionează nimic — aceeași regulă ca la restul profilului.

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
