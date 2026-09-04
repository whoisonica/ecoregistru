# EcoRegistru

Multi-tenant compliance SaaS for waste management in Romania: monthly waste records,
preparation of the mandatory SIM/AFM reports, deadline alerts, and one-click generation
of the inspection file.

Romanian waste operators are legally required to keep monthly waste-management records
(HG 856/2002) and to file SIM/AFM reports. Most still do it in spreadsheets — error-prone,
impossible to audit, and painful when an inspection arrives. Missing or incorrect records
carry fines of 20,000–40,000 RON (OUG 92/2021).

Built solo, end to end: architecture, backend, frontend and tests. Domain requirements
validated with compliance-reporting specialists.

```
/backend    Spring Boot 3.2 · Java 21 · PostgreSQL · Flyway · JWT · multi-tenant
/frontend   Vite · React 18 · TypeScript · Tailwind · TanStack Query
/docs       Regulatory research and the product one-pager
```

Code, comments and commits are in English; the UI is in Romanian
(`frontend/src/lib/strings.ts`).

---

## Engineering highlights

**Multi-tenancy that actually isolates.** Every table carries a `company_id`; a request-scoped
`TenantContext` populated by a `TenantFilter` scopes every query. Platform admins switch tenant
through an `X-Tenant-Id` header. A dedicated `TenantIsolationIT` suite asserts that no endpoint
ever returns another tenant's rows — the test I care most about in this codebase.

**Versioned schema, no surprises.** Flyway migrations with a real version history and
`ddl-auto=none` — the database is never shaped by Hibernate at runtime.

**A nomenclator generated from the primary source.** The 842 codes of the European List of Waste
(Commission Decision 2014/955/EU) are not hand-copied: `scripts/generate_waste_codes.py` parses the
Official Journal HTML on EUR-Lex and refuses to write the seed unless the structural checks pass —
unique codes, six-digit format, every code sitting under the chapter and subchapter its own digits
claim, and a per-chapter fingerprint. The same five checks run as a Java test, so a hand-edited or
half-downloaded list fails the build rather than a customer's records.

**Two registers, kept apart.** A movement records which legal evidence it belongs to: Anexa 1, for
waste the company generated in its own activity, or the chronological register of OUG 92/2021
art. 48, for goods taken over from third parties — which HG 856/2002 art. 2 alin. (1) keeps out of
Anexa 1 entirely. Every quantity that leaves the site carries its R/D operation code and its
operator, because that is what chapters 3 and 4 of the form report.

**Evidence engine.** `EvidenceCalculator` aggregates the Anexa 1 movements into twelve monthly
lines per (work point, waste code) per year, on the identity the form encodes —
`stock = previous + generated − recovered − disposed` — carrying the balance across empty months and
across years, and flagging negative balances. A handover is reported in the recovered or disposed
column its R/D code implies, never as a column of its own; a quantity that left without a code is
reported apart and marks the line incomplete. What left the site and is covered neither by opening
stock nor by a recorded generation is reported as generated too — the form's own header reads
"Generated — *of which:* recovered | disposed | left in stock", and a client who records only the
handover would otherwise get a sheet reading zero generated and a negative balance.
Exports to `.xlsx` (Apache POI) and `.pdf` (OpenPDF).

**Deadlines and alerts.** Automatic generation of the annual filing deadline and of the AFM
contributions a company actually owes — each on the cadence OUG 196/2005 art. 11 gives it: monthly
for the 2% withheld at source, quarterly for the circular-economy contribution, annually for
packaging. A single boolean would have sent a company with a yearly obligation eleven wrong alerts
a year. A daily cross-tenant scheduler emails T-7 / T-1 reminders with per-company deduplication.

**Inspection file.** `GET /api/v1/audit-file?year=&years=` streams a ZIP covering one to five years
— three is the retention period an inspection may ask for (OUG 92/2021 art. 48(5)), five the margin
the specialist asked for — a folder per year with the evidence (xlsx + pdf) and the official record
sheet, the partner authorizations once, and every movement attachment. It regenerates the monthly
evidence before packing: the cache is derived from movements, and a client who never pressed
"Regenerate" would otherwise be handed a bundle of empty official forms.

**Roles.** `PLATFORM_ADMIN` / `ADMIN` / `OPERATOR` / `CLIENT_VIEWER`, enforced at endpoint level.

