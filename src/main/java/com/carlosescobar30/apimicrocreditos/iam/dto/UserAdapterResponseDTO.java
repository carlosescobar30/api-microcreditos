package com.carlosescobar30.apimicrocreditos.iam.dto;

import lombok.Builder;

import java.util.UUID;

@Builder
public record UserAdapterResponseDTO(

        UUID userReference,
        boolean isIdentityVerified,
        int score

) {
}
