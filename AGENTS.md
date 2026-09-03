# AGENTS.md — pax_sdk

Flutter plugin for PAX payment terminals (Android). Neptune Lite API: NFC (PICC), thermal printer, barcode scanner (`IScanner`).

## Tooling

- Always use **FVM**: `fvm flutter …` / `fvm dart …`
- Flutter pin: see `.fvmrc` (currently `3.47.2`)
- IDE SDK path: `.fvm/flutter_sdk` (configured in `.vscode/settings.json`)

```bash
fvm flutter pub get
fvm flutter pub upgrade --major-versions
cd example && fvm flutter pub get
fvm flutter analyze
cd example && fvm flutter analyze
fvm flutter test
```

## Layout

| Path | Role |
| --- | --- |
| `lib/pax_sdk.dart` | Public Dart API (MethodChannel `pax_sdk`, EventChannel `pax_sdk/scanner`) |
| `android/src/main/java/.../paxSDK.java` | **Canonical** Android plugin implementation |
| `android/src/main/AndroidManifest.xml` | Plugin permissions (PICC / PRINTER / SCANNER) — merges into host apps |
| `android/libs/` | Neptune Lite + PAX jars (required) |
| `example/` | Sample host app |
| `doc.zip` | PAX Neptune Lite Javadoc reference |

Duplicate `paxSDK.java` copies under `android/app/` and `example/android/app/` exist for historical reasons. **Edit `android/src/main/.../paxSDK.java` first**, then sync copies if needed.

## Conventions

- Match existing Dart/Java style; minimal diffs; no drive-by refactors.
- New PAX hardware features: Dart wrappers in `lib/pax_sdk.dart` + native handlers in `paxSDK.java` + README/CHANGELOG.
- Scanner: `startScanner` / `stopScanner` + streams `onReadSuccess` / `scanResults`. Default `EScannerType.REAR`; allow FRONT/LEFT/RIGHT/EXTERNAL.
- Permissions belong in plugin `AndroidManifest.xml` so consumers get them automatically.
- Do not commit secrets, `.env`, or large generated build trees.
- Prefer not to commit `doc.zip` unpack noise; keep zip as reference if needed.
- Bump `pubspec.yaml` version + `CHANGELOG.md` when shipping API changes.

## Boundaries

- Android-first; iOS is not a supported PAX target for this plugin.
- Do not remove or replace Neptune Lite jars without an explicit request.
- Do not invent PAX APIs — check `doc.zip` / Neptune Lite Javadoc (`IScanner`, `IPrinter`, `IPicc`, `IDAL`).
- No force-push / destructive git unless explicitly asked.

## Commit style

Short imperative messages focused on why (e.g. `Add IScanner barcode support`). Only commit when the user asks.
