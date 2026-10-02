package com.carlosescobar30.apimicrocreditos.operational.service;

import com.carlosescobar30.apimicrocreditos.common.exception.bad_request.ActionNotPermitted;
import com.carlosescobar30.apimicrocreditos.common.exception.bad_request.RateAboveUsuryException;
import com.carlosescobar30.apimicrocreditos.common.exception.conflict.LoanProductConflictException;
import com.carlosescobar30.apimicrocreditos.iam.adapter.UserAdapter;
import com.carlosescobar30.apimicrocreditos.operational.domain.LoanProduct;
import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.CreditModality;
import com.carlosescobar30.apimicrocreditos.operational.dto.AvailableLoanProductDTO;
import com.carlosescobar30.apimicrocreditos.operational.dto.LoanProductRequestDTO;
import com.carlosescobar30.apimicrocreditos.operational.mappers.LoanProductMapper;
import com.carlosescobar30.apimicrocreditos.operational.repository.LoanProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanProductServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 15);
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-10-15T17:00:00Z"), ZoneId.of("America/Bogota"));
    private static final BigDecimal USURY_RATE = new BigDecimal("0.2859");

    @Mock
    private LoanProductRepository repository;
    @Mock
    private LoanProductMapper mapper;
    @Mock
    private UserAdapter userAdapter;
    @Mock
    private UsuryRateService usuryRateService;

    private LoanProductService loanProductService;

    @BeforeEach
    void setUp() {

        this.loanProductService = new LoanProductService(
                repository,
                mapper,
                userAdapter,
                usuryRateService,
                CLOCK);
    }

    @Nested
    @DisplayName("Tests for the create method")
    class CreateTests {

        @Test
        void aProductWithRatesUpToTheUsuryRateIsCreatedAsMonthly() {

            AvailableLoanProductDTO response = new AvailableLoanProductDTO(
                    UUID.randomUUID(), "Microcredit", new BigDecimal("1000000.0000"),
                    USURY_RATE, USURY_RATE, CreditModality.CONSUMER_AND_ORDINARY, 12);
            givenUsuryRateInForce();
            when(repository.existsByName("Microcredit")).thenReturn(false);
            when(repository.save(any(LoanProduct.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(mapper.toAvailableLoanProductsDTO(any(LoanProduct.class))).thenReturn(response);

            AvailableLoanProductDTO created = loanProductService.create(request("0.2859", "0.2859"));

            ArgumentCaptor<LoanProduct> saved = ArgumentCaptor.forClass(LoanProduct.class);
            verify(repository).save(saved.capture());
            assertThat(saved.getValue().getPeriodicity()).isEqualTo(12);
            assertThat(saved.getValue().getPenaltyRateEa()).isEqualByComparingTo("0.2859");
            assertThat(saved.getValue().getCreditModality()).isEqualTo(CreditModality.CONSUMER_AND_ORDINARY);
            assertThat(created).isEqualTo(response);
        }

        @Test
        void anInterestRateAboveTheUsuryRateIsRejected() {

            givenUsuryRateInForce();
            when(repository.existsByName("Microcredit")).thenReturn(false);

            assertThatThrownBy(() -> loanProductService.create(request("0.3000", "0.2000")))
                    .isInstanceOf(RateAboveUsuryException.class);

            verify(repository, never()).save(any());
        }

        @Test
        void aPenaltyRateAboveTheUsuryRateIsRejected() {

            givenUsuryRateInForce();
            when(repository.existsByName("Microcredit")).thenReturn(false);

            assertThatThrownBy(() -> loanProductService.create(request("0.2000", "0.3000")))
                    .isInstanceOf(RateAboveUsuryException.class);

            verify(repository, never()).save(any());
        }

        @Test
        void aRepeatedNameIsAConflict() {

            when(repository.existsByName("Microcredit")).thenReturn(true);

            assertThatThrownBy(() -> loanProductService.create(request("0.2000", "0.2000")))
                    .isInstanceOf(LoanProductConflictException.class);

            verifyNoInteractions(usuryRateService);
            verify(repository, never()).save(any());
        }

        @Test
        void aModalityWithoutAUsuryRateRegisteredCannotBeUsed() {

            when(repository.existsByName("Microcredit")).thenReturn(false);
            when(usuryRateService.rateInForce(CreditModality.CONSUMER_AND_ORDINARY, TODAY))
                    .thenThrow(new ActionNotPermitted("There is no usury rate registered"));

            assertThatThrownBy(() -> loanProductService.create(request("0.2000", "0.2000")))
                    .isInstanceOf(ActionNotPermitted.class);

            verify(repository, never()).save(any());
        }
    }

    private void givenUsuryRateInForce() {

        when(usuryRateService.rateInForce(CreditModality.CONSUMER_AND_ORDINARY, TODAY)).thenReturn(USURY_RATE);
    }

    private LoanProductRequestDTO request(String interestRate, String penaltyRateEa) {

        return new LoanProductRequestDTO(
                "Microcredit",
                new BigDecimal("1000000.0000"),
                new BigDecimal(interestRate),
                new BigDecimal(penaltyRateEa),
                CreditModality.CONSUMER_AND_ORDINARY,
                12,
                500);
    }
}
