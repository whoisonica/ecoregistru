-- FELIA G6 — declaraţia anuală (centralizatorul). Cele două rubrici de antet pe care modelul le
-- cere şi pe care nu le aveam nicăieri.
--
-- Sursa e foaia „raportare deseuri generate" din fişierele primite de la specialistă. Şablonul gol
-- (`RAPORTARE DESEURI GENERATE.xlsx`) îşi scrie antetul pe unsprezece rânduri, iar şase dintre cele
-- nouă fişiere completate îl reproduc identic:
--
--   Denumirea operatorului economic: … | Judet si localitate: … | Adresa: … | Tel/fax/e-mail: …
--   CUI: … | Autorizatie de mediu/nr inregistrare/data/valabilitate: … | COD CAEN 4677
--   Anul pentru care se realizeaza raportarea: … | punct de lucru: … | Unitatea de masura: kg
--
-- şi se termină cu patru rânduri de semnătură:
--
--   Intocmit: Andreea Oprea | Functia: Manager Mediu | Telefon: … | Email: …
--
-- Tot ce e acolo aveam deja pe `companies` sau pe `work_points`, în afară de două lucruri:
--
-- 1. `caen_code` — „COD CAEN 4677". Nu se poate deriva din nimic din ce ţinem: CUI-ul nu-l conţine,
--    iar tipul de cont (generator / colector) e o clasificare a noastră, nu a INS. Neştiut, rubrica
--    rămâne goală pe hârtie — nu se pune o cifră ghicită pe un formular depus la APM.
--
-- 2. `contact_role` — „Functia:". `contact_name`, `contact_phone` şi `contact_email` există din V1
--    şi acoperă restul rândurilor de semnătură; funcţia lipsea. În corpus e „Manager Mediu" şi
--    „Area Manager", deci text liber, nu un nomenclator.
--
-- Aditiv şi nullable, ca tot antetul: un cont existent nu le are completate, iar a bloca declaraţia
-- pe ele ar opri un client care poate depune şi fără (rubrica se completează de mână, cum se face
-- şi azi în Excel).

ALTER TABLE companies ADD COLUMN caen_code    VARCHAR(10);
ALTER TABLE companies ADD COLUMN contact_role VARCHAR(120);

COMMENT ON COLUMN companies.caen_code IS
    'Codul CAEN al activităţii, tipărit în antetul declaraţiei anuale (foaia „raportare deseuri generate"). Gol = nedeclarat, rubrica rămâne goală.';
COMMENT ON COLUMN companies.contact_role IS
    'Funcţia persoanei care întocmeşte declaraţia anuală („Functia:" din blocul de semnătură). Text liber — în corpus: „Manager Mediu", „Area Manager".';
