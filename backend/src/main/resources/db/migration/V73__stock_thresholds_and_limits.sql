-- Depozitul, F3 — pragurile de stoc (D3.3) și limitele din autorizație (D3.4). Temeiul D3.4: `surse-oficiale.md` §18.6.
--
-- Pragurile sunt ale firmei: min/max pe sortiment × depozit, pentru alertă (nu e lege).
-- Limitele sunt ale autorizației de mediu (OUG 92/2021 art. 34 alin. (2) lit. c), d), j)). Autorizațiile reale le scriu
-- în patru forme (pe cod în t/an, pe cod pe lună, pe grupe, sau deloc — runda 2), deci limita e un rând tastat din
-- PDF: felul, codul (sau toate), cantitatea cu unitatea, perioada, durata maximă de stocare și „aproximativ”. Doar
-- stocul „la un moment dat” se compară cu stocul la zi, iar ieșirile „pe an” cu ieșirile anului; restul se arată.
-- Depășirea e sancționată de OUG 195/2005 art. 96 alin. (3) pct. 1: pe ecran, avertisment ferm, fără blocare.
--
-- ⚠️ V73: V67 e a aplicației mobile; V68–V72 ale depozitului, tot locale.

CREATE TABLE stock_thresholds (
    id              UUID           PRIMARY KEY,
    company_id      UUID           NOT NULL REFERENCES companies (id) ON DELETE CASCADE,
    work_point_id   UUID           NOT NULL REFERENCES work_points (id) ON DELETE CASCADE,
    article_id      UUID           NOT NULL REFERENCES waste_articles (id) ON DELETE CASCADE,
    min_kg          NUMERIC(14,3),
    max_kg          NUMERIC(14,3),
    created_at      TIMESTAMP      NOT NULL,
    CONSTRAINT stock_thresholds_some CHECK (min_kg IS NOT NULL OR max_kg IS NOT NULL),
    CONSTRAINT stock_thresholds_order CHECK (min_kg IS NULL OR max_kg IS NULL OR min_kg <= max_kg),
    CONSTRAINT stock_thresholds_positive CHECK ((min_kg IS NULL OR min_kg >= 0) AND (max_kg IS NULL OR max_kg > 0))
);
CREATE UNIQUE INDEX uq_stock_thresholds ON stock_thresholds (work_point_id, article_id);

CREATE TABLE authorized_limits (
    id                  UUID           PRIMARY KEY,
    company_id          UUID           NOT NULL REFERENCES companies (id) ON DELETE CASCADE,
    work_point_id       UUID           NOT NULL REFERENCES work_points (id) ON DELETE CASCADE,
    -- STORED (lit. j: pe amplasament), TREATED (lit. c: tratat), OUTPUT (lit. d: ce iese).
    kind                VARCHAR(16)    NOT NULL,
    -- Null = toate codurile depozitului (autorizația dă o limită pe amplasament sau pe grupă).
    waste_code_id       UUID           REFERENCES waste_codes (id),
    quantity            NUMERIC(14,3)  NOT NULL,
    -- T, KG sau M3. Metrii cubi nu se compară cu stocul (aplicația cântărește, nu măsoară volumul).
    unit                VARCHAR(4)     NOT NULL,
    -- AT_ONCE (la un moment dat), MONTH, YEAR.
    period              VARCHAR(8)     NOT NULL,
    max_storage_days    INTEGER,
    approximate         BOOLEAN        NOT NULL DEFAULT FALSE,
    note                VARCHAR(500),
    created_at          TIMESTAMP      NOT NULL,
    CONSTRAINT authorized_limits_kind CHECK (kind IN ('STORED', 'TREATED', 'OUTPUT')),
    CONSTRAINT authorized_limits_unit CHECK (unit IN ('T', 'KG', 'M3')),
    CONSTRAINT authorized_limits_period CHECK (period IN ('AT_ONCE', 'MONTH', 'YEAR')),
    CONSTRAINT authorized_limits_quantity CHECK (quantity > 0),
    CONSTRAINT authorized_limits_days CHECK (max_storage_days IS NULL OR max_storage_days > 0)
);
CREATE INDEX idx_authorized_limits_depot ON authorized_limits (work_point_id);
