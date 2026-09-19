package ro.ecoregistru.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ro.ecoregistru.controller.response.ConsultancyBrandingResponse;
import ro.ecoregistru.entity.Consultancy;
import ro.ecoregistru.entity.ConsultancyBranding;
import ro.ecoregistru.exception.BadRequestException;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.repository.ConsultancyBrandingRepository;
import ro.ecoregistru.repository.ConsultancyRepository;
import ro.ecoregistru.security.SecurityUtils;
import ro.ecoregistru.service.export.ReportBranding;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.UUID;

import static ro.ecoregistru.exception.ErrorMessageEnum.*;

/**
 * P2.14 — the consultancy's header on the unofficial reports of its companies.
 *
 * <p>The consultant edits their own consultancy's (no id in the path, as the team); every report of a
 * company of that consultancy prints it, whoever downloads. Official forms never ask for it.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReportBrandingService {

    /** A logo is a header decoration; half a megabyte is already a large one. */
    public static final int MAX_LOGO_BYTES = 500 * 1024;
    public static final int MAX_HEADER_LINE = 200;

    ConsultancyBrandingRepository brandingRepository;
    ConsultancyRepository consultancyRepository;

    public record Logo(byte[] bytes, String contentType) {}

    /** What a company's unofficial reports print, or null: a direct client, or nothing set. */
    @Transactional(readOnly = true)
    public ReportBranding forCompany(UUID companyId) {
        return brandingRepository.findForCompany(companyId).filter(b -> !b.isEmpty()).orElse(null);
    }

    // --- the consultant's own ---

    @Transactional(readOnly = true)
    public ConsultancyBrandingResponse mine() {
        Consultancy consultancy = myConsultancy();
        return toResponse(consultancy, brandingRepository.findById(consultancy.getId()).orElse(null));
    }

    @Transactional
    public ConsultancyBrandingResponse updateHeaderLine(String headerLine) {
        String line = headerLine == null || headerLine.isBlank() ? null : headerLine.strip();
        if (line != null && line.length() > MAX_HEADER_LINE) {
            throw new BadRequestException(BRANDING_HEADER_TOO_LONG);
        }
        Consultancy consultancy = myConsultancy();
        ConsultancyBranding branding = rowFor(consultancy.getId());
        branding.setHeaderLine(line);
        branding.setUpdatedAt(Instant.now());
        return toResponse(consultancy, brandingRepository.save(branding));
    }

    @Transactional
    public ConsultancyBrandingResponse uploadLogo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException(BRANDING_LOGO_INVALID);
        }
        if (file.getSize() > MAX_LOGO_BYTES) {
            throw new BadRequestException(BRANDING_LOGO_TOO_LARGE);
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        // The bytes decide, not the name or the browser's content type: a renamed .txt stays a .txt.
        String contentType = imageType(bytes);
        if (contentType == null) {
            throw new BadRequestException(BRANDING_LOGO_INVALID);
        }
        // BUG-052: 500 KB comprimați pot fi 20000 × 20000 de pixeli, adică 400 MB la decodare, la
        // fiecare raport cu antet — peste heap-ul dyno-ului. Dimensiunile se citesc din antet, fără
        // decodare, înainte de orice altceva.
        long pixels = pixels(bytes);
        if (pixels < 0) {
            throw new BadRequestException(BRANDING_LOGO_INVALID);
        }
        if (pixels > MAX_LOGO_SIDE * MAX_LOGO_SIDE) {
            throw new BadRequestException(BRANDING_LOGO_TOO_MANY_PIXELS);
        }
        if (!decodes(bytes)) {
            throw new BadRequestException(BRANDING_LOGO_INVALID);
        }
        Consultancy consultancy = myConsultancy();
        ConsultancyBranding branding = rowFor(consultancy.getId());
        branding.setLogo(bytes);
        branding.setLogoContentType(contentType);
        branding.setUpdatedAt(Instant.now());
        return toResponse(consultancy, brandingRepository.save(branding));
    }

    @Transactional
    public ConsultancyBrandingResponse deleteLogo() {
        Consultancy consultancy = myConsultancy();
        ConsultancyBranding branding = rowFor(consultancy.getId());
        branding.setLogo(null);
        branding.setLogoContentType(null);
        branding.setUpdatedAt(Instant.now());
        return toResponse(consultancy, brandingRepository.save(branding));
    }

    @Transactional(readOnly = true)
    public Logo logo() {
        return brandingRepository.findById(myConsultancy().getId())
                .filter(b -> b.getLogo() != null)
                .map(b -> new Logo(b.getLogo(), b.getLogoContentType()))
                .orElseThrow(() -> new NotFoundException(BRANDING_LOGO_NOT_FOUND));
    }

    // ---

    private Consultancy myConsultancy() {
        Consultancy fromSession = SecurityUtils.currentUser().getConsultancy();
        if (fromSession == null) {
            throw new NotFoundException(CONSULTANCY_NOT_FOUND);
        }
        // Re-read here: the session's copy belongs to another persistence context, and the name is lazy.
        return consultancyRepository.findById(fromSession.getId())
                .orElseThrow(() -> new NotFoundException(CONSULTANCY_NOT_FOUND));
    }

    private ConsultancyBranding rowFor(UUID consultancyId) {
        return brandingRepository.findById(consultancyId)
                .orElseGet(() -> ConsultancyBranding.builder().consultancyId(consultancyId).build());
    }

    private static ConsultancyBrandingResponse toResponse(Consultancy consultancy, ConsultancyBranding branding) {
        return new ConsultancyBrandingResponse(
                consultancy.getName(),
                branding == null ? null : branding.getHeaderLine(),
                branding != null && branding.getLogo() != null,
                branding == null ? null : branding.getUpdatedAt());
    }

    private static String imageType(byte[] b) {
        if (b.length > 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return "image/png";
        }
        if (b.length > 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        return null;
    }

    static final long MAX_LOGO_SIDE = 2000;

    /** Lățime × înălțime din antetul imaginii, fără s-o decodeze; -1 dacă nu se poate citi. */
    private static long pixels(byte[] bytes) {
        try (var in = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) {
                return -1;
            }
            var reader = readers.next();
            try {
                reader.setInput(in);
                return (long) reader.getWidth(0) * reader.getHeight(0);
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException ex) {
            return -1;
        }
    }

    private static boolean decodes(byte[] bytes) {
        try {
            return ImageIO.read(new ByteArrayInputStream(bytes)) != null;
        } catch (IOException | RuntimeException ex) {
            return false;
        }
    }
}
