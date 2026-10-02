package com.carlosescobar30.apimicrocreditos.operational.domain.rate;

import com.carlosescobar30.apimicrocreditos.operational.attribute.RoundingAttributes;

import java.math.BigDecimal;

public final class EffectiveRates {

    private EffectiveRates() {
    }

    public static BigDecimal toPeriodicRate(BigDecimal effectiveAnnualRate, int periodsPerYear) {

        double periodicRate = Math.pow(1 + effectiveAnnualRate.doubleValue(), 1.0 / periodsPerYear) - 1;

        return BigDecimal.valueOf(periodicRate)
                .setScale(RoundingAttributes.RATE_SCALE, RoundingAttributes.ROUNDING_DEFAULT);

    }

}
