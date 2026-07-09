# Security Threat Model

## What StealthCrypt Protects
- **Message Content**: Uses Argon2id for Key Derivation and XChaCha20-Poly1305 for authenticated encryption. Text is encrypted before it hits the network.
- **Local Keys**: Keys are stored in the Android Keystore using `EncryptedSharedPreferences` backed by `MasterKey`.

## What StealthCrypt DOES NOT Protect
- **Metadata**: The chat provider (e.g., WhatsApp) still knows *who* you are talking to, *when* you are talking, and the *size* of the messages.
- **Chat App Terms of Service**: Sending large amounts of base64 ciphertext might violate some chat platforms' terms of service or trigger spam filters. Use at your own risk.
- **Screen Reading**: If your device is compromised, malware can read the screen or the custom keyboard inputs.

## Important Note
This is a proof of concept. The shared password model is a basic Minimum Viable Product (MVP). Future versions may include X25519 handshakes for perfect forward secrecy.
