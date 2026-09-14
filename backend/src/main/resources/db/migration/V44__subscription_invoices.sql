-- Plata abonamentelor, felia F2 (ecoregistru-docs/docs/plata-abonamente.md): facturile lunare pe
-- transfer, emise prin API-ul FGO. Cardul (Netopia) vine în F3, cu migrarea lui.
--
-- **Datele de facturare stau pe abonament**, nu pe firmă: plătitorul e firma directă sau cabinetul,
-- iar cabinetul n-are adresă în bază. FGO cere județul și localitatea din nomenclatorul lui pentru
-- un client din România. Coloanele sunt nule: un abonament fără ele nu ajunge la FGO, iar factura
-- rămâne DRAFT cu motivul scris în `last_error`.

ALTER TABLE subscriptions
    ADD COLUMN billing_email   VARCHAR(100),
    ADD COLUMN billing_county  VARCHAR(100),
    ADD COLUMN billing_city    VARCHAR(100),
    ADD COLUMN billing_address VARCHAR(500);

-- O factură pe perioadă. **Rândul se scrie înainte de cererea la FGO**: id-ul lui pleacă drept
-- `IdExtern`, cu `VerificareDuplicat`, deci o cerere reluată după o cădere nu emite a doua factură.
-- UNIQUE-ul pe (`subscription_id`, `period_start`) e cealaltă plasă: rulat de două ori în aceeași zi,
-- schedulerul nu rezervă a doua perioadă.
--
-- DRAFT  = rezervată la noi, încă neemisă în FGO (sau emiterea a căzut: vezi `last_error`);
-- ISSUED = emisă, cu număr, link și scadență;
-- PAID   = FGO spune că valoarea achitată acoperă factura.
CREATE TABLE subscription_invoices (
    id              UUID PRIMARY KEY,
    subscription_id UUID          NOT NULL REFERENCES subscriptions (id),
    period_start    DATE          NOT NULL,
    period_end      DATE          NOT NULL,
    -- Liniile și totalul, înghețate la începutul perioadei (decizia din 15.09: se numără atunci).
    total           NUMERIC(10,2) NOT NULL,
    lines_json      TEXT          NOT NULL,
    status          VARCHAR(16)   NOT NULL,
    due_date        DATE,
    fgo_serie       VARCHAR(50),
    fgo_numar       VARCHAR(50),
    fgo_link        VARCHAR(500),
    fgo_link_plata  VARCHAR(500),
    amount_paid     NUMERIC(10,2),
    last_error      VARCHAR(1000),
    issued_at       TIMESTAMPTZ,
    paid_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ   NOT NULL,

    CONSTRAINT subscription_invoices_period UNIQUE (subscription_id, period_start),
    CONSTRAINT subscription_invoices_status CHECK (status IN ('DRAFT', 'ISSUED', 'PAID')),
    CONSTRAINT subscription_invoices_issued
        CHECK (status = 'DRAFT' OR (fgo_numar IS NOT NULL AND due_date IS NOT NULL))
);

COMMENT ON TABLE subscription_invoices IS
    'Facturile abonamentelor, emise prin FGO. Rândul precede cererea: id-ul e IdExtern la FGO.';
