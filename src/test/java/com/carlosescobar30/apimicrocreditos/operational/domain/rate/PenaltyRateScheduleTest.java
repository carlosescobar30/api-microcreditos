package com.carlosescobar30.apimicrocreditos.operational.domain.rate;

import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.CreditModality;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class PenaltyRateScheduleTest {

    private static final Map<LocalDate, BigDecimal> USURY_RATES = Map.of(
            LocalDate.of(2026, 9, 1), new BigDecimal("0.2924"),
            LocalDate.of(2026, 10, 1), new BigDecimal("0.2859"));

    @Nested
    @DisplayName("Tests for the toDailyRate method")
    class ToDailyRateTests {

        @Test
        void convertsAnEffectiveAnnualRateIntoItsEquivalentDailyRate() {

            assertThat(PenaltyRateSchedule.toDailyRate(new BigDecimal("0.2859")))
                    .isEqualByComparingTo("0.0006891658");
        }

        @Test
        void compoundingTheDailyRateForAYearGivesBackTheAnnualRate() {

            BigDecimal dailyRate = PenaltyRateSchedule.toDailyRate(new BigDecimal("0.2859"));

            BigDecimal annualRate = BigDecimal.ONE.add(dailyRate).pow(365).subtract(BigDecimal.ONE);

            assertThat(annualRate).isCloseTo(new BigDecimal("0.2859"), within(new BigDecimal("0.000001")));
        }
    }

    @Nested
    @DisplayName("Tests for the effectiveRateEaOn method")
    class EffectiveRateTests {

        @Test
        void aContractualRateBelowTheUsuryRateIsKept() {

            PenaltyRateSchedule schedule = scheduleWithContractualRate("0.2000");

            assertThat(schedule.effectiveRateEaOn(LocalDate.of(2026, 10, 15))).isEqualByComparingTo("0.2000");
        }

        @Test
        void aContractualRateAboveTheUsuryRateIsCappedAtIt() {

            PenaltyRateSchedule schedule = scheduleWithContractualRate("0.4000");

            assertThat(schedule.effectiveRateEaOn(LocalDate.of(2026, 9, 15))).isEqualByComparingTo("0.2924");
            assertThat(schedule.effectiveRateEaOn(LocalDate.of(2026, 10, 15))).isEqualByComparingTo("0.2859");
        }

        @Test
        void aUsuryRateStaysInForceUntilTheNextOneIsRegistered() {

            PenaltyRateSchedule schedule = scheduleWithContractualRate("0.4000");

            assertThat(schedule.effectiveRateEaOn(LocalDate.of(2026, 12, 20))).isEqualByComparingTo("0.2859");
        }

        @Test
        void aDayBeforeTheFirstRegisteredUsuryRateIsRejected() {

            PenaltyRateSchedule schedule = scheduleWithContractualRate("0.4000");

            assertThatThrownBy(() -> schedule.effectiveRateEaOn(LocalDate.of(2026, 8, 31)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("CONSUMER_AND_ORDINARY");
        }

        @Test
        void theDailyRateIsTheEquivalentOfTheCappedRate() {

            PenaltyRateSchedule schedule = scheduleWithContractualRate("0.4000");

            assertThat(schedule.dailyRateOn(LocalDate.of(2026, 10, 15)))
                    .isEqualByComparingTo(PenaltyRateSchedule.toDailyRate(new BigDecimal("0.2859")));
        }
    }

    private PenaltyRateSchedule scheduleWithContractualRate(String contractualRateEa) {

        return new PenaltyRateSchedule(
                CreditModality.CONSUMER_AND_ORDINARY,
                new BigDecimal(contractualRateEa),
                USURY_RATES);
    }
}
