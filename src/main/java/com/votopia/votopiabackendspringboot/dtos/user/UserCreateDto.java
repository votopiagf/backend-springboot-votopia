package com.votopia.votopiabackendspringboot.dtos.user;

import com.votopia.votopiabackendspringboot.entities.auth.Role;
import com.votopia.votopiabackendspringboot.entities.auth.User;
import com.votopia.votopiabackendspringboot.entities.lists.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.stream.Collectors;


public record UserCreateDto (
    @NotBlank(message = "Il nome non può essere vuoto")
    @Size(max = 100)
    String name,

    @NotBlank(message = "Il cognome non può essere vuoto")
    @Size(max = 100)
    String surname,

    @NotBlank(message = "L'email non può essere vuota")
    @Size(max = 150)
    String email,

    @NotBlank(message = "La password non può essere vuota")
    String password,

    Set<Long> rolesId,

    Set<Long> listsId
) {
   public UserCreateDto(User u){
       this(
               u.getName(),
               u.getSurname(),
               u.getEmail(),
               u.getPassword(),
               u.getRoles().stream()
                       .map(Role::getId)
                       .collect(Collectors.toSet()),
               u.getLists().stream()
                       .map(List::getId)
                       .collect(Collectors.toSet())
       );
   }
}
