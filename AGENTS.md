# Agents Guidelines

- This repository uses Kotlin Multiplatform principles for the crypto module.
- Always run `./gradlew test lint` before submitting code.
- Be careful with `InputMethodService` configurations; ensure `method.xml` is present and referenced.
- When generating release builds, unsigned APKs are acceptable for CI testing purposes.
