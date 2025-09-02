package com.projek.tokweb.controller.admin;

import com.projek.tokweb.models.User;
import com.projek.tokweb.utils.AuthUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("admin")
public class AdminController {
    
 
    @GetMapping("/home")
    public String homeAdmin(Model model, RedirectAttributes redirectAttributes) {
        // Check if user is authenticated and is admin
        if (!AuthUtils.isAuthenticated()) {
            redirectAttributes.addFlashAttribute("error", "Anda harus login terlebih dahulu");
            return "redirect:/auth/login";
        }
        
        if (!AuthUtils.isAdmin()) {
            redirectAttributes.addFlashAttribute("error", "Akses ditolak. Hanya admin yang dapat mengakses halaman ini.");
            return "redirect:/auth/login";
        }
        
        // Get current user information
        User currentUser = AuthUtils.getCurrentUser();
        if (currentUser != null) {
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("userName", currentUser.getNamaLengkap());
            model.addAttribute("userEmail", currentUser.getEmail());
            model.addAttribute("userRole", "Administrator");
        }
        
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
