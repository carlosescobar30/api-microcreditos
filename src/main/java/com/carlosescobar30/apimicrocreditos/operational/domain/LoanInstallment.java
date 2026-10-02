package com.carlosescobar30.apimicrocreditos.operational.domain;

import com.carlosescobar30.apimicrocreditos.common.domain.EntityBaseClass;
import com.carlosescobar30.apimicrocreditos.operational.attribute.RoundingAttributes;
import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.ObligationStatus;
import com.carlosescobar30.apimicrocreditos.operational.domain.rate.PenaltyRateSchedule;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "loan_installments", schema = "operational")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class LoanInstallment extends EntityBaseClass {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @Column(nullable = false)
    private Integer installmentNumber;

    @Column(precision = 19, scale = 4, nullable = false)
    private BigDecimal principalAmount;

    @Column(precision = 19, scale = 4, nullable = false)
    private BigDecimal interestAmount;

    @Column(precision = 19, scale = 4, nullable = false)
    private BigDecimal paidArrearsAmount;

    @Column(precision = 19, scale = 4, nullable = false)
    private BigDecimal accruedArrearsAmount;

    private LocalDate arrearsAccruedUntil;

    @Column(precision = 19, scale = 4, nullable = false)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private LocalDate paymentDate;

    @Column(nullable = false)
    @Enumerated(value = EnumType.STRING)
    private ObligationStatus status;

    public void accrueArrears(LocalDate today, PenaltyRateSchedule penaltyRates) {

        if (status != ObligationStatus.OVERDUE) {
            return;
        }

        LocalDate accruedFrom = arrearsAccruedUntil != null ? arrearsAccruedUntil : paymentDate;

        if (!today.isAfter(accruedFrom)) {
            return;
        }

        BigDecimal dailyRatesSum = BigDecimal.ZERO;

        for (LocalDate day = accruedFrom.plusDays(1); !day.isAfter(today); day = day.plusDays(1)) {
            dailyRatesSum = dailyRatesSum.add(penaltyRates.dailyRateOn(day));
        }

        BigDecimal increment = principalAmount
                .multiply(dailyRatesSum)
                .setScale(RoundingAttributes.SCALE_DEFAULT, RoundingAttributes.ROUNDING_DEFAULT);

        accruedArrearsAmount = accruedArrearsAmount.add(increment);
        arrearsAccruedUntil = today;
        recalculateTotal();

    }

    public BigDecimal outstandingArrears() {

        return accruedArrearsAmount.subtract(paidArrearsAmount);

    }

    public void recalculateTotal() {

        totalAmount = principalAmount
                .add(interestAmount)
                .add(outstandingArrears());

    }

}
