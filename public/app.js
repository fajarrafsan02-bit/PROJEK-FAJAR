// Gold Price Manager Frontend
class GoldPriceManager {
    constructor() {
        this.currentPrices = {};
        this.priceChanges = [];
        this.priceHistory = [];
        this.init();
    }

    async init() {
        await this.loadCurrentPrices();
        await this.loadPriceChanges();
        await this.loadPriceHistory();
        this.setupEventListeners();
    }

    async loadCurrentPrices() {
        try {
            const response = await fetch('/api/prices');
            const data = await response.json();
            
            if (data.success) {
                this.currentPrices = data.data;
                this.updatePriceDisplay();
            }
        } catch (error) {
            console.error('Error loading current prices:', error);
            this.showNotification('Error loading prices', 'error');
        }
    }

    async loadPriceChanges() {
        try {
            const response = await fetch('/api/changes');
            const data = await response.json();
            
            if (data.success) {
                this.priceChanges = data.data;
                this.updateChangesDisplay();
            }
        } catch (error) {
            console.error('Error loading price changes:', error);
        }
    }

    async loadPriceHistory() {
        try {
            const response = await fetch('/api/history');
            const data = await response.json();
            
            if (data.success) {
                this.priceHistory = data.data.content;
                this.updateHistoryDisplay();
            }
        } catch (error) {
            console.error('Error loading price history:', error);
        }
    }

    updatePriceDisplay() {
        const formatPrice = (price) => {
            return new Intl.NumberFormat('id-ID', {
                style: 'currency',
                currency: 'IDR',
                minimumFractionDigits: 0
            }).format(price);
        };

        document.getElementById('price-24k').textContent = formatPrice(this.currentPrices['24k']);
        document.getElementById('price-22k').textContent = formatPrice(this.currentPrices['22k']);
        document.getElementById('price-18k').textContent = formatPrice(this.currentPrices['18k']);
    }

    updateChangesDisplay() {
        const container = document.getElementById('changes-container');
        
        if (this.priceChanges.length === 0) {
            container.innerHTML = `
                <div class="text-center text-gray-500 py-8">
                    <i class="fas fa-info-circle text-2xl mb-2"></i>
                    <p>Belum ada perubahan harga</p>
                </div>
            `;
            return;
        }

        const changesHTML = this.priceChanges.map(change => {
            const changeIcon = change.changeType === 'INCREASE' ? 'fa-arrow-up' : 'fa-arrow-down';
            const changeClass = change.changeType === 'INCREASE' ? 'change-positive' : 'change-negative';
            const changeText = change.changeType === 'INCREASE' ? 'Naik' : 'Turun';
            
            return `
                <div class="border-l-4 border-blue-500 pl-4 py-3 bg-gray-50 rounded-r-lg">
                    <div class="flex items-center justify-between">
                        <div>
                            <h4 class="font-semibold text-gray-800">${change.purity} - ${changeText}</h4>
                            <p class="text-sm text-gray-600">
                                ${new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR' }).format(change.oldPrice)} 
                                <i class="fas fa-arrow-right mx-2 text-gray-400"></i>
                                ${new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR' }).format(change.newPrice)}
                            </p>
                            <p class="text-xs text-gray-500 mt-1">
                                ${new Date(change.changeDate).toLocaleString('id-ID')} • ${change.changeSource}
                            </p>
                        </div>
                        <div class="text-right">
                            <div class="text-lg font-bold ${changeClass}">
                                <i class="fas ${changeIcon} mr-1"></i>
                                ${change.changePercent}%
                            </div>
                            <div class="text-sm text-gray-600">
                                ${new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR' }).format(Math.abs(change.changeAmount))}
                            </div>
                        </div>
                    </div>
                </div>
            `;
        }).join('');

        container.innerHTML = changesHTML;
    }

