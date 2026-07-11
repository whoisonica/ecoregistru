package ro.ecoregistru.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cloudinary client. Configured from a single CLOUDINARY_URL
 * (cloudinary://<api_key>:<api_secret>@<cloud_name>).
 * If unset (e.g. local dev without a Cloudinary account), the app still boots but
 * uploads will fail at call time with a clear error.
 */
@Slf4j
@Configuration
public class CloudinaryConfig {

    @Value("${app.cloudinary.url:}")
    private String cloudinaryUrl;

    @Bean
    public Cloudinary cloudinary() {
        if (cloudinaryUrl == null || cloudinaryUrl.isBlank()) {
            log.warn("CLOUDINARY_URL is not set — file uploads will not work until it is configured.");
            return new Cloudinary(ObjectUtils.emptyMap());
        }
        Cloudinary cloudinary = new Cloudinary(cloudinaryUrl);
        cloudinary.config.secure = true;
        return cloudinary;
    }
}
