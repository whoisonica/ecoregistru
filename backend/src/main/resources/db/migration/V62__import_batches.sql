-- Istoricul importurilor din Excel și anularea lor (16.09.2026). Importul îl face numai platforma (api v108);
-- un import greșit se ștergea mișcare cu mișcare. Acum fiecare mișcare importată ține minte importul din care a
-- venit, iar „Anulează” șterge (soft) doar mișcările importului pe care nimeni nu le-a modificat între timp.
CREATE TABLE import_batches (
    id                UUID         PRIMARY KEY,
    company_id        UUID         NOT NULL REFERENCES companies (id) ON DELETE CASCADE,
    -- Numele fișierului, cum l-a trimis browserul. Doar pentru om: amprenta rândurilor e în client_generated_id.
    file_name         VARCHAR(255),
    created_by        UUID         NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL,
    work_points_new   INT          NOT NULL,
    partners_new      INT          NOT NULL,
    movements_new     INT          NOT NULL,
    undone_at         TIMESTAMPTZ,
    undone_by         UUID
);
CREATE INDEX idx_import_batches_company ON import_batches (company_id, created_at DESC);

ALTER TABLE waste_movements ADD COLUMN import_batch_id UUID REFERENCES import_batches (id);
CREATE INDEX idx_waste_movements_import_batch ON waste_movements (import_batch_id) WHERE import_batch_id IS NOT NULL;
