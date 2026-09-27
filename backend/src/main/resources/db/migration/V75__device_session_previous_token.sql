-- G1 — reîmprospătarea al cărei răspuns s-a pierdut (evaluarea mobilului, 27.09.2026).
--
-- **Problema.** `/auth/refresh` înlocuia hash-ul la fiecare folosire, fără nicio toleranță. Dacă
-- răspunsul se pierdea pe drum (semnal slab la rampă), telefonul rămânea cu tokenul vechi, serverul
-- îl uitase deja, iar următoarea reîmprospătare primea 400 — omul era scos din cont pentru o rețea
-- căzută, exact ce voia să evite G1.
--
-- **Ce se schimbă față de V50.** Rândul ține și hash-ul tokenului de dinainte. Tokenul vechi mai
-- deschide numai cât timp cel nou n-a fost folosit niciodată: la prima folosire a celui nou, vechiul
-- e înlocuit cu el și nu mai deschide nimic. O sesiune revocată sau ieșită din termen rămâne închisă
-- pentru amândouă.
--
-- **V75, nu V74:** `V74` e al depozitului (`feat/depozit-d23`), construit în paralel — se deployează
-- după el.
--
-- Aditivă: o coloană care poate fi null; rândurile de azi n-au un token de dinainte.

ALTER TABLE device_sessions ADD COLUMN previous_token_hash CHAR(64);

CREATE INDEX device_sessions_previous_token_idx ON device_sessions (previous_token_hash);
