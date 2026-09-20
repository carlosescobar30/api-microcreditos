package com.carlosescobar30.apimicrocreditos.iam.internals;


import com.carlosescobar30.apimicrocreditos.common.identity.UserDetailsImpl;

public record RefreshTokenCreated(
        String rawToken,
        UserDetailsImpl userDetail
) {
}
