package ro.ecoregistru.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ro.ecoregistru.controller.request.EnergyCarriersRequest;
import ro.ecoregistru.controller.request.EnergyConsumptionRequest;
import ro.ecoregistru.controller.request.EnergyContactRequest;
import ro.ecoregistru.controller.request.EnergyDeclarationRequest;
import ro.ecoregistru.controller.response.EnergySheetResponse;
import ro.ecoregistru.service.energy.EnergyService;

/**
 * The annual energy declaration (Anexa 1, consum sub 1000 tep). Reading is open to every tenant member;
 * writing is not, because the figures end up on a form filed with an authority.
 */
@RestController
@RequestMapping("/api/v1/energy")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EnergyController {

    static final String CAN_WRITE = "hasAnyAuthority('PLATFORM_ADMIN','CONSULTANT','ADMIN','OPERATOR')";

    EnergyService energyService;

    @GetMapping
    public EnergySheetResponse sheet(@RequestParam int year) {
        return energyService.sheet(year);
    }

    @PutMapping("/carriers")
    @PreAuthorize(CAN_WRITE)
    public EnergySheetResponse carriers(@Valid @RequestBody EnergyCarriersRequest request) {
        return energyService.saveCarriers(request);
    }

    @PutMapping("/consumption")
    @PreAuthorize(CAN_WRITE)
    public EnergySheetResponse consumption(@Valid @RequestBody EnergyConsumptionRequest request) {
        return energyService.saveCell(request);
    }

    @PutMapping("/declaration")
    @PreAuthorize(CAN_WRITE)
    public EnergySheetResponse declaration(@Valid @RequestBody EnergyDeclarationRequest request) {
        return energyService.saveDeclaration(request);
    }

    @PutMapping("/contact")
    @PreAuthorize(CAN_WRITE)
    public EnergySheetResponse contact(@Valid @RequestBody EnergyContactRequest request) {
        return energyService.saveContact(request);
    }
}
