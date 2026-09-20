package com.carlosescobar30.apimicrocreditos.operational.domain;

import com.carlosescobar30.apimicrocreditos.common.domain.EntityBaseClass;
import jakarta.persistence.Column;
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

    @Column(precision = 5, scale = 4, nullable = false)
    private BigDecimal dailyPenaltyRate;

    @Column(nullable = false)
    private Integer installments;

    @Column(nullable = false)
    private Integer periodicity;

    @Column(nullable = false)
    private Integer minimumUserScore;

}
