-- Termene proprii (Andreea, 29.09.2026): firma își adaugă singură termene care nu vin din lege prin
-- profilul ei — măsurători de zgomot, analize de apă, emisii, reautorizări. Stau în același tabel ca
-- cele legale, ca să primească aceleași mailuri, aceeași bifă și același loc pe Acasă și în .ics.

ALTER TABLE reporting_deadlines ADD COLUMN title      VARCHAR(120);
ALTER TABLE reporting_deadlines ADD COLUMN recurrence VARCHAR(12);
ALTER TABLE reporting_deadlines ADD COLUMN details    VARCHAR(500);
-- Aparițiile aceluiași termen care se repetă: următoarea se creează la bifarea celei de acum.
ALTER TABLE reporting_deadlines ADD COLUMN series_id  UUID;

-- „Un singur termen de un fel pe zi” rămâne regula termenelor din lege. Două termene proprii pot
-- cădea în aceeași zi (zgomotul și apa, măsurate de același laborator).
ALTER TABLE reporting_deadlines DROP CONSTRAINT uq_deadline_scope;
CREATE UNIQUE INDEX uq_deadline_scope ON reporting_deadlines (company_id, report_type, due_date)
    WHERE report_type <> 'CUSTOM';
CREATE UNIQUE INDEX uq_deadline_series_due ON reporting_deadlines (series_id, due_date)
    WHERE series_id IS NOT NULL;

ALTER TABLE reporting_deadlines ADD CONSTRAINT ck_deadline_custom_fields CHECK (
    report_type <> 'CUSTOM' OR (title IS NOT NULL AND recurrence IS NOT NULL AND series_id IS NOT NULL));
