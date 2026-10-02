package com.carlosescobar30.apimicrocreditos.operational.service;

import com.carlosescobar30.apimicrocreditos.common.exception.bad_request.ActionNotPermitted;
import com.carlosescobar30.apimicrocreditos.common.exception.not_found.ResourceNotFoundException;
import com.carlosescobar30.apimicrocreditos.common.identity.UserDetailsImpl;
import com.carlosescobar30.apimicrocreditos.iam.adapter.UserAdapter;
import com.carlosescobar30.apimicrocreditos.iam.dto.UserAdapterResponseDTO;
import com.carlosescobar30.apimicrocreditos.operational.domain.Loan;
import com.carlosescobar30.apimicrocreditos.operational.domain.LoanProduct;
import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.LoanStatus;
import com.carlosescobar30.apimicrocreditos.operational.factory.LoanDTOFactory;
import com.carlosescobar30.apimicrocreditos.operational.mappers.LoanProductMapper;
import com.carlosescobar30.apimicrocreditos.operational.repository.LoanRepository;
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

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    private static final Long USER_ID = 7L;
    private static final UUID USER_REFERENCE = UUID.randomUUID();
    private static final UUID LOAN_REFERENCE = UUID.randomUUID();
    private static final UUID PRODUCT_REFERENCE = UUID.randomUUID();

    @Mock
    private UserAdapter userAdapter;
    @Mock
    private LoanRepository repository;
    @Mock
    private LoanProductService loanProductService;
    @Mock
    private LoanInstallmentService loanInstallmentService;
    @Mock
    private LoanDTOFactory loanDTOFactory;
    @Mock
    private LoanProductMapper mapper;

    private LoanService loanService;
    private UserDetailsImpl user;

    @BeforeEach
    void setUp() {

        this.loanService = new LoanService(
                userAdapter,
                repository,
                loanProductService,
                loanInstallmentService,
                loanDTOFactory,
                mapper,
                Clock.systemUTC());

        this.user = new UserDetailsImpl(USER_ID, "carlos", null, List.of());

        when(userAdapter.userInfo(USER_ID)).thenReturn(UserAdapterResponseDTO.builder()
                .userReference(USER_REFERENCE)
                .isIdentityVerified(true)
                .score(900)
                .build());
    }

    @Nested
    @DisplayName("Tests for the cancel method")
    class CancelTests {

        @Test
        void aPreApprovedLoanOfTheUserIsDeleted() {

            Loan loan = loanWithStatus(LoanStatus.PRE_APPROVED);
            when(repository.getByPublicIdAndUserReference(LOAN_REFERENCE, USER_REFERENCE))
                    .thenReturn(Optional.of(loan));

            loanService.cancel(user, LOAN_REFERENCE);

            verify(repository).delete(loan);
        }

        @Test
        void aLoanThatIsNotTheUsersOrDoesNotExistIsNotFound() {

            when(repository.getByPublicIdAndUserReference(LOAN_REFERENCE, USER_REFERENCE))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> loanService.cancel(user, LOAN_REFERENCE))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(repository, never()).delete(any());
        }

        @ParameterizedTest
        @EnumSource(value = LoanStatus.class, names = "PRE_APPROVED", mode = EnumSource.Mode.EXCLUDE)
        void aLoanPastPreApprovalCannotBeCancelled(LoanStatus status) {

            when(repository.getByPublicIdAndUserReference(LOAN_REFERENCE, USER_REFERENCE))
                    .thenReturn(Optional.of(loanWithStatus(status)));

            assertThatThrownBy(() -> loanService.cancel(user, LOAN_REFERENCE))
                    .isInstanceOf(ActionNotPermitted.class);

            verify(repository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("Tests for the request method")
    class RequestTests {

        @Test
        void theRequestsOfTheUserAreSerializedBeforeCheckingForAnotherLoanInProcess() {

            when(repository.existsByUserReferenceAndStatus(USER_REFERENCE, LoanStatus.IN_ARREARS)).thenReturn(false);
            when(loanProductService.getOneByReferenceAndUserScore(PRODUCT_REFERENCE, 900))
                    .thenReturn(Optional.of(LoanProduct.builder().name("Product").build()));
            when(repository.existsByUserReferenceAndStatus(USER_REFERENCE, LoanStatus.PRE_APPROVED)).thenReturn(true);

            loanService.request(user, PRODUCT_REFERENCE);

            InOrder order = inOrder(repository);
            order.verify(repository).lockLoanRequestsOf(USER_REFERENCE);
            order.verify(repository).existsByUserReferenceAndStatus(USER_REFERENCE, LoanStatus.IN_ARREARS);
            order.verify(repository).existsByUserReferenceAndStatus(USER_REFERENCE, LoanStatus.PRE_APPROVED);
            verify(repository, never()).save(any());
        }
    }

    private Loan loanWithStatus(LoanStatus status) {

        return Loan.builder()
                .userReference(USER_REFERENCE)
                .status(status)
                .build();
    }
}
