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

        List<StockResponse.Row> out = new ArrayList<>();
        for (Acc a : rows.values()) {
            if (a.stock.signum() == 0 && a.inTransit.signum() == 0 && a.committed.signum() == 0) {
                continue;
            }
            out.add(new StockResponse.Row(a.articleId, a.articleName, a.codeId, a.code, a.name, a.hazardous,
                    a.stock, a.inTransit, a.committed, a.stock.subtract(a.committed), a.stock.signum() < 0));
        }
        out.sort(Comparator.comparing(StockResponse.Row::wasteCode)
                .thenComparing(r -> r.articleName() == null ? "" : r.articleName()));
        return new StockResponse(at, workPointId, out, (int) out.stream().filter(StockResponse.Row::negative).count());
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
                            a.stock, BigDecimal.ZERO, BigDecimal.ZERO, a.stock, true);
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
