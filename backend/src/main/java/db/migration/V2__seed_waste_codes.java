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
 * Seeds the global waste code nomenclator from resources/seed/waste_codes.csv.
 *
 * The CSV is the source of truth. To (re)load an updated official list, add a new
 * versioned migration (copy this class to V3__reseed_waste_codes) — Flyway will not
 * re-run an already-applied version. See the TODO in the CSV.
 */
public class V2__seed_waste_codes extends BaseJavaMigration {

    private static final String CSV_PATH = "/seed/waste_codes.csv";

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        String sql = "INSERT INTO waste_codes (id, code, name, hazardous) VALUES (?, ?, ?, ?) "
                + "ON CONFLICT (code) DO NOTHING";

        try (InputStream in = getClass().getResourceAsStream(CSV_PATH)) {
            if (in == null) {
                throw new IllegalStateException("Seed file not found on classpath: " + CSV_PATH);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                 PreparedStatement ps = connection.prepareStatement(sql)) {

                String line;
                int batch = 0;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                        continue;
                    }
                    // Only split on the first two commas: name may contain commas is avoided by CSV design,
                    // but keep it robust by limiting the split to 3 parts.
                    String[] parts = trimmed.split(",", 3);
                    if (parts.length < 3) {
                        continue;
                    }
                    String code = parts[0].trim();
                    String name = parts[1].trim();
                    boolean hazardous = Boolean.parseBoolean(parts[2].trim());

                    ps.setObject(1, UUID.randomUUID());
                    ps.setString(2, code);
                    ps.setString(3, name);
                    ps.setBoolean(4, hazardous);
                    ps.addBatch();
                    batch++;
                }
                if (batch > 0) {
                    ps.executeBatch();
                }
            }
        }
    }
}
