# Hikmah

[READ IN ENGLISH](README.en.md)

Aplikasi Android untuk membaca, mencari, menyimpan, dan mengunduh hadits berbahasa Indonesia.

## Fitur

- Antarmuka Jetpack Compose dengan Material 3.
- Beranda dengan koleksi kitab dalam grid dua kolom.
- Teks Arab dan terjemahan Indonesia.
- Pencarian hadits dengan total jumlah hasil.
- Paginasi pencarian tanpa batas total hasil.
- Lompat langsung ke nomor hadits tanpa animasi scroll.
- Tombol kembali ke bagian atas pada daftar kitab.
- Favorit dengan penyimpanan lokal.
- Copy hadits beserta sumber kitab dan nomor hadits.
- Mode terang dan gelap yang tersimpan di perangkat.
- Download Manager untuk penggunaan offline.
- Unduh semua kitab atau pilih kitab tertentu.
- Lanjutkan, batalkan, dan hapus unduhan per kitab.
- Progres unduhan tersimpan di Room.
- Maksimal enam koneksi HTTP aktif untuk menjaga keseimbangan kecepatan dan stabilitas.
- Validasi nomor, slug kitab, duplikasi, dan isi respons hadits sebelum masuk cache.

## Koleksi

Aplikasi menyediakan koleksi berikut:

- Shahih Bukhari
- Shahih Muslim
- Sunan Abu Daud
- Jami' At-Tirmidzi
- Sunan An-Nasa'i
- Sunan Ibnu Majah
- Muwatha Malik
- Musnad Ahmad
- Sunan Ad-Darimi

## Teknologi

- Kotlin
- Jetpack Compose
- Material 3
- Room
- Retrofit dan Gson
- WorkManager
- Kotlin Coroutines
- KSP
- Gradle Wrapper 9.7.1

Package name aplikasi:

```text
org.opennur.hadits
```

Konfigurasi Android:

- Minimum SDK: 26
- Compile SDK: 36
- Target SDK: 36
- Java toolchain: 21

## Struktur Proyek

```text
app/src/main/java/org/opennur/hadits/
├── data/
│   ├── local/       # Database Room dan DAO
│   ├── remote/      # API, DTO, dan validator integritas
│   └── HadithRepository.kt
├── model/           # Model domain aplikasi
└── ui/
    ├── components/  # Komponen kartu dan state UI
    ├── screens/     # Beranda, pencarian, detail, dan download manager
    ├── theme/       # Material 3 light/dark theme
    └── ...
```

## Sumber Data

Pemuatan utama menggunakan [API Hadis Indonesia](https://github.com/renomureza/hadis-api-id). Fallback indeks pencarian menggunakan [Hadith API](https://github.com/fawazahmed0/hadith-api) melalui CDN jsDelivr.

Data yang sudah dibuka atau diunduh disimpan di Room. Karena data utama berasal dari layanan online, aplikasi membutuhkan koneksi internet untuk mengambil data yang belum tersedia di cache.


## Download Offline

Buka tab **Offline** atau kartu **Simpan untuk offline** di beranda.

- **Unduh semua resource** membuat antrean untuk seluruh kitab.
- Tombol download pada setiap kitab hanya mengunduh kitab tersebut.
- Unduhan yang dibatalkan dapat dilanjutkan dari progres terakhir.
- Tombol hapus menghapus hadits kitab tersebut dari cache lokal setelah konfirmasi.
- Status unduhan tetap tersimpan ketika aplikasi ditutup.

Download dijalankan memakai WorkManager. Dispatcher HTTP dibatasi maksimal enam request aktif secara global.

## Pencarian

Pencarian memakai cache Room dan mengambil hasil dalam batch 80 item. Angka 80 bukan batas total. Saat pengguna mendekati bagian bawah daftar, batch berikutnya dimuat otomatis.

Total hasil dihitung langsung dari database dan ditampilkan, misalnya:

```text
80 dari 1.245 hadits
```

Untuk hasil paling lengkap, unduh koleksi yang ingin dicari melalui tab **Offline** terlebih dahulu.

## Menjalankan Proyek

Prasyarat:

- JDK 21
- Android SDK dengan platform 36
- Android SDK Build Tools

Build debug:

```bash
./gradlew :app:assembleDebug
```

APK hasil build:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Menjalankan unit test dan lint:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
```

Perintah di atas tidak menggunakan flag `--no-daemon`.

## Validasi Data

Test dan validator aplikasi memeriksa integritas teknis respons, termasuk:

- Nomor hadits positif dan berada di rentang halaman.
- Tidak ada nomor duplikat dalam satu halaman.
- Slug kitab pada detail sesuai dengan kitab yang diminta.
- Isi Arab atau terjemahan tidak kosong.
- Fixture rujukan untuk Bukhari nomor 1 dan Ahmad nomor 1.

Validasi teknis ini tidak membuktikan sanad, derajat sahih, atau keautentikan keagamaan seluruh hadits. Untuk publikasi dengan klaim keagamaan, lakukan peninjauan dengan sumber primer dan ahli hadits.

## Bahasa

- Bahasa Indonesia: dokumen ini
- [English](README.en.md)
