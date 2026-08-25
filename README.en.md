# Hikmah

[Baca dalam Bahasa Indonesia](README.md)

An Android application for reading, searching, saving, and downloading Indonesian hadith collections.

## Features

- Jetpack Compose UI with Material 3.
- Home screen with a two-column book collection grid.
- Arabic text and Indonesian translation.
- Hadith search with a total result count.
- Unbounded search pagination.
- Instant jump to a hadith number without animated scrolling.
- Back-to-top action on book lists.
- Favorites stored locally.
- Copy hadith text with its collection source and hadith number.
- Persistent light and dark themes.
- Download Manager for offline use.
- Download all collections or select individual books.
- Resume, cancel, and delete downloads per book.
- Download progress persisted in Room.
- A maximum of six active HTTP connections for a balance of speed and stability.
- Validation of hadith numbers, collection slugs, duplicates, and response content before caching.

## Collections

The application provides these collections:

- Sahih Bukhari
- Sahih Muslim
- Sunan Abu Dawud
- Jami' At-Tirmidhi
- Sunan An-Nasa'i
- Sunan Ibn Majah
- Muwatta Malik
- Musnad Ahmad
- Sunan Ad-Darimi

## Technology

- Kotlin
- Jetpack Compose
- Material 3
- Room
- Retrofit and Gson
- WorkManager
- Kotlin Coroutines
- KSP
- Gradle Wrapper 9.7.1

Application package name:

```text
org.opennur.hadits
```

Android configuration:

- Minimum SDK: 26
- Compile SDK: 36
- Target SDK: 36
- Java toolchain: 21

## Project Structure

```text
app/src/main/java/org/opennur/hadits/
├── data/
│   ├── local/       # Room database and DAOs
│   ├── remote/      # API, DTOs, and integrity validators
│   └── HadithRepository.kt
├── model/           # Application domain models
└── ui/
    ├── components/  # Cards and UI state components
    ├── screens/     # Home, search, detail, and download manager
    ├── theme/       # Material 3 light/dark theme
    └── ...
```

## Data Sources

Primary loading uses the [Indonesian Hadith API](https://github.com/renomureza/hadis-api-id). The search indexing fallback uses [Hadith API](https://github.com/fawazahmed0/hadith-api) through the jsDelivr CDN.

Opened or downloaded data is stored in Room. Since the primary data comes from online services, an internet connection is required for data that is not already cached.

> Licensing, attribution, and data usage terms follow each upstream source. Check the upstream licenses before distributing the application publicly.

## Offline Downloads

Open the **Offline** tab or the **Save for offline use** card on the home screen.

- **Download all resources** queues every collection.
- The download action on each book downloads only that book.
- Cancelled downloads can resume from their previous progress.
- The delete action removes that book's hadith data from the local cache after confirmation.
- Download status is retained when the application is closed.

Downloads run through WorkManager. The HTTP dispatcher is globally limited to six active requests.

## Search

Search uses the Room cache and retrieves results in batches of 80 items. 80 is not the total result limit. When the user approaches the bottom of the list, the next batch loads automatically.

The total result count is calculated directly from the database and displayed, for example:

```text
80 of 1,245 hadiths
```

For the most complete results, download the collections you want to search from the **Offline** tab first.

## Running the Project

Prerequisites:

- JDK 21
- Android SDK platform 36
- Android SDK Build Tools

Build the debug APK:

```bash
./gradlew :app:assembleDebug
```

Generated APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Run unit tests and lint:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
```

The commands above intentionally do not use the `--no-daemon` flag.

## Signed Release

Release builds use the local signing configuration from `keystore.properties`, which is ignored by Git. The development keystore is located at `signing/release.keystore` and uses the `hikmah-release` alias.

Release builds enable R8 and resource shrinking to reduce APK size.

Build the signed release APK:

```bash
./gradlew :app:assembleRelease
```

Release APK:

```text
app/build/outputs/apk/release/app-release.apk
```

For production distribution, replace the development keystore with an organization-owned production keystore and never share its private key or password.

## Data Validation

Application tests and validators check the technical integrity of responses, including:

- Hadith numbers are positive and inside the requested page range.
- No duplicate numbers appear in one page.
- The detail slug matches the requested collection.
- Arabic text or translation is not empty.
- Reference fixtures for Bukhari hadith 1 and Ahmad hadith 1.

Technical validation does not prove the chain of narration, authenticity grade, or religious authenticity of every hadith. For a public release containing religious claims, review the content against primary sources and qualified hadith scholars.

## Languages

- English: this document
- [Bahasa Indonesia](README.md)
