-- Depozitul, F3 — D3.5: nota de preluare a soldurilor și inventarul. Temeiul: `surse-oficiale.md` §15.1 și cercetarea
-- din 27.09.2026 (`ecoregistru-docs/reports/Inventarul depozitului în lege.md`).
--
-- Nota de preluare NU e inventar: schimbarea programului nu e prilej legal de inventariere (Legea 82/1991 art. 7
-- alin. (1), Normele OMFP 2861/2009 pct. 2–3), iar scripticul e al evidenței firmei (pct. 35 alin. (1)). Soldul se ia
-- din fișele de magazie sau din analiticul contabil la data de tăiere (OMFP 2634/2015 anexa 1 pct. 58 lit. e), n), 61)
-- și intră în stoc ca linii OPENING_BALANCE — nici plus, nici minus.
--
-- Inventarul urmează Normele: decizia (pct. 6), declarația gestionarului (pct. 8 lit. a)), liniile faptic vs. scriptic
-- (pct. 15, 35, 39), procesul-verbal (pct. 42–43). La aprobare scrie INVENTORY_SURPLUS / INVENTORY_SHORTAGE, datate la
-- data de referință (OMFP 1802/2014 pct. 95 alin. (2)). Liniile de stoc stau în afara intrărilor și ieșirilor art. 48.
--
-- ⚠️ V74: V67 e a aplicației mobile; V68–V73 ale depozitului.

CREATE TABLE stock_openings (
    id               UUID           PRIMARY KEY,
    company_id       UUID           NOT NULL REFERENCES companies (id) ON DELETE CASCADE,
    work_point_id    UUID           NOT NULL REFERENCES work_points (id) ON DELETE CASCADE,
    -- Seria pe firmă, dată la confirmare (fără goluri); ciorna n-are număr.
    number           INTEGER,
    cut_off_date     DATE           NOT NULL,
    -- STOCK_CARDS (fișe de magazie), ACCOUNTING (analiticul contabil), PHYSICAL_COUNT (firma fără evidență cantitativă).
    source           VARCHAR(20)    NOT NULL,
    keeper_name      VARCHAR(200),
    accountant_name  VARCHAR(200),
    notes            TEXT,
    status           VARCHAR(12)    NOT NULL,
    confirmed_on     DATE,
    created_at       TIMESTAMP      NOT NULL,
    updated_at       TIMESTAMP      NOT NULL,
    version          BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT stock_openings_status CHECK (status IN ('DRAFT', 'CONFIRMED')),
    CONSTRAINT stock_openings_source CHECK (source IN ('STOCK_CARDS', 'ACCOUNTING', 'PHYSICAL_COUNT')),
    CONSTRAINT stock_openings_confirmed CHECK (status <> 'CONFIRMED'
        OR (number IS NOT NULL AND confirmed_on IS NOT NULL))
);
CREATE UNIQUE INDEX uq_stock_openings_confirmed ON stock_openings (work_point_id) WHERE status = 'CONFIRMED';
CREATE UNIQUE INDEX uq_stock_openings_number ON stock_openings (company_id, number);

CREATE TABLE stock_opening_lines (
    id               UUID           PRIMARY KEY,
    opening_id       UUID           NOT NULL REFERENCES stock_openings (id) ON DELETE CASCADE,
    line_no          INTEGER        NOT NULL,
    article_id       UUID           REFERENCES waste_articles (id),
    waste_code_id    UUID           NOT NULL REFERENCES waste_codes (id),
    kg               NUMERIC(14,3)  NOT NULL,
    CONSTRAINT stock_opening_lines_kg CHECK (kg > 0)
);
CREATE INDEX idx_stock_opening_lines_opening ON stock_opening_lines (opening_id);

