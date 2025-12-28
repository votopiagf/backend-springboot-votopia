package com.votopia.votopiabackendspringboot.dtos.campaign;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CampaignUpdateDto(
        @NotNull(message = "L'id non può essere null")
        Long id,
        String name,
        String description,
        LocalDate startDate,
        LocalDate endDate
) {}
