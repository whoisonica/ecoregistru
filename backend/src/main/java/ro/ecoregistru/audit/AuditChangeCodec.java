package ro.ecoregistru.audit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * Lista de modificări, dusă şi adusă din coloana {@code changes} ca JSON.
 *
 * <p>Static şi cu un {@link ObjectMapper} propriu, fiindcă e chemat şi din
 * {@link AuditWriter#writePending()}, adică din mijlocul unui commit: o dependenţă injectată
 * acolo ar fi încă un fir de tras prin cercul descris în {@link AuditInterceptor}.
 *
 * <p>Citirea nu aruncă niciodată. Un rând de jurnal cu JSON stricat e un rând care tot spune
 * <em>cine</em> şi <em>când</em> — iar asta e mai mult decât jumătate din răspuns. O excepţie aici
 * ar face ecranul întreg să nu se deschidă, din cauza unui singur rând vechi.
 */
public final class AuditChangeCodec {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<PendingAudit.FieldChange>> LIST_OF_CHANGES =
            new TypeReference<>() {
            };

    private AuditChangeCodec() {
    }

    public static String write(List<PendingAudit.FieldChange> changes) {
        if (changes == null || changes.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(changes);
        } catch (Exception e) {
            return null;
        }
    }

    public static List<PendingAudit.FieldChange> read(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return MAPPER.readValue(json, LIST_OF_CHANGES);
        } catch (Exception e) {
            return List.of();
        }
    }
}
