package com.projek.tokweb.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.projek.tokweb.models.User;
import com.projek.tokweb.models.Role;
import com.projek.tokweb.repository.UserRespository;

@Component
public class AuthUtils {
    
    private static UserRespository userRepository;
    
    @Autowired
    public void setUserRepository(UserRespository userRepository) {
        AuthUtils.userRepository = userRepository;
    }
    
    /**
     * Get current authenticated user from security context
     * @return User object if authenticated, null otherwise
     */
    public static User getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            System.out.println("🔍 [DEBUG] Authentication check - exists: " + (authentication != null));
            
            if (authentication != null && authentication.isAuthenticated()) {
                Object principal = authentication.getPrincipal();
                System.out.println("🔍 [DEBUG] Principal type: " + principal.getClass().getSimpleName());
                
                if (principal instanceof User) {
                    User user = (User) principal;
                    System.out.println("✅ [DEBUG] User found via principal - ID: " + user.getId() + ", Email: " + user.getEmail());
                    return user;
                } else if (principal instanceof String) {
                    // Handle case where principal is email string
                    String email = (String) principal;
                    System.out.println("🔍 [DEBUG] Principal is email string: " + email);
                    
                    if (userRepository != null) {
                        var userOpt = userRepository.findByEmail(email);
                        if (userOpt.isPresent()) {
                            User user = userOpt.get();
                            System.out.println("✅ [DEBUG] User found via email lookup - ID: " + user.getId() + ", Email: " + user.getEmail());
                            return user;
                        } else {
                            System.out.println("❌ [DEBUG] User not found in database for email: " + email);
                        }
                    } else {
                        System.out.println("❌ [DEBUG] UserRepository is null, cannot lookup user by email");
                    }
                } else {
                    System.out.println("❌ [DEBUG] Unknown principal type: " + principal.getClass());
                }
            } else {
                System.out.println("❌ [DEBUG] Authentication is null or not authenticated");
            }
        } catch (Exception e) {
            System.err.println("❌ Error getting current user: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("❌ [DEBUG] Returning null user");
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
