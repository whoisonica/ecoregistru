-- Angajamentul opțional de 12 luni (decizia din 24.09.2026, contract v2.2 art. 4.3).
--
-- Clientul care îl alege nu plătește implementarea (la cabinet: pornirea) pe prima factură. Plătește
-- tot lunar. Dacă oprește înainte de a 12-a perioadă, implementarea intră pe ultima lui factură, iar
-- lunile rămase nu se datorează. Fără angajament, nimic nu se schimbă: implementarea pe prima factură
-- și oprire oricând, cu preaviz de o lună.
ALTER TABLE subscriptions
    ADD COLUMN twelve_month_commitment BOOLEAN NOT NULL DEFAULT FALSE;
