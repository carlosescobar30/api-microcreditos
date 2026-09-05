package com.carlosescobar30.apimicrocreditos.iam.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequestDTO(@NotBlank String refreshToken){}
