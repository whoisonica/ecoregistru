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

    XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx"),
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
