-- Transportatorul se configurează pe partener, iar şoferii se ţin, nu se rescriu.
--
-- ---------- 1. Bifa „Transportator" pe partener ----------
--
-- `V10` pusese transportatorul pe mişcare (`transport_partner_id`) şi avea dreptate: cine a dus
-- marfa e un fapt despre transportul acela, nu o proprietate a firmei. Javadoc-ul lui `PartnerType`
-- scrie şi azi „There is deliberately no CARRIER", şi rămâne adevărat.
--
-- Ce lipsea era altceva: nu puteai **marca** un partener ca fiind în stare să transporte. Select-ul
-- „Transportator" din formularul de mişcare lista toţi partenerii activi, nefiltrat, iar cele două
-- câmpuri de licenţă se cereau tuturor. Cerut pe 02.09.2026: „vreau la parteneri să poţi configura
-- şi transportator [...] ideea e că uneori firma care colectează şi transportă, alteori nu, poate
-- să transporte o firmă de transport mai mare."
--
-- Deci e **o bifă, nu un tip**, exact ca rolul comercial din `V7`: aceeaşi firmă e şi colector, şi
-- transportator, iar un enum exclusiv ar fi obligat-o să existe de două ori — problema pe care a
-- rezolvat-o `V23` la punctele de lucru.
--
-- Backfill deliberat: primesc bifa cei care **erau deja folosiţi** ca transportatori (apar pe o
-- mişcare) şi cei cărora li s-a completat licenţa de transport. Amândouă sunt răspunsuri deja date
-- de om, nu ghiceli — restul rămân nebifaţi.

ALTER TABLE partners ADD COLUMN is_carrier BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN partners.is_carrier IS
    'Partenerul poate face transportul. Bifă, nu tip: acelaşi partener e des şi colector, şi '
    'transportator. De ea atârnă grupa „Transportatori" din select-ul de pe mişcare şi cele două '
    'câmpuri de licenţă de pe Anexa 3.';

UPDATE partners p SET is_carrier = TRUE
WHERE EXISTS (SELECT 1 FROM waste_movements m WHERE m.transport_partner_id = p.id)
   OR (p.transport_license_number IS NOT NULL AND btrim(p.transport_license_number) <> '');

-- ---------- 2. O firmă de transport pură n-are tip de deşeu ----------
--
-- `type` era NOT NULL, iar valorile sunt GENERATOR / COLLECTOR / RECOVERER: ce face partenerul cu
-- deşeul. O firmă de transport mare nu face nimic cu el — îl mută. A o trece „Colector" ar fi o
-- cifră ghicită pe o rubrică oficială (Anexa 3 nu întreabă asta, dar dosarul de control tipăreşte
-- coloana „Tip"), şi ar prebifa greşit caseta „Destinat:" de pe Anexa 3, care se ia chiar din tip.
--
-- Deci `type` devine nullable, cu înţelesul „doar transportator". E a doua relaxare de migrare din
-- proiect, după `quantity` (`V10`), şi urmează aceeaşi regulă: se relaxează doar când alternativa e
-- să ghicim. Constrângerea rămâne, mutată în serviciu: **ori tip, ori bifa de transportator**.
--
-- Niciun partener existent nu se schimbă: toţi au deja un tip.

ALTER TABLE partners ALTER COLUMN type DROP NOT NULL;

COMMENT ON COLUMN partners.type IS
    'Ce face partenerul cu deşeul: GENERATOR / COLLECTOR / RECOVERER. NULL = firmă de transport '
    'pură, care nu face nimic cu deşeul — permis numai împreună cu is_carrier. Serviciul refuză '
    'un partener fără niciunul din cele două.';

-- ---------- 3. Şoferii ----------
--
-- Anexa 3 la HG 1061/2008 cere „Date de identificare delegat şi nr. de înmatriculare mijloc de
-- transport". Până acum cele trei rubrici se scriau de mână la fiecare mişcare, deşi aceiaşi
-- doi-trei oameni vin cu aceleaşi maşini luni de zile. Cerut în aceeaşi zi: „să poţi cumva să
-- configurezi şi şoferii de acolo sau să scrii free text".
--
-- `partner_id` e **nullable**, şi asta e miezul tabelului:
--   * completat  → şoferii transportatorului, editaţi în fişa partenerului, ca punctele de lucru;
--   * NULL       → şoferii **noştri**, adică exact cazul „— transportăm noi —" din select, care
--                  altfel rămânea pe free text pe veci. Se administrează în Setări.
--
-- Ce se **salvează pe mişcare** rămân tot cele trei coloane text din `V10`, nu o cheie străină:
-- alegerea unui şofer precompletează câmpurile, atât. Motivul e că formularul tipăreşte un
-- instantaneu — actul de identitate de la data aia, maşina de la data aia — iar o mişcare veche
-- trebuie să tipărească mâine exact ce tipărea ieri, chiar dacă omul şi-a schimbat între timp
-- buletinul sau a plecat de la firmă. Free textul rămâne deci prima clasă, nu o portiţă.

CREATE TABLE drivers (
    id                   UUID         PRIMARY KEY,
    company_id           UUID         NOT NULL REFERENCES companies (id) ON DELETE CASCADE,
    partner_id           UUID         REFERENCES partners (id) ON DELETE CASCADE,
    name                 VARCHAR(255) NOT NULL,
    identification       VARCHAR(100),
    vehicle_registration VARCHAR(50),
    active               BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMP    NOT NULL
);

CREATE INDEX idx_drivers_company ON drivers (company_id);
CREATE INDEX idx_drivers_partner ON drivers (partner_id);

COMMENT ON TABLE drivers IS
    'Delegaţii care pot apărea pe Anexa 3. partner_id completat = şoferii transportatorului; '
    'partner_id NULL = şoferii firmei, pentru cazul „transportăm noi". Alegerea unui şofer '
    'precompletează cele trei rubrici de pe mişcare; mişcarea păstrează textul, nu legătura.';

COMMENT ON COLUMN drivers.identification IS
    'Cum se identifică pe hârtie: serie şi număr de CI, sau CNP. Un singur câmp liber, fiindcă '
    'formularul are o singură rubrică şi practica scrie când una, când alta.';

COMMENT ON COLUMN drivers.vehicle_registration IS
    'Maşina cu care vine de obicei. „De obicei", nu fix: pe mişcare rămâne editabilă.';
