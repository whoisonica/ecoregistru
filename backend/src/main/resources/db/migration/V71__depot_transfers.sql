-- Depozitul, F2 — transferurile între depozitele firmei (D2.5). Temeiul: `docs/surse-oficiale.md` §4 (runda 2).
--
-- Un transfer e o operațiune de cântar de tip TRANSFER: pleacă din depozitul A (`work_point_id`) spre B
-- (`target_work_point_id`). La plecare liniile ies din A (`waste_movements.operation = TRANSFERRED_OUT`) și
-- marfa e „în tranzit” (`status = IN_TRANSIT`): nu e în stocul niciunui depozit. La recepție, B își cântărește
-- marfa: liniile lui intră (`TRANSFERRED_IN`, pe aceeași operațiune) și transferul e FINALIZED. Pe firmă,
-- transferurile se scad din totaluri; pe depozit se văd.
--
-- Diferența dintre cele două cântăriri se compară cu toleranța celor două cântare (HG 710/2015 anexa 1 tab. 3,
-- dublată în exploatare); peste ea cere NIR (14-3-1A) și decizia comisiei (OMFP 2634/2015). Toleranța și
-- diferența se păstrează așa cum erau la recepție.
--
-- Autorizația de mediu se emite pe amplasament: depozitul își poate ține autorizația lui; fără ea se citește a
-- firmei. Un transfer spre un depozit fără autorizație valabilă se refuză (HG 1061/2008 art. 1 alin. (3)).
--
-- ⚠️ V71: V67 e a aplicației mobile; V68–V70 ale depozitului, tot locale.

ALTER TABLE weighing_operations DROP CONSTRAINT weighing_operations_status;
ALTER TABLE weighing_operations ADD CONSTRAINT weighing_operations_status
    CHECK (status IN ('IN_PROGRESS', 'IN_TRANSIT', 'FINALIZED', 'CANCELLED'));

ALTER TABLE weighing_operations ADD COLUMN target_work_point_id UUID REFERENCES work_points (id);
ALTER TABLE weighing_operations ADD COLUMN dispatched_at TIMESTAMP;
ALTER TABLE weighing_operations ADD COLUMN dispatched_by UUID;
ALTER TABLE weighing_operations ADD COLUMN received_on DATE;
ALTER TABLE weighing_operations ADD COLUMN receipt_scale_id UUID REFERENCES scales (id);
ALTER TABLE weighing_operations ADD COLUMN receipt_scale_state VARCHAR(20);
ALTER TABLE weighing_operations ADD COLUMN receipt_scale_override_reason VARCHAR(500);
ALTER TABLE weighing_operations ADD COLUMN receipt_gross_kg NUMERIC(12,3);
ALTER TABLE weighing_operations ADD COLUMN receipt_tare_kg NUMERIC(12,3);
ALTER TABLE weighing_operations ADD COLUMN tolerance_kg NUMERIC(12,3);
ALTER TABLE weighing_operations ADD COLUMN difference_kg NUMERIC(12,3);
ALTER TABLE weighing_operations ADD COLUMN nir_number VARCHAR(40);
ALTER TABLE weighing_operations ADD COLUMN difference_reason VARCHAR(1000);

-- Destinația e doar a transferului, și nu e chiar depozitul de plecare.
ALTER TABLE weighing_operations ADD CONSTRAINT weighing_operations_transfer_target
    CHECK ((type = 'TRANSFER') = (target_work_point_id IS NOT NULL) AND target_work_point_id IS DISTINCT FROM work_point_id);
-- „În tranzit” există doar la transfer și doar după plecare.
ALTER TABLE weighing_operations ADD CONSTRAINT weighing_operations_in_transit
    CHECK (status <> 'IN_TRANSIT' OR (type = 'TRANSFER' AND dispatched_at IS NOT NULL));
-- O diferență peste toleranță nu se închide fără NIR și decizia comisiei.
ALTER TABLE weighing_operations ADD CONSTRAINT weighing_operations_difference_explained
    CHECK (difference_kg IS NULL OR tolerance_kg IS NULL OR abs(difference_kg) <= tolerance_kg
           OR (nir_number IS NOT NULL AND difference_reason IS NOT NULL));

CREATE INDEX idx_weighing_operations_target ON weighing_operations (target_work_point_id)
    WHERE target_work_point_id IS NOT NULL;

ALTER TABLE work_points ADD COLUMN environmental_auth_number VARCHAR(100);
ALTER TABLE work_points ADD COLUMN environmental_auth_expiry DATE;
