// Fixed Update All Prices - Fungsi untuk update semua harga dengan rasio yang benar
// Fungsi ini memperbaiki masalah perhitungan rasio karat

class FixedPriceUpdater {
    constructor() {
        this.karatRatios = {
            '24k': 1.0,      // Base price
            '22k': 0.9167,   // 22/24 = 0.9167
            '18k': 0.75      // 18/24 = 0.75
        };
    }

    /**
     * Update harga 24K dan hitung otomatis harga 22K dan 18K
     * @param {number} harga24k - Harga 24K baru
     * @returns {object} Object berisi semua harga yang sudah dihitung
     */
    updateFrom24K(harga24k) {
        if (!harga24k || harga24k <= 0) {
            throw new Error('Harga 24K harus lebih dari 0');
        }

        const harga22k = Math.round(harga24k * this.karatRatios['22k']);
        const harga18k = Math.round(harga24k * this.karatRatios['18k']);

        return {
            '24k': harga24k,
            '22k': harga22k,
            '18k': harga18k,
            ratios: {
                '22k': (harga22k / harga24k * 100).toFixed(2) + '%',
                '18k': (harga18k / harga24k * 100).toFixed(2) + '%'
            }
        };
    }

    /**
     * Update harga 22K dan hitung ulang harga lainnya
     * @param {number} harga22k - Harga 22K baru
     * @returns {object} Object berisi semua harga yang sudah dihitung
     */
    updateFrom22K(harga22k) {
        if (!harga22k || harga22k <= 0) {
            throw new Error('Harga 22K harus lebih dari 0');
        }

        const harga24k = Math.round(harga22k / this.karatRatios['22k']);
        const harga18k = Math.round(harga24k * this.karatRatios['18k']);

        return {
            '24k': harga24k,
            '22k': harga22k,
            '18k': harga18k,
            ratios: {
                '22k': (harga22k / harga24k * 100).toFixed(2) + '%',
                '18k': (harga18k / harga24k * 100).toFixed(2) + '%'
            }
        };
    }

    /**
     * Update harga 18K dan hitung ulang harga lainnya
     * @param {number} harga18k - Harga 18K baru
     * @returns {object} Object berisi semua harga yang sudah dihitung
     */
    updateFrom18K(harga18k) {
        if (!harga18k || harga18k <= 0) {
            throw new Error('Harga 18K harus lebih dari 0');
        }

        const harga24k = Math.round(harga18k / this.karatRatios['18k']);
        const harga22k = Math.round(harga24k * this.karatRatios['22k']);

        return {
            '24k': harga24k,
            '22k': harga22k,
            '18k': harga18k,
            ratios: {
                '22k': (harga22k / harga24k * 100).toFixed(2) + '%',
                '18k': (harga18k / harga24k * 100).toFixed(2) + '%'
            }
        };
    }

    /**
     * Validasi rasio karat apakah sudah benar
     * @param {object} prices - Object berisi harga 24k, 22k, 18k
     * @param {number} tolerance - Toleransi dalam rupiah (default: 1000)
     * @returns {object} Object berisi hasil validasi
     */
    validateRatios(prices, tolerance = 1000) {
        const { '24k': harga24k, '22k': harga22k, '18k': harga18k } = prices;

        if (!harga24k || !harga22k || !harga18k) {
            return {
                valid: false,
                error: 'Semua harga harus tersedia'
            };
        }

        const expected22k = Math.round(harga24k * this.karatRatios['22k']);
        const expected18k = Math.round(harga24k * this.karatRatios['18k']);

        const diff22k = Math.abs(harga22k - expected22k);
        const diff18k = Math.abs(harga18k - expected18k);

        const valid22k = diff22k <= tolerance;
        const valid18k = diff18k <= tolerance;

        return {
            valid: valid22k && valid18k,
            details: {
                '22k': {
                    actual: harga22k,
                    expected: expected22k,
                    difference: diff22k,
                    valid: valid22k,
                    ratio: (harga22k / harga24k * 100).toFixed(2) + '%'
                },
                '18k': {
                    actual: harga18k,
                    expected: expected18k,
                    difference: diff18k,
                    valid: valid18k,
                    ratio: (harga18k / harga24k * 100).toFixed(2) + '%'
                }
            }
        };
    }

    /**
     * Format harga ke format Rupiah
     * @param {number} price - Harga dalam angka
     * @returns {string} Harga dalam format Rupiah
     */
    formatPrice(price) {
        return new Intl.NumberFormat('id-ID', {
            style: 'currency',
            currency: 'IDR',
            minimumFractionDigits: 0
        }).format(price);
    }

    /**
     * Hitung perubahan harga dan persentase
     * @param {object} oldPrices - Harga lama
     * @param {object} newPrices - Harga baru
     * @returns {object} Object berisi perubahan untuk setiap karat
     */
    calculateChanges(oldPrices, newPrices) {
        const changes = {};

        ['24k', '22k', '18k'].forEach(karat => {
            const oldPrice = oldPrices[karat];
            const newPrice = newPrices[karat];
            
            if (oldPrice && newPrice) {
                const changeAmount = newPrice - oldPrice;
                const changePercent = ((changeAmount / oldPrice) * 100);
                
                changes[karat] = {
                    oldPrice,
                    newPrice,
                    changeAmount,
                    changePercent: changePercent.toFixed(2),
                    changeType: changeAmount > 0 ? 'INCREASE' : changeAmount < 0 ? 'DECREASE' : 'NO_CHANGE'
                };
            }
        });

        return changes;
    }
}

// Export untuk Node.js
if (typeof module !== 'undefined' && module.exports) {
    module.exports = FixedPriceUpdater;
}

// Export untuk browser
if (typeof window !== 'undefined') {
    window.FixedPriceUpdater = FixedPriceUpdater;
}

// Contoh penggunaan
if (typeof console !== 'undefined') {
    console.log('=== Fixed Price Updater Loaded ===');
    
    const updater = new FixedPriceUpdater();
    
    // Contoh update dari 24K
    console.log('Update dari 24K (2.5 juta):');
    console.log(updater.updateFrom24K(2500000));
    
    // Contoh validasi
    console.log('Validasi rasio:');
    console.log(updater.validateRatios({
        '24k': 2500000,
        '22k': 2291750,
        '18k': 1875000
    }));
}