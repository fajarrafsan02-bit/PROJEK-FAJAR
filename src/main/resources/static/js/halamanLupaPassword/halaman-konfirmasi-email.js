// Sistem Konfirmasi Email
class EmailConfirmationSystem {
    constructor() {
        this.emailElement = document.getElementById("emailAddress");
        this.timerElement = document.getElementById("timerCountdown");
        this.openEmailBtn = document.getElementById("openEmailBtn");
        this.resendBtn = document.getElementById("resendBtn");

        this.timeLeft = 15 * 60; // 15 menit dalam detik
        this.resendCooldown = 60; // 60 detik cooldown untuk resend
        this.userEmail = "";

        this.init();
    }

    init() {
        this.loadUserEmail();
        this.setupEventListeners();
        this.startTimer();
        this.showWelcomeMessage();
    }

    loadUserEmail() {
        // Ambil email dari localStorage yang disimpan di halaman sebelumnya
        this.userEmail = localStorage.getItem('resetEmail') || 'user@example.com';
        this.emailElement.textContent = this.userEmail;
    }

    setupEventListeners() {
        this.openEmailBtn.addEventListener("click", () => this.handleOpenEmail());
        this.resendBtn.addEventListener("click", () => this.handleResend());

        document.getElementById("backToForgot").addEventListener("click", (e) => {
            e.preventDefault();
            this.handleBackToForgot();
        });
    }

    startTimer() {
        const timer = setInterval(() => {
            if (this.timeLeft <= 0) {
                clearInterval(timer);
                this.handleTimerExpired();
                return;
            }

            const minutes = Math.floor(this.timeLeft / 60);
            const seconds = this.timeLeft % 60;
            this.timerElement.textContent = `${minutes.toString().padStart(2, "0")}:${seconds.toString().padStart(2, "0")}`;

            // Ubah warna jika waktu hampir habis
            if (this.timeLeft <= 300) { // 5 menit terakhir
                document.querySelector('.timer-section').style.borderColor = 'rgba(239, 68, 68, 0.4)';
                document.querySelector('.timer-section').style.background = 'rgba(239, 68, 68, 0.15)';
            }

            this.timeLeft--;
        }, 1000);
    }

    handleOpenEmail() {
        // Deteksi provider email dan buka aplikasi email yang sesuai
        const emailDomain = this.userEmail.split('@')[1].toLowerCase();
        let emailUrl = 'mailto:';

        // URL khusus untuk provider email populer
        if (emailDomain.includes('gmail')) {
            emailUrl = 'https://mail.google.com';
        } else if (emailDomain.includes('yahoo')) {
            emailUrl = 'https://mail.yahoo.com';
        } else if (emailDomain.includes('outlook') || emailDomain.includes('hotmail')) {
            emailUrl = 'https://outlook.live.com';
        } else if (emailDomain.includes('icloud')) {
            emailUrl = 'https://www.icloud.com/mail';
        }

        // Buka di tab baru
        window.open(emailUrl, '_blank');

        // Tampilkan pesan
        this.showAlert('info', 'Membuka Email', 'Aplikasi email Anda akan terbuka di tab baru');
    }

    async handleResend() {
        if (this.resendBtn.disabled) return;

        this.resendBtn.disabled = true;
        this.resendBtn.classList.add('loading');

        try {
            // Simulasi API call untuk mengirim ulang email
            await new Promise(resolve => setTimeout(resolve, 2000));

            this.showAlert('success', 'Email Terkirim Ulang!', 'Link reset password baru telah dikirim ke email Anda');

            // Reset timer
            this.timeLeft = 15 * 60;

            // Reset tampilan timer
            document.querySelector('.timer-section').style.borderColor = 'rgba(239, 68, 68, 0.2)';
            document.querySelector('.timer-section').style.background = 'rgba(239, 68, 68, 0.1)';

            // Mulai cooldown untuk tombol resend
            this.startResendCooldown();

        } catch (error) {
            this.showAlert('error', 'Gagal Mengirim', 'Terjadi kesalahan saat mengirim ulang email. Silakan coba lagi.');
            this.resendBtn.disabled = false;
        } finally {
            this.resendBtn.classList.remove('loading');
        }
    }

    startResendCooldown() {
        let cooldown = this.resendCooldown;
        const originalText = this.resendBtn.querySelector('.btn-text').textContent;

        const cooldownTimer = setInterval(() => {
            if (cooldown <= 0) {
                clearInterval(cooldownTimer);
                this.resendBtn.disabled = false;
                this.resendBtn.querySelector('.btn-text').textContent = originalText;
                return;
            }

            this.resendBtn.querySelector('.btn-text').textContent = `Tunggu ${cooldown}s`;
            cooldown--;
        }, 1000);
    }

