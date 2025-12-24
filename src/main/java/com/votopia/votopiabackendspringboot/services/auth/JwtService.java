package com.votopia.votopiabackendspringboot.services.auth;

import com.votopia.votopiabackendspringboot.entities.auth.User;

public interface JwtService {
    String generateToken(User user);
    String extractEmail(String token);
    Long extractUserId(String token);
    Long extractOrgId(String token);
    boolean isTokenValid(String token);
}