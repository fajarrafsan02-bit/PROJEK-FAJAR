// Sistem Alert yang Canggih
class AlertSystem {
    constructor() {
        this.container = document.getElementById("alertContainer");
        this.alerts = [];
        this.maxAlertShown = 5;
    }

    // Mencegah alert duplikat
    preventDuplicate(type, title, message) {
        return this.alerts.some((alert) => {
            const el = alert.element;
            if (!el) return false;
            const currentType = el.classList.contains(type);
            const currentTitle = el.querySelector(".alert-title")?.innerText === title;
            const currentMessage = el.querySelector(".alert-message")?.innerText === message;
            return currentType && currentTitle && currentMessage;
        });
    }

    // Menampilkan alert
    show(type, title, message, duration = 5000) {
        if (this.preventDuplicate(type, title, message)) return;

        // Hapus alert lama jika sudah mencapai batas maksimal
        if (this.alerts.length >= this.maxAlertShown) {
            const oldest = this.alerts[0];
            this.hide(oldest.id);
        }

        const alertId = Date.now().toString(36) + Math.random().toString(36).substr(2, 5);
        const alertElement = document.createElement("div");
        alertElement.className = `custom-alert ${type}`;
        alertElement.dataset.alertId = alertId;

        alertElement.innerHTML = `
                    <div class="alert-content">
                        <div class="alert-icon ${type}">
                            <i class="fas ${this.getIcon(type)}"></i>
                        </div>
                        <div class="alert-text">
                            <div class="alert-title">${title}</div>
                            <div class="alert-message">${message}</div>
                        </div>
                        <button class="alert-close">
                            <i class="fas fa-times"></i>
                        </button>
                    </div>
                `;

        this.container.appendChild(alertElement);
        this.alerts.push({ id: alertId, element: alertElement, duration });

        // Tampilkan alert dengan animasi
        setTimeout(() => {
            alertElement.classList.add("show");
        }, 100);

        // Auto hide setelah durasi tertentu
        const autoHideTimeout = setTimeout(() => this.hide(alertId), duration);

        // Event listener untuk tombol close
        const closeBtn = alertElement.querySelector(".alert-close");
        closeBtn.addEventListener("click", (e) => {
            e.stopPropagation();
            clearTimeout(autoHideTimeout);
            this.hide(alertId);
        });

        // Event listener untuk klik pada alert
        alertElement.addEventListener("click", (e) => {
            if (!e.target.classList.contains("alert-close") && !e.target.closest(".alert-close")) {
                clearTimeout(autoHideTimeout);
                this.hide(alertId);
            }
        });

        return alertId;
    }

    // Menyembunyikan alert
    hide(alertId) {
        const alert = this.alerts.find((alert) => alert.id === alertId);
        if (!alert || !alert.element || alert.element.classList.contains("hide") || alert.isRemoving) return;

        alert.isRemoving = true;
        alert.element.classList.add("hide");

        requestAnimationFrame(() => {
            setTimeout(() => {
                try {
                    if (alert.element && alert.element.parentNode) {
                        alert.element.parentNode.removeChild(alert.element);
                    }
                } catch (e) {
                    console.warn("Alert cleanup error:", e);
                }
                this.alerts = this.alerts.filter((a) => a.id !== alertId);
            }, 400);
        });
    }

    // Mendapatkan icon berdasarkan tipe alert
    getIcon(type) {
        const icons = {
            success: "fa-check",
            error: "fa-exclamation-triangle",
            warning: "fa-exclamation",
            info: "fa-info",
        };
        return icons[type] || "fa-info";
    }

    // Method shortcut untuk berbagai tipe alert
    success(title, message, duration) {
        return this.show("success", title, message, duration);
    }

    error(title, message, duration) {
        return this.show("error", title, message, duration);
    }

    warning(title, message, duration) {
        return this.show("warning", title, message, duration);
    }

    info(title, message, duration) {
        return this.show("info", title, message, duration);
    }
}

// Inisialisasi sistem alert
const alertSystem = new AlertSystem();

// Sistem Lupa Password
class ForgotPasswordSystem {
    constructor() {
        this.emailInput = document.getElementById("emailAddress");
        this.submitBtn = document.getElementById("submitBtn");
        this.form = document.getElementById("forgotPasswordForm");
        this.isSubmitting = false;

        this.init();
    }

    init() {
        this.setupEventListeners();
        this.showWelcomeMessage();
    }

    setupEventListeners() {
        // Event listener untuk input email
        this.emailInput.addEventListener("input", (e) => this.handleEmailInput(e));
        this.emailInput.addEventListener("blur", () => this.validateEmail());
        this.emailInput.addEventListener("focus", () => this.handleFocus());

        // Event listener untuk form submit
        this.form.addEventListener("submit", (e) => this.handleSubmit(e));

        // Event listener untuk tombol kembali
        document.getElementById("backToLogin").addEventListener("click", (e) => {
            e.preventDefault();
            this.handleBackToLogin();
        });
    }

    handleEmailInput(e) {
        const email = e.target.value.trim();
        this.updateEmailValidation(email);
    }

    handleFocus() {
        // Hapus class invalid saat focus
        this.emailInput.classList.remove("invalid");
    }

