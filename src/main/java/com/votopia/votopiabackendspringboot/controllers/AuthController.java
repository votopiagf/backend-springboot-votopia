package com.votopia.votopiabackendspringboot.controllers;

import com.votopia.votopiabackendspringboot.dtos.auth.LoginRequestDto;
import com.votopia.votopiabackendspringboot.dtos.auth.LoginSummaryDto;
import com.votopia.votopiabackendspringboot.services.AuthService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/auth/")
public class AuthController {
    @Autowired
    private AuthService authService;

    @PostMapping("login/")
    @SecurityRequirements
    ResponseEntity<LoginSummaryDto> login(@RequestBody @Valid LoginRequestDto request){
        return ResponseEntity.ok(authService.login(request));
    }

}
