package com.votopia.votopiabackendspringboot.dtos.campaign;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CampaignCreateDto(
        @NotBlank(message = "Il nome della campagna è obbligatorio")
        String name,

        @NotNull(message = "L'ID della lista è obbligatorio")
        Long listId,

        String description,

        @NotNull(message = "La data di inizio è obbligatoria")
        LocalDate startDate,

        @NotNull(message = "La data di fine è obbligatoria")
        LocalDate endDate
) {}