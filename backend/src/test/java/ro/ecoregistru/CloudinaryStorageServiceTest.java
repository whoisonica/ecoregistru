package ro.ecoregistru;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import ro.ecoregistru.service.CloudinaryStorageService;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 11-bis, at the layer where the promise is actually kept.
 *
 * <p>{@link AttachmentAccessIT} proves our own door is locked. This proves the file behind it is
 * not simply lying in the open — that we ask Cloudinary for a non-public asset, and that we can
 * still read it ourselves.
 *
 * <p>The shapes below were measured against the real account on 09.09.2026 before any of this was
 * written: the same asset returned <b>401 unsigned</b> and <b>200 signed</b>, and an authenticated
 * upload came back with a {@code secure_url} that already carried an {@code s--…--} segment. That
 * last detail is the trap this feature is built around — for an authenticated asset the signed URL
 * <em>is</em> the credential, it never expires, and so it must never be stored or handed out. The
 * test that guards it is {@code theApiNeverHandsOutACloudinaryUrl}, next door.
 */
class CloudinaryStorageServiceTest {

    private CloudinaryStorageService service(Cloudinary cloudinary) {
        CloudinaryStorageService s = new CloudinaryStorageService(cloudinary);
        ReflectionTestUtils.setField(s, "baseFolder", "ecoregistru");
        return s;
    }

    /**
     * The one line that closes the hole. Uploading as {@code type=authenticated} is what makes
     * Cloudinary refuse the file to a bare request; with the default {@code upload} everything
     * else here would still pass and every file would still be world-readable.
     */
    @Test
    @SuppressWarnings("unchecked")
    void filesGoUpAsAuthenticated() throws Exception {
        Cloudinary cloudinary = mock(Cloudinary.class);
        Uploader uploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(), any())).thenReturn(Map.of(
                "secure_url", "https://res.cloudinary.com/c/image/authenticated/s--abc--/v1/x.pdf",
                "public_id", "ecoregistru/movements/1/x",
                "resource_type", "image", "type", "authenticated", "format", "pdf"));

        var stored = service(cloudinary).upload(
                new MockMultipartFile("file", "aviz.pdf", "application/pdf", "date".getBytes()),
                "movements/1");

        ArgumentCaptor<Map<String, Object>> options = ArgumentCaptor.forClass(Map.class);
        org.mockito.Mockito.verify(uploader).upload(any(), options.capture());
        assertThat(options.getValue())
                .containsEntry("type", "authenticated")
                .containsEntry("resource_type", "auto")
                .containsEntry("folder", "ecoregistru/movements/1");

        // All three coordinates are kept, because all three are needed to sign the URL later.
        assertThat(stored.resourceType()).isEqualTo("image");
        assertThat(stored.deliveryType()).isEqualTo("authenticated");
        assertThat(stored.format()).isEqualTo("pdf");
    }

    /**
     * The shape measured against the live account: {@code /image/authenticated/s--<8 chars>--/}.
     * Pinned because a URL that merely looks plausible is indistinguishable from a correct one
     * until Cloudinary answers 401, and by then it is a file the client cannot open.
     *
     * <p>Two things the SDK adds that surprised this test when it was first written, both checked
     * against the real account rather than reasoned about: a <b>{@code /v1/}</b> segment (it forces
     * a version onto any public id containing a slash) and an <b>{@code ?_a=} analytics
     * parameter</b>. Neither breaks delivery — {@code v1}, the asset's true version, and no version
     * at all all returned 200 on the same file, because Cloudinary treats the version as a cache
     * buster and not as part of the lookup. The signature is computed over the public id and format
     * alone, which is why the version can differ from the real one and the URL still verifies.
     */
    @Test
    void theDeliveryUrlIsSignedAndAuthenticated() {
        CloudinaryStorageService s = service(
                new Cloudinary("cloudinary://cheie:secret@cloudulnostru"));

        String url = s.signedUrl("ecoregistru/movements/1/aviz", "image", "authenticated", "pdf");

        assertThat(url).contains("/image/authenticated/");
        assertThat(url).containsPattern("/s--[A-Za-z0-9_-]{8}--/");
        assertThat(url).contains("/ecoregistru/movements/1/aviz.pdf");
    }

    /**
     * Rows uploaded before 11-bis have no delivery type; the service still has to produce
     * something rather than throw, and the caller decides whether to use it. Defaults match what
     * Cloudinary assumes when the fields are absent.
     */
    @Test
    void nullCoordinatesFallBackToCloudinaryDefaults() {
        CloudinaryStorageService s = service(
                new Cloudinary("cloudinary://cheie:secret@cloudulnostru"));

        String url = s.signedUrl("demo/sample", null, null, null);

        assertThat(url).contains("/image/authenticated/");
        assertThat(url).contains("/demo/sample");
    }
}