Current status and the feature-by-feature log: [`docs/status.md`](docs/status.md).

---

## The generator module

The application is built around what a waste generator actually has to do, in the order it happens.

- **Closed register.** There is no self-registration. A prospective client fills in a public intake
  form; support reads the answers and creates the company from them. The answers become the account
  profile, and the profile decides what the screens offer — the R/D operations this business works
  with, the waste codes on its authorization, and the transport details only a collector is asked
  for. An unanswered profile narrows nothing.
- **Two axes on a partner.** What they are — generator or collector — and which way the invoice
  travels — client (we hand waste over and we invoice them) or supplier (they do the work and they
  invoice us). One partner is routinely both, so the commercial role is two flags, not an enum.
  Hauling is neither: it is a rubric of one particular transport, on the movement.
- **Three location levels.** Company address, work point address, and the internal generator — the
  section inside the work point that produced the waste, which is what Anexa 1 cap. 2 prints under
  "Secţia".
- **No "handover" operation.** HG 856/2002 anexa nr. 1 cap. 1 has no such column, and cap. 3 / 4
  report a quantity together with its R/D operation *and* the operator who performed it. Handing
  waste to a recycler is therefore a recovery performed by that partner.
- **Anexa 3 la HG 1061/2008** — the transport form — is generated from a recorded movement, rubric
  by rubric after the filled models. A load the recipient will weigh is recorded with **no
  quantity**: the cell prints empty, exactly as it reaches the depot on paper, and the monthly
  evidence line is reported provisional until the weight comes back. Neither zero nor an estimate
  stands in for a measurement.

## Regulatory note

The generic evidence export (Excel/PDF) is implemented and explicitly labelled as an
**unofficial summary**. The official record — the **Anexa 1 form of HG 856/2002**, header plus the
four chapters, one page per waste code — is now generated too, but only because the specialist sent
completed sheets to check it against; it was deliberately withheld until then, and this project
still does not invent official formats. The five code lists of chapter 2 (storage type, treatment
method, purpose, transport means, destination) are checked value by value against the form's own
legend, from two independent copies of it. The **SIM/AFM structures remain unimplemented**: the SIM
questionnaires (PRODDES, COL-TRAT) sit behind a login and nobody has shown us one.

Careful with the name **"Anexa 1"** — it denotes two unrelated documents, and confusing them is the
easiest way to break this codebase:

| | What the app prints | What it does not |
|---|---|---|
| **HG 856/2002, anexa 1** | the waste-management record: four chapters × twelve months, one page per waste code. Hazardous codes print with the asterisk art. 4 alin. (3) gives them — `13 02 08*` — and chapters 3 and 4 cite the annexes **in force**: anexa nr. 3 for recovery, anexa nr. **7** for disposal | — |
| **Ordinul 794/2012, anexa 1** | the packaging declaration — two tables by material, in kg, as the **`.xls`** art. 6 asks for by name. Both tables are summed from the movements recorded on `15 01 xx` codes | — |

"Anexa 3" is likewise two documents: HG 1061/2008 (the handover form the app prints) and
Ordinul 794/2012 anexa 3 (the annual packaging report of collectors and traders).

Where the law is quoted, it is quoted verbatim with a link to the primary source and the date it was
read: [`docs/surse-oficiale.md`](docs/surse-oficiale.md). Where the text does not settle a question,
the question is written down and asked rather than guessed, and no default is offered in the form.
None of the official portals (SIM/ANPM, AFM-online, SIATD) exposes a public third-party submission
API, so the product model is "we prepare, you submit": the app produces the reports, the client
uploads them. Research: [`docs/legislatie.md`](docs/legislatie.md).

---

## Running it locally

**Requirements:** Java 21 (Temurin is fine), Node 20+, PostgreSQL 15+ (developed on 17).
Gradle does not need to be installed — use the wrapper.

### 1. Database

```sql
CREATE USER eco WITH PASSWORD 'eco';
CREATE DATABASE ecoregistru OWNER eco;
```

Flyway creates the schema on first boot.

### 2. Backend

```bash
cd backend
# Windows PowerShell:
$env:SPRING_PROFILES_ACTIVE = "dev"
./gradlew.bat bootRun
```

