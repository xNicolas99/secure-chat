# StealthCrypt

StealthCrypt is a proof-of-concept Android application that provides end-to-end encryption for any chat application (like WhatsApp) via a custom keyboard (IME). It encrypts text *before* it leaves your device, so the chat provider only ever sees encrypted ciphertexts.

## Features

- **Custom Keyboard**: Encrypts your text on-the-fly and sends it as base64-encoded ciphertext.
- **Auto-Decryption**: A Notification Listener service automatically decrypts incoming messages if you hold the correct shared key.
- **Manual Decryption**: Fallback decryption by sharing text from any app to StealthCrypt.
- **Local Key Storage**: Uses Android Keystore (`MasterKey` and `EncryptedSharedPreferences`) to store your shared key securely.

## Installation

StealthCrypt provides two flavors:
1. **Play Store Flavor (`play`)**: Conforms to standard Play Store rules.
2. **Sideload Flavor (`sideload`)**: Contains accessibility services and more aggressive notification reading (coming soon/optional). Currently identical except for flavor name.

### Sideload Installation

1. Go to the GitHub Actions page for this repo.
2. Download the `sideload-release-apk` artifact.
3. Install the APK manually on your Android device (ensure "Install from Unknown Sources" is enabled).

## Usage

1. Open **StealthCrypt** and generate a shared key, or type one in.
2. Scan the provided QR code with a friend's StealthCrypt app (or manually share the generated key securely).
3. Enable the **StealthCrypt Keyboard** in your Android Settings.
4. Open a chat app (e.g. WhatsApp), switch to the StealthCrypt keyboard, type a message, and press Send. The message will be replaced with ciphertext.
5. If the **Notification Listener** is enabled, incoming messages starting with the right structure will be automatically decrypted in a local overlay notification.

## Notice for Play Store

The `play` flavor is designed to be store-compliant. Note that custom keyboards and notification listeners are heavily scrutinized by Google Play Protect. We recommend the F-Droid/Sideload route for this type of privacy app.
