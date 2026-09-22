-- Registrul art. 30, O1 și III.6: cererile de cont soluționate de peste 12 luni care n-au devenit cont.
-- Se rulează trimestrial (1 ian., 1 apr., 1 iul., 1 oct.):
--   heroku pg:psql -a ecoregistru-api --file scripts/sterge-cereri-cont-vechi.sql
-- și se notează în registru ziua și numărul de cereri șterse (ultimul DELETE îl spune).
BEGIN;
CREATE TEMP TABLE vechi ON COMMIT DROP AS
    SELECT id FROM account_requests
     WHERE created_company_id IS NULL AND handled_at < now() - interval '12 months';
DELETE FROM account_request_market_roles    WHERE account_request_id IN (SELECT id FROM vechi);
DELETE FROM account_request_operation_codes WHERE account_request_id IN (SELECT id FROM vechi);
DELETE FROM account_requests                WHERE id IN (SELECT id FROM vechi);
COMMIT;
