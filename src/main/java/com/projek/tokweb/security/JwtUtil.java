package com.projek.tokweb.security;

import java.security.Key;
import java.util.Base64;
import java.util.Date;

import org.springframework.stereotype.Component;

import com.projek.tokweb.models.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;

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
        try {
            // Debug: Check token structure
            System.out.println("Token received: " + token);
            if (token == null || token.isEmpty()) {
                throw new IllegalArgumentException("Token is null or empty");
            }
            
            String[] parts = token.split("\\.");
            System.out.println("Token parts count: " + parts.length);
            
            if (parts.length != 3) {
                throw new MalformedJwtException("JWT must have 3 parts but has " + parts.length);
            }
            
            // Debug: Check header
            try {
                String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]));
                System.out.println("Header JSON: " + headerJson);
            } catch (Exception e) {
                System.err.println("Error decoding header: " + e.getMessage());
            }
            
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(kunci)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getSubject();
        } catch (ExpiredJwtException e) {
            System.err.println("Token expired: " + e.getMessage());
            throw e;
        } catch (UnsupportedJwtException e) {
            System.err.println("Unsupported JWT: " + e.getMessage());
            throw e;
        } catch (MalformedJwtException e) {
            System.err.println("Malformed JWT: " + e.getMessage());
            throw e;
        } catch (SignatureException e) {
            System.err.println("Invalid signature: " + e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            System.err.println("Illegal argument: " + e.getMessage());
            throw e;
        } catch (JwtException e) {
            System.err.println("JWT exception: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("Unexpected error parsing JWT token: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    public boolean validateToken(String token, String email) {
        try {
            String extractedEmail = extractEmail(token);
            return (extractedEmail.equals(email) && !isTokenExpired(token));
        } catch (Exception e) {
            System.err.println("Token validation failed: " + e.getMessage());
            return false;
        }
    }
    
    private boolean isTokenExpired(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(kunci)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            Date expiration = claims.getExpiration();
            return expiration.before(new Date());
        } catch (Exception e) {
            System.err.println("Error checking token expiration: " + e.getMessage());
            return true; // Consider expired if any error occurs
        }
    }
}
