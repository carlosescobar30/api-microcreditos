package com.carlosescobar30.apimicrocreditos.operational.service;

import com.carlosescobar30.apimicrocreditos.common.exception.not_found.ResourceNotFoundException;
import com.carlosescobar30.apimicrocreditos.operational.domain.Loan;
import com.carlosescobar30.apimicrocreditos.operational.domain.LoanInstallment;
import com.carlosescobar30.apimicrocreditos.operational.domain.LoanProduct;
import com.carlosescobar30.apimicrocreditos.operational.domain.Payment;
import com.carlosescobar30.apimicrocreditos.operational.domain.PaymentAllocation;
import com.carlosescobar30.apimicrocreditos.operational.domain.PenaltyRateSchedule;
import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.CreditModality;
import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.FinancialMethod;
import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.LoanStatus;
import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.ObligationStatus;
import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.PaymentApplication;
import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.TransactionStatus;
import com.carlosescobar30.apimicrocreditos.operational.dto.AllocationDetailsDTO;
import com.carlosescobar30.apimicrocreditos.operational.factory.PaymentAllocationDTOFactory;
import com.carlosescobar30.apimicrocreditos.operational.repository.PaymentAllocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentAllocationServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 15);
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-10-15T17:00:00Z"), ZoneId.of("America/Bogota"));

    @Mock
    private PaymentAllocationRepository repository;
    @Mock
    private LoanInstallmentService loanInstallmentService;
    @Mock
    private LoanService loanService;
    @Mock
    private UsuryRateService usuryRateService;

    private PaymentAllocationService paymentAllocationService;
    private LoanProduct product;
    private Loan loan;
    private PenaltyRateSchedule penaltyRates;

    @BeforeEach
    void setUp() {

        this.paymentAllocationService = new PaymentAllocationService(
                repository,
                loanInstallmentService,
                loanService,
                new PaymentAllocationDTOFactory(),
                usuryRateService,
                CLOCK);

        this.product = LoanProduct.builder()
                .name("Product")
                .penaltyRateEa(new BigDecimal("0.2859"))
                .creditModality(CreditModality.CONSUMER_AND_ORDINARY)
                .build();

        this.loan = Loan.builder()
                .loanProduct(product)
                .status(LoanStatus.IN_ARREARS)
                .build();

        this.penaltyRates = new PenaltyRateSchedule(
                CreditModality.CONSUMER_AND_ORDINARY,
                new BigDecimal("0.2859"),
                Map.of(LocalDate.of(2026, 10, 1), new BigDecimal("0.2859")));
    }

    @Nested
    @DisplayName("Cascade on an overdue installment")
    class OverdueInstallmentTests {

        @Test
        void theArrearsAreBroughtUpToDateAndPaidBeforeInterestAndThenPrincipal() {

            LoanInstallment installment = installment(1, "500000.0000", "10000.0000", ObligationStatus.OVERDUE);
            givenDueInstallments(installment);

            List<AllocationDetailsDTO> details = paymentAllocationService.allocatePayment(payment("263445.8290"));

            assertThat(details)
                    .extracting(AllocationDetailsDTO::application, AllocationDetailsDTO::amount)
                    .containsExactly(
                            tuple(PaymentApplication.ARREAR, new BigDecimal("3445.8290")),
                            tuple(PaymentApplication.INTEREST, new BigDecimal("10000.0000")),
                            tuple(PaymentApplication.PRINCIPAL, new BigDecimal("250000.0000")));
            assertThat(installment.getArrearsAccruedUntil()).isEqualTo(TODAY);
            assertThat(installment.getPrincipalAmount()).isEqualByComparingTo("250000.0000");
            assertThat(installment.getStatus()).isEqualTo(ObligationStatus.OVERDUE);
            assertThat(installment.getTotalAmount()).isEqualByComparingTo("250000.0000");
            verify(loanService).deductSettledAmount(loan, new BigDecimal("250000.0000"));
        }

        @Test
        void theNextDayArrearsKeepAccruingOnlyOnThePrincipalStillOwed() {

            LoanInstallment installment = installment(1, "500000.0000", "10000.0000", ObligationStatus.OVERDUE);
            givenDueInstallments(installment);
            paymentAllocationService.allocatePayment(payment("263445.8290"));

            installment.accrueArrears(TODAY.plusDays(1), penaltyRates);

            assertThat(installment.getAccruedArrearsAmount()).isEqualByComparingTo("3618.1205");
            assertThat(installment.getPaidArrearsAmount()).isEqualByComparingTo("3445.8290");
            assertThat(installment.getTotalAmount()).isEqualByComparingTo("250172.2915");
        }

        @Test
        void aPaymentThatOnlyCoversArrearsAndPartOfTheInterestLeavesThePrincipalUntouched() {

            LoanInstallment installment = installment(1, "500000.0000", "10000.0000", ObligationStatus.OVERDUE);
            givenDueInstallments(installment);

            List<AllocationDetailsDTO> details = paymentAllocationService.allocatePayment(payment("5000.0000"));

            assertThat(details)
                    .extracting(AllocationDetailsDTO::application)
                    .containsExactly(PaymentApplication.ARREAR, PaymentApplication.INTEREST);
            assertThat(details.get(1).amount()).isEqualByComparingTo("1554.1710");
            assertThat(installment.getInterestAmount()).isEqualByComparingTo("8445.8290");
            assertThat(installment.getPrincipalAmount()).isEqualByComparingTo("500000.0000");
            assertThat(installment.getTotalAmount()).isEqualByComparingTo("508445.8290");
            verify(loanService, never()).deductSettledAmount(any(), any());
        }
    }

    @Nested
    @DisplayName("Cascade across installments")
    class AcrossInstallmentsTests {

        @Test
        void aPaymentThatCoversAWholeInstallmentMarksItPaidAndMovesOnToTheNextOne() {

            LoanInstallment first = installment(1, "100000.0000", "2000.0000", ObligationStatus.CURRENT);
            LoanInstallment second = installment(2, "100000.0000", "1500.0000", ObligationStatus.UNPAID);
            givenDueInstallments(first, second);

            List<AllocationDetailsDTO> details = paymentAllocationService.allocatePayment(payment("150000.0000"));

            assertThat(details)
                    .extracting(AllocationDetailsDTO::loanInstallmentReference, AllocationDetailsDTO::application)
                    .containsExactly(
                            tuple(first.getPublicId(), PaymentApplication.INTEREST),
                            tuple(first.getPublicId(), PaymentApplication.PRINCIPAL),
                            tuple(second.getPublicId(), PaymentApplication.INTEREST),
                            tuple(second.getPublicId(), PaymentApplication.PRINCIPAL));
            assertThat(first.getStatus()).isEqualTo(ObligationStatus.PAID);
            assertThat(first.getTotalAmount()).isEqualByComparingTo("0");
            assertThat(second.getPrincipalAmount()).isEqualByComparingTo("53500.0000");
            assertThat(second.getStatus()).isEqualTo(ObligationStatus.UNPAID);
            verify(loanService).deductSettledAmount(loan, new BigDecimal("100000.0000"));
            verify(loanService).deductSettledAmount(loan, new BigDecimal("46500.0000"));
        }

        @Test
        void whatIsLeftAfterTheLastInstallmentIsRecordedAsSurplus() {

            LoanInstallment last = installment(12, "100000.0000", "2000.0000", ObligationStatus.CURRENT);
            givenDueInstallments(last);

            List<AllocationDetailsDTO> details = paymentAllocationService.allocatePayment(payment("110000.0000"));

            assertThat(details)
                    .extracting(AllocationDetailsDTO::application)
                    .containsExactly(PaymentApplication.INTEREST, PaymentApplication.PRINCIPAL, PaymentApplication.SURPLUS);
            assertThat(details.get(2).amount()).isEqualByComparingTo("8000.0000");
            assertThat(last.getStatus()).isEqualTo(ObligationStatus.PAID);
        }

        @Test
        void aLoanWithNothingDueCannotTakeAPayment() {

            when(loanInstallmentService.getAllDue(loan)).thenReturn(List.of());

            assertThatThrownBy(() -> paymentAllocationService.allocatePayment(payment("1000.0000")))
                    .isInstanceOf(ResourceNotFoundException.class);

            verifyNoInteractions(repository, usuryRateService);
        }
    }

    private void givenDueInstallments(LoanInstallment... installments) {

        when(loanInstallmentService.getAllDue(loan)).thenReturn(List.of(installments));
        when(usuryRateService.penaltyScheduleFor(product)).thenReturn(penaltyRates);
        when(repository.save(any(PaymentAllocation.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Payment payment(String amount) {

        return Payment.builder()
                .loan(loan)
                .transactionCode("TX-1")
                .amount(new BigDecimal(amount))
                .financialMethod(FinancialMethod.NEQUI)
                .description("irrelevant")
                .status(TransactionStatus.APPROVED)
                .applied(true)
                .build();
    }

    private LoanInstallment installment(int number, String principal, String interest, ObligationStatus status) {

        return LoanInstallment.builder()
                .loan(loan)
                .installmentNumber(number)
                .principalAmount(new BigDecimal(principal))
                .interestAmount(new BigDecimal(interest))
                .accruedArrearsAmount(BigDecimal.ZERO)
                .paidArrearsAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal(principal).add(new BigDecimal(interest)))
                .paymentDate(LocalDate.of(2026, 10, 5).plusMonths(number - 1))
                .status(status)
                .build();
    }
}
