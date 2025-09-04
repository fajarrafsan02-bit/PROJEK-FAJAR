# Fix Active Customer Count - Exclude Admin Users

## Masalah Yang Diperbaiki
Dashboard menampilkan jumlah customer aktif yang termasuk user dengan role ADMIN, padahal yang dibutuhkan hanya customer dengan role USER saja.

## Perbaikan Yang Dilakukan

### 1. Update AdminDashboardController
**File:** `src/main/java/com/projek/tokweb/controller/admin/AdminDashboardController.java`

**Perubahan:**
- Menggunakan `Role.ADMIN` sebagai enum instead of string "ADMIN"
- Menambahkan import untuk `Role` enum
- Menghitung user dengan filter role

**Sebelum:**
```java
long activeCustomersCount = userRepository.countByWaktuBuatAfter(thirtyDaysAgo);
if (activeCustomersCount == 0) {
    activeCustomersCount = userRepository.count();
}
```

**Sesudah:**
```java
// Count only users with role USER (exclude ADMIN) created in last 30 days
long activeCustomersCount = userRepository.countByWaktuBuatAfterAndRoleNot(thirtyDaysAgo, Role.ADMIN);

// Jika tidak ada user baru dalam 30 hari, ambil total user kecuali admin
if (activeCustomersCount == 0) {
    activeCustomersCount = userRepository.countByRoleNot(Role.ADMIN);
}
```

### 2. Update UserRepository
**File:** `src/main/java/com/projek/tokweb/repository/UserRespository.java`

**Metode baru yang ditambahkan:**
```java
// Method untuk menghitung user yang dibuat setelah tanggal tertentu, kecuali role tertentu
long countByWaktuBuatAfterAndRoleNot(LocalDateTime waktuBuat, Role role);

// Method untuk menghitung user kecuali role tertentu
long countByRoleNot(Role role);
```

**Import yang ditambahkan:**
```java
import com.projek.tokweb.models.Role;
```

## Cara Kerja Setelah Perbaikan

### Skenario 1: Ada User Baru dalam 30 Hari Terakhir
```java
userRepository.countByWaktuBuatAfterAndRoleNot(thirtyDaysAgo, Role.ADMIN)
```
- Menghitung user yang:
  - Dibuat dalam 30 hari terakhir (`waktuBuat > thirtyDaysAgo`)
  - Role bukan ADMIN (`role != Role.ADMIN`)

### Skenario 2: Tidak Ada User Baru dalam 30 Hari
```java
userRepository.countByRoleNot(Role.ADMIN)
```
- Menghitung semua user yang role-nya bukan ADMIN
- Fallback untuk memastikan ada angka yang ditampilkan

## Contoh Data

**Sebelum perbaikan:**
- Total users: 5 (4 customer + 1 admin)
- Dashboard menampilkan: 5 customer aktif ❌

**Setelah perbaikan:**
- Total users: 5 (4 customer + 1 admin)
- Users kecuali admin: 4 customer
- Dashboard menampilkan: 4 customer aktif ✅

## Database Query yang Dihasilkan

### Query untuk user 30 hari terakhir (kecuali admin):
```sql
SELECT COUNT(*) 
FROM users 
WHERE waktu_buat > '2025-08-05 00:30:00' 
AND role != 'ADMIN'
```

### Query fallback (semua user kecuali admin):
```sql
SELECT COUNT(*) 
FROM users 
WHERE role != 'ADMIN'
```

## API Response

### Endpoint: `/admin/api/dashboard/customers/active-count`

**Response Example:**
```json
{
  "success": true,
  "data": {
    "activeCustomersCount": 4,
    "calculatedAt": "2025-09-05T00:35:00",
    "periodDays": 30
  },
  "message": "Jumlah customer aktif berhasil diambil"
}
```

## Console Logging

Dashboard akan menampilkan log yang lebih akurat:
```
👥 Getting active customers count
✅ Active customers count: 4
📊 Updating dashboard stats with real data...
👥 Active Customers updated: 4
```

## Keuntungan Perbaikan

1. **Data Akurat**: Dashboard menampilkan jumlah customer yang sebenarnya
2. **Exclude Admin**: Admin tidak dihitung sebagai customer
3. **Flexible**: Bisa menambahkan role lain di masa depan
4. **Consistent**: Menggunakan enum Role yang sudah ada
5. **Fallback Safe**: Tetap menampilkan data meski tidak ada user baru

## Testing

Untuk memverifikasi perbaikan:

1. Login sebagai admin
2. Buka dashboard admin
3. Periksa card "Customer Aktif"
4. Pastikan angka yang ditampilkan tidak termasuk admin
5. Periksa console log untuk konfirmasi data

