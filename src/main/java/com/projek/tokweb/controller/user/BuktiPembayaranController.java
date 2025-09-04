package com.projek.tokweb.controller.user;

import com.projek.tokweb.models.customer.BuktiPembayaran;
import com.projek.tokweb.repository.customer.OrderRepository;
import com.projek.tokweb.service.customer.BuktiPembayaranService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
public class BuktiPembayaranController {

private final BuktiPembayaranService service;
    private final OrderRepository orderRepository;

    // User upload bukti
    @PostMapping("/checkout/upload-bukti")
    public ResponseEntity<?> uploadBukti(@RequestParam("file") MultipartFile file,
                                        @RequestParam("orderId") Long orderId) {
        try {
            BuktiPembayaran saved = service.saveBukti(orderId, file);
            // tautkan ke order
            com.projek.tokweb.models.customer.Order order = orderRepository.findById(orderId).orElse(null);
            if(order!=null){
                order.setBukti(saved);
                order.setStatus(com.projek.tokweb.models.customer.OrderStatus.PENDING_CONFIRMATION);
                orderRepository.save(order);
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(saved.getId());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Upload gagal: " + e.getMessage());
        }
    }

    // Endpoint untuk admin view bukti sudah dipindahkan ke AdminBuktiController
}

