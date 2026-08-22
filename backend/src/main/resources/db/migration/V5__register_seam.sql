-- ETAPA 2a — the register seam: splits the two legal evidences that today share one table.
--
-- HG 856/2002 art. 2 alin. (1) keeps Anexa 1 to "deşeurile generate în cadrul activităţilor
-- proprii". Art. 2 alin. (2), together with OUG 92/2021 art. 48, sends goods taken over from third
-- parties to a separate chronological register. Verbatim: docs/surse-oficiale.md §1.1 and §2.1.
--
-- Nothing is deleted here. COLLECTED rows stay in waste_movements, flagged out of Anexa 1; the
-- receptions/deliveries tables are created empty and start being written in Etapa 8, so the data
-- moves exactly once, when there are screens to move it into.

-- ---------- 1. Which register a movement belongs to ----------

ALTER TABLE waste_movements ADD COLUMN register VARCHAR(16);

UPDATE waste_movements
SET register = CASE WHEN operation = 'COLLECTED' THEN 'ART_48' ELSE 'ANEXA_1' END;

ALTER TABLE waste_movements ALTER COLUMN register SET NOT NULL;

-- Known limit of this backfill: it can only classify the takeover itself. A HANDED_OVER that
-- passed on previously collected goods looks exactly like handing over own waste, so it lands in
-- ANEXA_1 and has to be reclassified by the user. Nothing here can tell the two apart.

CREATE INDEX idx_movements_register ON waste_movements (company_id, register, date);

-- ---------- 2. Which column a handover lands in ----------

-- No new column here, on purpose. Anexa 1 cap. 1 has no "handed over" column, so a handover is
-- reported as "valorificată" or "eliminată final" — and cap. 3 / cap. 4 report that quantity
-- together with "Operaţia de valorificare"/"de eliminare" and the operator performing it. The
-- existing operation_code (V3) already carries that, and the V/E letter of anexa nr. 1 cap. 2
-- nota 3 follows from its family. Asking for the letter separately would have been strictly less
-- information than the form requires, and a second place for the same fact to go wrong.
--
-- Handovers recorded before the rule existed have no code and cannot be classified retroactively:
-- they stay NULL and have to be completed by the user. Guessing would put a made-up operation on
-- an official form.

-- ---------- 3. Company type becomes a real switch ----------

-- A company that has already recorded takeovers is, in fact, a collector. Widening its type keeps
-- those rows editable under the new rule instead of locking them out. This only ever widens.
UPDATE companies c
SET type = 'BOTH'
WHERE c.type = 'GENERATOR'
  AND EXISTS (SELECT 1 FROM waste_movements m
              WHERE m.company_id = c.id AND m.operation = 'COLLECTED');

-- ---------- 4. The art. 48 register: schema seam, no screens yet ----------

-- Inbound. The primary document of a depot: everything else derives from it — the art. 48
-- register, the 2% AFM contribution withheld at source, the HG 349/2005 art. 15 reception
-- register, the SIATD clock.
CREATE TABLE receptions (
    id                      UUID PRIMARY KEY,
    company_id              UUID          NOT NULL REFERENCES companies (id),
    work_point_id           UUID          NOT NULL REFERENCES work_points (id),
    date                    DATE          NOT NULL,
    waste_code_id           UUID          NOT NULL REFERENCES waste_codes (id),
    quantity                NUMERIC(14,3) NOT NULL,
    unit                    VARCHAR(10)   NOT NULL,
    -- Nullable: a scrap yard also buys from natural persons, and that is a borderou de achiziţie
    -- with a CNP on it (OUG 31/2011) — its own record, its own GDPR regime, Etapa 9.
    supplier_partner_id     UUID          REFERENCES partners (id),
    document_reference      VARCHAR(255),
    unit_price              NUMERIC(14,4),
    total_value             NUMERIC(16,2),
    notes                   VARCHAR(1000),
    client_generated_id     UUID,
    deleted                 BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at              TIMESTAMP,
    deleted_by              UUID,
    created_by              UUID          NOT NULL,
    created_at              TIMESTAMP     NOT NULL,
    updated_at              TIMESTAMP     NOT NULL,
    version                 BIGINT
);
CREATE INDEX idx_receptions_company ON receptions (company_id);
CREATE INDEX idx_receptions_scope ON receptions (company_id, work_point_id, date);
CREATE UNIQUE INDEX uq_receptions_client_id ON receptions (company_id, client_generated_id)
    WHERE client_generated_id IS NOT NULL;

-- Outbound. Goods passed on to the next authorised operator.
CREATE TABLE deliveries (
    id                      UUID PRIMARY KEY,
    company_id              UUID          NOT NULL REFERENCES companies (id),
    work_point_id           UUID          NOT NULL REFERENCES work_points (id),
    date                    DATE          NOT NULL,
    waste_code_id           UUID          NOT NULL REFERENCES waste_codes (id),
    quantity                NUMERIC(14,3) NOT NULL,
    unit                    VARCHAR(10)   NOT NULL,
    recipient_partner_id    UUID          NOT NULL REFERENCES partners (id),
    operation_code          VARCHAR(10),
    document_reference      VARCHAR(255),
    notes                   VARCHAR(1000),
    client_generated_id     UUID,
    deleted                 BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at              TIMESTAMP,
    deleted_by              UUID,
    created_by              UUID          NOT NULL,
    created_at              TIMESTAMP     NOT NULL,
    updated_at              TIMESTAMP     NOT NULL,
    version                 BIGINT
);
CREATE INDEX idx_deliveries_company ON deliveries (company_id);
CREATE INDEX idx_deliveries_scope ON deliveries (company_id, work_point_id, date);
CREATE UNIQUE INDEX uq_deliveries_client_id ON deliveries (company_id, client_generated_id)
    WHERE client_generated_id IS NOT NULL;
