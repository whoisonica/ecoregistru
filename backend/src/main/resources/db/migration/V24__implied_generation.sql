-- Ce a plecat de pe amplasament a fost, în mod necesar, generat acolo.
--
-- Observaţia specialistei, pe fişele scoase din contul ei (25.08.2026): *„cantitatea valorificată
-- să fie şi generată — cum poţi să valorifici ceva ce nu este generat?"*. Are dreptate, şi o spune
-- chiar antetul formularului: capitolul 1 scrie „Cantitatea de deşeuri **Generate**", iar sub el
-- **„din care:** valorificată | eliminată final | rămasă în stoc". Coloanele 2–4 sunt părţi din
-- coloana 1, deci nu pot fi mai mari decât ea.
--
-- Ce făceam până acum: un client care înregistrează doar predarea (cazul obişnuit — magazinul
-- predă cartonul şi nu ţine socoteala generării) primea o fişă cu **Generate 0**, valorificată 300
-- şi **stoc −300**. O fişă cu stoc negativ nu se depune nicăieri şi n-o poate explica nimeni.
--
-- ---------- Regula ----------
--
-- Pentru fiecare lună, în ordine:
--
--   ieşiri    = valorificat + eliminat + ieşiri fără cod R/D
--   acoperire = stoc la începutul lunii + generat înregistrat
--   dedus     = max(0, ieşiri − acoperire)
--
-- „Generate" tipărit = generat înregistrat + dedus, iar stocul nu mai poate ieşi negativ. Când
-- clientul chiar înregistrează generarea, sau când stocul reportat acoperă ieşirea, **dedusul e
-- zero** şi nu se schimbă nimic faţă de înainte.
--
-- Nu e o cifră inventată: e cea deja înregistrată la ieşire, recunoscută şi în coloana din care
-- provine. Partea dedusă se ţine separat, în `implied_generated`, ca să se poată spune oricând cât
-- din „Generate" a fost scris de om şi cât a rezultat din predări.
--
-- `total_generated` îşi schimbă înţelesul — devine „ce se tipăreşte", adică înregistrat + dedus —
-- aşa că tabela-cache se goleşte, ca la `V6`. Se reface din mişcări la prima regenerare, iar
-- documentele o cer oricum acum înainte să tipărească.

ALTER TABLE monthly_evidences
    ADD COLUMN implied_generated NUMERIC(14, 3) NOT NULL DEFAULT 0;

COMMENT ON COLUMN monthly_evidences.implied_generated IS
    'Cât din „Generate" nu a fost înregistrat ca atare, ci rezultă din ieşirile lunii '
    '(HG 856/2002, anexa 1, cap. 1: coloanele 2-4 sunt „din care" din coloana 1).';

COMMENT ON COLUMN monthly_evidences.total_generated IS
    'Ce se tipăreşte la „Generate": cantitatea înregistrată plus partea dedusă din ieşiri '
    '(implied_generated).';

DELETE FROM monthly_evidences;
