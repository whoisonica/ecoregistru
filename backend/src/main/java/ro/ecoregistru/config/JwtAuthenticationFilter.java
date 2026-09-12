package ro.ecoregistru.config;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.experimental.FieldDefaults;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Reads the Bearer token, validates it, and sets the authentication.
 *
 * <p>A token that is expired, malformed or signed with another key leaves the request
 * unauthenticated and the chain continues — {@link RestAuthenticationEntryPoint} then answers
 * 401. It must not throw: an exception escaping a filter is a 500, and a session that merely ran
 * out is not a server fault.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    final JwtService jwtService;
    final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);
        try {
            final String email = jwtService.extractEmail(jwt);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (JwtException | UsernameNotFoundException e) {
            // Expired, malformed, signed with another key — sau numind un cont care nu mai există.
            // Debug, not warn: this is what a month-old browser tab looks like, not an attack worth
            // a line in production logs.
            //
            // BUG-004: `UsernameNotFoundException` lipsea din prindere, iar ea nu e o `JwtException`.
            // Cum e aruncată dintr-un filtru, scăpa de tot lanţul: nu ajungea la `AdviceController`
            // (care stă după DispatcherServlet), deci ieşea pagina de eroare a containerului, cu 500
            // şi HTML, acolo unde frontendul aşteaptă plicul de 401 ca să spună „sesiunea a expirat".
            // Rândul dispare de sub sesiune la o invitaţie anulată — singurul „remove" din aplicaţie
            // care chiar şterge.
            log.debug("Rejected JWT: {}", e.getMessage());
        }
        filterChain.doFilter(request, response);
    }
}
