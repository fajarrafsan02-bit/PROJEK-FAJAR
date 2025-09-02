package com.projek.tokweb.security;

import java.io.IOException;
import java.io.PrintWriter;
// import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
// import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.projek.tokweb.models.User;
import com.projek.tokweb.repository.UserRespository;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final UserRespository userRespository;

    @Override
    @SuppressWarnings("null")
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();

        if (path.startsWith("/auth") ||
                path.startsWith("/css") ||
                path.startsWith("/js") ||
                path.startsWith("/assets") ||
                path.startsWith("/validasi") ||
                path.startsWith("/gold-price")) {

            filterChain.doFilter(request, response);
            return;
        }

        String token = null;

        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("jwt".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        if (token != null && !token.trim().isEmpty()) {
            try {
                String email = jwtUtil.extractEmail(token);
                Optional<User> userOpt = userRespository.findByEmail(email);

                if (userOpt.isPresent()) {

                    User user = userOpt.get();

                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(user, null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (ExpiredJwtException e) {
                System.out.println("Token Ambo Kardaluarsa " + e.getMessage());
                Cookie expiredCookie = new Cookie("jwt", "");
                expiredCookie.setPath("/");
                expiredCookie.setMaxAge(0);
                response.addCookie(expiredCookie);

                response.setContentType("text/html");
                response.setCharacterEncoding("UTF-8");
                PrintWriter out = response.getWriter();

                out.println("<!DOCTYPE html>");
                out.println("<html lang='id'>");
                out.println("<head>");
                out.println("    <meta charset='UTF-8'>");
                out.println("    <meta name='viewport' content='width=device-width, initial-scale=1.0'>");
                out.println("    <title>Session Expired</title>");
                out.println("    <script src='https://cdn.jsdelivr.net/npm/sweetalert2@11'></script>");
                out.println("    <style>");
                out.println("        body {");
                out.println(
                        "            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;");
                out.println("            background: #f8fafc;");
                out.println("            min-height: 100vh;");
                out.println("            margin: 0;");
                out.println("            display: flex;");
                out.println("            align-items: center;");
                out.println("            justify-content: center;");
                out.println("        }");
                out.println("        .swal2-popup {");
                out.println("            border-radius: 12px !important;");
                out.println("            border: 1px solid #e2e8f0 !important;");
                out.println("            box-shadow: 0 4px 20px rgba(59, 130, 246, 0.1) !important;");
                out.println("        }");
                out.println("        .swal2-title {");
                out.println("            color: #1e40af !important;");
                out.println("            font-size: 1.3rem !important;");
                out.println("            font-weight: 600 !important;");
                out.println("        }");
                out.println("        .swal2-html-container {");
                out.println("            color: #475569 !important;");
                out.println("            font-size: 0.95rem !important;");
                out.println("            line-height: 1.5 !important;");
                out.println("        }");
                out.println("        .swal2-confirm {");
                out.println("            background: #3b82f6 !important;");
                out.println("            border: none !important;");
                out.println("            border-radius: 8px !important;");
                out.println("            font-weight: 500 !important;");
                out.println("            padding: 12px 24px !important;");
                out.println("            transition: all 0.2s ease !important;");
                out.println("        }");
                out.println("        .swal2-confirm:hover {");
                out.println("            background: #2563eb !important;");
                out.println("            transform: translateY(-1px) !important;");
                out.println("        }");
                out.println("        .swal2-icon.swal2-warning {");
                out.println("            border-color: #3b82f6 !important;");
                out.println("            color: #3b82f6 !important;");
                out.println("        }");
                out.println("    </style>");
                out.println("</head>");
                out.println("<body>");
                out.println("    <script>");
                out.println("        Swal.fire({");
                out.println("            icon: 'warning',");
                out.println("            title: 'Sesi Berakhir',");
                out.println(
                        "            html: 'Maaf, token Anda sudah expired.<br>Silakan login kembali untuk melanjutkan.',");
                out.println("            confirmButtonText: 'Login Sekarang',");
                out.println("            allowOutsideClick: false,");
                out.println("            allowEscapeKey: false");
                out.println("        }).then((result) => {");
                out.println("            if (result.isConfirmed) {");
                out.println("                window.location.href = '/auth/login';");
                out.println("            }");
                out.println("        });");
                out.println("    </script>");
                out.println("</body>");
                out.println("</html>");
                return;
            } catch (Exception e) {
                System.out.println("JWT filter anda Error" + e.getMessage());
                request.getSession().setAttribute("errorLogin", "Jwt Filter Anda Error, Silahkan Login Ulang");
                response.sendRedirect("/auth/login");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

}
