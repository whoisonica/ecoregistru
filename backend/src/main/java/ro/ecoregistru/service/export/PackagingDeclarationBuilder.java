package ro.ecoregistru.service.export;

import org.springframework.stereotype.Component;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.PackagingMarketEntry;
import ro.ecoregistru.entity.Partner;
import ro.ecoregistru.entity.PartnerWorkPoint;
import ro.ecoregistru.entity.WasteMovement;
import ro.ecoregistru.enums.PackagingCategory;
import ro.ecoregistru.enums.PackagingMaterial;
import ro.ecoregistru.enums.Unit;
import ro.ecoregistru.enums.WasteOperation;
import ro.ecoregistru.enums.WasteRegister;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Assembles Anexa 1 Ambalaje (Ordinul 794/2012) out of the movements.
 *
 * <p>Both tables are derived, and both derive narrowly on purpose. Only <b>15 01 xx</b> counts: a
 * shop cardboard recorded under 20 01 01 is waste like any other and belongs on the
 * waste-management record, not in a packaging declaration. That is the distinction the specialist
 * drew on 24.08.2026 — "cartonul din magazine este 15 01 01" — and the code chosen when the
 * movement was recorded is what decides, because nothing here proposes codes.
 *
 * <p><b>What reaches this declaration is ticked, not deduced.</b> Only movements marked "ambalaj pus
 * de noi pe piaţa naţională" count ({@code V27}). The code alone was too wide: a shop's own
 * {@code 15 01 01} is waste it generated, but packaging its supplier placed on the market.
 *
 * <p><b>Tabelul 1 — ambalaje introduse pe piaţa naţională.</b> The kilograms the company put on the
 * market are the kilograms that come back as packaging waste, and the client records those anyway:
 * "omul, când reciclează acea cantitate de ambalaj pusă pe piaţă, o să adauge mişcare, ca să o
 * poată scoate şi să apară în gestiune şi în rapoarte" (user, 25.08.2026). So the table sums the
 * movements, split by the material and the kind of packaging the movement now carries.
 *
 * <p><b>Each kilogram is counted once.</b> A company that records both the generation and the
 * handover of the same load has two movements for one quantity. Per waste code and year, the
 * generations win when there are any, and the exits stand in when there are none — the same
 * substitution the evidence engine makes for implied generation ({@code V24}), and for the same
 * reason: the exit is the proof the waste existed.
 *
 * <p><b>Tabelul 2 — deşeuri de ambalaje gestionate.</b> Only handovers count: a quantity with no
 * operator named was treated on our own site and was never "încredinţat unui operator economic
 * autorizat".
 */
@Component
public class PackagingDeclarationBuilder {

    private static final BigDecimal KG_PER_TON = new BigDecimal("1000");

