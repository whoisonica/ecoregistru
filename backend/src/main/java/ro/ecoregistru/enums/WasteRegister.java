package ro.ecoregistru.enums;

import ro.ecoregistru.entity.WasteMovement;

import java.util.Collection;
import java.util.List;

/**
 * Which legal register a quantity belongs to. The two are distinct obligations with distinct
 * formats and distinct addressees, and a quantity belongs to exactly one of them.
 *
 * <p>{@code ANEXA_1} — waste the company generated in its own activity (HG 856/2002 art. 1
 * alin. (1)). Art. 2 alin. (1) makes this restrictive rather than permissive: an authorised
 * collector, transporter or treater keeps Anexa 1 <em>"numai pentru deşeurile generate în cadrul
 * activităţilor proprii"</em>.
 *
 * <p>{@code ART_48} — goods taken over from third parties and traded on: the monthly chronological
 * register of OUG 92/2021 art. 48 alin. (1), which HG 856 art. 2 alin. (2) reports separately, on
 * the authority's request. No official form is imposed for it.
 *
 * <p>Verbatim sources: docs/surse-oficiale.md §1.1 and §2.1.
 */
public enum WasteRegister {
    ANEXA_1,
    ART_48;

    /**
     * R1 (QA-FINAL-REPORT §5) — the one place a list of movements is cut down to a register. Every
     * reader that adds up or prints movements goes through here, and {@code RegisterSelectionInventoryTest}
     * fails on a reader that takes a movement list and never calls it: before, a fifth builder written
     * without the filter would have mixed goods taken over from third parties into Anexa 1, and no test
     * would have noticed.
     */
    public List<WasteMovement> select(Collection<WasteMovement> movements) {
        return movements.stream().filter(m -> m.getRegister() == this).toList();
    }
}
