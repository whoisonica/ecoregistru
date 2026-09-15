-- Plata abonamentelor, F3 + F4 (ecoregistru-docs/docs/plata-abonamente.md): cardul prin Netopia,
-- mementourile de neplată, doar-citirea și oprirea abonamentului.
--
-- **La card, factura se emite ÎNTÂI** (decizia din 15.09.2026, „cum e mai safe"): rularea zilnică emite
-- factura în FGO la fel ca la transfer, iar cardul plătește o factură existentă. Niciun ban nu intră fără
-- factură, iar un card refuzat lasă o factură neplătită, exact ca un transfer întârziat.

-- Cum plătește clientul. NULL = n-a ales încă, tratat ca transfer (factura are IBAN-ul oricum).
-- Tokenul debitează cardul fără client: nu pleacă la client și nu intră în log decât mascat. Abonamentul
-- nu e pe lista albă a AuditInterceptor, deci nici jurnalul de audit nu-l vede.
-- `ends_on`: ultima zi facturată după oprire (§9.3). După ea abonamentul e CANCELLED, iar contul doar citește.
ALTER TABLE subscriptions
    ADD COLUMN payment_method  VARCHAR(16),
    ADD COLUMN card_token      VARCHAR(500),
    ADD COLUMN card_pan_masked VARCHAR(32),
    ADD COLUMN card_expiry     VARCHAR(7),
    ADD COLUMN ends_on         DATE,
    ADD CONSTRAINT subscriptions_payment_method CHECK (payment_method IN ('CARD', 'TRANSFER'));

-- `paid_by`: CARD dacă a plătit-o Netopia, TRANSFER dacă FGO a văzut-o în extras.
-- `fgo_collected_at`: încasarea cardului trecută în FGO (`factura/incasare`). O plată cu cardul e PAID la noi
-- din clipa IPN-ului; FGO poate fi indisponibil atunci, iar rularea următoare reîncearcă.
-- Mementourile se scriu o singură dată fiecare, după trimitere, ca mailul căzut să plece din nou.
ALTER TABLE subscription_invoices
    ADD COLUMN paid_by              VARCHAR(16),
    ADD COLUMN fgo_collected_at     TIMESTAMPTZ,
    ADD COLUMN overdue_mailed_at    TIMESTAMPTZ,
    ADD COLUMN warning_mailed_at    TIMESTAMPTZ,
    ADD COLUMN read_only_mailed_at  TIMESTAMPTZ,
    ADD CONSTRAINT subscription_invoices_paid_by CHECK (paid_by IN ('CARD', 'TRANSFER'));

-- O încercare de plată cu cardul pentru o factură. `order_id` e ce trimitem la Netopia și ce ne întoarce
-- IPN-ul: legătura dintre notificare și factură. CHECKOUT = clientul plătește pe pagina Netopia;
-- TOKEN = debitarea automată cu cardul salvat.
CREATE TABLE card_payments (
    id           UUID PRIMARY KEY,
    invoice_id   UUID          NOT NULL REFERENCES subscription_invoices (id),
    order_id     VARCHAR(64)   NOT NULL UNIQUE,
    kind         VARCHAR(16)   NOT NULL,
    amount       NUMERIC(10,2) NOT NULL,
    status       VARCHAR(16)   NOT NULL,
    ntp_id       VARCHAR(64),
    payment_url  VARCHAR(1000),
    error        VARCHAR(1000),
    created_at   TIMESTAMPTZ   NOT NULL,
    updated_at   TIMESTAMPTZ   NOT NULL,

    CONSTRAINT card_payments_kind CHECK (kind IN ('CHECKOUT', 'TOKEN')),
    CONSTRAINT card_payments_status CHECK (status IN ('STARTED', 'PAID', 'FAILED'))
);

CREATE INDEX card_payments_invoice ON card_payments (invoice_id);

-- Notificările Netopia, brute, după verificarea semnăturii. Netopia retrimite o notificare până primește
-- 200; UNIQUE-ul pe (ntpID, stare) face ca a doua să nu mai încaseze nimic.
CREATE TABLE payment_notifications (
    id          UUID PRIMARY KEY,
    ntp_id      VARCHAR(64)  NOT NULL,
    order_id    VARCHAR(64),
    status      INTEGER      NOT NULL,
    body        TEXT         NOT NULL,
    received_at TIMESTAMPTZ  NOT NULL,

    CONSTRAINT payment_notifications_once UNIQUE (ntp_id, status)
);
