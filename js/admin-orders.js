/**
 * Admin Orders Management System with Animations
 * Features: Scroll animations, smooth transitions, responsive interactions
 */

class AdminOrdersManager {
    constructor() {
        this.currentPage = 1;
        this.ordersPerPage = 10;
        this.currentFilter = 'all';
        this.searchQuery = '';
        this.orders = [];
        this.isLoading = false;
        
        // Animation observer
        this.scrollObserver = null;
        this.animationQueue = [];
        
        this.init();
    }

    /**
     * Initialize the admin orders manager
     */
    init() {
        this.setupScrollAnimations();
        this.setupEventListeners();
        this.loadOrders();
        this.initializeUI();
    }

    /**
     * Setup intersection observer for scroll animations
     */
    setupScrollAnimations() {
        if ('IntersectionObserver' in window) {
            this.scrollObserver = new IntersectionObserver(
                (entries) => {
                    entries.forEach(entry => {
                        if (entry.isIntersecting) {
                            this.animateElement(entry.target);
                        }
                    });
                },
                {
                    threshold: 0.1,
                    rootMargin: '0px 0px -50px 0px'
                }
            );
        }
    }

    /**
     * Animate element when it comes into view
     */
    animateElement(element) {
        if (element.classList.contains('animated')) return;

        const animationType = element.dataset.animation || 'fadeInUp';
        const delay = element.dataset.delay || 0;

        setTimeout(() => {
            element.classList.add('animated', `animate-${animationType}`);
            this.scrollObserver?.unobserve(element);
        }, delay);
    }

    /**
     * Setup event listeners
     */
    setupEventListeners() {
        // Filter dropdown
        const filterSelect = document.getElementById('statusFilter');
        if (filterSelect) {
            filterSelect.addEventListener('change', (e) => {
                this.currentFilter = e.target.value;
                this.filterOrders();
            });
        }

        // Search input
        const searchInput = document.getElementById('searchOrders');
        if (searchInput) {
            let searchTimeout;
            searchInput.addEventListener('input', (e) => {
                clearTimeout(searchTimeout);
                searchTimeout = setTimeout(() => {
                    this.searchQuery = e.target.value.toLowerCase();
                    this.filterOrders();
                }, 300);
            });
        }

        // Pagination clicks
        document.addEventListener('click', (e) => {
            if (e.target.matches('.page-link')) {
                e.preventDefault();
                const page = parseInt(e.target.dataset.page);
                if (page && page !== this.currentPage) {
                    this.goToPage(page);
                }
            }
        });

        // Window resize for responsive adjustments
        window.addEventListener('resize', this.debounce(() => {
            this.handleResponsiveAdjustments();
        }, 250));

        // Keyboard shortcuts
        document.addEventListener('keydown', (e) => {
            if (e.ctrlKey || e.metaKey) {
                switch (e.key) {
                    case 'f':
                        e.preventDefault();
                        document.getElementById('searchOrders')?.focus();
                        break;
                    case 'r':
                        e.preventDefault();
                        this.refreshOrders();
                        break;
                }
            }
        });
    }

    /**
     * Initialize UI components
     */
    initializeUI() {
        // Add loading overlay
        if (!document.getElementById('loadingOverlay')) {
            const loadingOverlay = document.createElement('div');
            loadingOverlay.id = 'loadingOverlay';
            loadingOverlay.className = 'loading-overlay';
            loadingOverlay.innerHTML = '<div class="loading-spinner"></div>';
            document.body.appendChild(loadingOverlay);
        }

        // Mark container as loaded
        setTimeout(() => {
            const container = document.querySelector('.admin-container');
            if (container) {
                container.classList.add('loaded');
            }

            // Show filter section
            const filterSection = document.querySelector('.filter-section');
            if (filterSection) {
                setTimeout(() => {
                    filterSection.classList.add('show');
                }, 200);
            }
        }, 100);
    }

