package com.carlosescobar30.apimicrocreditos.iam.service;

import com.carlosescobar30.apimicrocreditos.common.exception.unauthorized.RefreshTokenInvalidException;
import com.carlosescobar30.apimicrocreditos.iam.configuration.RefreshTokenProperties;
import com.carlosescobar30.apimicrocreditos.iam.domain.RefreshToken;
import com.carlosescobar30.apimicrocreditos.iam.domain.User;
import com.carlosescobar30.apimicrocreditos.iam.domain.enums.RefreshTokenStatus;
import com.carlosescobar30.apimicrocreditos.iam.internals.RefreshHashToken;
import com.carlosescobar30.apimicrocreditos.iam.internals.RefreshTokenCreated;
import com.carlosescobar30.apimicrocreditos.iam.repository.RefreshTokenRepository;
import com.carlosescobar30.apimicrocreditos.iam.security.UserDetailsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceProcessTest {

    private static final Instant NOW = Instant.parse("2099-01-01T00:00:00Z");
    private static final Duration EXPIRATION = Duration.ofDays(7);
    private static final Duration GRACE_PERIOD = Duration.ofSeconds(5);
    private static final Long USER_ID = 42L;
    private static final String RAW_TOKEN = "raw-token";
    private static final String HASHED_TOKEN = "hashed-token";
    private static final String NEW_RAW_TOKEN = "new-raw-token";

    @Mock
    private RefreshTokenRepository repository;
    @Mock
    private RefreshTokenEngineService refreshTokenEngine;
    @Mock
    private UserService userService;

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {

        this.refreshTokenService = new RefreshTokenService(
                new RefreshTokenProperties(EXPIRATION, GRACE_PERIOD),
                repository,
                refreshTokenEngine,
                userService,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );

        when(refreshTokenEngine.hashToken(RAW_TOKEN)).thenReturn(HASHED_TOKEN);
    }

    @Nested
    @DisplayName("When the token cannot be used")
    class UnusableTokenTests {

        @Test
        void theLookupUsesTheHashedTokenNeverTheRawOne() {

            when(repository.findByTokenForUpdate(anyString())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> refreshTokenService.process(RAW_TOKEN))
                    .isInstanceOf(RefreshTokenInvalidException.class);

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(repository).findByTokenForUpdate(captor.capture());

            assertThat(captor.getValue())
                    .isEqualTo(HASHED_TOKEN)
                    .isNotEqualTo(RAW_TOKEN);
        }

        @Test
        void nothingIsRotatedOrRevokedWhenTheTokenDoesNotExist() {

            when(repository.findByTokenForUpdate(HASHED_TOKEN)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> refreshTokenService.process(RAW_TOKEN))
                    .isInstanceOf(RefreshTokenInvalidException.class);

            verify(repository, never()).rotate(any(), any());
            verify(repository, never()).revoke(any(), any());
            verify(repository, never()).revokeAllByUser(any(), any());
        }

        @Test
        void theTokenIsRevokedAndTheRequestFailsWhenItHasExpired() {

            RefreshToken token = tokenWith(RefreshTokenStatus.ACTIVE, null);
            when(repository.findByTokenForUpdate(HASHED_TOKEN)).thenReturn(Optional.of(token));
            when(refreshTokenEngine.isExpired(token)).thenReturn(true);

            assertThatThrownBy(() -> refreshTokenService.process(RAW_TOKEN))
                    .isInstanceOf(RefreshTokenInvalidException.class);

            verify(repository).revoke(HASHED_TOKEN, NOW);
            verify(repository, never()).rotate(any(), any());
        }

        @Test
        void theRequestFailsWithoutCascadeWhenTheTokenWasAlreadyRevoked() {

            RefreshToken token = tokenWith(RefreshTokenStatus.REVOKED, null);
            when(repository.findByTokenForUpdate(HASHED_TOKEN)).thenReturn(Optional.of(token));
            when(refreshTokenEngine.isExpired(token)).thenReturn(false);
            when(repository.rotate(HASHED_TOKEN, NOW)).thenReturn(0);

            assertThatThrownBy(() -> refreshTokenService.process(RAW_TOKEN))
                    .isInstanceOf(RefreshTokenInvalidException.class);

            verify(repository, never()).revokeAllByUser(any(), any());
        }

        @Test
        void theRequestFailsWhenTheStatusIsActiveButNothingWasRotated() {

            RefreshToken token = tokenWith(RefreshTokenStatus.ACTIVE, null);
            when(repository.findByTokenForUpdate(HASHED_TOKEN)).thenReturn(Optional.of(token));
            when(refreshTokenEngine.isExpired(token)).thenReturn(false);
            when(repository.rotate(HASHED_TOKEN, NOW)).thenReturn(0);

            assertThatThrownBy(() -> refreshTokenService.process(RAW_TOKEN))
                    .isInstanceOf(RefreshTokenInvalidException.class);

            verify(repository, never()).revokeAllByUser(any(), any());
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("When the token is active")
    class ActiveTokenTests {

        @Test
        void aNewRawTokenIsReturnedWhenTheRotationSucceeds() {

            RefreshToken token = tokenWith(RefreshTokenStatus.ACTIVE, null);
            User user = token.getUser();

            when(repository.findByTokenForUpdate(HASHED_TOKEN)).thenReturn(Optional.of(token));
            when(refreshTokenEngine.isExpired(token)).thenReturn(false);
            when(repository.rotate(HASHED_TOKEN, NOW)).thenReturn(1);
            when(userService.getReference(USER_ID)).thenReturn(user);
            when(refreshTokenEngine.generateRefreshToken(user))
                    .thenReturn(new RefreshHashToken(
                            tokenWith(RefreshTokenStatus.ACTIVE, null), NEW_RAW_TOKEN));
            when(userService.getUserDetailImpl(USER_ID)).thenReturn(userDetails());

            RefreshTokenCreated result = refreshTokenService.process(RAW_TOKEN);

            assertThat(result.rawToken()).isEqualTo(NEW_RAW_TOKEN);
            assertThat(result.userDetail().getId()).isEqualTo(USER_ID);

            verify(repository).save(any(RefreshToken.class));
            verify(repository, never()).revokeAllByUser(any(), any());
        }
    }

    @Nested
    @DisplayName("When the token was already rotated")
    class RotatedTokenTests {

        @Test
        void aNullRawTokenIsReturnedWhenTheRotationHappenedInsideTheGracePeriod() {

            RefreshToken token = tokenWith(
                    RefreshTokenStatus.ROTATED,
                    NOW.minus(GRACE_PERIOD).plusSeconds(1));

            when(repository.findByTokenForUpdate(HASHED_TOKEN)).thenReturn(Optional.of(token));
            when(refreshTokenEngine.isExpired(token)).thenReturn(false);
            when(repository.rotate(HASHED_TOKEN, NOW)).thenReturn(0);
            when(userService.getUserDetailImpl(USER_ID)).thenReturn(userDetails());

            RefreshTokenCreated result = refreshTokenService.process(RAW_TOKEN);

            assertThat(result.rawToken()).isNull();
            assertThat(result.userDetail().getId()).isEqualTo(USER_ID);
        }

        @Test
        void nothingIsRevokedWhenTheRotationHappenedInsideTheGracePeriod() {

            RefreshToken token = tokenWith(
                    RefreshTokenStatus.ROTATED,
                    NOW.minus(GRACE_PERIOD).plusSeconds(1));

            when(repository.findByTokenForUpdate(HASHED_TOKEN)).thenReturn(Optional.of(token));
            when(refreshTokenEngine.isExpired(token)).thenReturn(false);
            when(repository.rotate(HASHED_TOKEN, NOW)).thenReturn(0);
            when(userService.getUserDetailImpl(USER_ID)).thenReturn(userDetails());

            refreshTokenService.process(RAW_TOKEN);

            verify(repository, never()).revokeAllByUser(any(), any());
            verify(repository, never()).revoke(any(), any());
        }

        @Test
        void everyTokenOfTheUserIsRevokedWhenNowIsExactlyTheGracePeriodDeadline() {

            RefreshToken token = tokenWith(
                    RefreshTokenStatus.ROTATED,
                    NOW.minus(GRACE_PERIOD));

            when(repository.findByTokenForUpdate(HASHED_TOKEN)).thenReturn(Optional.of(token));
            when(refreshTokenEngine.isExpired(token)).thenReturn(false);
            when(repository.rotate(HASHED_TOKEN, NOW)).thenReturn(0);

            assertThatThrownBy(() -> refreshTokenService.process(RAW_TOKEN))
                    .isInstanceOf(RefreshTokenInvalidException.class);

            verify(repository).revokeAllByUser(USER_ID, NOW);
        }

        @Test
        void everyTokenOfTheUserIsRevokedWhenTheRotationHappenedOutsideTheGracePeriod() {

            RefreshToken token = tokenWith(
                    RefreshTokenStatus.ROTATED,
                    NOW.minus(GRACE_PERIOD).minusSeconds(1));

            when(repository.findByTokenForUpdate(HASHED_TOKEN)).thenReturn(Optional.of(token));
            when(refreshTokenEngine.isExpired(token)).thenReturn(false);
            when(repository.rotate(HASHED_TOKEN, NOW)).thenReturn(0);

            assertThatThrownBy(() -> refreshTokenService.process(RAW_TOKEN))
                    .isInstanceOf(RefreshTokenInvalidException.class);

            verify(repository).revokeAllByUser(USER_ID, NOW);
        }
    }

    private RefreshToken tokenWith(RefreshTokenStatus status, Instant rotatedAt) {

        User user = User.builder()
                .name("irrelevant")
                .lastName("irrelevant")
                .username("irrelevant")
                .passwordHash("irrelevant")
                .email("irrelevant")
                .roles(new HashSet<>())
                .isIdentityVerified(false)
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();

        user.setId(USER_ID);

        return RefreshToken.builder()
                .token(HASHED_TOKEN)
                .status(status)
                .user(user)
                .rotatedAt(rotatedAt)
                .expiresAt(NOW.plus(EXPIRATION))
                .build();
    }

    private UserDetailsImpl userDetails() {

        return new UserDetailsImpl(
                USER_ID,
                "irrelevant",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}
