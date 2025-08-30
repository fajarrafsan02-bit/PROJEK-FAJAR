package com.projek.tokweb.controller.authentication;

import java.time.Instant;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.projek.tokweb.dto.authentication.LoginRequest;
import com.projek.tokweb.dto.authentication.RegisterRequest;
import com.projek.tokweb.models.PasswordResetToken;
import com.projek.tokweb.repository.PasswordResetTokenRepository;

// import com.projek.tokweb.security.JwtUtil;
// import com.projek.tokweb.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/auth")
public class AuthController {
    // @Autowired
    // private AuthService authService;
    // @Autowired
    // private JwtUtil jwtUtil;
    // @Autowired
    // private ValidasiService validasiService;
    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @ModelAttribute
    public RegisterRequest registerRequest() {
        return new RegisterRequest();
    }

    @ModelAttribute
    public LoginRequest loginRequest() {
        return new LoginRequest();
    }

    @GetMapping("/register")
    public String tampilanHalamanRegister(Model model) {
        model.addAttribute("formType", "register");
        return "html/authentication/utama/halaman-auth";
    }

    @GetMapping("/login")
    public String halamanLogin(HttpServletRequest request, Model model) {
        model.addAttribute("formType", "login");

        HttpSession session = request.getSession(false);
        if (session != null) {
            Object errorLogin = session.getAttribute("errorLogin");
            if (errorLogin != null) {
                model.addAttribute("errorLogin", errorLogin.toString());
                session.removeAttribute("errorLogin");
            }
        }
        return "html/authentication/utama/halaman-auth";
    }

    @GetMapping("/verifikasi-token")
    public String halamanToken(Model model) {
        return "html/authentication/adminToken/halaman-verifikasi-token";
    }

    @GetMapping("/lupa-password")
    public String halamanLupaPassword(Model model) {
        System.out.println("MASUK FAJAR");
        return "html/halamanLupaPassword/halaman-lupa-password";

    }

    @GetMapping("/open-email-link")
    public String halamanOpenEmailLink() {
        return "html/halamanLupaPassword/konfirmasi-email-reset";
    }

    @GetMapping("/reset-password")
    public String halamanResetPassword(@RequestParam("token") String token, Model model, RedirectAttributes redirect) {
        try {

            Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository.findByToken(token);
            if (tokenOpt.isEmpty()) {
                redirect.addFlashAttribute("error", "Token Reset Password tidak valid atau Sudah Kardaluarsa");
                return "redirect:/auth/lupa-password";
            }
            PasswordResetToken resetToken = tokenOpt.get();
            if (resetToken.getExpiredAt().isBefore(Instant.now())) {
                passwordResetTokenRepository.delete(resetToken);
                redirect.addFlashAttribute("error",
                        "Token Reset Password sudah Kardaluarsa, Silahkan minta link reset baru");
                return "redirect:/auth/lupa-password";
            }
            model.addAttribute("token", token);
            model.addAttribute("email", resetToken.getUser().getEmail());
            return "html/halamanLupaPasswrod/reset-password";
        } catch (Exception e) {
            e.printStackTrace();
            redirect.addFlashAttribute("error", "Terjadi kesalahan saat memproses token reset password");
            return "redirect:/auth/lupa-password";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("jwt", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
        return "redirect:/auth/login";
    }
}
