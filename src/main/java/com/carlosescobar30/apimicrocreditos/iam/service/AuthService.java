package com.carlosescobar30.apimicrocreditos.iam.service;


import com.carlosescobar30.apimicrocreditos.iam.configuration.JwtProperties;
import com.carlosescobar30.apimicrocreditos.iam.dto.*;
import com.carlosescobar30.apimicrocreditos.iam.internals.RefreshTokenCreated;
import com.carlosescobar30.apimicrocreditos.iam.dto.TokenResponseDTO;
import com.carlosescobar30.apimicrocreditos.iam.security.JwtService;
import com.carlosescobar30.apimicrocreditos.iam.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final JwtProperties jwtProperties;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserService userService;
    private final AuthenticationManager authenticationManager;


    public TokenResponseDTO login(LoginRequestDTO req) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.username(), req.password())
        );

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        log.debug("Successful authentification for userId: {}", userDetails.getId());
        String accessToken = jwtService.generateJwt(userDetails);
        String refreshToken = refreshTokenService.create(userDetails.getId());

        log.info("Refresh token and JWT created and returned to userId: {}", userDetails.getId());

        return buildTokenResponse(accessToken, refreshToken);
    }

    public TokenResponseDTO refresh (RefreshRequestDTO req) {

        RefreshTokenCreated refreshTokenCreated = refreshTokenService.process(req.refreshToken());
        UserDetailsImpl userDetails = refreshTokenCreated.userDetail();
        String accessToken = jwtService.generateJwt(userDetails);
        log.info("Refresh token and JWT rotated for userId: {}", userDetails.getId());

        return buildTokenResponse(accessToken, refreshTokenCreated.rawToken());

    }

    @Transactional
    public void register (RegisterRequestDTO req) {

        userService.createUser(req);

    }

    public void logout (Long userId){

        refreshTokenService.revokeAll(userId);
        log.info("Logout completed successfully for userId: {}", userId);

    }

    private TokenResponseDTO buildTokenResponse(String accessToken, String refreshToken){

        return TokenResponseDTO.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.expiration().toSeconds())
                .refreshToken(refreshToken)
                .build();


    }


}
