package com.carlosescobar30.apimicrocreditos.operational.domain;

import com.carlosescobar30.apimicrocreditos.common.domain.EntityBaseClass;
import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.CreditModality;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "loan_products", schema = "operational")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class LoanProduct extends EntityBaseClass {

    @Column(nullable = false)
    private String name;

    @Column(precision = 19, scale = 4, nullable = false)
    private BigDecimal totalPrincipal;

    @Column(precision = 5, scale = 4, nullable = false)
    private BigDecimal interestRate;

    @Column(name = "penalty_rate_ea", precision = 5, scale = 4, nullable = false)
    private BigDecimal penaltyRateEa;

    @Column(nullable = false)
    @Enumerated(value = EnumType.STRING)
    private CreditModality creditModality;

    @Column(nullable = false)
    private Integer installments;

    @Column(nullable = false)
    private Integer periodicity;

    @Column(nullable = false)
    private Integer minimumUserScore;

}
