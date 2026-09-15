-- Modulul de depozit, felia 1: intrările și ieșirile, după forma exportului unui depozit funcțional
-- (15.09.2026). Forma lui: o operațiune de cântar numerotată, cu una sau mai multe linii, iar pe
-- fiecare linie brut, tara, neto și cantitatea „finală” acceptată.
--
-- **Liniile rămân în `waste_movements`.** Stocul, registrul art. 48, Anexa 3 și avizul le citesc de
-- acolo, deci nu se rescrie nimic din ele. Operațiunea e doar capul care le grupează. Tabelele
-- `receptions`/`deliveries` din V5 rămân goale și neatinse.
--
-- Deciziile proprietarului (15.09.2026):
--   * `quantity` de pe linie e cantitatea FINALĂ; neto se păstrează alături ca dovadă de cântar;
--   * doar operațiunile FINALIZATE contează în stoc și în registre; anularea nu șterge nimic;
--   * operațiunea poartă doar data, fără oră; ordinea din zi o dă numărul;
--   * prețul se trece pe intrări și pe ieșiri; cine îl vede e o setare a firmei.

-- ---------- 1. Cine vede prețurile ----------

ALTER TABLE companies ADD COLUMN price_visibility VARCHAR(20) NOT NULL DEFAULT 'COMPANY';
ALTER TABLE companies ADD CONSTRAINT companies_price_visibility
    CHECK (price_visibility IN ('COMPANY', 'NO_CONSULTANT', 'ADMIN_ONLY'));

-- ---------- 2. Sortimentele firmei ----------

-- Denumirea comercială legată de un cod LER: „Cupru”, „Alamă” și „Radiator alamă” sunt toate
-- 17 04 01. Pe registre se tipărește codul, iar pe ecran și pe borderou, sortimentul.
--
-- `metal`: deșeu metalic feros sau neferos. De bifă atârnă borderoul cu CNP (OUG 31/2011 art. 1) și
-- impozitul reținut la PF (Codul fiscal art. 114 alin. (2) lit. m²)). Ecranul o propune din cod,
-- omul o confirmă: codul nu hotărăște singur (17 04 07 „metale amestecate” e metal, 16 01 22 nu).
-- `forbidden_from_individuals`: ce OUG 31/2011 art. 1 alin. (1) interzice să fie cumpărat de la PF
-- (șine, capace de canal, cabluri de rețea). Serverul refuză sortimentul la o intrare de la PF.
CREATE TABLE waste_articles (
    id                         UUID PRIMARY KEY,
    company_id                 UUID         NOT NULL REFERENCES companies (id),
    waste_code_id              UUID         NOT NULL REFERENCES waste_codes (id),
    name                       VARCHAR(160) NOT NULL,
    metal                      BOOLEAN      NOT NULL DEFAULT FALSE,
    forbidden_from_individuals BOOLEAN      NOT NULL DEFAULT FALSE,
    active                     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at                 TIMESTAMP    NOT NULL
);
CREATE UNIQUE INDEX uq_waste_articles_name ON waste_articles (company_id, lower(name));

-- ---------- 3. Persoanele fizice de la care depozitul cumpără ----------

-- Borderoul de achiziție (OUG 31/2011, anexa) cere nume, serie și număr de act de identitate, CNP și
-- domiciliu, dar **numai la metale**. La hârtie sau plastic nicio lege în vigoare nu cere CNP-ul,
-- deci acolo se ține doar numele (Legea 190/2018 art. 4 alin. (2)). Obligativitatea e în serviciu,
-- pe operațiune, fiindcă aceeași persoană poate aduce azi carton și mâine cupru.
--
-- CNP-ul se validează cu cifra de control (`@ValidCnp`); CNP-ul și actul de identitate se redactează
-- în jurnalul de audit, ca la șoferi (V42). Termenul de păstrare e al borderoului, document
-- justificativ: 10 ani de la încheierea exercițiului (Legea 82/1991 art. 25), deci se șterg automat
-- după zece ani calendaristici întregi de la ultima operațiune a persoanei.
CREATE TABLE natural_persons (
    id              UUID PRIMARY KEY,
    company_id      UUID         NOT NULL REFERENCES companies (id),
    name            VARCHAR(160) NOT NULL,
    cnp             VARCHAR(13),
    identification  VARCHAR(100),
    address         VARCHAR(500),
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL
);
CREATE INDEX idx_natural_persons_company ON natural_persons (company_id);
CREATE UNIQUE INDEX uq_natural_persons_cnp ON natural_persons (company_id, cnp) WHERE cnp IS NOT NULL;

