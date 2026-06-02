# Firebase Setup Guide — RunTrack

Follow these steps in order. You only need to do this **once** per Firebase project.

---

## Step 1 — Create a Firebase project

1. Go to **https://console.firebase.google.com** and sign in with your Google account
2. Click **"Add project"**
3. Name it `RunTrack` and click **Continue**
4. Disable Google Analytics (not needed for this app) → **Create project**
5. Wait for it to finish, then click **Continue**

---

## Step 2 — Register your Android app

1. On the **Project Overview** page, find the row of platform icons (iOS, Android, Web, Unity, Flutter)
2. Click the **Android icon** (green robot)
3. Fill in the form:
   - **Android package name** *(required, exact match):*
     ```
     com.example.a211198_hasif_drnelson_Project2
     ```
   - **App nickname** *(optional):* `RunTrack`
   - **Debug signing certificate SHA-1** *(optional):* leave blank for now
4. Click **Register app**

---

## Step 3 — Download google-services.json

1. On the next screen, click **"Download google-services.json"**
2. Save the file somewhere you can find it
3. Move/copy the file into your project at:
   ```
   app/google-services.json
   ```
   (Same directory as `app/build.gradle.kts`)
4. Back in Firebase, click **Next → Next → Continue to console** (skip the SDK setup instructions — the implementation will handle that)

> ⚠️ **Never commit `google-services.json` to a public repo.** Add `app/google-services.json` to `.gitignore` if your repo is public.

---

## Step 4 — Enable Email/Password Authentication

1. In the left sidebar → **Build → Authentication**
2. Click **"Get started"**
3. On the **Sign-in method** tab, click **Email/Password**
4. Toggle **Enable** (the top toggle only — leave passwordless off)
5. Click **Save**

---

## Step 5 — Create Firestore Database

1. In the left sidebar → **Build → Firestore Database**
2. Click **"Create database"**
3. Choose **Start in test mode** (we'll tighten the security rules later)
4. Pick a region close to you (e.g., `asia-southeast1` for Malaysia) → **Enable**
5. Wait for the database to provision

---

## Done — what's next?

Once all 5 steps are complete and `google-services.json` is placed at `app/google-services.json`, tell Claude **"firebase setup done"** and the Phase 3 implementation will begin.

---

## What you should have at the end

- ✅ A Firebase project named `RunTrack`
- ✅ Android app registered with package `com.example.a211198_hasif_drnelson_Project2`
- ✅ File `app/google-services.json` in the project
- ✅ Email/Password sign-in enabled in Authentication
- ✅ Firestore database created in test mode