Runs on `http://localhost:8080` — Swagger UI at `/swagger-ui.html`, health at
`/actuator/health`. The `dev` profile ships a throwaway JWT secret and seeds a demo tenant.
Cloudinary and email are optional in dev: the app boots without them and fails with a clear
error only if you actually use upload or email.

Production environment variables: `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`,
`JWT_SECRET` (Base64), `CLOUDINARY_URL`, `MAIL_HOST/PORT/USERNAME/PASSWORD/FROM`,
`FRONTEND_BASE_URL`.

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

Runs on `http://localhost:5173`, proxying `/api` to `:8080`.

### Demo accounts (dev profile, password `Parola123` for all)

| Email                     | Role           | Tenant             |
|---------------------------|----------------|--------------------|
| platform@ecoregistru.ro   | PLATFORM_ADMIN | — (global)         |
| admin@demo.ro             | ADMIN          | Demo Reciclare SRL |
| operator@demo.ro          | OPERATOR       | Demo Reciclare SRL |
| viewer@demo.ro            | CLIENT_VIEWER  | Demo Reciclare SRL |

The dev seed is deliberately rich: 3 work points, 5 partners and 34 movements across 6 months,
so the cumulative stock actually carries over and the evidence screens have something to show.
It also contains the traded-goods flow — glass taken over at the depot and passed on — which must
stay out of Anexa 1 and is the reason movements carry a register at all.

### Seeing it in the UI

Log in as `admin@demo.ro`. The demo tenant is type BOTH and has a profile answered (R3, R4, R5,
R13, D5), so the narrowing is visible rather than theoretical.

