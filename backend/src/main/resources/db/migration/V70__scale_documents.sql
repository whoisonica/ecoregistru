-- Depozitul, F2 — fișierele cântarului (restul din D2.3): buletinul de verificare metrologică pe rândul
-- verificării (IML 3-05 art. 17) și dovada declarării la BRML pe cântar (OG 20/1992 art. 24 alin. 1).
--
-- Câte un fișier pe loc: `scale_event_id` NULL = dovada BRML a cântarului, altfel buletinul acelui rând.
-- Un fișier nou pe același loc îl înlocuiește pe cel vechi (serviciul șterge întâi activul din Cloudinary).
-- Stocarea e cea a atașamentelor de la mișcări (11-bis): `authenticated`, livrat doar prin API.
--
-- Nu stă în `attachments`: acolo `movement_id` e obligatoriu și îl citesc dosarul și estimarea lui.

CREATE TABLE scale_documents (
    id               UUID          PRIMARY KEY,
    scale_id         UUID          NOT NULL REFERENCES scales (id) ON DELETE CASCADE,
    scale_event_id   UUID          REFERENCES scale_events (id) ON DELETE CASCADE,
    public_id        VARCHAR(255)  NOT NULL,
    resource_type    VARCHAR(32)   NOT NULL,
    delivery_type    VARCHAR(32)   NOT NULL,
    format           VARCHAR(32),
    file_name        VARCHAR(255),
    content_type     VARCHAR(128),
    size_bytes       BIGINT        NOT NULL,
    created_by       UUID          NOT NULL,
    created_at       TIMESTAMP     NOT NULL
);

CREATE UNIQUE INDEX uq_scale_documents_brml ON scale_documents (scale_id) WHERE scale_event_id IS NULL;
CREATE UNIQUE INDEX uq_scale_documents_event ON scale_documents (scale_event_id) WHERE scale_event_id IS NOT NULL;
