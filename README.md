# Film Camera

A compact, native Android camera starter in Kotlin and Jetpack Compose. It uses CameraX for a live preview, photo capture, video recording, Android runtime permission requests, and MediaStore gallery output.

## Included

- Camera preview with a restrained, portrait-first interface.
- `PHOTO` and `VIDEO` controls; video can include audio after microphone permission.
- Photos save to `Pictures/Film Camera`; videos save to `Movies/Film Camera`.
- Six original looks: Original, Vivid Slide, Documentary, Pastel Negative, Warm Grain, and Silver Mono. These are custom film-style looks—not official profiles and not based on any proprietary camera color science.
- A live picker and responsive preview overlay for each look.

## Build and run

1. Open this folder in Android Studio.
2. Let Gradle synchronize, then install Android SDK Platform 35 if Studio asks.
3. Run on a physical Android device (Android 8.0/API 26 or newer); grant Camera and, for audio video, Microphone permissions.

The project uses Android Gradle Plugin 9.4.1 with built-in Kotlin 2.2.10, Compose, and CameraX 1.4.1. A physical device is recommended because most emulators do not provide a useful camera source.

## Current limits

The picker’s color wash/grain treatment is live in the preview, and the selected color look is baked into saved JPEGs. **Video visibly warns “LIVE LOOK PREVIEW · VIDEO SAVES ORIGINAL” before recording.** Applying the same GPU effects to encoded video requires a camera-effects/OpenGL pipeline and is deliberately left for the next iteration, so video is recorded without a baked-in look. Videos are staged briefly in the app cache, then copied into MediaStore.
