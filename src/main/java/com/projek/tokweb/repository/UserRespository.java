package com.projek.tokweb.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.projek.tokweb.models.Role;
import com.projek.tokweb.models.User;

public interface UserRespository extends JpaRepository<User, Long>{
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByNomorHp(String nomorHp);
    
    // Method untuk menghitung user yang dibuat setelah tanggal tertentu
    long countByWaktuBuatAfter(LocalDateTime waktuBuat);
    
    // Method untuk menghitung user yang dibuat setelah tanggal tertentu, kecuali role tertentu
    long countByWaktuBuatAfterAndRoleNot(LocalDateTime waktuBuat, Role role);
    
    // Method untuk menghitung user kecuali role tertentu
    long countByRoleNot(Role role);

    long countByWaktuBuatBetweenAndRoleNot(LocalDateTime start, LocalDateTime end, Role role);
}
