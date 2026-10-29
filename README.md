# Vyrup - Premium Minimalist Expense Tracker

Vyrup is a premium, minimalist offline-first expense tracker designed for the Indian context. It allows users to record expenses in under 5 seconds, track monthly spending against previous months, and manage split bills easily.

## Features
- **Lightning Fast Quick Add:** Record expenses with a custom keypad, voice entry, and smart category suggestions.
- **Offline First:** All your data stays securely on your device. Powered by Room Database. No cloud sync, no accounts.
- **Split Bills:** Mathematically flawless split bill tracking. Support for equal, exact, percentage, and shares allocation.
- **UPI Integration:** Launch your favorite UPI app directly from Vyrup to settle up.
- **Insightful Dashboard:** Smooth curve charts comparing this month's pace against the last.
- **CSV Export & Import:** Complete control over your data.

## Build Instructions
This project uses Jetpack Compose, Hilt, and Room.
To build the app:
1. Open the project in Android Studio.
2. Let Gradle sync.
3. Build the project using `Build > Make Project` or by running `./gradlew assembleDebug` in the terminal.

## Permissions & Privacy
- **No INTERNET Permission:** Vyrup does not have internet access. It is completely offline.
- Speech recognition leverages the Android System's `RecognizerIntent`, which may process data via Google's servers.

## License
MIT License. 
(Note: Replace placeholder fonts with appropriately licensed typefaces before commercial distribution).
