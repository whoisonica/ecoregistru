package ro.ecoregistru.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletResponse;
import ro.ecoregistru.exception.AdviceController;
import ro.ecoregistru.security.TenantContext;
import ro.ecoregistru.security.TooManyRequestsException;
import ro.ecoregistru.service.AuditFileService;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;

/**
 * Descărcarea dosarului ţine o conexiune la bază cât curge arhiva, deci câteva în paralel goleau
 * pool-ul pentru toate firmele (29.09.2026). O firmă primeşte un dosar odată, aplicaţia cel mult
 * {@link AuditFileController#MAX_PARALLEL}; restul, 429 cu un cod care spune ce e ocupat.
 *
 * <p>Serviciul e înlocuit cu unul care „împachetează" până îi dă proba drumul — aşa se ţine o
 * descărcare pe drum fără o bază şi fără un dosar adevărat.
 */
class AuditFileDownloadLimitTest {

    private final AuditFileService service = Mockito.mock(AuditFileService.class);
    private final AuditFileController controller = new AuditFileController(service);
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private final CountDownLatch release = new CountDownLatch(1);

    @AfterEach
    void tearDown() {
        release.countDown();
        pool.shutdownNow();
        TenantContext.clear();
    }

    @Test
    void aSecondDossierOfTheSameCompanyWaitsItsTurn() throws Exception {
        UUID company = UUID.randomUUID();
        CountDownLatch started = blockingWrites(1);
        Future<?> first = downloadInBackground(company);
        assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();

        TenantContext.set(company);
        TooManyRequestsException refused = (TooManyRequestsException) org.assertj.core.api.Assertions
                .catchThrowable(() -> controller.download(2026, 5, new MockHttpServletResponse()));
        assertThat(refused).isNotNull();
        assertThat(refused.getErrorCode()).isEqualTo("audit.file.busy");
        assertThat(new AdviceController().handleTooManyRequests(refused).getStatusCode().value()).isEqualTo(429);

        release.countDown();
        first.get(5, TimeUnit.SECONDS);
        // Locul se eliberează la capăt: următoarea descărcare trece.
        assertThatCode(() -> controller.download(2026, 1, new MockHttpServletResponse())).doesNotThrowAnyException();
    }

    @Test
    void theWholeApplicationPacksAtMostThreeAtOnce() throws Exception {
        CountDownLatch started = blockingWrites(AuditFileController.MAX_PARALLEL);
        for (int i = 0; i < AuditFileController.MAX_PARALLEL; i++) {
            downloadInBackground(UUID.randomUUID());
        }
        assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();

        TenantContext.set(UUID.randomUUID());
        assertThatThrownBy(() -> controller.download(2026, 1, new MockHttpServletResponse()))
                .isInstanceOf(TooManyRequestsException.class);
    }

    /** Fiecare scriere aşteaptă proba; zăvorul întors se deschide când au pornit {@code n} dintre ele. */
    private CountDownLatch blockingWrites(int n) {
        CountDownLatch started = new CountDownLatch(n);
        Mockito.doAnswer(inv -> {
            started.countDown();
            release.await(10, TimeUnit.SECONDS);
            return null;
        }).when(service).write(anyInt(), anyInt(), any());
        return started;
    }

    private Future<?> downloadInBackground(UUID company) throws InterruptedException {
        CountDownLatch inside = new CountDownLatch(1);
        Future<?> f = pool.submit(() -> {
            TenantContext.set(company);
            inside.countDown();
            try {
                controller.download(2026, 5, new MockHttpServletResponse());
            } finally {
                TenantContext.clear();
            }
            return null;
        });
        inside.await(5, TimeUnit.SECONDS);
        return f;
    }
}
