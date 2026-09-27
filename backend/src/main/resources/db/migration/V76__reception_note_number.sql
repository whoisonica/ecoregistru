-- D1.17a — intrarea de la o persoană fizică primește la finalizare borderoul (liniile plătite) și/sau NIR-ul 14-3-1A
-- (liniile preluate gratuit, OMFP 2634/2015 anexa 2). V75 e a sesiunii de mobil.
ALTER TABLE weighing_operations ADD COLUMN reception_note_number INTEGER;
CREATE UNIQUE INDEX uq_weighing_operations_reception_note ON weighing_operations (company_id, reception_note_number)
    WHERE reception_note_number IS NOT NULL;

-- Numerotarea retroactivă a intrărilor PF deja finalizate (și a celor anulate după finalizare: numărul lor ține seria
-- fără goluri), în ordinea finalizării. O linie cu preț NULL nu decide nimic.
WITH eligible AS (
    SELECT o.id, o.company_id,
           row_number() OVER (PARTITION BY o.company_id ORDER BY o.finalized_at, o.number) AS rn
    FROM weighing_operations o
    WHERE o.type = 'IN' AND o.natural_person_id IS NOT NULL AND o.finalized_at IS NOT NULL
      AND o.borderou_number IS NULL
      AND EXISTS (SELECT 1 FROM waste_movements m
                  WHERE m.weighing_operation_id = o.id AND NOT m.deleted AND m.unit_price > 0)
), base AS (
    SELECT company_id, coalesce(max(borderou_number), 0) AS top FROM weighing_operations GROUP BY company_id
)
UPDATE weighing_operations w SET borderou_number = base.top + eligible.rn
FROM eligible JOIN base ON base.company_id = eligible.company_id
WHERE w.id = eligible.id;

WITH eligible AS (
    SELECT o.id, row_number() OVER (PARTITION BY o.company_id ORDER BY o.finalized_at, o.number) AS rn
    FROM weighing_operations o
    WHERE o.type = 'IN' AND o.natural_person_id IS NOT NULL AND o.finalized_at IS NOT NULL
      AND o.reception_note_number IS NULL
      AND EXISTS (SELECT 1 FROM waste_movements m
                  WHERE m.weighing_operation_id = o.id AND NOT m.deleted AND m.unit_price = 0)
)
UPDATE weighing_operations w SET reception_note_number = eligible.rn
FROM eligible
WHERE w.id = eligible.id;
