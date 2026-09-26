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
>
> ⚠️ **Recitit pe 10.09.2026 pe forma consolidată a OUG 92/2021 — versiunea de la 11.07.2026.**
> §2 e rescris de la capăt: §2.1 (art. 48), plus §2.4–§2.7, noi. **Citatul din art. 48 alin. (1)
> care stătea aici era textul dinainte de 12.01.2023** — exact greşeala pe care documentul ăsta
> există ca s-o prevină. Un citat verbatim îmbătrâneşte; **data accesării nu e o formalitate, e
> termenul lui de valabilitate.** Regula nouă: la fiecare recitire se ia forma consolidată, nu
> forma de bază, şi se notează versiunea, nu doar ziua în care am deschis pagina.
>
> 🔁 **Recitit pe 26.09.2026 pentru punctele deschise ale modulului de depozit** (`ecoregistru-docs/docs/todo-colector.md`).
> Corectate: **§4** (art. 1 alin. (3), art. 20 alin. (5), art. 25 alin. (2) lit. a)), **§6** (6 fotografii, GPS-ul nu e pe
> tranzacție, termenele curg de la recepție, amenzile, cine intră în SIATD), **§7** (calendarul TRACE-DM, încă proiect),
> **§8** (**HG 349/2005 e abrogată din 2021** prin OG 2/2021), **§10** (forma curentă a OUG 196/2005), **§13** (art. 4
> alin. (2), nu art. 3), **§15** (inventarul și costul stocului, pe sursă primară), **§17** (**art. 29 din OG 20/1992 era
> textul din 1992**; interdicția e la art. 19), **§18.3** (borderoul PF devine implicit), **§18.6** (lit. a) și j)).
> Notele de cercetare, cu toate citatele: `ecoregistru-docs/research_notes/Depozit puncte deschise în lege/`; raportul:
> `ecoregistru-docs/reports/Depozit puncte deschise în lege.md`.
> **Runda 2 (aceeași zi, fișierele `runda2_*.md`)** a închis din text și ce lăsase prima rundă „la oameni”: calculul
> termenelor SIATD (Codul civil, §6.1), obligația SIATD a colectorilor de municipale (ierarhia actelor + anunțul AFM,
> §6.1), limita de 10 MB (§6.1), evidența pe punct de lucru, „regimul special”, registrul și diferența de cântar (§4),
> perisabilitatea, stocul degradat și preluarea gratuită (§15.1, §15.3), practica metodei de cost (§15.2), Ordinul 1271/2018
> (§18.3), codul după sortare, R12 și pierderile 99 99 99 (§18.6–18.7), autorizațiile reale și **`anpm.ro` dispărut** (§18.6).

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
conformitate; e temeiul punctului 2 din `istoric/audit-conformitate.md`.*

> Deşeurile periculoase prevăzute în anexa nr. 2 sunt marcate cu un **asterisc (*)**.

**Art. 5 alin. (1):**

> Tipurile de deşeuri prevăzute în anexa nr. 2 sunt definite în mod individual printr-un cod complet
> format din **6 cifre**.

> **Consecință pe export.** Antetul fișei cere „Tipul de deşeu … cod … **(conform codificării din
> anexa nr. 2)**" — deci codificarea la care trimite formularul e chiar cea în care asteriscul e
> marcajul de periculozitate. Anexa 2 tipărește 406 coduri cu asterisc; lista 2014 are 408.
> `waste_codes.csv` ține deliberat codul fără asterisc și periculozitatea ca boolean separat, ceea
> ce e corect pentru stocare — dar la **tipărire** asteriscul trebuie pus la loc, altfel un deșeu
> periculos arată pe fișă exact ca unul nepericulos. ✅ Pus la loc la tipărire: `util/WasteCodeLabel.official`.
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
> aceeaşi ordine — cu o excepţie fără efect: `TreatmentMethod` are `TT` înainte de `D`; legenda tipărită e în ordinea notei, fixată de `Anexa1FormIT`. *La verificarea din 02.09 exista o abatere deliberată — `TreatmentPurpose` n-avea `E` (motivul, la
> §1.3 punctul 1); din **16.09.2026** enum-ul are amândouă literele notei 3, `V` şi `E`, la cererea proprietarului, deci
> nu mai e nicio abatere.*

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
>
> ✅ **Închis pe text și pe corpus, 16.09.2026.** Prima jumătate o închisese specialista pe 24.08 („codurile
> alese de client”, `legislatie.md` punctul 6). A doua jumătate, cine e „agentul economic”, se închide
> din trei lucruri citite acum.
>
> **1. Notele de subsol ale anexelor 3 și 7 din OUG 92/2021.** Forma consolidată de pe Portalul Legislativ,
> [doc. 245846](https://legislatie.just.ro/Public/DetaliiDocument/245846), are ca ultimă modificare
> 11.07.2026 și a fost citită pe 16.09.2026. Pe 10.09 am copiat anexele fără note (§2.2, §2.3), iar
> răspunsul era tocmai în ele:
>
> > R12 Schimbul de deșeuri în vederea expunerii la oricare dintre operațiunile numerotate de la R 1 la R 11⁵
> > ⁵ În cazul în care nu există niciun alt cod R corespunzător, aceasta include operațiunile preliminare
> > înainte de valorificare, inclusiv preprocesarea, cum ar fi, printre altele, demontarea, sortarea,
> > sfărâmarea, compactarea, granularea, mărunțirea uscată, condiționarea, reambalarea, separarea și
> > amestecarea înainte de supunerea la oricare dintre operațiunile numerotate de la R1 la R11.
> >
> > R13 Stocarea deșeurilor înaintea oricărei operațiuni numerotate de la R 1 la R 12 (excluzând stocarea
> > temporară, înaintea colectării, la situl unde a fost generat deșeul)⁶
> > ⁶ Stocare temporară înseamnă stocare preliminară în conformitate cu anexa nr. 1 pct. 6.
> >
> > D13 Amestecarea anterioară oricărei operațiuni numerotate de la D1 la D12²
> > ² În cazul în care nu există niciun alt cod D corespunzător, aceasta include operațiunile preliminare
> > înainte de eliminare, inclusiv preprocesarea, cum ar fi, printre altele, sortarea, sfărâmarea, compactarea,
> > granularea, uscarea, mărunțirea uscată, condiționarea sau separarea […]
> >
> > D15 Stocarea înaintea oricărei operațiuni numerotate de la D1 la D14 (excluzând stocarea temporară,
> > înaintea colectării, în zona de generare a deșeurilor)³
> > ³ Stocare temporară înseamnă stocare preliminară în conformitate cu articolul 3 punctul 10.
>
> Anexa nr. 1 pct. 6: *„colectare - strângerea deșeurilor, inclusiv sortarea și stocarea preliminară a
> deșeurilor, în vederea transportării la o instalație de tratare”*. Pct. 32: *„tratare - operațiunile de
> valorificare sau eliminare, inclusiv pregătirea prealabilă valorificării sau eliminării”*.
> **Deci colectorul care doar stochează face el însuși o operațiune cu cod (R13/D15), iar cel care sortează
> sau balotează face R12/D13.** Nu există un gol în care operațiunea colectorului să n-aibă cod, deci nu e
> nevoie să împrumuți codul reciclatorului final.
>
> **2. Rubrica fișei e o pereche.** *„Operaţia de valorificare”* și *„Agentul economic care efectuează
> operaţia de valorificare”*: agentul e cel care face operația scrisă în coloana de alături. Perechea
> (R13, colectorul) e coerentă, la fel și (R3, reciclatorul). Perechea (R3, colectorul) nu e, fiindcă
> colectorul nu reciclează.
>
> **3. Corpusul, 9 fișe `.xlsx` din 2022–2024:** la agent se scrie **destinatarul direct**, iar codul e
> **operațiunea lui**. Doi operatori de colectare și sortare au **R12**, un operator de salubrizare care
> stochează înaintea eliminării are **D15**, depozitul de deșeuri are **D1**, iar trei reciclatori au **R3**.
> Pe nicio foaie nu apare un reciclator final în locul colectorului căruia i s-a predat. Numele partenerilor
> nu se scriu aici, fiindcă sunt date de client.
>
> ⇒ **Regula:** agentul economic = partenerul căruia i-ai predat deșeul (destinatarul de pe Anexa 3), iar
> codul = operațiunea pe care **o face el**, din autorizația lui de mediu: R13/D15 dacă doar stochează,
> R12/D13 dacă sortează sau balotează, R1–R11 sau D1–D10 dacă tratează efectiv. Generatorul nu poate
> ști ce se întâmplă după colector și nici nu i se cere. Răspunderea de la art. 24 alin. (1) (§2.1b) rămâne,
> dar o acoperă verificarea autorizației destinatarului (AH), nu codul de pe fișă. Aplicația făcea deja
> asta: partenerul e „agentul” (`strings.movements.partnerHint`). Din 16.09.2026, explicația de sub „Cod
> operațiune” spune regula pe înțeles (`operationCodeHintRecovery` / `operationCodeHintDisposal`). **Niciun cod
> nu se propune implicit**, fiindcă depinde de autorizația partenerului, pe care aplicația nu o ține pe coduri.

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
   → `TreatmentPurpose` avea un singur membru.
   ⚠️ **Înlocuit pe 16.09.2026 (decizia proprietarului):** se scriu amândouă literele notei 3 — `V` lângă un cod R, `E`
   lângă un cod D, derivate din codul R/D al mişcării (`WasteOperationCode.treatmentPurpose()`) —, ca o fişă depusă să nu
   aibă niciodată „Scopul” gol. Numărătoarea de mai jos rămâne ca descriere a practicii din corpus, nu ca regulă a codului.
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

Sursă: [legislatie.just.ro/Public/DetaliiDocument/245846](https://legislatie.just.ro/Public/DetaliiDocument/245846),
**forma consolidată**. Accesat 10.09.2026; **versiunea în vigoare: 11.07.2026**.

⚠️ **Cum s-a învechit secţiunea asta, ca să nu se repete.** Până pe 10.09.2026, citatele de mai jos
veneau dintr-un [PDF republicat pe ecoteca.ro](https://ecoteca.ro/wp-content/uploads/2022/01/92_OG_2021_RegimulDeseurilor_10ian2022.pdf)
datat **10 ianuarie 2022** — gazdă secundară *şi* text pre-2023. Actul fusese modificat de şase ori
de atunci. Formularea „accesat 22.08.2026" arăta proaspăt şi nu era: data spunea când am deschis
fişierul, nu ce versiune era în el.

**Istoricul modificărilor, complet la 10.09.2026** (din forma consolidată):

| În vigoare de la | Act modificator |
|---|---|
| **11-07-2026** | **Legea nr. 26 din 11 martie 2026** (M.Of. 189/11.03.2026) — adaugă **art. 34¹** şi redenumeşte ANPM → **ANMAP** |
| 15-12-2023 | OUG nr. 114 din 14 decembrie 2023 |
| 03-11-2023 | OUG nr. 96 din 2 noiembrie 2023 |
| 12-01-2023 | Legea nr. 17 din 6 ianuarie 2023 (legea de aprobare a OUG 92/2021) |
| 30-09-2022 | OUG nr. 133 din 29 septembrie 2022 |
| 07-04-2022 | OUG nr. 38 din 6 aprilie 2022 |

> 🆕 **ANPM se numeşte de la 11.07.2026 „Agenţia Naţională pentru Mediu şi Arii Protejate" —
> ANMAP.** Nota de la finalul actului: *„În întreg cuprinsul Ordonanţei de urgenţă a Guvernului
> nr. 92/2021 [...] denumirea «Agenţia Naţională pentru Protecţia Mediului» (ANPM) se înlocuieşte cu
> denumirea «Agenţia Naţională pentru Mediu şi Arii Protejate» (ANMAP)"* — art. IV din Legea
> 26/2026, aplicat în 44 de locuri în textul consolidat.
>
> ⚠️ **Redenumirea e în OUG 92/2021. Nu e un `sed` peste tot proiectul.** Dacă Ordinul 794/2012 şi
> HG 856/2002 n-au fost redenumite la rândul lor, formularele tipărite după ele trebuie să păstreze
> numele din **actul care le cere**, nu din legea-cadru. E o verificare separată, per act — vezi
> P3.8, închisă pe 11.09.2026 chiar mai jos (istoria ei în `istoric/status-pana-la-14.09.2026.md`).
>
> ✅ **Verificarea s-a făcut pe 11.09.2026, şi a răspuns în amândouă capetele** (felia G-5):
>
> | Actul | Redenumit? | Ce urmează din asta |
> |---|---|---|
> | **OUG 92/2021** | **da**, art. IV din Legea 26/2026, „în întreg cuprinsul" | tot ce citează legea-cadru spune **ANMAP** |
> | **Ordinul 794/2012** | **nu** — neatins de vreo modificare, scrie şi azi „Agenţia Naţională pentru Protecţia Mediului" (art. 2 alin. (2), art. 9) | actul rămâne cu numele lui |
> | **HG 856/2002** | **irelevant** — nu numeşte nicio agenţie: peste tot scrie „autoritatea publică centrală de protecţie a mediului" şi „autorităţile publice teritoriale" | nimic de schimbat |
>
> 🔴 **Şi inventarul feliei era greşit exact pe partea care o făcea grea.** Cele două locuri notate
> „ajunge pe hârtie? **da**" **nu ajung pe hârtie**: `PackagingOperatorRole.addressee()` n-are azi
> niciun apel, iar `anexa3AddresseeAnpm` (azi `anexa3AddresseeAnmap`, în `components/packaging/Anexa3Section.tsx`) se randează pe ecran. **Niciunul dintre
> cele şase documente oficiale pe care le tipărim nu poartă numele agenţiei** — modelele n-au
> rubrică de destinatar. Deci întreaga dilemă „numele actului sau numele de azi?" se juca pe patru
> şiruri de ecran, nu pe hârtie.
>
> ✅ **Regula aleasă, şi e generală:** *unde citim actul, numele din act; unde îi spunem clientului
> unde să se ducă, numele instituţiei de azi.* Pe ecran scrie **ANMAP** (destinatarul Anexei 3
> Ambalaje, eticheta comerciantului, exemplul din nota de termen), fiindcă omul care citeşte rândul
> va căuta „ANMAP" — site-ul e `anmap.gov.ro`. Citatele din Ordinul 794/2012 rămân cum le scrie
> ordinul. Dacă vreodată un formular chiar capătă rubrică de destinatar, regula spune singură ce se
> tipăreşte în ea.

### 2.1 ARTICOLUL 48 — Păstrarea evidenţei

**Alin. (1)** — cine, ce, până când. *Text curent, luat pe 10.09.2026 din forma consolidată:*

> Producătorii de deşeuri nepericuloase, unităţile şi întreprinderile prevăzute la art. 34,
> producătorii de deşeuri periculoase şi unităţile şi întreprinderile care colectează sau transportă
> deşeuri periculoase, nepericuloase cu titlu profesional **ori** acţionează în calitate de
> comercianţi şi de brokeri de deşeuri periculoase şi nepericuloase ţin o **evidenţă cronologică
> lunară**, **o publică în format tabelar** şi o pun la dispoziţia agenţiei judeţene pentru protecţia
> mediului **electronic în sistemul pus la dispoziţie de ANMAP**, **până la 15 martie anul următor
> raportării**, precum şi la cerere autorităţilor competente de control, după:
>
> a) codul deşeului potrivit art. 7 alin. (1), **cantitatea în tone**, natura şi originea deşeurilor
> generate, precum şi cantitatea de produse şi materiale care rezultă din pregătirea pentru
> reutilizare, din reciclare sau din alte operaţiuni de valorificare, eliminare;
> b) destinaţia, frecvenţa colectării, modul de transport şi metoda de tratare prevăzută pentru
> deşeuri, atunci când este relevant; şi
> c) **cantitatea de deşeuri în tone** încredinţată spre eliminare.

> 🔴 **Citatul de mai sus a fost greşit în fişierul ăsta de la 22.08.2026 până la 10.09.2026.**
> Ce stătea aici era partea introductivă **dinainte de 12.01.2023**, modificată de Legea 17/2023
> art. I pct. 23. Ce s-a schimbat, cuvânt cu cuvânt:
>
> | Textul vechi (până la 12.01.2023) | Textul în vigoare |
> |---|---|
> | „ţin o evidenţă cronologică lunară **tabelară**" | „ţin o evidenţă cronologică lunară, **o publică în format tabelar**" |
> | „o pun la dispoziţia agenţiei [...] **în format letric, la cerere**, şi electronic" | „o pun la dispoziţia agenţiei [...] **electronic**" (letricul a dispărut) |
> | „în sistemul pus la dispoziţie de **APM**" | „în sistemul pus la dispoziţie de **ANMAP**" (era ANPM; redenumit 11.07.2026) |
>
> **Ce contează pentru cod, şi ce nu.** Cele trei lit. a)–c), termenul de 15 martie şi tonele sunt
> **neschimbate** — deci nimic din ce s-a construit pe ele nu e greşit. Ce se schimbă e o nuanţă de
> obligaţie: „**o publică** în format tabelar" e mai mult decât „o ţine tabelară", iar depunerea nu
> mai are variantă pe hârtie. Nu ştim încă dacă „publică" înseamnă ceva peste încărcarea în sistem —
> e practică, nu text, şi nu blochează nimic.

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
> 1. Dosarul de control se dimensionează la **3 ani** (12 luni la transportatori). Închide D1. ✅ **Construit pe 24.08.2026** (Etapa 6): folder per an; azi `years=1..5` (`AuditFileService.MAX_YEARS`: 3 = termenul, 5 = marja cerută de specialistă).
> 2. **15 martie e termen legal**, nu cutumă ANPM. Închide C3.
> 3. Evidența lunară se ține în kg (practica fișei Anexa 1), dar **raportarea de la art. 48 e în
>    tone**. Conversia trebuie să existe într-un singur loc în cod, nu presărată prin export.
>    *(Notă 19.09.2026, verificat în cod: la generator, „Totalul anului” din Generare e în **kg** din 17.09.2026 —
>    specialista, AF: în SIM se tastează kg. Tonele rămân pe exportul registrului art. 48 al colectorului, `art48Hint`.)*
> 4. ~~🔴 **Articolul descrie conţinutul, nu forma — şi n-are anexă cu facsimil.** [...] e nevoie de
>    un exemplar completat. Vezi întrebarea **AD** — nu avem niciunul.~~ **Închis pe 15.09.2026, pe
>    text — vezi §2.1-bis.** Premisa „formatul există, doar că nu-l avem" era falsă.

**Alin. (3)** și **alin. (7)** — *lipseau din secțiunea asta până pe 15.09.2026, și erau exact
alineatele care răspundeau la AD. Luate pe 15.09.2026 din forma consolidată la 11.07.2026:*

> (3) ANMAP elaborează **procedura de raportare** a datelor şi informaţiilor prevăzute la alin. (1) în
> termen de 180 de zile de la data intrării în vigoare a prezentei ordonanţe de urgenţă, care va fi
> aprobată prin ordin al conducătorului autorităţii publice centrale pentru protecţia mediului.
>
> (7) ANMAP instituie un **registru electronic** sau registre coordonate pentru a înregistra datele
> privind deşeurile prevăzute la alin. (1) care acoperă întregul teritoriu geografic.

### 2.1-bis De ce AD se închide fără exemplar (15.09.2026)

**1. „Registrul art. 48" nu e un document al colectorului — e obligația tuturor.** Alin. (1) îi
numește pe **producătorii de deșeuri** (generatorii) înaintea colectorilor. Pentru un generator,
evidența de la art. 48 e fișa din anexa nr. 1 la HG 856/2002, iar cele 15 martie le depune deja.
Pentru colector, HG 856/2002 art. 2 alin. (2) cere doar ca evidența mărfii colectate să fie
„raportată la solicitare”. **Nu dă niciun model**, pentru că actul are doar două anexe (nr. 1 fișa, nr. 2 lista).
Numele intern „registrul art. 48" pentru `WasteRegister.ART_48` e o prescurtare de-a noastră, nu un
titlu din act. De asta nu l-a recunoscut specialista.

**2. Singura formă prescrisă e cea din alin. (1): „cronologică lunară", „format tabelar", pe lit.
a)–c).** Alin. (3) trimite restul la o **procedură aprobată prin ordin**. Ordinul n-a fost găsit (Portalul
Legislativ, căutare 15.09.2026). Nu afirmăm că nu există, doar că nu l-am găsit. Ce există sigur e alin. (7): **registrul
electronic al ANMAP**, adică aplicația SIM „Statistica Deșeurilor", unde datele se **tastează** (AG,
confirmat de specialistă pe 14.09: „nu există upload”). **Forma oficială e deci ecranul
portalului, nu o hârtie.** N-avem ce facsimil să reproducem, fiindcă nu există unul.

