package com.projek.tokweb.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.projek.tokweb.models.User;
import com.projek.tokweb.models.Role;

@Component
public class AuthUtils {
    
    /**
     * Get current authenticated user from security context
     * @return User object if authenticated, null otherwise
     */
    public static User getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication != null && authentication.isAuthenticated() 
                && authentication.getPrincipal() instanceof User) {
                return (User) authentication.getPrincipal();
            }
        } catch (Exception e) {
            System.err.println("Error getting current user: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Check if current user is authenticated
     * @return true if user is authenticated, false otherwise
     */
    public static boolean isAuthenticated() {
        return getCurrentUser() != null;
    }
    
    /**
     * Check if current user has ADMIN role
     * @return true if user is admin, false otherwise
     */
    public static boolean isAdmin() {
        User user = getCurrentUser();
        return user != null && user.getRole() == Role.ADMIN;
    }
    
    /**
     * Check if current user has USER role
     * @return true if user is regular user, false otherwise
     */
    public static boolean isUser() {
        User user = getCurrentUser();
        return user != null && user.getRole() == Role.USER;
    }
    
    /**
     * Get current user ID
     * @return user ID if authenticated, null otherwise
     */
    public static Long getCurrentUserId() {
        User user = getCurrentUser();
        return user != null ? user.getId() : null;
    }
    
    /**
     * Get current user email
     * @return user email if authenticated, null otherwise
     */
    public static String getCurrentUserEmail() {
        User user = getCurrentUser();
        return user != null ? user.getEmail() : null;
    }
    
    /**
     * Get current user role
     * @return user role if authenticated, null otherwise
     */
    public static Role getCurrentUserRole() {
        User user = getCurrentUser();
        return user != null ? user.getRole() : null;
    }
}
