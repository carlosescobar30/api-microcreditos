package com.carlosescobar30.apimicrocreditos.operational.job;

import com.carlosescobar30.apimicrocreditos.TestcontainersConfiguration;
import com.carlosescobar30.apimicrocreditos.operational.domain.Loan;
import com.carlosescobar30.apimicrocreditos.operational.domain.LoanInstallment;
import com.carlosescobar30.apimicrocreditos.operational.domain.LoanProduct;
import com.carlosescobar30.apimicrocreditos.operational.domain.Payment;
import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.CreditModality;
import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.FinancialMethod;
import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.LoanStatus;
import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.ObligationStatus;
import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.TransactionStatus;
import com.carlosescobar30.apimicrocreditos.operational.domain.rate.PenaltyRateSchedule;
import com.carlosescobar30.apimicrocreditos.operational.dto.TransactionValidationDTO;
import com.carlosescobar30.apimicrocreditos.operational.repository.LoanInstallmentRepository;
import com.carlosescobar30.apimicrocreditos.operational.repository.LoanProductRepository;
import com.carlosescobar30.apimicrocreditos.operational.repository.LoanRepository;
import com.carlosescobar30.apimicrocreditos.operational.repository.PaymentAllocationRepository;
import com.carlosescobar30.apimicrocreditos.operational.repository.PaymentRepository;
import com.carlosescobar30.apimicrocreditos.operational.service.LoanInstallmentService;
import com.carlosescobar30.apimicrocreditos.operational.service.PaymentService;
import com.carlosescobar30.apimicrocreditos.operational.service.UsuryRateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import({TestcontainersConfiguration.class, ArrearsAccrualFlowIT.MutableClockConfig.class})
class ArrearsAccrualFlowIT {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final LocalDate FIRST_OF_OCTOBER = LocalDate.of(2026, 10, 1);
    private static final BigDecimal TOTAL_PRINCIPAL = new BigDecimal("1000000.0000");
    private static final BigDecimal SEPTEMBER_DAILY_RATE = PenaltyRateSchedule.toDailyRate(new BigDecimal("0.2924"));
    private static final BigDecimal OCTOBER_DAILY_RATE = PenaltyRateSchedule.toDailyRate(new BigDecimal("0.2859"));

    @Autowired
    private UpdateJob updateJob;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private LoanInstallmentService loanInstallmentService;
    @Autowired
    private UsuryRateService usuryRateService;
    @Autowired
    private LoanProductRepository loanProductRepository;
    @Autowired
    private LoanRepository loanRepository;
    @Autowired
    private LoanInstallmentRepository loanInstallmentRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private PaymentAllocationRepository paymentAllocationRepository;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private MutableClock clock;

    private Long loanId;

    @BeforeEach
    void setUp() {

        paymentAllocationRepository.deleteAllInBatch();
        paymentRepository.deleteAllInBatch();
        loanInstallmentRepository.deleteAllInBatch();
        loanRepository.deleteAllInBatch();
        loanProductRepository.deleteAllInBatch();

        LoanProduct product = loanProductRepository.save(LoanProduct.builder()
                .name("Arrears product")
                .totalPrincipal(TOTAL_PRINCIPAL)
                .interestRate(new BigDecimal("0.2000"))
                .penaltyRateEa(new BigDecimal("0.4000"))
                .creditModality(CreditModality.CONSUMER_AND_ORDINARY)
                .installments(3)
                .periodicity(12)
                .minimumUserScore(1)
                .build());

        LocalDate startDate = LocalDate.of(2026, 8, 20);

        loanId = loanRepository.save(Loan.builder()
                .userReference(UUID.randomUUID())
                .loanProduct(product)
                .principalReceivable(TOTAL_PRINCIPAL)
                .startDate(startDate)
                .endDate(startDate.plusMonths(3))
                .payday(startDate.getDayOfMonth())
                .status(LoanStatus.ACTIVE)
                .build()).getId();

        transactionTemplate.executeWithoutResult(status ->
                loanInstallmentService.create(loanRepository.findById(loanId).orElseThrow()));

        clock.setTo(FIRST_OF_OCTOBER);
    }

    @Nested
    @DisplayName("Daily job against a real database")
    class DailyJob {

