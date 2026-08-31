package com.carlosescobar30.apimicrocreditos.iam.service;

import com.carlosescobar30.apimicrocreditos.iam.configuration.RefreshTokenProperties;
import com.carlosescobar30.apimicrocreditos.iam.domain.User;
import com.carlosescobar30.apimicrocreditos.iam.internals.RefreshHashToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.*;

import static org.assertj.core.api.Assertions.assertThat;


class RefreshTokenEngineServiceTest {


    private static final Instant NOW = Instant.parse("2099-01-01T00:00:00Z");
    private static final Duration EXPIRATION = Duration.ofDays(7);
    private static final Duration GRACE_PERIOD = Duration.ofSeconds(5);
    private RefreshTokenEngineService refreshTokenEngineService;
    private User user;

    @BeforeEach
    void setUp(){

        this.refreshTokenEngineService = new RefreshTokenEngineService(
                new RefreshTokenProperties(EXPIRATION,GRACE_PERIOD),
                Clock.fixed(NOW, ZoneOffset.UTC));

        this.user = User.builder().build();

    }

    @Nested
    @DisplayName("Tests for the generateRefreshToken method")
    class GenerateRefreshTokenTests {

        @Test
        void entityTokenIsDifferentFromTheRawToken (){

            RefreshHashToken tokens = refreshTokenEngineService.generateRefreshToken(user);

            assertThat(tokens.rawToken()).isNotEqualTo(tokens.refreshToken().getToken());


        }

        @Test
        void entityTokenIsTheSameAsTheHashedRawToken (){

            RefreshHashToken tokens = refreshTokenEngineService.generateRefreshToken(user);
            String hashedRawToken = refreshTokenEngineService.hashToken(tokens.rawToken());

            assertThat(tokens.refreshToken().getToken()).isEqualTo(hashedRawToken);

        }

        @Test
        void rawTokensAreAlwaysDifferent(){

            RefreshHashToken firstTokens = refreshTokenEngineService.generateRefreshToken(user);
            RefreshHashToken secondTokens = refreshTokenEngineService.generateRefreshToken(user);

            assertThat(firstTokens.rawToken()).isNotEqualTo(secondTokens.rawToken());

        }

    }

    @Nested
    @DisplayName("Tests for the isExpired method")
    class IsExpiredTests {

        @Test
        void trueWhenTheTokenHasExpired () {

            RefreshHashToken tokens = refreshTokenEngineService.generateRefreshToken(user);

            RefreshTokenEngineService refreshTokenEngineServiceInTheFuture = new RefreshTokenEngineService(
                    new RefreshTokenProperties(EXPIRATION,GRACE_PERIOD),
                    Clock.fixed(NOW.plus(EXPIRATION)
                                    .plusSeconds(1),
                            ZoneOffset.UTC));

            assertThat(
                    refreshTokenEngineServiceInTheFuture.
                            isExpired(tokens.refreshToken()))
                    .isTrue();

        }

        @Test
        void falseWhenTheTokenIsValid () {

            RefreshHashToken tokens = refreshTokenEngineService.generateRefreshToken(user);

            RefreshTokenEngineService refreshTokenEngineServiceInTheFuture = new RefreshTokenEngineService(
                    new RefreshTokenProperties(EXPIRATION,GRACE_PERIOD),
                    Clock.fixed(NOW.plus(EXPIRATION),
                            ZoneOffset.UTC));

            assertThat(
                    refreshTokenEngineServiceInTheFuture.
                            isExpired(tokens.refreshToken()))
                    .isFalse();

        }

    }




}
