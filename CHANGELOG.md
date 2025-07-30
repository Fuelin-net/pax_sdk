# Changelog

## [1.0.3] - 2025-07-30

### Fixed
- **Android Compilation Issues**: Resolved all Java compilation errors in the Android plugin
  - Fixed missing Flutter SDK dependencies in Android build configuration
  - Added proper Flutter embedding dependencies from local Flutter installation
  - Resolved `@NonNull` annotation import issues
  - Fixed Java version compatibility (upgraded to Java 17)
- **Build System**: Updated Android Gradle configuration for proper plugin compilation
  - Added Flutter SDK path configuration in gradle.properties
  - Configured Java 17 for Android Gradle plugin compatibility
  - Added AndroidX annotation dependencies
  - Suppressed compileSdk warnings for better build experience

### Technical
- Updated `android/build.gradle` with proper Flutter SDK dependencies
- Updated `android/gradle.properties` with Java 17 and Flutter SDK paths
- Enhanced build configuration for better compatibility with modern Flutter versions
- Improved plugin registration and method channel handling

### Testing
- ✅ Android library builds successfully without compilation errors
- ✅ Example app builds and runs successfully on PAX A920 Pro device
- ✅ Flutter plugin classes properly resolved and accessible
- ✅ All method channels and plugin interfaces working correctly

## [1.0.2] - 2024-01-XX

### Added
- **JAR Files**: PAX SDK JAR files are now properly included in the package distribution
  - `neptune-lite-api-v3.26.00-20210903.jar` (604KB) - Main PAX Neptune Lite API
  - `sdk.jar` (102KB) - PAX SDK core library
- **Package Distribution**: JAR files are now tracked in version control and included in package releases
- **Improved Installation**: Users can now install the package with all required PAX SDK dependencies

### Technical
- Removed JAR files from `.gitignore` to ensure they are included in package distribution
- Updated package structure to include essential PAX SDK libraries
- Enhanced package validation and distribution process

## [1.0.1] - 2024-01-XX

### Added
- Support for PAX A920 Pro and PAX A960 devices
- Improved error handling for device compatibility
- Enhanced documentation with tested devices list

## [1.0.0] - 2024-01-XX

### Added
- Initial release of PAX SDK Flutter plugin
- NFC card reading functionality
- Thermal printer support with text and image printing
- Advanced printer controls (font size, alignment, spacing, etc.)
- Release build support
- Comprehensive error handling and debugging tools
- ProGuard rules for release builds
- PAX-specific permissions handling

### Features
- `PaxSdk.checkCardPresence()` - Check if NFC card is present
- `PaxSdk.detectCard()` - Detect and read card information
- `PaxSdk.initializePrinter()` - Initialize printer
- `PaxSdk.printText()` - Print text with formatting options
- `PaxSdk.printImage()` - Print images
- `PaxSdk.getPrinterStatus()` - Get printer status
- `PaxSdk.cutPaper()` - Cut paper
- `PaxSdk.feedPaper()` - Feed paper
- Advanced printer controls (font size, double height/width, spacing, etc.)

### Technical Details
- Supports Android API level 21+
- Includes PAX SDK native libraries
- Proper plugin registration with Flutter
- Optimized for PAX payment terminals 