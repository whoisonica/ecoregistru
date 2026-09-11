-- G-7 / P3.5 — buletinele de analiză, OUG 92/2021 art. 8 alin. (4) și art. 48 alin. (2).
--
-- > **Art. 48 alin. (2):** Producătorii şi deţinătorii de deşeuri periculoase sunt obligaţi să
-- > **deţină buletinele de analiză** care caracterizează deşeurile periculoase şi să le transmită,
-- > la cerere, autorităţilor competente pentru protecţia mediului.
--
-- > **Art. 8 alin. (4):** [...] producătorii şi deţinătorii de deşeuri persoane juridice sunt
-- > obligaţi să **efectueze şi să deţină o caracterizare a deşeurilor periculoase generate din
-- > propria activitate** şi a deşeurilor care pot fi considerate periculoase din cauza originii sau
-- > compoziţiei [...]
--
-- Până azi se putea ataşa orice fişier la o **mişcare**, dar nimic nu lega un buletin de un **cod**
-- şi nimic nu semnala absenţa lui. La un client cu coduri periculoase, dosarul de control era
-- incomplet legal fără să se vadă — exact clasa de defect pe care proiectul o repară peste tot:
-- o lipsă trebuie să se vadă ca lipsă.
--
-- ⚠️ **De ce „per cod" şi nu „per transport", scris aici fiindcă a fost întrebarea deschisă opt
-- zile (AL).** Răspunsul e în art. 8 alin. (4), nu la specialistă: obligaţia e o caracterizare a
-- deşeurilor *generate din propria activitate*, iar scopurile pe care articolul le enumeră —
-- amestecare, pregătire prealabilă, reciclare, valorificare, eliminare — sunt proprietăţi ale
-- **tipului de deşeu**, nu ale unei curse. Deci cheia e (firmă, cod de deşeu).
--
-- ⚠️ **NU are dată de expirare, şi asta e o decizie, nu o scăpare.** Art. 48 alin. (2) cere „să
-- deţină" — obligaţie **continuă, fără termen** —, iar art. 48 alin. (5) pune podeaua evidenţei la
-- 3 ani (12 luni la transportatori). Singurul lucru pe care actul chiar nu-l spune e **cât de des
-- trebuie refăcut** buletinul pentru acelaşi cod, şi aia e practica inspectorului: e ce a mai rămas
-- din întrebarea AL. A pune azi o coloană „valabil până la" ar însemna să ghicim un termen pe care
-- actul nu-l dă şi să-l tipărim apoi într-un dosar care ajunge la un inspector — adică fix regula
-- de lucru 1. Când vine răspunsul, coloana se adaugă aditiv şi nimic din ce e aici nu se rescrie.
--
-- ⚠️ **Mai multe buletine pe acelaşi cod sunt istoricul lui, nu un duplicat de curăţat** — de aceea
-- NU există unicitate pe (company_id, waste_code_id). O reanaliză nu şterge buletinul vechi:
-- art. 48 alin. (5) cere păstrarea evidenţei cel puţin 3 ani, iar o fişă depusă în 2025 s-a
-- sprijinit pe buletinul de atunci. Cel mai recent e cel care răspunde la „ai caracterizarea?";
-- celelalte rămân, şi dosarul le numără.
--
-- Coloanele de stocare sunt aceleaşi cinci ca la `attachments` şi pentru acelaşi motiv (11-bis):
-- fişierul urcă `type=authenticated`, iar adresa semnată se construieşte pe server la fiecare
-- citire şi nu pleacă niciodată spre client. Un buletin de analiză numeşte firma, amplasamentul şi
-- compoziţia chimică a deşeului ei — n-are ce căuta la un URL public.

CREATE TABLE analysis_bulletins (
    id            UUID PRIMARY KEY,
    company_id    UUID NOT NULL REFERENCES companies (id),
    waste_code_id UUID NOT NULL REFERENCES waste_codes (id),

    issue_date    DATE NOT NULL,
    laboratory    VARCHAR(200) NOT NULL,

    url           TEXT NOT NULL,
    public_id     TEXT NOT NULL,
    resource_type TEXT,
    delivery_type TEXT,
    format        TEXT,
    file_name     TEXT,
    content_type  TEXT,

    created_at    TIMESTAMP NOT NULL,
    created_by    UUID
);

-- Cum se citeşte tabela peste tot: „ce buletine are firma asta, pe ce coduri". Dosarul de control
-- o întreabă o dată pe generare, iar registrul de mişcări o dată pe listare — niciodată per rând.
CREATE INDEX idx_analysis_bulletins_company_code
    ON analysis_bulletins (company_id, waste_code_id);

COMMENT ON TABLE analysis_bulletins IS
    'Buletinele de analiza care caracterizeaza un deseu periculos, legate de COD, nu de miscare '
    '(OUG 92/2021 art. 8 alin. (4) si art. 48 alin. (2)). Fara data de expirare: art. 48 alin. (2) '
    'cere sa le detii, obligatie continua; frecventa reanalizei e practica de inspector si e '
    'singura jumatate ramasa din intrebarea AL. Mai multe randuri pe acelasi cod = istoric.';

COMMENT ON COLUMN analysis_bulletins.issue_date IS
    'Data buletinului, asa cum o poarta hartia emisa de laborator. Nu e data incarcarii in '
    'aplicatie: la un control conteaza cand a fost facuta analiza.';

COMMENT ON COLUMN analysis_bulletins.laboratory IS
    'Laboratorul care l-a emis. Text liber: actul nu impune un nomenclator de laboratoare si nu '
    'exista unul public de la care sa-l luam.';
