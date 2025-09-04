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
                // User belum login: coba fallback ke cart lokal bila ada, tanpa error bising
                const local = this.safeGetLocalCart();
                this.cart = local.items;
                this.totalItems = local.totalItems;
                console.log('CartBadgeManager: Guest mode, using local cart. Items:', this.cart.length);
                return;
            }

            const response = await fetch(`/user/api/cart?userId=${userId}`, {
                method: 'GET',
                headers: { 'Accept': 'application/json' },
                credentials: 'same-origin'
            });
            if (response.ok) {
                const result = await response.json();
                if (result.success && result.data) {
                    this.cart = result.data.items || [];
                    this.totalItems = result.data.totalItems || 0;
                    console.log('Cart loaded from database:', this.cart, 'Total items:', this.totalItems);
                } else {
                    console.warn('Gagal load cart (response body):', result && result.message);
                    const local = this.safeGetLocalCart();
                    this.cart = local.items;
                    this.totalItems = local.totalItems;
                }
            } else {
                console.warn('HTTP error loading cart:', response.status);
                const local = this.safeGetLocalCart();
                this.cart = local.items;
                this.totalItems = local.totalItems;
            }
        } catch (error) {
            console.warn('Error loading cart (network?):', error);
            const local = this.safeGetLocalCart();
            this.cart = local.items;
            this.totalItems = local.totalItems;
        }
    }

    updateCartBadge() {
        // Update HANYA cart badge di halaman, JANGAN wishlist badge
        const cartBadges = document.querySelectorAll('.nav-icon[title="Keranjang"] .cart-badge, #cartBadge');

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

        // JANGAN update wishlist badge di sini!
        console.log('CartBadgeManager: Wishlist badges are managed separately and not affected by cart changes');
    }

    async getCurrentUserId() {
        try {
            const response = await fetch('/user/current', {
                method: 'GET',
                headers: { 'Accept': 'application/json' },
                credentials: 'same-origin'
            });
            if (response.ok) {
                const result = await response.json();
                if (result && result.success && result.data) {
                    console.log('Current user from API:', result.data.email, 'ID:', result.data.id);
                    return result.data.id;
                }
            } else {
                console.warn('Failed to get current user, HTTP status:', response.status);
            }
        } catch (error) {
            console.warn('Error getting current user (network?):', error);
        }
        // Tidak ada fallback ke user ID statis agar tidak memicu fetch yang gagal/403
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

    // Ambil cart lokal secara aman (guest mode) tanpa mengganggu fitur lain
    safeGetLocalCart() {
        try {
            // Coba beberapa kemungkinan key yang mungkin digunakan oleh halaman lain
            const possibleKeys = ['cart', 'cartItems', 'cart_list'];
            for (let i = 0; i < possibleKeys.length; i++) {
                const raw = localStorage.getItem(possibleKeys[i]);
                if (raw) {
                    try {
                        const parsed = JSON.parse(raw);
                        if (Array.isArray(parsed)) {
                            return { items: parsed, totalItems: parsed.length };
                        }
                        if (parsed && Array.isArray(parsed.items)) {
                            const total = typeof parsed.totalItems === 'number' ? parsed.totalItems : parsed.items.length;
                            return { items: parsed.items, totalItems: total };
                        }
                    } catch (_) {
                        // abaikan parsing error untuk key ini dan lanjutkan
                    }
                }
            }
        } catch (_) {
            // akses localStorage bisa gagal di mode tertentu; abaikan dengan aman
        }
        return { items: [], totalItems: 0 };
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
