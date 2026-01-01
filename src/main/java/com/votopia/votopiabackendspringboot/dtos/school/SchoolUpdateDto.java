package com.votopia.votopiabackendspringboot.dtos.school;

import jakarta.validation.constraints.NotNull;

public record SchoolUpdateDto(
        @NotNull(message = "L'id della scuola non può essere vuoto")
        Long id,

        String name,
        String addressStreet,
        String city,
        String schoolCode
) { }
