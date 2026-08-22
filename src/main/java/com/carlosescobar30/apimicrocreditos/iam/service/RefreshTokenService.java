package com.carlosescobar30.apimicrocreditos.iam.service;

import com.carlosescobar30.apimicrocreditos.iam.configuration.RefreshTokenProperties;
import com.carlosescobar30.apimicrocreditos.common.exception.unauthorized.RefreshTokenInvalidException;
import com.carlosescobar30.apimicrocreditos.iam.domain.RefreshToken;
import com.carlosescobar30.apimicrocreditos.iam.domain.User;
import com.carlosescobar30.apimicrocreditos.iam.domain.enums.RefreshTokenStatus;
import com.carlosescobar30.apimicrocreditos.iam.internals.RefreshHashToken;
import com.carlosescobar30.apimicrocreditos.iam.internals.RefreshTokenCreated;
import com.carlosescobar30.apimicrocreditos.iam.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenProperties refreshTokenProperties;
    private final RefreshTokenRepository repository;
    private final RefreshTokenEngineService refreshTokenEngine;
    private final UserService userService;
    private final Clock clock;



    @Transactional
    public String create(Long userId) {


        User userReference = userService.getReference(userId);
        RefreshHashToken refreshHashToken = refreshTokenEngine.generateRefreshToken(userReference);
        repository.save(refreshHashToken.refreshToken());
        log.info("Refresh Token created for userId: {}", userReference.getId());
        return refreshHashToken.rawToken();

    }

    @Transactional(noRollbackFor = RefreshTokenInvalidException.class)
    public RefreshTokenCreated process(String rawToken){

        String hashToken = refreshTokenEngine.hashToken(rawToken);

        RefreshToken refreshToken = repository.findByTokenForUpdate(hashToken)
                .orElseThrow(RefreshTokenInvalidException::new);

        Long userId = refreshToken.getUser().getId();

        Instant rotatedAt = refreshToken.getRotatedAt();


        log.debug("Refresh Token returned from db for userId: {}",
                userId);

        if (refreshTokenEngine.isExpired(refreshToken)){
            log.warn("Refresh Token is expired for userId: {}", userId);
            repository.revoke(refreshToken.getToken(), clock.instant());
            throw new RefreshTokenInvalidException();
        }

        RefreshTokenStatus status = refreshToken.getStatus();

        int affectedRows = repository.rotate(hashToken, clock.instant());



        if(affectedRows == 1){

            String token = create(userId);
            log.info("Refresh Token rotated for userId: {}",
                    userId);

            return new RefreshTokenCreated(token, userService.getUserDetailImpl(userId));
        }



        switch (status){


            case ROTATED -> {

                if (clock.instant().isBefore(rotatedAt.plusMillis(refreshTokenProperties.gracePeriod().toMillis()))){

                    log.info("Rotation request made during grace period for userId: {}",
                            userId);
                    return new RefreshTokenCreated(null, userService.getUserDetailImpl(userId));
                }

                log.warn("All Refresh Tokens revoked due to suspected reuse for userId: {}",
                        userId);
                repository.revokeAllByUser(userId, clock.instant());
                throw new RefreshTokenInvalidException();

            }

            case REVOKED ->{
                log.warn("Refresh Token has already been revoked for userId: {}",
                        userId);
                throw new RefreshTokenInvalidException();
            }
            default ->{
                log.error("Unexpected error with Refresh Token for userId: {}",
                        userId);
                throw new RefreshTokenInvalidException();
            }


        }



    }

    @Transactional
    public void revokeAll (Long userId){

        repository.revokeAllByUser(userId, clock.instant());
        log.info("Refresh Token has already been revoked for userId: {}", userId);

    }

    @Transactional
    public int cleanupRevokedAndExpiredRefreshToken() {

        return repository.removeRevokedAndExpired(clock.instant());
    }

    @Transactional
    public int cleanupRotatedRefreshToken() {

        return repository.removeRotated(clock.instant().minus(1, ChronoUnit.DAYS));

    }

}
