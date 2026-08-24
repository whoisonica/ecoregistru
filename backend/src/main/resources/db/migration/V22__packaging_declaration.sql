-- Anexa 1 Ambalaje (Ordinul 794/2012) — tabelul 1, singurul care nu se poate calcula.
--
-- Declaraţia are două tabele, şi ele vin din locuri diferite:
--
--   * **Tabelul 2 — deşeuri de ambalaje gestionate** iese din ce avem deja: predările pe coduri
--     `15 01 xx`, cu partenerul, CUI-ul lui şi codul R/D. Nu se stochează nimic pentru el; se
--     calculează la generare, exact ca fişa şi ca declaraţia anuală.
--   * **Tabelul 1 — ambalaje introduse pe piaţa naţională** nu se poate deduce **din nimic** din
--     ce ţine aplicaţia: e despre marfa pusă pe piaţă, nu despre deşeu. Cine vinde 5.192 kg de
--     ambalaj de oţel odată cu produsele lui e singurul care ştie cifra. Deci se introduce.
--
-- Tabela de mai jos e fix tabelul 1: un rând per material şi an, cu cele cinci cifre pe care le
-- cere formularul. „Total (col. 3+5)" şi rândurile de totaluri (Total plastic, Total metal, TOTAL)
-- **nu se stochează** — sunt sume, şi se fac la tipărire. Două surse pentru aceeaşi cifră ar
-- însemna două cifre care se pot contrazice pe un formular depus.
--
-- Materialele sunt cele unsprezece rânduri ale actului, în ordinea lui; cele calculate lipsesc
-- dinadins din enum. Nimic nu se completează singur aici: un rând care nu există se tipăreşte gol,
-- nu zero — regula casei, aceeaşi ca la codul CAEN şi la cantitatea necântărită.

CREATE TABLE packaging_market_entries (
    id                    UUID PRIMARY KEY,
    company_id            UUID         NOT NULL REFERENCES companies (id),
    year                  INT          NOT NULL,
    material              VARCHAR(30)  NOT NULL,
    -- col. 1: ambalaje de desfacere fabricate/importate
    sales_packaging       NUMERIC(14, 3),
    -- col. 3 şi 4: ambalaje primare, total şi din care reutilizabile
    primary_total         NUMERIC(14, 3),
    primary_reusable      NUMERIC(14, 3),
    -- col. 5 şi 6: ambalaje secundare şi de transport, total şi din care reutilizabile
    secondary_total       NUMERIC(14, 3),
    secondary_reusable    NUMERIC(14, 3),
    -- col. 7: ambalaje cu conţinut periculos, parte din col. 3
    hazardous_content     NUMERIC(14, 3),
    updated_at            TIMESTAMP    NOT NULL,
    CONSTRAINT uq_packaging_entry UNIQUE (company_id, year, material)
);

CREATE INDEX idx_packaging_entries_company_year
    ON packaging_market_entries (company_id, year);

COMMENT ON TABLE packaging_market_entries IS
    'Tabelul 1 din Anexa 1 Ambalaje (Ordinul 794/2012): ambalajele introduse pe piaţa naţională, '
    'în kilograme. Se introduc de client — nu se pot deduce din mişcări, fiindcă sunt despre marfă, '
    'nu despre deşeu. Tabelul 2 nu are tabelă: se calculează din predările pe coduri 15 01 xx.';