    /**
     * Load orders from API
     */
    async loadOrders() {
        try {
            this.showLoading(true);
            
            // Simulate API call - replace with actual API endpoint
            await this.delay(1000);
            
            // Mock data - replace with actual API call
            this.orders = this.generateMockOrders();
            
            this.renderOrders();
            this.renderPagination();
            
        } catch (error) {
            console.error('Error loading orders:', error);
            this.showErrorMessage('Failed to load orders');
        } finally {
            this.showLoading(false);
        }
    }

    /**
     * Generate mock orders data
     */
    generateMockOrders() {
        const statuses = ['PENDING_PAYMENT', 'PENDING_CONFIRMATION', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED'];
        const customers = [
            { name: 'John Doe', email: 'john@example.com' },
            { name: 'Jane Smith', email: 'jane@example.com' },
            { name: 'Bob Johnson', email: 'bob@example.com' },
            { name: 'Alice Brown', email: 'alice@example.com' },
            { name: 'Charlie Wilson', email: 'charlie@example.com' },
            { name: 'Diana Davis', email: 'diana@example.com' },
            { name: 'Edward Miller', email: 'edward@example.com' },
            { name: 'Fiona Garcia', email: 'fiona@example.com' }
        ];

        const orders = [];
        for (let i = 1; i <= 25; i++) {
            const customer = customers[Math.floor(Math.random() * customers.length)];
            const status = statuses[Math.floor(Math.random() * statuses.length)];
            const total = Math.floor(Math.random() * 5000000) + 500000;
            
            orders.push({
                id: i,
                orderId: `#ORD-${String(i).padStart(3, '0')}`,
                customer: customer,
                total: total,
                status: status,
                hasPaymentProof: Math.random() > 0.3,
                createdAt: new Date(Date.now() - Math.random() * 30 * 24 * 60 * 60 * 1000),
                trackingNumber: status === 'SHIPPED' || status === 'DELIVERED' ? `TRK${String(i).padStart(6, '0')}` : null
            });
        }

        return orders.sort((a, b) => b.createdAt - a.createdAt);
    }

    /**
     * Render orders table
     */
    renderOrders() {
        const tbody = document.getElementById('orderTableBody');
        if (!tbody) return;

        // Clear existing content
        tbody.innerHTML = '';

        // Filter and paginate orders
        const filteredOrders = this.getFilteredOrders();
        const startIndex = (this.currentPage - 1) * this.ordersPerPage;
        const endIndex = startIndex + this.ordersPerPage;
        const pageOrders = filteredOrders.slice(startIndex, endIndex);

        if (pageOrders.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="6" class="text-center py-5">
                        <div class="empty-state">
                            <i class="fas fa-inbox empty-state-icon"></i>
                            <h3>No orders found</h3>
                            <p>Try adjusting your filters or search query.</p>
                        </div>
                    </td>
                </tr>
            `;
            return;
        }

        // Render each order
        pageOrders.forEach((order, index) => {
            const row = this.createOrderRow(order, index);
            tbody.appendChild(row);

            // Setup scroll animation
            if (this.scrollObserver) {
                row.dataset.animation = 'fadeInUp';
                row.dataset.delay = index * 100;
                this.scrollObserver.observe(row);
            } else {
                // Fallback for browsers without intersection observer
                setTimeout(() => {
                    row.classList.add('show', `animate-stagger-${Math.min(index + 1, 5)}`);
                }, index * 100);
            }
        });

        // Update table stats
        this.updateTableStats(filteredOrders.length);
    }

    /**
     * Create order table row
     */
    createOrderRow(order, index) {
        const row = document.createElement('tr');
        row.className = 'order-row';
        row.dataset.orderId = order.id;

        const statusClass = this.getStatusClass(order.status);
        const statusText = this.getStatusText(order.status);
        const customerInitials = order.customer.name.split(' ').map(n => n[0]).join('');

        row.innerHTML = `
            <td>
                <strong>${order.orderId}</strong>
                <small class="d-block text-muted">${this.formatDate(order.createdAt)}</small>
            </td>
            <td>
                <div class="customer-info">
                    <div class="customer-avatar">${customerInitials}</div>
                    <div class="customer-details">
                        <strong>${order.customer.name}</strong><br>
                        <small class="text-muted">${order.customer.email}</small>
                    </div>
                </div>
            </td>
            <td>
                <div class="total-amount">${this.formatCurrency(order.total)}</div>
                ${order.trackingNumber ? `<small class="text-muted">Resi: ${order.trackingNumber}</small>` : ''}
            </td>
            <td>
                <span class="status-badge ${statusClass}">${statusText}</span>
            </td>
            <td>
                <div class="payment-proof-status" data-order-id="${order.id}">
                    ${this.renderPaymentProofStatus(order)}
                </div>
            </td>
            <td>
                <div class="action-buttons">
                    ${this.renderActionButtons(order)}
                </div>
            </td>
        `;

        return row;
    }

    /**
     * Render payment proof status
     */
    renderPaymentProofStatus(order) {
        if (order.hasPaymentProof) {
            return `
                <div class="proof-available" onclick="adminOrders.viewPaymentProof(${order.id})" title="Click to view proof">
                    <i class="fas fa-check-circle me-1"></i>
                    Available
                </div>
            `;
        } else {
            return `
                <div class="proof-not-available" title="No payment proof uploaded">
                    <i class="fas fa-times-circle me-1"></i>
                    Not Available
                </div>
            `;
        }
    }

    /**
     * Render action buttons based on order status
     */
    renderActionButtons(order) {
        const buttons = [];

        // View button (always available)
        buttons.push(`
            <button class="btn-action btn-view" onclick="adminOrders.viewOrderDetail(${order.id})" title="View Details">
                <i class="fas fa-eye"></i>
            </button>
        `);

        switch (order.status) {
            case 'PENDING_CONFIRMATION':
                if (order.hasPaymentProof) {
                    buttons.push(`
                        <button class="btn-action btn-confirm" onclick="adminOrders.confirmPayment(${order.id})" title="Confirm Payment">
                            <i class="fas fa-check"></i>
                        </button>
                    `);
                }
                buttons.push(`
                    <button class="btn-action btn-cancel" onclick="adminOrders.cancelOrder(${order.id})" title="Cancel Order">
                        <i class="fas fa-times"></i>
                    </button>
                `);
                break;

            case 'PROCESSING':
                buttons.push(`
                    <button class="btn-action btn-ship" onclick="adminOrders.shipOrder(${order.id})" title="Ship Order">
                        <i class="fas fa-truck"></i>
                    </button>
                `);
                break;

            case 'PENDING_PAYMENT':
                buttons.push(`
                    <button class="btn-action btn-edit" onclick="adminOrders.editOrder(${order.id})" title="Edit Order">
                        <i class="fas fa-edit"></i>
                    </button>
                `);
                buttons.push(`
                    <button class="btn-action btn-cancel" onclick="adminOrders.cancelOrder(${order.id})" title="Cancel Order">
                        <i class="fas fa-times"></i>
                    </button>
                `);
                break;
        }

        return buttons.join('');
    }

    /**
     * Get filtered orders based on current filters
     */
    getFilteredOrders() {
        let filtered = [...this.orders];

        // Apply status filter
        if (this.currentFilter !== 'all') {
            filtered = filtered.filter(order => order.status === this.currentFilter);
        }

        // Apply search filter
        if (this.searchQuery) {
            filtered = filtered.filter(order => 
                order.orderId.toLowerCase().includes(this.searchQuery) ||
                order.customer.name.toLowerCase().includes(this.searchQuery) ||
                order.customer.email.toLowerCase().includes(this.searchQuery)
            );
        }

        return filtered;
    }

    /**
     * Filter orders and re-render
     */
    filterOrders() {
        this.currentPage = 1;
        this.renderOrders();
        this.renderPagination();
    }

    /**
     * Render pagination
     */
    renderPagination() {
        const paginationContainer = document.querySelector('.pagination');
        if (!paginationContainer) return;

        const filteredOrders = this.getFilteredOrders();
        const totalPages = Math.ceil(filteredOrders.length / this.ordersPerPage);

        if (totalPages <= 1) {
            paginationContainer.style.display = 'none';
            return;
        }

        paginationContainer.style.display = 'flex';
        paginationContainer.innerHTML = '';

        // Previous button
        const prevLi = document.createElement('li');
        prevLi.className = `page-item ${this.currentPage === 1 ? 'disabled' : ''}`;
        prevLi.innerHTML = this.currentPage === 1 
            ? '<span class="page-link">Previous</span>'
            : `<a class="page-link" href="#" data-page="${this.currentPage - 1}">Previous</a>`;
        paginationContainer.appendChild(prevLi);

        // Page numbers
        const startPage = Math.max(1, this.currentPage - 2);
        const endPage = Math.min(totalPages, startPage + 4);

        for (let i = startPage; i <= endPage; i++) {
            const pageLi = document.createElement('li');
            pageLi.className = `page-item ${i === this.currentPage ? 'active' : ''}`;
            pageLi.innerHTML = i === this.currentPage
                ? `<span class="page-link">${i}</span>`
                : `<a class="page-link" href="#" data-page="${i}">${i}</a>`;
            paginationContainer.appendChild(pageLi);
        }

        // Next button
        const nextLi = document.createElement('li');
        nextLi.className = `page-item ${this.currentPage === totalPages ? 'disabled' : ''}`;
        nextLi.innerHTML = this.currentPage === totalPages
            ? '<span class="page-link">Next</span>'
            : `<a class="page-link" href="#" data-page="${this.currentPage + 1}">Next</a>`;
        paginationContainer.appendChild(nextLi);
    }

    /**
     * Go to specific page
     */
    goToPage(page) {
        if (page === this.currentPage) return;

        this.currentPage = page;
        
        // Smooth scroll to table
        const tableContainer = document.querySelector('.table-container');
        if (tableContainer) {
            tableContainer.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }

        // Add loading effect
        const tbody = document.getElementById('orderTableBody');
        if (tbody) {
            tbody.style.opacity = '0.5';
            tbody.style.transform = 'translateY(20px)';
            
            setTimeout(() => {
                this.renderOrders();
                this.renderPagination();
                
                tbody.style.opacity = '1';
                tbody.style.transform = 'translateY(0)';
            }, 150);
        }
    }

    /**
     * View order detail
     */
    async viewOrderDetail(orderId) {
        const order = this.orders.find(o => o.id === orderId);
        if (!order) return;

        try {
            const modalHtml = `
                <div class="modal fade" id="orderDetailModal" tabindex="-1">
                    <div class="modal-dialog modal-lg">
                        <div class="modal-content">
                            <div class="modal-header">
                                <h5 class="modal-title">
                                    <i class="fas fa-receipt me-2"></i>
                                    Order Detail - ${order.orderId}
                                </h5>
                                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                            </div>
                            <div class="modal-body">
                                <div class="row">
                                    <div class="col-md-6">
                                        <h6 class="fw-bold mb-3">Customer Information</h6>
                                        <div class="mb-3">
                                            <label class="form-label">Name:</label>
                                            <div>${order.customer.name}</div>
                                        </div>
                                        <div class="mb-3">
                                            <label class="form-label">Email:</label>
                                            <div>${order.customer.email}</div>
                                        </div>
                                    </div>
                                    <div class="col-md-6">
                                        <h6 class="fw-bold mb-3">Order Information</h6>
                                        <div class="mb-3">
                                            <label class="form-label">Order Date:</label>
                                            <div>${this.formatDate(order.createdAt)}</div>
                                        </div>
                                        <div class="mb-3">
                                            <label class="form-label">Status:</label>
                                            <div><span class="status-badge ${this.getStatusClass(order.status)}">${this.getStatusText(order.status)}</span></div>
                                        </div>
                                        <div class="mb-3">
                                            <label class="form-label">Total Amount:</label>
                                            <div class="total-amount">${this.formatCurrency(order.total)}</div>
                                        </div>
                                        ${order.trackingNumber ? `
                                            <div class="mb-3">
                                                <label class="form-label">Tracking Number:</label>
                                                <div><code>${order.trackingNumber}</code></div>
                                            </div>
                                        ` : ''}
                                    </div>
                                </div>
                                
                                <hr>
                                
                                <h6 class="fw-bold mb-3">Payment Information</h6>
                                <div class="mb-3">
                                    <label class="form-label">Payment Proof:</label>
                                    <div>${order.hasPaymentProof 
                                        ? '<span class="proof-available"><i class="fas fa-check-circle me-1"></i>Available</span>'
                                        : '<span class="proof-not-available"><i class="fas fa-times-circle me-1"></i>Not Available</span>'
                                    }</div>
                                </div>
                            </div>
                            <div class="modal-footer">
                                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
                                ${this.getModalActionButtons(order)}
                            </div>
                        </div>
                    </div>
                </div>
            `;

            // Remove existing modal
            const existingModal = document.getElementById('orderDetailModal');
            if (existingModal) {
                existingModal.remove();
            }

            // Add new modal
            document.body.insertAdjacentHTML('beforeend', modalHtml);

            // Show modal with animation
            const modal = new bootstrap.Modal(document.getElementById('orderDetailModal'));
            modal.show();

        } catch (error) {
            console.error('Error showing order detail:', error);
            this.showErrorMessage('Failed to load order details');
        }
    }

    /**
     * Get modal action buttons based on order status
     */
    getModalActionButtons(order) {
        const buttons = [];

        switch (order.status) {
            case 'PENDING_CONFIRMATION':
                if (order.hasPaymentProof) {
                    buttons.push(`
                        <button type="button" class="btn btn-success" onclick="adminOrders.confirmPayment(${order.id}); bootstrap.Modal.getInstance(document.getElementById('orderDetailModal')).hide();">
                            <i class="fas fa-check me-1"></i>Confirm Payment
                        </button>
                    `);
                }
                break;

            case 'PROCESSING':
                buttons.push(`
                    <button type="button" class="btn btn-info" onclick="adminOrders.shipOrder(${order.id}); bootstrap.Modal.getInstance(document.getElementById('orderDetailModal')).hide();">
                        <i class="fas fa-truck me-1"></i>Ship Order
                    </button>
                `);
                break;
        }

        return buttons.join('');
    }

    /**
     * Confirm payment
     */
    async confirmPayment(orderId) {
        const result = await Swal.fire({
            title: 'Confirm Payment',
            text: 'Are you sure you want to confirm this payment?',
            icon: 'question',
            showCancelButton: true,
            confirmButtonText: 'Yes, Confirm',
            cancelButtonText: 'Cancel',
            confirmButtonColor: '#198754'
        });

        if (result.isConfirmed) {
            try {
                this.showLoading(true);
                
                // Simulate API call
                await this.delay(1000);
                
                // Update order status
                const order = this.orders.find(o => o.id === orderId);
                if (order) {
                    order.status = 'PROCESSING';
                }
                
                this.renderOrders();
                
                Swal.fire({
                    title: 'Success!',
                    text: 'Payment has been confirmed',
                    icon: 'success',
                    timer: 2000,
                    showConfirmButton: false
                });
                
            } catch (error) {
                console.error('Error confirming payment:', error);
                this.showErrorMessage('Failed to confirm payment');
            } finally {
                this.showLoading(false);
            }
        }
    }

    /**
     * Ship order
     */
    async shipOrder(orderId) {
        const { value: formValues } = await Swal.fire({
            title: 'Ship Order',
            html: `
                <div class="mb-3 text-start">
                    <label class="form-label fw-bold">Tracking Number:</label>
                    <input type="text" class="form-control" id="trackingNumber" placeholder="Enter tracking number">
                </div>
                <div class="mb-3 text-start">
                    <label class="form-label fw-bold">Shipping Notes:</label>
                    <textarea class="form-control" id="shippingNotes" rows="3" placeholder="Optional shipping notes"></textarea>
                </div>
            `,
            showCancelButton: true,
            confirmButtonText: 'Ship Order',
            cancelButtonText: 'Cancel',
            confirmButtonColor: '#0dcaf0',
            preConfirm: () => {
                const trackingNumber = document.getElementById('trackingNumber').value;
                if (!trackingNumber) {
                    Swal.showValidationMessage('Tracking number is required');
                    return false;
                }
                return {
                    trackingNumber: trackingNumber,
                    notes: document.getElementById('shippingNotes').value
                };
            }
        });

        if (formValues) {
            try {
                this.showLoading(true);
                
                // Simulate API call
                await this.delay(1000);
                
                // Update order status
                const order = this.orders.find(o => o.id === orderId);
                if (order) {
                    order.status = 'SHIPPED';
                    order.trackingNumber = formValues.trackingNumber;
                }
                
                this.renderOrders();
                
                Swal.fire({
                    title: 'Success!',
                    text: `Order has been shipped with tracking number: ${formValues.trackingNumber}`,
                    icon: 'success',
                    timer: 3000,
                    showConfirmButton: false
                });
                
            } catch (error) {
                console.error('Error shipping order:', error);
                this.showErrorMessage('Failed to ship order');
            } finally {
                this.showLoading(false);
            }
        }
    }

    /**
     * Cancel order
     */
    async cancelOrder(orderId) {
        const result = await Swal.fire({
            title: 'Cancel Order',
            text: 'Are you sure you want to cancel this order? This action cannot be undone.',
            icon: 'warning',
            showCancelButton: true,
            confirmButtonText: 'Yes, Cancel Order',
            cancelButtonText: 'Keep Order',
            confirmButtonColor: '#dc3545'
        });

        if (result.isConfirmed) {
            try {
                this.showLoading(true);
                
                // Simulate API call
                await this.delay(1000);
                
                // Update order status
                const order = this.orders.find(o => o.id === orderId);
                if (order) {
                    order.status = 'CANCELLED';
                }
                
                this.renderOrders();
                
                Swal.fire({
                    title: 'Cancelled!',
                    text: 'Order has been cancelled',
                    icon: 'success',
                    timer: 2000,
                    showConfirmButton: false
                });
                
            } catch (error) {
                console.error('Error cancelling order:', error);
                this.showErrorMessage('Failed to cancel order');
            } finally {
                this.showLoading(false);
            }
        }
    }

    /**
     * Edit order
     */
    async editOrder(orderId) {
        // This would typically open a comprehensive edit form
        Swal.fire({
            title: 'Edit Order',
            text: 'Edit functionality would be implemented here',
            icon: 'info'
        });
    }

    /**
     * View payment proof
     */
    async viewPaymentProof(orderId) {
        const order = this.orders.find(o => o.id === orderId);
        if (!order || !order.hasPaymentProof) return;

        // Simulate loading payment proof image
        Swal.fire({
            title: `Payment Proof - ${order.orderId}`,
            html: `
                <div class="text-center">
                    <img src="https://via.placeholder.com/400x300/0d6efd/ffffff?text=Payment+Proof" 
                         class="img-fluid rounded shadow" 
                         alt="Payment Proof"
                         style="max-width: 100%; height: auto;">
                    <p class="mt-3 text-muted">Click outside to close</p>
                </div>
            `,
            showConfirmButton: false,
            customClass: {
                popup: 'payment-proof-modal'
            },
            backdrop: true,
            allowOutsideClick: true
        });
    }

    /**
     * Reset all filters
     */
    resetFilters() {
        // Reset filter values
        this.currentFilter = 'all';
        this.searchQuery = '';
        this.currentPage = 1;
        
        // Reset UI elements
        const searchInput = document.getElementById('searchOrders');
        const statusFilter = document.getElementById('statusFilter');
        
        if (searchInput) searchInput.value = '';
        if (statusFilter) statusFilter.value = 'all';
        
        // Re-render with animation
        this.renderOrders();
        this.renderPagination();
        
        // Show feedback
        const resetBtn = document.querySelector('[onclick*="resetFilters"]');
        if (resetBtn) {
            const originalContent = resetBtn.innerHTML;
            resetBtn.innerHTML = '<i class="fas fa-check me-1"></i>Reset';
            resetBtn.classList.add('btn-success');
            resetBtn.classList.remove('btn-outline-secondary');
            
            setTimeout(() => {
                resetBtn.innerHTML = originalContent;
                resetBtn.classList.remove('btn-success');
                resetBtn.classList.add('btn-outline-secondary');
            }, 1500);
        }
    }

    /**
     * Refresh orders
     */
    async refreshOrders() {
        await this.loadOrders();
        
        // Show refresh feedback
        const refreshBtn = document.querySelector('[onclick*="refreshOrders"]');
        if (refreshBtn) {
            const originalContent = refreshBtn.innerHTML;
            refreshBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';
            
            setTimeout(() => {
                refreshBtn.innerHTML = originalContent;
            }, 1000);
        }
    }

    /**
     * Handle responsive adjustments
     */
    handleResponsiveAdjustments() {
        const isMobile = window.innerWidth < 768;
        const table = document.querySelector('.orders-table');
        
        if (table) {
            if (isMobile) {
                table.classList.add('table-responsive-mobile');
            } else {
                table.classList.remove('table-responsive-mobile');
            }
        }
    }

    /**
     * Update table stats
     */
    updateTableStats(totalOrders) {
        let statsContainer = document.getElementById('tableStats');
        if (!statsContainer) {
            statsContainer = document.createElement('div');
            statsContainer.id = 'tableStats';
            statsContainer.className = 'table-stats mb-3';
            
            const tableContainer = document.querySelector('.table-container');
            if (tableContainer) {
                tableContainer.parentNode.insertBefore(statsContainer, tableContainer);
            }
        }

        const startIndex = (this.currentPage - 1) * this.ordersPerPage + 1;
        const endIndex = Math.min(this.currentPage * this.ordersPerPage, totalOrders);

        statsContainer.innerHTML = `
            <div class="d-flex justify-content-between align-items-center">
                <small class="text-muted">
                    Showing ${startIndex}-${endIndex} of ${totalOrders} orders
                </small>
                <button class="btn btn-sm btn-outline-primary" onclick="adminOrders.refreshOrders()">
                    <i class="fas fa-sync-alt me-1"></i>Refresh
                </button>
            </div>
        `;
    }

    /**
     * Utility functions
     */
    getStatusClass(status) {
        const statusClasses = {
            'PENDING_PAYMENT': 'status-pending',
            'PENDING_CONFIRMATION': 'status-pending',
            'PROCESSING': 'status-processing',
            'SHIPPED': 'status-confirmed',
            'DELIVERED': 'status-shipped',
            'CANCELLED': 'status-cancelled'
        };
        return statusClasses[status] || 'status-pending';
    }

    getStatusText(status) {
        const statusTexts = {
            'PENDING_PAYMENT': 'Pending Payment',
            'PENDING_CONFIRMATION': 'Pending Confirmation',
            'PROCESSING': 'Processing',
            'SHIPPED': 'Shipped',
            'DELIVERED': 'Delivered',
            'CANCELLED': 'Cancelled'
        };
        return statusTexts[status] || status;
    }

    formatCurrency(amount) {
        return new Intl.NumberFormat('id-ID', {
            style: 'currency',
            currency: 'IDR',
            minimumFractionDigits: 0
        }).format(amount);
    }

    formatDate(date) {
        return new Intl.DateTimeFormat('id-ID', {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        }).format(new Date(date));
    }

    showLoading(show) {
        const overlay = document.getElementById('loadingOverlay');
        if (overlay) {
            if (show) {
                overlay.classList.add('show');
            } else {
                overlay.classList.remove('show');
            }
        }
    }

    showErrorMessage(message) {
        Swal.fire({
            title: 'Error',
            text: message,
            icon: 'error',
            confirmButtonColor: '#dc3545'
        });
    }

    delay(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }

    debounce(func, wait) {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func(...args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    }
}

// Initialize admin orders manager when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    window.adminOrders = new AdminOrdersManager();
});

// Export for module systems
if (typeof module !== 'undefined' && module.exports) {
    module.exports = AdminOrdersManager;
}
