package com.votopia.votopiabackendspringboot.dtos.auth;

import com.votopia.votopiabackendspringboot.dtos.user.UserSummaryDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginSummaryDto {

    @NonNull
    private String token;

    @NonNull
    private UserSummaryDto userSummaryDto;
}
