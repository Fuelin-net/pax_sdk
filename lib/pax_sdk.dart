import 'package:flutter/services.dart';

class PaxSdk {
  static const MethodChannel _channel = MethodChannel('pax_sdk');

  // ============ NFC METHODS ============

  /// Detect and identify NFC card
  static Future<Map<String, dynamic>> detectCard() async {
    try {
      final result = await _channel.invokeMethod('detectCard');
      return Map<String, dynamic>.from(result);
    } on PlatformException catch (e) {
      return {
        'success': false,
        'error': 'Platform error: ${e.message}',
        'code': e.code,
      };
    } catch (e) {
      return {
        'success': false,
        'error': 'Unexpected error: $e',
      };
    }
  }

  /// Check if a card is present
  static Future<bool> checkCardPresence() async {
    try {
      final result = await _channel.invokeMethod('checkCardPresence');
      return result as bool;
    } on PlatformException catch (e) {
      print('Platform error checking card presence: ${e.message}');
      return false;
    } catch (e) {
      print('Error checking card presence: $e');
      return false;
    }
  }

  /// Wait for card and process when detected
  static Future<String> waitForCard() async {
    try {
      final result = await _channel.invokeMethod('waitForCard');
      return result as String;
    } on PlatformException catch (e) {
      return 'Platform error: ${e.message}';
    } catch (e) {
      return 'Error: $e';
    }
  }

  /// Test all detection modes
  static Future<String> tryAllModes() async {
    try {
      final result = await _channel.invokeMethod('tryAllModes');
      return result as String;
    } on PlatformException catch (e) {
      return 'Platform error: ${e.message}';
    } catch (e) {
      return 'Error: $e';
    }
  }

  // ============ PRINTER METHODS ============

  /// Initialize printer
  static Future<bool> initializePrinter() async {
    try {
      final result = await _channel.invokeMethod('initializePrinter');
      return result as bool;
    } on PlatformException catch (e) {
      print('Platform error initializing printer: ${e.message}');
      return false;
    } catch (e) {
      print('Error initializing printer: $e');
      return false;
    }
  }

  /// Print text with options
  static Future<Map<String, dynamic>> printText(
    String text, {
    Map<String, dynamic>? options,
  }) async {
    try {
      final result = await _channel.invokeMethod('printText', {
        'text': text,
        'options': options ?? {},
      });
      return Map<String, dynamic>.from(result);
    } on PlatformException catch (e) {
      return {
        'success': false,
        'error': 'Platform error: ${e.message}',
        'code': e.code,
      };
    } catch (e) {
      return {
        'success': false,
        'error': 'Unexpected error: $e',
      };
    }
  }

  /// Print image with options
  static Future<Map<String, dynamic>> printImage(
    List<int> imageData, {
    Map<String, dynamic>? options,
  }) async {
    try {
      final result = await _channel.invokeMethod('printImage', {
        'imageData': imageData,
        'options': options ?? {},
      });
      return Map<String, dynamic>.from(result);
    } on PlatformException catch (e) {
      return {
        'success': false,
        'error': 'Platform error: ${e.message}',
        'code': e.code,
      };
    } catch (e) {
      return {
        'success': false,
        'error': 'Unexpected error: $e',
      };
    }
  }

  /// Get printer status
  static Future<Map<String, dynamic>> getPrinterStatus() async {
    try {
      final result = await _channel.invokeMethod('getPrinterStatus');
      return Map<String, dynamic>.from(result);
    } on PlatformException catch (e) {
      return {
        'success': false,
        'error': 'Platform error: ${e.message}',
        'code': e.code,
      };
    } catch (e) {
      return {
        'success': false,
        'error': 'Unexpected error: $e',
      };
    }
  }

  /// Cut paper
  static Future<Map<String, dynamic>> cutPaper({int mode = 0}) async {
    try {
      final result = await _channel.invokeMethod('cutPaper', {
        'mode': mode,
      });
      return Map<String, dynamic>.from(result);
    } on PlatformException catch (e) {
      return {
        'success': false,
        'error': 'Platform error: ${e.message}',
        'code': e.code,
      };
    } catch (e) {
      return {
        'success': false,
        'error': 'Unexpected error: $e',
      };
    }
  }

