package ro.ecoregistru.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Uploads, signs and fetches attachment files on Cloudinary. resource_type=auto handles both
 * images (camera photos) and documents (PDF). Accepts multipart directly, so a future mobile
 * client can post camera captures without a browser flow.
 *
 * <p><b>Since 11-bis, nothing here is publicly deliverable.</b> Files go up as
 * {@code type=authenticated}, which Cloudinary refuses to serve without a signature — measured,
 * not assumed: the same asset returns 401 unsigned and 200 signed. The signed URL is built here
 * and used here; it is never returned to a client, because for an authenticated asset the signed
 * URL <em>is</em> the credential and it does not expire. Callers get bytes, not links.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CloudinaryStorageService {

    static final int FETCH_TIMEOUT_SECONDS = 20;

    final Cloudinary cloudinary;

    // Not final: @Value field, must stay out of the generated constructor.
    @Value("${app.cloudinary.folder:ecoregistru}")
    String baseFolder;

    public record StoredFile(String url, String publicId, String resourceType,
                             String deliveryType, String format) {}

    public StoredFile upload(MultipartFile file, String subFolder) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", baseFolder + "/" + subFolder,
                            "resource_type", "auto",
                            // The whole point of 11-bis: not deliverable without a signature.
                            "type", "authenticated"));
            return new StoredFile(
                    (String) result.get("secure_url"),
                    (String) result.get("public_id"),
                    (String) result.get("resource_type"),
                    (String) result.get("type"),
                    (String) result.get("format"));
        } catch (IOException e) {
            log.error("Cloudinary upload failed", e);
            throw new RuntimeException("Încărcarea fișierului a eșuat.", e);
        }
    }

    /**
     * A signed delivery URL for an authenticated asset. For rows predating 11-bis the three
     * coordinates are null and there is nothing to sign — the caller falls back to the stored URL,
     * which for those (and only those) is still public.
     */
    public String signedUrl(String publicId, String resourceType, String deliveryType, String format) {
        String source = (format == null || format.isBlank()) ? publicId : publicId + "." + format;
        return cloudinary.url()
                .resourceType(resourceType == null ? "image" : resourceType)
                .type(deliveryType == null ? "authenticated" : deliveryType)
                .signed(true)
                .generate(source);
    }

    /**
     * Fetches the asset's bytes. An overridable method rather than an inline HTTP call so tests
     * can substitute it — the alternative is a suite that only passes with a Cloudinary account
     * and a live network, which is no suite at all.
     */
    public byte[] fetch(String url) throws IOException, InterruptedException {
        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(FETCH_TIMEOUT_SECONDS)).build();
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(FETCH_TIMEOUT_SECONDS)).GET().build();
        HttpResponse<byte[]> res = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
        if (res.statusCode() != 200) {
            throw new IOException("Cloudinary a răspuns " + res.statusCode());
        }
        return res.body();
    }

    public void delete(String publicId, String resourceType, String deliveryType) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap(
                    "resource_type", resourceType == null ? "image" : resourceType,
                    // Cloudinary scopes destroy by delivery type; an authenticated asset is not
                    // found under the default "upload" and would be left behind silently.
                    "type", deliveryType == null ? "upload" : deliveryType));
        } catch (Exception e) {
            // Non-fatal: log and continue (the DB record is the source of truth for the movement).
            log.warn("Cloudinary delete failed for publicId={}", publicId, e);
        }
    }
}
