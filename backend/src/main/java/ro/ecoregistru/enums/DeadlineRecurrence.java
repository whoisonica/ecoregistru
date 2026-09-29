package ro.ecoregistru.enums;

/** Cât de des revine un termen propriu (V81). Următoarea apariție se creează la bifare. */
public enum DeadlineRecurrence {
    ONCE(0),
    MONTHLY(1),
    QUARTERLY(3),
    SEMIANNUAL(6),
    ANNUAL(12);

    private final int months;

    DeadlineRecurrence(int months) {
        this.months = months;
    }

    /** Lunile dintre două apariții; 0 pentru un termen care nu se repetă. */
    public int months() {
        return months;
    }
}
