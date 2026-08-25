# AURA DEFENS

AURA DEFENS is a native Android foundation for private, realistic mobile defense. Phase 1 establishes a stable Jetpack Compose and Material 3 app with a locally persisted Aura ID and a release build ready for Codemagic.

## Build locally

```bash
./gradlew clean assembleRelease
```

The generated APK is located at:

```text
app/build/outputs/apk/release/
```

## Build with Codemagic

Connect this repository to Codemagic and start the `aura-android-release` workflow. The workflow configures the Android SDK location, makes Gradle executable, cleans the project, and builds the release APK. The APK is exported from `app/build/outputs/apk/release/*.apk`.

## Phase 1

This phase creates the compilable native foundation only. It does not claim to scan devices, simulate threats, or provide security controls that are not implemented.

Next phases:

- Security Posture Engine
- Real App Scanner
- VPN Service
- Notification Guard
- Share Scanner
- QR Scanner
- Auras LAN
- Reports
- Encrypted Vault