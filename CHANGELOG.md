# Changelog

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