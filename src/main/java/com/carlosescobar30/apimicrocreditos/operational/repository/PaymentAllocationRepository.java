package com.carlosescobar30.apimicrocreditos.operational.repository;

import com.carlosescobar30.apimicrocreditos.operational.domain.Payment;
import com.carlosescobar30.apimicrocreditos.operational.domain.PaymentAllocation;
import com.carlosescobar30.apimicrocreditos.operational.dto.AllocationDetailsDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PaymentAllocationRepository extends JpaRepository<PaymentAllocation, Long> {

    @Query("""
            SELECT new com.carlosescobar30.apimicrocreditos.operational.dto.AllocationDetailsDTO(
            pa.loanInstallment.publicId,
            pa.amount,
            pa.appliedTo,
            pa.createdAt
            )
            FROM PaymentAllocation pa
            WHERE pa.payment = :payment
            """)
    List<AllocationDetailsDTO> findAllByPayment (@Param("payment")Payment payment);

}
