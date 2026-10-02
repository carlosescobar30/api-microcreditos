package com.carlosescobar30.apimicrocreditos.operational.job;

import com.carlosescobar30.apimicrocreditos.common.configuration.ClockConfig;
import com.carlosescobar30.apimicrocreditos.operational.domain.rate.PenaltyRateSchedule;
import com.carlosescobar30.apimicrocreditos.operational.service.LoanInstallmentService;
import com.carlosescobar30.apimicrocreditos.operational.service.LoanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class UpdateJob {

    private final LoanService loanService;
    private final LoanInstallmentService loanInstallmentService;

    @Scheduled(cron = "${operational.update-cron}", zone = ClockConfig.TIME_ZONE)
    public void run(){

        loanInstallmentService.updateStatus();
        accrueArrears();
        loanService.updateLoanStatus();

    }

    private void accrueArrears(){

        Map<Long, PenaltyRateSchedule> penaltyRatesByProduct = loanInstallmentService.penaltySchedulesForOverdueLoans();

        for (Long loanId : loanInstallmentService.findLoansWithOverdueInstallments()){

            try {

                loanInstallmentService.accrueArrearsOf(loanId, penaltyRatesByProduct);

            } catch (RuntimeException exception) {

                log.error("Arrears could not be accrued for loanId: {}", loanId, exception);

            }

        }

    }

}
