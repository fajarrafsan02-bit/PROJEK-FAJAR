# TokWeb - Sistem Toko Emas Online

Aplikasi e-commerce lengkap untuk toko perhiasan emas dengan sistem manajemen harga emas real-time, keranjang belanja, checkout, dan dashboard admin.

## 📋 Deskripsi

TokWeb adalah platform toko emas online yang menyediakan:
- *Katalog Produk*: Tampilan produk perhiasan emas yang menarik
- *Manajemen Harga Emas*: Sistem tracking harga emas 24K, 22K, dan 18K secara real-time
- *Keranjang Belanja*: Sistem cart yang terintegrasi dengan stok produk
- *Checkout & Pembayaran*: Integrasi dengan gateway pembayaran (Xendit)
- *Order Management*: Sistem pesanan lengkap dengan tracking status
- *Dashboard Admin*: Panel admin untuk mengelola produk, pesanan, dan harga emas
- *Authentication*: Sistem login/register dengan JWT dan email verification

## 🚀 Teknologi yang Digunakan

### Backend (Spring Boot)
- *Java 21*
- *Spring Boot 3.5.0*
- *Spring Security* - Authentication & Authorization
- *Spring Data JPA* - Database ORM
- *Thymeleaf* - Template Engine
- *MySQL* - Database utama
- *H2* - Database untuk testing
- *JWT (JSON Web Token)* - Token authentication
- *Spring Mail* - Email service
- *Quartz Scheduler* - Job scheduling
- *Jackson* - JSON processing
- *Lombok* - Code reduction
- *Swagger/OpenAPI* - API documentation
- *Spring WebFlux* - Reactive web framework
- *RabbitMQ/AMQP* - Message queue

### Frontend & Client-side
- *Thymeleaf Templates*
- *JavaScript/ES6+*
- *Tailwind CSS* - Utility-first CSS framework
- *HTML5 & CSS3*
- *Responsive Design*

### Node.js Components (Gold Price Manager)
- *Express.js* - Web framework
- *Axios* - HTTP client
- *CORS* - Cross-origin resource sharing
- *Nodemon* - Development tool

### Database & Storage
- *MySQL 8.0+* - Primary database
- *Cloudinary* - Image storage service
- *JPA/Hibernate* - ORM mapping

### Payment & External Services
- *Xendit* - Payment gateway integration
- *SMTP Gmail* - Email service
- *External Gold Price API* - Real-time price updates

## 🛠 Prerequisites

Pastikan sistem Anda memiliki:

- *Java 21* atau lebih tinggi
- *Maven 3.6+*
- *Node.js 16+* dan *npm*
- *MySQL 8.0+*
- *Git*

## 📦 Instalasi dan Setup

### 1. Clone Repository
bash
git clone <repository-url>
cd tokweb


### 2. Setup Database MySQL
sql
CREATE DATABASE toko_emas;
-- Database akan dibuat otomatis dengan konfigurasi yang ada


### 3. Konfigurasi Environment

#### Backend Configuration
Edit file [src/main/resources/application.properties](src/main/resources/application.properties):

properties
# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/toko_emas?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true
spring.datasource.username=your_mysql_username
spring.datasource.password=your_mysql_password

# Email Configuration
spring.mail.username=your_email@gmail.com
spring.mail.password=your_app_password

# Xendit Configuration (Optional)
app.payment.xendit.secret-key=your_xendit_secret_key
app.payment.xendit.callback-token=your_xendit_callback_token


#### Cloudinary Configuration (Optional)
Tambahkan konfigurasi Cloudinary untuk upload gambar produk.

### 4. Install Dependencies

#### Backend Dependencies
bash
mvn clean install


#### Frontend/Node.js Dependencies
bash
npm install


### 5. Database Schema Setup
Database schema akan dibuat otomatis oleh Hibernate, atau jalankan:
bash
mysql -u root -p toko_emas < database-schema.sql


## 🚀 Cara Menjalankan Aplikasi

### Development Mode

#### 1. Jalankan Spring Boot Application
bash
# Menggunakan Maven
mvn spring-boot:run

# Atau menggunakan Maven wrapper
./mvnw spring-boot:run

# Atau compile dan run JAR
mvn clean package
java -jar target/tokweb-0.0.1-SNAPSHOT.jar


#### 2. Jalankan Gold Price Manager (Node.js)
bash
npm run dev
# atau
npm start


### Production Mode

#### Build Application
bash
mvn clean package -DskipTests


#### Run Production
bash
java -jar target/tokweb-0.0.1-SNAPSHOT.jar


## 🌐 Akses Aplikasi

### Main Application
- *URL*: http://localhost:8080
- *Admin Panel*: http://localhost:8080/admin
- *API Documentation*: http://localhost:8080/swagger-ui/index.html

### Gold Price Manager
- *URL*: http://localhost:3000
- *API*: http://localhost:3000/api/prices

## 📚 Struktur Proyek


