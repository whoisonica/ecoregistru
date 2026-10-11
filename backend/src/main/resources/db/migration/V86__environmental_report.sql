-- Raportul anual de mediu (RAM), cerut de Andreea pe 09.10.2026. Temeiul: Ordinul 1150/2020, art. 5 alin. (1) lit. b) —
-- RAM-ul se depune la cererea vizei anuale, la autoritatea care a emis autorizația; termenul lui fix îl scrie
-- autorizația. Garda îl poate cere oricând la control.
--
--   * pe firmă, autorizația: cine a emis-o, data emiterii inițiale (din ea se socotește viza: cererea se depune cu cel
--     puțin 60 de zile înainte de ziua și luna emiterii inițiale, art. 5 alin. (4)), ultima revizuire, ultima viză;
--     termenul RAM-ului (precompletat 15 ianuarie, proprietarul, 11.10.2026) și, opțional, termenul vizei scris de
--     mână peste cel socotit; cui mai merge „spre știință” (Garda, alte agenții); administratorul care semnează.
--   * `environmental_reports`: răspunsurile anului. Un răspuns lipsă = NULL, niciodată „nu” implicit.
--   * `environmental_inspections` și `environmental_monitorings`: listele anului, în ordinea scrisă de om.
ALTER TABLE companies
    ADD COLUMN environmental_auth_issuer     VARCHAR(255),
    ADD COLUMN environmental_auth_issued_on  DATE,
    ADD COLUMN environmental_auth_revised_on DATE,
    ADD COLUMN environmental_auth_visa_on    DATE,
    ADD COLUMN ram_due_month                 INT      NOT NULL DEFAULT 1,
    ADD COLUMN ram_due_day                   INT      NOT NULL DEFAULT 15,
    ADD COLUMN visa_due_month                INT,
    ADD COLUMN visa_due_day                  INT,
    ADD COLUMN ram_copy_to                   VARCHAR(255),
    ADD COLUMN administrator_name            VARCHAR(255),
    ADD CONSTRAINT ck_ram_due CHECK (ram_due_month BETWEEN 1 AND 12 AND ram_due_day BETWEEN 1 AND 31),
    ADD CONSTRAINT ck_visa_due CHECK ((visa_due_month IS NULL AND visa_due_day IS NULL)
        OR (visa_due_month BETWEEN 1 AND 12 AND visa_due_day BETWEEN 1 AND 31));

CREATE TABLE environmental_reports (
    id                 UUID      PRIMARY KEY,
    company_id         UUID      NOT NULL REFERENCES companies (id),
    year               INT       NOT NULL,
    incidents          BOOLEAN,
    incidents_text     TEXT,
    complaints         BOOLEAN,
    complaints_text    TEXT,
    inspected          BOOLEAN,
    damage             BOOLEAN,
    damage_text        TEXT,
    other_requirements TEXT,
    updated_at         TIMESTAMP NOT NULL,
    CONSTRAINT uq_environmental_report UNIQUE (company_id, year)
);

CREATE TABLE environmental_inspections (
    id            UUID         PRIMARY KEY,
    report_id     UUID         NOT NULL REFERENCES environmental_reports (id) ON DELETE CASCADE,
    position      INT          NOT NULL,
    work_point    VARCHAR(255),
    institution   VARCHAR(255),
    report_number VARCHAR(100),
    report_date   DATE,
    measures      TEXT,
    updated_at    TIMESTAMP    NOT NULL,
    CONSTRAINT uq_environmental_inspection UNIQUE (report_id, position)
);

CREATE TABLE environmental_monitorings (
    id                 UUID         PRIMARY KEY,
    report_id          UUID         NOT NULL REFERENCES environmental_reports (id) ON DELETE CASCADE,
    position           INT          NOT NULL,
    work_point         VARCHAR(255),
    factor             VARCHAR(255),
    performed_on       DATE,
    test_report_number VARCHAR(100),
    test_report_date   DATE,
    result             TEXT,
    updated_at         TIMESTAMP    NOT NULL,
    CONSTRAINT uq_environmental_monitoring UNIQUE (report_id, position)
);