**3. Ghidul MMAP din 20.09.2023** (aprobat prin Ordinul nr. 2.436/2023, M.Of. 880 bis/29.09.2023,
[legislatie.just.ro/Public/DetaliiDocument/276420](https://legislatie.just.ro/Public/DetaliiDocument/276420))
**nu schimbă nimic**. E o compilație de texte existente: pct. 745–756 reiau art. 48, iar „Anexa nr. 5 —
Evidența gestiunii deșeurilor" e fișa din HG 856/2002. ⚠️ Pct. 745 citează **textul de dinainte de
12.01.2023** („în format letric, la cerere"), deci nu se citează din ghid, ci din OUG.

**4. Ce cere portalul de la un colector.** Chestionarul **„Colectare/Tratare"**, după *SIM
Statistica Deșeurilor — Ghid de utilizare*, ANPM, ianuarie 2014, pp. 34–52
([anmap.gov.ro/…/SIM.SD.GhidPublic.pdf](https://anmap.gov.ro/documents/27459/69882411/SIM.SD.GhidPublic.pdf)):

| Tabel | Pe rând | Rubrici |
|---|---|---|
| Cap. 1 · T1 Colectarea deșeurilor | un cod de deșeu | sursa colectării (listă), stoc la începutul anului, cantitate colectată, valorificat din colectat, eliminat din colectat, stoc la sfârșitul anului, cod R, cod D |
| Cap. 1 · T2 Dezmembrare | un cod | aceleași |
| Cap. 1 · T3 Generare fără colectat | un cod | aceleași, pentru deșeul propriu |
| Cap. 2 · A Valorificare | unitate × cod × R | unitatea care preia (după CUI; localitate, județ, SIRUTA se completează singure), cod, cantitate preluată, cod R |
| Cap. 2 · B Eliminare | unitate × cod × D | la fel, cu cod D |

Cantitățile se trec **în tone**. Corelațiile `REC_001`/`REC_002` cer, pe fiecare cod, stoc inițial +
colectat = valorificat + eliminat + stoc final, iar orice cod din Cap. 1 trebuie să apară și în Cap. 2.
⚠️ **Ghidul e din 2014, iar aplicația a fost refăcută** (termen de intrare în producție ~01.05.2025,
proiectul „servicii publice de mediu digitalizate”). Rubricile de mai sus sunt **conținutul**
chestionarului, verificat pe singura sursă oficială publică. Aranjarea exactă pe ecranul de azi nu se poate
verifica fără cont.

**Consecința pentru cod.** Tot ce cer lit. a)–c) și chestionarul se află deja pe `WasteMovement`: dată
(cronologic), cod, cantitate, `operation`, `operationCode` (R/D), `partner` (nume, CUI, adresă),
`transportMeans`, `treatmentMethod`, `wasteDestination`, plus stocul, care se deduce. Lipsește doar
„sursa colectării", o listă a portalului pe care ghidul n-o tipărește. (Din 16.09.2026, D1.12, evidența cronologică are coloana **„Originea”** de la lit. a), cu cuvintele notei 2 din Anexa 3 la Ordinul 794/2012 — „populaţie”, „generator persoană juridică”, „colector”, „comerciant” — citită din operațiunea de cântar sau din fișa partenerului; lista portalului rămâne necunoscută, deci nu se pretinde că e aceeași.) Exportul registrului `ART_48`
**nu inventează un formular oficial**: e tabelul cronologic lunar cerut de alin. (1), plus totalurile
anuale așezate ca în tabelele de mai sus, ca ajutor de copiat în portal.

**Alin. (2)** — buletinele de analiză. *Citat adăugat 02.09.2026 (audit, pct. 9).*

> Producătorii şi deţinătorii de deşeuri periculoase sunt obligaţi să deţină **buletinele de analiză
> care caracterizează deşeurile periculoase** şi să le transmită, la cerere, autorităţilor competente
> pentru protecţia mediului.

> **Consecință.** Ataşamentele generice de pe mişcare pot ţine fişierul, dar nimic nu leagă un
> buletin de un cod periculos şi nimic nu semnalează absenţa lui în dosarul de control. Pentru un
> client cu coduri periculoase, dosarul e incomplet legal fără să se vadă. Întrebarea **AL**.
>
> ⚠️ *(Notă 19.09.2026, verificat în cod: buletinele s-au construit pe 11.09 (`V38`) şi **s-au scos pe 14.09.2026, la sfatul
> specialistei**. Tabela din `V38` a rămas în schemă, fără cod care s-o citească. Obligaţia de la alin. (2)
> rămâne a clientului; aplicaţia n-o mai urmăreşte.)*

### 2.1b ARTICOLUL 23 — cui poţi preda deşeul, şi cine răspunde de el

*Adăugat 02.09.2026, la audit. E temeiul punctelor 5 şi 8 din `istoric/audit-conformitate.md`.*

**Alin. (1):**

> Producătorul de deşeuri iniţial sau, după caz, orice deţinător de deşeuri are obligaţia de a
> efectua operaţiunile de tratare în conformitate cu prevederile art. 4 alin. (1) - (3) şi art. 21
> prin mijloace proprii sau prin intermediul unui **operator economic autorizat** care desfăşoară
> activităţi de tratare a deşeurilor sau unui operator public ori privat de colectare a deşeurilor
> [...]

**Alin. (4) şi (5)** — persoana desemnată. *Corectat pe 17.09.2026: până atunci aici stătea forma de dinainte de
12.01.2023. Textul în vigoare, modificat de **Legea 17/2023** art. I pct. 16, citit pe Portalul Legislativ
([doc. 245846](https://legislatie.just.ro/Public/DetaliiDocument/245846)):*

> (4) Pentru îndeplinirea obligațiilor legale privind gestionarea deșeurilor, titularul unei
> **activități economice de gestionare a deșeurilor și/sau generatoare de deșeuri** are obligația să
> **desemneze o persoană** din rândul angajaților proprii sau să **delege această obligație unei terțe
> persoane**.
>
> (5) **Pentru activitățile care necesită autorizație de mediu/autorizație integrată de mediu**,
> persoanele desemnate, prevăzute la alin. (4), trebuie să fie **instruite** în domeniul prevenirii
> generării de deșeuri și al managementului deșeurilor, inclusiv în domeniul substanțelor periculoase,
> ca urmare a absolvirii unor programe de perfecționare și specializare recunoscute la nivel național
> [...]

> Forma veche spunea „titularul unei activităţi, pentru care autoritatea competentă [...] a emis o autorizaţie
> de mediu”, deci obligaţia părea doar a firmelor autorizate, iar instruirea a tuturor desemnaţilor. Acum e
> invers: **orice generator** desemnează, **doar cine are autorizaţie de mediu** cere şi certificatul.
> Încălcarea se sancţionează la art. 62 alin. (1) lit. a), 40.000–60.000 lei pentru persoana juridică.
> În cod: `AuditFileService.wasteManagerNote` (dosarul cere certificatul numai când firma are număr de autorizaţie).

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
>    **AH**. *(Notă 19.09.2026, verificat în cod: construit; avertismentul compară data mişcării cu expirarea sau cu sfârşitul vizei
>    anuale, `WasteMovementMapper.authorizationExpiredAtHandover`, `V41`. AH e închisă pe 17.09.2026: avertisment, nu refuz.)*
> 2. ~~Alin. (4)–(5) sunt o rubrică de dosar pe care n-o ținem nicăieri.~~ *Construită (`V29`), corectată pe forma în vigoare 17.09.2026.* `contactName`/`contactRole`
>    sunt blocul de semnătură al declarației, nu persoana desemnată, și nu poartă certificatul de
>    instruire. Notă de produs: **consultantul de mediu e chiar „terța persoană" de la alin. (4)** —
>    rubrica se completează singură pentru portofoliul specialistei. Întrebarea **AK**, închisă pe text pe 17.09.2026.
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
> era anexa 2), e adevărată doar pentru valorificare, rămasă la 3. Vezi `istoric/audit-conformitate.md`
> pct. 1. **Nu e o întrebare pentru specialistă** — actul o spune de două ori.

> **Nuanța D1 / D5 NU e tranșată de sursa primară.** Specialista a spus că deșeul menajer dus la
> depozit se codifică **D5**, nu D1, pentru că depozitele conforme au celule etanșe — iar textul lui
> D5 („compartimente separate etanşe [...] acoperite şi izolate") descrie exact asta. **Dar exemplul
> dat la D1 este literalmente „depozite de deşeuri".** E o judecată de încadrare, nu un fapt
> verificabil. Nu se propune ca valoare implicită în formular fără confirmare explicită. Rămâne
> întrebare deschisă.

---

### 2.3b Anexele 3 şi 7, reverificate pe forma consolidată (10.09.2026)

✅ **Neschimbate.** Cele 13 coduri R şi cele 15 coduri D de mai sus au fost comparate cuvânt cu
cuvânt cu forma consolidată la 11.07.2026: **identice**, inclusiv exemplele din paranteze şi nota
că D11 e interzisă. Niciuna dintre cele şase modificări ale actului n-a atins anexele 3 sau 7.
`WasteOperationCode` rămâne corect şi complet, iar tot ce tipăresc formularele pe „Operaţia" e bun.

*Notat aici fiindcă jumătate din valoarea unei reverificări e lista lucrurilor care nu trebuie
atinse. Anexele erau singurul lucru citit din PDF-ul pre-2023, deci singurul care putea fi vechi.*

---

### 2.4 ARTICOLUL 62 — sancţiunile. Cifra din materialul de vânzare era greşită

*Citit prima dată pe 10.09.2026. Nu fusese niciodată citat aici.*

**Alin. (1) lit. a)** — încălcarea evidenţei:

> cu amendă de la **5.000 lei la 15.000 lei, pentru persoanele fizice**, şi de la **40.000 lei la
> 60.000 lei, pentru persoanele juridice**, în cazul încălcării dispoziţiilor art. 8 alin. (1), (2)
> şi (4), art. 12 alin. (11) şi (12), art. 13 alin. (3), art. 15 alin. (1), alin. (2) lit. a) şi b)
> şi alin. (3), art. 16 alin. (1)-(3) şi (6), **art. 17 alin. (3)**, (4), (6) şi (7), art. 20
> alin. (1) şi (2), art. 22 alin. (3) şi (4), **art. 23**, art. 24 alin. (1), **art. 27 alin. (1)**,
> art. 28 alin. (1), (4) şi (5), **art. 29**, **31**, art. 34 alin. (1) şi (5), **art. 36
> alin. (2)**, art. 44 alin. (1) şi (3), **art. 48 alin. (1), (2), (5) şi (6)** şi art. 61 alin. (4);

**Alin. (1) lit. e)** — raportările:

> cu amendă de la **5.000 lei la 10.000 lei, pentru persoanele juridice**, în cazul încălcării
> dispoziţiilor art. 32 alin. (1) şi (2) şi **art. 49 alin. (9), (11), (12) şi (15)**.

**Alin. (1) lit. b)** — abandonarea, incendierea, îngroparea (art. 20 alin. (3)-(6)) şi
reclasificarea prin diluare (art. 11): **30.000–45.000 lei** persoane fizice, **50.000–70.000 lei**
persoane juridice.

**Alin. (3)** — cine verifică cifrele pe care le depune clientul:

> Constatarea corectitudinii datelor transmise ANMAP şi agenţiilor judeţene pentru protecţia
> mediului potrivit dispoziţiilor art. 48 alin. (1) se realizează de către comisari din cadrul
> **Gărzii Naţionale de Mediu**.

> 🔴 **Consecinţa, şi e în materialul de vânzare.** `legislatie.md` şi `monetizare.md` scriau
> **20.000–40.000 lei** pentru lipsa evidenţei. Cifra corectă pentru o **persoană juridică** e
> **40.000–60.000 lei**. Nu s-a învechit între timp: s-a schimbat pe **12-01-2023** prin Legea
> 17/2023 art. I pct. 27, deci nota a fost greşită **din ziua în care a fost scrisă** (22.08.2026).
> Reparat în ambele fişiere pe 10.09.2026, plus în `README.md`-ul repo-ului public, care o scria în
> engleză. Amenda reală e **mai mare**, deci argumentul comercial se întăreşte, nu se strică.
>
> 📌 **Şi mai util decât cifra: lista de la lit. a) e harta obligaţiilor clientului.** Fiecare
> articol îngroşat acolo e ceva ce aplicaţia ori ştie deja, ori ar putea spune în dosarul de
> control. Vezi §2.6.

**Art. 64 alin. (1¹)** — confiscarea, adăugată la deşeurile care au valoare:

> În cazul nerespectării dispoziţiilor art. 20 alin. (4) se aplică sancţiunea contravenţională
> complementară de confiscare a deşeurilor care au valoare, deţinute, păstrate în afara spaţiilor
> autorizate **şi/sau a căror provenienţă nu este dovedită**, în condiţiile legii.

> 💰 **„Provenienţa dovedită" e literalmente produsul.** Un registru ţinut la zi e dovada de
> provenienţă; fără el, marfa cu valoare se confiscă, nu se amendează. E cel mai direct argument
> comercial din tot actul şi merită pe pagina publică.

---

### 2.5 ARTICOLUL 49 alin. (9) — al doilea termen anual: **30 aprilie**

*Găsit pe 10.09.2026. Nu exista în calendarul aplicaţiei.*

> Titularii pe numele cărora au fost emise autorizaţii de construire şi/sau desfiinţări **şi
> producătorii şi deţinătorii de uleiuri uzate** trebuie să raporteze anual APM, **până la 30
> aprilie a anului următor celui pentru care se raportează**, conformarea cu art. 17 alin. (7) şi
> măsurile adoptate potrivit art. 31 alin. (1).

Ce se raportează, pe cele două categorii:

- **titularul de autorizaţie de construire/desfiinţare** → conformarea cu **art. 17 alin. (7)**:
  atingerea unui nivel de pregătire pentru reutilizare, reciclare şi alte operaţiuni de valorificare
  materială de **minimum 70% din masa deşeurilor nepericuloase** din construcţii şi desfiinţări
  (excepţie: `17 05 04`, materiale geologice naturale);
- **producătorul/deţinătorul de uleiuri uzate** → măsurile de la **art. 31 alin. (1)**: colectare
  separată în recipiente închise etanş, tratare cu prioritate prin regenerare, neamestecare, stocare
  în spaţii împrejmuite şi securizate.

**Sancţiune:** art. 62 alin. (1) lit. e) — **5.000–10.000 lei** pentru persoane juridice.

> 🔴 **Pe cine loveşte, la noi.** `DeadlineService` generează 15 martie, 25 ianuarie, 25 februarie şi
> cadenţele AFM. **30 aprilie nu există** — nici în `ReportType`, nici în vreun document până azi.
> Iar „deţinător de uleiuri uzate" e chiar clientul-tip: `13 02 08*` e codul din seed **şi** din
> exemplul de Anexa 2 pe care s-a construit modulul ieri.
>
> ✅ **Semnalul e derivabil din date, nu trebuie întrebat.** Dacă firma are mişcări pe coduri de ulei
> uzat, are termenul — la fel cum `PACKAGING_ANNUAL` se generează doar la un profil care pune
> ambalaje pe piaţă. Regula rămâne cea din `ReportType`: **o alertă e o afirmaţie**, deci se
> generează pe semnal pozitiv, nu pe tăcere. Felia: `todo-lansare.md` **P3.7**. ✅ **Construită:** `ReportType.APM_ANNUAL_APRIL`, `DeadlineService.aprilDeadline` — pe
> autorizaţia de construire din profil sau pe un cod de ulei uzat în anul raportat.

**Art. 31 alin. (3)** — obligaţia de fond, care explică de ce e nevoie de raportare:

> Producătorii şi deţinătorii de uleiuri uzate, cu excepţia persoanelor fizice, sunt obligaţi să
> predea **întreaga cantitate** numai operatorilor economici autorizaţi să desfăşoare activităţi de
> colectare, valorificare şi/sau de eliminare a uleiurilor uzate.

*Notă: HG 235/2007 privind gestionarea uleiurilor uzate a fost **abrogată** de art. 71 alin. (1)
lit. b) din OUG 92/2021, iar orice trimitere la ea se consideră făcută la OUG 92/2021 (alin. (2)).
Deci regimul uleiurilor uzate e în întregime în art. 31–32.*

---

### 2.6 ARTICOLUL 34¹ — lista publică a operatorilor autorizaţi (**nou, în vigoare 11.07.2026**)

*Adăugat de art. I din Legea nr. 26 din 11 martie 2026. Are patru luni.*

> **(1)** Agenţia Naţională pentru Mediu şi Arii Protejate are obligaţia de a publica pe pagina
> proprie de internet **o listă actualizată a tuturor operatorilor economici autorizaţi** pentru
> activităţi de gestionare a deşeurilor. Lista va cuprinde: denumirea operatorului economic, **codul
> fiscal**, tipul activităţilor autorizate, **documentul integral al autorizaţiei** de mediu sau,
> după caz, al autorizaţiei integrate de mediu deţinute, precum şi **statusul vizei anuale**
> aplicate, respectiv seria şi data deciziei privind viza anuală emisă pentru autorizaţia
> respectivă.
>
> **(2)** Actualizarea listei prevăzute la alin. (1) se realizează în termen de **maximum 10 zile
> lucrătoare** de la emiterea, modificarea, suspendarea sau anularea oricărei autorizaţii de
> mediu/autorizaţii integrate de mediu în domeniul gestionării deşeurilor ori de la emiterea unei
> decizii de viză anuală sau de neacordare a vizei anuale.

**Art. III din Legea 26/2026** (normă tranzitorie, nu intră în corpul OUG):

> (1) În termen de **60 de zile** de la data intrării în vigoare a prezentei legi, Agenţia Naţională
> pentru Mediu şi Arii Protejate elaborează **procedura** privind modalitatea de publicare şi
> actualizare a listei prevăzute la art. 34¹ [...] în vederea asigurării **accesibilităţii online,
> publice şi deschise**, a datelor privind autorizaţiile de mediu/autorizaţiile integrate de mediu,
> inclusiv prin utilizarea **Sistemului integrat de mediu** sau a altor platforme informatice.
> (2) Procedura prevăzută la alin. (1) se aprobă prin ordin al autorităţii publice centrale pentru
> protecţia mediului.

**Şi o a doua listă publică, pentru uleiuri — art. 31 alin. (4):**

> APM publică pe site-ul propriu lista cu operatorii economici autorizaţi să desfăşoare activităţi de
> salubritate, colectare, valorificare şi/sau de eliminare a uleiurilor uzate.

> 🟠 **Asta e sursa care lipsea la întrebarea AH.** Azi aplicaţia avertizează despre autorizaţia
> destinatarului **din ce a tastat clientul** (audit pct. 5, construit 04.09) — o dată pe care n-o
> verifică nimeni. De pe 11.07.2026 există un registru public de verificat, cu **CUI-ul** ca cheie
> de potrivire cu `Partner`.
>
> ⚠️ **Şi conţine un câmp pe care nu-l modelăm deloc: viza anuală.** O autorizaţie poate fi în
> termen şi viza refuzată — caz în care operatorul nu mai poate primi deşeu legal, iar
> `Partner.authorizationExpiry` din model spune „e bună". ✅ *Viza e în model din `V41`
> (`Partner.authorizationValidUntil()`: expirarea sau sfârşitul vizei, care vine întâi) — §2.6-bis.* Nu e o nuanţă: art. 34¹ alin. (2) pune
> suspendarea şi anularea în acelaşi rând cu viza, deci sunt evenimente aşteptate, nu excepţii.
>
> ⏳ ~~**Cele 60 de zile ale ANMAP s-au împlinit pe ~09.09.2026** — adică ieri. Primul pas al feliei
> e să ne uităm dacă lista şi ordinul chiar există.~~
>
> 🔴 **Ne-am uitat, pe 11.09.2026: nu există nici lista, nici ordinul.** `anmap.gov.ro` are un meniu
> „Autorizaţii" cu două intrări — *Listă autorizaţii de mediu 01.01.2024–31.07.2025* şi
> *Centralizator 01.08.2025 – prezent* —, iar centralizatorul e un **tabel HTML** cu: titlu, data
> postării, link, categorie, autoritate emitentă, adresa PDF-ului, **denumire titular**, judeţ,
> localitate, tip document, data emiterii, număr act. **Nu e lista de la art. 34¹**, şi îi lipsesc
> exact cele două câmpuri pentru care ne-ar folosi: **CUI-ul** (cheia de potrivire cu `Partner`) şi
> **statusul vizei anuale**. Nici ordinul de ministru de la art. III alin. (2) nu e publicat.
> **Deci felia nu se poate dimensiona azi**, şi asta e chiar răspunsul pasului întâi: nu se
> scrapează un tabel care va fi înlocuit de altul, cu altă formă, când apare procedura.
> ⏳ **De reverificat**, şi n-are rost mai des de o dată pe lună. Felia: `todo-lansare.md` **P3.9**,
> `istoric/status-pana-la-14.09.2026.md` **G-6**.

### 2.6-bis Viza anuală a autorizaţiei de mediu — ce spune actul (verificat 14.09.2026)

