-- G2 — notificările pe telefon (todo-mobil §4 G2, M1c).
--
-- **Ce e coloana.** Tokenul Expo Push al telefonului („ExponentPushToken[…]”), pe sesiunea lui de
-- dispozitiv (V50). Nu un tabel separat, dinadins: o sesiune stinsă (ieșire din cont, parolă schimbată,
-- cont dezactivat, 60 de zile nefolosită) nu mai primește nimic, fără nicio listă de ținut la zi.
--
-- **Nu e secret ca tokenul de reîmprospătare**, deci stă în clar: cu el se poate doar trimite o
-- notificare prin Expo, iar Expo cere oricum contul proiectului pentru asta când e setat un token de acces.
--
-- **Un telefon, o singură sesiune cu tokenul lui.** Când îl declară o sesiune nouă, serviciul îl șterge de
-- pe celelalte (aplicația reinstalată fără ieșire din cont lasă sesiunea veche vie). Altfel telefonul unui
-- magazioner nou ar fi primit și alertele celui dinainte.
--
-- **V56:** scrisă ca V54, renumerotată după ce depozitul a dus `V53` (D2.1, api v95) și `V55` (D2.2, api v96) pe producție;
-- `V54` a rămas nefolosit, iar Flyway fără out-of-order n-ar fi acceptat-o după `V55`.
--
-- Aditivă: o coloană nullabilă, nimic de completat pe rândurile existente.

ALTER TABLE device_sessions ADD COLUMN push_token VARCHAR(255);

CREATE INDEX device_sessions_push_token_idx ON device_sessions (push_token) WHERE push_token IS NOT NULL;
