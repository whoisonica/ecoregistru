package ro.ecoregistru.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * P0.6, the link that stayed unproven: a real 500, raised inside the application and carried by
 * {@code AdviceController.handleUnexpected} to Sentry. The ingestion was proven on 09.09.2026 with
 * a hand-sent event; nothing had ever walked the path an actual defect takes.
 *
 * <p>Only the platform admin reaches it, and it touches no data. Calling it produces exactly one
 * Sentry event, so the proof can be repeated after any change to the error path or the DSN.
 */
@RestController
@RequestMapping("/api/v1/platform")
public class PlatformDiagnosticsController {

    /** Deliberately unexpected: an exception no handler above the catch-all knows. */
    public static final class SentryProbeException extends RuntimeException {
        SentryProbeException() {
            super("Sentry probe — excepţie aruncată dinadins de administratorul platformei; se poate ignora.");
        }
    }

    @PostMapping("/sentry-probe")
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public void sentryProbe() {
        throw new SentryProbeException();
    }
}
