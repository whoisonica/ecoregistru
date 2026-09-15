-- P2.14 — antetul cabinetului pe rapoartele neoficiale ale firmelor lui.
--
-- **Ce primește.** Un logo și un rând de antet (de ex. telefonul și adresa cabinetului), tipărite sus pe
-- rezumatul evidenței (.pdf/.xlsx), pe lista autorizațiilor partenerilor și în README-ul dosarului de
-- control, pe fiecare firmă a cabinetului. **Formularele oficiale nu se ating** — pe ele nu se pune nimic
-- care nu e în model (fișa din HG 856/2002, evidența centralizată, anexele, avizul).
--
-- **De ce tabel separat, nu coloane pe `consultancies`.** Cabinetul se citește la fiecare cerere a unui
-- consultant (tenantul, panoul, rezumatul zilnic); un `bytea` pe rândul lui s-ar încărca odată cu el.
--
-- **De ce în bază, nu pe Cloudinary.** Un logo are sute de KB, iar Cloudinary ține fișierele în SUA
-- (P3.10); restul datelor stau în UE.
--
-- **V48, nu V47:** `V47` e rezervat plății cu cardul (F3/F4), construită în paralel. Se deployează după ea.
--
-- Aditivă: niciun rând existent nu se schimbă; un cabinet fără rând aici tipărește ca până azi.

CREATE TABLE consultancy_branding (
    consultancy_id     UUID PRIMARY KEY REFERENCES consultancies (id),
    header_line        VARCHAR(200),
    logo               BYTEA,
    logo_content_type  VARCHAR(32),
    updated_at         TIMESTAMPTZ NOT NULL,

    -- Logoul și tipul lui vin împreună; un tip fără imagine ar spune „are logo” unui cabinet care n-are.
    CONSTRAINT consultancy_branding_logo_type CHECK ((logo IS NULL) = (logo_content_type IS NULL)),
    CONSTRAINT consultancy_branding_logo_kind CHECK (logo_content_type IN ('image/png', 'image/jpeg'))
);
