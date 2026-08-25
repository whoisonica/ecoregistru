# Documentul de reguli — legislație deșeuri (EcoRegistru)

> **Scop:** sursa de adevăr pentru CE cere legea, ca să nu codăm rapoarte pe presupuneri.
> **Regula de aur (din planul de proiect):** un raport greșit = client amendat = business mort.
> Nimic din secțiunea „format oficial" nu se codează până nu e bifat de tata + un expert de mediu.
>
> **Legendă încredere:** ✅ confirmat cu sursă · 🟡 probabil corect, de verificat cu expert · 🔴 necunoscut / de cercetat
>
> **22.08.2026 — citatele verbatim s-au mutat în [`surse-oficiale.md`](surse-oficiale.md).** Acolo e
> textul de lege, cu link la sursa primară și data accesării. Documentul de față rămâne harta și
> analiza de gap; când cele două se contrazic, `surse-oficiale.md` are dreptate.
>
> Ultima actualizare: 2026-08-23 (gap analysis reîmprospătat după Etapa 2a — seam-ul de registru).
> Verificarea legislativă integrală pe surse primare (Portal Legislativ, EUR-Lex, sgglegis.gov.ro)
> e din 2026-08-22. Cercetarea anterioară, din 2026-07-11, era făcută pe surse secundare — o parte
> din afirmațiile ei au fost corectate mai jos.

---

## 1. Harta legislativă (pilonii)

