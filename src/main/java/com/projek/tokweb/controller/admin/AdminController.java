package com.projek.tokweb.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("admin")
public class AdminController {
    @GetMapping("/home")
    public String homeAdmin() {
        return "html/admin/home-admin";
    }

    @GetMapping("/produk")
    public String manajemenProduk() {
        return "html/admin/manajemen-produk";
    }

    @GetMapping("/pesanan")
    public String manajemenPesanan() {
        return "html/admin/manajemen-pesanan";
    }

    @GetMapping("/laporan")
    public String laporan() {
        return "html/admin/laporan";
    }

    @GetMapping("/harga-emas")
    public String hargaEmas() {
        return "html/admin/harga-emas";
    }
}
