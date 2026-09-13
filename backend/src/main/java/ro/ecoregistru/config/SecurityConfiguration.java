package ro.ecoregistru.config;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
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

    /** Publicly reachable endpoints (no auth). Everything else requires a valid token. */
    private static final String[] WHITELIST = {
            "/api/v1/auth/**",
            "/actuator/health",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
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

                        // Public intake form
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/account-requests"
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
                        new TenantFilter(),
                        JwtAuthenticationFilter.class
                );

        return http.build();
    }
}
