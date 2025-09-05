import java.security.Key;
import java.util.Base64;
import java.util.Date;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

public class JwtTokenTest {
    private static final String SECRET_KEY = "mySuperSecretKeyThatIsAtLeastThirtyTwoBytesLong!";
    
    public static void main(String[] args) {
        // Test with the provided token
        String token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJmYWphci5yYWZzYW4wMkBnbWFpbC5jb20iLCJyb2xlIjoiQURNSU4iLCJpYXQiOjE3NTcwNzUxNDIsImV4cCI6MTc1NzA3ODc0Mn0.T91mjyxCMHDvhUgqSgzSe8MnFHr0MbwvwSQf_N3pmTQ";
        
        System.out.println("Testing token: " + token);
        System.out.println("Token length: " + token.length());
        
        // Split token to check parts
        String[] parts = token.split("\\\\.");
        System.out.println("Token parts: " + parts.length);
        
        for (int i = 0; i < parts.length; i++) {
            System.out.println("Part " + i + " length: " + parts[i].length());
            try {
                if (i == 0) {
                    // Decode header
                    String decoded = new String(Base64.getUrlDecoder().decode(parts[i]));
                    System.out.println("Header (decoded): " + decoded);
                } else if (i == 1) {
                    // Decode payload
                    String decoded = new String(Base64.getUrlDecoder().decode(parts[i]));
                    System.out.println("Payload (decoded): " + decoded);
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
            
            System.out.println("SUCCESS: Token parsed successfully");
            System.out.println("Subject: " + claims.getSubject());
            System.out.println("Role: " + claims.get("role"));
            System.out.println("Issued at: " + claims.getIssuedAt());
            System.out.println("Expiration: " + claims.getExpiration());
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}