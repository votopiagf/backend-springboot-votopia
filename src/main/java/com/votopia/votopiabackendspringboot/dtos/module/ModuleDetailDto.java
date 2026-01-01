package com.votopia.votopiabackendspringboot.dtos.module;

import com.votopia.votopiabackendspringboot.dtos.plan.PlanDetailDto;
import com.votopia.votopiabackendspringboot.dtos.plan.PlanSummaryDto;
import com.votopia.votopiabackendspringboot.entities.organizations.Module;

import java.util.Set;
import java.util.stream.Collectors;

public record ModuleDetailDto(
        Long id,
        String name,
        String description,
        Set<PlanSummaryDto> plains
) {
    public ModuleDetailDto(Module m){
        this(
                m.getId(),
                m.getName(),
                m.getDescription(),
                m.getPlans().stream()
                        .map(PlanSummaryDto::new)
                        .collect(Collectors.toSet())
        );
    }
}
