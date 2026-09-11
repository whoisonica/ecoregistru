-- G-4 — codurile-oglindă, OUG 92/2021 art. 8 alin. (2).
--
-- > În cazul unui tip de deşeu care se încadrează [...] sub două coduri diferite în funcţie de
-- > posibila prezenţă a unor caracteristici periculoase — codurile marcate cu asterisc —,
-- > încadrarea ca deşeu **nepericulos** se realizează [...] **numai în baza unei analize a
-- > originii, testelor, buletinelor de analiză** şi a altor documente relevante.
--
-- Asta nu e o notă, e o regulă verificabilă: dacă un cod-oglindă e declarat nepericulos fără
-- document justificativ, încadrarea e nelegală — iar aplicaţia ştie amândouă lucrurile. Ce îi
-- lipsea era **perechea**: `waste_codes` ţine `hazardous` ca boolean izolat, fără nicio legătură
-- între cele două jumătăţi ale unei oglinzi.
--
-- Perechea nu trebuie cerută nimănui şi nu se tastează de mână: **numele codului nepericulos îl
-- numeşte pe cel periculos**, în chiar textul Deciziei 2014/955/UE. De aceea coloana se umple
-- aici, o dată, dintr-un singur `UPDATE` — nu dintr-o listă întreţinută cu mâna.
--
-- ⚠️ **Regula e „numele citează un cod periculos", NU o frază anume** — şi diferenţa e măsurată,
-- nu presupusă. Nota care a cerut felia se aştepta la „altele decât cele specificate la", 138 de
-- rânduri. Formularea aia prinde **130** de oglinzi (şapte din cele 138 sunt ele însele periculoase,
-- iar unul — `03 03 11` — citează un cod nepericulos, deci nu e oglindă). Actul mai scrie însă,
-- pentru exact acelaşi lucru, şi „altele decât cele **menţionate** la", şi „alte particule decât",
-- şi „(**cu excepţia** X)", şi „exclusiv praful de cazan specificat la". Regula de aici prinde
-- **161**, cu 31 mai multe, şi printre ele stau `18 01 01` şi `18 02 01` — obiectele ascuţite din
-- cap. 18, adică fix locul unde o încadrare greşită costă cel mai mult. Zero fals-pozitive: toate
-- cele 161 au fost citite una câte una pe 11.09.2026.
--
-- Filtrul „codul citat e periculos" îşi câştigă locul pe un singur rând, şi merită ştiut care:
-- `03 03 11` (nămoluri de la epurarea efluenţilor) citează `03 03 10`, **nepericulos**. Fără
-- filtru ar fi fost declarat oglindă, iar clientul ar fi primit un avertisment pentru o încadrare
-- pe care actul n-o condiţionează de nimic.
--
-- Aditivă: coloana e nullable, niciun rând nu se şterge, iar codurile fără pereche rămân NULL.
-- ⚠️ O viitoare reîncărcare a nomenclatorului din CSV (tiparul lui `V4`) **nu** reface coloana —
-- `ON CONFLICT DO UPDATE` de acolo atinge doar `name` şi `hazardous`. Cine rescrie nomenclatorul
-- rulează şi `UPDATE`-ul de mai jos, altfel o oglindă nouă intră mută.

ALTER TABLE waste_codes ADD COLUMN mirror_of TEXT;

COMMENT ON COLUMN waste_codes.mirror_of IS
    'Codurile periculoase pe care numele acestui cod nepericulos le citeaza, separate prin virgula '
    '- perechea de oglinda de la OUG 92/2021 art. 8 alin. (2). NULL = codul nu e oglinda. Derivata '
    'din numele oficial (Decizia 2014/955/UE), nu tastata: 161 de coduri la 11.09.2026.';

WITH mirror AS (
    SELECT nonhaz.id,
           string_agg(haz.code, ', ' ORDER BY haz.code) AS hazardous_codes
    FROM waste_codes nonhaz
    JOIN waste_codes haz
      ON haz.hazardous
     AND haz.code <> nonhaz.code
     AND nonhaz.name LIKE '%' || haz.code || '%'
    WHERE NOT nonhaz.hazardous
    GROUP BY nonhaz.id
)
UPDATE waste_codes w
SET mirror_of = mirror.hazardous_codes
FROM mirror
WHERE w.id = mirror.id;
