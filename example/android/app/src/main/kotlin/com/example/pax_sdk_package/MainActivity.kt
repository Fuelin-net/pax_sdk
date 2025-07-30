package com.example.pax_sdk_package

import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine

class MainActivity: FlutterActivity() {
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        
        // Register our custom PAX SDK plugin
        flutterEngine.plugins.add(paxSDK())
    }
}
