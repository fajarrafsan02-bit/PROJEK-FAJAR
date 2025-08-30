package com.projek.tokweb.service.customer;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projek.tokweb.dto.customer.CartItemDto;
import com.projek.tokweb.dto.customer.CartResponseDto;
import com.projek.tokweb.models.admin.Product;
import com.projek.tokweb.models.customer.Cart;
import com.projek.tokweb.models.customer.CartItem;
import com.projek.tokweb.models.User;
import com.projek.tokweb.repository.admin.ProductRepository;
import com.projek.tokweb.repository.customer.CartItemRepository;
import com.projek.tokweb.repository.customer.CartRepository;
import com.projek.tokweb.repository.UserRespository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CartService {
    
    @Autowired
    private CartRepository cartRepository;
    
    @Autowired
    private CartItemRepository cartItemRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private UserRespository userRepository;
    
    @Transactional
    public CartResponseDto addToCart(Long userId, Long productId, Integer quantity) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));
            
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Produk tidak ditemukan"));
            
            // Cari atau buat cart untuk user
            Cart cart = cartRepository.findByUserId(userId)
                    .orElseGet(() -> {
                        Cart newCart = Cart.builder()
                                .user(user)
                                .build();
                        return cartRepository.save(newCart);
                    });
            
            // Cek apakah produk sudah ada di cart
            CartItem existingItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId);
            
            if (existingItem != null) {
                // Update quantity
                existingItem.setQuantity(existingItem.getQuantity() + quantity);
                existingItem.setPriceAtTime(product.getFinalPrice());
                cartItemRepository.save(existingItem);
            } else {
                // Buat item baru
                CartItem newItem = CartItem.builder()
                        .cart(cart)
                        .product(product)
                        .quantity(quantity)
                        .priceAtTime(product.getFinalPrice())
                        .build();
                cartItemRepository.save(newItem);
            }
            
            return getCartResponse(userId);
            
        } catch (Exception e) {
            log.error("Error adding to cart: {}", e.getMessage());
            throw new RuntimeException("Gagal menambahkan ke keranjang: " + e.getMessage());
        }
    }
    
    @Transactional
    public CartResponseDto updateCartItemQuantity(Long userId, Long productId, Integer quantity) {
        try {
            Cart cart = cartRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Keranjang tidak ditemukan"));
            
            CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId);
            if (item == null) {
                throw new RuntimeException("Item tidak ditemukan di keranjang");
            }
            
            if (quantity <= 0) {
                cartItemRepository.delete(item);
            } else {
                // Refresh harga produk dari database untuk memastikan harga selalu up-to-date
                Product currentProduct = productRepository.findById(productId)
                        .orElseThrow(() -> new RuntimeException("Produk tidak ditemukan"));
                
                item.setQuantity(quantity);
                item.setPriceAtTime(currentProduct.getFinalPrice()); // Update harga ke harga terbaru
                cartItemRepository.save(item);
            }
            
            return getCartResponse(userId);
            
        } catch (Exception e) {
            log.error("Error updating cart item: {}", e.getMessage());
            throw new RuntimeException("Gagal update keranjang: " + e.getMessage());
        }
    }
    
    @Transactional
    public CartResponseDto removeFromCart(Long userId, Long productId) {
        try {
            Cart cart = cartRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Keranjang tidak ditemukan"));
            
            cartItemRepository.deleteByCartIdAndProductId(cart.getId(), productId);
            
            return getCartResponse(userId);
            
        } catch (Exception e) {
            log.error("Error removing from cart: {}", e.getMessage());
            throw new RuntimeException("Gagal menghapus dari keranjang: " + e.getMessage());
        }
    }
    
    @Transactional
    public CartResponseDto getCart(Long userId) {
        return getCartResponse(userId);
    }
    
    @Transactional
    public void clearCart(Long userId) {
        try {
            Cart cart = cartRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Keranjang tidak ditemukan"));
            
            cartItemRepository.deleteAll(cart.getItems());
            
        } catch (Exception e) {
            log.error("Error clearing cart: {}", e.getMessage());
            throw new RuntimeException("Gagal mengosongkan keranjang: " + e.getMessage());
        }
    }
    
    private CartResponseDto getCartResponse(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElse(null);
        
        if (cart == null || cart.getItems().isEmpty()) {
            return CartResponseDto.builder()
                    .items(List.of())
                    .totalItems(0)
                    .totalPrice(0.0)
                    .build();
        }
        
        List<CartItemDto> items = cart.getItems().stream()
                .map(this::mapToDto)
                .toList();
        
        return CartResponseDto.builder()
                .cartId(cart.getId())
                .items(items)
                .totalItems(cart.getTotalItems())
                .totalPrice(cart.getTotalPrice())
                .build();
    }
    
    private CartItemDto mapToDto(CartItem item) {
        return CartItemDto.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .imageUrl(item.getProduct().getImageUrl())
                .weight(item.getProduct().getWeight())
                .purity(item.getProduct().getPurity())
                .price(item.getPriceAtTime()) // Harga yang disimpan saat ditambahkan ke keranjang
                .priceAtTime(item.getPriceAtTime()) // Harga yang disimpan saat ditambahkan ke keranjang
                .quantity(item.getQuantity())
                .specs(String.format("Berat: %s gram | Kadar: %dK", 
                        item.getProduct().getFormattedWeight(), 
                        item.getProduct().getPurity()))
                .build();
    }
}

