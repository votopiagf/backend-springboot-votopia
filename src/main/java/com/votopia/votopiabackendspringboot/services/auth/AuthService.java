package com.votopia.votopiabackendspringboot.services.auth;

import com.votopia.votopiabackendspringboot.dtos.auth.LoginRequestDto;
import com.votopia.votopiabackendspringboot.dtos.auth.LoginSummaryDto;

public interface AuthService {
    LoginSummaryDto login(LoginRequestDto loginRequestDto);
}
