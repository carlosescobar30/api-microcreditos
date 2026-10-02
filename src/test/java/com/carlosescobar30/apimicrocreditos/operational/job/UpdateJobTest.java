package com.carlosescobar30.apimicrocreditos.operational.job;

import com.carlosescobar30.apimicrocreditos.operational.domain.rate.PenaltyRateSchedule;
import com.carlosescobar30.apimicrocreditos.operational.service.LoanInstallmentService;
import com.carlosescobar30.apimicrocreditos.operational.service.LoanService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateJobTest {

    private static final Map<Long, PenaltyRateSchedule> PENALTY_RATES = Map.of(1L, mock(PenaltyRateSchedule.class));

    @Mock
    private LoanService loanService;
    @Mock
    private LoanInstallmentService loanInstallmentService;

    @Test
    void statusesAreUpdatedThenArrearsLoanByLoanAndFinallyTheLoanStatus() {

        when(loanInstallmentService.penaltySchedulesForOverdueLoans()).thenReturn(PENALTY_RATES);
        when(loanInstallmentService.findLoansWithOverdueInstallments()).thenReturn(List.of(10L, 20L));

        new UpdateJob(loanService, loanInstallmentService).run();

        InOrder order = inOrder(loanInstallmentService, loanService);
        order.verify(loanInstallmentService).updateStatus();
        order.verify(loanInstallmentService).penaltySchedulesForOverdueLoans();
        order.verify(loanInstallmentService).findLoansWithOverdueInstallments();
        order.verify(loanInstallmentService).accrueArrearsOf(10L, PENALTY_RATES);
        order.verify(loanInstallmentService).accrueArrearsOf(20L, PENALTY_RATES);
        order.verify(loanService).updateLoanStatus();
        order.verifyNoMoreInteractions();
    }

    @Test
    void aLoanThatFailsDoesNotStopTheOthersNorTheLoanStatusUpdate() {

        when(loanInstallmentService.penaltySchedulesForOverdueLoans()).thenReturn(PENALTY_RATES);
        when(loanInstallmentService.findLoansWithOverdueInstallments()).thenReturn(List.of(10L, 20L));
        doThrow(new IllegalStateException("There is no usury rate registered"))
                .when(loanInstallmentService).accrueArrearsOf(eq(10L), any());

        new UpdateJob(loanService, loanInstallmentService).run();

        verify(loanInstallmentService).accrueArrearsOf(20L, PENALTY_RATES);
        verify(loanService).updateLoanStatus();
    }
}
