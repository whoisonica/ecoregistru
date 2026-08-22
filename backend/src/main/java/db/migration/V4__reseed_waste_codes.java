package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.UUID;

/**
 * Reloads the waste code nomenclator from resources/seed/waste_codes.csv, now that the CSV
 * holds the full European List of Waste (Decision 2014/955/EU, 842 codes) instead of the
 * ten placeholder rows seeded by V2. Flyway never re-runs V2, hence a new version.
 *
 * Differences from V2:
 * <ul>
 *   <li>ON CONFLICT DO UPDATE, not DO NOTHING: the ten placeholder codes already in the
 *       table carry hand-written names that must be replaced with the official wording.</li>
 *   <li>Official names contain commas, so a line is split on its first and last comma
 *       rather than by a plain three-way split.</li>
 *   <li>A malformed line aborts the migration instead of being skipped: a partially loaded
 *       nomenclator is worse than a failed deploy.</li>
 * </ul>
 *
 * Codes that disappear from a future edition of the list are intentionally NOT deleted:
 * waste_movements and monthly_evidences reference waste_codes, and historical documents
 * stay readable only while their code resolves (see docs/surse-oficiale.md 3.3 for
 * 13 03 05* -> 13 03 06*, the one renumbering so far).
 */
public class V4__reseed_waste_codes extends BaseJavaMigration {

    private static final String CSV_PATH = "/seed/waste_codes.csv";

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        String sql = "INSERT INTO waste_codes (id, code, name, hazardous) VALUES (?, ?, ?, ?) "
                + "ON CONFLICT (code) DO UPDATE SET name = EXCLUDED.name, hazardous = EXCLUDED.hazardous";

        try (InputStream in = getClass().getResourceAsStream(CSV_PATH)) {
            if (in == null) {
                throw new IllegalStateException("Seed file not found on classpath: " + CSV_PATH);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                 PreparedStatement ps = connection.prepareStatement(sql)) {

                String line;
                int lineNumber = 0;
                int batch = 0;
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                        continue;
                    }

                    int firstComma = trimmed.indexOf(',');
                    int lastComma = trimmed.lastIndexOf(',');
                    if (firstComma < 0 || lastComma == firstComma) {
                        throw new IllegalStateException(
                                "Malformed seed line " + lineNumber + " in " + CSV_PATH + ": " + trimmed);
                    }
                    String code = trimmed.substring(0, firstComma).trim();
                    String name = trimmed.substring(firstComma + 1, lastComma).trim();
                    boolean hazardous = Boolean.parseBoolean(trimmed.substring(lastComma + 1).trim());
                    if (code.isEmpty() || name.isEmpty()) {
                        throw new IllegalStateException(
                                "Empty code or name on seed line " + lineNumber + " in " + CSV_PATH);
                    }

                    ps.setObject(1, UUID.randomUUID());
                    ps.setString(2, code);
                    ps.setString(3, name);
                    ps.setBoolean(4, hazardous);
                    ps.addBatch();
                    batch++;
                }
                if (batch == 0) {
                    throw new IllegalStateException("Seed file is empty: " + CSV_PATH);
                }
                ps.executeBatch();
            }
        }
    }
}
