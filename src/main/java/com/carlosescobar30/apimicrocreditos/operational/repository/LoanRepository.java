package com.carlosescobar30.apimicrocreditos.operational.repository;

import com.carlosescobar30.apimicrocreditos.operational.domain.Loan;
import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.LoanStatus;
import com.carlosescobar30.apimicrocreditos.operational.dto.LoanInfoDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface LoanRepository extends JpaRepository<Loan, Long> {


    boolean existsByUserReferenceAndStatus(UUID userReference, LoanStatus loanStatus);
    boolean existsByUserReference(UUID userReference);
    Optional<Loan> getByPublicIdAndUserReference(UUID loanReference, UUID userReference);
    void deleteByUserReferenceAndLoanProduct_PublicId(UUID userReference, UUID loanProductReference);

    @Query("SELECT new com.carlosescobar30.apimicrocreditos.operational.dto.LoanInfoDTO(" +
            "l.publicId, l.loanProduct.publicId, " +
            "l.userReference, " +
            "l.principalReceivable," +
            "l.loanProduct.installments," +
            "(SELECT COUNT(li) FROM LoanInstallment li " +
            "WHERE li.loan = l " +
            "AND li.status = com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.ObligationStatus.OVERDUE)," +
            "l.payday, l.startDate, l.endDate) FROM Loan l WHERE l.userReference = :userReference ")
    Page<LoanInfoDTO> findAllByUserReference(@Param("userReference") UUID userReference, Pageable pageable);
    Optional<Loan> findByPublicId (UUID loanReference);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM Loan l WHERE l.id = :id")
    Optional<Loan> findByIdForUpdate(@Param("id") Long id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Loan l
            SET l.status = com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.LoanStatus.IN_ARREARS
            WHERE EXISTS(
            SELECT 1 FROM l.installments i
            WHERE i.status = com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.ObligationStatus.OVERDUE)
            AND l.status NOT IN (
            com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.LoanStatus.REJECTED,
            com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.LoanStatus.PRE_APPROVED)
            """)
    void updateStatusToInArrears();

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Loan l
            SET l.status = com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.LoanStatus.ACTIVE
            WHERE NOT EXISTS(
            SELECT 1 FROM l.installments i
            WHERE i.status = com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.ObligationStatus.OVERDUE)
            AND EXISTS(
            SELECT 1 FROM l.installments i
            WHERE i.status IN (
            com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.ObligationStatus.UNPAID,
            com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.ObligationStatus.CURRENT))
            AND l.status NOT IN (
            com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.LoanStatus.REJECTED,
            com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.LoanStatus.PRE_APPROVED)
            """)
    void updateStatusToActive();

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Loan l
            SET l.status = com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.LoanStatus.COMPLETED
            WHERE NOT EXISTS(
            SELECT 1 FROM l.installments i
            WHERE i.status != com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.ObligationStatus.PAID)
            AND l.status NOT IN (
            com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.LoanStatus.REJECTED,
            com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.LoanStatus.PRE_APPROVED)
            """)
    void updateStatusToCompleted();



}
