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
> Ultima actualizare: 2026-08-22 (verificare integrală pe surse primare: Portal Legislativ, EUR-Lex,
> sgglegis.gov.ro). Cercetarea anterioară, din 2026-07-11, era făcută pe surse secundare — o parte
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
>    − eliminat` — fără termen separat pentru predare.
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
| `WasteCode` (cod, nume, periculos) | nomenclatorul ✅ | **încărcarea listei 2014/955/UE (842 coduri)** peste cele 10 placeholder | 🔴 mare |
| `WasteMovement.operation` (GENERATED/COLLECTED/HANDED_OVER/RECOVERED/DISPOSED) | GENERATED / RECOVERED / DISPOSED ✅ | 🔴 **`COLLECTED` nu are ce căuta în Anexa 1** (art. 2(1)) și **`HANDED_OVER` nu e coloană pe fișă** — ambele se scad azi din stoc, deci dubla scădere. Predarea are nevoie de un **scop (V/E)**, exact câmpul „Scopul" din Cap. 2 | 🔴 critic |
| `WasteMovement` — Cap. 2 | — | **lipsesc integral**: `Secția` + cele **cinci nomenclatoare** (tip stocare, mod tratare, scop, mijloc transport, destinație). În date reale sunt constante pe 12 luni → se modelează ca **profil implicit per (punct de lucru, cod)**, cu override pe lună | mare |
| `Partner` (colector/transportator + autorizație) | operatorul din Cap. 3/4 ✅ | rol precis (valorificator vs. eliminator vs. transportator) | medie |
| `MonthlyEvidence` (totaluri/operație pe lună) | agregarea lunară + stoc ✅ | 🔴 formula greșită (vezi rândul de mai sus); **12 rânduri indiferent de mișcări**; grupurile cu stoc dar fără mișcări în an dispar din raport; regenerarea nu invalidează anii următori | 🔴 critic |
| `Company` (CUI, tip, autorizație) | identificarea agentului ✅ | CAEN, date suplimentare cerute de SIM (🟡 de văzut chestionarul) | medie |
| `ReportingDeadline` (AFM lunar auto pe 25) | calendarul ✅ | termen SIM anual **15 martie** (acum știm că e termen legal, art. 48(1)) | mare |
| — (nu există) | — | **`Reception` / `Delivery`** — marfa tranzacționată de un colector. Flux separat de `WasteMovement`, alimentează raportarea de colector, **NU** Anexa 1. Câmpurile viitoare sunt deja cunoscute din TRACE-DM art. 5 | după decizia de produs |
| — (nu există) | — | **conversia kg → tone** pentru raportarea art. 48; un singur loc în cod | medie |

### Insight-uri strategice
1. **Amenda: corectăm în tot materialul de vânzare la 20.000–40.000 lei** (per OUG 92/2021).
2. **AFM nu e pentru toți.** Termenul AFM lunar NU trebuie auto-generat pentru orice tenant — îl activăm doar dacă firma are obligații AFM (ambalaje/groapă/etc.). Altfel speriem/inducem în eroare clienți fără obligație. → `Company` primește un flag `hasAfmObligation` (sau un set de obligații AFM).
3. **Motorul de evidență trebuie să calculeze STOC**, nu doar totaluri. ⚠️ **CORECTAT 22.08.2026** — formula scrisă aici înainte (`+ generat − valorificat − eliminat − predat`) era **greșită** și e implementată greșit și în `EvidenceCalculator`. Cap. 1 nu are coloană de predare, deci predarea nu e un termen separat: **`stoc = stoc_anterior + generat − valorificat − eliminat`**. Predarea la un colector intră în „valorificat" sau „eliminat", după scop. Ce e acum în cod scade și `handedOver`, și `recovered`, și `disposed` — dublă scădere.
4. **Codurile R/D și starea fizică** trebuie capturate la nivel de mișcare (valorificare/eliminare), altfel fișa oficială nu se poate genera corect. Le adăugăm la model ACUM (ieftin), chiar dacă exportul oficial vine mai târziu.
5. **SIM = anual, per chestionar pe tip de operator.** Tipul firmei (GENERATOR/COLLECTOR/BOTH) determină chestionarul (PRODDES vs COL/TRAT). Deci `CompanyType` e relevant direct pentru raportare.

---

## 4. Întrebări pentru expert — stadiu la 22.08.2026

**Închise pe surse primare** (nu mai au nevoie de nimeni):

| # | Întrebare | Răspuns | Unde |
|---|---|---|---|
| 2 | Coloanele celor 4 capitole ale Anexei 1 | confirmate verbatim | `surse-oficiale.md` §1.2 |
| 3 | Termen de arhivare | **3 ani**; 12 luni la transportatori | OUG 92/2021 art. 48(5) |
| 6 | Starea fizică — listă standard | **nu există** listă închisă; e câmp liber în act | `surse-oficiale.md` §1.2 |
| 7 | Unitatea de raportare | evidența în kg; **raportarea art. 48 în tone** | OUG 92/2021 art. 48(1) |
| 9 | SIATD — cine intră | 15 categorii de operatori EPR; **generatorii mici nu** | Ordin 701/2024 art. 2 |
| — | Referința R/D de pus în export | **OUG 92/2021 anexa 3 și anexa 7** | `surse-oficiale.md` §2.2–2.3 |
| — | Care listă de coduri | **Decizia 2014/955/UE**, 842 coduri | `surse-oficiale.md` §3 |
| — | Termenul SIM | 15 martie, **termen legal** | OUG 92/2021 art. 48(1) |

**Rămân deschise:**

1. 🟡 Legea 211/2011 vs. OUG 92/2021 — ce mai e relevant pentru clienții tipici. *(Practic irelevantă acum: referințele care contau s-au mutat pe OUG 92/2021.)*
2. 🔴 **Capturile din SIM** — cum arată efectiv chestionarele PRODDES (generatori) și COL/TRAT (colectori). Sunt în spatele unui login; **niciun document public nu le poate da**. Singura întrebare care mai are rost să fie pusă specialistei.
3. ✅ **ÎNCHIS 22.08.2026 — Unitatea din Anexa 3 la Ordinul 794/2012 este `[kilograme]`**, la toate cele cinci anexe, verificat pe textul oficial. Fișierul în tone al specialistei e șablon modificat local. Modulul de ambalaje nu mai e blocat. *(Rămâne de întrebat, ca politețe operațională: APM-ul județean îi acceptă în practică varianta în tone?)*
4. 🟡 Care clienți tipici au **efectiv** obligație AFM lunară și pe ce contribuții.
5. 🟠 **D5 vs. D1** la menajer — judecată de încadrare, nu fapt. Nu se propune ca valoare implicită fără confirmare.
6. ⚪ AFM: merită generat un `.mdb` compatibil sau rămânem la fișa-rezumat? (vezi §5 — verdictul nostru e „fișă-rezumat")

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
