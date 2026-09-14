-- Plata abonamentelor, restul feliei F2 (ecoregistru-docs/docs/plata-abonamente.md §9.4, decisă
-- 15.09.2026): **factura o trimite aplicația**, de pe contact@wastehouse.ro, cu linkul PDF de la FGO.
--
-- `emailed_at` se scrie numai după ce mailul a plecat. Un server de mail căzut azi înseamnă o factură
-- trimisă la rularea de mâine, nu una pierdută; un abonament fără adresă de email rămâne cu factura
-- netrimisă până i se pune una.
ALTER TABLE subscription_invoices
    ADD COLUMN emailed_at TIMESTAMPTZ;

-- Facturile emise înainte de migrare (cele de test, pe firma Demo) nu se trimit acum, în dimineața
-- deployului, cu o lună întârziere.
UPDATE subscription_invoices
SET emailed_at = issued_at
WHERE issued_at IS NOT NULL;
