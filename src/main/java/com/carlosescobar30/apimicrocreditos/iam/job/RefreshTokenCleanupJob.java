package com.carlosescobar30.apimicrocreditos.iam.job;


import com.carlosescobar30.apimicrocreditos.iam.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenCleanupJob {


    private final RefreshTokenService refreshTokenService;

    @Scheduled(cron = "${security.refresh-token.cleanup-cron}")
    public void removeExpiredAndRevoked(){

        int removed = refreshTokenService.cleanupRevokedAndExpiredRefreshToken();
        log.info("Expired and revoked Refresh Tokens removed: {}", removed);

    }

    @Scheduled(cron = "${security.refresh-token.cleanup-cron}")
    public void removeRotated(){

        int removed = refreshTokenService.cleanupRotatedRefreshToken();
        log.info("Rotated Refresh Tokens removed: {}", removed);

    }

}
