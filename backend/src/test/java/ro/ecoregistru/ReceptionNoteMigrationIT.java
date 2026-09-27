package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.NaturalPerson;
import ro.ecoregistru.entity.Partner;
import ro.ecoregistru.entity.WasteArticle;
import ro.ecoregistru.entity.WasteMovement;
import ro.ecoregistru.entity.WeighingOperation;
import ro.ecoregistru.entity.WorkPoint;
import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.PartnerType;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.enums.Unit;
import ro.ecoregistru.enums.WasteOperation;
import ro.ecoregistru.enums.WasteRegister;
import ro.ecoregistru.enums.WeighingOperationStatus;
import ro.ecoregistru.enums.WeighingOperationType;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.NaturalPersonRepository;
import ro.ecoregistru.repository.PartnerRepository;
import ro.ecoregistru.repository.WasteArticleRepository;
import ro.ecoregistru.repository.WasteCodeRepository;
import ro.ecoregistru.repository.WasteMovementRepository;
import ro.ecoregistru.repository.WeighingOperationRepository;
import ro.ecoregistru.repository.WorkPointRepository;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * D1.17a — V76 numerotează retroactiv intrările PF deja finalizate: borderou pentru cele cu o linie plătită (după
 * maximul existent), NIR pentru cele cu o linie gratuită (de la 1), în ordinea finalizării. Se rulează instrucțiunile
 * de numerotare din fișierul migrării pe o firmă cu operațiuni „de dinainte”, fără numere.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class ReceptionNoteMigrationIT {

    private static final LocalDate DAY = LocalDate.of(2026, 3, 10);
    private static final Instant T = Instant.parse("2026-03-10T08:00:00Z");

    @Autowired JdbcTemplate jdbc;
    @Autowired AppUserRepository userRepository;
    @Autowired CompanyRepository companyRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired PartnerRepository partnerRepository;
    @Autowired NaturalPersonRepository personRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired WasteArticleRepository articleRepository;
    @Autowired WeighingOperationRepository operationRepository;
    @Autowired WasteMovementRepository movementRepository;

    Company company;
    AppUser admin;
    WorkPoint depot;
    NaturalPerson person;
    WasteArticle cardboard;
    int nextNumber = 1;

    @Test
    void retroactiveNumbersFollowFinalizationOrder() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        company = companyRepository.save(Company.builder()
                .name("Colector NIR " + suffix + " SRL").cui("RON" + suffix).address("Cluj")
                .type(CompanyType.COLLECTOR).active(true).createdAt(Instant.now()).build());
        admin = userRepository.save(AppUser.builder().email("admin+nir" + suffix + "@demo.ro").password("x")
                .role(Role.ADMIN).company(company).enabled(true).createdAt(Instant.now()).build());
        depot = workPointRepository.save(WorkPoint.builder()
                .company(company).name("Depozit NIR").active(true).createdAt(Instant.now()).build());
        person = personRepository.save(NaturalPerson.builder()
                .company(company).name("Ana Ionescu").active(true).createdAt(Instant.now()).build());
        cardboard = articleRepository.save(WasteArticle.builder()
                .company(company).wasteCode(wasteCodeRepository.findByCode("15 01 01").orElseThrow())
                .name("Carton").metal(false).active(true).createdAt(Instant.now()).build());
        Partner supplier = partnerRepository.save(Partner.builder()
                .company(company).name("Magazin Beta SRL").cui("RO" + suffix)
                .type(PartnerType.GENERATOR).supplier(true).client(true).active(true).createdAt(Instant.now()).build());

        WeighingOperation h = op(WeighingOperationStatus.FINALIZED, 0, null, "2");
        h.setBorderouNumber(4);
        operationRepository.saveAndFlush(h);
        WeighingOperation a = op(WeighingOperationStatus.FINALIZED, 1, null, "10");
        WeighingOperation b = op(WeighingOperationStatus.FINALIZED, 2, null, "0");
        WeighingOperation c = op(WeighingOperationStatus.FINALIZED, 3, null, "5", "0.00");
        WeighingOperation d = op(WeighingOperationStatus.FINALIZED, 4, null, (String) null);
        WeighingOperation e = op(WeighingOperationStatus.CANCELLED, 5, null, "3");
        WeighingOperation f = op(WeighingOperationStatus.IN_PROGRESS, 6, null, "7");
        WeighingOperation g = op(WeighingOperationStatus.FINALIZED, 7, supplier, "0");

        String migration = new ClassPathResource("db/migration/V76__reception_note_number.sql")
                .getContentAsString(StandardCharsets.UTF_8);
        Arrays.stream(migration.split(";"))
                .map(s -> s.replaceAll("(?m)^\\s*--.*$", "").strip())
                .filter(s -> s.startsWith("UPDATE") || s.startsWith("WITH"))
                .forEach(jdbc::execute);

        assertThat(numbers(h)).containsExactly(4, null);
        assertThat(numbers(a)).containsExactly(5, null);
        assertThat(numbers(b)).containsExactly(null, 1);
        assertThat(numbers(c)).containsExactly(6, 2);
        assertThat(numbers(d)).containsExactly(null, null);
        assertThat(numbers(e)).containsExactly(7, null);
        assertThat(numbers(f)).containsExactly(null, null);
        assertThat(numbers(g)).containsExactly(null, null);
    }

    private Integer[] numbers(WeighingOperation op) {
        WeighingOperation fresh = operationRepository.findById(op.getId()).orElseThrow();
        return new Integer[]{fresh.getBorderouNumber(), fresh.getReceptionNoteNumber()};
    }

    /** O intrare „de dinainte”: finalizată la T + {@code minutes}, fără numere, cu câte o linie pe fiecare preț. */
    private WeighingOperation op(WeighingOperationStatus status, int minutes, Partner partner, String... prices) {
        WeighingOperation.WeighingOperationBuilder b = WeighingOperation.builder()
                .company(company).workPoint(depot).type(WeighingOperationType.IN).number(nextNumber++).date(DAY)
                .status(status).createdBy(admin.getId());
        if (partner != null) {
            b.partner(partner);
        } else {
            b.naturalPerson(person);
        }
        if (status != WeighingOperationStatus.IN_PROGRESS) {
            b.finalizedAt(T.plusSeconds(60L * minutes)).finalizedBy(admin.getId());
        }
        if (status == WeighingOperationStatus.CANCELLED) {
            b.cancelledAt(T.plusSeconds(3600)).cancelledBy(admin.getId()).cancelReason("Dublură");
        }
        WeighingOperation op = operationRepository.saveAndFlush(b.build());
        int lineNo = 1;
        for (String price : prices) {
            BigDecimal unitPrice = price == null ? null : new BigDecimal(price);
            movementRepository.saveAndFlush(WasteMovement.builder()
                    .company(company).workPoint(depot).date(DAY).wasteCode(cardboard.getWasteCode())
                    .article(cardboard).weighingOperation(op).lineNo(lineNo++)
                    .netKg(BigDecimal.TEN).quantity(BigDecimal.TEN).unit(Unit.KG)
                    .unitPrice(unitPrice).totalValue(unitPrice == null ? null : unitPrice.multiply(BigDecimal.TEN))
                    .operation(WasteOperation.COLLECTED).register(WasteRegister.ART_48)
                    .deleted(false).createdBy(admin.getId()).build());
        }
        return op;
    }
}
