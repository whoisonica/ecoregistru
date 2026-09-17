package ro.ecoregistru.service;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/**
 * Scanarea din 17.09.2026: un atașament care trimite antetele și apoi tace ținea descărcarea dosarului pe loc la
 * nesfârșit ({@code HttpRequest.timeout} nu acoperă corpul). Termenul e pe toată descărcarea.
 */
class CloudinaryFetchTest {

    @Test
    void aBodyThatStallsEndsAtTheDeadline() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/stall", exchange -> {
            exchange.sendResponseHeaders(200, 1_000);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(new byte[10]);
                out.flush();
                release.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        server.createContext("/ok", exchange -> {
            byte[] body = "pdf".getBytes();
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
        String base = "http://127.0.0.1:" + server.getAddress().getPort();
        CloudinaryStorageService storage = new CloudinaryStorageService(null);
        try {
            assertThat(storage.fetch(base + "/ok", Duration.ofSeconds(2))).isEqualTo("pdf".getBytes());
            assertTimeoutPreemptively(Duration.ofSeconds(10), () ->
                    assertThatThrownBy(() -> storage.fetch(base + "/stall", Duration.ofSeconds(1)))
                            .isInstanceOf(IOException.class));
        } finally {
            release.countDown();
            server.stop(0);
        }
    }
}
