package ro.ecoregistru.controller.response;

import ro.ecoregistru.entity.DeviceSession;
import ro.ecoregistru.enums.DevicePlatform;

import java.time.Instant;
import java.util.UUID;

/**
 * Un rând din „Dispozitive conectate”. Fără hash și fără token — nimic de furat din listă.
 *
 * <p>Care e telefonul din mână nu spune serverul: clientul își știe {@code id}-ul de la login sau
 * de la ultima reîmprospătare și îl potrivește singur. Altfel ar fi trebuit un claim în JWT ca să
 * răspundem la o întrebare pe care cel care întreabă o știe deja.
 */
public record DeviceSessionResponse(
        UUID id,
        String deviceName,
        DevicePlatform platform,
        Instant createdAt,
        Instant lastUsedAt
) {
    public static DeviceSessionResponse of(DeviceSession s) {
        return new DeviceSessionResponse(s.getId(), s.getDeviceName(), s.getPlatform(),
                s.getCreatedAt(), s.getLastUsedAt());
    }
}