        @Test
        void theMigrationShipsTheCertifiedUsuryRates() {

            assertThat(usuryRateService.rateInForce(CreditModality.CONSUMER_AND_ORDINARY, LocalDate.of(2026, 10, 15)))
                    .isEqualByComparingTo("0.2859");
            assertThat(usuryRateService.rateInForce(CreditModality.POPULAR_PRODUCTIVE_URBAN, LocalDate.of(2026, 9, 10)))
                    .isEqualByComparingTo("0.8813");
        }

        @Test
        void anOverdueInstallmentAccruesCappedAtTheUsuryRateOfEachMonth() {

            updateJob.run();

            LoanInstallment first = installment(1);
            BigDecimal expected = first.getPrincipalAmount()
                    .multiply(SEPTEMBER_DAILY_RATE.multiply(BigDecimal.TEN).add(OCTOBER_DAILY_RATE))
                    .setScale(4, RoundingMode.HALF_UP);

            assertThat(first.getStatus()).isEqualTo(ObligationStatus.OVERDUE);
            assertThat(first.getAccruedArrearsAmount()).isEqualByComparingTo(expected);
            assertThat(first.getArrearsAccruedUntil()).isEqualTo(FIRST_OF_OCTOBER);
            assertThat(installment(2).getStatus()).isEqualTo(ObligationStatus.CURRENT);
            assertThat(loanRepository.findById(loanId).orElseThrow().getStatus()).isEqualTo(LoanStatus.IN_ARREARS);
        }

        @Test
        void runningTheJobTwiceOnTheSameDayAccruesNothingMore() {

            updateJob.run();
            BigDecimal afterFirstRun = installment(1).getAccruedArrearsAmount();

            updateJob.run();

            assertThat(installment(1).getAccruedArrearsAmount()).isEqualByComparingTo(afterFirstRun);
        }

        @Test
        void afterAPartialPrincipalPaymentArrearsAreNeverRecalculatedBackwards() {

            updateJob.run();
            LoanInstallment beforePayment = installment(1);
            BigDecimal accruedBeforePayment = beforePayment.getAccruedArrearsAmount();
            BigDecimal principalPaid = new BigDecimal("100000.0000");

            approvePayment(beforePayment.outstandingArrears()
                    .add(beforePayment.getInterestAmount())
                    .add(principalPaid));

            clock.setTo(FIRST_OF_OCTOBER.plusDays(1));
            updateJob.run();

            LoanInstallment afterPayment = installment(1);
            BigDecimal remainingPrincipal = beforePayment.getPrincipalAmount().subtract(principalPaid);
            BigDecimal oneMoreDay = remainingPrincipal.multiply(OCTOBER_DAILY_RATE).setScale(4, RoundingMode.HALF_UP);

            assertThat(afterPayment.getPrincipalAmount()).isEqualByComparingTo(remainingPrincipal);
            assertThat(afterPayment.getPaidArrearsAmount()).isEqualByComparingTo(accruedBeforePayment);
            assertThat(afterPayment.getAccruedArrearsAmount()).isEqualByComparingTo(accruedBeforePayment.add(oneMoreDay));
            assertThat(afterPayment.getTotalAmount()).isEqualByComparingTo(remainingPrincipal.add(oneMoreDay));
        }
    }

    private LoanInstallment installment(int number) {

        return loanInstallmentRepository.findAll().stream()
                .filter(installment -> installment.getInstallmentNumber() == number)
                .min(Comparator.comparing(LoanInstallment::getId))
                .orElseThrow();
    }

    private void approvePayment(BigDecimal amount) {

        transactionTemplate.executeWithoutResult(status -> paymentRepository.save(Payment.builder()
                .loan(loanRepository.getReferenceById(loanId))
                .transactionCode("TX-ARREARS")
                .amount(amount)
                .financialMethod(FinancialMethod.NEQUI)
                .description("irrelevant")
                .status(TransactionStatus.PENDING)
                .applied(false)
                .build()));

        paymentService.validateTransaction(new TransactionValidationDTO("TX-ARREARS", TransactionStatus.APPROVED));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class MutableClockConfig {

        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock(FIRST_OF_OCTOBER);
        }
    }

    static class MutableClock extends Clock {

        private volatile Instant instant;

        MutableClock(LocalDate day) {
            setTo(day);
        }

        void setTo(LocalDate day) {
            this.instant = day.atTime(LocalTime.NOON).atZone(BOGOTA).toInstant();
        }

        @Override
        public ZoneId getZone() {
            return BOGOTA;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
