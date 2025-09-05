package com.projek.tokweb.controller.publics;

import com.projek.tokweb.dto.admin.BestSellingProductDto;
import com.projek.tokweb.service.admin.BestSellingProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/public/api/best-selling")
@CrossOrigin(origins = "*")
public class PublicBestSellingProductController {

    @Autowired
    private BestSellingProductService bestSellingProductService;

    @GetMapping("/top")
    public ResponseEntity<Map<String, Object>> getTopBestSellingProducts(
            @RequestParam(defaultValue = "5") int limit) {
        try {
            List<BestSellingProductDto> products = bestSellingProductService.getTopBestSellingProducts(limit);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", products);
            response.put("message", "Berhasil mengambil data produk terlaris");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Gagal mengambil data produk terlaris: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}