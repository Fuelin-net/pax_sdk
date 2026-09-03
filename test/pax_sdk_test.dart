import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:pax_sdk/pax_sdk.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  const channel = MethodChannel('pax_sdk');
  const scannerChannel = EventChannel('pax_sdk/scanner');

  final log = <MethodCall>[];

  setUp(() {
    PaxSdk.resetForTest();
    log.clear();
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (MethodCall methodCall) async {
      log.add(methodCall);

      switch (methodCall.method) {
        case 'detectCard':
          return {
            'success': true,
            'cardData': {
              'uid': 'AABBCCD D',
              'cardType': {'cardType': 'Mifare', 'manufacturer': 'NXP'},
            },
          };
        case 'checkCardPresence':
          return true;
        case 'waitForCard':
          return 'Waiting for card...';
        case 'tryAllModes':
          return 'Testing all detection modes';
        case 'initializePrinter':
          return true;
        case 'printText':
          return {'success': true, 'message': 'printed'};
        case 'printImage':
          return {'success': true};
        case 'getPrinterStatus':
          return {'success': true, 'statusMessage': 'OK'};
        case 'cutPaper':
          return {'success': true, 'mode': methodCall.arguments['mode']};
        case 'feedPaper':
          return {'success': true, 'pixels': methodCall.arguments['pixels']};
        case 'isCutSupported':
          return true;
        case 'setFontSize':
          return {'success': true, 'fontSize': methodCall.arguments['fontSize']};
        case 'setSpacing':
          return {
            'success': true,
            'wordSpace': methodCall.arguments['wordSpace'],
            'lineSpace': methodCall.arguments['lineSpace'],
          };
        case 'setDoubleHeight':
        case 'setDoubleWidth':
        case 'setLeftIndent':
        case 'setInvert':
        case 'presetCutPaper':
        case 'setAlignMode':
        case 'setFontPath':
        case 'setPrinterSize':
        case 'setColorGray':
        case 'enableLowPowerPrint':
        case 'printBitmapWithMonoThreshold':
        case 'printColorBitmap':
        case 'printColorBitmapWithMonoThreshold':
          return {'success': true};
        case 'getCutMode':
        case 'getDotLine':
        case 'getPrinterSize':
        case 'isLowPowerPrintEnabled':
          return {'success': true, 'value': 0};
        case 'startScanner':
          return {
            'success': true,
            'scannerType': methodCall.arguments['scannerType'],
          };
        case 'stopScanner':
          return {'success': true};
        case 'getPlatformVersion':
          return 'Android 13';
        case 'testNativeLibraryLoading':
          return {'success': true};
        default:
          throw MissingPluginException(methodCall.method);
      }
    });
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, null);
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockStreamHandler(scannerChannel, null);
    PaxSdk.resetForTest();
  });

  group('NFC', () {
    test('detectCard returns mapped success payload', () async {
      final result = await PaxSdk.detectCard();
      expect(result['success'], isTrue);
      expect(result['cardData']['uid'], 'AABBCCD D');
      expect(log.single.method, 'detectCard');
    });

    test('checkCardPresence returns bool', () async {
      expect(await PaxSdk.checkCardPresence(), isTrue);
      expect(log.single.method, 'checkCardPresence');
    });

    test('waitForCard and tryAllModes return strings', () async {
      expect(await PaxSdk.waitForCard(), 'Waiting for card...');
      expect(await PaxSdk.tryAllModes(), 'Testing all detection modes');
      expect(log.map((c) => c.method), ['waitForCard', 'tryAllModes']);
    });

    test('detectCard maps PlatformException to failure map', () async {
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, (call) async {
        throw PlatformException(code: 'dal', message: 'DAL null');
      });

      final result = await PaxSdk.detectCard();
      expect(result['success'], isFalse);
      expect(result['error'], contains('DAL null'));
      expect(result['code'], 'dal');
    });

    test('checkCardPresence returns false on PlatformException', () async {
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, (call) async {
        throw PlatformException(code: 'err', message: 'boom');
      });

      expect(await PaxSdk.checkCardPresence(), isFalse);
    });
  });

  group('Printer', () {
    test('initializePrinter / status / cut / feed', () async {
      expect(await PaxSdk.initializePrinter(), isTrue);
      expect((await PaxSdk.getPrinterStatus())['statusMessage'], 'OK');
      expect((await PaxSdk.cutPaper(mode: 1))['mode'], 1);
      expect((await PaxSdk.feedPaper(pixels: 64))['pixels'], 64);
      expect(await PaxSdk.isCutSupported(), isTrue);

      expect(
        log.map((c) => c.method),
        [
          'initializePrinter',
          'getPrinterStatus',
          'cutPaper',
          'feedPaper',
          'isCutSupported',
        ],
      );
      expect(log[2].arguments['mode'], 1);
      expect(log[3].arguments['pixels'], 64);
    });

    test('printText forwards text and options', () async {
      final result = await PaxSdk.printText(
        'Hello',
        options: {'fontSize': 'large', 'alignment': 1},
      );
      expect(result['success'], isTrue);
      expect(log.single.method, 'printText');
      expect(log.single.arguments['text'], 'Hello');
      expect(log.single.arguments['options']['fontSize'], 'large');
      expect(log.single.arguments['options']['alignment'], 1);
    });

    test('printText uses empty options when omitted', () async {
      await PaxSdk.printText('x');
      expect(log.single.arguments['options'], isEmpty);
    });

    test('printImage forwards image bytes', () async {
      final result = await PaxSdk.printImage([1, 2, 3]);
      expect(result['success'], isTrue);
      expect(log.single.arguments['imageData'], [1, 2, 3]);
    });

    test('advanced controls forward arguments', () async {
      await PaxSdk.setFontSize('large');
      await PaxSdk.setSpacing(wordSpace: 2, lineSpace: 4);
      await PaxSdk.setDoubleHeight(isAscDouble: false, isLocalDouble: true);
      await PaxSdk.setAlignMode(1);

      expect(log[0].arguments['fontSize'], 'large');
      expect(log[1].arguments['wordSpace'], 2);
      expect(log[1].arguments['lineSpace'], 4);
      expect(log[2].arguments['isAscDouble'], isFalse);
      expect(log[3].arguments['alignMode'], 1);
    });

    test('initializePrinter returns false on platform error', () async {
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, (call) async {
        throw PlatformException(code: 'printer', message: 'offline');
      });
      expect(await PaxSdk.initializePrinter(), isFalse);
    });
  });

  group('Scanner', () {
    test('startScanner defaults to REAR and omits null timeout', () async {
      final result = await PaxSdk.startScanner();
      expect(result['success'], isTrue);
      expect(result['scannerType'], 'REAR');
      expect(log.single.method, 'startScanner');
      expect(log.single.arguments['scannerType'], 'REAR');
      expect(log.single.arguments.containsKey('timeoutMs'), isFalse);
    });

    test('startScanner forwards type and timeout', () async {
      final result = await PaxSdk.startScanner(
        scannerType: 'LEFT',
        timeoutMs: 15000,
      );
      expect(result['success'], isTrue);
      expect(log.single.arguments['scannerType'], 'LEFT');
      expect(log.single.arguments['timeoutMs'], 15000);
    });

    test('stopScanner returns success map', () async {
      final result = await PaxSdk.stopScanner();
      expect(result['success'], isTrue);
      expect(log.single.method, 'stopScanner');
    });

    test('startScanner maps PlatformException', () async {
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, (call) async {
        throw PlatformException(code: 'scan', message: 'open failed');
      });

      final result = await PaxSdk.startScanner();
      expect(result['success'], isFalse);
      expect(result['error'], contains('open failed'));
      expect(result['code'], 'scan');
    });

    test('scanResults and onReadSuccess emit decoded content', () async {
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockStreamHandler(
        scannerChannel,
        MockStreamHandler.inline(
          onListen: (Object? args, MockStreamHandlerEventSink events) {
            events.success({
              'event': 'onRead',
              'content': 'SKU-42',
              'format': 'CODE_128',
            });
            events.success({'event': 'onFinish'});
            events.success({'event': 'onCancel'});
            events.success({
              'event': 'onRead',
              'content': null,
              'format': 'EMPTY',
            });
          },
        ),
      );

      final events = await PaxSdk.scanResults.take(4).toList();
      expect(events[0]['event'], 'onRead');
      expect(events[0]['content'], 'SKU-42');
      expect(events[0]['format'], 'CODE_128');
      expect(events[1]['event'], 'onFinish');
      expect(events[2]['event'], 'onCancel');

      PaxSdk.resetForTest();
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockStreamHandler(
        scannerChannel,
        MockStreamHandler.inline(
          onListen: (Object? args, MockStreamHandlerEventSink events) {
            events.success({
              'event': 'onRead',
              'content': 'QR-99',
              'format': 'QR_CODE',
            });
            events.success({'event': 'onFinish'});
          },
        ),
      );

      final codes = await PaxSdk.onReadSuccess.take(1).toList();
      expect(codes, ['QR-99']);
    });
  });

  group('Utility', () {
    test('getPlatformVersion', () async {
      expect(await PaxSdk.getPlatformVersion(), 'Android 13');
    });

    test('testNativeLibraryLoading', () async {
      final result = await PaxSdk.testNativeLibraryLoading();
      expect(result['success'], isTrue);
    });

    test('getPlatformVersion maps PlatformException', () async {
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, (call) async {
        throw PlatformException(code: 'x', message: 'nope');
      });
      expect(await PaxSdk.getPlatformVersion(), 'Platform error: nope');
    });
  });
}
