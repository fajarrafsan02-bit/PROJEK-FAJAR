// Test JavaScript syntax from harga-emas.html
class GoldPriceManager {
    constructor() {
        this.currentPrices = {};
        this.priceHistory = [];
        this.priceChanges = {};
        this.baseUrl = '/gold-price';
        this.externalPrice = null;

        this.historyState = {
            page: 1,
            pageSize: 10,
            startDate: null,
            endDate: null
        };

        this.init();
    }

    async init() {
        this.setupEventListeners();
        await this.loadCurrentPrices();
        await this.loadPriceChanges();
        await this.loadPriceHistory();
        this.renderCurrentPrices();
        this.loadCurrentPricesInForm();
    }

    setupEventListeners() {
        const updateForm = document.getElementById('updateForm');
        if (updateForm) {
            updateForm.addEventListener('submit', (e) => {
                this.handlePriceUpdate(e);
            });
        }

        document.getElementById('updateAllBtn').addEventListener('click', () => {
            this.updateAllPrices();
        });

        document.getElementById('fetchExternalBtn').addEventListener('click', () => {
            this.fetchExternalPrice();
        });

        document.getElementById('updatePriceBtn').addEventListener('click', () => {
            this.updatePriceFromExternal();
        });

        const manualUpdateForm = document.getElementById('manualUpdateForm');
        if (manualUpdateForm) {
            manualUpdateForm.addEventListener('submit', (e) => {
                e.preventDefault();
                this.handleManualPriceUpdate(e);
            });
        }
    }

