package com.votopia.votopiabackendspringboot.adapters.dtos;

import com.votopia.votopiabackendspringboot.dtos.list.ListOptionDto;
import com.votopia.votopiabackendspringboot.dtos.role.RoleOptionDto;
import com.votopia.votopiabackendspringboot.dtos.user.UsersScreenInitDto;
import com.votopia.votopiabackendspringboot.entities.auth.Role;
import com.votopia.votopiabackendspringboot.entities.lists.List;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Adapter per costruire UsersScreenInitDto
 * Aggrega i dati da più fonti e li trasforma in un DTO completo
 */
@Component
public class UsersScreenDtoAdapter {

    private final ListDtoAdapter listDtoAdapter;
    private final RoleDtoAdapter roleDtoAdapter;

    public UsersScreenDtoAdapter(ListDtoAdapter listDtoAdapter, RoleDtoAdapter roleDtoAdapter) {
        this.listDtoAdapter = listDtoAdapter;
        this.roleDtoAdapter = roleDtoAdapter;
    }

    /**
     * Costruisce un UsersScreenInitDto da liste, ruoli e statistiche
     */
    public UsersScreenInitDto buildUsersScreenInitDto(
            Set<List> lists,
            Set<Role> orgRoles,
            Set<Role> listRoles,
            Long totalUsers,
            Long totalRoles,
            Long totalLists,
            boolean canFilterAllOrganization,
            boolean canFilterByList,
            Long restrictedToListId,
            String restrictedToListName) {

        Set<ListOptionDto> listDtos = listDtoAdapter.toOptionDtos(lists);
        Set<RoleOptionDto> orgRoleDtos = roleDtoAdapter.toOptionDtos(orgRoles);
        Set<RoleOptionDto> listRoleDtos = roleDtoAdapter.toOptionDtos(listRoles);

        return new UsersScreenInitDto(
                listDtos,
                orgRoleDtos,
                listRoleDtos,
                totalUsers,
                totalRoles,
                totalLists,
                canFilterAllOrganization,
                canFilterByList,
                restrictedToListId,
                restrictedToListName
        );
    }
}

