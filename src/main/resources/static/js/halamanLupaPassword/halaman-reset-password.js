// Sistem Reset Password
class ResetPasswordSystem {
    constructor() {
        this.newPasswordInput = document.getElementById("newPassword");
        this.confirmPasswordInput = document.getElementById("confirmPassword");
        this.resetBtn = document.getElementById("resetBtn");
        this.form = document.getElementById("resetPasswordForm");
        this.successState = document.getElementById("successState");
        this.continueBtn = document.getElementById("continueBtn");

        // Toggle password visibility
        this.toggleNewPassword = document.getElementById("toggleNewPassword");
        this.toggleConfirmPassword = document.getElementById("toggleConfirmPassword");

        // Password strength elements
        this.strengthFill = document.getElementById("strengthFill");
        this.strengthText = document.getElementById("strengthText");

        // Password requirements
        this.requirements = {
            length: document.getElementById("lengthReq"),
            uppercase: document.getElementById("uppercaseReq"),
            lowercase: document.getElementById("lowercaseReq"),
            number: document.getElementById("numberReq"),
            special: document.getElementById("specialReq")
        };

        this.isSubmitting = false;
        this.passwordStrength = 0;

        this.init();
    }

    init() {
        this.setupEventListeners();
        this.showWelcomeMessage();
        this.validateToken();
    }

    setupEventListeners() {
        // Event listener untuk input password
        this.newPasswordInput.addEventListener("input", (e) => this.handlePasswordInput(e));
        this.confirmPasswordInput.addEventListener("input", () => {
            this.validatePasswordMatch();
            this.updateSubmitButton(); // Tambahkan ini!
        });

        // Event listener untuk toggle password visibility
        this.toggleNewPassword.addEventListener("click", () => this.togglePasswordVisibility("newPassword"));
        this.toggleConfirmPassword.addEventListener("click", () => this.togglePasswordVisibility("confirmPassword"));

        // Event listener untuk form submit
        this.form.addEventListener("submit", (e) => this.handleSubmit(e));

        // Event listener untuk tombol continue
        this.continueBtn.addEventListener("click", () => this.handleContinue());

        // Event listener untuk tombol kembali
        document.getElementById("backToLogin").addEventListener("click", (e) => {
            e.preventDefault();
            this.handleBackToLogin();
        });
    }

    validateToken() {
        // Simulasi validasi token dari URL
        const urlParams = new URLSearchParams(window.location.search);
        const token = urlParams.get('token');
        console.log("Token:", token);

        if (!token) {
            // Jika tidak ada token, tampilkan pesan dan redirect
            this.showAlert('error', 'Link Tidak Valid', 'Link reset password tidak valid atau sudah kedaluwarsa. Anda akan diarahkan ke halaman reset password.');

            // Disable form
            this.form.style.opacity = '0.5';
            this.form.style.pointerEvents = 'none';

            setTimeout(() => {
                window.location.href = 'lupa-password.html';
            }, 3000);
            return false;
        }

        // Simulasi validasi token (dalam implementasi nyata, ini akan memanggil API)
        if (token === 'invalid' || token === 'expired') {
            this.showAlert('error', 'Token Kedaluwarsa', 'Token reset password sudah kedaluwarsa. Silakan minta link reset baru.');
            setTimeout(() => {
                window.location.href = 'lupa-password.html';
            }, 3000);
            return false;
        }

        // Token valid
        this.showAlert('success', 'Token Valid', 'Silakan masukkan password baru Anda.');
        return true;
    }

    handlePasswordInput(e) {
        const password = e.target.value;
        this.checkPasswordStrength(password);
        this.validatePasswordRequirements(password);
        this.validatePasswordMatch();
        this.updateSubmitButton();
    }

    checkPasswordStrength(password) {
        let strength = 0;
        let strengthText = "";
        let strengthClass = "";

        if (password.length === 0) {
            strengthText = "Masukkan password";
            strengthClass = "";
        } else if (password.length < 6) {
            strength = 1;
            strengthText = "Sangat Lemah";
            strengthClass = "weak";
        } else if (password.length < 8) {
            strength = 2;
            strengthText = "Lemah";
            strengthClass = "weak";
        } else {
            // Hitung berdasarkan kriteria
            if (/[a-z]/.test(password)) strength++;
            if (/[A-Z]/.test(password)) strength++;
            if (/[0-9]/.test(password)) strength++;
            if (/[^A-Za-z0-9]/.test(password)) strength++;

            if (strength === 1) {
                strengthText = "Lemah";
                strengthClass = "weak";
            } else if (strength === 2) {
                strengthText = "Cukup";
                strengthClass = "fair";
            } else if (strength === 3) {
                strengthText = "Baik";
                strengthClass = "good";
            } else if (strength === 4) {
                strengthText = "Kuat";
                strengthClass = "strong";
            }
        }

        this.passwordStrength = strength;
        this.strengthFill.className = `strength-fill ${strengthClass}`;
        this.strengthText.className = `strength-text ${strengthClass}`;
        this.strengthText.textContent = strengthText;
    }

