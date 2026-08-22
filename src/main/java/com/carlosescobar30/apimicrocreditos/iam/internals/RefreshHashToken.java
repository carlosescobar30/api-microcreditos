package com.carlosescobar30.apimicrocreditos.iam.internals;

import com.carlosescobar30.apimicrocreditos.iam.domain.RefreshToken;

public record RefreshHashToken(
        RefreshToken refreshToken,
        String rawToken
) {
}
