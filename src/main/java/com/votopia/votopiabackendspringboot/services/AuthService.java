package com.votopia.votopiabackendspringboot.services;

import com.votopia.votopiabackendspringboot.dtos.auth.LoginRequestDto;
import com.votopia.votopiabackendspringboot.dtos.auth.LoginSummaryDto;

public interface AuthService {
    LoginSummaryDto login(LoginRequestDto loginRequestDto);
}
