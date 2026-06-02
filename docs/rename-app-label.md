# Renaming the App Display Label

The launcher on a connected device was showing **`a211198_Hasif_DrNelson_Lab1`** instead of the intended Project 2 name. This is the user-facing app label that Android reads from a string resource — not the package name, module name, or Android Studio project name.

## Where the label comes from

`AndroidManifest.xml` declares the label on both the `<application>` and the launcher `<activity>`:

```xml
<application
    android:label="@string/app_name"
    ... >
    <activity
        android:name=".view.MainActivity"
        android:label="@string/app_name"
        ... >
```

Both point at the `app_name` string resource, so the displayed name is whatever `app_name` resolves to.

## The fix

Edited `app/src/main/res/values/strings.xml`:

```diff
 <resources>
-    <string name="app_name">a211198_Hasif_DrNelson_Lab1</string>
+    <string name="app_name">a211198_Hasif_DrNelson_Project2</string>
 </resources>
```

That single change updates the label everywhere the manifest references `@string/app_name` (launcher icon caption, task switcher, settings → apps).

## To see the change on the device

The old label can stick around in the launcher cache until the app is reinstalled:

1. Uninstall the existing app from the device, or run a clean build.
2. Re-run the app from Android Studio (or `./gradlew installDebug`).
3. The launcher icon should now read **a211198_Hasif_DrNelson_Project2**.

## What was *not* changed (and why)

- **Package / application ID** (`com.example.a211198_hasif_drnelson_Project2`) — already correct; changing it would break Firebase config and signing.
- **Theme name** (`Theme.A211198_Hasif_DrNelson_Lab4`) — internal identifier only, never shown to users. Renaming it would require updating every reference in manifest + themes.xml without changing anything the user sees.
- **Android Studio project / module folder names** — IDE-level only, do not affect the installed app.
