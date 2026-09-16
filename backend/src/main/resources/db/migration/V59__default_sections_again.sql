-- „Birouri" şi „Producţie" pe punctele de lucru care au rămas fără nicio secţie după `V25`.
--
-- `V25` a închis golul o dată, dar aprobarea unei cereri de cont crea primul punct de lucru direct
-- prin repository, ocolind `WorkPointService`, deci conturile deschise aşa porneau iar fără secţii.
-- Proprietarul, 16.09.2026: „generatori interni să fie predefinite birouri şi producţie […] alea 2
-- să apară de începutul contului". Codul le creează acum şi la aprobare; migrarea le pune pe cele
-- rămase în urmă.
--
-- Aceeaşi regulă ca în `V25`: numai unde nu există nicio secţie, activă sau nu, ca un client care
-- şi-a şters sau şi-a redenumit secţiile să nu le primească înapoi. Idempotentă.

INSERT INTO internal_generators (id, company_id, work_point_id, name, active, created_at)
SELECT gen_random_uuid(), wp.company_id, wp.id, s.name, TRUE, now()
FROM work_points wp
         CROSS JOIN (VALUES ('Birouri'), ('Producţie')) AS s(name)
WHERE NOT EXISTS (
    SELECT 1 FROM internal_generators g WHERE g.work_point_id = wp.id
);