tokweb/
├── src/main/java/com/projek/tokweb/
│   ├── TokwebApplication.java           # Main application class
│   ├── config/                         # Configuration classes
│   ├── controller/                     # REST & Web controllers
│   │   ├── admin/                      # Admin controllers
│   │   ├── user/                       # User controllers
│   │   ├── authentication/             # Auth controllers
│   │   └── publics/                    # Public API controllers
│   ├── models/                         # JPA entities
│   │   ├── admin/                      # Product, Revenue models
│   │   ├── customer/                   # Order, Cart, Payment models
│   │   └── goldPrice/                  # Gold price models
│   ├── repository/                     # Data repositories
│   ├── service/                        # Business logic services
│   ├── dto/                            # Data Transfer Objects
│   ├── enums/                          # Enum classes
│   └── security/                       # Security configurations
├── src/main/resources/
│   ├── application.properties          # Main configuration
│   ├── static/                         # Static assets (CSS, JS)
│   └── templates/                      # Thymeleaf templates
│       └── html/
│           ├── admin/                  # Admin pages
│           ├── user/                   # User pages
│           └── authentication/         # Auth pages
├── public/                             # Node.js static files
├── css/                                # Additional stylesheets
├── js/                                 # Additional JavaScript
├── uploads/                            # File uploads directory
├── package.json                        # Node.js dependencies
├── pom.xml                            # Maven dependencies
└── database-schema.sql                 # Database schema


## 🔧 API Endpoints

### Authentication
- POST /api/auth/login - Login user
- POST /api/auth/register - Register new user
- POST /api/auth/forgot-password - Reset password

### User Endpoints
- GET /user/home - User dashboard
- GET /user/katalog - Product catalog
- GET /user/cart - Shopping cart
- POST /user/checkout - Checkout process
- GET /user/my-orders - Order history

### Admin Endpoints
- GET /admin/dashboard - Admin dashboard
- GET /admin/products - Product management
- GET /admin/orders - Order management
- POST /admin/gold-prices - Update gold prices

### Public API
- GET /api/products - Get all products
- GET /api/gold-prices - Current gold prices
- GET /api/best-selling - Best selling products

### Gold Price API (Node.js - Port 3000)
- GET /api/prices - Current prices
- POST /api/prices - Update prices
- GET /api/history - Price history
- GET /api/changes - Price changes

## 🧪 Testing

### Run Tests
bash
mvn test


### Test Gold Price System
bash
npm test
node debug-price-changes.js


## 📊 Fitur Utama

### 🛒 E-Commerce Features
- *Product Catalog*: Tampilan produk dengan filter dan pencarian
- *Shopping Cart*: Keranjang belanja dengan persistent data
- *Checkout Process*: Proses pembelian yang aman dan mudah
- *Order Tracking*: Tracking status pesanan real-time
- *Wishlist*: Daftar produk favorit
- *User Account*: Profil pengguna dan riwayat pembelian

### 💰 Gold Price Management
- *Real-time Updates*: Harga emas 24K, 22K, 18K yang update otomatis
- *Price History*: Riwayat perubahan harga
- *Karat Calculator*: Perhitungan harga berdasarkan karat
- *External API Integration*: Integrasi dengan API harga emas eksternal

### 🔐 Security Features
- *JWT Authentication*: Token-based authentication
- *Password Encryption*: Enkripsi password dengan bcrypt
- *Email Verification*: Verifikasi email untuk akun baru
- *Role-based Access*: Pembatasan akses berdasarkan role
- *CSRF Protection*: Perlindungan dari CSRF attacks

### 💳 Payment Integration
- *Multiple Payment Methods*: Bank Transfer, E-Wallet, Credit Card
- *Xendit Gateway*: Integrasi dengan payment gateway
- *Payment Verification*: Verifikasi bukti pembayaran
- *Auto Status Update*: Update status otomatis setelah pembayaran

## 🔒 Keamanan

- *Spring Security* untuk authentication dan authorization
- *JWT tokens* untuk stateless authentication
- *Password hashing* dengan bcrypt
- *Input validation* dan *SQL injection protection*
- *CORS configuration* untuk API security
- *HTTPS ready* untuk production deployment

## 📈 Monitoring & Logging

- *Activity Logging*: Log semua aktivitas pengguna
- *Error Handling*: Comprehensive error handling
- *Health Checks*: Application health monitoring
- *Performance Tracking*: Response time monitoring

## 🚀 Deployment

### Docker (Recommended)
bash
# Build Docker image
docker build -t tokweb .

# Run with Docker Compose
docker-compose up -d


### Traditional Deployment
1. Build application: mvn clean package
2. Upload JAR file to server
3. Configure database connection
4. Run: java -jar tokweb-0.0.1-SNAPSHOT.jar

## 🤝 Contributing

1. Fork repository
2. Create feature branch (git checkout -b feature/AmazingFeature)
3. Commit changes (git commit -m 'Add some AmazingFeature')
4. Push to branch (git push origin feature/AmazingFeature)
5. Create Pull Request

## 📄 License

Distributed under the MIT License. See LICENSE file for more information.

## 📞 Contact & Support

- *Email*: support@tokweb.com
- *Documentation*: [Wiki](https://github.com/yourusername/tokweb/wiki)
- *Issues*: [GitHub Issues](https://github.com/yourusername/tokweb/issues)

## 🙏 Acknowledgments

- Spring Boot team untuk framework yang luar biasa
- Thymeleaf untuk template engine yang powerful
- Tailwind CSS untuk utility-first styling
- MySQL untuk database yang reliable
- Semua contributor dan tester yang telah membantu

---

### 📝 Catatan Tambahan

- Pastikan port 8080 (Spring Boot) dan 3000 (Node.js) tidak digunakan aplikasi lain
- Untuk production, ubah konfigurasi database dan security settings
- Backup database secara berkala untuk data safety
- Monitor aplikasi menggunakan tools seperti Actuator endpoints

*Happy coding! 🚀*
