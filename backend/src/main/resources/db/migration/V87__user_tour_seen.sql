-- Turul aplicației: la prima intrare clientul e plimbat o dată prin ecrane. O dată pe om, nu pe browser,
-- deci serverul ține minte când l-a văzut; NULL = încă nu.
--
-- Umplerea: cine are deja parolă lucrează în aplicație (proprietarul, conturile demo, clienții de azi) și nu
-- mai are nevoie de tur. Asta înseamnă conturile active și cele dezactivate — „Dezactivat” se atinge numai din
-- „Activ” (V34), deci și ele au lucrat. Rămân NULL doar invitațiile nefolosite (enabled = false,
-- deactivated_at IS NULL): omul vede turul la prima intrare, după ce își pune parola.
ALTER TABLE app_users ADD COLUMN tour_seen_at TIMESTAMPTZ;

UPDATE app_users SET tour_seen_at = now() WHERE enabled = true OR deactivated_at IS NOT NULL;

COMMENT ON COLUMN app_users.tour_seen_at IS
    'Când a văzut omul turul aplicației, sau NULL dacă încă nu. Se pune o singură dată, '
    'din POST /api/v1/users/me/tour-seen; a doua cerere nu o mută.';
