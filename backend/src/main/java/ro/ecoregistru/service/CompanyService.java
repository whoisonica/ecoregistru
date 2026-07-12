package ro.ecoregistru.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ecoregistru.controller.response.CompanyResponse;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.repository.CompanyRepository;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CompanyService {

    CompanyRepository companyRepository;

    /**
     * Lists every company on the platform. Intentionally NOT tenant-scoped: this is the
     * platform-admin tenant switcher's source of truth, so it must see all tenants.
     * Access is gated at the controller (PLATFORM_ADMIN only) — never expose this to
     * normal, tenant-scoped users.
     */
    @Transactional(readOnly = true)
    public List<CompanyResponse> listAll() {
        return companyRepository.findAll().stream()
                .sorted(Comparator.comparing(Company::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toResponse)
                .toList();
    }

    private CompanyResponse toResponse(Company c) {
        return new CompanyResponse(c.getId(), c.getName(), c.getCui(), c.getType(), c.isActive());
    }
}
