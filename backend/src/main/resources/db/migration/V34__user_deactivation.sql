-- P1.12 — o firmă îşi administrează utilizatorii. Momentul dezactivării.
--
-- **Ce era greşit.** `enabled` purta **două înţelesuri diferite** pe aceeaşi coloană:
--   * `false` la un cont **abia invitat**, care n-a apăsat încă linkul din mail şi n-are parolă
--     (`AuthenticationService.inviteUser` îi pune una aleatoare, imposibil de ghicit);
--   * `false` la un cont **dezactivat**, care a avut parolă şi ştie s-o folosească.
--
-- Câtă vreme singura operaţie era invitaţia, ambiguitatea nu se vedea. Ecranul de administrare o
-- scoate la iveală de două ori, şi a doua oară face rău:
--   * **lista** n-ar avea ce scrie în dreptul rândului — „În aşteptare" şi „Dezactivat" arată
--     identic în bază, deşi înseamnă lucruri opuse pentru cine se uită la ecran;
--   * **„Reactivează"** pe un invitat i-ar pune `enabled = true` peste parola aleatoare. Contul ar
--     apărea **Activ** în listă pentru totdeauna, fără ca omul să se poată autentifica vreodată, şi
--     fără ca cineva să înţeleagă de ce. Un buton care minte — exact ce spune P0.4 că nu vrem.
--
-- **De ce o dată şi nu un boolean.** Un `deactivated` boolean ar fi răspuns la fel de bine la
-- „care e starea", dar la nimic altceva. Data răspunde şi la „de când", care e prima întrebare pusă
-- când pleacă un angajat şi se caută ce a mai apucat să scrie. Costă acelaşi lucru.
--
-- **Cele trei stări, citite din pereche** (`enabled`, `deactivated_at`):
--   | enabled | deactivated_at | stare        | ce se poate face                          |
--   |---------|----------------|--------------|-------------------------------------------|
--   | true    | NULL           | Activ        | dezactivare, schimbare de rol              |
--   | false   | NULL           | În aşteptare | retrimitere invitaţie, anulare invitaţie   |
--   | false   | NOT NULL       | Dezactivat   | reactivare                                 |
--
-- ⚠️ **„În aşteptare" nu se dezactivează, se anulează** — şi asta e ce face coloana să fie
-- suficientă. Dacă un invitat ar putea ajunge „Dezactivat", reactivarea l-ar face „Activ" peste
-- parola aleatoare pe care nimeni n-a văzut-o: un rând care scrie Activ şi nu se poate autentifica
-- niciodată, adică exact defectul de mai sus, mutat cu un pas mai încolo. Serviciul refuză
-- tranziţia, deci „Dezactivat" se atinge **numai** din „Activ" şi oricine e acolo are parolă.
-- Anularea unei invitaţii şterge rândul de tot — se poate, fiindcă un cont în care nu s-a intrat
-- niciodată nu e `created_by` la nimic.
--
-- Perechea (true, NOT NULL) nu există: reactivarea şterge data. Nu e impusă prin constrângere
-- fiindcă `resetPassword` pune `enabled = true` pe orice cont care foloseşte linkul, iar un CHECK
-- ar transforma un flux care merge azi într-o eroare de bază de date. Serviciul o şterge la
-- reactivare, şi asta e singurul drum prin care un cont dezactivat se întoarce.
--
-- Aditivă, ca toate celelalte: coloana e nullable, deci rândurile existente rămân neatinse şi
-- toate se citesc ca „niciodată dezactivat" — ceea ce e adevărat.

ALTER TABLE app_users ADD COLUMN deactivated_at TIMESTAMPTZ;

COMMENT ON COLUMN app_users.deactivated_at IS
    'Momentul dezactivării contului, sau NULL dacă n-a fost dezactivat niciodată. Există ca să '
    'despartă cele două înţelesuri ale lui enabled = false: "invitat, n-a pus încă parola" '
    '(NULL) şi "dezactivat de administrator" (NOT NULL). Reactivarea o pune înapoi pe NULL.';
