-- ETAPA G1 — the foundation of the generator module (meeting of 23.08.2026).
--
-- Three things the meeting settled, in the order a generator meets them:
--   1. partners split into a commercial role — client (we hand waste over and we invoice them)
--      and supplier (they provide the service and they invoice us). One partner can be both.
--   2. the generator gets a third location level below the work point: the internal generator,
--      which is what HG 856/2002 anexa nr. 1 cap. 2 prints in the "Secţia" column.
--   3. a movement says which internal generator produced the waste.
--
-- Additive throughout: nothing is dropped, no quantity moves.

-- ---------- 1. The commercial role of a partner ----------

-- Two flags rather than one enum, because the meeting was explicit that a partner can be both:
-- you sell them your cardboard and you buy a bin-emptying service from them. An enum would have
-- needed a third BOTH member and a rule to keep it in sync, the way PartnerType already does.
--
-- Note this is orthogonal to partners.type (COLLECTOR / CARRIER / BOTH): that one says what they
-- are authorised to do with waste, this one says which way the invoice travels.
ALTER TABLE partners ADD COLUMN is_client   BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE partners ADD COLUMN is_supplier BOOLEAN NOT NULL DEFAULT FALSE;

-- No backfill, on purpose. Which way the money flows is not derivable from anything we store:
-- the same authorised collector is a client for the cardboard he buys and a supplier for the
-- mixed waste he takes away. Guessing would put a wrong colour on a screen whose whole point is
-- being read at a glance. Existing partners therefore land with no role, the service refuses to
-- save one without a role, and the UI marks them "rol nestabilit" until someone picks — the same
-- treatment the R/D code got in V5 for handovers recorded before it was required.

-- ---------- 2. The internal generator (Anexa 1 cap. 2 "Secţia") ----------

-- The level below the work point: the office, the production hall, the canteen. It is not an
-- address of its own — it sits inside the work point's address — which is why it carries a name
-- and a description and no address columns. In every filled Anexa 1 the specialist supplied
-- (deseuri generate_Cluj/Timisoara/Bragadiru, GESTIUNEA DESEURILOR_Oradea) this column holds the
-- same value on all twelve rows of a sheet: it is a property of the source, not of the month.
CREATE TABLE internal_generators (
    id            UUID PRIMARY KEY,
    company_id    UUID         NOT NULL REFERENCES companies (id),
    work_point_id UUID         NOT NULL REFERENCES work_points (id),
    name          VARCHAR(255) NOT NULL,
    description   VARCHAR(1000),
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL
);
CREATE INDEX idx_internal_generators_company ON internal_generators (company_id);
CREATE INDEX idx_internal_generators_scope ON internal_generators (company_id, work_point_id);

-- Two sections of the same work point cannot share a name — "birouri" has to mean one thing when
-- it is printed in cap. 2.
CREATE UNIQUE INDEX uq_internal_generators_name ON internal_generators (work_point_id, lower(name));

-- ---------- 3. Which internal generator produced the waste ----------

-- Nullable: movements recorded before this slice have no section, and inventing one would print a
-- made-up "Secţia" on an official form. The export leaves the cell empty instead.
ALTER TABLE waste_movements ADD COLUMN internal_generator_id UUID REFERENCES internal_generators (id);

-- ---------- 4. "Predare" stops being an operation ----------
--
-- HG 856/2002 anexa nr. 1 cap. 1 has four quantity columns — generate · valorificată ·
-- eliminată final · rămasă în stoc — and no "predat". Cap. 3 and cap. 4 report a quantity
-- together with the R/D operation performed on it AND "agentul economic care efectuează
-- operaţia". So handing waste to a recycler is a valorificare performed by that partner, and
-- handing it to a landfill an eliminare performed by that partner: the partner column already
-- says it was handed over. A separate HANDED_OVER let the same physical exit be recorded two
-- ways and produced a quantity the form has no column for.
--
-- The conversion follows the R/D code, which V5 already made mandatory for exits, so nothing is
-- guessed: an R code is a valorificare, a D code an eliminare.
UPDATE waste_movements
SET operation = 'RECOVERED'
WHERE operation = 'HANDED_OVER' AND operation_code LIKE 'R%';

UPDATE waste_movements
SET operation = 'DISPOSED'
WHERE operation = 'HANDED_OVER' AND operation_code LIKE 'D%';

-- The rows recorded before V5, with no code at all. They cannot be classified retroactively —
-- inventing an operation would print a made-up figure on an official form — so they get the state
-- V6 already reports them in: the quantity leaves the stock, enters neither official column, and
-- marks its evidence line incomplete until someone completes it. Editing one now requires an R/D
-- code, which is what turns it into a real valorificare or eliminare.
UPDATE waste_movements
SET operation = 'UNCLASSIFIED_OUT'
WHERE operation = 'HANDED_OVER';

-- ---------- 5. The evidence cache is stale again ----------

-- Same contract as V6: monthly_evidences is derived from waste_movements and rebuilt by
-- POST /api/v1/evidences/regenerate. The memo column "din care predat" now means "the part of
-- valorificat + eliminat that a partner performed", which no cached line was computed with.
DELETE FROM monthly_evidences;
