# Zenora Aplikasi Manajemen Keuangan Pribadi

## Deskripsi Aplikasi

Zenora adalah aplikasi manajemen keuangan pribadi berbasis desktop yang dirancang untuk membantu pengguna merencanakan, memantau, dan mencapai tujuan finansial mereka. Dibangun dengan **JavaFX 21** sebagai antarmuka, **Spring Boot 3** sebagai backend REST API, **H2 Database** untuk penyimpanan data, serta **JPA/Hibernate** dan **Spring Security** untuk pengelolaan data dan keamanan.

---

## Fitur Utama

### Autentikasi & Profil
- Registrasi dan login pengguna dengan Spring Security (HTTP Basic Auth)
- Setiap data terisolasi per pengguna (ownership-based access control)
- Pengaturan profil keuangan: pemasukan bulanan, pengeluaran, dan kapasitas tabungan

### Goal Planner
- Buat goal tabungan dengan target nominal, jangka waktu, dan bunga
- Kategori goal: Umum, Pendidikan, Liburan, Properti, Kendaraan, dan lainnya
- Pilih tempat penyimpanan: Bank, E-Wallet, Investasi, atau Tunai
- Sistem prioritas goal (1–5) untuk alokasi dana yang cerdas

### Kontribusi & Progress
- Catat setoran/kontribusi ke setiap goal secara berkala
- Pantau progres tabungan secara real-time dengan persentase pencapaian
- Riwayat lengkap kontribusi per goal

### Dana Darurat
- Hitung kebutuhan dana darurat berdasarkan pengeluaran bulanan dan jumlah bulan cakupan
- Rekomendasi otomatis sesuai profil keuangan pengguna

### Perencanaan Pensiun
- Kalkulasi dana pensiun dengan memperhitungkan inflasi dan return investasi
- Proyeksi kebutuhan bulanan di masa pensiun

### Debt Planner
- Catat dan kelola utang dengan detail: nama, saldo, APR, dan minimum pembayaran
- Hitung strategi pelunasan optimal

### Laporan & Analitik
- Dashboard ringkasan kondisi keuangan secara keseluruhan
- Laporan progres semua goal dalam satu tampilan
- Fitur What-If Analysis: simulasi skenario keuangan berbeda

### Export Data
- Export data keuangan ke format CSV

---

## Arsitektur & Teknologi

| Komponen | Teknologi |
|---|---|
| Antarmuka (GUI) | JavaFX 21 + FXML |
| Backend / REST API | Spring Boot 3.2.5 |
| Database | H2 (file-based, persistent) |
| ORM | JPA / Hibernate |
| Keamanan | Spring Security |
| Validasi | Spring Validation (Jakarta Bean Validation) |
| Build Tool | Apache Maven |
| Bahasa | Java 21 |



## Penerapan 4 Pilar PBO

### Encapsulation
Semua atribut di class model dan entity bersifat `private`, hanya dapat diakses melalui getter dan setter. Contoh: `GoalEntity`, `UserProfile`, `Debt`.

### Inheritance
- `GoalEntity` extends `BaseEntity` — mewarisi `id`, `createdAt`, `updatedAt`, dan audit lifecycle
- `SavingsGoal`, `EmergencyGoal`, `RetirementGoal` masing-masing extends `Goal`
- `StandardFinancialEngine` extends abstract class `FinancialEngine`
- Semua JavaFX controller mewarisi `BaseController` atau `BaseModuleController`

### Polymorphism
- Interface `Calculable` diimplementasikan secara berbeda oleh `SavingsGoal`, `EmergencyGoal`, dan `RetirementGoal` — masing-masing punya rumus kalkulasi yang unik
- Interface `RecommendationStrategy` diimplementasikan oleh `StandardRecommendation` dan `AggressiveRecommendation`

### Abstraction
- Abstract class `FinancialEngine` mendefinisikan kontrak kalkulasi keuangan tanpa mengekspos detail matematika ke controller
- Interface `Calculable` dan `RecommendationStrategy` menyembunyikan detail implementasi dari layer atas

---

## Cara Menjalankan

### Prasyarat
- Java 21 atau lebih baru
- Apache Maven 3.8+

### Langkah Instalasi

```bash
# 1. Clone repositori
git clone https://github.com/Abidalfakhri/UASPBO_Zenora__KelompokTujuh.git
cd UAS_PBO_Zenora_KelompokTujuh

# 2. Jalankan aplikasi (mode development)
mvn clean javafx:run
```

Aplikasi akan otomatis membuat database H2 di folder `./data/zenora_db.mv.db`.

### Akses Tambahan (Development)

| Layanan | URL |
|---|---|
| REST API | `http://localhost:8080` |
| H2 Console | `http://localhost:8080/h2-console` |

### Akun Default (Mode Development)

| Username | Password | Role |
|---|---|---|
| admin | admin123 | ROLE_ADMIN |
| user | user123 | ROLE_USER |

> Akun default hanya tersedia saat profil aktif bukan `prod`.

### Mode Production

```bash
java -Dspring.profiles.active=prod \
     -DDATABASE_URL=jdbc:h2:file:./data/zenora_db \
     -DPORT=8080 \
     -jar target/zenora-1.0.0.jar
```
