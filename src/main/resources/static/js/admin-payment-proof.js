/**
 * JavaScript untuk mengelola bukti pembayaran di halaman admin
 */

class PaymentProofManager {
    constructor() {
        this.baseUrl = '/admin/api/orders';
    }

    /**
     * Mengecek apakah order memiliki bukti pembayaran
     * @param {number} orderId - ID order
     * @returns {Promise<Object>} Response data
     */
    async checkPaymentProof(orderId) {
        try {
            const response = await fetch(`${this.baseUrl}/${orderId}/payment-proof/check`);
            const data = await response.json();
            return data;
        } catch (error) {
            console.error('Error checking payment proof:', error);
            return { success: false, message: error.message };
        }
    }

    /**
     * Mendapatkan URL untuk gambar bukti pembayaran
     * @param {number} orderId - ID order
     * @returns {string} URL gambar
     */
    getPaymentProofImageUrl(orderId) {
        return `${this.baseUrl}/${orderId}/payment-proof/image`;
    }

    /**
     * Menampilkan bukti pembayaran dalam modal/popup
     * @param {number} orderId - ID order
     */
    async showPaymentProofModal(orderId) {
        try {
            // Check apakah ada bukti pembayaran
            const checkResult = await this.checkPaymentProof(orderId);
            
            if (!checkResult.success) {
                this.showAlert('Error', 'Gagal mengecek bukti pembayaran: ' + checkResult.message, 'error');
                return;
            }

            if (!checkResult.hasPaymentProof) {
                this.showAlert('Info', 'Belum ada bukti pembayaran untuk pesanan ini', 'info');
                return;
            }

            // Buat modal untuk menampilkan gambar
            const imageUrl = this.getPaymentProofImageUrl(orderId);
            
            const modalHtml = `
                <div id="paymentProofModal" class="modal fade" tabindex="-1">
                    <div class="modal-dialog modal-lg">
                        <div class="modal-content">
                            <div class="modal-header">
                                <h5 class="modal-title">
                                    <i class="fas fa-receipt me-2"></i>
                                    Bukti Pembayaran - Order #${orderId}
                                </h5>
                                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                            </div>
                            <div class="modal-body text-center">
                                <div class="mb-3">
                                    <p class="text-muted mb-1">File: ${checkResult.fileName}</p>
                                    <p class="text-muted">Type: ${checkResult.contentType}</p>
                                </div>
                                <div class="position-relative">
                                    <img id="paymentProofImage" 
                                         src="${imageUrl}" 
                                         class="img-fluid rounded shadow"
                                         style="max-height: 500px; cursor: pointer;"
                                         alt="Bukti Pembayaran"
                                         onclick="window.open('${imageUrl}', '_blank')">
                                    <div id="imageLoader" class="position-absolute top-50 start-50 translate-middle d-none">
                                        <div class="spinner-border text-primary" role="status">
                                            <span class="visually-hidden">Loading...</span>
                                        </div>
                                    </div>
                                </div>
                                <small class="text-muted d-block mt-2">
                                    <i class="fas fa-info-circle me-1"></i>
                                    Klik gambar untuk membuka dalam tab baru
                                </small>
                            </div>
                            <div class="modal-footer">
                                <a href="${imageUrl}" download="${checkResult.fileName}" 
                                   class="btn btn-outline-primary">
                                    <i class="fas fa-download me-2"></i>Download
                                </a>
                                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">
                                    Tutup
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            `;

            // Remove existing modal if any
            const existingModal = document.getElementById('paymentProofModal');
            if (existingModal) {
                existingModal.remove();
            }

            // Add modal to body
            document.body.insertAdjacentHTML('beforeend', modalHtml);

            // Setup image loading
            const imageElement = document.getElementById('paymentProofImage');
            const loader = document.getElementById('imageLoader');

            imageElement.addEventListener('load', () => {
                loader.classList.add('d-none');
                imageElement.classList.remove('d-none');
            });

            imageElement.addEventListener('error', () => {
                loader.classList.add('d-none');
                imageElement.innerHTML = `
                    <div class="alert alert-danger">
                        <i class="fas fa-exclamation-triangle me-2"></i>
                        Gagal memuat gambar bukti pembayaran
                    </div>
                `;
            });

            // Show loading initially
            loader.classList.remove('d-none');
            imageElement.classList.add('d-none');

            // Show modal
            const modal = new bootstrap.Modal(document.getElementById('paymentProofModal'));
            modal.show();

            // Clean up after modal is hidden
            document.getElementById('paymentProofModal').addEventListener('hidden.bs.modal', function () {
                this.remove();
            });

        } catch (error) {
            console.error('Error showing payment proof modal:', error);
            this.showAlert('Error', 'Gagal menampilkan bukti pembayaran: ' + error.message, 'error');
        }
    }

