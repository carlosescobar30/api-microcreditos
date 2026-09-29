package com.carlosescobar30.apimicrocreditos.operational.service;

import com.carlosescobar30.apimicrocreditos.common.exception.not_found.ResourceNotFoundException;
import com.carlosescobar30.apimicrocreditos.common.identity.UserDetailsImpl;
import com.carlosescobar30.apimicrocreditos.iam.adapter.UserAdapter;
import com.carlosescobar30.apimicrocreditos.iam.dto.UserAdapterResponseDTO;
import com.carlosescobar30.apimicrocreditos.operational.dto.InstallmentsInfoDTO;
import com.carlosescobar30.apimicrocreditos.operational.repository.LoanInstallmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanInstallmentServiceTest {

    private static final Long USER_ID = 7L;
    private static final UUID USER_REFERENCE = UUID.randomUUID();
    private static final UUID INSTALLMENT_REFERENCE = UUID.randomUUID();

    @Mock
    private LoanInstallmentRepository repository;
    @Mock
    private LoanInstallmentEngineService engine;
    @Mock
    private LoanProductService loanProductService;
    @Mock
    private UserAdapter userAdapter;

    private LoanInstallmentService loanInstallmentService;
    private UserDetailsImpl user;

    @BeforeEach
    void setUp() {

        this.loanInstallmentService = new LoanInstallmentService(
                repository,
                engine,
                loanProductService,
                userAdapter,
                Clock.systemUTC());

        this.user = new UserDetailsImpl(USER_ID, "carlos", null, List.of());

        when(userAdapter.userInfo(USER_ID)).thenReturn(UserAdapterResponseDTO.builder()
                .userReference(USER_REFERENCE)
                .isIdentityVerified(true)
                .score(900)
                .build());
    }

    @Nested
    @DisplayName("Tests for the getOne method")
    class GetOneTests {

        @Test
        void theInstallmentIsSearchedOnlyAmongTheLoansOfTheCaller() {

            InstallmentsInfoDTO installment = new InstallmentsInfoDTO(
                    INSTALLMENT_REFERENCE,
                    new BigDecimal("100000.0000"),
                    new BigDecimal("22523.0000"),
                    new BigDecimal("122523.0000"),
                    LocalDate.of(2099, 2, 10));
            when(repository.findByInstallmentReferenceAndUserReference(INSTALLMENT_REFERENCE, USER_REFERENCE))
                    .thenReturn(Optional.of(installment));

            assertThat(loanInstallmentService.getOne(user, INSTALLMENT_REFERENCE)).isEqualTo(installment);
        }

        @Test
        void anInstallmentOfAnotherUserIsNotFound() {

            when(repository.findByInstallmentReferenceAndUserReference(INSTALLMENT_REFERENCE, USER_REFERENCE))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> loanInstallmentService.getOne(user, INSTALLMENT_REFERENCE))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
