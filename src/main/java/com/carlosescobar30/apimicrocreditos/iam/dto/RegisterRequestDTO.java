package com.carlosescobar30.apimicrocreditos.iam.dto;


import jakarta.validation.constraints.*;

import java.time.LocalDate;


public record RegisterRequestDTO(

        @NotNull(message = "This field cannot be empty")
        @Size(min = 1, max = 30, message = "This field must contain between 1 and 30 characters")
        String name,

        @NotNull(message = "This field cannot be empty")
        @Size(min = 1, max = 30, message = "This field must contain between 1 and 30 characters")
        String lastName,

        @NotNull(message = "This field cannot be empty")
        @Size(min = 1, max = 30, message = "This field must contain between 1 and 30 characters")
        @Pattern(regexp = "^[a-zA-Z0-9ñÑ]+$", message = "This field does not accept special characters")
        String username,

        @NotNull(message = "This field cannot be empty")
        @Size(min = 8, max = 30, message = "This field must contain between 8 and 30 characters")
        String password,

        @NotNull(message = "This field cannot be empty")
        @Email(message = "This field must be a valid email address")
        String email,

        @NotNull(message = "This field cannot be empty")
        @Past(message = "invalid date")
        LocalDate birthDate

) {
}
