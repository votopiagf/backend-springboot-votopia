package com.votopia.votopiabackendspringboot.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// Questa annotazione dice a Spring: "Ogni volta che viene lanciata questa eccezione, rispondi con 403 Forbidden"
@ResponseStatus(HttpStatus.FORBIDDEN)
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}