package ro.ecoregistru;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import ro.ecoregistru.config.CorsConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * How the allow-list is read, without booting anything.
 *
 * <p>Two shapes that would otherwise lock the frontend out of its own API with a header nobody
 * looks at: a {@code FRONTEND_BASE_URL} written with a trailing slash (a browser's {@code Origin}
 * never has one), and a list with spaces after the commas. Both fail silently in production and
 * look like „the app is down".
 */
class CorsOriginListTest {

    private static String allowOriginFor(String configured, String requestOrigin) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/work-points");
        request.addHeader("Origin", requestOrigin);
        MockHttpServletResponse response = new MockHttpServletResponse();
        new CorsConfig(configured).doFilter(request, response, mock(FilterChain.class));
        return response.getHeader("Access-Control-Allow-Origin");
    }

    @Test
    void aTrailingSlashInTheConfiguredValueStillMatches() throws Exception {
        assertThat(allowOriginFor("https://app.ecoregistru.ro/", "https://app.ecoregistru.ro"))
                .isEqualTo("https://app.ecoregistru.ro");
    }

    @Test
    void spacesAroundTheCommasAreTrimmed() throws Exception {
        assertThat(allowOriginFor("https://a.ro , https://b.ro", "https://b.ro"))
                .isEqualTo("https://b.ro");
    }

    /** An unset variable means „no browser may call this", not „every browser may". */
    @Test
    void anEmptyListAllowsNothing() throws Exception {
        assertThat(allowOriginFor("", "https://app.ecoregistru.ro")).isNull();
    }

    @Test
    void aDifferentSchemeOrPortIsADifferentOrigin() throws Exception {
        assertThat(allowOriginFor("https://app.ecoregistru.ro", "http://app.ecoregistru.ro")).isNull();
        assertThat(allowOriginFor("http://localhost:5173", "http://localhost:4173")).isNull();
    }

    /** Prefix matching would hand the API to anyone who registers the domain plus a suffix. */
    @Test
    void aLongerHostThatStartsTheSameIsNotAllowed() throws Exception {
        assertThat(allowOriginFor("https://ecoregistru.ro", "https://ecoregistru.ro.atacator.com"))
                .isNull();
    }
}
