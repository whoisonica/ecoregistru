-- Cantitatea pe tot anul (11.10.2026, Andreea): o rubrică se poate trece direct pe an, nu doar lună cu lună.
-- Rândul cu month = 0 e anul întreg: cât există, rubrica e completă pe toate cele douăsprezece luni, iar lunile ei
-- rămân în tabel, dar nu mai intră în totaluri (ca la o rubrică debifată). Șters, lunile contează din nou.
ALTER TABLE energy_consumptions
    DROP CONSTRAINT ck_energy_consumption_month,
    ADD CONSTRAINT ck_energy_consumption_month CHECK (month BETWEEN 0 AND 12);
