package com.sip.wbx.security;

import java.util.List;
import org.springframework.security.oauth2.jwt.Jwt;

/** Utilisateur connecté, lu dans le JWT (claims "uid" et "roles"). */
public record CurrentUser(Long id, String email, boolean admin) {

    public static CurrentUser of(Jwt jwt) {
        Number uid = jwt.getClaim("uid");
        List<String> roles = jwt.getClaimAsStringList("roles");
        return new CurrentUser(uid.longValue(), jwt.getSubject(), roles != null && roles.contains("ADMIN"));
    }
}
