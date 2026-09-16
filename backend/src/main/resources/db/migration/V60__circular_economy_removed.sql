-- „Economia circulară (trimestrial, 25)" scoasă din configurarea contului (proprietarul, 16.09.2026).
--
-- Contribuţia de la OUG 196/2005 art. 9 alin. (1) lit. c) e a depozitelor de deşeuri (gropi), nu a
-- clienţilor aplicaţiei. `CompanyService` n-o mai salvează şi `DeadlineService` n-o mai generează;
-- aici pleacă şi de pe firmele care o aveau bifată.
--
-- 1. Un cont care o avea ca SINGUR răspuns rămâne cu setul gol, iar setul gol + `afm_obligation`
--    înseamnă calea veche (12 termene lunare). Firma a răspuns deja la întrebare, deci bifa veche
--    se stinge, ca să nu primească alerte lunare pe care nu le-a cerut.
-- 2. Termenele trimestriale încă nefăcute se şterg: ar fi alerte pentru o obligaţie scoasă. Cele
--    marcate `DONE` rămân — sunt istoricul a ce a depus firma.

UPDATE companies c
SET afm_obligation = FALSE
WHERE EXISTS (SELECT 1 FROM company_afm_contributions a
              WHERE a.company_id = c.id AND a.contribution = 'CIRCULAR_ECONOMY')
  AND NOT EXISTS (SELECT 1 FROM company_afm_contributions a
                  WHERE a.company_id = c.id AND a.contribution <> 'CIRCULAR_ECONOMY');

DELETE FROM company_afm_contributions WHERE contribution = 'CIRCULAR_ECONOMY';

DELETE FROM reporting_deadlines WHERE report_type = 'AFM_QUARTERLY' AND status <> 'DONE';
