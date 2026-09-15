package ro.ecoregistru.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.ecoregistru.controller.response.ConsultancyOverviewResponse;
import ro.ecoregistru.service.ConsultancyOverviewService;

import java.util.List;

/**
 * P2.13, felia 2 — „Toate firmele mele". Fără firmă aleasă și fără id de cabinet în cale: cabinetul e
 * al sesiunii, ca la {@link ConsultancyTeamController}.
 */
@RestController
@RequestMapping("/api/v1/consultancy/overview")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConsultancyOverviewController {

    ConsultancyOverviewService overviewService;

    @GetMapping
    @PreAuthorize(ConsultancyTeamController.CONSULTANT_ONLY)
    public List<ConsultancyOverviewResponse> overview() {
        return overviewService.overview();
    }
}
