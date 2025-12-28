package com.votopia.votopiabackendspringboot.services.auth;

import com.votopia.votopiabackendspringboot.dtos.auth.LoginRequestDto;
import com.votopia.votopiabackendspringboot.dtos.auth.LoginSummaryDto;
import com.votopia.votopiabackendspringboot.entities.auth.User;

public interface AuthService {
    LoginSummaryDto login(LoginRequestDto loginRequestDto);
    User getAuthenticatedUser(Long authUserId);
}
