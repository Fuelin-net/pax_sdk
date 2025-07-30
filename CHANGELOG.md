# Changelog

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