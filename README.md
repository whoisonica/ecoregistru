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

**Evidence engine.** `EvidenceCalculator` aggregates waste movements into monthly lines per
(work point, waste code), carrying a cumulative stock balance forward month over month and
flagging negative balances. Exports to `.xlsx` (Apache POI) and `.pdf` (OpenPDF).

**Deadlines and alerts.** Automatic generation of the annual SIM deadline and the monthly AFM
deadline (only for companies subject to it), plus a daily cross-tenant scheduler that emails
T-7 / T-1 reminders with per-company deduplication.

**Inspection file.** `GET /api/v1/audit-file?year=` streams a ZIP containing the yearly evidence
(xlsx + pdf), partner authorization PDFs and every movement attachment.

**Roles.** `PLATFORM_ADMIN` / `ADMIN` / `OPERATOR` / `CLIENT_VIEWER`, enforced at endpoint level.

Current status and the feature-by-feature log: [`docs/status.md`](docs/status.md).

---

## Regulatory note

The generic evidence export (Excel/PDF) is implemented and explicitly labelled as an
**unofficial summary**. The official monthly record format (Anexa 1, HG 856/2002) and the
SIM/AFM structures are deliberately **not** implemented until a domain expert confirms them —
this project does not invent official formats. None of the official portals (SIM/ANPM,
AFM-online, SIATD) exposes a public third-party submission API, so the product model is
"we prepare, you submit": the app produces the reports, the client uploads them.
Research: [`docs/legislatie.md`](docs/legislatie.md).

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

### Tests

```bash
cd backend
./gradlew.bat test
```

Integration tests cover tenant isolation, evidence calculation, export correctness,
movement validation and company management.

---

## License

MIT — see [LICENSE](LICENSE).
