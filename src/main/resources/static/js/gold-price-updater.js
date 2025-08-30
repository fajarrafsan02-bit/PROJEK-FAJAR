/**
 * Gold Price Updater - Menangani update harga emas dengan persentase perubahan
 * File ini dibuat terpisah untuk tidak mengubah kode yang sudah ada
 */

class GoldPriceUpdater {
    constructor() {
        this.currentPrice = 0;
        this.previousPrice = 0;
        this.lastUpdateTime = null;
        this.updateInterval = null;
        this.init();
    }

    async init() {
        console.log('GoldPriceUpdater initialized');
        await this.loadGoldPrice();
        this.startAutoUpdate();
    }

    async loadGoldPrice() {
        try {
            // Coba endpoint utama dulu
            const response = await fetch('/gold-price/latest');
            if (response.ok) {
                const data = await response.json();
                if (data.success && data.data?.data?.hargaJual24k) {
                    const price = this.parsePrice(data.data.data.hargaJual24k);
                    if (price > 0) {
                        this.updatePrice(price);
                        return;
                    }
                }
            }

            // Fallback ke endpoint history jika endpoint utama gagal
            await this.loadFromHistory();
        } catch (error) {
            console.error('Error loading gold price:', error);
            await this.loadFromHistory();
        }
    }

    async loadFromHistory() {
        try {
            const response = await fetch('/gold-price/history?page=0&size=2&sortBy=tanggalAmbil&sortDirection=desc');
            if (response.ok) {
                const data = await response.json();
                if (data.success && data.data?.content && data.data.content.length > 0) {
                    const latestPrice = this.parsePrice(data.data.content[0].hargaJual24k);
                    if (latestPrice > 0) {
                        this.updatePrice(latestPrice);
                        
                        // Jika ada data sebelumnya, hitung perubahan
                        if (data.data.content.length > 1) {
                            const previousPrice = this.parsePrice(data.data.content[1].hargaJual24k);
                            if (previousPrice > 0) {
                                this.calculateAndDisplayChange(latestPrice, previousPrice, data.data.content[1].tanggalAmbil);
                            }
                        }
                    }
                }
            }
        } catch (error) {
            console.error('Error loading from history:', error);
        }
    }

    parsePrice(priceString) {
        if (!priceString) return 0;
        return parseInt(String(priceString).replace(/[^\d]/g, '')) || 0;
    }

    updatePrice(newPrice) {
        if (newPrice <= 0) return;
        
        this.previousPrice = this.currentPrice;
        this.currentPrice = newPrice;
        this.lastUpdateTime = new Date();
        
        this.updateDisplay();
        
        // Jika ada perubahan harga, hitung dan tampilkan perubahan
        if (this.previousPrice > 0 && this.previousPrice !== this.currentPrice) {
            this.calculateAndDisplayChange(this.currentPrice, this.previousPrice, this.lastUpdateTime);
        }
    }

    calculateAndDisplayChange(currentPrice, previousPrice, changeDate) {
        const changeAmount = currentPrice - previousPrice;
        const changePercent = ((changeAmount / previousPrice) * 100);
        
        let changeType = 'STABLE';
        if (changeAmount > 0) changeType = 'INCREASE';
        else if (changeAmount < 0) changeType = 'DECREASE';
        
        this.displayPriceChange(changeAmount, changePercent, changeType, changeDate);
    }

    displayPriceChange(changeAmount, changePercent, changeType, changeDate) {
        const priceChangeElement = document.getElementById('priceChange');
        const priceChangeTextElement = document.getElementById('priceChangeText');
        const iconElement = priceChangeElement?.querySelector('i');
        
        if (!priceChangeElement || !priceChangeTextElement || !iconElement) {
            console.warn('Price change elements not found');
            return;
        }

        // Format waktu perubahan
        let timeText = this.formatTimeAgo(changeDate);
        
        if (changeType === 'STABLE' || changeAmount === 0) {
            priceChangeTextElement.textContent = 'Tidak ada perubahan';
            priceChangeElement.className = 'price-change neutral';
            iconElement.className = 'fas fa-minus';
        } else if (changeType === 'INCREASE') {
            const formattedChange = this.formatCurrency(Math.abs(changeAmount));
            priceChangeTextElement.textContent = `+${changePercent.toFixed(1)}% (${formattedChange}) dari ${timeText}`;
            priceChangeElement.className = 'price-change positive';
            iconElement.className = 'fas fa-arrow-up';
        } else if (changeType === 'DECREASE') {
            const formattedChange = this.formatCurrency(Math.abs(changeAmount));
            priceChangeTextElement.textContent = `${changePercent.toFixed(1)}% (${formattedChange}) dari ${timeText}`;
            priceChangeElement.className = 'price-change negative';
            iconElement.className = 'fas fa-arrow-down';
        }
        
        console.log('Price change displayed:', {
            changeAmount,
            changePercent,
            changeType,
            timeText
        });
    }

    formatTimeAgo(date) {
        if (!date) return 'sebelumnya';
        
        try {
            const changeDate = new Date(date);
            const now = new Date();
            const diffTime = now.getTime() - changeDate.getTime();
            
            const diffMinutes = Math.floor(diffTime / (1000 * 60));
            const diffHours = Math.floor(diffTime / (1000 * 60 * 60));
            const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24));
            
            if (diffMinutes < 1) {
                return 'baru saja';
            } else if (diffMinutes < 60) {
                return `${diffMinutes} menit yang lalu`;
            } else if (diffHours < 24) {
                return diffHours === 1 ? '1 jam yang lalu' : `${diffHours} jam yang lalu`;
            } else if (diffDays === 1) {
                return 'kemarin';
            } else if (diffDays > 1) {
                return `${diffDays} hari yang lalu`;
            } else {
                // Jika tanggal sama, tampilkan waktu
                const changeDateStr = changeDate.toDateString();
                const nowDateStr = now.toDateString();
                
                if (changeDateStr === nowDateStr) {
                    const hours = changeDate.getHours().toString().padStart(2, '0');
                    const minutes = changeDate.getMinutes().toString().padStart(2, '0');
                    return `hari ini jam ${hours}:${minutes}`;
                } else {
                    return 'sebelumnya';
                }
            }
        } catch (e) {
            console.warn('Error formatting time:', e);
            return 'sebelumnya';
        }
    }

    formatCurrency(amount) {
        return new Intl.NumberFormat('id-ID', {
            style: 'currency',
            currency: 'IDR',
            minimumFractionDigits: 0,
            maximumFractionDigits: 0
        }).format(amount);
    }

    updateDisplay() {
        const priceElement = document.getElementById('goldPrice');
        if (priceElement && this.currentPrice > 0) {
            priceElement.textContent = this.formatCurrency(this.currentPrice) + '/gram';
        }
    }

    startAutoUpdate() {
        // Update setiap 5 menit
        this.updateInterval = setInterval(async () => {
            console.log('Auto-updating gold price...');
            await this.loadGoldPrice();
        }, 300000); // 5 menit
    }

    stopAutoUpdate() {
        if (this.updateInterval) {
            clearInterval(this.updateInterval);
            this.updateInterval = null;
        }
    }

    // Method untuk force refresh
    async forceRefresh() {
        console.log('Force refreshing gold price...');
        await this.loadGoldPrice();
    }
}

// Inisialisasi ketika DOM ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
        window.goldPriceUpdater = new GoldPriceUpdater();
    });
} else {
    window.goldPriceUpdater = new GoldPriceUpdater();
}

// Export untuk penggunaan global
window.GoldPriceUpdater = GoldPriceUpdater;