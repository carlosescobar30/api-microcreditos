package com.carlosescobar30.apimicrocreditos.iam.internals;


import com.carlosescobar30.apimicrocreditos.iam.security.UserDetailsImpl;

public record RefreshTokenCreated(
        String rawToken,
        UserDetailsImpl userDetail
) {
}
