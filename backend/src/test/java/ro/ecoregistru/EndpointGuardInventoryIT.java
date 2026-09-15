package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import ro.ecoregistru.service.CloudinaryStorageService;

import java.util.*;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/**
 * Gaura structurală 10 din audit: <b>un endpoint nou scris fără adnotare de rol e pur şi simplu
 * deschis</b>, şi nu cade niciun test.
 *
 * <p>{@code RoleAuthorizationMatrixIT} cere fiecare endpoint gatuit cu fiecare rol de dedesubt —
 * dar lista lui e scrisă de mână. Un {@code @PostMapping} adăugat mâine fără {@code @PreAuthorize}
 * nu intră în ea, deci trece neobservat şi rămâne deschis oricui are o sesiune, inclusiv unui
 * {@code CLIENT_VIEWER}.
 *
 * <p>Clasa asta nu scrie lista: o <b>citeşte din Spring</b>, din tabela de rute pe care o
 * foloseşte chiar aplicaţia. Deci un endpoint nou intră singur sub regulă. Excepţiile sunt numite,
 * şi o excepţie care nu mai există cade şi ea — o listă de scutiri care nu se curăţă devine o uşă.
 *
 * <p>Regula e pe <b>scrieri</b>. Citirile sunt deschise oricărui membru al firmei prin design (rolul
 * de vizualizare există ca să citească); izolarea între firme le apără, probată separat.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class EndpointGuardInventoryIT {

    /** Singurele endpointuri la care se ajunge fără sesiune — şi de ce. */
    private static final Set<String> PUBLIC = Set.of(
            "AuthenticationController.login",                 // uşa de intrare
            "AuthenticationController.requestResetPassword",  // „am uitat parola", fără cont deschis
            "AuthenticationController.resetPassword",         // linkul din mail, inclusiv invitaţia
            "AuthenticationController.ping",
            "AccountRequestController.submit",                // formularul public de cerere de cont
            "NetopiaIpnController.ipn"                        // rezultatul plății de la Netopia, crezut doar semnat (NetopiaIpnIT)
    );

    /**
     * Pragurile aplicaţiei. Un şir scris greşit ar refuza tot sau ar lăsa tot.
     *
     * <p>P2.13 a adăugat {@code CONSULTANT} în cele două praguri de firmă — un consultant lucrează
     * într-o firmă a cabinetului lui ca un administrator al ei — plus două praguri noi: al
     * directorului de firme (platformă sau consultant) şi al echipei de cabinet (numai consultant).
     */
    private static final Set<String> KNOWN_GUARDS = Set.of(
            "hasAuthority('PLATFORM_ADMIN')",
            "hasAnyAuthority('PLATFORM_ADMIN','CONSULTANT','ADMIN','OPERATOR')",
            "hasAnyAuthority('PLATFORM_ADMIN','CONSULTANT','ADMIN')",
            "hasAnyAuthority('PLATFORM_ADMIN','CONSULTANT')",
            "hasAuthority('CONSULTANT')"
    );

    private static final Set<RequestMethod> WRITES =
            EnumSet.of(RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.DELETE);

    @Autowired MockMvc mockMvc;
    @Autowired @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping mapping;

    @MockBean CloudinaryStorageService storageService;

    private record Endpoint(String name, Set<RequestMethod> methods, List<String> paths, HandlerMethod handler) {}

    private List<Endpoint> endpoints() {
        List<Endpoint> out = new ArrayList<>();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> e : mapping.getHandlerMethods().entrySet()) {
            Class<?> type = ClassUtils.getUserClass(e.getValue().getBeanType());
            if (!type.getPackageName().startsWith("ro.ecoregistru")) continue;
            out.add(new Endpoint(type.getSimpleName() + "." + e.getValue().getMethod().getName(),
                    e.getKey().getMethodsCondition().getMethods(),
                    List.copyOf(e.getKey().getPatternValues()),
                    e.getValue()));
        }
        assertThat(out).as("rutele aplicaţiei au fost citite").hasSizeGreaterThan(50);
        return out;
    }

    private static Optional<PreAuthorize> guard(HandlerMethod h) {
        PreAuthorize onMethod = AnnotatedElementUtils.findMergedAnnotation(h.getMethod(), PreAuthorize.class);
        if (onMethod != null) return Optional.of(onMethod);
        return Optional.ofNullable(AnnotatedElementUtils.findMergedAnnotation(
                ClassUtils.getUserClass(h.getBeanType()), PreAuthorize.class));
    }

    @Test
    void everyWriteEndpointSaysWhoMayCallIt() {
        List<String> open = endpoints().stream()
                .filter(e -> e.methods().isEmpty() || e.methods().stream().anyMatch(WRITES::contains))
                .filter(e -> !PUBLIC.contains(e.name()))
                .filter(e -> guard(e.handler()).isEmpty())
                .map(e -> e.name() + " " + e.methods() + " " + e.paths())
                .sorted().toList();
        assertThat(open).as("scrieri fără @PreAuthorize — deschise oricui are sesiune").isEmpty();
    }

    @Test
    void everyGuardIsOneOfTheKnownThresholds() {
        List<String> odd = endpoints().stream()
                .flatMap(e -> guard(e.handler()).stream().map(g -> e.name() + " → " + g.value()))
                .filter(s -> KNOWN_GUARDS.stream().noneMatch(s::endsWith))
                .sorted().toList();
        assertThat(odd).as("praguri care nu sunt ale aplicaţiei").isEmpty();
    }

    @Test
    void theExemptionsNameEndpointsThatStillExist() {
        Set<String> names = new HashSet<>();
        endpoints().forEach(e -> names.add(e.name()));
        assertThat(names).containsAll(PUBLIC);
    }

    /**
     * Cealaltă jumătate a regulii, în {@code SecurityConfiguration}: tot ce nu e pe lista publică
     * cere o sesiune. Probat pe fiecare rută reală, cu verbul ei, fără token.
     */
    @Test
    void withoutASessionOnlyThePublicEndpointsAnswer() throws Exception {
        List<String> reachable = new ArrayList<>();
        for (Endpoint e : endpoints()) {
            if (PUBLIC.contains(e.name())) continue;
            for (String pattern : e.paths()) {
                String path = pattern.replaceAll("\\{[^}]+}", UUID.randomUUID().toString());
                for (RequestMethod m : e.methods()) {
                    int status = mockMvc.perform(request(HttpMethod.valueOf(m.name()), path))
                            .andReturn().getResponse().getStatus();
                    if (status != 401) reachable.add(m + " " + path + " → " + status + " (" + e.name() + ")");
                }
            }
        }
        assertThat(reachable).as("rute la care se ajunge fără sesiune").isEmpty();
    }
}
