package com.votopia.votopiabackendspringboot.dtos.plan;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Set;

public record PlanUpdateDto(
        @NotNull(message = "L'id non può essere vuoto")
        Long id,

        String name,
        BigDecimal price,
        Set<Long> addModules,
        Set<Long> removeModules
) {
}
