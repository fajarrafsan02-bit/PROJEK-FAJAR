package com.projek.tokweb.controller.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.projek.tokweb.dto.ApiResponse;
import com.projek.tokweb.dto.customer.CartResponseDto;
import com.projek.tokweb.dto.customer.CartItemDto;
import com.projek.tokweb.service.customer.CartService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

@RestController
@RequestMapping("/api/cart")
public class CartControllerApi {
    
    @Autowired
    private CartService cartService;
    
    @Operation(summary = "Get Cart", description = "Mendapatkan isi keranjang user")
    @GetMapping
    public ResponseEntity<?> getCart(@Parameter(description = "ID user") @RequestParam Long userId) {
        try {
            CartResponseDto cart = cartService.getCart(userId);
            return ResponseEntity.ok(ApiResponse.success("Keranjang berhasil diambil", cart));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Gagal mengambil keranjang: " + e.getMessage()));
        }
    }
    
    @Operation(summary = "Add to Cart", description = "Menambahkan produk ke keranjang")
    @PostMapping("/add")
    public ResponseEntity<?> addToCart(@RequestBody CartItemDto cartItemDto) {
        try {
            CartResponseDto cart = cartService.addToCart(
                cartItemDto.getUserId(), 
                cartItemDto.getProductId(), 
                cartItemDto.getQuantity()
            );
            return ResponseEntity.ok(ApiResponse.success("Produk berhasil ditambahkan ke keranjang", cart));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Gagal menambahkan ke keranjang: " + e.getMessage()));
        }
    }
    
    @Operation(summary = "Update Cart Item", description = "Update quantity item di keranjang")
    @PutMapping("/update")
    public ResponseEntity<?> updateCartItem(
            @Parameter(description = "ID user") @RequestParam Long userId,
            @Parameter(description = "ID produk") @RequestParam Long productId,
            @Parameter(description = "Quantity baru") @RequestParam Integer quantity) {
        try {
            CartResponseDto cart = cartService.updateCartItemQuantity(userId, productId, quantity);
            return ResponseEntity.ok(ApiResponse.success("Keranjang berhasil diupdate", cart));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Gagal update keranjang: " + e.getMessage()));
        }
    }
    
    @Operation(summary = "Remove from Cart", description = "Menghapus produk dari keranjang")
    @DeleteMapping("/remove")
    public ResponseEntity<?> removeFromCart(
            @Parameter(description = "ID user") @RequestParam Long userId,
            @Parameter(description = "ID produk") @RequestParam Long productId) {
        try {
            CartResponseDto cart = cartService.removeFromCart(userId, productId);
            return ResponseEntity.ok(ApiResponse.success("Produk berhasil dihapus dari keranjang", cart));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Gagal menghapus dari keranjang: " + e.getMessage()));
        }
    }
    
    @Operation(summary = "Clear Cart", description = "Mengosongkan keranjang")
    @DeleteMapping("/clear")
    public ResponseEntity<?> clearCart(@Parameter(description = "ID user") @RequestParam Long userId) {
        try {
            cartService.clearCart(userId);
            return ResponseEntity.ok(ApiResponse.success("Keranjang berhasil dikosongkan", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Gagal mengosongkan keranjang: " + e.getMessage()));
        }
    }
}

