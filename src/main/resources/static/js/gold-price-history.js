/**
 * Gold Price History Handler - Menangani riwayat perubahan harga emas
 * File ini dibuat terpisah untuk tidak mengubah kode yang sudah ada
 */

class GoldPriceHistoryHandler {
    constructor() {
        this.priceHistory = [];
        this.currentPrice = 0;
        this.init();
    }

    async init() {
        console.log('GoldPriceHistoryHandler initialized');
        await this.loadPriceHistory();
        this.startPeriodicUpdate();
    }

    async loadPriceHistory() {
        try {
            // Coba ambil riwayat harga emas dari database
            const response = await fetch('/gold-price/history?page=0&size=10&sortBy=tanggalAmbil&sortDirection=desc');
            
            if (response.ok) {
                const data = await response.json();
                if (data.success && data.data?.content) {
                    this.priceHistory = data.data.content;
                    this.processPriceHistory();
                } else {
                    console.warn('No price history data available');
                }
            } else {
                console.error('Failed to fetch price history:', response.status);
            }
        } catch (error) {
            console.error('Error loading price history:', error);
        }
    }

    processPriceHistory() {
        if (this.priceHistory.length === 0) return;

        // Ambil harga terbaru
        const latestPrice = this.parsePrice(this.priceHistory[0].hargaJual24k);
        if (latestPrice > 0) {
            this.currentPrice = latestPrice;
        }

        // Hitung perubahan dari data yang ada
        if (this.priceHistory.length >= 2) {
            const currentPrice = this.parsePrice(this.priceHistory[0].hargaJual24k);
            const previousPrice = this.parsePrice(this.priceHistory[1].hargaJual24k);
            
            if (currentPrice > 0 && previousPrice > 0) {
                this.calculatePriceChange(currentPrice, previousPrice, this.priceHistory[1].tanggalAmbil);
            }
        }

        // Update tampilan dengan data yang ada
        this.updateDisplay();
    }

    parsePrice(priceString) {
        if (!priceString) return 0;
        return parseInt(String(priceString).replace(/[^\d]/g, '')) || 0;
    }

    calculatePriceChange(currentPrice, previousPrice, changeDate) {
        const changeAmount = currentPrice - previousPrice;
        const changePercent = ((changeAmount / previousPrice) * 100);
        
        let changeType = 'STABLE';
        if (changeAmount > 0) changeType = 'INCREASE';
        else if (changeAmount < 0) changeType = 'DECREASE';
        
        this.displayPriceChange(changeAmount, changePercent, changeType, changeDate);
        
        console.log('Price change calculated:', {
            currentPrice,
            previousPrice,
            changeAmount,
            changePercent: changePercent.toFixed(2) + '%',
            changeType,
            changeDate
        });
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
            
            // Tambahkan animasi success
            setTimeout(() => {
                priceChangeElement.classList.add('success');
                setTimeout(() => {
                    priceChangeElement.classList.remove('success');
                }, 600);
            }, 100);
        } else if (changeType === 'DECREASE') {
            const formattedChange = this.formatCurrency(Math.abs(changeAmount));
            priceChangeTextElement.textContent = `${changePercent.toFixed(1)}% (${formattedChange}) dari ${timeText}`;
            priceChangeElement.className = 'price-change negative';
            iconElement.className = 'fas fa-arrow-down';
            
            // Tambahkan animasi error
            setTimeout(() => {
                priceChangeElement.classList.add('error');
                setTimeout(() => {
                    priceChangeElement.classList.remove('error');
                }, 600);
            }, 100);
        }
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
        // Update harga emas jika ada
        const priceElement = document.getElementById('goldPrice');
        if (priceElement && this.currentPrice > 0) {
            priceElement.textContent = this.formatCurrency(this.currentPrice) + '/gram';
        }
    }

    startPeriodicUpdate() {
        // Update setiap 10 menit
        setInterval(async () => {
            console.log('Periodically updating gold price history...');
            await this.loadPriceHistory();
        }, 600000); // 10 menit
    }

    // Method untuk force refresh
    async forceRefresh() {
        console.log('Force refreshing gold price history...');
        await this.loadPriceHistory();
    }

    // Method untuk mendapatkan statistik perubahan harga
    getPriceStatistics() {
        if (this.priceHistory.length < 2) return null;

        const prices = this.priceHistory
            .map(item => this.parsePrice(item.hargaJual24k))
            .filter(price => price > 0);

        if (prices.length < 2) return null;

        const changes = [];
        for (let i = 0; i < prices.length - 1; i++) {
            const change = prices[i] - prices[i + 1];
            const changePercent = ((change / prices[i + 1]) * 100);
            changes.push({
                amount: change,
                percent: changePercent,
                type: change > 0 ? 'INCREASE' : change < 0 ? 'DECREASE' : 'STABLE'
            });
        }

        const totalIncrease = changes.filter(c => c.type === 'INCREASE').length;
        const totalDecrease = changes.filter(c => c.type === 'DECREASE').length;
        const totalStable = changes.filter(c => c.type === 'STABLE').length;

        return {
            totalChanges: changes.length,
            increases: totalIncrease,
            decreases: totalDecrease,
            stable: totalStable,
            averageChange: changes.reduce((sum, c) => sum + Math.abs(c.percent), 0) / changes.length,
            latestChange: changes[0] || null
        };
    }
}

// Inisialisasi ketika DOM ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
        window.goldPriceHistoryHandler = new GoldPriceHistoryHandler();
    });
} else {
    window.goldPriceHistoryHandler = new GoldPriceHistoryHandler();
}

// Export untuk penggunaan global
window.GoldPriceHistoryHandler = GoldPriceHistoryHandler;