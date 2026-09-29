package com.carlosescobar30.apimicrocreditos.operational.service;

import com.carlosescobar30.apimicrocreditos.common.exception.bad_request.ActionNotPermitted;
import com.carlosescobar30.apimicrocreditos.common.exception.not_found.ResourceNotFoundException;
import com.carlosescobar30.apimicrocreditos.operational.factory.LoanDTOFactory;
import com.carlosescobar30.apimicrocreditos.common.identity.UserDetailsImpl;
import com.carlosescobar30.apimicrocreditos.iam.adapter.UserAdapter;
import com.carlosescobar30.apimicrocreditos.iam.dto.UserAdapterResponseDTO;
import com.carlosescobar30.apimicrocreditos.operational.domain.Loan;
import com.carlosescobar30.apimicrocreditos.operational.domain.LoanProduct;
import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.LoanStatus;
import com.carlosescobar30.apimicrocreditos.operational.dto.*;
import com.carlosescobar30.apimicrocreditos.operational.enums.LoanAvailability;
import com.carlosescobar30.apimicrocreditos.operational.mappers.LoanProductMapper;
import com.carlosescobar30.apimicrocreditos.operational.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LoanService {

    private final UserAdapter userAdapter;
    private final LoanRepository repository;
    private final LoanProductService loanProductService;
    private final LoanInstallmentService loanInstallmentService;
    private final LoanDTOFactory loanDTOFactory;
    private final LoanProductMapper mapper;

    @Transactional
    public LoanAvailabilityDTO request(UserDetailsImpl userDetails, UUID loanProductReference){

        LoanAvailability loanAvailability = LoanAvailability.AVAILABLE;
        UserAdapterResponseDTO userAdapterResponseDTO = userAdapter.userInfo(userDetails.getId());

        if (!userAdapterResponseDTO.isIdentityVerified()){

            return loanDTOFactory.buildRejected(LoanAvailability.REJECTED_BY_UNVERIFIED_IDENTITY);

        }

        if (repository.existsByUserReferenceAndStatus(userAdapterResponseDTO.userReference(), LoanStatus.IN_ARREARS)){

            return loanDTOFactory.buildRejected(LoanAvailability.REJECTED_BY_ARRERS);

        }

        Optional<LoanProduct> product = loanProductService
                .getOneByReferenceAndUserScore(loanProductReference, userAdapterResponseDTO.score());


        if (product.isEmpty()){

            return loanDTOFactory.buildRejected(LoanAvailability.REJECTED_BY_LOW_SCORE);

        }

        if (repository.existsByUserReferenceAndStatus(userAdapterResponseDTO.userReference(), LoanStatus.PRE_APPROVED)){

            return loanDTOFactory.buildRejected(LoanAvailability.REJECTED_BY_OTHER_LOAN_IN_PROCESS);

        }

        Loan loan = create(
                userAdapterResponseDTO.userReference(),
                product.get());

        repository.save(loan);
        AvailableLoanProductDTO productDTO =  mapper.toAvailableLoanProductsDTO(product.get());

        return loanDTOFactory.buildPreApproved(productDTO);

    }


    @Transactional
    public void cancel(UserDetailsImpl userDetails, UUID loanProductReference){

        UUID userReference = userAdapter.userInfo(userDetails.getId()).userReference();

        if (repository.existsByUserReferenceAndStatus(userReference, LoanStatus.PRE_APPROVED)){

            repository.deleteByUserReferenceAndLoanProduct_PublicId(userReference, loanProductReference);
            return;

        }

        if (repository.existsByUserReference(userReference)){

            throw new ActionNotPermitted("The loan cannot be cancelled");

        }

        throw new ResourceNotFoundException("The loan does not exist");

    }

    @Transactional
    public LoanAndInstallmentsInfoDTO accept(UserDetailsImpl userDetails, UUID loanReference ){

        UUID userReference = userAdapter.userInfo(userDetails.getId()).userReference();
        Loan loan = repository.getByPublicIdAndUserReference(loanReference, userReference)
                .orElseThrow(() -> new ResourceNotFoundException("The loan does not exist"));

        if (!loan.getStatus().equals(LoanStatus.PRE_APPROVED)){

            throw new ActionNotPermitted("The loan cannot perform this action");

        }

        updateToActive(loan);
        loanInstallmentService.create(loan);
        Page<InstallmentsInfoDTO> installments = loanInstallmentService.getAllByLoanId(loan.getId());
        long overdueInstallments = loanInstallmentService.countOverdue(loan.getId());
        return loanDTOFactory.buildAccepted(loan, installments, overdueInstallments);

    }

    @Transactional
    public void deductSettledAmount (Loan loan, BigDecimal payment){

        BigDecimal newPrincipalReceivable = loan.getPrincipalReceivable().subtract(payment);
        loan.setPrincipalReceivable(newPrincipalReceivable);

    }

    @Transactional(readOnly = true)
    public Page<LoanInfoDTO> getAll (UserDetailsImpl userDetails, Pageable pageable){

        UUID userReference = userAdapter.userInfo(userDetails.getId()).userReference();
        return repository.findAllByUserReference(userReference, pageable);


    }

    @Transactional(readOnly = true)
    public LoanInfoDTO getOne(UserDetailsImpl userDetails, UUID loanReference){

        UUID userReference = userAdapter.userInfo(userDetails.getId()).userReference();
        Loan loan = repository.getByPublicIdAndUserReference(loanReference, userReference)
                .orElseThrow(() -> new ResourceNotFoundException("The loan does not exist"));

        long overdueInstallments = loanInstallmentService.countOverdue(loan.getId());
        return loanDTOFactory.buildLoanInfo(loan, overdueInstallments);

    }

    @Transactional
    public Loan getOneEntity(UUID loanReference){

        return repository.findByPublicId(loanReference)
                .orElseThrow(() -> new ResourceNotFoundException("The loan does not exist"));

    }

    @Transactional
    public Loan getOneEntityForUpdate(Long loanId){

        return repository.findByIdForUpdate(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("The loan does not exist"));

    }

    @Transactional
    public void updateLoanStatus(){

        repository.updateStatusToInArrears();
        repository.updateStatusToActive();
        repository.updateStatusToCompleted();

    }


    private void updateToActive(Loan loan){

        LoanProduct loanProduct = loan.getLoanProduct();
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.getDayOfMonth() < 29
                ? today
                : today.plusMonths(1).withDayOfMonth(1);

        loan.setPrincipalReceivable(loanProduct.getTotalPrincipal());
        loan.setStartDate(startDate);
        loan.setEndDate((startDate.plusMonths(loanProduct.getInstallments())));
        loan.setPayday(startDate.getDayOfMonth());
        loan.setStatus(LoanStatus.ACTIVE);

    }

    private Loan create(UUID userReference, LoanProduct loanProduct) {

        return Loan.builder()
                .userReference(userReference)
                .loanProduct(loanProduct)
                .status(LoanStatus.PRE_APPROVED)
                .build();

    }








}
