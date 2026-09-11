-- G-3 / P3.7 — termenul de 30 aprilie, OUG 92/2021 art. 49 alin. (9).
--
-- Articolul cere o raportare anuală la APM, până la 30 aprilie a anului următor celui raportat, de
-- la DOUĂ categorii: titularii de autorizaţii de construire şi/sau desfiinţare (conformarea cu
-- art. 17 alin. (7), ţinta de 70% la deşeurile din construcţii) şi producătorii/deţinătorii de
-- uleiuri uzate (măsurile de la art. 31 alin. (1)). Sancţiune: art. 62 alin. (1) lit. e),
-- 5.000–10.000 lei pentru persoane juridice. Termenul nu exista deloc în calendarul aplicaţiei —
-- nici în `ReportType`, nici în vreun document — până pe 10.09.2026.
--
-- ⚠️ **Migrarea asta e pentru O SINGURĂ jumătate a semnalului, şi asta e tot conţinutul ei.**
-- Jumătatea de uleiuri NU are coloană: se citeşte din mişcări, prin `util/UsedOilCodes`, fiindcă
-- un cod de ulei uzat în evidenţă *chiar* spune că firma deţine uleiuri uzate. Jumătatea de
-- construcţii nu se poate citi la fel: capitolul 17 are 38 de coduri şi apare la oricine mişcă
-- moloz, pe când obligaţia e a **titularului autorizaţiei de construire**, care e altceva. Un
-- transportator de moloz ar primi o alertă falsă pentru o raportare care nu e a lui — exact
-- zgomotul pe care `V21` a costat o migrare să-l scoată.
--
-- De aceea coloana e **BOOLEAN NULL, cu trei stări**, ca `waste_manager_external`:
--   NULL  = nimeni n-a răspuns  -> NU se generează nimic pentru jumătatea de construcţii;
--   TRUE  = firma e titulară    -> termenul se generează;
--   FALSE = firma nu e titulară -> nu se generează, şi acum se ştie de ce.
-- Regula din `ReportType` rămâne: o alertă e o afirmaţie, deci se face pe semnal pozitiv, nu pe
-- tăcere. NULL şi FALSE se comportă la fel azi, dar înseamnă lucruri diferite, şi diferenţa se
-- vede în ecranul de profil: una e o rubrică necompletată, cealaltă un răspuns.
--
-- Aditivă: coloana e nullable, niciun rând existent nu se atinge, şi toate firmele de până acum se
-- citesc ca „n-a răspuns la întrebarea asta" — ceea ce e adevărat.

ALTER TABLE companies ADD COLUMN construction_permit_holder BOOLEAN;

COMMENT ON COLUMN companies.construction_permit_holder IS
    'Firma e titulara unei autorizatii de construire si/sau desfiintare (OUG 92/2021 art. 49 '
    'alin. (9))? NULL = nu s-a raspuns, TRUE/FALSE = raspuns. Din ea se genereaza jumatatea de '
    'constructii a termenului de 30 aprilie; jumatatea de uleiuri uzate se deriva din miscari si '
    'nu are coloana. Nu se poate deduce din codurile capitolului 17: acelea apar si la cine doar '
    'transporta moloz, iar obligatia e a titularului autorizatiei.';
