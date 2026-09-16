-- „Rămâne în stoc” iese de pe ecranul „Generare” (proprietarul, 16.09.2026).
--
-- Un generator n-are cântar și nu ține stoc: deșeul stă în pubelă până vine colectorul, iar cantitatea
-- se află abia la predare, de pe tichetul lui. Deci un rând pe „Generare” e mereu o predare —
-- valorificare (cod R) sau eliminare (cod D) — iar generarea e cea dedusă din ea (`V24`). Serverul
-- refuză de acum o generare fără ieșire (`movement.generation.needs.exit`), și la import.
--
-- Rândurile vechi se șterg, cu acordul proprietarului: pe producție erau 23, toate pe firme de probă
-- (Demo Reciclare, Ardeal Reciclare, Ecodoc), numărate înainte de migrare. Ștergerea e cea obișnuită a
-- aplicației, `deleted = true`, nu una fizică: atașamentele și destinațiile de transport rămân legate.
--
-- Cache-ul evidenței se golește, ca la `V6` și `V24`: o ștergere prin SQL nu trece prin `updated_at`,
-- deci garda de prospețime n-ar vedea-o, iar fișa ar tipări mai departe generările șterse.

UPDATE waste_movements
SET deleted    = TRUE,
    deleted_at = now()
WHERE operation = 'GENERATED'
  AND NOT deleted;

DELETE FROM monthly_evidences;
