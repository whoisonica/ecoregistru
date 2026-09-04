-- Persoana desemnată cu gestiunea deşeurilor (OUG 92/2021, art. 23 alin. (4) şi (5)).
--
-- Punctul 8 al auditului de conformitate din 02.09.2026. Actul, verbatim:
--
--   (4) [...] titularul unei activităţi, pentru care autoritatea competentă [...] a emis o
--       autorizaţie de mediu [...], are obligaţia să **desemneze o persoană** din rândul
--       angajaţilor proprii sau să **delege această obligaţie unei terţe persoane**.
--   (5) Persoanele desemnate [...] trebuie să fie **instruite** [...] ca urmare a absolvirii unor
--       programe de perfecţionare şi specializare recunoscute la nivel naţional [...]
--
-- E printre primele lucruri pe care le cere un inspector, şi aplicaţia nu-l ţinea nicăieri.
-- `contact_name` / `contact_role` există, dar sunt **altceva**: blocul de semnătură al declaraţiei
-- anuale („Întocmit" / „Funcţia"), care se completează cu cine a redactat documentul. Persoana
-- desemnată poate fi altcineva, e desemnată prin decizie internă, şi poartă certificat de instruire
-- — pe care blocul de semnătură nu-l poartă. Deci câmpuri proprii, nu refolosirea celor două.
--
-- **De ce se construieşte fără să aşteptăm răspunsul specialistei (întrebarea AK).** Obligaţia e a
-- legii, nu a practicii: alin. (4) o pune în sarcina oricărui titular de autorizaţie de mediu.
-- Ce rămâne de întrebat e dacă vrea rubrica în dosarul de control şi ce anume cere inspectorul din
-- certificat — iar dacă răspunsul e „nu cere nimeni", costul greşelii e patru coloane nefolosite,
-- nu o felie de aruncat. Toate sunt nullable: un profil necompletat nu se schimbă cu nimic, şi
-- niciun document nu tipăreşte o ghicitură (regula de lucru 1).
--
-- Nota de produs care i se trimite odată cu AK: **consultantul de mediu ESTE „terţa persoană"** de
-- la alin. (4). Pentru portofoliul ei, rubrica se completează cu datele ei, o dată per client.

ALTER TABLE companies ADD COLUMN waste_manager_name VARCHAR(160);
ALTER TABLE companies ADD COLUMN waste_manager_role VARCHAR(120);
ALTER TABLE companies ADD COLUMN waste_manager_external BOOLEAN;
ALTER TABLE companies ADD COLUMN waste_manager_training VARCHAR(255);

COMMENT ON COLUMN companies.waste_manager_name IS
    'Persoana desemnată cu gestiunea deşeurilor (OUG 92/2021 art. 23 alin. (4)). NULL = nedesemnată '
    'sau necompletată; nu se deduce din contact_name, care e blocul de semnătură al declaraţiei.';

COMMENT ON COLUMN companies.waste_manager_role IS
    'Funcţia ei în firmă, sau calitatea în care e delegată („consultant de mediu"). Text liber.';

COMMENT ON COLUMN companies.waste_manager_external IS
    'TRUE = obligaţia e delegată unei terţe persoane (art. 23 alin. (4), a doua variantă), '
    'FALSE = angajat propriu, NULL = nu s-a răspuns.';

COMMENT ON COLUMN companies.waste_manager_training IS
    'Certificatul de instruire cerut de art. 23 alin. (5): program, număr şi dată, ca text liber. '
    'Actul cere ca persoana să fie instruită prin programe recunoscute naţional, dar nu impune '
    'forma dovezii, deci nu se structurează pe ghicit — vezi întrebarea AL/AK.';
