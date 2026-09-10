-- P3.2 — Anexa 2 la HG 1061/2008: formularul de expediţie/transport deşeuri periculoase.
--
-- Cele patru rubrici pe care modelul le cere şi pe care modelul nostru de date nu le avea. Restul
-- formularului se completează din ce există deja: codul şi denumirea din nomenclator, delegatul şi
-- numărul de înmatriculare din câmpurile Anexei 3, cele cinci casete ale destinatarului din
-- `waste_movement_transport_destinations` (acelaşi `Set`, fiindcă şi Anexa 3 lua mai multe bife),
-- CUI-urile din `companies` şi `partners`, datele din `date`, iar tipul mijlocului de transport din
-- `transport_means` (Anexa 1 cap. 2, nota 4 — e aceeaşi întrebare, n-are rost pusă a doua oară).
--
-- ⚠️ **`anexa2_number` NU se alocă de noi**, şi asta e opusul lui `anexa3_number` (`max+1` pe
-- firmă, cu index unic). Nota `*1)` de sub modelul oficial spune textual: „Număr înscris de către
-- **agenţia judeţeană pentru protecţia mediului**." Deci coloana e un câmp tastat, gol implicit;
-- dacă am genera noi un număr, am tipări un număr inventat pe un formular oficial. De aici şi
-- tipul: `varchar`, nu `integer` — ce scrie APM-ul e un şir, nu o secvenţă a noastră.
--
-- ⚠️ **`anexa2_below_one_ton` e `boolean` NULL, şi nullable e tot ce contează la ea.** Pragul de
-- 1 t/an (art. 5, 7, 15 alin. (1)) e scris „din aceeaşi **categorie** de deşeuri periculoase", iar
-- actul nu defineşte „categorie": art. 2 trimite la „anexa nr. I A la OUG nr. 78/2000", act
-- abrogat, deci lanţul de definiţii e rupt şi nicio sursă publică nu-l reface. NULL înseamnă
-- „n-a răspuns nimeni, se propune din cumulul pe cod"; `true`/`false` înseamnă „omul a decis".
-- Riscul e asimetric — sub prag sari peste aprobarea din art. 7, ceea ce e contravenţie de
-- 10.000–20.000 lei (art. 25 alin. (2) lit. b) — deci propunerea se arată cu cifra lângă ea şi
-- rămâne editabilă. Unde actul tace, câmpul se propune, nu se impune.
--
-- Aditivă, ca toate celelalte: toate coloanele sunt nullable, niciun rând existent nu se atinge, şi
-- toate mişcările de până acum se citesc ca „formular necompletat" — ceea ce e adevărat.

ALTER TABLE waste_movements ADD COLUMN anexa2_number VARCHAR(30);
ALTER TABLE waste_movements ADD COLUMN anexa2_approval_number VARCHAR(60);
ALTER TABLE waste_movements ADD COLUMN anexa2_packaging VARCHAR(255);
ALTER TABLE waste_movements ADD COLUMN anexa2_below_one_ton BOOLEAN;

COMMENT ON COLUMN waste_movements.anexa2_number IS
    'Numarul formularului de expeditie/transport deseuri periculoase (anexa 2 la HG 1061/2008). '
    'Se tasteaza, nu se aloca: nota *1) a modelului spune ca numarul il inscrie agentia judeteana '
    'pentru protectia mediului. Gol implicit.';

COMMENT ON COLUMN waste_movements.anexa2_approval_number IS
    'Nr. formularului de aprobare al transportului (anexa 1 la HG 1061/2008), cerut de art. 7 peste '
    '1 t/an. Gol sub prag: art. 6 alin. (1) scoate tocmai aprobarea.';

COMMENT ON COLUMN waste_movements.anexa2_packaging IS
    'Rubrica "Numar si tip de ambalaje utilizate pentru transportul deseurilor periculoase" de la '
    'coada formularului. Text liber: actul nu da nomenclator.';

COMMENT ON COLUMN waste_movements.anexa2_below_one_ton IS
    'Bifa "Deseuri periculoase < 1t/an" de pe formular. NULL = nimeni n-a decis inca, se propune '
    'din cumulul anual pe cod; true/false = alegerea utilizatorului, care ramane a lui fiindca '
    'actul nu defineste "aceeasi categorie de deseuri periculoase".';
