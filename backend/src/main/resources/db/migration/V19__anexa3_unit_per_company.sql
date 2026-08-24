-- Unitatea în care i se tipăresc firmei Anexele 3.
--
-- Actul (HG 1061/2008, anexa nr. 3) are la „Cantitate" rubricile **tone** şi **mc**, iar două din
-- cele trei modele completate primite îi dau dreptate — inclusiv cel ştampilat de la Hamburger
-- Recycling, unde 76 de kilograme sunt scrise `0,076`. Al treilea model (`ANEXA 3 model_CARTON.docx`)
-- scrie KG şi pare adaptat local, exact ca fişierul de ambalaje în tone despre care s-a lămurit deja
-- că nu e forma oficială.
--
-- Nu tranşăm noi în locul clientului. Aplicaţia ţine evidenţa în kilograme (practica fişei Anexa 1),
-- dar formularul care pleacă la transportator e al firmei, iar unele lucrează în tone. Deci fiecare
-- firmă alege, o dată:
--
--   NULL  = aşa cum e înregistrată mişcarea (comportamentul de până acum, deci nimic nu se schimbă
--           pentru conturile existente — regula casei: un profil necompletat nu restrânge şi nu
--           rescrie nimic)
--   'KG'   = tipăreşte mereu kilograme
--   'TONS' = tipăreşte mereu tone
--
-- Când unitatea aleasă diferă de cea înregistrată, cifra se converteşte la tipărire (×1000 sau
-- ÷1000, exact, prin mutarea virgulei). Cifra şi unitatea de pe hârtie sunt întotdeauna de acord —
-- o eroare de 1000× pe un formular oficial e exact ce nu vrem.
--
-- Rămâne deschisă întrebarea A3.4 către specialistă: contează la control că scrie „kg" lângă cifră,
-- dacă unitatea e trecută clar? Dacă răspunsul e „trebuie tone", mutăm implicitul, nu modelul.

ALTER TABLE companies ADD COLUMN anexa3_unit VARCHAR(10);

COMMENT ON COLUMN companies.anexa3_unit IS
    'Unitatea tipărită pe Anexa 3: KG, TONS, sau NULL = unitatea în care e înregistrată mişcarea.';
