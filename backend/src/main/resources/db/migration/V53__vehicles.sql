-- Depozitul, F2 — flota (D2.1).
--
-- Până acum mașina era doar text: pe mișcare (V10), pe șofer (V28) și pe operațiunea de cântar (V46).
-- Tabelul de aici nu înlocuiește textul, îl precompletează. Operațiunea păstrează numărul tipărit ca
-- instantaneu (aceeași regulă ca la șoferi, V28): o mașină vândută sau reînmatriculată nu rescrie
-- Anexa 3 de anul trecut.
--
-- ⚠️ Numărul V53: sesiunea aplicației mobile (M1c, push) plănuia tot V53. Flyway rulează fără
-- out-of-order, deci cine ajunge al doilea pe dyno își renumerotează migrarea înainte de deploy.

CREATE TABLE vehicles (
    id                        UUID          PRIMARY KEY,
    company_id                UUID          NOT NULL REFERENCES companies (id) ON DELETE CASCADE,
    -- Numărul de înmatriculare, scris cu majuscule și fără spații („CJ12ABC”), ca același camion
    -- tastat „cj 12 abc” să nu devină a doua mașină.
    registration              VARCHAR(20)   NOT NULL,
    -- Tipul, liber: „Camion 20 t cu macara”, „Autoutilitară”, „Remorcă”. O listă fixă ar lăsa pe
    -- dinafară exact mașina pe care o are clientul.
    kind                      VARCHAR(100),
    -- Tara standard: cât cântărește goală. Formularul de cântar o arată alături (nu o completează), iar F4 o compară cu tara măsurată.
    standard_tare_kg          NUMERIC(14,3),
    -- Masa maximă autorizată peste 3,5 t: de aici încolo rubrica licenței de transport se completează.
    -- Aceeași bifă și același prag ca la parteneri (V42, cererea specialistei).
    heavy                     BOOLEAN       NOT NULL DEFAULT FALSE,
    itp_expiry                DATE,
    transport_license_number  VARCHAR(100),
    transport_license_expiry  DATE,
    -- Depozitul unde stă de obicei. Informativ: operațiunea își alege depozitul ei.
    home_work_point_id        UUID          REFERENCES work_points (id) ON DELETE SET NULL,
    -- Transportatorul, când mașina nu e a noastră. NULL = flota firmei.
    partner_id                UUID          REFERENCES partners (id) ON DELETE SET NULL,
    -- Data pentru care s-a trimis deja alerta (cea mai apropiată dintre ITP și licență). Deduplicare
    -- după valoare, ca la autorizația partenerului (V30): o dată reînnoită rearmează alerta singură.
    expiry_warning_sent_for   DATE,
    active                    BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at                TIMESTAMP     NOT NULL,

    CONSTRAINT vehicles_tare_positive CHECK (standard_tare_kg IS NULL OR standard_tare_kg > 0),
    -- Licența are sens doar peste 3,5 t; sub prag rubricile rămân goale.
    CONSTRAINT vehicles_license_only_heavy
        CHECK (heavy OR (transport_license_number IS NULL AND transport_license_expiry IS NULL))
);

CREATE INDEX idx_vehicles_company ON vehicles (company_id);
CREATE UNIQUE INDEX uq_vehicles_registration ON vehicles (company_id, registration);

-- Mașina aleasă din flotă, pe operațiune. Textul `vehicle_registration` rămâne ce se tipărește;
-- ștergerea fișei nu atinge operațiunea.
ALTER TABLE weighing_operations
    ADD COLUMN vehicle_id UUID REFERENCES vehicles (id) ON DELETE SET NULL;
