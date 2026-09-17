-- Curățenia bazei de producție (17.09.2026): rămâne numai contul PLATFORM_ADMIN dat în `-v admin=…`,
-- catalogul de coduri de deșeuri și șablonul de audit. Tot restul (firme, cabinete, mișcări, abonamente,
-- facturi, cereri de cont, jurnal) se șterge. Planul: ecoregistru-docs/docs/plan-curatenie-prod-demo.md.
--
--   heroku pg:backups:capture -a ecoregistru-api        # ÎNTÂI backupul
--   heroku pg:psql -a ecoregistru-api -- -v admin=<email> --file scripts/curatenie-prod.sql
--
-- ⚠️ Istoric: a rulat o dată, pe 17.09.2026, pe V62. Garda de mai jos îl oprește pe orice altă schemă.
--
-- Scris pentru schema V62. Oprește fără să șteargă nimic dacă: schema nu e V62, în `public` există o
-- tabelă pe care lista de mai jos n-o știe, sau adminul lipsește / nu e PLATFORM_ADMIN.
--
-- ⚠️ Fără CASCADE: `app_users.company_id → companies` ar face ca un `truncate companies cascade` să
-- golească și `app_users`, adică și contul adminului. De aceea copiii se golesc explicit, apoi
-- utilizatorii, apoi firmele și cabinetele.
\set ON_ERROR_STOP on
\if :{?admin}
\else
  \echo 'lipsește adminul: psql … -v admin=<emailul contului PLATFORM_ADMIN>'
  \quit
\endif

begin;
select set_config('wh.admin', :'admin', true);

do $$
declare
  expected text[] := array[
    -- se păstrează
    'flyway_schema_history', 'waste_codes', 'audit_checklist_templates', 'app_users',
    'companies', 'consultancies',
    -- se golesc
    'account_request_market_roles', 'account_request_operation_codes', 'account_requests',
    'analysis_bulletins', 'attachments', 'audit_log', 'audit_results', 'card_payments',
    'company_afm_contributions', 'company_market_roles', 'company_operation_codes',
    'company_waste_codes', 'consultancy_branding', 'deliveries', 'device_sessions', 'drivers',
    'import_batches', 'internal_generators', 'monthly_evidences', 'natural_persons',
    'packaging_market_entries', 'partner_work_points', 'partners', 'payment_notifications',
    'receptions', 'reporting_deadlines', 'subscription_invoices', 'subscriptions', 'vehicles',
    'verification_records', 'waste_articles', 'waste_movement_transport_destinations',
    'waste_movements', 'weighing_operations', 'work_points'];
  unknown text;
  missing text;
  v int;
begin
  select max(version::int) into v from flyway_schema_history where version is not null and success;
  if v is distinct from 62 then
    raise exception 'schema e la V%, scriptul e pentru V62 — nu șterg nimic', v;
  end if;

  select string_agg(tablename, ', ') into unknown
    from pg_tables where schemaname = 'public' and tablename <> all(expected);
  if unknown is not null then
    raise exception 'tabele necunoscute scriptului: % — nu șterg nimic', unknown;
  end if;

  select string_agg(t, ', ') into missing
    from unnest(expected) t where not exists (select 1 from pg_tables where schemaname = 'public' and tablename = t);
  if missing is not null then
    raise exception 'tabele care lipsesc: % — nu șterg nimic', missing;
  end if;

  if (select count(*) from app_users where lower(email) = lower(current_setting('wh.admin')) and role = 'PLATFORM_ADMIN') <> 1 then
    raise exception 'adminul % lipsește sau nu e PLATFORM_ADMIN — nu șterg nimic', current_setting('wh.admin');
  end if;
end $$;

truncate
  account_request_market_roles, account_request_operation_codes, account_requests,
  analysis_bulletins, attachments, audit_log, audit_results, card_payments,
  company_afm_contributions, company_market_roles, company_operation_codes,
  company_waste_codes, consultancy_branding, deliveries, device_sessions, drivers,
  import_batches, internal_generators, monthly_evidences, natural_persons,
  packaging_market_entries, partner_work_points, partners, payment_notifications,
  receptions, reporting_deadlines, subscription_invoices, subscriptions, vehicles,
  verification_records, waste_articles, waste_movement_transport_destinations,
  waste_movements, weighing_operations, work_points;

delete from app_users where lower(email) <> lower(:'admin');
update app_users set company_id = null, consultancy_id = null where lower(email) = lower(:'admin');
delete from companies;
delete from consultancies;

-- proba, în aceeași tranzacție: ce a rămas nevid
select relname as tabela, n as randuri
  from (select c.relname, (xpath('/row/n/text()',
          query_to_xml(format('select count(*) as n from public.%I', c.relname), false, true, '')))[1]::text::int as n
          from pg_class c join pg_namespace s on s.oid = c.relnamespace
         where s.nspname = 'public' and c.relkind = 'r') x
 where n > 0
 order by relname;

select email, role, company_id, consultancy_id, enabled from app_users;

commit;
