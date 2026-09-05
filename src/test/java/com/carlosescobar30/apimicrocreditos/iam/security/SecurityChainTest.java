package com.carlosescobar30.apimicrocreditos.iam.security;

import com.carlosescobar30.apimicrocreditos.iam.configuration.JwtProperties;
import com.carlosescobar30.apimicrocreditos.iam.configuration.SecurityConfig;
import com.carlosescobar30.apimicrocreditos.iam.controller.AuthController;
import com.carlosescobar30.apimicrocreditos.iam.service.AuthService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationEntryPoint.class,
        JwtAccessDeniedHandler.class,
        SecurityChainTest.SecurityChainTestConfig.class
})
class SecurityChainTest {

    private static final Instant NOW = Instant.parse("2099-01-01T00:00:00Z");
    private static final Long USER_ID = 42L;
    private static final String FOREIGN_SECRET_KEY =
            "b3RyYS1jbGF2ZS1kaXN0aW50YS1wYXJhLXRlc3RzLWRlLWZpcm1hLWludmFsaWRhISE=";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private JwtProperties jwtProperties;

    @MockitoBean
    private AuthService authService;
    @MockitoBean
    private UserDetailsService userDetailsService;

    @Nested
    @DisplayName("Endpoints that do not require a token")
    class PublicEndpointTests {

        @Test
        void theRequestIsAcceptedWithoutAnyAuthorizationHeader() throws Exception {

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"carlos","password":"plain-password"}
                                    """))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Endpoints that require authentication")
    class AuthenticationTests {

        @Test
        void unauthorizedWhenNoAuthorizationHeaderIsSent() throws Exception {

            mockMvc.perform(post("/auth/logout"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        }

        @Test
        void unauthorizedWhenTheHeaderDoesNotUseTheBearerScheme() throws Exception {

            mockMvc.perform(post("/auth/logout")
                            .header("Authorization", "Basic Y2FybG9zOnBhc3N3b3Jk"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        }

        @Test
        void unauthorizedWithItsOwnErrorCodeWhenTheTokenHasExpired() throws Exception {

            JwtService issuerInThePast = new JwtService(
                    Clock.fixed(NOW.minus(jwtProperties.expiration()).minus(Duration.ofMinutes(1)),
                            ZoneOffset.UTC),
                    jwtProperties);

            mockMvc.perform(post("/auth/logout")
                            .header("Authorization", "Bearer "
                                    + issuerInThePast.generateJwt(userDetails("carlos", "ROLE_USER"))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("TOKEN_EXPIRED"));
        }

        @Test
        void unauthorizedWhenTheTokenWasSignedWithAnotherKey() throws Exception {

            JwtService foreignIssuer = new JwtService(
                    Clock.fixed(NOW, ZoneOffset.UTC),
                    new JwtProperties(FOREIGN_SECRET_KEY, jwtProperties.expiration()));

            mockMvc.perform(post("/auth/logout")
                            .header("Authorization", "Bearer "
                                    + foreignIssuer.generateJwt(userDetails("carlos", "ROLE_USER"))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        }

        @Test
        void noContentWhenTheTokenIsValid() throws Exception {

            mockMvc.perform(post("/auth/logout")
                            .header("Authorization", bearerTokenFor("carlos", "ROLE_USER")))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("Endpoints restricted by role")
    class AuthorizationTests {

        @Test
        void forbiddenWhenTheUserDoesNotHaveTheAdminRole() throws Exception {

            mockMvc.perform(get("/admin/ping")
                            .header("Authorization", bearerTokenFor("carlos", "ROLE_USER")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN_REQUEST"));
        }

        @Test
        void okWhenTheUserHasTheAdminRole() throws Exception {

            mockMvc.perform(get("/admin/ping")
                            .header("Authorization", bearerTokenFor("root", "ROLE_ADMIN")))
                    .andExpect(status().isOk());
        }
    }

    private String bearerTokenFor(String username, String... roles) {
        return "Bearer " + jwtService.generateJwt(userDetails(username, roles));
    }

    private UserDetailsImpl userDetails(String username, String... roles) {

        return new UserDetailsImpl(
                USER_ID,
                username,
                null,
                Arrays.stream(roles).map(SimpleGrantedAuthority::new).toList());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class SecurityChainTestConfig {

        @Bean
        Clock clock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }

        @Bean
        JwtService jwtService(Clock clock, JwtProperties jwtProperties) {
            return new JwtService(clock, jwtProperties);
        }

        @Bean
        AdminProbeController adminProbeController() {
            return new AdminProbeController();
        }
    }

    @RestController
    static class AdminProbeController {

        @GetMapping("/admin/ping")
        String ping() {
            return "pong";
        }
    }
}
