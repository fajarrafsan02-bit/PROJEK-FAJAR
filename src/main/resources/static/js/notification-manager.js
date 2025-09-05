class NotificationManager {
    constructor() {
        // Don't use localStorage for notifications, fetch directly from database
        this.notifications = [];
        this.init();
    }

    init() {
        this.setupEventListeners();
        this.updateNotificationBadge();
        this.renderNotifications();
        this.startPeriodicCheck();
        this.setupStorageListener();
    }

    // Setup storage event listener for cross-tab synchronization
    setupStorageListener() {
        // We don't need storage listener since we're not using localStorage
        console.log('NotificationManager: Storage listener disabled (using database instead of localStorage)');
    }

    setupEventListeners() {
        const notificationIcon = document.getElementById('notificationIcon');
        const notificationDropdown = document.getElementById('notificationDropdown');
        const clearNotificationsBtn = document.getElementById('clearNotifications');

        // Toggle notification dropdown
        if (notificationIcon) {
            notificationIcon.addEventListener('click', (e) => {
                e.stopPropagation();
                notificationDropdown.classList.toggle('show');
                
                // Mark all notifications as read when opening
                if (notificationDropdown.classList.contains('show')) {
                    this.markAllAsRead();
                }
            });
        }

        // Close dropdown when clicking outside
        document.addEventListener('click', (e) => {
            if (notificationDropdown && !notificationDropdown.contains(e.target) && 
                notificationIcon && !notificationIcon.contains(e.target)) {
                notificationDropdown.classList.remove('show');
            }
        });

        // Clear all notifications
        if (clearNotificationsBtn) {
            clearNotificationsBtn.addEventListener('click', () => {
                this.clearAllNotifications();
            });
        }
    }

    // Add a new notification (only used internally now)
    addNotification(title, message, orderId = null) {
        // Create a unique key for this notification to prevent duplicates
        const notificationKey = `${title}|${message}|${orderId || 'no-order'}`;
        
        // Check if a similar notification was recently added (within last 5 minutes)
        const fiveMinutesAgo = Date.now() - (5 * 60 * 1000);
        const existingNotification = this.notifications.find(n => 
            n.notificationKey === notificationKey && new Date(n.timestamp).getTime() > fiveMinutesAgo);
        
        if (existingNotification) {
            console.log('Recent notification already exists, skipping:', title, message);
            return;
        }
        
        const notification = {
            id: Date.now().toString(),
            notificationKey: notificationKey,
            title: title,
            message: message,
            orderId: orderId,
            timestamp: new Date().toISOString(),
            read: false
        };

        this.notifications.unshift(notification);
        // Keep only the last 20 notifications
        if (this.notifications.length > 20) {
            this.notifications = this.notifications.slice(0, 20);
        }

        // Don't save to localStorage, we're fetching from database
        this.updateNotificationBadge();
        this.renderNotifications();
        
        // Show toast notification
        this.showToast(title, message);
    }

    // Mark all notifications as read
    markAllAsRead() {
        this.notifications.forEach(notification => {
            notification.read = true;
        });
        // Don't save to localStorage, we're fetching from database
        this.updateNotificationBadge();
        this.renderNotifications();
    }

    // Clear all notifications
    clearAllNotifications() {
        this.notifications = [];
        // Don't save to localStorage, we're fetching from database
        this.updateNotificationBadge();
        this.renderNotifications();
    }

    // Update notification badge count
    updateNotificationBadge() {
        const badge = document.getElementById('notificationBadge');
        if (!badge) return;

        const unreadCount = this.notifications.filter(n => !n.read).length;
        badge.textContent = unreadCount > 0 ? unreadCount : '0';
        badge.style.display = unreadCount > 0 ? 'inline-block' : 'none';
    }

    // Render notifications in the dropdown
    renderNotifications() {
        const notificationList = document.getElementById('notificationList');
        if (!notificationList) return;

        if (this.notifications.length === 0) {
            notificationList.innerHTML = `
                <li class="notification-empty">
                    <i class="fas fa-bell-slash"></i>
                    <p>Tidak ada notifikasi</p>
                </li>
            `;
            return;
        }

        notificationList.innerHTML = this.notifications.map(notification => {
            const timeAgo = this.getTimeAgo(new Date(notification.timestamp));
            const unreadClass = !notification.read ? 'unread' : '';
            
            return `
                <li class="notification-item ${unreadClass}" data-id="${notification.id}" data-order-id="${notification.orderId || ''}">
                    <div class="notification-title">${notification.title}</div>
                    <div class="notification-message">${notification.message}</div>
                    <div class="notification-time">${timeAgo}</div>
                </li>
            `;
        }).join('');

        // Add click event for notification items
        notificationList.querySelectorAll('.notification-item').forEach(item => {
            item.addEventListener('click', (e) => {
                const orderId = item.dataset.orderId;
                if (orderId) {
                    // Navigate to order details page
                    window.location.href = `/user/my-orders?orderId=${orderId}`;
                }
            });
        });
    }

    // Show toast notification
    showToast(title, message) {
        // Remove existing toast notifications
        document.querySelectorAll('.notification-toast').forEach(toast => {
            toast.classList.add('hide');
            setTimeout(() => {
                if (toast.parentNode) {
                    toast.parentNode.removeChild(toast);
                }
            }, 300);
        });

        const toast = document.createElement('div');
        toast.className = 'notification-toast';
        toast.innerHTML = `
            <div class="toast-icon">
                <i class="fas fa-bell"></i>
            </div>
            <div class="toast-content">
                <div class="toast-title">${title}</div>
                <div class="toast-message">${message}</div>
            </div>
        `;

        document.body.appendChild(toast);

        // Auto remove after 5 seconds
        setTimeout(() => {
            toast.classList.add('hide');
            setTimeout(() => {
                if (toast.parentNode) {
                    toast.parentNode.removeChild(toast);
                }
            }, 300);
        }, 5000);
    }

    // Get time ago string
    getTimeAgo(date) {
        const now = new Date();
        const diff = now - date;
        const minutes = Math.floor(diff / 60000);
        const hours = Math.floor(diff / 3600000);
        const days = Math.floor(diff / 86400000);

        if (minutes < 1) return 'baru saja';
        if (minutes < 60) return `${minutes} menit yang lalu`;
        if (hours < 24) return `${hours} jam yang lalu`;
        return `${days} hari yang lalu`;
    }

    // Check for real order status changes from database
    async checkForOrderUpdates() {
        try {
            // Check if user is authenticated
            const userResponse = await fetch('/user/current');
            if (!userResponse.ok) {
                console.log('User not authenticated, skipping order updates check');
                // Clear notifications when user is not authenticated
                this.notifications = [];
                this.updateNotificationBadge();
                this.renderNotifications();
                return;
            }
            
            const userData = await userResponse.json();
            if (!userData.success || !userData.data) {
                console.log('User data not available, skipping order updates check');
                return;
            }
            
            const userId = userData.data.id;
            console.log('Checking order updates for user ID:', userId);
            
            // Fetch user's orders directly from database
            const ordersResponse = await fetch(`/user/api/orders/my-orders?userId=${userId}`);
            if (!ordersResponse.ok) {
                console.log('Failed to fetch orders, status:', ordersResponse.status);
                // Clear notifications on error
                this.notifications = [];
                this.updateNotificationBadge();
                this.renderNotifications();
                return;
            }
            
            const ordersData = await ordersResponse.json();
            if (!ordersData.success || !ordersData.data || !ordersData.data.orders) {
                console.log('No orders data available');
                // Clear notifications when no data
                this.notifications = [];
                this.updateNotificationBadge();
                this.renderNotifications();
                return;
            }
            
            const orders = ordersData.data.orders;
            console.log('Fetched', orders.length, 'orders for user');
            
            // Clear existing notifications and rebuild from real data
            this.notifications = [];
            
            // Create notifications for each order with meaningful status
            for (const order of orders) {
                const orderNumber = order.orderNumber;
                const status = order.status;
                const statusDisplayName = order.statusDisplayName;
                
                let title, message;
                switch(status) {
                    case 'PENDING_PAYMENT':
                        title = 'Menunggu Pembayaran';
                        message = `Pesanan ${orderNumber} menunggu pembayaran. Silakan lakukan pembayaran untuk melanjutkan.`;
                        break;
                    case 'PENDING_CONFIRMATION':
                        title = 'Menunggu Konfirmasi';
                        message = `Pesanan ${orderNumber} menunggu konfirmasi pembayaran. Admin akan memverifikasi pembayaran Anda.`;
                        break;
                    case 'PAID':
                        title = 'Pembayaran Diterima';
                        message = `Pembayaran untuk pesanan ${orderNumber} telah diterima. Pesanan akan segera diproses.`;
                        break;
                    case 'PROCESSING':
                        title = 'Pesanan Diproses';
                        message = `Pesanan ${orderNumber} sedang diproses. Kami akan segera mengirimkan barang Anda.`;
                        break;
                    case 'SHIPPED':
                        title = 'Pesanan Dikirim';
                        message = `Pesanan ${orderNumber} telah dikirim. Perkiraan sampai dalam 2-3 hari kerja.`;
                        break;
                    case 'DELIVERED':
                        title = 'Pesanan Selesai';
                        message = `Pesanan ${orderNumber} telah selesai. Terima kasih atas pembelian Anda!`;
                        break;
                    case 'CANCELLED':
                        title = 'Pesanan Dibatalkan';
                        message = `Pesanan ${orderNumber} telah dibatalkan.`;
                        break;
                    default:
                        // Don't create notifications for other statuses
                        continue;
                }
                
                // Add the notification
                this.addNotification(title, message, orderNumber);
                console.log('Added notification for order', orderNumber, 'with status', status);
            }
            
            // Update UI with notifications
            this.updateNotificationBadge();
            this.renderNotifications();
            
        } catch (error) {
            console.error('Error checking for order updates:', error);
            // Clear notifications on error
            this.notifications = [];
            this.updateNotificationBadge();
            this.renderNotifications();
        }
    }

    // Start periodic checking for updates
    startPeriodicCheck() {
        // Check for updates every 30 seconds
        setInterval(() => {
            this.checkForOrderUpdates();
        }, 30000);
    }
}

    setupEventListeners() {
        const notificationIcon = document.getElementById('notificationIcon');
        const notificationDropdown = document.getElementById('notificationDropdown');
        const clearNotificationsBtn = document.getElementById('clearNotifications');

        // Toggle notification dropdown
        if (notificationIcon) {
            notificationIcon.addEventListener('click', (e) => {
                e.stopPropagation();
                notificationDropdown.classList.toggle('show');
                
                // Mark all notifications as read when opening
                if (notificationDropdown.classList.contains('show')) {
                    this.markAllAsRead();
                }
            });
        }

        // Close dropdown when clicking outside
        document.addEventListener('click', (e) => {
            if (notificationDropdown && !notificationDropdown.contains(e.target) && 
                notificationIcon && !notificationIcon.contains(e.target)) {
                notificationDropdown.classList.remove('show');
            }
        });

        // Clear all notifications
        if (clearNotificationsBtn) {
            clearNotificationsBtn.addEventListener('click', () => {
                this.clearAllNotifications();
            });
        }
    }

    // Add a new notification
    addNotification(title, message, orderId = null) {
        // Create a unique key for this notification to prevent duplicates
        const notificationKey = `${title}|${message}|${orderId || 'no-order'}`;
        
        // Check if a similar notification was recently added (within last 5 minutes)
        const fiveMinutesAgo = Date.now() - (5 * 60 * 1000);
        const existingNotification = this.notifications.find(n => 
            n.notificationKey === notificationKey && new Date(n.timestamp).getTime() > fiveMinutesAgo);
        
        if (existingNotification) {
            console.log('Recent notification already exists, skipping:', title, message);
            return;
        }
        
        const notification = {
            id: Date.now().toString(),
            notificationKey: notificationKey,
            title: title,
            message: message,
            orderId: orderId,
            timestamp: new Date().toISOString(),
            read: false
        };

        this.notifications.unshift(notification);
        // Keep only the last 20 notifications
        if (this.notifications.length > 20) {
            this.notifications = this.notifications.slice(0, 20);
        }

        this.saveToLocalStorage();
        this.updateNotificationBadge();
        this.renderNotifications();
        
        // Show toast notification
        this.showToast(title, message);
    }

    // Mark all notifications as read
    markAllAsRead() {
        this.notifications.forEach(notification => {
            notification.read = true;
        });
        this.saveToLocalStorage();
        this.updateNotificationBadge();
        this.renderNotifications();
    }

    // Clear all notifications
    clearAllNotifications() {
        this.notifications = [];
        this.saveToLocalStorage();
        this.updateNotificationBadge();
        this.renderNotifications();
    }

    // Save notifications to localStorage - REMOVED since we're not using localStorage
    /*
    saveToLocalStorage() {
        const notificationsJson = JSON.stringify(this.notifications);
        localStorage.setItem('userNotifications', notificationsJson);
        
        // Dispatch storage event for cross-tab synchronization
        if (typeof window !== 'undefined' && window.dispatchEvent) {
            const event = new StorageEvent('storage', {
                key: 'userNotifications',
                oldValue: null,
                newValue: notificationsJson,
                url: window.location.href,
                storageArea: localStorage
            });
            window.dispatchEvent(event);
        }
    }
    */

    // Update notification badge count
    updateNotificationBadge() {
        const badge = document.getElementById('notificationBadge');
        if (!badge) return;

        const unreadCount = this.notifications.filter(n => !n.read).length;
        badge.textContent = unreadCount > 0 ? unreadCount : '0';
        badge.style.display = unreadCount > 0 ? 'inline-block' : 'none';
    }

    // Render notifications in the dropdown
    renderNotifications() {
        const notificationList = document.getElementById('notificationList');
        if (!notificationList) return;

        if (this.notifications.length === 0) {
            notificationList.innerHTML = `
                <li class="notification-empty">
                    <i class="fas fa-bell-slash"></i>
                    <p>Tidak ada notifikasi</p>
                </li>
            `;
            return;
        }

        notificationList.innerHTML = this.notifications.map(notification => {
            const timeAgo = this.getTimeAgo(new Date(notification.timestamp));
            const unreadClass = !notification.read ? 'unread' : '';
            
            return `
                <li class="notification-item ${unreadClass}" data-id="${notification.id}" data-order-id="${notification.orderId || ''}">
                    <div class="notification-title">${notification.title}</div>
                    <div class="notification-message">${notification.message}</div>
                    <div class="notification-time">${timeAgo}</div>
                </li>
            `;
        }).join('');

        // Add click event for notification items
        notificationList.querySelectorAll('.notification-item').forEach(item => {
            item.addEventListener('click', (e) => {
                const orderId = item.dataset.orderId;
                if (orderId) {
                    // Navigate to order details page
                    window.location.href = `/user/my-orders?orderId=${orderId}`;
                }
            });
        });
    }

    // Show toast notification
    showToast(title, message) {
        // Remove existing toast notifications
        document.querySelectorAll('.notification-toast').forEach(toast => {
            toast.classList.add('hide');
            setTimeout(() => {
                if (toast.parentNode) {
                    toast.parentNode.removeChild(toast);
                }
            }, 300);
        });

        const toast = document.createElement('div');
        toast.className = 'notification-toast';
        toast.innerHTML = `
            <div class="toast-icon">
                <i class="fas fa-bell"></i>
            </div>
            <div class="toast-content">
                <div class="toast-title">${title}</div>
                <div class="toast-message">${message}</div>
            </div>
        `;

        document.body.appendChild(toast);

        // Auto remove after 5 seconds
        setTimeout(() => {
            toast.classList.add('hide');
            setTimeout(() => {
                if (toast.parentNode) {
                    toast.parentNode.removeChild(toast);
                }
            }, 300);
        }, 5000);
    }

    // Get time ago string
    getTimeAgo(date) {
        const now = new Date();
        const diff = now - date;
        const minutes = Math.floor(diff / 60000);
        const hours = Math.floor(diff / 3600000);
        const days = Math.floor(diff / 86400000);

        if (minutes < 1) return 'baru saja';
        if (minutes < 60) return `${minutes} menit yang lalu`;
        if (hours < 24) return `${hours} jam yang lalu`;
        return `${days} hari yang lalu`;
    }

    // Check for real order status changes from database
    async checkForOrderUpdates() {
        try {
            // Check if user is authenticated
            const userResponse = await fetch('/user/current');
            if (!userResponse.ok) {
                console.log('User not authenticated, skipping order updates check');
                return;
            }
            
            const userData = await userResponse.json();
            if (!userData.success || !userData.data) {
                console.log('User data not available, skipping order updates check');
                return;
            }
            
            const userId = userData.data.id;
            console.log('Checking order updates for user ID:', userId);
            
            // Fetch user's orders
            const ordersResponse = await fetch('/user/api/orders/my-orders');
            if (!ordersResponse.ok) {
                console.log('Failed to fetch orders, status:', ordersResponse.status);
                return;
            }
            
            const ordersData = await ordersResponse.json();
            if (!ordersData.success || !ordersData.data || !ordersData.data.orders) {
                console.log('No orders data available');
                return;
            }
            
            const orders = ordersData.data.orders;
            console.log('Fetched', orders.length, 'orders for user');
            
            // Get previously stored order statuses to detect changes
            const previousOrderStatuses = JSON.parse(localStorage.getItem('orderStatuses_' + userId) || '{}');
            const currentOrderStatuses = {};
            
            // Check each order for status changes
            for (const order of orders) {
                const orderNumber = order.orderNumber;
                const status = order.status;
                const statusDisplayName = order.statusDisplayName;
                
                // Store current status for next comparison
                currentOrderStatuses[orderNumber] = status;
                
                // Check if status has changed from previous check
                const previousStatus = previousOrderStatuses[orderNumber];
                if (previousStatus && previousStatus !== status) {
                    // Status has changed, create notification
                    let title, message;
                    switch(status) {
                        case 'PENDING_PAYMENT':
                            title = 'Menunggu Pembayaran';
                            message = `Pesanan ${orderNumber} menunggu pembayaran. Silakan lakukan pembayaran untuk melanjutkan.`;
                            break;
                        case 'PENDING_CONFIRMATION':
                            title = 'Menunggu Konfirmasi';
                            message = `Pesanan ${orderNumber} menunggu konfirmasi pembayaran. Admin akan memverifikasi pembayaran Anda.`;
                            break;
                        case 'PAID':
                            title = 'Pembayaran Diterima';
                            message = `Pembayaran untuk pesanan ${orderNumber} telah diterima. Pesanan akan segera diproses.`;
                            break;
                        case 'PROCESSING':
                            title = 'Pesanan Diproses';
                            message = `Pesanan ${orderNumber} sedang diproses. Kami akan segera mengirimkan barang Anda.`;
                            break;
                        case 'SHIPPED':
                            title = 'Pesanan Dikirim';
                            message = `Pesanan ${orderNumber} telah dikirim. Perkiraan sampai dalam 2-3 hari kerja.`;
                            break;
                        case 'DELIVERED':
                            title = 'Pesanan Selesai';
                            message = `Pesanan ${orderNumber} telah selesai. Terima kasih atas pembelian Anda!`;
                            break;
                        case 'CANCELLED':
                            title = 'Pesanan Dibatalkan';
                            message = `Pesanan ${orderNumber} telah dibatalkan.`;
                            break;
                        default:
                            // Don't create notifications for other statuses
                            continue;
                    }
                    
                    // Add the notification
                    this.addNotification(title, message, orderNumber);
                    console.log('Added notification for order', orderNumber, 'status changed to', status);
                }
            }
            
            // Save current order statuses for next comparison
            localStorage.setItem('orderStatuses_' + userId, JSON.stringify(currentOrderStatuses));
            
        } catch (error) {
            console.error('Error checking for order updates:', error);
        }
    }

    // Start periodic checking for updates
    startPeriodicCheck() {
        // Check for updates every 30 seconds
        setInterval(() => {
            this.checkForOrderUpdates();
        }, 30000);
    }
}

// Initialize notification manager when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    // Initialize notification manager
    window.notificationManager = new NotificationManager();
});