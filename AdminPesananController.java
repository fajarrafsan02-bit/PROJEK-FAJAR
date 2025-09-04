package com.projek.tokweb.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminPesananController {

    @GetMapping("/pesanan")
    public String showAdminOrders() {
        return "html/admin/manajemen-pesanan";
    }
    
    // Jika Anda punya endpoint lain untuk pesanan
    @GetMapping("/orders")
    public String showOrders() {
        return "html/admin/manajemen-pesanan";
    }
}
