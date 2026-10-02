package com.carlosescobar30.apimicrocreditos.operational.service;

import com.carlosescobar30.apimicrocreditos.TestcontainersConfiguration;
import com.carlosescobar30.apimicrocreditos.common.exception.conflict.PaymentAlreadyProcessedException;
import com.carlosescobar30.apimicrocreditos.operational.domain.Loan;
import com.carlosescobar30.apimicrocreditos.operational.domain.LoanInstallment;
import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.CreditModality;
import com.carlosescobar30.apimicrocreditos.operational.domain.LoanProduct;
import com.carlosescobar30.apimicrocreditos.operational.domain.Payment;
import com.carlosescobar30.apimicrocreditos.operational.domain.PaymentAllocation;
import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.FinancialMethod;
import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.LoanStatus;
import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.PaymentApplication;
import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.TransactionStatus;
import com.carlosescobar30.apimicrocreditos.operational.dto.PaymentInfoDetailsDTO;
import com.carlosescobar30.apimicrocreditos.operational.dto.TransactionValidationDTO;
import com.carlosescobar30.apimicrocreditos.operational.repository.LoanInstallmentRepository;
import com.carlosescobar30.apimicrocreditos.operational.repository.LoanProductRepository;
import com.carlosescobar30.apimicrocreditos.operational.repository.LoanRepository;
import com.carlosescobar30.apimicrocreditos.operational.repository.PaymentAllocationRepository;
import com.carlosescobar30.apimicrocreditos.operational.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class PaymentValidationFlowIT {

    private static final UUID USER_REFERENCE = UUID.randomUUID();
    private static final BigDecimal TOTAL_PRINCIPAL = new BigDecimal("1200000.0000");
    private static final BigDecimal PAYMENT_AMOUNT = new BigDecimal("150000.0000");

    @Autowired
    private PaymentService paymentService;
    @Autowired
    private LoanInstallmentService loanInstallmentService;
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

    private Long loanId;

    @BeforeEach
    void setUp() {

        paymentAllocationRepository.deleteAllInBatch();
        paymentRepository.deleteAllInBatch();
        loanInstallmentRepository.deleteAllInBatch();
        loanRepository.deleteAllInBatch();
        loanProductRepository.deleteAllInBatch();

        LoanProduct product = loanProductRepository.save(LoanProduct.builder()
                .name("Test product")
                .totalPrincipal(TOTAL_PRINCIPAL)
                .interestRate(new BigDecimal("0.2500"))
                .penaltyRateEa(new BigDecimal("0.2500"))
                .creditModality(CreditModality.CONSUMER_AND_ORDINARY)
                .installments(12)
                .periodicity(12)
                .minimumUserScore(1)
                .build());

        LocalDate startDate = LocalDate.of(2099, 1, 10);

        loanId = loanRepository.save(Loan.builder()
                .userReference(USER_REFERENCE)
                .loanProduct(product)
                .principalReceivable(TOTAL_PRINCIPAL)
                .startDate(startDate)
                .endDate(startDate.plusMonths(12))
                .payday(startDate.getDayOfMonth())
                .status(LoanStatus.ACTIVE)
                .build()).getId();

        transactionTemplate.executeWithoutResult(status ->
                loanInstallmentService.create(loanRepository.findById(loanId).orElseThrow()));
    }

    @Nested
    @DisplayName("Concurrent validations against a real database")
    class Concurrency {

        @Test
        void twoSimultaneousApprovalsOfTheSamePaymentAllocateItOnlyOnce() throws Exception {

            createPayment("TX-1");

            List<PaymentInfoDetailsDTO> results = runConcurrently(
                    () -> approve("TX-1"),
                    () -> approve("TX-1"));

            assertThat(results).allMatch(result -> result.status() == TransactionStatus.APPROVED);
            assertThat(results.get(0).details()).hasSameSizeAs(results.get(1).details());

            assertThat(allocatedTotal()).isEqualByComparingTo(PAYMENT_AMOUNT);
            assertThatLoanBalanceMatchesItsInstallments();
        }

        @Test
        void twoDifferentPaymentsOfTheSameLoanAreAppliedOneAfterTheOther() throws Exception {

            createPayment("TX-1");
            createPayment("TX-2");

            runConcurrently(
                    () -> approve("TX-1"),
                    () -> approve("TX-2"));

            assertThat(allocatedTotal()).isEqualByComparingTo(PAYMENT_AMOUNT.multiply(BigDecimal.TWO));
            assertThatLoanBalanceMatchesItsInstallments();
        }
    }

    @Nested
    @DisplayName("Final states persist")
    class FinalStates {

        @Test
        void anApprovedPaymentCannotBeDeclinedAndItsAllocationsStay() {

            createPayment("TX-1");
            approve("TX-1");
            long allocationsAfterApproval = paymentAllocationRepository.count();

            assertThatThrownBy(() -> decline("TX-1"))
                    .isInstanceOf(PaymentAlreadyProcessedException.class);

            assertThat(statusOf("TX-1")).isEqualTo(TransactionStatus.APPROVED);
            assertThat(paymentAllocationRepository.count()).isEqualTo(allocationsAfterApproval);
            assertThat(allocatedTotal()).isEqualByComparingTo(PAYMENT_AMOUNT);
        }

        @Test
        void aDeclinedPaymentCannotBeApprovedLater() {

            createPayment("TX-1");
            decline("TX-1");

            assertThatThrownBy(() -> approve("TX-1"))
                    .isInstanceOf(PaymentAlreadyProcessedException.class);

            assertThat(statusOf("TX-1")).isEqualTo(TransactionStatus.DECLINED);
            assertThat(paymentAllocationRepository.count()).isZero();
            assertThat(loanRepository.findById(loanId).orElseThrow().getPrincipalReceivable())
                    .isEqualByComparingTo(TOTAL_PRINCIPAL);
        }
    }

    private void assertThatLoanBalanceMatchesItsInstallments() {

        BigDecimal principalPaid = paymentAllocationRepository.findAll().stream()
                .filter(allocation -> allocation.getAppliedTo() == PaymentApplication.PRINCIPAL)
                .map(PaymentAllocation::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal principalLeftInInstallments = loanInstallmentRepository.findAll().stream()
                .map(LoanInstallment::getPrincipalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal loanBalance = loanRepository.findById(loanId).orElseThrow().getPrincipalReceivable();

        assertThat(loanBalance).isEqualByComparingTo(TOTAL_PRINCIPAL.subtract(principalPaid));
        assertThat(principalLeftInInstallments).isEqualByComparingTo(loanBalance);
    }

    private BigDecimal allocatedTotal() {

        return paymentAllocationRepository.findAll().stream()
                .map(PaymentAllocation::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private TransactionStatus statusOf(String transactionCode) {

        return paymentRepository.findAll().stream()
                .filter(payment -> payment.getTransactionCode().equals(transactionCode))
                .findFirst()
                .map(Payment::getStatus)
                .orElseThrow();
    }

    private void createPayment(String transactionCode) {

        transactionTemplate.executeWithoutResult(status -> paymentRepository.save(Payment.builder()
                .loan(loanRepository.getReferenceById(loanId))
                .transactionCode(transactionCode)
                .amount(PAYMENT_AMOUNT)
                .financialMethod(FinancialMethod.NEQUI)
                .description("irrelevant")
                .status(TransactionStatus.PENDING)
                .applied(false)
                .build()));
    }

    private PaymentInfoDetailsDTO approve(String transactionCode) {

        return paymentService.validateTransaction(
                new TransactionValidationDTO(transactionCode, TransactionStatus.APPROVED));
    }

    private PaymentInfoDetailsDTO decline(String transactionCode) {

        return paymentService.validateTransaction(
                new TransactionValidationDTO(transactionCode, TransactionStatus.DECLINED));
    }

    @SafeVarargs
    private List<PaymentInfoDetailsDTO> runConcurrently(Callable<PaymentInfoDetailsDTO>... tasks) throws Exception {

        ExecutorService executor = Executors.newFixedThreadPool(tasks.length);
        CountDownLatch startLine = new CountDownLatch(1);

        try {

            List<Future<PaymentInfoDetailsDTO>> futures = Arrays.stream(tasks)
                    .map(task -> executor.submit(() -> {
                        startLine.await();
                        return task.call();
                    }))
                    .toList();

            startLine.countDown();

            List<PaymentInfoDetailsDTO> results = new ArrayList<>();
            for (Future<PaymentInfoDetailsDTO> future : futures) {
                results.add(future.get(15, TimeUnit.SECONDS));
            }
            return results;

        } finally {
            executor.shutdownNow();
        }
    }
}
