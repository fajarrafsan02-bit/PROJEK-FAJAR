// Enhanced Custom Alert System
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
        `

        this.container.appendChild(alertElement)
        this.alerts.push({ id: alertId, element: alertElement, duration })

        setTimeout(() => {
            alertElement.classList.add("show")
        }, 100)

        const autoHideTimeout = setTimeout(() => this.hide(alertId), duration)

        const closeBtn = alertElement.querySelector(".alert-close")
        closeBtn.addEventListener("click", (e) => {
            e.stopPropagation()
            clearTimeout(autoHideTimeout)
            this.hide(alertId)
        })

        alertElement.addEventListener("click", (e) => {
            if (!e.target.classList.contains("alert-close") && !e.target.closest(".alert-close")) {
                clearTimeout(autoHideTimeout)
                this.hide(alertId)
            }
        })

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

// Initialize alert system
const alertSystem = new AlertSystem()

// JWT Token Verification System
class JWTTokenVerification {
    constructor() {
        this.jwtInput = document.getElementById("jwtToken")
        this.verifyBtn = document.getElementById("verifyBtn")
        this.resendBtn = document.getElementById("resendBtn")
        this.timerElement = document.getElementById("timerCountdown")
        this.timerContainer = document.getElementById("timerContainer")
        this.resendTimer = document.getElementById("resendTimer")
        this.verificationForm = document.getElementById("verificationForm")
        this.successState = document.getElementById("successState")
        this.continueBtn = document.getElementById("continueBtn")

        // JWT specific elements
        this.pasteBtn = document.getElementById("pasteBtn")
        this.clearBtn = document.getElementById("clearBtn")
        this.decodeBtn = document.getElementById("decodeBtn")
        this.jwtModal = document.getElementById("jwtModal")
        this.closeModal = document.getElementById("closeModal")
        this.headerStatus = document.getElementById("headerStatus")
        this.payloadStatus = document.getElementById("payloadStatus")
        this.signatureStatus = document.getElementById("signatureStatus")

        this.tokenExpiry = 5 * 60 // 5 minutes in seconds
        this.resendCooldown = 60 // 60 seconds
        this.currentToken = ""
        this.isVerifying = false
        this.jwtParts = null

        this.init()
    }

    init() {
        this.setupEventListeners()
        this.startTimer()
        this.startResendCooldown()
        this.showWelcomeMessage()
    }

    setupEventListeners() {
        this.jwtInput.addEventListener("input", (e) => this.handleJWTInput(e))
        this.jwtInput.addEventListener("paste", (e) => this.handlePaste(e))
        this.jwtInput.addEventListener("focus", () => this.handleFocus())
        this.jwtInput.addEventListener("blur", () => this.handleBlur())

        this.pasteBtn.addEventListener("click", () => this.handlePasteClick())
        this.clearBtn.addEventListener("click", () => this.handleClear())
        this.decodeBtn.addEventListener("click", () => this.handleDecode())

        this.verificationForm.addEventListener("submit", (e) => this.handleSubmit(e))
        this.resendBtn.addEventListener("click", () => this.handleResend())
        this.continueBtn.addEventListener("click", () => this.handleContinue())

        this.closeModal.addEventListener("click", () => this.hideModal())
        this.jwtModal.addEventListener("click", (e) => {
            if (e.target === this.jwtModal) this.hideModal()
        })

        // Back to login
        document.getElementById("backToLogin").addEventListener("click", (e) => {
            e.preventDefault()
            this.handleBackToLogin()
        })
    }

    handleJWTInput(e) {
        this.currentToken = e.target.value.trim()
        this.validateJWT()
        this.updateUI()
    }

    handlePaste(e) {
        setTimeout(() => {
            this.currentToken = this.jwtInput.value.trim()
            this.validateJWT()
            this.updateUI()
            if (this.isValidJWT()) {
                alertSystem.success("Token JWT Ditempel", "Token JWT berhasil ditempel dan tervalidasi")
            }
        }, 100)
    }

    async handlePasteClick() {
        try {
            const text = await navigator.clipboard.readText()
            this.jwtInput.value = text.trim()
            this.currentToken = text.trim()
            this.validateJWT()
            this.updateUI()

            if (this.isValidJWT()) {
                alertSystem.success("Token JWT Ditempel", "Token JWT berhasil ditempel dari clipboard")
            } else {
                alertSystem.warning("Format Token", "Pastikan token yang ditempel adalah format JWT yang valid")
            }
        } catch (error) {
            alertSystem.error("Gagal Menempel", "Tidak dapat mengakses clipboard. Silakan tempel manual dengan Ctrl+V")
        }
    }

    handleClear() {
        this.jwtInput.value = ""
        this.currentToken = ""
        this.jwtParts = null
        this.updateUI()
        this.jwtInput.focus()
        alertSystem.info("Token Dihapus", "Token JWT telah dihapus dari form")
    }

    handleDecode() {
        if (!this.isValidJWT()) {
            alertSystem.warning("Token Tidak Valid", "Masukkan token JWT yang valid terlebih dahulu")
            return
        }

        try {
            const parts = this.currentToken.split(".")
            const header = JSON.parse(this.base64UrlDecode(parts[0]))
            const payload = JSON.parse(this.base64UrlDecode(parts[1]))

            document.getElementById("jwtHeader").textContent = JSON.stringify(header, null, 2)
            document.getElementById("jwtPayload").textContent = JSON.stringify(payload, null, 2)
            document.getElementById("jwtSignature").textContent = "Signature verification requires server-side validation"

            this.showModal()
        } catch (error) {
            alertSystem.error("Gagal Decode", "Tidak dapat mendecode token JWT. Pastikan format token benar")
        }
    }

    validateJWT() {
        if (!this.currentToken) {
            this.jwtParts = null
            return false
        }

        const parts = this.currentToken.split(".")

        if (parts.length !== 3) {
            this.jwtParts = { valid: false, parts: parts.length }
            return false
        }

        try {
            // Validate each part can be base64 decoded
            const header = this.base64UrlDecode(parts[0])
            const payload = this.base64UrlDecode(parts[1])

            // Try to parse as JSON
            JSON.parse(header)
            JSON.parse(payload)

            this.jwtParts = {
                valid: true,
                header: true,
                payload: true,
                signature: true, // We assume signature is present if format is correct
            }

            return true
        } catch (error) {
            this.jwtParts = {
                valid: false,
                header: this.isValidBase64Url(parts[0]),
                payload: this.isValidBase64Url(parts[1]),
                signature: parts[2] && parts[2].length > 0,
            }
            return false
        }
    }

    isValidJWT() {
        return this.jwtParts && this.jwtParts.valid
    }

    isValidBase64Url(str) {
        try {
            this.base64UrlDecode(str)
            return true
        } catch {
            return false
        }
    }

    base64UrlDecode(str) {
        // Convert base64url to base64
        str = str.replace(/-/g, "+").replace(/_/g, "/")

        // Add padding if needed
        while (str.length % 4) {
            str += "="
        }

        return atob(str)
    }

    updateUI() {
        // Update input styling
        this.jwtInput.classList.remove("valid", "invalid")
        if (this.currentToken) {
            if (this.isValidJWT()) {
                this.jwtInput.classList.add("valid")
            } else {
                this.jwtInput.classList.add("invalid")
            }
        }

        // Update JWT parts status
        this.updatePartStatus("header", this.jwtParts?.header)
        this.updatePartStatus("payload", this.jwtParts?.payload)
        this.updatePartStatus("signature", this.jwtParts?.signature)

        // Update verify button
        this.verifyBtn.disabled = !this.isValidJWT() || this.isVerifying

        // Update action buttons
        this.clearBtn.disabled = !this.currentToken
        this.decodeBtn.disabled = !this.isValidJWT()
    }

    updatePartStatus(partName, isValid) {
        const partElement = document.querySelector(`.jwt-part.${partName}`)
        const statusElement = document.getElementById(`${partName}Status`)

        partElement.classList.remove("valid", "invalid")

        if (isValid === true) {
            partElement.classList.add("valid")
        } else if (isValid === false) {
            partElement.classList.add("invalid")
        }
    }

    handleFocus() {
        document.querySelector(".jwt-input-wrapper").classList.add("focused")
    }

    handleBlur() {
        setTimeout(() => {
            document.querySelector(".jwt-input-wrapper").classList.remove("focused")
        }, 100)
    }

    showModal() {
        this.jwtModal.classList.add("show")
        document.body.style.overflow = "hidden"
    }

    hideModal() {
        this.jwtModal.classList.remove("show")
        document.body.style.overflow = ""
    }

    async handleSubmit(e) {
        e.preventDefault()

        if (this.isVerifying || !this.isValidJWT()) {
            return
        }

        this.isVerifying = true
        this.verifyBtn.classList.add("loading")

        try {
            // Call your Spring Boot REST controller
            const result = await this.verifyJWTToken(this.currentToken)

            if (result.success) {
                this.showSuccess(result.redirect)
            } else {
                alertSystem.error(
                    "Token Tidak Valid",
                    result.pesan || result.message || "Token JWT yang Anda masukkan tidak valid atau sudah kedaluwarsa",
                )
            }
        } catch (error) {
            console.error("Verification error:", error)
            alertSystem.error("Kesalahan Verifikasi", "Terjadi kesalahan saat memverifikasi token JWT. Silakan coba lagi.")
        } finally {
            this.isVerifying = false
            this.verifyBtn.classList.remove("loading")
        }
    }

    async verifyJWTToken(token) {
        try {
            const params = new URLSearchParams();
            params.append("token", this.currentToken);

            const response = await fetch("/auth/verifikasi-token", {
                method: "POST",
                headers: {
                    "Content-Type": "application/x-www-form-urlencoded"
                },
                body: params.toString()
            });
            console.log(token);
            console.log("INI TOKEN SAYA " + JSON.stringify({
                token: token
            }));
            const data = await response.json()

            if (response.ok) {
                return {
                    success: true,
                    pesan: data.pesan,
                    redirect: data.redirect
                }
            } else {
                return {
                    success: false,
                    pesan: data.pesan || data.message,
                    status: response.status
                }
            }
        } catch (error) {
            console.error("Network error:", error)
            throw new Error("Gagal terhubung ke server. Periksa koneksi internet Anda.")
        }
    }

    showSuccess(redirectUrl = null) {
        this.verificationForm.style.display = "none"
        this.successState.style.display = "block"

        alertSystem.success("Verifikasi JWT Berhasil!", "Token JWT Anda telah berhasil diverifikasi di Fajar Gold")

        // Store redirect URL for later use
        this.redirectUrl = redirectUrl

        // Auto redirect after 3 seconds
        setTimeout(() => {
            this.handleContinue()
        }, 3000)
    }

    handleContinue() {
        const redirectUrl = this.redirectUrl || "admin/home"
        alertSystem.info("Mengalihkan...", `Anda akan diarahkan ke ${redirectUrl}`)

        setTimeout(() => {
            window.location.href = redirectUrl
        }, 1500)
    }

    async handleResend() {
        if (this.resendBtn.disabled) return

        this.resendBtn.disabled = true
        this.resendBtn.style.opacity = "0.6"

        try {
            await new Promise((resolve) => setTimeout(resolve, 1500))

            alertSystem.success(
                "Token JWT Baru Terkirim",
                "Token JWT verifikasi baru telah dikirim ke email Anda. Periksa inbox dan folder spam.",
            )

            // Reset timer
            this.tokenExpiry = 5 * 60
            this.timerContainer.classList.remove("expired")

            // Clear current token
            this.handleClear()

            // Start cooldown again
            this.startResendCooldown()
        } catch (error) {
            alertSystem.error("Gagal Mengirim", "Terjadi kesalahan saat mengirim ulang token JWT. Silakan coba lagi.")
            this.resendBtn.disabled = false
            this.resendBtn.style.opacity = "1"
        }
    }

    handleBackToLogin() {
        alertSystem.info("Kembali ke Login", "Anda akan diarahkan ke halaman login")
        setTimeout(() => {
            window.location.href = "index.html"
        }, 1000)
    }

    startTimer() {
        const timer = setInterval(() => {
            if (this.tokenExpiry <= 0) {
                clearInterval(timer)
                this.handleTokenExpired()
                return
            }

            const minutes = Math.floor(this.tokenExpiry / 60)
            const seconds = this.tokenExpiry % 60
            this.timerElement.textContent = `${minutes.toString().padStart(2, "0")}:${seconds.toString().padStart(2, "0")}`

            if (this.tokenExpiry <= 60) {
                this.timerContainer.classList.add("warning")
            }

            this.tokenExpiry--
        }, 1000)
    }

    startResendCooldown() {
        let cooldown = this.resendCooldown
        this.resendBtn.disabled = true
        this.resendBtn.style.opacity = "0.6"
        document.getElementById("resendCooldown").style.display = "block"

        const cooldownTimer = setInterval(() => {
            if (cooldown <= 0) {
                clearInterval(cooldownTimer)
                this.resendBtn.disabled = false
                this.resendBtn.style.opacity = "1"
                document.getElementById("resendCooldown").style.display = "none"
                return
            }

            this.resendTimer.textContent = cooldown
            cooldown--
        }, 1000)
    }

    handleTokenExpired() {
        this.timerContainer.classList.add("expired")
        this.timerElement.textContent = "00:00"
        this.verifyBtn.disabled = true

        alertSystem.warning(
            "Sesi Verifikasi Kedaluwarsa",
            "Sesi verifikasi telah kedaluwarsa. Silakan minta token JWT baru dengan menekan tombol 'Kirim Ulang Kode'.",
        )

        // Disable JWT input
        this.jwtInput.disabled = true
        this.jwtInput.style.opacity = "0.5"
    }

    showWelcomeMessage() {
        setTimeout(() => {
            alertSystem.info(
                "Verifikasi Token JWT 🔐",
                "Tempel token JWT yang telah dikirim ke email Anda untuk menyelesaikan proses verifikasi akun Fajar Gold. Token akan divalidasi secara real-time.",
                7000,
            )
        }, 1500)
    }
}

// Initialize JWT Token Verification
const jwtTokenVerification = new JWTTokenVerification()

// Enhanced keyboard shortcuts for JWT
document.addEventListener("keydown", (e) => {
    if (e.key === "Escape") {
        // Close modal if open
        if (jwtTokenVerification.jwtModal.classList.contains("show")) {
            jwtTokenVerification.hideModal()
            return
        }

        // Close all alerts
        alertSystem.alerts.forEach((alert) => {
            alertSystem.hide(alert.id)
        })
    }

    if (e.key === "Enter" && e.ctrlKey) {
        // Quick submit if JWT is valid
        if (jwtTokenVerification.isValidJWT()) {
            jwtTokenVerification.verificationForm.dispatchEvent(new Event("submit"))
        }
    }

    if (e.key === "r" && e.ctrlKey) {
        // Quick resend
        e.preventDefault()
        if (!jwtTokenVerification.resendBtn.disabled) {
            jwtTokenVerification.handleResend()
        }
    }

    if (e.key === "v" && e.ctrlKey && e.target !== jwtTokenVerification.jwtInput) {
        // Quick paste to JWT input
        e.preventDefault()
        jwtTokenVerification.handlePasteClick()
    }

    if (e.key === "d" && e.ctrlKey) {
        // Quick decode
        e.preventDefault()
        if (jwtTokenVerification.isValidJWT()) {
            jwtTokenVerification.handleDecode()
        }
    }
})

// Auto-focus JWT input on load
window.addEventListener("load", () => {
    setTimeout(() => {
        if (jwtTokenVerification.jwtInput) {
            jwtTokenVerification.jwtInput.focus()
        }
    }, 1000)
})

console.log("Fajar Gold - JWT Token Verification System with Spring Boot Integration Loaded Successfully! 🔐")

// Enhanced accessibility
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

// Enhanced floating shapes interaction
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
            "Fajar Gold Security",
            "Sistem keamanan berlapis untuk melindungi akun dan data pribadi Anda dengan teknologi enkripsi terdepan.",
            5000,
        )
    })
}

// Performance optimization
document.addEventListener("DOMContentLoaded", () => {
    // Add intersection observer for better performance
    const observer = new IntersectionObserver((entries) => {
        entries.forEach((entry) => {
            if (entry.isIntersecting) {
                entry.target.classList.add("animate")
            }
        })
    })

    // Observe animated elements
    document.querySelectorAll(".shape, .brand-logo, .verification-form").forEach((el) => {
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