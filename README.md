# RouteLab: Cycling Route Intelligence Platform

> **Tagline:** Analyze the Ride Before You Ride.  
> **Workflow:** ROUTE → ANALYZE → UNDERSTAND → PLAN → RIDE

RouteLab adalah platform cerdas untuk menganalisis dan memahami rute sepeda sebelum pengguna melakukan perjalanan. RouteLab mengubah data mentah koordinat dan elevasi peta menjadi wawasan perjalanan (*route intelligence*) yang actionable dan mudah dipahami oleh pesepeda.

---

## Fitur Utama (MVP)

1. **Peta Lintasan Interaktif (Route Map)**
   - Visualisasi lintasan polyline GPS 2D dengan pewarnaan segmen gradien (Hijau: Datar, Kuning: Rolling, Merah: Tanjakan terjal).
   - Penanda Titik Start, Garis Finish, dan Puncak Tanjakan (*Summit*).
   - Sinkronisasi kursor crosshair dengan profil elevasi secara *real-time*.

2. **Profil Elevasi Interaktif (Elevation Profile)**
   - Grafik elevasi interaktif dengan kontrol sentuh/geser (*scrubbing*).
   - Menampilkan KM posisi, ketinggian absolut (mdpl), dan persentase gradien seketika.
   - Penyorotan sektor tanjakan terdeteksi langsung di atas grafik.

3. **Deteksi & Kategorisasi Tanjakan (Climb Analyzer)**
   - Algoritma deteksi tanjakan berkelanjutan berbasis elevasi dan jarak.
   - Kategorisasi objektif standar UCI (Kategori 4, 3, 2, 1, dan Hors Catégorie / HC).
   - Statistik per tanjakan: KM awal-akhir, panjang (km), elevasi (+m), gradien rata-rata, dan gradien maksimum.

4. **Segmentasi Karakteristik Rute (Route Segmentation)**
   - Membagi rute ke dalam segmen-segmen logis berdasarkan transisi kontur medan (Datar, Rolling, Tanjakan, Turunan).
   - Rekomendasi manajemen tenaga per segmen (misal: Zone 2 Pacing, Hemat Tenaga, Pemulihan).

5. **Sistem Penilaian Tingkat Kesulitan Objektif (Difficulty Rating)**
   - Skor transparan (10-100) dan klasifikasi tingkat kesulitan: *Mudah*, *Moderat*, *Menantang*, *Sangat Menantang*.
   - Rationale deskriptif yang menjelaskan faktor utama pemicu kesulitan rute.

6. **Perencanaan Target Waktu & Pacing (COT Strategy)**
   - Slider interaktif untuk menyesuaikan target waktu tempuh total (*Cut-Off Time*).
   - Perhitungan pace realistis per sektor berdasarkan kontur dan alokasi waktu istirahat (*stop allowance*).

7. **Rencana Nutrisi & Hidrasi (Fueling & Rest Strategy)**
   - Garis waktu pengingat hidrasi berkala (setiap 30-40 km).
   - Rekomendasi asupan karbohidrat strategis 15 menit sebelum memasuki tanjakan berat.

8. **Asisten Rute Cerdas (AI Route Assistant)**
   - Tanya jawab kontekstual yang membaca data rute aktif.
   - Menjawab pertanyaan seperti tanjakan terberat, sektor hemat tenaga, perbandingan paruh rute, dan dampak istirahat terhadap target waktu.

9. **Dukungan Berkas GPX & Rute Bawaan**
   - Impor rute GPX eksternal melalui pemilih berkas (*file picker*).
   - Tersedia 3 rute sampel realistis: *Sentul Rainbow Hills : KM 0*, *Puncak Pass Epic Challenge*, dan *PIK 2 Coastal Fast Paceline*.

---

## Arsitektur & Teknologi

- **Bahasa:** Kotlin 1.9.24
- **UI Framework:** Jetpack Compose + Material 3 (BOM 2024.06.00)
- **Tema Desain:** Dark Mode (OLED) berspesifikasi sports/GPS outdoor sesuai panduan [MASTER.md](file:///design-system/routelab/MASTER.md)
- **Build System:** Android Gradle Plugin (AGP) 8.5.2 + Gradle 8.7 (KTS)
- **Kompilasi:** Java 17, compileSdk 34, minSdk 24

---

## Otomasi CI/CD (GitHub Actions)

Repositori ini dilengkapi dengan pipeline otomatis pada `.github/workflows/build-release-apk.yml`:

```text
Push / PR ke main
       ↓
GitHub Actions (Ubuntu Runner)
       ↓
Setup JDK 17 & Android SDK
       ↓
Run Lint (./gradlew lintDebug)
       ↓
Run Unit Tests (./gradlew testDebugUnitTest)
       ↓
Build Debug APK (./gradlew assembleDebug)
       ↓
Upload Artifact (RouteLab-debug.apk)
       ↓
(Jika Secret Keystore Tersedia)
Build Signed Release APK (./gradlew assembleRelease)
       ↓
Upload Artifact & Buat GitHub Release (pada tag v*)
```

### Menjalankan Perintah Build & Tes Lokal

```bash
# Menjalankan validasi lint
./gradlew lintDebug

# Menjalankan unit tests
./gradlew testDebugUnitTest

# Membangun Debug APK
./gradlew assembleDebug
```

---

## Audit & Kepatuhan Antislop

- **R-02 (Copywriting):** Bebas dari karakter em dash (`—`) pada seluruh salinan UI; digantikan dengan tanda baca natural (`:`, `,`, `-`).
- **R-16 & R-17 (Data & Claims):** Bebas dari buzzword generik AI dan statistik palsu; seluruh metrik dihitung melalui formula fisik nyata (Haversine, rasio gradien, UCI climb index).
- **R-21 & R-25 (Contrast & Theme):** Menggunakan palet Dark Mode OLED kontras tinggi yang ramah baterai dan memenuhi standar WCAG AA/AAA.
- **R-26 (Interactive Elements):** Tidak ada tombol mati (*dead controls*); setiap interaksi (penggeser, tab, pemilih rute, keping prompt AI) memiliki respon fungsional nyata.
- **R-27 (UI States):** Mendukung kondisi *Loading*, *Content*, dan *Error* secara tangguh.
