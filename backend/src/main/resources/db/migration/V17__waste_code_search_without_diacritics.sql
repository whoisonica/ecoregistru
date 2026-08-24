-- Căutarea în nomenclator nu mai depinde de diacritice.
--
-- `WasteCodeRepository.search` compara `lower(name) like %q%`, iar cele 842 de denumiri din Lista
-- Europeană a Deșeurilor (Decizia 2014/955/UE) sunt scrise **cu** diacritice. Cine tastează
-- „deseuri" — adică aproape toată lumea, la o tastatură fără layout românesc — nu găsea nimic.
-- Cu zece coduri de test nu se vedea; de la Etapa 1 încoace, cu 842, e primul lucru pe care îl
-- lovește un utilizator nou în formularul de mișcare.
--
-- De ce o coloană generată, și nu extensia `unaccent`:
--   * `unaccent` cere `CREATE EXTENSION` (privilegii pe care nu le avem garantat pe toate
--     mediile) și nu e immutable, deci nici indexabilă direct;
--   * o coloană `GENERATED ALWAYS AS ... STORED` se recalculează singură la orice INSERT sau
--     UPDATE, deci o reîncărcare viitoare a nomenclatorului (un `V__reseed_waste_codes` nou) nu
--     are nimic de ținut minte. `lower()` și `translate()` sunt immutable, deci expresia e legală.
--
-- Perechea din Java: `Diacritics.fold(...)` pliază la fel textul căutat, prin NFD + ștergerea
-- semnelor diacritice. Cele două trebuie să rămână în acord — de asta literele sunt enumerate
-- aici explicit, nu deduse: ă â î ș ș(cedilă) ț ț(cedilă), în ambele forme Unicode, fiindcă
-- fișierele oficiale le amestecă.
--
-- Nu punem index: 842 de rânduri, iar `like '%...%'` nu s-ar folosi oricum de un btree.

ALTER TABLE waste_codes
    ADD COLUMN search_text TEXT
        GENERATED ALWAYS AS (
            lower(translate(code || ' ' || name,
                            'ăâîșşțţĂÂÎȘŞȚŢ',
                            'aaissttAAISSTT'))
        ) STORED;
