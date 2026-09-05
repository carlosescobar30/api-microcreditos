package com.carlosescobar30.apimicrocreditos.iam.controller;

import com.carlosescobar30.apimicrocreditos.common.exception.conflict.EmailConflictException;
import com.carlosescobar30.apimicrocreditos.common.exception.unauthorized.RefreshTokenInvalidException;
import com.carlosescobar30.apimicrocreditos.iam.configuration.JwtProperties;
import com.carlosescobar30.apimicrocreditos.iam.configuration.SecurityConfig;
import com.carlosescobar30.apimicrocreditos.iam.dto.LoginRequestDTO;
import com.carlosescobar30.apimicrocreditos.iam.dto.RefreshRequestDTO;
import com.carlosescobar30.apimicrocreditos.iam.dto.RegisterRequestDTO;
import com.carlosescobar30.apimicrocreditos.iam.dto.TokenResponseDTO;
import com.carlosescobar30.apimicrocreditos.iam.security.JwtAccessDeniedHandler;
import com.carlosescobar30.apimicrocreditos.iam.security.JwtAuthenticationEntryPoint;
import com.carlosescobar30.apimicrocreditos.iam.security.JwtService;
import com.carlosescobar30.apimicrocreditos.iam.security.UserDetailsImpl;
import com.carlosescobar30.apimicrocreditos.iam.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
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
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationEntryPoint.class,
        JwtAccessDeniedHandler.class,
        AuthControllerTest.JwtTestConfig.class
})
class AuthControllerTest {

    private static final Instant NOW = Instant.parse("2099-01-01T00:00:00Z");
    private static final Long USER_ID = 42L;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private AuthService authService;
    @MockitoBean
    private UserDetailsService userDetailsService;

    @Nested
    @DisplayName("POST /auth/login")
    class LoginTests {

        @Test
        void theTokenPairIsReturnedAndTheResponseIsNeverCached() throws Exception {

            given(authService.login(any(LoginRequestDTO.class))).willReturn(
                    TokenResponseDTO.builder()
                            .accessToken("access-token")
                            .tokenType("Bearer")
                            .expiresIn(300L)
                            .refreshToken("refresh-token")
                            .build());

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"carlos","password":"plain-password"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Cache-Control", "no-store"))
                    .andExpect(jsonPath("$.accessToken").value("access-token"))
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.expiresIn").value(300))
                    .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
        }

        @Test
        void theOffendingFieldIsReportedWhenTheUsernameHasSpecialCharacters() throws Exception {

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"carlos@!","password":"plain-password"}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.fields.username").exists())
                    .andExpect(jsonPath("$.traceId").exists());
        }
    }

    @Nested
    @DisplayName("POST /auth/register")
    class RegisterTests {

        @Test
        void anEmptyCreatedResponseIsReturnedWhenTheRegistrationSucceeds() throws Exception {

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validRegistrationBody()))
                    .andExpect(status().isCreated())
                    .andExpect(content().string(""));
        }

        @Test
        void aConflictWithItsErrorCodeIsReturnedWhenTheEmailIsAlreadyTaken() throws Exception {

            doThrow(new EmailConflictException())
                    .when(authService).register(any(RegisterRequestDTO.class));

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validRegistrationBody()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));
        }
    }

    @Nested
    @DisplayName("POST /auth/refresh")
    class RefreshTests {

        @Test
        void theRefreshTokenKeyIsAbsentWhenTheRotationHappenedInsideTheGracePeriod() throws Exception {

            given(authService.refresh(any(RefreshRequestDTO.class))).willReturn(
                    TokenResponseDTO.builder()
                            .accessToken("access-token")
                            .tokenType("Bearer")
                            .expiresIn(300L)
                            .refreshToken(null)
                            .build());

            mockMvc.perform(post("/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"refreshToken":"some-raw-token"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("access-token"))
                    .andExpect(jsonPath("$.refreshToken").doesNotExist());
        }

        @Test
        void anUnauthorizedWithItsErrorCodeIsReturnedWhenTheRefreshTokenIsInvalid() throws Exception {

            given(authService.refresh(any(RefreshRequestDTO.class)))
                    .willThrow(new RefreshTokenInvalidException());

            mockMvc.perform(post("/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"refreshToken":"some-raw-token"}
                                    """))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_INVALID"));
        }
    }

    @Nested
    @DisplayName("POST /auth/logout")
    class LogoutTests {

        @Test
        void anEmptyNoContentResponseIsReturnedWhenTheCallerIsAuthenticated() throws Exception {

            mockMvc.perform(post("/auth/logout")
                            .header("Authorization", bearerTokenFor("carlos", "ROLE_USER")))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));
        }
    }

    private String bearerTokenFor(String username, String... roles) {

        UserDetailsImpl userDetails = new UserDetailsImpl(
                USER_ID,
                username,
                null,
                Arrays.stream(roles).map(SimpleGrantedAuthority::new).toList());

        return "Bearer " + jwtService.generateJwt(userDetails);
    }

    private String validRegistrationBody() {

        return """
                {
                  "name":"Carlos",
                  "lastName":"Escobar",
                  "username":"carlos",
                  "password":"plain-password",
                  "email":"carlos@mail.com",
                  "birthDate":"1990-01-01"
                }
                """;
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
