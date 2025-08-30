package com.projek.tokweb.controller.authentication;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/error")
public class CustomErrorController {
    
    @GetMapping("/403")
    public String halamanAksesDitolak(Model model) {
        model.addAttribute("message", "Maaf, Anda tidak memiliki izin untuk mengakses halaman ini. Silakan login dengan akun yang memiliki akses yang sesuai.");
        return "html/error/halaman-403";
    }
}
