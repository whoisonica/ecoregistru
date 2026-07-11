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

---

## Surse (web, iulie 2026)
- OUG 92/2021 — [Portal Legislativ](https://legislatie.just.ro/Public/DetaliiDocument/245846) · [Lege5](https://lege5.ro/Gratuit/ha3tsnbtgi4a/ordonanta-de-urgenta-nr-92-2021-privind-regimul-deseurilor) · [ANMAP](https://anmap.gov.ro/oug-92-2021-privind-regimul-deseurilor/)
- HG 856/2002 — [Portal Legislativ](https://legislatie.just.ro/Public/DetaliiDocumentAfis/38294) · [Lege5 + Anexa 1](https://lege5.ro/Gratuit/gm3tgnrw/anexa-nr-1-hotarare-856-2002)
- SIM / ANPM — [raportare.anpm.ro](https://raportare.anpm.ro/) · [ghid registrul-deseurilor](https://registrul-deseurilor.ro/2025/08/07/anpm-a-deschis-sesiunea-de-raportare-pentru-2024-ce-trebuie-sa-stii-si-cum-te-ajuta-registrul-deseurilor/)
- AFM — [Calendar fiscal AFM](https://www.afm.ro/taxe_calendar_fiscal.php) · [Instrucțiuni completare declarație](https://www.afm.ro/main/venituri/afm_declaratii-instructiuni.pdf) · [Lege5 instrucțiuni 11.12.2023](https://lege5.ro/gratuit/ge2dmmrvgayda/instructiunile-de-completare-a-formularului-declaratie-privind-obligatiile-la-fondul-pentru-mediu-din-11122023)
