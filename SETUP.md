# Build a Signed Release APK on GitHub Actions — Setup Guide

This guide wires up a **signed Release APK** build that runs entirely on GitHub. The flow: push code → GitHub builds → you get an installable signed APK (pushing a tag also auto-creates a GitHub Release).

## What's in the box

```
your-project/
├── .github/workflows/
│   └── build-release-apk.yml      ← build workflow (provided)
├── app/
│   └── build.gradle.kts            ← add signingConfigs here (reference provided)
└── scripts/
    └── encode-keystore.sh          ← keystore → base64 helper (provided)
```

Signing uses **environment variable injection** rather than committing the keystore in plaintext: the keystore is stored as base64 in a GitHub Secret and decoded at CI runtime; passwords go through Secrets too. **The keystore never enters git.**

---

## Step 1 — Generate a signing keystore (once only)

```bash
keytool -genkeypair -v \
  -keystore release.keystore \
  -alias my-key-alias \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass <your-keystore-password> \
  -keypass <your-key-password>
```

Fill in the name/organization prompts. **Back up this `release.keystore` file** — if you lose it, you can never ship an update for the same app (the signature must stay identical).

Write down these four values:

| Item | Meaning | Example |
|---|---|---|
| keystore file | `release.keystore` | — |
| `KEYSTORE_PASSWORD` | keystore password | `••••••••` |
| `KEY_ALIAS` | key alias | `my-key-alias` |
| `KEY_PASSWORD` | key password | `••••••••` |

## Step 2 — Add the signing config to `app/build.gradle.kts`

Using the provided `app/build.gradle.kts` as reference, add these blocks at the matching spots in your project:

```kotlin
android {
    // ...

    // Read the keystore location once; everything below is guarded on it being set.
    val keystorePath = System.getenv("KEYSTORE_PATH")
    signingConfigs {
        create("release") {
            if (!keystorePath.isNullOrBlank()) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
                keyAlias = System.getenv("KEY_ALIAS") ?: ""
                keyPassword = System.getenv("KEY_PASSWORD") ?: ""
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Sign only when credentials are present (i.e. in CI).
            if (!keystorePath.isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
}
```

Credentials are read from environment variables — injected from Secrets in CI, or `export`ed locally.

> **Why the `isNullOrBlank()` guard matters.** Writing `file(System.getenv("KEYSTORE_PATH") ?: "")` throws
> `path may not be null or empty string` at *configuration* time whenever the variables are unset — which
> breaks debug builds, IDE sync, and even `gradle wrapper`. Always guard it as shown above.

> Also add `release.keystore` to `.gitignore` so it can't be committed by accident.

## Step 3 — Encode the keystore to base64

```bash
chmod +x scripts/encode-keystore.sh
./scripts/encode-keystore.sh /path/to/release.keystore
```

Copy the long base64 string it prints.

## Step 4 — Configure GitHub Secrets

Repo → **Settings → Secrets and variables → Actions → New repository secret**, add each one:

| Secret name | Value |
|---|---|
| `KEYSTORE_BASE64` | base64 string from Step 3 |
| `KEYSTORE_PASSWORD` | keystore password |
| `KEY_ALIAS` | key alias |
| `KEY_PASSWORD` | key password |

## Step 5 — Commit and trigger the build

```bash
git add .github/workflows/build-release-apk.yml app/build.gradle.kts scripts/encode-keystore.sh
git commit -m "ci: build signed release APK on GitHub Actions"
git push
```

Head to the **Actions** tab to watch it run. On success:

- **Any push/PR** → produces a `release-apk` build artifact, downloadable from that run's page.
- **Pushing a tag** (e.g. `git tag v1.0.0 && git push --tags`) → additionally auto-creates a GitHub Release with the APK attached.

## Local verification (optional)

Reproduce the CI behavior before pushing:

```bash
export KEYSTORE_PATH=$PWD/app/release.keystore
export KEYSTORE_PASSWORD=...
export KEY_ALIAS=...
export KEY_PASSWORD=...
./gradlew assembleRelease
```

Output lands at `app/build/outputs/apk/release/app-release.apk`.

---

## Troubleshooting

- **JDK version**: the workflow uses JDK 17 (required by AGP 8.x). On AGP 7.x, change `java-version: '17'` to `'11'`.
- **`gradlew` permission**: `chmod +x gradlew` already runs in the workflow; if you hit "permission denied" locally, run that command once.
- **minify errors**: `isMinifyEnabled` defaults to `false`. Before enabling R8, make sure your ProGuard keep rules are correct — otherwise the release build crashes.
- **Keystore mismatch**: switching to a new keystore changes the signature, so existing users can't install updates over the old version. Keep one keystore, backed up, forever.
- **Where's the output?**: CI run page → Artifacts section → `release-apk` → download and unzip for the `.apk`.
