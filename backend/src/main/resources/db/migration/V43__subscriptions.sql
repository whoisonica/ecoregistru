-- Plata abonamentelor, felia F1 (ecoregistru-docs/docs/plata-abonamente.md): pachetul și prețul
-- fiecărui client, în aplicație. Facturile (FGO), cardul (Netopia) și doar-citirea vin în feliile
-- următoare, fiecare cu migrarea ei; aici nu e nimic ce F1 nu scrie.
--
-- **Un abonament e al unei firme SAU al unui cabinet.** Firmele unui cabinet n-au abonament propriu:
-- le plătește cabinetul, pe tranșe (monetizare.md §3.2). CHECK-ul de mai jos o spune în bază.
--
-- **Prețul se copiază pe abonament la creare.** O grilă schimbată mâine nu atinge abonamentele de
-- azi, iar clientul fondator are prețul blocat doi ani fără nicio regulă în plus.
--
-- **Firmele fără rând aici rămân exact cum sunt** (pilotul, conturile interne, toată producția de
-- azi). Migrarea nu adaugă niciun rând, deci nimeni nu e facturat sau restricționat dimineața.

CREATE TABLE subscriptions (
    id                      UUID PRIMARY KEY,
    company_id              UUID UNIQUE REFERENCES companies (id),
    consultancy_id          UUID UNIQUE REFERENCES consultancies (id),
    plan                    VARCHAR(32)   NOT NULL,
    status                  VARCHAR(16)   NOT NULL,

    -- Grila, copiată la creare. Lei, fără TVA (ONSIA neplătitoare).
    monthly_price           NUMERIC(10,2) NOT NULL,
    implementation_fee      NUMERIC(10,2) NOT NULL,
    extra_work_point_price  NUMERIC(10,2),
    company_price_tier1     NUMERIC(10,2),
    company_price_tier2     NUMERIC(10,2),
    company_price_tier3     NUMERIC(10,2),
    packaging_company_price NUMERIC(10,2),

    -- Primii 30 de clienți până la 31.10.2026: implementare gratuită, preț blocat doi ani.
    founder                 BOOLEAN       NOT NULL DEFAULT FALSE,
    started_at              DATE          NOT NULL,
    created_at              TIMESTAMPTZ   NOT NULL,

    CONSTRAINT subscriptions_owner
        CHECK ((company_id IS NULL) <> (consultancy_id IS NULL)),
    CONSTRAINT subscriptions_plan
        CHECK (plan IN ('GENERATOR', 'GENERATOR_PACKAGING', 'FULL_SERVICE', 'CONSULTANCY')),
    CONSTRAINT subscriptions_status
        CHECK (status IN ('PENDING', 'ACTIVE', 'PAST_DUE', 'READ_ONLY', 'CANCELLED')),
    -- Planul de cabinet numai pe un cabinet, cu tranșele lui; ceilalți numai pe o firmă, cu prețul
    -- punctului de lucru. Un rând care le amestecă n-ar avea cum să fie calculat.
    CONSTRAINT subscriptions_consultancy_plan
        CHECK ((plan = 'CONSULTANCY') = (consultancy_id IS NOT NULL)),
    CONSTRAINT subscriptions_consultancy_prices
        CHECK (consultancy_id IS NULL OR (company_price_tier1 IS NOT NULL
               AND company_price_tier2 IS NOT NULL AND company_price_tier3 IS NOT NULL
               AND packaging_company_price IS NOT NULL)),
    CONSTRAINT subscriptions_company_prices
        CHECK (company_id IS NULL OR extra_work_point_price IS NOT NULL)
);

COMMENT ON TABLE subscriptions IS
    'Abonamentul unei firme directe sau al unui cabinet. Lipsa rândului = firmă nefacturată și nerestricționată.';