    async fetchExternalPrice() {
        try {
            this.showUpdateMessage("Mengambil harga emas dari backend...", 'info');

            const response = await fetch('/gold-price/fetch-external');
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: Gagal mengambil harga dari backend`);
            }

            const result = await response.json();
            console.log('Backend Response:', result);

            if (result.success && result.data) {
                this.externalPrice = result.data;
                this.updateApiStatus();
                
                this.showUpdateMessage(`✅ Harga emas berhasil diambil dari ${this.externalPrice.source}:\n24K: ${this.formatCurrency(this.externalPrice.harga24k)}\n22K: ${this.formatCurrency(this.externalPrice.harga22k)}\n18K: ${this.formatCurrency(this.externalPrice.harga18k)}`, 'success');
                this.showNotification('Harga emas berhasil diambil!', 'success');

            } else {
                throw new Error(result.message || 'Gagal mengambil harga eksternal');
            }

        } catch (error) {
            console.error('Error fetching external price:', error);
            this.showUpdateMessage("❌ Error: " + error.message, 'error');
            this.showNotification('Gagal mengambil harga eksternal: ' + error.message, 'error');
        }
    }

    updateApiStatus() {
        const statusElement = document.getElementById('apiStatus');
        if (statusElement && this.externalPrice) {
            if (this.externalPrice.source === "Metal Price API") {
                statusElement.innerHTML = `
                    <div class="alert alert-success">
                        <i class="fas fa-check-circle"></i>
                        <strong>✅ Metal Price API Aktif</strong><br>
                        <small>24K: ${this.formatCurrency(this.externalPrice.harga24k)} | 22K: ${this.formatCurrency(this.externalPrice.harga22k)} | 18K: ${this.formatCurrency(this.externalPrice.harga18k)}</small><br>
                        <small>Last Update: ${new Date(this.externalPrice.timestamp * 1000).toLocaleString('id-ID')}</small>
                    </div>
                `;
            } else if (this.externalPrice.source === "Backend Fallback") {
                statusElement.innerHTML = `
                    <div class="alert alert-warning">
                        <i class="fas fa-exclamation-triangle"></i>
                        <strong>⚠️ Menggunakan Backend Fallback</strong><br>
                        <small>Metal Price API tidak tersedia, menggunakan data dari database</small><br>
                        <small>Note: ${this.externalPrice.note || 'Data mungkin tidak reliable'}</small>
                    </div>
                `;
            } else {
                statusElement.innerHTML = `
                    <div class="alert alert-info">
                        <i class="fas fa-info-circle"></i>
                        <strong>ℹ️ Data Tersedia</strong><br>
                        <small>Source: ${this.externalPrice.source || 'Unknown'}</small>
                    </div>
                `;
            }
        } else {
            statusElement.innerHTML = `
                <div class="alert alert-info">
                    <i class="fas fa-info-circle"></i>
                    <strong>Status API</strong><br>
                    Klik "Ambil Harga Eksternal" untuk cek status Metal Price API
                </div>
            `;
        }
    }

    async updateAllPrices() {
        try {
            this.isUpdating = true;
            this.showUpdateMessage("Memulai update harga emas dari API eksternal...", 'info');

            await this.fetchExternalPrice();

            if (!this.externalPrice) {
                throw new Error("Tidak ada data harga eksternal yang tersedia");
            }

            console.log('External price data:', this.externalPrice);
            
            let harga24k = 0;
            let harga22k = 0;
            let harga18k = 0;
            
            if (this.externalPrice.harga24k || this.externalPrice.price24k || this.externalPrice.harga24K || this.externalPrice.price24K) {
                harga24k = this.externalPrice.harga24k || this.externalPrice.price24k || this.externalPrice.harga24K || this.externalPrice.price24K;
            }
            
            if (this.externalPrice.harga22k || this.externalPrice.price22k || this.externalPrice.harga22K || this.externalPrice.price22K) {
                harga22k = this.externalPrice.harga22k || this.externalPrice.price22k || this.externalPrice.harga22K || this.externalPrice.price22K;
            }
            
            if (this.externalPrice.harga18k || this.externalPrice.price18k || this.externalPrice.harga18K || this.externalPrice.price18K) {
                harga18k = this.externalPrice.harga18k || this.externalPrice.price18k || this.externalPrice.harga18K || this.externalPrice.price18K;
            }
            
            if (!harga24k && !harga22k && !harga18k) {
                const generalPrice = this.externalPrice.price || this.externalPrice.harga || 
                                   this.externalPrice.harga24k || this.externalPrice.price24k || 
                                   this.externalPrice.harga24K || this.externalPrice.price24K;
                
                if (generalPrice && generalPrice > 0) {
                    harga24k = generalPrice;
                    harga22k = Math.round(generalPrice * 0.917);
                    harga18k = Math.round(generalPrice * 0.75);
                    console.log('Using general price and calculating ratios:', { harga24k, harga22k, harga18k });
                }
            }

            if (!harga24k && !harga22k && !harga18k) {
                throw new Error("Tidak ada data harga yang valid dari API eksternal");
            }

            console.log('Parsed external prices:', { harga24k, harga22k, harga18k });
            
            const currentPrices = this.currentPrices;
            let hasChanges = false;
            let samePriceMessage = '';
            let updateData = {};
            let forceUpdate = false;

            const tolerance = 10;

            if (harga24k > 0) {
                const currentPrice24k = currentPrices['24k'] || 0;
                if (currentPrice24k > 0 && Math.abs(harga24k - currentPrice24k) < tolerance) {
                    samePriceMessage += `24K: ${this.formatCurrency(currentPrice24k)} (sama), `;
                } else {
                    hasChanges = true;
                    updateData.harga24k = harga24k;
                }
            }

            if (harga22k > 0) {
                const currentPrice22k = currentPrices['22k'] || 0;
                if (currentPrice22k > 0 && Math.abs(harga22k - currentPrice22k) < tolerance) {
                    samePriceMessage += `22K: ${this.formatCurrency(currentPrice22k)} (sama), `;
                } else {
                    hasChanges = true;
                    updateData.harga22k = harga22k;
                }
            }

            if (harga18k > 0) {
                const currentPrice18k = currentPrices['18k'] || 0;
                if (currentPrice18k > 0 && Math.abs(harga18k - currentPrice18k) < tolerance) {
                    samePriceMessage += `18K: ${this.formatCurrency(currentPrice18k)} (sama), `;
                } else {
                    hasChanges = true;
                    updateData.harga18k = harga18k;
                }
            }

            if (!hasChanges) {
                samePriceMessage = samePriceMessage.replace(/,\s*$/, '');
                
                const forceUpdateMessage = `Semua harga sudah sama dengan harga saat ini: ${samePriceMessage}. Apakah Anda ingin tetap melakukan update?`;
                this.showUpdateMessage(forceUpdateMessage, 'warning');
                
                const userWantsUpdate = confirm(forceUpdateMessage + '\n\nKlik OK untuk tetap update, Cancel untuk batal.');
                
                if (userWantsUpdate) {
                    if (harga24k > 0) updateData.harga24k = harga24k;
                    if (harga22k > 0) updateData.harga22k = harga22k;
                    if (harga18k > 0) updateData.harga18k = harga18k;
                    
                    hasChanges = true;
                    forceUpdate = true;
                    
                    this.showUpdateMessage("Force update diaktifkan. Akan mengupdate semua harga yang tersedia dari API eksternal.", 'info');
                } else {
                    this.showNotification('Update dibatalkan oleh user', 'info');
                    this.externalPrice = null;
                    return;
                }
            }

            if (samePriceMessage) {
                samePriceMessage = samePriceMessage.replace(/,\s*$/, '');
                this.showUpdateMessage(`Beberapa harga sama: ${samePriceMessage}`, 'warning');
            }

            const updateMessage = Object.keys(updateData).map(key => {
                const karat = key.replace('harga', '').toUpperCase();
                return `${karat}: ${this.formatCurrency(updateData[key])}`;
            }).join(', ');
            
            this.showUpdateMessage(`Mengirim request update untuk: ${updateMessage}`, 'info');

            console.log('Sending request with data:', updateData);

            if (hasChanges) {
                const response = await fetch('/gold-price/update', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify(updateData)
                });

                if (!response.ok) {
                    throw new Error(`HTTP ${response.status}: Gagal mengupdate harga emas`);
                }

                const result = await response.json();
                
                if (result.success) {
                    this.showUpdateMessage(`Update berhasil! Harga yang diupdate: ${updateMessage}`, 'success');
                    this.showNotification('Update harga emas berhasil!', 'success');
                } else {
                    throw new Error(result.message || 'Gagal mengupdate harga emas');
                }
            } else {
                samePriceMessage = samePriceMessage.replace(/,\s*$/, '');
                
                const forceUpdateMessage = `Semua harga sudah sama dengan harga saat ini: ${samePriceMessage}. Apakah Anda ingin tetap melakukan update?`;
                this.showUpdateMessage(forceUpdateMessage, 'warning');
                
                const userWantsUpdate = confirm(forceUpdateMessage + '\n\nKlik OK untuk tetap update, Cancel untuk batal.');
                
                if (userWantsUpdate) {
                    const forceUpdateData = {};
                    if (harga24k > 0) forceUpdateData.harga24k = harga24k;
                    if (harga22k > 0) forceUpdateData.harga22k = harga22k;
                    if (harga18k > 0) forceUpdateData.harga18k = harga18k;
                    
                    this.showUpdateMessage("Force update diaktifkan. Mengupdate semua harga...", 'info');
                    
                    const response = await fetch('/gold-price/update', {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json',
                        },
                        body: JSON.stringify(forceUpdateData)
                    });

                    if (!response.ok) {
                        throw new Error(`HTTP ${response.status}: Gagal melakukan force update`);
                    }

                    const result = await response.json();
                    
                    if (result.success) {
                        this.showUpdateMessage("Force update berhasil! Semua harga telah diperbarui.", 'success');
                        this.showNotification('Force update berhasil!', 'success');
                    } else {
                        throw new Error(result.message || 'Gagal melakukan force update');
                    }
                } else {
                    this.showUpdateMessage('Update dibatalkan oleh user', 'info');
                    this.showNotification('Update dibatalkan oleh user', 'info');
                }
            }

            this.externalPrice = null;

            await this.loadCurrentPrices();
            await this.loadPriceChanges();
            await this.loadPriceHistory();
            this.renderCurrentPrices();

        } catch (error) {
            console.error('Error updating all prices:', error);
            this.showUpdateMessage("Error: " + error.message, 'error');
            this.showNotification('Gagal mengupdate harga emas: ' + error.message, 'error');

        } finally {
            this.isUpdating = false;
        }
    }

    // Placeholder methods for testing
    showUpdateMessage(message, type) { console.log(message, type); }
    showNotification(message, type) { console.log(message, type); }
    formatCurrency(amount) { return `Rp ${amount}`; }
    loadCurrentPrices() { return Promise.resolve(); }
    loadPriceChanges() { return Promise.resolve(); }
    loadPriceHistory() { return Promise.resolve(); }
    renderCurrentPrices() { }
    loadCurrentPricesInForm() { }
    handlePriceUpdate(e) { }
    updatePriceFromExternal() { }
    handleManualPriceUpdate(e) { }
    loadCurrentPricesToForm() { }
    toggleMobileSidebar() { }
    toggleDesktopSidebar() { }
    closeMobileSidebar() { }
    calculateProductPrice() { }
}

// Test instantiation
const manager = new GoldPriceManager();
console.log('GoldPriceManager created successfully');