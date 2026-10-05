-- Plata anuală (05.10.2026). billing_months = lungimea perioadei de facturare: 1 = lunar, 12 = anual;
-- la anual, monthly_price e prețul pe an. Perioada cu indicele anchor_period începe pe period_anchor
-- (NULL = started_at): o comutare lunar <-> anual mută ancora, iar indicele perioadei rămâne absolut.
-- Rândurile existente: lunar, fără ancoră, anchor_period 0 — exact perioadele de dinainte.
ALTER TABLE subscriptions
    ADD COLUMN billing_months SMALLINT NOT NULL DEFAULT 1,
    ADD COLUMN period_anchor DATE,
    ADD COLUMN anchor_period INT NOT NULL DEFAULT 0,
    ADD CONSTRAINT subscriptions_billing_months CHECK (billing_months IN (1, 12));
