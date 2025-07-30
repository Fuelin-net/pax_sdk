# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# PAX SDK ProGuard Rules
-keep class com.pax.** { *; }
-keep class com.pax.dal.** { *; }
-keep class com.pax.neptunelite.** { *; }
-keep class NeptuneLiteUser { *; }
-keep class IDAL { *; }
-keep class IPrinter { *; }
-keep class IPicc { *; }
-keep class PiccCardInfo { *; }
-keep class EDetectMode { *; }
-keep class EFontTypeAscii { *; }
-keep class EFontTypeExtCode { *; }
-keep class EPiccType { *; }
-keep class PrinterDevException { *; }

# Keep native method declarations
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep PAX SDK native libraries
-keep class com.example.pax_sdk_package.paxSDK { *; } 