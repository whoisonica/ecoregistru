-- EcoRegistru Phase 1 schema. Every domain table carries company_id (tenant).

-- ---------- Tenant + auth ----------

CREATE TABLE companies (
    id                          UUID PRIMARY KEY,
    name                        VARCHAR(255) NOT NULL,
    cui                         VARCHAR(32)  NOT NULL UNIQUE,
    type                        VARCHAR(20)  NOT NULL,
    environmental_auth_number   VARCHAR(128),
    environmental_auth_expiry   DATE,
    address                     VARCHAR(512),
    contact_name                VARCHAR(255),
    contact_email               VARCHAR(255),
    contact_phone               VARCHAR(64),
    active                      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at                  TIMESTAMP    NOT NULL
);

CREATE TABLE app_users (
    id          UUID PRIMARY KEY,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password    VARCHAR(60)  NOT NULL,
    role        VARCHAR(20)  NOT NULL,
    company_id  UUID         REFERENCES companies (id),
    first_name  VARCHAR(128),
    last_name   VARCHAR(128),
    enabled     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL
);
CREATE INDEX idx_app_users_company ON app_users (company_id);

CREATE TABLE verification_records (
    id                          UUID PRIMARY KEY,
    user_id                     UUID         NOT NULL REFERENCES app_users (id),
    code                        VARCHAR(64)  NOT NULL,
    verification_record_type    VARCHAR(32)  NOT NULL,
    confirmed                   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at                  TIMESTAMP    NOT NULL,
    expires_at                  TIMESTAMP    NOT NULL
);
CREATE INDEX idx_verification_code ON verification_records (code);

-- ---------- Domain ----------

CREATE TABLE work_points (
    id          UUID PRIMARY KEY,
    company_id  UUID         NOT NULL REFERENCES companies (id),
    name        VARCHAR(255) NOT NULL,
    address     VARCHAR(512),
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL
);
CREATE INDEX idx_work_points_company ON work_points (company_id);

CREATE TABLE partners (
    id                      UUID PRIMARY KEY,
    company_id              UUID         NOT NULL REFERENCES companies (id),
    name                    VARCHAR(255) NOT NULL,
    cui                     VARCHAR(32),
    authorization_number    VARCHAR(128),
    authorization_expiry    DATE,
    type                    VARCHAR(20)  NOT NULL,
    active                  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP    NOT NULL
);
CREATE INDEX idx_partners_company ON partners (company_id);

-- Global nomenclator (NOT per tenant).
CREATE TABLE waste_codes (
    id          UUID PRIMARY KEY,
    code        VARCHAR(16)  NOT NULL UNIQUE,
    name        VARCHAR(512) NOT NULL,
    hazardous   BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE TABLE waste_movements (
    id                      UUID PRIMARY KEY,
    company_id              UUID          NOT NULL REFERENCES companies (id),
    work_point_id           UUID          NOT NULL REFERENCES work_points (id),
    date                    DATE          NOT NULL,
    waste_code_id           UUID          NOT NULL REFERENCES waste_codes (id),
    quantity                NUMERIC(14,3) NOT NULL,
    unit                    VARCHAR(10)   NOT NULL,
    operation               VARCHAR(20)   NOT NULL,
    partner_id              UUID          REFERENCES partners (id),
    document_reference      VARCHAR(255),
    notes                   VARCHAR(1000),
    client_generated_id     UUID,
    deleted                 BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at              TIMESTAMP,
    deleted_by              UUID,
    created_by              UUID          NOT NULL,
    created_at              TIMESTAMP     NOT NULL,
    updated_at              TIMESTAMP     NOT NULL,
    version                 BIGINT
);
CREATE INDEX idx_movements_company ON waste_movements (company_id);
CREATE INDEX idx_movements_scope ON waste_movements (company_id, work_point_id, date);
CREATE INDEX idx_movements_updated ON waste_movements (company_id, updated_at);
-- Idempotency for (future) offline sync: a client-generated id is unique within a tenant.
CREATE UNIQUE INDEX uq_movements_client_id ON waste_movements (company_id, client_generated_id)
    WHERE client_generated_id IS NOT NULL;

CREATE TABLE attachments (
    id              UUID PRIMARY KEY,
    movement_id     UUID         NOT NULL REFERENCES waste_movements (id),
    url             VARCHAR(1024) NOT NULL,
    public_id       VARCHAR(512) NOT NULL,
    file_name       VARCHAR(255),
    content_type    VARCHAR(128),
    created_at      TIMESTAMP    NOT NULL
);
CREATE INDEX idx_attachments_movement ON attachments (movement_id);

CREATE TABLE monthly_evidences (
    id                  UUID PRIMARY KEY,
    company_id          UUID          NOT NULL REFERENCES companies (id),
    work_point_id       UUID          NOT NULL REFERENCES work_points (id),
    year                INT           NOT NULL,
    month               INT           NOT NULL,
    waste_code_id       UUID          NOT NULL REFERENCES waste_codes (id),
    total_generated     NUMERIC(16,3) NOT NULL DEFAULT 0,
    total_collected     NUMERIC(16,3) NOT NULL DEFAULT 0,
    total_handed_over   NUMERIC(16,3) NOT NULL DEFAULT 0,
    total_recovered     NUMERIC(16,3) NOT NULL DEFAULT 0,
    total_disposed      NUMERIC(16,3) NOT NULL DEFAULT 0,
    generated_at        TIMESTAMP     NOT NULL,
    CONSTRAINT uq_evidence_scope UNIQUE (company_id, work_point_id, year, month, waste_code_id)
);
CREATE INDEX idx_evidence_period ON monthly_evidences (company_id, work_point_id, year, month);

CREATE TABLE reporting_deadlines (
    id              UUID PRIMARY KEY,
    company_id      UUID         NOT NULL REFERENCES companies (id),
    report_type     VARCHAR(20)  NOT NULL,
    due_date        DATE         NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    completed_at    TIMESTAMP,
    completion_note VARCHAR(500),
    warned_7_days   BOOLEAN      NOT NULL DEFAULT FALSE,
    warned_1_day    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP    NOT NULL,
    CONSTRAINT uq_deadline_scope UNIQUE (company_id, report_type, due_date)
);
CREATE INDEX idx_deadlines_company ON reporting_deadlines (company_id, due_date);

-- ---------- Audit checklist (Phase 1.5: schema only) ----------

CREATE TABLE audit_checklist_templates (
    id          UUID PRIMARY KEY,
    text        VARCHAR(1000) NOT NULL,
    category    VARCHAR(128),
    sort_order  INT           NOT NULL DEFAULT 0,
    active      BOOLEAN       NOT NULL DEFAULT TRUE
);

CREATE TABLE audit_results (
    id          UUID PRIMARY KEY,
    company_id  UUID          NOT NULL REFERENCES companies (id),
    template_id UUID          REFERENCES audit_checklist_templates (id),
    status      VARCHAR(20)   NOT NULL,
    note        VARCHAR(1000),
    created_at  TIMESTAMP     NOT NULL
);
CREATE INDEX idx_audit_results_company ON audit_results (company_id);