**Expected Result:** Card "Customer Aktif" menampilkan jumlah user dengan role USER saja, tidak termasuk admin.# Fix Active Customer Count - Exclude Admin Users

## Masalah Yang Diperbaiki
Dashboard menampilkan jumlah customer aktif yang termasuk user dengan role ADMIN, padahal yang dibutuhkan hanya customer dengan role USER saja.

## Perbaikan Yang Dilakukan

### 1. Update AdminDashboardController
**File:** `src/main/java/com/projek/tokweb/controller/admin/AdminDashboardController.java`

**Perubahan:**
- Menggunakan `Role.ADMIN` sebagai enum instead of string "ADMIN"
- Menambahkan import untuk `Role` enum
- Menghitung user dengan filter role

**Sebelum:**
```java
long activeCustomersCount = userRepository.countByWaktuBuatAfter(thirtyDaysAgo);
if (activeCustomersCount == 0) {
    activeCustomersCount = userRepository.count();
}
```

**Sesudah:**
```java
// Count only users with role USER (exclude ADMIN) created in last 30 days
long activeCustomersCount = userRepository.countByWaktuBuatAfterAndRoleNot(thirtyDaysAgo, Role.ADMIN);

// Jika tidak ada user baru dalam 30 hari, ambil total user kecuali admin
if (activeCustomersCount == 0) {
    activeCustomersCount = userRepository.countByRoleNot(Role.ADMIN);
}
```

### 2. Update UserRepository
**File:** `src/main/java/com/projek/tokweb/repository/UserRespository.java`

**Metode baru yang ditambahkan:**
```java
// Method untuk menghitung user yang dibuat setelah tanggal tertentu, kecuali role tertentu
long countByWaktuBuatAfterAndRoleNot(LocalDateTime waktuBuat, Role role);

// Method untuk menghitung user kecuali role tertentu
long countByRoleNot(Role role);
```

**Import yang ditambahkan:**
```java
import com.projek.tokweb.models.Role;
```

## Cara Kerja Setelah Perbaikan

### Skenario 1: Ada User Baru dalam 30 Hari Terakhir
```java
userRepository.countByWaktuBuatAfterAndRoleNot(thirtyDaysAgo, Role.ADMIN)
```
- Menghitung user yang:
  - Dibuat dalam 30 hari terakhir (`waktuBuat > thirtyDaysAgo`)
  - Role bukan ADMIN (`role != Role.ADMIN`)

### Skenario 2: Tidak Ada User Baru dalam 30 Hari
```java
userRepository.countByRoleNot(Role.ADMIN)
```
- Menghitung semua user yang role-nya bukan ADMIN
- Fallback untuk memastikan ada angka yang ditampilkan

## Contoh Data

**Sebelum perbaikan:**
- Total users: 5 (4 customer + 1 admin)
- Dashboard menampilkan: 5 customer aktif ❌

**Setelah perbaikan:**
- Total users: 5 (4 customer + 1 admin)
- Users kecuali admin: 4 customer
- Dashboard menampilkan: 4 customer aktif ✅

## Database Query yang Dihasilkan

### Query untuk user 30 hari terakhir (kecuali admin):
```sql
SELECT COUNT(*) 
FROM users 
WHERE waktu_buat > '2025-08-05 00:30:00' 
AND role != 'ADMIN'
```

### Query fallback (semua user kecuali admin):
```sql
SELECT COUNT(*) 
FROM users 
WHERE role != 'ADMIN'
```

## API Response

### Endpoint: `/admin/api/dashboard/customers/active-count`

**Response Example:**
```json
{
  "success": true,
  "data": {
    "activeCustomersCount": 4,
    "calculatedAt": "2025-09-05T00:35:00",
    "periodDays": 30
  },
  "message": "Jumlah customer aktif berhasil diambil"
}
```

## Console Logging

Dashboard akan menampilkan log yang lebih akurat:
```
👥 Getting active customers count
✅ Active customers count: 4
📊 Updating dashboard stats with real data...
👥 Active Customers updated: 4
```

## Keuntungan Perbaikan

1. **Data Akurat**: Dashboard menampilkan jumlah customer yang sebenarnya
2. **Exclude Admin**: Admin tidak dihitung sebagai customer
3. **Flexible**: Bisa menambahkan role lain di masa depan
4. **Consistent**: Menggunakan enum Role yang sudah ada
5. **Fallback Safe**: Tetap menampilkan data meski tidak ada user baru

## Testing

Untuk memverifikasi perbaikan:

1. Login sebagai admin
2. Buka dashboard admin
3. Periksa card "Customer Aktif"
4. Pastikan angka yang ditampilkan tidak termasuk admin
5. Periksa console log untuk konfirmasi data

**Expected Result:** Card "Customer Aktif" menampilkan jumlah user dengan role USER saja, tidak termasuk admin.