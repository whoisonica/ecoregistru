-- Obligaţia la Fondul pentru mediu devine un set de contribuţii, fiecare cu ritmul ei.
--
-- Până acum: `companies.afm_obligation`, un boolean, din care generam **un termen lunar pe 25**
-- pentru oricine îl avea pornit. OUG 196/2005 art. 11 are însă trei cadenţe, deci o firmă care
-- datorează doar contribuţia anuală pe ambalaje primea de la noi **11 alerte greşite pe an**. Era
-- cel mai vechi output greşit din aplicaţie.
--
-- Ce a deblocat felia: răspunsul specialistei din 24.08.2026 la întrebarea L — „2% pe orice deşeu,
-- păstrăm alerta. De obicei plăteşte colectorul şi se reflectă în factură." Deci cei 2% (art. 9
-- alin. (1) lit. a) **nu ţin de ambalaje**: se datorează la orice vânzare de deşeu şi îi reţine la
-- sursă colectorul. Contribuţia pe ambalaje (lit. d) rămâne anuală, pe 25 ianuarie.
--
-- ---------- Ce se completează singur, şi ce nu ----------
--
-- Regula casei: nu se ghiceşte. Se completează doar ce se poate **deriva** din ce ştim deja:
--
--   * o firmă care ţine registrul art. 48 (colector sau amândouă) şi avea obligaţia pornită →
--     `WITHHOLDING_2_PERCENT`, fiindcă ea e cea care reţine cei 2% la sursă;
--   * o firmă care pune produse ambalate pe piaţă (producător sau importator) şi avea obligaţia
--     pornită → `PACKAGING`.
--
-- Restul rămân cu setul **gol**, şi tocmai de asta `afm_obligation` NU se şterge: cât timp setul e
-- gol, generatorul de termene se poartă exact ca înainte, cu termenul lunar zgomotos. A stinge o
-- alertă pe o presupunere e mai rău decât a lăsa una prea gălăgioasă — acelaşi tratament ca la
-- rolul de partener din `V7` şi la codul R/D din `V5`. Pe măsură ce conturile sunt completate,
-- calea veche se stinge singură.
--
-- `CIRCULAR_ECONOMY` nu se completează nicăieri automat: e a depozitelor, iar profilul de groapă
-- nu există încă (şi îi lipseşte oricum cuantumul din anexa nr. 2).

CREATE TABLE company_afm_contributions (
    company_id   UUID        NOT NULL REFERENCES companies (id) ON DELETE CASCADE,
    contribution VARCHAR(40) NOT NULL,
    PRIMARY KEY (company_id, contribution)
);

COMMENT ON TABLE company_afm_contributions IS
    'Contribuţiile la Fondul pentru mediu pe care le datorează firma, fiecare cu cadenţa ei '
    '(OUG 196/2005 art. 11). Set gol = nu s-a răspuns, iar termenele rămân pe calea veche.';

-- Colectorii reţin cei 2% la sursă (art. 9 alin. (1) lit. a), deci pentru ei termenul lunar era
-- corect şi rămâne — dar de-acum numit după contribuţia care îl produce.
INSERT INTO company_afm_contributions (company_id, contribution)
SELECT id, 'WITHHOLDING_2_PERCENT'
FROM companies
WHERE afm_obligation = TRUE
  AND type IN ('COLLECTOR', 'BOTH');

-- Cine pune produse ambalate pe piaţă datorează contribuţia pe ambalaje (lit. d), anual.
INSERT INTO company_afm_contributions (company_id, contribution)
SELECT DISTINCT c.id, 'PACKAGING'
FROM companies c
         JOIN company_market_roles r ON r.company_id = c.id
WHERE c.afm_obligation = TRUE
  AND r.market_role IN ('PRODUCER', 'IMPORTER');

COMMENT ON COLUMN companies.afm_obligation IS
    'Moştenit: „datorează ceva la AFM, nu se ştie ce". Cât timp setul de contribuţii e gol, el e '
    'cel care produce termenul lunar de dinainte. Se scoate când nu mai are niciun cont pe el.';
