# Firebase Setup Guide — Google Sign-In (Phase 5.4b)

This adds **"Continue with Google"** to the Login screen. The app code is already
written and compiles — these are the one-time Firebase Console + `local.properties`
steps needed before it works on a device.

Do these **in order**. You only need to do this **once** per Firebase project.

> **Project:** `enerva-be887` · **Package:** `com.example.a211198_hasif_drnelson_Project2`

---

## Step 1 — Add your debug SHA-1 fingerprint to Firebase

Google Sign-In refuses to issue a token unless Firebase knows your app's signing
certificate. Your **debug** SHA-1 (already extracted via `./gradlew :app:signingReport`) is:

```
F2:D9:93:2F:4F:6C:DF:1F:EF:1D:EB:0D:1B:9C:7C:CC:C5:31:FD:DE
```

1. Go to **https://console.firebase.google.com** → open project **`enerva-be887`**
2. Click the **gear icon** (top-left) → **Project settings**
3. Scroll to **Your apps** → select the Android app
   (`com.example.a211198_hasif_drnelson_Project2`)
4. Click **Add fingerprint**
5. Paste the SHA-1 above → **Save**

> If you later test on a **release** build or a different machine, add that machine's
> debug SHA-1 too — each signing key needs its own fingerprint.

---

## Step 2 — Enable the Google sign-in provider

1. Left sidebar → **Build → Authentication**
2. **Sign-in method** tab
3. Click **Google** in the provider list
4. Toggle **Enable**
5. Set a **Project support email** (your own email) — required
6. Click **Save**

> Enabling Google here automatically creates a **Web client ID** (an OAuth client of
> type 3). You need that ID in Step 4.

---

## Step 3 — Re-download `google-services.json`

The current `app/google-services.json` has an **empty `oauth_client` array** because
it was downloaded before Google sign-in existed. Replace it:

1. **Project settings** → **Your apps** → Android app
2. Click **Download google-services.json**
3. Replace the file in your project at:
   ```
   app/google-services.json
   ```
   (same directory as `app/build.gradle.kts` — overwrite the old one)

> ✅ Verify: open the new file and confirm `oauth_client` is **no longer empty** — it
> should list at least one entry with `"client_type": 3` (the Web client).

---

## Step 4 — Copy the Web client ID into `local.properties`

1. **Build → Authentication → Sign-in method → Google** → expand
2. Expand **Web SDK configuration**
3. Copy the **Web client ID** — it looks like:
   ```
   95395130249-xxxxxxxxxxxxxxxx.apps.googleusercontent.com
   ```
4. Open `local.properties` (project root) and fill in the placeholder line:
   ```properties
   GOOGLE_WEB_CLIENT_ID=95395130249-xxxxxxxxxxxxxxxx.apps.googleusercontent.com
   ```

> `local.properties` is gitignored — the client ID never gets committed. The app reads
> it at build time via `BuildConfig.GOOGLE_WEB_CLIENT_ID`.

---

## Step 5 — Rebuild and test

1. In Android Studio: **Sync Project with Gradle Files** (or `./gradlew :app:assembleDebug`)
2. Run the app on a device/emulator that has a **Google account** signed in
3. On the Login screen, tap **Continue with Google**
4. Pick your account in the system picker

**Expected:** you land on **Home**, and the account appears in
**Firebase Console → Authentication → Users** (linked as a *Google* provider, not Email/Password).

---

## Troubleshooting

| Symptom | Cause / fix |
|---|---|
| Toast: *"Google Sign-In not configured (missing Web client ID)"* | Step 4 not done — `GOOGLE_WEB_CLIENT_ID` is still blank in `local.properties`. Fill it and rebuild. |
| Toast: *"No Google account found on this device"* | No Google account signed in on the device → add one in system Settings. |
| Picker opens then fails with a `DEVELOPER_ERROR` / `10:` error | SHA-1 missing or wrong (Step 1), or `google-services.json` not re-downloaded (Step 3). Re-check both. |
| Nothing happens after tapping (no picker) | `oauth_client` still empty in `google-services.json` → re-download it (Step 3) and Gradle-sync. |
| Picker appears but you dismiss it | Expected — cancelling is a no-op (no error toast by design). |

---

## What you should have at the end

- ✅ Debug SHA-1 `F2:D9:…:FD:DE` added to the Firebase Android app
- ✅ Google provider **enabled** in Authentication
- ✅ Fresh `app/google-services.json` with a non-empty `oauth_client`
- ✅ `GOOGLE_WEB_CLIENT_ID=…` set in `local.properties`
- ✅ "Continue with Google" lands on Home; user shows in Firebase Console as a Google provider
