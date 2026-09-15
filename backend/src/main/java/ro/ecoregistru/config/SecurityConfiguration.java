package ro.ecoregistru.config;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.SubscriptionRepository;
import ro.ecoregistru.security.SubscriptionAccessFilter;
import ro.ecoregistru.security.TenantFilter;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SecurityConfiguration {

    final AuthenticationProvider authenticationProvider;
    final JwtAuthenticationFilter jwtAuthenticationFilter;
    final RestAuthenticationEntryPoint authenticationEntryPoint;
    /** For {@link TenantFilter}: whether a consultant's consultancy manages the requested tenant. */
    final CompanyRepository companyRepository;
    /** For {@link SubscriptionAccessFilter}: who pays for the account, and whether it is read-only. */
    final SubscriptionRepository subscriptionRepository;

    /** Publicly reachable endpoints (no auth). Everything else requires a valid token. */
    private static final String[] WHITELIST = {
            "/api/v1/auth/**",
            "/actuator/health"
    };

    /**
     * BUG-011: public only while the docs are switched on, which is the {@code dev} profile alone.
     * With the docs off these paths fall under {@code authenticated()}, so a stranger gets 401 rather
     * than a map of the API, and a signed-in user gets 404.
     */
    private static final String[] API_DOCS = {
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            @Value("${springdoc.api-docs.enabled:true}") boolean apiDocsEnabled,
            @Value("${app.billing.read-only-enabled:false}") boolean readOnlyEnabled) throws Exception {
        if (apiDocsEnabled) {
            http.authorizeHttpRequests(auth -> auth.requestMatchers(API_DOCS).permitAll());
        }
        http
                // CORS is handled by CorsConfig
                .cors(cors -> {})

                // CSRF is not needed for this stateless JWT API
                .csrf(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(auth -> auth
                        // CORS preflight requests must always be allowed
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Public authentication endpoints
                        .requestMatchers(WHITELIST).permitAll()

                        // Public intake form; NETOPIA's payment notification, verified by its signature
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/account-requests",
                                "/api/v1/billing/netopia/ipn"
                        ).permitAll()

                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )

                // Return 401 when there is no valid authentication
                .exceptionHandling(ex ->
                        ex.authenticationEntryPoint(authenticationEntryPoint)
                )

                // JWT API → stateless sessions
                .sessionManagement(sm ->
                        sm.sessionCreationPolicy(STATELESS)
                )

                .authenticationProvider(authenticationProvider)

                // Authenticate JWT before Spring's username/password filter
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )

                // Resolve tenant after JWT authentication
                .addFilterAfter(
                        new TenantFilter(companyRepository),
                        JwtAuthenticationFilter.class
                )

                // F4 — read-only subscriptions write nothing; needs the tenant, so after TenantFilter
                .addFilterAfter(
                        new SubscriptionAccessFilter(subscriptionRepository, readOnlyEnabled),
                        TenantFilter.class
                );

        return http.build();
    }
}
