package com.carlosescobar30.apimicrocreditos.iam.service;

import com.carlosescobar30.apimicrocreditos.TestcontainersConfiguration;
import com.carlosescobar30.apimicrocreditos.common.exception.unauthorized.RefreshTokenInvalidException;
import com.carlosescobar30.apimicrocreditos.iam.configuration.RefreshTokenProperties;
import com.carlosescobar30.apimicrocreditos.iam.domain.RefreshToken;
import com.carlosescobar30.apimicrocreditos.iam.domain.User;
import com.carlosescobar30.apimicrocreditos.iam.domain.enums.RefreshTokenStatus;
import com.carlosescobar30.apimicrocreditos.iam.internals.RefreshTokenCreated;
import com.carlosescobar30.apimicrocreditos.iam.repository.RefreshTokenRepository;
import com.carlosescobar30.apimicrocreditos.iam.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import({TestcontainersConfiguration.class, RefreshTokenFlowIT.MutableClockConfig.class})
class RefreshTokenFlowIT {

    private static final Instant NOW = Instant.parse("2099-01-01T00:00:00Z");

    @Autowired
    private RefreshTokenService refreshTokenService;
    @Autowired
    private RefreshTokenEngineService refreshTokenEngine;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RefreshTokenProperties refreshTokenProperties;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private MutableClock clock;

    private Long userId;
    private Long otherUserId;

    @BeforeEach
    void setUp() {

        clock.setTo(NOW);
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        userId = userRepository.save(newUser("carlos", "carlos@mail.com")).getId();
        otherUserId = userRepository.save(newUser("otro", "otro@mail.com")).getId();
    }

    @Nested
    @DisplayName("Queries that only a real database can verify")
    class RepositoryQueryTests {

        @Test
        void rotateOnlyAffectsTokensThatAreStillActive() {

            String rawToken = refreshTokenService.create(userId);
            String hashedToken = refreshTokenEngine.hashToken(rawToken);

            int firstRotation = transactionTemplate.execute(
                    status -> refreshTokenRepository.rotate(hashedToken, clock.instant()));

            int secondRotation = transactionTemplate.execute(
                    status -> refreshTokenRepository.rotate(hashedToken, clock.instant()));

            assertThat(firstRotation).isEqualTo(1);
            assertThat(secondRotation).isZero();
        }

        @Test
        void revokeAllByUserLeavesTheTokensOfOtherUsersUntouched() {

            String myToken = refreshTokenService.create(userId);
            String foreignToken = refreshTokenService.create(otherUserId);

            refreshTokenService.revokeAll(userId);

            assertThat(statusOf(myToken)).isEqualTo(RefreshTokenStatus.REVOKED);
            assertThat(statusOf(foreignToken)).isEqualTo(RefreshTokenStatus.ACTIVE);
        }
    }

    @Nested
    @DisplayName("Rotation against a real transaction")
    class RotationTests {

        @Test
        void theOldTokenIsRotatedAndANewActiveOneIsIssued() {

            String originalToken = refreshTokenService.create(userId);

            RefreshTokenCreated result = refreshTokenService.process(originalToken);

            assertThat(result.rawToken()).isNotNull().isNotEqualTo(originalToken);
            assertThat(result.userDetail().getId()).isEqualTo(userId);
            assertThat(statusOf(originalToken)).isEqualTo(RefreshTokenStatus.ROTATED);
            assertThat(statusOf(result.rawToken())).isEqualTo(RefreshTokenStatus.ACTIVE);
        }

        @Test
        void theRevocationSurvivesTheExceptionWhenAReusedTokenIsDetected() {

            String originalToken = refreshTokenService.create(userId);
            refreshTokenService.process(originalToken);

            clock.advance(refreshTokenProperties.gracePeriod().plusSeconds(1));

            assertThatThrownBy(() -> refreshTokenService.process(originalToken))
                    .isInstanceOf(RefreshTokenInvalidException.class);

            assertThat(refreshTokenRepository.findAll())
                    .isNotEmpty()
                    .allMatch(token -> token.getStatus() == RefreshTokenStatus.REVOKED);
        }

        @Test
        void twoSimultaneousRotationsProduceOneNewTokenAndOneGracePeriodResponse() throws Exception {

            String originalToken = refreshTokenService.create(userId);

            ExecutorService executor = Executors.newFixedThreadPool(2);
            CountDownLatch startLine = new CountDownLatch(1);

            Callable<RefreshTokenCreated> rotation = () -> {
                startLine.await();
                return refreshTokenService.process(originalToken);
            };

            Future<RefreshTokenCreated> firstAttempt = executor.submit(rotation);
            Future<RefreshTokenCreated> secondAttempt = executor.submit(rotation);

            startLine.countDown();

            RefreshTokenCreated firstResult = firstAttempt.get(10, TimeUnit.SECONDS);
            RefreshTokenCreated secondResult = secondAttempt.get(10, TimeUnit.SECONDS);
            executor.shutdown();

            List<String> rawTokens = Stream.of(firstResult, secondResult)
                    .map(RefreshTokenCreated::rawToken)
                    .toList();

            assertThat(rawTokens.stream().filter(Objects::isNull).count()).isEqualTo(1);
            assertThat(rawTokens.stream().filter(Objects::nonNull).count()).isEqualTo(1);

            assertThat(firstResult.userDetail().getId()).isEqualTo(userId);
            assertThat(secondResult.userDetail().getId()).isEqualTo(userId);

            assertThat(refreshTokenRepository.findAll())
                    .filteredOn(token -> token.getStatus() == RefreshTokenStatus.ACTIVE)
                    .hasSize(1);
        }
    }

    private RefreshTokenStatus statusOf(String rawToken) {

        String hashedToken = refreshTokenEngine.hashToken(rawToken);

        return refreshTokenRepository.findAll().stream()
                .filter(token -> token.getToken().equals(hashedToken))
                .findFirst()
                .map(RefreshToken::getStatus)
                .orElseThrow();
    }

    private User newUser(String username, String email) {

        return User.builder()
                .name("irrelevant")
                .lastName("irrelevant")
                .username(username)
                .passwordHash("irrelevant")
                .email(email)
                .roles(new HashSet<>())
                .isIdentityVerified(false)
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class MutableClockConfig {

        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock(NOW);
        }
    }

    static class MutableClock extends Clock {

        private volatile Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        void setTo(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration amount) {
            this.instant = this.instant.plus(amount);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
