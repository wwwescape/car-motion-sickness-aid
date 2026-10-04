<p align="center">
  <img src="assets/logo.svg" alt="Car Motion Sickness Aid logo" width="120" />
</p>

<h1 align="center">Car Motion Sickness Aid</h1>

<p align="center">
  Motion cues for Android phones and tablets that ease car sickness while you read, browse, or
  watch, built with Material 3 Expressive to feel at home alongside Google's own Pixel apps. No
  backend, no analytics, no internet access.
</p>

<p align="center">
  <a href="https://github.com/wwwescape/car-motion-sickness-aid/releases"><img src="https://img.shields.io/github/v/release/wwwescape/car-motion-sickness-aid.svg?style=flat-square" alt="GitHub release" /></a>
  <a href="https://github.com/wwwescape/car-motion-sickness-aid/commits/master"><img src="https://img.shields.io/github/last-commit/wwwescape/car-motion-sickness-aid.svg?style=flat-square" alt="GitHub last commit" /></a>
  <a href="https://github.com/wwwescape/car-motion-sickness-aid"><img src="https://img.shields.io/github/languages/code-size/wwwescape/car-motion-sickness-aid.svg?color=red&style=flat-square" alt="GitHub code size" /></a>
</p>

## Features

- **Motion cues over any app** — animated dots in a non-touchable overlay move the way your body
  is pushed: back when the car speeds up, forward when it brakes, and sideways in turns, so what
  your eyes see agrees with what your inner ear feels. Touches always pass straight through.
- **Accelerometer, gyroscope, or both** — both sensors are combined by default, or either can be
  used on its own. Readings are measured relative to the vehicle, so the cues work whether the
  device lies flat, is propped up, or is held upright, in portrait or landscape.
- **Cue styles** — side dots, frame dots (all four edges), a full-screen dot grid, or a horizon
  line that banks in turns.
- **Customizable** — color (including an adaptive white-with-outline that reads over anything),
  size, number of dots, opacity, sensitivity, smoothing, and "hide when still".
- **Live preview** — the Home screen draws the cues from the real sensors, so you can tune them
  before you set off.
- **Hands-free activation** — optionally start automatically when you get into a vehicle and stop
  when you get out (on-device Activity Recognition via Google Play services), plus a Quick
  Settings tile and Pause/Stop actions in the notification.
- **Phones and tablets** — preview and controls side by side on wide screens.
- **Material 3 Expressive design** — light, dark, and system themes, dynamic color (Material
  You), 16+ curated color themes, adjustable contrast, and Pure Black / Absolute Black modes.
- **Light on battery** — sensors pause while the screen is off, and the overlay only redraws when
  the cues actually move.

## Installation

Download the APK from the [latest release](https://github.com/wwwescape/car-motion-sickness-aid/releases/latest)
and install it on your device. Grant the **Display over other apps** permission when asked, so the
cues can be drawn over other apps.

Requires Android 8.0 (API 26) or newer.

## Privacy

Car Motion Sickness Aid collects nothing and has no internet permission. Sensor readings are used
live and never stored. See the in-app Privacy statement (**Settings → About**).

## Development

### Prerequisites

- [Android Studio](https://developer.android.com/studio) (Narwhal or newer)
- JDK 17+ (bundled with Android Studio)
- An Android device running Android 8.0 (API 26) or newer — an emulator works for the UI, but
  you'll want a real device in a real car to judge the cues

### Build & run

```bash
git clone https://github.com/wwwescape/car-motion-sickness-aid.git
cd car-motion-sickness-aid
./gradlew installDebug
```

Or open the project in Android Studio and run the `app` configuration.

### Test

```bash
./gradlew lint testDebugUnitTest
```

### Release a new version

Bump `versionCode` and `versionName` in `app/build.gradle.kts`, commit, then:

```bash
git tag v0.1.0
git push origin v0.1.0
```

The tag push builds a signed release APK and AAB and attaches them to a new GitHub Release (see
`.github/workflows/release.yml`). It needs the `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`,
`KEY_ALIAS`, and `KEY_PASSWORD` repository secrets, which are kept locally in the gitignored
`keystore.properties`.

### Icons

The icon is Material Symbols "car_fan_recirculate_2" with its arrow replaced by "lens_blur".
Regenerate every icon resource with `python scripts/generate_icons.py` (needs
`pip install resvg-py`).

### Project layout

```
app/       Kotlin, Jetpack Compose (Material 3), single module
  motion/      Sensor engine, vehicle-frame math (unit tested), per-frame animator, and the
               Canvas renderer shared by the overlay and the in-app preview
  overlay/     Foreground service + non-touchable overlay window
  tile/        Quick Settings tile
  auto/        Activity Recognition "in vehicle" auto start/stop, boot re-registration
  data/        Settings (DataStore)
  ui/          Compose screens, navigation, and theme
scripts/   generate_icons.py — builds every launcher/notification icon from the glyphs
design/    Play Store icon assets
assets/    README assets
```

## License

GPL-3.0 — see [LICENSE](LICENSE).

## Support

If you find Car Motion Sickness Aid useful, consider buying me a coffee:

[<img src="https://cdn.buymeacoffee.com/buttons/v2/default-yellow.png" alt="Buy Me A Coffee" height="40" />](https://buymeacoffee.com/wwwescape)