### A. OUG 92/2021 privind regimul deșeurilor — LEGEA-CADRU ✅
- Actul principal care guvernează regimul deșeurilor în România (transpune directivele UE). Consolidează obligațiile și **regimul de sancțiuni**.
- Înlocuiește ca lege-cadru vechea **Lege 211/2011** (211/2011 e abrogată/înlocuită de OUG 92/2021 — 🟡 de confirmat exact ce articole mai sunt referite).
- **Amendă pentru lipsa evidenței: 20.000–40.000 lei** ✅ (ATENȚIE: planul de proiect scria „10.000–40.000" — **corectăm la 20.000–40.000**).
- Definește: ierarhia deșeurilor, obligațiile generatorilor/colectorilor/transportatorilor, autorizarea, codurile de operațiuni R (valorificare) și D (eliminare).

### B. HG 856/2002 privind evidența gestiunii deșeurilor — EVIDENȚA ✅
- **Încă în vigoare**, corelată direct cu OUG 92/2021.
- Obligă TOȚI generatorii/gestionarii de deșeuri să țină **evidența gestiunii deșeurilor** după modelul din **Anexa 1**, **pentru fiecare tip de deșeu**, cu înregistrare **lunară**.
- **Anexa 2** = lista deșeurilor (coduri de 6 cifre, cele cu `*` = periculoase). ⚠️ **CORECTAT 22.08.2026:** Anexa 2 e versiunea 2002/2007, **pre-2014** — îi lipsesc coduri introduse prin Decizia 2014/955/UE. Nomenclatorul `waste_codes` se încarcă din **Decizia 2014/955/UE (842 coduri)**, nu din Anexa 2. Motivul, reconcilierea celor două liste și amprenta de validare: `surse-oficiale.md` §3.
- **Art. 2 alin. (1) — restrictiv, nu permisiv** ✅: operatorii autorizați pentru colectare/transport/depozitare temporară/valorificare/eliminare țin Anexa 1 **numai pentru deșeurile generate în activitatea proprie**. Marfa preluată de la terți NU intră în Anexa 1; art. 2(2) o trimite la o raportare separată. **Două fluxuri, două evidențe** — vezi §3, rândul `WasteMovement`.
- Evidența se ține **per punct de lucru**. Termenul de păstrare **nu** e în HG 856 (art. 3(3) privește autoritățile, nu firma) — e la **OUG 92/2021 art. 48 alin. (5): cel puțin 3 ani, cu excepția transportatorilor — 12 luni** ✅.

### C. Raportarea SIM (Sistemul Integrat de Mediu) → ANPM — ANUAL ✅
- Platformă ANPM. Toți cei care generează/gestionează deșeuri raportează **anual, în format centralizat**, datele din evidența lunară.
- **Termen: 15 martie** pentru anul anterior ✅. ⚠️ **CORECTAT 22.08.2026:** nu e cutumă ANPM, ci **termen legal** — OUG 92/2021 **art. 48 alin. (1)** îl scrie explicit („electronic în sistemul pus la dispoziţie de APM până la 15 martie anul următor raportării"). Faptul că sesiunea s-a deschis târziu în practică nu schimbă termenul din lege; alertele se calibrează pe 15 martie.
- ⚠️ **Unitatea diferă de evidența lunară:** art. 48 alin. (1) lit. a) și c) cer cantitatea **în tone**. Evidența lunară (fișa Anexa 1) se ține în **kg**. Conversia trebuie să existe într-un singur loc în cod.
- **Chestionare diferite pe tip de operator** ✅:
  - `PRODDES` — generatori de deșeuri (majoritatea clienților noștri „generator")
  - `COL/TRAT` — operatori de colectare/valorificare
  - `TRAT` — instalații de tratare
  - `MUN` — operatori de deșeuri municipale
  - `NĂMOL` — stații de epurare
- Autentificare separată pe `raportare.anpm.ro`. **Transmiterea automată e ÎN AFARA Fazei 1** — noi pregătim datele, clientul le încarcă.

### D. Declarația AFM (Fondul pentru Mediu) — LUNARĂ ✅
- „Declarație privind obligațiile la Fondul pentru mediu", **electronic exclusiv** prin AFM-online (din iulie 2022). ✅
- ⚠️ **CORECTAT 22.08.2026 — sunt TREI cadențe, nu una.** OUG 196/2005 art. 11: **lunar** până pe 25 a lunii următoare pentru contribuțiile de la art. 9(1) lit. a), b), e), f), s); **trimestrial** până pe 25 a lunii următoare trimestrului pentru lit. c) (economia circulară — depozitele); **anual** până pe **25 ianuarie** pentru lit. d), i), j), p), v), w), x) (ambalaje, anvelope, UAT). Text verbatim: `surse-oficiale.md` §10.3. Codul de azi generează un singur termen lunar pentru orice firmă cu `afmObligation` — **prea grosier**.
- 🟢 **Un colector datorează AFM lunar prin definiție:** art. 9(1) lit. a) — contribuția de **2% din veniturile din vânzarea deșeurilor**, **reținută la sursă de operatorul care colectează/valorifică**. Nu depinde de ambalaje. Vezi `surse-oficiale.md` §10.1.
- **IMPORTANT — nuanță strategică:** declarația AFM NU e despre evidența deșeurilor în sine, ci despre **contribuții la fondul de mediu**: ambalaje (răspundere extinsă a producătorului/EPR), anvelope, uleiuri, baterii/acumulatori, EEE, substanțe periculoase, taxa pentru deșeuri încredințate spre eliminare la groapă, emisii etc.
- **Deci NU orice client are obligație AFM lunară.** Depinde de activitate (dacă introduce pe piață ambalaje/produse, dacă duce la groapă etc.). Instrucțiuni oficiale de completare: OMM/AFM din 11.12.2023 (🟡 de citit integral cu expertul).
- ✅ **CONFIRMAT 24.08.2026 de specialistă:** „obligația AFM, doar generatorii de deșeuri de ambalaj — producători/importatorii". Adică **cine pune pe piață marfă ambalată**, nu fabricanții de ambalaje. Un generator obișnuit nu primește niciun termen AFM. 🟠 Rămâne de confirmat dacă răspunsul acoperă și celelalte două contribuții (2% la sursă, economie circulară) sau doar pe cea de ambalaje — întrebarea L. Și 🟠 calitatea asta **nu se întreabă nicăieri** azi în aplicație — întrebarea M.
- Amendă/penalități AFM: până la 250.000 lei + penalități zilnice la plată. 🟡

### E. Pe radar, ÎN AFARA Fazei 1 (de notat, nu de construit acum)
- **RO e-Transport** — notificarea transporturilor (inclusiv deșeuri) în SAF-T/e-Transport. 🟡
- **SGR (Sistemul Garanție-Returnare)** — relevant doar dacă clientul pune pe piață băuturi ambalate. 🟡
- **Legea 249/2015** (ambalaje) — dacă avem clienți cu obligații de ambalaje.

---

## 2. Formatul evidenței — Anexa 1 HG 856/2002 ✅ CONFIRMAT 22.08.2026

