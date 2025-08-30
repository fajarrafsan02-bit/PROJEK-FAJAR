package com.projek.tokweb.controller.user;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.projek.tokweb.service.admin.ProductService;
import com.projek.tokweb.dto.ApiResponse;
import com.projek.tokweb.models.User;
import com.projek.tokweb.repository.UserRespository;
// import com.projek.tokweb.model.User;
// import com.projek.tokweb.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;


@RestController
@RequestMapping("/user")
public class UserControllerApi {

    @Autowired 
    private ProductService productService;
    
    @Autowired
    private UserRespository userRepository;
    
    @Operation(summary = "Get Current User", description = "Mendapatkan informasi user yang sedang login")
    @GetMapping("/current")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
        try {
            // Untuk sementara, ambil user pertama (untuk testing)
            // Nanti bisa diganti dengan sistem auth yang proper
            User user = userRepository.findAll().stream().findFirst().orElse(null);
            if (user == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Tidak ada user tersedia"));
            }
            
            return ResponseEntity.ok(ApiResponse.success("User berhasil diambil", user));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Gagal mengambil user: " + e.getMessage()));
        }
    }
}
