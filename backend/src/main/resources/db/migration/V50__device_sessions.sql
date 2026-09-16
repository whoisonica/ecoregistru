-- G1 — sesiunea pe dispozitiv, pentru aplicația mobilă („WasteHouse pe teren”, todo-mobil §4 G1).
--
-- **Problema.** Tokenul de acces ține 8 ore și nu se reînnoiește (`JwtService.TOKEN_VALIDITY_MS`).
-- Pe web e în regulă: omul se loghează dimineața. Pe telefon, magazionerul de la rampă nu-și ține
-- parola minte și n-o s-o tasteze de două ori pe zi — fără asta aplicația mobilă nu se poate folosi.
--
-- **Ce e un rând aici.** Un dispozitiv care are voie să-și ceară un token nou. Se naște la login
-- (numai dacă cel care se loghează spune că e un dispozitiv) și moare la ieșire, la 60 de zile de
-- nefolosire, la schimbarea parolei sau la dezactivarea contului.
--
-- **De ce hash și nu tokenul.** Tabelul e citit după token, deci nu poate fi bcrypt (n-ai după ce
-- căuta). E SHA-256 peste 32 de octeți aleatori: o bază scursă nu dă nimănui o sesiune, iar
-- tokenul are destulă entropie ca dicționarul să nu ajute. Parolele rămân bcrypt, ele sunt ghicibile.
--
-- **Rotire.** Fiecare folosire înlocuiește hash-ul. Un refresh token folosit de două ori e fie o
-- reîncercare după o rețea căzută, fie un token furat; oricum ar fi, al doilea nu mai deschide nimic.
--
-- **V50, nu V49:** `V49` e rezervat modulului de depozit, construit în paralel.
--
-- Aditivă: tabel nou, nicio coloană atinsă. Webul nu cere sesiuni de dispozitiv, deci nu se schimbă.

CREATE TABLE device_sessions (
    id             UUID PRIMARY KEY,
    user_id        UUID        NOT NULL REFERENCES app_users (id) ON DELETE CASCADE,

    -- SHA-256 hex al tokenului de reîmprospătare. Unic: două dispozitive nu pot ajunge pe el.
    token_hash     CHAR(64)    NOT NULL UNIQUE,

    -- Ce scrie omul în „Dispozitive conectate”: „iPhone 17”, „Pixel 8”. Spus de client, deci mărginit.
    device_name    VARCHAR(80) NOT NULL,
    platform       VARCHAR(16) NOT NULL,

    created_at     TIMESTAMPTZ NOT NULL,
    last_used_at   TIMESTAMPTZ NOT NULL,
    expires_at     TIMESTAMPTZ NOT NULL,

    -- Când a fost revocată, și null cât timp e vie. Nu se șterge rândul: „Dispozitive conectate”
    -- arată numai rândurile vii, dar o sesiune revocată rămâne o urmă pentru cine se uită după.
    revoked_at     TIMESTAMPTZ,

    CONSTRAINT device_sessions_platform CHECK (platform IN ('IOS', 'ANDROID'))
);

-- Revocarea în masă (parolă schimbată, cont dezactivat) și lista din Setări trec pe aici.
CREATE INDEX device_sessions_user_idx ON device_sessions (user_id);
