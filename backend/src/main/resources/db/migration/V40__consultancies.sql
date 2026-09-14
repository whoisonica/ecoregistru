-- P2.13, felia 1 — cabinetul de consultanţă: un cont care ţine evidenţa pentru mai multe firme.
--
-- **Ce lipsea.** Singurul rol care vedea mai multe firme era `PLATFORM_ADMIN`, iar el le vede pe
-- **toate** — e personalul nostru. Un consultant căruia i s-ar fi dat rolul acela ar fi văzut şi
-- clienţii celorlalţi consultanţi. Trebuia un rol care vede **un set** de firme, şi un loc unde să
-- stea setul.
--
-- **De ce cabinet şi nu „consultantul are o listă de firme".** Contractul-cadru se semnează cu firma
-- de consultanţă, nu cu omul; DPA-ul (situaţia B) o face pe ea împuternicitul fiecărei firme-client;
-- iar un cabinet are de obicei doi-trei oameni care lucrează pe aceleaşi firme. Cu lista pe om, al
-- doilea angajat ar fi cerut o copie a listei, iar două copii ajung să nu mai spună acelaşi lucru.
--
-- **O firmă are cel mult un cabinet** — coloană pe `companies`, nu tabelă de legătură. Aşa e şi în
-- realitate: cabinetul e împuternicitul firmei pentru datele ei, iar două cabinete pe aceeaşi firmă
-- ar însemna două contracte de prelucrare pentru aceleaşi date. NULL = client direct, ca până azi.
--
-- **Un cont e al unei firme SAU al unui cabinet, niciodată al amândurora.** Constrângerile de mai
-- jos o spun în bază, nu doar în serviciu: un `CONSULTANT` legat şi de o firmă ar primi firma aceea
-- ca tenant fix pe o ramură şi setul cabinetului pe cealaltă, după cum se citeşte `TenantFilter`.
-- Ambele CHECK-uri sunt adevărate pe toate rândurile existente (niciun `CONSULTANT`, niciun
-- `consultancy_id`), deci migrarea nu poate cădea pe producţie.
--
-- Aditivă, ca toate celelalte: nimic şters, nimic mutat.

CREATE TABLE consultancies (
    id          UUID PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    -- Acelaşi format ca la firme; unic, fiindcă e identitatea din contract.
    cui         VARCHAR(32)  NOT NULL UNIQUE,
    created_at  TIMESTAMPTZ  NOT NULL
);

ALTER TABLE companies ADD COLUMN consultancy_id UUID REFERENCES consultancies (id);
CREATE INDEX idx_companies_consultancy ON companies (consultancy_id);

ALTER TABLE app_users ADD COLUMN consultancy_id UUID REFERENCES consultancies (id);
CREATE INDEX idx_app_users_consultancy ON app_users (consultancy_id);

-- Un consultant are cabinet şi n-are firmă.
ALTER TABLE app_users ADD CONSTRAINT app_users_consultant_scope
    CHECK (role <> 'CONSULTANT' OR (consultancy_id IS NOT NULL AND company_id IS NULL));

-- Un cabinet are numai consultanţi. Fără asta, un `ADMIN` de firmă cu `consultancy_id` completat ar
-- fi un rând pe care nicio ramură din `TenantFilter` nu-l citeşte cum crede cine l-a scris.
ALTER TABLE app_users ADD CONSTRAINT app_users_consultancy_role
    CHECK (consultancy_id IS NULL OR role = 'CONSULTANT');

COMMENT ON COLUMN companies.consultancy_id IS
    'Cabinetul de consultanţă care gestionează firma, sau NULL pentru un client direct. Decide ce '
    'consultanţi pot selecta firma prin X-Tenant-Id.';
COMMENT ON COLUMN app_users.consultancy_id IS
    'Cabinetul unui cont CONSULTANT. NULL pentru orice alt rol; un consultant n-are company_id.';