-- ---------- 4. Operațiunea de cântar ----------

-- `type`: F1 folosește doar IN și OUT. TRANSFER (F2), ADJUSTMENT (F3) și PROCESSING (F5) sunt în
-- constrângere de acum, ca numerotarea și filtrele să nu se schimbe la fiecare felie.
--
-- Vehiculul n-are încă tabel (vine în F2, cu `vehicle_id`); până atunci rămâne textul, ca pe mișcare.
-- Șoferul se poate alege din `drivers`, dar numele se păstrează și ca text: documentul tipărește
-- instantaneul de la data operațiunii (aceeași regulă ca în V28).
CREATE TABLE weighing_operations (
    id                  UUID PRIMARY KEY,
    company_id          UUID          NOT NULL REFERENCES companies (id),
    work_point_id       UUID          NOT NULL REFERENCES work_points (id),
    type                VARCHAR(12)   NOT NULL,
    -- Numerotare separată pe tip, pe firmă, dată la creare și păstrată la anulare.
    number              INTEGER       NOT NULL,
    date                DATE          NOT NULL,
    partner_id          UUID          REFERENCES partners (id),
    natural_person_id   UUID          REFERENCES natural_persons (id),
    -- „Originea” de la art. 48 alin. (1) lit. a), cu valorile din nota 2 a Anexei 3 la Ordinul
    -- 794/2012 (`PackagingOrigin`). La o PF e mereu POPULATIE; la un partener se propune din fișa lui.
    origin              VARCHAR(32),
    driver_id           UUID          REFERENCES drivers (id),
    driver_name         VARCHAR(255),
    vehicle_registration VARCHAR(50),
    order_number        VARCHAR(60),
    -- Brutul și tara întregii operațiuni (mașina plină și goală). Liniile au propriile lor cântăriri.
    gross_kg            NUMERIC(14,3),
    tare_kg             NUMERIC(14,3),

    -- Plata. Numerarul către o PF are plafon de 10.000 lei pe zi (Legea 70/2015 art. 4), iar
    -- borderoul cere chitanța sau viramentul în 3 zile lucrătoare (OUG 31/2011 art. 1 alin. (1^2)).
    payment_method      VARCHAR(10),
    receipt_number      VARCHAR(60),

    -- Reținerile la sursă, calculate la finalizare și păstrate: o cotă schimbată mâine nu rescrie
    -- ce s-a reținut ieri. 2% AFM pe orice intrare cu preț (OUG 196/2005 art. 9 alin. (1) lit. a));
    -- impozitul doar la metalele de la PF, cu cota firmei (Codul fiscal art. 114 alin. (2) lit. m²)).
    afm_contribution    NUMERIC(16,2),
    income_tax          NUMERIC(16,2),

    -- Borderoul de achiziție, doar la intrările de metal de la persoane fizice (OUG 31/2011 art. 1
    -- alin. (1^3): „regim intern de numerotare”). Numărul se dă la prima tipărire și se păstrează.
    borderou_number     INTEGER,
    own_household       BOOLEAN,

    status              VARCHAR(16)   NOT NULL,
    finalized_at        TIMESTAMP,
    finalized_by        UUID,
    cancelled_at        TIMESTAMP,
    cancelled_by        UUID,
    cancel_reason       VARCHAR(1000),

    notes               VARCHAR(1000),
    created_by          UUID          NOT NULL,
    created_at          TIMESTAMP     NOT NULL,
    updated_at          TIMESTAMP     NOT NULL,
    version             BIGINT,

    CONSTRAINT weighing_operations_type
        CHECK (type IN ('IN', 'OUT', 'TRANSFER', 'ADJUSTMENT', 'PROCESSING')),
    CONSTRAINT weighing_operations_status CHECK (status IN ('IN_PROGRESS', 'FINALIZED', 'CANCELLED')),
    CONSTRAINT weighing_operations_origin
        CHECK (origin IS NULL OR origin IN ('POPULATIE', 'GENERATOR_PJ', 'COLECTOR', 'COMERCIANT')),
    CONSTRAINT weighing_operations_payment_method
        CHECK (payment_method IS NULL OR payment_method IN ('VIREMENT', 'NUMERAR')),
    CONSTRAINT weighing_operations_one_counterparty
        CHECK (partner_id IS NULL OR natural_person_id IS NULL),
    -- O persoană fizică doar vinde depozitului; o ieșire merge la un operator autorizat.
    CONSTRAINT weighing_operations_person_only_in
        CHECK (natural_person_id IS NULL OR type = 'IN'),
    CONSTRAINT weighing_operations_person_origin
        CHECK (natural_person_id IS NULL OR origin = 'POPULATIE'),
    CONSTRAINT weighing_operations_cancel_reason
        CHECK (status <> 'CANCELLED' OR (cancel_reason IS NOT NULL AND cancelled_at IS NOT NULL)),
    CONSTRAINT weighing_operations_finalized
        CHECK (status <> 'FINALIZED' OR finalized_at IS NOT NULL)
);
CREATE UNIQUE INDEX uq_weighing_operations_number ON weighing_operations (company_id, type, number);
CREATE UNIQUE INDEX uq_weighing_operations_borderou ON weighing_operations (company_id, borderou_number)
    WHERE borderou_number IS NOT NULL;
