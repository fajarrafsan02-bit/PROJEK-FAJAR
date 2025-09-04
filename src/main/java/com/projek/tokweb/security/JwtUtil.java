package com.projek.tokweb.security;

import java.security.Key;
import java.util.Date;

import org.springframework.stereotype.Component;

import com.projek.tokweb.models.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {
    private final Key kunci = Keys.hmacShaKeyFor("mySuperSecretKeyThatIsAtLeastThirtyTwoBytesLong!".getBytes());

    public String generateToken(User user) {
        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("role", user.getRole().name())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60 * 60 * 1000))
                // .setExpiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(kunci, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractEmail(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(kunci)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }
}
