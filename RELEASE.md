# Release Engineering

To build a signed release for the Play Store or Sideloading:

1. Generate a keystore if you don't have one:
   ```bash
   keytool -genkey -v -keystore stealthcrypt-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias stealthcrypt
   ```
2. Do **not** commit `stealthcrypt-release-key.jks` to the repository.

3. Build the release APKs:
   ```bash
   ./gradlew assembleRelease
   ```
   (This builds both the `play` and `sideload` product flavors).

4. To sign the APKs manually (if not configured in `build.gradle`):
   ```bash
   apksigner sign --ks stealthcrypt-release-key.jks app/build/outputs/apk/play/release/app-play-release-unsigned.apk
   apksigner sign --ks stealthcrypt-release-key.jks app/build/outputs/apk/sideload/release/app-sideload-release-unsigned.apk
   ```

5. The resulting signed APKs can be distributed to users or uploaded to the Google Play Console.
