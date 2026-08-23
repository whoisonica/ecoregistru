-- ETAPA G2 — the intake form: the questions the client answers before an account exists.
--
-- EcoRegistru is a closed register (V7 removed self-registration), so this table is what replaces
-- it: the client fills in a form, support reads it and creates the account. Nothing here is a
-- login, a session or a permission — a row is a request, and it stays a request until a
-- PLATFORM_ADMIN turns it into a company.
--
-- The questions are the account profile of V8, in the order a client can actually answer them:
-- who they are, where they work, what they do with waste, and — only for a collector — what they
-- transport with. Support transcribes the answers into the company, and from then on the screens
-- offer only what this kind of business needs.

CREATE TABLE account_requests (
    id                        UUID PRIMARY KEY,

    -- Who they are.
    company_name              VARCHAR(255) NOT NULL,
    cui                       VARCHAR(20)  NOT NULL,
    company_type              VARCHAR(20)  NOT NULL,
    -- Two addresses, deliberately: the registered office and the site where waste is actually
    -- produced are routinely different, and the legal records are kept per work point.
    company_address           VARCHAR(500),
    work_point_name           VARCHAR(255),
    work_point_address        VARCHAR(500),

    -- Who to talk to.
    contact_name              VARCHAR(255),
    contact_email             VARCHAR(255) NOT NULL,
    contact_phone             VARCHAR(50),

    -- The environmental authorization.
    environmental_auth_number VARCHAR(255),
    environmental_auth_expiry DATE,

    -- Asked only of a collector.
    transport_means           VARCHAR(500),
    transport_license_number  VARCHAR(255),
    transport_license_expiry  DATE,

    -- What happens to the waste. Free text for the waste codes on purpose: the nomenclator search
    -- is behind authentication, and a client typing "carton, folie, moloz" is more honest than a
    -- client guessing six-digit codes. Support maps them to real codes when creating the account.
    waste_codes_text          VARCHAR(2000),
    notes                     VARCHAR(2000),

    -- Lifecycle. A request is never deleted: it is the paper trail behind an account.
    status                    VARCHAR(20)  NOT NULL DEFAULT 'NEW',
    created_company_id        UUID         REFERENCES companies (id),
    handled_by                UUID,
    handled_at                TIMESTAMP,
    created_at                TIMESTAMP    NOT NULL
);

CREATE INDEX idx_account_requests_status ON account_requests (status, created_at);

-- Which R/D operations the client says they work with. Same shape as company_operation_codes,
-- so approving a request is a copy rather than a translation.
CREATE TABLE account_request_operation_codes (
    account_request_id UUID        NOT NULL REFERENCES account_requests (id),
    operation_code     VARCHAR(10) NOT NULL,
    PRIMARY KEY (account_request_id, operation_code)
);
