package com.carlosescobar30.apimicrocreditos.operational.service;

import com.carlosescobar30.apimicrocreditos.common.exception.bad_request.ActionNotPermitted;
import com.carlosescobar30.apimicrocreditos.common.exception.conflict.PaymentAlreadyProcessedException;
import com.carlosescobar30.apimicrocreditos.common.exception.conflict.TransactionCodeConflictException;
import com.carlosescobar30.apimicrocreditos.common.exception.not_found.ResourceNotFoundException;
import com.carlosescobar30.apimicrocreditos.common.identity.UserDetailsImpl;
import com.carlosescobar30.apimicrocreditos.iam.adapter.UserAdapter;
import com.carlosescobar30.apimicrocreditos.iam.dto.UserAdapterResponseDTO;
import com.carlosescobar30.apimicrocreditos.operational.domain.Loan;
import com.carlosescobar30.apimicrocreditos.operational.domain.Payment;
import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.FinancialMethod;
import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.LoanStatus;
import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.PaymentApplication;
import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.TransactionStatus;
import com.carlosescobar30.apimicrocreditos.operational.dto.AllocationDetailsDTO;
import com.carlosescobar30.apimicrocreditos.operational.dto.PayRequestDTO;
import com.carlosescobar30.apimicrocreditos.operational.dto.PaymentInfoDTO;
import com.carlosescobar30.apimicrocreditos.operational.dto.PaymentInfoDetailsDTO;
import com.carlosescobar30.apimicrocreditos.operational.dto.TransactionValidationDTO;
import com.carlosescobar30.apimicrocreditos.operational.factory.PaymentDTOFactory;
import com.carlosescobar30.apimicrocreditos.operational.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final Long LOAN_ID = 21L;
    private static final UUID USER_REFERENCE = UUID.randomUUID();
    private static final String TRANSACTION_CODE = "TX-001";
    private static final Long USER_ID = 7L;
    private static final UUID LOAN_REFERENCE = UUID.randomUUID();
    private static final UserDetailsImpl USER = new UserDetailsImpl(USER_ID, "carlos", null, List.of());

    @Mock
    private PaymentRepository repository;
    @Mock
    private LoanInstallmentService loanInstallmentService;
    @Mock
    private LoanService loanService;
    @Mock
    private PaymentAllocationService paymentAllocationService;
    @Mock
    private UserAdapter userAdapter;

    private PaymentService paymentService;
    private Loan loan;

    @BeforeEach
    void setUp() {

        this.paymentService = new PaymentService(
                repository,
                loanInstallmentService,
                loanService,
                paymentAllocationService,
                new PaymentDTOFactory(),
                userAdapter);

        this.loan = Loan.builder()
                .userReference(USER_REFERENCE)
                .status(LoanStatus.ACTIVE)
                .build();
        this.loan.setId(LOAN_ID);
    }

    @Nested
    @DisplayName("Requests rejected before any state changes")
    class RejectedRequests {

        @Test
        void pendingIsNotAValidTargetStatusAndNothingIsLocked() {

            assertThatThrownBy(() -> validate(TransactionStatus.PENDING))
                    .isInstanceOf(ActionNotPermitted.class);

            verifyNoInteractions(repository, loanService, paymentAllocationService);
        }

        @Test
        void anUnknownTransactionCodeIsNotFound() {

            when(repository.findByTransactionCodeForUpdate(TRANSACTION_CODE)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> validate(TransactionStatus.APPROVED))
                    .isInstanceOf(ResourceNotFoundException.class);

            verifyNoInteractions(paymentAllocationService);
        }
    }

    @Nested
    @DisplayName("Transitions out of PENDING")
    class PendingTransitions {

        @Test
        void approvingAPendingPaymentAllocatesItExactlyOnce() {

            Payment payment = givenOwnedPayment(TransactionStatus.PENDING);
            List<AllocationDetailsDTO> allocations = List.of(allocation());
            when(paymentAllocationService.allocatePayment(payment)).thenReturn(allocations);

            PaymentInfoDetailsDTO result = validate(TransactionStatus.APPROVED);

            assertThat(result.status()).isEqualTo(TransactionStatus.APPROVED);
            assertThat(result.details()).isEqualTo(allocations);
            assertThat(payment.getApplied()).isTrue();
            verify(paymentAllocationService).allocatePayment(payment);
        }

        @Test
        void decliningAPendingPaymentNeverAllocatesIt() {

            Payment payment = givenOwnedPayment(TransactionStatus.PENDING);

            PaymentInfoDetailsDTO result = validate(TransactionStatus.DECLINED);

            assertThat(result.status()).isEqualTo(TransactionStatus.DECLINED);
            assertThat(result.details()).isEmpty();
            assertThat(payment.getApplied()).isFalse();
            verifyNoInteractions(paymentAllocationService);
        }

        @Test
        void thePaymentIsLockedBeforeTheLoanAndBothBeforeAllocating() {

            Payment payment = givenOwnedPayment(TransactionStatus.PENDING);
            when(paymentAllocationService.allocatePayment(payment)).thenReturn(List.of());

            validate(TransactionStatus.APPROVED);

            InOrder order = inOrder(repository, loanService, paymentAllocationService);
            order.verify(repository).findByTransactionCodeForUpdate(TRANSACTION_CODE);
            order.verify(loanService).getOneEntityForUpdate(LOAN_ID);
            order.verify(paymentAllocationService).allocatePayment(payment);
        }
    }

    @Nested
    @DisplayName("Final states cannot change")
    class FinalStates {

        @Test
        void anApprovedPaymentCannotBeDeclined() {

            Payment payment = givenOwnedPayment(TransactionStatus.APPROVED);

            assertThatThrownBy(() -> validate(TransactionStatus.DECLINED))
                    .isInstanceOf(PaymentAlreadyProcessedException.class);

            assertThat(payment.getStatus()).isEqualTo(TransactionStatus.APPROVED);
            assertThat(payment.getApplied()).isTrue();
            verify(paymentAllocationService, never()).allocatePayment(any());
        }

        @Test
        void aDeclinedPaymentCannotBeApprovedLater() {

            Payment payment = givenOwnedPayment(TransactionStatus.DECLINED);

            assertThatThrownBy(() -> validate(TransactionStatus.APPROVED))
                    .isInstanceOf(PaymentAlreadyProcessedException.class);

            assertThat(payment.getStatus()).isEqualTo(TransactionStatus.DECLINED);
            assertThat(payment.getApplied()).isFalse();
            verify(paymentAllocationService, never()).allocatePayment(any());
        }
    }

    @Nested
    @DisplayName("Retries with the same status are idempotent")
    class Retries {

        @Test
        void retryingAnApprovalReturnsTheOriginalAllocationsWithoutAllocatingAgain() {

            Payment payment = givenOwnedPayment(TransactionStatus.APPROVED);
            List<AllocationDetailsDTO> original = List.of(allocation());
            when(paymentAllocationService.getAllByPayment(payment)).thenReturn(original);

            PaymentInfoDetailsDTO result = validate(TransactionStatus.APPROVED);

            assertThat(result.status()).isEqualTo(TransactionStatus.APPROVED);
            assertThat(result.details()).isEqualTo(original);
            verify(paymentAllocationService, never()).allocatePayment(any());
        }

        @Test
        void retryingADeclineChangesNothing() {

            givenOwnedPayment(TransactionStatus.DECLINED);

            PaymentInfoDetailsDTO result = validate(TransactionStatus.DECLINED);

            assertThat(result.status()).isEqualTo(TransactionStatus.DECLINED);
            assertThat(result.details()).isEmpty();
            verifyNoInteractions(paymentAllocationService);
        }
    }

    @Nested
    @DisplayName("Payments registered by the user")
    class PaymentRequests {

        @ParameterizedTest
        @EnumSource(value = LoanStatus.class, names = {"ACTIVE", "IN_ARREARS"})
        void aPaymentIsRegisteredAsPendingWhileTheLoanIsActiveOrInArrears(LoanStatus status) {

            givenLoanOfTheCallerIn(status);
            when(repository.existsByTransactionCode(TRANSACTION_CODE)).thenReturn(false);
            when(repository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

            PaymentInfoDTO payment = paymentService.pay(USER, LOAN_REFERENCE, payRequest(FinancialMethod.NEQUI));

            assertThat(payment.status()).isEqualTo(TransactionStatus.PENDING);
            assertThat(payment.amount()).isEqualByComparingTo("150000.0000");
        }

        @ParameterizedTest
        @EnumSource(value = LoanStatus.class, names = {"PRE_APPROVED", "COMPLETED", "REJECTED"})
        void aLoanThatIsNotActiveOrInArrearsDoesNotAcceptPayments(LoanStatus status) {

            givenLoanOfTheCallerIn(status);

            assertThatThrownBy(() -> paymentService.pay(USER, LOAN_REFERENCE, payRequest(FinancialMethod.NEQUI)))
                    .isInstanceOf(ActionNotPermitted.class);

            verify(repository, never()).save(any());
        }

        @Test
        void anAdjustmentCannotBeRegisteredByAUser() {

            assertThatThrownBy(() -> paymentService.pay(USER, LOAN_REFERENCE, payRequest(FinancialMethod.ADJUSTMENT)))
                    .isInstanceOf(ActionNotPermitted.class);

            verifyNoInteractions(loanService, repository);
        }

        @Test
        void aTransactionCodeAlreadyRegisteredIsAConflict() {

            givenLoanOfTheCallerIn(LoanStatus.ACTIVE);
            when(repository.existsByTransactionCode(TRANSACTION_CODE)).thenReturn(true);

            assertThatThrownBy(() -> paymentService.pay(USER, LOAN_REFERENCE, payRequest(FinancialMethod.NEQUI)))
                    .isInstanceOf(TransactionCodeConflictException.class);

            verify(repository, never()).save(any());
        }

        @Test
        void aLoanOfAnotherUserIsNotFound() {

            when(loanService.getOneEntity(LOAN_REFERENCE)).thenReturn(loan);
            when(userAdapter.userInfo(USER_ID)).thenReturn(UserAdapterResponseDTO.builder()
                    .userReference(UUID.randomUUID())
                    .build());

            assertThatThrownBy(() -> paymentService.pay(USER, LOAN_REFERENCE, payRequest(FinancialMethod.NEQUI)))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("The loan does not exist");

            verify(repository, never()).save(any());
        }

        private void givenLoanOfTheCallerIn(LoanStatus status) {

            loan.setStatus(status);
            when(loanService.getOneEntity(LOAN_REFERENCE)).thenReturn(loan);
            when(userAdapter.userInfo(USER_ID)).thenReturn(UserAdapterResponseDTO.builder()
                    .userReference(USER_REFERENCE)
                    .build());
        }

        private PayRequestDTO payRequest(FinancialMethod method) {

            return new PayRequestDTO(TRANSACTION_CODE, method, new BigDecimal("150000.0000"), "October installment");
        }
    }

    private PaymentInfoDetailsDTO validate(TransactionStatus status) {

        return paymentService.validateTransaction(new TransactionValidationDTO(TRANSACTION_CODE, status));
    }

    private Payment givenOwnedPayment(TransactionStatus status) {

        Payment payment = paymentWithStatus(status);
        givenLockedPaymentAndLoan(payment);
        return payment;
    }

    private void givenLockedPaymentAndLoan(Payment payment) {

        when(repository.findByTransactionCodeForUpdate(TRANSACTION_CODE)).thenReturn(Optional.of(payment));
        when(loanService.getOneEntityForUpdate(LOAN_ID)).thenReturn(loan);
    }

    private Payment paymentWithStatus(TransactionStatus status) {

        return Payment.builder()
                .loan(loan)
                .transactionCode(TRANSACTION_CODE)
                .amount(new BigDecimal("150000.0000"))
                .financialMethod(FinancialMethod.NEQUI)
                .description("irrelevant")
                .status(status)
                .applied(status == TransactionStatus.APPROVED)
                .build();
    }

    private AllocationDetailsDTO allocation() {

        return new AllocationDetailsDTO(UUID.randomUUID(), new BigDecimal("150000.0000"),
                PaymentApplication.PRINCIPAL, Instant.parse("2099-01-01T00:00:00Z"));
    }
}
