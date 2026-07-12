# Documentul de reguli — legislație deșeuri (EcoRegistru)

> **Scop:** sursa de adevăr pentru CE cere legea, ca să nu codăm rapoarte pe presupuneri.
> **Regula de aur (din planul de proiect):** un raport greșit = client amendat = business mort.
> Nimic din secțiunea „format oficial" nu se codează până nu e bifat de tata + un expert de mediu.
>
> **Legendă încredere:** ✅ confirmat cu sursă · 🟡 probabil corect, de verificat cu expert · 🔴 necunoscut / de cercetat
>
> Ultima actualizare: 2026-07-11. Cercetare făcută cu surse web (vezi linkuri la final). Cunoștințele modelului au limită ianuarie 2026 — orice modificare legislativă ulterioară NU e acoperită aici.

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
- **Anexa 2** = Lista Europeană a Deșeurilor (codurile de 6 cifre, cele cu `*` = periculoase). ✅ — acesta e nomenclatorul nostru `waste_codes`.
- Evidența se ține **per punct de lucru** și se păstrează pentru control (min. 3 ani; 🟡 de confirmat termenul exact de arhivare).

### C. Raportarea SIM (Sistemul Integrat de Mediu) → ANPM — ANUAL ✅
- Platformă ANPM. Toți cei care generează/gestionează deșeuri raportează **anual, în format centralizat**, datele din evidența lunară.
- **Termen standard: 15 martie** pentru anul anterior (🟡 în practică s-a tot amânat din cauza aplicației ANPM — ex. raportarea pe 2024 s-a deschis în vara 2025).
- **Chestionare diferite pe tip de operator** ✅:
  - `PRODDES` — generatori de deșeuri (majoritatea clienților noștri „generator")
  - `COL/TRAT` — operatori de colectare/valorificare
  - `TRAT` — instalații de tratare
  - `MUN` — operatori de deșeuri municipale
  - `NĂMOL` — stații de epurare
- Autentificare separată pe `raportare.anpm.ro`. **Transmiterea automată e ÎN AFARA Fazei 1** — noi pregătim datele, clientul le încarcă.

### D. Declarația AFM (Fondul pentru Mediu) — LUNARĂ ✅
- „Declarație privind obligațiile la Fondul pentru mediu", **electronic exclusiv** prin AFM-online (din iulie 2022). ✅
- **Termen: 25 a lunii următoare** ✅ (excepție eco-taxa la pungi = trimestrial).
- **IMPORTANT — nuanță strategică:** declarația AFM NU e despre evidența deșeurilor în sine, ci despre **contribuții la fondul de mediu**: ambalaje (răspundere extinsă a producătorului/EPR), anvelope, uleiuri, baterii/acumulatori, EEE, substanțe periculoase, taxa pentru deșeuri încredințate spre eliminare la groapă, emisii etc.
- **Deci NU orice client are obligație AFM lunară.** Depinde de activitate (dacă introduce pe piață ambalaje/produse, dacă duce la groapă etc.). Instrucțiuni oficiale de completare: OMM/AFM din 11.12.2023 (🟡 de citit integral cu expertul).
- Amendă/penalități AFM: până la 250.000 lei + penalități zilnice la plată. 🟡

### E. Pe radar, ÎN AFARA Fazei 1 (de notat, nu de construit acum)
- **RO e-Transport** — notificarea transporturilor (inclusiv deșeuri) în SAF-T/e-Transport. 🟡
- **SGR (Sistemul Garanție-Returnare)** — relevant doar dacă clientul pune pe piață băuturi ambalate. 🟡
- **Legea 249/2015** (ambalaje) — dacă avem clienți cu obligații de ambalaje.

---

## 2. Formatul evidenței — Anexa 1 HG 856/2002 (de verificat cu textul oficial) 🟡

Evidența e **per (agent economic / punct de lucru, tip de deșeu, an)**, cu **luni pe rânduri**. Câmpuri de identificare pe fișă:
- Agent economic (+ punct de lucru), Anul
- **Cod deșeu** (Anexa 2 / LED)
- **Starea fizică** a deșeului (solid / lichid / nămol / etc.)
- **Unitatea de măsură** (de regulă tone/kg)

Structura pe **4 capitole**, fiecare cu tabel pe 12 luni + total (🟡 coloanele exacte se validează cu documentul Word oficial):
1. **Cap. 1 — Generarea deșeurilor**: cantitate generată; din care valorificată, eliminată; **stoc** (rămas la sfârșitul lunii).
2. **Cap. 2 — Stocarea provizorie, tratarea și transportul**: cantitate stocată/tratată/transportată; mod de stocare/tratare; **operatorul de transport + destinația (nume, adresă)**.
3. **Cap. 3 — Valorificarea deșeurilor**: cantitate valorificată; **codul operației R (R1–R13)**; **operatorul care valorifică**.
4. **Cap. 4 — Eliminarea deșeurilor**: cantitate eliminată; **codul operației D (D1–D15)**; **operatorul care elimină**.

Codurile **R1–R13** (valorificare) și **D1–D15** (eliminare) provin din Directiva-cadru UE / Anexele OUG 92/2021 ✅ (stabile).

---

## 3. Ce avem ÎN aplicație vs. ce cere legea (gap analysis)

| Entitate actuală | Acoperă | LIPSEȘTE / de adăugat | Prioritate |
|---|---|---|---|
| `WasteCode` (cod, nume, periculos) | Anexa 2 / LED ✅ | Lista oficială completă (avem 10 placeholder) | 🔴 mare |
| `WasteMovement.operation` (GENERATED/COLLECTED/HANDED_OVER/RECOVERED/DISPOSED) | maparea pe cele 4 capitole ✅ aprox. | **cod R/D** pe valorificare/eliminare; **starea fizică**; distincția transport vs. predare | mare |
| `Partner` (colector/transportator + autorizație) | operatorul din Cap. 2/3/4 ✅ | rol precis (valorificator vs. eliminator vs. transportator) + adresă completă pe fișă | medie |
| `MonthlyEvidence` (totaluri/operație pe lună) | agregarea lunară ✅ | **stoc (sold cumulativ) lună-de-lună**; defalcare pe cod R/D | mare |
| `Company` (CUI, tip, autorizație) | identificarea agentului ✅ | CAEN, date suplimentare cerute de SIM (🟡 de văzut chestionarul) | medie |
| `ReportingDeadline` (AFM lunar auto pe 25) | calendarul ✅ | **AFM NU e universal** — trebuie flag per-tenant „are obligație AFM?"; + termen SIM anual (15 martie) | mare |

### Insight-uri strategice
1. **Amenda: corectăm în tot materialul de vânzare la 20.000–40.000 lei** (per OUG 92/2021).
2. **AFM nu e pentru toți.** Termenul AFM lunar NU trebuie auto-generat pentru orice tenant — îl activăm doar dacă firma are obligații AFM (ambalaje/groapă/etc.). Altfel speriem/inducem în eroare clienți fără obligație. → `Company` primește un flag `hasAfmObligation` (sau un set de obligații AFM).
3. **Motorul de evidență trebuie să calculeze STOC**, nu doar totaluri. Stoc lună = stoc lună anterioară + generat − valorificat − eliminat − predat. E ieftin acum, greu de adăugat retroactiv la exporturi.
4. **Codurile R/D și starea fizică** trebuie capturate la nivel de mișcare (valorificare/eliminare), altfel fișa oficială nu se poate genera corect. Le adăugăm la model ACUM (ieftin), chiar dacă exportul oficial vine mai târziu.
5. **SIM = anual, per chestionar pe tip de operator.** Tipul firmei (GENERATOR/COLLECTOR/BOTH) determină chestionarul (PRODDES vs COL/TRAT). Deci `CompanyType` e relevant direct pentru raportare.

---

## 4. Întrebări pentru tata + expert (înainte de a coda formate)
1. Legea 211/2011 — ce mai e valabil din ea vs. OUG 92/2021 pentru clienții noștri tipici?
2. Anexa 1 HG 856/2002 — confirmăm coloanele exacte ale celor 4 capitole (avem nevoie de documentul Word oficial curent).
3. Termen de arhivare a evidenței (3 ani? 5 ani?) — pentru „dosarul de control".
4. Care clienți tipici au **efectiv** obligație AFM lunară și pe ce contribuții (ca să modelăm corect obligațiile)?
5. SIM: ce câmpuri suplimentare cere chestionarul PRODDES (generatori) și COL/TRAT (colectori) dincolo de ce avem?
6. Starea fizică — listă de valori standard folosite în fișă.
7. Unități: legea preferă tone; noi avem KG/TONS — confirmăm conversia și unitatea de raportare.
8. **Declarația la Fondul pentru mediu**: aplicația oficială „AFM – Declarații" acceptă date doar prin **restaurarea unui backup al bazei ei (Access `.mdb`)**, nu import XML/CSV din terți (vezi §5). Confirmăm: merită să generăm un `.mdb` compatibil, sau rămânem la o fișă-rezumat pe care o transcrie clientul/contabilul?
9. **SIATD** (Ordin 701/2024): care dintre clienții noștri tipici intră efectiv sub obligație — sau e strict pentru operatori EPR (colectori/reciclatori/OIREP), nu pentru generatorii mici? (vezi §5)

---

## 5. Cercetare portaluri oficiale — SIATD / AFM-online / „AFM – Declarații" (2026-07-12) 🟡

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
