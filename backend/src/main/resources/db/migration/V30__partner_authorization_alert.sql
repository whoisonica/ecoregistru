-- Alerta de expirare a autorizaţiei de mediu a partenerului, cu 60 de zile înainte.
--
-- Ultimul slice deschis din FAZA TERMENE. `MailType.PARTNER_AUTHORIZATION_EXPIRING` exista în enum
-- din faza aia şi **nu era folosit nicăieri** — felia n-a fost construită niciodată, doar numită.
--
-- **Temeiul.** OUG 92/2021 art. 23 alin. (1): predarea deşeului e legală numai către un operator
-- **autorizat**, iar alin. (2) adaugă că predarea nu descarcă de răspundere. Citatul: §2.1b din
-- docs/surse-oficiale.md.
--
-- **De ce nu e o dublare a deciziei 36.** Cele două rezolvă lucruri diferite, şi de asta stau
-- amândouă în picioare:
--   * decizia 36 (04.09.2026) compară expirarea cu **data mişcării** şi arată un avertisment galben
--     pe ecran — deci **semnalează după**, când predarea s-a întâmplat deja;
--   * asta de aici scrie un mail cu 60 de zile înainte — deci **previne**, cât mai e timp ca
--     partenerul să-şi reînnoiască autorizaţia sau clientul să aleagă altul.
-- Prima e o constatare, a doua e o şansă.
--
-- **Fereastra e numai înainte: [azi, azi+60].** O autorizaţie deja expirată nu intră pe mail, şi
-- asta e deliberat. Este acoperită în alte două locuri — badge-ul din ecranul Parteneri (acelaşi
-- prag de 60 de zile, dar calculat cu `<=`, deci prinde şi trecutul) şi avertismentul de la
-- predare al deciziei 36. Un mail despre o autorizaţie expirată acum doi ani nu previne nimic: ar
-- fi doar zgomot la prima rulare, pe fiecare partener vechi deodată.
--
-- **De ce coloana ţine o DATĂ, nu un boolean.** Cheia de deduplicare e chiar expirarea pentru care
-- am scris ultima oară. Un boolean ar fi cerut ca cineva să-l stingă manual la reînnoirea
-- autorizaţiei — şi nimeni n-ar fi făcut-o, deci al doilea termen ar fi trecut tăcut. Cu o dată,
-- reînnoirea **rearmează singură** alerta: `authorization_warning_sent_for` rămâne la termenul
-- vechi, nu mai e egal cu `authorization_expiry`, şi partenerul redevine candidat. Nicio linie de
-- cod în `PartnerService` nu trebuie să ştie de coloana asta.
--
-- NULL = nu s-a scris niciodată pentru partenerul ăsta. Aditivă, ca toate celelalte.

ALTER TABLE partners ADD COLUMN authorization_warning_sent_for DATE;

COMMENT ON COLUMN partners.authorization_warning_sent_for IS
    'Termenul de expirare pentru care s-a trimis deja alerta de 60 de zile. Egal cu '
    'authorization_expiry = s-a scris; diferit sau NULL = se rescrie. Ţine o dată, nu un boolean, '
    'ca reînnoirea autorizaţiei să rearmeze alerta fără ca nimeni să stingă un flag.';