    /**
     * Update tampilan status bukti pembayaran di tabel order
     * @param {number} orderId - ID order
     * @param {HTMLElement} element - Element yang akan diupdate
     */
    async updatePaymentProofStatus(orderId, element) {
        try {
            const checkResult = await this.checkPaymentProof(orderId);
            
            if (checkResult.success && checkResult.hasPaymentProof) {
                element.innerHTML = `
                    <button class="btn btn-sm btn-success" onclick="paymentProofManager.showPaymentProofModal(${orderId})">
                        <i class="fas fa-eye me-1"></i>
                        Lihat Bukti
                    </button>
                `;
            } else {
                element.innerHTML = `
                    <span class="badge bg-warning text-dark">
                        <i class="fas fa-clock me-1"></i>
                        Belum Upload
                    </span>
                `;
            }
        } catch (error) {
            console.error('Error updating payment proof status:', error);
            element.innerHTML = `
                <span class="badge bg-danger">
                    <i class="fas fa-exclamation-triangle me-1"></i>
                    Error
                </span>
            `;
        }
    }

    /**
     * Menampilkan alert menggunakan SweetAlert atau alert biasa
     * @param {string} title - Judul alert
     * @param {string} message - Pesan alert
     * @param {string} type - Type alert (success, error, info, warning)
     */
    showAlert(title, message, type = 'info') {
        if (typeof Swal !== 'undefined') {
            // Gunakan SweetAlert jika tersedia
            Swal.fire({
                title: title,
                text: message,
                icon: type,
                confirmButtonText: 'OK'
            });
        } else {
            // Fallback ke alert biasa
            alert(`${title}: ${message}`);
        }
    }

    /**
     * Initialize payment proof checking untuk semua order di tabel
     */
    initializePaymentProofChecks() {
        // Cari semua element dengan class 'payment-proof-status'
        const statusElements = document.querySelectorAll('.payment-proof-status[data-order-id]');
        
        statusElements.forEach(element => {
            const orderId = element.getAttribute('data-order-id');
            this.updatePaymentProofStatus(orderId, element);
        });
    }
}

// Initialize global payment proof manager
const paymentProofManager = new PaymentProofManager();

// Auto-initialize ketika DOM ready
document.addEventListener('DOMContentLoaded', function() {
    paymentProofManager.initializePaymentProofChecks();
});

/**
 * Helper functions yang bisa dipanggil dari HTML
 */

// Function untuk menampilkan bukti pembayaran (bisa dipanggil dari onclick)
function showPaymentProof(orderId) {
    paymentProofManager.showPaymentProofModal(orderId);
}

// Function untuk refresh status bukti pembayaran
function refreshPaymentProofStatus(orderId) {
    const element = document.querySelector(`.payment-proof-status[data-order-id="${orderId}"]`);
    if (element) {
        paymentProofManager.updatePaymentProofStatus(orderId, element);
    }
}

// Export untuk module system jika diperlukan
if (typeof module !== 'undefined' && module.exports) {
    module.exports = PaymentProofManager;
}
