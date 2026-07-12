package ro.ecoregistru.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.ecoregistru.controller.response.CompanyResponse;
import ro.ecoregistru.service.CompanyService;

import java.util.List;

/**
 * Platform-level company directory. The only global (NOT tenant-scoped) domain endpoint:
 * PLATFORM_ADMIN lists every tenant to drive the tenant switcher (X-Tenant-Id). A deliberate,
 * documented exception to the "everything is tenant-scoped" rule — hence the strict role gate.
 */
@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CompanyController {

    CompanyService companyService;

    @GetMapping
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public List<CompanyResponse> list() {
        return companyService.listAll();
    }
}
