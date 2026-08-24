-- FELIA G6, restanţa ei — cele două rubrici de antet ale declaraţiei anuale se întreabă acum şi în
-- formularul public de cerere de cont, nu doar pe firmă.
--
-- `V15` le-a adăugat pe `companies`, fiindcă acolo le citeşte generatorul de PDF. Dar acolo le
-- completa doar administratorul platformei, din ce afla pe telefon; clientul, care le ştie, n-avea
-- unde să le scrie. Cererea de cont e locul: se cer o dată, la intrare, şi aprobarea rămâne o
-- copiere, nu o traducere — exact tratamentul dat lui `market_roles` în `V13`.
--
--   caen_code    — „COD CAEN 4677" din antetul foii `raportare deseuri generate`
--   contact_role — „Functia:" din blocul de semnătură („Manager Mediu", „Area Manager")
--
-- Ambele opţionale, ca tot ce nu e strict necesar ca să putem răspunde unei cereri: un formular
-- care refuză să plece e un formular pe care nu-l trimite nimeni. Necompletate, rubrica rămâne
-- goală pe hârtie — nu se ghiceşte nimic pe un formular depus la APM.
--
-- ⚠️ Formularea celor două întrebări e de validat cu specialista (întrebarea S din
-- docs/intrebari-specialist.md): un patron de magazin trebuie să răspundă corect din prima, iar
-- la CAEN nu ştim încă dacă se trece codul principal al firmei sau cel al activităţii de pe
-- amplasamentul care generează deşeul. Până la răspuns, eticheta spune ce ştim şi nu presupune
-- nimic în plus.

ALTER TABLE account_requests ADD COLUMN caen_code    VARCHAR(10);
ALTER TABLE account_requests ADD COLUMN contact_role VARCHAR(120);

COMMENT ON COLUMN account_requests.caen_code IS
    'Codul CAEN declarat în cerere; se copiază pe firmă la aprobare şi se tipăreşte în antetul declaraţiei anuale.';
COMMENT ON COLUMN account_requests.contact_role IS
    'Funcţia persoanei de contact; se copiază pe firmă la aprobare şi se tipăreşte la „Întocmit / Funcţia".';
