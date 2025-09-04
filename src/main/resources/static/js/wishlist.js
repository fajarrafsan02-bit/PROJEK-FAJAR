/**
 * Wishlist Management System
 * Integrated with Spring Boot Backend API
 */
class WishlistManager {
    constructor() {
        this.wishlist = [];
        this.selectedItems = new Set();
        this.isLoading = false;
        
        this.init();
    }

    init() {
        this.setupEventListeners();
        this.loadWishlist();
        this.updateCartBadge(); // Update cart badge from localStorage
    }

    setupEventListeners() {
        // Search functionality
        const searchBtn = document.getElementById('searchBtn');
        const searchInput = document.getElementById('searchInput');
        
        if (searchBtn) searchBtn.addEventListener('click', () => this.handleSearch());
        if (searchInput) {
            searchInput.addEventListener('keypress', (e) => {
                if (e.key === 'Enter') this.handleSearch();
            });
        }

        // Bulk actions
        const selectAll = document.getElementById('selectAll');
        const addAllToCart = document.getElementById('addAllToCart');
        const removeSelected = document.getElementById('removeSelected');
        const addAllToCartSummary = document.getElementById('addAllToCartSummary');
        const shareWishlist = document.getElementById('shareWishlist');
        const sortSelect = document.getElementById('sortSelect');

        if (selectAll) selectAll.addEventListener('change', (e) => this.toggleSelectAll(e.target.checked));
        if (addAllToCart) addAllToCart.addEventListener('click', () => this.addSelectedToCart());
        if (removeSelected) removeSelected.addEventListener('click', () => this.removeSelectedItems());
        if (addAllToCartSummary) addAllToCartSummary.addEventListener('click', () => this.addAllToCart());
        if (shareWishlist) shareWishlist.addEventListener('click', () => this.shareWishlist());
        if (sortSelect) sortSelect.addEventListener('change', (e) => this.sortWishlist(e.target.value));

        // Mobile menu toggle
        const mobileToggle = document.querySelector('.mobile-menu-toggle');
        const navMenu = document.querySelector('.nav-menu');
        
        if (mobileToggle && navMenu) {
            mobileToggle.addEventListener('click', () => {
                navMenu.style.display = navMenu.style.display === 'flex' ? 'none' : 'flex';
            });
        }
    }

