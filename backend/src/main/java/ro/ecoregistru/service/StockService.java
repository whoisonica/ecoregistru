package ro.ecoregistru.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ecoregistru.controller.response.StockResponse;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.repository.WasteMovementRepository;
import ro.ecoregistru.repository.WasteMovementRepository.StockLine;
import ro.ecoregistru.repository.WorkPointRepository;
import ro.ecoregistru.security.TenantContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static ro.ecoregistru.exception.ErrorMessageEnum.WORK_POINT_NOT_FOUND;

/**
 * F3 — stocul, calculat din liniile care contează în registre (nu dintr-un tabel de solduri: regula din QA, „nu
 * optimizăm pe presupunere”). Pe depozit: soldul la sfârșitul zilei, marfa în tranzit spre el și cea angajată. Pe firmă:
 * suma depozitelor pe care utilizatorul le vede (D2.4), plus ce e în tranzit între ele — marfa plecată din A și
 * nerecepționată la B e tot a firmei.
 *
 * <p>D3.2 — un sold negativ nu blochează nimic (omul poate introduce intrarea după), dar se vede: la finalizarea ieșirii
 * și în raportul „de corectat”.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StockService {

    WasteMovementRepository movementRepository;
    WorkPointRepository workPointRepository;
    DepotAccess depotAccess;
    ro.ecoregistru.repository.StockThresholdRepository thresholdRepository;
    ro.ecoregistru.repository.AuthorizedLimitRepository limitRepository;

    /** OG 2/2021 art. 3 alin. (2) lit. b): 1 an înainte de eliminare, 3 ani înainte de valorificare (§8, §18.6). */
    static final int ONE_YEAR_DAYS = 365;
    static final int THREE_YEARS_DAYS = 3 * 365;

    @Transactional(readOnly = true)
    public StockResponse stock(UUID workPointId, LocalDate date) {
        UUID tenantId = TenantContext.require();
        LocalDate at = date == null ? DeadlineService.today() : date;
        if (workPointId != null) {
            depotAccess.require(workPointRepository.findByIdAndCompany_Id(workPointId, tenantId)
                    .orElseThrow(() -> new NotFoundException(WORK_POINT_NOT_FOUND)));
        }
        Set<UUID> allowed = depotAccess.allowed();
        boolean today = !at.isBefore(DeadlineService.today());

        Map<String, Acc> rows = new LinkedHashMap<>();
        for (StockLine l : visible(movementRepository.stockAt(tenantId, workPointId, at), allowed)) {
            acc(rows, l).stock = acc(rows, l).stock.add(l.getKg());
        }
        for (StockLine l : visible(movementRepository.inTransitAt(tenantId, workPointId, at), allowed)) {
            acc(rows, l).inTransit = acc(rows, l).inTransit.add(l.getKg());
        }
        // Angajatul e al zilei de azi: o ieșire în lucru nu spune nimic despre o dată din trecut.
        if (today) {
            for (StockLine l : visible(movementRepository.committed(tenantId, workPointId), allowed)) {
                acc(rows, l).committed = acc(rows, l).committed.add(l.getKg());
            }
        }
        // Pe firmă, ce e în tranzit e tot al firmei: intră în sold (plecarea din A l-a scos din A).
        if (workPointId == null) {
            rows.values().forEach(a -> {
                a.stock = a.stock.add(a.inTransit);
                a.inTransit = BigDecimal.ZERO;
            });
        }

        // D3.3 și D3.4 — pragurile, vechimea și limitele sunt ale unui depozit.
        Map<UUID, ro.ecoregistru.entity.StockThreshold> thresholds = new java.util.HashMap<>();
        Map<UUID, Integer> ages = Map.of();
        List<ro.ecoregistru.entity.AuthorizedLimit> limits = List.of();
        if (workPointId != null) {
            thresholdRepository.findAllByCompanyIdAndWorkPoint_Id(tenantId, workPointId)
                    .forEach(t -> thresholds.put(t.getArticle().getId(), t));
            ages = oldestLotDays(tenantId, workPointId, at);
            limits = limitRepository.findAllByCompanyIdAndWorkPoint_IdOrderByCreatedAtAsc(tenantId, workPointId);
        }

        List<StockResponse.Row> out = new ArrayList<>();
        for (Acc a : rows.values()) {
            if (a.stock.signum() == 0 && a.inTransit.signum() == 0 && a.committed.signum() == 0) {
                continue;
            }
            var threshold = a.articleId == null ? null : thresholds.get(a.articleId);
            BigDecimal min = threshold == null ? null : threshold.getMinKg();
            BigDecimal max = threshold == null ? null : threshold.getMaxKg();
            Integer days = a.stock.signum() > 0 ? ages.get(a.codeId) : null;
            out.add(new StockResponse.Row(a.articleId, a.articleName, a.codeId, a.code, a.name, a.hazardous,
                    a.stock, a.inTransit, a.committed, a.stock.subtract(a.committed), a.stock.signum() < 0,
                    min, max, min != null && a.stock.compareTo(min) < 0, max != null && a.stock.compareTo(max) > 0,
                    days, ageFlag(days, a.codeId, limits)));
        }
        out.sort(Comparator.comparing(StockResponse.Row::wasteCode)
                .thenComparing(r -> r.articleName() == null ? "" : r.articleName()));
        return new StockResponse(at, workPointId, out, (int) out.stream().filter(StockResponse.Row::negative).count(),
                limitStates(limits, out, tenantId, workPointId, at));
    }

    /**
     * D3.4 — vechimea stocului: pe fiecare cod, intrările devin loturi, iar ieșirile le consumă pe cele mai vechi
     * (FIFO). Rămâne câte zile are cel mai vechi lot încă pe amplasament.
     */
    Map<UUID, Integer> oldestLotDays(UUID tenantId, UUID workPointId, LocalDate at) {
        Map<UUID, java.util.ArrayDeque<Object[]>> lots = new java.util.HashMap<>();
        for (var move : movementRepository.stockMoves(tenantId, workPointId, at)) {
            var queue = lots.computeIfAbsent(move.getWasteCodeId(), k -> new java.util.ArrayDeque<>());
            BigDecimal kg = move.getKg();
            if (kg.signum() > 0) {
                queue.addLast(new Object[]{move.getDate(), kg});
                continue;
            }
            BigDecimal out = kg.negate();
            while (out.signum() > 0 && !queue.isEmpty()) {
                Object[] lot = queue.peekFirst();
                BigDecimal left = (BigDecimal) lot[1];
                if (left.compareTo(out) <= 0) {
                    out = out.subtract(left);
                    queue.pollFirst();
                } else {
                    lot[1] = left.subtract(out);
                    out = BigDecimal.ZERO;
                }
            }
        }
        Map<UUID, Integer> days = new java.util.HashMap<>();
        lots.forEach((code, queue) -> {
            if (!queue.isEmpty()) {
                days.put(code, (int) java.time.temporal.ChronoUnit.DAYS.between((LocalDate) queue.peekFirst()[0], at));
            }
        });
        return days;
    }

    /** Durata din autorizație are întâietate; fără ea, plafoanele din lege. */
    static String ageFlag(Integer days, UUID codeId, List<ro.ecoregistru.entity.AuthorizedLimit> limits) {
        if (days == null) {
            return null;
        }
        for (var l : limits) {
            if ("STORED".equals(l.getKind()) && l.getMaxStorageDays() != null
                    && (l.getWasteCode() == null || l.getWasteCode().getId().equals(codeId))
                    && days > l.getMaxStorageDays()) {
                return "LIMIT";
            }
        }
        return days > THREE_YEARS_DAYS ? "THREE_YEARS" : days > ONE_YEAR_DAYS ? "ONE_YEAR" : null;
    }

    /**
     * D3.4 — starea fiecărei limite: stocul „la un moment dat” (t/kg) se compară cu soldul de acum, pe cod sau pe tot
     * depozitul; ieșirile „pe an” cu ieșirile din anul datei (predări, ieșiri fără cod și transferuri trimise: ce a
     * plecat de pe amplasament). Restul (m³, tratat, pe lună) se arată fără comparație.
     */
    private List<StockResponse.Limit> limitStates(List<ro.ecoregistru.entity.AuthorizedLimit> limits,
                                                  List<StockResponse.Row> rows, UUID tenantId, UUID workPointId,
                                                  LocalDate at) {
        if (limits.isEmpty()) {
            return List.of();
        }
        Map<UUID, BigDecimal> exits = null;
        List<StockResponse.Limit> out = new ArrayList<>();
        for (var l : limits) {
            UUID code = l.getWasteCode() == null ? null : l.getWasteCode().getId();
            BigDecimal limitKg = "M3".equals(l.getUnit()) ? null
                    : "T".equals(l.getUnit()) ? l.getQuantity().multiply(BigDecimal.valueOf(1000)) : l.getQuantity();
            BigDecimal used = null;
            if (limitKg != null && "STORED".equals(l.getKind()) && "AT_ONCE".equals(l.getPeriod())) {
                used = rows.stream().filter(r -> code == null || code.equals(r.wasteCodeId()))
                        .map(StockResponse.Row::stockKg).filter(kg -> kg.signum() > 0)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            } else if (limitKg != null && "OUTPUT".equals(l.getKind()) && "YEAR".equals(l.getPeriod())) {
                if (exits == null) {
                    exits = exitsOfYear(tenantId, workPointId, at);
                }
                used = code == null ? exits.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                        : exits.getOrDefault(code, BigDecimal.ZERO);
            }
            out.add(new StockResponse.Limit(l.getId(), l.getKind(), code,
                    l.getWasteCode() == null ? null : l.getWasteCode().getCode(), l.getQuantity(), l.getUnit(),
                    l.getPeriod(), l.getMaxStorageDays(), l.isApproximate(), l.getNote(), used != null, used,
                    used != null && used.compareTo(limitKg) > 0));
        }
        return out;
    }

    private Map<UUID, BigDecimal> exitsOfYear(UUID tenantId, UUID workPointId, LocalDate at) {
        Map<UUID, BigDecimal> exits = new java.util.HashMap<>();
        LocalDate from = at.withDayOfYear(1);
        for (var move : movementRepository.stockMoves(tenantId, workPointId, at)) {
            if (move.getKg().signum() < 0 && !move.getDate().isBefore(from)) {
                exits.merge(move.getWasteCodeId(), move.getKg().negate(), BigDecimal::add);
            }
        }
        return exits;
    }

    /**
     * D3.2 — soldurile unui depozit care au ieșit negative la data dată, pe perechile sortiment × cod atinse de o
     * operațiune; pentru avertismentul de la finalizare.
     */
    @Transactional(readOnly = true)
    public List<StockResponse.Row> negativeAfter(UUID workPointId, LocalDate date, Set<String> keys) {
        UUID tenantId = TenantContext.require();
        Map<String, Acc> rows = new LinkedHashMap<>();
        for (StockLine l : movementRepository.stockAt(tenantId, workPointId, date)) {
            acc(rows, l).stock = acc(rows, l).stock.add(l.getKg());
        }
        return rows.entrySet().stream()
                .filter(e -> keys.contains(e.getKey()) && e.getValue().stock.signum() < 0)
                .map(e -> {
                    Acc a = e.getValue();
                    return new StockResponse.Row(a.articleId, a.articleName, a.codeId, a.code, a.name, a.hazardous,
                            a.stock, BigDecimal.ZERO, BigDecimal.ZERO, a.stock, true, null, null, false, false, null, null);
                })
                .toList();
    }

    /** Cheia unui rând de stoc: sortimentul (sau lipsa lui, la o mișcare scrisă de mână) și codul. */
    public static String key(UUID articleId, UUID wasteCodeId) {
        return articleId + "|" + wasteCodeId;
    }

    private static List<StockLine> visible(List<StockLine> lines, Set<UUID> allowed) {
        return allowed == null ? lines : lines.stream().filter(l -> allowed.contains(l.getWorkPointId())).toList();
    }

    private static Acc acc(Map<String, Acc> rows, StockLine l) {
        return rows.computeIfAbsent(key(l.getArticleId(), l.getWasteCodeId()), k -> new Acc(l));
    }

    private static final class Acc {
        final UUID articleId;
        final String articleName;
        final UUID codeId;
        final String code;
        final String name;
        final boolean hazardous;
        BigDecimal stock = BigDecimal.ZERO;
        BigDecimal inTransit = BigDecimal.ZERO;
        BigDecimal committed = BigDecimal.ZERO;

        Acc(StockLine l) {
            articleId = l.getArticleId();
            articleName = l.getArticleName();
            codeId = l.getWasteCodeId();
            code = l.getCode();
            name = l.getName();
            hazardous = l.getHazardous();
        }
    }
}
