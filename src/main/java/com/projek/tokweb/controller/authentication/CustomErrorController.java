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
        model.addAttribute("error", "403");
        model.addAttribute("title", "Akses Ditolak");
        model.addAttribute("message", "Maaf, Anda tidak memiliki izin untuk mengakses halaman ini. Silakan login dengan akun yang memiliki akses yang sesuai.");
        return "html/error/halaman-403";
    }
    
    @GetMapping("/404")
    public String halamanTidakDitemukan(Model model) {
        model.addAttribute("error", "404");
        model.addAttribute("title", "Halaman Tidak Ditemukan");
        model.addAttribute("message", "Halaman yang Anda cari tidak ditemukan.");
        return "html/error/halaman-404";
    }
    
    @GetMapping("/500")
    public String halamanErrorServer(Model model) {
        model.addAttribute("error", "500");
        model.addAttribute("title", "Error Server");
        model.addAttribute("message", "Terjadi kesalahan internal pada server. Silakan coba lagi nanti.");
        return "html/error/halaman-500";
    }
}
