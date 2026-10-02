package com.carlosescobar30.apimicrocreditos.operational.service;

import com.carlosescobar30.apimicrocreditos.common.exception.not_found.ResourceNotFoundException;
import com.carlosescobar30.apimicrocreditos.common.identity.UserDetailsImpl;
import com.carlosescobar30.apimicrocreditos.iam.adapter.UserAdapter;
import com.carlosescobar30.apimicrocreditos.operational.attribute.RoundingAttributes;
import com.carlosescobar30.apimicrocreditos.operational.domain.Loan;
import com.carlosescobar30.apimicrocreditos.operational.domain.LoanInstallment;
import com.carlosescobar30.apimicrocreditos.operational.domain.LoanProduct;
import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.ObligationStatus;
import com.carlosescobar30.apimicrocreditos.operational.domain.rate.EffectiveRates;
import com.carlosescobar30.apimicrocreditos.operational.domain.rate.PenaltyRateSchedule;
import com.carlosescobar30.apimicrocreditos.operational.dto.InstallmentsInfoDTO;
import com.carlosescobar30.apimicrocreditos.operational.repository.LoanInstallmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.*;


@Service
@RequiredArgsConstructor
public class LoanInstallmentService {

    private final LoanInstallmentRepository repository;
    private final LoanInstallmentEngineService engine;
    private final UsuryRateService usuryRateService;
    private final UserAdapter userAdapter;
    private final Clock clock;

    @Transactional
    public void create(Loan loan) {

        LoanProduct loanProduct = loan.getLoanProduct();
        int installments = loanProduct.getInstallments();
        int periodicity = loanProduct.getPeriodicity();
        BigDecimal annualRate = loanProduct.getInterestRate();
        BigDecimal periodicInterest = EffectiveRates.toPeriodicRate(annualRate, periodicity);
        BigDecimal balance = loanProduct.getTotalPrincipal();
        BigDecimal principalAmount =  engine.calculatePrincipalAmount (loanProduct);
        List<LoanInstallment> allInstallments = new ArrayList<>();

        for(int installmentNumber = 1; installmentNumber <= installments; installmentNumber++){

            BigDecimal interestAmount = balance.multiply(periodicInterest)
                    .setScale(RoundingAttributes.SCALE_DEFAULT,
                            RoundingAttributes.ROUNDING_DEFAULT);

            if (installmentNumber < installments) {

                allInstallments.add(engine.createLoanInstallment(loan, principalAmount, interestAmount, installmentNumber));
            }

            if (installmentNumber == installments){

                balance = balance.setScale(
                        RoundingAttributes.SCALE_DEFAULT,
                        RoundingAttributes.ROUNDING_DEFAULT);
                allInstallments.add(engine.createLoanInstallment(loan, balance, interestAmount, installmentNumber));


            }

            balance = balance.subtract(principalAmount);

        }

        repository.saveAll(allInstallments);


    }

    @Transactional(readOnly = true)
    public Page<InstallmentsInfoDTO> getAllByLoanId (Long loanId){

        return repository.findByLoanId(loanId, Pageable.unpaged());

    }

    @Transactional(readOnly = true)
    public InstallmentsInfoDTO getOne (UserDetailsImpl userDetails, UUID loanInstallmentReference){

        UUID userReference = userAdapter.userInfo(userDetails.getId()).userReference();
        return repository.findByInstallmentReferenceAndUserReference(loanInstallmentReference, userReference)
                .orElseThrow(() -> new ResourceNotFoundException("Installment not found"));

    }

    @Transactional(readOnly = true)
    public LoanInstallment getOneEntity (UUID loanInstallmentReference){

        return repository.findByPublicId(loanInstallmentReference)
                .orElseThrow(() -> new ResourceNotFoundException("Installment not found"));

    }

    @Transactional(readOnly = true)
    public InstallmentsInfoDTO getCurrent (UserDetailsImpl userDetails, UUID loanReference){

        UUID userReference = userAdapter.userInfo(userDetails.getId()).userReference();
        return repository.findByStatusAndLoanReference(ObligationStatus.CURRENT, loanReference, userReference)
                .orElseThrow(() -> new ResourceNotFoundException("Installment not found"));

    }

    @Transactional(readOnly = true)
    public Page<InstallmentsInfoDTO> getAllOverdue (UserDetailsImpl userDetails, UUID loanReference, Pageable pageable){

        UUID userReference = userAdapter.userInfo(userDetails.getId()).userReference();

        if (!repository.existsByLoan_PublicIdAndLoan_UserReference(loanReference, userReference)){

            throw new ResourceNotFoundException("Installments not found");

        }

        Page<InstallmentsInfoDTO> installments = repository.findAllByStatusAndLoanReference(
                ObligationStatus.OVERDUE,
                loanReference,
                userReference,
                pageable);

        return installments;

    }

    @Transactional(readOnly = true)
    public Page<InstallmentsInfoDTO> getAll (UserDetailsImpl userDetails, UUID loanReference, Pageable pageable){

        UUID userReference = userAdapter.userInfo(userDetails.getId()).userReference();
        if (!repository.existsByLoan_PublicIdAndLoan_UserReference(loanReference, userReference)){

            throw new ResourceNotFoundException("Installments not found");

        }

        Page<InstallmentsInfoDTO> installments = repository.findAllByLoanReference(
                loanReference,
                userReference,
                pageable
        );

        return installments;


    }

    @Transactional(readOnly = true)
    public long countOverdue(Long loanId){

        return repository.countByLoan_IdAndStatus(loanId, ObligationStatus.OVERDUE);

    }

    public List<LoanInstallment> getAllDue (Loan loan){

        return repository.findAllByLoanAndStatusNotOrderByInstallmentNumberAsc(loan, ObligationStatus.PAID);

    }

    @Transactional
    public void updateStatus(){

        LocalDate today = LocalDate.now(clock);
        repository.changeStatusToOverdue(clock.instant(), today);
        repository.changeStatusToCurrent(clock.instant(), today.plusMonths(1));

    }

    @Transactional
    public void updateArrears (){

        LocalDate today = LocalDate.now(clock);
        List<LoanInstallment> installments = repository.findAllByStatus(ObligationStatus.OVERDUE);
        Map<Long, PenaltyRateSchedule> penaltyRatesByProduct = new HashMap<>();

        for (LoanInstallment installment : installments){

            LoanProduct product = installment.getLoan().getLoanProduct();
            PenaltyRateSchedule penaltyRates = penaltyRatesByProduct.computeIfAbsent(product.getId(),
                    id -> usuryRateService.penaltyScheduleFor(product));

            installment.accrueArrears(today, penaltyRates);

        }

    }

}
