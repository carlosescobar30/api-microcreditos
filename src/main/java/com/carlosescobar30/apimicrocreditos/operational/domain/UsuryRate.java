package com.carlosescobar30.apimicrocreditos.operational.domain;

import com.carlosescobar30.apimicrocreditos.common.domain.EntityBaseClass;
import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.CreditModality;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "usury_rates", schema = "operational")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UsuryRate extends EntityBaseClass {

    @Column(nullable = false)
    @Enumerated(value = EnumType.STRING)
    private CreditModality creditModality;

    @Column(nullable = false)
    private LocalDate validFrom;

    @Column(precision = 5, scale = 4, nullable = false)
    private BigDecimal rateEa;

    private String resolution;

}
