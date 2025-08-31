-- Database Schema untuk Sistem Pesanan
-- Jalankan script ini di database MySQL/MariaDB

-- Tabel untuk menyimpan pesanan
CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_number VARCHAR(50) UNIQUE NOT NULL,
    user_id BIGINT NOT NULL,
    total_amount DECIMAL(15,2) NOT NULL,
    shipping_address TEXT,
    customer_name VARCHAR(255),
    customer_phone VARCHAR(20),
    customer_email VARCHAR(255),
    payment_method VARCHAR(100),
    status ENUM('PENDING_PAYMENT', 'PENDING_CONFIRMATION', 'PAID', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED', 'REFUNDED') NOT NULL DEFAULT 'PENDING_PAYMENT',
    notes TEXT,
    is_reserved_stock BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NULL,
    paid_at TIMESTAMP NULL,
    cancelled_at TIMESTAMP NULL,
    
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    INDEX idx_order_number (order_number)
);

-- Tabel untuk item-item dalam pesanan
CREATE TABLE IF NOT EXISTS order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(15,2) NOT NULL,
    subtotal DECIMAL(15,2) NOT NULL,
    notes TEXT,
    
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id),
    
    INDEX idx_order_id (order_id),
    INDEX idx_product_id (product_id)
);

-- Tabel untuk transaksi pembayaran
CREATE TABLE IF NOT EXISTS payment_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    external_payment_id VARCHAR(255),
    amount DECIMAL(15,2) NOT NULL,
    payment_method VARCHAR(100),
    status ENUM('PENDING', 'PROCESSING', 'SUCCESS', 'FAILED', 'EXPIRED', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
    gateway_response TEXT,
    raw_payload TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    notes TEXT,
    
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    
    INDEX idx_order_id (order_id),
    INDEX idx_external_payment_id (external_payment_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
);

-- Insert sample data untuk testing (opsional)
INSERT INTO orders (order_number, user_id, total_amount, customer_name, customer_phone, customer_email, shipping_address, payment_method, status, notes) VALUES
('ORD-001-2024', 1, 1500000.00, 'John Doe', '081234567890', 'john@example.com', 'Jl. Contoh No. 123, Jakarta', 'BANK_TRANSFER', 'PENDING_CONFIRMATION', 'Pesanan pertama'),
('ORD-002-2024', 1, 2500000.00, 'Jane Smith', '081234567891', 'jane@example.com', 'Jl. Sample No. 456, Bandung', 'E_WALLET', 'PENDING_PAYMENT', 'Pesanan kedua'),
('ORD-003-2024', 2, 3000000.00, 'Bob Johnson', '081234567892', 'bob@example.com', 'Jl. Test No. 789, Surabaya', 'CREDIT_CARD', 'PAID', 'Pesanan ketiga'),
('ORD-004-2024', 2, 1800000.00, 'Alice Brown', '081234567893', 'alice@example.com', 'Jl. Demo No. 321, Medan', 'BANK_TRANSFER', 'PROCESSING', 'Pesanan keempat'),
('ORD-005-2024', 3, 2200000.00, 'Charlie Wilson', '081234567894', 'charlie@example.com', 'Jl. Trial No. 654, Semarang', 'E_WALLET', 'SHIPPED', 'Pesanan kelima');

-- Insert sample order items
INSERT INTO order_items (order_id, product_id, quantity, unit_price, subtotal) VALUES
(1, 1, 2, 750000.00, 1500000.00),
(2, 2, 1, 2500000.00, 2500000.00),
(3, 3, 3, 1000000.00, 3000000.00),
(4, 1, 1, 1800000.00, 1800000.00),
(5, 2, 1, 2200000.00, 2200000.00);

-- Insert sample payment transactions
INSERT INTO payment_transactions (order_id, external_payment_id, amount, payment_method, status, notes) VALUES
(1, 'PAY-001-2024', 1500000.00, 'BANK_TRANSFER', 'SUCCESS', 'Pembayaran berhasil'),
(2, 'PAY-002-2024', 2500000.00, 'E_WALLET', 'PENDING', 'Menunggu konfirmasi'),
(3, 'PAY-003-2024', 3000000.00, 'CREDIT_CARD', 'SUCCESS', 'Pembayaran berhasil'),
(4, 'PAY-004-2024', 1800000.00, 'BANK_TRANSFER', 'SUCCESS', 'Pembayaran berhasil'),
(5, 'PAY-005-2024', 2200000.00, 'E_WALLET', 'SUCCESS', 'Pembayaran berhasil');

-- Update orders dengan payment transaction yang berhasil
UPDATE orders SET 
    status = 'PAID',
    paid_at = NOW()
WHERE id IN (1, 3, 4, 5);

-- Update orders dengan status yang sesuai
UPDATE orders SET 
    status = 'PROCESSING'
WHERE id = 4;

UPDATE orders SET 
    status = 'SHIPPED'
WHERE id = 5;