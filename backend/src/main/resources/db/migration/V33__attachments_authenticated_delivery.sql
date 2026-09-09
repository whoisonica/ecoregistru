-- 11-bis: atașamentele nu mai stau la un URL public.
--
-- Până acum `url` ținea `secure_url` de la Cloudinary, iar API-ul îl dădea mai departe
-- clientului. Adică fiecare atașament era un link public: fără autentificare, fără sesiune și
-- fără nicio verificare de tenant, pe o aplicație prin care trec avize, contracte și acte de
-- identitate de șofer. Nu era o deducție — pe 09.09.2026 un `curl` gol a descărcat un document
-- de producție.
--
-- De acum fișierele se urcă `type=authenticated` (nelivrabile fără semnătură) și se citesc
-- printr-un endpoint al nostru, care verifică tenantul. Ca să putem construi URL-ul semnat
-- pe server avem nevoie de cele trei fețe ale assetului pe care Cloudinary le cere înapoi.
--
-- Coloanele sunt NULL pentru rândurile vechi, și asta e semnalul, nu o scăpare:
-- NULL înseamnă „asset dinainte de 11-bis, livrat `type=upload`", iar codul cade înapoi pe `url`
-- pentru ele. Migrarea rămâne aditivă (regula 4) — nimic nu se rescrie și nimic nu se pierde.
-- ⚠️ Rândurile vechi rămân public livrabile la URL-ul lor; se sting urcând fișierul din nou sau
-- ștergându-l, nu dintr-o migrare (assetul stă la Cloudinary, nu la noi).
ALTER TABLE attachments ADD COLUMN resource_type VARCHAR(32);
ALTER TABLE attachments ADD COLUMN delivery_type VARCHAR(32);
ALTER TABLE attachments ADD COLUMN format        VARCHAR(32);