    validatePasswordRequirements(password) {
        const requirements = {
            length: password.length >= 8,
            uppercase: /[A-Z]/.test(password),
            lowercase: /[a-z]/.test(password),
            number: /[0-9]/.test(password),
            special: /[^A-Za-z0-9]/.test(password)
        };

        // Update tampilan setiap requirement
        Object.keys(requirements).forEach(key => {
            const element = this.requirements[key];
            const isValid = requirements[key];

            if (isValid) {
                element.classList.add("valid");
                element.querySelector("i").className = "fas fa-check";
            } else {
                element.classList.remove("valid");
                element.querySelector("i").className = "fas fa-times";
            }
        });

        return Object.values(requirements).every(req => req);
    }

    validatePasswordMatch() {
        const newPassword = this.newPasswordInput.value;
        const confirmPassword = this.confirmPasswordInput.value;
        if (!confirmPassword) {
            this.confirmPasswordInput.classList.remove("valid", "invalid");
            return false;
        }

        if (newPassword !== confirmPassword) {
            this.confirmPasswordInput.classList.add("invalid");
            this.confirmPasswordInput.classList.remove("valid");
            return false;
        } else {
            this.confirmPasswordInput.classList.add("valid");
            this.confirmPasswordInput.classList.remove("invalid");
            return true;
        }
    }

    togglePasswordVisibility(inputId) {
        const input = document.getElementById(inputId);
        const toggleBtn = document.getElementById(`toggle${inputId.charAt(0).toUpperCase() + inputId.slice(1)}`);
        const icon = toggleBtn.querySelector("i");

        if (input.type === "password") {
            input.type = "text";
            icon.className = "fas fa-eye-slash";
        } else {
            input.type = "password";
            icon.className = "fas fa-eye";
        }
    }

    updateSubmitButton() {
        const newPassword = this.newPasswordInput.value;
        const confirmPassword = this.confirmPasswordInput.value;

        const isPasswordValid = this.validatePasswordRequirements(newPassword);
        const isPasswordMatch = this.validatePasswordMatch();
        const isStrengthGood = this.passwordStrength >= 3;

        // this.resetBtn.disabled = !isPasswordValid || !isPasswordMatch || !isStrengthGood || this.isSubmitting;
        const shouldEnable = isPasswordValid && isPasswordMatch && isStrengthGood && !this.isSubmitting;
        this.resetBtn.disabled = !shouldEnable;
        if (shouldEnable) {
            this.resetBtn.style.opacity = '1';
            this.resetBtn.style.cursor = 'pointer';
        } else {
            this.resetBtn.style.opacity = '0.6';
            this.resetBtn.style.cursor = 'not-allowed';
        }
    }

    async handleSubmit(e) {
        e.preventDefault();

        if (this.isSubmitting) return;

        const newPassword = this.newPasswordInput.value;
        const confirmPassword = this.confirmPasswordInput.value;

        // Validasi final
        if (!this.validatePasswordRequirements(newPassword)) {
            this.showAlert('error', 'Password Tidak Valid', 'Password tidak memenuhi persyaratan yang ditentukan');
            return;
        }

        if (newPassword !== confirmPassword) {
            this.showAlert('error', 'Password Tidak Cocok', 'Konfirmasi password tidak sesuai dengan password baru');
            this.confirmPasswordInput.focus();
            return;
        }

        if (this.passwordStrength < 3) {
            this.showAlert('warning', 'Password Terlalu Lemah', 'Gunakan password yang lebih kuat untuk keamanan akun Anda');
            return;
        }

        this.isSubmitting = true;
        this.resetBtn.classList.add("loading");

        try {
            // Simulasi API call untuk reset password
            const result = await this.resetPassword(newPassword);

            if (result.success) {
                this.showSuccess();
            } else {
                this.showAlert('error', 'Gagal Reset Password', result.message || 'Terjadi kesalahan saat mereset password');
            }

        } catch (error) {
            this.showAlert('error', 'Kesalahan Sistem', 'Terjadi kesalahan pada server. Silakan coba lagi nanti.');
        } finally {
            this.isSubmitting = false;
            this.resetBtn.classList.remove("loading");
        }
    }

