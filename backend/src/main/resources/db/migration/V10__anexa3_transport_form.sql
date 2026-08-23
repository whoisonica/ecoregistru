-- ETAPA G3 — Anexa 3 la HG 1061/2008, "formularul de încărcare-descărcare deşeuri nepericuloase",
-- generat din mişcarea deja înregistrată.
--
-- Modelul completat primit de la specialistă (seria HMB 180) dă lista exactă de rubrici, iar una
-- dintre ele decide forma acestei migrări: cantitatea de pe hârtie — "1,02" — e **scrisă de mână**,
-- după cântărire. Un magazin de cartier nu are cântar: predă deşeul, iar colectorul îl cântăreşte
-- la depozit. Formularul pleacă deci cu rubrica goală, şi trebuie să putem face la fel.

-- ---------- 1. Cantitatea poate lipsi, dar numai declarat ----------

-- Nu "cantitate zero" şi nu o estimare: zero ar fi o cifră falsă pe un document oficial, iar o
-- estimare ar intra în stocul din Anexa 1 ca şi cum ar fi fost măsurată. Cantitatea lipseşte,
-- se vede că lipseşte, şi se completează când vine cântarul de la colector.
ALTER TABLE waste_movements ALTER COLUMN quantity DROP NOT NULL;

-- Steagul care face lipsa legitimă. Fără el, cantitatea rămâne obligatorie ca până acum.
ALTER TABLE waste_movements ADD COLUMN weighed_at_unloading BOOLEAN NOT NULL DEFAULT FALSE;

-- Volumul, singura măsură pe care o are cine n-are cântar. Modelul completat îl poartă în aceeaşi
-- rubrică: "17 mc" lângă tonaj.
ALTER TABLE waste_movements ADD COLUMN volume_m3 NUMERIC(12,3);

-- ---------- 2. Rubricile formularului care nu existau în model ----------

-- Formularul are două date: încărcarea şi descărcarea. `date` era încărcarea; descărcarea e nouă.
ALTER TABLE waste_movements ADD COLUMN unload_date DATE;

-- "Date de identificare transportator". Poate fi destinatarul, un cărăuş separat sau chiar firma
-- noastră (NULL = transportăm noi).
ALTER TABLE waste_movements ADD COLUMN transport_partner_id UUID REFERENCES partners (id);

-- "Date de identificare delegat şi nr. de înmatriculare mijloc de transport".
ALTER TABLE waste_movements ADD COLUMN driver_name           VARCHAR(255);
-- Un singur câmp liber, nu o coloană de CNP. Modelul completat are acolo şi serie/nr. CI, şi CNP;
-- o coloană dedicată de CNP ar cere regimul GDPR separat pe care OUG 31/2011 îl impune pentru
-- borderoul de achiziţie (Etapa 9), iar aici nu avem nevoie de el ca dată structurată.
ALTER TABLE waste_movements ADD COLUMN driver_identification VARCHAR(100);
ALTER TABLE waste_movements ADD COLUMN vehicle_registration  VARCHAR(50);

-- "Serie şi număr", alocate la prima generare şi păstrate, ca retipărirea să dea acelaşi document.
ALTER TABLE waste_movements ADD COLUMN anexa3_series VARCHAR(20);
ALTER TABLE waste_movements ADD COLUMN anexa3_number INTEGER;

CREATE UNIQUE INDEX uq_movements_anexa3_number
    ON waste_movements (company_id, anexa3_number)
    WHERE anexa3_number IS NOT NULL;

-- "Destinat:" — colectării / stocării temporare / tratării / valorificării / eliminării.
-- Multiplu, nu unic: în modelul completat sunt bifate două ("Colectării" şi "Valorificării").
CREATE TABLE waste_movement_transport_destinations (
    waste_movement_id UUID        NOT NULL REFERENCES waste_movements (id),
    destination       VARCHAR(30) NOT NULL,
    PRIMARY KEY (waste_movement_id, destination)
);

-- Seria formularelor firmei. Multe firme cumpără carnete pre-tipărite cu seria lor ("HMB"), deci
-- e configurabilă; numărul îl alocăm noi, crescător pe firmă.
ALTER TABLE companies ADD COLUMN anexa3_series VARCHAR(20);

-- ---------- 3. Datele partenerului pe care le tipăreşte formularul ----------

-- Rubrica "Date de identificare destinatar" cere adresă şi nr. de la Registrul Comerţului, pe care
-- partenerul nu le avea. Rubrica "transportator" cere şi licenţa de transport mărfuri.
ALTER TABLE partners ADD COLUMN address                  VARCHAR(500);
ALTER TABLE partners ADD COLUMN trade_register_number    VARCHAR(50);
ALTER TABLE partners ADD COLUMN transport_license_number VARCHAR(255);
ALTER TABLE partners ADD COLUMN transport_license_expiry DATE;

-- Aceeaşi rubrică, pentru noi, la "Date de identificare expeditor" (CUI îl avem deja).
ALTER TABLE companies ADD COLUMN trade_register_number VARCHAR(50);

-- ---------- 4. Evidenţa nu închide peste o cantitate necântărită ----------

-- O predare în aşteptarea cântarului a plecat fizic, dar cu cât nu se ştie încă. Linia lunară nu
-- poate fi raportată aşa: se marchează incompletă, la fel ca ieşirile fără cod R/D din V6.
ALTER TABLE monthly_evidences ADD COLUMN awaiting_weighing BOOLEAN NOT NULL DEFAULT FALSE;

-- Cache regenerabil din mişcări, ca la fiecare schimbare de formulă.
DELETE FROM monthly_evidences;
