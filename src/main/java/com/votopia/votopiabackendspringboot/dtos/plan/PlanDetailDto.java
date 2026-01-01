package com.votopia.votopiabackendspringboot.dtos.plan;

import com.votopia.votopiabackendspringboot.dtos.module.ModuleDetailDto;
import com.votopia.votopiabackendspringboot.dtos.module.ModuleSummaryDto;
import com.votopia.votopiabackendspringboot.entities.organizations.Plan;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

public record PlanDetailDto(
        Long id,
        String name,
        BigDecimal price,
        LocalDateTime createdAt,
        Set<ModuleDetailDto> modules
){
    public PlanDetailDto(Plan p){
        this(
                p.getId(),
                p.getName(),
                p.getPrice(),
                p.getCreatedAt(),
                p.getModules().stream()
                        .map(ModuleDetailDto::new)
                        .collect(Collectors.toSet())
        );
    }
}
