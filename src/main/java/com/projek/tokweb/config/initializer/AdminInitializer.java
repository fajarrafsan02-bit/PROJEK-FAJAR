package com.projek.tokweb.config.initializer;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.projek.tokweb.models.Role;
import com.projek.tokweb.models.User;
import com.projek.tokweb.repository.UserRespository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AdminInitializer {
    private final UserRespository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    public void iniAdminUser() {
        String emailAdmin = "fajar.rafsan02@gmail.com";

        if (userRepository.findByEmail(emailAdmin).isEmpty()) {
            User admin = User.builder()
                    .namaLengkap("Admin")
                    .email(emailAdmin)
                    .password(passwordEncoder.encode("Admin123"))
                    .nomorHp("081286196886")
                    .role(Role.ADMIN)
                    .terferifikasi(true)
                    .build();
            userRepository.save(admin);
            System.out.println(">> Admin Berhasil Dibuat: " + emailAdmin);
        } else {
            System.out.println(">> Admin sudah terdaftar: " + emailAdmin);
        }
    }
}
