package com.carlosescobar30.apimicrocreditos.iam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginRequestDTO(

        @NotBlank(message = "This field cannot be empty")
        @Size(min = 1, max = 30, message = "This field must contain between 1 and 30 characters")
        @Pattern(regexp = "^[a-zA-Z0-9ñÑ]+$", message = "This field does not accept special characters.")
        String username,

        @NotBlank(message = "This field cannot be empty")
        String password) {
}
