package ro.ecoregistru.service.export;

import java.util.List;

/**
 * D4.7 — un raport fix al depozitului, înainte de a fi fișier: titlul, rândurile de sub el (firma, perioada, depozitul),
 * secțiuni de tabel cu coloane tipizate, note și semnăturile (doar pe PDF). Aceeași formă o scriu {@link DepotReportXlsx}
 * și {@link DepotReportPdf}, deci un raport nou e o metodă care umple modelul, nu încă o clasă POI.
 *
 * <p>Celulele sunt {@code String}, {@code BigDecimal} sau {@code Integer}; {@code null} = celulă goală. Datele se scriu
 * deja ca text („15.09.2026”), cum le citește omul.
 */
public record DepotReport(String title, List<String> heading, List<Section> sections, List<String> notes,
                          List<String> signatures) {

    /** Cum se scrie o coloană: text, kilograme (trei zecimale), lei (două), număr întreg. */
    public enum Kind { TEXT, KG, LEI, INT }

    public record Column(String name, Kind kind) {
        public static Column text(String name) {
            return new Column(name, Kind.TEXT);
        }

        public static Column kg(String name) {
            return new Column(name, Kind.KG);
        }

        public static Column lei(String name) {
            return new Column(name, Kind.LEI);
        }

        public static Column integer(String name) {
            return new Column(name, Kind.INT);
        }
    }

    /**
     * Un tabel. {@code total} e rândul îngroșat de la capăt (sau {@code null}). Fără rânduri, randările scriu „Nimic în
     * perioadă” (sau {@code empty}), ca un fișier gol să spună de ce e gol.
     */
    public record Section(String title, List<Column> columns, List<List<Object>> rows, List<Object> total,
                          String empty) {
        public Section(String title, List<Column> columns, List<List<Object>> rows, List<Object> total) {
            this(title, columns, rows, total, EMPTY);
        }
    }

    public static final String EMPTY = "Nimic în perioadă";
}
