# Movement — Person Movement Register Android App

A digital person movement register Android application designed for factories, offices, industrial plants, and organizational security checkpoints to manually record, track, and manage entry, exit, movement history, outside duration, and exportable reports.

---

## 🚀 Key Features

1. **Digital IN & OUT Movement Logging**:
   - Explicit movement recording (**OUT**: Leaving premises, **IN**: Returning to premises).
   - Predefined and custom purpose tracking (*Official Work, Personal Work, Medical, Lunch / Break, Meeting, Emergency, Other*).
   - Predefined and custom destination tracking (*Outside Factory, Head Office, Client Location, etc.*).
   - Automatic local timezone timestamps and officer session logging (*Recorded By*).

2. **Strict Movement State Engine**:
   - Real-time person status (**INSIDE** or **OUTSIDE**).
   - **Duplicate OUT prevention**: Disallows recording another OUT movement if the person is already marked OUTSIDE.
   - **Duplicate IN prevention**: Disallows recording an IN movement if the person is already marked INSIDE.
   - Historical preservation: Multiple daily movements are stored as discrete, immutable records.

3. **Automatic Outside Duration Calculation**:
   - Calculates duration between matching OUT and IN timestamps.
   - Ongoing duration live ticker for personnel currently outside premises.
   - Midnight-crossing duration support.
   - Daily and monthly cumulative outside duration statistics.

4. **Personnel Registry**:
   - Person registration with Full Name, unique Employee/Person ID (`EMP-xxxx`), Designation, Department, Mobile, Address, Notes, and photograph attachment.
   - Fast instant search and filtering (All, Currently Inside, Currently Outside, Inactive).
   - Individual profile and movement history views.

5. **Multi-Format Report Exporting**:
   - **PDF Generation**: Generates multi-page official reports with document headers, summary metrics, and tabular movement records.
   - **Excel / Spreadsheet CSV Export**: Exports RFC 4180 UTF-8 formatted spreadsheets with column headers and total summary rows, opening natively in Microsoft Excel and Google Sheets.
   - **Image Export**: Generates high-resolution PNG / JPG report cards for instant messaging sharing.
   - Direct integration with Android `Intent.ACTION_SEND` and `FileProvider`.

6. **Offline-First & Remote Sync Architecture**:
   - Device-local **Room Database** acts as the single source of truth.
   - Works 100% offline without requiring internet connectivity.
   - Built-in REST API sync client (`MovementApiService`) supporting PHP 8 + MySQL backends (such as InfinityFree, cPanel, or VPS).
   - UUID-based idempotent queueing to prevent duplicate sync entries.

7. **Security & Role-Based Access Control**:
   - Role switching for **Admin** (record correction, user creation, settings), **Register Operator** (movement logging, person registration), and **Viewer** (read-only auditing).
   - Record correction functionality with mandatory audit trail reason logging.

---

## 🛠 Tech Stack

- **Language**: Kotlin 2.2
- **UI Toolkit**: Jetpack Compose with Material 3 Design
- **Architecture**: MVVM with Repository Pattern, StateFlow, Coroutines
- **Local Persistence**: Android Room Database (SQLite) + AndroidX DataStore
- **Networking**: Retrofit 2 + Moshi + OkHttp
- **Document Generation**: Android `PdfDocument`, `Canvas` Bitmap graphics, RFC 4180 CSV
- **Image Handling**: Coil + Android zero-permission Photo Picker
- **Target SDK**: Android 36 (Min SDK: 26)

---

## 📂 Project Structure

```
com.example/
├── data/
│   ├── export/             # PDF, Excel CSV, and Image report generators
│   │   ├── PdfExportManager.kt
│   │   ├── ExcelExportManager.kt
│   │   └── ImageExportManager.kt
│   ├── local/
│   │   ├── database/       # Room Database, Entities, and DAOs
│   │   │   ├── entities/   # PersonEntity, MovementEntity, UserEntity, SyncQueueEntity
│   │   │   ├── dao/        # PersonDao, MovementDao, UserDao, SyncQueueDao
│   │   │   └── MovementDatabase.kt
│   │   └── preferences/    # AppPreferences (DataStore)
│   ├── remote/             # Retrofit REST API client & DTOs
│   │   ├── api/MovementApiService.kt
│   │   └── dto/SyncDtos.kt
│   └── repository/         # MovementRepository, UserRepository, SyncRepository
├── domain/
│   └── utils/              # TimeUtils, Duration calculations, Date helpers
├── ui/
│   ├── components/         # PersonPhotoAvatar, MovementTypeBadge, StatusBadge
│   ├── dashboard/          # DashboardScreen & DashboardViewModel
│   ├── movement/           # MovementEntryScreen, MovementHistoryScreen & ViewModels
│   ├── navigation/         # MainScreen & NavHost
│   ├── persons/            # PersonsScreen & PersonsViewModel
│   ├── reports/            # ReportsScreen & ReportsViewModel
│   ├── settings/           # SettingsScreen & SettingsViewModel
│   └── theme/              # Color, Type, MovementTheme
└── MovementApp.kt          # Application class and dependency container
```

---

## 💻 Local Build & Execution

To assemble the debug APK locally:

```bash
# Using Gradle Wrapper
./gradlew assembleDebug

# Or using system Gradle
gradle assembleDebug
```

The resulting debug APK will be generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

To run unit tests:
```bash
gradle testDebugUnitTest
```

---

## ⚙️ Automated GitHub Actions Workflow

This repository contains an automated GitHub Actions CI workflow in `.github/workflows/android-build.yml` that:
1. Sets up JDK 17 and Android SDK.
2. Executes test suite (`testDebugUnitTest`).
3. Runs `assembleDebug` to produce a genuine debug APK.
4. Verifies the APK size, unpacks archive integrity, and computes SHA-256 hashes.
5. Saves the APK artifact to `APK_DOWNLOAD/app-debug.apk` and uploads it as a build artifact.

---

## 🌐 Optional PHP / MySQL Backend API

The app connects to any RESTful backend implementing the following standard endpoints:
- `POST /api/auth/login.php`
- `GET /api/person/list.php`
- `POST /api/person/create.php`
- `POST /api/person/update.php`
- `POST /api/movement/create.php`
- `GET /api/movement/list.php`
- `POST /api/sync/upload.php`
- `GET /api/sync/download.php`

Configure the URL in **Settings → Remote REST API Sync**.
