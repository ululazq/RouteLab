# Build a Signed Release APK on GitHub Actions

Push code → GitHub builds → you get an installable signed APK. Pushing a version tag also auto-creates a GitHub Release with the APK attached.

---

## ✅ Already done for you (verified in this workspace)

| Item | Status |
|---|---|
| Android project scaffold | Kotlin + Gradle KTS, AGP 8.5.2, Gradle 8.7, compileSdk 34 |
| Signing keystore | generated at `app/release.keystore` |
| Local release build | **passed** — `app-release.apk` (4.5 MB) |
| Signature verified | `apksigner` → v2 scheme, cert `CN=MyApp` |
| Git repo | initialized, first commit made |

## Signing credentials for THIS project

| Field | Value |
|---|---|
| Keystore | `app/release.keystore` (gitignored) |
| `KEY_ALIAS` | `my-key-alias` |
| `KEYSTORE_PASSWORD` | `MyApp2024!` |
| `KEY_PASSWORD` | `MyApp2024!` |

> ⚠️ **These are demo credentials** I generated so the pipeline works out of the box. Before publishing a real app, regenerate your own keystore with a strong password (Step 1 below) — and back up the file. Lose it and you can never ship an update for that app.

---

## Step 1 — (Optional) Regenerate the keystore

```bash
keytool -genkeypair -v \
  -keystore app/release.keystore \
  -alias my-key-alias \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass '<strong-password>' -keypass '<strong-password>' \
  -dname "CN=YourName, OU=Dev, O=YourCompany, L=City, ST=State, C=US"
```

## Step 2 — Create the GitHub repo and push

I can't push for you (no GitHub auth in this environment), so run one of these:

**With GitHub CLI** (creates the repo *and* pushes):
```bash
gh auth login
gh repo create my-android-app --private --source=. --push
```

**Manually** — create an empty repo on github.com, then:
```bash
git remote add origin git@github.com:<you>/<repo>.git
git push -u origin main
```

## Step 3 — Add the 4 Secrets

Encode the keystore (never commit it — it's already in `.gitignore`):
```bash
./scripts/encode-keystore.sh app/release.keystore > /tmp/keystore.b64
```

**Via CLI:**
```bash
gh secret set KEYSTORE_BASE64   < /tmp/keystore.b64
gh secret set KEYSTORE_PASSWORD -b "MyApp2024!"
gh secret set KEY_ALIAS         -b "my-key-alias"
gh secret set KEY_PASSWORD      -b "MyApp2024!"
rm /tmp/keystore.b64      # clean up the plaintext base64
```

**Or via UI:** Repo → Settings → Secrets and variables → Actions → New repository secret, adding each of the four names above.

## Step 4 — Trigger the build

```bash
git push                                    # normal build
git tag v1.0.0 && git push --tags           # also creates a GitHub Release
```

Then check the **Actions** tab. Artifacts → `release-apk` → download for the `.apk`.

---

## How the signing config works

Credentials are read from environment variables — injected from Secrets in CI, or `export`ed locally:

```kotlin
android {
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

> **Why the `isNullOrBlank()` guard matters.** Writing `file(System.getenv("KEYSTORE_PATH") ?: "")` throws
> `path may not be null or empty string` at *configuration* time whenever the variables are unset — which
> breaks debug builds, IDE sync, and even `gradle wrapper`. This exact bug was hit and fixed during setup.

## Local build (reproduce CI)

```bash
export ANDROID_HOME=$HOME/Android/Sdk      # or wherever your SDK lives
export KEYSTORE_PATH=$PWD/app/release.keystore
export KEYSTORE_PASSWORD='MyApp2024!'
export KEY_ALIAS=my-key-alias
export KEY_PASSWORD='MyApp2024!'
./gradlew assembleRelease
# -> app/build/outputs/apk/release/app-release.apk
```

Verify the signature:
```bash
$ANDROID_HOME/build-tools/34.0.0/apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
```

---

## Troubleshooting

- **JDK version**: workflow uses JDK 17 (required by AGP 8.x). On AGP 7.x, change `java-version: '17'` to `'11'`.
- **`gradlew` permission**: `chmod +x gradlew` runs in the workflow; locally, run it once if you get "permission denied".
- **minify errors**: `isMinifyEnabled` is `false`. Before enabling R8, make sure your ProGuard keep rules are correct — otherwise the release build crashes.
- **Keystore mismatch**: a new keystore changes the signature, so existing users can't install updates over the old version. Keep one keystore, backed up, forever.
- **Where's the output?**: CI run page → Artifacts → `release-apk` → download and unzip for the `.apk`.
