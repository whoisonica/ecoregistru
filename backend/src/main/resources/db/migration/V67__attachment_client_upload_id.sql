-- Cheia de idempotență a urcării unui atașament (aplicația mobilă, 26.09.2026).
--
-- Coada de pe telefon urcă poza avizului după ce predarea are id. Dacă răspunsul se pierde pe drum
-- (semnal slab la rampă), telefonul nu știe că poza a urcat și o trimite din nou — deci aceeași poză
-- ieșea de două ori în dosar. Telefonul trimite acum o cheie a lui, aceeași la fiecare reîncercare;
-- a doua urcare cu aceeași cheie pe aceeași mișcare primește atașamentul existent. Webul nu trimite
-- cheia, deci rândurile lui rămân cu NULL, iar indexul parțial nu le atinge.
ALTER TABLE attachments
    ADD COLUMN client_upload_id UUID;

CREATE UNIQUE INDEX ux_attachments_movement_client_upload
    ON attachments (movement_id, client_upload_id)
    WHERE client_upload_id IS NOT NULL;
