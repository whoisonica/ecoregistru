package ro.ecoregistru.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.controller.request.LoginRequest;
import ro.ecoregistru.exception.BusinessException;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.VerificationRecordRepository;
import ro.ecoregistru.security.RateLimiter;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

/**
 * O adresă fără cont costă la autentificare aceeaşi muncă de bcrypt ca una cu cont (29.09.2026).
 *
 * <p>BUG-065 a făcut mesajul acelaşi; timpul rămăsese diferit: adresa necunoscută ieşea înainte de
 * bcrypt, în câteva milisecunde, iar cea cunoscută după ~100 ms. Proba nu măsoară ceasul — ar fi
 * fragilă pe CI —, ci cere ca encoderul să fie chemat şi pe drumul fără cont, cu un hash bcrypt
 * adevărat (deci cu acelaşi cost), făcut o singură dată.
 */
class LoginTimingTest {

    private final PasswordEncoder encoder = Mockito.spy(new BCryptPasswordEncoder());
    private final AppUserRepository users = Mockito.mock(AppUserRepository.class);
    private final RateLimiter rateLimiter = Mockito.mock(RateLimiter.class);
    private final AuthenticationService service = new AuthenticationService(
            Mockito.mock(JwtService.class), Mockito.mock(EmailService.class), encoder, users,
            Mockito.mock(VerificationRecordRepository.class), rateLimiter,
            Mockito.mock(DeviceSessionService.class));

    @Test
    void anUnknownAddressStillPaysForBcrypt() {
        Mockito.when(users.findByEmail(anyString())).thenReturn(Optional.empty());
        Mockito.when(rateLimiter.tryConsume(any(), anyString())).thenReturn(0L);

        for (int i = 0; i < 2; i++) {
            assertThatThrownBy(() -> service.login(new LoginRequest("nimeni@exemplu.ro", "Parola123", null, null)))
                    .isInstanceOf(BusinessException.class);
        }

        Mockito.verify(encoder, Mockito.times(2)).matches(eq("Parola123"), Mockito.argThat(h -> h.startsWith("$2")));
        // Hash-ul se face o dată, nu la fiecare încercare: altfel drumul fără cont ar costa dublu.
        Mockito.verify(encoder, Mockito.times(1)).encode(any());
    }
}