  /// Feed paper
  static Future<Map<String, dynamic>> feedPaper({int pixels = 48}) async {
    try {
      final result = await _channel.invokeMethod('feedPaper', {
        'pixels': pixels,
      });
      return Map<String, dynamic>.from(result);
    } on PlatformException catch (e) {
      return {
        'success': false,
        'error': 'Platform error: ${e.message}',
        'code': e.code,
      };
    } catch (e) {
      return {
        'success': false,
        'error': 'Unexpected error: $e',
      };
    }
  }

  /// Check if cutting is supported
  static Future<bool> isCutSupported() async {
    try {
      final result = await _channel.invokeMethod('isCutSupported');
      return result as bool;
    } on PlatformException catch (e) {
      print('Platform error checking cut support: ${e.message}');
      return false;
    } catch (e) {
      print('Error checking cut support: $e');
      return false;
    }
  }

  // ============ ADVANCED PRINTER METHODS ============

  /// Set font size
  static Future<Map<String, dynamic>> setFontSize(String fontSize) async {
    try {
      final result = await _channel.invokeMethod('setFontSize', {
        'fontSize': fontSize,
      });
      return Map<String, dynamic>.from(result);
    } on PlatformException catch (e) {
      return {
        'success': false,
        'error': 'Platform error: ${e.message}',
        'code': e.code,
      };
    } catch (e) {
      return {
        'success': false,
        'error': 'Unexpected error: $e',
      };
    }
  }

  /// Set font path
  static Future<Map<String, dynamic>> setFontPath(String fontPath) async {
    try {
      final result = await _channel.invokeMethod('setFontPath', {
        'fontPath': fontPath,
      });
      return Map<String, dynamic>.from(result);
    } on PlatformException catch (e) {
      return {
        'success': false,
        'error': 'Platform error: ${e.message}',
        'code': e.code,
      };
    } catch (e) {
      return {
        'success': false,
        'error': 'Unexpected error: $e',
      };
    }
  }

  /// Set double height
  static Future<Map<String, dynamic>> setDoubleHeight({
    bool isAscDouble = true,
    bool isLocalDouble = true,
  }) async {
    try {
      final result = await _channel.invokeMethod('setDoubleHeight', {
        'isAscDouble': isAscDouble,
        'isLocalDouble': isLocalDouble,
      });
      return Map<String, dynamic>.from(result);
    } on PlatformException catch (e) {
      return {
        'success': false,
        'error': 'Platform error: ${e.message}',
        'code': e.code,
      };
    } catch (e) {
      return {
        'success': false,
        'error': 'Unexpected error: $e',
      };
    }
  }

  /// Set double width
  static Future<Map<String, dynamic>> setDoubleWidth({
    bool isAscDouble = true,
    bool isLocalDouble = true,
  }) async {
    try {
      final result = await _channel.invokeMethod('setDoubleWidth', {
        'isAscDouble': isAscDouble,
        'isLocalDouble': isLocalDouble,
      });
      return Map<String, dynamic>.from(result);
    } on PlatformException catch (e) {
      return {
        'success': false,
        'error': 'Platform error: ${e.message}',
        'code': e.code,
      };
    } catch (e) {
      return {
        'success': false,
        'error': 'Unexpected error: $e',
      };
    }
  }

  /// Set left indent
  static Future<Map<String, dynamic>> setLeftIndent(int indent) async {
    try {
      final result = await _channel.invokeMethod('setLeftIndent', {
        'indent': indent,
      });
      return Map<String, dynamic>.from(result);
    } on PlatformException catch (e) {
      return {
        'success': false,
        'error': 'Platform error: ${e.message}',
        'code': e.code,
      };
    } catch (e) {
      return {
        'success': false,
        'error': 'Unexpected error: $e',
      };
    }
  }

