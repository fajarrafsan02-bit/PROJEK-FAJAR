package com.projek.tokweb.controller.user;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/user")
public class UserController {
    
    @GetMapping("/home")
    public String homeUser() {
        return "html/user/dashboard-user";
    }
    
    @GetMapping("/katalog")
    public String katalog() {
        return "html/user/katalog";
    }
    
    @GetMapping("/cart")
    public String cart() {
        return "html/user/cart";
    }
    
    @GetMapping("/checkout")
    public String checkout() {
        return "html/user/checkout";
    }
}
