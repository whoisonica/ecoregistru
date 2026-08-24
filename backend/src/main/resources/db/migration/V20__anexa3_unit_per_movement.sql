-- Unitatea de pe Anexa 3, aleasă la introducerea mişcării.
--
-- `V19` a pus alegerea pe firmă, ca setare făcută o dată. Răspunsul specialistei din 24.08.2026 la
-- întrebarea A3.4 o coboară un nivel: „da, e bine să poată selecta la introducerea mişcării".
--
-- Motivul e practic. Formularul pleacă cu marfa, iar destinatarul nu e mereu acelaşi: un colector
-- care lucrează în tone şi un altul care scrie kilograme primesc de la aceeaşi firmă hârtii care
-- trebuie să le semene cu ale lor. Setarea de firmă rămâne — e ce se întâmplă când nimeni nu alege
-- nimic —, dar acum poate fi întoarsă pentru un transport anume.
--
-- Ordinea în care se citeşte unitatea tipărită, de la cea mai specifică:
--
--   1. waste_movements.anexa3_unit   — alegerea pentru transportul ăsta
--   2. companies.anexa3_unit         — alegerea firmei (`V19`)
--   3. waste_movements.unit          — cum a fost înregistrată cantitatea (comportamentul iniţial)
--
-- Aditivă şi nullable, deci **nicio mişcare existentă nu-şi schimbă hârtia**: NULL înseamnă în
-- continuare „ca la firmă, iar dacă nici acolo nu s-a ales, ca în mişcare". Conversia rămâne exactă,
-- prin mutarea virgulei — cifra şi unitatea de pe formular trebuie să fie mereu de acord.

ALTER TABLE waste_movements ADD COLUMN anexa3_unit VARCHAR(10);

COMMENT ON COLUMN waste_movements.anexa3_unit IS
    'Unitatea tipărită pe Anexa 3 pentru transportul ăsta: KG, TONS, sau NULL = alegerea firmei, '
    'iar în lipsa ei unitatea în care e înregistrată mişcarea.';
