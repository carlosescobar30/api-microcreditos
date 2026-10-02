package com.carlosescobar30.apimicrocreditos.operational.service;

import com.carlosescobar30.apimicrocreditos.TestcontainersConfiguration;
import com.carlosescobar30.apimicrocreditos.common.identity.UserDetailsImpl;
import com.carlosescobar30.apimicrocreditos.iam.adapter.UserAdapter;
import com.carlosescobar30.apimicrocreditos.iam.dto.UserAdapterResponseDTO;
import com.carlosescobar30.apimicrocreditos.operational.domain.Loan;
import com.carlosescobar30.apimicrocreditos.operational.domain.LoanProduct;
import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.CreditModality;
import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.LoanStatus;
import com.carlosescobar30.apimicrocreditos.operational.dto.LoanAvailabilityDTO;
import com.carlosescobar30.apimicrocreditos.operational.enums.LoanAvailability;
import com.carlosescobar30.apimicrocreditos.operational.repository.LoanInstallmentRepository;
import com.carlosescobar30.apimicrocreditos.operational.repository.LoanProductRepository;
import com.carlosescobar30.apimicrocreditos.operational.repository.LoanRepository;
import com.carlosescobar30.apimicrocreditos.operational.repository.PaymentAllocationRepository;
import com.carlosescobar30.apimicrocreditos.operational.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class LoanRequestConcurrencyIT {

    private static final Long USER_ID = 7L;
    private static final UUID USER_REFERENCE = UUID.randomUUID();
    private static final UserDetailsImpl USER = new UserDetailsImpl(USER_ID, "carlos", null, List.of());

    @MockitoBean
    private UserAdapter userAdapter;

    @Autowired
    private LoanService loanService;
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

    private LoanProduct product;

    @BeforeEach
    void setUp() {

        paymentAllocationRepository.deleteAllInBatch();
        paymentRepository.deleteAllInBatch();
        loanInstallmentRepository.deleteAllInBatch();
        loanRepository.deleteAllInBatch();
        loanProductRepository.deleteAllInBatch();

        product = loanProductRepository.save(LoanProduct.builder()
                .name("Concurrency product")
                .totalPrincipal(new BigDecimal("1000000.0000"))
                .interestRate(new BigDecimal("0.2000"))
                .penaltyRateEa(new BigDecimal("0.2000"))
                .creditModality(CreditModality.CONSUMER_AND_ORDINARY)
                .installments(12)
                .periodicity(12)
                .minimumUserScore(1)
                .build());

        when(userAdapter.userInfo(USER_ID)).thenReturn(UserAdapterResponseDTO.builder()
                .userReference(USER_REFERENCE)
                .isIdentityVerified(true)
                .score(900)
                .build());
    }

    @Test
    void twoSimultaneousRequestsOfTheSameUserPreApproveOnlyOneLoan() throws Exception {

        List<LoanAvailabilityDTO> results = runConcurrently(2);

        assertThat(results)
                .extracting(LoanAvailabilityDTO::loanAvailability)
                .containsExactlyInAnyOrder(
                        LoanAvailability.AVAILABLE,
                        LoanAvailability.REJECTED_BY_OTHER_LOAN_IN_PROCESS);
        assertThat(loanRepository.findAll())
                .filteredOn(loan -> loan.getStatus() == LoanStatus.PRE_APPROVED)
                .hasSize(1);
    }

    @Test
    void theDatabaseRejectsASecondPreApprovedLoanForTheSameUser() {

        loanRepository.saveAndFlush(preApprovedLoan());

        assertThatThrownBy(() -> loanRepository.saveAndFlush(preApprovedLoan()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Loan preApprovedLoan() {

        return Loan.builder()
                .userReference(USER_REFERENCE)
                .loanProduct(product)
                .status(LoanStatus.PRE_APPROVED)
                .build();
    }

    private List<LoanAvailabilityDTO> runConcurrently(int requests) throws Exception {

        ExecutorService executor = Executors.newFixedThreadPool(requests);
        CountDownLatch startLine = new CountDownLatch(1);

        try {

            List<Future<LoanAvailabilityDTO>> futures = new ArrayList<>();
            for (int i = 0; i < requests; i++) {
                futures.add(executor.submit(() -> {
                    startLine.await();
                    return loanService.request(USER, product.getPublicId());
                }));
            }

            startLine.countDown();

            List<LoanAvailabilityDTO> results = new ArrayList<>();
            for (Future<LoanAvailabilityDTO> future : futures) {
                results.add(future.get(15, TimeUnit.SECONDS));
            }
            return results;

        } finally {
            executor.shutdownNow();
        }
    }
}
