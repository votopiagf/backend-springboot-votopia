package com.votopia.votopiabackendspringboot.dtos.plan;

import com.votopia.votopiabackendspringboot.entities.organizations.Module;
import com.votopia.votopiabackendspringboot.entities.organizations.Plan;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

public record PlanCreateDto(
        @NotNull(message = "L'id non può essere vuoto")
        Long id,

        @NotBlank(message = "Il nome non può essere vuoto")
        @Size(max = 50)
        String name,

        @NotNull(message = "Il prezzo non può essere vuoto")
        BigDecimal price,

        Set<Long> modulesId
) {
    public PlanCreateDto(Plan p){
        this(
                p.getId(),
                p.getName(),
                p.getPrice(),
                p.getModules().stream()
                        .map(Module::getId)
                        .collect(Collectors.toSet())
        );
    }
}