  /// Set invert
  static Future<Map<String, dynamic>> setInvert(bool isInvert) async {
    try {
      final result = await _channel.invokeMethod('setInvert', {
        'isInvert': isInvert,
      });
      return Map<String, dynamic>.from(result);
    } on PlatformException catch (e) {
      return {
        'success': false,
        'error': 'Platform error: ${e.message}',
        'code': e.code,
      };
    } catch (e) {
      return {
        'success': false,
        'error': 'Unexpected error: $e',
      };
    }
  }

  /// Set spacing
  static Future<Map<String, dynamic>> setSpacing({
    int wordSpace = 0,
    int lineSpace = 0,
  }) async {
    try {
      final result = await _channel.invokeMethod('setSpacing', {
        'wordSpace': wordSpace,
        'lineSpace': lineSpace,
      });
      return Map<String, dynamic>.from(result);
    } on PlatformException catch (e) {
      return {
        'success': false,
        'error': 'Platform error: ${e.message}',
        'code': e.code,
      };
    } catch (e) {
      return {
        'success': false,
        'error': 'Unexpected error: $e',
      };
    }
  }

  /// Preset cut paper
  static Future<Map<String, dynamic>> presetCutPaper({int mode = 0}) async {
    try {
      final result = await _channel.invokeMethod('presetCutPaper', {
        'mode': mode,
      });
      return Map<String, dynamic>.from(result);
    } on PlatformException catch (e) {
      return {
        'success': false,
        'error': 'Platform error: ${e.message}',
        'code': e.code,
      };
    } catch (e) {
      return {
        'success': false,
        'error': 'Unexpected error: $e',
      };
    }
  }

  /// Get cut mode
  static Future<Map<String, dynamic>> getCutMode() async {
    try {
      final result = await _channel.invokeMethod('getCutMode');
      return Map<String, dynamic>.from(result);
    } on PlatformException catch (e) {
      return {
        'success': false,
        'error': 'Platform error: ${e.message}',
        'code': e.code,
      };
    } catch (e) {
      return {
        'success': false,
        'error': 'Unexpected error: $e',
      };
    }
  }

  /// Get dot line
  static Future<Map<String, dynamic>> getDotLine() async {
    try {
      final result = await _channel.invokeMethod('getDotLine');
      return Map<String, dynamic>.from(result);
    } on PlatformException catch (e) {
      return {
        'success': false,
        'error': 'Platform error: ${e.message}',
        'code': e.code,
      };
    } catch (e) {
      return {
        'success': false,
        'error': 'Unexpected error: $e',
      };
    }
  }

  /// Get printer size
  static Future<Map<String, dynamic>> getPrinterSize() async {
    try {
      final result = await _channel.invokeMethod('getPrinterSize');
      return Map<String, dynamic>.from(result);
    } on PlatformException catch (e) {
      return {
        'success': false,
        'error': 'Platform error: ${e.message}',
        'code': e.code,
      };
    } catch (e) {
      return {
        'success': false,
        'error': 'Unexpected error: $e',
      };
    }
  }

  /// Set printer size
  static Future<Map<String, dynamic>> setPrinterSize(int size) async {
    try {
      final result = await _channel.invokeMethod('setPrinterSize', {
        'size': size,
      });
      return Map<String, dynamic>.from(result);
    } on PlatformException catch (e) {
      return {
        'success': false,
        'error': 'Platform error: ${e.message}',
        'code': e.code,
      };
    } catch (e) {
      return {
        'success': false,
        'error': 'Unexpected error: $e',
      };
    }
  }

  /// Set color gray
  static Future<Map<String, dynamic>> setColorGray({
    int blackLevel = 0,
    int colorLevel = 0,
  }) async {
    try {
      final result = await _channel.invokeMethod('setColorGray', {
        'blackLevel': blackLevel,
        'colorLevel': colorLevel,
      });
      return Map<String, dynamic>.from(result);
    } on PlatformException catch (e) {
      return {
        'success': false,
        'error': 'Platform error: ${e.message}',
        'code': e.code,
      };
    } catch (e) {
      return {
        'success': false,
        'error': 'Unexpected error: $e',
      };
    }
  }

