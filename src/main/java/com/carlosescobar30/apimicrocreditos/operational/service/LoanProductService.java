package com.carlosescobar30.apimicrocreditos.operational.service;

import com.carlosescobar30.apimicrocreditos.common.exception.not_found.ResourceNotFoundException;
import com.carlosescobar30.apimicrocreditos.iam.adapter.UserAdapter;
import com.carlosescobar30.apimicrocreditos.operational.domain.LoanProduct;
import com.carlosescobar30.apimicrocreditos.operational.dto.AvailableLoanProductDTO;
import com.carlosescobar30.apimicrocreditos.operational.dto.LoanProductSpecificationDTO;
import com.carlosescobar30.apimicrocreditos.operational.mappers.LoanProductMapper;
import com.carlosescobar30.apimicrocreditos.operational.repository.LoanProductRepository;
import com.carlosescobar30.apimicrocreditos.operational.repository.specification.LoanProductSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LoanProductService {

    private final LoanProductRepository repository;
    private final LoanProductMapper mapper;
    private final UserAdapter userAdapter;

    @Transactional(readOnly = true)
    public Page<AvailableLoanProductDTO> getAll(Pageable pageable){

        return repository.findAllLoanProducts(pageable);

    }

    @Transactional(readOnly = true)
    public AvailableLoanProductDTO getOneByReference(UUID reference) {

        return repository.findOneProduct(reference);

    }

    @Transactional(readOnly = true)
    public Optional<LoanProduct> getOneByReferenceAndUserScore(UUID reference, Integer userScore) {

        return repository.findByPublicIdAndMinimumUserScoreLessThanEqual(reference, userScore);

    }

    @Transactional(readOnly = true)
    public Page<AvailableLoanProductDTO> getBySpecification(LoanProductSpecificationDTO dto,
                                                            Pageable pageable) {

        Specification<LoanProduct> specification = LoanProductSpecification.build(dto);
        Page<LoanProduct> loanProducts = repository.findAll(specification,pageable);

        return loanProducts.map(mapper::toAvailableLoanProductsDTO);

    }

    @Transactional(readOnly = true)
    public LoanProduct getById(Long id){

        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("The product does not exist"));

    }

}
