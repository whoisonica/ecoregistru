-- Modulul de depozit, D1.9 și D1.10: reținerile la sursă de pe o intrare (16.09.2026).
--
-- V46 păstra deja sumele reținute (`afm_contribution`, `income_tax`). Se adaugă **bazele**, fiindcă
-- un document financiar arată din ce s-a calculat, nu doar cât a ieșit: borderoul de achiziție
-- tipărește „reținute la sursă din valoarea brută” (OUG 31/2011, anexa), iar declarațiile lunare
-- (D100, AFM) se depun pe bază și pe sumă. Ținând baza lângă sumă, cota aplicată atunci rămâne
-- citibilă și după ce legea o schimbă — nicio declarație veche nu se rescrie singură.
--
-- Cele două se calculează independent, amândouă pe valoarea brută fără TVA (surse-oficiale §18.1):
--   * 2% AFM pe toată valoarea unei intrări, de la persoană fizică sau juridică, pe orice deșeu
--     (OUG 196/2005 art. 9 alin. (1) lit. a), baza la art. 10 alin. (5));
--   * 10% impozit pe venit doar pe liniile de metal ale unei intrări de la o persoană fizică
--     (Codul fiscal art. 114 alin. (2) lit. m^2) și art. 115 alin. (1) lit. a)).

ALTER TABLE weighing_operations ADD COLUMN afm_base NUMERIC(16,2);
ALTER TABLE weighing_operations ADD COLUMN income_tax_base NUMERIC(16,2);

-- O sumă reținută fără baza ei ar fi o cifră fără document.
ALTER TABLE weighing_operations ADD CONSTRAINT weighing_operations_afm_base
    CHECK (afm_contribution IS NULL OR afm_base IS NOT NULL);
ALTER TABLE weighing_operations ADD CONSTRAINT weighing_operations_income_tax_base
    CHECK (income_tax IS NULL OR income_tax_base IS NOT NULL);
