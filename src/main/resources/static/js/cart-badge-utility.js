// Utility untuk mengelola cart badge di semua halaman
class CartBadgeManager {
    constructor() {
        this.cart = [];
        this.totalItems = 0;
        this.init();
    }

    async init() {
        await this.loadCartFromDatabase();
        this.updateCartBadge();
        this.startAutoUpdate();
        this.setupEventListeners();
    }

    async loadCartFromDatabase() {
        try {
            const userId = await this.getCurrentUserId();
            if (!userId) {
                console.log('User belum login, cart kosong');
                this.cart = [];
                this.totalItems = 0;
                return;
            }

            const response = await fetch(`/user/api/cart?userId=${userId}`);
            if (response.ok) {
                const result = await response.json();
                if (result.success && result.data) {
                    this.cart = result.data.items || [];
                    this.totalItems = result.data.totalItems || 0;
                    console.log('Cart loaded from database:', this.cart, 'Total items:', this.totalItems);
                } else {
                    console.error('Gagal load cart:', result.message);
                    this.cart = [];
                    this.totalItems = 0;
                }
            } else {
                console.error('HTTP error loading cart:', response.status);
                this.cart = [];
                this.totalItems = 0;
            }
        } catch (error) {
            console.error('Error loading cart:', error);
            this.cart = [];
            this.totalItems = 0;
        }
    }

    updateCartBadge() {
        // Update semua cart badge di halaman
        const cartBadges = document.querySelectorAll('.nav-icon[title="Keranjang"] .cart-badge, .cart-badge');

        cartBadges.forEach(badge => {
            // Hitung jumlah produk unik (bukan total quantity)
            const uniqueProducts = this.cart ? this.cart.length : 0;
            badge.textContent = uniqueProducts;

            // Sembunyikan badge jika kosong
            if (uniqueProducts === 0) {
                badge.style.display = 'none';
            } else {
                badge.style.display = 'inline-block';
            }

            console.log('CartBadgeManager: Cart badge updated:', uniqueProducts, 'unique products (from', this.totalItems, 'total items)');
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
        // Update cart badge setiap 30 detik (lebih sering untuk responsivitas)
        setInterval(async () => {
            await this.loadCartFromDatabase();
            this.updateCartBadge();
        }, 30000); // 30 detik
    }

    // Setup event listeners untuk real-time updates
    setupEventListeners() {
        // Listen untuk custom cart events
        window.addEventListener('cartUpdated', async (event) => {
            console.log('CartBadgeManager: Cart updated event received', event.detail);
            await this.refreshCart();
        });

        // Listen untuk storage events (untuk sinkronisasi antar tab)
        window.addEventListener('storage', (event) => {
            if (event.key === 'cartChanged') {
                console.log('CartBadgeManager: Storage cart change detected');
                this.refreshCart();
            }
        });

        // Listen untuk focus events (refresh saat user kembali ke tab)
        window.addEventListener('focus', async () => {
            console.log('CartBadgeManager: Window focused, refreshing cart');
            await this.refreshCart();
        });

        // Listen untuk visibility change events
        document.addEventListener('visibilitychange', async () => {
            if (!document.hidden) {
                console.log('CartBadgeManager: Page visible, refreshing cart');
                await this.refreshCart();
            }
        });
    }

    // Method untuk refresh cart setelah operasi add/remove
    async refreshCart() {
        await this.loadCartFromDatabase();
        this.updateCartBadge();
    }

    // Method untuk trigger cart update event (untuk digunakan oleh halaman lain)
    static triggerCartUpdate(details = {}) {
        // Dispatch custom event
        const event = new CustomEvent('cartUpdated', {
            detail: { ...details, timestamp: new Date().toISOString() }
        });
        window.dispatchEvent(event);

        // Set storage flag untuk sinkronisasi antar tab
        localStorage.setItem('cartChanged', Date.now().toString());
        
        console.log('CartBadgeManager: Cart update event triggered', details);
    }

    // Method untuk force refresh dari external
    static async forceRefresh() {
        if (window.cartBadgeManager) {
            await window.cartBadgeManager.refreshCart();
        }
    }
}

// Auto-initialize jika DOM sudah ready dan tidak ada manager lain
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
        // Hanya inisialisasi jika tidak ada HomepageManager
        if (!window.homepageManager) {
            console.log('CartBadgeManager: Initializing CartBadgeManager...');
            window.cartBadgeManager = new CartBadgeManager();
            console.log('CartBadgeManager: Initialized successfully');
        } else {
            console.log('CartBadgeManager: HomepageManager detected, skipping initialization');
        }
    });
} else {
    // Hanya inisialisasi jika tidak ada HomepageManager
    if (!window.homepageManager) {
        console.log('CartBadgeManager: Initializing CartBadgeManager (DOM already loaded)...');
        window.cartBadgeManager = new CartBadgeManager();
        console.log('CartBadgeManager: Initialized successfully');
    } else {
        console.log('CartBadgeManager: HomepageManager detected, skipping initialization');
    }
}
