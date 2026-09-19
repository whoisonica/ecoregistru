package ro.ecoregistru;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.entity.*;
import ro.ecoregistru.enums.*;
import ro.ecoregistru.repository.*;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * QA de lansare, generator — Faza 2: consistența documentelor, ca test de proprietate.
 *
 * <p>Pentru fiecare seed: o firmă GENERATOR nouă, 2 puncte de lucru × 4 coduri (unul periculos) × 3 ani
 * (2024–2026) de mișcări valide, toate prin API, ca un client: predări R/D, cu și fără partener, KG și
 * TONS cu 3 zecimale, încărcături cântărite la descărcare și completate ulterior, ștergeri, corecturi
 * care mută o mișcare pe alt cod, alt punct de lucru, altă lună sau alt an.
 *
 * <p><b>Oracolul</b> e un model în memorie al mișcărilor vii, socotit de mână: pe (punct, cod, an, lună)
 * valorificat, eliminat, predat, „de cântărit”. Din V58 un generator nu mai înregistrează generarea:
 * ea e ieșirea însăși (G03), deci generat = valorificat + eliminat, iar stocul e zero în fiecare lună.
 * Invariantul de stoc e deci degenerat la generator; se verifică totuși, pe ce întorc documentele.
 *
 * <p><b>Ce se compară</b>, pe fiecare (punct, cod, lună): {@code GET /evidences}, {@code /movements/totals},
 * {@code /movements/summary}, fișa Anexa 1 (PDF), evidența centralizată (PDF), exportul generic (xlsx și
 * pdf; xls e refuzat din construcție) și cele două PDF-uri din dosarul de control pe 3 ani. După fiecare
 * corectură (editare, ștergere, cântărire) {@code GET /evidences} pe anii atinși trebuie să fie proaspăt.
 *
 * <p>La eșec: seed-ul și cea mai scurtă secvență de operații care încă pică (micșorare lacomă, câte o
 * operație scoasă, rejucată pe o firmă nouă). Numărul de seed-uri: variabila de mediu {@code QA_SEEDS} (implicit 50).
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class DocumentConsistencyPropertyIT {

    private static final int[] YEARS = {2024, 2025, 2026};
    private static final String[] CODES = {"20 01 01", "20 01 02", "15 01 02", "13 02 08"};
    private static final String HAZARDOUS = "13 02 08";
    private static final String[] MONTHS = {"Ianuarie", "Februarie", "Martie", "Aprilie", "Mai", "Iunie",
            "Iulie", "August", "Septembrie", "Octombrie", "Noiembrie", "Decembrie"};
    private static final BigDecimal THOUSAND = BigDecimal.valueOf(1000);

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JwtService jwtService;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired CompanyRepository companyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired PartnerRepository partnerRepository;

    // ---------- proprietățile ----------

    /** Corecturile de dată rămân în același an. Controlul: fără BUG-031, totul trebuie să treacă. */
    @Test
    void everyDocumentAgreesWithTheMovements_correctionsWithinTheYear() throws Exception {
        runSeeds(false);
    }

    /** Aceleași seed-uri, cu corecturi care mută o mișcare și între ani (inclusiv 31.12 → ianuarie). */
    @Test
    void everyDocumentAgreesWithTheMovements_correctionsAcrossYears() throws Exception {
        runSeeds(true);
    }

    /**
     * Aceleași mutări între ani, dar după fiecare corectură se citește întâi anul cel mai nou: ocolirea
     * cunoscută a BUG-031 ({@code findEarliestStaleYear} prinde atunci anul vechi). Așa, restul
     * proprietăților se probează și pe mutările între ani, fără ca BUG-031 să le acopere.
     */
    @Test
    void everyDocumentAgreesWithTheMovements_correctionsAcrossYears_newestYearReadFirst() throws Exception {
        newestFirst = true;
        try {
            runSeeds(true);
        } finally {
            newestFirst = false;
        }
    }

    private boolean newestFirst;

    private void runSeeds(boolean acrossYears) throws Exception {
        int seeds = Integer.parseInt(Optional.ofNullable(System.getenv("QA_SEEDS")).orElse("50"));
        for (long seed = 1; seed <= seeds; seed++) {
            List<Op> ops = generate(seed, acrossYears);
            String failure = replay(ops);
            if (failure != null) {
                List<Op> shortest = shrink(ops);
                StringBuilder msg = new StringBuilder("seed " + seed + " (" + ops.size() + " operații): "
                        + failure + "\n\ncea mai scurtă secvență care pică (" + shortest.size() + "):\n");
                shortest.forEach(op -> msg.append("  ").append(op).append('\n'));
                msg.append("\ncăderea pe ea: ").append(replay(shortest));
                throw new AssertionError(msg.toString());
            }
        }
    }

    // ---------- operațiile ----------

    /** O mișcare, așa cum o vede clientul. {@code quantity} null = de cântărit la descărcare. */
    private record Mv(int point, int code, LocalDate date, BigDecimal quantity, Unit unit,
                      boolean recovered, Integer partner) {
        BigDecimal kg() {
            return quantity == null ? null : unit == Unit.TONS ? quantity.multiply(THOUSAND) : quantity;
        }

        @Override
        public String toString() {
            return "PL" + point + " " + CODES[code] + " " + date + " "
                    + (quantity == null ? "de cântărit" : quantity.toPlainString() + " " + unit)
                    + (recovered ? " R" : " D") + (partner == null ? " fără partener" : " partener" + partner);
        }
    }

    private sealed interface Op permits Create, Update, Delete, Weigh {}

    private record Create(int key, Mv mv) implements Op {
        public String toString() { return "creează #" + key + " " + mv; }
    }

    private record Update(int key, Mv mv) implements Op {
        public String toString() { return "corectează #" + key + " → " + mv; }
    }

    private record Delete(int key) implements Op {
        public String toString() { return "șterge #" + key; }
    }

    private record Weigh(int key, BigDecimal quantity, Unit unit) implements Op {
        public String toString() { return "cântărește #" + key + " " + quantity.toPlainString() + " " + unit; }
    }

    private List<Op> generate(long seed, boolean acrossYears) {
        Random rnd = new Random(seed);
        List<Op> ops = new ArrayList<>();
        Map<Integer, Mv> live = new LinkedHashMap<>();
        int next = 0;

        for (int i = 0; i < 36; i++) {
            Mv mv = randomMovement(rnd, rnd.nextInt(10) == 0);
            ops.add(new Create(next, mv));
            live.put(next++, mv);
        }
        // Capetele de an, ca mutările decembrie → ianuarie să aibă pe ce lucra.
        for (int year : YEARS) {
            Mv mv = new Mv(rnd.nextInt(2), rnd.nextInt(CODES.length), LocalDate.of(year, 12, 31),
                    quantity(rnd), rnd.nextBoolean() ? Unit.KG : Unit.TONS, true, 0);
            ops.add(new Create(next, mv));
            live.put(next++, mv);
        }

        for (int i = 0; i < 24; i++) {
            List<Integer> keys = new ArrayList<>(live.keySet());
            int key = keys.get(rnd.nextInt(keys.size()));
            Mv mv = live.get(key);
            int kind = rnd.nextInt(10);
            if (mv.quantity() == null) {
                if (kind < 7) {
                    BigDecimal q = quantity(rnd);
                    Unit u = rnd.nextBoolean() ? Unit.KG : Unit.TONS;
                    ops.add(new Weigh(key, q, u));
                    live.put(key, new Mv(mv.point(), mv.code(), mv.date(), q, u, mv.recovered(), mv.partner()));
                } else {
                    ops.add(new Delete(key));
                    live.remove(key);
                }
            } else if (kind < 2) {
                ops.add(new Delete(key));
                live.remove(key);
            } else {
                Mv moved = correct(rnd, mv, acrossYears);
                ops.add(new Update(key, moved));
                live.put(key, moved);
            }
        }
        return ops;
    }

    private Mv randomMovement(Random rnd, boolean unweighed) {
        int year = YEARS[rnd.nextInt(YEARS.length)];
        LocalDate date = LocalDate.of(year, 1 + rnd.nextInt(12), 1 + rnd.nextInt(28));
        boolean recovered = rnd.nextInt(4) != 0;
        Integer partner = unweighed || rnd.nextInt(4) != 0 ? rnd.nextInt(2) : null;
        return new Mv(rnd.nextInt(2), rnd.nextInt(CODES.length), date,
                unweighed ? null : quantity(rnd), rnd.nextBoolean() ? Unit.KG : Unit.TONS, recovered, partner);
    }

    /** Între 0,001 și ~5000, cu trei zecimale. */
    private static BigDecimal quantity(Random rnd) {
        return BigDecimal.valueOf(1 + rnd.nextInt(5_000_000), 3);
    }

    private Mv correct(Random rnd, Mv mv, boolean acrossYears) {
        return switch (rnd.nextInt(6)) {
            case 0 -> new Mv(1 - mv.point(), mv.code(), mv.date(), mv.quantity(), mv.unit(), mv.recovered(), mv.partner());
            case 1 -> new Mv(mv.point(), (mv.code() + 1 + rnd.nextInt(CODES.length - 1)) % CODES.length,
                    mv.date(), mv.quantity(), mv.unit(), mv.recovered(), mv.partner());
            case 2 -> new Mv(mv.point(), mv.code(), mv.date(), quantity(rnd),
                    rnd.nextBoolean() ? Unit.KG : Unit.TONS, mv.recovered(), mv.partner());
            case 3 -> new Mv(mv.point(), mv.code(), mv.date(), mv.quantity(), mv.unit(), !mv.recovered(), mv.partner());
            case 4 -> {
                // Altă lună în același an.
                LocalDate d = LocalDate.of(mv.date().getYear(), 1 + rnd.nextInt(12), 1 + rnd.nextInt(28));
                yield new Mv(mv.point(), mv.code(), d, mv.quantity(), mv.unit(), mv.recovered(), mv.partner());
            }
            default -> {
                if (!acrossYears) {
                    LocalDate d = mv.date().withMonth(mv.date().getMonthValue() == 12 ? 1 : 12);
                    yield new Mv(mv.point(), mv.code(), d, mv.quantity(), mv.unit(), mv.recovered(), mv.partner());
                }
                // Decembrie → ianuarie anul următor, sau înapoi.
                int y = mv.date().getYear();
                LocalDate d = y < YEARS[YEARS.length - 1] && (mv.date().getMonthValue() == 12 || rnd.nextBoolean())
                        ? LocalDate.of(y + 1, 1, 1 + rnd.nextInt(10))
                        : LocalDate.of(y - 1 < YEARS[0] ? y : y - 1, 12, 20 + rnd.nextInt(12));
                yield new Mv(mv.point(), mv.code(), d, mv.quantity(), mv.unit(), mv.recovered(), mv.partner());
            }
        };
    }

    // ---------- micșorarea ----------

    private List<Op> shrink(List<Op> ops) throws Exception {
        List<Op> current = new ArrayList<>(ops);
        boolean progress = true;
        while (progress) {
            progress = false;
            for (int i = current.size() - 1; i >= 0; i--) {
                List<Op> candidate = new ArrayList<>(current);
                candidate.remove(i);
                if (replay(candidate) != null) {
                    current = candidate;
                    progress = true;
                }
            }
        }
        return current;
    }

    // ---------- rejucarea, pe o firmă nouă ----------

    private final class Run {
        final String token;
        final UUID[] points = new UUID[2];
        final String[] pointNames = new String[2];
        final UUID[] partners = new UUID[2];
        final UUID[] codeIds = new UUID[CODES.length];
        final String[] codeNames = new String[CODES.length];
        final Map<Integer, UUID> ids = new HashMap<>();
        final Map<Integer, Mv> live = new HashMap<>();

        Run() {
            String suffix = UUID.randomUUID().toString().substring(0, 8);
            Company company = companyRepository.save(Company.builder()
                    .name("Proprietate " + suffix).cui(TestCui.random()).type(CompanyType.GENERATOR)
                    .active(true).createdAt(Instant.now()).build());
            token = jwtService.generateToken(appUserRepository.save(AppUser.builder()
                    .email("prop+" + suffix + "@demo.ro")
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .role(Role.ADMIN).company(company).enabled(true).createdAt(Instant.now()).build()));
            for (int p = 0; p < 2; p++) {
                pointNames[p] = (p == 0 ? "Punct Nord " : "Punct Sud ") + suffix;
                points[p] = workPointRepository.save(WorkPoint.builder().company(company)
                        .name(pointNames[p]).active(true).createdAt(Instant.now()).build()).getId();
                partners[p] = partnerRepository.save(Partner.builder().company(company)
                        .name("Colector " + p + " " + suffix).authorizationNumber("AM " + p + "/2024")
                        .type(PartnerType.COLLECTOR).supplier(true).active(true).createdAt(Instant.now())
                        .build()).getId();
            }
            for (int c = 0; c < CODES.length; c++) {
                WasteCode code = wasteCodeRepository.findByCode(CODES[c]).orElseThrow();
                codeIds[c] = code.getId();
                codeNames[c] = code.getName();
            }
        }
    }

    /** Null dacă toate proprietățile țin; altfel prima abatere, în cuvinte. */
    private String replay(List<Op> ops) throws Exception {
        Run run = new Run();
        for (Op op : ops) {
            String failure = apply(run, op);
            if (failure != null) {
                return "după „" + op + "”: " + failure;
            }
        }
        for (int year : YEARS) {
            String failure = checkYear(run, year);
            if (failure != null) {
                return failure;
            }
        }
        return checkDossier(run);
    }

    private String apply(Run run, Op op) throws Exception {
        Set<Integer> touched = new TreeSet<>();
        switch (op) {
            case Create c -> {
                String body = send(run, post("/api/v1/movements"), json(run, c.mv()), 200);
                run.ids.put(c.key(), UUID.fromString(objectMapper.readTree(body).get("id").asText()));
                run.live.put(c.key(), c.mv());
                return null; // o mișcare nouă e deja probată de EvidenceFreshnessIT
            }
            case Update u -> {
                Mv before = run.live.get(u.key());
                if (before == null || before.quantity() == null) {
                    return null; // cheia a căzut la micșorare
                }
                send(run, put("/api/v1/movements/" + run.ids.get(u.key())), json(run, u.mv()), 200);
                run.live.put(u.key(), u.mv());
                touched.add(before.date().getYear());
                touched.add(u.mv().date().getYear());
            }
            case Delete d -> {
                Mv before = run.live.remove(d.key());
                if (before == null) {
                    return null;
                }
                send(run, delete("/api/v1/movements/" + run.ids.get(d.key())), null, 204, 200);
                touched.add(before.date().getYear());
            }
            case Weigh w -> {
                Mv before = run.live.get(w.key());
                if (before == null || before.quantity() != null) {
                    return null;
                }
                send(run, post("/api/v1/movements/" + run.ids.get(w.key()) + "/weight"),
                        "{\"quantity\": " + w.quantity().toPlainString() + ", \"unit\": \"" + w.unit() + "\"}", 200);
                run.live.put(w.key(), new Mv(before.point(), before.code(), before.date(), w.quantity(), w.unit(),
                        before.recovered(), before.partner()));
                touched.add(before.date().getYear());
            }
        }
        // Proaspăt imediat: anii atinși, în ordinea crescătoare (cum îi deschide un om: întâi anul vechi).
        for (int year : newestFirst ? ((TreeSet<Integer>) touched).descendingSet() : touched) {
            String failure = checkEvidence(run, year);
            if (failure != null) {
                return failure;
            }
        }
        return null;
    }

    private String json(Run run, Mv mv) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("workPointId", run.points[mv.point()].toString());
        body.put("date", mv.date().toString());
        body.put("wasteCodeId", run.codeIds[mv.code()].toString());
        if (mv.quantity() != null) {
            body.put("quantity", mv.quantity());
        } else {
            body.put("weighedAtUnloading", true);
        }
        body.put("unit", mv.unit().name());
        body.put("physicalState", CODES[mv.code()].equals(HAZARDOUS) ? "LIQUID" : "SOLID");
        body.put("register", "ANEXA_1");
        body.put("operation", mv.recovered() ? "RECOVERED" : "DISPOSED");
        body.put("wasteDestination", mv.recovered() ? "Vr" : "I");
        body.put("storageType", "CT");
        body.put("transportMeans", "AN");
        body.put("packagingCategory", "SECONDARY");
        body.put("operationCode", mv.recovered() ? "R13" : "D10");
        if (mv.partner() != null) {
            body.put("partnerId", run.partners[mv.partner()].toString());
        }
        return objectMapper.writeValueAsString(body);
    }

    // ---------- oracolul ----------

    /** Pe (punct, cod, lună) al unui an: valorificat, eliminat, predat, de cântărit. */
    private record Cell4(BigDecimal recovered, BigDecimal disposed, BigDecimal handedOver, boolean awaiting) {
        static final Cell4 EMPTY = new Cell4(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, false);

        BigDecimal generated() {
            return recovered.add(disposed);
        }

        Cell4 plus(Mv mv) {
            BigDecimal kg = mv.kg();
            if (kg == null) {
                return new Cell4(recovered, disposed, handedOver, true);
            }
            return new Cell4(mv.recovered() ? recovered.add(kg) : recovered,
                    mv.recovered() ? disposed : disposed.add(kg),
                    mv.partner() != null ? handedOver.add(kg) : handedOver, awaiting);
        }

        boolean isEmpty() {
            return !awaiting && recovered.signum() == 0 && disposed.signum() == 0;
        }
    }

    private record Key(int point, int code, int month) {}

    private static Map<Key, Cell4> oracle(Run run, int year) {
        Map<Key, Cell4> out = new HashMap<>();
        for (Mv mv : run.live.values()) {
            if (mv.date().getYear() == year) {
                out.merge(new Key(mv.point(), mv.code(), mv.date().getMonthValue()), Cell4.EMPTY.plus(mv),
                        (a, b) -> a.plus(mv));
            }
        }
        return out;
    }

    /** Activitatea unui (punct, cod) pe an: fișa și rândul centralizat există doar pentru ele. */
    private static Set<List<Integer>> sheets(Map<Key, Cell4> oracle) {
        Set<List<Integer>> out = new TreeSet<>(Comparator.<List<Integer>>comparingInt(l -> l.get(0)).thenComparingInt(l -> l.get(1)));
        oracle.forEach((k, v) -> out.add(List.of(k.point(), k.code())));
        return out;
    }

    private static Cell4 at(Map<Key, Cell4> oracle, int point, int code, int month) {
        return oracle.getOrDefault(new Key(point, code, month), Cell4.EMPTY);
    }

    // ---------- verificările ----------

    private String checkYear(Run run, int year) throws Exception {
        String failure = checkEvidence(run, year);
        if (failure == null) failure = checkTotals(run, year);
        if (failure == null) failure = checkSummary(run, year);
        for (int p = 0; p < 2 && failure == null; p++) {
            byte[] pdf = bytes(run, "/api/v1/evidences/anexa1?year=" + year + "&workPointId=" + run.points[p]);
            failure = checkAnexa1(run, year, Golden.pdfText(pdf), Set.of(p), "fișa Anexa 1 " + year + " PL" + p);
        }
        for (int p = 0; p < 2 && failure == null; p++) {
            byte[] pdf = bytes(run, "/api/v1/evidences/declaratie-anuala?year=" + year + "&workPointId=" + run.points[p]);
            failure = checkDeclaration(run, year, Golden.pdfText(pdf), Set.of(p), "centralizata " + year + " PL" + p);
        }
        if (failure == null) failure = checkExportXlsx(run, year);
        if (failure == null) failure = checkExportPdf(run, year);
        return failure;
    }

    private String checkEvidence(Run run, int year) throws Exception {
        Map<Key, Cell4> oracle = oracle(run, year);
        JsonNode rows = objectMapper.readTree(send(run, get("/api/v1/evidences?year=" + year), null, 200));
        Map<Key, JsonNode> seen = new HashMap<>();
        Map<List<Integer>, BigDecimal[]> stock = new HashMap<>();
        for (JsonNode r : rows) {
            Key key = new Key(point(run, r.get("workPointId").asText()), code(r.get("wasteCode").asText()),
                    r.get("month").asInt());
            if (seen.put(key, r) != null) {
                return "/evidences " + year + ": două rânduri pe " + key;
            }
        }
        // Invariantul de stoc lună de lună, pe ce a întors serverul.
        for (Map.Entry<Key, JsonNode> e : new TreeMap<Key, JsonNode>(Comparator.comparingInt(Key::point)
                .thenComparingInt(Key::code).thenComparingInt(Key::month)) {{ putAll(seen); }}.entrySet()) {
            JsonNode r = e.getValue();
            List<Integer> sheet = List.of(e.getKey().point(), e.getKey().code());
            BigDecimal[] prev = stock.computeIfAbsent(sheet, s -> new BigDecimal[]{BigDecimal.ZERO});
            BigDecimal expected = prev[0].add(dec(r, "totalGenerated")).subtract(dec(r, "totalRecovered"))
                    .subtract(dec(r, "totalDisposed")).subtract(dec(r, "totalUnclassifiedOut"));
            if (expected.compareTo(dec(r, "closingStock")) != 0) {
                return "/evidences " + year + " " + e.getKey() + ": stocul " + dec(r, "closingStock")
                        + " nu urmează din luna trecută (" + expected + ")";
            }
            prev[0] = dec(r, "closingStock");
        }
        Set<Key> keys = new HashSet<>(oracle.keySet());
        keys.addAll(seen.keySet());
        for (Key key : keys) {
            Cell4 want = oracle.getOrDefault(key, Cell4.EMPTY);
            JsonNode got = seen.get(key);
            if (got == null) {
                if (!want.isEmpty()) return "/evidences " + year + ": lipsește rândul " + key + ", aștept " + want;
                continue;
            }
            String where = "/evidences " + year + " PL" + key.point() + " " + CODES[key.code()] + " luna " + key.month();
            String diff = differ(where, "generat", want.generated(), dec(got, "totalGenerated"))
                    + differ(where, "valorificat", want.recovered(), dec(got, "totalRecovered"))
                    + differ(where, "eliminat", want.disposed(), dec(got, "totalDisposed"))
                    + differ(where, "predat", want.handedOver(), dec(got, "totalHandedOver"))
                    + differ(where, "neclasificat", BigDecimal.ZERO, dec(got, "totalUnclassifiedOut"))
                    + differ(where, "stoc", BigDecimal.ZERO, dec(got, "closingStock"));
            if (want.awaiting() != got.get("awaitingWeighing").asBoolean()) {
                diff += where + ": de cântărit " + got.get("awaitingWeighing") + ", aștept " + want.awaiting() + "; ";
            }
            if (!diff.isEmpty()) return diff;
        }
        return null;
    }

    private String checkTotals(Run run, int year) throws Exception {
        for (int p = 0; p < 2; p++) {
            for (int m = 1; m <= 12; m++) {
                BigDecimal kg = BigDecimal.ZERO, rec = BigDecimal.ZERO, dis = BigDecimal.ZERO;
                long rows = 0, awaiting = 0;
                for (Mv mv : run.live.values()) {
                    if (mv.point() == p && mv.date().getYear() == year && mv.date().getMonthValue() == m) {
                        rows++;
                        if (mv.kg() == null) { awaiting++; continue; }
                        kg = kg.add(mv.kg());
                        if (mv.recovered()) rec = rec.add(mv.kg()); else dis = dis.add(mv.kg());
                    }
                }
                JsonNode t = objectMapper.readTree(send(run, get("/api/v1/movements/totals?year=" + year + "&month=" + m
                        + "&workPointId=" + run.points[p]), null, 200));
                String where = "/movements/totals " + year + "-" + m + " PL" + p;
                String diff = differ(where, "kg", kg, dec(t, "quantityKg"))
                        + differ(where, "valorificat", rec, dec(t, "recoveredKg"))
                        + differ(where, "eliminat", dis, dec(t, "disposedKg"));
                if (t.get("rows").asLong() != rows) diff += where + ": rânduri " + t.get("rows") + ", aștept " + rows + "; ";
                if (t.get("awaitingWeighing").asLong() != awaiting) {
                    diff += where + ": de cântărit " + t.get("awaitingWeighing") + ", aștept " + awaiting + "; ";
                }
                if (!diff.isEmpty()) return diff;
            }
        }
        return null;
    }

    private String checkSummary(Run run, int year) throws Exception {
        for (int m = 1; m <= 12; m++) {
            BigDecimal kg = BigDecimal.ZERO;
            long rows = 0;
            for (Mv mv : run.live.values()) {
                if (mv.date().getYear() == year && mv.date().getMonthValue() == m) {
                    rows++;
                    if (mv.kg() != null) kg = kg.add(mv.kg());
                }
            }
            JsonNode s = objectMapper.readTree(send(run, get("/api/v1/movements/summary?year=" + year + "&month=" + m), null, 200));
            String where = "/movements/summary " + year + "-" + m;
            String diff = differ(where, "kg", kg, dec(s, "quantityKg"));
            if (s.get("movements").asLong() != rows) diff += where + ": mișcări " + s.get("movements") + ", aștept " + rows + "; ";
            if (!diff.isEmpty()) return diff;
        }
        return null;
    }

    private static final Pattern CHAPTER_ONE = Pattern.compile("1\\. GENERAREA DE");

    /**
     * Fișa: o pagină (sau mai multe) pe (punct, cod), cu cap. 1 pe douăsprezece rânduri
     * „N Luna generat valorificat eliminat stoc”. Fișa se recunoaște după ultimul punct și ultimul cod
     * tipărite în antet, înaintea titlului „1. GENERAREA DEŞEURILOR”; cap. 1 se citește până la cap. 2.
     */
    private String checkAnexa1(Run run, int year, String text, Set<Integer> points, String doc) {
        Map<Key, Cell4> oracle = oracle(run, year);
        Set<List<Integer>> expected = new HashSet<>();
        for (List<Integer> s : sheets(oracle)) if (points.contains(s.get(0))) expected.add(s);

        Matcher starts = CHAPTER_ONE.matcher(text);
        List<Integer> positions = new ArrayList<>();
        while (starts.find()) positions.add(starts.start());
        Set<List<Integer>> found = new HashSet<>();
        for (int i = 0; i < positions.size(); i++) {
            int at = positions.get(i);
            int next = text.indexOf("2. STOCAREA", at);
            int end = next > 0 ? next : text.length();
            String before = text.substring(0, at);
            int point = lastOf(before, run.pointNames);
            int code = lastOf(before, CODES);
            if (point < 0 || code < 0) return doc + ": fișa nr. " + (i + 1) + " fără punct sau cod în antet";
            List<Integer> sheet = List.of(point, code);
            if (!found.add(sheet)) return doc + ": două fișe pe PL" + point + " " + CODES[code];
            String body = text.substring(at, end);
            BigDecimal gen = BigDecimal.ZERO, rec = BigDecimal.ZERO, dis = BigDecimal.ZERO;
            for (int m = 1; m <= 12; m++) {
                Cell4 c = at(oracle, point, code, m);
                String row = m + " " + MONTHS[m - 1] + " " + kg3(c.generated()) + " " + kg3(c.recovered()) + " "
                        + kg3(c.disposed()) + " " + kg3(BigDecimal.ZERO);
                if (!body.contains(row)) {
                    return doc + " PL" + point + " " + CODES[code] + ": lipsește rândul „" + row + "”"
                            + excerpt(body, m + " " + MONTHS[m - 1]);
                }
                gen = gen.add(c.generated()); rec = rec.add(c.recovered()); dis = dis.add(c.disposed());
            }
            String total = "TOTAL AN" + kg3(gen) + kg3(rec) + kg3(dis) + kg3(BigDecimal.ZERO);
            if (!Golden.flat(body).contains(Golden.flat(total))) {
                return doc + " PL" + point + " " + CODES[code] + ": lipsește „" + total + "”";
            }
        }
        if (!found.equals(expected)) {
            return doc + ": fișe " + describe(found) + ", aștept " + describe(expected);
        }
        return null;
    }

    /**
     * Centralizata: pe fiecare pagină un punct de lucru, pe rând un cod: cod, denumire, stoc la 01.01,
     * generat, valorificat, eliminat, stoc. Comparat pe textul strâns, rând întreg.
     */
    private String checkDeclaration(Run run, int year, String text, Set<Integer> points, String doc) {
        Map<Key, Cell4> oracle = oracle(run, year);
        Set<List<Integer>> expected = sheets(oracle);
        String[] pages = text.split("Punct de lucru:");
        for (List<Integer> s : expected) {
            int point = s.get(0), code = s.get(1);
            if (!points.contains(point)) continue;
            BigDecimal gen = BigDecimal.ZERO, rec = BigDecimal.ZERO, dis = BigDecimal.ZERO;
            boolean awaiting = false;
            for (int m = 1; m <= 12; m++) {
                Cell4 c = at(oracle, point, code, m);
                gen = gen.add(c.generated()); rec = rec.add(c.recovered()); dis = dis.add(c.disposed());
                awaiting |= c.awaiting();
            }
            String label = CODES[code] + (CODES[code].equals(HAZARDOUS) ? "*" : "");
            // BUG-032: o încărcătură de cântărit pune „(**)” după cifra din „Generat”.
            String row = Golden.flat(label + run.codeNames[code] + kg3(BigDecimal.ZERO) + kg3(gen)
                    + (awaiting ? "(**)" : "") + kg3(rec)
                    + kg3(dis) + kg3(BigDecimal.ZERO));
            boolean onItsPage = false;
            for (String page : pages) {
                String head = page.length() > 200 ? page.substring(0, 200) : page;
                if (head.contains(run.pointNames[point]) && Golden.flat(page).contains(row)) {
                    onItsPage = true;
                }
            }
            if (!onItsPage) {
                return doc + ": lipsește rândul PL" + point + " „" + row + "”";
            }
        }
        for (int point : points) {
            for (int code = 0; code < CODES.length; code++) {
                if (!expected.contains(List.of(point, code))) {
                    for (String page : pages) {
                        String head = page.length() > 200 ? page.substring(0, 200) : page;
                        if (head.contains(run.pointNames[point]) && page.contains(CODES[code])) {
                            return doc + ": rând pe PL" + point + " " + CODES[code] + ", fără nicio mișcare în " + year;
                        }
                    }
                }
            }
        }
        return null;
    }

    private String checkExportXlsx(Run run, int year) throws Exception {
        Map<Key, Cell4> oracle = oracle(run, year);
        byte[] xlsx = bytes(run, "/api/v1/evidences/export?year=" + year + "&format=xlsx");
        Map<Key, List<Object>> seen = new HashMap<>();
        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(xlsx))) {
            Sheet sh = wb.getSheetAt(0);
            for (int r = 0; r <= sh.getLastRowNum(); r++) {
                Row row = sh.getRow(r);
                if (row == null || row.getCell(9) == null || row.getCell(9).getCellType() != CellType.NUMERIC) continue;
                int point = indexOf(row.getCell(0).getStringCellValue(), run.pointNames);
                int month = Arrays.asList(MONTHS).indexOf(row.getCell(1).getStringCellValue()) + 1;
                int code = code(row.getCell(2).getStringCellValue());
                if (point < 0 || month < 1 || code < 0) return "export xlsx " + year + ": rând necunoscut " + r;
                List<Object> cells = Golden.cells(sh, r, 4, 9);
                if (seen.put(new Key(point, code, month), cells) != null) {
                    return "export xlsx " + year + ": două rânduri pe PL" + point + " " + CODES[code] + " luna " + month;
                }
            }
        }
        Set<Key> keys = new HashSet<>(oracle.keySet());
        keys.addAll(seen.keySet());
        for (Key key : keys) {
            Cell4 want = oracle.getOrDefault(key, Cell4.EMPTY);
            List<Object> got = seen.get(key);
            String where = "export xlsx " + year + " PL" + key.point() + " " + CODES[key.code()] + " luna " + key.month();
            if (got == null) {
                if (want.isEmpty()) continue;
                return where + ": lipsește";
            }
            List<Object> expected = List.of(CODES[key.code()].equals(HAZARDOUS) ? "Da" : "Nu",
                    want.generated().doubleValue(), want.recovered().doubleValue(), want.disposed().doubleValue(), 0d, 0d);
            if (!got.equals(expected)) return where + ": " + got + ", aștept " + expected;
        }
        return null;
    }

    private String checkExportPdf(Run run, int year) throws Exception {
        Map<Key, Cell4> oracle = oracle(run, year);
        String text = Golden.flat(Golden.pdfText(bytes(run, "/api/v1/evidences/export?year=" + year + "&format=pdf")));
        DecimalFormat ro = new DecimalFormat("#,##0.###", DecimalFormatSymbols.getInstance(Locale.of("ro", "RO")));
        for (Map.Entry<Key, Cell4> e : oracle.entrySet()) {
            Key k = e.getKey();
            Cell4 c = e.getValue();
            String row = Golden.flat(run.pointNames[k.point()] + MONTHS[k.month() - 1] + CODES[k.code()]
                    + run.codeNames[k.code()] + (CODES[k.code()].equals(HAZARDOUS) ? "Da" : "Nu")
                    + ro.format(c.generated()) + ro.format(c.recovered()) + ro.format(c.disposed()) + "0" + "0");
            if (!text.contains(row)) return "export pdf " + year + ": lipsește rândul „" + row + "”";
        }
        return null;
    }

    /** Dosarul pe 3 ani: fișa și centralizata fiecărui an, citite din arhivă, pe toate punctele. */
    private String checkDossier(Run run) throws Exception {
        int last = YEARS[YEARS.length - 1];
        byte[] zip = bytes(run, "/api/v1/audit-file?year=" + last + "&years=" + YEARS.length);
        Map<String, byte[]> entries = new HashMap<>();
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
            for (ZipEntry e; (e = in.getNextEntry()) != null; ) entries.put(e.getName(), in.readAllBytes());
        }
        for (int year : YEARS) {
            byte[] sheet = entries.get(year + "/rapoarte/evidenta-gestiunii-deseurilor-" + year + ".pdf");
            byte[] central = entries.get(year + "/rapoarte/evidenta-centralizata-" + year + ".pdf");
            if (sheet == null || central == null) return "dosar: lipsesc rapoartele pe " + year + " în " + entries.keySet();
            String failure = checkAnexa1(run, year, Golden.pdfText(sheet), Set.of(0, 1), "dosar, fișa " + year);
            if (failure == null) failure = checkDeclaration(run, year, Golden.pdfText(central), Set.of(0, 1), "dosar, centralizata " + year);
            if (failure != null) return failure;
        }
        return null;
    }

    // ---------- mecanica ----------

    private String send(Run run, MockHttpServletRequestBuilder req, String body, int... ok) throws Exception {
        req.header("Authorization", "Bearer " + run.token);
        if (body != null) req.contentType(MediaType.APPLICATION_JSON).content(body);
        MvcResult r = mockMvc.perform(req).andReturn();
        int status = r.getResponse().getStatus();
        if (Arrays.stream(ok).noneMatch(s -> s == status)) {
            throw new AssertionError(req.buildRequest(null).getMethod() + " → " + status + ": "
                    + r.getResponse().getContentAsString() + "\ncorp: " + body);
        }
        return r.getResponse().getContentAsString();
    }

    private byte[] bytes(Run run, String url) throws Exception {
        MvcResult r = mockMvc.perform(get(url).header("Authorization", "Bearer " + run.token)).andReturn();
        assertThat(r.getResponse().getStatus()).as(url).isEqualTo(200);
        return r.getResponse().getContentAsByteArray();
    }

    private static BigDecimal dec(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? BigDecimal.ZERO : v.decimalValue();
    }

    private static String differ(String where, String what, BigDecimal want, BigDecimal got) {
        return want.compareTo(got) == 0 ? "" : where + ": " + what + " " + got.toPlainString() + ", aștept "
                + want.toPlainString() + "; ";
    }

    private static String kg3(BigDecimal kg) {
        return kg.setScale(3, RoundingMode.UNNECESSARY).toPlainString();
    }

    private static int point(Run run, String id) {
        for (int p = 0; p < 2; p++) if (run.points[p].toString().equals(id)) return p;
        return -1;
    }

    private static int code(String code) {
        return Arrays.asList(CODES).indexOf(code.replace("*", "").trim());
    }

    private static int indexOf(String value, String[] options) {
        return Arrays.asList(options).indexOf(value);
    }

    /** Care dintre opțiuni apare ultima în text; -1 dacă niciuna. */
    private static int lastOf(String text, String[] options) {
        int best = -1, at = -1;
        for (int i = 0; i < options.length; i++) {
            int idx = text.lastIndexOf(options[i]);
            if (idx > at) { at = idx; best = i; }
        }
        return best;
    }

    private static String describe(Set<List<Integer>> sheets) {
        return sheets.stream().map(s -> "PL" + s.get(0) + " " + CODES[s.get(1)]).sorted().toList().toString();
    }

    private static String excerpt(String body, String label) {
        int i = body.indexOf(label);
        return i < 0 ? " (luna lipsește cu totul)" : "; pe fișă: „" + body.substring(i, Math.min(body.length(), i + 60)).split("\n")[0] + "”";
    }
}
