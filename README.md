# Producție Puhoi — Android

Aplicație Android nativă (Java + WebView) pentru **https://solar.puhoi.eu/**.

## Ce include
- Branding „Producție Puhoi”, logo solar, splash-ul de pornire al sistemului și bară de navigare mobilă.
- WebView HTTPS către dashboardul Puhoi V8 existent, fără copierea datelor sau modificarea Raspberry Pi.
- Autentificare HTTP Basic Nginx la prima conectare; opțional, acreditări criptate local folosind Android Keystore (AES-GCM). Nu există parolă în codul aplicației.
- Protecție TLS: erorile de certificat sunt refuzate; conexiunile HTTP în clar nu sunt permise.
- Reîncărcare după eroare de conexiune, starea online/offline, control încărcare și navigare rapidă.
- Linkurile externe deschise în browser.

## Compilare APK (fără instalarea Android Studio)
Este necesar JDK 17+ și Android SDK Command-line Tools cu platforma API 35, Build Tools 35.x și Gradle 8.9.

1. Instalează Android SDK command-line tools de la https://developer.android.com/studio#command-tools
2. Instalează `platforms;android-35` și `build-tools;35.0.0` folosind `sdkmanager`.
3. Instalează Gradle 8.9 (https://gradle.org/releases/) sau generează un wrapper cu `gradle wrapper --gradle-version 8.9`.
4. În directorul proiectului, rulează `gradle assembleDebug` (sau `./gradlew assembleDebug` dacă există wrapper).
5. Fișierul rezultat: `app/build/outputs/apk/debug/app-debug.apk`.

APK-ul debug este semnat automat pentru instalare manuală. Pentru publicare în Play Store trebuie semnare release, configurare și testare suplimentară.

## Limitări
Designul dashboardului redat în WebView este cel al site-ului existent, nu o reproducere fidelă a machetei conceptuale; bara superioară și navigația de jos sunt native. Camerele, graficele și alte pagini depind de compatibilitatea lor cu Android WebView. Meniul nativ încearcă să activeze navigarea existentă prin `data-screen`; etichetele pot necesita ajustare după testarea pe un dispozitiv real.

Nu am compilat și testat acest proiect într-un emulator sau pe un dispozitiv Android în mediul de pregătire a arhivei; verifică funcționalitatea înainte de distribuire.

## Compilare online, fără Android Studio (GitHub Actions)
Proiectul conține `.github/workflows/android-apk.yml`.
1. Creează un repository privat nou pe GitHub, încarcă **conținutul** directorului `productie-puhoi-android` în rădăcina repository-ului.
2. Deschide secțiunea **Actions**, workflow-ul **Compilare APK Producție Puhoi** și apasă **Run workflow** (ori îl pornește automat un push pe ramura `main`).
3. După finalizare, descarcă artefactul **Productie-Puhoi-APK**, în interiorul căruia se află `app-debug.apk` instalabil.
Nu introduce username/parola dashboardului în GitHub; APK-ul solicită acreditările la prima deschidere.
