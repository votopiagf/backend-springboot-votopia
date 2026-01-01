package com.votopia.votopiabackendspringboot.dtos.user;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

/*@Data
@Getter
@Setter
public class UserUpdateDto {
    private Long id;
    private String name;
    private String surname;
    private String email;
    private Boolean resetPassword;
    Set<Long> addLists;
    Set<Long> removeLists;
}
*/

public record UserUpdateDto(
        Long id,
        String name,
        String surname,
        String email,
        Boolean resetPassword,
        Set<Long> addLists,
        Set<Long> removeLists
) {
}