-- Modulul de depozit, D1.13: documentele de transport se tipăresc pe operațiune, nu pe linie (16.09.2026).
--
-- O ieșire cu douăsprezece sortimente e un singur camion, deci un singur formular (HG 1061/2008 art. 20
-- alin. (4): „fiecare transport ... însoţit de un formular”). Seria și numărul Anexei 3 stau pe capul
-- operațiunii, alocate la prima tipărire și păstrate, ca la mișcare (V10). Liniile nu le pot ține:
-- cât operațiunea e în lucru, salvarea formularului le șterge și le scrie din nou.
--
-- Numărul vine din aceeași serie ca al mișcărilor (maximul din amândouă tabelele + 1, sub lacăt
-- consultativ), deci unicitatea pe firmă o păzește serviciul; indexul de aici e plasa pe tabelul lui.
ALTER TABLE weighing_operations ADD COLUMN anexa3_series VARCHAR(20);
ALTER TABLE weighing_operations ADD COLUMN anexa3_number INTEGER;

CREATE UNIQUE INDEX uq_weighing_operations_anexa3_number
    ON weighing_operations (company_id, anexa3_number)
    WHERE anexa3_number IS NOT NULL;
