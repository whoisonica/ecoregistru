package ro.ecoregistru;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.entity.WasteMovement;
import ro.ecoregistru.repository.PartnerRepository;
import ro.ecoregistru.repository.WasteMovementRepository;
import ro.ecoregistru.repository.WasteCodeRepository;
import ro.ecoregistru.repository.WorkPointRepository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Paginarea, căutarea și sortarea, de când s-au mutat de la browser la server (P3.1).
 *
 * <p>Proba nu e „vin 25 de rânduri". Un tabel paginat greșit arată exact ca unul paginat corect,
 * dar pierde rânduri pe drum — două mișcări egale pe coloana sortată se pot plimba între pagina 1
 * și pagina 2 la două cereri diferite, iar una dintre ele nu se vede niciodată. De asta proba de
 * paginare adună paginile și verifică <b>reuniunea</b>, nu lungimile.
 *
 * <p>Cealaltă jumătate e căutarea, care <b>trebuia</b> să se mute odată cu paginarea: o casetă care
 * caută doar în cele 25 de rânduri de pe ecran răspunde sigur pe sine și greșit. Regulile ei sunt
 * cele din `useTableView.ts`, iar probele de mai jos sunt scrise pe exemplele din comentariile lui:
 * „deseuri" fără diacritice, expresia „15 01 02" care nu e trei numere, și „02" care nu are voie
 * să prindă „2026".
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class MovementPagingIT {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired AppUserRepository appUserRepository;
    @Autowired ObjectMapper objectMapper;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired PartnerRepository partnerRepository;
    @Autowired WasteMovementRepository movementRepository;

    private String token;
    private UUID workPointId;
    private UUID partnerId;

    /*
     * Fiecare probă își are anul ei, și nu din cochetărie.
     *
     * <p>Clasa asta nu rulează în tranzacții care se dau înapoi: rândurile scrise de o probă rămân
     * în bază pentru următoarea. Lecția e plătită de două ori în proiectul ăsta — rândurile lăsate
     * de o probă devin premisa alteia, iar proba a doua trece sau cade după ordinea în care JUnit
     * s-a hotărât să le ruleze. Un an per probă le desparte prin construcție, fiindcă `?year=` e un
     * filtru real al endpointului, iar anii de mai jos nu sunt atinși nici de seed, nici de restul
     * suitei. Și fiecare probă își afirmă premisa: `totalElements` e cât a scris ea.
     */

    @BeforeEach
    void setUp() throws Exception {
        AppUser admin = appUserRepository.findByEmail("admin@demo.ro").orElseThrow();
        token = jwtService.generateToken(admin);
        workPointId = workPointRepository.findAllByCompany_Id(admin.getCompany().getId()).get(0).getId();
        partnerId = partnerRepository.findAllByCompany_Id(admin.getCompany().getId()).stream()
                .filter(p -> p.getName().equals("Transport Deșeuri SRL"))
                .findFirst().orElseThrow().getId();
    }

    /** Șapte rânduri tăiate în pagini de trei: nimic pierdut, nimic de două ori. */
    @Test
    void pagesCoverEveryRowExactlyOnce() throws Exception {
        String year = "2011";
        List<UUID> created = new ArrayList<>();
        for (int day = 1; day <= 7; day++) {
            created.add(create(year + "-03-%02d".formatted(day), "20 01 01", null, null));
        }

        Set<UUID> seen = new LinkedHashSet<>();
        JsonNode first = page("year", year, "size", "3", "page", "0");
        assertThat(first.get("totalElements").asLong()).isEqualTo(7);
        assertThat(first.get("totalPages").asInt()).isEqualTo(3);
        assertThat(first.get("size").asInt()).isEqualTo(3);

        for (int p = 0; p < 3; p++) {
            List<UUID> ids = idsOf(page("year", year, "size", "3", "page", String.valueOf(p)));
            for (UUID id : ids) {
                assertThat(seen.add(id)).as("rândul %s apare pe două pagini", id).isTrue();
            }
        }
        assertThat(seen).containsExactlyInAnyOrderElementsOf(created);
    }

    /**
     * O pagină dincolo de ultima vine goală, nu cu o eroare — un rând șters de altcineva scurtează
     * tabelul sub tine, iar un 400 în locul unei pagini goale ar fi o pagină de eroare pentru o
     * apăsare pe „înainte".
     */
    @Test
    void aPagePastTheEndIsEmptyNotAnError() throws Exception {
        String year = "2012";
        create(year + "-04-01", "20 01 01", null, null);
        JsonNode beyond = page("year", year, "size", "10", "page", "9");
        assertThat(beyond.get("content")).isEmpty();
        assertThat(beyond.get("totalElements").asLong()).isEqualTo(1);
    }

    /** Cine tastează „deseuri" la o tastatură fără layout românesc caută „Transport Deșeuri SRL". */
    @Test
    void searchIgnoresDiacritics() throws Exception {
        String year = "2013";
        UUID withPartner = create(year + "-05-10", "20 01 01", partnerId, null);
        create(year + "-05-11", "20 01 01", null, null);

        assertThat(idsOf(page("year", year, "search", "deseuri"))).containsExactly(withPartner);
        assertThat(idsOf(page("year", year, "search", "deșeuri"))).containsExactly(withPartner);
    }

    /**
     * „15 01 02" e un cod, nu trei numere: dacă îl poartă cineva ca atare, numai el se arată.
     *
     * <p>Rândul al doilea e ales anume — codul lui, `15 01 07`, împarte primele două grupe cu cel
     * căutat, iar data lui poartă un `02`. O căutare pe cuvinte independente l-ar fi scos și pe el.
     */
    @Test
    void thePhraseWinsOverItsWords() throws Exception {
        String year = "2014";
        UUID exact = create(year + "-06-10", "15 01 02", null, null);
        create(year + "-06-02", "15 01 07", null, null);

        assertThat(idsOf(page("year", year, "search", "15 01 02"))).containsExactly(exact);
    }

    /**
     * Când expresia nu se potrivește nicăieri, cuvintele preiau — dar fiecare trebuie să
     * <b>înceapă</b> un cuvânt al rândului.
     *
     * <p>„hamburger 15 01" nu e expresia nimănui și descrie totuși un rând exact; aici perechea e
     * „deseuri 15". Iar „deseuri 02" nu are voie să scoată nimic: singurul `02` de pe rând stă
     * înăuntrul lui `2026`… sau, aici, al lui `2015`. Asta a fost greșeala reparată pe 07.09.2026,
     * când o căutare după un cod de deșeu întorcea alt cod de deșeu.
     */
    @Test
    void aWordMustStartAWord() throws Exception {
        String year = "2015";
        UUID row = create(year + "-07-19", "15 01 02", partnerId, null);

        assertThat(idsOf(page("year", year, "search", "deseuri 15"))).containsExactly(row);
        assertThat(idsOf(page("year", year, "search", "deseuri 015"))).isEmpty();
    }

    /** O mișcare fără partener rămâne găsibilă după codul ei: joinurile sunt LEFT, nu INNER. */
    @Test
    void aMovementWithoutAPartnerIsStillSearchable() throws Exception {
        String year = "2016";
        UUID orphan = create(year + "-08-08", "15 01 07", null, "AVIZ-5150");

        assertThat(idsOf(page("year", year, "search", "AVIZ-5150"))).containsExactly(orphan);
        assertThat(idsOf(page("year", year, "search", "15 01 07"))).contains(orphan);
    }

    /**
     * „De cântărit" stă la coadă în ambele sensuri: nu e nici cea mai mică, nici cea mai mare
     * cantitate, e nespusă. Regula e `missingLast` din `useTableView.ts`, aici `NULLS LAST`.
     */
    @Test
    void missingQuantityStaysLastInBothDirections() throws Exception {
        String year = "2017";
        UUID light = create(year + "-09-01", "20 01 01", null, null, "1.000");
        UUID heavy = create(year + "-09-02", "20 01 01", null, null, "9.000");
        UUID unweighed = createAwaitingWeight(year + "-09-03");

        assertThat(idsOf(page("year", year, "sort", "quantity", "asc", "true")))
                .containsExactly(light, heavy, unweighed);
        assertThat(idsOf(page("year", year, "sort", "quantity", "asc", "false")))
                .containsExactly(heavy, light, unweighed);
    }

    /**
     * Ce se cere prin adresă nu ajunge niciodată nefiltrat la baza de date: o pagină de zece mii
     * ar fi vechea problemă înapoi, iar un `?sort=` liber ar sorta după coloane pe care ecranul nu
     * le arată. Amândouă se îndoaie, nu se refuză — un semn vechi din bara de adrese trebuie să
     * deschidă un tabel, nu o eroare.
     */
    @Test
    void sizeIsCappedAndAnUnknownSortFallsBack() throws Exception {
        String year = "2018";
        create(year + "-10-01", "20 01 01", null, null);

        assertThat(page("year", year, "size", "10000").get("size").asInt()).isEqualTo(200);
        assertThat(page("year", year, "sort", "password").get("content")).isNotEmpty();
    }


    /**
     * Registrul de predări întreabă „ce a plecat de pe amplasament", nu „ce are nevoie de cod R/D".
     *
     * <p>Până la P3.1 întrebarea se punea în browser, peste tot ce venise de la server. Acum o pune
     * baza de date — altfel registrul ar fi arătat prima pagină de **mișcări**, filtrată după aceea
     * la câte ieșiri se nimeriseră pe ea.
     */
    @Test
    void leftSiteKeepsOnlyWhatWentOut() throws Exception {
        String year = "2020";
        create(year + "-02-03", "20 01 01", null, null);
        UUID handedOver = createHandover(year + "-02-04", null);

        assertThat(idsOf(page("year", year, "leftSite", "true"))).containsExactly(handedOver);
        assertThat(idsOf(page("year", year))).hasSize(2);
    }

    /**
     * „Arată-mi doar ce blochează depunerea" — linkul de pe Panou către rândurile fără cod R/D.
     *
     * <p>Rândul de probă e făcut cum se nasc și cele adevărate: o mișcare corectă, căreia i se
     * șterge codul direct în bază. Prin API nu se poate crea una fără cod — și e bine că nu se
     * poate; cele fără cod sunt rânduri vechi, scrise înainte ca el să fie obligatoriu.
     */
    @Test
    void missingOperationCodeIsolatesTheRowsThatBlockTheFiling() throws Exception {
        String year = "2021";
        UUID complete = createHandover(year + "-03-03", null);
        UUID broken = createHandover(year + "-03-04", null);
        WasteMovement legacy = movementRepository.findById(broken).orElseThrow();
        legacy.setOperationCode(null);
        movementRepository.save(legacy);

        assertThat(idsOf(page("year", year, "leftSite", "true", "missingOperationCode", "true")))
                .containsExactly(broken);
        assertThat(idsOf(page("year", year, "leftSite", "true"))).contains(complete, broken);
    }

    /**
     * Coloana „Data predării" arată descărcarea când se știe și data mișcării altfel — iar antetul
     * apăsat trebuie să așeze rândurile după cifrele scrise în ele, nu după o coloană ascunsă.
     *
     * <p>Rândurile sunt alese ca cele două ordini să fie <b>diferite</b>: mișcarea din 10 descărcată
     * pe 25 stă după cea din 20 care n-a fost descărcată. Sortată după `unloadDate` gol, ar fi
     * ieșit invers — de asta cheia e o expresie, nu o coloană.
     */
    @Test
    void handoverDateSortsByWhatTheColumnShows() throws Exception {
        String year = "2022";
        UUID unloadedLate = createHandover(year + "-04-10", year + "-04-25");
        UUID neverUnloaded = createHandover(year + "-04-20", null);

        assertThat(idsOf(page("year", year, "leftSite", "true", "sort", "handoverDate", "asc", "true")))
                .containsExactly(neverUnloaded, unloadedLate);
    }

    /**
     * Cele două cifre ale Panoului, socotite de bază — și de ce stau în proba paginării.
     *
     * <p>Sunt aici fiindcă <b>de paginare atârnă</b>: până la P3.1 panoul cerea toate mișcările
     * lunii și le aduna în browser, iar cu 25 de rânduri pe pagină aceeași adunare ar fi dat totalul
     * unei pagini sub titlul „luna aceasta". Endpointul `/summary` există ca răspunsul să fie al
     * lunii, nu al feliei aduse.
     *
     * <p>Rândurile sunt alese ca unitățile să se bată cap în cap: 1000 kg și 1 tonă sunt aceeași
     * cantitate, iar o adunare pe cantitatea brută ar fi scris 1001. Plus una plecată fără cântar,
     * care se numără ca mișcare și <b>nu</b> adaugă un zero la kilograme.
     */
    @Test
    void theMonthSummaryAddsTonnesAsKilograms() throws Exception {
        String year = "2019";
        create(year + "-11-05", "20 01 01", null, null, "1000.000");
        createTonnes(year + "-11-06", "1.000");
        createAwaitingWeight(year + "-11-07");

        JsonNode summary = summary(year, "11");
        assertThat(summary.get("movements").asLong()).isEqualTo(3);
        assertThat(summary.get("quantityKg").asDouble()).isEqualTo(2000.0);
    }

    /** O lună fără mișcări răspunde cu zerouri, nu cu null: pe ecran se scrie o cifră. */
    @Test
    void anEmptyMonthSummarisesToZero() throws Exception {
        JsonNode summary = summary("2019", "12");
        assertThat(summary.get("movements").asLong()).isZero();
        assertThat(summary.get("quantityKg").asDouble()).isZero();
    }

    // ---------- helpers ----------

    private UUID create(String date, String code, UUID partner, String docRef) throws Exception {
        return create(date, code, partner, docRef, "5.000");
    }

    private UUID create(String date, String code, UUID partner, String docRef, String quantity) throws Exception {
        UUID wasteCodeId = wasteCodeRepository.findByCode(code).orElseThrow().getId();
        String body = """
                {
                  "workPointId": "%s",
                  "date": "%s",
                  "wasteCodeId": "%s",
                  "quantity": %s,
                  "unit": "KG",
                  "physicalState": "SOLID",
                  "operation": "GENERATED"%s%s
                }
                """.formatted(workPointId, date, wasteCodeId, quantity,
                partner == null ? "" : ",\n  \"partnerId\": \"" + partner + "\"",
                docRef == null ? "" : ",\n  \"documentReference\": \"" + docRef + "\"");
        return createdId(body);
    }

    /**
     * O mișcare plecată fără cântar: cantitatea vine de la destinatar, mai târziu.
     *
     * <p>Are partener fiindcă <b>trebuie</b> să aibă unul — cineva face cântărirea, și acela e cel
     * care ia deșeul (`WEIGHING_NEEDS_RECIPIENT`).
     */
    private UUID createAwaitingWeight(String date) throws Exception {
        UUID wasteCodeId = wasteCodeRepository.findByCode("20 01 01").orElseThrow().getId();
        return createdId("""
                {
                  "workPointId": "%s",
                  "date": "%s",
                  "wasteCodeId": "%s",
                  "weighedAtUnloading": true,
                  "partnerId": "%s",
                  "unit": "KG",
                  "physicalState": "SOLID",
                  "operation": "GENERATED"
                }
                """.formatted(workPointId, date, wasteCodeId, partnerId));
    }


    /**
     * O predare adevărată: are destinatar și cod R, fiindcă fără ele API-ul o refuză.
     *
     * <p>Și are <b>registrul spus pe față</b>. Firma demo e `BOTH` — și generator, și colector —
     * deci la o ieșire serverul nu poate deduce de unde vine deșeul și întreabă
     * (`movement.register.required`). `ANEXA_1` e răspunsul „din activitatea proprie", adică
     * exact ce arată registrul de predări.
     */
    private UUID createHandover(String date, String unloadDate) throws Exception {
        UUID wasteCodeId = wasteCodeRepository.findByCode("20 01 01").orElseThrow().getId();
        return createdId("""
                {
                  "workPointId": "%s",
                  "date": "%s",
                  "wasteCodeId": "%s",
                  "quantity": 3.000,
                  "unit": "KG",
                  "physicalState": "SOLID",
                  "operation": "RECOVERED",
                  "register": "ANEXA_1",
                  "operationCode": "R13",
                  "partnerId": "%s"%s
                }
                """.formatted(workPointId, date, wasteCodeId, partnerId,
                unloadDate == null ? "" : ",\n  \"unloadDate\": \"" + unloadDate + "\""));
    }

    /** Aceeași mișcare, dar cântărită în tone — perechea care arată dacă suma normalizează. */
    private UUID createTonnes(String date, String quantity) throws Exception {
        UUID wasteCodeId = wasteCodeRepository.findByCode("20 01 01").orElseThrow().getId();
        return createdId("""
                {
                  "workPointId": "%s",
                  "date": "%s",
                  "wasteCodeId": "%s",
                  "quantity": %s,
                  "unit": "TONS",
                  "physicalState": "SOLID",
                  "operation": "GENERATED"
                }
                """.formatted(workPointId, date, wasteCodeId, quantity));
    }

    /** Numit așa, nu `post`, ca să nu acopere `MockMvcRequestBuilders.post` importat static. */
    private UUID createdId(String body) throws Exception {
        String created = mockMvc.perform(post("/api/v1/movements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(created).get("id").asText());
    }

    private JsonNode page(String... params) throws Exception {
        var request = get("/api/v1/movements").header("Authorization", "Bearer " + token);
        for (int i = 0; i < params.length; i += 2) {
            request = request.param(params[i], params[i + 1]);
        }
        return objectMapper.readTree(mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    private JsonNode summary(String year, String month) throws Exception {
        return objectMapper.readTree(mockMvc.perform(get("/api/v1/movements/summary")
                        .header("Authorization", "Bearer " + token)
                        .param("year", year)
                        .param("month", month))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    private List<UUID> idsOf(JsonNode page) {
        List<UUID> ids = new ArrayList<>();
        for (JsonNode row : page.get("content")) ids.add(UUID.fromString(row.get("id").asText()));
        return ids;
    }
}
