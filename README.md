# Morrow

**A quieter kind of home.**

Morrow is an Android home launcher built from scratch with native Kotlin and platform views. It is designed around a simple idea: the home screen should feel like a place to land, not another feed demanding attention.

## The idea

- **Warm paper, deep forest.** An earthy light palette with a low-glare midnight alternative.
- **Time first.** A large, live clock and a date that make the home screen feel grounded.
- **Your apps, not our agenda.** A small favourites row and a searchable, alphabetised app library.
- **Small and native.** No third-party runtime dependencies, no analytics, and no network permission.
- **Made to be a home app.** Morrow registers for Android's HOME role and can be selected as the default launcher.

## Current features

- Android HOME / DEFAULT intent handling.
- Discovers launchable apps installed on the device.
- Opens apps from a favourites strip or the app library.
- Search filters the app library as you type, with filtering performed away from the UI thread.
- The app library uses a recycling grid, so only visible app tiles need to be drawn.
- App discovery and icon loading run in the background.
- Choose and save up to four home-screen shortcuts, or reset to automatic suggestions.
- Live clock and localised date.
- Warm-paper and midnight appearance modes.
- Remembers the selected appearance and shortcut choices locally.

## Build

Requirements: JDK 17, Android SDK Platform 35, and Android Build Tools 35.0.0.

```bash
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

On Windows:

```bat
gradlew.bat assembleDebug
```

## Install

Install the debug APK with:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Then open **Morrow Launcher** and choose it as your Home app when Android asks, or change the default in Android Settings.

## Project shape

This is an intentionally small early release. The UI is built with Android platform views, so the project does not depend on a UI framework or a third-party launcher base. Widgets, icon packs, gesture customisation, and backup/restore are not implemented yet.

## License

MIT. See [LICENSE](LICENSE).