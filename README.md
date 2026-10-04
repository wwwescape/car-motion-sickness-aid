<p align="center">
  <img src="assets/logo.svg" alt="Car Motion Sickness Aid logo" width="120" />
</p>

<h1 align="center">Car Motion Sickness Aid</h1>

<p align="center">
  Motion cues for Android phones and tablets that ease car sickness while you read, browse or
  watch. Visually aligned with first-party Google Pixel apps (Material 3 Expressive). No
  backend, no analytics, no internet access.
</p>

## Features

- **Motion cues over any app** — animated dots drawn in a non-touchable overlay that move the
  way your body is pushed: back when the car speeds up, forward when it brakes, sideways in
  turns. What your eyes see then agrees with what your inner ear feels. Touches always pass
  straight through.
- **Accelerometer, gyroscope, or both** — fuses the accelerometer (speeding up/braking/turns)
  with the gyroscope (turns) by default, or uses either sensor on its own. Readings are projected
  onto the vehicle's horizontal plane using gravity, so the cues work whether the device lies
  flat, is propped up, or is held upright, in portrait or landscape.
- **Cue styles** — side dots, frame dots (all four edges), full-screen dot grid, or a horizon
  line that banks in turns.
- **Customizable** — color (including an adaptive white-with-outline that reads over anything),
  size, number of dots, opacity, sensitivity, smoothing, and "hide when still".
- **Live preview** — the Home screen renders the cues from the real sensors so you can tune
  them before you set off.
- **Hands-free activation** — optionally start automatically when you get into a vehicle and
  stop when you get out (on-device Activity Recognition via Google Play services), a Quick
  Settings tile, and Pause/Stop actions in the notification.
- **Battery-friendly** — sensors pause while the screen is off, and the overlay only redraws
  when the cues actually move.
- **Phones and tablets** — side-by-side preview and controls on wide screens.
- **Material 3 Expressive design** — light/dark mode, dynamic color (Material You), curated
  color themes, contrast and pure/absolute black options.

## Prerequisites

- Android Studio (Narwhal or newer): https://developer.android.com/studio
- JDK 17+ (bundled with Android Studio)
- An Android device running API 26 (Android 8.0) or newer — an emulator works for the UI, but
  you'll want a real device in a real car to judge the cues

## Build & run

```
git clone https://github.com/wwwescape/car-motion-sickness-aid.git
cd car-motion-sickness-aid
```

Open the project in Android Studio and run the `app` configuration, or from the command line:

```
./gradlew installDebug
```

## Test

```
./gradlew lint testDebugUnitTest
```

## Release a new version

```
git tag v0.1.0
git push origin v0.1.0
```

That tag push builds a signed release APK and AAB and attaches them to an auto-generated
GitHub Release. See `.github/workflows/release.yml`; it needs the
`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD` repository secrets set
(see `keystore.properties`, which is gitignored and holds these locally).

## Project layout

```
app/       Kotlin, Jetpack Compose (Material 3), single module
  motion/      Sensor engine, vehicle-frame math (unit tested), per-frame animator, and the
               Canvas renderer shared by the overlay and the in-app preview
  overlay/     Foreground service + non-touchable overlay window
  tile/        Quick Settings tile
  auto/        Activity Recognition "in vehicle" auto start/stop, boot re-registration
  data/        Settings (DataStore)
  ui/          Compose screens/navigation/theme
scripts/   generate_icons.py — builds every launcher/notification icon from the glyphs
design/    Play Store icon assets
assets/    README/repo assets
```

## Icon

Material Symbols "car_fan_recirculate_2" with its arrow replaced by "lens_blur". Regenerate all
icon resources with `python scripts/generate_icons.py` (needs `pip install resvg-py`).

## Privacy

Car Motion Sickness Aid collects nothing and has no internet permission. Sensor readings are
used live and never stored. See the in-app Privacy statement (Settings → About).

## License

GPL-3.0 — see `LICENSE`.

## Support

If you find Car Motion Sickness Aid useful, consider buying me a coffee:

[<img src="https://cdn.buymeacoffee.com/buttons/v2/default-yellow.png" alt="Buy Me A Coffee" height="40" />](https://buymeacoffee.com/wwwescape)
