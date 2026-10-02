package com.carlosescobar30.apimicrocreditos.operational.domain;

import com.carlosescobar30.apimicrocreditos.operational.attribute.RoundingAttributes;
import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.CreditModality;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;


public final class PenaltyRateSchedule {

    private static final double DAYS_PER_YEAR = 365;

    private final CreditModality creditModality;
    private final BigDecimal contractualRateEa;
    private final NavigableMap<LocalDate, BigDecimal> usuryRatesEa;
    private final Map<BigDecimal, BigDecimal> dailyRates = new HashMap<>();

    public PenaltyRateSchedule(CreditModality creditModality,
                               BigDecimal contractualRateEa,
                               Map<LocalDate, BigDecimal> usuryRatesEa) {

        this.creditModality = creditModality;
        this.contractualRateEa = contractualRateEa;
        this.usuryRatesEa = new TreeMap<>(usuryRatesEa);

    }

    public BigDecimal effectiveRateEaOn(LocalDate day) {

        Map.Entry<LocalDate, BigDecimal> usuryRate = usuryRatesEa.floorEntry(day);

        if (usuryRate == null) {

            throw new IllegalStateException(
                    "There is no usury rate registered for " + creditModality + " on " + day);

        }

        return contractualRateEa.min(usuryRate.getValue());

    }

    public BigDecimal dailyRateOn(LocalDate day) {

        return dailyRates.computeIfAbsent(effectiveRateEaOn(day), PenaltyRateSchedule::toDailyRate);

    }

    public static BigDecimal toDailyRate(BigDecimal effectiveAnnualRate) {

        double dailyRate = Math.pow(1 + effectiveAnnualRate.doubleValue(), 1 / DAYS_PER_YEAR) - 1;

        return BigDecimal.valueOf(dailyRate)
                .setScale(RoundingAttributes.RATE_SCALE, RoundingAttributes.ROUNDING_DEFAULT);

    }

}
