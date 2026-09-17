-- Ecranul „Facturare” (todo-clienti-abonamente.md, F-A, 17.09.2026). Rezultatul rulării era un toast care
-- dispărea, iar rularea de la 06:30 nu-l arăta nimănui: „căzute” rămânea o cifră fără firmă. Acum fiecare
-- rulare cu FGO configurat își scrie rezultatul, cu firma și motivul pe fiecare rând.
CREATE TABLE billing_runs (
    id            UUID         PRIMARY KEY,
    started_at    TIMESTAMPTZ  NOT NULL,
    finished_at   TIMESTAMPTZ  NOT NULL,
    -- SCHEDULED = 06:30, MANUAL = butonul „Rulează facturarea acum”.
    kind          VARCHAR(16)  NOT NULL,
    triggered_by  UUID,
    -- BillingRunService.Result ca JSON: cifrele și rândurile, citite întocmai de ecran.
    result_json   TEXT         NOT NULL,

    CONSTRAINT billing_runs_kind CHECK (kind IN ('SCHEDULED', 'MANUAL'))
);
CREATE INDEX idx_billing_runs_started ON billing_runs (started_at DESC);

-- „verificat la 10:29” lângă o factură emisă: când a întrebat ultima oară aplicația FGO de plată, reușit.
ALTER TABLE subscription_invoices ADD COLUMN payment_checked_at TIMESTAMPTZ;
