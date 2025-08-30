// Fungsi untuk kembali ke halaman sebelumnya
function goBack() {
    if (window.history.length > 1) {
        window.history.back();
    } else {
        // Jika tidak ada history, redirect ke home
        goHome();
    }
}

// Fungsi untuk kembali ke beranda
function goHome() {
    // Ganti dengan URL beranda yang sesuai
    window.location.href = '/';
}

// Animasi sederhana saat halaman dimuat
document.addEventListener('DOMContentLoaded', function() {
    // Tambahkan efek hover pada tombol
    const buttons = document.querySelectorAll('.btn-primary, .btn-secondary');
    
    buttons.forEach(button => {
        button.addEventListener('mouseenter', function() {
            this.style.transform = 'translateY(-1px)';
        });
        
        button.addEventListener('mouseleave', function() {
            this.style.transform = 'translateY(0)';
        });
    });
});

// Keyboard shortcuts
document.addEventListener('keydown', function(e) {
    // ESC untuk kembali
    if (e.key === 'Escape') {
        goBack();
    }
    
    // Home key untuk ke beranda
    if (e.key === 'Home') {
        goHome();
    }
});