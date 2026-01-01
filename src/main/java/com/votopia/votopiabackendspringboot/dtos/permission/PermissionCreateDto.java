package com.votopia.votopiabackendspringboot.dtos.permission;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PermissionCreateDto (
        @NotBlank(message = "Il nome non può essere vuoto")
        @Size(max = 50)
        String name,

        @Size(max = 244)
        String description
){
}
