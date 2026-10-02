package com.carlosescobar30.apimicrocreditos.operational.service;

import com.carlosescobar30.apimicrocreditos.iam.adapter.UserAdapter;
import com.carlosescobar30.apimicrocreditos.operational.domain.Loan;
import com.carlosescobar30.apimicrocreditos.operational.domain.LoanInstallment;
import com.carlosescobar30.apimicrocreditos.operational.domain.LoanProduct;
import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.CreditModality;
import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.LoanStatus;
import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.ObligationStatus;
import com.carlosescobar30.apimicrocreditos.operational.domain.rate.PenaltyRateSchedule;
import com.carlosescobar30.apimicrocreditos.operational.repository.LoanInstallmentRepository;
import com.carlosescobar30.apimicrocreditos.operational.repository.LoanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanInstallmentServiceScheduleTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-10-16T03:00:00Z"), ZoneId.of("America/Bogota"));
    private static final LocalDate TODAY_IN_BOGOTA = LocalDate.of(2026, 10, 15);

    @Mock
    private LoanInstallmentRepository repository;
    @Mock
    private LoanRepository loanRepository;
    @Mock
    private UsuryRateService usuryRateService;
    @Mock
    private UserAdapter userAdapter;
    @Captor
    private ArgumentCaptor<List<LoanInstallment>> installmentsCaptor;

    private LoanInstallmentService loanInstallmentService;

    @BeforeEach
    void setUp() {

        this.loanInstallmentService = new LoanInstallmentService(
                repository,
                loanRepository,
                new LoanInstallmentEngineService(),
                usuryRateService,
                userAdapter,
                CLOCK);
    }

    @Nested
    @DisplayName("Tests for the create method")
    class CreateTests {

        @Test
        void buildsAGermanScheduleWhosePrincipalAddsUpExactlyToTheLoan() {

            Loan loan = loan(product(1L, "1000000.0000", 3), LocalDate.of(2026, 10, 15));

            loanInstallmentService.create(loan);

            verify(repository).saveAll(installmentsCaptor.capture());
            List<LoanInstallment> installments = installmentsCaptor.getValue();

            assertThat(installments).hasSize(3);
            assertThat(installments.get(0).getPrincipalAmount()).isEqualByComparingTo("333333.3333");
            assertThat(installments.get(1).getPrincipalAmount()).isEqualByComparingTo("333333.3333");
            assertThat(installments.get(2).getPrincipalAmount()).isEqualByComparingTo("333333.3334");
            assertThat(installments.stream()
                    .map(LoanInstallment::getPrincipalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add))
                    .isEqualByComparingTo("1000000.0000");
        }

        @Test
        void theInterestUsesTheExactMonthlyEquivalentOfTheAnnualRate() {

            Loan loan = loan(product(1L, "1000000.0000", 3), LocalDate.of(2026, 10, 15));

            loanInstallmentService.create(loan);

            verify(repository).saveAll(installmentsCaptor.capture());

            assertThat(installmentsCaptor.getValue())
                    .extracting(LoanInstallment::getInterestAmount)
                    .usingElementComparator(BigDecimal::compareTo)
                    .containsExactly(
                            new BigDecimal("18769.2651"),
                            new BigDecimal("12512.8434"),
                            new BigDecimal("6256.4217"));
        }

        @Test
        void theInterestDecreasesAsThePrincipalIsPaidDown() {

            Loan loan = loan(product(1L, "1000000.0000", 3), LocalDate.of(2026, 10, 15));

            loanInstallmentService.create(loan);

            verify(repository).saveAll(installmentsCaptor.capture());
            List<LoanInstallment> installments = installmentsCaptor.getValue();

            assertThat(installments.get(0).getInterestAmount())
                    .isGreaterThan(installments.get(1).getInterestAmount());
            assertThat(installments.get(1).getInterestAmount())
                    .isGreaterThan(installments.get(2).getInterestAmount());
        }

        @Test
        void everyInstallmentIsDueOneMonthAfterThePreviousOneAndStartsUnpaid() {

            Loan loan = loan(product(1L, "1000000.0000", 3), LocalDate.of(2026, 10, 15));

            loanInstallmentService.create(loan);

            verify(repository).saveAll(installmentsCaptor.capture());

            assertThat(installmentsCaptor.getValue())
                    .extracting(LoanInstallment::getInstallmentNumber, LoanInstallment::getPaymentDate)
                    .containsExactly(
                            tuple(1, LocalDate.of(2026, 11, 15)),
                            tuple(2, LocalDate.of(2026, 12, 15)),
                            tuple(3, LocalDate.of(2027, 1, 15)));
            assertThat(installmentsCaptor.getValue()).allSatisfy(installment -> {
                assertThat(installment.getStatus()).isEqualTo(ObligationStatus.UNPAID);
                assertThat(installment.getTotalAmount()).isEqualByComparingTo(
                        installment.getPrincipalAmount().add(installment.getInterestAmount()));
            });
        }
    }

    @Nested
    @DisplayName("Tests for the daily update")
    class DailyUpdateTests {

        @Test
        void statusesAreUpdatedWithTheDateInColombiaNotInUtc() {

            loanInstallmentService.updateStatus();

            verify(repository).changeStatusToOverdue(CLOCK.instant(), TODAY_IN_BOGOTA);
            verify(repository).changeStatusToCurrent(CLOCK.instant(), TODAY_IN_BOGOTA.plusMonths(1));
        }

        @Test
        void aLoanIsLockedBeforeItsOverdueInstallmentsAreReadAndAccrued() {

            Loan loan = loan(product(1L, "1000000.0000", 3), LocalDate.of(2026, 8, 5));
            LoanInstallment first = overdue(loan, 1);
            LoanInstallment second = overdue(loan, 2);
            when(loanRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(loan));
            when(repository.findAllByLoan_IdAndStatus(10L, ObligationStatus.OVERDUE)).thenReturn(List.of(first, second));

            loanInstallmentService.accrueArrearsOf(10L, Map.of(1L, schedule()));

            InOrder order = inOrder(loanRepository, repository);
            order.verify(loanRepository).findByIdForUpdate(10L);
            order.verify(repository).findAllByLoan_IdAndStatus(10L, ObligationStatus.OVERDUE);
            assertThat(List.of(first, second)).allSatisfy(installment -> {
                assertThat(installment.getArrearsAccruedUntil()).isEqualTo(TODAY_IN_BOGOTA);
                assertThat(installment.getAccruedArrearsAmount()).isPositive();
            });
            verifyNoInteractions(usuryRateService);
        }

        @Test
        void aProductMissingFromThePreloadedSchedulesIsLoadedOnTheSpot() {

            LoanProduct product = product(1L, "1000000.0000", 3);
            Loan loan = loan(product, LocalDate.of(2026, 8, 5));
            LoanInstallment installment = overdue(loan, 1);
            when(loanRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(loan));
            when(repository.findAllByLoan_IdAndStatus(10L, ObligationStatus.OVERDUE)).thenReturn(List.of(installment));
            when(usuryRateService.penaltyScheduleFor(product)).thenReturn(schedule());

            loanInstallmentService.accrueArrearsOf(10L, Map.of());

            assertThat(installment.getArrearsAccruedUntil()).isEqualTo(TODAY_IN_BOGOTA);
        }

        @Test
        void theSchedulesOfTheNightAreLoadedForEveryProductWithOverdueInstallmentsAtOnce() {

            List<LoanProduct> products = List.of(product(1L, "1000000.0000", 3), product(2L, "1000000.0000", 3));
            Map<Long, PenaltyRateSchedule> schedules = Map.of(1L, schedule(), 2L, schedule());
            when(repository.findProductsWithOverdueInstallments()).thenReturn(products);
            when(usuryRateService.penaltySchedulesFor(products)).thenReturn(schedules);

            assertThat(loanInstallmentService.penaltySchedulesForOverdueLoans()).isEqualTo(schedules);
        }
    }

    private LoanProduct product(Long id, String totalPrincipal, int installments) {

        LoanProduct product = LoanProduct.builder()
                .name("Product " + id)
                .totalPrincipal(new BigDecimal(totalPrincipal))
                .interestRate(new BigDecimal("0.2500"))
                .penaltyRateEa(new BigDecimal("0.2859"))
                .creditModality(CreditModality.CONSUMER_AND_ORDINARY)
                .installments(installments)
                .periodicity(12)
                .minimumUserScore(1)
                .build();
        product.setId(id);
        return product;
    }

    private Loan loan(LoanProduct product, LocalDate startDate) {

        return Loan.builder()
                .loanProduct(product)
                .principalReceivable(product.getTotalPrincipal())
                .startDate(startDate)
                .status(LoanStatus.ACTIVE)
                .build();
    }

    private LoanInstallment overdue(Loan loan, int number) {

        return LoanInstallment.builder()
                .loan(loan)
                .installmentNumber(number)
                .principalAmount(new BigDecimal("100000.0000"))
                .interestAmount(BigDecimal.ZERO)
                .accruedArrearsAmount(BigDecimal.ZERO)
                .paidArrearsAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("100000.0000"))
                .paymentDate(LocalDate.of(2026, 10, 5))
                .status(ObligationStatus.OVERDUE)
                .build();
    }

    private PenaltyRateSchedule schedule() {

        return new PenaltyRateSchedule(
                CreditModality.CONSUMER_AND_ORDINARY,
                new BigDecimal("0.2859"),
                Map.of(LocalDate.of(2026, 10, 1), new BigDecimal("0.2859")));
    }
}
