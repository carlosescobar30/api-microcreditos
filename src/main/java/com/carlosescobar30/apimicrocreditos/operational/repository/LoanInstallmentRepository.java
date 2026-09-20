package com.carlosescobar30.apimicrocreditos.operational.repository;

import com.carlosescobar30.apimicrocreditos.operational.domain.Loan;
import com.carlosescobar30.apimicrocreditos.operational.domain.LoanInstallment;
import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.ObligationStatus;
import com.carlosescobar30.apimicrocreditos.operational.dto.InstallmentsInfoDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoanInstallmentRepository extends JpaRepository<LoanInstallment, Long> {

    @Query("SELECT new com.carlosescobar30.apimicrocreditos.operational.dto.InstallmentsInfoDTO(" +
            "li.publicId," +
            "li.principalAmount," +
            "li.interestAmount," +
            "li.totalAmount," +
            "li.paymentDate" +
            ") " +
            "FROM LoanInstallment li " +
            "WHERE li.loan.id = :loanId " +
            "ORDER BY li.installmentNumber")
    Page<InstallmentsInfoDTO> findByLoanId(@Param("loanId") Long loanId, Pageable pageable);

    @Query("SELECT new com.carlosescobar30.apimicrocreditos.operational.dto.InstallmentsInfoDTO(" +
            "li.publicId," +
            "li.principalAmount," +
            "li.interestAmount," +
            "li.totalAmount," +
            "li.paymentDate" +
            ") " +
            "FROM LoanInstallment li " +
            "WHERE li.publicId = :installmentReference ")
    Optional<InstallmentsInfoDTO> findBySInstallmentReference(@Param("installmentReference") UUID installmentReference);

    @Query("SELECT new com.carlosescobar30.apimicrocreditos.operational.dto.InstallmentsInfoDTO(" +
            "li.publicId," +
            "li.principalAmount," +
            "li.interestAmount," +
            "li.totalAmount," +
            "li.paymentDate" +
            ") " +
            "FROM LoanInstallment li " +
            "WHERE li.loan.publicId = :loanReference " +
            "AND li.status = :status " +
            "AND li.loan.userReference = :userReference")
    Optional<InstallmentsInfoDTO> findByStatusAndLoanReference(@Param("status") ObligationStatus status,
                                                               @Param("loanReference") UUID loanReference,
                                                               @Param("userReference") UUID userReference);

    @Query("SELECT new com.carlosescobar30.apimicrocreditos.operational.dto.InstallmentsInfoDTO(" +
            "li.publicId," +
            "li.principalAmount," +
            "li.interestAmount," +
            "li.totalAmount," +
            "li.paymentDate" +
            ") " +
            "FROM LoanInstallment li " +
            "WHERE li.loan.publicId = :loanReference " +
            "AND li.loan.userReference = :userReference")
    Page<InstallmentsInfoDTO> findAllByLoanReference(@Param("loanReference") UUID loanReference,
                                                     @Param("userReference") UUID userReference,
                                                     Pageable pageable);

    @Query("SELECT new com.carlosescobar30.apimicrocreditos.operational.dto.InstallmentsInfoDTO(" +
            "li.publicId," +
            "li.principalAmount," +
            "li.interestAmount," +
            "li.totalAmount," +
            "li.paymentDate" +
            ") " +
            "FROM LoanInstallment li " +
            "WHERE li.loan.publicId = :loanReference " +
            "AND li.status = :status " +
            "AND li.loan.userReference = :userReference")
    Page<InstallmentsInfoDTO> findAllByStatusAndLoanReference(@Param("status") ObligationStatus status,
                                                              @Param("loanReference") UUID loanReference,
                                                              @Param("userReference") UUID userReference,
                                                              Pageable pageable);

    Optional<LoanInstallment> findByPublicId(UUID publicId);
    boolean existsByLoan_PublicIdAndLoan_UserReference(UUID loanReference, UUID userReference);
    long countByLoan_IdAndStatus(Long loanId, ObligationStatus status);

    List<LoanInstallment> findAllByLoanAndStatusNotOrderByInstallmentNumberAsc(Loan loan,
                                                                               ObligationStatus status);


    //CRON JOBS

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE LoanInstallment li
            SET
            li.status = com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.ObligationStatus.OVERDUE,
            li.lastUpdate = :now
            WHERE li.paymentDate < :today
            AND li.status IN (
            com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.ObligationStatus.CURRENT,
            com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.ObligationStatus.UNPAID)
            """)
    int changeStatusToOverdue (@Param("now") Instant now, @Param("today")LocalDate today);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE LoanInstallment li
            SET
            li.status = com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.ObligationStatus.CURRENT,
            li.lastUpdate = :now
            WHERE li.paymentDate < :todayPlusOneMonth
            AND li.status = com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.ObligationStatus.UNPAID
            """)
    int changeStatusToCurrent (@Param("now") Instant now, @Param("todayPlusOneMonth")LocalDate todayPlusOneMonth);


    List<LoanInstallment> findAllByStatus( ObligationStatus status);
}
