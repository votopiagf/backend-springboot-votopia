package com.votopia.votopiabackendspringboot.adapters.dtos;

import com.votopia.votopiabackendspringboot.dtos.list.ListOptionDto;
import com.votopia.votopiabackendspringboot.entities.lists.List;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Adapter per convertire entità List in DTO ListOptionDto
 * Responsabile della trasformazione dati dal dominio alla presentazione
 */
@Component
public class ListDtoAdapter {

    /**
     * Converte una singola entità List in ListOptionDto
     */
    public ListOptionDto toOptionDto(List list) {
        if (list == null) {
            return null;
        }
        return new ListOptionDto(
                list.getId(),
                list.getName(),
                list.getOrg() != null ? list.getOrg().getName() : null
        );
    }

    /**
     * Converte un set di List in un set di ListOptionDto
     */
    public Set<ListOptionDto> toOptionDtos(Set<List> lists) {
        if (lists == null) {
            return Set.of();
        }
        return lists.stream()
                .map(this::toOptionDto)
                .collect(Collectors.toSet());
    }
}

