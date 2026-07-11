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
import java.util.Map;

/**
 * Uploads/deletes attachment files on Cloudinary. resource_type=auto handles
 * both images (camera photos) and documents (PDF). Accepts multipart directly,
 * so a future mobile client can post camera captures without a browser flow.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CloudinaryStorageService {

    final Cloudinary cloudinary;

    // Not final: @Value field, must stay out of the generated constructor.
    @Value("${app.cloudinary.folder:ecoregistru}")
    String baseFolder;

    public record StoredFile(String url, String publicId) {}

    public StoredFile upload(MultipartFile file, String subFolder) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", baseFolder + "/" + subFolder,
                            "resource_type", "auto"));
            String url = (String) result.get("secure_url");
            String publicId = (String) result.get("public_id");
            return new StoredFile(url, publicId);
        } catch (IOException e) {
            log.error("Cloudinary upload failed", e);
            throw new RuntimeException("Încărcarea fișierului a eșuat.", e);
        }
    }

    public void delete(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "auto"));
        } catch (Exception e) {
            // Non-fatal: log and continue (the DB record is the source of truth for the movement).
            log.warn("Cloudinary delete failed for publicId={}", publicId, e);
        }
    }
}
