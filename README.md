# PAX SDK Flutter Plugin

A Flutter plugin for PAX payment terminals providing NFC card reading and thermal printer functionality.

## Features

- **NFC Card Reading**: Detect and read NFC cards on PAX devices
- **Thermal Printing**: Print text and images with various formatting options
- **Advanced Printer Controls**: Font size, alignment, spacing, and more
- **Release Build Support**: Works properly with `flutter run --release`

## Installation

Add this to your package's `pubspec.yaml` file:

```yaml
dependencies:
  pax_sdk: ^1.0.0
```

## Usage

### Import the package

```dart
import 'package:pax_sdk/pax_sdk.dart';
```

### NFC Card Reading

```dart
// Check if a card is present
bool isPresent = await PaxSdk.checkCardPresence();

// Detect and read card details
Map<String, dynamic> cardResult = await PaxSdk.detectCard();
if (cardResult['success']) {
  print('Card UID: ${cardResult['cardData']['uid']}');
}
```

### Printer Functionality

```dart
// Initialize printer
bool initialized = await PaxSdk.initializePrinter();

// Print text
Map<String, dynamic> result = await PaxSdk.printText(
  'Hello World!',
  options: {
    'fontSize': 'large',
    'alignment': 1, // Center alignment
  },
);

// Print image
List<int> imageData = [...]; // Your image data
Map<String, dynamic> result = await PaxSdk.printImage(imageData);

// Get printer status
Map<String, dynamic> status = await PaxSdk.getPrinterStatus();
```

### Advanced Printer Controls

```dart
// Set font size
await PaxSdk.setFontSize('large');

// Set double height and width
await PaxSdk.setDoubleHeight(isAscDouble: true, isLocalDouble: true);
await PaxSdk.setDoubleWidth(isAscDouble: true, isLocalDouble: true);

// Set spacing
await PaxSdk.setSpacing(wordSpace: 2, lineSpace: 4);

// Cut paper
await PaxSdk.cutPaper(mode: 0);
```

## API Reference

### NFC Methods

- `checkCardPresence()` - Check if an NFC card is present
- `detectCard()` - Detect and read card information
- `waitForCard()` - Wait for a card to be placed
- `tryAllModes()` - Test all NFC detection modes

### Printer Methods

- `initializePrinter()` - Initialize the printer
- `printText(text, options)` - Print text with formatting options
- `printImage(imageData, options)` - Print an image
- `getPrinterStatus()` - Get current printer status
- `cutPaper(mode)` - Cut paper
- `feedPaper(pixels)` - Feed paper by specified pixels

### Advanced Printer Methods

- `setFontSize(size)` - Set font size (small, medium, large, extra_large)
- `setDoubleHeight(isAscDouble, isLocalDouble)` - Set double height printing
- `setDoubleWidth(isAscDouble, isLocalDouble)` - Set double width printing
- `setSpacing(wordSpace, lineSpace)` - Set character and line spacing
- `setLeftIndent(indent)` - Set left indentation
- `setInvert(isInvert)` - Set invert printing
- `presetCutPaper(mode)` - Preset cut paper mode

## Requirements

- Android API level 21 or higher
- PAX payment terminal device
- PAX SDK libraries (included in the package)

## Tested on

- **PAX A920 Pro**
- **PAX A960**

## Permissions

The following permissions are automatically added to your Android app:

```xml
<uses-permission android:name="com.pax.permission.PICC" />
<uses-permission android:name="com.pax.permission.PRINTER" />
```

## Example

```dart
import 'package:flutter/material.dart';
import 'package:pax_sdk/pax_sdk.dart';

class PaxSdkExample extends StatefulWidget {
  @override
  _PaxSdkExampleState createState() => _PaxSdkExampleState();
}

class _PaxSdkExampleState extends State<PaxSdkExample> {
  String _status = 'Ready';

  Future<void> _testNfc() async {
    setState(() => _status = 'Testing NFC...');
    
    try {
      final cardResult = await PaxSdk.detectCard();
      setState(() {
        _status = cardResult['success'] 
          ? 'Card detected: ${cardResult['cardData']['uid']}'
          : 'No card detected';
      });
    } catch (e) {
      setState(() => _status = 'Error: $e');
    }
  }

  Future<void> _testPrinter() async {
    setState(() => _status = 'Testing printer...');
    
    try {
      final result = await PaxSdk.printText(
        'PAX SDK Test\n${DateTime.now()}',
        options: {'fontSize': 'large', 'alignment': 1},
      );
      setState(() {
        _status = result['success'] ? 'Print successful' : 'Print failed';
      });
    } catch (e) {
      setState(() => _status = 'Error: $e');
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('PAX SDK Example')),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text(_status),
            SizedBox(height: 20),
            ElevatedButton(
              onPressed: _testNfc,
              child: Text('Test NFC'),
            ),
            SizedBox(height: 10),
            ElevatedButton(
              onPressed: _testPrinter,
              child: Text('Test Printer'),
            ),
          ],
        ),
      ),
    );
  }
}
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Repository

This package is available on GitHub: [https://github.com/Fuelin-net/pax_sdk](https://github.com/Fuelin-net/pax_sdk)
