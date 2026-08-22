package ro.ecoregistru.enums;

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
    ART_48
}
