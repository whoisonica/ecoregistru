-- Lista deșeurilor, ediția pentru baterii: Decizia delegată (UE) 2025/934 a Comisiei
-- (JO L 20.05.2025), care modifică anexa la Decizia 2000/532/CE. **Se aplică de la 9 noiembrie 2026**
-- (art. 2). Text RO citit în CELLAR pe 28.09.2026; extrasul și contextul: docs/surse-oficiale.md §3.5.
--
-- Ce schimbă decizia, punct cu punct din anexa ei:
--   * 42 de coduri noi: 10 08 21–26, 16 06 07–15, 16 06 22–35, 19 02 12–13, capitolul 19 14, 20 01 42–44;
--   * 4 coduri scoase: 16 06 05, 19 02 11*, 20 01 33*, 20 01 34;
--   * 6 coduri rămân, cu alt nume: 09 01 11*, 16 06 01–04, 16 06 06 — iar **16 06 04 devine periculos**.
--
-- De ce trei feluri de coloane, și nu o reîncărcare a CSV-ului (tiparul lui V4):
--   * `valid_from` / `valid_to` — un cod se poate alege doar pe o mișcare din intervalul lui. Codurile noi
--     nu există în drept înainte de 9.11.2026, cele scoase nu mai există după 8.11.2026. Nimic nu se
--     șterge: mișcările și fișele vechi numesc codul prin cheie străină (vezi comentariul din V4).
--   * `pending_from` / `pending_name` / `pending_hazardous` — schimbarea unui cod care rămâne. Codul e
--     unic în tabel, deci nu pot sta două rânduri pentru 16 06 04. Schimbarea se scrie în bază la data ei
--     de `WasteCodeListScheduler` (zilnic și la pornire), iar de atunci încolo toate citirile — inclusiv
--     interogările de totaluri, care citesc `name` și `hazardous` direct — văd lista nouă. Fără deploy la
--     dată fixă.
--   * `mirror_of` pentru codurile noi, cu aceeași regulă ca în V37 („numele citează un cod periculos").
--     Planificatorul o reface pentru tot tabelul după ce aplică o schimbare (16 06 04 periculos își pierde
--     perechea față de 16 06 03).
--
-- ⚠️ Liberă a fost V79, dar e rezervată pentru F6b (pachetul SIATD, modulul de depozit). Flyway rulează
-- aici fără out-of-order: dacă V80 ajunge pe producție înaintea F6b, F6b se renumerotează în V81.
--
-- Amprenta după 9.11.2026: 880 de coduri valabile (842 + 42 − 4), din care 435 periculoase
-- (408 + 28 noi + 16 06 04 − 19 02 11* − 20 01 33*). În tabel rămân 884 de rânduri.

ALTER TABLE waste_codes
    ADD COLUMN valid_from DATE,
    ADD COLUMN valid_to DATE,
    ADD COLUMN pending_from DATE,
    ADD COLUMN pending_name TEXT,
    ADD COLUMN pending_hazardous BOOLEAN;

COMMENT ON COLUMN waste_codes.valid_from IS
    'Prima zi in care codul exista in lista deseurilor. NULL = din lista de baza (Decizia 2014/955/UE, 1.06.2015).';
COMMENT ON COLUMN waste_codes.valid_to IS
    'Ultima zi in care codul exista in lista deseurilor. NULL = in vigoare.';
COMMENT ON COLUMN waste_codes.pending_from IS
    'Ziua de la care codul primeste pending_name / pending_hazardous. Aplicata de WasteCodeListScheduler, apoi golita.';

-- Scoase de Decizia (UE) 2025/934, anexa pct. 3, 4 și 7.
UPDATE waste_codes SET valid_to = DATE '2026-11-08'
WHERE code IN ('16 06 05', '19 02 11', '20 01 33', '20 01 34');

-- Rămân, cu alt nume (anexa pct. 1 și 3). 16 06 04 devine periculos.
UPDATE waste_codes w
SET pending_from = DATE '2026-11-09',
    pending_name = v.name,
    pending_hazardous = v.hazardous
FROM (VALUES
    ('09 01 11', 'aparate fotografice de unică folosință cu baterii, incluse la 16 06 01-16 06 04, 16 06 07-16 06 11 sau 16 06 14', true),
    ('16 06 01', 'deșeuri de acumulatori cu plumb acid', true),
    ('16 06 02', 'deșeuri de baterii cu nichel-cadmiu', true),
    ('16 06 03', 'deșeuri de baterii cu conținut de mercur', true),
    ('16 06 04', 'deșeuri de baterii alcaline (altele decât cele menționate la 16 06 03)', true),
    ('16 06 06', 'electroliți colectați separat din deșeuri de baterii', true)
) AS v(code, name, hazardous)
WHERE w.code = v.code;

-- Coduri noi (anexa pct. 2, 3, 5, 6 și 8), valabile de la 9.11.2026.
INSERT INTO waste_codes (id, code, name, hazardous, valid_from)
SELECT gen_random_uuid(), v.code, v.name, v.hazardous, DATE '2026-11-09'
FROM (VALUES
    ('10 08 21', 'zguri provenite din reciclarea deșeurilor de baterii pe bază de litiu cu conținut de substanțe periculoase', true),
    ('10 08 22', 'zguri provenite din reciclarea deșeurilor de baterii pe bază de litiu, altele decât cele menționate la 10 08 21', false),
    ('10 08 23', 'zguri provenite din reciclarea deșeurilor de baterii pe bază de nichel cu conținut de substanțe periculoase', true),
    ('10 08 24', 'zguri provenite din reciclarea deșeurilor de baterii pe bază de nichel, altele decât cele menționate la 10 08 23', false),
    ('10 08 25', 'zguri provenite din reciclarea altor deșeuri de baterii cu conținut de substanțe periculoase, cu excepția celor menționate la 10 04 01, 10 08 21 și 10 08 23', true),
    ('10 08 26', 'zguri provenite din reciclarea altor deșeuri de baterii, altele decât cele menționate la 10 08 25', false),
    ('16 06 07', 'deșeuri de baterii pe bază de litiu', true),
    ('16 06 08', 'deșeuri de baterii pe bază de nichel, altele decât cele menționate la 16 06 02 (de exemplu NiMH, Na-NiCl2)', true),
    ('16 06 09', 'deșeuri de baterii pe bază de zinc, inclusiv baterii cu oxid de argint', true),
    ('16 06 10', 'deșeuri de baterii pe bază de sodiu cu conținut de substanțe periculoase (cu excepția celor menționate la 16 06 11)', true),
    ('16 06 11', 'deșeuri de baterii pe bază de sodiu și sulf', true),
    ('16 06 12', 'alte deșeuri de baterii pe bază de sodiu (cu excepția celor menționate la 16 06 10 și 16 06 11)', false),
    ('16 06 13', 'deșeuri de baterii mixte', true),
    ('16 06 14', 'alte deșeuri de baterii cu conținut de substanțe periculoase', true),
    ('16 06 15', 'deșeuri de baterii nespecificate în altă parte, altele decât cele menționate la 16 06 12 și 16 06 14', false),
    ('16 06 22', 'deșeuri provenite din fabricarea acumulatorilor cu plumb acid cu conținut de substanțe periculoase (de exemplu pastă de plumb)', true),
    ('16 06 23', 'deșeuri provenite din fabricarea acumulatorilor cu plumb acid, altele decât cele menționate la 16 06 22', false),
    ('16 06 24', 'deșeuri provenite din fabricarea bateriilor pe bază de litiu cu conținut de substanțe periculoase (de exemplu resturi de material catodic, suspensii catodice, celule, module și/sau ansamble de baterii care nu sunt conforme cu specificațiile)', true),
    ('16 06 25', 'deșeuri provenite din fabricarea bateriilor pe bază de litiu, altele decât cele menționate la 16 06 24 (de exemplu resturi de material anodic)', false),
    ('16 06 26', 'deșeuri provenite din fabricarea bateriilor pe bază de nichel cu conținut de substanțe periculoase (de exemplu material catodic lichid și solid)', true),
    ('16 06 27', 'deșeuri provenite din fabricarea bateriilor pe bază de nichel, altele decât cele menționate la 16 06 26', false),
    ('16 06 28', 'deșeuri provenite din fabricarea bateriilor alcaline cu conținut de substanțe periculoase', true),
    ('16 06 29', 'deșeuri provenite din fabricarea bateriilor alcaline, altele decât cele menționate la 16 06 28', false),
    ('16 06 30', 'deșeuri provenite din fabricarea bateriilor pe bază de zinc cu conținut de substanțe periculoase', true),
    ('16 06 31', 'deșeuri provenite din fabricarea bateriilor pe bază de zinc, altele decât cele menționate la 16 06 30', false),
    ('16 06 32', 'deșeuri provenite din fabricarea bateriilor pe bază de sodiu cu conținut de substanțe periculoase', true),
    ('16 06 33', 'deșeuri provenite din fabricarea bateriilor pe bază de sodiu, altele decât cele menționate la 16 06 32', false),
    ('16 06 34', 'deșeuri provenite din fabricarea bateriilor cu conținut de substanțe periculoase, altele decât cele menționate la 16 06 22, 16 06 24, 16 06 26, 16 06 28, 16 06 30 și 16 06 32', true),
    ('16 06 35', 'deșeuri provenite din fabricarea bateriilor, altele decât cele menționate la 16 06 23, 16 06 25, 16 06 27, 16 06 29, 16 06 31 și 16 06 33', false),
    ('19 02 12', 'săruri solide și soluții cu conținut de metale grele provenite din reciclarea bateriilor', true),
    ('19 02 13', 'alte deșeuri cu conținut de substanțe periculoase', true),
    ('19 14 01', 'fracțiuni intermediare din tratarea termică și/sau mecanică a deșeurilor de acumulatori cu plumb acid și a deșeurilor provenite din fabricarea acumulatorilor cu plumb acid conținând un amestec de materiale de electrozi', true),
    ('19 14 02', 'fracțiuni intermediare din tratarea termică și/sau mecanică a deșeurilor de baterii pe bază de litiu și a deșeurilor provenite din fabricarea bateriilor pe bază de litiu conținând un amestec de materiale de electrozi', true),
    ('19 14 03', 'fracțiuni intermediare din tratarea termică și/sau mecanică a deșeurilor de baterii pe bază de nichel și a deșeurilor provenite din fabricarea bateriilor pe bază de nichel conținând un amestec de materiale de electrozi', true),
    ('19 14 04', 'fracțiuni intermediare din tratarea termică și/sau mecanică a deșeurilor de baterii alcaline și a deșeurilor provenite din fabricarea bateriilor alcaline conținând un amestec de materiale de electrozi', true),
    ('19 14 05', 'fracțiuni intermediare din tratarea termică și/sau mecanică a deșeurilor de baterii pe bază de zinc și a deșeurilor provenite din fabricarea bateriilor pe bază de zinc conținând un amestec de materiale de electrozi', true),
    ('19 14 06', 'fracțiuni intermediare din tratarea termică și/sau mecanică a deșeurilor de baterii pe bază de sodiu și a deșeurilor provenite din fabricarea bateriilor pe bază de sodiu conținând un amestec de materiale de electrozi', true),
    ('19 14 07', 'fracțiuni intermediare din tratarea termică și/sau mecanică a deșeurilor de baterii și a deșeurilor provenite din fabricarea bateriilor conținând un amestec de materiale de electrozi, nespecificate la 19 14 01-19 14 06', true),
    ('19 14 08', 'aliaje provenite din reciclarea deșeurilor de baterii (în formă masivă)', false),
    ('20 01 42', 'deșeuri de baterii incluse la mențiunile 16 06 01-16 06 04, 16 06 08-16 06 11 sau 16 06 14 și deșeuri de baterii mixte care conțin respectivele deșeuri de baterii, inclusiv pe cele de la 16 06 07', true),
    ('20 01 43', 'deșeuri de baterii pe bază de litiu incluse la 16 06 07', true),
    ('20 01 44', 'deșeuri de baterii, altele decât cele menționate la 20 01 42 și 20 01 43', false)
) AS v(code, name, hazardous);

-- Perechile-oglindă ale codurilor noi, cu regula din V37: 10 08 22/24/26, 16 06 12, 16 06 15,
-- 16 06 23/25/27/29/31/33 și 20 01 44 (16 06 35 citează numai coduri nepericuloase, deci nu e oglindă).
WITH mirror AS (
    SELECT nonhaz.id,
           string_agg(haz.code, ', ' ORDER BY haz.code) AS hazardous_codes
    FROM waste_codes nonhaz
    JOIN waste_codes haz
      ON haz.hazardous
     AND haz.code <> nonhaz.code
     AND nonhaz.name LIKE '%' || haz.code || '%'
    WHERE NOT nonhaz.hazardous
      AND nonhaz.valid_from = DATE '2026-11-09'
    GROUP BY nonhaz.id
)
UPDATE waste_codes w
SET mirror_of = mirror.hazardous_codes
FROM mirror
WHERE w.id = mirror.id;
