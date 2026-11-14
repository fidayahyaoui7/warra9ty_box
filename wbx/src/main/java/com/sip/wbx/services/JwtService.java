package com.sip.wbx.services;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import com.sip.wbx.entities.Role;
import com.sip.wbx.entities.User;

@Service
public class JwtService {

    private final JwtEncoder encoder;
    private final long expirationMinutes;

    public JwtService(JwtEncoder encoder,
                      @Value("${app.jwt.expiration-minutes:120}") long expirationMinutes) {
        this.encoder = encoder;
        this.expirationMinutes = expirationMinutes;
    }

    public String generate(User u) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .subject(u.getEmail())
            .issuedAt(now)
            .expiresAt(now.plus(expirationMinutes, ChronoUnit.MINUTES))
            .claim("uid", u.getId())
            .claim("roles", u.getRoles().stream().map(Role::getLibelle).toList())
            .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}