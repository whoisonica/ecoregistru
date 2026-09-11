-- P1.11 — jurnalul de audit: cine, ce, când.
--
-- Până azi întrebarea „cine a modificat cantitatea asta şi când" n-avea răspuns nicăieri în
-- aplicaţie. Singura urmă era `created_by` + `created_at` pe `waste_movements`, adică **autorul
-- primei scrieri** — iar o fişă de evidenţă se depune la autoritate pe baza rândurilor de după
-- toate modificările, nu a celor de la naştere. Pe un produs a cărui promisiune e apărarea la
-- control, lipsa asta e şi igienă, şi argument de vânzare: concurenţa vinde „garanţie amenzi",
-- noi putem vinde trasabilitatea care o face verificabilă. DPA-ul (art. 28, anexa B.6) o anunţa
-- deja ca planificată — de azi anexa se poate scrie la timpul prezent.
--
-- ⚠️ **Ce se scrie aici e ÎNGUST, dinadins.** Nu tot ce atinge baza, ci scrierile care ajung pe
-- hârtie sau care privesc o persoană: mişcări, firma, parteneri, puncte de lucru, şoferi
-- (aici stă elementul de identificare — serie CI **sau CNP**), buletine de analiză, ataşamente,
-- conturi de utilizator, plus regenerarea evidenţei. Evidenţa lunară însăşi NU e auditată rând cu
-- rând: e un **cache recalculabil**, iar o regenerare de an ar scrie mii de rânduri de jurnal
-- despre o singură apăsare de buton. Regenerarea se scrie ca **un** rând, cu anul şi cu câte linii
-- au ieşit.
--
-- ⚠️ **Un rând de jurnal are întotdeauna un om şi o firmă.** Scrierile de sistem — planificatorul
-- de termene, care umblă peste toţi tenanţii fără nicio sesiune — nu produc rânduri aici, fiindcă
-- răspunsul la „cine" ar fi „nimeni", iar un „nimeni" înregistrat de o mie de ori pe zi ar îneca
-- exact rândurile pentru care există tabela.

CREATE TABLE audit_log (
    id           UUID PRIMARY KEY,
    company_id   UUID NOT NULL REFERENCES companies (id),

    -- Numele simplu al clasei („WasteMovement"), nu numele tabelei: jurnalul se citeşte lângă cod.
    entity_type  VARCHAR(60) NOT NULL,
    -- Rândul atins. NULL doar pentru faptele care nu sunt despre un rând anume — regenerarea.
    entity_id    UUID,
    action       VARCHAR(20) NOT NULL,

    -- Cum se numea rândul în clipa faptei, ca text. Scris la momentul scrierii, nu rezolvat la
    -- citire, fiindcă un rând şters nu mai poate fi întrebat cum îl chema — şi tocmai ştergerea
    -- e fapta despre care se întreabă cel mai des.
    label        TEXT,

    -- Câmpurile schimbate, ca JSON: [{"field":"quantity","from":"5.000","to":"7.000"}].
    -- TEXT şi nu JSONB: nu interogăm niciodată *în* el — se citeşte întreg, pe rândul lui — iar
    -- un tip dedicat ar cere o punte de Hibernate pentru zero câştig. Gol la creare şi la
    -- ştergere: „a creat mişcarea" nu are nevoie de lista celor patruzeci de rubrici ale ei.
    changes      TEXT,

    -- Autorul, copiat aici şi nu doar referit. Un cont se dezactivează, se redenumeşte, îşi
    -- schimbă adresa — iar jurnalul trebuie să spună cine a fost atunci, nu cine e acum.
    actor_id     UUID,
    actor_email  VARCHAR(200),
    actor_role   VARCHAR(20),

    occurred_at  TIMESTAMP NOT NULL
);

-- Cum se citeşte tabela: „ce s-a întâmplat la firma asta, cele mai noi întâi" — ecranul din
-- Setări — şi „ce s-a întâmplat cu rândul ăsta", drumul de la o cifră de pe hârtie la autorul ei.
CREATE INDEX idx_audit_log_company_at ON audit_log (company_id, occurred_at DESC);
CREATE INDEX idx_audit_log_entity ON audit_log (company_id, entity_type, entity_id, occurred_at DESC);

COMMENT ON TABLE audit_log IS
    'P1.11 — cine, ce, cand, peste scrierile care ajung pe hartie sau privesc o persoana. '
    'Nu e un jurnal al bazei: evidenta lunara e cache recalculabil si se scrie ca un singur rand '
    'per regenerare, iar scrierile de sistem (planificatorul de termene) nu produc randuri, '
    'fiindca n-au un "cine".';

COMMENT ON COLUMN audit_log.label IS
    'Numele randului in clipa faptei, scris ca text. Un rand sters nu mai poate fi intrebat cum '
    'il chema, iar stergerea e chiar fapta despre care se intreaba cel mai des.';

COMMENT ON COLUMN audit_log.changes IS
    'JSON: [{"field":"...","from":"...","to":"..."}]. Campurile care trimit la alt rand poarta '
    'identificatorul, nu numele — numele se rezolva la citire, cu o singura interogare pe pagina; '
    'a-l rezolva la scriere ar insemna initializarea unui proxy in mijlocul unui flush.';
