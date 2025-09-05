package com.projek.tokweb.controller.user;


import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.projek.tokweb.service.admin.ProductService;
import com.projek.tokweb.dto.ApiResponse;
import com.projek.tokweb.dto.admin.ProductResponseDto;
import com.projek.tokweb.models.User;
import com.projek.tokweb.repository.UserRespository;
import com.projek.tokweb.utils.AuthUtils;
// import com.projek.tokweb.model.User;
// import com.projek.tokweb.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
            // Menggunakan AuthUtils untuk mendapatkan current user dari security context
            User currentUser = AuthUtils.getCurrentUser();
            
            if (currentUser == null) {
                // Jika tidak ada user yang login, kembalikan error
                System.out.println("❌ No authenticated user found");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("User tidak terautentikasi"));
            }
            
            System.out.println("✅ Authenticated user found: " + currentUser.getEmail() + ", ID: " + currentUser.getId());
            return ResponseEntity.ok(ApiResponse.success("User berhasil diambil", currentUser));
            
        } catch (Exception e) {
            System.err.println("Error getting current user: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Gagal mengambil user: " + e.getMessage()));
        }
    }
    
    @Operation(summary = "Get Active Products for User", description = "Mendapatkan produk aktif untuk user (tanpa perlu auth admin)")
    @GetMapping("/products/active")
    public ResponseEntity<?> getActiveProductsForUser(
            @Parameter(description = "Nomor halaman", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Jumlah item per halaman", example = "10") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Field untuk sorting", example = "name") @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Arah sorting (asc/desc)", example = "desc") @RequestParam(defaultValue = "desc") String sortDirection) {

        try {
            // Validasi input
            if (page < 0) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Nomor halaman tidak boleh negatif"));
            }
            
            if (size <= 0 || size > 100) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Ukuran halaman harus antara 1-100"));
            }
            
            if (!sortDirection.equalsIgnoreCase("asc") && !sortDirection.equalsIgnoreCase("desc")) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Arah sorting harus 'asc' atau 'desc'"));
            }
            
            Page<ProductResponseDto> productPage = productService
                    .getAllActiveProductsWithPaginationAndFormattedResponse(page, size, sortBy, sortDirection);

            Map<String, Object> response = new HashMap<>();
            response.put("content", productPage.getContent());
            response.put("currentPage", productPage.getNumber());
            response.put("totalPages", productPage.getTotalPages());
            response.put("totalItems", productPage.getTotalElements());
            response.put("size", productPage.getSize());
            response.put("hasNext", productPage.hasNext());
            response.put("hasPrevious", productPage.hasPrevious());

            String message = productPage.isEmpty() 
                ? "Belum ada produk aktif tersedia" 
                : "Data produk aktif berhasil diambil";
                
            return ResponseEntity.ok(ApiResponse.success(message, response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Gagal mengambil data produk: " + e.getMessage()));
        }
    }
    
    @Operation(summary = "Search Active Products for User", description = "Mencari produk aktif berdasarkan keyword untuk user")
    @GetMapping("/products/search")
    public ResponseEntity<?> searchActiveProductsForUser(
            @Parameter(description = "Keyword pencarian", example = "cincin") @RequestParam(required = false) String keyword,
            @Parameter(description = "Nomor Halaman (dimulai dari 0)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Jumlah Item Per Halaman", example = "10") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Field untuk sorting", example = "name") @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Arah sorting (asc/desc)", example = "desc") @RequestParam(defaultValue = "desc") String sortDirection) {
        try {
            // Validasi input
            if (page < 0) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Nomor halaman tidak boleh negatif"));
            }

            if (size <= 0 || size > 100) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Ukuran halaman harus antara 1-100"));
            }

            if (!sortDirection.equalsIgnoreCase("asc") && !sortDirection.equalsIgnoreCase("desc")) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Arah sorting harus 'asc' atau 'desc'"));
            }

            // Gunakan service yang sama tapi hanya untuk produk aktif
            Page<ProductResponseDto> productPage = productService.searchActiveProductsWithPaginationAndFormattedResponse(
                    keyword, page, size, sortBy, sortDirection);

            Map<String, Object> response = new HashMap<>();
            response.put("content", productPage.getContent());
            response.put("currentPage", productPage.getNumber());
            response.put("totalPages", productPage.getTotalPages());
            response.put("totalItems", productPage.getTotalElements());
            response.put("size", productPage.getSize());
            response.put("hasNext", productPage.hasNext());
            response.put("hasPrevious", productPage.hasPrevious());
            response.put("keyword", keyword);

            String message = productPage.getContent().isEmpty()
                    ? (keyword != null && !keyword.trim().isEmpty()
                            ? "Tidak ada produk yang ditemukan dengan keyword: '" + keyword + "'"
                            : "Tidak ada produk aktif yang ditemukan")
                    : "Data pencarian produk berhasil diambil";

            return ResponseEntity.ok(ApiResponse.success(message, response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Gagal mencari produk: " + e.getMessage()));
        }
    }
}
