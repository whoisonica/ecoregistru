-- Depozitul, F2 — șoferii extinși (D2.2).
--
-- Vehiculul implicit NU primește coloană: șoferul are deja textul `vehicle_registration` (V28), iar
-- de la D2.1 cântarul recunoaște numărul tastat în flotă. O cheie în plus ar spune același lucru de
-- două ori și s-ar putea contrazice cu textul.
--
-- ⚠️ Numărul V55: V54 e ținut de aplicația mobilă (M1c, push, necommis la 16.09). Flyway rulează fără
-- out-of-order, deci cine ajunge al doilea pe dyno își renumerotează migrarea înainte de deploy.

-- Depozitul unde lucrează de obicei. Informativ, ca la vehicule: operațiunea își alege depozitul ei.
ALTER TABLE drivers ADD COLUMN home_work_point_id UUID REFERENCES work_points (id) ON DELETE SET NULL;

-- Atestatul profesional (CPC) sau certificatul ADR, cu data până la care e valabil. Un singur rând:
-- la un depozit de deșeuri contează că șoferul are hârtia valabilă, nu ce fel de hârtie e.
ALTER TABLE drivers ADD COLUMN attestation_number VARCHAR(100);
ALTER TABLE drivers ADD COLUMN attestation_expiry DATE;

-- Data pentru care s-a trimis deja alerta; deduplicare după valoare, ca la vehicule (V53).
ALTER TABLE drivers ADD COLUMN attestation_warning_sent_for DATE;

-- Șoferul ales pe operațiunea de cântar (V46) nu lăsa fișa să se șteargă: cheia n-avea ON DELETE, deci
-- „Șterge definitiv” cădea pe orice șofer folosit la cântar. Nu se vedea fiindcă ecranul nu trimitea
-- șoferul din listă până la D2.2. Operațiunea își păstrează numele tipărit ca text, ca la vehicule.
ALTER TABLE weighing_operations DROP CONSTRAINT weighing_operations_driver_id_fkey;
ALTER TABLE weighing_operations
    ADD CONSTRAINT weighing_operations_driver_id_fkey
    FOREIGN KEY (driver_id) REFERENCES drivers (id) ON DELETE SET NULL;
