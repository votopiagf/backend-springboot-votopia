package com.votopia.votopiabackendspringboot.dtos;

public record SuccessResponse<T> (
    boolean success,
    int status,
    T data,
    String message,
    long timestamp
) {}
