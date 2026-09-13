package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.repository.AppUserRepository;

import java.util.List;
import java.util.Properties;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * BUG-011 — harta API-ului nu se publică în producţie.
 *
 * <p>Până pe 13.09.2026, {@code /v3/api-docs} şi {@code /swagger-ui/index.html} răspundeau
 * {@code 200} pe producţie, fără sesiune: oricine avea lista celor ~70 de endpointuri, cu
 * parametri şi forme de corp. Nu dă acces la nimic, dar e recon gratuit — iar cine are lista de
 * căi o poate plimba cu verbe.
 *
 * <p><b>De ce testul porneşte documentaţia oprită explicit.</b> Toate testele rulează pe profilul
 * {@code dev}, unde documentaţia rămâne pornită dinadins. Producţia n-are profil activ, deci
 * citeşte doar {@code application.yml}. Clasa face două lucruri separat: pune contextul în starea
 * producţiei şi probează ce vede un străin, iar ultimul test citeşte chiar fişierele de configurare
 * — altfel un {@code true} pus la loc în {@code application.yml} n-ar cădea nicăieri.
 */
@SpringBootTest(properties = {
        "springdoc.api-docs.enabled=false",
        "springdoc.swagger-ui.enabled=false"
})
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class ApiDocsExposureIT {

    private static final List<String> DOC_PATHS = List.of(
            "/v3/api-docs",
            "/v3/api-docs/swagger-config",
            "/swagger-ui.html",
            "/swagger-ui/index.html"
    );

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired AppUserRepository appUserRepository;

    @Test
    void withoutASessionTheMapOfTheApiIsNotPublic() throws Exception {
        for (String path : DOC_PATHS) {
            int status = mockMvc.perform(get(path)).andReturn().getResponse().getStatus();
            assertThat(status).as(path).isEqualTo(401);
        }
    }

    /**
     * Cu sesiune, adresa pur şi simplu nu există — şi trebuie să spună asta ca 404, nu ca 500 cu
     * alertă în Sentry. Dacă numele proprietăţii ar fi greşit, aici ar ieşi 200 cu documentul.
     */
    @Test
    void withASessionTheDocsAreNotServedAndTheAnswerIsANotFound() throws Exception {
        String token = jwtService.generateToken(appUserRepository.findByEmail("admin@demo.ro").orElseThrow());
        for (String path : DOC_PATHS) {
            var response = mockMvc.perform(get(path).header("Authorization", "Bearer " + token))
                    .andReturn().getResponse();
            assertThat(response.getStatus()).as(path).isEqualTo(404);
            assertThat(response.getContentAsString()).as(path).doesNotContain("openapi", "swagger");
        }
    }

    /** Ce citeşte dyno-ul: {@code application.yml} singur. Profilul dev o porneşte la loc. */
    @Test
    void productionConfigTurnsTheDocsOffAndOnlyDevTurnsThemOn() {
        Properties base = yaml("application.yml");
        assertThat(base.getProperty("springdoc.api-docs.enabled")).isEqualTo("false");
        assertThat(base.getProperty("springdoc.swagger-ui.enabled")).isEqualTo("false");

        Properties dev = yaml("application-dev.yml");
        assertThat(dev.getProperty("springdoc.api-docs.enabled")).isEqualTo("true");
        assertThat(dev.getProperty("springdoc.swagger-ui.enabled")).isEqualTo("true");
    }

    private static Properties yaml(String name) {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(new ClassPathResource(name));
        return factory.getObject();
    }
}