*Pornit de la răspunsul specialistei la **AH** (14.09.2026): „se ţine viza, nu data de expirare;
e valabilă 1 an de la data autorizaţiei". Citit pe Portalul Legislativ, **forma consolidată**:
OUG 195/2005 la versiunea din **07.04.2022** (ultima din istoric; nicio modificare a art. 16–17 după
18.11.2019), [doc. 67634](https://legislatie.just.ro/Public/DetaliiDocument/67634); Procedura aprobată
prin Ordinul 1150/2020 la versiunea din **09.01.2023** (Ordinul 3.309/2022, ultima din istoric),
[doc. 226703](https://legislatie.just.ro/Public/DetaliiDocument/226703).*

**OUG 195/2005, art. 16** (introdus de Legea 219/2019, în vigoare din 18.11.2019):

> **(2)** Abrogat.
> **(2^1)** Autorizaţia de mediu şi autorizaţia integrată de mediu **îşi păstrează valabilitatea pe
> toată perioada în care beneficiarii lor obţin viza anuală.**
> **(2^6)** În cazul în care [...] Agenţia Naţională pentru Protecţia Mediului constată că **nu a fost
> solicitată sau obţinută viza anuală, se aplică dispoziţiile art. 17 alin. (3) şi (4).**

**Art. 17 alin. (3)–(4)** — ce înseamnă lipsa vizei, în trepte, nu dintr-odată:

> **(3)** [...] autorizaţia de mediu [...] **se suspendă** de către autoritatea emitentă [...] după o
> notificare prealabilă prin care se poate acorda un termen de cel mult 60 de zile [...]. Suspendarea
> se menţine până la eliminarea cauzelor, dar nu mai mult de 6 luni. **Pe perioada suspendării,
> desfăşurarea [...] activităţii este interzisă.**
> **(4)** În cazul în care nu s-au îndeplinit condiţiile stabilite prin actul de suspendare, [...]
> dispune, după expirarea termenului de suspendare, **anularea** [...] autorizaţiei.

**Legea 219/2019, art. II** (reprodus în notă la art. 16) — ⚠️ **data de expirare NU a dispărut peste tot:**

> **(1)** Obţinerea vizei anuale este obligatorie atât pentru autorizaţiile [...] emise la data intrării
> în vigoare a prezentei legi, cât şi pentru cele emise ulterior.
> **(2)** Valabilitatea autorizaţiilor [...] emise la data intrării în vigoare a prezentei legi **se poate
> modifica la cererea titularilor**, în sensul menţinerii valabilităţii [...] pe toată perioada în care
> titularul obţine viza anuală.
> **(3)** În cazul în care titularul **nu solicită modificarea valabilităţii** [...], acesta este obligat
> ca, înainte cu cel puţin 6 luni de la expirarea valabilităţii actului de reglementare, să solicite
> emiterea unei noi autorizaţii [...], **chiar dacă pe parcursul termenului de valabilitate a obţinut
> viza anuală.**

Vechiul art. 16 dădea **5 ani** autorizaţiei de mediu şi **10 ani** celei integrate. Deci o autorizaţie
de mediu de dinainte de 18.11.2019 nemodificată a expirat cel târziu în noiembrie 2024 — azi nu mai
contează —, dar **o autorizaţie integrată de mediu** (depozite, incineratoare, instalaţii mari) emisă
între 2009 şi 2019 şi nemodificată **poate avea încă o dată de expirare, până în noiembrie 2029.**

**Procedura (Ordinul 1150/2020), art. 5 alin. (4)–(6)** — de unde se socoteşte „anul":

> **(4)** Termenul în care titularul activităţii solicită aplicarea vizei anuale este de **maximum 90 de
> zile şi de minimum 60 de zile înainte de ziua şi luna corespunzătoare zilei şi lunii în care a fost
> emisă autorizaţia** pe care acesta o deţine. În cazul în care autorizaţia [...] a fost revizuită,
> termenul de 60 de zile se va calcula în funcţie de ziua şi luna în care a fost emisă **autorizaţia
> iniţială**.
> **(5)** Pentru autorizaţia [...] revizuită, titularul solicită aplicarea vizei în anul imediat următor
> revizuirii [...].
> **(6)** Pentru solicitările transmise în termen mai scurt [...], autoritatea [...] decide dacă este
> posibilă derularea procedurii [...] până la data corespunzătoare zilei şi lunii în care a fost emisă
> autorizaţia [...] iniţială sau este necesară aplicarea prevederilor art. 17 alin. (3) şi (4) [...].

**Art. 6 alin. (6)** — respingerea: după notificare (max. 60 de zile) şi suspendare, „[...] emite
decizia motivată de respingere a vizei anuale [...]. **Autorizaţia [...] îşi pierde valabilitatea**, iar
titularul are obligaţia de a solicita emiterea unei noi autorizaţii". **Art. 7:** decizia (model în
anexa nr. 4) „**devine anexă la autorizaţie** şi face parte integrantă din aceasta".

**Anexa nr. 4, modelul deciziei:**

> DECIZIE **Nr. ...... din .............** [...] **Articolul 1** Începând cu data comunicării prezentei
> decizii **se aplică viza pentru perioada ...............** / se respinge viza, pentru Autorizaţia de
> mediu [...] nr. ...... din .................. a titularului [...].

**Ce rezultă, pe rubrici** (şi e mai precis decât „1 an de la data autorizaţiei"):

1. **Ziua şi luna emiterii autorizaţiei iniţiale** sunt ancora anului de viză — **nu** data deciziei de
   viză şi nici data unei revizuiri. Asta confirmă răspunsul Andreei.
2. **Decizia de viză are număr, dată şi „perioada"** pentru care se aplică. Perioada e scrisă de
   agenţie pe decizie, deci **se tastează de pe hârtie, nu se calculează** — aplicaţia o poate doar
   **propune** din aniversare. Sunt aceleaşi câmpuri pe care art. 34¹ din OUG 92/2021 le cere în lista
   publică („seria şi data deciziei privind viza anuală").
3. **Data de expirare rămâne ca rubrică opţională**, pentru autorizaţiile integrate de dinainte de
   18.11.2019 nemodificate (Legea 219/2019 art. II alin. (3)). Nu se şterge.
4. **Lipsa vizei nu anulează autorizaţia de drept, în ziua aniversării** — duce la suspendare (activitate
   interzisă), apoi la anulare, fiecare printr-un act al agenţiei. Deci pe ecran e **avertisment**, nu
   verdict: „perioada vizei a trecut şi nu e trecută o viză nouă — verifică". Aceeaşi linie ca la AH.
5. **Termenul titularului** (60–90 de zile înainte de aniversare) e al **partenerului**, nu al clientului
   nostru. Devine termen propriu al clientului doar pe **autorizaţia firmei lui** (`Company`), dacă o are.


---

### 2.7 ARTICOLUL 29 alin. (2) — un document de transport pe care nu-l ştiam

*Găsit pe 10.09.2026, la o zi după ce s-a construit Anexa 2 la HG 1061/2008 pentru exact acelaşi
transport.*

> Transferul deşeurilor periculoase pe teritoriul naţional trebuie să fie însoţit de **documentul de
> identificare prevăzut în anexa IB la Regulamentul (CE) nr. 1.013/2006**, cu modificările şi
> completările ulterioare.

Şi alineatul de dinainte, care e obligaţia de ambalare-etichetare:

> **(1)** Producătorii şi deţinătorii de deşeuri sunt obligaţi să se asigure că pe durata efectuării
> operaţiunilor de colectare, transport şi stocare a deşeurilor periculoase, acestea sunt **ambalate
> şi etichetate** potrivit prevederilor Regulamentului (CE) nr. 1.272/2008 [CLP].

Iar temeiul HG-ului 1061/2008 e chiar în act, la **art. 27 alin. (3)**:

> Modalitatea de reglementare a transferului deşeurilor periculoase pe teritoriul naţional este
> reglementată prin hotărâre a Guvernului.

> 🟡 **Ce nu ştim, şi de ce nu ghicim.** „1013/2006" apare de **zero ori** în tot proiectul. Art. 29
> e în lista sancţionată cu **40.000–60.000 lei** (§2.4). Textul e neechivoc că documentul trebuie
> să însoţească transferul; ce **nu** se poate citi din act e dacă în practică documentul din anexa
> IB se suprapune peste formularul de expediţie din anexa 2 la HG 1061/2008 (acelaşi transport,
> acelaşi drum) sau se cumulează cu el.
>
> Asta e exact forma unei întrebări de practică, nu de text — deci **întrebarea AS**, în
> `intrebari-specialist.md`. Până la răspuns nu generăm nimic: a inventa un al doilea formular
> oficial e fix ce nu facem.

#### 🔴 11.09.2026 — regulamentul citat de art. 29 alin. (2) **e abrogat de aproape patru luni**

*Citit direct în textele oficiale ale Uniunii, prin **CELLAR** (depozitul Oficiului pentru
Publicaţii, `publications.europa.eu`), fiindcă `eur-lex.europa.eu` întoarce în sesiune o provocare
AWS WAF (`202` + JS) şi nu se poate citi nici cu `curl`, nici cu unealta de web.*

**Regulamentul (UE) 2024/1157, art. 85 — „Abrogare şi dispoziţii tranzitorii":**

> (1) Regulamentul (CE) nr. 1013/2006 **se abrogă de la 20 mai 2024**.
> (2) Cu toate acestea, dispoziţiile din Regulamentul (CE) nr. 1013/2006 **continuă să se aplice
> până la 21 mai 2026**, cu excepţia: (a) articolului 30 […]; (b) articolului 37, care continuă să
> se aplice până la 21 mai 2027; (c) articolului 51 […] până la 31 decembrie 2025.
> […]
> (8) **Trimiterile la Regulamentul (CE) nr. 1013/2006 care se abrogă se interpretează ca trimiteri
> la prezentul regulament** şi se citesc în conformitate cu tabelul de corespondenţă din anexa XIII.

Şi **art. 86 alin. (2)**: regulamentul nou **se aplică de la 21 mai 2026**. Fişa oficială a actului
vechi (CELEX `32006R1013`) poartă `END-OF-VALIDITY = 2027-05-21`, dar data aia e carve-out-ul de la
art. 85(2)(b) — **articolul 37**, nu regimul general.

**Ce urmează din asta, în ordine:**

| Ce | Unde scrie |
|---|---|
| Trimiterea din OUG 92/2021 art. 29(2) e la un act **ieşit din aplicare din 21.05.2026** | art. 85(1)–(2) din Reg. 2024/1157 |
| Se citeşte ca trimitere la regulamentul **nou**, prin tabelul de corespondenţă | art. 85(8) + anexa XIII |
| Documentul **îşi păstrează numărul**: „Anexele IA, IB şi IC → Anexele IA, IB şi IC" | anexa XIII, tabelul de corespondenţă |
| Anexa IB se numeşte, în chiar titlul ei, *„Document de circulaţie pentru circulaţia/transferurile **transfrontaliere** de deşeuri"* | anexa IB la Reg. 1013/2006, forma consolidată RO |
| **De la 21 mai 2026 documentele se transmit ELECTRONIC** (art. 27); hârtia rămâne doar pentru transferurile care implică ţări terţe | anexa IC la Reg. 2024/1157 |
| Transportul **exclusiv intern** rămâne al statului membru — fostul art. 33 e acum **art. 36** | art. 36 din Reg. 2024/1157 |

🟢 **Ce se închide din AS:** decizia „nu generăm nimic" nu mai stă pe o necunoscută, ci pe un motiv
citit în act — **documentul nu se mai tipăreşte deloc** după 21.05.2026, se depune electronic, şi e
prin propriul titlu un document transfrontalier. Un formular tipărit de noi ar fi un obiect care nu
mai există în dreptul Uniunii.

🟡 **Ce rămâne de întrebat, şi e altă întrebare decât cea veche:** pentru un transport **intern**,
autoritatea chiar aşteaptă documentul electronic de circulaţie, în condiţiile în care legea română
trimite la un regulament abrogat, iar regimul intern e lăsat de Uniune în seama statului membru
(adică HG 1061/2008, cu temei în art. 27 alin. (3))?

⚠️ **Şi lecţia, a treia de acelaşi fel în două zile:** *„cu modificările şi completările
ulterioare" dintr-o lege română nu spune că actul citat mai există.* Trimiterea îmbătrâneşte
singură, fără să se schimbe o literă în legea care o poartă. Se verifică la sursă, în fişa actului
citat, nu în actul care citează.

---

### 2.8 Obligaţii ale clientului pe care actul le cere şi dosarul de control nu le numeşte

*Toate citite pe 10.09.2026, toate în lista sancţionată de art. 62 alin. (1) lit. a) cu
**40.000–60.000 lei** pentru persoane juridice. Niciuna nu era pomenită în vreun document.* ✅ *De atunci dosarul le numește: `AuditFileService.otherObligationsNote()`, blocul „Alte obligații pe care le verifică inspectorul”.*

**Art. 17 alin. (3)** — colectarea separată, obligaţie a **oricărui** producător şi deţinător:

> Producătorii de deşeuri şi deţinătorii de deşeuri cu condiţia respectării prevederilor art. 16
> alin. (1) şi (4) introduc colectarea separată **cel puţin pentru hârtie, metal, plastic şi
> sticlă**, iar **până la data de 1 ianuarie 2025 şi pentru textile**.

**Art. 36 alin. (1)–(2)** — înregistrarea celor care nu se autorizează:

> **(1)** ANMAP ţine un registru cu următoarele tipuri de operatori economici, care nu se supun
> autorizării: a) operatorii economici care **transportă deşeuri nepericuloase în sistem
> profesional**; b) **comercianţii** care nu intră fizic în posesia deşeurilor sau **brokerii**.
> **(2)** Operatorii economici prevăzuţi la alin. (1) sunt obligaţi să se **înscrie în registrul**
> ţinut de ANMAP.

**Art. 8 alin. (4)** — caracterizarea deşeului periculos, care **restrânge întrebarea AL**:

> În scopul determinării posibilităţilor de amestecare, a metodelor de pregătire prealabilă,
> reciclare, valorificare şi eliminare a deşeurilor, producătorii şi deţinătorii de deşeuri persoane
> juridice sunt obligaţi să **efectueze şi să deţină o caracterizare a deşeurilor periculoase
> generate din propria activitate** şi a deşeurilor care pot fi considerate periculoase din cauza
> originii sau compoziţiei [...]

**Art. 8 alin. (2)** — codurile-oglindă:

> În cazul unui tip de deşeu care se încadrează [...] sub două coduri diferite în funcţie de posibila
> prezenţă a unor caracteristici periculoase - **codurile marcate cu asterisc**, încadrarea ca deşeu
> **nepericulos** se realizează [...] **numai în baza unei analize a originii, testelor, buletinelor
> de analiză** şi a altor documente relevante [...]

**Art. 27 alin. (1)** — şi ăsta e temeiul textual al întregului produs:

> [...] astfel încât să se poată asigura un grad ridicat de protecţie a mediului şi a sănătăţii
> populaţiei potrivit prevederilor art. 21, incluzând **asigurarea trasabilităţii de la locul de
> generare la destinaţia finală, pentru a îndeplini prevederile art. 48**.

> 📌 **Trei consecinţe.**
> 1. **AL se restrânge la jumătate.** Art. 8 alin. (4) spune că obligaţia e o caracterizare a
>    **deşeurilor generate din propria activitate**, iar scopul enumerat (amestecare, pregătire,
>    reciclare, valorificare, eliminare) e o proprietate a **tipului de deşeu**, nu a unei curse.
>    Deci „per ce" e în act: **per cod**, nu per transport. Ce rămâne de întrebat e doar **cât se
>    păstrează** — actul tace, şi aia chiar e practica inspectorului.
> 2. **Art. 8 alin. (2) e o regulă verificabilă, nu o notă.** Dacă un cod-oglindă e declarat
>    nepericulos fără buletin, încadrarea e nelegală prin construcţie. Aplicaţia ştie ambele lucruri
>    (codul are pereche cu asterisc; există sau nu un buletin ataşat — din 14.09.2026, orice document ataşat pe mişcare, fiindcă buletinele pe cod au ieşit: `WasteMovementMapper`) — deci poate să o spună.
>    ✅ **Construită pe 11.09.2026** (felia G-4): `V37` pune perechea în nomenclator, iar registrul
>    de mişcări poartă badge-ul „Cod-oglindă". Ce a ieşit din construcţie şi merită ştiut e mai jos,
>    la **§3.4**: perechea e derivabilă din numele oficial, dar **nu după o frază**.
> 3. **Art. 17 alin. (3) şi art. 36 alin. (2) sunt rubrici de dosar**, în acelaşi tipar cu persoana
>    desemnată de la art. 23 alin. (4): **absenţa unei obligaţii legale e ea însăşi constatarea.**
>    Nu cer ecrane noi, cer o propoziţie în dosarul de control. ✅ Construit: `AuditFileService.otherObligationsNote()`, punctele 1–2.

---

### 2.9 ARTICOLUL 44 alin. (3) — al patrulea termen anual: **31 mai** (găsit 11.09.2026)

*Şi de data asta articolul era deja transcris în fişierul ăsta — la §2.4, în lista de amenzi a
art. 62. Transcris, niciodată citit ca obligaţie. **A doua oară în două zile când un termen legal
stă ascuns într-un text pe care îl scrisesem noi.***

> **Art. 44 alin. (1):** Persoana juridică ce exercită o activitate de natură comercială sau
> industrială, **pentru care autoritatea competentă pentru protecţia mediului a emis o autorizaţie
> de mediu/autorizaţie integrată de mediu**, având în vedere rezultatele unui **audit de deşeuri**,
> este obligată să întocmească şi să implementeze un **program de prevenire şi reducere a
> cantităţilor de deşeuri** generate din activitatea proprie […] şi să adopte măsuri de reducere a
> periculozităţii deşeurilor.
>
> **(2)** Programul […] se poate elabora şi de către o terţă persoană/asociaţie profesională.
>
> **(3)** Programul […] **se publică pe pagina de internet a persoanei juridice** şi **se transmite
> anual agenţiei judeţene pentru protecţia mediului, inclusiv progresul înregistrat, până la 31 mai
> anul următor raportării**.

**Sancţiunea: art. 62 alin. (1) lit. a) — 40.000–60.000 lei** pentru persoane juridice. Alineatele
(1) **şi** (3) sunt amândouă în listă, verificate cuvânt cu cuvânt în enumerarea articolului: aceeaşi
cifră ca lipsa evidenţei, şi **de şase ori** cât termenul de 30 aprilie.

📌 **Semnalul e deja în profil, şi e pozitiv în sensul strict al regulii noastre.** Articolul nu
leagă obligaţia de o activitate pe care am deduce-o, ci de faptul că **autoritatea a emis firmei o
autorizaţie de mediu** — iar `Company.environmentalAuthNumber` e chiar rubrica aia. Nu se întreabă
nimic nou şi nu se derivă nimic din tăcere.

⚠️ **Alin. (3) cere DOUĂ lucruri, nu unul**, şi numai unul se poate păzi din aplicaţie: transmiterea
la agenţie e termen în calendar (`APM_ANNUAL_MAY`), iar **publicarea pe site-ul propriu** nu se
poate observa de aici — se numeşte în dosarul de control şi se predă clientului, nu se presupune.

⚠️ **Expirarea autorizaţiei nu se citeşte**, şi e o decizie: raportarea e a anului raportat, iar o
autorizaţie stinsă între timp nu şterge ce se datora cât a ţinut. A citi `environmentalAuthExpiry`
aici ar tăcea exact pentru clientul rămas în urmă.

**Unde e în cod:** `ReportType.APM_ANNUAL_MAY`, `DeadlineService.mayDeadline()`,
`AuditFileService.otherObligationsNote()` (azi punctul 4 din patru). **Fără migrare** — coloana
`report_type` e `VARCHAR(20)` şi valoarea încape, iar semnalul exista deja.

---

### 2.10 Două lucruri mărunte, din aceeaşi citire (11.09.2026)

**Art. 36 alin. (3)** — pe lângă cele trei categorii din alin. (1)–(2), se mai înscriu într-un
registru ţinut de ANMAP **operatorii economici care desfăşoară activităţi de reparare a
produselor**. ⚠️ **Alt registru şi, atenţie, în afara listei de la art. 62 alin. (1) lit. a)** —
deci se numeşte în dosar, dar nu sub cifra de 40.000–60.000 lei.

**Art. 48 alin. (2) şi (5)**, pentru jumătatea deschisă a lui **AL** (cât se păstrează buletinele
de analiză):

> **(2)** Producătorii şi deţinătorii de deşeuri periculoase sunt obligaţi **să deţină** buletinele
> de analiză care caracterizează deşeurile periculoase şi să le transmită, la cerere, autorităţilor.
>
> **(5)** Operatorii economici prevăzuţi la alin. (1) sunt obligaţi să păstreze evidenţa gestiunii
> deşeurilor **cel puţin 3 ani**, cu excepţia celor care desfăşoară activităţi de transport, care
> trebuie să păstreze evidenţa timp de cel puţin **12 luni**.

