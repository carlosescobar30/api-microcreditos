package com.carlosescobar30.apimicrocreditos.operational.controller;

import com.carlosescobar30.apimicrocreditos.common.identity.UserDetailsImpl;
import com.carlosescobar30.apimicrocreditos.iam.configuration.JwtProperties;
import com.carlosescobar30.apimicrocreditos.iam.configuration.SecurityConfig;
import com.carlosescobar30.apimicrocreditos.iam.security.JwtAccessDeniedHandler;
import com.carlosescobar30.apimicrocreditos.iam.security.JwtAuthenticationEntryPoint;
import com.carlosescobar30.apimicrocreditos.iam.security.JwtService;
import com.carlosescobar30.apimicrocreditos.operational.dto.PayRequestDTO;
import com.carlosescobar30.apimicrocreditos.operational.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationEntryPoint.class,
        JwtAccessDeniedHandler.class,
        PaymentControllerTest.JwtTestConfig.class
})
class PaymentControllerTest {

    private static final Instant NOW = Instant.parse("2099-01-01T00:00:00Z");
    private static final UUID LOAN_REFERENCE = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private PaymentService paymentService;
    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void aValidPaymentIsCreated() throws Exception {

        pay("""
                {"transactionCode":"TX-001","financialMethod":"NEQUI","amount":150000.50,"description":"October installment"}
                """)
                .andExpect(status().isCreated());

        verify(paymentService).pay(any(UserDetailsImpl.class), eq(LOAN_REFERENCE), any(PayRequestDTO.class));
    }

    @Test
    void aMissingAmountIsABadRequest() throws Exception {

        pay("""
                {"transactionCode":"TX-001","financialMethod":"NEQUI","description":"October installment"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.amount").exists());

        verify(paymentService, never()).pay(any(), any(), any());
    }

    @Test
    void anAmountThatIsNotPositiveIsABadRequest() throws Exception {

        pay("""
                {"transactionCode":"TX-001","financialMethod":"NEQUI","amount":0,"description":"October installment"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.amount").exists());

        verify(paymentService, never()).pay(any(), any(), any());
    }

    @Test
    void anAmountWithMoreThanFourDecimalsIsABadRequest() throws Exception {

        pay("""
                {"transactionCode":"TX-001","financialMethod":"NEQUI","amount":10.12345,"description":"October installment"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.amount").exists());

        verify(paymentService, never()).pay(any(), any(), any());
    }

    @Test
    void aBlankTransactionCodeOrDescriptionIsABadRequest() throws Exception {

        pay("""
                {"transactionCode":" ","financialMethod":"NEQUI","amount":150000,"description":""}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.transactionCode").exists())
                .andExpect(jsonPath("$.fields.description").exists());

        verify(paymentService, never()).pay(any(), any(), any());
    }

    @Test
    void anUnknownFinancialMethodIsAMalformedRequest() throws Exception {

        pay("""
                {"transactionCode":"TX-001","financialMethod":"CASH","amount":150000,"description":"October installment"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));

        verify(paymentService, never()).pay(any(), any(), any());
    }

    @Test
    void aBodyThatIsNotJsonIsAMalformedRequest() throws Exception {

        pay("{not json")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));

        verify(paymentService, never()).pay(any(), any(), any());
    }

    private ResultActions pay(String body) throws Exception {

        return mockMvc.perform(post("/payment/" + LOAN_REFERENCE)
                .header("Authorization", "Bearer " + jwtService.generateJwt(new UserDetailsImpl(
                        1L, "someone", null, List.of())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class JwtTestConfig {

        @Bean
        Clock clock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }

        @Bean
        JwtService jwtService(Clock clock, JwtProperties jwtProperties) {
            return new JwtService(clock, jwtProperties);
        }
    }
}
