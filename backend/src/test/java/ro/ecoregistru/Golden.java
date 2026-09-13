package ro.ecoregistru;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.util.ArrayList;
import java.util.List;

/**
 * P1.12 — the readers the golden tests share. A golden test reads a figure off the printed page or
 * the rendered sheet, never off the DTO behind it: the builders and the generators are separate
 * code paths, and every older test stopped at the builder.
 */
final class Golden {

    private Golden() {
    }

    /** Every page of the PDF, as the extractor sees it. */
    static String pdfText(byte[] pdf) throws Exception {
        PdfReader reader = new PdfReader(pdf);
        try {
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            StringBuilder all = new StringBuilder();
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                all.append(extractor.getTextFromPage(page)).append('\n');
            }
            return all.toString();
        } finally {
            reader.close();
        }
    }

    /**
     * Text with every whitespace removed, so a rubric and the value printed under it compare as one
     * string. The extractor breaks a line wherever a cell wraps, and puts a space between some
     * neighbouring cells and not others — "Sticlă 80.000" but "Hârtie carton500.000" in the same
     * table — so the only stable comparison is the squeezed one, applied to both sides.
     *
     * <p>Also undoes the extractor's one mis-decoding: a Cp1250 "ă" comes back as "ª". And folds the
     * comma-below ş/ţ onto the cedilla forms the generators print, so expected text can be written
     * either way.
     */
    static String flat(String text) {
        return text.replaceAll("\\s+", "")
                .replace('ª', 'ă')
                .replace('ș', 'ş').replace('Ș', 'Ş')
                .replace('ț', 'ţ').replace('Ț', 'Ţ');
    }

    /** The index of the first row whose column 1 reads exactly {@code label}. */
    static int rowOf(Sheet sh, String label) {
        for (int r = 0; r <= sh.getLastRowNum(); r++) {
            Row row = sh.getRow(r);
            Cell cell = row == null ? null : row.getCell(1);
            if (cell != null && cell.getCellType() == CellType.STRING
                    && cell.getStringCellValue().equals(label)) {
                return r;
            }
        }
        throw new AssertionError("no row labelled " + label);
    }

    /**
     * Cells {@code from..to} of one row, in order: a number as a {@code Double}, text as itself, and
     * an empty cell as {@code ""}. Position by position, so an empty cell is a place and not a gap —
     * the one thing the squeezed PDF text cannot tell apart.
     */
    static List<Object> cells(Sheet sh, int r, int from, int to) {
        List<Object> values = new ArrayList<>();
        Row row = sh.getRow(r);
        for (int c = from; c <= to; c++) {
            Cell cell = row == null ? null : row.getCell(c);
            if (cell == null) {
                values.add("");
            } else if (cell.getCellType() == CellType.NUMERIC) {
                values.add(cell.getNumericCellValue());
            } else if (cell.getCellType() == CellType.STRING) {
                values.add(cell.getStringCellValue());
            } else {
                values.add("");
            }
        }
        return values;
    }
}
