-- „Birouri" şi „Producţie" şi pentru punctele de lucru care existau deja.
--
-- `WorkPointService` le creează de pe 25.08.2026 la orice punct de lucru nou, dar cele existente au
-- rămas fără nicio secţie — inclusiv contul specialistei, unde coloana „Secţia" din capitolul 2
-- ieşea goală tocmai fiindcă n-avea ce alege nimeni. Migrarea asta închide golul pentru ele.
--
-- Sunt **valori de pornire, nu o listă închisă**: se redenumesc, se dezactivează, şi se pot adăuga
-- oricâte altele din Setări → Generatori interni („cantină", „atelier", „depozit"). Cererea, în
-- cuvintele ei: „acolo trebuie să scrii tu predefinit, dar şi să poată adăuga altele".
--
-- Numele sunt verbatim ce scrie în coloana „Secţia" din fişele ei completate — „birouri",
-- „productie" — cu diacritice şi majusculă, cum le scrie aplicaţia peste tot.
--
-- Se adaugă **doar** unde nu există deja nicio secţie: un client care şi-a definit propriile secţii
-- nu vrea două în plus peste ele. Idempotentă din aceleaşi motiv — rulată de două ori, a doua oară
-- nu face nimic.

INSERT INTO internal_generators (id, company_id, work_point_id, name, active, created_at)
SELECT gen_random_uuid(), wp.company_id, wp.id, s.name, TRUE, now()
FROM work_points wp
         CROSS JOIN (VALUES ('Birouri'), ('Producţie')) AS s(name)
WHERE NOT EXISTS (
    SELECT 1 FROM internal_generators g WHERE g.work_point_id = wp.id
);
