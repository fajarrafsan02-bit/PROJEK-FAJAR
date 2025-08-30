package com.projek.tokweb.service.cloudinary;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CloudinaryService {

    @Autowired
    private final Cloudinary cloudinary;

    @SuppressWarnings("unchecked")
    public String uploadFile(MultipartFile file) throws IOException {
        Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(),
                ObjectUtils.asMap("folder", "produk-emas"));
        return uploadResult.get("secure_url").toString();
    }

    @SuppressWarnings("unchecked")
    public String uploadFileWithCheck(MultipartFile file) throws IOException {
        // Generate hash dari file untuk mengecek duplikasi
        String fileHash = generateFileHash(file);

        // Cek apakah image dengan hash yang sama sudah ada
        String existingImageUrl = findImageByHash(fileHash);
        if (existingImageUrl != null) {
            System.out.println("Image dengan hash yang sama sudah ada: " + existingImageUrl);
            return existingImageUrl;
        }

        // Jika tidak ada, upload image baru
        Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(),
                ObjectUtils.asMap("folder", "produk-emas"));

        String imageUrl = uploadResult.get("secure_url").toString();
        System.out.println("Image baru diupload: " + imageUrl);
        return imageUrl;
    }

    @SuppressWarnings("unchecked")
    public String uploadFileWithReplacement(MultipartFile file, String oldImageUrl) throws IOException {
        // Generate hash dari file baru
        String newFileHash = generateFileHash(file);

        // Cek apakah image dengan hash yang sama sudah ada
        String existingImageUrl = findImageByHash(newFileHash);
        if (existingImageUrl != null && !existingImageUrl.equals(oldImageUrl)) {
            System.out.println("Image dengan hash yang sama sudah ada: " + existingImageUrl);
            // Hapus image lama jika berbeda
            if (oldImageUrl != null && !oldImageUrl.equals(existingImageUrl)) {
                deleteImage(oldImageUrl);
            }
            return existingImageUrl;
        }

        // Jika image baru sama dengan yang lama, return yang lama
        if (oldImageUrl != null && existingImageUrl != null && existingImageUrl.equals(oldImageUrl)) {
            System.out.println("Image tidak berubah, menggunakan yang lama: " + oldImageUrl);
            return oldImageUrl;
        }

        // Hapus image lama jika ada
        if (oldImageUrl != null) {
            deleteImage(oldImageUrl);
        }

        // Upload image baru
        Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(),
                ObjectUtils.asMap("folder", "produk-emas"));

        String imageUrl = uploadResult.get("secure_url").toString();
        System.out.println("Image baru diupload menggantikan yang lama: " + imageUrl);
        return imageUrl;
    }

    private String generateFileHash(MultipartFile file) throws IOException {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(file.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new IOException("Gagal generate hash file", e);
        }
    }

    private String findImageByHash(String fileHash) {
        try {
            // Cari image berdasarkan hash di database atau cache
            // Untuk sementara, kita akan menggunakan pendekatan sederhana
            // dengan mencari berdasarkan nama file atau metadata

            // Implementasi ini bisa disesuaikan dengan kebutuhan
            // Misalnya menyimpan hash di database atau menggunakan cache

            return null; // Return null jika tidak ditemukan
        } catch (Exception e) {
            System.err.println("Error mencari image berdasarkan hash: " + e.getMessage());
            return null;
        }
    }

    // Method untuk mengecek apakah dua file sama
    public boolean isSameImage(MultipartFile file1, String imageUrl2) throws IOException {
        if (file1 == null || imageUrl2 == null) {
            return false;
        }

        String hash1 = generateFileHash(file1);
        System.out.println("Hash file1: " + hash1);
        // Untuk image URL, kita perlu download dulu untuk generate hash
        // Ini bisa dioptimasi dengan menyimpan hash di database

        return false; // Sementara return false
    }

    @SuppressWarnings("unchecked")
    public void deleteImage(String imageUrl) throws IOException {
        try {
            String publicId = extractPublicIdFromUrl(imageUrl);
            if (publicId != null) {
                Map<String, Object> deleteResult = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                System.out.println("Image Di hapus dari cloudinary " + publicId);
                System.out.println("hasil hapus " + deleteResult);
            } else {
                System.err.println("Cloud gagal extract public Id dari URL: " + imageUrl);
            }
        } catch (Exception e) {
            System.err.println("Terjadi kesalahan saat menghapus gambar dari Cloudinary: " + e.getMessage());
            throw new IOException("Gagal menghapus gambar dari cloudinary: " + e.getMessage(), e);
        }
    }

    private String extractPublicIdFromUrl(String imageUrl) {
        try {
            if (imageUrl == null || !imageUrl.contains("cloudinary")) {
                return null;
            }

            // Split URL by "/"
            String[] parts = imageUrl.split("/");

            // Find the index of "upload"
            int uploadIndex = -1;
            for (int i = 0; i < parts.length; i++) {
                if ("upload".equals(parts[i])) {
                    uploadIndex = i;
                    break;
                }
            }

            if (uploadIndex == -1 || uploadIndex + 1 >= parts.length) {
                return null;
            }

            // Skip version number if present (starts with 'v')
            int startIndex = uploadIndex + 1;
            if (startIndex < parts.length && parts[startIndex].startsWith("v")) {
                startIndex++;
            }

            StringBuilder publicId = new StringBuilder();
            for (int i = startIndex; i < parts.length; i++) {
                if (i > startIndex) {
                    publicId.append("/");
                }
                String part = parts[i];
                if (i == parts.length - 1) {
                    int dotIndex = part.lastIndexOf(".");
                    if (dotIndex > 0) {
                        part = part.substring(0, dotIndex);
                    }
                }
                publicId.append(part);
            }
            return publicId.toString();
        } catch (Exception e) {
            System.err.println("Error extracting public ID from URL: " + imageUrl);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public boolean imageExists(String imageUrl) {
        try {
            String publicId = extractPublicIdFromUrl(imageUrl);
            if (publicId == null) {
                return false;
            }
            Map<String, Object> result = cloudinary.api().resource(publicId, ObjectUtils.emptyMap());
            return result != null && result.containsKey("public_id");
        } catch (Exception e) {
            System.err.println("Kesalahan saat memeriksa keberadaan gambar: " + e.getMessage());
            return false;
        }
    }
}
