import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:pax_sdk/pax_sdk.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  group('plugin package config', () {
    test('AndroidManifest declares PICC, PRINTER, SCANNER permissions', () {
      final manifest = File('android/src/main/AndroidManifest.xml');
      expect(manifest.existsSync(), isTrue);

      final xml = manifest.readAsStringSync();
      expect(xml, contains('com.pax.permission.PICC'));
      expect(xml, contains('com.pax.permission.PRINTER'));
      expect(xml, contains('com.pax.permission.SCANNER'));
    });

    test('pubspec registers Android plugin class paxSDK', () {
      final pubspec = File('pubspec.yaml').readAsStringSync();
      expect(pubspec, contains('pluginClass: paxSDK'));
      expect(pubspec, contains('package: com.example.pax_sdk_package'));
    });

    test('canonical plugin Java source defines scanner methods', () {
      final java = File(
        'android/src/main/java/com/example/pax_sdk_package/paxSDK.java',
      );
      expect(java.existsSync(), isTrue);

      final src = java.readAsStringSync();
      expect(src, contains('case "startScanner"'));
      expect(src, contains('case "stopScanner"'));
      expect(src, contains('pax_sdk/scanner'));
      expect(src, contains('IScanner'));
      expect(src, contains('implements FlutterPlugin'));
    });
  });

  group('PaxSdk API surface', () {
    test('scanner entry points exist', () {
      // Tear-offs prove public API is wired; do not listen (needs EventChannel mock).
      expect(PaxSdk.startScanner, isA<Function>());
      expect(PaxSdk.stopScanner, isA<Function>());
    });
  });
}