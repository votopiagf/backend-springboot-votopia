package com.votopia.votopiabackendspringboot.dtos.school;

import com.votopia.votopiabackendspringboot.entities.lists.School;

import java.time.LocalDateTime;

public record SchoolDetailDto(
        Long id,
        String name,
        String addressStreet,
        String city,
        String schoolCode,
        LocalDateTime createdAt
) {
    public SchoolDetailDto(School s){
        this(
                s.getId(),
                s.getName(),
                s.getAddressStreet(),
                s.getCity(),
                s.getSchoolCode(),
                s.getCreatedAt()
        );
    }
}
