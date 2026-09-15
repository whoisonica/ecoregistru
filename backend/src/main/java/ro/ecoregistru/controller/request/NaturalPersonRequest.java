package ro.ecoregistru.controller.request;

import jakarta.validation.constraints.Size;
import ro.ecoregistru.util.ValidCnp;

/**
 * O persoană fizică, cum o trimite formularul din tabul „Persoane fizice” (D1.7b).
 *
 * <p>Doar numele e obligatoriu. CNP-ul, actul și domiciliul se cer abia la o operațiune cu metal
 * (OUG 31/2011 art. 1 alin. (1^2)), iar acolo le verifică {@code WeighingOperationService}. Un CNP
 * trimis trebuie totuși să fie valid: unul cu cifra de control greșită e al altcuiva.
 */
public record NaturalPersonRequest(
        @Size(max = 160) String name,
        @ValidCnp String cnp,
        @Size(max = 100) String identification,
        @Size(max = 500) String address) {
}
