-- Viza anuală a autorizaţiei de mediu a partenerului — răspunsul specialistei la AH, 14.09.2026:
-- „am viza din locul datei, de la data autorizării de mediu e valabil 1 an".
--
-- **Ce spune actul** (verificat pe forma consolidată, docs/surse-oficiale.md §2.6-bis):
--   * OUG 195/2005 art. 16 alin. (2^1): autorizaţia de mediu îşi păstrează valabilitatea **pe toată
--     perioada în care beneficiarul obţine viza anuală** — deci nu mai are, de regulă, o dată de
--     expirare;
--   * Procedura aprobată prin Ordinul 1150/2020, art. 5 alin. (4): anul de viză se socoteşte de la
--     **ziua şi luna emiterii autorizaţiei iniţiale**, chiar dacă a fost revizuită;
--   * anexa nr. 4 la procedură: decizia de viză are **număr, dată** şi „se aplică viza pentru
--     **perioada** ...". Perioada o scrie agenţia, deci se tastează de pe hârtie;
--   * Legea 219/2019 art. II alin. (3): o autorizaţie de dinainte de 18.11.2019 nemodificată îşi
--     păstrează data de expirare. De aceea `authorization_expiry` rămâne.
--
-- **Care dată decide.** Cea mai apropiată dintre expirare şi sfârşitul perioadei vizei
-- (`Partner.authorizationValidUntil()`). Ea aprinde avertismentul de la predare, alerta de 60 de zile
-- şi rubrica „Data la care expiră autorizaţia de mediu" de pe Anexa 3.
--
-- **Ce nu face.** Lipsa vizei nu e o constatare: fără dată tastată, nu se afirmă nimic. Iar sfârşitul
-- perioadei nu anulează autorizaţia de drept — art. 16 alin. (2^6) trimite la suspendare şi apoi la
-- anulare, fiecare printr-un act al agenţiei. Pe ecran e avertisment, ca până acum.
--
-- Aditivă: patru coloane, toate nule pe rândurile existente.

ALTER TABLE partners ADD COLUMN authorization_issue_date DATE;
ALTER TABLE partners ADD COLUMN visa_decision_number    VARCHAR(100);
ALTER TABLE partners ADD COLUMN visa_decision_date      DATE;
ALTER TABLE partners ADD COLUMN visa_valid_until        DATE;

COMMENT ON COLUMN partners.authorization_issue_date IS
    'Ziua emiterii autorizaţiei de mediu iniţiale. Ancora anului de viză (Ordinul 1150/2020, art. 5 alin. (4)).';
COMMENT ON COLUMN partners.visa_decision_number IS
    'Numărul deciziei de aplicare a vizei anuale (anexa nr. 4 la procedura din Ordinul 1150/2020).';
COMMENT ON COLUMN partners.visa_decision_date IS
    'Data deciziei de aplicare a vizei anuale.';
COMMENT ON COLUMN partners.visa_valid_until IS
    'Ultima zi a perioadei pentru care s-a aplicat viza, cum scrie pe decizie. NULL = netastată, nu expirată.';
