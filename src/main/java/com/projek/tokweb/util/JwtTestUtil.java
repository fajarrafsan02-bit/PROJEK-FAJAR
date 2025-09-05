package com.projek.tokweb.util;

import java.security.Key;
import java.util.Base64;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

public class JwtTestUtil {
    
    // Use the same key as in your JwtUtil class
    private static final String SECRET_KEY = "mySuperSecretKeyThatIsAtLeastThirtyTwoBytesLong!";
    
    public static void testToken(String token) {
        System.out.println("Testing token: " + token);
        System.out.println("Token length: " + (token != null ? token.length() : "null"));
        
        if (token == null || token.isEmpty()) {
            System.out.println("Token is null or empty");
            return;
        }
        
        // Split token to check parts
        String[] parts = token.split("\\\\.");
        System.out.println("Token parts: " + parts.length);
        for (int i = 0; i < parts.length; i++) {
            System.out.println("Part " + i + " length: " + parts[i].length());
            try {
                // Decode and print header (part 0)
                if (i == 0) {
                    String decoded = new String(Base64.getUrlDecoder().decode(parts[i]));
                    System.out.println("Header (decoded): " + decoded);
                }
            } catch (Exception e) {
                System.out.println("Error decoding part " + i + ": " + e.getMessage());
            }
        }
        
        // Try to parse the token
        try {
            Key key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            System.out.println("Claims: " + claims);
            System.out.println("Subject: " + claims.getSubject());
        } catch (Exception e) {
            System.out.println("Error parsing token: " + e.getMessage());
            e.printStackTrace();
        }
    }
}