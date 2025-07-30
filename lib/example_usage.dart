import 'pax_sdk.dart';

/// Example usage of PAX SDK method channels
class PaxSdkExamples {
  /// Example: Basic NFC card detection
  static Future<void> detectNfcCard() async {
    print('=== NFC Card Detection Example ===');

    // Check if card is present
    final isPresent = await PaxSdk.checkCardPresence();
    print('Card present: $isPresent');

    if (isPresent) {
      // Detect and analyze card
      final result = await PaxSdk.detectCard();

      if (result['success']) {
        final cardData = result['cardData'];
        final manufacturerData = result['manufacturerData'];

        print('Card detected successfully!');
        print('UID: ${cardData['uid']}');
        print('Card type: ${cardData['cardType']['cardType']}');
        print('Manufacturer: ${cardData['cardType']['manufacturer']}');

        if (manufacturerData['block0Data'] != null) {
          print('Manufacturer block: ${manufacturerData['block0Data']}');
        }
      } else {
        print('Card detection failed: ${result['error']}');
      }
    } else {
      print('No card present');
    }
  }

  /// Example: Basic printing
  static Future<void> printBasicReceipt() async {
    print('=== Basic Printing Example ===');

    // Initialize printer
    final initialized = await PaxSdk.initializePrinter();
    if (!initialized) {
      print('Failed to initialize printer');
      return;
    }

    // Check printer status
    final status = await PaxSdk.getPrinterStatus();
    if (!status['success']) {
      print('Printer error: ${status['error']}');
      return;
    }

    print('Printer status: ${status['statusMessage']}');

    // Print receipt
    final printResult = await PaxSdk.printText(
      '=== RECEIPT ===\n\n'
      'Date: ${DateTime.now()}\n'
      'Time: ${DateTime.now().toLocal()}\n\n'
      'Item 1: \$10.00\n'
      'Item 2: \$15.50\n'
      'Item 3: \$5.25\n\n'
      'Total: \$30.75\n\n'
      'Thank you!\n'
      '===============\n',
      options: {
        'fontSize': 'medium',
        'alignment': 1, // Center
      },
    );

    if (printResult['success']) {
      print('Print successful!');

      // Feed paper
      await PaxSdk.feedPaper(pixels: 48);

      // Cut paper if supported
      final cutSupported = await PaxSdk.isCutSupported();
      if (cutSupported) {
        await PaxSdk.cutPaper(mode: 0);
        print('Paper cut');
      }
    } else {
      print('Print failed: ${printResult['error']}');
    }
  }

  /// Example: Advanced printing with formatting
  static Future<void> printAdvancedReceipt() async {
    print('=== Advanced Printing Example ===');

    // Initialize printer
    final initialized = await PaxSdk.initializePrinter();
    if (!initialized) {
      print('Failed to initialize printer');
      return;
    }

    // Set advanced formatting
    await PaxSdk.setFontSize('large');
    await PaxSdk.setDoubleHeight(isAscDouble: true, isLocalDouble: true);
    await PaxSdk.setDoubleWidth(isAscDouble: true, isLocalDouble: true);
    await PaxSdk.setSpacing(wordSpace: 2, lineSpace: 4);

    // Print header
    await PaxSdk.printText(
      'COMPANY NAME\n',
      options: {
        'fontSize': 'large',
        'alignment': 1, // Center
      },
    );

    // Reset to normal size
    await PaxSdk.setFontSize('medium');
    await PaxSdk.setDoubleHeight(isAscDouble: false, isLocalDouble: false);
    await PaxSdk.setDoubleWidth(isAscDouble: false, isLocalDouble: false);

    // Print receipt content
    await PaxSdk.printText(
      'Receipt #: 12345\n'
      'Date: ${DateTime.now().toString().split(' ')[0]}\n'
      'Time: ${DateTime.now().toString().split(' ')[1].split('.')[0]}\n\n'
      'Items:\n'
      '  Coffee     \$3.50\n'
      '  Sandwich   \$8.75\n'
      '  Cookie     \$2.25\n\n'
      'Subtotal:   \$14.50\n'
      'Tax:        \$1.45\n'
      'Total:      \$15.95\n\n'
      'Payment: Credit Card\n'
      'Card: **** **** **** 1234\n\n'
      'Thank you for your purchase!\n'
      'Please come again.\n\n'
      '=====================\n',
      options: {
        'fontSize': 'medium',
        'alignment': 0, // Left
      },
    );

    // Feed and cut
    await PaxSdk.feedPaper(pixels: 48);

    final cutSupported = await PaxSdk.isCutSupported();
    if (cutSupported) {
      await PaxSdk.cutPaper(mode: 0);
    }

    print('Advanced receipt printed successfully!');
  }

