registerForm.addEventListener('submit', async function (e) {
    e.preventDefault();

    const firstName = document.getElementById("firstName").value
    const lastName = document.getElementById("lastName").value
    const email = document.getElementById("registerEmail").value
    const phone = document.getElementById("phoneNumber").value
    const password = document.getElementById("registerPassword").value
    const registerBtn = document.getElementById("registerBtn")

    if (!validateName(firstName)) {
        alertSystem.error("Nama Tidak Valid", "Nama depan harus minimal 2 karakter")
        addShakeAnimation(document.getElementById("firstName").closest(".input-wrapper"))
        return
    }

    if (!validateName(lastName)) {
        alertSystem.error("Nama Tidak Valid", "Nama belakang harus minimal 2 karakter")
        addShakeAnimation(document.getElementById("lastName").closest(".input-wrapper"))
        return
    }

    if (!validateEmail(email)) {
        alertSystem.error("Email Tidak Valid", "Silakan masukkan alamat email yang benar")
        addShakeAnimation(document.getElementById("registerEmail").closest(".input-wrapper"))
        return
    }

    if (!validatePhone(phone)) {
        alertSystem.error("Nomor Telepon Tidak Valid", "Nomor telepon harus 10-13 digit")
        addShakeAnimation(document.getElementById("phoneNumber").closest(".input-wrapper"))
        return
    }

    if (!validatePassword(password)) {
        alertSystem.error("Password Tidak Valid", errorMessage)
        addShakeAnimation(document.getElementById("registerPassword").closest(".input-wrapper"))
        return
    }

    setLoading(registerBtn, true);
    alertSystem.info('Membuat Akun', 'Sedang memproses registrasi Anda...', 2500);

    try {
        // Simulate API call
        // await new Promise(resolve => setTimeout(resolve, 2500));

        const [emailResponse, hpResponse, hpTerdaftarResponse] = await Promise.all([
            fetch(`/validasi/cek-email?email=${encodeURIComponent(email)}`),
            fetch(`/validasi/cek-nomor-hp?nomorHp=${encodeURIComponent(phone)}`),
            fetch(`/validasi/cek-daftar-hp?nomorHp=${encodeURIComponent(phone)}`)]);
        const emailTerdaftar = await emailResponse.json();
        const nomorHpValid = await hpResponse.json();
        const nomorHpDaftar = await hpTerdaftarResponse.json();

        if (nomorHpValid.status === "error") {
            alertSystem.error('Nomor HP Tidak Valid', nomorHpValid.message, 4000);
            document.getElementById('phoneNumber').focus();
            addShakeAnimation(registerForm);
            return;
        }

        if (nomorHpDaftar) {
            console.log("INI SUDAH MASUK 1")
            alertSystem.error('Nomor HP Tidak Valid', 'Nomor Hp Anda Sudah Terdaftar di Akun Yang Berbeda, Gunakan Nomor Hp Yang Berbeda.');
            addShakeAnimation(document.getElementById("phoneNumber").closest(".input-wrapper"))
            addShakeAnimation(registerForm);
            return;
        }
        
        if (!nomorHpValid) {
            console.log("INI SUDAH MASUK 2")
            alertSystem.error('Nomor HP Tidak Valid', 'Nomor Hp Anda Tidak Valid, Berikan Nomor Hp yang Aktif.');
            addShakeAnimation(document.getElementById("phoneNumber").closest(".input-wrapper"))
            addShakeAnimation(registerForm);
            return
        }
        
        if (emailTerdaftar) {
            console.log("INI SUDAH MASUK 3")
            alertSystem.error('Email Tidak Valid', 'Email sudah terdaftar. Gunakan email lain.');
            addShakeAnimation(document.getElementById("registerEmail").closest(".input-wrapper"))
            addShakeAnimation(registerForm);
            return;
        }

        fetch(`/auth/register`, {
            method: 'POST',
            body: new FormData(document.getElementById('registerForm'))
        })
            .then(async response => {
                if (!response.ok) {
                    console.log("INI SAYA");
                    alertSystem.error(
                        'Error!',
                        'Terjadi kesalahan saat memproses permintaan.',
                        5000
                    );
                    addShakeAnimation(registerForm);
                    return;
                }
                const result = await response.json();
                alertSystem.success(
                    'Registrasi Berhasil! 🎉',
                    `Selamat datang ${firstName} ${lastName}! Akun Anda telah berhasil dibuat.`,
                    5000
                );
                registerForm.style.transform = 'scale(1.02)';
                setTimeout(() => {
                    registerForm.style.transform = 'scale(1)';
                }, 200);
                setTimeout(() => {
                    registerForm.reset();
                    alertSystem.success(
                        'Akun Siap Digunakan',
                        'Silakan login dengan akun baru Anda untuk mulai berbelanja!'
                    );

                    setTimeout(() => {
                        authContainer.classList.remove('show-register');
                        document.getElementById('loginEmail').value = email;
                        alertSystem.info('Formulir Login', 'Email Anda telah diisi otomatis. Masukkan password untuk login.', 3000);
                    }, 4000);
                }, 5000);
            }).catch(errorNya => {
                nsole.error('Error:', errorNya);
                alertSystem.error(
                    'Kesalahan Sistem',
                    'Gagal Menghubungi Server.',
                    5000
                );
            })

        // Check if email already exists (simulation)
        // const existingEmails = ['admin@aureliagold.com', 'user@aureliagold.com'];
        // if (existingEmails.includes(email)) {
        //     alertSystem.error(
        //         'Email Sudah Terdaftar',
        //         'Email ini sudah digunakan. Silakan gunakan email lain atau login dengan akun yang ada.',
        //         5000
        //     );
        //     addShakeAnimation(registerForm);
        //     return;
        // }

        // // Simulate successful registration
        // alertSystem.success(
        //     'Registrasi Berhasil! 🎉',
        //     `Selamat datang ${firstName} ${lastName}! Akun Anda telah berhasil dibuat.`,
        //     5000
        // );

        // Success animation
        // registerForm.style.transform = 'scale(1.02)';
        // setTimeout(() => {
        //     registerForm.style.transform = 'scale(1)';
        // }, 200);

        // Reset form and redirect to login
        // setTimeout(() => {
        //     registerForm.reset();
        //     alertSystem.success(
        //         'Akun Siap Digunakan',
        //         'Silakan login dengan akun baru Anda untuk mulai berbelanja!',
        //         4000
        //     );

        //     setTimeout(() => {
        //         authContainer.classList.remove('show-register');
        //         document.getElementById('loginEmail').value = email;
        //         alertSystem.info('Formulir Login', 'Email Anda telah diisi otomatis. Masukkan password untuk login.', 3000);
        //     }, 4000);
        // }, 5000);

    } catch (error) {
        alertSystem.error(
            'Kesalahan Sistem',
            'Terjadi kesalahan saat membuat akun. Silakan coba lagi dalam beberapa saat.',
            5000
        );
    } finally {
        setLoading(registerBtn, false);
    }
});