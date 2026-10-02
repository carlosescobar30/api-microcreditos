package com.carlosescobar30.apimicrocreditos.operational.domain;

import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.CreditModality;
import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.ObligationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LoanInstallmentTest {

    private static final LocalDate DUE_DATE = LocalDate.of(2026, 10, 5);
    private static final PenaltyRateSchedule OCTOBER_RATES = new PenaltyRateSchedule(
            CreditModality.CONSUMER_AND_ORDINARY,
            new BigDecimal("0.2859"),
            Map.of(LocalDate.of(2026, 10, 1), new BigDecimal("0.2859")));

    @Nested
    @DisplayName("Tests for the accrueArrears method")
    class AccrueArrearsTests {

        @Test
        void accruesTheOverduePrincipalTimesTheDailyRateForEveryDayPastTheDueDate() {

            LoanInstallment installment = overdueInstallment("500000.0000", "10000.0000");

            installment.accrueArrears(LocalDate.of(2026, 10, 15), OCTOBER_RATES);

            assertThat(installment.getAccruedArrearsAmount()).isEqualByComparingTo("3445.8290");
            assertThat(installment.getArrearsAccruedUntil()).isEqualTo(LocalDate.of(2026, 10, 15));
            assertThat(installment.getTotalAmount()).isEqualByComparingTo("513445.8290");
        }

        @Test
        void aPrincipalPaymentLowersTheBaseOnlyFromThatDayOn() {

            LoanInstallment installment = overdueInstallment("500000.0000", "10000.0000");
            installment.accrueArrears(LocalDate.of(2026, 10, 15), OCTOBER_RATES);

            installment.setPaidArrearsAmount(installment.getAccruedArrearsAmount());
            installment.setInterestAmount(BigDecimal.ZERO);
            installment.setPrincipalAmount(new BigDecimal("250000.0000"));
            installment.recalculateTotal();

            installment.accrueArrears(LocalDate.of(2026, 10, 16), OCTOBER_RATES);

            assertThat(installment.getAccruedArrearsAmount()).isEqualByComparingTo("3618.1205");
            assertThat(installment.outstandingArrears()).isEqualByComparingTo("172.2915");
            assertThat(installment.getTotalAmount()).isEqualByComparingTo("250172.2915");
        }

        @Test
        void accruingTwiceOnTheSameDayAddsNothing() {

            LoanInstallment installment = overdueInstallment("500000.0000", "10000.0000");

            installment.accrueArrears(LocalDate.of(2026, 10, 15), OCTOBER_RATES);
            installment.accrueArrears(LocalDate.of(2026, 10, 15), OCTOBER_RATES);

            assertThat(installment.getAccruedArrearsAmount()).isEqualByComparingTo("3445.8290");
        }

        @Test
        void missedDaysAreCaughtUpOnTheNextAccrual() {

            LoanInstallment installment = overdueInstallment("500000.0000", "10000.0000");

            installment.accrueArrears(LocalDate.of(2026, 10, 12), OCTOBER_RATES);
            installment.accrueArrears(LocalDate.of(2026, 10, 15), OCTOBER_RATES);

            assertThat(installment.getAccruedArrearsAmount()).isEqualByComparingTo("3445.8290");
        }

        @Test
        void anInstallmentThatIsNotOverdueDoesNotAccrue() {

            LoanInstallment installment = overdueInstallment("500000.0000", "10000.0000");
            installment.setStatus(ObligationStatus.CURRENT);

            installment.accrueArrears(LocalDate.of(2026, 10, 15), OCTOBER_RATES);

            assertThat(installment.getAccruedArrearsAmount()).isEqualByComparingTo("0");
            assertThat(installment.getArrearsAccruedUntil()).isNull();
        }

        @Test
        void nothingAccruesOnTheDueDateItself() {

            LoanInstallment installment = overdueInstallment("500000.0000", "10000.0000");

            installment.accrueArrears(DUE_DATE, OCTOBER_RATES);

            assertThat(installment.getAccruedArrearsAmount()).isEqualByComparingTo("0");
        }

        @Test
        void eachDayUsesTheRateOfItsOwnMonthWhenArrearsCrossAMonthChange() {

            PenaltyRateSchedule rates = new PenaltyRateSchedule(
                    CreditModality.CONSUMER_AND_ORDINARY,
                    new BigDecimal("0.3500"),
                    Map.of(LocalDate.of(2026, 9, 1), new BigDecimal("0.2924"),
                            LocalDate.of(2026, 10, 1), new BigDecimal("0.2859")));
            LoanInstallment installment = overdueInstallment("300000.0000", "0.0000");
            installment.setPaymentDate(LocalDate.of(2026, 9, 28));

            installment.accrueArrears(LocalDate.of(2026, 10, 2), rates);

            BigDecimal twoSeptemberDays = new BigDecimal("0.0007029893").multiply(BigDecimal.TWO);
            BigDecimal twoOctoberDays = new BigDecimal("0.0006891658").multiply(BigDecimal.TWO);
            BigDecimal expected = new BigDecimal("300000.0000")
                    .multiply(twoSeptemberDays.add(twoOctoberDays))
                    .setScale(4, RoundingMode.HALF_UP);
            assertThat(installment.getAccruedArrearsAmount()).isEqualByComparingTo(expected);
        }
    }

    @Nested
    @DisplayName("Tests for the recalculateTotal method")
    class RecalculateTotalTests {

        @Test
        void theTotalIsPrincipalPlusInterestPlusTheArrearsStillUnpaid() {

            LoanInstallment installment = overdueInstallment("100.0000", "10.0000");
            installment.setAccruedArrearsAmount(new BigDecimal("5.0000"));
            installment.setPaidArrearsAmount(new BigDecimal("2.0000"));

            installment.recalculateTotal();

            assertThat(installment.getTotalAmount()).isEqualByComparingTo("113.0000");
        }
    }

    private LoanInstallment overdueInstallment(String principal, String interest) {

        return LoanInstallment.builder()
                .installmentNumber(1)
                .principalAmount(new BigDecimal(principal))
                .interestAmount(new BigDecimal(interest))
                .accruedArrearsAmount(BigDecimal.ZERO)
                .paidArrearsAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal(principal).add(new BigDecimal(interest)))
                .paymentDate(DUE_DATE)
                .status(ObligationStatus.OVERDUE)
                .build();
    }
}
