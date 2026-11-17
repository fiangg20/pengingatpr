# Pengingat PR

Aplikasi Android (Kotlin + Jetpack Compose, tema Material 3 Expressive) untuk mencatat dan mengingatkan PR.
Jadwal pelajaran dapat diimpor dari PDF aSc Timetables.

## Cara membangun APK tanpa Android Studio

1. Ekstrak berkas zip. Buka folder hasil ekstrak sampai terlihat `app`, `build.gradle.kts`, dan `settings.gradle.kts`.
2. Buka github.com/new, isi nama repositori, lalu klik **Create repository**.
3. Klik tautan **uploading an existing file**. Pilih semua isi DI DALAM folder (bukan foldernya), seret ke halaman, tunggu selesai, lalu klik **Commit changes**.
4. Klik **Add file > Create new file**. Pada kolom nama ketik `.github/workflows/build.yml`. Buka `build.yml.txt`, salin seluruh isinya, tempel ke kotak besar, lalu klik **Commit changes**.
5. Buka tab **Actions** dan tunggu 3 sampai 6 menit sampai muncul tanda centang hijau. Bila tidak berjalan sendiri, klik nama workflow lalu **Run workflow**.
6. Setelah hijau, buka halaman **Releases** repositori (`github.com/<pengguna>/<repo>/releases/latest`) dan unduh berkas `.apk`. Paling mudah dibuka langsung di browser ponsel. Pasang, lalu izinkan "pasang aplikasi tidak dikenal" bila diminta.
