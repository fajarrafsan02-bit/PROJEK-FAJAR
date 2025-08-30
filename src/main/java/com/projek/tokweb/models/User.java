package com.projek.tokweb.models;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class User{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String namaLengkap;
    private String email;
    private String password;
    private String nomorHp;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private Role role = Role.USER;

    @Builder.Default
    private Boolean terferifikasi = false;
    @Builder.Default
    private LocalDateTime waktuBuat = LocalDateTime.now();

    // @Override
    // public String getPassword(){
    //     return this.password;
    // }

    // @Override
    // public String getUsername(){
    //     return this.email;
    // }
    // @Override
    // public boolean isAccountNonExpired(){
    //     return true;
    // }
    // @Override
    // public boolean isAccountNonLocked(){
    //     return true;
    // }
    // @Override
    // public boolean isCredentialsNonExpired(){
    //     return true;
    // }
    // @Override
    // public Collection<? extends GrantedAuthority> getAuthorities() {
    //     return List.of(() -> role.name());
    // }

    // @Override
    // public boolean isEnabled() {
    //     return true;
    // }
}
