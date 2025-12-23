package com.votopia.votopiabackendspringboot.dtos.list;

import jakarta.validation.constraints.NotBlank;

public record ListCreateDto(
        @NotBlank(message = "Il nome è obbligatorio") String name,
        String description,
        String slogan,
        String colorPrimary,
        String colorSecondary,
        Long logoFileId
) {}