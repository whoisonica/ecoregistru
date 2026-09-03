package ro.ecoregistru.service.export;

import lombok.Getter;
import ro.ecoregistru.exception.BadRequestException;

import static ro.ecoregistru.exception.ErrorMessageEnum.EXPORT_FORMAT_UNSUPPORTED;

/**
 * Supported download formats for the generic evidence export. Each carries the HTTP
 * content type and the filename extension used in Content-Disposition.
 */
@Getter
public enum ExportFormat {

    /**
     * OOXML, the modern spreadsheet. Used by the <em>generic</em> evidence export, which is an
     * unofficial summary and may take the convenient format.
     */
    XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx"),

    /**
     * BIFF8, the legacy Excel format — and the one Ordinul 794/2012 art. 6 names on sight for the
     * packaging declaration: <em>"Datele de raportare se transmit în format electronic «.xls»"</em>.
     * Separate from {@link #XLSX} on purpose: only the filing the act constrains uses it, so the
     * generic export is not dragged back to a 1997 format for no reason.
     */
    XLS("application/vnd.ms-excel", "xls"),

    PDF("application/pdf", "pdf");

    private final String contentType;
    private final String extension;

    ExportFormat(String contentType, String extension) {
        this.contentType = contentType;
        this.extension = extension;
    }

    /** Parses the {@code format} query param (case-insensitive); unknown values are 400s. */
    public static ExportFormat fromParam(String format) {
        if (format != null) {
            for (ExportFormat f : values()) {
                if (f.extension.equalsIgnoreCase(format)) {
                    return f;
                }
            }
        }
        throw new BadRequestException(EXPORT_FORMAT_UNSUPPORTED);
    }
}
