# EcoRegistru

Multi-tenant compliance SaaS for waste management in Romania: monthly waste records,
preparation of the mandatory SIM/AFM reports, deadline alerts, and one-click generation
of the inspection file.

Romanian waste operators are legally required to keep monthly waste-management records
(HG 856/2002) and to file SIM/AFM reports. Most still do it in spreadsheets — error-prone,
impossible to audit, and painful when an inspection arrives. Missing or incorrect records
carry fines of 40,000–60,000 RON for legal entities (OUG 92/2021 art. 62 alin. (1) lit. a),
for breaches of art. 48). Waste that has value and whose provenance cannot be proven is
confiscated outright, not fined (art. 64 alin. (1^1)).

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

**Deadlines and alerts.** Automatic generation of **four annual filing deadlines** and of the AFM
contributions a company actually owes — each on the cadence OUG 196/2005 art. 11 gives it: monthly
for the 2% withheld at source, quarterly for the circular-economy contribution, annually for
packaging. The annual terms are 15 March (the evidence itself, OUG 92/2021 art. 48(1)), 25 February
(the packaging report, Ordinul 794/2012 art. 6), **30 April** (used oils and construction waste,
art. 49(9)) and **31 May** (the waste prevention programme, art. 44(3)) — the last two found by
re-reading the framework act on its consolidated form, in September 2026, after they had been
missing from the calendar for a year. Each is generated only on a **positive** signal: a used-oil
code in the evidence, a recorded environmental permit, an answered profile. A company that owes
none of them is told nothing, because an alert is a claim. A single boolean would have sent a company with a yearly obligation eleven wrong alerts
a year. A daily cross-tenant scheduler emails T-7 / T-1 reminders with per-company deduplication.

**Inspection file.** `GET /api/v1/audit-file?year=&years=` streams a ZIP covering one to five years
— three is the retention period an inspection may ask for (OUG 92/2021 art. 48(5)), five the margin
the specialist asked for — a folder per year with the evidence (xlsx + pdf) and the official record
sheet, the partner authorizations once, and every movement attachment. It regenerates the monthly
evidence before packing: the cache is derived from movements, and a client who never pressed
"Regenerate" would otherwise be handed a bundle of empty official forms.

**It reports rather than recites.** Five obligations an inspection checks — separate collection, the
ANMAP register, the characterisation of hazardous waste, used oils, the prevention programme — sit in
the same sanctioned list as the evidence itself (art. 62(1)(a), 40,000–60,000 RON), and the dossier
used to pass over them in silence. Three of the five it can now *check* against what it holds and
prints as findings, naming the codes or the permit it found; the other two it can only *name*,
because nothing in the data decides them — and saying which is which is the point. The
characterisation is the sharpest: analysis bulletins attach to a **waste code**, not to a movement
(art. 8(4) describes a kind of waste, not a lorry run), so the dossier reports coverage per code —
"1 of 3, missing for 13 02 08*" — instead of restating the obligation. They carry no expiry date:
art. 48(2) says *hold* them, and how often an analysis must be redone is inspector practice, not
something in the act to be guessed at and then printed in a file an inspector reads.

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
None of the official portals (SIM/ANMAP, AFM-online, SIATD) exposes a public third-party submission
API, so the product model is "we prepare, you submit": the app produces the reports, the client
uploads them. Research: [`docs/legislatie.md`](docs/legislatie.md).

---

### Interface tests

`npm run e2e` in `frontend/` drives the **installed Chrome** through `playwright-core` — no browser
download — against the local dev server and a backend on the `dev` profile. Eleven suites, 273
checks: every screen opens clean, the action column stays reachable when a table scrolls, search
and sort and the URL filters do what they claim, typing `deseuri` finds as much as `deșeuri`, the
month filter is a real select that starts on the current month, the movement form marks the fields
it rejects, Escape inside the waste-code picker closes the list and not the whole form, a started
form asks before it closes and Escape over that question closes only the question, nothing scrolls
sideways at 375px, the public intake form marks the three fields it requires and scrolls to the
first one it rejects, the request inbox reads back every answer the client gave, the red "no R/D
code" badge leads from the dashboard through the filtered register to the movement itself, a
deactivated row can be found through the state filter and brought back, the packaging override
grid says which rows were saved and which were not, the packaging-origin question is asked of
an account that takes waste over from third parties and of no other — proved on both, by switching
tenant, because a narrowing that hides the field from everyone passes a one-sided check just as
easily as the right one does — and the dashboard's next-action band names the most expensive thing
still open, proved on two tenants because a band that always said the same sentence would pass a
single-tenant check just as well. The eleventh suite is the only one that does not look for a
particular string: it reads everything on eight screens, collects every «number + known noun» pair,
and checks the form of each against Romanian's three — `1 linie` · `2 linii` · `20 de linii` — so a
string written tomorrow falls under the rule without anyone adding a check for it.

