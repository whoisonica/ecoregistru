-- F6a — SIATD (Ordinul 701/2024): modulele în care e înrolată firma, fiecare cu data înrolării. O dată nenulă înseamnă
-- „bifat, înrolat din”; nu există stare „bifat fără dată”, fiindcă termenul curge de la ea (art. 18 alin. (10)).
ALTER TABLE companies ADD COLUMN siatd_municipal_from DATE;
ALTER TABLE companies ADD COLUMN siatd_packaging_from DATE;
ALTER TABLE companies ADD COLUMN siatd_weee_from DATE;
ALTER TABLE companies ADD COLUMN siatd_battery_from DATE;
ALTER TABLE companies ADD COLUMN siatd_tyre_from DATE;

-- Confirmarea recepției în SIATD. Termenul nu se stochează: se calculează la citire din dată, linii și înrolare.
ALTER TABLE weighing_operations ADD COLUMN siatd_confirmed_at TIMESTAMPTZ;
ALTER TABLE weighing_operations ADD COLUMN siatd_confirmed_by UUID REFERENCES app_users (id);
ALTER TABLE weighing_operations ADD COLUMN siatd_code VARCHAR(60);
ALTER TABLE weighing_operations ADD CONSTRAINT weighing_operations_siatd_confirmation
    CHECK ((siatd_confirmed_at IS NULL) = (siatd_confirmed_by IS NULL)
           AND (siatd_code IS NULL OR siatd_confirmed_at IS NOT NULL));
