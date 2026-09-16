-- Cât cântărește dosarul de control înainte de descărcare (todo-lansare, „Interfață”).
--
-- Atașamentele sunt aproape toată greutatea arhivei, iar mărimea lor nu era ținută nicăieri: Cloudinary
-- o știe, noi nu. Se scrie de acum la încărcare, din fișierul primit.
--
-- Nullabilă, fără completare: rândurile vechi n-au mărimea, iar a o cere de la Cloudinary pentru fiecare ar
-- fi un apel pe atașament. Ecranul le numără separat („și N fără mărime cunoscută”), nu le ghicește.

ALTER TABLE attachments ADD COLUMN size_bytes BIGINT;
