package ro.ecoregistru.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.ecoregistru.controller.response.CompanyLookupResponse;
import ro.ecoregistru.exception.ErrorMessageEnum;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.service.AnafClient;

/**
 * A company's registered data by CUI, from ANAF, for the partner form. Read-only and not
 * tenant-scoped, like the waste-code nomenclator: it answers about a public registry, not about the
 * company making the request.
 *
 * <p>Signed-in users only. The public intake form could use it too, but an open door onto a service
 * that sanctions overload would make every visitor's traffic ours to answer for.
 */
@RestController
@RequestMapping("/api/v1/company-lookup")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CompanyLookupController {

    AnafClient anafClient;

    @GetMapping("/{cui}")
    public CompanyLookupResponse lookup(@PathVariable String cui) {
        return anafClient.lookup(cui)
                .map(CompanyLookupResponse::from)
                .orElseThrow(() -> new NotFoundException(ErrorMessageEnum.ANAF_CUI_NOT_FOUND));
    }
}
