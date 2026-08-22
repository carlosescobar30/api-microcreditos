package com.carlosescobar30.apimicrocreditos.iam.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TokenResponseDTO(
        @JsonProperty String accessToken,
        @JsonProperty String tokenType,
        @JsonProperty Long expiresIn,
        @JsonProperty String refreshToken) {
}
