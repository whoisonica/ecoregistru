-- Un an de evidență fără nicio linie nu avea unde să-și noteze că a fost socotit.
--
-- `monthly_evidences` ține prospețimea pe linie (`generated_at`), iar un an poate ieși gol pe drept:
-- fără mișcări proprii și fără stoc adus din decembrie, reconstrucția n-are ce scrie. Pentru un
-- asemenea an `min(generated_at)` e null și la a suta citire, deci ieșea mereu „învechit" — iar
-- fiecare deschidere de ecran reconstruia lanțul întreg al firmei, pe veci. Se declanșa singur la
-- 1 ianuarie, când anul nou e gol prin definiție (20.09.2026).
--
-- Cele două coloane sunt urma trecerii, nu un al doilea cache: când s-a socotit ultima oară și până
-- la ce an a ajuns. Un an gol e la zi dacă trecerea l-a cuprins și e mai nouă decât ultima schimbare
-- de mișcare. O corectură pe o mișcare urcă `updated_at`-ul ei peste `evidence_generated_at`, deci
-- invalidarea rămâne cea de până acum — nu e nimic de șters aici la o schimbare.
ALTER TABLE companies
    ADD COLUMN evidence_generated_at      TIMESTAMPTZ,
    ADD COLUMN evidence_generated_through INTEGER;
