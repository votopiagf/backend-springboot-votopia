package com.votopia.votopiabackendspringboot.dtos.module;

import com.votopia.votopiabackendspringboot.entities.organizations.Module;
import jakarta.validation.constraints.NotNull;

public record ModuleUpdateDto (
        @NotNull(message = "L'id non può essere vuoto")
        Long id,
        String name,
        String description
) {
}
