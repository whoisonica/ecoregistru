-- Cererile specialistei din 15.09.2026.
--
-- 1. „La anexa 3 să poți să selectezi și data încărcare." Până acum încărcarea era data mișcării.
--    Coloana e opțională: nulă, formularul tipărește tot data mișcării, deci rândurile vechi nu se
--    schimbă pe hârtie.
ALTER TABLE waste_movements ADD COLUMN load_date DATE;

-- 2. „La parteneri să te pună să adaugi licența de transport doar dacă ai peste 3,5 tone."
--    Licența de transport rutier de mărfuri se cere pentru vehicule cu masa maximă autorizată peste
--    3,5 t. Bifa hotărăște dacă rubrica se completează. Un transportator care are deja o licență
--    trecută pornește bifat, ca să nu i se piardă datele la prima salvare.
ALTER TABLE partners ADD COLUMN heavy_vehicles BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE partners
   SET heavy_vehicles = TRUE
 WHERE (transport_license_number IS NOT NULL AND btrim(transport_license_number) <> '')
    OR transport_license_expiry IS NOT NULL;

-- 3. CNP-ul șoferului, pe avizul de însoțire a mărfii („Nume | CNP | CI seria"), după modelul trimis
--    de specialistă. Rubrică separată de actul de identitate, validată cu cifra de control, ca un
--    CNP tastat greșit (adică al altcuiva) să nu intre. Aceleași reguli ca actul de identitate:
--    ascuns în jurnalul de audit, șters de pe mișcări după trei ani calendaristici întregi,
--    șters odată cu fișa șoferului.
ALTER TABLE drivers ADD COLUMN cnp VARCHAR(13);
ALTER TABLE waste_movements ADD COLUMN driver_cnp VARCHAR(13);
