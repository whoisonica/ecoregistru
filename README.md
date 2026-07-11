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

## Stadiu: B0 — Fundația (gata)

- ✅ Auth JWT (login, verificare email, resend, reset parolă) — portat din an earlier project
- ✅ Multi-tenancy: `tenant_id` pe fiecare tabelă de domeniu + `TenantContext` +
  `TenantFilter` (izolare la nivel de request; PLATFORM_ADMIN comută tenantul via `X-Tenant-Id`)
- ✅ Roluri: `PLATFORM_ADMIN`, `ADMIN`, `OPERATOR`, `CLIENT_VIEWER`
- ✅ Schema completă Faza 1 (Flyway) + seed nomenclator coduri deșeuri din CSV
- ✅ Envelope de erori consistent, OpenAPI (springdoc), Cloudinary + email (skeleton)
- ✅ Seed tenant demo (profil `dev`) + shell frontend cu login și layout cu sidebar

Următorul pas: **B1 — CRUD WasteMovement (+atașamente) → motor evidențe + exporturi.**

---

## Cerințe

- **Java 21** (Temurin OK)
- **Node 20+**
- **PostgreSQL 15+** care rulează local

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

Formatul oficial al fișei de gestiune lunare și structurile SIM/AFM **nu sunt încă
implementate** — sunt în spatele interfeței `EvidenceExporter` (vine în B1), cu o
primă implementare „tabel generic" (Excel/PDF). Formatele oficiale rămân TODO până
la confirmarea spec-ului. Nomenclatorul de coduri deșeuri e un placeholder cu 10
rânduri — vezi TODO în `backend/src/main/resources/seed/waste_codes.csv`.
