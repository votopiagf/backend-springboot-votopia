package com.votopia.votopiabackendspringboot.dtos.school;

import com.votopia.votopiabackendspringboot.entities.lists.School;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SchoolCreatedDto(
        @NotBlank(message = "Il nome non può essere vuoto")
        @Size(max = 100)
        String name,

        @NotBlank(message = "L'indirizzo non può essere vuoto")
        String addressStreet,

        @NotBlank(message = "La città non può essere vuota")
        String city,

        @NotBlank(message = "Il codice meccanografico non può essere vuoto")
        @Size(max = 20)
        String schoolCode
) {
    public SchoolCreatedDto(School s){
        this(
                s.getName(),
                s.getAddressStreet(),
                s.getCity(),
                s.getSchoolCode()
        );
    }
}
