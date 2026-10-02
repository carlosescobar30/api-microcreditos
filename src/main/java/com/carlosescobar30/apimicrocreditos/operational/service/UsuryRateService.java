package com.carlosescobar30.apimicrocreditos.operational.service;

import com.carlosescobar30.apimicrocreditos.common.exception.bad_request.ActionNotPermitted;
import com.carlosescobar30.apimicrocreditos.common.exception.conflict.UsuryRateConflictException;
import com.carlosescobar30.apimicrocreditos.operational.domain.LoanProduct;
import com.carlosescobar30.apimicrocreditos.operational.domain.UsuryRate;
import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.CreditModality;
import com.carlosescobar30.apimicrocreditos.operational.domain.rate.PenaltyRateSchedule;
import com.carlosescobar30.apimicrocreditos.operational.dto.UsuryRateDTO;
import com.carlosescobar30.apimicrocreditos.operational.dto.UsuryRateRequestDTO;
import com.carlosescobar30.apimicrocreditos.operational.mappers.UsuryRateMapper;
import com.carlosescobar30.apimicrocreditos.operational.repository.UsuryRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsuryRateService {

    private final UsuryRateRepository repository;
    private final UsuryRateMapper mapper;

    @Transactional
    public UsuryRateDTO register(UsuryRateRequestDTO request){

        if (request.validFrom().getDayOfMonth() != 1){

            throw new ActionNotPermitted("Usury rates are certified per month: validFrom must be the first day of a month");

        }

        if (repository.existsByCreditModalityAndValidFrom(request.creditModality(), request.validFrom())){

            throw new UsuryRateConflictException();

        }

        UsuryRate usuryRate = repository.save(UsuryRate.builder()
                .creditModality(request.creditModality())
                .validFrom(request.validFrom())
                .rateEa(request.rateEa())
                .resolution(request.resolution())
                .build());

        return mapper.toUsuryRateDTO(usuryRate);

    }

    @Transactional(readOnly = true)
    public Page<UsuryRateDTO> getAll(CreditModality creditModality, Pageable pageable){

        Page<UsuryRate> usuryRates = creditModality == null
                ? repository.findAll(pageable)
                : repository.findAllByCreditModality(creditModality, pageable);

        return usuryRates.map(mapper::toUsuryRateDTO);

    }

    @Transactional(readOnly = true)
    public BigDecimal rateInForce(CreditModality creditModality, LocalDate day){

        return repository.findFirstByCreditModalityAndValidFromLessThanEqualOrderByValidFromDesc(creditModality, day)
                .map(UsuryRate::getRateEa)
                .orElseThrow(() -> new ActionNotPermitted(
                        "There is no usury rate registered for " + creditModality + " on " + day));

    }

    @Transactional(readOnly = true)
    public PenaltyRateSchedule penaltyScheduleFor(LoanProduct loanProduct){

        Map<LocalDate, BigDecimal> usuryRates = repository.findAllByCreditModality(loanProduct.getCreditModality())
                .stream()
                .collect(Collectors.toMap(UsuryRate::getValidFrom, UsuryRate::getRateEa));

        return new PenaltyRateSchedule(
                loanProduct.getCreditModality(),
                loanProduct.getPenaltyRateEa(),
                usuryRates);

    }

}
