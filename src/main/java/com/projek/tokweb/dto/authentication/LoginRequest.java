package com.projek.tokweb.dto.authentication;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoginRequest {
    @NotBlank(message = "Email Anda Wajib Di Isi")
    @Email(message = "Format Email Tidak Valid")
    private String email;
    @NotBlank(message = "Password Anda Wajib Di Isi")
    @Size(min = 6,message = "Password Minimal 6 karakter")
    private String password;
}
