package com.votopia.votopiabackendspringboot.dtos.module;

import com.votopia.votopiabackendspringboot.entities.organizations.Module;

public record ModuleSummaryDto(
        Long id,
        String name,
        String description
) {
    public ModuleSummaryDto(Module m){
        this(
                m.getId(),
                m.getName(),
                m.getDescription()
        );
    }
}