They exist because on 07.09.2026, after sixteen UI slices that all passed `tsc --noEmit` and
`vite build`, the first real run found **seven defects** — four of them needed a button pressed. A
review the same day found three more, in the primitives rather than the screens, so they reached
every screen at once — and the user found a fourth: search demanded diacritics, so `miscari` found
nothing at all. See `frontend/e2e/README.md`, which also records what the suite **cannot**
cover — and the seed debt it named is now paid: the demo tenant carries an exit with no R/D code,
a handover awaiting the weighbridge, a partner with no authorization expiry, a movement with two
attachments, and a handover to a partner whose authorization had already lapsed on the day, so the
rules written around those states are proved on rows rather than on an empty table.

`E2E_CHANNEL=msedge` picks Edge; an empty `E2E_CHANNEL` falls back to Playwright's own Chromium,
for a machine with no Chromium-family browser at all.

## What is not in this repository

This repo is public. The working notes, the commercial planning, and the **reference corpus** — ten
completed Anexa 1 sheets received from real clients — live in a separate private repository, because
they name companies and quote from their filings. Several rules in this codebase are derived from
that corpus; where that is the case, the code comment cites **how many sheets support the rule**
rather than which ones.

If a comment refers to something you cannot find, that is why — not because it went missing.

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

Optional, both with a working default:

| Variable | Default | What it changes |
|---|---|---|
| `CORS_ALLOWED_ORIGINS` | `FRONTEND_BASE_URL` | Comma-separated list of origins a browser may call the API from. Matched **exactly** — a different scheme or port is a different origin. Only needed for a second client (own domain, mobile app). |
| `SENTRY_DSN` · `VITE_SENTRY_DSN` | *(empty)* | Error reporting, one per end. Empty means nothing is initialised and nothing is sent: no extra request, no cookie. Only unhandled 500s are reported, never a handled 4xx. |

Sessions last **8 hours**. Disabling a user, or resetting their password, ends their open sessions
on the next request — there is no revocation list to maintain, just a counter on the row
(`app_users.token_version`, `V32`).

The three endpoints reachable without a token — login, password reset, and the intake form — are
rate limited per IP, and login and reset also per email address. Over the quota they answer `429`
with `Retry-After`. Behind a proxy the client address is read from the **last** `X-Forwarded-For`
hop, which is the one Heroku's router appends; the first hop is whatever the caller chose to send.

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

Runs on `http://localhost:5173`, proxying `/api` to `:8080`.

### Demo accounts (dev profile)

The seed creates four accounts — a `PLATFORM_ADMIN` with no tenant, and an `ADMIN`, an `OPERATOR`
and a `CLIENT_VIEWER` of "Demo Reciclare SRL". **The password is not in this repository.** Set
`DEMO_PASSWORD` and it is used for all four; leave it unset and a random one is generated on every
boot and written to the log:

```
DEMO_PASSWORD nu e setată — parola conturilor demo pe pornirea asta: 8Qb2…
```

The addresses are printed by the seeder on the same line it logs the password.

Why it is not written down: this repository is public, and on 09.09.2026 the same accounts turned
out to exist in the **production** database as well — so a table here was handing any reader a
working platform-admin login to a live system. A fixture password is only a fixture password while
it cannot reach anything real.

The interface suite needs a password it knows in advance, so it takes the same value from
`E2E_PASSWORD` and refuses to start without it:

```bash
cd backend  && SPRING_PROFILES_ACTIVE=dev DEMO_PASSWORD=<ceva> ./gradlew bootRun
cd frontend && E2E_PASSWORD=<ceva> npm run e2e
```

The dev seed is deliberately rich: 3 work points, 5 partners and 34 movements across 6 months,
so the cumulative stock actually carries over and the evidence screens have something to show.
It also contains the traded-goods flow — glass taken over at the depot and passed on — which must
stay out of Anexa 1 and is the reason movements carry a register at all.

### Seeing it in the UI

Log in as `admin@demo.ro`. The demo tenant is type BOTH and has a profile answered (R3, R4, R5,
R13, D5), so the narrowing is visible rather than theoretical.

