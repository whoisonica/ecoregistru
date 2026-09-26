-- Depozitul, F2 — cântarul depozitului (D2.3). Temeiul: `docs/surse-oficiale.md` §17.1.
--
-- Un cântar e legal la o cântărire dacă e în uz, are o verificare ADMIS în termen (sau e în primul an de
-- la punerea în funcțiune) și a fost declarat la BRML înainte (OG 20/1992 art. 19, art. 24; L.O.-2022
-- art. 8–9). Regula stă în `ScaleLegality`; aici stau faptele din care se calculează.
--
-- ⚠️ V68: V67 e a aplicației mobile (`attachment_client_upload_id`, 26.09). Numărul se dă în ordinea în
-- care felia ajunge pe dyno — dacă depozitul pleacă primul, se renumerotează cu sesiunea de mobil.

CREATE TABLE scales (
    id                        UUID          PRIMARY KEY,
    company_id                UUID          NOT NULL REFERENCES companies (id) ON DELETE CASCADE,
    -- Cântarul stă într-un depozit; o operațiune a altui depozit nu-l poate folosi.
    work_point_id             UUID          NOT NULL REFERENCES work_points (id),
    -- Cum îi spun oamenii: „Podul basculă”, „Platforma de la hală”.
    name                      VARCHAR(100)  NOT NULL,
    serial_number             VARCHAR(100),
    -- Tipul, liber (pod basculă, platformă, automat) — ca la vehicule, o listă fixă l-ar rata pe al clientului.
    kind                      VARCHAR(100),
    -- Clasa de precizie și diviziunea de verificare „e”, din plăcuța cântarului. Din ele se calculează
    -- toleranța la transfer (D2.5; HG 710/2015 anexa 1 tab. 3). Opționale: nu blochează înregistrarea.
    accuracy_class            VARCHAR(4),
    division_kg               NUMERIC(10,3),
    commissioned_on           DATE,
    -- Declararea la BRML (art. 24 alin. 1): data și dovada (numărul de înregistrare). NULL = nedeclarat.
    brml_declared_on          DATE,
    brml_reference            VARCHAR(100),
    status                    VARCHAR(16)   NOT NULL DEFAULT 'IN_USE',
    -- Data pentru care s-a trimis deja alerta de 30 de zile; o dată nouă o rearmează (ca la vehicule, V53).
    expiry_warning_sent_for   DATE,
    created_at                TIMESTAMP     NOT NULL,

    CONSTRAINT scales_status_known CHECK (status IN ('IN_USE', 'OUT_OF_USE', 'SEALED')),
    CONSTRAINT scales_class_known CHECK (accuracy_class IS NULL OR accuracy_class IN ('I', 'II', 'III', 'IIII')),
    CONSTRAINT scales_division_positive CHECK (division_kg IS NULL OR division_kg > 0)
);

CREATE INDEX idx_scales_company ON scales (company_id);
CREATE UNIQUE INDEX uq_scales_name ON scales (company_id, work_point_id, name);

-- Istoricul: verificările (cu buletinul), reparațiile și incidentele. Se scrie în ordine, nu se rescrie
-- valabilitatea în sus.
CREATE TABLE scale_events (
    id                UUID          PRIMARY KEY,
    scale_id          UUID          NOT NULL REFERENCES scales (id) ON DELETE CASCADE,
    kind              VARCHAR(16)   NOT NULL,
    event_date        DATE          NOT NULL,
    -- Doar la verificare: ADMIS (true) / RESPINS (false) și numărul buletinului (IML 3-05 art. 17 alin. (2)).
    admitted          BOOLEAN,
    bulletin_number   VARCHAR(60),
    -- „Valabil până la”, doar la ADMIS: cel mult un an de la verificare. Se poate scrie mai scurt (dacă
    -- buletinul zice așa), niciodată mai lung.
    valid_until       DATE,
    laboratory        VARCHAR(255),
    verifier          VARCHAR(255),
    notes             VARCHAR(1000),
    created_by        UUID          NOT NULL,
    created_at        TIMESTAMP     NOT NULL,

    CONSTRAINT scale_events_kind_known CHECK (kind IN ('VERIFICATION', 'REPAIR', 'INCIDENT')),
    CONSTRAINT scale_events_verification_fields CHECK (
        (kind = 'VERIFICATION' AND admitted IS NOT NULL AND bulletin_number IS NOT NULL)
        OR (kind <> 'VERIFICATION' AND admitted IS NULL AND bulletin_number IS NULL)),
    CONSTRAINT scale_events_validity_only_admitted CHECK (
        (admitted IS TRUE AND valid_until IS NOT NULL
             AND valid_until >= event_date AND valid_until <= event_date + INTERVAL '12 months')
        OR (admitted IS NOT TRUE AND valid_until IS NULL))
);

CREATE INDEX idx_scale_events_scale ON scale_events (scale_id, event_date);

-- Pe operațiune: cântarul folosit. Fără ON DELETE: un cântar cu cântăriri nu se șterge, se scoate din uz.
-- Dacă la finalizare cântarul nu era legal, cine a aprobat a confirmat cu motiv (decizia proprietarului,
-- 26.09.2026); se păstrează și starea de atunci, fiindcă istoricul cântarului se mai poate completa.
ALTER TABLE weighing_operations
    ADD COLUMN scale_id           UUID          REFERENCES scales (id),
    ADD COLUMN scale_state        VARCHAR(16),
    ADD COLUMN scale_override_reason VARCHAR(1000),
    ADD CONSTRAINT weighing_operations_scale_reason
        CHECK (scale_override_reason IS NULL OR (scale_state IS NOT NULL AND scale_state <> 'VALID'));
