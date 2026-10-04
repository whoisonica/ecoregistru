-- Declaratia anuala de energie, sub 1000 tep (art. 9 alin. (7) din Legea 121/2014): Anexa 1 (consumul lunar pe
-- purtator) si declaratia pe proprie raspundere, cu auditul, masurile de economisire si interesul POIM.
--
--   * `energy_carriers_used`: rubricile Anexei bifate ca folosite de firma. O rubrica debifata isi pastreaza lunile
--     in `energy_consumptions` (o rebifare le readuce), dar iese din totaluri si se tipareste 0.
--   * `energy_consumptions`: o cantitate pe firma, an, purtator si luna. `tep` se scrie de mana numai la carbune si
--     la alti combustibili (fara coeficient pe Anexa); la restul se calculeaza si nu se stocheaza.
--   * `energy_declarations`: raspunsurile anului (IMM, audit, POIM) si recipisa depunerii. Un raspuns lipsa = NULL,
--     niciodata o valoare implicita. Recipisa sta aici, nu in `attachments`, fiindca acolo `movement_id` e NOT NULL
--     (acelasi tipar ca `scale_documents`, V70).
--   * `energy_saving_measures`: pana la 20 de masuri de economisire pe declaratie.

CREATE TABLE energy_carriers_used (
    id          UUID         PRIMARY KEY,
    company_id  UUID         NOT NULL REFERENCES companies (id),
    carrier     VARCHAR(30)  NOT NULL,
    updated_at  TIMESTAMP    NOT NULL,
    CONSTRAINT uq_energy_carrier_used UNIQUE (company_id, carrier),
    CONSTRAINT ck_energy_carrier_used CHECK (carrier IN ('ELECTRICITY', 'HEAT', 'NATURAL_GAS', 'FUEL_OIL',
        'LIGHT_FUEL_OIL', 'PETROL', 'DIESEL', 'COAL', 'OTHER_FUEL', 'RENEWABLE_ELECTRICITY', 'RENEWABLE_HEAT'))
);

CREATE TABLE energy_consumptions (
    id          UUID           PRIMARY KEY,
    company_id  UUID           NOT NULL REFERENCES companies (id),
    year        INT            NOT NULL,
    carrier     VARCHAR(30)    NOT NULL,
    month       INT            NOT NULL,
    quantity    NUMERIC(14, 3) NOT NULL,
    tep         NUMERIC(14, 4),
    updated_at  TIMESTAMP      NOT NULL,
    CONSTRAINT uq_energy_consumption UNIQUE (company_id, year, carrier, month),
    CONSTRAINT ck_energy_consumption_carrier CHECK (carrier IN ('ELECTRICITY', 'HEAT', 'NATURAL_GAS', 'FUEL_OIL',
        'LIGHT_FUEL_OIL', 'PETROL', 'DIESEL', 'COAL', 'OTHER_FUEL', 'RENEWABLE_ELECTRICITY', 'RENEWABLE_HEAT')),
    CONSTRAINT ck_energy_consumption_month CHECK (month BETWEEN 1 AND 12),
    CONSTRAINT ck_energy_consumption_quantity CHECK (quantity >= 0),
    CONSTRAINT ck_energy_consumption_tep CHECK (tep >= 0),
    CONSTRAINT ck_energy_consumption_tep_by_hand CHECK (tep IS NULL OR carrier IN ('COAL', 'OTHER_FUEL'))
);

CREATE INDEX idx_energy_consumptions_company_year ON energy_consumptions (company_id, year);

CREATE TABLE energy_declarations (
    id                     UUID          PRIMARY KEY,
    company_id             UUID          NOT NULL REFERENCES companies (id),
    year                   INT           NOT NULL,
    sme                    BOOLEAN,
    audit_date             DATE,
    auditor                VARCHAR(255),
    audit_scope            TEXT,
    audit_share_pct        NUMERIC(5, 2),
    poim_interest          BOOLEAN,
    poim_project           BOOLEAN,
    receipt_public_id      VARCHAR(255),
    receipt_resource_type  VARCHAR(32),
    receipt_delivery_type  VARCHAR(32),
    receipt_format         VARCHAR(32),
    receipt_file_name      VARCHAR(255),
    receipt_size_bytes     BIGINT,
    receipt_uploaded_at    TIMESTAMP,
    updated_at             TIMESTAMP     NOT NULL,
    CONSTRAINT uq_energy_declaration UNIQUE (company_id, year),
    CONSTRAINT ck_energy_audit_share CHECK (audit_share_pct BETWEEN 0 AND 100)
);

CREATE TABLE energy_saving_measures (
    id                      UUID           PRIMARY KEY,
    declaration_id          UUID           NOT NULL REFERENCES energy_declarations (id) ON DELETE CASCADE,
    position                INT            NOT NULL,
    name                    VARCHAR(500),
    cost_estimated          NUMERIC(14, 3),
    cost_actual             NUMERIC(14, 3),
    savings_tep_estimated   NUMERIC(14, 3),
    savings_tep_actual      NUMERIC(14, 3),
    savings_cost_estimated  NUMERIC(14, 3),
    savings_cost_actual     NUMERIC(14, 3),
    updated_at              TIMESTAMP      NOT NULL,
    CONSTRAINT uq_energy_saving_measure UNIQUE (declaration_id, position),
    CONSTRAINT ck_energy_saving_position CHECK (position BETWEEN 1 AND 20)
);

ALTER TABLE companies
    ADD COLUMN fax                       VARCHAR(50),
    ADD COLUMN website                   VARCHAR(255),
    ADD COLUMN activity_sector           VARCHAR(255),
    ADD COLUMN energy_contact_name       VARCHAR(255),
    ADD COLUMN energy_contact_email      VARCHAR(255),
    ADD COLUMN energy_contact_phone      VARCHAR(50),
    ADD COLUMN energy_contact_mobile     VARCHAR(50),
    ADD COLUMN energy_contact_attested_on DATE;

COMMENT ON TABLE energy_consumptions IS
    'Anexa 1 energie (Legea 121/2014, art. 9 alin. 7): consumul lunar pe purtator. tep se scrie de mana doar la carbune si alti combustibili.';