    async resetPassword(newPassword) {
        // Simulasi API call dengan delay
        // await new Promise(resolve => setTimeout(resolve, 3000));
        const urlParams = new URLSearchParams(window.location.search);
        const token = urlParams.get('token');
        console.log("Resetting password with token:", token);

        try {
            const response = await fetch('/auth/reset-password', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    token: token,
                    passwordBaru: newPassword
                })
            });

            if (!response.ok) {
                const errorData = await response.json();
                return {
                    success: false,
                    message: errorData.message
                };
            }

            const data = await response.json();
            return data;
        } catch (error) {
            console.log('error', error);
            return {
                success: false,
                message: "Terjadi Kesalahan jaringan atau server"
            };
        }

        // Simulasi berbagai kondisi
        // if (newPassword.toLowerCase().includes('password')) {
        //     return {
        //         success: false,
        //         message: 'Password tidak boleh mengandung kata "password"'
        //     };
        // } else if (newPassword.length < 8) {
        //     return {
        //         success: false,
        //         message: 'Password harus minimal 8 karakter'
        //     };
        // } else {
        //     return {
        //         success: true,
        //         message: 'Password berhasil direset'
        //     };
        // }
    }

    showSuccess() {
        this.form.style.display = "none";
        this.successState.style.display = "block";

        this.showAlert('success', 'Password Berhasil Direset!', 'Password Anda telah berhasil diperbarui. Silakan login dengan password baru.');

        // Auto redirect setelah 5 detik
        setTimeout(() => {
            this.handleContinue();
        }, 5000);
    }

    handleContinue() {
        this.showAlert('info', 'Mengalihkan...', 'Anda akan diarahkan ke halaman login');
        setTimeout(() => {
            window.location.href = 'index.html';
        }, 1500);
    }

    handleBackToLogin() {
        this.showAlert('info', 'Kembali ke Login', 'Anda akan diarahkan ke halaman login');
        setTimeout(() => {
            window.location.href = 'index.html';
        }, 1000);
    }

    showWelcomeMessage() {
        setTimeout(() => {
            this.showAlert('info', 'Reset Password Fajar Gold 🔐',
                'Buat password baru yang kuat dengan kombinasi huruf besar, kecil, angka, dan karakter khusus untuk keamanan maksimal.', 8000);
        }, 1500);
    }

    showAlert(type, title, message, duration = 5000) {
        if (window.alertSystem) {
            return window.alertSystem[type](title, message, duration);
        } else {
            // Fallback alert dengan tombol close yang berfungsi
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
                        <div style="font-weight: 700; margin-bottom: 5px; color: #0a0a0a; display: flex; justify-content: space-between; align-items: center;">
                            ${title}
                            <button class="close-alert" style="background: none; border: none; font-size: 18px; cursor: pointer; color: #666; padding: 0; width: 20px; height: 20px;">&times;</button>
                        </div>
                        <div style="font-size: 14px; color: #4a5568;">${message}</div>
                    `;

            document.body.appendChild(alertDiv);

            // Event listener untuk tombol close
            alertDiv.querySelector('.close-alert').addEventListener('click', () => {
                alertDiv.remove();
            });

            // Auto remove
            setTimeout(() => {
                if (alertDiv.parentNode) {
                    alertDiv.parentNode.removeChild(alertDiv);
                }
            }, duration);
        }
    }
}

// Inisialisasi sistem reset password
const resetPasswordSystem = new ResetPasswordSystem();

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

        resetPasswordSystem.showAlert('info', 'Fajar Gold Security',
            'Sistem keamanan berlapis melindungi proses reset password dengan enkripsi tingkat enterprise dan validasi multi-layer.', 6000);
    });
}

// Keyboard shortcuts
document.addEventListener("keydown", (e) => {
    // Ctrl+Enter untuk submit form
    if (e.key === "Enter" && e.ctrlKey) {
        if (!resetPasswordSystem.resetBtn.disabled) {
            resetPasswordSystem.form.dispatchEvent(new Event("submit"));
        }
    }

    // Ctrl+H untuk toggle password visibility
    if (e.key === "h" && e.ctrlKey) {
        e.preventDefault();
        resetPasswordSystem.togglePasswordVisibility("newPassword");
    }

    // Ctrl+J untuk toggle confirm password visibility
    if (e.key === "j" && e.ctrlKey) {
        e.preventDefault();
        resetPasswordSystem.togglePasswordVisibility("confirmPassword");
    }
});

// Auto focus pada input password saat halaman dimuat
window.addEventListener("load", () => {
    setTimeout(() => {
        if (resetPasswordSystem.newPasswordInput) {
            resetPasswordSystem.newPasswordInput.focus();
        }
    }, 1000);
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

// Error handling global
window.addEventListener("error", (e) => {
    console.error("JavaScript Error:", e.error);
    resetPasswordSystem.showAlert('error', 'Terjadi Kesalahan',
        'Mohon refresh halaman atau hubungi customer service jika masalah berlanjut.', 8000);
});

console.log("Fajar Gold - Reset Password System Loaded Successfully! 🔐");