    updateHistoryDisplay() {
        const container = document.getElementById('history-container');
        
        if (this.priceHistory.length === 0) {
            container.innerHTML = `
                <div class="text-center text-gray-500 py-8">
                    <i class="fas fa-info-circle text-2xl mb-2"></i>
                    <p>Belum ada riwayat harga</p>
                </div>
            `;
            return;
        }

        const historyHTML = this.priceHistory.map(item => {
            return `
                <div class="border border-gray-200 rounded-lg p-4">
                    <div class="flex items-center justify-between mb-3">
                        <h4 class="font-semibold text-gray-800">
                            ${new Date(item.tanggalAmbil).toLocaleDateString('id-ID', { 
                                weekday: 'long', 
                                year: 'numeric', 
                                month: 'long', 
                                day: 'numeric',
                                hour: '2-digit',
                                minute: '2-digit'
                            })}
                        </h4>
                        <span class="px-2 py-1 bg-blue-100 text-blue-800 text-xs rounded-full">
                            ${item.goldPriceEnum || 'MANUAL'}
                        </span>
                    </div>
                    <div class="grid grid-cols-3 gap-4 text-sm">
                        <div class="text-center">
                            <div class="font-semibold text-gray-800">24K</div>
                            <div class="text-gray-600">${new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR' }).format(item.hargaJual24k)}</div>
                        </div>
                        <div class="text-center">
                            <div class="font-semibold text-gray-800">22K</div>
                            <div class="text-gray-600">${new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR' }).format(item.hargaJual22k)}</div>
                        </div>
                        <div class="text-center">
                            <div class="font-semibold text-gray-800">18K</div>
                            <div class="text-gray-600">${new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR' }).format(item.hargaJual18k)}</div>
                        </div>
                    </div>
                </div>
            `;
        }).join('');

        container.innerHTML = historyHTML;
    }

    async updatePrice(priceData) {
        try {
            const response = await fetch('/api/prices', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(priceData)
            });

            const data = await response.json();
            
            if (data.success) {
                this.currentPrices = data.data;
                this.updatePriceDisplay();
                await this.loadPriceChanges();
                await this.loadPriceHistory();
                this.showNotification('Harga berhasil diupdate!', 'success');
                return true;
            } else {
                this.showNotification(data.message || 'Error updating price', 'error');
                return false;
            }
        } catch (error) {
            console.error('Error updating price:', error);
            this.showNotification('Error updating price', 'error');
            return false;
        }
    }

    async fetchExternalPrice() {
        try {
            const response = await fetch('/api/external');
            const data = await response.json();
            
            if (data.success) {
                const success = await this.updatePrice({
                    harga24k: data.data.harga24k,
                    source: 'EXTERNAL_API'
                });
                
                if (success) {
                    this.showNotification('Harga eksternal berhasil diambil!', 'success');
                }
            } else {
                this.showNotification('Error fetching external price', 'error');
            }
        } catch (error) {
            console.error('Error fetching external price:', error);
            this.showNotification('Error fetching external price', 'error');
        }
    }

    async refreshData() {
        await this.loadCurrentPrices();
        await this.loadPriceChanges();
        await this.loadPriceHistory();
        this.showNotification('Data berhasil di-refresh!', 'success');
    }

    setupEventListeners() {
        // Update form submission
        document.getElementById('updateForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            
            const harga24k = document.getElementById('harga24k').value;
            if (!harga24k) {
                this.showNotification('Masukkan harga 24K', 'error');
                return;
            }

            const success = await this.updatePrice({
                harga24k: parseInt(harga24k),
                source: 'MANUAL'
            });

            if (success) {
                closeUpdateModal();
            }
        });
    }

    showNotification(message, type = 'info') {
        // Create notification element
        const notification = document.createElement('div');
        notification.className = `fixed top-4 right-4 p-4 rounded-lg shadow-lg z-50 ${
            type === 'success' ? 'bg-green-500 text-white' :
            type === 'error' ? 'bg-red-500 text-white' :
            'bg-blue-500 text-white'
        }`;
        notification.innerHTML = `
            <div class="flex items-center">
                <i class="fas ${
                    type === 'success' ? 'fa-check-circle' :
                    type === 'error' ? 'fa-exclamation-circle' :
                    'fa-info-circle'
                } mr-2"></i>
                <span>${message}</span>
            </div>
        `;

        document.body.appendChild(notification);

        // Remove notification after 3 seconds
        setTimeout(() => {
            if (notification.parentNode) {
                notification.parentNode.removeChild(notification);
            }
        }, 3000);
    }
}

// Modal functions
function openUpdateModal() {
    const modal = document.getElementById('updateModal');
    modal.classList.remove('hidden');
    modal.classList.add('flex');
    document.getElementById('harga24k').focus();
}

function closeUpdateModal() {
    const modal = document.getElementById('updateModal');
    modal.classList.add('hidden');
    modal.classList.remove('flex');
    document.getElementById('updateForm').reset();
}

// Global functions
function fetchExternalPrice() {
    goldPriceManager.fetchExternalPrice();
}

function refreshData() {
    goldPriceManager.refreshData();
}

// Initialize the application
let goldPriceManager;
document.addEventListener('DOMContentLoaded', () => {
    goldPriceManager = new GoldPriceManager();
});

// Close modal when clicking outside
document.getElementById('updateModal').addEventListener('click', (e) => {
    if (e.target === e.currentTarget) {
        closeUpdateModal();
    }
});