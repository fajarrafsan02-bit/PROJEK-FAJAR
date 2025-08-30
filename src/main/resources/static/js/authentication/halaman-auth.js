class AlertSystem {
    constructor() {
        this.container = document.getElementById("alertContainer")
        this.alerts = []
        this.maxAlertShown = 5
    }

    preventDuplicate(type, title, message) {
        return this.alerts.some((alert) => {
            const el = alert.element
            if (!el) return false
            const currentType = el.classList.contains(type)
            const currentTitle = el.querySelector(".alert-title")?.innerText === title
            const currentMessage = el.querySelector(".alert-message")?.innerText === message
            return currentType && currentTitle && currentMessage
        })
    }

    show(type, title, message, duration = 5000) {
        if (this.preventDuplicate(type, title, message)) return

        if (this.alerts.length >= this.maxAlertShown) {
            const oldest = this.alerts[0]
            this.hide(oldest.id)
        }

        const alertId = Date.now().toString(36) + Math.random().toString(36).substr(2, 5)
        const alertElement = document.createElement("div")
        alertElement.className = `custom-alert ${type}`
        alertElement.dataset.alertId = alertId

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
            <div class="alert-progress">
                <div class="alert-progress-bar ${type}"></div>
            </div>
        `

        this.container.appendChild(alertElement)
        this.alerts.push({ id: alertId, element: alertElement, duration })

        // Enhanced show animation
        setTimeout(() => {
            alertElement.classList.add("show")
        }, 100)

        // Enhanced progress bar animation
        const progressBar = alertElement.querySelector(".alert-progress-bar")
        setTimeout(() => {
            progressBar.classList.add("animate")
            progressBar.style.transitionDuration = `${duration}ms`
        }, 200)

        // Auto hide with enhanced timing
        const autoHideTimeout = setTimeout(() => this.hide(alertId), duration)

        // Enhanced close button interaction
        const closeBtn = alertElement.querySelector(".alert-close")
        closeBtn.addEventListener("click", (e) => {
            e.stopPropagation()
            clearTimeout(autoHideTimeout)
            this.hide(alertId)
        })

        // Enhanced click to dismiss
        alertElement.addEventListener("click", (e) => {
            if (!e.target.classList.contains("alert-close") && !e.target.closest(".alert-close")) {
                clearTimeout(autoHideTimeout)
                this.hide(alertId)
            }
        })

        // Enhanced backup cleanup
        setTimeout(() => {
            this.hide(alertId)
        }, duration + 2000)

        return alertId
    }

    hide(alertId) {
        const alert = this.alerts.find((alert) => alert.id === alertId)
        if (!alert || !alert.element || alert.element.classList.contains("hide") || alert.isRemoving) return

        alert.isRemoving = true
        alert.element.classList.add("hide")

        requestAnimationFrame(() => {
            setTimeout(() => {
                try {
                    if (alert.element && alert.element.parentNode) {
                        alert.element.parentNode.removeChild(alert.element)
                    }
                } catch (e) {
                    console.warn("Alert cleanup error:", e)
                }
                this.alerts = this.alerts.filter((a) => a.id !== alertId)
            }, 400)
        })
    }

    getIcon(type) {
        const icons = {
            success: "fa-check",
            error: "fa-exclamation-triangle",
            warning: "fa-exclamation",
            info: "fa-info",
        }
        return icons[type] || "fa-info"
    }

    success(title, message, duration) {
        return this.show("success", title, message, duration)
    }

    error(title, message, duration) {
        return this.show("error", title, message, duration)
    }

    warning(title, message, duration) {
        return this.show("warning", title, message, duration)
    }

    info(title, message, duration) {
        return this.show("info", title, message, duration)
    }
}

// Initialize enhanced alert system
const alertSystem = new AlertSystem()



// DOM Elements
const authContainer = document.getElementById('authContainer');
const loginForm = document.getElementById('loginForm');
const registerForm = document.getElementById('registerForm');
const showRegisterBtn = document.getElementById('showRegisterBtn');
const showLoginBtn = document.getElementById('showLoginBtn');
const formType = document.getElementById("formType")?.value;

if (formType === "register") {
    authContainer.classList.add('show-register');
    history.replaceState({}, "", "/auth/register");
} else if (formType === "login") {

    authContainer.classList.remove('show-register');
    history.replaceState({}, "", "/auth/login");
}

// Form switching functionality
// Enhanced form switching functionality
showRegisterBtn.addEventListener("click", function (e) {
    e.preventDefault()
    authContainer.classList.add("show-register")
    window.history.pushState({}, "", "/auth/register")
    if (loginForm) loginForm.reset()

    // Enhanced ripple effect
    this.style.transform = "scale(0.95)"
    setTimeout(() => {
        this.style.transform = ""
    }, 150)

    alertSystem.info("Formulir Registrasi", "Silakan lengkapi data Anda untuk membuat akun baru di Fajar Gold", 3500)
})

showLoginBtn.addEventListener("click", function (e) {
    e.preventDefault()
    authContainer.classList.remove("show-register")
    window.history.pushState({}, "", "/auth/login")
    if (registerForm) registerForm.reset()

    // Enhanced ripple effect
    this.style.transform = "scale(0.95)"
    setTimeout(() => {
        this.style.transform = ""
    }, 150)

    alertSystem.info("Formulir Login", "Masukkan email dan password Anda untuk mengakses akun Fajar Gold", 3500)
})

// Password toggle functionality
function setupPasswordToggle(inputId, toggleId) {
    const input = document.getElementById(inputId)
    const toggle = document.getElementById(toggleId)

    if (!input || !toggle) return

    toggle.addEventListener("click", function () {
        const type = input.getAttribute("type") === "password" ? "text" : "password"
        input.setAttribute("type", type)

        this.classList.toggle("fa-eye")
        this.classList.toggle("fa-eye-slash")

        // Enhanced animation
        this.style.transform = "translateY(-50%) scale(0.8)"
        setTimeout(() => {
            this.style.transform = "translateY(-50%) scale(1)"
        }, 150)
    })
}

setupPasswordToggle("loginPassword", "loginPasswordToggle")
setupPasswordToggle("registerPassword", "registerPasswordToggle")

// Validation functions
function validateEmail(email) {
    const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
    return emailRegex.test(email);
}

let errorMessage = "";

function validatePassword(password) {
    if (/^[0-9]/.test(password)) {
        errorMessage = "Password tidak boleh diawali dengan angka!";
        return false;
    } else if (!/^[A-Z]/.test(password)) {
        errorMessage = "Password harus diawali huruf kapital!";
        return false;
    } else if (!/^[A-Z][a-zA-Z]*\d{3,}$/.test(password)) {
        errorMessage = "Password harus diakhiri dengan minimal 3 digit angka!";
        return false;
    }
    return true;
}


function validatePhone(phone) {
    const phoneRegex = /^[0-9]{10,13}$/;
    return phoneRegex.test(phone.replace(/\D/g, ''));
}

function validateName(name) {
    return name.trim().length >= 2;
}

function setLoading(button, isLoading) {
    button.classList.toggle('loading', isLoading);
    button.disabled = isLoading;
}

function addShakeAnimation(element) {
    element.style.animation = "shake 0.8s ease-in-out"
    setTimeout(() => {
        element.style.animation = ""
    }, 800)
}

// Social login handlers
const socialButtons = [
    "loginGoogleBtn",
    "loginFacebookBtn",
    "loginAppleBtn",
    "registerGoogleBtn",
    "registerFacebookBtn",
    "registerAppleBtn",
]

socialButtons.forEach((btnId) => {
    const btn = document.getElementById(btnId)
    if (!btn) return

    btn.addEventListener("click", function () {
        const provider = btnId.includes("Google") ? "Google" : btnId.includes("Facebook") ? "Facebook" : "Apple"
        const action = btnId.includes("login") ? "Login" : "Registrasi"

        this.style.transform = "translateY(-3px) scale(0.98)"
        setTimeout(() => {
            this.style.transform = ""
        }, 200)

        alertSystem.info(
            `${action} dengan ${provider}`,
            `Fitur ${action.toLowerCase()} dengan ${provider} akan segera tersedia di Fajar Gold. Terima kasih atas kesabaran Anda!`,
            4500,
        )
    })
})

// Forgot password handler
// const forgotPasswordBtn = document.getElementById("forgotPassword")
// if (forgotPasswordBtn) {
//     forgotPasswordBtn.addEventListener("click", function (e) {
//         e.preventDefault()

//         this.style.transform = "translateY(-2px)"
//         setTimeout(() => {
//             this.style.transform = ""
//         }, 200)

//         alertSystem.success(
//             "Reset Password",
//             "Link reset password telah dikirim ke email Anda. Periksa inbox dan folder spam dalam beberapa menit.",
//             6000,
//         )
//     })
// }

// Enhanced input interactions
const inputs = document.querySelectorAll(".form-input")
inputs.forEach((input) => {
    input.addEventListener("input", function () {
        // Remove error styling
        const wrapper = this.closest(".input-wrapper")
        wrapper.style.borderColor = ""
        wrapper.style.boxShadow = ""
    })

    input.addEventListener("focus", function () {
        const wrapper = this.closest(".input-wrapper")
        wrapper.style.transform = "translateY(-2px)"
    })

    input.addEventListener("blur", function () {
        const wrapper = this.closest(".input-wrapper")
        wrapper.style.transform = ""
    })
})

// Enhanced phone number formatting
const phoneInput = document.getElementById("phoneNumber")
if (phoneInput) {
    phoneInput.addEventListener("input", function () {
        let value = this.value.replace(/\D/g, "")
        if (value.startsWith("0")) {
            value = value.substring(1)
        }
        if (value.length > 13) {
            value = value.substring(0, 13)
        }
        this.value = value
    })
}
// Load saved credentials
// Enhanced welcome message on page load
window.addEventListener("load", () => {
    setTimeout(() => {
        const hour = new Date().getHours()
        let greeting = "Selamat Datang"

        if (hour < 12) {
            greeting = "Selamat Pagi"
        } else if (hour < 17) {
            greeting = "Selamat Siang"
        } else {
            greeting = "Selamat Malam"
        }

        alertSystem.info(
            `${greeting}! ✨`,
            "Temukan koleksi perhiasan emas terbaik di Fajar Gold. Login atau daftar untuk pengalaman berbelanja yang personal dan eksklusif.",
            6000,
        )
    }, 2000)
})

// Enhanced keyboard shortcuts
document.addEventListener("keydown", (e) => {
    if (e.key === "Escape") {
        // Close all alerts
        alertSystem.alerts.forEach((alert) => {
            alertSystem.hide(alert.id)
        })
    }

    if (e.key === "Enter" && e.ctrlKey) {
        // Quick form switch
        if (authContainer.classList.contains("show-register")) {
            showLoginBtn.click()
        } else {
            showRegisterBtn.click()
        }
    }
})

// Enhanced ripple effect
// Enhanced ripple effect for buttons
function createRipple(e) {
    const button = e.currentTarget
    const ripple = document.createElement("span")
    const rect = button.getBoundingClientRect()
    const size = Math.max(rect.width, rect.height)
    const x = e.clientX - rect.left - size / 2
    const y = e.clientY - rect.top - size / 2

    ripple.style.cssText = `
        position: absolute;
        width: ${size}px;
        height: ${size}px;
        left: ${x}px;
        top: ${y}px;
        background: rgba(255, 255, 255, 0.5);
        border-radius: 50%;
        transform: scale(0);
        animation: ripple 0.8s ease-out;
        pointer-events: none;
        z-index: 1;
    `

    button.appendChild(ripple)
    setTimeout(() => ripple.remove(), 800)
}

// Enhanced ripple animation styles
const style = document.createElement("style")
style.textContent = `
    @keyframes ripple {
        to {
            transform: scale(3);
            opacity: 0;
        }
    }
    .submit-btn, .social-btn {
        position: relative;
        overflow: hidden;
    }
`
document.head.appendChild(style)

// Apply enhanced ripple to buttons
document.querySelectorAll(".submit-btn, .social-btn").forEach((btn) => {
    btn.addEventListener("click", createRipple)
})

// Enhanced floating shapes animation
const shapes = document.querySelectorAll(".shape")
shapes.forEach((shape, index) => {
    shape.addEventListener("mouseenter", function () {
        this.style.animationPlayState = "paused"
        this.style.transform = "scale(1.2)"
        this.style.opacity = "0.8"
    })

    shape.addEventListener("mouseleave", function () {
        this.style.animationPlayState = "running"
        this.style.transform = ""
        this.style.opacity = ""
    })
})

// Enhanced brand logo interaction
const brandLogo = document.querySelector(".brand-logo")
if (brandLogo) {
    brandLogo.addEventListener("click", function () {
        this.style.animation = "pulse 0.6s ease-in-out"
        setTimeout(() => {
            this.style.animation = ""
        }, 600)

        alertSystem.info(
            "Fajar Gold",
            "Koleksi perhiasan emas berkualitas tinggi dengan desain eksklusif dan pelayanan terpercaya sejak 1995.",
            5000,
        )
    })
}

// Enhanced performance optimization
document.addEventListener("DOMContentLoaded", () => {
    // Preload critical animations
    const preloadAnimations = ["slideInUp", "fadeInDown", "float", "rotate", "pulse"]

    // Add intersection observer for better performance
    const observer = new IntersectionObserver((entries) => {
        entries.forEach((entry) => {
            if (entry.isIntersecting) {
                entry.target.classList.add("animate")
            }
        })
    })

    // Observe animated elements
    document.querySelectorAll(".shape, .brand-logo, .auth-form").forEach((el) => {
        observer.observe(el)
    })
})

// Enhanced error handling
window.addEventListener("error", (e) => {
    console.error("JavaScript Error:", e.error)
    alertSystem.error(
        "Terjadi Kesalahan",
        "Mohon refresh halaman atau hubungi customer service jika masalah berlanjut.",
        8000,
    )
})

// Enhanced accessibility improvements
document.addEventListener("keydown", (e) => {
    if (e.key === "Tab") {
        document.body.classList.add("keyboard-navigation")
    }
})

document.addEventListener("mousedown", () => {
    document.body.classList.remove("keyboard-navigation")
})

// Add focus styles for keyboard navigation
const focusStyle = document.createElement("style")
focusStyle.textContent = `
    .keyboard-navigation *:focus {
        outline: 3px solid rgba(212, 175, 55, 0.6) !important;
        outline-offset: 2px !important;
    }
`
document.head.appendChild(focusStyle)

console.log("Fajar Gold - Enhanced Authentication System Loaded Successfully! ✨")