    handleTimerExpired() {
        this.timerElement.textContent = "00:00";
        document.querySelector('.timer-section').style.borderColor = 'rgba(239, 68, 68, 0.6)';
        document.querySelector('.timer-section').style.background = 'rgba(239, 68, 68, 0.2)';

        this.showAlert('warning', 'Link Kedaluwarsa', 'Link reset password telah kedaluwarsa. Silakan kirim ulang email reset.');

        // Disable tombol buka email
        this.openEmailBtn.disabled = true;
        this.openEmailBtn.style.opacity = '0.5';
    }

    handleBackToForgot() {
        this.showAlert('info', 'Kembali ke Form', 'Anda akan diarahkan ke halaman reset password');
        setTimeout(() => {
            window.location.href = 'lupa-password.html';
        }, 1000);
    }

    showWelcomeMessage() {
        setTimeout(() => {
            this.showAlert('success', 'Email Reset Berhasil Dikirim! 📧',
                `Link reset password telah dikirim ke ${this.userEmail}. Periksa inbox dan folder spam Anda.`, 8000);
        }, 1000);
    }

    showAlert(type, title, message, duration = 5000) {
        // Implementasi alert sederhana (bisa diganti dengan sistem alert yang lebih kompleks)
        const alertDiv = document.createElement('div');
        alertDiv.style.cssText = `
                    position: fixed;
                    top: 20px;
                    right: 20px;
                    background: white;
                    padding: 15px 20px;
                    border-radius: 10px;
                    box-shadow: 0 10px 30px rgba(0,0,0,0.2);
                    z-index: 10000;
                    max-width: 350px;
                    border-left: 4px solid ${type === 'success' ? '#10b981' : type === 'error' ? '#ef4444' : type === 'warning' ? '#f59e0b' : '#3b82f6'};
                `;

        alertDiv.innerHTML = `
                    <div style="font-weight: 700; margin-bottom: 5px; color: #0a0a0a;">${title}</div>
                    <div style="font-size: 14px; color: #4a5568;">${message}</div>
                `;

        document.body.appendChild(alertDiv);

        // Auto remove
        setTimeout(() => {
            if (alertDiv.parentNode) {
                alertDiv.parentNode.removeChild(alertDiv);
            }
        }, duration);
    }
}

// Inisialisasi sistem konfirmasi email
const emailConfirmationSystem = new EmailConfirmationSystem();

// Interaksi dengan floating shapes
const shapes = document.querySelectorAll(".shape");
shapes.forEach((shape, index) => {
    shape.addEventListener("mouseenter", function () {
        this.style.animationPlayState = "paused";
        this.style.transform = "scale(1.2)";
        this.style.opacity = "0.8";
    });

    shape.addEventListener("mouseleave", function () {
        this.style.animationPlayState = "running";
        this.style.transform = "";
        this.style.opacity = "";
    });
});

// Interaksi dengan logo brand
const brandLogo = document.querySelector(".brand-logo");
if (brandLogo) {
    brandLogo.addEventListener("click", function () {
        this.style.animation = "pulse 0.6s ease-in-out";
        setTimeout(() => {
            this.style.animation = "";
        }, 600);

        emailConfirmationSystem.showAlert('info', 'Fajar Gold Security',
            'Sistem keamanan berlapis melindungi proses reset password Anda dengan enkripsi tingkat enterprise.', 5000);
    });
}

// Keyboard shortcuts
document.addEventListener("keydown", (e) => {
    if (e.key === "r" && e.ctrlKey) {
        e.preventDefault();
        if (!emailConfirmationSystem.resendBtn.disabled) {
            emailConfirmationSystem.handleResend();
        }
    }

    if (e.key === "o" && e.ctrlKey) {
        e.preventDefault();
        emailConfirmationSystem.handleOpenEmail();
    }
});

// Enhanced accessibility
document.addEventListener("keydown", (e) => {
    if (e.key === "Tab") {
        document.body.classList.add("keyboard-navigation");
    }
});

document.addEventListener("mousedown", () => {
    document.body.classList.remove("keyboard-navigation");
});

// Focus styles untuk keyboard navigation
const focusStyle = document.createElement("style");
focusStyle.textContent = `
            .keyboard-navigation *:focus {
                outline: 3px solid rgba(212, 175, 55, 0.6) !important;
                outline-offset: 2px !important;
            }
        `;
document.head.appendChild(focusStyle);

console.log("Fajar Gold - Email Confirmation System Loaded Successfully! 📧");