| What | Where | What to look for |
|---|---|---|
| Intake form | `/cerere-cont` — public, no login | Choose "Colector" and the transport block appears; choose "Generator" and it does not. Press "Trimite" on an empty form: the three required fields mark themselves and the page scrolls to the first — a banner that marks nothing is the defect this page kept longest |
| Not knowing the R/D codes | `/cerere-cont` → "Ce se întâmplă cu deșeul" | Twenty-eight tickboxes are folded behind a choice whose first option is "Nu știu — le stabilim împreună". An empty set was always a valid answer; now the form says so |
| Reading a request | **Clienți** → a request row → "Vezi cererea" | Every answer the client gave, in the order they gave them — including `notes`, the free-text box. An unanswered field shows as an empty dash rather than being skipped; a section nobody filled in collapses to one line |
| Request → company | **Clienți** → an approved request → "Vezi firma creată" | The row used to end at "Cont creat". It now opens the company it produced |
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
| Chapter 2 | **Mișcări** → add | Storage type and treatment method under "Depozitare și tratare"; transport means and destination under "Transport" — the form reads as eight named sections, not one list of thirty fields |
| Anexa 3 | **Mișcări** → the row's ⋯ menu, or **Evidențe** → row action | Three identical pages, drawn rubric by rubric against the stamped model: one header line, no copy labels — on paper the three copies are a carbon booklet, sorted after signing. The "Destinat:" box carries an X only where the movement was ticked — nothing is derived from the R/D code |
| Exit with no R/D code | **Mișcări**, or **Evidențe** → "Anexa 1 — lunar" | A red **"Fără cod R/D"** badge, not the amber one: the quantity left the site and reaches neither official column, so the sheet cannot be filed as it stands. Amber "De cântărit" is a legitimate wait; red is a gap |
| Setting a password | `/reseteaza-parola?code=…` — from the invite mail | The page an invited client lands on. Choosing a password is what enables the account; `/parola-uitata` issues a fresh link when the 30-minute code has expired |
| Handover register | **Evidențe** (default view) | Date, code, quantity, V/R or D + code, partner — and "De cântărit" where the weight is pending |
| Monthly Anexa 1 | **Evidențe** → "Anexa 1 — lunar" | The running stock, which is the only figure the register cannot show |
| What the movement does | **Mișcări** → add | A strip at the top of the form names the official documents the quantity will reach — Anexa 1, the art. 48 register, the packaging declaration — and updates as you answer. It reads the same expressions `buildInput` does, so it cannot disagree with what gets saved |
| Duplicate a movement | **Mișcări** → the row's ⋯ menu → "Duplică mișcarea" | Everything comes across except the date and the document number — the two rubrics that actually differ between two handovers |
| Compliance status | **Panou** | Green when nothing blocks filing. Otherwise the blockers, each with its consequence and a link: red for an exit with no R/D code (a gap), amber for a line awaiting the weighbridge (a legitimate wait) |
| From the blocker to the fix | **Panou** → red blocker → "Vezi liniile" | The handover register, filtered to the exits with no R/D code, with the filter named and removable. Each row carries "Completează codul", which opens that movement — the badge used to be a dead end on three screens |
| Platform admin with no company picked | log in as `platform@ecoregistru.ro` | Every company-scoped screen says which company it needs and where to pick one, instead of rendering and firing four requests that answer `400`. **Clienți** is the exception, on purpose: it is the screen the companies are picked from |
| Search any table | any table with more than ten rows | The search box appears from ten rows up and works on what is already loaded. Every word must match somewhere, so "hamburger 15 01" finds the row |
| Keyboard | anywhere | **Ctrl+K** jumps to any screen *and* starts one — type "fișa", "anexa 1" or "inspector" and the screen that prints that document comes up; "predare" offers "Adaugă mișcare", which opens the form on **Mișcări**. Nothing in the palette writes: "regenerează" is deliberately absent, because from a palette you cannot see which year it would rewrite · **/** focuses the current table's search · **N** opens the add form where the account may write |
| Narrow screens | resize below 1024px | The sidebar becomes a drawer behind a menu button; form grids stack; the movement dialog rises from the bottom edge |
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
| Picking a month | **Mișcări** → the "Luna" filter | Two plain selects, month then year, because `<input type="month">` does not exist in Safari or Firefox — there it degrades to a free-text box. The screen opens on the current month, so it no longer fetches the whole history on every visit; "Tot anul" is the way back, and a month with no rows says which month is empty and offers the year |
| Closing a started form | **Mișcări** → add → type anything → Escape | It asks. On an untouched form it just closes — a question about an empty form is noise. Escape over the question closes only the question: what you typed is still there |
| Your own company's data | **Setări** → "Datele firmei" | CAEN, the environmental authorization, the designated person, the Anexa 3 series — read-only, because they are edited from **Clienți**, which is platform-admin. Unfilled rubrics show as "Necompletat" rather than being skipped: an empty CAEN prints empty on the annual declaration |
| Undoing a deactivation | **Setări** or **Parteneri** → deactivate a row, then switch the state filter to "Inactive" | "Reactivează" on the row. Deactivation never deleted anything — old movements quote the row — so it was never meant to be final. The state filter starts on "Active" and stays hidden until something has actually been deactivated |
| Reading an attachment | **Mișcări** → June 2026 → the "📎 2" cell | It opens the files by name, each a link. It used to be a number and nothing else: the only way to the document ran through the thirty-field edit form — which a **VIEWER** cannot open at all, since "Editează" sits behind `canWrite`. Read-only on purpose: deleting stays in the form, next to uploading, where the confirmation is |
| Adding a partner | **Parteneri** → "Adaugă partener" | Five named sections instead of a column of fifteen blocks: who they are · what they do · transport · authorization · what prints on Anexa 3. The CUI now sits next to the name, where it used to be separated from it by a question about lorries |
| Finding your way down **Ambalaje** | **Ambalaje** | A sticky table of contents over the longest page in the app: four large tables plus a 66-cell grid. It sits above the amber "what blocks the declaration" panel, which comes and goes with the month — a contents bar that moved with it would be a different bar on every visit |
| Which rows of the override grid were saved | **Ambalaje** → "Scrie cifre proprii" → type in a cell | The grid saves a row at a time, when you leave a cell, and now says so: the row reads "nesalvat" while the figure is still yours, "se salvează…" while it flies, "salvat" once it lands, and a line under the grid counts the rows still unsaved. Only errors used to be visible, so sixty-six cells gave no way of knowing how many had arrived |
| Who is asked where the packaging came from | **Parteneri** → "Adaugă partener" | The question only appears on an account that can take waste over from third parties. It feeds the Anexa 3 Ambalaje of Ordinul 794/2012 — collectors, traders, recyclers — so on a pure generator's account it has no answer to give. A partner that already carries one keeps showing it |
| What the stock tile counts | **Panou** | Codes with stock, and the largest three by name — not kilograms summed across codes, which was paper plus waste oil plus household waste in one figure that exists nowhere physically. A negative stock on one code used to be cancelled by the positives of the others; now it is counted, named and coloured |
| From a deadline to the document that clears it | **Termene** | Each row says how many days are left (or by how many it is overdue), and the annual filings link to the screen that prints them — for the year **reported**, not the year of the deadline. The AFM contributions link nowhere on purpose: those are declared in AFM's own application, and we print no form for them |
| What to do next | **Panou** | One band at the top names a single thing to do, picked by what costs most if left undone: an overdue deadline, then an exit with no R/D code, then a filing within thirty days (linking to the document that clears it, for the year *reported*), then a lapsing authorization, then the weighbridge. It stays silent until every source has loaded — "you are up to date" written over a half-loaded answer is the false green this codebase has paid for before |
| When an answer did not arrive | **Panou** — stop the API and reload | The band, the compliance card, the tiles and both lists say so, each in its own words. The guard used to cover requests *in flight* only: a **failed** one leaves `isLoading` false and `data` undefined, which `?? []` turns into an empty list — so a company with nine overdue deadlines and an unclassified exit was told "you are up to date", with no error anywhere on the screen. "I could not read this" is the third state, and the expensive one to be missing |
| A company with nothing recorded yet | **Panou** on a new account | Not "you are up to date" — that is true and useless. No work point yet: "add the first work point", because a movement is recorded *on* one. Work point but nothing recorded: "record the first movement", because the register, the record sheet and the declarations are all computed *from* movements. Neither step is a guess: both are dependencies the code already enforces |
| Unofficial exports | **Evidențe** → "Alte descărcări" | The two official documents stay in the header; the generic exports moved into a menu that says what they are. They print "unofficial summary" on themselves, so five equally prominent buttons claimed all five were the same kind of thing — and squeezed the page title onto three lines |
| A partner you may already have | **Parteneri** → "Adaugă partener" → type two letters of a name you already use | The duplicate suggestion used to swap the dialog into edit mode silently, throwing away everything typed — the only sign was the title. It still switches, because that is what you wanted; it now says so, and offers the way back with the name you typed restored. Fill in more than the name first and it asks before switching |
| A recipient whose authorization had lapsed | **Mișcări** → a handover with the amber "Autorizație expirată" badge | The badge told you to update the partner's record and led nowhere. It is a link now, carrying the expiry date on screen rather than behind a hover — it cannot be a tooltip, because a tooltip renders its own button and could not wrap a link |
| Why a driver's ID papers are held | **Setări** → "Șoferii noștri", and a carrier's own form | A retention note where the field is typed: it is held for the "Date de identificare delegat" rubric of Anexa 3 and printed on it; it stays as long as the record must be kept (OUG 92/2021 art. 48 alin. (5) — at least three years, twelve months for carriers); and deactivating a driver does not remove them from movements already recorded, which keep the snapshot of the day |

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
