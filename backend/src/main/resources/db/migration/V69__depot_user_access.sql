-- Depozitul, F2 — accesul pe depozit (D2.4). Un operator de cântar din Baciu nu vede Turda
-- (Legea 190/2018: minimizarea datelor — operațiunile poartă persoane fizice cu CNP).
--
-- Decizia proprietarului (16.09.2026): utilizatorii existenți primesc implicit TOATE depozitele. De aceea
-- nu se scriu rânduri pentru fiecare utilizator × depozit (un depozit deschis mâine le-ar lipsi), ci un
-- semn pe utilizator: `all_work_points = TRUE` înseamnă „toate, și cele de mâine”. Doar la FALSE se citește
-- lista de mai jos. Semnul contează numai la OPERATOR și CLIENT_VIEWER; adminul, consultantul și platforma
-- văd mereu tot (`DepotAccess`).
--
-- ⚠️ V69: V67 e a aplicației mobile, V68 cântarul (D2.3, tot local). Numărul se dă în ordinea în care
-- felia ajunge pe dyno.

ALTER TABLE app_users ADD COLUMN all_work_points BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE user_work_points (
    user_id        UUID NOT NULL REFERENCES app_users (id) ON DELETE CASCADE,
    work_point_id  UUID NOT NULL REFERENCES work_points (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, work_point_id)
);

CREATE INDEX idx_user_work_points_work_point ON user_work_points (work_point_id);
