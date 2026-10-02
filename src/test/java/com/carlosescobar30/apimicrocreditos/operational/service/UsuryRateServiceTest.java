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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuryRateServiceTest {

    private static final CreditModality MODALITY = CreditModality.CONSUMER_AND_ORDINARY;

    @Mock
    private UsuryRateRepository repository;
    @Mock
    private UsuryRateMapper mapper;

    private UsuryRateService usuryRateService;

    @BeforeEach
    void setUp() {

        this.usuryRateService = new UsuryRateService(repository, mapper);
    }

    @Nested
    @DisplayName("Tests for the register method")
    class RegisterTests {

        @Test
        void aRateIsRegisteredForTheMonthItIsCertifiedFor() {

            UsuryRateDTO response = new UsuryRateDTO(
                    UUID.randomUUID(), MODALITY, LocalDate.of(2026, 11, 1), new BigDecimal("0.2800"), "Res. 1400");
            when(repository.existsByCreditModalityAndValidFrom(MODALITY, LocalDate.of(2026, 11, 1))).thenReturn(false);
            when(repository.save(any(UsuryRate.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(mapper.toUsuryRateDTO(any(UsuryRate.class))).thenReturn(response);

            UsuryRateDTO registered = usuryRateService.register(request(LocalDate.of(2026, 11, 1)));

            ArgumentCaptor<UsuryRate> saved = ArgumentCaptor.forClass(UsuryRate.class);
            verify(repository).save(saved.capture());
            assertThat(saved.getValue().getCreditModality()).isEqualTo(MODALITY);
            assertThat(saved.getValue().getValidFrom()).isEqualTo(LocalDate.of(2026, 11, 1));
            assertThat(saved.getValue().getRateEa()).isEqualByComparingTo("0.2800");
            assertThat(registered).isEqualTo(response);
        }

        @Test
        void aRateThatDoesNotStartOnTheFirstDayOfAMonthIsRejected() {

            assertThatThrownBy(() -> usuryRateService.register(request(LocalDate.of(2026, 11, 15))))
                    .isInstanceOf(ActionNotPermitted.class);

            verifyNoInteractions(repository);
        }

        @Test
        void aSecondRateForTheSameModalityAndMonthIsAConflict() {

            when(repository.existsByCreditModalityAndValidFrom(MODALITY, LocalDate.of(2026, 11, 1))).thenReturn(true);

            assertThatThrownBy(() -> usuryRateService.register(request(LocalDate.of(2026, 11, 1))))
                    .isInstanceOf(UsuryRateConflictException.class);

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Tests for the queries")
    class QueryTests {

        @Test
        void theRateInForceIsTheLatestOneRegisteredUpToThatDay() {

            when(repository.findFirstByCreditModalityAndValidFromLessThanEqualOrderByValidFromDesc(
                    MODALITY, LocalDate.of(2026, 10, 15)))
                    .thenReturn(Optional.of(rate(LocalDate.of(2026, 10, 1), "0.2859")));

            assertThat(usuryRateService.rateInForce(MODALITY, LocalDate.of(2026, 10, 15)))
                    .isEqualByComparingTo("0.2859");
        }

        @Test
        void aDayWithoutAnyRateRegisteredIsRejected() {

            when(repository.findFirstByCreditModalityAndValidFromLessThanEqualOrderByValidFromDesc(
                    MODALITY, LocalDate.of(2025, 12, 31)))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> usuryRateService.rateInForce(MODALITY, LocalDate.of(2025, 12, 31)))
                    .isInstanceOf(ActionNotPermitted.class);
        }

        @Test
        void thePenaltyScheduleOfAProductCapsItsRateWithEveryRegisteredMonth() {

            LoanProduct product = LoanProduct.builder()
                    .penaltyRateEa(new BigDecimal("0.4000"))
                    .creditModality(MODALITY)
                    .build();
            when(repository.findAllByCreditModality(MODALITY)).thenReturn(List.of(
                    rate(LocalDate.of(2026, 9, 1), "0.2924"),
                    rate(LocalDate.of(2026, 10, 1), "0.2859")));

            PenaltyRateSchedule schedule = usuryRateService.penaltyScheduleFor(product);

            assertThat(schedule.effectiveRateEaOn(LocalDate.of(2026, 9, 15))).isEqualByComparingTo("0.2924");
            assertThat(schedule.effectiveRateEaOn(LocalDate.of(2026, 10, 15))).isEqualByComparingTo("0.2859");
        }

        @Test
        void theSchedulesOfSeveralProductsAreBuiltWithASingleQuery() {

            LoanProduct consumer = product(1L, CreditModality.CONSUMER_AND_ORDINARY, "0.4000");
            LoanProduct popular = product(2L, CreditModality.POPULAR_PRODUCTIVE_URBAN, "0.9500");
            when(repository.findAllByCreditModalityIn(
                    Set.of(CreditModality.CONSUMER_AND_ORDINARY, CreditModality.POPULAR_PRODUCTIVE_URBAN)))
                    .thenReturn(List.of(
                            rate(LocalDate.of(2026, 10, 1), "0.2859"),
                            UsuryRate.builder()
                                    .creditModality(CreditModality.POPULAR_PRODUCTIVE_URBAN)
                                    .validFrom(LocalDate.of(2026, 10, 1))
                                    .rateEa(new BigDecimal("0.8760"))
                                    .build()));

            Map<Long, PenaltyRateSchedule> schedules = usuryRateService.penaltySchedulesFor(List.of(consumer, popular));

            assertThat(schedules.get(1L).effectiveRateEaOn(LocalDate.of(2026, 10, 15))).isEqualByComparingTo("0.2859");
            assertThat(schedules.get(2L).effectiveRateEaOn(LocalDate.of(2026, 10, 15))).isEqualByComparingTo("0.8760");
            verify(repository, never()).findAllByCreditModality(any(CreditModality.class));
        }

        @Test
        void noProductsMeansNoQuery() {

            assertThat(usuryRateService.penaltySchedulesFor(List.of())).isEmpty();

            verifyNoInteractions(repository);
        }

        @Test
        void listingWithoutAModalityReturnsEveryRate() {

            Pageable pageable = PageRequest.of(0, 20);
            when(repository.findAll(pageable)).thenReturn(Page.empty(pageable));

            usuryRateService.getAll(null, pageable);

            verify(repository).findAll(pageable);
            verify(repository, never()).findAllByCreditModality(any(), any(Pageable.class));
        }
    }

    private LoanProduct product(Long id, CreditModality creditModality, String penaltyRateEa) {

        LoanProduct product = LoanProduct.builder()
                .penaltyRateEa(new BigDecimal(penaltyRateEa))
                .creditModality(creditModality)
                .build();
        product.setId(id);
        return product;
    }

    private UsuryRateRequestDTO request(LocalDate validFrom) {

        return new UsuryRateRequestDTO(MODALITY, validFrom, new BigDecimal("0.2800"), "Res. 1400");
    }

    private UsuryRate rate(LocalDate validFrom, String rateEa) {

        return UsuryRate.builder()
                .creditModality(MODALITY)
                .validFrom(validFrom)
                .rateEa(new BigDecimal(rateEa))
                .build();
    }
}
