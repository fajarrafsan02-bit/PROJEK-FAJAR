package com.projek.tokweb.controller.authentication;

import java.util.Map;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/error")
public class CustomErrorController implements ErrorController {
    
    @RequestMapping
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleError(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        
        if (status != null) {
            Integer statusCode = Integer.valueOf(status.toString());
            
            // Check if this is an API request
            String requestUri = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
            if (requestUri != null && requestUri.contains("/api/")) {
                // Return JSON response for API requests
                return switch (statusCode) {
                    case 403 -> ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of(
                            "success", false,
                            "error", "403",
                            "message", "Akses ditolak. Silakan login dengan akun yang memiliki akses yang sesuai."
                        ));
                    case 404 -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                            "success", false,
                            "error", "404",
                            "message", "Endpoint tidak ditemukan"
                        ));
                    case 405 -> ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                        .body(Map.of(
                            "success", false,
                            "error", "405",
                            "message", "Method tidak diizinkan untuk endpoint ini"
                        ));
                    case 500 -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of(
                            "success", false,
                            "error", "500",
                            "message", "Terjadi kesalahan internal pada server"
                        ));
                    default -> ResponseEntity.status(HttpStatus.valueOf(statusCode))
                        .body(Map.of(
                            "success", false,
                            "error", statusCode.toString(),
                            "message", "Terjadi kesalahan: " + statusCode
                        ));
                };
            }
        }
        
        // Default error response
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of(
                "success", false,
                "error", "500",
                "message", "Terjadi kesalahan pada server"
            ));
    }
    
    @GetMapping("/403")
    public String halamanAksesDitolak(Model model) {
        model.addAttribute("error", "403");
        model.addAttribute("title", "Akses Ditolak");
        model.addAttribute("message", "Maaf, Anda tidak memiliki izin untuk mengakses halaman ini. Silakan login dengan akun yang memiliki akses yang sesuai.");
        return "html/error/halaman-403";
    }
    
    @GetMapping("/404")
    public String halamanTidakDitemukan(Model model) {
        model.addAttribute("error", "404");
        model.addAttribute("title", "Halaman Tidak Ditemukan");
        model.addAttribute("message", "Halaman yang Anda cari tidak ditemukan.");
        return "html/error/halaman-404";
    }
    
    @GetMapping("/500")
    public String halamanErrorServer(Model model) {
        model.addAttribute("error", "500");
        model.addAttribute("title", "Error Server");
        model.addAttribute("message", "Terjadi kesalahan internal pada server. Silakan coba lagi nanti.");
        return "html/error/halaman-500";
    }
}
