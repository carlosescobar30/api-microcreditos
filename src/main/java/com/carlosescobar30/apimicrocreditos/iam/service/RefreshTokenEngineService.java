package com.carlosescobar30.apimicrocreditos.iam.service;

import com.carlosescobar30.apimicrocreditos.iam.configuration.RefreshTokenProperties;
import com.carlosescobar30.apimicrocreditos.iam.domain.RefreshToken;
import com.carlosescobar30.apimicrocreditos.iam.domain.User;
import com.carlosescobar30.apimicrocreditos.iam.domain.enums.RefreshTokenStatus;
import com.carlosescobar30.apimicrocreditos.iam.internals.RefreshHashToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenEngineService {

    private final RefreshTokenProperties refreshTokenProperties;
    private final Clock clock;



    private String generateRawToken(){

        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

    }

    public RefreshHashToken generateRefreshToken (User user){

        String rawToken = generateRawToken();
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(hashToken(rawToken))
                .status(RefreshTokenStatus.ACTIVE)
                .rotatedAt(null)
                .expiresAt(clock.instant().plusMillis(refreshTokenProperties.expiration().toMillis()))
                .build();

        log.debug("Refresh Token created for userId: {}, expires: {} ",
                refreshToken.getUser().getId(),
                refreshToken.getExpiresAt());

        return new RefreshHashToken(refreshToken,rawToken);
    }



    public Boolean isExpired (RefreshToken token) {

        return token.getExpiresAt().isBefore(clock.instant());

    }

    public String hashToken(String rawToken) {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] rawHash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(rawHash);
        }
        catch (NoSuchAlgorithmException e ){
            throw new IllegalStateException("Algorithm SHA-256 not found", e);
        }

    }


}
