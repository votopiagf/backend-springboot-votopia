package com.votopia.votopiabackendspringboot.dtos.plan;

import com.votopia.votopiabackendspringboot.entities.organizations.Plan;

import java.math.BigDecimal;
import java.util.Set;

public record PlanSummaryDto(
        Long id,
        String name,
        BigDecimal price
) {
    public PlanSummaryDto(Plan p){
        this(
                p.getId(),
                p.getName(),
                p.getPrice()
        );
    }
}
