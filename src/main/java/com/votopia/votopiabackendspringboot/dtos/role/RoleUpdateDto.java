package com.votopia.votopiabackendspringboot.dtos.role;

import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record RoleUpdateDto(
        @NotNull(message = "L'id non può essere vuoto")
        Long id,
        String name,
        String color,
        Integer level,
        Set<Long> permissions
) {}