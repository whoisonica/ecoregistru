package ro.ecoregistru;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.service.EmailService;

import java.util.Map;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P1.12 — a client firm administers its own users.
 *
 * <p>What this is really testing is the row of guards, not the CRUD. The five ways a firm can hurt
 * itself with this screen — lock itself out, resurrect a month-old session, hand somebody a
 * password-reset link for a live colleague's account, activate an account nobody can sign in to,
 * reach into another tenant — are each one method below. The happy paths are here to make the
 * refusals mean something.
 *
 * <p>Every test builds its own throwaway users through the invite endpoint, so nothing depends on
 * the order they run in. The three seeded demo accounts are only ever read.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class CompanyUsersIT {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired AppUserRepository appUserRepository;
    @Autowired ObjectMapper objectMapper;

    /** Mocked so the invite mails go nowhere and can still be counted. */
    @MockBean EmailService emailService;

    private String adminToken;
    private String operatorToken;
    private String platformToken;

    @BeforeEach
    void setUp() {
        adminToken = tokenFor("admin@demo.ro");
        operatorToken = tokenFor("operator@demo.ro");
        platformToken = tokenFor("platform@ecoregistru.ro");
    }

    // --- the list ---

    @Test
    void adminSeesTheirOwnColleagues() throws Exception {
        mockMvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].email", hasItem("admin@demo.ro")))
                .andExpect(jsonPath("$[*].email", hasItem("operator@demo.ro")))
                .andExpect(jsonPath("$[*].email", hasItem("viewer@demo.ro")));
    }

    /**
     * The platform admin's own row must not show up in any tenant's list. It cannot: that account
     * has no company, and every query here is scoped by one. Asserted anyway, because it is the
     * property the whole controller leans on.
     */
    @Test
    void theListNeverContainsThePlatformAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.email == 'platform@ecoregistru.ro')]").isEmpty());
    }

    @Test
    void operatorMayNotSeeWhoElseWorksHere() throws Exception {
        mockMvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$['error-code']", is("access.denied")));
    }

    // --- invite ---

    @Test
    void adminInvitesOntoTheirOwnCompanyWithoutNamingIt() throws Exception {
        String email = uniqueEmail("nou");

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inviteBody(email, "OPERATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is(email)))
                .andExpect(jsonPath("$.status", is("PENDING_INVITE")))
                .andExpect(jsonPath("$.enabled", is(false)));

        AppUser created = appUserRepository.findByEmail(email).orElseThrow();
        // The company came from the session, not from the request body — there is nowhere to put it.
        assertNotNull(created.getCompany());
        assertEquals(
                appUserRepository.findByEmail("admin@demo.ro").orElseThrow().getCompany().getId(),
                created.getCompany().getId());
        assertNull(created.getDeactivatedAt());
    }

    @Test
    void adminMayNotInviteAPlatformAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inviteBody(uniqueEmail("sef"), "PLATFORM_ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']", is("invite.role.invalid")));
    }

    // --- resend ---

    @Test
    void theInviteCanBeSentAgainToSomebodyWhoNeverCameIn() throws Exception {
        String id = invite(uniqueEmail("uitat"), "OPERATOR");

        mockMvc.perform(post("/api/v1/users/" + id + "/resend-invite")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // Once for the invite itself, once for the resend.
        verify(emailService, times(2)).sendPasswordResetEmail(any(), anyString());
    }

    /**
     * An admin must not be able to mint a password-reset link for a colleague's live account.
     * That is a different feature from "resend the invitation", and a worse one.
     */
    @Test
    void theInviteIsNotResentToAnAccountThatAlreadyHasAPassword() throws Exception {
        String viewerId = appUserRepository.findByEmail("viewer@demo.ro").orElseThrow().getId().toString();

        mockMvc.perform(post("/api/v1/users/" + viewerId + "/resend-invite")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']", is("user.not.pending")));
    }

    // --- deactivate / reactivate ---

    /** The row P0.4 was built for: the button says access is gone, so access must be gone. */
    @Test
    void deactivatingClosesTheOpenSessionOnTheNextRequest() throws Exception {
        String email = uniqueEmail("pleaca");
        String id = invite(email, "OPERATOR");
        AppUser user = enable(email);
        String theirToken = jwtService.generateToken(user);

        mockMvc.perform(get("/api/v1/work-points").header("Authorization", "Bearer " + theirToken))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/users/" + id).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/work-points").header("Authorization", "Bearer " + theirToken))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$[?(@.email == '" + email + "')].status", hasItem("DEACTIVATED")));
    }

    /**
     * The reason deactivation bumps the session counter and not just {@code enabled}. Tokens live
     * thirty days; without the bump, switching somebody back on would also switch back on every
     * token issued before they left — including the one on the laptop that was the reason.
     */
    @Test
    void reactivatingGivesBackTheAccountButNotTheOldSessions() throws Exception {
        String email = uniqueEmail("revine");
        String id = invite(email, "OPERATOR");
        AppUser user = enable(email);
        String tokenFromBefore = jwtService.generateToken(user);

        mockMvc.perform(delete("/api/v1/users/" + id).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/users/" + id + "/reactivate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        AppUser back = appUserRepository.findById(UUID.fromString(id)).orElseThrow();
        assertEquals(true, back.isEnabled());
        assertNull(back.getDeactivatedAt());

        // The account works again — a freshly issued token is fine.
        mockMvc.perform(get("/api/v1/work-points")
                        .header("Authorization", "Bearer " + jwtService.generateToken(back)))
                .andExpect(status().isOk());

        // The old one does not come back with it.
        mockMvc.perform(get("/api/v1/work-points").header("Authorization", "Bearer " + tokenFromBefore))
                .andExpect(status().isUnauthorized());
    }

    /**
     * An invited user is not a deactivated one. Enabling them would produce a row that reads
     * Active over the random password {@code inviteUser} generated — unable to sign in, forever,
     * with nothing on the screen saying why.
     */
    @Test
    void aPendingInviteIsNotSomethingToReactivate() throws Exception {
        String id = invite(uniqueEmail("inca-nu"), "CLIENT_VIEWER");

        mockMvc.perform(post("/api/v1/users/" + id + "/reactivate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']", is("user.not.deactivated")));
    }

    /**
     * The door to that broken state, shut. Found by running the endpoints against a live server
     * rather than by reading them: deactivating a pending invite was allowed, and reactivating it
     * afterwards would have set {@code enabled = true} over the unusable random password — the
     * exact row {@code V34} was added to make impossible, reached one step further along.
     *
     * <p>So DEACTIVATED is reachable only from ACTIVE, and everybody in it has a password.
     */
    @Test
    void aPendingInviteIsCancelledRatherThanDeactivated() throws Exception {
        String email = uniqueEmail("gresit");
        String id = invite(email, "OPERATOR");

        mockMvc.perform(delete("/api/v1/users/" + id).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']", is("user.still.pending")));

        mockMvc.perform(delete("/api/v1/users/" + id + "/invitation")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // The row is gone, so the address is free to invite again — the point of cancelling.
        assertTrue(appUserRepository.findByEmail(email).isEmpty());
        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inviteBody(email, "OPERATOR")))
                .andExpect(status().isOk());
    }

    /** An account somebody has actually used is never deleted: the movements name it. */
    @Test
    void anAccountThatHasBeenUsedIsNotCancelled() throws Exception {
        String email = uniqueEmail("vechi");
        String id = invite(email, "OPERATOR");
        enable(email);

        mockMvc.perform(delete("/api/v1/users/" + id + "/invitation")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']", is("user.not.invitation")));
    }

    // --- the guards that keep a firm inside its own account ---

    @Test
    void anAdminMayNotSwitchThemselvesOff() throws Exception {
        String selfId = appUserRepository.findByEmail("admin@demo.ro").orElseThrow().getId().toString();

        mockMvc.perform(delete("/api/v1/users/" + selfId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']", is("user.cannot.manage.self")));
    }

    /**
     * A firm whose only enabled admin is switched off has no way back in: nothing left in it can
     * invite, promote or reactivate anybody.
     *
     * <p>On a company of its own, not on the demo one. The first version of this test used the
     * seeded firm and passed in isolation — then another test in this class left a second enabled
     * admin behind, the count stopped being 1, and this one silently <em>deactivated
     * admin@demo.ro</em> instead of failing. Every later test in the class then came back 401 or
     * 403 for reasons that had nothing to do with what they were testing.
     */
    @Test
    void theLastEnabledAdminIsNotSwitchedOff() throws Exception {
        String companyId = createCompany();
        String adminId = inviteInto(companyId, "ADMIN");

        mockMvc.perform(delete("/api/v1/users/" + adminId)
                        .header("Authorization", "Bearer " + platformToken)
                        .header("X-Tenant-Id", companyId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']", is("user.last.admin")));
    }

    /** Demotion is the other way out of the same door. */
    @Test
    void theLastEnabledAdminIsNotDemotedEither() throws Exception {
        String companyId = createCompany();
        String adminId = inviteInto(companyId, "ADMIN");

        mockMvc.perform(put("/api/v1/users/" + adminId + "/role")
                        .header("Authorization", "Bearer " + platformToken)
                        .header("X-Tenant-Id", companyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("role", "OPERATOR"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']", is("user.last.admin")));
    }

    /** With a second enabled admin in the room, the same call goes through. */
    @Test
    void withCoverTheAdminCanBeDemoted() throws Exception {
        String companyId = createCompany();
        String firstAdminId = inviteInto(companyId, "ADMIN");
        inviteInto(companyId, "ADMIN");

        mockMvc.perform(put("/api/v1/users/" + firstAdminId + "/role")
                        .header("Authorization", "Bearer " + platformToken)
                        .header("X-Tenant-Id", companyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("role", "OPERATOR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role", is("OPERATOR")));
    }

    /**
     * An admin who never set a password is not cover: they cannot let anybody back in. So the
     * count is of enabled admins, and a pending one leaves the last real admin still last.
     */
    @Test
    void aPendingAdminDoesNotCountAsCover() throws Exception {
        String companyId = createCompany();
        String adminId = inviteInto(companyId, "ADMIN");
        invitePendingInto(companyId, "ADMIN");   // invited, never enabled

        mockMvc.perform(delete("/api/v1/users/" + adminId)
                        .header("Authorization", "Bearer " + platformToken)
                        .header("X-Tenant-Id", companyId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']", is("user.last.admin")));
    }

    @Test
    void nobodyIsPromotedToPlatformAdmin() throws Exception {
        String id = invite(uniqueEmail("ambitios"), "OPERATOR");

        mockMvc.perform(put("/api/v1/users/" + id + "/role")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("role", "PLATFORM_ADMIN"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']", is("invite.role.invalid")));
    }

    /**
     * Another firm's user id is 404, not 403: the answer must not confirm that the id is real
     * somewhere else.
     */
    @Test
    void anotherTenantsUserDoesNotExistFromInHere() throws Exception {
        String strangerId = inviteIntoAFreshCompany();

        mockMvc.perform(delete("/api/v1/users/" + strangerId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$['error-code']", is("user.not.found")));

        mockMvc.perform(put("/api/v1/users/" + strangerId + "/role")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("role", "OPERATOR"))))
                .andExpect(status().isNotFound());
    }

    // --- helpers ---

    /** Invites onto the demo company through the endpoint under test; returns the new id. */
    private String invite(String email, String role) throws Exception {
        String body = mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inviteBody(email, role)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    /**
     * What using the reset link does, without the link: the invitee now has a password. Written
     * directly because the point of these tests is what happens <em>after</em> that, and the
     * reset flow has its own.
     */
    private AppUser enable(String email) {
        AppUser user = appUserRepository.findByEmail(email).orElseThrow();
        user.setEnabled(true);
        return appUserRepository.saveAndFlush(user);
    }

    /** A user on a company the demo admin has nothing to do with. */
    private String inviteIntoAFreshCompany() throws Exception {
        return invitePendingInto(createCompany(), "OPERATOR");
    }

    /** A brand-new tenant, so a test that counts admins counts only its own. */
    private String createCompany() throws Exception {
        long n = Math.floorMod(UUID.randomUUID().getMostSignificantBits(), 100_000_000L);
        String body = objectMapper.writeValueAsString(Map.of(
                "name", "Străină " + n + " SRL",
                "cui", String.format("RO%08d", n),
                "type", "GENERATOR",
                "afmObligation", false));
        String created = mockMvc.perform(post("/api/v1/companies")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(created).get("id").asText();
    }

    /** Invites onto another company and puts them through the reset link. Returns the new id. */
    private String inviteInto(String companyId, String role) throws Exception {
        String email = uniqueEmail("membru");
        String id = invitePendingInto(companyId, role, email);
        enable(email);
        return id;
    }

    private String invitePendingInto(String companyId, String role) throws Exception {
        return invitePendingInto(companyId, role, uniqueEmail("membru"));
    }

    private String invitePendingInto(String companyId, String role, String email) throws Exception {
        String body = mockMvc.perform(post("/api/v1/companies/" + companyId + "/users")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inviteBody(email, role)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    private String inviteBody(String email, String role) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "email", email, "role", role, "firstName", "Test", "lastName", "User"));
    }

    private String uniqueEmail(String prefix) {
        return prefix + "+" + UUID.randomUUID().toString().substring(0, 8) + "@client.ro";
    }

    private String tokenFor(String email) {
        return jwtService.generateToken(appUserRepository.findByEmail(email).orElseThrow());
    }
}
