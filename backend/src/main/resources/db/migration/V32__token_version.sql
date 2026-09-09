-- P0.4 — sesiuni care se pot închide. Contorul de sesiuni al fiecărui utilizator.
--
-- **Ce era greşit.** `TOKEN_VALIDITY_MS` era **30 de zile**, nu exista refresh, nu exista listă de
-- tokenuri revocate, iar tokenul stă în `localStorage`. Consecinţa, scrisă pe litere: **dezactivezi
-- un utilizator şi el continuă să lucreze o lună**. Pe un produs de conformitate, întrebarea „ce se
-- întâmplă când pleacă un angajat" primea răspunsul „nimic, încă o lună".
--
-- **Cele două jumătăţi ale reparaţiei, şi de ce e nevoie de amândouă.**
--   * `enabled` se verifică acum la fiecare cerere (`JwtService.isTokenValid`) — asta închide pe loc
--     sesiunea unui cont dezactivat, fără nicio coloană nouă;
--   * coloana asta închide ce `enabled` nu poate: o **schimbare de parolă**. Contul rămâne activ,
--     deci nimic din starea lui nu s-ar fi schimbat, iar tokenurile emise cu parola veche ar fi
--     rămas valabile — inclusiv cel al persoanei de la care ţi-ai luat contul înapoi. Resetarea
--     parolei incrementează contorul, tokenul poartă valoarea de la emitere, iar la prima
--     nepotrivire cererea iese 401.
--
-- **De ce un întreg şi nu o dată.** O dată ar fi cerut ceas sincronizat între emitere şi
-- verificare, şi ar fi lăsat o fereastră cât diferenţa dintre ele. Un contor care creşte n-are
-- fereastră: ori e egal, ori nu.
--
-- **De ce `DEFAULT 0` şi nu `DEFAULT 1`.** Tokenurile emise **înainte** de migrarea asta n-au deloc
-- claimul `tv`. Ele sunt tratate ca versiunea 0, deci rămân valabile până le expiră singure —
-- altfel migrarea ar fi deconectat pe loc pe toată lumea, la o oră aleasă de Flyway. Cine vrea
-- exact asta (şi la lansare chiar se vrea, o dată) rulează un UPDATE care pune contorul pe 1.
--
-- Aditivă, ca toate celelalte.

ALTER TABLE app_users ADD COLUMN token_version INTEGER NOT NULL DEFAULT 0;

COMMENT ON COLUMN app_users.token_version IS
    'Contorul de sesiuni. Fiecare JWT poartă valoarea de la emitere, în claimul "tv"; la '
    'nepotrivire cererea e 401. Creşte la resetarea parolei. Un token fără claim (emis înainte de '
    'V32) e citit ca 0, deci nu se invalidează retroactiv. Un UPDATE pe coloana asta = '
    '"deconectează-l de peste tot", fără listă de tokenuri revocate.';
