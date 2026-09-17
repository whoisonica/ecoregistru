-- Numărătoarea read-only dinaintea și de după curățenie (plan-curatenie-prod-demo.md, pașii 2.2 și 2.4):
-- versiunea schemei, rândurile nevide pe fiecare tabelă din `public` și conturile rămase.
--   heroku pg:psql -a ecoregistru-api -- -v admin=<email> --file scripts/numara-randuri.sql
\set ON_ERROR_STOP on
\if :{?admin}
\else
  \echo 'lipsește adminul: psql … -v admin=<emailul contului PLATFORM_ADMIN>'
  \quit
\endif
begin read only;
select max(version::int) as schema from flyway_schema_history where version is not null and success;
select relname as tabela, n as randuri
  from (select c.relname, (xpath('/row/n/text()',
          query_to_xml(format('select count(*) as n from public.%I', c.relname), false, true, '')))[1]::text::int as n
          from pg_class c join pg_namespace s on s.oid = c.relnamespace
         where s.nspname = 'public' and c.relkind = 'r') x
 where n > 0
 order by relname;
select role, count(*) as conturi,
       count(*) filter (where lower(email) = lower(:'admin')) as adminul
  from app_users group by role order by role;
commit;
