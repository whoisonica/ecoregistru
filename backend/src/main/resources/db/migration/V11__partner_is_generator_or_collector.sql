-- ETAPA G3b — un partener e generator sau colector, atât.
--
-- Decizia de la meeting: "transportatorul trebuie eliminat, este doar colector/generator (din
-- parteneri)". Are logica ei — transportatorul nu e o categorie de partener, ci o rubrică a unui
-- transport anume, iar `V10` tocmai a pus-o acolo unde îi e locul: pe mişcare
-- (`transport_partner_id`), lângă şofer şi numărul de înmatriculare, aşa cum o cere Anexa 3.
--
-- Ce rămâne pe partener e ce ESTE partenerul faţă de deşeu: îl produce (generator) sau îl preia
-- (colector). Direcţia banilor e o axă separată, pe care V7 a pus-o deja: client / furnizor.

-- ---------- 1. Vechile valori se pliază pe colector ----------

-- `CARRIER` şi `BOTH` descriau amândouă un operator care mişcă sau preia deşeu — niciunul nu era
-- un generator. Un partener-generator e o noţiune nouă, pe care nimeni n-avea cum s-o fi
-- înregistrat până acum, deci nu se pierde nimic: tot ce exista devine `COLLECTOR`.
UPDATE partners SET type = 'COLLECTOR' WHERE type IN ('CARRIER', 'BOTH');

-- ---------- 2. Adresele şi autorizaţia pe care le cere Anexa 3 ----------

-- `address` (sediul social) a venit cu V10. Formularul cere însă şi punctul de lucru unde se face
-- efectiv descărcarea — pe modelul completat, destinatarul e scris cu "P.L. ILFOV, Şos. de Centura
-- nr. 2-8, Bragadiru", nu cu sediul. Sunt două adrese, deci două coloane.
ALTER TABLE partners ADD COLUMN work_point_address VARCHAR(500);

-- Numărul şi expirarea autorizaţiei de mediu existau deja pe partener (V1): formularul le
-- tipăreşte pe amândouă, sub "Autorizaţie de mediu nr." şi "Data la care expiră autorizaţia".
