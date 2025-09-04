package com.projek.tokweb.controller.admin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.projek.tokweb.dto.ApiResponse;
import com.projek.tokweb.dto.admin.ProductRequestDto;
import com.projek.tokweb.dto.admin.ProductResponseDto;
import com.projek.tokweb.models.admin.Product;
import com.projek.tokweb.service.admin.ProductService;
import com.projek.tokweb.service.cloudinary.CloudinaryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

@RestController
@RequestMapping("/admin")
public class AdminControllerApi {
    @Autowired
    private ProductService productService;
    @Autowired
    private CloudinaryService cloudinaryService;

    // AdminControllerApi(AdminController adminController) {
    // this.adminController = adminController;
    // }

    @PostMapping(value = "/add-product", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> addProduct(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "weight", required = false) Double weight,
            @RequestParam(value = "purity", required = false) Integer purity,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "markup", required = false) Double markup,
            @RequestParam(value = "stock", required = false) Integer stock,
            @RequestParam(value = "minStock", required = false) Integer minStock,
            @RequestParam(value = "isActive", required = false) Boolean isActive,
            @RequestParam(value = "image", required = false) MultipartFile file) {

        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Gambar Produk tidak boleh kosong."));
            }

            ProductRequestDto request = ProductRequestDto.builder()
                    .name(name)
                    .description(description)
                    .weight(weight)
                    .purity(purity)
                    .category(category)
                    .markup(markup)
                    .stock(stock)
                    .minStock(minStock)
                    .isActive(isActive)
                    .build();

            List<String> validationErrors = validateProductRequest(request);
            if (!validationErrors.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Validasi gagal", validationErrors));
            }

            System.out.println("Request DTO: " + request);

            String imageUrl = cloudinaryService.uploadFileWithCheck(file);
            request.setImageUrl(imageUrl);

            ProductResponseDto saved = productService.addProductWithFormattedResponse(request);
            return ResponseEntity.ok(ApiResponse.success("Produk Berhasil Di Simpan.",
                    saved));

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Gagal mengunggah gambar: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping(value = "/update-product/{productId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateProduct(
            @PathVariable Long productId,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "weight", required = false) Double weight,
            @RequestParam(value = "purity", required = false) Integer purity,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "markup", required = false) Double markup,
            @RequestParam(value = "stock", required = false) Integer stock,
            @RequestParam(value = "minStock", required = false) Integer minStock,
            @RequestParam(value = "isActive", required = false) Boolean isActive,
            @RequestParam(value = "image", required = false) MultipartFile file) {
        try {
            // Cek apakah produk ada
            if (!productService.existsById(productId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Produk tidak ditemukan dengan ID: " + productId));
            }
            
            ProductResponseDto existingProduct = productService.getProductByIdWithFormattedResponse(productId);
            System.out.println("Existing product: " + existingProduct);

            ProductRequestDto request = ProductRequestDto.builder()
                    .name(name)
                    .description(description)
                    .weight(weight)
                    .purity(purity)
                    .category(category)
                    .stock(stock)
                    .minStock(minStock)
                    .markup(markup)
                    .isActive(isActive != null ? isActive : true)
                    .build();

            // Validasi data untuk update (tanpa validasi image karena opsional)
            List<String> validationErrors = validateProductRequestForUpdate(request);
            if (!validationErrors.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Validasi gagal", validationErrors));
            }

            // Handle image upload dengan replacement
            if (file != null && !file.isEmpty()) {
                try {
                    String oldImageUrl = existingProduct.getImageUrl();
                    String newImageUrl = cloudinaryService.uploadFileWithReplacement(file, oldImageUrl);
                    request.setImageUrl(newImageUrl);
                } catch (IOException e) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(ApiResponse.error("Gagal mengunggah gambar: " + e.getMessage()));
                }
            } else {
                // Jika tidak ada file baru, gunakan image yang lama
                request.setImageUrl(existingProduct.getImageUrl());
            }

            ProductResponseDto updated = productService.updateProductWithFormattedResponse(productId, request);
            return ResponseEntity.ok(ApiResponse.success("Produk Berhasil Di Update.", updated));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Gagal memperbarui produk: " + e.getMessage()));
        }
    }

    @Operation(summary = "Update Min Stock", description = "Update stok minimum produk")
    @PostMapping("/product/{productId}/min-stock")
    public ResponseEntity<?> updateMinStock(
            @Parameter(description = "ID produk", required = true) @PathVariable Long productId,
            @Parameter(description = "Stok minimum baru", required = true) @RequestParam int minStock) {

        try {
            // Validasi input
            if (minStock < 0) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Stok minimum tidak boleh negatif"));
            }

            // Ambil produk untuk validasi
            ProductResponseDto existingProduct = productService.getProductByIdWithFormattedResponse(productId);

            // Validasi minStock tidak boleh lebih besar dari stock saat ini
            if (minStock > existingProduct.getStock()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Stok minimum (" + minStock
                                + ") tidak boleh lebih besar dari stok saat ini (" + existingProduct.getStock() + ")"));
            }

            // Update menggunakan ProductRequestDto
            ProductRequestDto request = ProductRequestDto.builder()
                    .minStock(minStock)
                    .build();

            Product updated = productService.updateProduct(productId, request);

            return ResponseEntity.ok(ApiResponse.success("Stok minimum berhasil diupdate", updated));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Gagal update stok minimum: " + e.getMessage()));
        }
    }

    private List<String> validateProductRequest(ProductRequestDto request) {
        List<String> errors = new ArrayList<>();

        if (request.getName() == null || request.getName().trim().isEmpty()) {
            errors.add("Nama produk tidak boleh kosong");
        } else if (request.getName().length() < 3 || request.getName().length() > 100) {
            errors.add("Nama produk harus antara 3-100 karakter");
        }

        if (request.getDescription() == null ||
                request.getDescription().trim().isEmpty()) {
            errors.add("Deskripsi produk tidak boleh kosong");
        } else if (request.getDescription().length() < 5 ||
                request.getDescription().length() > 500) {
            errors.add("Deskripsi produk harus antara 5-500 karakter");
        }

        if (request.getWeight() == null || request.getWeight() <= 0) {
            errors.add("Berat produk harus lebih dari 0");
        } else if (request.getWeight() > 10000) {
            errors.add("Berat produk maksimal 10.000 gram");
        }

        if (request.getPurity() == null || request.getPurity() < 10 ||
                request.getPurity() > 24) {
            errors.add("Kadar kemurnian harus antara 10-24 karat");
        }

        if (request.getCategory() == null || request.getCategory().trim().isEmpty()) {
            errors.add("Kategori produk tidak boleh kosong");
        } else if (!request.getCategory().matches("^(CINCIN|KALUNG|GELANG|BATANGAN)$")) {
            errors.add("Kategori harus CINCIN, KALUNG, GELANG, atau BATANGAN");
        }

        if (request.getMarkup() == null || request.getMarkup() < 0) {
            errors.add("Markup tidak boleh negatif");
        } else if (request.getMarkup() > 1000) {
            errors.add("Markup maksimal 1000%");
        }

        // Validasi stok
        if (request.getStock() != null && request.getStock() < 0) {
            errors.add("Stok tidak boleh negatif");
        }

        if (request.getMinStock() != null && request.getMinStock() < 0) {
            errors.add("Stok minimum tidak boleh negatif");
        }

        // Validasi minStock tidak boleh lebih besar dari stock
        if (request.getStock() != null && request.getMinStock() != null) {
            if (request.getMinStock() > request.getStock()) {
                errors.add("Stok minimum tidak boleh lebih besar dari stok");
            }
        }

        return errors;
    }

    // Method validasi khusus untuk update (lebih fleksibel)
    private List<String> validateProductRequestForUpdate(ProductRequestDto request) {
        List<String> errors = new ArrayList<>();

        if (request.getName() != null) {
            if (request.getName().trim().isEmpty()) {
                errors.add("Nama produk tidak boleh kosong");
            } else if (request.getName().length() < 3 || request.getName().length() > 100) {
                errors.add("Nama produk harus antara 3-100 karakter");
            }
        }

        if (request.getDescription() != null) {
            if (request.getDescription().trim().isEmpty()) {
                errors.add("Deskripsi produk tidak boleh kosong");
            } else if (request.getDescription().length() < 5 || request.getDescription().length() > 500) {
                errors.add("Deskripsi produk harus antara 5-500 karakter");
            }
        }

        if (request.getWeight() != null) {
            if (request.getWeight() <= 0) {
                errors.add("Berat produk harus lebih dari 0");
            } else if (request.getWeight() > 10000) {
                errors.add("Berat produk maksimal 10.000 gram");
            }
        }

        if (request.getPurity() != null) {
            if (request.getPurity() < 10 || request.getPurity() > 24) {
                errors.add("Kadar kemurnian harus antara 10-24 karat");
            }
        }

        if (request.getCategory() != null) {
            if (request.getCategory().trim().isEmpty()) {
                errors.add("Kategori produk tidak boleh kosong");
            } else if (!request.getCategory().matches("^(CINCIN|KALUNG|GELANG|BATANGAN)$")) {
                errors.add("Kategori harus CINCIN, KALUNG, GELANG, atau BATANGAN");
            }
        }

        if (request.getMarkup() != null) {
            if (request.getMarkup() < 0) {
                errors.add("Markup tidak boleh negatif");
            } else if (request.getMarkup() > 1000) {
                errors.add("Markup maksimal 1000%");
            }
        }

        if (request.getStock() != null && request.getStock() < 0) {
            errors.add("Stok tidak boleh negatif");
        }

        if (request.getMinStock() != null && request.getMinStock() < 0) {
            errors.add("Stok minimum tidak boleh negatif");
        }

        // Validasi minStock tidak boleh lebih besar dari stock
        if (request.getStock() != null && request.getMinStock() != null) {
            if (request.getMinStock() > request.getStock()) {
                errors.add("Stok minimum tidak boleh lebih besar dari stok");
            }
        }

        return errors;
    }

    @Operation(summary = "get All Products With Pagination", description = "Mengambil semua produk dengan pagination dan sorting")
    @GetMapping("/products")
    public ResponseEntity<?> getAllProducts(
            @Parameter(description = "Nomor Halaman (dimulai dari 0)", example = "0") @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Jumlah Item Per Halaman", example = "10") @RequestParam(defaultValue = "10") int size,

            @Parameter(description = "Field untuk sorting", example = "name") @RequestParam(defaultValue = "id") String sortBy,

            @Parameter(description = "Arah sorting (asc/desc)", example = "desc") @RequestParam(defaultValue = "desc") String sortDirection) {
        try {
            if (page < 0) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Nomor Halaman Tidak Boleh Negatif"));
            }
            if (size <= 0 || size > 100) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Ukuran halaman harus antara 1-100"));
            }
            if (!sortDirection.equalsIgnoreCase("asc") && !sortDirection.equalsIgnoreCase("desc")) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Arah sorting harus 'asc' atau 'desc'"));
            }

            Page<ProductResponseDto> productPage = productService.getAllProductsWithPaginationAndFormattedResponse(page,
                    size, sortBy, sortDirection);

            Map<String, Object> response = new HashMap<>();
            response.put("content", productPage.getContent());
            response.put("currentPage", productPage.getNumber());
            response.put("totalPages", productPage.getTotalPages());
            response.put("totalItems", productPage.getTotalElements());
            response.put("size", productPage.getSize());
            response.put("hasNext", productPage.hasNext());
            response.put("HasPrevious", productPage.hasPrevious());

            return ResponseEntity.ok(ApiResponse.success("Data product berhasil di ambil ", response));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Gagal mengambil data produk" + e.getMessage()));
        }
    }

    @Operation(summary = "Search Products with Pagination", description = "Mencari produk berdasarkan keyword dengan pagination")
    @GetMapping("/products/search")
    public ResponseEntity<?> searchProducts(
            @Parameter(description = "Keyword pencarian", example = "cincin") @RequestParam(required = false) String keyword,

            @Parameter(description = "Nomor Halaman (dimulai dari 0)", example = "0") @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Jumlah Item Per Halaman", example = "10") @RequestParam(defaultValue = "10") int size,

            @Parameter(description = "Field untuk sorting", example = "name") @RequestParam(defaultValue = "id") String sortBy,

            @Parameter(description = "Arah sorting (asc/desc)", example = "desc") @RequestParam(defaultValue = "desc") String sortDirection) {
        try {
            if (page < 0) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("sortDirection"));
            }

            if (size <= 0 || size > 100) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Ukuran halaman harus antara 1-100"));
            }

            if (!sortDirection.equalsIgnoreCase("asc") && !sortDirection.equalsIgnoreCase("desc")) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Arah sorting harus 'asc atau 'desc"));
            }

            Page<ProductResponseDto> productPage = productService.searchProductsWithPaginationAndFormattedResponse(
                    keyword, page, size, sortBy,
                    sortDirection);

            if (productPage.getContent().isEmpty()) {
                String message = keyword != null && !keyword.trim().isEmpty()
                        ? "Tidak ada produk yang ditemukan dengan keyword: '" + keyword + "'"
                        : "Tidak ada produk yang ditemukan";

                Map<String, Object> emptyResponse = new HashMap<>();
                emptyResponse.put("content", new ArrayList<>());
                emptyResponse.put("currentPage", productPage.getNumber());
                emptyResponse.put("totalPages", productPage.getTotalPages());
                emptyResponse.put("totalItems", productPage.getTotalElements());
                emptyResponse.put("size", productPage.getSize());
                emptyResponse.put("hasNext", productPage.hasNext());
                emptyResponse.put("hasPrevious", productPage.hasPrevious());
                emptyResponse.put("keyword", keyword);

                return ResponseEntity.ok(ApiResponse.success(message, emptyResponse));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("content", productPage.getContent());
            response.put("currentPage", productPage.getNumber());
            response.put("totalPages", productPage.getTotalPages());
            response.put("totalItems", productPage.getTotalElements());
            response.put("size", productPage.getSize());
            response.put("hasNext", productPage.hasNext());
            response.put("HasPrevious", productPage.hasPrevious());
            response.put("keyword", keyword);

            return ResponseEntity.ok(ApiResponse.success("Data pencarian produk berhasil di ambil", response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Gagal mencari produk : " + e.getMessage()));
        }

    }

    @Operation(summary = "Get Products with Low Stock", description = "Mendapatkan produk dengan stok rendah")
    @GetMapping("/products/low-stock")
    public ResponseEntity<?> getProductsWithLowStock() {
        try {
            List<ProductResponseDto> products = productService.getProductsWithLowStockWithFormattedResponse();
            if (products.isEmpty()) {
                return ResponseEntity
                        .ok(ApiResponse.success("Stok aman!, Tidak ada produk di bawah stok minimun", new HashMap<>()));
            }
            return ResponseEntity.ok(ApiResponse.success("Data produk stok rendah berhasil diambil", products));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Gagal mengambil data produk stok rendah: " + e.getMessage()));
        }
    }

    @Operation(summary = "Get Active Products", description = "Mendapatkan produk aktif saja")
    @GetMapping("/products/active")
    public ResponseEntity<?> getActiveProducts(
            @Parameter(description = "Nomor halaman", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Jumlah item per halaman", example = "10") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Field untuk sorting", example = "name") @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Arah sorting (asc/desc)", example = "desc") @RequestParam(defaultValue = "desc") String sortDirection) {

        try {
            Page<ProductResponseDto> productPage = productService
                    .getAllActiveProductsWithPaginationAndFormattedResponse(page, size, sortBy,
                            sortDirection);

            Map<String, Object> response = new HashMap<>();
            response.put("content", productPage.getContent());
            response.put("currentPage", productPage.getNumber());
            response.put("totalPages", productPage.getTotalPages());
            response.put("totalItems", productPage.getTotalElements());
            response.put("size", productPage.getSize());
            response.put("hasNext", productPage.hasNext());
            response.put("hasPrevious", productPage.hasPrevious());

            return ResponseEntity.ok(ApiResponse.success("Data produk aktif berhasil diambil", response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Gagal mengambil data produk aktif: " + e.getMessage()));
        }
    }

    @Operation(summary = "Delete Product", description = "Menghapus produk berdasarkan ID")
    @DeleteMapping("/product/{productId}")
    public ResponseEntity<?> deleteProduct(
            @Parameter(description = "ID produk yang akan di hapus", required = true) @PathVariable Long productId) {
        try {
            if (!productService.existsById(productId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Produk tidak di temukan dengan ID: " + productId));
            }

            productService.deleteProduct(productId);
            return ResponseEntity.ok(ApiResponse.success("Product berhasil di hapus", null));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Gagal menghapus produk: " + e.getMessage()));
        }
    }

    @Operation(summary = "Delete Multiple Products", description = "Menghapus beberapa produk berdasarkan array ID")
    @DeleteMapping("/products/batch")
    public ResponseEntity<?> deleteMultipleProducts(
            @Parameter(description = "Array ID produk yang akan di hapus", required = true) @RequestBody Long[] productIds) {
        try {

            System.out.println("Ini request saya : " + productIds);
            // Validasi input
            if (productIds == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Array ID produk tidak boleh null"));
            }

            if (productIds.length == 0) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Array ID produk tidak boleh kosong"));
            }
            List<String> deletedIds = new ArrayList<>();
            List<String> notFoundIds = new ArrayList<>();

            for (Long productId : productIds) {
                try {
                    if (productService.existsById(productId)) {
                        productService.deleteProduct(productId);
                        deletedIds.add(productId.toString());
                    } else {
                        notFoundIds.add(productId.toString());
                    }
                } catch (Exception e) {
                    notFoundIds.add(productId.toString());
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("deletedIds", deletedIds);
            result.put("notFoundIds", notFoundIds);
            result.put("totalDeleted", deletedIds.size());
            result.put("totalNotFound", notFoundIds.size());

            String message = String.format("Berhasil menghapus %d produk, %d tidak ditemukan", deletedIds.size(),
                    notFoundIds.size());
            return ResponseEntity.ok(ApiResponse.success(message, result));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Gagal menghapus produk: " + e.getMessage()));
        }
    }

    @Operation(summary = "Update Harga Semua Produk", description = "Mengupdate harga semua produk aktif berdasarkan harga emas terbaru")
    @PostMapping("/update-all")
    public ResponseEntity<?> updateAllProductPrices() {
        try {
            Map<String, Object> result = productService.updateAllProductPrices();

            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(ApiResponse.success(
                        (String) result.get("message"),
                        result));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ApiResponse.error((String) result.get("message")));
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Gagal mengupdate harga produk: " + e.getMessage()));
        }
    }

    @Operation(summary = "Get Product Statistics for Filters", description = "Mendapatkan statistik produk untuk filter katalog")
    @GetMapping("/products/filter-stats")
    public ResponseEntity<?> getProductFilterStats() {
        try {
            Map<String, Object> stats = productService.getProductFilterStats();
            return ResponseEntity.ok(ApiResponse.success("Statistik filter berhasil diambil", stats));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Gagal mengambil statistik filter: " + e.getMessage()));
        }
    }


}
