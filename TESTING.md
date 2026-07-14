# Testing Guide

## Automated Tests
- The `:crypto` module has pure Kotlin unit tests for encryption/decryption, round-tripping, and tampering.
- Run them via: `./gradlew test`

## Manual End-to-End Testing (Smoke Test)

1. **Install on Device A**: Install the debug or release APK on an Android device or emulator.
2. **Install on Device B**: Install the APK on a second device.
3. **Setup Keys**: Open the app on Device A. Generate a random key. Type the exact same key into Device B (or copy-paste via a secure channel if testing locally).
4. **Enable Keyboard**: On Device A, go to Settings -> System -> Languages & input -> Virtual keyboard -> Manage keyboards. Enable **StealthCrypt Keyboard**.
5. **Enable Notification Listener**: On Device B, open StealthCrypt and click "Enable Notification Listener" to grant the permission.
6. **Send Message**: On Device A, open WhatsApp (or an SMS app if using emulators). Switch the keyboard to StealthCrypt. Ensure the lock icon shows `🔒` (Encryption enabled). Type "Secret test message" and hit **Send**. The text field will populate with `SC...` base64 ciphertext. Send it.
7. **Receive Message**: On Device B, receive the WhatsApp/SMS message. If the Notification Listener is working, you should see a local system notification popping up saying `🔓 Decrypted Message: Secret test message`.
8. **Manual Decryption**: On Device B, long-press the ciphertext in the chat, select "Share", and choose **StealthCrypt**. A dialog should appear showing the decrypted text.
