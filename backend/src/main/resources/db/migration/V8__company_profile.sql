-- ETAPA G2 — the account profile: what this client answered, and therefore what he sees.
--
-- The principle from the meeting of 23.08.2026: the client fills an intake form, support creates
-- the account from it, and from then on the screens offer only what his kind of business needs.
-- A joinery that hands cardboard to a recycler should never scroll past D7 "evacuare în mări".
--
-- Everything here is additive, and an EMPTY profile means NO restriction — the accounts that
-- exist today have not answered the form yet, and narrowing their screens on an empty answer
-- would hide options they are already using.

-- ---------- 1. Which R/D operations this account works with ----------

CREATE TABLE company_operation_codes (
    company_id     UUID        NOT NULL REFERENCES companies (id),
    operation_code VARCHAR(10) NOT NULL,
    PRIMARY KEY (company_id, operation_code)
);

-- ---------- 2. Which waste codes his authorization covers ----------

-- "Cu ce transportă" for a collector, "ce generez" for a generator: the codes on the environmental
-- authorization. Feeds the code picker so the 842-entry nomenclator stops being a haystack.
CREATE TABLE company_waste_codes (
    company_id    UUID NOT NULL REFERENCES companies (id),
    waste_code_id UUID NOT NULL REFERENCES waste_codes (id),
    PRIMARY KEY (company_id, waste_code_id)
);

-- ---------- 3. The transport data a collector is asked for ----------

-- These are also the fields the Anexa 3 handover form prints on the carrier's side: "Licenţa de
-- transport mărfuri" and "Data la care expiră licenţa de transport mărfuri nepericuloase".
-- The environmental authorization itself is already on companies (V1).
ALTER TABLE companies ADD COLUMN transport_means           VARCHAR(500);
ALTER TABLE companies ADD COLUMN transport_license_number  VARCHAR(255);
ALTER TABLE companies ADD COLUMN transport_license_expiry  DATE;

-- ---------- 4. Cap. 2 of Anexa 1: storage and treatment ----------

-- The two closed nomenclators of "2. STOCAREA PROVIZORIE, TRATAREA ŞI TRANSPORTUL DEŞEURILOR",
-- verbatim from notes 1 and 2 of the form. The third column of that chapter, "Scopul", is not
-- stored: it follows from the R/D code (see TreatmentPurpose).
--
-- Nullable: movements recorded before this slice have neither, and the export leaves the cells
-- empty rather than inventing a container type.
ALTER TABLE waste_movements ADD COLUMN storage_type     VARCHAR(10);
ALTER TABLE waste_movements ADD COLUMN treatment_method VARCHAR(10);
