# EcoRegistru

SaaS B2B pentru conformitate în gestiunea deșeurilor (România): evidența lunară a
gestiunii deșeurilor, pregătirea raportărilor (SIM/AFM), alerte de termene și
generarea dosarului de control.

Monorepo:

```
/backend    Spring Boot 3.2 · Java 21 · PostgreSQL · Flyway · JWT (multi-tenant)
/frontend   Vite · React 18 · TypeScript · Tailwind · TanStack Query
REUSE_MAP.md   Ce a fost portat din an earlier project, ce e nou (citește-l primul)
```

Cod, comentarii, commit-uri: engleză. Text UI: română (`frontend/src/lib/strings.ts`).

---

## Stadiu

- ✅ **B0 — Fundația:** auth JWT (login, verificare email, resend, reset parolă), multi-tenancy
  (`company_id` pe fiecare tabelă + `TenantContext` + `TenantFilter`, izolare la nivel de request;
  PLATFORM_ADMIN comută tenantul via `X-Tenant-Id`), roluri `PLATFORM_ADMIN`/`ADMIN`/`OPERATOR`/`CLIENT_VIEWER`,
  schema completă Faza 1 (Flyway), seed nomenclator coduri din CSV, envelope de erori, OpenAPI, seed tenant demo.
- ✅ **B1-core:** CRUD `WasteMovement` (+atașamente Cloudinary, idempotent, soft-delete, `?since=`),
  `Partner` CRUD, `WorkPoint` CRUD, căutare `WasteCode`.
- ✅ **Faza M:** stare fizică + cod operație R/D (validat) pe mișcări, flag obligație AFM pe firmă,
  stoc cumulativ pe evidența lunară (Flyway `V3`).
- ✅ **UI-1 / U0:** infra frontend (toast, primitive UI, hooks TanStack Query, tipuri) +
  ecran **Setări / Puncte de lucru** (CRUD).
- ✅ **UI-1 / U1:** ecranul **Mișcări** — tabel + filtre (lună / punct de lucru) + dialog de
  adăugare rapidă/editare cu combobox de căutare coduri, validare R/D condiționată, atașamente
  (drag-drop) și ștergere soft. Plus primitive noi (`select`, `date-input`, `combobox`, `file-dropzone`).
- ✅ **UI-1 / U2:** ecranul **Parteneri** — tabel + CRUD + badge expirare autorizație + dezactivare soft.
- ✅ **EVID / E1 (backend):** motor de evidență — `EvidenceCalculator` agregă mișcările în linii
  lunare per (punct de lucru, cod) cu **stoc cumulativ**; `GET /api/v1/evidences`,
  `POST /api/v1/evidences/regenerate?year=`. Test de corectitudine verde.
- ✅ **EVID / E3 (frontend):** ecranul **Evidențe** (`/evidente`) — filtre an / lună / punct de lucru,
  tabel cu totaluri pe operațiune + stoc cumulativ (roșu când e negativ), buton **Regenerează**
  (gated pe rol), empty-state per an. Aduce EVID în UI. Type-check verde.
- 🔜 **Următorul:** E2-generic — export „tabel generic" Excel (Apache POI) + PDF (OpenPDF) din evidență.
  Apoi U4 (selector tenant). Detalii în `docs/prompt-continuare.md`.

Set demo bogat (dev): 3 puncte de lucru, 5 parteneri, 34 de mișcări pe 6 luni (feb–iul 2026) cu stoc
care se reportează lună-de-lună. Vezi și `docs/prezentare.html` — pagină de prezentare a produsului.

Model de produs: Faza 1 = „pregătim, nu transmitem" — ținem evidența și generăm ce trebuie raportat
(SIM/AFM), clientul încarcă în portalul oficial (portalurile n-au API public de transmitere de la terți).

---

## Cerințe

- **Java 21** (Temurin OK)
- **Node 20+**
- **PostgreSQL 15+** care rulează local (dezvoltat pe Postgres 17)

> Gradle nu trebuie instalat global — folosește wrapper-ul (`backend/gradlew`).

---

## 1. Baza de date

Creează baza și utilizatorul folosite de profilul `dev`:

```sql
CREATE USER eco WITH PASSWORD 'eco';
CREATE DATABASE ecoregistru OWNER eco;
```

(Din `psql -U postgres`, sau pgAdmin.) Flyway creează schema automat la pornire.

## 2. Backend

```bash
cd backend
# Windows PowerShell:
$env:SPRING_PROFILES_ACTIVE = "dev"
./gradlew.bat bootRun
```

Pornește pe `http://localhost:8080`.
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/actuator/health`

Profilul `dev` include un secret JWT de test și pornește `DevDataSeeder`
(seed tenant demo). **Cloudinary și email sunt opționale în dev** — aplicația
pornește fără ele (uploadul/emailul dau eroare clară doar când sunt folosite).

Variabile pentru producție (NU sunt necesare în dev):
`DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `JWT_SECRET` (Base64),
`CLOUDINARY_URL`, `MAIL_HOST/PORT/USERNAME/PASSWORD/FROM`, `FRONTEND_BASE_URL`.

## 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

Pornește pe `http://localhost:5173` (proxy `/api` → `:8080`).

---

## Conturi demo (profil dev, parola pentru toate: `Parola123`)

| Email                     | Rol            | Tenant             |
|---------------------------|----------------|--------------------|
| platform@ecoregistru.ro   | PLATFORM_ADMIN | — (global)         |
| admin@demo.ro             | ADMIN          | Demo Reciclare SRL |
| operator@demo.ro          | OPERATOR       | Demo Reciclare SRL |
| viewer@demo.ro            | CLIENT_VIEWER  | Demo Reciclare SRL |

---

## Cum testezi B0 rapid

1. Pornește DB + backend + frontend.
2. Deschide `http://localhost:5173`, loghează-te cu `admin@demo.ro / Parola123`.
   Ar trebui să ajungi în Panou, cu sidebar-ul EcoRegistru.
3. Sau direct pe API:
   ```bash
   curl -X POST http://localhost:8080/api/v1/auth/login \
     -H "Content-Type: application/json" \
     -d '{"email":"admin@demo.ro","password":"Parola123"}'
   ```
   Răspunsul conține `token`, `role`, `tenantId`.
4. Verifică datele seed în DB: `SELECT * FROM waste_movements;` (4 mișcări demo).

---

## Note reglementare (important)

Formatul oficial al fișei de gestiune lunare (Anexa 1 HG 856/2002) și structurile SIM/AFM
**nu sunt încă implementate** — vor sta în spatele unui `EvidenceExporter`, cu o primă
implementare „tabel generic" (Excel/PDF). Formatele oficiale rămân TODO până la confirmarea
expertului. Cercetarea portalurilor oficiale (SIM/ANPM, AFM-online/„AFM – Declarații", SIATD)
e în `docs/legislatie.md §5`: **niciunul nu are API public / import de terți** → modelul e
„pregătim, nu transmitem". Nomenclatorul de coduri deșeuri e un placeholder cu 10 rânduri —
vezi TODO în `backend/src/main/resources/seed/waste_codes.csv`.
