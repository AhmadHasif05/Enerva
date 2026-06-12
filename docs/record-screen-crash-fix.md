# Fix: Record screen crashed the app on the physical phone

## The problem

Opening the **Record** screen on the physical test phone instantly closed the app.

## Why it happened

The Record screen shows a map using the **MapLibre** library.

- Our map library (`ramani-maplibre`) was quietly pulling in **MapLibre version 13.0.2**.
- MapLibre 13.x only knows how to draw maps using **Vulkan** (a modern graphics system).
- The test phone (a Unisoc `sp7731e`, Android 11) has an **old GPU with no Vulkan support**.

So the moment the map tried to draw, MapLibre gave up and crashed the whole app with this error:

```
java.lang.Error: No Vulkan compatible GPU found
```

## The fix

We told the project to use an **older MapLibre version (11.11.0)** that can still draw maps the old-fashioned way (OpenGL ES), which every phone supports — including the old one.

In `app/build.gradle.kts`:

```kotlin
implementation("org.maplibre.gl:android-sdk") {
    version { strictly("11.11.0") }
}
```

`strictly` means "always use exactly this version, even if something else asks for a newer one."

**Important:** Don't remove this or let MapLibre upgrade back to 12.x/13.x unless every phone you support has Vulkan. If you ever bump `ramani-maplibre`, re-check which MapLibre version it pulls in.

## Bonus: making the app install on a low-storage phone

The test phone was almost full (only ~587 MB free), and the normal app file was **81 MB**, so it refused to install ("not enough space").

The app file is big because it normally packs the code for **four different phone chip types (ABIs)** into one file. The test phone only needs **one** of them (`armeabi-v7a`).

So for the **debug** build we now include only that one chip type, shrinking the install from **81 MB to ~46 MB**. In `app/build.gradle.kts`:

```kotlin
debug {
    ndk {
        abiFilters += "armeabi-v7a"
    }
}
```

- This only affects the **debug** (testing) build. The **release** build still includes everything, so your final submission is unaffected.
- `armeabi-v7a` works on both the old test phone and newer phones.
- If you ever test on an **emulator**, add `"x86_64"` to that line.

## How to install on the phone

Plain command, no extra flags needed:

```bash
./gradlew :app:installDebug
```

Or just press **Run ▶** in Android Studio.

## How we confirmed it works

Installed the fixed build on the phone, opened the Record screen, and the **map rendered correctly** with street names and the live location dot — no crash.
