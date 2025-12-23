package com.votopia.votopiabackendspringboot.dtos.role;

import com.votopia.votopiabackendspringboot.entities.Permission;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Data
@Getter
@Setter
public class RoleCreateDto {
    @NonNull
    private String name;
    private String color;
    @NonNull
    private Integer level;
    private Long listId;
    private Set<Long> permissionsId = new HashSet<>();
}
