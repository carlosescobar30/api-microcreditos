package com.carlosescobar30.apimicrocreditos.operational.job;

import com.carlosescobar30.apimicrocreditos.operational.service.LoanInstallmentService;
import com.carlosescobar30.apimicrocreditos.operational.service.LoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UpdateJob {

    private final LoanService loanService;
    private final LoanInstallmentService loanInstallmentService;

    @Scheduled(cron = "${operational.update-cron}")
    public void updateStatusInstallments(){

        loanInstallmentService.updateStatus();

    }

    @Scheduled(cron = "${operational.update-cron}")
    public void updateArrears(){

        loanInstallmentService.updateArrears();

    }

    @Scheduled(cron = "${operational.update-cron}")
    public void updateLoanStatus(){

        loanService.updateLoanStatus();

    }

}
