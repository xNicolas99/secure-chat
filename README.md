# StealthCrypt

StealthCrypt is a proof-of-concept Android application that provides end-to-end encryption for any chat application (like WhatsApp) via a custom keyboard (IME). It encrypts text *before* it leaves your device, so the chat provider only ever sees encrypted ciphertexts.

## Features

- **Custom Keyboard**: Samsung-style dark keyboard (German QWERTZ with umlauts, number row, symbols page) that encrypts your text on-the-fly and sends it as base64-encoded ciphertext. A toggle switches between encrypted and plain typing.
- **Emoji Picker**: Full emoji panel (😊 key) with categories, recently-used list and skin-tone variants via long-press. Emoji glyphs come from the system font, so on Samsung devices they look exactly like the Samsung keyboard's emojis.
- **Works with any keyboard**: Prefer the Samsung or Google keyboard? Type normally, select the text and choose **🔒 Encrypt** / **🔓 Decrypt** from Android's text-selection menu. In editable fields the selection is replaced in place; in read-only contexts (e.g. a received chat bubble) the result is shown in a dialog with a copy button.
- **Auto-Decryption**: A Notification Listener service automatically decrypts incoming messages from any messenger (WhatsApp, Signal, Telegram, SMS, ...) if you hold the correct shared key. It reads plain, expanded and MessagingStyle notification texts and uses a cheap envelope pre-check so normal messages cost no CPU.
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
3. Enable the **Notification Listener** in your Android Settings — incoming encrypted messages are then decrypted automatically in a local notification.

Then send messages either way:

**With the StealthCrypt keyboard (fully automatic):**
1. Enable the **StealthCrypt Keyboard** in your Android Settings.
2. Open a chat app (e.g. WhatsApp), switch to the StealthCrypt keyboard, type a message, and press ↵. The ciphertext is inserted into the text field — hit the app's send button.

**With your own keyboard (Samsung, Gboard, ...):**
1. Type your message normally.
2. Select the text and choose **🔒 Encrypt** from the selection menu — the text is replaced with ciphertext.
3. To read a received message without the Notification Listener: select it and choose **🔓 Decrypt**.

## Notice for Play Store

The `play` flavor is designed to be store-compliant. Note that custom keyboards and notification listeners are heavily scrutinized by Google Play Protect. We recommend the F-Droid/Sideload route for this type of privacy app.