  /// Example: Print image
  static Future<void> printImageExample() async {
    print('=== Image Printing Example ===');

    // Initialize printer
    final initialized = await PaxSdk.initializePrinter();
    if (!initialized) {
      print('Failed to initialize printer');
      return;
    }

    // Create a simple test image (black and white)
    // In a real app, you would load an actual image
    final imageData =
        List<int>.filled(384 * 200 ~/ 8, 0); // Simple black rectangle

    // Print the image
    final result = await PaxSdk.printImage(
      imageData,
      options: {
        'alignment': 1, // Center
      },
    );

    if (result['success']) {
      print('Image printed successfully!');
      await PaxSdk.feedPaper(pixels: 48);
    } else {
      print('Image print failed: ${result['error']}');
    }
  }

  /// Example: Test all printer features
  static Future<void> testAllPrinterFeatures() async {
    print('=== Testing All Printer Features ===');

    // Initialize
    final initialized = await PaxSdk.initializePrinter();
    print('Initialized: $initialized');

    // Get status
    final status = await PaxSdk.getPrinterStatus();
    print('Status: ${status['statusMessage']}');

    // Test font sizes
    for (final fontSize in ['small', 'medium', 'large', 'extra_large']) {
      await PaxSdk.setFontSize(fontSize);
      await PaxSdk.printText('Font size: $fontSize\n');
    }

    // Test alignments
    final alignments = ['Left', 'Center', 'Right'];
    for (int i = 0; i < alignments.length; i++) {
      await PaxSdk.printText(
        'Alignment: ${alignments[i]}\n',
        options: {'alignment': i},
      );
    }

    // Test double height/width
    await PaxSdk.setDoubleHeight(isAscDouble: true, isLocalDouble: true);
    await PaxSdk.printText('Double Height Text\n');

    await PaxSdk.setDoubleWidth(isAscDouble: true, isLocalDouble: true);
    await PaxSdk.printText('Double Width Text\n');

    // Reset to normal
    await PaxSdk.setDoubleHeight(isAscDouble: false, isLocalDouble: false);
    await PaxSdk.setDoubleWidth(isAscDouble: false, isLocalDouble: false);

    // Test spacing
    await PaxSdk.setSpacing(wordSpace: 3, lineSpace: 6);
    await PaxSdk.printText('Text with custom spacing\n');

    // Test invert
    await PaxSdk.setInvert(true);
    await PaxSdk.printText('Inverted Text\n');
    await PaxSdk.setInvert(false);

    // Feed and cut
    await PaxSdk.feedPaper(pixels: 48);

    final cutSupported = await PaxSdk.isCutSupported();
    if (cutSupported) {
      await PaxSdk.cutPaper(mode: 0);
    }

    print('All printer features tested!');
  }

  /// Example: Comprehensive NFC testing
  static Future<void> testAllNfcFeatures() async {
    print('=== Testing All NFC Features ===');

    // Check card presence
    final isPresent = await PaxSdk.checkCardPresence();
    print('Card present: $isPresent');

    if (isPresent) {
      // Detect card
      final result = await PaxSdk.detectCard();

      if (result['success']) {
        final cardData = result['cardData'];
        final manufacturerData = result['manufacturerData'];

        print('\n=== Card Information ===');
        print('UID: ${cardData['uid']}');
        print('UID Length: ${cardData['uidLength']} bytes');

        if (cardData['cardType'] != null) {
          final cardType = cardData['cardType'];
          print('Card Type: ${cardType['cardType']}');
          print('Manufacturer: ${cardType['manufacturer']}');
          print('First Byte: ${cardType['firstByte']}');
        }

        if (manufacturerData['block0Data'] != null) {
          print('\n=== Manufacturer Block ===');
          print('Block 0 Data: ${manufacturerData['block0Data']}');

          if (manufacturerData['parsedData'] != null) {
            final parsed = manufacturerData['parsedData'];
            print('Embedded UID: ${parsed['embeddedUid']}');
            print('BCC: ${parsed['bcc']}');
            if (parsed['sak'] != null) {
              print('SAK: ${parsed['sak']}');
            }
            if (parsed['atqa'] != null) {
              print('ATQA: ${parsed['atqa']}');
            }
          }
        }

        if (manufacturerData['authResult'] != null) {
          final authResult = manufacturerData['authResult'];
          if (authResult['success']) {
            print('\n=== Authentication Result ===');
            print('Key Index: ${authResult['keyIndex']}');
            print('Data: ${authResult['data']}');
          }
        }
      } else {
        print('Card detection failed: ${result['error']}');
      }
    } else {
      print('No card present for testing');
    }

    // Test all detection modes
    print('\n=== Testing All Detection Modes ===');
    await PaxSdk.tryAllModes();
  }
}

/// Usage examples
void main() async {
  print('PAX SDK Examples');
  print('================\n');

  // Uncomment the examples you want to test:

  // await PaxSdkExamples.detectNfcCard();
  // await PaxSdkExamples.printBasicReceipt();
  // await PaxSdkExamples.printAdvancedReceipt();
  // await PaxSdkExamples.printImageExample();
  // await PaxSdkExamples.testAllPrinterFeatures();
  // await PaxSdkExamples.testAllNfcFeatures();

  print('\nExamples completed!');
}
