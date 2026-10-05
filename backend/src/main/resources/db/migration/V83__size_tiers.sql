-- Treptele de angajați (05.10.2026). Treapta e intervalul ales de client (1 = 0–2 ... 5 = 40+),
-- nu un număr de angajați. NULL = abonament dinainte de 05.10.2026, serviciu complet sau consultant.
ALTER TABLE subscriptions
    ADD COLUMN size_tier SMALLINT,
    ADD COLUMN custom_price BOOLEAN NOT NULL DEFAULT FALSE,
    ADD CONSTRAINT subscriptions_size_tier CHECK (size_tier BETWEEN 1 AND 5);

ALTER TABLE account_requests
    ADD COLUMN size_tier SMALLINT,
    ADD CONSTRAINT account_requests_size_tier CHECK (size_tier BETWEEN 1 AND 5);
