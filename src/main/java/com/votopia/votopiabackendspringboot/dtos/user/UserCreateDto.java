package com.votopia.votopiabackendspringboot.dtos.user;

import com.votopia.votopiabackendspringboot.entities.List;
import lombok.*;
import org.springframework.lang.NonNullApi;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UserCreateDto {
    @NonNull
    private String name;

    @NonNull
    private String surname;

    @NonNull
    private String email;

    @NonNull
    private String password;

    private Set<Long> rolesId;

    private Set<Long> listsId;
}
