package com.votopia.votopiabackendspringboot.dtos.candidate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CandidateCreateDto(
        @NotNull(message = "L'id dell'user non può essere vuoto")
        Long userId,

        @NotBlank(message = "La classe non può essere vuota")
        String schoolClass,

        Long photoFileId,

        String bio,

        @NotNull(message = "La campagna si deve riferire ad una lista")
        Long listId
) {
}