🟢 **Deci „cât se păstrează" nu mai e o întrebare pentru specialistă:** obligaţia de la alin. (2) e
**continuă şi fără termen** („să deţină"), iar podeaua celor 3 ani e scrisă în act. Ce rămâne de
practică e **frecvenţa reanalizei** — cât de des trebuie refăcut buletinul —, care e altă întrebare
decât cea din listă. ~~**G-7 se poate construi.**~~ ✅ **Construită pe 11.09.2026** (`V38`): buletinul
se leagă de **cod**, iar dosarul de control **constată per cod** — „Buletine încărcate: 1 din 3.
LIPSESC pentru: …" —, în loc să numească obligaţia şi să se oprească.
⚠️ **Tabela nu are dată de expirare, şi tocmai fiindcă întrebarea de mai sus e deschisă:** actul nu
dă niciun termen buletinului, iar a inventa unul ar însemna să-l tipărim într-un dosar citit de un
inspector. Când vine răspunsul la frecvenţa reanalizei, coloana se adaugă **aditiv**.
⚠️ *(Notă 19.09.2026, verificat în cod: buletinele s-au scos pe 14.09.2026, la sfatul specialistei — vezi nota de la §2.1 alin. (2).)*

⚠️ **Şi o lărgire a lui art. 8 alin. (4) pe care n-o citisem:** caracterizarea nu e cerută doar
pentru codurile cu asterisc, ci şi pentru *„deşeurile care **pot fi considerate periculoase din
cauza originii sau compoziţiei**"*. Perechea-oglindă de la §3.4 e un subset al obligaţiei, nu tot ea.

---

### 2.11 Ce depuneri se pot deduce din generări — căutarea din 16.09.2026

*Întrebarea proprietarului: „din ce generări ai, să-ți genereze automat ce trebuie să depui”. Citite pe 16.09.2026
pe Portalul Legislativ: OUG 92/2021 ([245846](https://legislatie.just.ro/Public/DetaliiDocument/245846)), Ordinul
794/2012 ([135672](https://legislatie.just.ro/Public/DetaliiDocument/135672)); plus ghidul SIM al ANMAP
([SIM.SD.GhidPublic.pdf](https://anmap.gov.ro/documents/27459/69882411/SIM.SD.GhidPublic.pdf)), pp. 6–8.*

**1. OUG 92/2021 nu s-a mai schimbat după 11.07.2026.** Forma consolidată are șapte date de modificare. Tabelul din
§2 nu avea una: **15-05-2025, Legea nr. 56 din 8 mai 2025** (M.Of. 433/12.05.2025), care modifică art. 66 alin. (1^2)
lit. c) — infracțiunile. Nu atinge nicio obligație de raportare.

**2. În tot actul, un producător sau deținător de deșeuri are doar trei depuneri cu dată fixă.** Am căutat „până la”, „anul următor”,
„anual” și „raportează” în tot textul:

| Termen | Articol | Cine, cuvânt cu cuvânt | Ce declanșează obligația |
|---|---|---|---|
| 15 martie | art. 48 alin. (1) | „Producătorii de deşeuri nepericuloase [...] producătorii de deşeuri periculoase [...] care colectează sau transportă [...]” | **calitatea** de producător: art. 3 pct. 24, „orice persoană ale cărei activităţi generează deşeuri” |
| 30 aprilie | art. 49 alin. (9) | titularii autorizațiilor de construire/desființare și „producătorii şi deţinătorii de uleiuri uzate” | autorizația, respectiv **uleiul uzat** |
| 31 mai | art. 44 alin. (3) | persoana juridică cu autorizație de mediu | autorizația |

Celelalte „anual” din art. 49 sunt ale ANMAP și ale ministerelor (alin. (1)–(7), (10), (13)), ale UAT-urilor și
operatorilor de salubritate (alin. (14)) sau ale sistemului de apărare (alin. (11), (16)). **Alin. (15)** spune doar
unde se depune: „Producătorii şi deţinătorii de deşeuri prevăzuţi la alin. (9), precum şi la art. 48 raportează datele
în registrul/registrele instituit/e de ANMAP.”

**3. Pentru 15 martie, legea nu spune ce face cine n-a generat nimic într-un an.** Obligația cade pe *producător*,
adică pe o calitate dată de activitate, nu pe cantitatea din anul raportat. În ghidul SIM, la p. 7, chestionarele sunt
„**alocate** spre completare” de ANMAP: „Proddes — este completat de operatorii economici generatori de deşeuri”. La
p. 8 scrie: „Daca chestionarele alocate nu corespund activitatii dvs, va rugam efectuati sesizari la adresa
suportsim@anpm.ro”. ⚠️ *(26.09.2026: adresa e **suportsim@anmap.gov.ro** din 01.06.2025, anunțată pe paginile DJM ale ANMAP, ex. [djmcs.anmap.gov.ro](https://djmcs.anmap.gov.ro/); domeniul anpm.ro n-are server de mail. Textul aplicației e corectat.)* Deci depunerea se leagă de chestionarul alocat firmei, nu de faptul că există generări. **Termenul de 15
martie rămâne pentru orice generator.** Întrebare de practică pentru specialistă: *cu chestionarul PRODDES alocat și
zero generări în an, se trimite gol sau se face sesizarea?*

**4. Ordinul 794/2012: Anexa 3 Ambalaje e o obligație a colectorului, cu termen, și calendarul nu o avea.**

> **Art. 4 alin. (1)** — Operatorii economici autorizaţi pentru desfăşurarea activităţii de colectare, reciclare sau
> valorificare a deşeurilor de ambalaje, comercianţii de deşeuri de ambalaje şi operatorii de salubritate care
> desfăşoară şi activitatea de colectare ca operator economic autorizat pentru colectarea deşeurilor de ambalaje sunt
> obligaţi să raporteze datele prevăzute în anexa nr. 3, tabelul 1 sau, după caz, tabelul 2, referitoare la ambalajele
> gestionate.
>
> **Alin. (4)** — Raportarea se realizează pentru fiecare punct de lucru în parte [...]
>
> **Art. 6** — Datele de raportare se transmit în format electronic ".xls" protejat împotriva modificării datelor şi
> pe suport hârtie, până cel târziu la data de **25 februarie** a fiecărui an pentru anul anterior [...]
>
> **Art. 8 alin. (3)** — Identificarea prin coduri a deşeurilor de ambalaje se face prin încadrarea în unul dintre
> următoarele coduri [...] **15 01** [...]

Aici chiar se poate deduce din mișcări. Un colector, reciclator, valorificator sau comerciant care a gestionat un cod
**15 01** în anul raportat datorează Anexa 3 până pe 25 februarie. `DeadlineService` crea atunci 25 februarie numai
pentru Anexa 1 (`PACKAGING_ANNUAL`, din rolul de piață). ✅ Construit în aceeași zi: `PACKAGING_ANNEX3`, numai la firmele cu
registrul art. 48 care au o preluare `15 01` în anul raportat (`DeadlineService.packagingWasteDeadline`).
⚠️ **Generatorul nu e în art. 4.** Anexa 3 la generator, cu ieșirile (16.09.2026), e o decizie a proprietarului, nu o
obligație din ordin. Din ea nu se deduce un termen.

**5. Ce nu se poate deduce din generări, și de ce:** Anexa 1 Ambalaje (marfa ambalată *pusă pe piață*, art. 1, nu
deșeul 15 01), notificarea AFM din 25 ianuarie (art. 3, îndeplinirea individuală a obiectivelor), 31 mai
(autorizația), contribuțiile AFM (bani, OUG 196/2005). HG 856/2002 nu are termen (§1.1).

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

### 3.4 Perechile-oglindă: **161** din 842, derivate din numele oficial (11.09.2026)

Art. 8 alin. (2) din OUG 92/2021 vorbeşte despre „un tip de deşeu care se încadrează sub două coduri
diferite". Modelul nostru n-avea perechea deloc — `WasteCode` ţine un `hazardous` izolat. Dar
**perechea e în chiar numele codului nepericulos**, aşa cum îl scrie Decizia 2014/955/UE:

```
17 05 04,pământ și pietre, altele decât cele specificate la 17 05 03,false
18 01 01,obiecte ascuțite (cu excepția 18 01 03),false
```

Deci nu trebuie cerută nimănui şi nu se tastează: se extrage o dată, la migrare (`V37`).

🔴 **Şi aici e ce merită ţinut minte, fiindcă e o lecţie de acelaşi fel cu celelalte din fişierul
ăsta: regula nu e a unei fraze, e a codului citat.** Nota care a cerut felia se sprijinea pe
formularea „**altele decât cele specificate la**" — 138 de rânduri în CSV. Numărate una câte una:

| Regula | Câte prinde | Ce scapă |
|---|---|---|
| fraza „altele decât cele specificate la" | **130** *(din 138: şapte sunt ele însele periculoase, iar `03 03 11` citează un cod nepericulos)* | 31 de oglinzi adevărate |
| „numele citează un cod periculos" | **161** | — |

Cele 31 în plus sunt scrise de act în alte patru feluri, pentru exact acelaşi lucru: „altele decât
cele **menţionate** la" (`01 04 08`), „**alte particule** decât cele specificate la" (`10 09 12`),
„(**cu excepţia** X)" (`16 06 04`, `18 01 01`, `18 02 01`) şi „**exclusiv** praful de cazan
specificat la" (`10 01 01`). **Toate 161 au fost citite pe rând: zero fals-pozitive.** Şi printre
cele 31 stau **obiectele ascuţite din cap. 18** — locul unde o încadrare greşită costă cel mai mult.

📌 **Filtrul „codul citat e periculos" îşi câştigă locul pe un singur rând din 842**, şi merită
numit: `03 03 11` (nămoluri de la epurarea efluenţilor) citează `03 03 10`, care e **nepericulos**.
Fără filtru, clientul ar fi primit un avertisment pentru o încadrare pe care actul n-o condiţionează
de nimic — adică fix alerta falsă pe care `V21` a costat o migrare s-o scoată.

⚠️ Jumătatea periculoasă a unei oglinzi **nu** e oglindă, oricât ar semăna numele (`16 01 21*`
poartă chiar fraza). Art. 8 alin. (2) condiţionează încadrarea *ca nepericulos*; cine a declarat
periculos n-are ce dovedi.

**Unde e în cod:** `V37__mirror_waste_codes.sql` (coloana `waste_codes.mirror_of`),
`WasteMovementMapper.mirrorClassificationUnproven`, proba `MirrorWasteCodeIT` (11 teste, dintre care
unul pinuieşte cifra 161 şi unul marginile de formulare).

---

## 4. HG 1061/2008 — transportul deșeurilor pe teritoriul României

Sursă: [legislatie.just.ro/Public/DetaliiDocument/97706](https://legislatie.just.ro/Public/DetaliiDocument/97706),
consolidare 23.01.2026. **În vigoare.** Accesat 22.08.2026.

> 🆕 **Recitit 15.09.2026, pentru modulul de depozit: transferul între punctele de lucru proprii.**
> **Art. 20 alin. (4):** *„Fiecare transport de deşeuri nepericuloase trebuie să fie însoţit de un
> formular”*. **Art. 23 alin. (1)** scoate de sub hotărâre doar *„transportul deşeurilor municipale,
> efectuat de către operatorii economici autorizaţi să presteze serviciul de salubrizare”*. **Art. 23
> alin. (2)** cere formularul și când operatorul își transportă propriile deșeuri nepericuloase.
> **Art. 1 alin. (3):** transportul se face numai către operatori cu autorizație de mediu.
> **Consecință:** un transfer între două depozite ale aceleiași firme **are Anexa 3**. Nu există
> scutire pentru rută internă.

> 🆕 **Recitit 26.09.2026 (forma din 23.01.2026, nemodificată de atunci; niciun proiect care s-o înlocuiască).**
> MMAP a confirmat pe 22.08.2025 că HG 1061/2008 e actul aplicabil transportului intern
> ([FAQ MMAP pct. 9](https://mmediu.ro/domenii/mediu/deseuri/faq-si-prezentari/30-cele-mai-frecvente-intrebari-privind-gestionarea-deseurilor-si-a-siturilor-contaminate/)).
> Trei lucruri care nu erau aici:
> - **art. 1 alin. (3)**, destinatarul trebuie să fie autorizat; încălcarea costă **15.000–30.000 lei** (art. 25 alin. (2)
>   lit. a)). Deci **un transfer spre un depozit fără autorizație valabilă nu se creează** (blocare, nu avertisment);
> - **art. 20 alin. (5)**: destinatarul înregistrează formularul primit *„într-un registru securizat, înseriat și
>   numerotat”*. E o obligație a oricărei recepții cu Anexa 3, nu doar a transferului, iar aplicația **nu are** încă un
>   asemenea registru;
> - **art. 4 alin. (11)** (deja în tabelul din §4.1): la periculoase procedura se aplică și când expeditorul,
>   transportatorul și destinatarul sunt același operator.
>
> **Rămâne deschis (întrebări la Andreea):** dacă evidența art. 48 și SIM se țin pe punct de lucru sau pe firmă (textul
> numește doar „unitățile și întreprinderile”) și ce cantitate se scrie pe formular când cântarul de la sosire diferă de
> cel de la plecare (Anexa 3 are o singură rubrică de cantitate). Până la răspuns: ieșire la A, intrare la B, marcate
> „transfer intern”; pe formular cantitatea de la sosire; diferența, rând de ajustare la A (D3.6).

> 🔁 **Runda 2, 26.09.2026** (note: `runda2_mediu.md`):
> - **Pe punct de lucru — da, în practică.** Legea tace („punct de lucru” apare de 0 ori în OUG 92/2021 art. 48–49 și HG
>   856/2002), dar tot ce e operațional e legat de punctul de lucru: contul SIM *„va fi asociat unui singur punct de lucru”*
>   (DJM/ANMAP), chestionarul COL/TRAT are bloc de identificare cu adresa, SIRUTA și coordonatele, autorizația se emite pe
>   amplasament, iar raportarea de ambalaje e *„pentru fiecare punct de lucru în parte”* (Ordinul 794/2012 art. 4 alin. (4)).
>   **Pentru cod:** evidența art. 48 și totalurile COL/TRAT se țin și se exportă **pe depozit**; vederea pe firmă e o sumă
>   din care se scad transferurile interne. La Andreea rămâne doar cum declară A un transfer spre B cu același CUI.
> - **„Tipizat, cu regim special” nu mai are conținut legal.** Tipărirea și înserierea cu regim special s-au oprit la
>   01.01.2007 (OMFP 2226/2006 art. 2); HG 831/1997 e abrogată (HG 105/2009); OMFP 2634/2015 nu mai cunoaște „regim special”
>   și admite formulare *„pretipărite sau editate cu ajutorul sistemelor informatice”* (pct. 22), cu numerotare internă
>   stabilită de firmă, eventual pe punct de lucru (pct. 24). **Și alin. (1) privește doar deșeurile nepericuloase
>   „destinate eliminării”**; transportul spre colectare/stocare/valorificare e la art. 21, fără „regim special”.
>   **Pentru cod:** Anexa 3 generată cu serie proprie pe depozit, numere secvențiale fără goluri, anulatele păstrate în
>   șir, needitabilă după emitere.
> - **Diferența de cântar la transfer (runda 2, `runda2_autorizatii_cantar.md`):**
>   - Anexa 3 are **o singură rubrică de cantitate**, completată de expeditor (art. 20 alin. (2)), plus date de încărcare /
>     descărcare și „Observații”; **nicio rubrică de cantitate primită**. Doar Anexa 2 (periculoase) are cantități predate,
>     primite, acceptate și respinse separat.
>   - Toleranța legală: HG 710/2015 anexa 1 tabelul 3, clasa III — ±0,5 e până la 500 e, ±1 e până la 2 000 e, ±1,5 e până
>     la 10 000 e; pct. 4.2 le **dublează în exploatare**. La ~10 t cu diviziunea de 20 kg, fiecare pod poate greși cu cel mult
>     ±20 kg, deci două poduri conforme diferă cu cel mult ~40 kg (80 kg dacă netul vine din două cântăriri). **180 kg nu e
>     toleranță, e diferență reală.**
>   - Contabil: diferența la recepție cere **NIR 14-3-1A** (verso „Diferențe (+/-)” și concluziile comisiei); transferul între
>     depozite cu adrese diferite merge cu **Aviz de însoțire a mărfii 14-3-6A** marcat „Fără factură” (bonul de transfer
>     14-3-3A e doar în aceeași incintă).
>   - Nicio îndrumare ANPM/ANMAP/MMAP despre ce greutate intră în evidența art. 48.
>   - **Pentru cod (deducție, nu text):** Anexa 3 poartă cantitatea expeditorului (10,00 t), B își notează 9,82 t la
>     „Observații”; în evidența art. 48, A are ieșire 10,00 t, B intrare 9,82 t; cele 0,18 t se închid prin NIR și decizia
>     comisiei; pragul de toleranță se calculează din clasa și diviziunea celor două cântare (registrul D2.3); diferențele în
>     ambele sensuri cer NIR. ~~Pe formular cantitatea de la sosire~~ (regula provizorie de mai sus se înlocuiește).
> - **„Registru securizat” nu e definit nicăieri.** eIDAS art. 46 interzice refuzul unui document doar pentru că e
>   electronic, dar „înseriat și numerotat pe fiecare pagină” descrie un registru paginat. **Pentru cod (D2.6):** jurnal
>   append-only pe depozit (corectura = rând nou), export PDF cu seria registrului și „Pagina X din Y”, tipăribil și
>   șnuruibil. La Andreea rămâne doar dacă APM/GNM cer hârtie tipografică.
> 🟠 **Neclar:** persoana fizică ce își aduce singură deșeul la centru. Actul n-o scutește explicit,
> dar nici n-o numește; practica nu cere formular. E întrebarea **AX** din `intrebari-specialist.md`.

**Cele trei anexe, reverificate pe 02.09.2026 la audit** — fiindcă aplicaţia trimite clientul la una
dintre ele într-un mesaj de eroare, deci numărul trebuie să fie corect:

| Anexă | Ce e | Îl generăm? |
|---|---|---|
| **nr. 1** | Formular pentru **aprobarea** transportului deşeurilor periculoase | ❌ nu |
| **nr. 2** | Formular de **expediţie/transport deşeuri periculoase** | ✅ da *(10.09.2026)*, doar la colectori din 14.09.2026 — model în §4.1 |
| **nr. 3** | Formular de **încărcare-descărcare deşeuri nepericuloase** | ✅ da |

> ✅ Mesajul `ANEXA3_HAZARDOUS_NOT_ALLOWED` trimite corect la anexa 2 pentru un cod periculos —
> iar de pe **10.09.2026** trimite la un formular care **există**: `Anexa2FormGenerator`, migrarea
> `V35`. Regulile şi modelul verbatim sunt în **§4.1** de mai jos.
>
> ⚠️ *(Notă 19.09.2026, verificat în cod: din 14.09.2026 Anexa 2 se tipăreşte **numai la colectori** —
> `ANEXA2_COLLECTORS_ONLY`, la sfatul specialistei. La un generator, butonul Anexa 3 nu apare pe un cod periculos
> (`canPrintAnexa3`), deci mesajul de mai sus, care trimite la „butonul Anexa 2”, se vede doar la un apel direct al API-ului.)*
>
> 🟠 ~~**Dar nu punem nimic în loc.**~~ *(adevărat până pe 10.09.2026, păstrat fiindcă spune de ce
> felia era cea mai valoroasă rămasă)* Un generator obişnuit *are* coduri periculoase — un service
> auto are ulei uzat (`13 02 xx*`) şi filtre, o clinică are `18 01 03*`, un birou are tuburi
> fluorescente (`20 01 21*`). Pentru ei ţineam fişa corect şi nu puteam tipări niciun document de
> transport.
>
> ⬜ **Ce tot nu generăm, şi nu e o scăpare:** **aprobarea prealabilă** din anexa nr. 1, cerută de
> art. 7 peste 1 t/an — o procedură la APM, cu 7 zile lucrătoare (art. 4 alin. (5)), nu o pagină.
> Numărul ei se tastează pe formular, iar ecranul spune ce mai lipseşte când bifa cade peste prag.
> ⛔ Şi fluxul art. 24 al **deşeurilor medicale**, care e al transportatorului. Vezi
> `istoric/audit-conformitate.md` pct. 6.

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
   Bragadiru”). De aceea partenerul are sediul și punctele lui de lucru (`PartnerWorkPoint`, `V23`).

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


### 4.1 ANEXA Nr. 2 — formularul de expediţie/transport deşeuri periculoase (citit pe 10.09.2026)

Sursă: aceeaşi, [DetaliiDocument/97706](https://legislatie.just.ro/Public/DetaliiDocument/97706),
**forma consolidată la 23.01.2026**. Actul reproduce formularul **în facsimil**, ca HG 856/2002 la
fişă — deci forma nu trebuia cerută de la nimeni, şi asta a închis întrebarea **AI**.

⚠️ **Lecţia care a costat o zi:** pe 10.09 dimineaţa, din **exemplarul completat** (TEMPO PAM,
13.01.2022, în repo-ul privat) s-a dedus că formularul îl emite colectorul, fiindcă purta numărul
1047. **Art. 8 spune contrariul, textual.** Un exemplar completat arată **cum se completează în
practică** — e preţios exact pentru asta — dar **nu spune cine e obligat**, şi poate fi neconform cu
modelul: celui primit îi lipsesc trei rubrici (tipul mijloacelor de transport, ambalajele,
observaţiile) şi scurtează legenda semnăturii destinatarului. **Se construieşte după act.**

#### Regulile, pe articole

| Ce | Unde | Text |
|---|---|---|
| **Cine completează** | **art. 8** | „**Expeditorul** completează, semnează şi ştampilează formularul de expediţie/transport deşeuri periculoase" |
| Cine e expeditorul | art. 4 alin. (1) | „de la generator sau deţinător, denumit în continuare **expeditor**" |
| Transportatorul | art. 9 alin. (1) | „**semnează şi ştampilează** […] la primirea deşeurilor" |
| Destinatarul | art. 10 alin. (1) | „la primirea deşeurilor […] **semnează şi ştampilează** […] confirmând acceptarea" |
| **Sub 1 t/an: fără aprobare** | **art. 6 alin. (1)** | formularul „**nu trebuie să conţină aprobarea** agenţiei pentru protecţia mediului" |
| Sub 1 t/an: ce se scrie | art. 15 alin. (1) | „se **precizează clar**" că deşeurile sunt generate în cantitate mai mică de 1 t/an |
| **Sub 1 t/an: 3 exemplare** | **art. 15 alin. (2)** | o copie la expeditor, una la destinatar, una la transportator |
| Sub 1 t/an, totuşi la APM | **art. 6 alin. (2)** | destinatarul depune un exemplar la agenţie — cele „3 exemplare" spun cine **păstrează**, nu cine **primeşte** |
| Peste 1 t/an | art. 7 | transportul e însoţit de formularul de expediţie **şi de formularul pentru aprobare** (anexa nr. 1) |
| **Peste 1 t/an: 6 exemplare** | art. 12 + art. 4 alin. (10) | expeditor, destinatar, transportator, APM care a aprobat, **ISU** al judeţului expeditorului, APM al judeţului expeditorului |
| Termen de aprobare | art. 4 alin. (5) | APM judeţean, **7 zile lucrătoare** |
| Valabilitatea aprobării | art. 5 | un transport, sau mai multe — atunci **2 ani** |
| Şi când e aceeaşi firmă | art. 4 alin. (11) | regulile se aplică şi dacă expeditor, transportator şi destinatar sunt **acelaşi operator** |
| Proba păstrată | art. 10 alin. (2) | destinatarul prelevează o probă din fiecare transport, **cel puţin 3 luni** |
| După operaţie | art. 13 | destinatarul semnează din nou, copie la expeditor + APM-ul expeditorului + APM-ul instalaţiei |
| Formularul aprobat, la **ISU** | **art. 4 alin. (8) lit. b)** | îl duce **expeditorul**, la ISU-ul judeţului său, „pentru **autorizarea rutei** transportului" |
| Notificarea de **48 de ore** | art. 14 alin. (1) | o face **ISU-ul**, nu clientul: anunţă celelalte ISU-uri judeţene şi inspectoratele de poliţie de pe traseu |
| ⛔ **Deşeurile medicale** | **art. 24** | **transportatorul** — „chiar dacă acesta este şi destinatar" — întocmeşte formularele, cu „cantitatea cumulată […] pe un transport dintr-o anumită zonă" şi „o anexă cu expeditorii şi cantităţile individuale" |
| Sancţiuni | art. 25 alin. (2) | lit. a) **art. 1 alin. (3)** (destinatar fără autorizație) şi **art. 19**: **15.000–30.000 lei** · lit. b) art. 4, 7, **8**, 14(3)–(5), **20** şi 21: **10.000–20.000 lei** · lit. c) art. 9, 10, 12, 15(2), 18(3), 22, 23(2): **5.000–10.000 lei** *(lit. a) şi lista completă adăugate 26.09.2026)* |

⚠️ **Art. 2 trimite definiţiile la „anexa nr. I A la OUG nr. 78/2000" — act abrogat.** Deci
„**aceeaşi categorie** de deşeuri periculoase", cuvântul pe care stă pragul de 1 t/an (art. 5, 7,
15(1)), **nu e definit nicăieri**: lanţul de definiţii e rupt. Riscul e asimetric — cumulul pe cod
subestimează dacă „categorie" e mai larg, iar subestimarea sare peste aprobare (art. 7 → amenda de
la lit. b); supraestimarea cere doar o aprobare în plus. Regula proiectului: **unde actul tace,
câmpul se propune, nu se impune.**

⚠️ Singura schimbare din 2026: **art. 25 alin. (3) a fost anulat** — confiscarea mijlocului de
transport (Sentinţa nr. 225/F-CONT/27.09.2024, Curtea de Apel Piteşti, M. Of. nr. 51/23.01.2026).
**Nu atinge formularele**; e doar motivul pentru care consolidarea poartă data 23.01.2026.

#### Modelul, verbatim din act

```
             Formular de expediție/transport deșeuri periculoase

                                nr.*1) |_|_|_|_|_|


                                    - model -

Denumirea deșeurilor periculoase*2)   |_|_|_|_|_|_|_|_|_|_|
Cod deșeuri periculoase               Deșeuri periculoase < 1t/an               Nr. formularului de aprobare
|_|_|_|_|_|                           |_|                                       al transportului*)
                                      Deșeuri periculoase > 1t/an               |_|_|_|_|_|
                                      |_|
Nr. de înregistrare al expeditorului  Nr. de înregistrare al transportatorului  Nr. de înregistrare al destinatarului
|_|_|_|_|_|                           |_|_|_|_|_|                               |_|_|_|_|_|
În calitate de:                       În calitate de:                           În calitate de operator
Generator |_|                         Nume delegat:                             economic care realizează
Operator economic care                Nr. de înmatriculare mijloc               operația de:
realizează operația de:               transport:                                Colectare |_|
Colectare  |_|                                                                  Stocare temporară |_|
Stocare temporară |_|                                                           Tratare |_|
                                                                                Valorificare |_|
                                                                                Eliminare |_|
Cantitatea predată în tone            Cantitatea primită în tone                Cantitatea recepționată în tone
|_|_|_|_|_|                           |_|_|_|_|_|                               |_|_|_|_|_|
                                                                                Cantitatea respinsă în tone
                                                                                |_|_|_|_|_|
Data predării (zi, lună, an)          Data predării (zi, lună, an)              Data primirii (zi, lună, an)
|_|_|_|_|_|_|                         |_|_|_|_|_|_|                             |_|_|_|_|_|_|
Denumirea societății, sediul,         Denumirea societății, sediul,             Denumirea societății, sediul,
cod unic de identificare              cod unic de identificare                  cod unic de identificare
|_|_|_|_|_|                           |_|_|_|_|_|                               |_|_|_|_|_|
Semnatura și ștampila                 Semnatura și ștampila                     Semnatura și ștampila
(asigurare pentru o declarație        (asigurare pentru transport               (asigurare pentru preluare
corectă)                              regulamentar)                             în vederea unei colectări sau
 .............................         .......................                   stocări temporare, tratare/
                                                                                valorificare/eliminare
                                                                                conform prevederilor legale)
                                                                                .............................
 Tipul mijloacelor de transport:
Numar și tip de ambalaje utilizate pentru transportul deșeurilor periculoase:
Observații:
___________
*1) Număr înscris de către agenția județeană pentru protecția mediului.
*2) Conform Hotărârii Guvernului nr. 856/2002 privind evidența gestiunii deșeurilor și pentru aprobarea listei cuprinzand deșeurile, inclusiv deșeurile periculoase, cu completările ulterioare.
        Notă
        *) Nu este necesară aprobare pentru cantităţi < 1t/an.
```

**Cele două note ale modelului sunt cele care decid două câmpuri din aplicaţie:**

* `*1)` — „**Număr înscris de către agenţia judeţeană pentru protecţia mediului**". 🔴 Deci numărul
  formularului **nu se alocă de noi**, spre deosebire de `anexa3Number` (`max+1` pe firmă). Câmp
  tastat, gol implicit: altfel tipărim un număr inventat pe un formular oficial.
* `*2)` — denumirile vin din **HG 856/2002**, nomenclatorul pe care îl avem; asteriscul stă **numai
  în coloana de cod** (§1, `WasteCodeLabel.official`).

**„Nr. de înregistrare" ×3 = codul unic de înregistrare.** Anexa 2 nu-l defineşte, dar blocul de
identificare de dedesubt scrie de trei ori „cod unic de identificare", pentru aceleaşi trei părţi.
Se ia din `Company.cui` şi `Partner.cui`; niciun câmp nou.

**Patru rubrici se tipăresc goale, deliberat:** `Cantitatea primită`, `Cantitatea recepţionată`,
`Cantitatea respinsă` şi `Data primirii`. Expeditorul completează formularul (art. 8), dar acelea
sunt declaraţiile transportatorului şi destinatarului, care semnează **la primirea** deşeurilor
(art. 9(1), art. 10(1)). Exemplarul primit le are completate fiindcă a fost umplut **după** transport;
dacă l-am copia, am tipări declaraţia altcuiva. Precedentul e la Anexa 3 — cantitatea cântărită la
descărcare e rubrică goală.

#### Cum se citeşte pagina, dacă trebuie recitită

`curl` simplu întoarce **`403`**; cu un `User-Agent` de browser vine pagina întreagă, iar anexele
**nu** sunt trunchiate (spre deosebire de OUG 196/2005 — §10.2). Textul stă în `<span class="S_PAR">`
cu spaţierea coloanelor păstrată, deci facsimilul se reface tăind **doar tagurile cunoscute**:

```bash
curl -s -A "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 Chrome/126.0" \
  "https://legislatie.just.ro/Public/DetaliiDocument/97706" -o hg1061.html
```

⚠️ **Capcana, şi e exact pe rubrica pragului:** un `re.sub(r'<[^>]+>', '', …)` **mănâncă
`< 1t/an`** — `<` deschide un tag fals şi înghite tot până la următorul `>`. Se pierd caseta
„Deşeuri periculoase < 1t/an |_|", titlul „Nr. formularului de aprobare al transportului*)" şi
coada notei `*)`. Prima extragere de pe 10.09 a păţit-o, şi a arătat ca o trunchiere a portalului.
Se taie cu o listă de taguri (`</?(?:span|a|br|p|div|sup|b|i|font|table|tr|td|tbody)\b[^>]*>|<!--.*?-->`),
nu cu `<[^>]+>`.

---


### 4.2 ANEXA Nr. 1 — formularul pentru aprobarea transportului (citit pe 10.09.2026)

Cerut de **art. 7** pentru fiecare transport de peste 1 t/an din aceeaşi categorie. **Nu îl
generăm** — dar modelul lui e tot în facsimil în act, deci se poate construi oricând, iar ce
împiedică azi nu e informaţia, ci datele. Notat aici ca să nu fie recitit a treia oară.

#### Drumul hârtiei, pe articole — şi nu e ce pare

| Pasul | Cine | Unde scrie |
|---|---|---|
| 1. completează formularul | **expeditorul** (clientul nostru) | art. 4 alin. (2) |
| 2. îl trimite destinatarului, semnat şi ştampilat | expeditorul | art. 4 alin. (3) |
| 3. acceptă sau cere lămuriri; dacă acceptă, semnează şi ştampilează | destinatarul | art. 4 alin. (4) |
| 4. merge la **APM-ul din raza instalaţiei destinatarului** — nu a expeditorului | destinatarul | art. 4 alin. (5) |
| 5. **7 zile lucrătoare** de la primirea tuturor informaţiilor | APM | art. 4 alin. (5) |
| 6. semnează şi ştampilează, după ce verifică operaţiile şi regimul ariilor protejate / Natura 2000 | APM | art. 4 alin. (7) |
| 7. îl trimite înapoi expeditorului **şi** APM-ului expeditorului | APM-ul instalaţiei | art. 4 alin. (8) lit. a) |
| 8. **îl duce la ISU-ul judeţului său, pentru autorizarea rutei** | **expeditorul** | art. 4 alin. (8) lit. b) |
| 9. anunţă, cu **48 de ore** înainte, celelalte ISU-uri şi poliţiile de pe traseu | **ISU-ul**, nu clientul | art. 14 alin. (1) |

🔴 **Pasul 9 a fost scris greşit în prima variantă a documentaţiei de azi** — ca o obligaţie a
clientului, „notificarea ISU cu 48 de ore", şi ajunsese şi într-un text de pe ecran. Art. 14 alin. (1)
spune contrariul: notificarea o face **inspectoratul**, după ce primeşte formularele. Obligaţia
clientului e pasul 8, care e alt lucru şi în alt moment. Aceeaşi lecţie ca dimineaţă, a treia oară
în aceeaşi zi: **citeşte articolul, nu rezumatul lui.**

⚠️ Valabilitatea: art. 5 — o aprobare pe **un** transport, sau pe mai multe, şi atunci **2 ani** de
la acordare.

#### De ce nu e o felie mică: pct. VIII

Modelul are opt secţiuni. I–IV (expeditor, transportator, destinatar, amplasamentul instalaţiei),
V (ce se aprobă: un transport sau mai multe, plus cele cinci operaţii), VI (numărul de transporturi
planificate) şi VII (cantitatea planificată, **în tone**) se completează din modelul nostru de date,
aproape rubrică-la-rubrică cu Anexa 2.

**Pct. VIII cere compoziţia chimică a deşeului: 38 de parametri numerotaţi** — arseniu, plumb,
cadmiu, crom-VI, cupru, nichel, mercur, seleniu, staniu, stibiu, taliu, telur, sulfuri, cloruri,
fluoruri, bromuri, ioduri, nitriţi, fenoli, PCB/PCT, CCO, pH, conductibilitate, punct de topire, de
inflamabilitate, putere calorică ş.a.m.d. — plus 10 rânduri libere de „alţi parametri".

🔴 **Aia nu e o rubrică de completat, e un buletin de analiză.** Nu-l putem calcula, nu-l putem
deduce şi nu-l putem inventa. Deci felia „formularul de aprobare" **stă pe** întrebarea **AL**
(buletinele de analiză, OUG 92/2021 art. 48 alin. (2)) — iar pct. VIII e chiar răspunsul parţial la
ea: actul spune **ce parametri**, nu spune per ce se face buletinul şi cât se ţine.

⚠️ **Şi aici lanţul de definiţii e rupt, a doua oară în acelaşi act:** pct. V lit. C trimite
operaţia la *„anexele nr. IA, II A şi, respectiv, II B din Ordonanţa de urgenţă a Guvernului
nr. 78/2000"* — abrogată. Codurile R/D şi-au păstrat numerotarea prin Legea 211/2011 şi OUG 92/2021,
deci se tipăreşte codul pe care îl avem deja pe mişcare; e o decizie a noastră, scrisă, nu o
întrebare.

#### Modelul, verbatim din act

```
Formular pentru aprobarea transportului deșeurilor periculoase
          nr.*1) |_|_|_|_|_|
___________
  *1) Număr înscris de agenția județeană pentru protecția mediului în a cărei rază teritorială se află instalația de tratare/valorificare/eliminare.
               - model -


    I. Date privind expeditorul/generatorul deșeurilor periculoase
    Societatea ...................................................
    Sediul .......... nr. ........, cod poștal ........, localitatea ........
    Persoana responsabilă ...................................................
    Telefon ................. fax ............. e-mail ......................
    Autorizație de mediu nr. |_|_|_|_|_|
    Data la care expiră autorizația de mediu |_|_|_|_|_|_|
    Cod unic de înregistrare |_|_|_|_|_|_|

    II. Date privind transportatorul deșeurilor periculoase
    Societatea .............................................................
    Sediul ........... nr. ......., cod poștal ......., localitatea ........
    Persoana responsabilă ..................................................
    Telefon .................. fax ............. e-mail ....................
    Licența de transport mărfuri periculoase nr. .........
    Data la care expiră licența de transport mărfuri periculoase ...........
    Autorizație de mediu nr. |_|_|_|_|_|
    Data la care expiră autorizația de mediu |_|_|_|_|_|_|
    Cod unic de înregistrare |_|_|_|_|_|_|
    Nr. de înmatriculare mijloc de transport ...............................
    Delegat (nume, prenume, funcție) .......................................

    III. Date privind destinatarul deșeurilor periculoase
    Societatea .............................................................
    Sediul ......... nr. ........, cod poștal ........., localitatea .......
    Persoana responsabilă ..................................................
    Telefon ................... fax ............. e-mail ...................
    Cod unic de înregistrare |_|_|_|_|_|_|

    IV. Date privind amplasamentul instalației de tratare/valorificare/eliminare
    Denumirea ...............................................................
    Sediul ......... nr. ......., cod poștal ......., localitatea ...........
    Persoana responsabilă ...................................................
    Telefon .................... fax............. e-mail ....................
    Autorizație de mediu nr. |_|_|_|_|_|
    Data la care expiră autorizația de mediu |_|_|_|_|_|_|

    V. Aprobare pentru:

    A. (i) un singur transport                                |_|
    (îi) mai multe transporturi                               |_|
    Data la care expiră aprobarea                       |_|_|_|_|_|_|

    B. (i) colectare                                          |_|
    (îi) stocare temporară                                    |_|
    (iii) tratare                                             |_|
    (iv) valorificare                                         |_|
    (v) eliminare                                             |_|

    C. Operația de colectare/stocare temporară/tratare/valorificare/eliminare
    nr. ....... (conform anexelor nr. IA, II A și, respectiv, II B din
    Ordonanța de urgență a Guvernului nr. 78/2000 privind regimul deșeurilor,
    aprobată cu modificări și completări prin Legea nr. 426/2001, cu
    modificările și completările ulterioare)

   VI. Numărul total de transporturi planificate:

   VII. Cantitatea de deșeuri planificată a fi transportată (în tone)
                                                               |_|_|_|_|_| t

   VIII. Denumirea și compoziția chimică a deșeurilor:

   - Denumirea deșeurilor (conform Hotărârii Guvernului nr. 856/2002 privind
   evidența gestiunii deșeurilor și pentru aprobarea listei cuprinzând
   deșeurile, inclusiv deșeurile periculoase, cu completările ulterioare)
                                                               |_|_|_|_|_|
   - Codul deșeurilor (conform Hotărârii Guvernului nr. 856/2002,
     cu completările ulterioare)                               |_|_|_|_|_|
   - Conținutul deșeurilor în:

    1. Arseniu                                       |_|_|_|_|_|      mg/l
    2. Plumb                                         |_|_|_|_|_|      mg/l
    3. Cadmiu                                        |_|_|_|_|_|      mg/l
    4. Crom-Vl                                       |_|_|_|_|_|      mg/l
    5. Cupru                                         |_|_|_|_|_|      mg/l
    6. Nichel                                        |_|_|_|_|_|      mg/l
    7. Mercur                                        |_|_|_|_|_|      mg/l
    8. Seleniu                                       |_|_|_|_|_|      mg/l
    9. Staniu                                        |_|_|_|_|_|      mg/l
    10. Stibiu                                       |_|_|_|_|_|      mg/l
    11. Taliu                                        |_|_|_|_|_|      mg/l
    12. Telur                                        |_|_|_|_|_|      mg/l
    13. Sulfuri                                      |_|_|_|_|_|      mg/l
    14. Cloruri                                      |_|_|_|_|_|      mg/l
    15. Fluoruri                                     |_|_|_|_|_|      mg/l
    16. Bromuri                                      |_|_|_|_|_|      Greu %
    17. Ioduri                                       |_|_|_|_|_|      Greu %
    18. Nitriti                                      |_|_|_|_|_|      Greu %
    19. Fenoli                                       |_|_|_|_|_|      mg/l
    20. PCB/PCT                                      |_|_|_|_|_|      mg/l
    21. Consum Chimic de Oxigen (CCO)                |_|_|_|_|_|      mg/l
    22. Valoare pH                                   |_|_|_|_|_|      mg/l
    23. Conductibilitate                             |_|_|_|_|_|      S/cm
    24. Materiale lipofile greu volatile             |_|_|_|_|_|      mg/l
    25. Componența extractibilă a substanței de bază |_|_|_|_|_|      Greu %
    26. Materiale lipofile extractibile              |_|_|_|_|_|      Greu %
    27. Pierderi la calcinare                        |_|_|_|_|_|      Greu %
    28. Componența dizolvabilă în apă                |_|_|_|_|_|      Greu %
    29. Conținut de apă                              |_|_|_|_|_|      %
    30. Rezistența la forfecare                      |_|_|_|_|_|      kN/m"
    31. Deformare axială                             |_|_|_|_|_|      %
    32. Rezistența la presare uniaxială              |_|_|_|_|_|      kN/m"
    33. Punct de topire                              |_|_|_|_|_|      °C
    34. Punct de inflamabilitate                     |_|_|_|_|_|      °C
    35. Punct de condensare/domeniu de condens       |_|_|_|_|_|      °C
    36. Putere calorică                              |_|_|_|_|_|      kJ/k
    37. Emisie de gaze prin reacții ulterioare       |_|_|_|_|_|
        a) în contact cu ambalajul                   |_|_|_|_|_|
        b) în contact cu aerul                       |_|_|_|_|_|
        c) în contact cu roca de sare                |_|_|_|_|_|
        d) la temperatura de °C ...............      |_|_|_|_|_|
    38. Menționarea componentelor periculoase        |_|_|_|_|_|
        a) ale deșeurilor                            |_|_|_|_|_|
        b) ale produselor rezultate din              |_|_|_|_|_|
           valorificarea/eliminarea deșeurilor

        Alți parametri                               valoare     dimensiune
    39. |_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|        |_|_|_|_|_|   |_|_|_|_|_|
    40. |_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|        |_|_|_|_|_|   |_|_|_|_|_|
    41. |_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|        |_|_|_|_|_|   |_|_|_|_|_|
    42. |_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|        |_|_|_|_|_|   |_|_|_|_|_|
    43. |_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|        |_|_|_|_|_|   |_|_|_|_|_|
    44. |_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|        |_|_|_|_|_|   |_|_|_|_|_|
    45. |_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|        |_|_|_|_|_|   |_|_|_|_|_|
    46. |_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|        |_|_|_|_|_|   |_|_|_|_|_|
    47. |_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|        |_|_|_|_|_|   |_|_|_|_|_|
    48. |_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|        |_|_|_|_|_|   |_|_|_|_|_|

    49. Alte mențiuni                 |_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|

    NOTĂ: La completarea pct. 38-49 se au în vedere prevederile art. 8 din
    Hotărârea Guvernului nr. 856/2002, cu completările ulterioare.

    IX. Modalități de transport |_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|
    X. Tipuri de ambalare |_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|
    XI. Declarația expeditorului/generatorului:

    Declar pe propria răspundere că datele și informațiile de mai sus sunt
    complete și corecte. Declar, de asemenea, că îmi asum obligațiile
    contractuale stabilite conform legii.

         Nume            Semnatura/Ștampila             Data
    ................    ......................     ...............

    XII. Acceptarea transportului de către destinatar
    Suntem de acord să preluăm deșeurile declarate.
    Asigurăm că deșeurile vor fi tratate/valorificate/eliminate în
    instalația noastră, conform legii.

         Nume            Semnatura/Ștampila             Data
    ................    ......................     ...............

    XIII. Autorizarea transportului deșeurilor de către agenția județeană
    pentru protecția mediului în a cărei rază teritorială are loc colectarea/
    stocarea temporară/tratarea/valorificarea/eliminarea deșeurilor.
    Agenția pentru Protecția Mediului (județul) autorizează

         Nume            Semnatura/Ștampila             Data
    ................    ......................     ...............
     Condiții specifice |_|  nu   |_| da

    Dacă se impun condiții specifice, acestea se trec pe verso.
    XIV. Autorizarea rutei de transport de către Inspectoratul pentru
    Situații de Urgență
    Pentru transportul deșeurilor periculoase trebuie utilizată următoarea
    rută: ............................
    Dacă această rută nu poate fi utilizată din motive obiective, va fi
    utilizată urmatoarea rută:
Nume
.............
Semnatura/Ștampila
.......................
Data
...............
```

Nota `*1)` a acestui formular e **altfel** decât a Anexei 2: numărul îl înscrie „agenţia judeţeană
pentru protecţia mediului **în a cărei rază teritorială se află instalaţia** de
tratare/valorificare/eliminare" — adică a destinatarului, nu a noastră. Acelaşi principiu: nu-l
alocăm noi.

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
> | În aplicație | G5, livrat | modulul de ambalaje *(nescris la 22.08; construit din 25.08, azi tabul „Ambalaje” din Generare)* |
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
publică. De aici vine `PackagingDeclarationXlsGenerator` (până pe 04.09.2026 `…XlsxGenerator`, vezi §5.4), iar PDF-ul rămâne pentru dosarul de
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
nimeni dacă a transferat obligaţiile), dar e o întrebare de pus. *(Notă 19.09.2026, verificat în cod: profilul tot nu întreabă, dar
explicaţia termenului spune regula — cu un OIREP nu se depune Anexa 1, `strings.ts`.)*

