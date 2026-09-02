package ro.ecoregistru.controller.response;

import java.util.UUID;

/**
 * A delegate that can be named on Anexa 3.
 *
 * @param partnerId the carrier he drives for; null means he is ours, the "transportăm noi" case
 */
public record DriverResponse(
        UUID id,
        UUID partnerId,
        String partnerName,
        String name,
        String identification,
        String vehicleRegistration,
        boolean active
) {}
