package ro.ecoregistru.controller.request;

import java.util.List;
import java.util.UUID;

/** F6a — recepțiile confirmate în SIATD; codul unic al tranzacției doar când e una singură. */
public record SiatdConfirmRequest(List<UUID> operationIds, String code) {
}
