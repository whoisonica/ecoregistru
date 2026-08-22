-- ETAPA 2b — the Anexa 1 stock formula.
--
-- HG 856/2002 anexa nr. 1, cap. 1 has four quantity columns and no "handed over" one:
--   Generate | din care: valorificată | eliminată final | rămasă în stoc
-- so the stock identity the form encodes is
--   stock = previous stock + generated − recovered − disposed
-- and a handover is reported under "valorificată" or "eliminată final" according to the R/D code
-- of the operation the recipient performs (cap. 3 / cap. 4 name that operation and its operator).
-- Verbatim source: docs/surse-oficiale.md §1.2.
--
-- Two facts the old cache could not express are added here. Nothing is dropped.

-- ---------- 1. Waste that left the site without an R/D code ----------

-- Handovers recorded before the code became mandatory (V5) cannot be classified retroactively:
-- inventing an operation would put a made-up figure on an official form. The quantity is real —
-- it physically left — so it leaves the stock, but it enters neither official column and marks the
-- line as incomplete instead of letting Anexa 1 close silently over missing data.
ALTER TABLE monthly_evidences ADD COLUMN total_unclassified_out NUMERIC(16,3) NOT NULL DEFAULT 0;

-- ---------- 2. Handovers that may be passing on third-party goods ----------

-- The V5 backfill could classify a takeover (COLLECTED → ART_48) but not a handover that passes
-- previously collected goods on: it looks exactly like handing over own waste. Such a line is
-- flagged for review, never rewritten — Etapa 8 moves the art. 48 flow to receptions/deliveries.
ALTER TABLE monthly_evidences ADD COLUMN resale_suspected BOOLEAN NOT NULL DEFAULT FALSE;

-- ---------- 3. total_collected leaves Anexa 1 ----------

-- Goods taken over from third parties are kept out of Anexa 1 by HG 856 art. 2 alin. (1); they
-- belong to the art. 48 register. The column is no longer written by the evidence engine — it is
-- left in place, with a default, so this migration stays additive; Etapa 8 drops it together with
-- the movement rows it describes.
ALTER TABLE monthly_evidences ALTER COLUMN total_collected SET DEFAULT 0;

-- ---------- 4. The cache is stale by definition ----------

-- monthly_evidences is a cache: waste_movements is the source of truth and every line here is
-- rebuilt by POST /api/v1/evidences/regenerate. Lines computed with the old formula would show an
-- Anexa 1 that the law does not recognise, so they go; the screen asks for a regeneration anyway.
DELETE FROM monthly_evidences;
