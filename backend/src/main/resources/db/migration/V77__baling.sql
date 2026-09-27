-- F5 — balotarea (proprietarul, 27.09.2026): operatorul scrie doar câți baloți a făcut. Sortimentul balotat știe din ce
-- se face și cât cântărește un balot; operațiunea PROCESSING ține numărul de baloți și greutatea din ziua ei, ca o
-- greutate standard schimbată mâine să nu rescrie o balotare de ieri.
ALTER TABLE waste_articles ADD COLUMN source_article_id UUID REFERENCES waste_articles (id);
ALTER TABLE waste_articles ADD COLUMN bale_weight_kg NUMERIC(10, 3);
ALTER TABLE waste_articles ADD CONSTRAINT waste_articles_bale_complete
    CHECK ((source_article_id IS NULL) = (bale_weight_kg IS NULL));
ALTER TABLE waste_articles ADD CONSTRAINT waste_articles_bale_weight_positive
    CHECK (bale_weight_kg IS NULL OR bale_weight_kg > 0);
ALTER TABLE waste_articles ADD CONSTRAINT waste_articles_bale_not_self
    CHECK (source_article_id IS NULL OR source_article_id <> id);

ALTER TABLE weighing_operations ADD COLUMN bale_count INTEGER;
ALTER TABLE weighing_operations ADD COLUMN bale_weight_kg NUMERIC(10, 3);
ALTER TABLE weighing_operations ADD CONSTRAINT weighing_operations_baling
    CHECK (CASE WHEN type = 'PROCESSING' THEN bale_count > 0 AND bale_weight_kg > 0
                ELSE bale_count IS NULL AND bale_weight_kg IS NULL END);
