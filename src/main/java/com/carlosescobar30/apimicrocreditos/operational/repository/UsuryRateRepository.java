package com.carlosescobar30.apimicrocreditos.operational.repository;

import com.carlosescobar30.apimicrocreditos.operational.domain.UsuryRate;
import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.CreditModality;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UsuryRateRepository extends JpaRepository<UsuryRate, Long> {

    List<UsuryRate> findAllByCreditModality(CreditModality creditModality);

    Page<UsuryRate> findAllByCreditModality(CreditModality creditModality, Pageable pageable);

    Optional<UsuryRate> findFirstByCreditModalityAndValidFromLessThanEqualOrderByValidFromDesc(
            CreditModality creditModality, LocalDate day);

    boolean existsByCreditModalityAndValidFrom(CreditModality creditModality, LocalDate validFrom);

}
