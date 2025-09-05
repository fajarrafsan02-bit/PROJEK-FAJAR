package com.projek.tokweb.controller.authentication;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.projek.tokweb.dto.authentication.EmailRequestDto;
import com.projek.tokweb.dto.authentication.LoginRequest;
import com.projek.tokweb.dto.authentication.RegisterRequest;
import com.projek.tokweb.models.PasswordResetToken;
import com.projek.tokweb.models.Role;
import com.projek.tokweb.models.User;
import com.projek.tokweb.repository.PasswordResetTokenRepository;
import com.projek.tokweb.repository.UserRespository;
import com.projek.tokweb.security.JwtUtil;
import com.projek.tokweb.service.authentication.AuthService;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/auth")
public class AuthControllerApi {

    @Autowired
    AuthService authService;
    @Autowired
    UserRespository userRespository;
    @Autowired
    JwtUtil jwtUtil;
    @Autowired
    PasswordResetTokenRepository passwordResetTokenRepository;
    @Autowired
    PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    @ResponseBody
    public ResponseEntity<?> handleLogin(@RequestBody LoginRequest loginRequest, HttpSession session,
            HttpServletResponse response) {
        try {
            System.out.println("INI SUDAH POST LOGIN");
            String token = authService.login(loginRequest);
            String email = jwtUtil.extractEmail(token);
            User user = userRespository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User Tidak Di Temukan."));

            if (user.getRole() == Role.ADMIN) {

                session.setAttribute("email", email);
                return ResponseEntity.ok(Map.of(
                        "status", "token-required",
                        "pesan", "Kode Token Di Kirim Ke Email"));
            } else {

                ResponseCookie cookie = ResponseCookie.from("jwt", token)
                        .httpOnly(true)
                        .secure(false)
                        .path("/")
                        .maxAge(Duration.ofHours(1))
                        .sameSite("Lax")
                        .build();
                return ResponseEntity.ok()
                        .header(HttpHeaders.SET_COOKIE, cookie.toString())
                        .body(Map.of("status", "success"));
            }
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Email Anda Tidak di temukan / Belum Terdaftar"));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Password Anda Salah"));
        } catch (MessagingException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Gagal Mengirim Token Ke Email anda"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Terjadi Kesalahan Saat Login"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> handleRegister(@ModelAttribute("registerRequest") @Valid RegisterRequest request,
            BindingResult result) {
        try {
            // Check for validation errors first
            if (result.hasErrors()) {
                Map<String, String> errors = new HashMap<>();
                result.getFieldErrors().forEach(error -> {
                    errors.put(error.getField(), error.getDefaultMessage());
                });
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Data tidak valid",
                    "errors", errors
                ));
            }
            
            // Call register service
            Map<String, Object> registerResult = authService.register(request);
            
            if ((Boolean) registerResult.get("success")) {
                return ResponseEntity.ok(registerResult);
            } else {
                return ResponseEntity.badRequest().body(registerResult);
            }
            
        } catch (Exception e) {
            System.err.println("Registration error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Terjadi kesalahan pada server. Silakan coba lagi."
            ));
        }
    }

    @PostMapping("/verifikasi-token")
    public ResponseEntity<?> verifikasiToken(@RequestParam("token") String token, HttpSession session,
            HttpServletResponse httpServletResponse) {
        System.out.println("=== DEBUG VERIFIKASI TOKEN ===");
        System.out.println("INI TOKEN SAYA : " + token);
        System.out.println("Panjang token: " + (token != null ? token.length() : "null"));
        
        // Check token format
        if (token != null) {
            String[] parts = token.split("\\.");
            System.out.println("Jumlah bagian token: " + parts.length);
            for (int i = 0; i < parts.length; i++) {
                System.out.println("Bagian " + i + " panjang: " + parts[i].length());
            }
        }
        
        String emailDiSession = (String) session.getAttribute("email");
        System.out.println("Email di session: " + emailDiSession);

        if (token == null || token.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Token tidak boleh kosong."));
        }

        try {
            String emailDariToken = jwtUtil.extractEmail(token);
            System.out.println("Email dari token: " + emailDariToken);

            if (emailDariToken != null && emailDariToken.equals(emailDiSession)) {
                // Validasi tambahan
                if (!jwtUtil.validateToken(token, emailDiSession)) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                            "success", false,
                            "message", "Token tidak valid atau sudah kedaluwarsa."));
                }
                
                Cookie cookie = new Cookie("jwt", token);
                cookie.setHttpOnly(true);
                cookie.setMaxAge(86400);
                cookie.setPath("/");
                httpServletResponse.addCookie(cookie);

                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "pesan", "Token Valid",
                        "redirect", "/admin/home"));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "success", false,
                        "message", "Token Yang Masukkan Salah."));
            }
        } catch (ExpiredJwtException e) {
            System.err.println("Token expired: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "Token Anda sudah kedaluwarsa"));
        } catch (MalformedJwtException e) {
            System.err.println("Malformed token: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Format token tidak valid. Token rusak atau tidak lengkap."));
        } catch (IllegalArgumentException e) {
            System.err.println("Illegal argument: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Token tidak valid atau kosong."));
        } catch (Exception e) {
            System.err.println("Unexpected error during token verification: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Terjadi kesalahan pada token Anda: " + e.getMessage()));
        }
    }

    @PostMapping("/reset-password-request")
    public ResponseEntity<?> resetPasswordReset(@RequestBody EmailRequestDto emailRequest) {
        try {
            Map<String, Object> hasil = authService.resetPasswordRequest(emailRequest.getEmail());
            return ResponseEntity.ok(hasil);
        } catch (MessagingException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Gagal mengirim email reset password."));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPasswordBaru(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String passwordBaru = request.get("passwordBaru");

        if (token == null || passwordBaru == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Token dan Password Baru baru harus di isi"));
        }

        if (passwordBaru.length() < 8) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Password harus minimal 8 karakter"));
        }

        if (passwordBaru.toLowerCase().contains("password")) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Password tidak boleh mengandung kata 'password'"));
        }

        try {
            Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository.findByToken(token);
            if (tokenOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Token reset password tidak valid"));
            }

            PasswordResetToken resetToken = tokenOpt.get();
            if (resetToken.getExpiredAt().isBefore(Instant.now())) {
                passwordResetTokenRepository.delete(resetToken);
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Token reset password sudah kedaluwarsa"));
            }

            User user = resetToken.getUser();
            user.setPassword(passwordEncoder.encode(passwordBaru));
            userRespository.save(user);

            passwordResetTokenRepository.delete(resetToken);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Password berhasil direset"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Terjadi kesalahan saat mereset password"));
        }

    }

}
