package ro.ecoregistru.service.notification;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.DeviceSession;
import ro.ecoregistru.repository.DeviceSessionRepository;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * G2 — ce pleacă spre Expo Push și ce se întâmplă cu răspunsul. Forma e cea din documentația Expo
 * („Send push notifications using a server”); că Expo o primește se vede abia cu un token adevărat, de pe
 * un telefon cu proiect EAS.
 */
class PushNotifierTest {

    private static final PushNotifier.Message MESSAGE = new PushNotifier.Message("Titlu", "Corp", "termene");
    private static final List<AppUser> USERS = List.of(AppUser.builder().email("a@b.ro").build());
    private static final String URL = "https://expo.test/--/api/v2/push/send";

    private final DeviceSessionRepository repository = Mockito.mock(DeviceSessionRepository.class);
    private final RestClient.Builder builder = RestClient.builder().baseUrl("https://expo.test");
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

    @Test
    void offByDefaultNothingIsAskedAndNothingLeaves() {
        int sent = new PushNotifier(builder.build(), repository, false, "").send(USERS, MESSAGE);

        assertThat(sent).isZero();
        verify(repository, never()).findLiveWithPushToken(anyCollection(), any());
        server.verify();
    }

    @Test
    void theMessageGoesWithTheScreenItOpensAndTheAccessToken() {
        tokens("ExponentPushToken[aaa]");
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer cheie"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].to").value("ExponentPushToken[aaa]"))
                .andExpect(jsonPath("$[0].title").value("Titlu"))
                .andExpect(jsonPath("$[0].body").value("Corp"))
                .andExpect(jsonPath("$[0].data.screen").value("termene"))
                .andRespond(withSuccess("{\"data\":[{\"status\":\"ok\",\"id\":\"t-1\"}]}", MediaType.APPLICATION_JSON));

        assertThat(notifier("cheie").send(USERS, MESSAGE)).isEqualTo(1);
        server.verify();
    }

    /** Un telefon de pe care s-a dezinstalat aplicația nu mai primește nimic, la nesfârșit. */
    @Test
    void aTokenExpoNoLongerKnowsIsForgotten() {
        tokens("ExponentPushToken[viu]", "ExponentPushToken[dus]");
        server.expect(requestTo(URL)).andRespond(withSuccess("""
                {"data":[{"status":"ok","id":"t-1"},
                         {"status":"error","message":"not registered","details":{"error":"DeviceNotRegistered"}}]}""",
                MediaType.APPLICATION_JSON));

        assertThat(notifier("").send(USERS, MESSAGE)).isEqualTo(1);
        verify(repository).clearPushTokens(List.of("ExponentPushToken[dus]"));
    }

    /** Altă eroare (plafon, mesaj prea mare) nu șterge tokenul: telefonul există. */
    @Test
    void anotherErrorKeepsTheToken() {
        tokens("ExponentPushToken[aaa]");
        server.expect(requestTo(URL)).andRespond(withSuccess(
                "{\"data\":[{\"status\":\"error\",\"details\":{\"error\":\"MessageRateExceeded\"}}]}",
                MediaType.APPLICATION_JSON));

        assertThat(notifier("").send(USERS, MESSAGE)).isZero();
        verify(repository, never()).clearPushTokens(anyCollection());
    }

    /** Expo căzut nu e treaba alertei: mailul a plecat deja, schedulerul nu trebuie să afle nimic. */
    @Test
    void expoDownThrowsNothing() {
        tokens("ExponentPushToken[aaa]");
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        assertThat(notifier("").send(USERS, MESSAGE)).isZero();
    }

    @Test
    void moreThanAHundredPhonesGoInTwoRequests() {
        tokens(IntStream.range(0, 101).mapToObj(i -> "ExponentPushToken[t" + i + "]").toArray(String[]::new));
        server.expect(ExpectedCount.once(), requestTo(URL))
                .andExpect(jsonPath("$.length()").value(100))
                .andRespond(withSuccess("{\"data\":[]}", MediaType.APPLICATION_JSON));
        server.expect(ExpectedCount.once(), requestTo(URL))
                .andExpect(jsonPath("$.length()").value(1))
                .andRespond(withSuccess("{\"data\":[]}", MediaType.APPLICATION_JSON));

        notifier("").send(USERS, MESSAGE);
        server.verify();
    }

    private PushNotifier notifier(String accessToken) {
        return new PushNotifier(builder.build(), repository, true, accessToken);
    }

    private void tokens(String... tokens) {
        when(repository.findLiveWithPushToken(anyCollection(), any())).thenReturn(
                Arrays.stream(tokens).map(t -> DeviceSession.builder().pushToken(t).build()).toList());
    }
}
