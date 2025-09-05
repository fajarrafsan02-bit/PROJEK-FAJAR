package com.projek.tokweb.controller.debug;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.projek.tokweb.security.JwtUtil;
import com.projek.tokweb.util.JwtTestUtil;

@RestController
@RequestMapping("/debug")
public class JwtDebugController {
    
    private final JwtUtil jwtUtil;
    
    public JwtDebugController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }
    
    @GetMapping("/test-token")
    public String testToken(@RequestParam("token") String token) {
        try {
            System.out.println("=== DEBUGGING TOKEN ===");
            JwtTestUtil.testToken(token);
            
            System.out.println("=== USING JWT UTIL ===");
            String email = jwtUtil.extractEmail(token);
            return "Token is valid. Email: " + email;
        } catch (Exception e) {
            return "Token validation failed: " + e.getMessage();
        }
    }
}