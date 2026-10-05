package ro.ecoregistru.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SizeTierTest {

    private static final SizeTier[] TIERS = SizeTier.values();

    @Test
    void ofReturnsEachTier() {
        assertThat(SizeTier.of(1)).isEqualTo(SizeTier.TIER_1);
        assertThat(SizeTier.of(2)).isEqualTo(SizeTier.TIER_2);
        assertThat(SizeTier.of(3)).isEqualTo(SizeTier.TIER_3);
        assertThat(SizeTier.of(4)).isEqualTo(SizeTier.TIER_4);
        assertThat(SizeTier.of(5)).isEqualTo(SizeTier.TIER_5);
        for (SizeTier tier : TIERS) {
            assertThat(SizeTier.of(tier.number())).isEqualTo(tier);
        }
    }

    @Test
    void ofRefusesZeroAndSix() {
        assertThatThrownBy(() -> SizeTier.of(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SizeTier.of(6)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void generatorPricesFollowTheGrid() {
        int[] expected = {35, 50, 75, 100, 149};
        for (int i = 0; i < 5; i++) {
            assertThat(TIERS[i].monthlyPrice(SubscriptionPlan.GENERATOR)).isEqualByComparingTo(String.valueOf(expected[i]));
        }
    }

    @Test
    void packagingAddsTheTierSupplement() {
        int[] expected = {50, 70, 100, 130, 199};
        for (int i = 0; i < 5; i++) {
            assertThat(TIERS[i].monthlyPrice(SubscriptionPlan.GENERATOR_PACKAGING)).isEqualByComparingTo(String.valueOf(expected[i]));
        }
    }

    @Test
    void otherPlansHaveNoTierPrice() {
        assertThatThrownBy(() -> SizeTier.TIER_1.monthlyPrice(SubscriptionPlan.FULL_SERVICE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SizeTier.TIER_1.monthlyPrice(SubscriptionPlan.CONSULTANCY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rangesAreTheSpecText() {
        assertThat(TIERS).extracting(SizeTier::range)
                .containsExactly("0–2", "3–9", "10–19", "20–39", "40+");
    }
}