  /// Enable low power print
  static Future<Map<String, dynamic>> enableLowPowerPrint(bool enable) async {
    try {
      final result = await _channel.invokeMethod('enableLowPowerPrint', {
        'enable': enable,
      });
      return Map<String, dynamic>.from(result);
    } on PlatformException catch (e) {
      return {
        'success': false,
        'error': 'Platform error: ${e.message}',
        'code': e.code,
      };
    } catch (e) {
      return {
        'success': false,
        'error': 'Unexpected error: $e',
      };
    }
  }

  /// Check if low power print is enabled
  static Future<Map<String, dynamic>> isLowPowerPrintEnabled() async {
    try {
      final result = await _channel.invokeMethod('isLowPowerPrintEnabled');
      return Map<String, dynamic>.from(result);
    } on PlatformException catch (e) {
      return {
        'success': false,
        'error': 'Platform error: ${e.message}',
        'code': e.code,
      };
    } catch (e) {
      return {
        'success': false,
        'error': 'Unexpected error: $e',
      };
    }
  }

  /// Print bitmap with mono threshold
  static Future<Map<String, dynamic>> printBitmapWithMonoThreshold(
    List<int> imageData, {
    int grayThreshold = 128,
  }) async {
    try {
      final result =
          await _channel.invokeMethod('printBitmapWithMonoThreshold', {
        'imageData': imageData,
        'grayThreshold': grayThreshold,
      });
      return Map<String, dynamic>.from(result);
    } on PlatformException catch (e) {
      return {
        'success': false,
        'error': 'Platform error: ${e.message}',
        'code': e.code,
      };
    } catch (e) {
      return {
        'success': false,
        'error': 'Unexpected error: $e',
      };
    }
  }

  /// Print color bitmap
  static Future<Map<String, dynamic>> printColorBitmap(
      List<int> imageData) async {
    try {
      final result = await _channel.invokeMethod('printColorBitmap', {
        'imageData': imageData,
      });
      return Map<String, dynamic>.from(result);
    } on PlatformException catch (e) {
      return {
        'success': false,
        'error': 'Platform error: ${e.message}',
        'code': e.code,
      };
    } catch (e) {
      return {
        'success': false,
        'error': 'Unexpected error: $e',
      };
    }
  }

  /// Print color bitmap with mono threshold
  static Future<Map<String, dynamic>> printColorBitmapWithMonoThreshold(
    List<int> imageData, {
    int grayThreshold = 128,
  }) async {
    try {
      final result =
          await _channel.invokeMethod('printColorBitmapWithMonoThreshold', {
        'imageData': imageData,
        'grayThreshold': grayThreshold,
      });
      return Map<String, dynamic>.from(result);
    } on PlatformException catch (e) {
      return {
        'success': false,
        'error': 'Platform error: ${e.message}',
        'code': e.code,
      };
    } catch (e) {
      return {
        'success': false,
        'error': 'Unexpected error: $e',
      };
    }
  }

  /// Set align mode
  static Future<Map<String, dynamic>> setAlignMode(int alignMode) async {
    try {
      final result = await _channel.invokeMethod('setAlignMode', {
        'alignMode': alignMode,
      });
      return Map<String, dynamic>.from(result);
    } on PlatformException catch (e) {
      return {
        'success': false,
        'error': 'Platform error: ${e.message}',
        'code': e.code,
      };
    } catch (e) {
      return {
        'success': false,
        'error': 'Unexpected error: $e',
      };
    }
  }

  // ============ UTILITY METHODS ============

  /// Get platform version
  static Future<String> getPlatformVersion() async {
    try {
      final result = await _channel.invokeMethod('getPlatformVersion');
      return result as String;
    } on PlatformException catch (e) {
      return 'Platform error: ${e.message}';
    } catch (e) {
      return 'Error: $e';
    }
  }

  /// Test native library loading
  static Future<Map<String, dynamic>> testNativeLibraryLoading() async {
    try {
      final result = await _channel.invokeMethod('testNativeLibraryLoading');
      return Map<String, dynamic>.from(result);
    } on PlatformException catch (e) {
      return {
        'success': false,
        'error': 'Platform error: ${e.message}',
        'code': e.code,
      };
    } catch (e) {
      return {
        'success': false,
        'error': 'Unexpected error: $e',
      };
    }
  }
}
