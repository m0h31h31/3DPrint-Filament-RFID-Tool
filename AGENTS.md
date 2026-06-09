# AGENTS.md

This file provides guidance to Codex (Codex.ai/code) when working with code in this repository.

## Project Overview

BambuRfidReader is an Android app (min SDK 28, target SDK 36) that reads, clones, and manages Bambu Lab RFID filament tags (Mifare Classic NFC cards) with filament inventory management.

## Build Commands

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Install on connected device
./gradlew installDebug

# Run unit tests
./gradlew test

# Run lint
./gradlew lint

# Clean
./gradlew clean
```

## Architecture

### Core Data Flow

```
NFC Tag (Mifare Classic)
  → NfcTagReader.readRaw()       # Pure I/O, returns RawTagReadResult sealed class
  → NfcTagProcessor.parseAndPersist()  # Parsing + DB writes, returns ProcessedTagData
  → SQLite (FilamentDbHelper)    # Persistence
  → Compose UI screens           # Display
```

### Key Files

| File | Role |
|------|------|
| `MainActivity.kt` (~4400 lines) | NFC callbacks, all UI state, DB management, file I/O; also defines `NfcUiState`, `ParsedField`, `DisplayData`, `ReaderBrand`, `CrealityTagData`, `SnapmakerTagData`, `FilamentDbHelper`, `LogCollector` |
| `NfcTagReader.kt` | Mifare Classic I/O, HKDF-SHA256 key derivation from UID |
| `NfcTagProcessor.kt` | Block parsing, color/filament extraction, inventory sync |
| `ui/navigation/AppNavigation.kt` | Bottom nav (6 tabs: Reader, Inventory, Data, Tag, Creality, Misc) |
| `ui/screens/WriteScreen.kt` | NDEF write UI; defines `NdefWriteRequest` / `NdefWriteType` used by MainActivity |
| `utils/ConfigManager.kt` | Remote config fetch (Gitee primary, GitHub backup), local asset fallback |
| `utils/AnalyticsReporter.kt` | Install/launch event reporting via `EVENT_API_KEY` in BuildConfig |
| `utils/NetworkUtils.kt` | HTTP fetch/post, SHA-256 hash helper |

### NFC Layer (`NfcTagReader.kt`)

- `readRaw()` is the main entry point; returns `RawTagReadResult` (sealed: Success/Failure)
- Keys are derived per-UID via HKDF-SHA256 and cached to avoid repeated CPU cost
- Tunable stability parameters at the top of the file (`READ_BLOCK_RETRY_COUNT`, `AUTH_RETRY_COUNT`)
- `reconnectMifareClassic()` handles connection recovery between sector reads

### Data Processing (`NfcTagProcessor.kt`)

- `parseForPreview()` — parse only, no DB write (used for tag preview before write)
- `parseAndPersist()` — parse + upsert inventory to DB
- Multi-color tags handled via `parseAdditionalColors()` (block16 extension)
- `buildDisplayData()` enriches parsed data with filament catalog lookup

### Multi-Brand Support

`ReaderBrand` enum (`BAMBU`, `CREALITY`, `SNAPMAKER`) gates brand-specific logic throughout `MainActivity`. `CrealityTagData` and `SnapmakerTagData` are separate data classes defined in `MainActivity.kt`. The Creality tab is feature-gated via `crealityEnabled` preference.

### Remote Config & Assets

Four bundled JSON assets in `app/src/main/assets/` are the authoritative seed data:
- `filaments_color_codes.json` — filament catalog
- `filaments_type_mapping.json` — base-type → specific-type mapping
- `creality_material_list.json` — Creality filament catalog
- `AppConfig.json` — app version/message/links config

`ConfigManager` fetches updated versions from Gitee (primary) / GitHub (backup) on launch and saves them to `context.getExternalFilesDir(null)`. `getLocalConfig()` reads from external storage first, falling back to the bundled asset. `syncFilamentDatabase()` / `syncCrealityMaterialDatabase()` re-seed the DB after an update.

### Database (`FilamentDbHelper` inside `MainActivity.kt`)

SQLite, version 18, with migration logic in `onUpgrade()`. Tables:
- `filaments` — material/color catalog (seeded from bundled data)
- `filament_inventory` (`tray_uid_table`) — per-tray remaining weight/percent tracking
- `share_tags` — cloned tag storage (uid, material, color, copy count)
- `creality_materials` — Creality printer filament catalog
- `filament_type_mapping` — base type → specific type mapping
- `meta_v2` — key-value app metadata

### UI

- Jetpack Compose with Material 3
- Two UI style options: `NEUMORPHIC` and `MIUIX` (toggled via SharedPreferences)
- Theme modes: `SYSTEM`, `LIGHT`, `DARK`; Color palettes: `OCEAN` + others
- Features gated by preferences: `inventoryEnabled`, `crealityEnabled`
- Custom components in `ui/components/`: `NeuPanel`, `ColorSwatch`, `InfoLine`

## Key Libraries

- **MIUIX Android** (`sh.calvin.reorderable`, miuix) — alternative UI component set
- **Reorderable** — drag-and-drop list support
- Navigation Compose 2.8.4 with typed routes
- No third-party NFC or crypto libraries — uses `android.nfc.*` and `javax.crypto.Mac` directly

## Important Patterns

- **Sealed classes for NFC results**: always handle all branches of `RawTagReadResult` and `RawTagReadFailureReason`
- **State in MainActivity**: all screen state is `mutableStateOf()` in `MainActivity`, passed down to composables
- **DB version bumps**: increment `DATABASE_VERSION` and add a migration case in `onUpgrade()` for any schema changes
- **Key caching**: HKDF keys are cached by UID in `NfcTagReader` — avoid breaking this when modifying key derivation
- **Logging**: use the top-level `logDebug(message)` function (defined in `MainActivity.kt`) everywhere — it routes to both `Log.d` and the in-app `LogCollector`
- **Config file priority**: external storage file → bundled asset (never hardcode data that belongs in the JSON assets)
