package com.votopia.votopiabackendspringboot.dtos;

public record ErrorResponse(
        boolean success,
        int status,
        String message,
        long timestamp
) {}