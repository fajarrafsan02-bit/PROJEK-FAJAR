// Login form submission
loginForm.addEventListener('submit', async function (e) {
    e.preventDefault();

    const email = document.getElementById("loginEmail").value
    const password = document.getElementById("loginPassword").value
    const loginBtn = document.getElementById("loginBtn")

    console.log(email);
    console.log(password);

    if (!validateEmail(email)) {
        alertSystem.error("Email Tidak Valid", "Silakan masukkan alamat email yang benar")
        addShakeAnimation(document.getElementById("loginEmail").closest(".input-wrapper"))
        return
    }

    if (!password) {
        alertSystem.error("Password Kosong", "Silakan masukkan password Anda")
        addShakeAnimation(document.getElementById("loginPassword").closest(".input-wrapper"))
        return
    }

    setLoading(loginBtn, true)
    alertSystem.info('Memproses Login', 'Sedang memverifikasi kredensial Anda...', 2000);

    try {
        // await new Promise(resolve => setTimeout(resolve, 2000));
        const response = await fetch("/auth/login", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ email, password })
        });

        const hasil = await response.json();

        if (response.ok) {
            if (hasil.status === "token-required") {
                alertSystem.success("Verifikasi Diperlukan", "Email verifikasi telah dikirim. Silakan cek kotak masuk Anda.");

                loginForm.style.transform = 'scale(1.02)';
                setTimeout(() => {
                    loginForm.style.transform = 'scale(1)';
                }, 200);

                alertSystem.success('Mengalihkan...', 'Menuju dashboard Admin Aurelia Gold');
                setTimeout(() => {
                    window.location.href = '/auth/verifikasi-token';
                }, 3000);
            } else if (hasil.status === "success") {
                alertSystem.success(
                    "Login Berhasil! 🎉",
                    "Selamat datang kembali! Anda akan diarahkan ke halaman utama."
                );
                loginForm.style.transform = 'scale(1.02)';
                setTimeout(() => loginForm.style.transform = 'scale(1)', 200);

                alertSystem.success('Mengalihkan...', 'Menuju dashboard User Aurelia Gold');
                setTimeout(() => {
                    window.location.href = '/user/home';
                }, 3000);
            }
        } else {
            alertSystem.error(
                'Login Gagal! ❌',
                'Email atau password yang Anda masukkan salah. Silakan periksa kembali.',
                5000
            );
            addShakeAnimation(loginForm);
        }
    } catch (error) {
        alertSystem.error(
            'Kesalahan Sistem',
            'Terjadi kesalahan pada server. Silakan coba lagi dalam beberapa saat.',
            5000
        );
    } finally {
        setLoading(loginBtn, false);
    }
});
