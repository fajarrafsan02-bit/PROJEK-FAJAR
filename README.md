# Gold Price Manager

Sistem manajemen harga emas dengan tracking perubahan real-time dan perhitungan rasio karat yang akurat.

## 🚀 Fitur

- **Manajemen Harga Real-time**: Update harga emas 24K, 22K, dan 18K
- **Perhitungan Rasio Otomatis**: Sistem menghitung otomatis rasio karat yang benar
- **Tracking Perubahan**: Mencatat semua perubahan harga dengan detail
- **API Eksternal**: Integrasi dengan API eksternal untuk update harga
- **Interface Modern**: UI yang responsif dan user-friendly
- **Riwayat Harga**: Menyimpan dan menampilkan riwayat perubahan harga

## 📊 Rasio Karat

Sistem menggunakan rasio karat yang standar:
- **24K**: 100% (harga dasar)
- **22K**: 91.67% (22/24)
- **18K**: 75% (18/24)

## 🛠️ Instalasi

1. **Clone repository**
```bash
git clone <repository-url>
cd gold-price-manager
```

2. **Install dependencies**
```bash
npm install
```

3. **Jalankan aplikasi**
```bash
npm start
```

4. **Buka browser**
```
http://localhost:3000
```

## 📁 Struktur Proyek

```
gold-price-manager/
├── public/
│   ├── index.html          # Interface utama
│   └── app.js             # Frontend JavaScript
├── index.js               # Backend server
├── fixed_updateAllPrices.js # Fungsi perbaikan rasio karat
├── package.json           # Dependencies
└── README.md             # Dokumentasi
```

## 🔧 API Endpoints

### GET `/api/prices`
Mendapatkan harga emas saat ini
```json
{
  "success": true,
  "data": {
    "24k": 2500000,
    "22k": 2291750,
    "18k": 1875000
  },
  "timestamp": "2024-01-01T00:00:00.000Z"
}
```

### POST `/api/prices`
Update harga emas
```json
{
  "harga24k": 2600000,
  "source": "MANUAL"
}
```

### GET `/api/changes`
Mendapatkan riwayat perubahan harga
```json
{
  "success": true,
  "data": [
    {
      "id": 1704067200000,
      "purity": "24k",
      "oldPrice": 2500000,
      "newPrice": 2600000,
      "changeAmount": 100000,
      "changePercent": "4.00",
      "changeType": "INCREASE",
      "changeDate": "2024-01-01T00:00:00.000Z",
      "changeSource": "MANUAL",
      "notes": "Updated via MANUAL"
    }
  ]
}
```

### GET `/api/history`
Mendapatkan riwayat harga
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1704067200000,
        "tanggalAmbil": "2024-01-01T00:00:00.000Z",
        "hargaJual24k": 2600000,
        "hargaJual22k": 2383420,
        "hargaJual18k": 1950000,
        "goldPriceEnum": "MANUAL"
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "currentPage": 0
  }
}
```

### GET `/api/external`
Simulasi API eksternal
```json
{
  "success": true,
  "data": {
    "harga24k": 2550000,
    "harga22k": 2338425,
    "harga18k": 1912500,
    "source": "EXTERNAL_API",
    "timestamp": "2024-01-01T00:00:00.000Z"
  }
}
```

## 🧪 Testing

### Test Rasio Karat
```bash
node test-karat-ratio.js
```

### Test API Eksternal
```bash
node test-external-api-update.js
```

### Debug Perubahan Harga
```bash
node debug-price-changes.js
```

## 🔧 Penggunaan FixedPriceUpdater

```javascript
const FixedPriceUpdater = require('./fixed_updateAllPrices.js');
const updater = new FixedPriceUpdater();

// Update dari harga 24K
const prices = updater.updateFrom24K(2500000);
console.log(prices);
// Output:
// {
//   '24k': 2500000,
//   '22k': 2291750,
//   '18k': 1875000,
//   ratios: { '22k': '91.67%', '18k': '75.00%' }
// }

// Validasi rasio
const validation = updater.validateRatios(prices);
console.log(validation.valid); // true
```

## 🐛 Perbaikan yang Dilakukan

1. **Perhitungan Rasio Karat**: Memperbaiki perhitungan rasio 22K dan 18K
2. **API Endpoints**: Menambahkan endpoint yang lengkap
3. **Error Handling**: Menambahkan error handling yang lebih baik
4. **Data Validation**: Validasi input dan output
5. **UI/UX**: Interface yang lebih modern dan responsif

## 📝 Scripts

- `npm start`: Jalankan aplikasi
- `npm run dev`: Jalankan dengan nodemon (development)
- `npm test`: Jalankan test
- `npm run debug`: Jalankan debug mode

## 🤝 Kontribusi

1. Fork repository
2. Buat branch fitur (`git checkout -b feature/AmazingFeature`)
3. Commit perubahan (`git commit -m 'Add some AmazingFeature'`)
4. Push ke branch (`git push origin feature/AmazingFeature`)
5. Buat Pull Request

## 📄 Lisensi

Distributed under the MIT License. See `LICENSE` for more information.

## 📞 Kontak

- Email: your.email@example.com
- Project Link: [https://github.com/yourusername/gold-price-manager](https://github.com/yourusername/gold-price-manager)