> **Structura de mai jos e confirmată verbatim** pe textul oficial de pe Portalul Legislativ.
> Coloanele exacte, notele cu cele cinci nomenclatoare închise ale Cap. 2 și câmpurile de
> identificare sunt în [`surse-oficiale.md` §1.2](surse-oficiale.md). Ce urmează e rezumatul.
>
> **Trei lucruri care contrazic ce scria aici înainte:**
> 1. **Cap. 1 NU are coloană de „predare".** Coloanele sunt exact `Generate | valorificată |
>    eliminată final | rămasă în stoc`. Predarea la un colector se raportează ca valorificare sau
>    eliminare, cu operatorul în Cap. 3 / Cap. 4. Deci `stoc = stoc_anterior + generat − valorificat
>    − eliminat` — fără termen separat pentru predare. **Coloana o dă codul R/D**, nu un câmp
>    separat: Cap. 3 și Cap. 4 cer și „Operaţia de valorificare"/„de eliminare", și agentul economic
>    care o efectuează, deci orice ieșire poartă un cod (vezi §3 și `surse-oficiale.md` §1.2).
> 2. **„Starea fizică" și „Unitatea de măsură" sunt câmpuri libere** în act, fără listă închisă.
> 3. **Referința R/D din facsimil e abrogată** (Legea 426/2001). Referința corectă în export:
>    **OUG 92/2021, anexa nr. 3 (valorificare) și anexa nr. 7 (eliminare)**.

Evidența e **per (agent economic / punct de lucru, tip de deșeu, an)**, cu **luni pe rânduri**. Câmpuri de identificare pe fișă:
- Agent economic (+ punct de lucru), Anul
- **Cod deșeu** (Anexa 2 / LED)
- **Starea fizică** a deșeului (solid / lichid / nămol / etc.)
- **Unitatea de măsură** (de regulă tone/kg)

Structura pe **4 capitole**, fiecare cu tabel pe 12 luni + total ✅:
1. **Cap. 1 — Generarea deșeurilor**: `Generate | din care: valorificată | eliminată final | rămasă în stoc`.
2. **Cap. 2 — Stocarea provizorie, tratarea și transportul**: `Secția | Stocare (Cantitatea, Tipul) | Tratare (Cantitatea, Modul, Scopul) | Transport (Mijlocul, Destinația)`. Ultimele cinci sunt **nomenclatoare închise**, enumerate în notele 1–5 ale anexei. Nu conține numele transportatorului — doar mijlocul și destinația, codificate.
3. **Cap. 3 — Valorificarea deșeurilor**: cantitate valorificată; **codul operației R**; **agentul economic care efectuează operația**.
4. **Cap. 4 — Eliminarea deșeurilor**: cantitate eliminată; **codul operației D**; **agentul economic care efectuează operația**.

Codurile **R1–R13** și **D1–D15** sunt cele din **OUG 92/2021, anexa nr. 3 și anexa nr. 7** ✅ — text verbatim în `surse-oficiale.md` §2.2–2.3. `WasteOperationCode` (13 R + 15 D) e corect și complet.

⚠️ **D5 vs. D1 pentru menajer rămâne judecată de încadrare, nu fapt.** Textul lui D5 („compartimente separate etanşe [...] acoperite şi izolate") descrie un depozit conform, dar exemplul dat la D1 e literalmente „depozite de deşeuri". Nu se propune ca valoare implicită în formular fără confirmare — vezi `raspunsuri-specialist.md` R1.

---

## 3. Ce avem ÎN aplicație vs. ce cere legea (gap analysis)

| Entitate actuală | Acoperă | LIPSEȘTE / de adăugat | Prioritate |
|---|---|---|---|
| `WasteCode` (cod, nume, periculos) | nomenclatorul ✅ + **lista 2014/955/UE completă (842 coduri)**, încărcată 22.08.2026 (Etapa 1) ✅ | — | ✅ închis |
| `WasteMovement.operation` + `register` + `operationCode` | ✅ **Etapa 2a (23.08.2026):** `register` (`ANEXA_1`/`ART_48`) scoate preluarea din Anexa 1 (art. 2(1)); `operationCode` e obligatoriu la **orice ieșire**, fiindcă Cap. 3/4 cer operaţia + operatorul. ✅ **Etapa G1 (23.08.2026):** „predare” nu mai e operațiune — predarea e o valorificare/eliminare **făcută de partenerul numit**; litera „Scopul” se derivă din cod și e **doar `V`** (pe cele 10 Anexe 1 completate primite, fișele de eliminare au liniuță, nu `E`) | 🟠 ieșirile vechi n-au cod și nu pot fi clasificate retroactiv → 2b le marchează incomplete, nu le ghicește. ✅ **închis 24.08:** ce cod se trece la predarea către un colector îl **alege clientul la înregistrare** — nu există implicit de codat; 🟠 rămâne doar cine e „agentul economic" din rubrică (colector vs. reciclator final) | ✅ model închis |
| `WasteMovement` — Cap. 2 | — | **lipsesc integral**: `Secția` + cele **cinci nomenclatoare** (tip stocare, mod tratare, scop, mijloc transport, destinație). În date reale sunt constante pe 12 luni → se modelează ca **profil implicit per (punct de lucru, cod)**, cu override pe lună | mare |
| `Partner` (colector/transportator + autorizație) | operatorul din Cap. 3/4 ✅ | rol precis (valorificator vs. eliminator vs. transportator) | medie |
| `MonthlyEvidence` (totaluri/operație pe lună) | agregarea lunară + stoc ✅ | 🔴 **formula e încă greșită în cod** (adună `collected`, scade `handedOver` peste `recovered`/`disposed`); **12 rânduri indiferent de mișcări**; grupurile cu stoc dar fără mișcări în an dispar din raport; regenerarea nu invalidează anii următori. Modelul de sub ea e acum corect — rămâne calculatorul | 🔴 critic — **Etapa 2b, următoarea** |
| `Company` (CUI, tip, autorizație) | identificarea agentului ✅; `CompanyType` e de la Etapa 2a **comutator funcțional** (`keepsArt48Register()`) — o firmă doar-generator nu poate scrie în registrul art. 48 | CAEN, date suplimentare cerute de SIM (🟡 de văzut chestionarul); `afmObligation` boolean → **set de contribuții cu trei cadențe** (Etapa 7) | medie |
| `ReportingDeadline` (AFM lunar auto pe 25) | calendarul ✅ | termen SIM anual **15 martie** (acum știm că e termen legal, art. 48(1)) | mare |
| `Reception` / `Delivery` | ✅ **schemă + entități (Etapa 2a)** — marfa tranzacționată, flux separat de `WasteMovement`, alimentează registrul art. 48, **NU** Anexa 1. Recepția e documentul primar (are și prețul, pentru contribuția AFM de 2%) | ecrane, servicii, controllere — **Etapa 8**; mișcările `COLLECTED` vechi se mută fizic acolo atunci, o singură dată | mare |
| — (nu există) | — | **conversia kg → tone** pentru raportarea art. 48; un singur loc în cod | medie |

### Insight-uri strategice
1. **Amenda: corectăm în tot materialul de vânzare la 20.000–40.000 lei** (per OUG 92/2021).
2. **AFM nu e pentru toți.** Termenul AFM lunar NU trebuie auto-generat pentru orice tenant — îl activăm doar dacă firma are obligații AFM (ambalaje/groapă/etc.). Altfel speriem/inducem în eroare clienți fără obligație. → `Company` primește un flag `hasAfmObligation` (sau un set de obligații AFM).
3. **Motorul de evidență trebuie să calculeze STOC**, nu doar totaluri. ⚠️ **CORECTAT 22.08.2026** — formula scrisă aici înainte (`+ generat − valorificat − eliminat − predat`) era **greșită** și e implementată greșit și în `EvidenceCalculator`. Cap. 1 nu are coloană de predare, deci predarea nu e un termen separat: **`stoc = stoc_anterior + generat − valorificat − eliminat`**. Predarea la un colector intră în „valorificat" sau „eliminat", după scop. Ce e acum în cod scade și `handedOver`, și `recovered`, și `disposed` — dublă scădere.
4. **Codurile R/D și starea fizică** trebuie capturate la nivel de mișcare, altfel fișa oficială nu se poate genera corect. ⚠️ **EXTINS 23.08.2026:** codul R/D nu e cerut doar la valorificare/eliminare cu mijloace proprii, ci la **orice cantitate care iese de pe amplasament** — Cap. 3 și Cap. 4 raportează cantitatea alături de „Operaţia" **și** de „Agentul economic care efectuează operaţia". Un câmp de scop V/E ar fi fost strict mai puțină informație decât cere formularul; litera se derivă din cod (`WasteOperationCode.treatmentPurpose()`).
5. **SIM = anual, per chestionar pe tip de operator.** Tipul firmei (GENERATOR/COLLECTOR/BOTH) determină chestionarul (PRODDES vs COL/TRAT). Deci `CompanyType` e relevant direct pentru raportare.
6. **Sunt trei evidențe, nu două.** Anexa 1 (deșeu propriu) · registrul cronologic art. 48 (marfa tranzacționată) · registrul de recepție al depozitului (HG 349/2005 art. 15(1) lit. d). Primele două sunt separate în model de la Etapa 2a prin `WasteMovement.register`; a treia vine cu profilul de groapă (Etapa 10).

---

## 4. Întrebări pentru expert — stadiu la 22.08.2026

**Închise pe surse primare** (nu mai au nevoie de nimeni):

| # | Întrebare | Răspuns | Unde |
|---|---|---|---|
| 2 | Coloanele celor 4 capitole ale Anexei 1 | confirmate verbatim | `surse-oficiale.md` §1.2 |
| 3 | Termen de arhivare | **3 ani**; 12 luni la transportatori | OUG 92/2021 art. 48(5) — ✅ în cod din 24.08: dosarul acceptă 1–3 ani |
| 6 | Starea fizică — listă standard | **nu există** listă închisă; e câmp liber în act | `surse-oficiale.md` §1.2 |
| 7 | Unitatea de raportare | evidența în kg; **raportarea art. 48 în tone**; **ambalajele în kg** (Ordin 794/2012 **art. 8 alin. (1)**, nu doar antetul anexelor); **Anexa 3 în tone** în act, dar firma alege (`V19`) | OUG 92/2021 art. 48(1) · HG 1061/2008 anexa 3 · `surse-oficiale.md` §5.1 |
| 12 | Formatul de depunere al Anexei 1 Ambalaje | **`.xls`**, cerut pe nume de act; ANPM publică formatul | Ordin 794/2012 art. 6 + art. 7 — ✅ în cod din 25.08 (`PackagingDeclarationXlsxGenerator`) |
| 14 | Raportul colectorului/reciclatorului de ambalaje | **Anexa 3 la Ordinul 794/2012**, per **punct de lucru**, la agenția din raza de activitate; comercianții la ANPM. Cere „Proveniența" (populație / colectori / generatori pers. juridice) — dimensiune pe care mișcarea n-o are. **Neconstruit**, întrebarea AA | Ordin 794/2012 art. 4 |
| 13 | Unde se depune Anexa 1 Ambalaje | **agenţia judeţeană/regională de mediu**, din raza sediului social, pe **25 februarie**. Notificarea de la art. 3 e altceva: la **AFM**, pe **25 ianuarie** | Ordin 794/2012 art. 1, 3, 6 |
| 9 | SIATD — cine intră | 15 categorii de operatori EPR; **generatorii mici nu** | Ordin 701/2024 art. 2 |
| — | Referința R/D de pus în export | **OUG 92/2021 anexa 3 și anexa 7** | `surse-oficiale.md` §2.2–2.3 |
| — | Care listă de coduri | **Decizia 2014/955/UE**, 842 coduri | `surse-oficiale.md` §3 |
| — | Termenul SIM | 15 martie, **termen legal** | OUG 92/2021 art. 48(1) |

**Rămân deschise** *(actualizat 24.08.2026, după runda de răspunsuri R19–R26)*:

1. 🟡 Legea 211/2011 vs. OUG 92/2021 — ce mai e relevant pentru clienții tipici. *(Practic irelevantă acum: referințele care contau s-au mutat pe OUG 92/2021.)*
2. 🟡 **Layoutul chestionarelor SIM** — PRODDES (generatori) și COL/TRAT (colectori), în spatele unui login. **Coborât de la 🔴 pe 24.08:** specialista confirmă că „SIM se bazează pe documentele pe care le avem" — Anexa 1 pentru producător/importator, evidența pentru colector. Deci **niciun câmp nu lipsește din model** și niciun client nu va trebui să completeze retroactiv; rămâne nevăzută doar forma ecranului, de care are nevoie exportul SIM.
3. ✅ **ÎNCHIS 22.08.2026 — Unitatea din Anexa 3 la Ordinul 794/2012 este `[kilograme]`**, la toate cele cinci anexe, verificat pe textul oficial. **Reconfirmat de specialistă pe 24.08:** „kilograme". Fișierul în tone e șablon modificat local. Modulul de ambalaje nu mai e blocat pe unitate.
4. 🟠 **AFM — răspuns parțial pe 24.08:** „doar generatorii de deșeuri de ambalaj, producători/importatorii". Deci un generator obișnuit **nu primește niciun termen AFM**, iar cel lunar generat azi pentru orice firmă cu flagul pornit e greșit. **Ce rămâne de confirmat** (întrebarea L): răspunsul acoperă doar contribuția pe ambalaje (art. 9(1) lit. d), iar cei **2% reținuți la sursă** de un colector (lit. a, lunar) și **economia circulară** a gropilor (lit. c, trimestrial) rămân valabile? De asta atârnă forma Etapei 7. **Și o consecință practică** (întrebarea M): calitatea de producător/importator **nu e întrebată nicăieri** azi — nici în formularul de cerere, nici pe firmă.
5. 🟠 **D5 vs. D1** la menajer — judecată de încadrare, nu fapt. Confirmat de trei ori de specialistă (20.08, 23.08, 24.08); aplicat în datele demo, nu în validări.
6. ✅ **ÎNCHIS 24.08.2026 — ce cod de operațiune se trece la predarea către un colector.** Răspunsul: „**la înregistrare, codurile alese de client**". Nu există regulă de codat — nici R13 implicit, nici operațiunea finală dedusă din material. Exact ce face codul azi: se cere codul R/D la orice ieșire, fără implicit. Cele 13 predări vechi fără cod **nu se migrează**; rămân `UNCLASSIFIED_OUT` / `incomplete` până le completează clientul. 🟠 **Jumătatea a doua rămâne deschisă** (întrebarea C): cine se scrie la „agentul economic care efectuează operaţia" — colectorul căruia i-am predat sau reciclatorul final? Momentan scriem partenerul de pe Anexa 3.
7. ✅ **ÎNCHIS 24.08.2026 — Anexa 1 a unui centru de colectare.** Răspunsul: „**evidența pentru colector**" — colectorul ține registrul cronologic art. 48 pentru marfa care trece prin el, nu o fișă de gestiune per cod preluat. Confirmă seam-ul din Etapa 2a. Nuanța păstrată: tot ține o fișă Anexa 1, mică, pentru deșeul din activitatea proprie (refuzul de la sortare) — de aceea `GENERATED` rămâne disponibil la toate tipurile de cont.
8. ⚪ AFM: merită generat un `.mdb` compatibil sau rămânem la fișa-rezumat? (vezi §5 — verdictul nostru e „fișă-rezumat")
9. ✅ **ÎNCHISĂ 24.08.2026 — despre care „Anexa 1" vorbește specialista.** Răspunsul: despre
   **anexa 1 la Ordinul 794/2012** (declarația de ambalaje), nu despre fișa de gestiune din HG 856.
   Ce a lămurit-o e clasificarea pe care o cerea în aceeași frază — „ce tip generator: importator /
   producător / comercial" —, care e literalmente trioul din **Legea 249/2015, anexa nr. 1**:
   „producătorii de ambalaje şi produse ambalate, **importatorii, comercianţii, distribuitorii**".
   E o clasificare de ambalaje, deci fraza e despre documentul de ambalaje. **Fișa de gestiune nu se
   restrânge**: art. 1 alin. (1) HG 856/2002 o cere oricui generează deșeu, comerciant inclusiv.
   În cod: `MarketRole`, migrarea `V13`. Sursa: `surse-oficiale.md` §11. *(Textul de mai jos e
   întrebarea așa cum a fost pusă; se păstrează fiindcă explică de ce nu era evident.)*

   ~~🔴 NOU 24.08.2026 — despre care „Anexa 1" vorbește specialista~~ când spune „e strict pentru generatorii de deșeuri de ambalaj, producători/importatorii"? Vezi caseta de mai jos. Nu blochează cod; decide cui spunem că are nevoie de aplicație. Întrebarea K din `intrebari-specialist.md`.

---

### ⚠️ „Anexa 1" înseamnă două documente diferite

Confuzia asta e cea mai ieftină cale de a strica modulul de generatori, deci se scrie o dată, aici:

| | Fișa de gestiune | Declarația de ambalaje |
|---|---|---|
| Act | **HG 856/2002, anexa 1** | **Ordinul 794/2012, anexa 1** |
| Titlu | „Evidenţa gestiunii deşeurilor" | „Producători şi importatori de ambalaje de desfacere, **de produse ambalate**, supraambalatori de produse ambalate" |
| Cine o ține | orice generator, per cod de deșeu (art. 1 alin. (1)) | cine **pune pe piaţă marfă ambalată** — deci pune și ambalajul, adică generează deșeu de ambalaj în piață |
| Formă | 4 capitole × 12 luni, o pagină per cod | tabele pe materiale (PET, hârtie, aluminiu, oțel, lemn, sticlă), în kg |
| În aplicație | **G5, livrat** | modulul de ambalaje, **livrat 25.08** (`V22` + `V26`): ambele tabele se însumează din mişcările pe coduri `15 01 xx`, iar descărcarea e `.xls` cu două foi |

Titlul celui de-al doilea se citește **până la capăt**: nu „producători şi importatori *de ambalaje*", ci „*de ambalaje de desfacere, de produse ambalate*". Nu fabricanții de ambalaje — **oricine pune pe piață produse ambalate**. Categorie largă, nu de nișă.

Aceeași capcană la „Anexa 3": HG 1061/2008 (dovada predării, generată azi) vs. Ordinul 794/2012 anexa 3 (raportarea anuală a colectorilor și comercianților de ambalaje).


---

## 5. Cercetare portaluri oficiale — SIATD / AFM-online / „AFM – Declarații" (2026-07-12) 🟡

> **Actualizare 22.08.2026.** Partea de SIATD (§5.A) a fost verificată între timp pe textul oficial
> al **Ordinului MMAP 701/2024** — categoriile de operatori obligați, termenele de confirmare
> (3/5/15 zile) și datele cerute per tranzacție sunt în [`surse-oficiale.md` §6](surse-oficiale.md).
> Concluzia „generatorii mici nu intră" ✅ se confirmă.
>
> A apărut și ceva nou, care nu era pe radar în iulie: **proiectul de HG pentru sistemul TRACE-DM**
> (consultare publică MMAP, mai 2026) — trasabilitatea deșeurilor reciclabile predate de **persoane
> fizice**, administrat de AFM, cu înregistrare în **24 de ore** și amendă 20.000–40.000 lei. E
> proiect, nu act, deci **nu se codează nimic pe el** — dar dă câmpurile unei viitoare entități
> `Reception` și e argumentul comercial al modulului de depozit. Detalii: `surse-oficiale.md` §7.
>
> Partea de AFM (§5.B) **nu** a fost reverificată — rămâne 🟡, pe surse secundare.

> **Concluzie de produs:** niciun portal oficial NU are API public / import machine-to-machine de la terți care să merite construit în Faza 1. **Confirmă modelul „pregătim, nu transmitem".** Detalii mai jos, cu surse la final.

### A. SIATD — Sistemul Informatic de Asigurare a Trasabilității Deșeurilor (`siatd.afm.ro`)
- **NU e** evidența generală HG 856/2002. E trasabilitatea **tranzacțiilor** cu deșeuri în sistemul **răspunderii extinse a producătorului (EPR)**, gestionat de AFM. ✅
- **Ordin MMAP 701/2024** (în vigoare 10 apr. 2024) l-a **extins** de la doar ambalaje la: **ambalaje, anvelope, EEE, baterii/acumulatori portabili** (+ menționează deșeuri municipale). Bază: Legea 249/2015. ✅
- **Cine e obligat:** operatori profesioniști din lanț — OIREP, colectare/brokeraj/salubritate/sortare/tratare, valorificare/reciclare, UAT-uri. **NU generatorii mici tipici** (clientul nostru de bază). ✅ (surse secundare confirmă explicit)
- **Obligația:** validezi **coduri unice de tranzacție** la recepția deșeului, în **3/5/15 zile** (după tip). Acces doar cu **semnătură electronică calificată** (înrolare). ✅
- **API / import terți:** **nedocumentat** pe sursele oficiale. 🟡 (absență de dovezi, nu dovadă de absență)
- **Impact la noi:** în afara scopului de bază. Eventual modul opțional viitor „alerte de validare SIATD (3/5/15 zile)" **doar** pentru clienții din lanț EPR. NU în Faza 1.

### B. Declarația la Fondul pentru mediu — „AFM – Declarații" + eTAX AFM-online (`online.afm.ro`)
- E despre **contribuții la fondul de mediu** (ambalaje/EPR, anvelope, uleiuri, baterii, EEE, taxa la groapă), **NU** evidența deșeurilor. O datorează doar firmele cu obligații AFM (avem deja flagul `Company.afmObligation`). ✅
- Flux: aplicație **desktop oficială „AFM – Declarații"** → date introduse **manual** SAU **restaurate dintr-un backup** al bazei ei → generează declarația → **semnare digitală** → încărcare prin **eTAX AFM-online** (Ordin 572/2019). ✅
- **Formatul de import:** aplicația folosește o bază **Microsoft Access `.mdb`**; **nu** acceptă XML/CSV din programe terțe (spre deosebire de D394 la ANAF). Singura „poartă" = fișierul de **backup `.mdb`**. 🟡 (confirmat de surse secundare SAGA + existența instrucțiunilor oficiale de backup/restore; formatul exact `.mdb` de confirmat vizual în aplicație)
- **Verdict:** a genera un `.mdb` compatibil = fragil + neoficial (schemă nepublicată, versionată) → **nu merită în Faza 1** și nu e miezul nostru. Pentru clienții cu obligație AFM facem: (a) **termenul** (25 a lunii) în TERMENE; (b) o **fișă-rezumat** de date pe care o transcrie clientul/contabilul. Nu import automat.

---

## Surse (web, iulie 2026)
- OUG 92/2021 — [Portal Legislativ](https://legislatie.just.ro/Public/DetaliiDocument/245846) · [Lege5](https://lege5.ro/Gratuit/ha3tsnbtgi4a/ordonanta-de-urgenta-nr-92-2021-privind-regimul-deseurilor) · [ANMAP](https://anmap.gov.ro/oug-92-2021-privind-regimul-deseurilor/)
- HG 856/2002 — [Portal Legislativ](https://legislatie.just.ro/Public/DetaliiDocumentAfis/38294) · [Lege5 + Anexa 1](https://lege5.ro/Gratuit/gm3tgnrw/anexa-nr-1-hotarare-856-2002)
- SIM / ANPM — [raportare.anpm.ro](https://raportare.anpm.ro/) · [ghid registrul-deseurilor](https://registrul-deseurilor.ro/2025/08/07/anpm-a-deschis-sesiunea-de-raportare-pentru-2024-ce-trebuie-sa-stii-si-cum-te-ajuta-registrul-deseurilor/)
- AFM — [Calendar fiscal AFM](https://www.afm.ro/taxe_calendar_fiscal.php) · [Instrucțiuni completare declarație](https://www.afm.ro/main/venituri/afm_declaratii-instructiuni.pdf) · [Lege5 instrucțiuni 11.12.2023](https://lege5.ro/gratuit/ge2dmmrvgayda/instructiunile-de-completare-a-formularului-declaratie-privind-obligatiile-la-fondul-pentru-mediu-din-11122023)
- SIATD / AFM (cercetare §5, iulie 2026) — [Ordin 701/2024 – Portal Legislativ](https://legislatie.just.ro/Public/DetaliiDocument/281612) · [Instrucțiuni SIATD 2020 – Portal Legislativ](https://legislatie.just.ro/Public/DetaliiDocumentAfis/229538) · [Ordin 701/2024 PDF (ecologic.rec.ro)](https://ecologic.rec.ro/wp-content/uploads/2024/04/Ordin-MMAP-701-2024-Instructiuni-Aplicatia-SIATD.pdf) · [Ecologic – SIATD obligatoriu aproape tot domeniul](https://ecologic.rec.ro/siatd-ul-a-devenit-obligatoriu-pentru-aproape-tot-domeniul-gestionarii-deseurilor/) · [AFM – Legislație declarații/obligații](https://www.afm.ro/legislatie_acte_normative_declaratii_obligatii.php) · [Ordin 572/2019 PDF](https://www.afm.ro/main/venituri/ordin572-2019.pdf) · [AFM – Instrucțiuni backup bază de date (PDF)](https://www.afm.ro/main/venituri/instructiuni_backup.pdf)

---

## Ce s-a implementat din toate astea (24.08.2026, etapele G1–G8)

| Cerință legală | Unde e în cod | Stare |
|---|---|---|
| Anexa 1 cap. 1 — patru coloane de cantitate, fără „predare” | `WasteOperation` (fără `HANDED_OVER`), `EvidenceCalculator` | ✅ |
| Anexa 1 cap. 2 — „Secţia” | `InternalGenerator`, sub punctul de lucru | ✅ |
| Anexa 1 cap. 2 — Stocare (Tipul) și Tratare (Modul) | `StorageType`, `TreatmentMethod`, nota 1 și 2 verbatim | ✅ |
| Anexa 1 cap. 2 — Scopul (V/E) | `TreatmentPurpose`, derivat din codul R/D; **doar `V`** se scrie | ✅ |
| Anexa 1 cap. 2 — Transport (Mijlocul, Destinaţia) | `TransportMeans`, `WasteDestination`, nota 4 și 5 verbatim | ✅ |
| Anexa 1 cap. 3 și 4 — cantitate + operaţie + operator | `operationCode` + `partner` pe mişcare | ✅ |
| **Fişa oficială Anexa 1 tipărită** (antet + cele 4 capitole) | `Anexa1SheetBuilder` + `Anexa1FormGenerator` | ✅ |
| Anexa 3 la HG 1061/2008 — formularul de transport | `Anexa3FormGenerator` | ✅ |
| Cantitate necunoscută la predare (cântărire la destinatar) | `weighedAtUnloading`, cantitate nullable, linie provizorie | ✅ |
| Registrul art. 48 OUG 92/2021 | `Reception` / `Delivery` — schemă, fără ecrane | 🔜 Etapa 8 |
| Declaraţia anuală (foaia „raportare deseuri generate”) | `AnnualDeclarationBuilder` + `AnnualDeclarationGenerator` — un rând per cod, o pagină per punct de lucru | ✅ |
