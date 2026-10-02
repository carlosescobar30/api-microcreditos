package com.carlosescobar30.apimicrocreditos.operational.repository;

import com.carlosescobar30.apimicrocreditos.operational.domain.LoanProduct;
import com.carlosescobar30.apimicrocreditos.operational.dto.AvailableLoanProductDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LoanProductRepository extends JpaRepository<LoanProduct, Long>, JpaSpecificationExecutor<LoanProduct> {

    @Query("SELECT new com.carlosescobar30.apimicrocreditos.operational.dto.AvailableLoanProductDTO " +
            "(lp.publicId, lp.name, lp.totalPrincipal, lp.interestRate, lp.penaltyRateEa, lp.creditModality, lp.installments) " +
            "FROM LoanProduct lp")
    Page<AvailableLoanProductDTO> findAllLoanProducts(Pageable pageable);

    @Query("SELECT new com.carlosescobar30.apimicrocreditos.operational.dto.AvailableLoanProductDTO " +
            "(lp.publicId, lp.name, lp.totalPrincipal, lp.interestRate, lp.penaltyRateEa, lp.creditModality, lp.installments) " +
            "FROM LoanProduct lp " +
            "WHERE lp.publicId = :loanProductReference")
    AvailableLoanProductDTO findOneProduct(@Param("loanProductReference") UUID loanProductReference);

    Optional<LoanProduct> findByPublicIdAndMinimumUserScoreLessThanEqual(UUID publicId, Integer minimumUserScore);

    boolean existsByName(String name);



}
