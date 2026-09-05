package com.carlosescobar30.apimicrocreditos.iam.security;

import com.carlosescobar30.apimicrocreditos.iam.configuration.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final Instant NOW = Instant.parse("2099-01-01T00:00:00Z");
    private static final Duration EXPIRATION = Duration.ofMinutes(5);
    private static final String SECRET_KEY =
            "cG9ydGZvbGlvLW1pY3JvY3JlZGl0b3Mtand0LXNpZ25pbmcta2V5LWZvci10ZXN0cw==";

    private JwtService jwtService;
    private UserDetailsImpl userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(
                Clock.fixed(NOW, ZoneOffset.UTC),
                new JwtProperties(SECRET_KEY, EXPIRATION)
        );

        userDetails = new UserDetailsImpl(
                42L,
                "John",
                "irrelevant",
                List.of(
                        new SimpleGrantedAuthority("ROLE_USER"),
                        new SimpleGrantedAuthority("ROLE_ADMIN")
                )
        );
    }

    @Nested
    @DisplayName("Tests for the generateJwt method")
    class GenerateJwtTests{

        @Test
        void writesSubjectUserIdAndRolesUnderTheirClaimNames() {
            String token = jwtService.generateJwt(userDetails);

            Claims claims = jwtService.extractAllClaims(token);

            assertThat(claims.getSubject()).isEqualTo("John");
            assertThat(claims.get("uId", Long.class)).isEqualTo(42L);
            assertThat(claims.get("roles", List.class))
                    .containsExactly("ROLE_USER", "ROLE_ADMIN");
        }

        @Test
        void derivesIssuedAtAndExpirationFromTheInjectedClock() {
            String token = jwtService.generateJwt(userDetails);

            Claims claims = jwtService.extractAllClaims(token);

            assertThat(claims.getIssuedAt()).isEqualTo(Date.from(NOW));
            assertThat(claims.getExpiration()).isEqualTo(Date.from(NOW.plus(EXPIRATION)));
        }

    }

    @Nested
    @DisplayName("Tests for the extractAllClaims method")
    class ExtractAllClaimsTests{

        @Test
        void throwsWhenThePayloadWasSwapped() {
            String victimToken = jwtService.generateJwt(userDetails);

            String intruderToken = jwtService.generateJwt(new UserDetailsImpl(
                    99L,
                    "intruder",
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
            ));

            String[] victimParts = victimToken.split("\\.");
            String[] intruderParts = intruderToken.split("\\.");
            String forgedToken = victimParts[0] + "." + intruderParts[1] + "." + victimParts[2];

            assertThatThrownBy(() -> jwtService.extractAllClaims(forgedToken))
                    .isInstanceOf(SignatureException.class);
        }

        @Test
        void throwsWhenReadAfterTheExpirationInstant() {
            String token = jwtService.generateJwt(userDetails);

            JwtService jwtServiceInTheFuture = new JwtService(
                    Clock.fixed(NOW.plus(EXPIRATION).plusSeconds(1), ZoneOffset.UTC),
                    new JwtProperties(SECRET_KEY, EXPIRATION)
            );

            assertThatThrownBy(() -> jwtServiceInTheFuture.extractAllClaims(token))
                    .isInstanceOf(ExpiredJwtException.class);
        }

    }

}
