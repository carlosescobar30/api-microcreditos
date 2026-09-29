package com.carlosescobar30.apimicrocreditos.iam.security;

import com.carlosescobar30.apimicrocreditos.common.identity.UserDetailsImpl;
import com.carlosescobar30.apimicrocreditos.iam.configuration.JwtProperties;
import com.carlosescobar30.apimicrocreditos.iam.configuration.SecurityConfig;
import com.carlosescobar30.apimicrocreditos.iam.controller.AdminUserController;
import com.carlosescobar30.apimicrocreditos.iam.service.UserService;
import com.carlosescobar30.apimicrocreditos.operational.controller.AdminPaymentController;
import com.carlosescobar30.apimicrocreditos.operational.dto.TransactionValidationDTO;
import com.carlosescobar30.apimicrocreditos.operational.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AdminPaymentController.class, AdminUserController.class})
@Import({
        SecurityConfig.class,
        JwtAuthenticationEntryPoint.class,
        JwtAccessDeniedHandler.class,
        AdminEndpointsSecurityTest.JwtTestConfig.class
})
class AdminEndpointsSecurityTest {

    private static final Instant NOW = Instant.parse("2099-01-01T00:00:00Z");
    private static final String VALIDATION_BODY = """
            {"transactionCode":"TX-001","status":"APPROVED"}
            """;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private PaymentService paymentService;
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private UserDetailsService userDetailsService;

    @Nested
    @DisplayName("POST /admin/payment/validate")
    class PaymentValidationTests {

        @Test
        void forbiddenForARegularUserAndThePaymentIsNotTouched() throws Exception {

            mockMvc.perform(post("/admin/payment/validate")
                            .header("Authorization", bearerTokenFor("ROLE_USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALIDATION_BODY))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN_REQUEST"));

            verify(paymentService, never()).validateTransaction(any());
        }

        @Test
        void okForAnAdmin() throws Exception {

            mockMvc.perform(post("/admin/payment/validate")
                            .header("Authorization", bearerTokenFor("ROLE_ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALIDATION_BODY))
                    .andExpect(status().isOk());

            verify(paymentService).validateTransaction(any(TransactionValidationDTO.class));
        }

        @Test
        void badRequestWhenTheStatusIsMissing() throws Exception {

            mockMvc.perform(post("/admin/payment/validate")
                            .header("Authorization", bearerTokenFor("ROLE_ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"transactionCode":"TX-001"}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

            verify(paymentService, never()).validateTransaction(any());
        }
    }

    @Nested
    @DisplayName("PATCH /admin/users/{username}/identity-verification")
    class IdentityVerificationTests {

        @Test
        void forbiddenForARegularUser() throws Exception {

            mockMvc.perform(patch("/admin/users/carlos/identity-verification")
                            .header("Authorization", bearerTokenFor("ROLE_USER")))
                    .andExpect(status().isForbidden());

            verify(userService, never()).verifyIdentity(any());
        }

        @Test
        void noContentForAnAdmin() throws Exception {

            mockMvc.perform(patch("/admin/users/carlos/identity-verification")
                            .header("Authorization", bearerTokenFor("ROLE_ADMIN")))
                    .andExpect(status().isNoContent());

            verify(userService).verifyIdentity("carlos");
        }
    }

    private String bearerTokenFor(String role) {

        return "Bearer " + jwtService.generateJwt(new UserDetailsImpl(
                1L,
                "someone",
                null,
                List.of(new SimpleGrantedAuthority(role))));
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
