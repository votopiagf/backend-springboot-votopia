package com.votopia.votopiabackendspringboot.dtos.module;

import com.votopia.votopiabackendspringboot.entities.organizations.Module;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ModuleCrateDto(
        @NotBlank(message = "Il nome non può essere vuoto")
        @Size(max = 50)
        String name,

        String description
) {
    public ModuleCrateDto(Module m){
        this(
                m.getName(),
                m.getDescription()
        );
    }
}