CREATE INDEX idx_weighing_operations_scope ON weighing_operations (company_id, date);
CREATE INDEX idx_weighing_operations_person ON weighing_operations (natural_person_id)
    WHERE natural_person_id IS NOT NULL;

-- ---------- 5. Liniile ----------

ALTER TABLE waste_movements ADD COLUMN weighing_operation_id UUID REFERENCES weighing_operations (id);
ALTER TABLE waste_movements ADD COLUMN article_id UUID REFERENCES waste_articles (id);
ALTER TABLE waste_movements ADD COLUMN gross_kg NUMERIC(14,3);
ALTER TABLE waste_movements ADD COLUMN tare_kg NUMERIC(14,3);
ALTER TABLE waste_movements ADD COLUMN net_kg NUMERIC(14,3);
-- Lei/kg, fără TVA (taxare inversă, Codul fiscal art. 331). Valoarea se calculează din cantitatea
-- finală și se păstrează, ca o factură rotunjită altfel să poată fi trecută cum e.
ALTER TABLE waste_movements ADD COLUMN unit_price NUMERIC(14,4);
ALTER TABLE waste_movements ADD COLUMN total_value NUMERIC(16,2);

-- Finalul e neto minus impurități: niciodată mai mult decât s-a cântărit. Doar pe liniile de
-- operațiune, unde `quantity` e în kg; mișcările vechi rămân neatinse.
ALTER TABLE waste_movements ADD CONSTRAINT waste_movements_final_within_net
    CHECK (net_kg IS NULL OR quantity IS NULL OR quantity <= net_kg);
ALTER TABLE waste_movements ADD CONSTRAINT waste_movements_net_positive
    CHECK (net_kg IS NULL OR net_kg > 0);

CREATE INDEX idx_movements_weighing_operation ON waste_movements (weighing_operation_id);
