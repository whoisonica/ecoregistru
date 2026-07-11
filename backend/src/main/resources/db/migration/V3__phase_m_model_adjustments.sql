-- FAZA M: model adjustments from the legislative analysis (docs/legislatie.md).
-- Data-model only; no official reporting formats. Cheap now, expensive retroactively.

-- M1: physical state + R/D operation code, captured per movement for Anexa 1.
ALTER TABLE waste_movements ADD COLUMN physical_state VARCHAR(20);
ALTER TABLE waste_movements ADD COLUMN operation_code VARCHAR(10);

-- M2: AFM (Fondul pentru Mediu) is NOT universal — flag it per company so the monthly
-- deadline is only ever auto-generated for companies that actually owe it.
ALTER TABLE companies ADD COLUMN afm_obligation BOOLEAN NOT NULL DEFAULT FALSE;

-- M3: cumulative closing stock per monthly evidence line (Anexa 1, Cap. 1).
ALTER TABLE monthly_evidences ADD COLUMN closing_stock NUMERIC(16,3) NOT NULL DEFAULT 0;