#### 2. Formatul: `.xls` **protejat**, şi **pe hârtie**

> **Art. 6** — „Datele de raportare se transmit în format electronic „.xls" **protejat împotriva
> modificării datelor** şi **pe suport hârtie**, până cel târziu la data de 25 februarie a fiecărui
> an pentru anul anterior celui pentru care se realizează raportarea."

Două consecinţe directe: foile generate sunt **protejate** (parolă goală, ca să poată fi ridicată —
vezi `PackagingDeclarationXlsGenerator`), iar **PDF-ul nu e un moft**: e exemplarul pe hârtie.
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
✅ **Făcută pe 04.09.2026:** `HSSFWorkbook`, `.xls` BIFF8 — `PackagingDeclarationXlsGenerator` şi, din 16.09,
`PackagingAnexa3XlsGenerator`.

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
- ~~**Date per tranzacţie (art. 18 alin. 2):** [...] coordonate GPS ale punctului de lucru, fotografii (2–4 unghiuri la
  ambalaje).~~ **Greșit — corectat mai jos, 26.09.2026.**
- ~~**Sancţiune:** suspendarea accesului.~~ **Incomplet — amenzile sunt în OUG 196/2005, mai jos.**

### 6.1 Recitit integral pe 26.09.2026 (Ordinul și Instrucțiunile, nemodificate)

Pe Portal, „Acțiuni suferite” răspunde *„Nu exista actiuni suferite de acest act”* atât pentru Ordin (281611), cât și
pentru Instrucțiuni (281612). Nu există ordin SIATD mai nou; 1595/2020 e abrogat. siatd.afm.ro funcționează (HTTP 200,
26.09.2026) și nu s-a găsit nicio amânare.