    validateEmail() {
        const email = this.emailInput.value.trim();
        const isValid = this.isValidEmail(email);

        if (email && !isValid) {
            this.emailInput.classList.add("invalid");
            this.emailInput.classList.remove("valid");
        } else if (email && isValid) {
            this.emailInput.classList.add("valid");
            this.emailInput.classList.remove("invalid");
        } else {
            this.emailInput.classList.remove("valid", "invalid");
        }

        return isValid;
    }

    updateEmailValidation(email) {
        if (email) {
            const isValid = this.isValidEmail(email);
            if (isValid) {
                this.emailInput.classList.add("valid");
                this.emailInput.classList.remove("invalid");
            } else {
                this.emailInput.classList.remove("valid");
            }
        } else {
            this.emailInput.classList.remove("valid", "invalid");
        }

        // Update status tombol submit
        this.updateSubmitButton();
    }

    isValidEmail(email) {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return emailRegex.test(email);
    }

    updateSubmitButton() {
        const email = this.emailInput.value.trim();
        const isValid = this.isValidEmail(email);

        this.submitBtn.disabled = !isValid || this.isSubmitting;
    }

    async handleSubmit(e) {
        e.preventDefault();

        if (this.isSubmitting) return;

        const email = this.emailInput.value.trim();

        // Validasi email
        if (!this.isValidEmail(email)) {
            alertSystem.error(
                "Email Tidak Valid",
                "Silakan masukkan alamat email yang valid"
            );
            this.emailInput.focus();
            return;
        }

        this.isSubmitting = true;
        this.submitBtn.classList.add("loading");

        try {
            // Simulasi API call untuk mengirim email reset
            const result = await this.sendResetEmail(email);

            if (result.success) {
                // Simpan email ke localStorage untuk halaman konfirmasi
                localStorage.setItem('resetEmail', email);

                alertSystem.success(
                    "Email Terkirim!",
                    "Link reset password telah dikirim ke email Anda"
                );

                // Redirect ke halaman konfirmasi setelah 2 detik
                setTimeout(() => {
                    window.location.href = "/auth/open-email-link";
                }, 2000);

            } else {
                alertSystem.error(
                    "Gagal Mengirim Email",
                    result.message || "Terjadi kesalahan saat mengirim email reset"
                );
            }

        } catch (error) {
            alertSystem.error(
                "Kesalahan Sistem",
                "Terjadi kesalahan pada server. Silakan coba lagi nanti."
            );
        } finally {
            this.isSubmitting = false;
            this.submitBtn.classList.remove("loading");
        }
    }

    async sendResetEmail(email) {
        try {
            const response = await fetch(`/auth/reset-password-request`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ email: email })
            })

            const data = await response.json();

            return {
                success: data.success,
                message: data.message
            }
        }
        catch (error) {
            return {
                success: false,
                message: 'Terjadi kesalahan jaringan atau server.'
            }
        }
    }

    handleBackToLogin() {
        alertSystem.info("Kembali ke Login", "Anda akan diarahkan ke halaman login");
        setTimeout(() => {
            window.location.href = "index.html";
        }, 1000);
    }

    showWelcomeMessage() {
        setTimeout(() => {
            alertSystem.info(
                "Reset Password Fajar Gold 🔑",
                "Masukkan email terdaftar Anda untuk menerima link reset password. Pastikan email yang dimasukkan benar dan aktif.",
                7000
            );
        }, 1500);
    }
}

// Inisialisasi sistem lupa password
const forgotPasswordSystem = new ForgotPasswordSystem();

// Keyboard shortcuts
document.addEventListener("keydown", (e) => {
    // ESC untuk menutup semua alert
    if (e.key === "Escape") {
        alertSystem.alerts.forEach((alert) => {
            alertSystem.hide(alert.id);
        });
    }

    // Ctrl+Enter untuk submit form
    if (e.key === "Enter" && e.ctrlKey) {
        if (forgotPasswordSystem.isValidEmail(forgotPasswordSystem.emailInput.value.trim())) {
            forgotPasswordSystem.form.dispatchEvent(new Event("submit"));
        }
    }
});

// Auto focus pada input email saat halaman dimuat
window.addEventListener("load", () => {
    setTimeout(() => {
        if (forgotPasswordSystem.emailInput) {
            forgotPasswordSystem.emailInput.focus();
        }
    }, 1000);
});

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

        alertSystem.info(
            "Fajar Gold Security",
            "Sistem keamanan berlapis untuk melindungi akun Anda dengan enkripsi tingkat enterprise.",
            5000
        );
    });
}

// Enhanced accessibility
document.addEventListener("keydown", (e) => {
    if (e.key === "Tab") {
        document.body.classList.add("keyboard-navigation");
    }
});

document.addEventListener("mousedown", () => {
    document.body.classList.remove("keyboard-navigation");
});

// Tambahkan focus styles untuk keyboard navigation
const focusStyle = document.createElement("style");
focusStyle.textContent = `
            .keyboard-navigation *:focus {
                outline: 3px solid rgba(212, 175, 55, 0.6) !important;
                outline-offset: 2px !important;
            }
        `;
document.head.appendChild(focusStyle);

// Error handling global
window.addEventListener("error", (e) => {
    console.error("JavaScript Error:", e.error);
    alertSystem.error(
        "Terjadi Kesalahan",
        "Mohon refresh halaman atau hubungi customer service jika masalah berlanjut.",
        8000
    );
});

console.log("Fajar Gold - Forgot Password System Loaded Successfully! 🔑");