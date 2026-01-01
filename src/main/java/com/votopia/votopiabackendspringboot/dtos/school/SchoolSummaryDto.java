package com.votopia.votopiabackendspringboot.dtos.school;

import com.votopia.votopiabackendspringboot.entities.lists.School;

public record SchoolSummaryDto (
    Long id,
    String name,
    String addressStreet,
    String city
)
{
    public SchoolSummaryDto(School s){
        this(
                s.getId(),
                s.getName(),
                s.getAddressStreet(),
                s.getCity()
        );
    }
}
