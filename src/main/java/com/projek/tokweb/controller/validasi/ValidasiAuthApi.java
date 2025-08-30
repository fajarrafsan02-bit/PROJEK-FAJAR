package com.projek.tokweb.controller.validasi;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.projek.tokweb.service.ValidasiService;
import com.projek.tokweb.service.authentication.AuthService;

@RestController
@RequestMapping("/validasi")
public class ValidasiAuthApi {
    @Autowired
    private AuthService authService;
    @Autowired
    private ValidasiService validasiService;

    @GetMapping("/cek-email")
    @ResponseBody
    public ResponseEntity<?> validasiEmail(@RequestParam String email) {
        boolean emailExists = authService.emailSudahTerdaftar(email);
        return ResponseEntity.ok(emailExists);
    }

    @GetMapping("/cek-daftar-hp")
    @ResponseBody
    public ResponseEntity<?> validasiNomorHp(@RequestParam String nomorHp) {
        boolean nomorHpExists = authService.nomorHPSudahTerdaftar(nomorHp);
        return ResponseEntity.ok(nomorHpExists);
    }

    @GetMapping("/cek-nomor-hp")
    public ResponseEntity<?> cekNomorHp(@RequestParam String nomorHp) {

        try {
            boolean valid = validasiService.nomorHandphoneValid(nomorHp);
            return ResponseEntity.ok(valid);
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", e.getMessage()));
        }
    }

}
