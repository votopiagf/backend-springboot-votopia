package com.votopia.votopiabackendspringboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.password.PasswordEncoder;

// Aggiungi queste esclusioni specifiche
@SpringBootApplication(exclude = {
        org.springdoc.core.configuration.SpringDocDataRestConfiguration.class,
        org.springdoc.core.configuration.SpringDocHateoasConfiguration.class
})
public class VotopiaBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(VotopiaBackendApplication.class, args);
    }
}