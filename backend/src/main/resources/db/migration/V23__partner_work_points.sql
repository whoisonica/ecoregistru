-- Un partener poate avea mai multe puncte de lucru, iar mişcarea spune la care a ajuns marfa.
--
-- `V11` pusese o singură adresă de punct de lucru pe partener, fiindcă atât cerea Anexa 3: pe
-- modelul completat destinatarul e scris „P.L. ILFOV, Şos. de Centura nr. 2-8, Bragadiru", nu cu
-- sediul social. Adevărul e însă că un colector mare are mai multe depozite, iar acelaşi partener
-- primeşte marfa când într-unul, când în altul — cerut pe 24.08.2026: „să poţi să adaugi mai multe
-- puncte".
--
-- Deci punctul de lucru al destinatarului devine o alegere a **transportului**, nu o proprietate
-- fixă a partenerului. Exact tratamentul pe care l-a primit transportatorul în `V11`: ce ţine de un
-- transport anume stă pe mişcare.
--
-- ---------- Migrarea nu pierde nimic ----------
--
-- Adresa unică existentă devine primul punct de lucru al partenerului, numit „Punct de lucru".
-- Coloana `partners.work_point_address` **rămâne**, dar nu se mai citeşte şi nu se mai scrie: se
-- şterge într-o migrare viitoare, când nu mai are nimeni nevoie de ea ca plasă de siguranţă.
-- Precedentul e `total_collected` (`V18`), scoasă abia după ce a stat nefolosită.
--
-- Mişcările existente rămân cu `partner_work_point_id` NULL, adică „punctul de lucru al
-- partenerului, dacă are unul singur" — deci Anexa 3 tipărită azi pentru o mişcare veche arată
-- exact ce arăta ieri.

CREATE TABLE partner_work_points (
    id          UUID         PRIMARY KEY,
    partner_id  UUID         NOT NULL REFERENCES partners (id) ON DELETE CASCADE,
    name        VARCHAR(255),
    address     VARCHAR(500) NOT NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL
);

CREATE INDEX idx_partner_work_points_partner ON partner_work_points (partner_id);

COMMENT ON TABLE partner_work_points IS
    'Punctele de lucru ale unui partener. Cel ales pe mişcare se tipăreşte pe Anexa 3 la '
    '„Date de identificare destinatar"; în lipsa unei alegeri se foloseşte singurul punct, dacă e '
    'unul singur.';

-- Adresa de dinainte devine primul punct de lucru.
INSERT INTO partner_work_points (id, partner_id, name, address, active, created_at)
SELECT gen_random_uuid(), id, 'Punct de lucru', work_point_address, TRUE, created_at
FROM partners
WHERE work_point_address IS NOT NULL
  AND btrim(work_point_address) <> '';

COMMENT ON COLUMN partners.work_point_address IS
    'Moştenit din V11 şi mutat în partner_work_points de V23. Nu se mai citeşte şi nu se mai '
    'scrie; se şterge într-o migrare viitoare.';

-- La care punct de lucru al destinatarului a ajuns transportul. NULL = nu s-a ales.
ALTER TABLE waste_movements
    ADD COLUMN partner_work_point_id UUID REFERENCES partner_work_points (id);

COMMENT ON COLUMN waste_movements.partner_work_point_id IS
    'Punctul de lucru al destinatarului unde s-a descărcat marfa; se tipăreşte pe Anexa 3.';
