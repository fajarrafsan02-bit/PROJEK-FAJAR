// Utility untuk mengelola cart badge di semua halaman
class CartBadgeManager {
    constructor() {
        this.cart = [];
        this.init();
    }

    async init() {
        await this.loadCartFromDatabase();
        this.updateCartBadge();
        this.startAutoUpdate();
    }

    async loadCartFromDatabase() {
        try {
            const userId = await this.getCurrentUserId();
            if (!userId) {
                console.log('User belum login, cart kosong');
                this.cart = [];
                return;
            }

            const response = await fetch(`/api/cart?userId=${userId}`);
            if (response.ok) {
                const result = await response.json();
                if (result.success) {
                    this.cart = result.data.items || [];
                    console.log('Cart loaded from database:', this.cart);
                } else {
                    console.error('Gagal load cart:', result.message);
                    this.cart = [];
                }
            } else {
                console.error('HTTP error loading cart:', response.status);
                this.cart = [];
            }
        } catch (error) {
            console.error('Error loading cart:', error);
            this.cart = [];
        }
    }

    updateCartBadge() {
        // Update semua cart badge di halaman
        const cartBadges = document.querySelectorAll('.nav-icon[title="Keranjang"] .cart-badge, .cart-badge');
        
        cartBadges.forEach(badge => {
            // Hitung jumlah produk unik (bukan total quantity)
            const uniqueProductCount = this.cart.length;
            badge.textContent = uniqueProductCount;
            console.log('Cart badge updated:', uniqueProductCount, 'unique products');
        });
    }

    async getCurrentUserId() {
        try {
            const response = await fetch('/user/current');
            if (response.ok) {
                const result = await response.json();
                if (result.success && result.data) {
                    return result.data.id;
                }
            }
        } catch (error) {
            console.error('Error getting current user:', error);
        }
        return null;
    }

    startAutoUpdate() {
        // Update cart badge setiap 1 menit
        setInterval(async () => {
            await this.loadCartFromDatabase();
            this.updateCartBadge();
        }, 60000); // 1 menit
    }

    // Method untuk refresh cart setelah operasi add/remove
    async refreshCart() {
        await this.loadCartFromDatabase();
        this.updateCartBadge();
    }
}

// Auto-initialize jika DOM sudah ready dan tidak ada manager lain
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
        // Hanya inisialisasi jika tidak ada HomepageManager
        if (!window.homepageManager) {
            window.cartBadgeManager = new CartBadgeManager();
        }
    });
} else {
    // Hanya inisialisasi jika tidak ada HomepageManager
    if (!window.homepageManager) {
        window.cartBadgeManager = new CartBadgeManager();
    }
}
