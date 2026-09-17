package ro.ecoregistru;

import org.junit.jupiter.api.Test;
import ro.ecoregistru.util.BillingAddress;

import static org.assertj.core.api.Assertions.assertThat;

/** Adresa cumpărătorului pe factura FGO: județul și localitatea o singură dată (WH 1, ONSIA S.R.L., 17.09.2026). */
class BillingAddressTest {

    @Test
    void theAnafAddressOfOnsiaKeepsOnlyTheStreet() {
        String city = "Sat Sântandrei Com. Sântandrei";
        assertThat(BillingAddress.city(city)).isEqualTo("Sântandrei");
        assertThat(BillingAddress.street("JUD. BIHOR, SAT SÂNTANDREI COM. SÂNTANDREI, STR. FĂCLIEI, NR.79", "Bihor", city))
                .isEqualTo("STR. FĂCLIEI, NR.79");
    }

    @Test
    void aVillageOfAnotherCommuneKeepsTheCommune() {
        assertThat(BillingAddress.city("Sat Palota Com. Sântandrei")).isEqualTo("Palota, com. Sântandrei");
        assertThat(BillingAddress.city("Mun. Oradea")).isEqualTo("Oradea");
        assertThat(BillingAddress.street("JUD. BIHOR, MUN. ORADEA, STR. REPUBLICII, NR.1, BL. A", "Bihor", "Mun. Oradea"))
                .isEqualTo("STR. REPUBLICII, NR.1, BL. A");
    }

    @Test
    void bucharestKeepsTheSectorAndATypedAddressStaysAsWritten() {
        assertThat(BillingAddress.street("MUNICIPIUL BUCUREŞTI, SECTOR 1, STR. VICTORIEI, NR.5", "Bucuresti", "Sector 1"))
                .isEqualTo("STR. VICTORIEI, NR.5");
        assertThat(BillingAddress.street("Str. Horea nr. 14, ap. 3", "Cluj", "Cluj-Napoca")).isEqualTo("Str. Horea nr. 14, ap. 3");
        assertThat(BillingAddress.street("Cluj-Napoca", "Cluj", "Cluj-Napoca")).isEqualTo("Cluj-Napoca");
    }
}
