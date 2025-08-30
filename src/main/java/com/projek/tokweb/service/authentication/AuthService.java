package com.projek.tokweb.service.authentication;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.projek.tokweb.dto.authentication.LoginRequest;
import com.projek.tokweb.dto.authentication.RegisterRequest;
import com.projek.tokweb.models.PasswordResetToken;
import com.projek.tokweb.models.Role;
import com.projek.tokweb.models.User;
import com.projek.tokweb.repository.PasswordResetTokenRepository;
import com.projek.tokweb.repository.UserRespository;
import com.projek.tokweb.security.JwtUtil;

import jakarta.mail.MessagingException;

@Service
public class AuthService {
    @Autowired
    private UserRespository userRespository;
    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private EmailService emailService;

    public void register(RegisterRequest request) {
        User user = User.builder()
                .namaLengkap(request.getNamaDepan() + " " + request.getNamaBelakang())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nomorHp(request.getNomorHp())
                .terferifikasi(true).build();
        userRespository.save(user);
    }

    public String login(LoginRequest request) throws MessagingException {
        User user = userRespository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    return new UsernameNotFoundException("Email Anda tidak ditemukan!");
                });
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Password Anda Salah!");
        }
        if (user.getRole() == Role.ADMIN) {
            String token = jwtUtil.generateToken(user);
            emailService.kirimKodeToken(user.getEmail(), token);
            return token;
        } else {
            String token = jwtUtil.generateToken(user);
            return token;
        }
    }

    public Map<String, Object> verifikasiToken(String token) {
        try {
            String email = jwtUtil.extractEmail(token);
            Optional<User> userOpt = userRespository.findByEmail(email);

            if (userOpt.isEmpty()) {
                return Map.of("success", false, "message", "Token tidak valid");
            }

            User user = userOpt.get();
            return Map.of(
                    "success", true,
                    "message", "Token valid",
                    "user", user,
                    "role", user.getRole().name());
        } catch (Exception e) {
            return Map.of("success", false, "message", "Token tidak valid atau sudah kadaluarsa");
        }
    }

    public boolean emailSudahTerdaftar(String email) {
        return userRespository.existsByEmail(email);
    }

    public boolean nomorHPSudahTerdaftar(String nomorHp) {
        return userRespository.existsByNomorHp(nomorHp);
    }

    public Map<String, Object> resetPasswordRequest(String email) throws MessagingException {
        Optional<User> userOpt = userRespository.findByEmail(email);

        if (userOpt.isEmpty()) {
            return Map.of("success", false, "message", "Email Tidak Terdaftar.");
        }
        User user = userOpt.get();

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiredAt(Instant.now().plus(Duration.ofMinutes(15))).build();
        passwordResetTokenRepository.save(resetToken);

        String link = "http://localhost:8080/auth/reset-password?token=" + token;
        emailService.kirimResetPasswordLink(user.getEmail(), link);

        return Map.of("success", true, "message", "Link reset sudah dikirim");
    }
}
