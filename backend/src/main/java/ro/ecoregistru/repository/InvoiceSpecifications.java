package ro.ecoregistru.repository;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.Consultancy;
import ro.ecoregistru.entity.Subscription;
import ro.ecoregistru.entity.SubscriptionInvoice;
import ro.ecoregistru.enums.InvoiceFilter;
import ro.ecoregistru.enums.InvoiceStatus;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * F-B2 — the Facturare screen asks the server for one page, not for every invoice ever issued. The filter, the
 * month of the period and the search are predicates here, so the counts on the filter keys and the rows under them
 * come from the same rule.
 */
public final class InvoiceSpecifications {

    private InvoiceSpecifications() {}

    public static Specification<SubscriptionInvoice> of(InvoiceFilter filter, YearMonth month, String query, LocalDate today) {
        return (root, cq, cb) -> {
            List<Predicate> and = new ArrayList<>();

            Predicate failed = cb.and(cb.equal(root.get("status"), InvoiceStatus.DRAFT), cb.isNotNull(root.get("lastError")));
            Predicate issued = cb.equal(root.get("status"), InvoiceStatus.ISSUED);
            Predicate overdue = cb.and(issued, cb.lessThan(root.get("dueDate"), today));
            switch (filter) {
                case ACTION -> and.add(cb.or(failed, overdue));
                case FAILED -> and.add(failed);
                case OVERDUE -> and.add(overdue);
                case UNPAID -> and.add(issued);
                case PAID -> and.add(cb.equal(root.get("status"), InvoiceStatus.PAID));
                case ALL -> { }
            }

            if (month != null) {
                and.add(cb.between(root.get("periodStart"), month.atDay(1), month.atEndOfMonth()));
            }

            String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
            if (!q.isEmpty()) {
                Join<SubscriptionInvoice, Subscription> s = root.join("subscription", JoinType.INNER);
                Join<Subscription, Company> company = s.join("company", JoinType.LEFT);
                Join<Subscription, Consultancy> cabinet = s.join("consultancy", JoinType.LEFT);
                String like = "%" + q.replace("%", "").replace("_", "") + "%";
                List<Predicate> or = new ArrayList<>(List.of(
                        cb.like(cb.lower(company.get("name")), like),
                        cb.like(cb.lower(cabinet.get("name")), like)));
                // „WH 12”, „wh12” sau „12”: numărul facturii, fără serie.
                String digits = q.replaceFirst("^wh\\s*", "");
                if (digits.matches("\\d+")) {
                    or.add(cb.equal(root.get("fgoNumar"), digits));
                }
                and.add(cb.or(or.toArray(Predicate[]::new)));
            }
            return cb.and(and.toArray(Predicate[]::new));
        };
    }
}
