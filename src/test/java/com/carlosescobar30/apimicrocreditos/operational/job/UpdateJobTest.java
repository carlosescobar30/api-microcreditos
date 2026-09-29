package com.carlosescobar30.apimicrocreditos.operational.job;

import com.carlosescobar30.apimicrocreditos.operational.service.LoanInstallmentService;
import com.carlosescobar30.apimicrocreditos.operational.service.LoanService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class UpdateJobTest {

    @Mock
    private LoanService loanService;
    @Mock
    private LoanInstallmentService loanInstallmentService;

    @Test
    void installmentsAreUpdatedBeforeArrearsAndArrearsBeforeTheLoanStatus() {

        new UpdateJob(loanService, loanInstallmentService).run();

        InOrder order = inOrder(loanInstallmentService, loanService);
        order.verify(loanInstallmentService).updateStatus();
        order.verify(loanInstallmentService).updateArrears();
        order.verify(loanService).updateLoanStatus();
        order.verifyNoMoreInteractions();
    }
}
