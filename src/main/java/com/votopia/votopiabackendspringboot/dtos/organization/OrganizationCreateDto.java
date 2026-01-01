package com.votopia.votopiabackendspringboot.dtos.organization;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OrganizationCreateDto(
        @NotBlank(message = "Il nome non può essere vuoto")
        @Size(max = 100)
        String name,

        Long planId
) {
}
