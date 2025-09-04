package com.projek.tokweb.controller.admin;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.projek.tokweb.models.customer.BuktiPembayaran;
import com.projek.tokweb.service.customer.BuktiPembayaranService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminBuktiController {

    private final BuktiPembayaranService buktiPembayaranService;

    /**
     * Endpoint untuk menampilkan gambar bukti pembayaran berdasarkan ID bukti
     * URL: /admin/bukti/{buktiId}
     */
    @GetMapping("/bukti/{buktiId}")
    public ResponseEntity<byte[]> getBuktiPembayaranImage(@PathVariable Long buktiId) {
        try {
            System.out.println("🖼️ Getting payment proof image for bukti ID: " + buktiId);
            
            if (buktiId == null || buktiId <= 0) {
                System.out.println("❌ Invalid bukti ID: " + buktiId);
                return ResponseEntity.badRequest().build();
            }
            
            BuktiPembayaran bukti = buktiPembayaranService.getById(buktiId);
            
            if (bukti == null) {
                System.out.println("❌ No payment proof found for bukti ID: " + buktiId);
                return ResponseEntity.notFound().build();
            }
            
            if (bukti.getFileData() == null || bukti.getFileData().length == 0) {
                System.out.println("❌ File data is null or empty for bukti ID: " + buktiId);
                return ResponseEntity.notFound().build();
            }
            
            // Set appropriate headers for image display
            HttpHeaders headers = new HttpHeaders();
            
            // Determine content type
            String contentType = bukti.getContentType();
            if (contentType == null || contentType.trim().isEmpty()) {
                // Default to jpeg if no content type
                contentType = "image/jpeg";
                System.out.println("⚠️ No content type found, defaulting to: " + contentType);
            }
            
            try {
                headers.setContentType(MediaType.parseMediaType(contentType));
            } catch (Exception ex) {
                System.out.println("⚠️ Invalid content type '" + contentType + "', defaulting to image/jpeg");
                headers.setContentType(MediaType.IMAGE_JPEG);
            }
            
            // Set content disposition for inline display
            String fileName = bukti.getFileName();
            if (fileName == null || fileName.trim().isEmpty()) {
                fileName = "bukti_pembayaran_" + buktiId + ".jpg";
            }
            headers.add(HttpHeaders.CONTENT_DISPOSITION, 
                "inline; filename=\"" + fileName + "\"");
            
            headers.setContentLength(bukti.getFileData().length);
            
            System.out.println("✅ Serving payment proof image for bukti ID " + buktiId + 
                             " - Size: " + bukti.getFileData().length + " bytes, Type: " + contentType + 
                             ", FileName: " + fileName);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(bukti.getFileData());
                    
        } catch (Exception e) {
            System.out.println("❌ Error serving payment proof image for bukti ID " + buktiId + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}
