package com.projek.tokweb.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.projek.tokweb.models.User;

public interface UserRespository extends JpaRepository<User, Long>{
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByNomorHp(String nomorHp);
}
