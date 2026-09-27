package ro.ecoregistru.entity;

import java.util.List;

/**
 * D3.5 — declarația scrisă a gestionarului, luată înainte de inventariere (Normele OMFP 2861/2009 pct. 8 lit. a)):
 * cele șapte întrebări, în ordinea din Norme, fiecare cu răspunsul și detaliul la „Da”. Textele întrebărilor stau în
 * {@link #QUESTIONS}; se păstrează doar răspunsurile.
 */
public record KeeperDeclaration(List<Answer> answers) {

    public record Answer(boolean yes, String detail) {
    }

    /** Pct. 8 lit. a), verbatim, în ordine. */
    public static final List<String> QUESTIONS = List.of(
            "gestionează bunuri şi în alte locuri de depozitare",
            "în afara bunurilor entităţii respective are în gestiune şi alte bunuri aparţinând terţilor, primite cu sau fără documente",
            "are plusuri sau lipsuri în gestiune, despre a căror cantitate ori valoare are cunoştinţă",
            "are bunuri nerecepţionate sau care trebuie expediate (livrate), pentru care s-au întocmit documentele aferente",
            "a primit sau a eliberat bunuri fără documente legale",
            "deţine numerar sau alte hârtii de valoare rezultate din vânzarea bunurilor aflate în gestiunea sa",
            "are documente de primire-eliberare care nu au fost operate în evidenţa gestiunii sau care nu au fost predate la contabilitate");

    /** Indicele întrebării despre bunurile terților (aplicația nu le ține — avertisment). */
    public static final int THIRD_PARTY_GOODS = 1;
}