    async loadWishlist() {
        if (this.isLoading) return;
        
        this.isLoading = true;
        this.showLoading(true);
        
        try {
            console.log('🔄 Loading wishlist data...');
            const response = await fetch('/user/api/wishlist', {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                },
                credentials: 'include'
            });

            const result = await response.json();
            console.log('📦 Wishlist response:', result);

            if (result.success && result.data) {
                this.wishlist = result.data.items || [];
                this.updateUI();
                console.log(`✅ Loaded ${this.wishlist.length} wishlist items`);
            } else {
                if (response.status === 401) {
                    this.showNotification('Silakan login terlebih dahulu', 'warning');
                    // Redirect to login page after 2 seconds
                    setTimeout(() => {
                        window.location.href = '/auth/login';
                    }, 2000);
                    return;
                }
                
                console.warn('⚠️ Failed to load wishlist:', result.message);
                this.wishlist = [];
                this.updateUI();
            }
        } catch (error) {
            console.error('❌ Error loading wishlist:', error);
            this.showNotification('Gagal memuat wishlist. Silakan refresh halaman.', 'error');
            this.wishlist = [];
            this.updateUI();
        } finally {
            this.isLoading = false;
            this.showLoading(false);
        }
    }

    async addToWishlist(productId, notes = null) {
        try {
            console.log('💝 Adding product to wishlist:', productId);
            
            const response = await fetch('/user/api/wishlist/add', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                credentials: 'include',
                body: JSON.stringify({
                    productId: productId,
                    notes: notes
                })
            });

            const result = await response.json();
            
            if (result.success) {
                console.log('✅ Added to wishlist:', result.data);
                this.showNotification(result.message, 'success');
                
                // Reload wishlist to get updated data
                await this.loadWishlist();
                return true;
            } else {
                console.warn('⚠️ Failed to add to wishlist:', result.message);
                this.showNotification(result.message, 'error');
                return false;
            }
        } catch (error) {
            console.error('❌ Error adding to wishlist:', error);
            this.showNotification('Gagal menambahkan ke wishlist', 'error');
            return false;
        }
    }

    async removeFromWishlist(productId) {
        if (!confirm('Apakah Anda yakin ingin menghapus produk ini dari wishlist?')) {
            return false;
        }

        try {
            console.log('🗑️ Removing product from wishlist:', productId);
            
            const response = await fetch(`/user/api/wishlist/remove/${productId}`, {
                method: 'DELETE',
                headers: {
                    'Content-Type': 'application/json',
                },
                credentials: 'include'
            });

            const result = await response.json();
            
            if (result.success) {
                console.log('✅ Removed from wishlist:', result.data);
                this.showNotification(result.message, 'success');
                
                // Remove from local array
                this.wishlist = this.wishlist.filter(item => item.product.id !== productId);
                this.selectedItems.delete(productId);
                this.updateUI();
                return true;
            } else {
                console.warn('⚠️ Failed to remove from wishlist:', result.message);
                this.showNotification(result.message, 'error');
                return false;
            }
        } catch (error) {
            console.error('❌ Error removing from wishlist:', error);
            this.showNotification('Gagal menghapus dari wishlist', 'error');
            return false;
        }
    }

    async removeMultipleFromWishlist(wishlistIds) {
        if (!wishlistIds || wishlistIds.length === 0) {
            this.showNotification('Pilih item yang ingin dihapus', 'warning');
            return false;
        }

        if (!confirm(`Apakah Anda yakin ingin menghapus ${wishlistIds.length} produk dari wishlist?`)) {
            return false;
        }

        try {
            console.log('🗑️ Removing multiple items from wishlist:', wishlistIds);
            
            const response = await fetch('/user/api/wishlist/remove-multiple', {
                method: 'DELETE',
                headers: {
                    'Content-Type': 'application/json',
                },
                credentials: 'include',
                body: JSON.stringify({
                    wishlistIds: wishlistIds
                })
            });

            const result = await response.json();
            
            if (result.success) {
                console.log('✅ Removed multiple items from wishlist:', result.data);
                this.showNotification(result.message, 'success');
                
                // Remove from local array
                this.wishlist = this.wishlist.filter(item => !wishlistIds.includes(item.id));
                this.selectedItems.clear();
                this.updateUI();
                return true;
            } else {
                console.warn('⚠️ Failed to remove multiple items:', result.message);
                this.showNotification(result.message, 'error');
                return false;
            }
        } catch (error) {
            console.error('❌ Error removing multiple items:', error);
            this.showNotification('Gagal menghapus dari wishlist', 'error');
            return false;
        }
    }

    async clearWishlist() {
        if (!confirm('Apakah Anda yakin ingin mengosongkan seluruh wishlist?')) {
            return false;
        }

        try {
            console.log('🗑️ Clearing entire wishlist');
            
            const response = await fetch('/user/api/wishlist/clear', {
                method: 'DELETE',
                headers: {
                    'Content-Type': 'application/json',
                },
                credentials: 'include'
            });

            const result = await response.json();
            
            if (result.success) {
                console.log('✅ Cleared wishlist:', result.data);
                this.showNotification(result.message, 'success');
                
                this.wishlist = [];
                this.selectedItems.clear();
                this.updateUI();
                return true;
            } else {
                console.warn('⚠️ Failed to clear wishlist:', result.message);
                this.showNotification(result.message, 'error');
                return false;
            }
        } catch (error) {
            console.error('❌ Error clearing wishlist:', error);
            this.showNotification('Gagal mengosongkan wishlist', 'error');
            return false;
        }
    }

    updateUI() {
        this.renderWishlist();
        this.updateStats();
        this.updateWishlistBadge();
        this.updateBulkActionsVisibility();
    }

    renderWishlist() {
        const emptyWishlist = document.getElementById('emptyWishlist');
        const wishlistGrid = document.getElementById('wishlistGrid');
        const wishlistActions = document.getElementById('wishlistActions');
        const wishlistSummary = document.getElementById('wishlistSummary');

        if (this.wishlist.length === 0) {
            // Show empty state
            if (emptyWishlist) emptyWishlist.style.display = 'block';
            if (wishlistGrid) wishlistGrid.style.display = 'none';
            if (wishlistActions) wishlistActions.style.display = 'none';
            if (wishlistSummary) wishlistSummary.style.display = 'none';
        } else {
            // Show wishlist items
            if (emptyWishlist) emptyWishlist.style.display = 'none';
            if (wishlistGrid) wishlistGrid.style.display = 'grid';
            if (wishlistActions) wishlistActions.style.display = 'flex';
            if (wishlistSummary) wishlistSummary.style.display = 'block';

            this.renderWishlistItems();
        }
    }

    renderWishlistItems() {
        const wishlistGrid = document.getElementById('wishlistGrid');
        if (!wishlistGrid) return;

        wishlistGrid.innerHTML = '';

        this.wishlist.forEach(item => {
            const wishlistItem = this.createWishlistItemElement(item);
            wishlistGrid.appendChild(wishlistItem);
        });
    }

    createWishlistItemElement(item) {
        const div = document.createElement('div');
        div.className = 'wishlist-item';
        div.innerHTML = `
            <input type="checkbox" class="item-checkbox" data-id="${item.id}" ${this.selectedItems.has(item.id) ? 'checked' : ''}>
            
            <div class="product-image">
                <img src="${item.product.imageUrl || '/images/default-product.jpg'}" alt="${item.product.name}" onerror="this.src='/images/default-product.jpg'">
                <div class="date-added">${item.formattedDate}</div>
            </div>
            
            <button class="remove-btn" onclick="wishlistManager.removeFromWishlist(${item.product.id})">
                <i class="fas fa-times"></i>
            </button>
            
            <div class="product-info">
                <h3 class="product-name">${item.product.name}</h3>
                <div class="product-specs">
                    <span class="spec-item">
                        <i class="fas fa-weight"></i>
                        ${item.product.weight}g
                    </span>
                    <span class="spec-item">
                        <i class="fas fa-certificate"></i>
                        ${item.product.purity}%
                    </span>
                </div>
                <div class="product-price">${item.product.formattedPrice}</div>
                <div class="product-availability ${item.product.isAvailable ? 'available' : 'unavailable'}">
                    <i class="fas ${item.product.isAvailable ? 'fa-check-circle' : 'fa-times-circle'}"></i>
                    ${item.product.isAvailable ? 'Tersedia' : 'Stok Habis'}
                </div>
            </div>
            
            <div class="item-actions">
                <button class="action-btn primary" onclick="wishlistManager.addToCart(${item.product.id})" ${!item.product.isAvailable ? 'disabled' : ''}>
                    <i class="fas fa-cart-plus"></i>
                    Tambah ke Keranjang
                </button>
                <a href="/product/${item.product.id}" class="action-btn secondary">
                    <i class="fas fa-eye"></i>
                    Lihat Detail
                </a>
            </div>
            
            ${item.notes ? `<div class="product-notes">
                <i class="fas fa-sticky-note"></i>
                ${item.notes}
            </div>` : ''}
        `;

        // Add event listener for checkbox
        const checkbox = div.querySelector('.item-checkbox');
        checkbox.addEventListener('change', (e) => {
            if (e.target.checked) {
                this.selectedItems.add(item.id);
            } else {
                this.selectedItems.delete(item.id);
            }
            this.updateSelectAllState();
        });

        return div;
    }

    async addToCart(productId) {
        try {
            // Find product in wishlist
            const wishlistItem = this.wishlist.find(item => item.product.id === productId);
            if (!wishlistItem) {
                this.showNotification('Produk tidak ditemukan', 'error');
                return;
            }

            const product = wishlistItem.product;
            
            // Check if product is available
            if (!product.isAvailable) {
                this.showNotification('Produk tidak tersedia', 'warning');
                return;
            }

            // Add to cart API call
            console.log('🛒 Adding to cart:', productId);
            
            const response = await fetch('/user/api/cart/add', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                credentials: 'include',
                body: JSON.stringify({
                    productId: productId,
                    quantity: 1
                })
            });

            const result = await response.json();
            
            if (result.success) {
                console.log('✅ Added to cart:', result.data);
                this.showNotification(`${product.name} berhasil ditambahkan ke keranjang`, 'success');
                this.updateCartBadge();
            } else {
                console.warn('⚠️ Failed to add to cart:', result.message);
                this.showNotification(result.message, 'error');
            }
        } catch (error) {
            console.error('❌ Error adding to cart:', error);
            this.showNotification('Gagal menambahkan ke keranjang', 'error');
        }
    }

    toggleSelectAll(checked) {
        this.selectedItems.clear();
        
        if (checked) {
            this.wishlist.forEach(item => {
                this.selectedItems.add(item.id);
            });
        }

        // Update all checkboxes
        document.querySelectorAll('.item-checkbox').forEach(checkbox => {
            checkbox.checked = checked;
        });
    }

    updateSelectAllState() {
        const selectAllCheckbox = document.getElementById('selectAll');
        if (!selectAllCheckbox) return;

        const totalItems = this.wishlist.length;
        const selectedCount = this.selectedItems.size;

        if (selectedCount === 0) {
            selectAllCheckbox.checked = false;
            selectAllCheckbox.indeterminate = false;
        } else if (selectedCount === totalItems) {
            selectAllCheckbox.checked = true;
            selectAllCheckbox.indeterminate = false;
        } else {
            selectAllCheckbox.checked = false;
            selectAllCheckbox.indeterminate = true;
        }
    }

    async addSelectedToCart() {
        const selectedItems = Array.from(this.selectedItems);
        if (selectedItems.length === 0) {
            this.showNotification('Pilih produk yang ingin ditambahkan ke keranjang', 'warning');
            return;
        }

        let successCount = 0;
        let failCount = 0;

        for (const wishlistId of selectedItems) {
            const wishlistItem = this.wishlist.find(item => item.id === wishlistId);
            if (wishlistItem && wishlistItem.product.isAvailable) {
                const success = await this.addToCart(wishlistItem.product.id);
                if (success !== false) successCount++;
                else failCount++;
            } else {
                failCount++;
            }
        }

        if (successCount > 0) {
            this.showNotification(`${successCount} produk berhasil ditambahkan ke keranjang`, 'success');
        }
        if (failCount > 0) {
            this.showNotification(`${failCount} produk gagal ditambahkan`, 'warning');
        }

        this.selectedItems.clear();
        this.updateSelectAllState();
    }

    async removeSelectedItems() {
        const selectedIds = Array.from(this.selectedItems);
        await this.removeMultipleFromWishlist(selectedIds);
    }

    async addAllToCart() {
        const availableItems = this.wishlist.filter(item => item.product.isAvailable);
        
        if (availableItems.length === 0) {
            this.showNotification('Tidak ada produk yang tersedia untuk ditambahkan ke keranjang', 'warning');
            return;
        }

        let successCount = 0;
        let failCount = 0;

        for (const item of availableItems) {
            const success = await this.addToCart(item.product.id);
            if (success !== false) successCount++;
            else failCount++;
        }

        if (successCount > 0) {
            this.showNotification(`${successCount} produk berhasil ditambahkan ke keranjang`, 'success');
        }
        if (failCount > 0) {
            this.showNotification(`${failCount} produk gagal ditambahkan`, 'warning');
        }
    }

    sortWishlist(sortBy) {
        switch (sortBy) {
            case 'newest':
                this.wishlist.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
                break;
            case 'oldest':
                this.wishlist.sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt));
                break;
            case 'price-high':
                this.wishlist.sort((a, b) => b.product.finalPrice - a.product.finalPrice);
                break;
            case 'price-low':
                this.wishlist.sort((a, b) => a.product.finalPrice - b.product.finalPrice);
                break;
            case 'name':
                this.wishlist.sort((a, b) => a.product.name.localeCompare(b.product.name));
                break;
        }
        
        this.renderWishlistItems();
    }

    updateStats() {
        const totalItems = this.wishlist.length;
        const availableItems = this.wishlist.filter(item => item.product.isAvailable).length;
        const totalValue = this.wishlist.reduce((sum, item) => {
            return sum + (item.product.isAvailable ? item.product.finalPrice : 0);
        }, 0);
        const avgPrice = availableItems > 0 ? totalValue / availableItems : 0;

        // Update stats display
        this.updateElement('totalItems', totalItems.toString());
        this.updateElement('totalValue', this.formatCurrency(totalValue));
        this.updateElement('avgPrice', this.formatCurrency(avgPrice));
        this.updateElement('summaryTotal', this.formatCurrency(totalValue));
    }

    updateWishlistBadge() {
        this.updateElement('wishlistBadge', this.wishlist.length.toString());
    }

    updateCartBadge() {
        // Update cart badge from localStorage or API
        const cart = JSON.parse(localStorage.getItem('cart')) || [];
        const totalCartItems = cart.reduce((sum, item) => sum + (item.quantity || 1), 0);
        this.updateElement('cartBadge', totalCartItems.toString());
    }

    updateBulkActionsVisibility() {
        const wishlistActions = document.getElementById('wishlistActions');
        if (wishlistActions) {
            wishlistActions.style.display = this.wishlist.length > 0 ? 'flex' : 'none';
        }
    }

    shareWishlist() {
        const wishlistData = {
            items: this.wishlist.map(item => ({
                name: item.product.name,
                price: item.product.formattedPrice,
                category: item.product.category
            })),
            totalValue: this.formatCurrency(this.wishlist.reduce((sum, item) => 
                sum + (item.product.isAvailable ? item.product.finalPrice : 0), 0)),
            itemCount: this.wishlist.length
        };

        const shareText = `Lihat wishlist emas saya di Fajar Gold!\n\n${wishlistData.itemCount} produk dengan total nilai ${wishlistData.totalValue}\n\nKunjungi: ${window.location.origin}`;

        if (navigator.share) {
            navigator.share({
                title: 'Wishlist Fajar Gold',
                text: shareText,
                url: window.location.href
            });
        } else {
            // Fallback: copy to clipboard
            navigator.clipboard.writeText(shareText).then(() => {
                this.showNotification('Link wishlist berhasil disalin!', 'success');
            });
        }
    }

    handleSearch() {
        const query = document.getElementById('searchInput')?.value.trim();
        if (query) {
            window.location.href = `/katalog?search=${encodeURIComponent(query)}`;
        }
    }

    // Utility methods
    updateElement(id, content) {
        const element = document.getElementById(id);
        if (element) {
            element.textContent = content;
        }
    }

    formatCurrency(amount) {
        return new Intl.NumberFormat('id-ID', {
            style: 'currency',
            currency: 'IDR',
            minimumFractionDigits: 0
        }).format(amount);
    }

    showLoading(show) {
        const wishlistGrid = document.getElementById('wishlistGrid');
        if (!wishlistGrid) return;

        if (show) {
            wishlistGrid.innerHTML = `
                <div style="text-align: center; padding: 50px; grid-column: 1/-1;">
                    <div class="loading"></div>
                    <p style="margin-top: 15px; color: var(--gray-elegant);">Memuat wishlist...</p>
                </div>
            `;
        }
    }

    showNotification(message, type = 'info') {
        const notification = document.createElement('div');
        notification.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            background: ${type === 'success' ? '#10b981' : type === 'error' ? '#ef4444' : type === 'warning' ? '#f59e0b' : '#3b82f6'};
            color: white;
            padding: 15px 20px;
            border-radius: 10px;
            box-shadow: 0 10px 30px rgba(0,0,0,0.2);
            z-index: 10000;
            font-weight: 500;
            max-width: 300px;
            animation: slideIn 0.3s ease-out;
        `;
        notification.textContent = message;
        document.body.appendChild(notification);

        setTimeout(() => {
            notification.style.animation = 'slideOut 0.3s ease-in';
            setTimeout(() => {
                if (notification.parentNode) {
                    notification.parentNode.removeChild(notification);
                }
            }, 300);
        }, 3000);
    }
}

// Global functions for onclick handlers
window.wishlistManager = null;

// Initialize wishlist manager when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    window.wishlistManager = new WishlistManager();
    console.log('💝 Wishlist Manager initialized');
});

// Export for use in other files
if (typeof module !== 'undefined' && module.exports) {
    module.exports = WishlistManager;
}
