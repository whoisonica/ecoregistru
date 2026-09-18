# WasteHouse

*Formerly EcoRegistru. The app runs at [app.wastehouse.ro](https://app.wastehouse.ro), the product page at [wastehouse.ro](https://wastehouse.ro). The Java package `ro.ecoregistru`, the Heroku apps and this repository keep the old name on purpose: none of them reach a client, and renaming them would touch applied migrations and the deploy pipeline for no visible gain.*

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
/backend    Spring Boot 3.5 · Java 21 · PostgreSQL · Flyway · JWT · multi-tenant
/frontend   Vite · React 18 · TypeScript · Tailwind · TanStack Query
/docs       Regulatory research and the product one-pager
```

Code, comments and commits are in English or Romanian, following whichever the file being
touched already uses (`CLAUDE.md`); the UI is in Romanian (`frontend/src/lib/strings.ts`).

---

## Engineering highlights

**Multi-tenancy that actually isolates.** Every table carries a `company_id`; a request-scoped
`TenantContext` populated by a `TenantFilter` scopes every query. Platform admins switch tenant
through an `X-Tenant-Id` header. Three dedicated suites attack that boundary rather than assume it:
one walks the whole resource × verb matrix with two fully populated tenants, one exercises the
platform admin's tenant switch (A→B→A, plus a missing, malformed or borrowed header), and one
sweeps every guarded endpoint with every role beneath it. Refusals expect `404`, never `403` — a
403 confirms the id is real — and **every refused write is followed by a read of the other tenant's
row from the database**, because a 404 that still writes is worse than an honest 200. These are the
tests I care most about in this codebase.

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

**The movement list is paged, searched and sorted by the database.** Not paging alone, and that is
the design: a search box that only looks inside the twenty-five rows on screen answers confidently
and wrongly, so the three moved together, with the browser's rules carried over word for word —
diacritics folded away, the typed phrase winning over its words, and every word having to *begin* a
word of the row, so that "02" does not match the 2026 of a date. The ordering is always total (a
created-at tiebreaker), because two rows tied on the sorted column are otherwise free to swap places
between requests, which shows one row on two pages and another on none. Sortable columns are a
whitelist, and anything outside it falls back to the default order rather than being refused: a stale
bookmark should open a table, not an error.

**An audit journal that cannot be forgotten.** Who changed what, and when, over the writes that
reach a filed document or concern a person. It is captured in Hibernate's flush interceptor rather
than by a call written into each service — a rule kept by discipline is a rule that the sixth edit
path will not know about — and bounded by an explicit allowlist of ten types, because an audit trail
is a promise made in a data-processing agreement, not a debug log: what goes in has to be a choice
rather than a remainder. Soft deletes and deactivations are written as **deeds**, not as a boolean
flipping: nobody searches a journal for "deleted: false → true". Rows are written in the same
transaction as the change they describe, so a refused write leaves no trace and a successful one
cannot fail to leave one. Nothing in the application can edit or remove a row.

**Deadlines and alerts.** Automatic generation of **five annual filing deadlines** and of the AFM
contributions a company actually owes — each on the cadence OUG 196/2005 art. 11 gives it: monthly
for the 2% withheld at source, annually for packaging (the quarterly circular-economy contribution
belongs to landfills and was removed in `V60`). The annual terms are 15 March (the evidence itself,
OUG 92/2021 art. 48(1)), 25 February (the packaging report, Ordinul 794/2012 art. 6, and for a
collector the Anexa 3 of the same order, art. 4), **30 April** (used oils and construction waste,
art. 49(9)) and **31 May** (the waste prevention programme, art. 44(3)) — the last two found by
re-reading the framework act on its consolidated form, in September 2026, after they had been
missing from the calendar for a year. Each is generated only on a **positive** signal: a used-oil
code in the evidence, a recorded environmental permit, an answered profile. A company that owes
none of them is told nothing, because an alert is a claim. A single boolean would have sent a company with a yearly obligation eleven wrong alerts
a year. Only the **next** occurrence of each kind is kept on the calendar; a missed one stays overdue until it is ticked and
gets one notice the day after. **Termene** has three tabs — to do, ticked, past (the current year, including what the profile
says was owed but was never saved, marked "Calculat"). A daily cross-tenant scheduler, on Romanian time, emails T-7 / T-1
reminders with per-company deduplication.

**Inspection file.** `GET /api/v1/audit-file?year=&years=` streams a ZIP covering one to five years
— three is the retention period an inspection may ask for (OUG 92/2021 art. 48(5)), five the margin
the specialist asked for — a contents file, the partner authorizations once, `rapoarte/` with the official
reports (the record sheet, the centralised evidence and the packaging annexes that apply) and `atasamente/`
with every movement attachment, a folder per year when there are several. The unofficial summary stays on the
Generare screen, not in the dossier. Each attachment download has a deadline on the whole body, so a stalled file is
listed as not included rather than holding the archive open. It regenerates the monthly
evidence before packing: the cache is derived from movements, and a client who never pressed
"Regenerate" would otherwise be handed a bundle of empty official forms.

**It reports rather than recites.** Four obligations an inspection checks — separate collection, the
ANMAP register, used oils, the prevention programme — sit in the same sanctioned list as the evidence
itself (art. 62(1)(a), 40,000–60,000 RON), and the dossier used to pass over them in silence. Two of
the four it can *check* against what it holds and prints as findings, naming the codes or the permit
it found; the other two it can only *name*, because nothing in the data decides them — and saying
which is which is the point. A fifth, the analysis bulletins that characterise hazardous waste, was
built and then removed on 14.09.2026 on the environmental specialist's advice: a generator does not
file them, and the rules are being rewritten.

**Roles.** `PLATFORM_ADMIN` / `ADMIN` / `OPERATOR` / `CLIENT_VIEWER` / `CONSULTANT`, enforced at
endpoint level. A consultant belongs to a consultancy (`V40`) and may pick only the companies that
consultancy manages; any other `X-Tenant-Id` answers exactly like an id that does not exist.

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

### Interface style

The UI follows one written direction, **"Cântar"** (the weighbridge), chosen by the owner on 15.09.2026 after a
friendlier SaaS-looking pass was rejected: a graphite side panel with keys and an LCD-style month display, the page
itself white like a weighbridge ticket, IBM Plex Sans and Mono served by the app itself (not from Google, which would
hand the client's IP to a third party), thin rules instead of shadows, small corners, states shown as an LED square
plus a word, and a bin-colour swatch before every waste code. Generation, intake and dispatch are three screens with
their own routes and server-side totals above each list. Screen copy names what the person does before the article
of law that requires it. The tokens live in `frontend/tailwind.config.js` and `frontend/src/index.css`, the panel in
`frontend/src/components/panel`, the primitives in `frontend/src/components/ui`, and the rules — twelve for forms,
plus what never changes: the movement form's field order, the printed documents, red versus amber — in
[`docs/stil-interfata.md`](docs/stil-interfata.md). There is no dark theme, on purpose.

### Interface tests

`npm run e2e` in `frontend/` drives the **installed Chrome** through `playwright-core` — no browser
download — against the local dev server and a backend on the `dev` profile. Thirty-seven probes (listed in `frontend/e2e/run.mjs`, run in CI on a fresh database): every screen opens clean, the action column stays reachable when a table scrolls, search
and sort and the URL filters do what they claim, typing `deseuri` finds as much as `deșeuri`, the
month filter is a real select that starts on the current month, the movement form marks the fields
it rejects, Escape inside the waste-code picker closes the list and not the whole form, a started
form asks before it closes and Escape over that question closes only the question, nothing scrolls
sideways at 375px, the public intake form, in four steps, marks the fields it requires and scrolls to the
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

**Requirements:** Java 21 (Temurin is fine), Node 20+, PostgreSQL 15+ (production runs 18.3; the test suite runs 15.6 embedded).
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

### 4. Mobile app (optional, early)

`mobile/` is an Expo SDK 57 app (React Native, Expo Router, TanStack Query) that talks to the same backend. It is a
**development build**, not Expo Go, so the first run compiles the native app: Xcode with an iOS simulator runtime and
CocoaPods for iOS; the Android SDK with an emulator for Android.

```bash
cd mobile
npm install
npx expo run:ios --no-bundler -d "iPhone 17"     # builds and installs once
npx expo run:android --no-bundler                 # with an emulator already running
npx expo start --dev-client                       # Metro, for both
```

**The app talks to the deployed API out of the box** — nothing to start on the machine. For local work, put
`EXPO_PUBLIC_API_URL` in `mobile/.env.local` (git-ignored, see `.env.local.example`): `http://localhost:8080` for the
iOS simulator, `http://10.0.2.2:8080` for the Android emulator, the machine's LAN address for a real phone. Expo reads
`EXPO_PUBLIC_*` when Metro starts, so changing it means restarting the bundler. Screen text and types are imported
straight from
`frontend/src/lib` (`strings.ts`, `types.ts`, `movementScreens.ts`, `binColor.ts`), so new copy and the rule for which
movement screens a company gets live in one place. The screen checks are Maestro flows under `mobile/maestro/` — their
headers say how to run them.

**Photographing the delivery note.** "Adaugă" → "Pozează avizul" reads the aviz on the phone (Vision on iOS, ML Kit on
Android, with the model bundled), and every field it found asks for "Corect" before the handover is saved. The parser is
plain code with its own tests (`npm run test:aviz`, invented notes only). A handover saved without signal waits in a
SQLite outbox and leaves on its own — once, thanks to a `clientGeneratedId` given at creation.

**Device sessions.** A phone signs in with `deviceName` in the login body and gets a long-lived refresh token next to
the eight-hour access token; `POST /api/v1/auth/refresh` exchanges it for a new pair, rotating it each time. The row
behind it lives in `device_sessions` as a SHA-256 hash, expires 60 days after last use, and is revoked by signing out,
by a password reset and by deactivating the account. `GET /api/v1/auth/devices` is the "connected devices" list. A
browser sends no `deviceName`, so nothing about the web session changed.

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
| Depot weighbridge | **Cântar** (only for companies that keep the art. 48 register) | One numbered operation per truck, with a line per sortiment: type gross and tare and the net fills itself and locks — the figure comes from the weighbridge, not from typing. Fill a price and the payment block shows what is withheld at source (2% to the environment fund, 10% income tax on metal bought from an individual) and what is left to pay. Nothing counts anywhere until you finalize |
| Withheld at source | **Cântar** → the strip above the list | What the month owes and the date it is due: 25th of the next month. The rates come from the server, so an old month keeps the rate that was withheld then |
| Purchase slip from an individual | **Cântar** → a finalized intake from an individual → "Borderou" | The OUG 31/2011 slip, numbered at first print: at metal the CNP, ID card, household declaration and both withholdings with the rates in force; without metal only the name. Paying one person more than 10,000 lei in cash on the same day shows a warning in the form |
| Transport papers per truck | **Cântar** → a saved outgoing operation → "Anexa 3" / "Aviz" | One form with every line of the truck, not one per sortiment. The Anexa 3 number comes from the same series as the movements; hazardous lines stay off Anexa 3 and on the aviz. Anexa 2 stays on the movement |
| The depot's month | **Cântar** → "Registrul lunii" | An xlsx with one row per sortiment, in weighbridge order: gross, tare, net, status, who cancelled and why. Prices only for someone allowed to see them |
| Origin on the art. 48 record | **Intrări și ieșiri** → the chronological export | An "Originea" column on takeovers: "populaţie" for an individual, otherwise what the operation or the partner says. Nobody classified it → the cell stays empty |
| Intake form | `/cerere-cont` — public, no login | Four steps: company, work point, contact, wastes; almost every field is required. Choose "Colector" and the transport block appears; choose "Generator" and it does not. Press "Continuă" on an empty step: the fields mark themselves and the page scrolls to the first — a banner that marks nothing is the defect this page kept longest |
| Not knowing the R/D codes | `/cerere-cont` → "Ce se întâmplă cu deșeul" | Twenty-eight tickboxes are folded behind a choice whose first option is "Nu știu — le stabilim împreună". An empty set was always a valid answer; now the form says so |
| Reading a request | **Clienți** → tab "Cereri de cont" → "Vezi cererea" | Every answer the client gave, in the order they gave them — including `notes`, the free-text box. An unanswered field shows as an empty dash rather than being skipped; a section nobody filled in collapses to one line |
| Request → company | **Clienți** → "Cereri de cont" → "Creează contul", or an approved request → "Deschide firma" | "Creează contul" opens **Client nou** (`/clienti/nou?cerere=`) with the answers filled in; an approved request opens the company it produced |
| Declaration header | `/cerere-cont` → "Cod CAEN" and "Funcția" | Both optional, and the hint says so: leave them blank and the annual declaration prints the rubric empty rather than a guess. They travel onto the company on approval |
| Type of generator | `/cerere-cont` → "Tipul de generator" | Producător / importator / comerciant. Tick only "Comerciant" and the form says what follows: no packaging declaration, but the Anexa 1 sheet stays |
| New client | **Clienți** → "Client nou" (N) | Four steps — the company (CUI → ANAF), what it does, the subscription, the administrator — saved in one transaction: a CUI already taken or an email that already has an account undoes the company too. A CUI is the same company with or without "RO" |
| Account profile | **Clienți** → "Deschide" → tab "Profil" (`/clienti/:id`) | R/D codes, the waste codes of the authorization, transport details for a collector |
| Partner roles | **Parteneri** | Green = client, amber = supplier, grey = "rol nestabilit"; filter by role |
| Internal generators | **Setări**, under work points | The "Secţia" of Anexa 1 cap. 2 — birouri, producţie |
| Carrier | **Parteneri** → edit, tick "Transportator" | A tick, not a type: the same firm can be a collector *and* haul it. Only then does it ask for the goods-transport licence, and only then can it carry drivers. A pure haulage firm picks "— doar transportator —" as its type |
| Drivers | **Parteneri** → a carrier's form, and **Setări** for our own | Name, ID papers, usual plate. On **Mișcări** the carrier select groups the ticked ones first, and "Alege delegatul" fills the three Anexa 3 rubrics — still editable, and "— altcineva —" types them by hand |
| Origin of the waste | **Generare** or **Intrări și ieșiri** on the "both" demo account | No radio any more: the screen is the answer. A movement recorded on "Intrări și ieșiri" is taken over from third parties and never reaches Anexa 1 or the evidence sheet — it is not your waste (HG 856/2002 art. 2 alin. (1)). Rows marked **Mișcări** in this table now live on whichever of the two screens holds the movement; `/miscari` still redirects |
| Packaging on the market | **Mișcări** → record a movement on a `15 01 xx` code | A tick, "Ambalaj pus de noi pe piața națională", decides whether the movement reaches Anexa 1 Ambalaje. Leave it off and the quantity stays in the waste record only — the boxes your stock arrived in were placed on the market by your supplier |
| Packaging register | **Ambalaje** | Every movement on a `15 01 xx` code, with what each line is missing. Record one with no material chosen on `15 01 04` and the row turns amber: the quantity stays out of the declaration until someone says aluminium or steel |
| Packaging declaration | **Ambalaje** → "XLS — formatul de depunere" | Two sheets, `Tabelul nr. 1` and `Tabelul nr. 2`, at the same cell addresses as the model. Table 1 is summed from the movements; the material gives the row, the kind of packaging gives the column |
| Narrowed operations | **Mișcări** → add | No "Predare" in the list; the R/D codes are the five in the profile, not all 28 |
| Weighed at unloading | **Mișcări** → add, tick the box | "Cantitate" is replaced by "Volum (mc)" — the only measure you have without a scale — and the movement saves with no weight at all |
| Chapter 2 | **Mișcări** → add | Storage type and treatment method under "Depozitare și tratare"; transport means and destination under "Transport" — the form reads as eight named sections, not one list of thirty fields |
| Anexa 3 | **Generare** → the row's ⋯ menu | Three identical pages, drawn rubric by rubric against the stamped model: one header line, no copy labels — on paper the three copies are a carbon booklet, sorted after signing. The "Destinat:" box carries an X only where the movement was ticked — nothing is derived from the R/D code |
| Exit with no R/D code | **Generare**, either tab | A red **"Fără cod R/D"** badge, not the amber one: the quantity left the site and reaches neither official column, so the sheet cannot be filed as it stands. Amber "De cântărit" is a legitimate wait; red is a gap |
| Setting a password | `/reseteaza-parola?code=…` — from the invite mail | The page an invited client lands on. Choosing a password is what enables the account; `/parola-uitata` issues a fresh link when the 30-minute code has expired |
| Handover register | **Generare** → *Mișcări* | Date, code, quantity, V/R or D + code, partner — and "De cântărit" where the weight is pending. Until 18.09.2026 this list had a second home on an "Evidențe" screen, which aggregated the very same `ANEXA_1` movements |
| The year's totals per waste code | **Generare** → *Totalul anului* | One row per waste code: generated, recovered, disposed, and whether it can be filed ("Gata" · "N de cântărit" · "N kg fără cod R/D"). The two official documents sit **above** the table, not under it — on 15 March you come here for the paper — and each button carries the document's own name rather than "Descarcă", because two identically anonymous buttons side by side are how the wrong sheet gets filed. "Recalculează acum" sits last and deliberately looks lighter: it is the one control here that *writes* (it rebuilds the evidence, and the later years with it, because stock carries over), not one that hands you a file. There is no stock column: a generator keeps no stock, so on Anexa 1 the figure is zero by construction (see the row below). These are the figures typed into SIM on 15 March; the monthly breakdown stays on the printed sheet |
| What the movement does | **Mișcări** → add | A strip at the top of the form names the official documents the quantity will reach — Anexa 1, the art. 48 register, the packaging declaration — and updates as you answer. It reads the same expressions `buildInput` does, so it cannot disagree with what gets saved |
| Duplicate a movement | **Mișcări** → the row's ⋯ menu → "Duplică mișcarea" | Everything comes across except the date and the document number — the two rubrics that actually differ between two handovers |
| Compliance status | **Panou** | Green when nothing blocks filing. Otherwise the blockers, each with its consequence and a link: red for an exit with no R/D code (a gap), amber for a line awaiting the weighbridge (a legitimate wait) |
| From the blocker to the fix | **Panou** → red blocker → "Vezi liniile" | **Generare** → *Mișcări*, the whole year, filtered to the movements with no R/D code, with the filter named and removable. The row is edited where it lives — the badge used to be a dead end on three screens |
| Platform admin with no company picked | log in as `platform@ecoregistru.ro` | Every company-scoped screen says which company it needs and where to pick one, instead of rendering and firing four requests that answer `400`. **Clienți** is the exception, on purpose: it is the screen the companies are picked from |
| Search any table | any table with more than ten rows | The search box appears from ten rows up and works on what is already loaded. Every word must match somewhere, so "hamburger 15 01" finds the row |
| Keyboard | anywhere | **Ctrl+K** jumps to any screen *and* starts one — type "fișa", "anexa 1" or "inspector" and the screen that prints that document comes up; "predare" offers "Adaugă mișcare", which opens the form on **Mișcări**. Nothing in the palette writes: "regenerează" is deliberately absent, because from a palette you cannot see which year it would rewrite · **/** focuses the current table's search · **N** opens the add form where the account may write |
| Narrow screens | resize below 1024px | The sidebar becomes a drawer behind a menu button; form grids stack; the movement dialog rises from the bottom edge |
| **The Anexa 1 form** | **Generare** → *Totalul anului*, or **Dosar de control** | A PDF titled "Evidenţa gestiunii deşeurilor generate «year»", one page per waste code: header plus the four chapters, twelve rows and a TOTAL AN each |
| **The aviz** | **Generare** → "Aviz de însoțire" on any handover | A new tab with the delivery note (*aviz de însoțire a mărfii*): header, sender, recipient, transport, the waste line and two signature boxes. The driver prints as "name \| CNP \| ID"; the note's number is the movement's document reference |
| PDFs without downloading | any PDF button — Anexa 3, Anexa 2, aviz, the two evidence sheets | The document opens in a new browser tab, where it can be printed or saved. Spreadsheets and the dossier ZIP still download |
| **The centralised evidence** | **Generare** → *Totalul anului*, or **Dosar de control** | The centralizator: one line per waste code — opening stock, generated, recovered, disposed, closing stock, and through whom — one page per work point. A row whose exits carry no R/D code is marked `(*)` on the stock, with the reason under the table |
| Control dossier | **Dosar de control** → download | `00-cuprins.txt` (names the 15 March deadline), `autorizatii-parteneri.pdf` (landscape, diacritics, coloured status, waste codes per partner, page numbers), `rapoarte/` with `evidenta-gestiunii-deseurilor-«year».pdf` — the same four-chapter sheet — `evidenta-centralizata-«year».pdf`, `anexa1-ambalaje-«year».xls/.pdf` for a producer or importer and the Anexa 3 Ambalaje pairs, and `atasamente/`. The unofficial summary table is not inside the archive, on purpose — it is downloaded from the same screen, under the list |
| Every document in one place | **Dosar de control** → „Documentele anului” | One row per document for the chosen company and years — Intră / Intră fără date / Nu intră / Lipsește — with the reason (no movements, trader, profile without market or packaging role, no packaging that year), read by `GET /api/v1/audit-file/contents` with the same rules as the archive. Since 18.09.2026 each row also has its own **Descarcă**: the sheet, the centralised evidence, Anexa 1 and Anexa 3 Ambalaje (.xls / PDF). What is generated only inside the archive — the partner authorization list, the attachments — says "în arhivă" instead |
| Three years of dossier | **Dosar de control** → *Perioada* → „Ultimii 3 ani" | One folder per year (`2024/`, `2025/`, `2026/`), `autorizatii-parteneri.pdf` once at the root, and a `00-cuprins.txt` that names any year with no evidence lines instead of shipping a blank sheet |
| Search without diacritics | **Mișcări** → add → waste code box → type `deseuri` | Results appear: the nomenclator is searched on a folded copy of code and name (V17) |
| The 15 March deadline | **Termene** | Reads "Anexa 1 — evidența gestiunii deșeurilor generate (anual, 15 martie)": the document, not the portal |
| The 25 February deadline | **Termene**, on a company whose profile answers "producător" or "importator" | The packaging report of Ordinul 794/2012 art. 6, at the county agency. Tick only "Comerciant", or leave the question unanswered, and it does not appear — an alert asserts something, so it stays silent where a screen would still offer |
| Lapsed recipient authorization | **Mișcări** → hand waste to a partner whose authorization expired before that date | An amber **"Autorizație expirată"** badge next to the partner, naming the expiry date. Anexa 3 still prints: the handover happened, and the warning stays off the paper that reaches the inspector |
| The year's totals for the filing | **Generare** → *Totalul anului* | The year's totals per waste code in **kg**, the unit the specialist confirmed the filing is typed in (the screen showed tonnes until 17.09.2026, and "1,060 t" read as a thousand tonnes). Nothing printed changes |
| Designated waste manager | **Clienți** → "Deschide" → "Profil" | Name, capacity, employee vs. delegated third party, training certificate — OUG 92/2021 art. 23 alin. (4)–(5). Not the contact person, who is the declaration's signature block. Leave it blank and the control dossier's `00-cuprins.txt` says so out loud, because its absence is itself the finding |
| Picking a month | **Mișcări** → the "Luna" filter | Two plain selects, month then year, because `<input type="month">` does not exist in Safari or Firefox — there it degrades to a free-text box. The screen opens on the current month, so it no longer fetches the whole history on every visit; "Tot anul" is the way back, and a month with no rows says which month is empty and offers the year |
| Closing a started form | **Mișcări** → add → type anything → Escape | It asks. On an untouched form it just closes — a question about an empty form is noise. Escape over the question closes only the question: what you typed is still there |
| Your own company's data | **Setări** → "Datele firmei" | CAEN, the environmental authorization, the designated person, the Anexa 3 series — read-only, because they are edited from **Clienți**, which is platform-admin. Unfilled rubrics show as "Necompletat" rather than being skipped: an empty CAEN prints empty on the annual declaration |
| Undoing a deactivation | **Setări** or **Parteneri** → deactivate a row, then switch the state filter to "Inactive" | "Reactivează" on the row. Deactivation never deleted anything — old movements quote the row — so it was never meant to be final. The state filter starts on "Active" and stays hidden until something has actually been deactivated |
| Reading an attachment | **Mișcări** → June 2026 → the "📎 2" cell | It opens the files by name, each a link. It used to be a number and nothing else: the only way to the document ran through the thirty-field edit form — which a **VIEWER** cannot open at all, since "Editează" sits behind `canWrite`. Read-only on purpose: deleting stays in the form, next to uploading, where the confirmation is |
| Adding a partner | **Parteneri** → "Adaugă partener" | Five named sections instead of a column of fifteen blocks: who they are · what they do · transport · authorization · what prints on Anexa 3. The CUI now sits next to the name, where it used to be separated from it by a question about lorries |
| Finding your way down **Ambalaje** | **Ambalaje** | A sticky table of contents over the longest page in the app: four large tables plus a 66-cell grid. It sits above the amber "what blocks the declaration" panel, which comes and goes with the month — a contents bar that moved with it would be a different bar on every visit |
| Which rows of the override grid were saved | **Ambalaje** → "Scrie cifre proprii" → type in a cell | The grid saves a row at a time, when you leave a cell, and now says so: the row reads "nesalvat" while the figure is still yours, "se salvează…" while it flies, "salvat" once it lands, and a line under the grid counts the rows still unsaved. Only errors used to be visible, so sixty-six cells gave no way of knowing how many had arrived |
| A recipient must be authorised | **Parteneri** → a collector or recoverer without "Nr. autorizație" → Save; or a handover to such a partner on **Generare** | Both refuse: whoever takes the waste must hold an environmental authorization (`partner.authorization.required`). A source generator and a pure haulier are not asked |
| What prints in cap. 2 "Modul" / "Scopul" | **Generare** → a handover with "Tratare — ce se face" and a destination, then the record sheet | "Modul" is what was picked, "-" when "— fără —"; "Scopul" is V for an R code, E for a D code. "Transport — destinația" offers DO, I, Vr and A only |
| Anexa 3 Ambalaje in the dossier | **Dosar de control** → download | One `.xls` + PDF per work point that moved packaging that year; a work point without packaging gets no blank sheet |
| What waste a new client has | `/cerere-cont` → "Deșeurile" | Sixteen everyday wastes to tick, each with its code, and "Alte deșeuri" for the rest — all optional. Ticks and text arrive as one line on the request ("Carton și ambalaje de hârtie (15 01 01), toner") |
| AFM contributions | **Clienți** → "Deschide" → "Profil" | Monthly 2% and annual packaging only; "Economia circulară" (landfills) is gone from the form, the server and the deadlines (`V60`) |
| Who is asked where the packaging came from | **Parteneri** → "Adaugă partener" | The question only appears on an account that can take waste over from third parties. It feeds the Anexa 3 Ambalaje of Ordinul 794/2012 — collectors, traders, recyclers — so on a pure generator's account it has no answer to give. A partner that already carries one keeps showing it |
| Why nothing on screen shows stock | **Panou**, **Generare** | A generator owns no weighbridge and keeps no stock: the waste sits in the bin until the collector comes, and the quantity is learnt at handover, from their ticket. So the server refuses a generation without an exit (`V58`), the generation is the figure *derived* from the handover (`V24`), and `stock = previous + generated − recovered − disposed` comes out zero by construction. The dashboard tile went on 16.09.2026 and the column on 18.09.2026. "Left in stock" survives where the law puts it: on the printed Anexa 1 (cap. 1) and on the centralizator |
| From a deadline to the document that clears it | **Termene** | Each row says how many days are left (or by how many it is overdue), and the annual filings link to the screen that prints them — for the year **reported**, not the year of the deadline. The AFM contributions link nowhere on purpose: those are declared in AFM's own application, and we print no form for them |
| What to do next | **Panou** | One band at the top names a single thing to do, picked by what costs most if left undone: an overdue deadline, then an exit with no R/D code, then a filing within thirty days (linking to the document that clears it, for the year *reported*), then a lapsing authorization, then the weighbridge. It stays silent until every source has loaded — "you are up to date" written over a half-loaded answer is the false green this codebase has paid for before |
| When an answer did not arrive | **Panou** — stop the API and reload | The band, the compliance card, the tiles and both lists say so, each in its own words. The guard used to cover requests *in flight* only: a **failed** one leaves `isLoading` false and `data` undefined, which `?? []` turns into an empty list — so a company with nine overdue deadlines and an unclassified exit was told "you are up to date", with no error anywhere on the screen. "I could not read this" is the third state, and the expensive one to be missing |
| A company with nothing recorded yet | **Panou** on a new account | Not "you are up to date" — that is true and useless. No work point yet: "add the first work point", because a movement is recorded *on* one. Work point but nothing recorded: "record the first movement", because the register, the record sheet and the declarations are all computed *from* movements. Neither step is a guess: both are dependencies the code already enforces |
| Unofficial exports | **Generare** → *Totalul anului* → "Alte descărcări", or **Dosar de control** | The two official documents stay in the header; the generic exports moved into a menu that says what they are. They print "unofficial summary" on themselves, so five equally prominent buttons claimed all five were the same kind of thing — and squeezed the page title onto three lines |
| A partner you may already have | **Parteneri** → "Adaugă partener" → type two letters of a name you already use | The duplicate suggestion used to swap the dialog into edit mode silently, throwing away everything typed — the only sign was the title. It still switches, because that is what you wanted; it now says so, and offers the way back with the name you typed restored. Fill in more than the name first and it asks before switching |
| A recipient whose authorization had lapsed | **Mișcări** → a handover with the amber "Autorizație expirată" badge | The badge told you to update the partner's record and led nowhere. It is a link now, carrying the expiry date on screen rather than behind a hover — it cannot be a tooltip, because a tooltip renders its own button and could not wrap a link |
| Why a driver's ID papers are held | **Setări** → "Șoferii noștri", and a carrier's own form | A retention note where the field is typed: it is held for the "Date de identificare delegat" rubric of Anexa 3 and printed on it, and it asks for the ID series, not the CNP. Movements keep the snapshot of the day for as long as the record must be kept (OUG 92/2021 art. 48 alin. (5), at least three years); after three full calendar years a daily job clears the driver's name and ID from them |
| Deleting one of our drivers | **Setări** → "Șoferii noștri" → deactivate, then "Șterge definitiv" | The record goes for good, but only once deactivated, so one wrong click cannot be final. Movements are not touched: they hold their own copy, because the Anexa 3 must print the same while the record is kept |
| The partner's annual visa | **Parteneri** → edit → authorization | Issue date of the original authorization, the visa decision (number, date) and the visa period, typed from the decision; the screen proposes the issue anniversary. The handover badge, the 60-day alert, the Anexa 3 rubric and the dossier all read whichever comes first, the expiry or the end of the visa |
| Two movement screens | the menu, on each company type | A **generator** sees **"Generare"** (`/generare`, Anexa 1), a **collector** only **"Intrări și ieșiri"** (`/intrari-iesiri`, the art. 48 register), "Generator și colector" both. A pure collector gets a note instead: its own waste goes on Anexa 1, so such a company is set to "both". On a generator there is no Anexa 2 button (`anexa2.collectors.only`); its Anexa 3 Ambalaje (on **Ambalaje**) carries only what it handed over |
| The art. 48 chronological record | **Intrări și ieșiri** → "Evidența cronologică (.xlsx)" / "(PDF)" | The year in the month filter, one work point or all. First the chronological table of the `ART_48` movements (kg and t, partner, R/D, transport, document), then the year's totals shaped like the SIM "Colectare/Tratare" questionnaire, in tonnes: cap. 1 per code (opening stock from earlier years, collected, recovered, disposed, closing) and cap. 2 A/B per recipient. OUG 92/2021 art. 48 alin. (1) prescribes the content and "tabular", not a form, and the portal is typed by hand, so the xlsx is the copy aid (`docs/surse-oficiale.md` §2.1-bis). A generator gets `art48.register.collectors.only` |
| A client's subscription | **Clienți** → "Deschide" → "Abonament și facturi" on a direct company, or "⋯ → Abonament" on a consultancy (platform admin) | Plan, start date and founder flag; the prices are copied from the grid when the subscription is created and change only with the plan. The dialog shows the first invoice and the monthly one: a period runs from the start day to the day before it next month, at full price. A company inside a consultancy has no subscription of its own (`subscription.company.in.consultancy`), and a company with one cannot join a consultancy (`company.has.own.subscription`). Invoices are issued from the app on the due day and mailed to the client; a client without a subscription is neither billed nor restricted |
| Invoicing | **Facturare** (`/facturare`, B, platform admin) | The last run (06:30 or the button), row by row: who failed and why, what was issued and paid. Every invoice, paged on the server, with "De rezolvat" first; "Verifică plata" asks FGO about one invoice, "Oprește" drops one FGO refused. A billing county outside FGO's list is refused when saved, not at 06:30 |
| The client's own subscription | **Abonament** (`/abonament`, a company admin or a consultant) | What is owed, as a receipt: one line per unpaid invoice, the total, the bank transfer with each value to copy (IBAN copied without spaces), and "Am plătit — verifică acum", which asks FGO at most once every two minutes per invoice — also after an attempt FGO did not answer — and never waits in line behind another FGO call. The client keeps the billing data up to date itself (not the name or the CUI); the change goes in the company's journal and the old address is told |
| A consultancy | **Clienți** → "Cabinete" (platform admin) · "Echipa cabinetului" (consultant) | The platform creates a consultancy and invites its first consultant; the consultant invites colleagues and creates companies, which join the consultancy. A consultant's company list holds only that consultancy's companies, and moving a company between consultancies is left to the platform |
| All of a consultancy's companies | **Firmele mele** (`/cabinet`, consultant only) | One row per active company: overdue deadlines and the next one, lines with no R/D code or awaiting the weighbridge, mirror codes with no document, partner authorizations lapsing within 60 days. A cell switches to that company and opens the screen that clears it. A daily 07:30 mail lists the week's open deadlines, and sends nothing when there is nothing |
| The consultancy's letterhead | **Clienți** → "Antetul cabinetului pe rapoarte" (consultant) | A logo and a contact line printed on the unofficial reports — the evidence summary, the partner authorizations, the dossier `00-cuprins.txt`. The official forms never carry it. The image type is read from its bytes, not its name |
| History from a spreadsheet | **Import din Excel** (`/import`, platform admin only — **Setări** once a company is picked) | Imports are ours to run during onboarding: the client sends the spreadsheet, and company admins, consultants and operators get `403` (the address takes them home). Download the template, fill it, press "Verifică": the file runs and rolls back, listing every row's errors. "Importă" stays locked until that same file has passed with no errors, and then writes all or nothing; importing the same file again adds nothing, and a corrected file skips the rows already in the company (same date, site, code, quantity, operation, partner and document), listing them as warnings. Below, the company's saved imports: "Retrage" deletes the movements nobody has edited since, and keeps partners and work points. A small file that inflates huge is refused before it is parsed |
| Filling a partner from its CUI | **Parteneri** → "Adaugă partener" → type the CUI → "Completează din ANAF" (the same button sits on **Client nou** and on a consultancy) | Name, registered address and trade register number come from ANAF's public taxpayer registry (web service v9) into the rubrics that are still **empty** — nothing typed is overwritten, because the name you use and the unloading address may differ from the registry. An inactive taxpayer is flagged. Signed-in users only; calls are spaced one a second, as ANAF's documentation asks, and a company found is kept for a day. ANAF down answers `503`, and the form says to type the data by hand. A CUI ANAF does not know comes back from ANAF as HTTP 404 with an empty `found` list, and the form says exactly that |
| Waste articles (depot) | **Setări** → "Sortimente", only on a company with the art. 48 register | The depot's own catalogue, each article tied to a waste code. "Metal" is proposed from the code; "not accepted from individuals" makes an intake from a private person refuse that article. Anyone who writes may edit it |

### Tests

```bash
cd backend
./gradlew.bat test
```

932 tests across 112 classes, on an embedded PostgreSQL (zonky), through the real HTTP stack rather
than service calls. They cover tenant isolation, role authorization, session handling, evidence
calculation, export correctness, movement validation, company management and the official documents
the app prints — the HG 856/2002 record sheet, the annual declaration, the HG 1061/2008 transport
form, and the packaging declaration of Ordinul 794/2012 — plus the Excel import, the consultancy
panel, subscriptions and invoicing, and the depot module's weighing operations.

The pre-launch QA audit added fourteen suites and found sixteen defects, each one first pinned by a
test asserting the **correct** behaviour and seen failing, then turned green by its fix — so every
repair is proved by a test written before it rather than after. None of the sixteen crossed an access
boundary. A seventeenth came from a load-focused pass after the audit closed: a small `.xlsx` that
inflates to many megabytes exhausted the single dyno's heap for every client, and is now refused
before parsing, proved on a 300 MB heap. Thirteen more (to BUG-030) came from later passes over new
slices and from a whole-codebase scan on 17.09.2026 — among them the same company accepted twice with
and without the "RO" prefix, reminders sent on UTC, and an attachment download that could wait forever —
each fixed the same way, a test first and the rule removed once to see it fail. Every official document is also checked as printed —
figures read back out of the PDF and the `.xls`, not out of the data behind them — and each of those
checks was shown to fail when its column was swapped in the generator.

Three of those suites were validated the only way an exclusion can be: **by removing the rule from
the production code and confirming the tests actually fail.** That is how the register filter, the
implied-generation term and the `clientGeneratedId` idempotency turned out to have had no test that
would notice their disappearance — each one correct, each one commented, none of them load-bearing.
A comment explaining a rule is not evidence the rule is still there.

---

## License

MIT — see [LICENSE](LICENSE).
