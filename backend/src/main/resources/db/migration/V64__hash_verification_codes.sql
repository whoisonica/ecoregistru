-- Codurile de resetare și de invitație se țin de acum ca amprentă SHA-256, nu în clar
-- (`AuthenticationService.fingerprint`). Coloana rămâne `VARCHAR(64)`: hexul are exact 64 de semne.
--
-- Rândurile vechi poartă codul întreg, care nu se mai poate potrivi cu o amprentă, deci ar fi
-- rămas linkuri moarte în cutiile poștale. Se șterg cele nefolosite: cine are un link în mail
-- cere altul din „Parolă uitată" sau primește invitația retrimisă din Setări → Utilizatori.
--
-- Cele confirmate se șterg la fel: sunt consumate, `isValid()` le refuză oricum, iar singura lor
-- valoare rămasă era tocmai codul în clar pe care migrația asta îl scoate din bază.
DELETE FROM verification_records;
