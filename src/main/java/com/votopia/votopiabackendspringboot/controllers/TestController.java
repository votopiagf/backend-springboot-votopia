package com.votopia.votopiabackendspringboot.controllers;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequestMapping("/api/test")
public class TestController {
    @Autowired
    PasswordEncoder passwordEncoder;
    @GetMapping("/encrypt-password/")
    @SecurityRequirements
    ResponseEntity<String> encryptPassword(@Valid @RequestParam String param){
        return ResponseEntity.ok(passwordEncoder.encode(param));
    }
}