CREATE TABLE inventories (
    id                      UUID           PRIMARY KEY,
    company_id              UUID           NOT NULL REFERENCES companies (id) ON DELETE CASCADE,
    work_point_id           UUID           NOT NULL REFERENCES work_points (id) ON DELETE CASCADE,
    number                  INTEGER        NOT NULL,
    -- Decizia (pct. 6 alin. (1)) și felul după pct. 2 alin. (1).
    decision_number         VARCHAR(50),
    decision_date           DATE,
    kind                    VARCHAR(24)    NOT NULL,
    counts_as_annual        BOOLEAN        NOT NULL DEFAULT FALSE,
    mode                    TEXT,
    method                  TEXT,
    -- Data de început e și data de referință a scripticului (pct. 1 alin. (1), pct. 8 lit. d)).
    starts_on               DATE           NOT NULL,
    ends_on                 DATE           NOT NULL,
    keeper_name             VARCHAR(200)   NOT NULL,
    receiving_keeper_name   VARCHAR(200),
    keeper_representative   VARCHAR(200),
    -- Declarația gestionarului (pct. 8 lit. a)): cele 7 răspunsuri cu detaliile lor, ca JSON.
    declaration             JSONB,
    declaration_date        DATE,
    last_entry_doc          VARCHAR(200),
    last_exit_doc           VARCHAR(200),
    -- Procesul-verbal (pct. 42), elementele care nu se calculează.
    pv_date                 DATE,
    pv_causes               TEXT,
    pv_measures             TEXT,
    pv_slow_stock           TEXT,
    pv_storage_findings     TEXT,
    pv_other                TEXT,
    keeper_objections       TEXT,
    commission_conclusions  TEXT,
    status                  VARCHAR(12)    NOT NULL,
    closed_on               DATE,
    approved_on             DATE,
    cancel_reason           VARCHAR(500),
    created_at              TIMESTAMP      NOT NULL,
    updated_at              TIMESTAMP      NOT NULL,
    version                 BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT inventories_status CHECK (status IN ('OPEN', 'CLOSED', 'APPROVED', 'CANCELLED')),
    CONSTRAINT inventories_kind CHECK (kind IN ('ANNUAL', 'CONTROL', 'SUSPECTED_DIFFERENCES', 'HANDOVER',
                                                'REORGANIZATION', 'FORCE_MAJEURE', 'OTHER')),
    CONSTRAINT inventories_period CHECK (ends_on >= starts_on),
    CONSTRAINT inventories_receiving_keeper CHECK (receiving_keeper_name IS NULL OR kind = 'HANDOVER'),
    CONSTRAINT inventories_cancel_reason CHECK (status <> 'CANCELLED' OR cancel_reason IS NOT NULL),
    CONSTRAINT inventories_approved CHECK (status <> 'APPROVED' OR (closed_on IS NOT NULL AND approved_on IS NOT NULL))
);
CREATE UNIQUE INDEX uq_inventories_active ON inventories (work_point_id) WHERE status IN ('OPEN', 'CLOSED');
CREATE UNIQUE INDEX uq_inventories_number ON inventories (company_id, number);

CREATE TABLE inventory_commission (
    inventory_id  UUID          NOT NULL REFERENCES inventories (id) ON DELETE CASCADE,
    position      INTEGER       NOT NULL,
    name          VARCHAR(200)  NOT NULL,
    role          VARCHAR(200),
    president     BOOLEAN       NOT NULL DEFAULT FALSE,
    PRIMARY KEY (inventory_id, position)
);

CREATE TABLE inventory_lines (
    id                  UUID           PRIMARY KEY,
    inventory_id        UUID           NOT NULL REFERENCES inventories (id) ON DELETE CASCADE,
    line_no             INTEGER        NOT NULL,
    article_id          UUID           REFERENCES waste_articles (id),
    waste_code_id       UUID           NOT NULL REFERENCES waste_codes (id),
    book_kg             NUMERIC(14,3)  NOT NULL,
    counted_kg          NUMERIC(14,3),
    count_method        VARCHAR(10),
    technical_data      TEXT,
    explanation         TEXT,
    shortage_nature     VARCHAR(14),
    responsible_person  VARCHAR(200),
    slow_moving         BOOLEAN        NOT NULL DEFAULT FALSE,
    added_manually      BOOLEAN        NOT NULL DEFAULT FALSE,
    CONSTRAINT inventory_lines_counted CHECK (counted_kg IS NULL OR counted_kg >= 0),
    CONSTRAINT inventory_lines_method CHECK (count_method IS NULL
        OR count_method IN ('WEIGHED', 'COUNTED', 'MEASURED', 'TECHNICAL')),
    CONSTRAINT inventory_lines_nature CHECK (shortage_nature IS NULL
        OR shortage_nature IN ('NON_IMPUTABLE', 'IMPUTABLE'))
);
CREATE INDEX idx_inventory_lines_inventory ON inventory_lines (inventory_id);

ALTER TABLE waste_movements ADD COLUMN stock_opening_id UUID REFERENCES stock_openings (id);
ALTER TABLE waste_movements ADD COLUMN inventory_id UUID REFERENCES inventories (id);
CREATE INDEX idx_waste_movements_inventory ON waste_movements (inventory_id) WHERE inventory_id IS NOT NULL;
CREATE INDEX idx_waste_movements_opening ON waste_movements (stock_opening_id) WHERE stock_opening_id IS NOT NULL;
-- O linie de stoc fără documentul ei ar fi o cifră fără document justificativ (fișa de magazie „document cu document”).
ALTER TABLE waste_movements ADD CONSTRAINT waste_movements_opening_link
    CHECK ((operation = 'OPENING_BALANCE') = (stock_opening_id IS NOT NULL));
ALTER TABLE waste_movements ADD CONSTRAINT waste_movements_inventory_link
    CHECK ((operation IN ('INVENTORY_SURPLUS', 'INVENTORY_SHORTAGE')) = (inventory_id IS NOT NULL));
