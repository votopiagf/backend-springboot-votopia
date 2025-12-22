package com.votopia.votopiabackendspringboot.services;

import com.votopia.votopiabackendspringboot.entities.User;

public interface JwtService {
    String generateToken(User user);
    String extractEmail(String token);
    Long extractUserId(String token);
    Long extractOrgId(String token);
    boolean isTokenValid(String token);
}