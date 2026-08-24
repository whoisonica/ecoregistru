package ro.ecoregistru.service.export;

import org.springframework.stereotype.Component;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.PackagingMarketEntry;
import ro.ecoregistru.entity.Partner;
import ro.ecoregistru.entity.WasteMovement;
import ro.ecoregistru.enums.PackagingMaterial;
import ro.ecoregistru.enums.Unit;
import ro.ecoregistru.enums.WasteRegister;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Assembles Anexa 1 Ambalaje (Ordinul 794/2012) from the two halves it has: the market figures the
 * client answered, and the handovers already recorded.
 *
 * <p>Tabelul 2 is derived, and it is derived narrowly on purpose. Only <b>15 01 xx</b> counts: a
 * shop's cardboard recorded under 20 01 01 is waste like any other and belongs on the
 * waste-management record, not in a packaging declaration. That is the distinction the specialist
 * drew on 24.08.2026 — "cartonul din magazine este 15 01 01" — and the code chosen when the
 * movement was recorded is what decides, because nothing here proposes codes.
 */
@Component
public class PackagingDeclarationBuilder {

    private static final BigDecimal KG_PER_TON = new BigDecimal("1000");

    public PackagingDeclaration build(Company company,
                                      int year,
                                      List<PackagingMarketEntry> entries,
                                      List<WasteMovement> movements) {
        Map<PackagingMaterial, PackagingMarketEntry> byMaterial = new LinkedHashMap<>();
        for (PackagingMarketEntry entry : entries) {
            byMaterial.put(entry.getMaterial(), entry);
        }

        List<PackagingDeclaration.MarketRow> marketRows = new ArrayList<>();
        for (PackagingMaterial material : PackagingMaterial.values()) {
            PackagingMarketEntry e = byMaterial.get(material);
            marketRows.add(new PackagingDeclaration.MarketRow(
                    material,
                    e == null ? null : e.getSalesPackaging(),
                    e == null ? null : e.getPrimaryTotal(),
                    e == null ? null : e.getPrimaryReusable(),
                    e == null ? null : e.getSecondaryTotal(),
                    e == null ? null : e.getSecondaryReusable(),
                    e == null ? null : e.getHazardousContent()));
        }

        Set<String> ambiguous = new LinkedHashSet<>();
        List<PackagingDeclaration.HandoverRow> handovers = handovers(movements, ambiguous);

        return new PackagingDeclaration(
                company.getName(),
                // "Judeţ şi localitate" e o rubrică proprie pe formular, iar noi ţinem o singură
                // adresă, liberă. A o tipări de două ori ar arăta ca două răspunsuri la două
                // întrebări, deci rubrica rămâne goală până când o cere cineva ca dată separată.
                null,
                company.getAddress(),
                contact(company),
                company.getCaenCode(),
                company.getCui(),
                year,
                marketRows,
                handovers,
                List.copyOf(ambiguous),
                company.getContactName(),
                company.getContactRole());
    }

    /**
     * Tabelul 2: one line per (material, operator), which the annex asks for in writing — nota 1,
     * "câte o rubrică distinctă pentru fiecare dintre operatorii care au preluat deşeurile de
     * ambalaje din materialul respectiv".
     *
     * <p>Only handovers count: a quantity with no operator named was treated on our own site and
     * was never "încredinţat unui operator economic autorizat". Quantities still waiting for the
     * recipient's weighbridge contribute nothing to the figure but keep their line, so the operator
     * is on the form and the missing weight is visible.
     */
    private List<PackagingDeclaration.HandoverRow> handovers(List<WasteMovement> movements,
                                                             Set<String> ambiguous) {
        Map<String, PackagingDeclaration.HandoverRow> rows = new LinkedHashMap<>();
        Map<String, BigDecimal> quantities = new LinkedHashMap<>();

        for (WasteMovement m : movements.stream()
                .filter(m -> m.getRegister() == WasteRegister.ANEXA_1)
                .filter(m -> m.getOperation().isExit())
                .filter(m -> m.getPartner() != null)
                .sorted(Comparator.comparing(WasteMovement::getDate))
                .toList()) {

            String code = m.getWasteCode().getCode();
            PackagingMaterial material = PackagingMaterial.forWasteCode(code).orElse(null);
            if (material == null) {
                continue;
            }
            if (PackagingMaterial.isAmbiguous(code)) {
                ambiguous.add(code);
            }
            Partner operator = m.getPartner();
            String operation = m.getOperationCode() == null ? "" : m.getOperationCode().name();
            String key = material + "|" + operator.getId() + "|" + operation;

            rows.putIfAbsent(key, new PackagingDeclaration.HandoverRow(
                    material, null, operator.getName(), unloadingPlace(m, operator),
                    operator.getCui(), operation));
            if (m.getQuantity() != null) {
                quantities.merge(key, kg(m), BigDecimal::add);
            }
        }

        return rows.entrySet().stream()
                .map(e -> {
                    PackagingDeclaration.HandoverRow r = e.getValue();
                    return new PackagingDeclaration.HandoverRow(
                            r.material(), quantities.get(e.getKey()), r.operatorName(),
                            r.operatorAddress(), r.operatorCui(), r.operation());
                })
                .sorted(Comparator.comparing(r -> r.material().ordinal()))
                .toList();
    }

    /**
     * "Denumirea, adresă punct de lucru" of tabelul 2 — the same place Anexa 3 names as the
     * recipient: the work point this load went to, their only one if they have exactly one, and
     * the head office otherwise. A partner with several depots and no choice made is not assigned
     * one here either.
     */
    private String unloadingPlace(WasteMovement m, Partner operator) {
        if (m.getPartnerWorkPoint() != null) {
            return m.getPartnerWorkPoint().label();
        }
        List<ro.ecoregistru.entity.PartnerWorkPoint> points = operator.getWorkPoints();
        if (points != null && points.size() == 1) {
            return points.get(0).label();
        }
        return operator.getAddress();
    }

    private String contact(Company company) {
        StringBuilder sb = new StringBuilder();
        if (company.getContactPhone() != null) {
            sb.append(company.getContactPhone());
        }
        if (company.getContactEmail() != null) {
            if (!sb.isEmpty()) {
                sb.append(" / ");
            }
            sb.append(company.getContactEmail());
        }
        return sb.toString();
    }

    private BigDecimal kg(WasteMovement m) {
        return m.getUnit() == Unit.TONS ? m.getQuantity().multiply(KG_PER_TON) : m.getQuantity();
    }
}
