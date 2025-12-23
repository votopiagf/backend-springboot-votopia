package com.votopia.votopiabackendspringboot.dtos.list;

import jakarta.validation.constraints.NotNull;

public record ListUpdateDto(
        @NotNull(message = "L'ID della lista è obbligatorio") Long listId,
        String name,
        String description,
        String slogan,
        String colorPrimary,
        String colorSecondary,
        Long logoFileId
) {}