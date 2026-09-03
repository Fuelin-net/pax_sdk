# Claude Code

@AGENTS.md

- Prefer Plan mode before large Android/plugin API changes.
- Use FVM for every Flutter/Dart command (`fvm flutter`, `fvm dart`).
- When adding Neptune Lite APIs, verify method signatures against `doc.zip` (or extracted Javadoc) before coding.
- After editing `android/src/main/java/.../paxSDK.java`, sync the duplicate copies under `android/app/` and `example/android/app/` if they still exist.
- Do not publish to pub.dev or push remotes unless explicitly asked.
