package com.projek.tokweb.service.authentication;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
// import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {
    @Autowired
    private JavaMailSender javaMailSender;

    // @Async
    public void kirimKodeToken(String toEmail, String token) throws MessagingException {

        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(toEmail);
        helper.setSubject("TOKEN Login ADMIN");

        String htmlContent = String.format(
                """
                        <!DOCTYPE html>
                        <html lang="id">
                        <head>
                            <meta charset="UTF-8">
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                            <style>
                                body { margin: 0; padding: 0; background-color: #f7fafc; font-family: 'Segoe UI', Arial, sans-serif; color: #1a3557; }
                                .container { max-width: 600px; margin: 20px auto; background: rgba(255, 255, 255, 0.95); border-radius: 12px; overflow: hidden; box-shadow: 0 6px 20px rgba(0, 0, 0, 0.1); backdrop-filter: blur(5px); }
                                .header { background: #3b82f6; padding: 30px; text-align: center; color: #ffffff; border-bottom: 2px solid rgba(255, 255, 255, 0.2); }
                                .header h2 { margin: 0; font-size: 24px; font-weight: 600; text-transform: uppercase; letter-spacing: 1px; }
                                .content { padding: 30px; }
                                .content p { font-size: 16px; line-height: 1.6; margin: 15px 0; color: #2d3748; }
                                .otp-box {
                                    background: linear-gradient(135deg, rgba(230, 240, 255, 0.9), rgba(230, 240, 255, 0.7));
                                    padding: 20px;
                                    border-radius: 10px;
                                    text-align: center;
                                    margin: 25px 0;
                                    border: 1px solid #e2e8f0;
                                    position: relative;
                                }
                                .otp-box h3 {
                                    font-size: 20px;
                                    font-weight: 600;
                                    color: #1a3557;
                                    margin: 0 auto;
                                    max-width: 90%%;
                                    word-wrap: break-word;
                                    line-height: 1.4;
                                    text-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
                                }
                                .otp-box::before {
                                    content: "🔒";
                                    position: absolute;
                                    top: 10px;
                                    left: 10px;
                                    font-size: 18px;
                                    color: #3b82f6;
                                }
                                .footer { background: #f7fafc; padding: 20px; text-align: center; font-size: 14px; color: #64748b; border-top: 1px solid #e2e8f0; }
                                .footer p { margin: 5px 0; }
                                .button {
                                    display: inline-block;
                                    padding: 12px 24px;
                                    background: #3b82f6;
                                    color: #ffffff;
                                    text-decoration: none;
                                    border-radius: 6px;
                                    font-weight: 600;
                                    margin-top: 20px;
                                    border: none;
                                    cursor: pointer;
                                    transition: transform 0.2s ease, background 0.3s ease;
                                    box-shadow: 0 2px 6px rgba(59, 130, 246, 0.3);
                                }
                                .button:hover {
                                    background: #1e40af;
                                    transform: translateY(-2px);
                                    box-shadow: 0 4px 10px rgba(59, 130, 246, 0.4);
                                }
                                .button:active {
                                    transform: translateY(0);
                                    box-shadow: 0 2px 6px rgba(59, 130, 246, 0.3);
                                }
                                @media (max-width: 600px) {
                                    .content { padding: 20px; }
                                    .header h2 { font-size: 20px; }
                                    .otp-box h3 { font-size: 18px; }
                                    .otp-box { padding: 15px; }
                                    .button { padding: 10px 20px; font-size: 14px; }
                                }
                            </style>
                        </head>
                        <body>
                            <div class="container">
                                <div class="header">
                                    <h2>Kode OTP Login Admin</h2>
                                </div>
                                <div class="content">
                                    <p>Halo Admin,</p>
                                    <p>Gunakan kode OTP berikut untuk melanjutkan proses login Anda:</p>
                                    <div class="otp-box">
                                        <h3 id="kodeToken">%s</h3>
                                    </div>
                                    <p>Kode OTP ini berlaku selama <b>24 jam</b>. Jangan bagikan kepada siapa pun.</p>
                                    <p>Jika Anda tidak meminta ini, segera hubungi tim keamanan.</p>
                                    // <a class="button" id="copyTokenBtn" type="button">Salin Token</a>
                                </div>
                                <div class="footer">
                                    <p>Salam,<br><b>Tim Keamanan AstraCom</b></p>
                                    <p>© 2025 AstraCom. All rights reserved.</p>
                                </div>
                            </div>
                        </body>
                        </html>
                        """,
                token);

        helper.setText(htmlContent, true);
        javaMailSender.send(message);
    }

    public void kirimResetPasswordLink(String toEmail, String resetLink) throws MessagingException {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Reset Password - Fajar Gold");
        message.setText("Halo,\n\n"
                + "Klik link berikut untuk mereset password akunmu:\n"
                + resetLink + "\n\n"
                + "Link ini hanya berlaku selama 15 menit.\n\n"
                + "Terima kasih,\nTim Fajar Gold");
        javaMailSender.send(message);
        System.out.println("Email reset berhasil dikirim ke: " + toEmail);

    }

}
