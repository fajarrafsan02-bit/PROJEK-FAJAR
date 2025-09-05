package com.projek.tokweb.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

@Service
public class NotificationService {

    // Method untuk menyimpan notifikasi ke database
    public void saveNotification(String userId, String title, String message, String orderId) {
        // Implementasi untuk menyimpan notifikasi ke database
        System.out.println("Menyimpan notifikasi untuk user: " + userId + 
                          ", title: " + title + 
                          ", message: " + message + 
                          ", orderId: " + orderId);
    }

    // Method untuk mendapatkan notifikasi pengguna
    public List<Map<String, Object>> getUserNotifications(String userId) {
        // Implementasi untuk mengambil notifikasi dari database
        return new ArrayList<>();
    }

    // Method untuk menandai notifikasi sebagai sudah dibaca
    public void markAsRead(String notificationId) {
        // Implementasi untuk menandai notifikasi sebagai sudah dibaca
        System.out.println("Menandai notifikasi " + notificationId + " sebagai sudah dibaca");
    }

    // Method untuk menghapus notifikasi
    public void deleteNotification(String notificationId) {
        // Implementasi untuk menghapus notifikasi
        System.out.println("Menghapus notifikasi " + notificationId);
    }

    // Method untuk membuat notifikasi pesanan berubah
    public void createOrderStatusNotification(String userId, String orderId, String status) {
        String title = "";
        String message = "";
        
        switch(status.toLowerCase()) {
            case "diproses":
                title = "Pesanan Diproses";
                message = "Pesanan #" + orderId + " sedang diproses. Kami akan segera mengirimkan barang Anda.";
                break;
            case "dikirim":
                title = "Pesanan Dikirim";
                message = "Pesanan #" + orderId + " telah dikirim. Perkiraan sampai dalam 2-3 hari kerja.";
                break;
            case "selesai":
                title = "Pesanan Selesai";
                message = "Pesanan #" + orderId + " telah selesai. Terima kasih atas pembelian Anda!";
                break;
            case "dibatalkan":
                title = "Pesanan Dibatalkan";
                message = "Pesanan #" + orderId + " telah dibatalkan.";
                break;
            default:
                title = "Status Pesanan Berubah";
                message = "Status pesanan #" + orderId + " telah berubah menjadi " + status + ".";
        }
        
        saveNotification(userId, title, message, orderId);
    }
}