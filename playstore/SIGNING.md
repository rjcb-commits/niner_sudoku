# Release signing setup

Niner stores the release keystore at `app/release.jks` and reads the passwords from `local.properties` (gitignored by default). Same pattern as StupidSmall so the muscle memory carries over.

## 1. Generate the release keystore

Run this once. **Save the keystore file and passwords somewhere safe** (1Password, etc.) — losing either means you can never publish updates to the same Play Store listing.

```bash
keytool -genkey -v \
  -keystore /Users/rjcb/SudokuApp/app/release.jks \
  -alias niner \
  -keyalg RSA -keysize 2048 \
  -validity 10000
```

You'll be prompted for:
- **Keystore password** — pick one and store it safely
- **Key password** — can be the same as the keystore password (simplest)
- Name / org / city / etc. — visible in signed APKs but don't matter functionally

## 2. Fill in `local.properties`

`local.properties` lives at the repo root and is gitignored. It already has the release keys stubbed — just fill in the passwords:

```properties
sdk.dir=/Users/rjcb/Android/sdk

# Release signing
RELEASE_STORE_FILE=release.jks
RELEASE_STORE_PASSWORD=<the keystore password you chose>
RELEASE_KEY_ALIAS=niner
RELEASE_KEY_PASSWORD=<the key password you chose>
```

`RELEASE_STORE_FILE` is resolved relative to the `app/` module, so `release.jks` maps to `app/release.jks`.

The `*.jks` entry in [.gitignore](.gitignore) keeps the keystore file out of git; the `local.properties` entry keeps the passwords out of git.

## 3. Build a signed release

```bash
cd /Users/rjcb/SudokuApp
./gradlew bundleRelease
# → app/build/outputs/bundle/release/app-release.aab   (what you upload to Play)

./gradlew assembleRelease
# → app/build/outputs/apk/release/app-release.apk      (handy for sideload testing)
```

Without the keystore + passwords, the release build will fail with a clear "Keystore file not set for signing config" error. That's a feature — it stops you from accidentally uploading an unsigned artefact.

## 4. Verify the signature

```bash
$ANDROID_HOME/build-tools/*/apksigner verify --print-certs \
  app/build/outputs/apk/release/app-release.apk | head
```

## 5. Upload to Play Console

Use the `.aab` file — Play Store prefers it over `.apk` because it lets Google generate per-device optimised APKs.

## First-time Play Console checklist

- Create app listing, category "Games / Puzzle"
- Upload [playstore/app_icon_512.png](app_icon_512.png) (512×512, no alpha)
- Upload [playstore/feature_graphic_1024x500.png](feature_graphic_1024x500.png) (1024×500)
- Upload phone screenshots from [playstore/screenshots/](screenshots/) (at least 2, ideally 4+)
- Privacy policy URL: `https://niner-privacy.pages.dev/privacy` (already live)
- Content rating questionnaire: pick "Everyone"
- Target audience: "Ages 13+"
- Data safety form: "No data collected, no data shared"
- Pricing: free
- Countries: all (or start with your own for a soft launch)

## If you lose the keystore

You can't update the app. You'd have to publish under a new package name and ask existing users to reinstall. Backup tips:
- Store `app/release.jks` in 1Password as an attached file
- Store the two passwords as separate secure notes
- Email yourself a copy to a non-primary address as a last-resort backup
