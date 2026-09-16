-- Mailul de a doua zi după un termen ratat (16.09.2026). Termenele nebifate cu data trecută se ascunseseră
-- toate (api v105), deci un termen ratat dispărea fără semn. Acum, de la 17.09.2026 încolo, rămâne pe ecran ca
-- depășit și primește o singură notificare; fanionul o ține să nu plece din nou în fiecare zi.
ALTER TABLE reporting_deadlines ADD COLUMN warned_missed BOOLEAN NOT NULL DEFAULT FALSE;
