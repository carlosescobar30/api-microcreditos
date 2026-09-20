package com.carlosescobar30.apimicrocreditos.operational.domain;

import com.carlosescobar30.apimicrocreditos.common.domain.EntityBaseClass;
import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.LoanStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;


@Entity
@Table(name = "loans", schema = "operational")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Loan extends EntityBaseClass {

    @Column(name = "user_id", nullable = false)
    private UUID userReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_product_id", nullable = false)
    private LoanProduct loanProduct;

    @Column(precision = 19, scale = 4)
    private BigDecimal principalReceivable;

    private LocalDate startDate;

    private LocalDate endDate;

    private Integer payday;

    @OneToMany(mappedBy = "loan")
    private List<LoanInstallment> installments;

    @Column(nullable = false)
    @Enumerated(value = EnumType.STRING)
    private LoanStatus status;

}
