-- Ce intră în Anexa 1 Ambalaje se bifează, nu se deduce din cod.
--
-- Până acum regula era: orice mişcare pe un cod `15 01 xx`, din registrul Anexa 1, intră în
-- declaraţie. Codul decidea singur. Utilizatorul a arătat de ce e prea larg (25.08.2026):
--
--   „Anexa 1 ambalaje este pentru producători şi importatori care pun ambalaje pe piaţă. Trebuie
--    cumva să bifezi un checkbox. [...] Dacă pun 15 01 01 cred că îmi blochează, pentru că se
--    consideră automat ambalaj după cod."
--
-- Şi are dreptate în drept, nu doar ca senzaţie. Declaraţia se cheamă „Producători şi importatori
-- de ambalaje de desfacere, de produse ambalate, supraambalatori" şi raportează **ambalajul pe
-- care firma l-a introdus pe piaţa naţională** — nu orice deşeu de ambalaj care trece prin curte.
-- Un magazin care aruncă cutiile în care i-a venit marfa generează deşeu pe `15 01 01`, dar
-- ambalajul acela l-a pus pe piaţă furnizorul lui. Aceleaşi kilograme, două documente diferite:
-- fişa de evidenţă a gestiunii — mereu — şi Anexa 1 Ambalaje — numai dacă el le-a introdus.
--
-- Deci decizia se mută la om, pe mişcarea în care el ştie răspunsul.
--
-- `NULL` înseamnă **mişcare de dinaintea întrebării**, şi se poartă ca înainte: intră în declaraţie,
-- ca să nu se schimbe de la sine o cifră deja tipărită. Tabul o arată ca „din cod, neconfirmat", ca
-- diferenţa să se vadă şi să poată fi confirmată. `FALSE` e un răspuns, nu o lipsă: cantitatea
-- rămâne în evidenţa gestiunii şi stă în afara declaraţiei.
--
-- ⚠️ Regula e trimisă specialistei şi aşteaptă confirmare (întrebarea AC): „Bazat pe ce regulă să
-- se afişeze în tabul Ambalaje şi în «Anexa 1 Ambalaje» mişcările care se includ în anexă? Să fac
-- un checkbox «Deşeuri de ambalaj puse pe piaţa naţională» pentru codurile de ambalaj?"

ALTER TABLE waste_movements ADD COLUMN packaging_on_market BOOLEAN;

COMMENT ON COLUMN waste_movements.packaging_on_market IS
    'Bifa „Ambalaj pus de noi pe piaţa naţională": decide dacă mişcarea intră în Anexa 1 Ambalaje '
    '(Ordinul 794/2012). Se întreabă doar pe coduri 15 01 xx. TRUE = intră; FALSE = nu intră, dar '
    'rămâne în evidenţa gestiunii deşeurilor; NULL = mişcare de dinaintea întrebării, se poartă ca '
    'TRUE ca să nu se schimbe o cifră deja tipărită, şi se arată ca neconfirmată.';
