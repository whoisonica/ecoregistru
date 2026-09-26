-- Depozitul, F2 — registrul formularelor primite (D2.6). HG 1061/2008 art. 20 alin. (1), (5): destinatarul trece
-- fiecare formular de transport primit „într-un registru securizat, înseriat și numerotat”; amenda e la art. 25
-- alin. (2) lit. b). „Securizat” nu e definit nicăieri (`surse-oficiale.md` §4, runda 2) → jurnal append-only pe
-- depozit: un rând scris nu se mai schimbă, o greșeală se îndreaptă cu un rând nou care îl numește pe cel greșit.
-- Seria registrului e a depozitului; numărul de ordine crește fără goluri (lacăt consultativ în serviciu).
--
-- ⚠️ V72: V67 e a aplicației mobile; V68–V71 ale depozitului, tot locale.

ALTER TABLE work_points ADD COLUMN received_forms_series VARCHAR(20);

CREATE TABLE received_forms (
    id                     UUID          PRIMARY KEY,
    company_id             UUID          NOT NULL REFERENCES companies (id) ON DELETE CASCADE,
    work_point_id          UUID          NOT NULL REFERENCES work_points (id),
    entry_no               INTEGER       NOT NULL,
    received_on            DATE          NOT NULL,
    -- ANEXA_3 (nepericuloase) sau ANEXA_2 (periculoase).
    form_kind              VARCHAR(16)   NOT NULL,
    form_series            VARCHAR(20),
    form_number            VARCHAR(30)   NOT NULL,
    form_date              DATE,
    sender_name            VARCHAR(255)  NOT NULL,
    sender_cui             VARCHAR(20),
    waste_description      VARCHAR(500),
    quantity_kg            NUMERIC(12,3),
    -- Operațiunea de cântar la care a venit formularul (intrare sau recepția unui transfer), dacă e în aplicație.
    weighing_operation_id  UUID          REFERENCES weighing_operations (id),
    -- Corectura: rândul greșit și de ce. Rândul greșit rămâne, cu numărul lui.
    corrects_id            UUID          REFERENCES received_forms (id),
    correction_reason      VARCHAR(500),
    created_by             UUID          NOT NULL,
    created_at             TIMESTAMP     NOT NULL,
    CONSTRAINT received_forms_kind CHECK (form_kind IN ('ANEXA_3', 'ANEXA_2')),
    CONSTRAINT received_forms_correction CHECK ((corrects_id IS NULL) = (correction_reason IS NULL)),
    CONSTRAINT received_forms_quantity CHECK (quantity_kg IS NULL OR quantity_kg > 0)
);

CREATE UNIQUE INDEX uq_received_forms_entry ON received_forms (work_point_id, entry_no);
CREATE INDEX idx_received_forms_scope ON received_forms (company_id, work_point_id, received_on);

-- Append-only în bază, nu doar în serviciu: nicio cale (nici o comandă SQL scrisă de mână) nu rescrie un rând.
-- Ștergerea rămâne doar pe calea firmei (ON DELETE CASCADE), pe care aplicația n-o oferă.
CREATE FUNCTION received_forms_append_only() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'received_forms e append-only: o corectură se scrie ca rând nou (D2.6)';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER received_forms_no_update BEFORE UPDATE ON received_forms
    FOR EACH ROW EXECUTE FUNCTION received_forms_append_only();
