-- Anexa 3 la Ordinul 794/2012 — raportarea anuală a colectorilor, comercianţilor, reciclatorilor
-- şi valorificatorilor de deşeuri de ambalaje. Termen: 25 februarie, ca şi anexa 1.
--
-- Modulul de ambalaje tipărea până acum doar **anexa 1** (ce pune firma pe piaţă). Anexa 3 e
-- celălalt capăt al lanţului: ce a **preluat** de la terţi şi ce a făcut cu marfa aia. Structura e
-- citită din act şi din modelul completat primit de la specialistă
-- (`documente oficiale/RAPORTARE DESEURI DE AMBALAJ COLECTATE ANUAL.ods`) — spre deosebire de
-- registrul art. 48, unde n-avem niciun exemplar şi de asta etapa aşteaptă (întrebarea AD).
--
-- Trei coloane, fiecare cu motivul ei.

-- ---------- 1. Ce calitate are firma, şi deci care tabel îl completează ----------

-- Art. 4 alin. (1): cei enumeraţi „sunt obligaţi să raporteze datele prevăzute în anexa nr. 3,
-- **tabelul 1 sau, după caz, tabelul 2**". Tabelul 1 = colectori/comercianţi, tabelul 2 =
-- reciclatori/valorificatori. Alin. (3) adaugă că toţi depun la agenţia din raza punctului de
-- lucru, **în afară de comerciant**, care depune la ANPM — de aceea comerciantul e valoare
-- separată, nu o nuanţă de colector.
--
-- **Se întreabă, nu se deduce.** Scurtătura ar fi să ne uităm dacă firma a înregistrat operaţiuni
-- R3/R4/R5 proprii şi s-o numim reciclator. Ar fi o ghicitură pe un formular oficial (regula de
-- lucru 1) şi, mai rău, una instabilă: un trimestru fără mişcări ar muta firma pe celălalt tabel.
-- O calitate e ce **este** firma, nu ce a apucat să înregistreze luna trecută.
--
-- NULL = nu s-a răspuns, şi atunci **nu se tipăreşte niciun tabel**. Nu contrazice decizia 6
-- („profil gol = fără restricţie"): e decizia 37 aplicată unui document — un ecran e o ofertă, un
-- document e o afirmaţie. Să scriem „Colectori/Comercianţi" în antetul unui formular fără să ni se
-- fi spus că se aplică ar însemna să afirmăm calitatea juridică a firmei în locul ei.
ALTER TABLE companies ADD COLUMN packaging_operator_role VARCHAR(32);

COMMENT ON COLUMN companies.packaging_operator_role IS
    'Calitatea din Ordinul 794/2012 art. 4 alin. (1): COLECTOR / COMERCIANT / RECICLATOR / '
    'VALORIFICATOR. Decide care tabel al anexei 3 se tipăreşte. NULL = nu s-a răspuns, deci nu se '
    'tipăreşte niciunul.';

-- ---------- 2. Provenienţa deşeului de ambalaj preluat ----------

-- Nota 2 a ambelor tabele, verbatim: „Se menţionează, după caz, «populaţie», «generator persoană
-- juridică», «colector», «comerciant», în funcţie de persoanele juridice sau fizice **de la care
-- provin** deşeurile de ambalaje preluate."
--
-- **Patru valori, nu trei.** Modelul `.ods` primit are doar primele trei; e şablon modificat local,
-- exact ca antetul lui în „tone", care contrazice art. 8 alin. (1) lit. a) („se raportează în
-- kilograme"). Unde modelul şi sursa primară se contrazic pe o chestiune de drept, câştigă sursa
-- primară (regula de lucru 2).
--
-- **Stă pe partener**, fiindcă nota descrie **sursa**, nu transportul: un colector de la care
-- cumperi e colector la fiecare transport pe care ţi-l aduce. Răspuns o dată = răspuns definitiv,
-- iar pe mişcare ar fi acelaşi fapt tastat de o sută de ori, cu o sută de ocazii să difere.
ALTER TABLE partners ADD COLUMN packaging_origin VARCHAR(32);

COMMENT ON COLUMN partners.packaging_origin IS
    'Provenienţa din nota 2 a anexei 3 (Ordinul 794/2012): POPULATIE / GENERATOR_PJ / COLECTOR / '
    'COMERCIANT. Proprietate a sursei, deci se răspunde o dată pe partener. NULL = nu s-a răspuns.';

-- **Şi o suprascriere pe mişcare** — care nu e un moft, ci singura cale către o valoare din act.
-- „Populaţia" nu e partener şi n-o să fie niciodată: o persoană fizică n-are CUI, n-are autorizaţie
-- şi n-are ce căuta în registrul de parteneri. Fără coloana asta, rândul „populaţie" al
-- formularului n-ar putea fi completat deloc — iar centrele de colectare cumpără de la populaţie
-- zilnic. Restul valorilor o folosesc doar ca excepţie, când un partener aduce o dată altceva
-- decât aduce de obicei.
ALTER TABLE waste_movements ADD COLUMN packaging_origin VARCHAR(32);

COMMENT ON COLUMN waste_movements.packaging_origin IS
    'Suprascrie provenienţa partenerului pentru mişcarea asta, şi e singurul loc unde se poate '
    'spune POPULATIE — o persoană fizică nu e partener. NULL = se ia de pe partener.';

-- **Niciun index nou.** Prima variantă a migrării adăuga unul parţial pe
-- `(company_id, packaging_origin)`, cu justificarea că restrânge citirea la codurile de ambalaj —
-- ceea ce e fals de două ori: indexul nu conţine codul, iar documentul citeşte mişcările prin
-- `findAllByCompany_IdAndDeletedFalseAndDateBetween` (acoperit deja de indexul din `V5`) şi
-- filtrează în memorie. Un index pe care nu-l foloseşte nicio interogare e cost de scriere fără
-- cititor. Se adaugă când şi dacă filtrarea se mută în SQL.
