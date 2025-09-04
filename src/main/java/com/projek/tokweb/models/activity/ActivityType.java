package com.projek.tokweb.models.activity;

public enum ActivityType {
    // Gold Price Activities
    GOLD_PRICE_UPDATE_API("Update Harga Emas dari API"),
    GOLD_PRICE_UPDATE_MANUAL("Update Harga Emas Manual"),
    GOLD_PRICE_FETCH_EXTERNAL("Ambil Harga Emas Eksternal"),
    GOLD_PRICE_NO_CHANGE("Harga Emas Tidak Berubah"),
    
    // Product Activities
    PRODUCT_CREATE("Buat Produk Baru"),
    PRODUCT_UPDATE("Update Produk"),
    PRODUCT_DELETE("Hapus Produk"),
    PRODUCT_PRICE_UPDATE("Update Harga Produk"),
    
    // Order Activities
    ORDER_NEW("Pesanan Baru Masuk"),
    ORDER_CREATE("Pesanan Baru"),
    ORDER_UPDATE("Update Pesanan"),
    ORDER_STATUS_UPDATE("Update Status Pesanan"),
    ORDER_CONFIRMED("Pesanan Dikonfirmasi"),
    ORDER_PROCESSING("Pesanan Sedang Diproses"),
    ORDER_SHIPPED("Pesanan Dikirim"),
    ORDER_COMPLETED("Pesanan Selesai"),
    ORDER_CANCELLED("Pesanan Dibatalkan"),
    ORDER_PAYMENT("Pembayaran Pesanan"),
    ORDER_CANCEL("Batalkan Pesanan"),
    ORDER_COMPLETE("Selesaikan Pesanan"),
    
    // User Activities
    USER_LOGIN("Login User"),
    USER_LOGOUT("Logout User"),
    USER_REGISTER("Registrasi User"),
    USER_UPDATE_PROFILE("Update Profil User"),
    
    // Admin Activities
    ADMIN_LOGIN("Login Admin"),
    ADMIN_LOGOUT("Logout Admin"),
    ADMIN_SYSTEM_ACTION("Aksi Sistem Admin"),
    
    // System Activities
    SYSTEM_START("Sistem Mulai"),
    SYSTEM_ERROR("Error Sistem"),
    SYSTEM_MAINTENANCE("Maintenance Sistem"),
    SYSTEM_BACKUP("Backup Sistem"),
    
    // Other Activities
    OTHER("Aktivitas Lainnya");
    
    private final String description;
    
    ActivityType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