    public PackagingDeclaration build(Company company,
                                      int year,
                                      List<PackagingMarketEntry> entries,
                                      List<WasteMovement> movements) {

        List<WasteMovement> packaging = movements.stream()
                .filter(m -> m.getRegister() == WasteRegister.ANEXA_1)
                .filter(m -> PackagingMaterial.isPackagingCode(m.getWasteCode().getCode()))
                .filter(PackagingDeclarationBuilder::putOnMarketByUs)
                .sorted(Comparator.comparing(WasteMovement::getDate))
                .toList();

        List<PackagingDeclaration.UnclassifiedRow> unclassified = new ArrayList<>();
        List<PackagingDeclaration.MarketRow> marketRows =
                marketRows(packaging, entries, unclassified);

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
                handovers(packaging),
                unclassified,
                company.getContactName(),
                company.getContactRole());
    }

    // ---------------------------------------------------------------- tabelul 1

    /**
     * One row per material, in the order the form prints them, summed from the movements that put
     * the packaging on the record — and replaced wholesale where the client stored their own
     * figures for that material.
     */
    private List<PackagingDeclaration.MarketRow> marketRows(
            List<WasteMovement> packaging,
            List<PackagingMarketEntry> entries,
            List<PackagingDeclaration.UnclassifiedRow> unclassified) {

        Map<PackagingMaterial, PackagingMarketEntry> overrides =
                new EnumMap<>(PackagingMaterial.class);
        for (PackagingMarketEntry entry : entries) {
            overrides.put(entry.getMaterial(), entry);
        }

        Map<PackagingMaterial, Sums> computed = new EnumMap<>(PackagingMaterial.class);
        for (WasteMovement m : countOnce(packaging)) {
            PackagingMaterial material =
                    PackagingMaterial.resolve(m.getPackagingMaterial(), m.getWasteCode().getCode())
                            .orElse(null);
            PackagingCategory category = m.getPackagingCategory();

            if (material == null || category == null) {
                unclassified.add(new PackagingDeclaration.UnclassifiedRow(
                        m.getId(), m.getDate(), m.getWasteCode().getCode(),
                        m.getQuantity() == null ? null : kg(m), material, category,
                        material == null, category == null));
                continue;
            }
            if (m.getQuantity() == null) {
                // De cântărit: linia îşi păstrează locul în ecran prin lista de mişcări, dar nu
                // aduce nicio cifră aici. Un zero ar spune "n-a fost nimic", ceea ce e altceva.
                continue;
            }
            computed.computeIfAbsent(material, k -> new Sums()).add(category, m, kg(m));
        }

        List<PackagingDeclaration.MarketRow> rows = new ArrayList<>();
        for (PackagingMaterial material : PackagingMaterial.values()) {
            PackagingMarketEntry override = overrides.get(material);
            if (override != null) {
                rows.add(new PackagingDeclaration.MarketRow(
                        material, override.getSalesPackaging(), override.getPrimaryTotal(),
                        override.getPrimaryReusable(), override.getSecondaryTotal(),
                        override.getSecondaryReusable(), override.getHazardousContent(), true));
                continue;
            }
            Sums s = computed.get(material);
            rows.add(s == null
                    ? new PackagingDeclaration.MarketRow(
                            material, null, null, null, null, null, null, false)
                    : s.toRow(material));
        }
        return rows;
    }

    /**
     * The movements whose kilograms tabelul 1 may count, one physical quantity at a time.
     *
     * <p>Grouped by waste code: where the year holds recorded generations for a code, only those
     * count; where it holds none, the exits stand in for them. A company that records both would
     * otherwise declare the same load twice, and a company that records only the handover — the
     * usual case, and the one that forced implied generation in {@code V24} — would declare nothing
     * at all.
     */
    private List<WasteMovement> countOnce(List<WasteMovement> packaging) {
        Map<String, List<WasteMovement>> byCode = new LinkedHashMap<>();
        for (WasteMovement m : packaging) {
            byCode.computeIfAbsent(m.getWasteCode().getCode(), k -> new ArrayList<>()).add(m);
        }

        List<WasteMovement> basis = new ArrayList<>();
        for (List<WasteMovement> group : byCode.values()) {
            List<WasteMovement> generated = group.stream()
                    .filter(m -> m.getOperation() == WasteOperation.GENERATED)
                    .toList();
            basis.addAll(generated.isEmpty()
                    ? group.stream()
                            .filter(m -> m.getOperation() != WasteOperation.COLLECTED)
                            .toList()
                    : generated);
        }
        return basis;
    }

    /** The six figures of one material row, accumulated as the movements come in. */
    private static final class Sums {
        BigDecimal sales;
        BigDecimal primaryTotal;
        BigDecimal primaryReusable;
        BigDecimal secondaryTotal;
        BigDecimal secondaryReusable;
        BigDecimal hazardous;

        void add(PackagingCategory category, WasteMovement m, BigDecimal kg) {
            switch (category) {
                case SALES -> sales = plus(sales, kg);
                case PRIMARY -> {
                    primaryTotal = plus(primaryTotal, kg);
                    if (Boolean.TRUE.equals(m.getPackagingReusable())) {
                        primaryReusable = plus(primaryReusable, kg);
                    }
                }
                case SECONDARY -> {
                    secondaryTotal = plus(secondaryTotal, kg);
                    if (Boolean.TRUE.equals(m.getPackagingReusable())) {
                        secondaryReusable = plus(secondaryReusable, kg);
                    }
                }
            }
            // Nota 3: ambalajele cu conţinut periculos "sunt tot ambalaje primare şi se regăsesc şi
            // în coloana 3". Deci cantitatea se numără în amândouă, nu se mută din una în alta.
            if (Boolean.TRUE.equals(m.getPackagingHazardousContent())) {
                hazardous = plus(hazardous, kg);
            }
        }

        PackagingDeclaration.MarketRow toRow(PackagingMaterial material) {
            return new PackagingDeclaration.MarketRow(material, sales, primaryTotal,
                    primaryReusable, secondaryTotal, secondaryReusable, hazardous, false);
        }

        private static BigDecimal plus(BigDecimal acc, BigDecimal kg) {
            return acc == null ? kg : acc.add(kg);
        }
    }

    // ---------------------------------------------------------------- tabelul 2

    /**
     * Tabelul 2: one line per (material, operator, operation), which the annex asks for in writing
     * — nota 1, "câte o rubrică distinctă pentru fiecare dintre operatorii care au preluat
     * deşeurile de ambalaje din materialul respectiv".
     *
     * <p>Quantities still waiting for the recipient weighbridge contribute nothing to the figure
     * but keep their line, so the operator is on the form and the missing weight is visible.
     * A movement whose material nobody settled has no row to sit on and is reported as unclassified
     * by tabelul 1 instead — the same gap, named once.
     */
    private List<PackagingDeclaration.HandoverRow> handovers(List<WasteMovement> packaging) {
        Map<String, PackagingDeclaration.HandoverRow> rows = new LinkedHashMap<>();
        Map<String, BigDecimal> quantities = new LinkedHashMap<>();

        for (WasteMovement m : packaging) {
            if (!m.getOperation().isExit() || m.getPartner() == null) {
                continue;
            }
            PackagingMaterial material =
                    PackagingMaterial.resolve(m.getPackagingMaterial(), m.getWasteCode().getCode())
                            .orElse(null);
            if (material == null) {
                continue;
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
        List<PartnerWorkPoint> points = operator.getWorkPoints();
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

    /** The act prints [kilograme] at the head of every table, so everything converts to kg. */
    private BigDecimal kg(WasteMovement m) {
        return m.getUnit() == Unit.TONS ? m.getQuantity().multiply(KG_PER_TON) : m.getQuantity();
    }

    /**
     * Whether the company put this packaging on the national market itself — the tick that decides
     * whether the movement reaches this declaration at all.
     *
     * <p>The waste code is not enough, and that was the flaw: a shop throwing out the boxes its
     * stock arrived in records {@code 15 01 01} like anyone else, but its supplier placed that
     * packaging on the market. The declaration reports what <b>the declarant</b> introduced —
     * "Producători şi importatori [...] de produse ambalate" — so the answer belongs to the person
     * recording the movement, who is the only one who knows.
     *
     * <p>Null keeps the behaviour movements had before the question existed: included. Changing a
     * figure already printed, silently, on a form filed with an authority, is the one thing this
     * module never does; the tab marks those rows as unconfirmed instead.
     */
    private static boolean putOnMarketByUs(WasteMovement m) {
        return !Boolean.FALSE.equals(m.getPackagingOnMarket());
    }

    /** The material row a movement lands on: what the client chose, or what the code proposes. */
    public static Optional<PackagingMaterial> materialOf(WasteMovement m) {
        return PackagingMaterial.resolve(m.getPackagingMaterial(), m.getWasteCode().getCode());
    }
}
