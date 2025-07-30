import 'package:flutter/material.dart';
import 'package:pax_sdk/pax_sdk.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'PAX SDK Demo',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
        useMaterial3: true,
      ),
      home: const PaxSdkDemo(),
    );
  }
}

class PaxSdkDemo extends StatefulWidget {
  const PaxSdkDemo({super.key});

  @override
  State<PaxSdkDemo> createState() => _PaxSdkDemoState();
}

class _PaxSdkDemoState extends State<PaxSdkDemo> {
  String _platformVersion = 'Unknown';
  String _nfcStatus = 'Not tested';
  String _printerStatus = 'Not tested';
  bool _isLoading = false;

  @override
  void initState() {
    super.initState();
    _getPlatformVersion();
  }

  Future<void> _getPlatformVersion() async {
    final version = await PaxSdk.getPlatformVersion();
    setState(() {
      _platformVersion = version;
    });
  }

  Future<void> _testNfc() async {
    setState(() {
      _isLoading = true;
      _nfcStatus = 'Testing NFC...';
    });

    try {
      // Check if card is present
      final isPresent = await PaxSdk.checkCardPresence();

      if (isPresent) {
        // Detect card details
        final cardResult = await PaxSdk.detectCard();

        setState(() {
          _nfcStatus = cardResult['success']
              ? 'Card detected: ${cardResult['cardData']?['uid'] ?? 'Unknown'}'
              : 'Card detection failed: ${cardResult['error']}';
        });
      } else {
        setState(() {
          _nfcStatus = 'No card present';
        });
      }
    } catch (e) {
      setState(() {
        _nfcStatus = 'NFC error: $e';
      });
    } finally {
      setState(() {
        _isLoading = false;
      });
    }
  }

  Future<void> _testPrinter() async {
    setState(() {
      _isLoading = true;
      _printerStatus = 'Testing printer...';
    });

    try {
      // Initialize printer
      final initialized = await PaxSdk.initializePrinter();

      if (initialized) {
        // Get printer status
        final statusResult = await PaxSdk.getPrinterStatus();

        if (statusResult['success']) {
          final status = statusResult['status'];
          final statusMessage = statusResult['statusMessage'];

          setState(() {
            _printerStatus = 'Printer ready - Status: $statusMessage ($status)';
          });

          // Test printing
          await _testPrinting();
        } else {
          setState(() {
            _printerStatus = 'Printer error: ${statusResult['error']}';
          });
        }
      } else {
        setState(() {
          _printerStatus = 'Printer initialization failed';
        });
      }
    } catch (e) {
      setState(() {
        _printerStatus = 'Printer error: $e';
      });
    } finally {
      setState(() {
        _isLoading = false;
      });
    }
  }

  Future<void> _testPrinting() async {
    try {
      // Print a test receipt
      final printResult = await PaxSdk.printText(
        '=== PAX SDK TEST ===\n\n'
        'Platform: $_platformVersion\n'
        'Time: ${DateTime.now()}\n\n'
        'This is a test print from\n'
        'Flutter PAX SDK integration\n\n'
        '=====================\n',
        options: {
          'fontSize': 'medium',
          'alignment': 1, // Center alignment
        },
      );

      if (printResult['success']) {
        // Feed paper
        await PaxSdk.feedPaper(pixels: 48);

        // Check if cutting is supported
        final cutSupported = await PaxSdk.isCutSupported();
        if (cutSupported) {
          await PaxSdk.cutPaper(mode: 0);
        }

        setState(() {
          _printerStatus = 'Test print completed successfully';
        });
      } else {
        setState(() {
          _printerStatus = 'Print failed: ${printResult['error']}';
        });
      }
    } catch (e) {
      setState(() {
        _printerStatus = 'Print test error: $e';
      });
    }
  }

  Future<void> _testAdvancedPrinter() async {
    setState(() {
      _isLoading = true;
      _printerStatus = 'Testing advanced printer...';
    });

    try {
      // Test native library loading first
      final nativeTest = await PaxSdk.testNativeLibraryLoading();
      print('Native library test: $nativeTest');

      // Initialize printer
      final initialized = await PaxSdk.initializePrinter();

      if (initialized) {
        // Test advanced printer features
        final printResult = await PaxSdk.printText(
          'Advanced Test\nPAX SDK Working!\n${DateTime.now()}',
          options: {
            'fontSize': 'large',
            'alignment': 1, // Center
          },
        );

        setState(() {
          _printerStatus = printResult['success']
              ? 'Advanced print successful'
              : 'Advanced print failed: ${printResult['error']}';
        });
      } else {
        setState(() {
          _printerStatus = 'Printer initialization failed';
        });
      }
    } catch (e) {
      setState(() {
        _printerStatus = 'Advanced print error: $e';
      });
    } finally {
      setState(() {
        _isLoading = false;
      });
    }
  }

  Future<void> _testNativeLibraries() async {
    setState(() {
      _isLoading = true;
      _nfcStatus = 'Testing native libraries...';
    });

    try {
      final result = await PaxSdk.testNativeLibraryLoading();
      print('Native library test result: $result');

      setState(() {
        _nfcStatus = result['success']
            ? 'Native libraries: ${result['paxSdkLoaded'] ? 'Loaded' : 'Failed'}'
            : 'Native library test failed: ${result['error']}';
      });
    } catch (e) {
      setState(() {
        _nfcStatus = 'Native library test error: $e';
      });
    } finally {
      setState(() {
        _isLoading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        title: const Text('PAX SDK Demo'),
      ),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Platform Version',
                      style: Theme.of(context).textTheme.headlineSmall,
                    ),
                    const SizedBox(height: 8),
                    Text(_platformVersion),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 16),
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'NFC Status',
                      style: Theme.of(context).textTheme.headlineSmall,
                    ),
                    const SizedBox(height: 8),
                    Text(_nfcStatus),
                    const SizedBox(height: 16),
                    Row(
                      children: [
                        Expanded(
                          child: ElevatedButton(
                            onPressed: _isLoading ? null : _testNfc,
                            child: const Text('Test NFC'),
                          ),
                        ),
                        const SizedBox(width: 8),
                        Expanded(
                          child: ElevatedButton(
                            onPressed: _isLoading ? null : _testNativeLibraries,
                            child: const Text('Test Libraries'),
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 16),
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Printer Status',
                      style: Theme.of(context).textTheme.headlineSmall,
                    ),
                    const SizedBox(height: 8),
                    Text(_printerStatus),
                    const SizedBox(height: 16),
                    Row(
                      children: [
                        Expanded(
                          child: ElevatedButton(
                            onPressed: _isLoading ? null : _testPrinter,
                            child: const Text('Test Printer'),
                          ),
                        ),
                        const SizedBox(width: 8),
                        Expanded(
                          child: ElevatedButton(
                            onPressed: _isLoading ? null : _testAdvancedPrinter,
                            child: const Text('Advanced Test'),
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),
            if (_isLoading)
              const Padding(
                padding: EdgeInsets.all(16.0),
                child: Center(
                  child: CircularProgressIndicator(),
                ),
              ),
          ],
        ),
      ),
    );
  }
}
