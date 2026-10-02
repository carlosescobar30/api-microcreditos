package com.carlosescobar30.apimicrocreditos.operational.domain.rate;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class EffectiveRatesTest {

    @Test
    void convertsAnEffectiveAnnualRateIntoItsMonthlyEquivalentWithoutTruncatingIt() {

        assertThat(EffectiveRates.toPeriodicRate(new BigDecimal("0.2500"), 12))
                .isEqualByComparingTo("0.0187692651");
    }

    @Test
    void convertsAnEffectiveAnnualRateIntoItsDailyEquivalent() {

        assertThat(EffectiveRates.toPeriodicRate(new BigDecimal("0.2859"), 365))
                .isEqualByComparingTo("0.0006891658");
    }

    @Test
    void compoundingTheMonthlyRateForAYearGivesBackTheAnnualRate() {

        BigDecimal monthlyRate = EffectiveRates.toPeriodicRate(new BigDecimal("0.2500"), 12);

        BigDecimal annualRate = BigDecimal.ONE.add(monthlyRate).pow(12).subtract(BigDecimal.ONE);

        assertThat(annualRate).isCloseTo(new BigDecimal("0.2500"), within(new BigDecimal("0.000001")));
    }
}
