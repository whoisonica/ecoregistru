-- Ambalajele se declară din mişcări, nu dintr-o grilă separată.
--
-- `V22` a pornit de la premisa că **tabelul 1** al Anexei 1 Ambalaje (Ordinul 794/2012) nu se poate
-- deduce din nimic din ce ţine aplicaţia, fiindcă e despre marfa pusă pe piaţă, nu despre deşeu.
-- Premisa era prea strictă. Utilizatorul a arătat fluxul real (25.08.2026): *„omul, când reciclează
-- acea cantitate de ambalaj pusă pe piaţă, o să adauge mişcare, ca să o poată scoate şi să apară în
-- gestiune şi în rapoarte"*. Deci kilogramele **trec oricum printr-o mişcare** pe un cod `15 01 xx`;
-- ce lipsea nu era cifra, ci **felul ambalajului**, singura rubrică a tabelului 1 pe care mişcarea
-- n-o purta.
--
-- Coloanele de mai jos sunt exact rubricile formularului:
--
--   * `packaging_material`          → rândul de material: Sticlă · PET · Alte plastice ·
--                                     Hârtie carton · Aluminiu · Oţel · Lemn · Altele. Nu se poate
--                                     citi din codul de deşeu: `15 01 04` e „ambalaje metalice" şi
--                                     acoperă şi aluminiul, şi oţelul; `15 01 02` e „ambalaje de
--                                     materiale plastice" şi acoperă şi PET-ul, şi navetele. Deci
--                                     se alege pe mişcare, iar codul doar propune, acolo unde
--                                     propune singur: `15 01 01` → Hârtie carton, `15 01 03` →
--                                     Lemn, `15 01 07` → Sticlă, `15 01 02` → Alte plastice.
--   * `packaging_category`          → col. 1 „ambalaje de desfacere fabricate/importate",
--                                     col. 3 „ambalaje primare", col. 5 „ambalaje secundare şi de
--                                     transport". Col. 2 e suma 3+5 şi nu se stochează.
--   * `packaging_reusable`          → col. 4 şi col. 6, „din care: ambalaj reutilizabil".
--   * `packaging_hazardous_content` → col. 7, „ambalaje cu conţinut periculos", parte din col. 3.
--
-- Toate sunt **nullable**, şi nullul nu se completează singur. O mişcare fără material sau fără
-- categorie nu intră în tabelul 1 şi se **vede** ca atare în tabul Ambalaje — regula casei: pe un
-- formular depus, ce lipseşte se arată că lipseşte, nu se ghiceşte. Rândul „Altele" **nu** mai e
-- găleata în care cădea ce nu se putea aşeza: specialista spune că în practică rămâne gol, deci se
-- foloseşte doar dacă îl alege cineva. Modelul completat pe care îl avem
-- (`documente oficiale/RAPORTARE AMBALAJE 2021_anexa 1_ HRR.xlsx`) are un singur rând, Oţel
-- 5192 kg, aşezat pe „secundare şi de transport" — de aceea formularul propune categoria asta, dar
-- o propune vizibil, în ecran, nu tăcut aici.
--
-- Mişcările de dinaintea migrării rămân fără categorie, deci nicio cifră tipărită până acum nu se
-- schimbă de la sine.

ALTER TABLE waste_movements ADD COLUMN packaging_material VARCHAR(20);
ALTER TABLE waste_movements ADD COLUMN packaging_category VARCHAR(20);
ALTER TABLE waste_movements ADD COLUMN packaging_reusable BOOLEAN;
ALTER TABLE waste_movements ADD COLUMN packaging_hazardous_content BOOLEAN;

COMMENT ON COLUMN waste_movements.packaging_material IS
    'Rândul de material din tabelul 1 al Anexei 1 Ambalaje (Ordinul 794/2012). Se alege pe mişcare '
    'fiindcă Lista Europeană nu-l decide: 15 01 04 acoperă şi aluminiul, şi oţelul. Null = se ia ce '
    'propune codul (15 01 01 hârtie, 15 01 02 alte plastice, 15 01 03 lemn, 15 01 07 sticlă), iar '
    'dacă nici codul nu propune, cantitatea nu intră în tabel şi se arată ca neîncadrată.';

COMMENT ON COLUMN waste_movements.packaging_category IS
    'Felul ambalajului, pentru tabelul 1 al Anexei 1 Ambalaje (Ordinul 794/2012): SALES = ambalaje '
    'de desfacere fabricate/importate (col. 1), PRIMARY = ambalaje primare (col. 3), SECONDARY = '
    'ambalaje secundare şi de transport (col. 5). Are sens doar pe coduri 15 01 xx. Null = nu s-a '
    'răspuns: cantitatea nu intră în tabelul 1 şi se arată ca necategorisită.';

COMMENT ON COLUMN waste_movements.packaging_reusable IS
    'Col. 4 / col. 6 din tabelul 1, „din care: ambalaj reutilizabil". Nota 2 a formularului: se '
    'raportează o singură dată, la prima livrare în circuitul de umplere.';

COMMENT ON COLUMN waste_movements.packaging_hazardous_content IS
    'Col. 7 din tabelul 1, „ambalaje cu conţinut periculos". Nota 3: sunt tot ambalaje primare şi '
    'se regăsesc şi în col. 3, deci cantitatea se numără în ambele coloane, nu se mută.';

COMMENT ON TABLE packaging_market_entries IS
    'Tabelul 1 din Anexa 1 Ambalaje (Ordinul 794/2012), în kilograme. De la V26 **nu mai e sursa**: '
    'tabelul se însumează din mişcările pe coduri 15 01 xx, care poartă acum felul ambalajului. '
    'Rândurile de aici rămân ca **suprascriere pe material**, pentru cazul în care cifra reală de '
    'piaţă diferă de ce arată mişcările — un rând scris aici înlocuieşte rândul calculat.';