**Termenele, art. 18 alin. (6)–(10), verbatim pe scurt:**
- alin. (6), ambalaje: cine preia *„în vederea valorificării/reciclării/tratării/**revânzării**”* confirmă *„într-un termen
  de maximum 5 zile **de la data recepției deșeurilor**”*;
- alin. (7), DEEE și baterii portabile: 15 zile; alin. (8), anvelope: 5 zile;
- alin. (9), municipale: 3 zile, **numai** pentru cine preia *„în vederea sortării/tratării/compostării/digestării/depozitării”*
  — colectarea pură nu e numită;
- alin. (10): dacă recepția a fost înainte de înrolare, termenul curge **de la înrolare**.

~~Textul spune „zile”, fără „lucrătoare” [...] → zile calendaristice, fără prelungire la weekend (conservator).~~
🔁 **Runda 2, 26.09.2026 — calculul e al Codului civil.** Nici Instrucțiunile, nici OUG 196/2005 n-au o regulă proprie, deci
se aplică dreptul comun ([Codul civil, 109884](https://legislatie.just.ro/Public/DetaliiDocument/109884), consolidare 03.05.2026):
> **Art. 2.551:** Durata termenelor, fără deosebire de natura și izvorul lor, se calculează potrivit regulilor stabilite de
> prezentul titlu.
> **Art. 2.553 (1)–(2):** Când termenul se stabilește pe zile, nu se ia în calcul prima și ultima zi a termenului. Termenul
> se va împlini la ora 24,00 a ultimei zile.
> **Art. 2.554:** Dacă ultima zi a termenului este o zi nelucrătoare, termenul se consideră împlinit la sfârșitul primei
> zile lucrătoare care îi urmează.

Codul de procedură civilă art. 181–182 dă aceeași dată; Codul de procedură fiscală art. 75 trimite la el; OG 2/2001 și
Codul administrativ n-au regulă proprie. **Formula:** termen = prima zi lucrătoare ≥ (data recepției sau a înrolării + N + 1),
ora 24:00; zilele nelucrătoare = weekend + sărbătorile legale (Codul muncii art. 139). **Exemple de probă:** recepție vineri
02.10.2026 → 5 zile: **joi 08.10**; 3 zile: marți 06.10; 15 zile: duminică 18.10 → **luni 19.10**; recepție duminică 04.10,
5 zile: sâmbătă 10.10 → **luni 12.10**. Prudență: ÎCCJ (RIL 8/2024) a înlăturat art. 2.553 alin. (1) la preavizul din Codul
muncii, deci mementoul aplicației vine cu o zi înainte (recepție + N). La AFM rămâne doar cum numără platforma.

**Dovezile pe tranzacție, art. 18 alin. (2)** — pentru persoanele de la art. 2 lit. b), c), e), f), h), i), k), l):
> a) datele de identificare și coordonatele de contact ale contractanților; b) cantitățile și tipul de deșeuri
> tranzacționate; c) operatorii economici autorizați pentru implementarea răspunderii extinse a producătorului [...];
> d) avizul de însoțire a mărfii și informații cuprinse în factură, astfel: seria și numărul, data emiterii, cumpărătorul
> și vânzătorul; e) **fotografii din cel puțin două unghiuri ale încărcăturii**, în cazul deșeurilor de ambalaje;
> f) **fotografii din cel puțin 4 unghiuri ale mijlocului de transport încărcat înaintea deplasării**, după cum urmează: o
> fotografie din față, o fotografie laterală, o fotografie cu kilometrajul indicat la bord și o fotografie din spate care va
> trebui să cuprindă întreaga încărcătură și numărul de înmatriculare al mijlocului de transport, în cazul deșeurilor de
> ambalaje; [...]

Deci la ambalaje **6 fotografii** (2 + 4). **GPS-ul nu e pe tranzacție:** *„punctele de lucru, inclusiv coordonatele GPS ale
acestora”* sunt date pe care OIREP/UAT le înregistrează despre cocontractanți (alin. (1) lit. c)). La o tranzacție cu
ambalaje platforma **generează singură** Anexa 3 (HG 1061/2008) și, după caz, Anexa VII (alin. (4)), cu un cod unic de
tranzacție (alin. (5)). Orice transmitere cere **semnătură electronică calificată** (art. 4). Mărimea și formatul
fișierelor nu sunt în ordin (trimite la „caracteristicile tehnice”, nepublicate); ~~manualul AFM e v1.0 din 2018~~; **nu există
API public** (`/api/account` → 401). Aplicația poate doar **pregăti pachetul** tranzacției pentru introducerea manuală.

🔁 **Runda 2 — limita e 10 MB pe fișier**, din codul public al platformei: [i18n/ro/transactions.json](https://siatd.afm.ro/i18n/ro/transactions.json)
→ `"file-exceeds": "Fișierul încărcat depășește dimensiunea maxim admisă (10MB)!"` (serverul răspunde 413). Cele 6 sloturi
de fotografii n-au restricție de format în client; „doar PDF” e la documente (înrolare, acte semnate). **Manualele AFM la zi
sunt publice:** [ambalaje, 03.2022](https://siatd.afm.ro/api/document/download-manual),
[DEEE, 12.2025](https://siatd.afm.ro/api/document/download-manual-deee), [municipale, 02.2026](https://siatd.afm.ro/api/document/download-manual-mw).
Ele cer PDF ≤ 10 MB pentru contract, autorizație, viză, licență; la DEEE și la municipale documentele tranzacției se
**referă fără fișier** — *„Pentru fiecare tranzacție va fi referit bonul de cântar, fără a încărca documentul propriu-zis.”*
**Pentru cod:** fotografii JPEG ≤ 10 MB (țintă ~2 MB), una pe slot; documente PDF ≤ 10 MB; aviz/factură/borderou/bon doar
cu număr și dată. Formatele de imagine acceptate de server rămân neprobate (ar cere un POST real).

Alte articole: **art. 6** — *„Utilizarea aplicației SIATD se face în limita cantităților prevăzute în autorizația de
mediu”*; **art. 5** — garanție de 50 lei/t pentru cine raportează pentru OIREP.

**Cine intră — conflict nerezolvat (D6.4, întrebare la AFM):**
- **Instrucțiunile, art. 2**, listează colectorii numai când *„gestionează deșeuri pentru”* o OIREP (lit. b), e), h), k));
  la municipale doar UAT/ADI (lit. m), salubrizarea (lit. n) și valorificarea/reciclarea (lit. o);
- **OUG 196/2005 art. 10 alin. (12)**, act superior: *„Începând cu data de 1 ianuarie 2024, aplicația informatică SIATD se
  utilizează de către persoanele juridice care gestionează deșeuri municipale, astfel cum sunt prevăzute la pct. 13 din
  anexa nr. 1 la Ordonanța de urgență a Guvernului nr. 92/2021”* — iar „gestionarea” include colectarea (OUG 92/2021
  anexa 1 pct. 19). Hârtia și PET-ul aduse de o PF sunt municipale după pct. 13.

**Sancțiunile sunt în OUG 196/2005** ([Portal 67529](https://legislatie.just.ro/Public/DetaliiDocument/67529), forma din
07.03.2025), nu doar suspendarea accesului:

| Faptă | Articol | Amendă |
|---|---|---|
| neutilizarea SIATD de cei de la art. 10 alin. (9), (11)–(13) | art. 15 alin. (5) | **80.000–100.000 lei** |
| nerespectarea termenelor din Instrucțiuni | art. 15 alin. (6) | **9.000–10.000 lei** |
| raportare de date false | art. 15 alin. (12) | **200.000–250.000 lei** |

Plus: cantitățile de ambalaje (din 2023) și de anvelope/DEEE/baterii (din 2024) care nu sunt în SIATD nu se iau în
calcul la obiective (art. 12 alin. (8)–(9)).

~~**Pentru cod:** flag pe firmă „utilizator SIATD (modul)”, **nebifat implicit** [...] până răspunde AFM.~~

🔁 **Runda 2 — conflictul se rezolvă pe ierarhia actelor: OUG-ul se aplică direct.** Legea 24/2000
([art. 77–78](https://legislatie.just.ro/Public/DetaliiDocument/21698)):
> **Art. 77:** Ordinele cu caracter normativ, instrucțiunile și alte asemenea acte ale conducătorilor ministerelor [...] se
> emit numai pe baza și în executarea legilor, a hotărârilor și a ordonanțelor Guvernului.
> **Art. 78:** Ordinele, instrucțiunile și alte asemenea acte trebuie să se limiteze strict la cadrul stabilit de actele pe
> baza și în executarea cărora au fost emise și nu pot conține soluții care să contravină prevederilor acestora.

Ordinul 701/2024 se emite chiar *„în temeiul prevederilor art. 10 alin. (9) - (12)”* din OUG 196/2005, iar art. 2 din
Instrucțiuni nu se declară exhaustiv. Deci **colectorul care preia deșeuri municipale din gospodării (hârtie, PET, sticlă,
metal, ambalaje — anexa 1 pct. 13 lit. a) la OUG 92/2021) trebuie să folosească SIATD din 01.01.2024**, oricare ar fi lista
ordinului; altfel 80.000–100.000 lei. AFM spune același lucru într-un anunț adresat
*„OPERATORILOR ECONOMICI CARE GESTIONEAZĂ DEȘEURI MUNICIPALE”* ([PDF](https://www.afm.ro/main/venituri/anunt_utilizare_aplicatie_siatd.pdf),
fișier din 05.06.2025): *„aveți obligația de a va înrola [...] în aplicația informatică SIATD și de a adera la modulul
"Deșeuri municipale"”* (ghid: municipale.siatd@afm.ro). Pentru aceste intrări, art. 18 alin. (3) scutește tocmai de
încărcarea sursei (borderoul PF).

**Pentru cod:** la o firmă de colectare care cumpără de la populație, flagul „utilizator SIATD — modul Deșeuri municipale”
se **propune bifat**, cu avertismentul amenzii; D6.1/D6.2 apar când e bifat. **La AFM rămâne doar practica:** ce trece un
colector la „Număr contract delegare” / „Număr licență” în cererea de înrolare (anexa 3), ce înregistrează pe tranzacție
(art. 18 alin. (2) nu numește lit. m)–o)) și dacă termenul de 3 zile îl privește (doar dacă preia „în vederea sortării”).

Surse: [Portal 281612](https://legislatie.just.ro/Public/DetaliiDocument/281612), [Portal 281611](https://legislatie.just.ro/Public/DetaliiDocument/281611),
[siatd.afm.ro](https://siatd.afm.ro/), [afm.ro — FAQ SIATD](https://www.afm.ro/siatd-intrebari_raspunsuri.php); toate accesate 26.09.2026.

---

## 7. Proiect HG — sistemul TRACE-DM (NEADOPTAT)

Sursă: [sgglegis.gov.ro — proiect HG, mai 2026](https://sgglegis.gov.ro/legislativ/docs/2026/05/s954rx0c2hmqfj8_ndb3.pdf).
Consultare publică MMAP, mai 2026. Accesat 22.08.2026.

> **Stare: PROIECT.** Nu se codează nimic pe baza lui. E consemnat aici pentru că definește câmpurile
> unei eventuale înregistrări de recepție (depozitul cântărește azi prin `WeighingOperation`; `Reception` a rămas cusătură nefolosită, P3.6) și pentru că e argumentul comercial al modulului de depozit.

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

### 7.1 Recitit pe 26.09.2026: **tot PROIECT**, cu trei lucruri care lipseau

**Starea:** nicio HG adoptată. Pagina MMAP are doar consultarea din 08.05.2026, fără versiune revizuită; căutarea după
titlu pe Portal întoarce 0 rezultate; comunicatele ședințelor de Guvern din 11, 18 și 24.09.2026 nu-l pomenesc.

**Calendarul (art. 16–18)** — „180 de zile” de mai sus nu e tot:
- art. 18: HG-ul intră în vigoare *„la 30 de zile de la data publicării”*;
- art. 16: ordinul de procedură și formatul declarațiilor, *„în termen de 180 de zile de la data publicării”*;
- art. 10 lit. b): ghidul tehnic AFM, la 60 de zile de la ordin (proiectul trimite greșit la „art. 14”);
- art. 17: înregistrarea operatorilor în 60 de zile de la ghid, iar *„Obligațiile prevăzute la art. 8 devin aplicabile
  după expirarea termenului prevăzut la alin. (1)”*.

Deci obligațiile vin la ~10 luni după publicare, în cel mai rău caz: **nimic TRACE-DM nu se poate aplica în 2026**.

**Art. 13 alin. (1) are trei trepte, nu două:** a) art. 12 lit. a), b), d), **e)** (inclusiv nepăstrarea evidențelor) —
20.000–40.000 lei; b) art. 12 lit. c) — 10.000–20.000 lei; **c) art. 12 lit. f)**, *„utilizarea sistemului TRACE-DM cu
încălcarea condițiilor tehnice și procedurale”* — **5.000–10.000 lei**. Suspendarea și reanalizarea autorizației țin doar
de lit. a), b) și d).

**Pragurile sunt ambigue:** art. 5 cere datele *„pentru fiecare tranzacție”*, dar art. 3 lit. c) definește pragul ca
cantitatea anuală *„peste care se aplică regimul prevăzut la art. 8”* (verificarea actului, 24 h, declarația). Textul nu
spune dacă sub prag se înregistrează. Art. 6 alin. (2) exceptează calamitățile. Trimiterile greșite arată un text
nefinalizat: forma adoptată probabil va diferi.

---

## 8. ~~HG 349/2005~~ OG 2/2021 — depozitarea deșeurilor (profilul „groapă")

> 🔴 **Găsit 26.09.2026: HG 349/2005 e ABROGATĂ din 2021.** OG 2/2021 art. 39 alin. (2): *„La data intrării în vigoare a
> prezentei ordonanțe se abrogă Hotărârea Guvernului nr. 349/2005 privind depozitarea deșeurilor, [...] și orice trimitere
> la aceasta se consideră a fi făcută la prezenta ordonanță.”* ([OG 2/2021](https://legislatie.just.ro/Public/DetaliiDocument/245381),
> forma din 03.11.2023, accesat 26.09.2026). **Citatele din §8.1 și §8.2 de mai jos sunt din actul abrogat** și se
> recitesc pe OG 2/2021 înainte de orice cod pe profilul „groapă”. Aceeași lecție ca la art. 29 alin. (2) din OUG 92/2021
> (§2.7): o trimitere îmbătrânește fără să se schimbe o literă în actul care o poartă.
>
> **Ce contează din OG 2/2021 pentru depozitul-colector (D3.4), art. 3 alin. (2) lit. b):** „depozit” include *„o
> suprafață permanent amenajată, respectiv pentru o perioadă de peste un an, pentru stocarea temporară a deșeurilor, dar
> exclusiv: [...] (ii) stocarea deșeurilor înainte de valorificare sau tratare pentru o perioadă **mai mică de 3 ani**, ca
> regulă generală, sau stocarea deșeurilor înainte de eliminare, pentru o perioadă **mai mică de un an**”*. Un centru
> de colectare unde deșeul stă peste aceste durate devine deci, juridic, o groapă neautorizată.

Sursă (istoric, act abrogat): [legislatie.just.ro/Public/DetaliiDocument/61498](https://legislatie.just.ro/Public/DetaliiDocument/61498),
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
> general al Legii contabilității — ~~**de verificat separat**, nu se presupune~~ **verificat pe
> 15.09.2026: 10 ani**, vezi §15.

### 9.1 Recitit integral pe 15.09.2026 (forma consolidată din 05.06.2022)

**Art. 1 alin. (1^2)** — operatorii autorizați care cumpără metale de la persoane fizice au obligația:

> a) să efectueze plata contravalorii bunurilor achiziţionate [...] **prin virament bancar, în contul
> specificat de vânzător, sau cu numerar**, cu respectarea legislaţiei fiscale;
> b) să completeze [...] un **borderou de achiziţie de deşeuri metalice** [...]

„Cu respectarea legislației fiscale” trimite la plafonul de numerar din Legea 70/2015 art. 4 (§13).

**Art. 1 alin. (2)** — contravenţiile. **Alin. (3)**: la operatorii de colectare, sancțiunea
complementară e **revocarea autorizației de colectare**.

| Faptă | Amendă |
|---|---|
| achiziția metalelor interzise de la PF, alin. (1): infrastructură de transport, rețele de utilități, gaze/țiței, semnalizare rutieră, cale ferată, produse de prelucrare chimică/termică | 100.000–150.000 lei |
| metal care nu provine din gospodăria proprie, alin. (1^1) | 100.000–150.000 lei |
| nerespectarea regulilor de plată | 30.000–50.000 lei |
| borderou necompletat sau incomplet | 10.000–30.000 lei |
| la repetare | limitele cresc cu 50% |

Constată: Poliția, Jandarmeria, Poliția de Frontieră, Garda Națională de Mediu, ANAF. Metalele cu păgubit
neidentificat se confiscă.

**Anexa — modelul borderoului, rubrică cu rubrică:**

- antet: *„denumire, formă juridică, adresă sediu social/punct de lucru, nr. din registrul comerţului,
  CUI, CIF, nr. şi data emiterii autorizaţiei de mediu”* — operatorul economic colector/valorificator;
- deținătorul: nume, seria și numărul actului de identitate, emitentul, CNP, domiciliu, mijlocul de transport;
- tabel: **Denumirea deşeului şi descrierea acestuia (0) · Codul conform HG nr. 856/2002 (1) ·
  Cantitatea (kg) (2) · Preţul unitar (lei/kg) (3) · Valoarea (lei) (4 = 2 × 3)** · TOTAL;
- plata: *„Se achită suma de ... lei [...] cu chitanţa nr. ... sau în termen de maximum 3 zile lucrătoare
  de la data prezentei, prin virament bancar în contul deţinătorului”*;
- reținerile: *„Impozitul pe venit de **16%** şi contribuţia de **3%** la Administraţia Fondului pentru
  Mediu [...] au fost reţinute la sursă din valoarea brută.”*;
- *„Gestionar primitor”*;
- declarația: *„Declar pe propria răspundere că deşeurile pe care le predau provin din gospodăria proprie.”*

> ⚠️ **Procentele din model sunt cele din 2011 și nu mai sunt cele în vigoare.** AFM e **2%** (§10.1).
> Impozitul pe venit la metalele PF a fost reintrodus de Legea 141/2025 (§12), cu altă cotă.
> Tipărirea cu cotele actuale, calculate, e de confirmat cu contabilul.
> **CNP-ul se cere doar aici**, la metale. Pentru hârtie sau plastic cumpărate de la PF nicio lege în
> vigoare nu cere CNP. Colectat fără obligație legală, ar cădea sub Legea 190/2018 art. 4 alin. (2) (§16).

---

## 10. OUG 196/2005 — Fondul pentru mediu: cine, cât, cât de des

Sursă: [legislatie.just.ro/Public/DetaliiDocumentAfis/258980](https://legislatie.just.ro/Public/DetaliiDocumentAfis/258980),
versiune consolidată, accesat 22.08.2026.
⚠️ **26.09.2026:** pagina 258980 e o formă din 20.09.2022. Forma curentă e
[DetaliiDocument/67529](https://legislatie.just.ro/Public/DetaliiDocument/67529) (consolidare **07.03.2025**, ultimul act
modificator OUG 9/2025). Textul art. 9 alin. (1) lit. a) și art. 11 citat mai jos e **identic** în forma curentă; SIATD
(art. 10 alin. (9)–(14) și amenzile de la art. 15) e în §6.1.

### 10.1 Art. 9 alin. (1) lit. a) — contribuția de 2%, reținută la sursă

> o contribuţie de **2% din veniturile realizate din vânzarea deşeurilor**, obţinute de către
> deţinătorul deşeurilor, persoană fizică sau juridică. Sumele **se reţin la sursă de către operatorii
> economici care desfăşoară activităţi de colectare şi/sau valorificare a deşeurilor**, care au
> obligaţia să le vireze la Fondul pentru mediu.

> **Cel mai important lucru găsit pentru modulul de depozit.** Contribuția o *datorează* vânzătorul,
> dar o **reține și o virează colectorul** — adică exact clientul-depozit. Deci un centru de colectare
> are o obligație AFM **lunară**, structural, prin simplul fapt că cumpără deșeu. Nu e opțională și nu
> depinde de ambalaje. Calculul se face pe fiecare achiziție → ✅ din 16.09.2026 pe intrarea de depozit: `DepotRetentions`, pe
> `WeighingOperation` (`V46` + `V51`), la finalizare. (`Reception`, gândită inițial pentru asta, a rămas nefolosită.)

> 🆕 **Baza de calcul, citită pe 15.09.2026** (forma consolidată din 07.03.2025). **Art. 10 alin. (5):**
> baza pentru procentele de la art. 9 alin. (1) lit. a), e), f) şi j) este *„valoarea de vânzare,
> **exclusiv taxa pe valoarea adăugată** aferentă”*. Ordonanța nu are nicio scutire pentru persoane
> fizice sau pentru deșeul din gospodărie: textul spune „deţinătorul deşeurilor, persoană fizică sau
> juridică”. Deci cei 2% se rețin **și** de la PF, **și** la hârtie și plastic, nu doar la metale.

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

> **Consecință pe modulul Termene — gap confirmat (închis în `V21`, vezi dedesubt).** Până la `V21` generam un singur termen AFM, lunar pe 25,
> pentru orice firmă cu `afmObligation`. Realitatea are **trei cadențe**, după *care* contribuție o
> datorează firma: lunar (inclusiv contribuția de 2% a colectorului), trimestrial (economia circulară
> — depozitele), anual pe 25 ianuarie (ambalaje, anvelope, UAT). Flagul boolean `afmObligation` e prea
> sărac: are nevoie să devină un **set de contribuții datorate**, fiecare cu cadența ei.
>
> ✅ **Construit în `V21`:** `AfmContribution` e set pe firmă — `WITHHOLDING_2_PERCENT` lunar, `CIRCULAR_ECONOMY`
> trimestrial, `PACKAGING` anual. `CIRCULAR_ECONOMY` a fost scoasă din configurarea contului pe 16.09.2026 (`V60`):
> e a gropilor de deşeuri, nu a clienţilor aplicaţiei.

> **Ce a spus specialista pe 24.08 (R25), și ce rămâne de lămurit.** Întrebată cine are obligație
> AFM, a răspuns: *„doar generatorii de deșeuri de ambalaj — producători/importatorii"*. Adică
> **cine pune pe piață marfă ambalată**, nu fabricanții de ambalaje.
>
> Asta se potrivește peste **lit. d)**, contribuția pe ambalaje, care e **anuală** (alin. (2)) — și
> confirmă că termenul lunar generat atunci (până la `V21`) pentru un generator obișnuit era greșit. Ce **nu** spune
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

## 12. Codul fiscal — impozitul pe venit la metalele cumpărate de la PF (Legea 141/2025)

Sursă primară: [legislatie.just.ro/Public/DetaliiDocument/300022](https://legislatie.just.ro/Public/DetaliiDocument/300022)
(Legea 141/2025), accesat **15.09.2026**.

**Art. 62 lit. f)**, venituri neimpozabile, în forma nouă:

> veniturile de orice fel, în bani sau în natură, primite ca urmare a predării deşeurilor din patrimoniul
> personal, **altele decât metale feroase şi neferoase şi aliajele acestora** din patrimoniul personal

**Art. 114 alin. (2) lit. m²)**, nou, la „venituri din alte surse”:

> veniturile de orice fel, în bani sau în natură, primite ca urmare a predării metalelor feroase şi
> neferoase şi aliajelor acestora, din patrimoniul personal, încadrate ca deşeuri potrivit legii

**Aplicare:** art. VII alin. (1) lit. b) — *„pentru veniturile plătite începând cu data de 1 august 2025”*.

> ✅ **Cota e citită pe sursă primară pe 15.09.2026: 10% din venitul brut**, vezi §18.1. Nota de
> dinainte („16% pe Lege5, 10% doar din surse secundare, de confirmat cu contabilul”) a fost închisă pe
> Codul fiscal consolidat de pe Portalul Legislativ, forma din 08.08.2026.
>
> **Ce se închide cu asta:** răspunsul **L** al specialistei, „dacă deșeul nu e din gospodărie
> proprie, încă 10%” (`status.md`), era un impozit pe venit din Codul fiscal, nu AFM, cum bănuiam.
> Textul în vigoare îl leagă însă de **metale**, nu de proveniență. Hârtia, plasticul și acumulatorii
> de la PF rămân neimpozabili; doar cei 2% AFM se rețin.

## 13. Legea 70/2015 — plafonul de numerar

Sursă: [legislatie.just.ro/Public/DetaliiDocument/167088](https://legislatie.just.ro/Public/DetaliiDocument/167088),
consolidare 01.01.2026, accesat 15.09.2026.

**Art. 4:** operațiunile de încasări și plăți în numerar între persoane juridice și persoane fizice
*„se efectuează cu încadrarea în plafonul zilnic de **10.000 lei** către/de la o persoană”* (forma dată de OUG
115/2023). **Art. 4 alin. (2)**: *„Sunt interzise încasările și plățile fragmentate de la/către o persoană, pentru
operațiunile de încasări/plăți în numerar prevăzute la alin. (1), cu o valoare mai mare decât plafonul prevăzut la
alin. (1)”*. *(Corectat 26.09.2026: aici scria „art. 3 alin. (2)–(3)”; art. 3 privește plățile între persoane juridice —
5.000 lei/zi către o persoană, 10.000 lei/zi în total.)*
🆕 **Legea 239/2025, din 01.01.2026:** persoana juridică trebuie să aibă un cont de plăți (art. 1 alin. (1^2)–(1^3)),
sub amendă de 3.000–10.000 lei. Plafonul și art. 4 nu s-au schimbat (reverificat 26.09.2026, fără act ulterior).
**Consecință:** un depozit plătește cash unei PF cel mult 10.000 lei pe zi, iar restul prin virament
(OUG 31/2011 art. 1 alin. (1^2) lit. a), §9.1).

## 14. OPANAF 802/2022 — RO e-Transport: deșeurile nu sunt pe listă

Sursă: [legislatie.just.ro/Public/DetaliiDocument/254608](https://legislatie.just.ro/Public/DetaliiDocument/254608),
accesat 15.09.2026.

Anexa cuprinde legume (NC 0701–0714), fructe (0801–0814), băuturi (2201–2208), sare și ciment (2505,
2517), îmbrăcăminte (6101–6117, 6201–6212, 6214–6217), încălțăminte (6401–6405) și bare de oțel
(7213–7214). **Niciun cod de deșeuri sau resturi**: 7204, 7404, 7602, 4707 și 3915 lipsesc.
Surse din 2026 ([fiscalitatea.ro](https://www.fiscalitatea.ro/ro-e-transport-din-1072024-lista-cu-bunuri-cu-risc-fiscal-se-modifica-23464))
spun că lista nu s-a mai schimbat.
**Consecință:** transportul intern de deșeuri **nu cere cod UIT**. Rămân doar transporturile
internaționale, pe OUG 41/2022, dacă apare un client exportator. Corectează `legislatie.md` §E.

## 15. Legea 82/1991 — păstrarea documentelor și inventarul

Sursă: [legislatie.just.ro/Public/DetaliiDocumentAfis/58588](https://legislatie.just.ro/Public/DetaliiDocumentAfis/58588),
consolidare 04.02.2025, accesat 15.09.2026.

**Art. 25 alin. (1):**

> Registrul-jurnal, Registrul-inventar şi Cartea mare, precum şi **documentele justificative** care stau
> la baza înregistrărilor în contabilitatea financiară se păstrează în arhiva persoanelor prevăzute la
> art. 1, timp de **10 ani**, cu începere de la data încheierii exerciţiului financiar în cursul căruia
> au fost întocmite, cu excepţia statelor de salarii, care se păstrează timp de 50 de ani.

~~**Art. 7** (citit prin surse secundare)~~ — **citit pe sursă primară pe 26.09.2026** (consolidare 04.02.2025, [Portal 58588](https://legislatie.just.ro/Public/DetaliiDocument/58588)):

> **Art. 6 alin. (1):** Orice operațiune economico-financiară efectuată se consemnează **în momentul efectuării ei** într-un
> document care stă la baza înregistrărilor în contabilitate, dobândind astfel calitatea de document justificativ.
>
> **Art. 7 alin. (1):** Persoanele prevăzute la art. 1 au obligația să efectueze inventarierea generală a elementelor de
> natura activelor, datoriilor și capitalurilor proprii deținute la începutul activității, **cel puțin o dată în cursul
> exercițiului financiar**, precum și în cazul fuziunii, divizării ori transformării sau al lichidării și în alte situații
> prevăzute de lege.

Art. 41 pct. 2 lit. d) + art. 42 alin. (1) lit. d): nerespectarea normelor de inventariere — **3.000–20.000 lei** (amenda
e a firmei clientului).

**Consecințe:** borderoul PF și CNP-ul de pe el se păstrează 10 ani, nu 3 cum rămân datele șoferului.
Stocul depozitului trebuie inventariat cel puțin anual.

### 15.1 Normele de inventariere — OMFP 2861/2009, în vigoare (citit 26.09.2026)

[Ordinul, 112430](https://legislatie.just.ro/Public/DetaliiDocument/112430) · [Normele, 112431](https://legislatie.just.ro/Public/DetaliiDocument/112431).
Portalul nu arată abrogare și nu s-a găsit un act care să-l înlocuiască (lista de modificări n-a putut fi consultată —
mică incertitudine). Ordinul, art. 1 alin. (5): firma emite **proceduri proprii** de inventariere, aprobate de administrator.

Pașii, cu punctul din Norme:
1. **Decizia de numire** cu cinci elemente: comisia, modul, metoda, gestiunea, datele de început și sfârșit (pct. 6 alin. (1)).
   **Gestionarul și contabilul gestiunii nu pot fi în comisie** (pct. 6 alin. (5)); la firmele mici, o singură persoană
   (alin. (2)); fără salariați, administratorul (alin. (7)).
2. **Declarația gestionarului**, cu ultimul document de intrare și de ieșire (pct. 8 lit. a)).
3. Stocul faptic prin cântărire, numărare, măsurare sau **„calcule tehnice”** — la grămezi, cu datele tehnice scrise în
   listă (pct. 15 alin. (3)–(4)).
4. **Liste pe loc de depozitare, gestiune, categorie** (pct. 18); bunurile terților pe liste separate (pct. 19); semnătură
   pe fiecare filă, mențiunea gestionarului pe ultima (pct. 33).
5. Faptic față de scriptic pe fiecare poziție, după corectarea erorilor (pct. 35); explicații scrise pentru fiecare
   diferență (pct. 39); compensarea lipsurilor cu plusurile doar pe lista anuală de sorturi confundabile (pct. 40);
   perisabilitățile *„nu se aplică anticipat [...] nu se aplică automat”* (pct. 41 alin. (2)).
6. **Procesul-verbal** cu treisprezece elemente (pct. 42), la administrator în **7 zile lucrătoare** (pct. 43).
7. Rezultatul se înregistrează în **cel mult 7 zile lucrătoare** de la aprobare (pct. 45 alin. (1)).

**Formularele, OMFP 2634/2015** ([173682](https://legislatie.just.ro/Public/DetaliiDocument/173682), anexele 2–3 la
[286045](https://legislatie.just.ro/Public/DetaliiDocument/286045) / [286046](https://legislatie.just.ro/Public/DetaliiDocument/286046)):
**Lista de inventariere 14-3-12** (15 coloane; variantele 14-3-12/b și /a), Registrul-inventar 14-1-2 (al contabilului,
nu al modulului), Decizia de imputare 14-8-2. **Procesul-verbal n-are model oficial** — conținutul e cel din pct. 42.
Art. 3–4: obligatoriu e doar conținutul minim (anexa 1 pct. 2 și 10); *„Fiecare entitate poate adapta [...] modelele”*.

**Perisabilitate: nu există pentru deșeuri.** HG 831/2004 are 60 de grupe de mărfuri, **niciuna de deșeuri, metale,
maculatură sau plastic** ([Norme, 52624](https://legislatie.just.ro/Public/DetaliiDocument/52624)); art. 2 scoate oricum
furtul și neglijența. Lipsa neimputabilă e nedeductibilă (Codul fiscal art. 25 alin. (4) lit. c)), iar TVA-ul dedus —
inclusiv cel autolichidat (art. 331 alin. (3)) — se ajustează după art. 304. **Aplicația nu oferă „perisabilitate” ca
motiv de ajustare la deșeuri.**

🔁 **Runda 2, 26.09.2026** (note: `runda2_contabil.md`; Codul fiscal 08.08.2026, Normele HG 1/2016 31.03.2026):
- **HG 831/2004 e în vigoare** și se leagă de codul actual prin art. 502 alin. (2) (trimiterile la Legea 571/2003 *„se
  consideră a fi făcute la prezentul cod”*), adică la art. 25 alin. (3) lit. d) — perisabilitățile *„potrivit legii”*.
  Normele HG 1/2016 nu o numesc. Art. 1 o limitează la *„rețeaua de distribuție”*; art. 10 permite cote proprii doar în
  limitele grupei; art. 11: peste norme, nedeductibil. La microîntreprinderi (art. 12 privește doar impozitul pe profit)
  contează doar ajustarea TVA. **Singura normă proprie cu efect fiscal** e norma de consum tehnologic (pct. 78 alin. (10)
  lit. e) din Norme, la TVA) — pentru sortare/presare, nu pentru depozitare. Minusurile de depozit rămân *lipsă
  neimputabilă*: nedeductibile, cu TVA de ajustat.
- **Stocul degradat trimis la eliminare:** Codul fiscal art. 25 alin. (4) lit. c) pct. 3 cere *„se face dovada
  distrugerii”*, iar **Normele, Titlul II, pct. 17 alin. (3)**: *„condiția referitoare la distrugerea stocurilor [...] se
  consideră îndeplinită atât în situația în care distrugerea se efectuează prin mijloace proprii, cât și în cazul în care
  stocurile [...] sunt predate către unități specializate.”* Deci **predarea la un operator autorizat, cu Anexa 3 și factura
  lui, e dovada**; la TVA, pct. 78 alin. (10) lit. d), (11), (12). Condiția de fond: bunul e degradat și nevalorificabil
  (vânzarea la reciclare e livrare, nu distrugere). La contabil rămâne doar dacă predarea **gratuită** la un reciclator
  (cod R) contează ca „unitate specializată”.
- ⚠️ În notele primei runde art. 25 alin. (4) lit. c) era citat cu 7 puncte; textul în vigoare are 4. Concluzia rămâne.

### 15.2 Valoarea stocului — OMFP 1802/2014 (consolidare 19.11.2025, [294347](https://legislatie.just.ro/Public/DetaliiDocument/294347))

> **Pct. 96 alin. (1):** Costul de achiziție [...] al stocurilor din aceeași categorie [...] se calculează prin aplicarea
> uneia din următoarele metode: a) metoda costului mediu ponderat - CMP; b) metoda primul intrat-primul ieșit - FIFO;
> c) metoda ultimul intrat-primul ieșit - LIFO.
> **Alin. (2):** [...] Media poate fi calculată periodic sau după fiecare recepție. Perioada de calcul nu trebuie să
> depășească durata medie de stocare.
>
> **Pct. 287 alin. (4):** O diferență în localizarea geografică nu este suficientă pentru a justifica alegerea de metode
> diferite.

Deci CMP e **o opțiune, nu o regulă**; metoda e politica contabilă a firmei, consecventă de la un exercițiu la altul (pct.
50, 287 alin. (1)) și **aceeași pe toate depozitele**. La inventar, valoarea e minimul dintre valoarea contabilă și
valoarea realizabilă netă (pct. 88). Pct. 95 alin. (2): lipsurile *„se scot din evidență la data constatării”*.
**Pentru D3.7:** setare pe firmă „Metoda de cost”, fixă pe exercițiu; valoarea afișată e „evidență de gestiune”.

🔁 **Runda 2 — ce folosește sectorul** (politici contabile publicate, note: `runda2_siatd_fisiere_cost.md`): colector de
maculatură cu fabrică de hârtie — **FIFO**; reciclator de aluminiu — FIFO la materii prime, CMP la produse finite;
reciclator de mase plastice — **CMP**; grup cu reciclare PVC — **CMP** la materii prime și mărfuri; un colector pur de fier
vechi nu-și declară metoda. Nicio normă sau îndrumare ANAF nu preferă o metodă la deșeuri (Codul fiscal pomenește FIFO doar la
titluri, art. 97). **Pentru cod:** implicit CMP pe fiecare recepție (decizia din 26.09); opțiuni CMP lunar (cu avertismentul
„perioada ≤ durata medie de stocare”) și FIFO; **LIFO nu apare în interfață**.

### 15.3 Preluarea gratuită de la o persoană fizică — NIR, nu borderou (runda 2, 26.09.2026)

**OMFP 2634/2015, anexa 2 — NIR 14-3-1A**, document de recepție obligatoriu între altele pentru *„bunurilor materiale
procurate de la persoane fizice”* și *„bunurilor materiale care sosesc neînsoțite de documente de livrare”*
([static.anaf.ro](https://static.anaf.ro/static/10/Anaf/legislatie/OMFP_2634_2015.pdf)). Borderoul 14-4-13 e pentru
*„bunuri cumpărate”* și *„sume plătite”* — un borderou cu preț 0 ar descrie o cumpărare care n-a avut loc.
**OMFP 1802/2014 pct. 75 alin. (1) lit. d):** bunurile obținute cu titlu gratuit intră *„la valoarea justă”* (371 = 758);
venitul e impozabil și la impozitul pe profit (art. 19), și la microîntreprinderi (art. 53).
**Pentru cod:** intrare PF **fără plată** → NIR (furnizor = numele persoanei; CNP doar la metal), valoare propusă = prețul
curent de achiziție al firmei, marcată „de confirmat de contabil”; preluările gratuite de ambalaje se țin separat de cele
cumpărate (Legea 249/2015 art. 20 alin. (7) lit. b) și (10) vorbesc de predare „contra cost” și de cantități „achiziționate”).

## 16. Legea 190/2018 art. 4 — CNP-ul

Sursă: [legislatie.just.ro/Public/DetaliiDocument/203151](https://legislatie.just.ro/Public/DetaliiDocument/203151),
accesat 15.09.2026.

> **(1)** Prelucrarea unui număr de identificare naţional, inclusiv prin colectarea sau dezvăluirea
> documentelor ce îl conţin, se poate efectua în situaţiile prevăzute de art. 6 alin. (1) din
> Regulamentul general privind protecţia datelor.
> **(2)** Prelucrarea [...] în scopul prevăzut la art. 6 alin. (1) lit. f) [...], respectiv al
> realizării intereselor legitime urmărite de operator sau de o parte terţă, se efectuează cu
> instituirea de către operator a următoarelor garanţii: [măsuri tehnice și organizatorice, **responsabil
> cu protecția datelor**, termene de stocare și ștergere, instruirea periodică a celor care prelucrează].

**Consecință:** la metale CNP-ul are temei de obligație legală (art. 6 alin. (1) lit. c) GDPR, OUG
31/2011), fără garanțiile de la alin. (2). La orice altă cumpărare de la PF, un CNP cerut „ca să fie”
ar cere DPO la client. Deci aplicația **cere CNP doar când e metal**.

## 17. Codul fiscal art. 331 și OG 20/1992 — TVA și cântarul

**Art. 331 alin. (2) lit. a)** ([Lege5](https://lege5.ro/Gratuit/g43donzvgi/masuri-de-simplificare-codul-fiscal?dp=hazdimzygizdi),
sursă secundară; 15.09.2026): **taxarea inversă** se aplică între plătitori de TVA la livrarea de:
1. deşeuri feroase şi neferoase și rebuturi, inclusiv semifabricatele din prelucrarea lor;
2. reziduuri reciclabile din metale, zgură, cenuşă;
3. *„deşeuri de materiale reciclabile şi materiale reciclabile uzate constând în hârtie, carton,
   material textil, cabluri, cauciuc, plastic, cioburi de sticlă şi sticlă”*;
4. materialele de la pct. 1–3 după curăţare, selecţie, tăiere, presare etc.

**Consecință:** prețurile din depozit se țin **fără TVA**, la fel ca baza de 2% (§10.1).
🆕 **26.09.2026, pe sursă primară** ([Codul fiscal, 171282](https://legislatie.just.ro/Public/DetaliiDocument/171282),
consolidare 08.08.2026): lit. a) **nu expiră** — termenul de la alin. (6) privește doar lit. c)–f) și i)–l). Art. 331 nu
are nicio regulă despre inventar.

### 17.1 OG 20/1992 — cântarul (recitit pe forma în vigoare, 26.09.2026)

> 🔴 **Ce stătea aici era textul inițial, de pe Lege5.** Art. 29 a fost modificat de Legea 211/1998, OG 104/1999,
> Legea 178/2003 și Legea 98/2004; fraza *„utilizarea mijloacelor de măsurare neverificate [...] cu termenul de verificare
> depăşit”* **nu mai e în vigoare**. Interdicția stă acum la **art. 19**.

Sursă: [Portal 2171](https://legislatie.just.ro/Public/DetaliiDocument/2171), consolidare 29.08.2010 (ultima modificare OG
23/2010; nerepublicată, neabrogată), accesat 26.09.2026.

> **Art. 19:** Mijloacele de măsurare care nu au corespuns la controlul metrologic legal sau pe care sunt aplicate
> marcaje de verificare cu termen de valabilitate depăşit **nu au calitatea de mijloace de măsurare legale**. Este interzisă
> introducerea pe piaţă, punerea în funcţiune sau **utilizarea** pentru măsurările din domeniile de interes public prevăzute
> la art. 3 alin. 1 a mijloacelor de măsurare prevăzute la alin. 1. Deţinătorii acestor mijloace de măsurare sunt obligaţi,
> după caz, să le retragă de pe piaţă sau să le scoată din uz. [...]
>
> **Art. 24 alin. 1:** Persoanele fizice sau juridice care utilizează ori încredinţează spre utilizare, în calitate de
> proprietar, mijloace de măsurare supuse controlului metrologic legal sunt obligate să asigure legalitatea acestora şi
> **să le declare, înainte de punerea în funcţiune/utilizare, Biroului Român de Metrologie Legala**.

Art. 3 și 21 lit. a) pun sub control măsurările din *„tranzacţii comerciale”*. BRML are declarare online
([brml.ro](https://www.brml.ro/declararea-mijloacelor-de-m%C4%83surare-0)).

**Amenzile, art. 29** (în lei vechi; ÷ 10.000 după Legea 348/2004 — conversie aritmetică, **de confirmat de jurist**
înainte de a apărea în aplicație sau pe landing):

| Faptă | Lit. | Lei vechi | Lei noi | Complementar |
|---|---|---|---|---|
| folosirea unui cântar nelegal (art. 19 alin. 2) | c) | 50–150 mil. | 5.000–15.000 | sigilare |
| nedeclarare la BRML (art. 24 alin. 1) | h) | 50–100 mil. | 5.000–10.000 | — |
| deținerea în locul de cântărire a unui cântar fără marcaj în termen (art. 24 alin. 2) | i) | 50–200 mil. | 5.000–20.000 | sigilare/confiscare |

La persoane fizice limitele se înjumătățesc (art. 31). Nicio normă găsită nu anulează cântăririle făcute cu un cântar
expirat: sunt interzise și sancționabile, nu nule.

**Intervalul — L.O.-2022** (Ordinul BRML 77/2022, modificat doar de 204/2024, [Portal 253632](https://legislatie.just.ro/Public/DetaliiDocument/253632)):
cântare neautomate L52-1 (inclusiv podul basculă static), automate L53-1…L53-5, cântărire în mers L55-1 — toate
**VP 1 an**. Art. 8: intervalul e *„între două verificări metrologice succesive, oricare ar fi acestea”* (inclusiv după
reparare). Art. 9: la un cântar nou, primul an curge **de la punerea în funcțiune**.

**Verificarea — IML 3-05 / IML 4-05** (anexe la HG 1660/2005, nearogate, [121475](https://legislatie.just.ro/Public/DetaliiDocument/121475), [121476](https://legislatie.just.ro/Public/DetaliiDocument/121476)):
- art. 17 alin. (2), conținutul minim al buletinului: laboratorul; numărul și data; identificarea cântarului;
  reglementarea aplicată; etalonul; **ADMIS / RESPINS**; **valabilitatea**; verificatorul metrolog; cine a primit cântarul;
- art. 13: după un incident, deținătorul **scoate cântarul din uz și îl prezintă la verificare, indiferent de termen**;
- art. 7 lit. d) + art. 14 alin. (2): după orice reparație sau modificare, reverificare (o prezintă reparatorul);
- IML 4-05 art. 7: marcajul e valabil *„numai în intervalul de timp precizat în buletinul de verificare metrologică”*.

Stratul UE: 2014/31/UE (neautomate) e transpusă prin **HG 710/2015**, 2014/32/UE prin **HG 711/2015** — ambele privesc
doar punerea pe piață (CE + „M”); verificarea periodică rămâne pe OG 20/1992 și L.O.-2022. Nu există registru public al
buletinelor, deci aplicația nu poate verifica un buletin automat.

**Pentru D2.3:** cântar (serie, tip, punere în funcțiune, declarat la BRML — dată și dovadă, stare în uz / scos din uz /
sigilat) + istoric de verificări (nr. buletin, dată, ADMIS/RESPINS, „valabil până la” = data + 12 luni, editabil doar în
jos, laborator, verificator, buletinul atașat). Scadența = ultima verificare ADMIS; RESPINS nu dă valabilitate; o
reparație sau un incident înregistrat o anulează pe loc; alerta la 30 de zile e alegerea noastră, legea nu cere preaviz.

---

## 18. Blocantele modulului de depozit, închise pe text (15.09.2026)

Întrebările C1–C3 (contabil) și AX–AZ (specialistă) din `intrebari-specialist.md` au fost citite pe
sursă primară în aceeași zi. Formele citite, toate de pe Portalul Legislativ: Codul fiscal **08.08.2026**
(171282), OUG 31/2011 **05.06.2022** (127186), OUG 196/2005 **07.03.2025** (~~258980~~ **67529** — 258980 e forma din 2022, corectat 26.09.2026), OUG 92/2021
**11.07.2026** (245846), HG 1061/2008 **23.01.2026** (97706), HG 1132/2008 **17.12.2021** (97608),
Ordinul 701/2024 (281612). Regulamentul (UE) 2023/1542 e citit din CELLAR, în română.

### 18.1 C1 — impozitul la metalele PF: **10% din venitul brut**, reținut la plată

**Codul fiscal art. 115 alin. (1)** (modificat de OUG 89/2025, de la 01.01.2026):

> Impozitul pe venit se calculează prin reținere la sursă la momentul acordării veniturilor de către
> plătitorii de venituri, prin aplicarea asupra venitului brut a unei cote de: a) **10%** pentru
> veniturile prevăzute la art. 114 alin. (2) lit. a)-g), j)-k^1) **și m^2)**; b) 16% pentru veniturile
> prevăzute la art. 114 alin. (2) lit. h) și i).

- **Alin. (2):** *„Impozitul calculat și reținut reprezintă impozit final.”*
- **Alin. (3):** *„Impozitul astfel reținut se plătește la bugetul de stat până la data de 25 inclusiv a lunii următoare celei în care a fost reținut.”*
- **Art. 132 alin. (1):** plătitorul *„calculează, reține, plătește și declară”* până la termenul de plată, deci lunar (D100).
- **Art. 132 alin. (2):** plătitorul depune *„o declarație privind calcularea și reținerea impozitului pentru fiecare beneficiar de venit [...] până în ultima zi a lunii februarie inclusiv a anului curent pentru anul expirat”* (D205).

**Baza** e *„venitul brut”*, adică valoarea de cumpărare. Contribuția AFM se calculează și ea din
aceeași valoare (§10.1: *„2% din veniturile realizate din vânzarea deșeurilor”*). Cele două se
calculează **independent, pe valoarea brută**, și nu una din cealaltă. Așa scrie și borderoul:
*„reținute la sursă din valoarea brută”*.

**Normele metodologice** (HG 1/2016, anexa, forma consolidată din 02.02.2026, Portal 289668) au fost
căutate la reverificare. **N-au nimic despre lit. m²) sau despre metalele din patrimoniul personal**, deci
nu schimbă baza: rămâne „venitul brut” din art. 115.

**Pentru cod:** cota e fixă prin lege, deci stă în cod cu trimiterea la articol, nu ca setare pe firmă.
Nu se aplică la hârtie, plastic sau acumulatori de la PF: art. 62 lit. f) îi lasă neimpozabili (§12).

### 18.2 C2 — cotele de pe borderou: se tipăresc **cele în vigoare**

Modelul din anexa OUG 31/2011 scrie *„Impozitul pe venit de 16% și contribuția de 3% la Administrația
Fondului pentru Mediu [...] au fost reținute la sursă din valoarea brută.”* Ambele cifre sunt depășite:
10% (§18.1) și 2% (OUG 196/2005 art. 9 alin. (1) lit. a), în vigoare). Cei 3% la metale mai apar pe Portal
doar într-o notă despre decizia ÎCCJ HP nr. 9/2020, ca istoric.

**Ce decide:** conținutul obligatoriu al borderoului e lista din **art. 1 alin. (1^2) lit. b) pct. (i)–(viii)**:
operatorul, persoana fizică, codul, cantitatea, valoarea, contul, chitanța și declarația de gospodărie
proprie. **Fraza cu procentele nu e în listă.** Ea e o mențiune din model care trimite la actele fiscale.
Tipărirea cotelor în vigoare, cu sumele calculate, respectă conținutul cerut. Tipărirea lui 16% + 3% ar
declara pe un document financiar-contabil (alin. (1^3)) o reținere care nu s-a făcut.

### 18.3 C3 — hârtia și plasticul de la PF: **textul nu tranșează**, deci întrebarea BA la specialistă

⚠️ **Corectat la reverificare, tot pe 15.09.2026.** Prima formă a acestei secțiuni spunea „borderou la
orice intrare PF, din cauza SIATD”. Recitit întreg, ordinul nu susține concluzia.

OUG 31/2011 privește doar metalele. **Ordinul 701/2024 art. 18 alin. (3)** spune:

> La inițierea unei tranzacții în aplicația SIATD pentru fiecare transport/tranzacție cu deșeuri,
> înregistrat(ă) în platformă, **cu excepția deșeurilor municipale preluate de operatorul economic
> colector**, se va indica sursa deșeurilor în formatul corespunzător cerințelor SIATD, inclusiv
> borderoul de achiziție în cazul deșeurilor preluate de la persoane fizice.

Două texte trag în direcții opuse:
- **OUG 92/2021, definiția de la pct. 13:** *„deșeuri municipale înseamnă: a) deșeuri amestecate și deșeuri colectate separat de la gospodării, inclusiv hârtia și cartonul, sticla, metalele, materialele plastice, [...] ambalajele, deșeurile de echipamente electrice și electronice, deșeurile de baterii și acumulatori”*. Hârtia sau PET-ul adus de o PF la centru e deci, după definiție, deșeu municipal, iar excepția l-ar scoate;
- **același alineat** numește însă borderoul „în cazul deșeurilor preluate de la persoane fizice”. Dacă toate deșeurile de la PF ar fi municipale, mențiunea n-ar avea obiect.

În plus, **art. 2** face SIATD obligatoriu doar pentru operatorii care *„gestionează deșeuri pentru”* o
OIREP, pentru anvelope sau pentru deșeuri municipale. Nu orice centru de colectare e în SIATD.

~~**Ce decide până la răspuns:**~~ *(înlocuit pe 26.09.2026, vezi mai jos)*
- ~~borderoul e **obligatoriu doar la metal** (OUG 31/2011), cu CNP, act și domiciliu (D1.7, deja în cod);~~
- ~~la celelalte intrări PF, borderoul se poate tipări **la cerere**, fără CNP (minimizarea din Legea 190/2018 art. 4, §16);~~
- impozitul se calculează doar pe liniile de metal, iar AFM pe toate.

Întrebarea e trecută la specialistă ca **BA** (`intrebari-specialist.md`).

#### 🔁 26.09.2026 — BA închisă pe partea contabilă: **borderoul e implicit la orice cumpărare plătită de la PF**

Temeiul nu e de mediu, e contabil:
- **Legea 82/1991 art. 6 alin. (1)** (§15): orice operațiune se consemnează *„în momentul efectuării ei”* într-un document
  justificativ;
- **OMFP 2634/2015, anexa 2 — „BORDEROU DE ACHIZIŢIE (Cod 14-4-13)”**: *„document de înregistrare în gestiune a bunurilor
  cumpărate de la persoane fizice [...] Borderoul de achiziție se întocmeşte de către persoana care efectuează
  aprovizionarea cu bunuri de la persoane fizice, în momentul achiziţiei.”* ([mfinante.gov.ro](https://mfinante.gov.ro/documents/35673/219198/anexe2_3ordin2634_2015.pdf));
- **anexa 1 pct. 2**: elementele obligatorii cer *„menţionarea părţilor”*, **nu CNP-ul**; **pct. 6**: documentele de
  cumpărare de la PF *„pot fi înregistrate în contabilitate numai în cazul în care se face dovada intrării în gestiune”*
  (bonul de cântar) ([static.anaf.ro](https://static.anaf.ro/static/10/Anaf/legislatie/OMFP_2634_2015.pdf)).
  Modificările OMFP 2634 (1447/2023, 4058/2024) nu ating aceste puncte.

Metodologia AFM a contribuției (Ordinul 578/2006, [72607](https://legislatie.just.ro/Public/DetaliiDocument/72607)) vorbește
încă de „3%” și numai la metale; pentru reținerea de 2% la nemetale **nu există text care să numească documentul** —
borderoul acoperă și golul acesta.

**Ce decide acum:**
- borderoul 14-4-13 se emite **implicit la orice intrare PF plătită**, fără opțiunea de a-l omite;
- **CNP, act și declarația de gospodărie proprie rămân doar la metal** (OUG 31/2011; minimizarea din Legea 190/2018 art. 4, §16);
- impozitul doar pe liniile de metal, AFM pe toate (neschimbat).

**Obligație nouă, Legea 249/2015 art. 20** ([172506](https://legislatie.just.ro/Public/DetaliiDocument/172506)):
> **(8)** Operatorii economici autorizați care preiau prin achiziție ambalaje folosite de la populație prin puncte de
> colectare [...] au obligația să notifice desfășurarea activității asociației de dezvoltare intercomunitară sau, după caz,
> unității administrativ-teritoriale [...] și să raporteze trimestrial acesteia cantitățile de deșeuri de ambalaje
> colectate de la persoanele fizice.
> **(10)** În cazul achiziției ambalajelor de la populație [...] operatorii economici autorizați au obligația să țină o
> evidență și să raporteze distinct cantitățile achiziționate.

Amendă **20.000–40.000 lei** (art. 26 alin. (1) lit. d)), constată Garda de Mediu. Alin. (7) lit. c) trimite încă la Legea
211/2011, abrogată (echivalent: OUG 92/2021 art. 60 pct. A lit. h)/i)). ~~Ordinul de la alin. (11) n-a fost căutat.~~
🔁 **Runda 2: ordinul există — Ordinul MM nr. 1.271/29.11.2018** (M.Of. 34/11.01.2019,
[209942](https://legislatie.just.ro/Public/DetaliiDocument/209942), procedura [209943](https://legislatie.just.ro/Public/DetaliiDocument/209943)),
fără modificări. Privește formal doar alin. (9) (achiziția „de la locul de generare”, cu dosar de înregistrare la UAT);
un depozit fix e la alin. (8) — notificare + raport trimestrial fără formă prescrisă, pentru care **anexa 3 a procedurii
e forma firească**: un raport pe punct de lucru, în kg, pe rânduri Hârtie/Carton, Plastic, Sticlă, Oțel, Aluminiu, Total
metal, Lemn, Altele; coloane cantitatea preluată, cantitatea comercializată și cumpărătorul (CUI, denumire, adresa punctului
de lucru, țara la export). Niciun termen în trimestru. Art. 20 n-a mai fost modificat din 2019.
**Pentru cod:** câmp „ambalaj de la populație” pe linie, raport trimestrial pe depozit în forma anexei 3, fișa notificării
către ADI/UAT pe depozit și memento trimestrial. La Andreea: forma și termenul cerute efectiv de UAT și dacă „cantitatea
comercializată” e toată vânzarea materialului sau doar partea de la PF.

**Rămâne deschisă doar latura SIATD** (dacă borderoul și CNP-ul PF se încarcă în SIATD), legată acum de OUG 196/2005
art. 10 alin. (12) (§6.1). Observație din platformă, nu normă: clientul web SIATD **cere CNP valid** la orice sursă PF
declarată. Risc de practică: ANAF a recalificat ca venit impozabil cumpărări mari de la PF când deșeul nu era „din
patrimoniul personal” (caz din 2023, [avocatnet.ro](https://www.avocatnet.ro/articol_65817/Impunere-suplimentar%C4%83-la-o-firm%C4%83-care-cump%C4%83ra-de%C8%99euri-reciclabile-de-la-persoane-fizice-dar-nu-pl%C4%83tea-impozitul-aferent-pl%C4%83%C8%9Bilor.html), sursă secundară).

### 18.4 AX — PF care își aduce singură deșeul: **borderou, nu Anexa 3**

Ce spune HG 1061/2008:
- **art. 21:** transportul spre colectare se face *„pe baza formularului [...] completat și semnat de către expeditorul, transportatorul și destinatarul”*;
- **art. 23:** scutește numai salubrizarea;
- **anexa nr. 3:** la expeditor are rubricile *„Autorizație de mediu nr.”*, *„Data la care expiră autorizația de mediu”* și *„Semnatura și ștampila”*, iar la transportator *„Licența de transport mărfuri nepericuloase nr.”*.

Consolidarea din 23.01.2026 n-a schimbat nimic aici: e sentința Curții de Apel Pitești nr. 225/F-CONT/2024,
care anulează art. 25 alin. (3) (confiscarea mijlocului de transport).

Formularul e construit pentru operatori economici: o persoană fizică n-are autorizație de mediu
(OUG 92/2021 art. 34 alin. (1^1): *„se emite numai pentru operatorii economici persoane juridice”*),
n-are ștampilă și n-are licență de transport.

Ce spun celelalte acte:
- **OUG 92/2021 art. 30 alin. (4):** *„Persoanele fizice au obligația să depună deșeurile provenite din gospodărie, pe tipuri, în sistemul de colectare separată [...] inclusiv în centrele prevăzute la art. 60 pct. A lit. h) și i)”*. PF *depune*, nu expediază un transport;
- Ordinul 701/2024 art. 18 alin. (3) numește borderoul ca sursă pentru deșeurile de la PF, dar cu excepția municipalelor (§18.3), deci **nu e un sprijin sigur** pentru AX și nu mai e numărat aici;
- formularul de transport apare în alin. (4) al aceluiași articol, dar la tranzacțiile cu ambalaje, între operatori.

**Ce decide:** la o intrare de la PF aplicația **nu** generează Anexa 3. Documentul e borderoul, alături
de bonul de cântar.
⚠️ **Rămâne un risc, și e scris aici, nu ascuns:** HG 1061/2008 nu conține o scutire *expresă* pentru
PF. Concluzia se sprijină pe rubricile formularului, pe art. 30 alin. (4) și pe SIATD, nu pe o frază
care să spună „nu se aplică”. Dacă un inspector cere totuși formularul, se poate adăuga ca opțiune
fără schimbare de model.

### 18.5 AY — acumulatorii auto (16 06 01\*) de la PF: **se pot primi**, ca deșeu periculos, fără CNP și fără impozit

**HG 1132/2008 art. 7 alin. (18)** (în vigoare pe Portal, forma din 17.12.2021):

> Utilizatorul final de baterii și acumulatori auto și industriali este obligat să predea deșeurile de
> baterii și acumulatori auto și industriale separat de alte deșeuri către: a) distribuitorii [...];
> b) unitățile care prestează servicii de înlocuire [...]; **c) punctele de colectare pentru deșeuri de
> baterii și acumulatori**; d) producător, după caz.

Utilizatorul final e definit la art. 3 ca *„orice persoană fizică [...] care cumpără ori dobândește
baterii [...] în scopul utilizării lor”*.

**Regulamentul (UE) 2023/1542 art. 61 alin. (1)** obligă producătorii să preia deșeurile de baterii SLI de
la distribuitori, refabricanți, instalațiile de tratare VSU/DEEE și autoritățile publice. Tot acolo:
*„Statele membre **pot** adopta măsuri prin care să impună condiția ca entitățile menționate [...] să poată
colecta [...] numai dacă au încheiat un contract cu producătorii”*. **O astfel de măsură românească nu
s-a găsit:** HG 1132/2008 n-a mai fost modificată din 2021.

**Ce decide:**
- un centru de colectare autorizat pentru 16 06 01\* îi poate primi de la PF;
- **nu** e metal după OUG 31/2011, deci nu cere CNP și nu intră sub impozitul de la §18.1: art. 62 lit. f) exceptă doar metalele;
- AFM 2% se reține;
- codul periculos intră doar dacă autorizația firmei îl acoperă. Asta e condiția depozitului, iar aplicația nu o poate verifica singură.

⚠️ **Rămâne deschis:** o eventuală măsură națională după art. 61 alin. (1) al doilea paragraf. Se reverifică
la D7.4.

### 18.6 AZ — capacitatea din autorizație: **pe tip de deșeu, în tone și volum**

**OUG 92/2021 art. 34 alin. (2)**, autorizația de mediu *„trebuie să conțină următoarea listă care nu este
exhaustivă”*:
- **lit. c):** *„tipurile și cantitățile exprimate în **tone și volum** de deșeuri care pot fi tratate, inclusiv originea acestora”*;
- **lit. d):** *„tipurile și cantitățile de deșeuri și/sau produse care rezultă din instalație exprimate în **tone/an** și volum”*.

**Ce decide pentru D3.4:** capacitatea se ține **pe depozit × cod**, în tone și în m³. Perioada se ia din
autorizație: lit. d) spune explicit „tone/an” pentru ce iese, iar lit. c) nu fixează perioada pentru ce
intră. Deci perioada („la un moment dat” sau „pe an”) e un câmp al rândului de capacitate, nu o
presupunere a aplicației.

#### 🔁 26.09.2026 — citit doar pe jumătate: art. 34 alin. (2) mai are **lit. a)** și **lit. j)**

([OUG 92/2021](https://legislatie.just.ro/Public/DetaliiDocument/245846), forma din 11.07.2026)
- **lit. a):** *„codul/codurile operațiilor de eliminare/valorificare potrivit anexelor nr. 3 și 7”*; **lit. b):**
  *„tehnologia aplicată pentru fiecare tip de operațiune”*;
- **lit. j):** *„specificarea perioadei de timp și a capacității de stocare exprimate în volum și tone a deșeurilor de pe
  amplasament”*.

**Ce decide pentru D3.4 (înlocuiește paragraful de mai sus):** trei limite distincte pe depozit —
1. **stocul de pe amplasament** (lit. j), în t și m³, cu **durata maximă de stocare**, comparat cu stocul la zi;
2. **cantitățile tratate** (lit. c), cu perioada scrisă în autorizație;
3. **ieșirile** (lit. d), în t/an.

Plus **vechimea stocului** pe depozit × cod (FIFO pe loturi), cu plafonul legal când autorizația tace: 3 ani înainte de
valorificare, 1 an înainte de eliminare (OG 2/2021 art. 3 alin. (2) lit. b), §8). Pe ecran, avertisment ferm, fără
blocare (deșeul e deja pe amplasament), ca la stocul negativ.

**Sancțiunea nu e în OUG 92/2021** (art. 62 alin. (1) lit. a) sancționează doar art. 34 alin. (1) și (5)), ci în **OUG
195/2005 art. 96 alin. (3) pct. 1**: *„obligația [...] persoanelor juridice de a funcționa cu respectarea prevederilor
autorizației”* — **50.000–100.000 lei** la persoane juridice ([67634](https://legislatie.just.ro/Public/DetaliiDocument/67634),
consolidare 07.04.2022 — un modificator ulterior nu e exclus).

**Pentru D5.2 (lit. a)):** fiecare depozit ține lista codurilor R/D din autorizația lui; o procesare poate purta **numai un
cod din listă** — R12 pentru sortare, balotare, compactare, mărunțire înainte de reciclare (nota 5, §2.2), D13/D14 înainte
de eliminare; R13/D15 e starea stocului, nu o operațiune de înregistrat. Fără R12 în autorizație, aplicația avertizează.
Niciun cod propus implicit. ~~**Rămâne deschis:** dacă sortarea colectorului schimbă codul (20 01 01 → 19 12 01)~~ — lista
deșeurilor pune 19 12 la *„tratarea mecanică a deșeurilor (de exemplu, sortare, sfărâmare, compactare, peletizare)”*, dar
definiția colectării (anexa 1 pct. 6) include și ea „sortarea” ([Decizia 2014/955/UE](http://publications.europa.eu/resource/celex/32014D0955);
[Comunicarea 2018/C 124/01](http://publications.europa.eu/resource/celex/52018XC0409%2801%29)). Aplicația permite un cod de ieșire
diferit, nu îl impune. Refuzul de la sortare (19 12 12) e o ieșire cu cod R/D, nu o ajustare.

#### 🔁 Runda 2, 26.09.2026 — codul după sortare: **regula e scrisă de ANPM** (note: `runda2_inventar_coduri.md`)

Sursa: ANPM, *„Statistica deșeurilor — Probleme întâmpinate la verificarea/validarea datelor”*, rev. 2022
([PDF, anmap.gov.ro](https://anmap.gov.ro/documents/14457/2230184/Recomandari%20completare%20chestionare%20SIM-SD%20_rev2022%20(003).pdf)):
*„chestionarul TRAT este singurul chestionar în care se poate schimba cod-ul de deșeu ca urmare a tratării acestuia”*;
ambalajele sortate separat de restul materialului **rămân la capitolul 15**, restul trece la 19; ieșirea pe material, fără
separarea ambalajelor, e **19 12**; deșeul de ambalaj colectat e 15 01, nu 20 01. Ordinul 794/2012 art. 8 alin. (3):
deșeurile de ambalaje se identifică numai cu coduri 15 01. Manualul Eurostat (2024): stocarea R13/D15 *nu* schimbă codul;
R12 nu e în lista operațiunilor care păstrează deșeul. Ghidurile SEPA (Scoția) și Schleswig-Holstein spun la fel
(sursă persuasivă, nu normă).

**Pentru cod (D5.2):**
- balotarea/compactarea **unui singur flux** păstrează codul de intrare (propus în formular);
- sortarea unui amestec pe fracții → **19 12 xx**, refuzul 19 12 12; fracția de ambalaj ținută separat rămâne **15 01 xx**;
- 15 01 01 + 20 01 01 balotate **împreună** → 19 12 01 și ies din raportarea de ambalaje ⇒ aplicația ține originea
  „ambalaj / neambalaj” pe tot drumul procesării și avertizează înainte de amestec;
- metale: colectarea, stocarea, sortarea pe calități păstrează codul; mărunțirea (shredder) → 19 10 01/02; capitolul 17
  nu se folosește niciodată pentru ieșiri de tratare;
- o schimbare de cod poartă avertismentul „se raportează în chestionarul TRAT”.

**R12 la sortarea preliminară (runda 2, `runda2_mediu.md`) — textul trage în ambele sensuri** (anexa 1 pct. 6 pune sortarea
în colectare; pct. 19 adaugă, doar în versiunea română, *„valorificarea (inclusiv sortarea)”*; nota de la R12 numește
sortarea și compactarea). **Regula de lucru:** separarea pe tipuri la recepție (fără refuz, fără schimbare de cod) nu cere
R12; orice procesare cu intrări și ieșiri cântărite (balotare, compactare, sortare cu refuz, 20 xx → 19 12 xx) cere R12 în
lista depozitului, altfel avertisment (fără blocare). Presele sunt „instalații” în TRAT cap. 8 (ANPM 2022).

#### 🔁 Runda 2, 26.09.2026 — cum scriu autorizațiile reale lit. c), d), j) (`runda2_autorizatii_cantar.md`)

> ⚠️ **`anpm.ro` nu mai există** (NXDOMAIN): agențiile județene sunt acum **Direcții Județene de Mediu ale ANMAP**, pe
> `djm<județ>.anmap.gov.ro` (WordPress; `/wp-json/wp/v2/media?search=<termen>` listează PDF-urile de autorizații). Orice
> link `apm*.anpm.ro` din documentele noastre e mort.

Citite: 7 autorizații emise (2022–2026) și 1 proiect (2026), din 7 DJM — fier vechi, hârtie/plastic/ambalaje, DEEE:
- **Niciuna nu scrie lit. j) cum o cere legea** (perioadă + capacitate în volum și tone).
- Cantitățile vin în patru forme: pe cod în t/an, pe cod în t sau kg pe lună, pe grupe de coduri în t/an, sau **deloc**
  (o autorizație are coloana goală la toate cele 13 coduri).
- „Stocate temporar” repetă de obicei cantitatea colectată, ca flux (ex. 600 t/an în ambele rubrici) — nu e o limită de
  stoc. **Doar două dau o limită reală:** un centru DEEE — *„Cantitatea maxima de deseuri existenta pe amplasament la un
  moment dat nu poate fi mai mare de 30 to”* — și un colector de hârtie/plastic (2022), cu coloană „Capacitate stocare” în t
  pe cod.
- m³ apar doar în proiect, ca m³/an; altfel capacitatea e dată în m² și volume de containere.
- Durata maximă apare într-o singură autorizație și repetă regula de 3 ani / 1 an din OG 2/2021.
- **R12 domină**, chiar unde autorizația spune că pe amplasament nu se sortează; una scrie că cântărirea, sortarea și
  stocarea sunt *„incluse în codul R12”*. R13 apare o dată. Balotarea și tăierea sunt descrise în text, fără cod separat.
- **Fiecare centru fix are autorizație pe punct de lucru**, cu adresă.

**Pentru cod (D3.4, înlocuiește „trei limite fixe”):** limita din autorizație e un rând tastat din PDF, cu **fel**
(colectat / stocat / valorificat), **bază** (pe cod / pe grupă / pe amplasament), **unitate** (t, kg, m³), **perioadă**
(an, lună, „la un moment dat”, niciuna) și **marcaj „aproximativ”** („cca.”, „aprox.”). Doar „la un moment dat” se compară cu
stocul la zi; an/lună se compară cu totalul perioadei. Fără durată în autorizație → 3 ani / 1 an (OG 2/2021). Lista R/D a
depozitului se copiază tot din autorizație, cu R12 frecvent pus „din oficiu”.

### 18.7 D3.6 — ajustarea de inventar: **legea deșeurilor tace** (26.09.2026)

OUG 92/2021 nu conține „perisabil”, „inventar” sau „umiditate”. Singurele „pierderi” sunt ratele medii de sortare de la
art. 19 alin. (5)–(6), folosite de ANMAP pentru obiectivele naționale, nu în evidența operatorului. „Cantitatea [...] care
rezultă” de la art. 48 alin. (1) lit. a) e ce iese dintr-o **operațiune** de valorificare/eliminare, nu o diferență de
inventar. Ghidul MMAP 2436/2023 ([276420](https://legislatie.just.ro/Public/DetaliiDocument/276420)) nu spune nimic.
Partea contabilă e închisă prin §15.1. **Până răspunde Andreea:** ajustarea e un document propriu, legat de
procesul-verbal, fără cod R/D, inclus în stoc, ținut în afara intrărilor și ieșirilor art. 48.

🔁 **Runda 2 — ce spune statistica (ANPM 2022, `runda2_inventar_coduri.md`):** chestionarul COLECTARE/TRATARE n-are coloană de
pierderi; pe fiecare cod trebuie să iasă *stoc inițial + colectat = valorificat + eliminat + stoc final* (verificarea
REC_002), deci o lipsă ascunsă doar în stocul final strică bilanțul. **Pentru pierderile de proces la o instalație de
tratare** ANPM a scris regula: *„pentru corelarea cantităților se va utiliza codul 99 99 99, iar în nota de introducere se va
preciza că au loc pierderi materiale”* — în tone, la ID-ul propriei instalații, cu codul R/D al ei. **Pentru o lipsă pură la
colectare (furt, eroare de cântar, uscare în stoc) nu există regulă scrisă**; „furt”/„sustragere” nu apar în OUG 92/2021,
HG 1061/2008 sau HG 856/2002 — furtul rămâne o chestiune contabilă.
**Pentru cod:** ajustarea rămâne document propriu; exportul pentru SIM calculează dezechilibrul pe cod și îl arată **ca rând
separat**, niciodată ascuns în stocul final; eticheta „99 99 99” doar pentru pierderile de proces ale unei instalații
declarate (presă, R12), nu pentru lipsuri sau furt. **La Andreea, restrâns:** la o lipsă a unui colector care depune doar
COLECTARE/TRATARE, APM acceptă rândul 99 99 99 sau doar stocul mai mic, cu verificarea în roșu și o notă? Un colector cu presă
depune și TRAT?

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
