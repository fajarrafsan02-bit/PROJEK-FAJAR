class BestSellingProducts {
    constructor() {
        this.container = document.getElementById('bestSellingProductsContainer');
        this.loadingElement = document.getElementById('bestSellingProductsLoading');
        this.errorElement = document.getElementById('bestSellingProductsError');
        this.init();
    }
    
    async init() {
        if (!this.container) {
            console.warn('BestSellingProducts container element not found');
            return;
        }
        
        await this.loadBestSellingProducts();
    }
    
    async loadBestSellingProducts() {
        this.showLoading(true);
        this.hideError();
        
        try {
            const response = await fetch('/api/best-selling/top?limit=5');
            const result = await response.json();
            
            if (result.success && result.data) {
                this.renderBestSellingProducts(result.data);
            } else {
                throw new Error(result.message || 'Gagal memuat produk terlaris');
            }
        } catch (error) {
            console.error('Error loading best selling products:', error);
            this.showError('Gagal memuat produk terlaris: ' + error.message);
        } finally {
            this.showLoading(false);
        }
    }
    
    renderBestSellingProducts(products) {
        if (!products || products.length === 0) {
            this.container.innerHTML = `
                <div class="best-selling-products-no-data">
                    <i class="fas fa-chart-line"></i>
                    <h3>Belum Ada Produk Terlaris</h3>
                    <p>Produk terlaris akan muncul setelah ada pembelian.</p>
                </div>
            `;
            return;
        }
        
        const html = products.map((product, index) => {
            const rank = index + 1;
            const rankStyles = this.getRankStyles(rank);
            
            return `
                <div class="best-selling-product-card fade-in-up stagger-animation" style="animation-delay: ${index * 0.1}s;">
                    <div class="product-rank" ${rankStyles.style}>
                        <span class="rank-number">${rank}</span>
                        ${rankStyles.icon ? `<i class="${rankStyles.icon}"></i>` : ''}
                    </div>
                    <div class="product-image">
                        <img src="${product.productImage || '/placeholder.svg?height=200&width=200&text=Produk'}" 
                             alt="${product.productName}"
                             onerror="this.src='/placeholder.svg?height=200&width=200&text=Produk'">
                    </div>
                    <div class="product-info">
                        <h3 class="product-name">${product.productName}</h3>
                        <p class="product-category">${product.productCategory || 'Kategori'}</p>
                        <div class="product-sales">
                            <i class="fas fa-fire"></i>
                            <span class="sales-count">${product.salesCount || 0} terjual</span>
                        </div>
                        <div class="product-price">${this.formatCurrency(product.productPrice)}</div>
                    </div>
                    <button class="add-to-cart-btn" 
                            data-id="${product.productId}"
                            data-name="${product.productName.replace(/"/g, '&quot;')}"
                            data-price="${product.productPrice}"
                            data-image="${product.productImage || ''}"
                            onclick="bestSellingProducts.addToCart(this)">
                        <i class="fas fa-cart-plus"></i> Tambah ke Keranjang
                    </button>
                </div>
            `;
        }).join('');
        
        this.container.innerHTML = `
            <div class="best-selling-products-grid">
                ${html}
            </div>
        `;
        
        // Setup animation observer untuk produk baru
        setTimeout(() => {
            const productCards = this.container.querySelectorAll('.best-selling-product-card');
            productCards.forEach(card => {
                card.classList.add('animate');
            });
        }, 100);
    }
    
    getRankStyles(rank) {
        switch (rank) {
            case 1:
                return {
                    style: 'background: linear-gradient(135deg, #ffd700 0%, #ffed4e 100%); color: #000;',
                    icon: 'fas fa-medal'
                };
            case 2:
                return {
                    style: 'background: linear-gradient(135deg, #c0c0c0 0%, #e6e6e6 100%); color: #000;',
                    icon: 'fas fa-award'
                };
            case 3:
                return {
                    style: 'background: linear-gradient(135deg, #cd7f32 0%, #e0bb9d 100%); color: #fff;',
                    icon: 'fas fa-trophy'
                };
            default:
                return {
                    style: 'background: #f0f0f0; color: #333;',
                    icon: ''
                };
        }
    }
    
    formatCurrency(amount) {
        return new Intl.NumberFormat('id-ID', {
            style: 'currency',
            currency: 'IDR',
            minimumFractionDigits: 0,
            maximumFractionDigits: 0,
        }).format(Number(amount) || 0);
    }
    
    async addToCart(button) {
        const productData = {
            id: button.dataset.id,
            name: button.dataset.name,
            price: parseFloat(button.dataset.price) || 0,
            image: button.dataset.image,
            quantity: 1
        };
        
        // Disable tombol saat proses
        button.disabled = true;
        const originalContent = button.innerHTML;
        button.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Menambahkan...';
        
        try {
            const userId = await this.getCurrentUserId();
            if (!userId) {
                this.showNotification('Silakan login terlebih dahulu', 'error');
                return;
            }
            
            const response = await fetch('/user/api/cart/add', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    userId: userId,
                    productId: productData.id,
                    quantity: productData.quantity
                })
            });
            
            const result = await response.json();
            
            if (result.success) {
                this.showNotification('Produk berhasil ditambahkan ke keranjang!', 'success');
                
                // Update keranjang badge jika ada
                if (typeof CartBadgeManager !== 'undefined' && CartBadgeManager.triggerCartUpdate) {
                    CartBadgeManager.triggerCartUpdate({
                        action: 'add',
                        productId: productData.id,
                        productName: productData.name,
                        source: 'best-selling-products',
                        timestamp: new Date().toISOString()
                    });
                }
                
                if (typeof window.cartBadgeManager !== 'undefined' && window.cartBadgeManager.refreshCart) {
                    setTimeout(async () => {
                        await window.cartBadgeManager.refreshCart();
                    }, 100);
                }
            } else {
                this.showNotification('Gagal menambahkan ke keranjang: ' + (result.message || 'Kesalahan tidak diketahui'), 'error');
            }
        } catch (error) {
            console.error('Error adding product to cart:', error);
            this.showNotification('Gagal menambahkan ke keranjang: ' + error.message, 'error');
        } finally {
            // Kembalikan tombol ke kondisi awal
            button.disabled = false;
            button.innerHTML = originalContent;
        }
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
    
    showNotification(message, type = 'info') {
        // Membuat elemen notifikasi
        const notification = document.createElement('div');
        notification.className = `notification-toast ${type}`;
        notification.innerHTML = `
            <div class="toast-icon">
                <i class="fas fa-${type === 'success' ? 'check-circle' : type === 'error' ? 'exclamation-triangle' : 'info-circle'}"></i>
            </div>
            <div class="toast-content">
                <div class="toast-message">${message}</div>
            </div>
        `;
        
        // Menambahkan ke body
        document.body.appendChild(notification);
        
        // Menghapus notifikasi setelah 3 detik
        setTimeout(() => {
            notification.classList.add('hide');
            setTimeout(() => {
                if (notification.parentNode) {
                    notification.parentNode.removeChild(notification);
                }
            }, 300);
        }, 3000);
    }
    
    showLoading(show) {
        if (this.loadingElement) {
            this.loadingElement.style.display = show ? 'flex' : 'none';
        }
    }
    
    showError(message) {
        if (this.errorElement) {
            this.errorElement.textContent = message;
            this.errorElement.style.display = 'block';
        }
    }
    
    hideError() {
        if (this.errorElement) {
            this.errorElement.style.display = 'none';
        }
    }
}

// Inisialisasi saat DOM siap
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
        window.bestSellingProducts = new BestSellingProducts();
    });
} else {
    window.bestSellingProducts = new BestSellingProducts();
}