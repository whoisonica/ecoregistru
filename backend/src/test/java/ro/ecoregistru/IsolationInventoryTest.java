package ro.ecoregistru;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Izolarea între firme și mascarea CNP-ului, ca <b>regulă citită din cod</b>, nu ca obicei.
 *
 * <p>{@code TenantContext} scrie în javadocul lui că „orice interogare de domeniu se filtrează pe
 * {@code require()}", iar CNP-ul se maschează la trei apeluri. Amândouă erau convenții: un
 * repository nou care uită {@code company_id} sau un DTO nou care întoarce un CNP întreg nu făceau
 * să cadă nimic — abia o citire de cod le găsea. Aşa au apărut BUG-043 şi BUG-053.
 *
 * <p>Clasa asta face din ele o gardă, în tiparul lui {@link EndpointGuardInventoryIT}: <b>citeşte
 * lista din cod</b>, nu o scrie. Un repository nou intră singur sub regulă, iar o scutire care nu
 * mai are obiect cade şi ea — o listă de scutiri care nu se curăţă devine o uşă.
 *
 * <p>Reflexie curată, fără context Spring: rulează în milisecunde şi nu depinde de nicio bază.
 */
class IsolationInventoryTest {

    /**
     * Entitățile care aparțin unei firme. Se citesc din cod — au un câmp {@code company} — deci o
     * entitate nouă cu firmă intră singură sub regulă.
     */
    private static Set<Class<?>> tenantOwnedEntities() throws Exception {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(Object.class));
        Set<Class<?>> owned = new java.util.HashSet<>();
        for (BeanDefinition bd : scanner.findCandidateComponents("ro.ecoregistru.entity")) {
            Class<?> type = Class.forName(bd.getBeanClassName());
            boolean hasCompany = java.util.Arrays.stream(type.getDeclaredFields())
                    .anyMatch(f -> f.getName().equals("company"));
            if (hasCompany) {
                owned.add(type);
            }
        }
        return owned;
    }

    /**
     * Interogările care întorc rânduri ale unei firme se filtrează pe firmă.
     *
     * <p>Scutirile sunt numite, fiecare cu motivul ei. Ce nu e aici şi întoarce o entitate cu
     * {@code company} trebuie să poarte firma în numele metodei sau în {@code @Query}.
     */
    private static final Set<String> NOT_TENANT_SCOPED = Set.of(
            // ── Metodele moştenite de la `JpaRepository` ────────────────────────────────────────
            // Nu le scriem noi şi n-au cum purta firma. Cheia primară e un UUID nepredictibil, iar
            // tiparul casei e `findByIdAndCompany_Id`; unde totuşi se cheamă `findById`, apelantul
            // compară firma imediat după.
            "findById", "findAllById", "existsById", "count", "findAll", "deleteById", "delete",
            "deleteAll", "deleteAllById", "save", "saveAll", "saveAndFlush", "saveAllAndFlush",
            "flush", "getOne", "getById", "getReferenceById", "deleteAllInBatch",
            "deleteAllByIdInBatch", "findBy",

            // ── Uşa de intrare ─────────────────────────────────────────────────────────────────
            // Adresa e unică pe platformă şi e chiar lucrul după care se caută contul la login:
            // n-are firmă fiindcă încă nu ştim a cui e.
            "findByEmail",

            // ── Scopare pe cabinet, nu pe firmă ────────────────────────────────────────────────
            // Un consultant aparţine unui cabinet, nu unei firme (P2.13). `Consultancy_Id` e axa
            // lui de izolare, verificată la fel de strict — vezi `ConsultantAccessIT`.
            "findAllByConsultancy_IdAndEnabledTrue", "findAllByConsultancy_Id",
            "findByIdAndConsultancy_Id", "findOpenForConsultancy", "findByConsultancy_Id",

            // ── Joburi de noapte, peste toate firmele dinadins ─────────────────────────────────
            // Rulează fără sesiune, deci n-au de unde lua o firmă; ce trimit pleacă la firma
            // rândului. `ScheduledTransactionBoundaryTest` le păzeşte tranzacţia.
            "findAttestationWarningCandidates", "findWarningCandidates",
            "findAllByActiveTrueAndAuthorizationExpiryNotNullAndAuthorizationExpiryLessThanEqual",
            "findByStatusAndDueDateBetween", "findAllByDeletedFalseAndDateBefore",
            "findDueForReminder", "findExpiringBefore",

            // ── Abonamente: ale platformei, peste toate firmele ────────────────────────────────
            "findAllByStatusNotAndStartedAtLessThanEqual", "findAllByStatusNotAndStartedAtAfter",
            "findAllByStatusIn", "findAllOfCompanies",

            // ── Scopare indirectă, verificată la apelant (20.09.2026, citit în cod) ────────────
            // Lotul de import se ia întâi cu firma (`findByIdAndCompanyId` / `findAllByCompanyId…`
            // în `ImportBatchService`), iar mişcările lui sunt ale aceleiaşi firme prin construcţie.
            // La fel operaţiunea de cântar. ⚠️ Sunt sigure **fiindcă** apelantul verifică: cine
            // adaugă un apelant nou cu un id neverificat deschide o uşă.
            "findAllByImportBatchIdAndDeletedFalse",
            "findAllByWeighingOperation_IdInOrderByLineNoAsc",
            "findAllByWeighingOperation_IdOrderByLineNoAsc"
    );

    @Test
    void everyQueryThatReturnsACompanysRowsFiltersByCompany() throws Exception {
        Set<Class<?>> owned = tenantOwnedEntities();
        // Repository-urile sunt **interfeţe**, iar scanerul implicit le sare: `isCandidateComponent`
        // cere o clasă concretă. Fără suprascrierea asta lista ieşea goală, iar proba ar fi fost
        // verde degeaba — motivul pentru care mai jos stă un control pozitiv.
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false) {
                    @Override
                    protected boolean isCandidateComponent(
                            org.springframework.beans.factory.annotation.AnnotatedBeanDefinition bd) {
                        return bd.getMetadata().isIndependent();
                    }
                };
        scanner.addIncludeFilter(new AssignableTypeFilter(Repository.class));

        List<String> checked = new ArrayList<>();
        List<String> offenders = new ArrayList<>();
        for (BeanDefinition bd : scanner.findCandidateComponents("ro.ecoregistru.repository")) {
            Class<?> repo = Class.forName(bd.getBeanClassName());
            for (Method m : repo.getDeclaredMethods()) {
                if (!returnsAny(m, owned)) {
                    continue;
                }
                checked.add(repo.getSimpleName() + "." + m.getName());
                if (NOT_TENANT_SCOPED.contains(m.getName()) || scopesByCompany(m)) {
                    continue;
                }
                offenders.add(repo.getSimpleName() + "." + m.getName());
            }
        }

        // Controlul pozitiv: scanarea chiar vede interogările. Fără el, o listă goală de vinovaţi
        // ar fi trecut şi dacă scanarea n-ar fi găsit niciun repository.
        assertThat(checked)
                .as("scanarea găseşte interogările care întorc rânduri de firmă")
                .contains("WasteMovementRepository.findCountedBetween",
                        "PartnerRepository.findAllByCompany_Id");
        assertThat(offenders)
                .as("interogări care întorc rânduri ale unei firme fără s-o filtreze")
                .isEmpty();
    }

    private static boolean scopesByCompany(Method m) {
        String name = m.getName().toLowerCase(Locale.ROOT);
        if (name.contains("company")) {
            return true;
        }
        Query q = m.getAnnotation(Query.class);
        if (q == null) {
            return false;
        }
        String sql = q.value().toLowerCase(Locale.ROOT);
        return sql.contains("company.id") || sql.contains("company_id");
    }

    /** Întoarce metoda una din entităţile firmei — direct, sau într-un {@code List}/{@code Page}/{@code Optional}? */
    private static boolean returnsAny(Method m, Set<Class<?>> owned) {
        if (owned.contains(m.getReturnType())) {
            return true;
        }
        Type generic = m.getGenericReturnType();
        if (generic instanceof ParameterizedType p) {
            for (Type arg : p.getActualTypeArguments()) {
                if (arg instanceof Class<?> c && owned.contains(c)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Un CNP nu pleacă dintr-un DTO nou fără ca cineva să fi hotărât cum e apărat.
     *
     * <p>BUG-043 a mascat CNP-ul în listele de mişcări şi de şoferi, iar BUG-053 a închis avizul —
     * de fiecare dată după ce cineva a citit codul şi a observat. Lista de mai jos e decizia scrisă:
     * un DTO nou cu CNP cade aici până când primeşte un rând şi un motiv.
     */
    private static final Set<String> CNP_CARRYING_RESPONSES = Set.of(
            // Mascat prin `SecurityUtils.cnpForCurrentUser` în `WasteMovementMapper:68`.
            "WasteMovementResponse",
            // Mascat în `DriverService:164` şi `PartnerService:287`.
            "DriverResponse",
            // Depozit (persoane fizice) — în afara modulului de generator, apărat de rolurile lui.
            "NaturalPersonResponse", "NaturalPersonSummary",
            // Recordul imbricat din `DepotRetentionReport` — raportul de retenţie al
            // depozitului. Aici CNP-ul NU trece prin `cnpForCurrentUser`; ecranul e al
            // depozitului şi îl apără rolurile lui. Găsit de proba asta, 20.09.2026.
            "Beneficiary"
    );

    @Test
    void everyResponseCarryingACnpIsOnTheDeclaredList() throws Exception {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(Object.class));

        List<String> found = new ArrayList<>();
        for (BeanDefinition bd : scanner.findCandidateComponents("ro.ecoregistru.controller.response")) {
            Class<?> type = Class.forName(bd.getBeanClassName());
            boolean carriesCnp = type.isRecord()
                    ? java.util.Arrays.stream(type.getRecordComponents())
                            .map(RecordComponent::getName).anyMatch(n -> n.toLowerCase(Locale.ROOT).contains("cnp"))
                    : java.util.Arrays.stream(type.getDeclaredFields())
                            .map(java.lang.reflect.Field::getName)
                            .anyMatch(n -> n.toLowerCase(Locale.ROOT).contains("cnp"));
            if (carriesCnp) {
                found.add(type.getSimpleName());
            }
        }

        // Controlul pozitiv: scanarea chiar găseşte CNP-uri.
        assertThat(found).contains("WasteMovementResponse", "DriverResponse");
        assertThat(found)
                .as("un DTO nou cu CNP: adaugă-l în CNP_CARRYING_RESPONSES cu motivul, după ce "
                        + "hotărăşti cum e apărat")
                .allMatch(CNP_CARRYING_RESPONSES::contains);
        // Şi invers: o scutire care nu mai are obiect nu rămâne în listă.
        assertThat(CNP_CARRYING_RESPONSES)
                .as("scutire pentru un DTO care nu mai poartă CNP")
                .allMatch(found::contains);
    }
}
