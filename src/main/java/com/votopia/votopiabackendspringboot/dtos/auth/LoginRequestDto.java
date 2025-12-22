package com.votopia.votopiabackendspringboot.dtos.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequestDto {
    @NonNull
    private String codeOrg;

    @NonNull
    private String email;

    @NonNull
    private String password;
}
