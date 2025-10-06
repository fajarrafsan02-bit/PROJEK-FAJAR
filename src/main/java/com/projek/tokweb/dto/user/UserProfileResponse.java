package com.projek.tokweb.dto.user;

import java.time.LocalDateTime;

import com.projek.tokweb.models.Role;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UserProfileResponse {
    Long id;
    String namaLengkap;
    String email;
    String nomorHp;
    Role role;
    Boolean terferifikasi;
    LocalDateTime waktuBuat;
}