| What | Where | What to look for |
|---|---|---|
| Intake form | `/cerere-cont` — public, no login | Choose "Colector" and the transport block appears; choose "Generator" and it does not |
| Declaration header | `/cerere-cont` → "Cod CAEN" and "Funcția" | Both optional, and the hint says so: leave them blank and the annual declaration prints the rubric empty rather than a guess. They travel onto the company on approval |
| Type of generator | `/cerere-cont` → "Tipul de generator" | Producător / importator / comerciant. Tick only "Comerciant" and the form says what follows: no packaging declaration, but the Anexa 1 sheet stays |
| Requests inbox | **Clienți**, below the company list | "Creează contul" turns a request into a company with its profile and work point |
| Account profile | **Clienți** → edit a company | R/D codes, the waste codes of the authorization, transport details for a collector |
| Partner roles | **Parteneri** | Green = client, amber = supplier, grey = "rol nestabilit"; filter by role |
| Internal generators | **Setări**, under work points | The "Secţia" of Anexa 1 cap. 2 — birouri, producţie |
| Carrier | **Parteneri** → edit, tick "Transportator" | A tick, not a type: the same firm can be a collector *and* haul it. Only then does it ask for the goods-transport licence, and only then can it carry drivers. A pure haulage firm picks "— doar transportator —" as its type |
| Drivers | **Parteneri** → a carrier's form, and **Setări** for our own | Name, ID papers, usual plate. On **Mișcări** the carrier select groups the ticked ones first, and "Alege delegatul" fills the three Anexa 3 rubrics — still editable, and "— altcineva —" types them by hand |
| Origin of the waste | **Mișcări** → record a handover on a collector/"both" account | A radio with no preselection, and each option says which official form it reaches. Pick "preluat de la terți" and the quantity leaves Anexa 1 and the evidence sheet entirely — it is not your waste (HG 856/2002 art. 2 alin. (1)) |
| Packaging on the market | **Mișcări** → record a movement on a `15 01 xx` code | A tick, "Ambalaj pus de noi pe piața națională", decides whether the movement reaches Anexa 1 Ambalaje. Leave it off and the quantity stays in the waste record only — the boxes your stock arrived in were placed on the market by your supplier |
| Packaging register | **Ambalaje** | Every movement on a `15 01 xx` code, with what each line is missing. Record one with no material chosen on `15 01 04` and the row turns amber: the quantity stays out of the declaration until someone says aluminium or steel |
| Packaging declaration | **Ambalaje** → "XLS — formatul de depunere" | Two sheets, `Tabelul nr. 1` and `Tabelul nr. 2`, at the same cell addresses as the model. Table 1 is summed from the movements; the material gives the row, the kind of packaging gives the column |
| Narrowed operations | **Mișcări** → add | No "Predare" in the list; the R/D codes are the five in the profile, not all 28 |
| Weighed at unloading | **Mișcări** → add, tick the box | "Cantitate" is replaced by "Volum (mc)" — the only measure you have without a scale — and the movement saves with no weight at all |
| Chapter 2 | **Mișcări** → add | Storage type, treatment method, transport means and destination, under the waste code |
| Anexa 3 | **Mișcări** or **Evidențe** → row action | Three identical pages, drawn rubric by rubric against the stamped model: one header line, no copy labels — on paper the three copies are a carbon booklet, sorted after signing. The "Destinat:" box carries an X only where the movement was ticked — nothing is derived from the R/D code |
| Exit with no R/D code | **Mișcări**, or **Evidențe** → "Anexa 1 — lunar" | A red **"Fără cod R/D"** badge, not the amber one: the quantity left the site and reaches neither official column, so the sheet cannot be filed as it stands. Amber "De cântărit" is a legitimate wait; red is a gap |
| Setting a password | `/reseteaza-parola?code=…` — from the invite mail | The page an invited client lands on. Choosing a password is what enables the account; `/parola-uitata` issues a fresh link when the 30-minute code has expired |
| Handover register | **Evidențe** (default view) | Date, code, quantity, V/R or D + code, partner — and "De cântărit" where the weight is pending |
| Monthly Anexa 1 | **Evidențe** → "Anexa 1 — lunar" | The running stock, which is the only figure the register cannot show |
| **The Anexa 1 form** | **Evidențe** → "Fișa Anexa 1" | A PDF titled "Evidenţa gestiunii deşeurilor generate «year»", one page per waste code: header plus the four chapters, twelve rows and a TOTAL AN each |
| **The annual declaration** | **Evidențe** → "Declarația anuală" | The centralizator: one line per waste code — opening stock, generated, recovered, disposed, closing stock, and through whom — one page per work point. A row whose exits carry no R/D code is marked `(*)` on the stock, with the reason under the table |
| Control dossier | **Dosar de control** → download | The ZIP opens with `anexa1-«year».pdf` — the same four-chapter sheet — then `declaratie-anuala-«year».pdf`, and its `README.txt` names the 15 March deadline |
| Three years of dossier | **Dosar de control** → *Perioada* → „Ultimii 3 ani" | One folder per year (`2024/`, `2025/`, `2026/`), partner authorizations once at the root, and a `README.txt` that names any year with no evidence lines instead of shipping a blank sheet |
| Search without diacritics | **Mișcări** → add → waste code box → type `deseuri` | Results appear: the nomenclator is searched on a folded copy of code and name (V17) |
| The 15 March deadline | **Termene** | Reads "Anexa 1 — evidența gestiunii deșeurilor generate (anual, 15 martie)": the document, not the portal |
| The 25 February deadline | **Termene**, on a company whose profile answers "producător" or "importator" | The packaging report of Ordinul 794/2012 art. 6, at the county agency. Tick only "Comerciant", or leave the question unanswered, and it does not appear — an alert asserts something, so it stays silent where a screen would still offer |
| Lapsed recipient authorization | **Mișcări** → hand waste to a partner whose authorization expired before that date | An amber **"Autorizație expirată"** badge next to the partner, naming the expiry date. Anexa 3 still prints: the handover happened, and the warning stays off the paper that reaches the inspector |
| Tonnes for the filing | **Evidențe** → "Anexa 1 — lunar", below the table | The year's totals per waste code in tonnes, because OUG 92/2021 art. 48 alin. (1) asks for tonnes at filing while the sheet itself stays in kg. Nothing printed changes — it saves dividing by 1000 by hand on the day |
| Designated waste manager | **Clienți** → edit a company | Name, capacity, employee vs. delegated third party, training certificate — OUG 92/2021 art. 23 alin. (4)–(5). Not the contact person, who is the declaration's signature block. Leave it blank and the control dossier's `README.txt` says so out loud, because its absence is itself the finding |

### Tests

```bash
cd backend
./gradlew.bat test
```

Integration tests cover tenant isolation, evidence calculation, export correctness, movement
validation, company management and the four official documents the app prints — the HG 856/2002
record sheet, the annual declaration, the HG 1061/2008 transport form, and the packaging
declaration of Ordinul 794/2012.

---

## License

MIT — see [LICENSE](LICENSE).
