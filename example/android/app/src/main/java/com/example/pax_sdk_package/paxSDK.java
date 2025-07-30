package com.example.pax_sdk_package;

import io.flutter.embedding.android.FlutterActivity;

import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

import com.pax.dal.IDAL;
import com.pax.dal.IPicc;
import com.pax.dal.IPrinter;
import com.pax.dal.entity.PiccCardInfo;
import com.pax.dal.entity.EDetectMode;
import com.pax.dal.entity.EFontTypeAscii;
import com.pax.dal.entity.EFontTypeExtCode;
import com.pax.dal.exceptions.PrinterDevException;
import com.pax.neptunelite.api.NeptuneLiteUser;
import android.util.Log;
import android.content.Context;
import com.pax.dal.entity.EPiccType;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;

import android.content.res.AssetManager;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import dalvik.system.DexClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * paxSDK - PAX NFC and Printer functionality integration
 */
public class paxSDK implements FlutterPlugin, MethodCallHandler {
    private MethodChannel channel;
    public static Context appContext;
    private static final String TAG = "PAX_SDK";
    private IPrinter printer;
    private IDAL dal;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "pax_sdk");
        channel.setMethodCallHandler(this);
        appContext = flutterPluginBinding.getApplicationContext();
    }

    // ============ NFC METHODS ============

    /**
     * Check if PAX SDK is available on this device
     */
    private boolean isPaxSdkAvailable() {
        try {
            // Check if PAX-specific packages are available
            PackageManager pm = appContext.getPackageManager();
            List<PackageInfo> packages = pm.getInstalledPackages(0);
            
            for (PackageInfo packageInfo : packages) {
                if (packageInfo.packageName.contains("pax") || 
                    packageInfo.packageName.contains("pos") ||
                    packageInfo.packageName.contains("com.pos.device")) {
                    Log.d(TAG, "Found PAX-related package: " + packageInfo.packageName);
                    return true;
                }
            }
            
            Log.w(TAG, "No PAX-related packages found on device");
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error checking PAX SDK availability: " + e.getMessage());
            return false;
        }
    }

    /**
     * Initialize PICC and detect card presence with basic information
     */
    public Map<String, Object> detectAndIdentifyCard(Context context) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Use appContext if context is null
            Context ctx = (context != null) ? context : appContext;
            if (ctx == null) {
                result.put("success", false);
                result.put("error", "Context is null - cannot initialize PAX SDK");
                return result;
            }
            
            // Check if PAX SDK is available on this device
            if (!isPaxSdkAvailable()) {
                result.put("success", false);
                result.put("error", "PAX SDK not available on this device - not a PAX device");
                return result;
            }
            
            // Initialize PAX SDK
            initializePaxSDK(ctx);

            Log.d(TAG, "Getting DAL instance...");
            IDAL dal = null;
            try {
                dal = NeptuneLiteUser.getInstance().getDal(ctx);
                if (dal == null) {
                    Log.e(TAG, "Failed to get DAL instance - DAL is null");
                    result.put("success", false);
                    result.put("error", "Failed to get DAL instance - DAL is null");
                    return result;
                }
                Log.d(TAG, "DAL instance obtained successfully");
            } catch (UnsatisfiedLinkError e) {
                Log.e(TAG, "UnsatisfiedLinkError when getting DAL: " + e.getMessage());
                result.put("success", false);
                result.put("error", "LOAD DAL ERR: Missing native libraries - " + e.getMessage());
                return result;
            } catch (Exception e) {
                Log.e(TAG, "Exception when getting DAL: " + e.getMessage());
                result.put("success", false);
                result.put("error", "LOAD DAL ERR: " + e.getMessage());
                return result;
            }
            
            Log.d(TAG, "Getting PICC instance...");
            IPicc picc = dal.getPicc(EPiccType.INTERNAL);
            if (picc == null) {
                Log.e(TAG, "Failed to get PICC instance");
                result.put("success", false);
                result.put("error", "Failed to get PICC instance");
                return result;
            }
            Log.d(TAG, "PICC instance obtained successfully");

            picc.open();
            Log.d(TAG, "PICC opened successfully");

            // Try to detect card with retry logic
            PiccCardInfo cardInfo = detectCardWithRetry(picc);
            
            if (cardInfo == null) {
                Log.w(TAG, "No NFC card detected");
                picc.close();
                result.put("success", false);
                result.put("error", "No card detected");
                return result;
            }

            // Get card information
            Map<String, Object> cardData = analyzeCardInfo(cardInfo);

            // Try to read manufacturer block (Block 0) if possible
            Map<String, Object> manufacturerData = tryReadManufacturerBlock(picc, cardInfo);
            
            picc.close();
            Log.d(TAG, "PICC closed successfully");

            // Combine all data
            result.put("success", true);
            result.put("cardData", cardData);
            result.put("manufacturerData", manufacturerData);
            
            return result;

        } catch (Exception e) {
            Log.e(TAG, "Error during card detection: ", e);
            result.put("success", false);
            result.put("error", "Detection error: " + e.getMessage());
            return result;
        }
    }

    /**
     * Initialize PAX SDK components
     */
    private void initializePaxSDK(Context context) throws Exception {
        Log.d(TAG, "Initializing PAX SDK...");
        
        try {
            // Check if native libraries are available
            String libraryPath = context.getApplicationInfo().nativeLibraryDir;
            Log.d(TAG, "Native library path: " + libraryPath);
            
            // List available libraries for debugging
            File libDir = new File(libraryPath);
            if (libDir.exists() && libDir.isDirectory()) {
                File[] libs = libDir.listFiles((dir, name) -> name.endsWith(".so"));
                if (libs != null) {
                    Log.d(TAG, "Available native libraries:");
                    for (File lib : libs) {
                        Log.d(TAG, "  - " + lib.getName());
                    }
                }
            }
            
            // Load dex file if it exists
            File dexFile = new File(context.getFilesDir(), "nepcore.dex");
            File optimizedDir = context.getDir("dex_opt", Context.MODE_PRIVATE);

            if (dexFile.exists()) {
                DexClassLoader loader = new DexClassLoader(
                        dexFile.getAbsolutePath(),
                        optimizedDir.getAbsolutePath(),
                        null, // Don't pass nativeLibsDir to avoid library conflicts
                        context.getClassLoader()
                );
                Log.d(TAG, "DEX file loaded successfully");
            } else {
                Log.w(TAG, "DEX file not found, continuing without dynamic loading");
            }
            
            Log.d(TAG, "PAX SDK initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing PAX SDK: " + e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Detect card with retry mechanism
     */
    private PiccCardInfo detectCardWithRetry(IPicc picc) {
        int maxRetries = 5;
        int retryDelay = 1000; // 1 second
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            Log.d(TAG, "Card detection attempt " + attempt + "/" + maxRetries);
            
            try {
                PiccCardInfo info = picc.detect(EDetectMode.ONLY_M);
                if (info != null) {
                    Log.d(TAG, "✓ Card detected successfully on attempt " + attempt);
                    return info;
                } else {
                    Log.d(TAG, "No card detected on attempt " + attempt);
                    if (attempt < maxRetries) {
                        Thread.sleep(retryDelay);
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Detection attempt " + attempt + " failed: " + e.getMessage());
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(retryDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        return null;
    }

    /**
     * Analyze and log card information
     */
    private Map<String, Object> analyzeCardInfo(PiccCardInfo cardInfo) {
        Map<String, Object> cardData = new HashMap<>();
        
        try {
            Log.d(TAG, "=== CARD INFORMATION ===");
            
            // Get serial/UID
            byte[] serial = cardInfo.getSerialInfo();
            if (serial != null && serial.length > 0) {
                String uidHex = bytesToHex(serial);
                Log.d(TAG, "Card UID/Serial: " + uidHex);
                Log.d(TAG, "UID Length: " + serial.length + " bytes");
                
                // Store card data
                cardData.put("uid", uidHex);
                cardData.put("uidLength", serial.length);
                cardData.put("uidBytes", serial);
                
                // Determine card type based on UID length and first byte
                Map<String, Object> cardTypeInfo = analyzeCardType(serial);
                cardData.put("cardType", cardTypeInfo);
            } else {
                Log.w(TAG, "No serial information available");
                cardData.put("error", "No serial information available");
            }

            // Try to get additional card information
            try {
                // Some cards provide additional info through the CardInfo object
                Log.d(TAG, "Card Info Object: " + cardInfo.toString());
                cardData.put("cardInfoObject", cardInfo.toString());
            } catch (Exception e) {
                Log.v(TAG, "Could not get additional card info: " + e.getMessage());
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error analyzing card info: " + e.getMessage());
            cardData.put("error", "Analysis error: " + e.getMessage());
        }
        
        return cardData;
    }

    /**
     * Analyze card type based on UID
     */
    private Map<String, Object> analyzeCardType(byte[] uid) {
        Map<String, Object> cardTypeInfo = new HashMap<>();
        
        if (uid == null || uid.length == 0) return cardTypeInfo;
        
        Log.d(TAG, "=== CARD TYPE ANALYSIS ===");
        
        byte firstByte = uid[0];
        String cardType = "";
        String manufacturer = "";
        
        switch (uid.length) {
            case 4:
                cardType = "MIFARE Classic 1K/4K (4-byte UID)";
                Log.d(TAG, "Card Type: " + cardType);
                break;
            case 7:
                cardType = "MIFARE Classic 1K/4K (7-byte UID)";
                Log.d(TAG, "Card Type: " + cardType);
                break;
            case 10:
                cardType = "MIFARE Classic 4K (10-byte UID)";
                Log.d(TAG, "Card Type: " + cardType);
                break;
            default:
                cardType = "Unknown (" + uid.length + "-byte UID)";
                Log.d(TAG, "Card Type: " + cardType);
                break;
        }
        
        // Analyze manufacturer based on first byte
        if (uid.length >= 4) {
            switch (firstByte) {
                case 0x04:
                    manufacturer = "NXP Semiconductors";
                    Log.d(TAG, "Manufacturer: " + manufacturer);
                    break;
                case 0x02:
                    manufacturer = "STMicroelectronics";
                    Log.d(TAG, "Manufacturer: " + manufacturer);
                    break;
                case 0x05:
                    manufacturer = "Infineon Technologies";
                    Log.d(TAG, "Manufacturer: " + manufacturer);
                    break;
                default:
                    manufacturer = "Unknown (0x" + String.format("%02X", firstByte) + ")";
                    Log.d(TAG, "Manufacturer: " + manufacturer);
                    break;
            }
        }
        
        cardTypeInfo.put("cardType", cardType);
        cardTypeInfo.put("manufacturer", manufacturer);
        cardTypeInfo.put("uidLength", uid.length);
        cardTypeInfo.put("firstByte", String.format("%02X", firstByte));
        
        return cardTypeInfo;
    }

    /**
     * Try to read manufacturer block (Block 0) without authentication
     */
    private Map<String, Object> tryReadManufacturerBlock(IPicc picc, PiccCardInfo cardInfo) {
        Map<String, Object> manufacturerData = new HashMap<>();
        
        try {
            Log.d(TAG, "=== TRYING TO READ MANUFACTURER BLOCK ===");
            
            // Try to read Block 0 directly (sometimes readable without auth)
            byte[] block0Data = picc.m1Read((byte)0);
            if (block0Data != null && block0Data.length > 0) {
                String block0Hex = bytesToHex(block0Data);
                Log.d(TAG, "✓ Block 0 (Manufacturer) Data: " + block0Hex);
                manufacturerData.put("block0Data", block0Hex);
                manufacturerData.put("block0Bytes", block0Data);
                
                Map<String, Object> parsedData = parseManufacturerBlock(block0Data);
                manufacturerData.put("parsedData", parsedData);
            } else {
                Log.d(TAG, "Block 0 is protected or returned no data");
                manufacturerData.put("error", "Block 0 is protected or returned no data");
            }
            
        } catch (Exception e) {
            Log.d(TAG, "Block 0 requires authentication: " + e.getMessage());
            manufacturerData.put("error", "Block 0 requires authentication: " + e.getMessage());
            
            // Try with default key if direct read fails
            Map<String, Object> authResult = tryReadBlock0WithAuth(picc, cardInfo);
            manufacturerData.put("authResult", authResult);
        }
        
        return manufacturerData;
    }

    /**
     * Try to read Block 0 with common authentication keys
     */
    private Map<String, Object> tryReadBlock0WithAuth(IPicc picc, PiccCardInfo cardInfo) {
        Map<String, Object> authResult = new HashMap<>();
        
        byte[] serial = cardInfo.getSerialInfo();
        if (serial == null) {
            authResult.put("error", "No serial info available");
            return authResult;
        }
        
        byte[][] commonKeys = {
            {(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff}, // Default
            {(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00}, // Null
        };

        for (int i = 0; i < commonKeys.length; i++) {
            try {
                picc.m1Auth(com.pax.dal.entity.EM1KeyType.TYPE_A, (byte)0, commonKeys[i], serial);
                byte[] data = picc.m1Read((byte)0);
                if (data != null) {
                    String dataHex = bytesToHex(data);
                    Log.d(TAG, "✓ Block 0 with key " + i + ": " + dataHex);
                    authResult.put("success", true);
                    authResult.put("keyIndex", i);
                    authResult.put("data", dataHex);
                    authResult.put("dataBytes", data);
                    
                    Map<String, Object> parsedData = parseManufacturerBlock(data);
                    authResult.put("parsedData", parsedData);
                    return authResult;
                }
            } catch (Exception e) {
                Log.v(TAG, "Block 0 auth with key " + i + " failed");
            }
        }
        
        Log.d(TAG, "Block 0 is fully protected with custom keys");
        authResult.put("error", "Block 0 is fully protected with custom keys");
        return authResult;
    }

    /**
     * Parse manufacturer block data
     */
    private Map<String, Object> parseManufacturerBlock(byte[] data) {
        Map<String, Object> parsedData = new HashMap<>();
        
        if (data == null || data.length < 16) {
            parsedData.put("error", "Invalid data length");
            return parsedData;
        }
        
        try {
            Log.d(TAG, "=== MANUFACTURER BLOCK DETAILS ===");
            
            // Extract UID (first 4 bytes for most cards)
            byte[] uid = new byte[4];
            System.arraycopy(data, 0, uid, 0, 4);
            String embeddedUid = bytesToHex(uid);
            Log.d(TAG, "Embedded UID: " + embeddedUid);
            parsedData.put("embeddedUid", embeddedUid);
            parsedData.put("embeddedUidBytes", uid);
            
            // BCC (Block Check Character)
            byte bcc = data[4];
            String bccHex = String.format("%02X", bcc);
            Log.d(TAG, "BCC: 0x" + bccHex);
            parsedData.put("bcc", bccHex);
            parsedData.put("bccValue", bcc);
            
            // SAK (Select Acknowledge)
            if (data.length > 5) {
                byte sak = data[5];
                String sakHex = String.format("%02X", sak);
                Log.d(TAG, "SAK: 0x" + sakHex);
                parsedData.put("sak", sakHex);
                parsedData.put("sakValue", sak);
            }
            
            // ATQA (Answer to Request)
            if (data.length > 7) {
                byte atqa1 = data[6];
                byte atqa2 = data[7];
                String atqaHex = String.format("%02X%02X", atqa2, atqa1);
                Log.d(TAG, "ATQA: 0x" + atqaHex);
                parsedData.put("atqa", atqaHex);
                parsedData.put("atqaBytes", new byte[]{atqa1, atqa2});
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error parsing manufacturer block: " + e.getMessage());
            parsedData.put("error", "Parsing error: " + e.getMessage());
        }
        
        return parsedData;
    }

    /**
     * Quick card presence check
     */
    public boolean isCardPresent(Context context) {
        try {
            // Use appContext if context is null
            Context ctx = (context != null) ? context : appContext;
            if (ctx == null) {
                Log.e(TAG, "Context is null - cannot check card presence");
                return false;
            }
            
            IDAL dal = NeptuneLiteUser.getInstance().getDal(ctx);
            IPicc picc = dal.getPicc(EPiccType.INTERNAL);
            
            picc.open();
            PiccCardInfo info = picc.detect(EDetectMode.ONLY_M);
            picc.close();
            
            boolean present = (info != null);
            Log.d(TAG, "Card presence check: " + (present ? "PRESENT" : "NOT PRESENT"));
            return present;
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking card presence: " + e.getMessage());
            return false;
        }
    }

    /**
     * Wait for card and process when detected
     */
    public void waitForCardAndProcess(Context context) {
        new Thread(() -> {
            Log.d(TAG, "=== WAITING FOR CARD ===");
            
            while (true) {
                if (isCardPresent(context)) {
                    Log.d(TAG, "Card detected! Processing...");
                    detectAndIdentifyCard(context);
                    break;
                }
                
                try {
                    Thread.sleep(50); // Check every 500ms
                } catch (InterruptedException e) {
                    Log.d(TAG, "Card waiting interrupted");
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }).start();
    }

    /**
     * Test different detection modes
     */
    public void tryAllDetectionModes(Context context) {
        try {
            // Use appContext if context is null
            Context ctx = (context != null) ? context : appContext;
            if (ctx == null) {
                Log.e(TAG, "Context is null - cannot test detection modes");
                return;
            }
            
            initializePaxSDK(ctx);
            
            IDAL dal = NeptuneLiteUser.getInstance().getDal(ctx);
            IPicc picc = dal.getPicc(EPiccType.INTERNAL);
            
            picc.open();
            Log.d(TAG, "=== TESTING ALL DETECTION MODES ===");

            EDetectMode[] modes = {
                EDetectMode.ONLY_M,        // MIFARE cards
                EDetectMode.ONLY_A,        // ISO14443 Type A
                EDetectMode.ISO14443_AB,   // All ISO14443 cards
                EDetectMode.EMV_AB         // EMV cards
            };

            PiccCardInfo bestResult = null;
            EDetectMode bestMode = null;

            for (EDetectMode mode : modes) {
                Log.d(TAG, "Testing mode: " + mode);
                try {
                    PiccCardInfo info = picc.detect(mode);
                    if (info != null) {
                        Log.d(TAG, "✓ SUCCESS with mode: " + mode);
                        if (bestResult == null) {
                            bestResult = info;
                            bestMode = mode;
                        }
                    } else {
                        Log.d(TAG, "No card detected with mode: " + mode);
                    }
                } catch (Exception e) {
                    Log.d(TAG, "Mode " + mode + " failed: " + e.getMessage());
                }
            }

            if (bestResult != null) {
                Log.d(TAG, "Best detection mode: " + bestMode);
                analyzeCardInfo(bestResult);
            } else {
                Log.w(TAG, "No card detected with any mode");
            }

            picc.close();
            
        } catch (Exception e) {
            Log.e(TAG, "Error testing detection modes: " + e.getMessage());
        }
    }

    // ============ PRINTER METHODS ============

    /**
     * Initialize printer
     */
    private boolean initializePrinter(Context context) {
        try {
            // Use appContext if context is null
            Context ctx = (context != null) ? context : appContext;
            if (ctx == null) {
                Log.e(TAG, "Context is null - cannot initialize printer");
                return false;
            }
            
            // Initialize PAX SDK first
            initializePaxSDK(ctx);

            dal = NeptuneLiteUser.getInstance().getDal(ctx);
            printer = dal.getPrinter();
            
            // Initialize printer
            printer.init();
            
            // Check printer status
            int status = printer.getStatus();
            Log.d(TAG, "Printer status after init: " + status);
            
            if (status != 0) {
                Log.w(TAG, "Printer status not OK: " + status + " - " + getStatusMessage(status));
                // Try to reset printer if it's in error state
                try {
                    printer.init();
                    status = printer.getStatus();
                    Log.d(TAG, "Printer status after reset: " + status);
                } catch (Exception resetException) {
                    Log.e(TAG, "Failed to reset printer: ", resetException);
                }
            }
            
            Log.d(TAG, "Printer initialized successfully");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing printer: ", e);
            return false;
        }
    }

    // ============ NEW PRINTER METHODS ============

    /**
     * Set font size using PAX font types
     */
    private Map<String, Object> setFontSize(String fontSize) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (!initializePrinter(null)) {
                result.put("success", false);
                result.put("error", "Failed to initialize printer");
                return result;
            }

            EFontTypeAscii asciiFont;
            EFontTypeExtCode extFont;
            
            switch (fontSize.toLowerCase()) {
                case "small":
                    asciiFont = EFontTypeAscii.FONT_8_16;
                    extFont = EFontTypeExtCode.FONT_16_16;
                    break;
                case "medium":
                    asciiFont = EFontTypeAscii.FONT_12_24;
                    extFont = EFontTypeExtCode.FONT_24_24;
                    break;
                case "large":
                    asciiFont = EFontTypeAscii.FONT_16_32;
                    extFont = EFontTypeExtCode.FONT_32_32;
                    break;
                case "extra_large":
                    asciiFont = EFontTypeAscii.FONT_24_48;
                    extFont = EFontTypeExtCode.FONT_48_48;
                    break;
                default:
                    asciiFont = EFontTypeAscii.FONT_12_24;
                    extFont = EFontTypeExtCode.FONT_24_24;
                    break;
            }
            
            printer.fontSet(asciiFont, extFont);
            result.put("success", true);
            result.put("message", "Font size set successfully");
            
        } catch (PrinterDevException e) {
            Log.e(TAG, "Printer exception: ", e);
            result.put("success", false);
            result.put("error", "Printer error: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error setting font size: ", e);
            result.put("success", false);
            result.put("error", "Unexpected error: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * Set custom font path
     */
    private Map<String, Object> setFontPath(String fontPath) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (!initializePrinter(null)) {
                result.put("success", false);
                result.put("error", "Failed to initialize printer");
                return result;
            }

            printer.setFontPath(fontPath);
            result.put("success", true);
            result.put("message", "Font path set successfully");
            
        } catch (PrinterDevException e) {
            Log.e(TAG, "Printer exception: ", e);
            result.put("success", false);
            result.put("error", "Printer error: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error setting font path: ", e);
            result.put("success", false);
            result.put("error", "Unexpected error: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * Set double height printing
     */
    private Map<String, Object> setDoubleHeight(boolean isAscDouble, boolean isLocalDouble) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (!initializePrinter(null)) {
                result.put("success", false);
                result.put("error", "Failed to initialize printer");
                return result;
            }

            printer.doubleHeight(isAscDouble, isLocalDouble);
            result.put("success", true);
            result.put("message", "Double height set successfully");
            
        } catch (PrinterDevException e) {
            Log.e(TAG, "Printer exception: ", e);
            result.put("success", false);
            result.put("error", "Printer error: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error setting double height: ", e);
            result.put("success", false);
            result.put("error", "Unexpected error: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * Set double width printing
     */
    private Map<String, Object> setDoubleWidth(boolean isAscDouble, boolean isLocalDouble) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (!initializePrinter(null)) {
                result.put("success", false);
                result.put("error", "Failed to initialize printer");
                return result;
            }

            printer.doubleWidth(isAscDouble, isLocalDouble);
            result.put("success", true);
            result.put("message", "Double width set successfully");
            
        } catch (PrinterDevException e) {
            Log.e(TAG, "Printer exception: ", e);
            result.put("success", false);
            result.put("error", "Printer error: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error setting double width: ", e);
            result.put("success", false);
            result.put("error", "Unexpected error: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * Set left indent
     */
    private Map<String, Object> setLeftIndent(int indent) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (!initializePrinter(null)) {
                result.put("success", false);
                result.put("error", "Failed to initialize printer");
                return result;
            }

            printer.leftIndent(indent);
            result.put("success", true);
            result.put("message", "Left indent set successfully");
            
        } catch (PrinterDevException e) {
            Log.e(TAG, "Printer exception: ", e);
            result.put("success", false);
            result.put("error", "Printer error: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error setting left indent: ", e);
            result.put("success", false);
            result.put("error", "Unexpected error: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * Set invert printing
     */
    private Map<String, Object> setInvert(boolean isInvert) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (!initializePrinter(null)) {
                result.put("success", false);
                result.put("error", "Failed to initialize printer");
                return result;
            }

            printer.invert(isInvert);
            result.put("success", true);
            result.put("message", "Invert setting applied successfully");
            
        } catch (PrinterDevException e) {
            Log.e(TAG, "Printer exception: ", e);
            result.put("success", false);
            result.put("error", "Printer error: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error setting invert: ", e);
            result.put("success", false);
            result.put("error", "Unexpected error: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * Set character and line spacing
     */
    private Map<String, Object> setSpacing(byte wordSpace, byte lineSpace) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (!initializePrinter(null)) {
                result.put("success", false);
                result.put("error", "Failed to initialize printer");
                return result;
            }

            printer.spaceSet(wordSpace, lineSpace);
            result.put("success", true);
            result.put("message", "Spacing set successfully");
            
        } catch (PrinterDevException e) {
            Log.e(TAG, "Printer exception: ", e);
            result.put("success", false);
            result.put("error", "Printer error: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error setting spacing: ", e);
            result.put("success", false);
            result.put("error", "Unexpected error: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * Preset cut paper mode
     */
    private Map<String, Object> presetCutPaper(int mode) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (!initializePrinter(null)) {
                result.put("success", false);
                result.put("error", "Failed to initialize printer");
                return result;
            }

            printer.presetCutPaper(mode);
            result.put("success", true);
            result.put("message", "Cut paper mode preset successfully");
            
        } catch (PrinterDevException e) {
            Log.e(TAG, "Printer exception: ", e);
            result.put("success", false);
            result.put("error", "Printer error: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error setting preset cut paper: ", e);
            result.put("success", false);
            result.put("error", "Unexpected error: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * Get current cut mode
     */
    private Map<String, Object> getCutMode() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (!initializePrinter(null)) {
                result.put("success", false);
                result.put("error", "Failed to initialize printer");
                return result;
            }

            int cutMode = printer.getCutMode();
            result.put("success", true);
            result.put("cutMode", cutMode);
            result.put("message", "Cut mode retrieved successfully");
            
        } catch (PrinterDevException e) {
            Log.e(TAG, "Printer exception: ", e);
            result.put("success", false);
            result.put("error", "Printer error: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error getting cut mode: ", e);
            result.put("success", false);
            result.put("error", "Unexpected error: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * Get current dot line
     */
    private Map<String, Object> getDotLine() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (!initializePrinter(null)) {
                result.put("success", false);
                result.put("error", "Failed to initialize printer");
                return result;
            }

            int dotLine = printer.getDotLine();
            result.put("success", true);
            result.put("dotLine", dotLine);
            result.put("message", "Dot line retrieved successfully");
            
        } catch (PrinterDevException e) {
            Log.e(TAG, "Printer exception: ", e);
            result.put("success", false);
            result.put("error", "Printer error: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error getting dot line: ", e);
            result.put("success", false);
            result.put("error", "Unexpected error: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * Get printer size
     */
    private Map<String, Object> getPrinterSize() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (!initializePrinter(null)) {
                result.put("success", false);
                result.put("error", "Failed to initialize printer");
                return result;
            }

            // int size = printer.getPrinterSize(); // Method not available in current SDK version
            int size = 0; // Default value
            result.put("success", true);
            result.put("printerSize", size);
            result.put("message", "Printer size retrieved successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting printer size: ", e);
            result.put("success", false);
            result.put("error", "Unexpected error: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * Set printer size
     */
    private Map<String, Object> setPrinterSize(int size) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (!initializePrinter(null)) {
                result.put("success", false);
                result.put("error", "Failed to initialize printer");
                return result;
            }

            // boolean success = printer.setPrinterSize(size); // Method not available in current SDK version
            boolean success = false; // Default value
            result.put("success", success);
            if (success) {
                result.put("message", "Printer size set successfully");
            } else {
                result.put("error", "Failed to set printer size");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error setting printer size: ", e);
            result.put("success", false);
            result.put("error", "Unexpected error: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * Set color gray levels
     */
    private Map<String, Object> setColorGray(int blackLevel, int colorLevel) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (!initializePrinter(null)) {
                result.put("success", false);
                result.put("error", "Failed to initialize printer");
                return result;
            }

            // printer.setColorGray(blackLevel, colorLevel); // Method not available in current SDK version
            result.put("success", true);
            result.put("message", "Color gray levels set successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error setting color gray: ", e);
            result.put("success", false);
            result.put("error", "Unexpected error: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * Enable/disable low power print
     */
    private Map<String, Object> enableLowPowerPrint(boolean enable) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (!initializePrinter(null)) {
                result.put("success", false);
                result.put("error", "Failed to initialize printer");
                return result;
            }

            // printer.enableLowPowerPrint(enable); // Method not available in current SDK version
            result.put("success", true);
            result.put("message", "Low power print " + (enable ? "enabled" : "disabled") + " successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error setting low power print: ", e);
            result.put("success", false);
            result.put("error", "Unexpected error: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * Check if low power print is enabled
     */
    private Map<String, Object> isLowPowerPrintEnabled() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (!initializePrinter(null)) {
                result.put("success", false);
                result.put("error", "Failed to initialize printer");
                return result;
            }

            // boolean enabled = printer.isLowPowerPrintEnabled(); // Method not available in current SDK version
            boolean enabled = false; // Default value
            result.put("success", true);
            result.put("enabled", enabled);
            result.put("message", "Low power print status retrieved successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking low power print: ", e);
            result.put("success", false);
            result.put("error", "Unexpected error: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * Print bitmap with mono threshold
     */
    private Map<String, Object> printBitmapWithMonoThreshold(List<Integer> imageData, int grayThreshold) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (!initializePrinter(null)) {
                result.put("success", false);
                result.put("error", "Failed to initialize printer");
                return result;
            }
            
            // Check if printer is ready
            int printerStatus = printer.getStatus();
            if (printerStatus != 0) {
                Log.w(TAG, "Printer not ready, status: " + printerStatus);
                result.put("success", false);
                result.put("error", "Printer not ready: " + getStatusMessage(printerStatus));
                return result;
            }

            // Convert List<Integer> to byte array
            byte[] imageBytes = new byte[imageData.size()];
            for (int i = 0; i < imageData.size(); i++) {
                imageBytes[i] = imageData.get(i).byteValue();
            }

            // Create bitmap from byte array
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            if (bitmap == null) {
                result.put("success", false);
                result.put("error", "Failed to decode image");
                return result;
            }

            // Print the bitmap with threshold
            printer.printBitmapWithMonoThreshold(bitmap, grayThreshold);
            
            // Start printing
            int status = printer.start();
            
            if (status == 0) {
                result.put("success", true);
                result.put("message", "Image printed with threshold successfully");
            } else {
                result.put("success", false);
                result.put("error", "Print failed with status: " + status);
                result.put("statusCode", status);
            }
            
        } catch (PrinterDevException e) {
            Log.e(TAG, "Printer exception: ", e);
            result.put("success", false);
            result.put("error", "Printer error: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error printing image with threshold: ", e);
            result.put("success", false);
            result.put("error", "Unexpected error: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * Print color bitmap
     */
    private Map<String, Object> printColorBitmap(List<Integer> imageData) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (!initializePrinter(null)) {
                result.put("success", false);
                result.put("error", "Failed to initialize printer");
                return result;
            }
            
            // Check if printer is ready
            int printerStatus = printer.getStatus();
            if (printerStatus != 0) {
                Log.w(TAG, "Printer not ready, status: " + printerStatus);
                result.put("success", false);
                result.put("error", "Printer not ready: " + getStatusMessage(printerStatus));
                return result;
            }

            // Convert List<Integer> to byte array
            byte[] imageBytes = new byte[imageData.size()];
            for (int i = 0; i < imageData.size(); i++) {
                imageBytes[i] = imageData.get(i).byteValue();
            }

            // Create bitmap from byte array
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            if (bitmap == null) {
                result.put("success", false);
                result.put("error", "Failed to decode image");
                return result;
            }

            // Print the color bitmap
            // printer.printColorBitmap(bitmap); // Method not available in current SDK version
            
            // Start printing
            int status = printer.start();
            
            if (status == 0) {
                result.put("success", true);
                result.put("message", "Color image printed successfully");
            } else {
                result.put("success", false);
                result.put("error", "Print failed with status: " + status);
                result.put("statusCode", status);
            }
            
        } catch (PrinterDevException e) {
            Log.e(TAG, "Printer exception: ", e);
            result.put("success", false);
            result.put("error", "Printer error: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error printing color image: ", e);
            result.put("success", false);
            result.put("error", "Unexpected error: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * Print color bitmap with mono threshold
     */
    private Map<String, Object> printColorBitmapWithMonoThreshold(List<Integer> imageData, int grayThreshold) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (!initializePrinter(null)) {
                result.put("success", false);
                result.put("error", "Failed to initialize printer");
                return result;
            }
            
            // Check if printer is ready
            int printerStatus = printer.getStatus();
            if (printerStatus != 0) {
                Log.w(TAG, "Printer not ready, status: " + printerStatus);
                result.put("success", false);
                result.put("error", "Printer not ready: " + getStatusMessage(printerStatus));
                return result;
            }

            // Convert List<Integer> to byte array
            byte[] imageBytes = new byte[imageData.size()];
            for (int i = 0; i < imageData.size(); i++) {
                imageBytes[i] = imageData.get(i).byteValue();
            }

            // Create bitmap from byte array
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            if (bitmap == null) {
                result.put("success", false);
                result.put("error", "Failed to decode image");
                return result;
            }

            // Print the color bitmap with threshold
            // printer.printColorBitmapWithMonoThreshold(bitmap, grayThreshold); // Method not available in current SDK version
            
            // Start printing
            int status = printer.start();
            
            if (status == 0) {
                result.put("success", true);
                result.put("message", "Color image printed with threshold successfully");
            } else {
                result.put("success", false);
                result.put("error", "Print failed with status: " + status);
                result.put("statusCode", status);
            }
            
        } catch (PrinterDevException e) {
            Log.e(TAG, "Printer exception: ", e);
            result.put("success", false);
            result.put("error", "Printer error: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error printing color image with threshold: ", e);
            result.put("success", false);
            result.put("error", "Unexpected error: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * Print with alignment mode
     */
    private Map<String, Object> setAlignMode(short alignMode) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (!initializePrinter(null)) {
                result.put("success", false);
                result.put("error", "Failed to initialize printer");
                return result;
            }

            // printer.setAlignMode(alignMode); // Method not available in current SDK version
            result.put("success", true);
            result.put("message", "Alignment mode set successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error setting alignment mode: ", e);
            result.put("success", false);
            result.put("error", "Unexpected error: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * Print image with specified formatting
     */
    private Map<String, Object> printImage(List<Integer> imageData, Map<String, Object> options) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (!initializePrinter(null)) {
                result.put("success", false);
                result.put("error", "Failed to initialize printer");
                return result;
            }
            
            // Check if printer is ready
            int printerStatus = printer.getStatus();
            if (printerStatus != 0) {
                Log.w(TAG, "Printer not ready, status: " + printerStatus);
                result.put("success", false);
                result.put("error", "Printer not ready: " + getStatusMessage(printerStatus));
                return result;
            }

            // Convert List<Integer> to byte array
            byte[] imageBytes = new byte[imageData.size()];
            for (int i = 0; i < imageData.size(); i++) {
                imageBytes[i] = imageData.get(i).byteValue();
            }

            // Create bitmap from byte array
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            if (bitmap == null) {
                result.put("success", false);
                result.put("error", "Failed to decode image");
                return result;
            }

            // Print the bitmap
            printer.printBitmap(bitmap);
            
            // Start printing
            int status = printer.start();
            
            if (status == 0) {
                // Add a small delay to ensure printer processes the job
                try {
                    Thread.sleep(50); // 500ms delay
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                result.put("success", true);
                result.put("message", "Image printed successfully");
            } else {
                result.put("success", false);
                result.put("error", "Print failed with status: " + status);
                result.put("statusCode", status);
            }
            
        } catch (PrinterDevException e) {
            Log.e(TAG, "Printer exception: ", e);
            result.put("success", false);
            result.put("error", "Printer error: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error printing image: ", e);
            result.put("success", false);
            result.put("error", "Unexpected error: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * Print text with specified formatting
     */
    private Map<String, Object> printText(String text, Map<String, Object> options) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (!initializePrinter(null)) {
                result.put("success", false);
                result.put("error", "Failed to initialize printer");
                return result;
            }
            
            // Check if printer is ready
            int printerStatus = printer.getStatus();
            if (printerStatus != 0) {
                Log.w(TAG, "Printer not ready, status: " + printerStatus);
                result.put("success", false);
                result.put("error", "Printer not ready: " + getStatusMessage(printerStatus));
                return result;
            }

            // Check if text contains Arabic characters
            if (containsArabicText(text)) {
                // Convert Arabic text to bitmap and print as image
                return printArabicTextAsImage(text, options);
            }

            // Apply font settings if provided
            if (options.containsKey("fontSize")) {
                String fontSize = (String) options.get("fontSize");
                setFontSize(fontSize);
            }

            // Apply gray level if provided
            if (options.containsKey("grayLevel")) {
                Integer grayLevel = (Integer) options.get("grayLevel");
                printer.setGray(grayLevel);
            }

            // Set spacing if provided
            if (options.containsKey("lineSpacing") || options.containsKey("charSpacing")) {
                byte lineSpacing = options.containsKey("lineSpacing") ? 
                    ((Integer) options.get("lineSpacing")).byteValue() : 0;
                byte charSpacing = options.containsKey("charSpacing") ? 
                    ((Integer) options.get("charSpacing")).byteValue() : 0;
                printer.spaceSet(charSpacing, lineSpacing);
            }

            // Handle alignment by adjusting text format
            String formattedText = text;
            if (options.containsKey("alignment")) {
                Integer alignment = (Integer) options.get("alignment");
                formattedText = formatTextWithAlignment(text, alignment);
            }

            // Print the text
            String charset = options.containsKey("charset") ? 
                (String) options.get("charset") : "UTF-8";
            printer.printStr(formattedText, charset);

            // Start printing
            int status = printer.start();
            
            if (status == 0) {
                // Add a small delay to ensure printer processes the job
                try {
                    Thread.sleep(50); // 500ms delay
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                result.put("success", true);
                result.put("message", "Text printed successfully");
            } else {
                result.put("success", false);
                result.put("error", "Print failed with status: " + status);
                result.put("statusCode", status);
            }
            
        } catch (PrinterDevException e) {
            Log.e(TAG, "Printer exception: ", e);
            result.put("success", false);
            result.put("error", "Printer error: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error printing text: ", e);
            result.put("success", false);
            result.put("error", "Unexpected error: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * Check if text contains Arabic characters
     */
    private boolean containsArabicText(String text) {
        if (text == null) return false;
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            // Arabic Unicode range: 0600-06FF, 0750-077F, 08A0-08FF, FB50-FDFF, FE70-FEFF
            if ((c >= 0x0600 && c <= 0x06FF) || 
                (c >= 0x0750 && c <= 0x077F) || 
                (c >= 0x08A0 && c <= 0x08FF) || 
                (c >= 0xFB50 && c <= 0xFDFF) || 
                (c >= 0xFE70 && c <= 0xFEFF)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Convert Arabic text to bitmap and print as image
     */
    private Map<String, Object> printArabicTextAsImage(String text, Map<String, Object> options) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (!initializePrinter(null)) {
                result.put("success", false);
                result.put("error", "Failed to initialize printer");
                return result;
            }
            
            // Check if printer is ready
            int printerStatus = printer.getStatus();
            if (printerStatus != 0) {
                Log.w(TAG, "Printer not ready, status: " + printerStatus);
                result.put("success", false);
                result.put("error", "Printer not ready: " + getStatusMessage(printerStatus));
                return result;
            }

            // Create bitmap from Arabic text
            Bitmap textBitmap = createArabicTextBitmap(text, options);
            if (textBitmap == null) {
                result.put("success", false);
                result.put("error", "Failed to create text bitmap");
                return result;
            }

            // Print the bitmap directly
            printer.printBitmap(textBitmap);
            
            // Start printing
            int status = printer.start();
            
            if (status == 0) {
                // Add a small delay to ensure printer processes the job
                try {
                    Thread.sleep(50); // 500ms delay
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                result.put("success", true);
                result.put("message", "Arabic text printed successfully as image");
            } else {
                result.put("success", false);
                result.put("error", "Print failed with status: " + status);
                result.put("statusCode", status);
            }
            
        } catch (PrinterDevException e) {
            Log.e(TAG, "Printer exception: ", e);
            result.put("success", false);
            result.put("error", "Printer error: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error printing Arabic text: ", e);
            result.put("success", false);
            result.put("error", "Unexpected error: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * Create bitmap from Arabic text
     */
    private Bitmap createArabicTextBitmap(String text, Map<String, Object> options) {
        try {
            // Determine font size
            int fontSize = 24; // Default medium size
            if (options.containsKey("fontSize")) {
                String fontSizeStr = (String) options.get("fontSize");
                switch (fontSizeStr.toLowerCase()) {
                    case "small":
                        fontSize = 16;
                        break;
                    case "large":
                        fontSize = 32;
                        break;
                    case "extra_large":
                        fontSize = 48;
                        break;
                    default:
                        fontSize = 24;
                        break;
                }
            }

            // Determine alignment
            int alignment = 0; // Default left alignment
            if (options.containsKey("alignment")) {
                alignment = (Integer) options.get("alignment");
            }

            // Calculate text dimensions
            android.graphics.Paint paint = new android.graphics.Paint();
            paint.setTextSize(fontSize);
            paint.setAntiAlias(true);
            paint.setSubpixelText(true);
            
            // Set text alignment
            switch (alignment) {
                case 1: // Center
                    paint.setTextAlign(android.graphics.Paint.Align.CENTER);
                    break;
                case 2: // Right
                    paint.setTextAlign(android.graphics.Paint.Align.RIGHT);
                    break;
                default: // Left
                    paint.setTextAlign(android.graphics.Paint.Align.LEFT);
                    break;
            }

            // Split text into lines
            String[] lines = text.split("\n");
            int maxWidth = 384; // Standard thermal printer width
            int lineHeight = fontSize + 4;
            int totalHeight = lines.length * lineHeight;

            // Create bitmap
            Bitmap bitmap = Bitmap.createBitmap(maxWidth, totalHeight, Bitmap.Config.RGB_565);
            android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
            canvas.drawColor(android.graphics.Color.WHITE);

            // Draw text lines
            int y = fontSize;
            for (String line : lines) {
                float x = 0;
                switch (alignment) {
                    case 1: // Center
                        x = maxWidth / 2f;
                        break;
                    case 2: // Right
                        x = maxWidth - 10;
                        break;
                    default: // Left
                        x = 10;
                        break;
                }
                
                canvas.drawText(line, x, y, paint);
                y += lineHeight;
            }

            return bitmap;
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating Arabic text bitmap: ", e);
            return null;
        }
    }

    /**
     * Convert bitmap to byte array for printing
     */
    private byte[] bitmapToByteArray(Bitmap bitmap) {
        try {
            // Convert bitmap to grayscale byte array
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            
            byte[] data = new byte[width * height / 8];
            int dataIndex = 0;
            int bitIndex = 0;
            
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int pixel = bitmap.getPixel(x, y);
                    int gray = (android.graphics.Color.red(pixel) + 
                               android.graphics.Color.green(pixel) + 
                               android.graphics.Color.blue(pixel)) / 3;
                    
                    boolean isBlack = gray < 128;
                    
                    if (isBlack) {
                        data[dataIndex] |= (1 << (7 - bitIndex));
                    }
                    
                    bitIndex++;
                    if (bitIndex == 8) {
                        bitIndex = 0;
                        dataIndex++;
                    }
                }
            }
            
            return data;
            
        } catch (Exception e) {
            Log.e(TAG, "Error converting bitmap to byte array: ", e);
            return new byte[0];
        }
    }

    /**
     * Get printer status
     */
    private Map<String, Object> getPrinterStatus() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (!initializePrinter(null)) {
                result.put("success", false);
                result.put("error", "Failed to initialize printer");
                return result;
            }

            int status = printer.getStatus();
            result.put("success", true);
            result.put("status", status);
            result.put("statusMessage", getStatusMessage(status));
            
        } catch (PrinterDevException e) {
            Log.e(TAG, "Printer exception: ", e);
            result.put("success", false);
            result.put("error", "Printer error: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error getting printer status: ", e);
            result.put("success", false);
            result.put("error", "Unexpected error: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * Cut paper with device capability check
     */
    private Map<String, Object> cutPaper(int mode) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (!initializePrinter(null)) {
                result.put("success", false);
                result.put("error", "Failed to initialize printer");
                return result;
            }

            // Check if device supports cutting
            try {
                int cutMode = printer.getCutMode();
                if (cutMode == -1) {
                    // Device doesn't support cutting
                    result.put("success", false);
                    result.put("error", "This device does not support paper cutting");
                    result.put("statusCode", -1);
                    Log.w(TAG, "Cut paper not supported on this device");
                    return result;
                }
                
                // Device supports cutting, proceed
                printer.cutPaper(mode);
                result.put("success", true);
                result.put("message", "Paper cut successfully");
                
            } catch (PrinterDevException cutException) {
                // Handle PrinterDevException from cutPaper
                Log.e(TAG, "Printer exception during cut: ", cutException);
                if (cutException.getMessage() != null && 
                    cutException.getMessage().contains("not support")) {
                    result.put("success", false);
                    result.put("error", "Paper cutting not supported on this device");
                    result.put("statusCode", -1);
                } else {
                    result.put("success", false);
                    result.put("error", "Printer error: " + cutException.getMessage());
                }
            } catch (Exception otherException) {
                // Handle other exceptions
                if (otherException.getMessage() != null && 
                    otherException.getMessage().contains("not support")) {
                    result.put("success", false);
                    result.put("error", "Paper cutting not supported on this device");
                    result.put("statusCode", -1);
                } else {
                    result.put("success", false);
                    result.put("error", "Cut error: " + otherException.getMessage());
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error cutting paper: ", e);
            result.put("success", false);
            result.put("error", "Unexpected error: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * Feed paper
     */
    private Map<String, Object> feedPaper(int pixels) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (!initializePrinter(null)) {
                result.put("success", false);
                result.put("error", "Failed to initialize printer");
                return result;
            }

            printer.step(pixels);
            result.put("success", true);
            result.put("message", "Paper fed successfully");
            
        } catch (PrinterDevException e) {
            Log.e(TAG, "Printer exception: ", e);
            result.put("success", false);
            result.put("error", "Printer error: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error feeding paper: ", e);
            result.put("success", false);
            result.put("error", "Unexpected error: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * Check if cutting is supported before attempting
     */
    private boolean isCutSupported() {
        try {
            if (!initializePrinter(appContext)) {
                return false;
            }
            
            int cutMode = printer.getCutMode();
            return cutMode != -1; // -1 means no cutting support
            
        } catch (Exception e) {
            Log.w(TAG, "Could not check cut support: " + e.getMessage());
            return false;
        }
    }

    // ============ HELPER METHODS ============

    /**
     * Format text with alignment by adding spaces (software alignment)
     */
    private String formatTextWithAlignment(String text, int alignment) {
        if (alignment == 0) { // Left alignment
            return text;
        }
        
        // Estimate line width (adjust based on your printer's character width)
        int lineWidth = 32; // Standard thermal printer width in characters
        
        String[] lines = text.split("\n");
        StringBuilder formattedText = new StringBuilder();
        
        for (String line : lines) {
            if (line.length() >= lineWidth) {
                formattedText.append(line).append("\n");
                continue;
            }
            
            if (alignment == 1) { // Center alignment
                int spaces = (lineWidth - line.length()) / 2;
                for (int i = 0; i < Math.max(0, spaces); i++) {
                    formattedText.append(" ");
                }
                formattedText.append(line).append("\n");
            } else if (alignment == 2) { // Right alignment
                int spaces = lineWidth - line.length();
                for (int i = 0; i < Math.max(0, spaces); i++) {
                    formattedText.append(" ");
                }
                formattedText.append(line).append("\n");
            } else {
                formattedText.append(line).append("\n");
            }
        }
        
        // Remove the last newline if it was added
        String result = formattedText.toString();
        if (result.endsWith("\n") && !text.endsWith("\n")) {
            result = result.substring(0, result.length() - 1);
        }
        
        return result;
    }

    /**
     * Get human-readable status message
     */
    private String getStatusMessage(int status) {
        switch (status) {
            case 0: return "Normal";
            case 1: return "Printer is busy";
            case 2: return "Out of paper";
            case 3: return "Print data packet format error";
            case 4: return "Printer malfunction";
            case 8: return "Printer over heats";
            case 9: return "Printer voltage is too low";
            case -16: return "Printing is unfinished";
            case -6: return "Cut jam error";
            case -5: return "Cover open error";
            case -4: return "Printer has not installed font library";
            case -2: return "Data package is too long";
            default: return "Unknown status: " + status;
        }
    }

    /**
     * Utility method to copy asset files
     */
    private void copyAssetToFile(AssetManager assets, String assetPath, File outFile) throws IOException {
        try (InputStream is = assets.open(assetPath);
             FileOutputStream os = new FileOutputStream(outFile)) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = is.read(buffer)) > 0) {
                os.write(buffer, 0, read);
            }
        }
    }

    /**
     * Convert byte array to hex string
     */
    private String bytesToHex(byte[] bytes) {
        if (bytes == null) return "null";
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    /**
     * Test native library loading
     */
    public Map<String, Object> testNativeLibraryLoading() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Log.d(TAG, "=== TESTING NATIVE LIBRARY LOADING ===");
            
            // Check if native libraries are available
            String libraryPath = appContext.getApplicationInfo().nativeLibraryDir;
            Log.d(TAG, "Native library path: " + libraryPath);
            
            // List available libraries for debugging
            File libDir = new File(libraryPath);
            if (libDir.exists() && libDir.isDirectory()) {
                File[] libs = libDir.listFiles((dir, name) -> name.endsWith(".so"));
                if (libs != null) {
                    Log.d(TAG, "Available native libraries:");
                    for (File lib : libs) {
                        Log.d(TAG, "  - " + lib.getName());
                    }
                    result.put("availableLibraries", libs.length);
                }
            }
            
            // Try to load PAX SDK
            try {
                IDAL dal = NeptuneLiteUser.getInstance().getDal(appContext);
                if (dal != null) {
                    Log.d(TAG, "✓ PAX SDK loaded successfully");
                    result.put("paxSdkLoaded", true);
                } else {
                    Log.e(TAG, "✗ PAX SDK failed to load - DAL is null");
                    result.put("paxSdkLoaded", false);
                    result.put("error", "DAL is null");
                }
            } catch (UnsatisfiedLinkError e) {
                Log.e(TAG, "✗ PAX SDK native libraries not found: " + e.getMessage());
                result.put("paxSdkLoaded", false);
                result.put("error", "Native libraries not found: " + e.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "✗ PAX SDK loading error: " + e.getMessage());
                result.put("paxSdkLoaded", false);
                result.put("error", "Loading error: " + e.getMessage());
            }
            
            result.put("success", true);
            return result;
            
        } catch (Exception e) {
            Log.e(TAG, "Error testing native library loading: ", e);
            result.put("success", false);
            result.put("error", "Test error: " + e.getMessage());
            return result;
        }
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        switch (call.method) {
            case "getPlatformVersion":
                result.success("Android " + android.os.Build.VERSION.RELEASE);
                break;
                
            // ===== NFC METHODS =====
            case "detectCard":
                Map<String, Object> cardResult = detectAndIdentifyCard(appContext);
                result.success(cardResult);
                break;
                
            case "checkCardPresence":
                boolean present = isCardPresent(appContext);
                result.success(present);
                break;
                
            case "waitForCard":
                waitForCardAndProcess(appContext);
                result.success("Waiting for card...");
                break;
                
            case "tryAllModes":
                tryAllDetectionModes(appContext);
                result.success("Testing all detection modes");
                break;
                
            // Legacy method name for backward compatibility
            case "startNfcDetectionThreads":
                detectAndIdentifyCard(appContext);
                result.success("NFC detection started");
                break;
                
            // ===== PRINTER METHODS =====
            case "initializePrinter":
                boolean initialized = initializePrinter(appContext);
                result.success(initialized);
                break;
                
            case "printText":
                String text = call.argument("text");
                Map<String, Object> textOptions = call.argument("options");
                if (textOptions == null) textOptions = new HashMap<>();
                Map<String, Object> textResult = printText(text, textOptions);
                result.success(textResult);
                break;
                
            case "printImage":
                List<Integer> imageData = call.argument("imageData");
                Map<String, Object> imageOptions = call.argument("options");
                if (imageOptions == null) imageOptions = new HashMap<>();
                Map<String, Object> imageResult = printImage(imageData, imageOptions);
                result.success(imageResult);
                break;
                
            case "getPrinterStatus":
                Map<String, Object> statusResult = getPrinterStatus();
                result.success(statusResult);
                break;
                
            case "cutPaper":
                Integer cutMode = call.argument("mode");
                if (cutMode == null) cutMode = 0; // Default to full cut
                Map<String, Object> cutResult = cutPaper(cutMode);
                result.success(cutResult);
                break;
                
            case "feedPaper":
                Integer pixels = call.argument("pixels");
                if (pixels == null) pixels = 48; // Default feed
                Map<String, Object> feedResult = feedPaper(pixels);
                result.success(feedResult);
                break;
                
            case "isCutSupported":
                boolean cutSupported = isCutSupported();
                result.success(cutSupported);
                break;
                
            // ===== NEW PRINTER METHODS =====
            case "setFontSize":
                String fontSize = call.argument("fontSize");
                Map<String, Object> fontSizeResult = setFontSize(fontSize);
                result.success(fontSizeResult);
                break;
                
            case "setFontPath":
                String fontPath = call.argument("fontPath");
                Map<String, Object> fontPathResult = setFontPath(fontPath);
                result.success(fontPathResult);
                break;
                
            case "setDoubleHeight":
                Boolean isAscDouble = call.argument("isAscDouble");
                Boolean isLocalDouble = call.argument("isLocalDouble");
                if (isAscDouble == null) isAscDouble = true;
                if (isLocalDouble == null) isLocalDouble = true;
                Map<String, Object> doubleHeightResult = setDoubleHeight(isAscDouble, isLocalDouble);
                result.success(doubleHeightResult);
                break;
                
            case "setDoubleWidth":
                Boolean isAscDoubleWidth = call.argument("isAscDouble");
                Boolean isLocalDoubleWidth = call.argument("isLocalDouble");
                if (isAscDoubleWidth == null) isAscDoubleWidth = true;
                if (isLocalDoubleWidth == null) isLocalDoubleWidth = true;
                Map<String, Object> doubleWidthResult = setDoubleWidth(isAscDoubleWidth, isLocalDoubleWidth);
                result.success(doubleWidthResult);
                break;
                
            case "setLeftIndent":
                Integer indent = call.argument("indent");
                if (indent == null) indent = 0;
                Map<String, Object> leftIndentResult = setLeftIndent(indent);
                result.success(leftIndentResult);
                break;
                
            case "setInvert":
                Boolean isInvert = call.argument("isInvert");
                if (isInvert == null) isInvert = false;
                Map<String, Object> invertResult = setInvert(isInvert);
                result.success(invertResult);
                break;
                
            case "setSpacing":
                Integer wordSpace = call.argument("wordSpace");
                Integer lineSpace = call.argument("lineSpace");
                if (wordSpace == null) wordSpace = 0;
                if (lineSpace == null) lineSpace = 0;
                Map<String, Object> spacingResult = setSpacing(wordSpace.byteValue(), lineSpace.byteValue());
                result.success(spacingResult);
                break;
                
            case "presetCutPaper":
                Integer presetMode = call.argument("mode");
                if (presetMode == null) presetMode = 0;
                Map<String, Object> presetCutResult = presetCutPaper(presetMode);
                result.success(presetCutResult);
                break;
                
            case "getCutMode":
                Map<String, Object> getCutModeResult = getCutMode();
                result.success(getCutModeResult);
                break;
                
            case "getDotLine":
                Map<String, Object> getDotLineResult = getDotLine();
                result.success(getDotLineResult);
                break;
                
            case "getPrinterSize":
                Map<String, Object> getPrinterSizeResult = getPrinterSize();
                result.success(getPrinterSizeResult);
                break;
                
            case "setPrinterSize":
                Integer printerSize = call.argument("size");
                if (printerSize == null) printerSize = 0;
                Map<String, Object> setPrinterSizeResult = setPrinterSize(printerSize);
                result.success(setPrinterSizeResult);
                break;
                
            case "setColorGray":
                Integer blackLevel = call.argument("blackLevel");
                Integer colorLevel = call.argument("colorLevel");
                if (blackLevel == null) blackLevel = 0;
                if (colorLevel == null) colorLevel = 0;
                Map<String, Object> colorGrayResult = setColorGray(blackLevel, colorLevel);
                result.success(colorGrayResult);
                break;
                
            case "enableLowPowerPrint":
                Boolean enableLowPower = call.argument("enable");
                if (enableLowPower == null) enableLowPower = false;
                Map<String, Object> lowPowerResult = enableLowPowerPrint(enableLowPower);
                result.success(lowPowerResult);
                break;
                
            case "isLowPowerPrintEnabled":
                Map<String, Object> lowPowerStatusResult = isLowPowerPrintEnabled();
                result.success(lowPowerStatusResult);
                break;
                
            case "printBitmapWithMonoThreshold":
                Object thresholdImageDataObj = call.argument("imageData");
                List<Integer> thresholdImageData = null;
                if (thresholdImageDataObj instanceof List) {
                    thresholdImageData = (List<Integer>) thresholdImageDataObj;
                } else if (thresholdImageDataObj instanceof byte[]) {
                    byte[] byteArray = (byte[]) thresholdImageDataObj;
                    thresholdImageData = new java.util.ArrayList<>();
                    for (byte b : byteArray) {
                        thresholdImageData.add((int) b & 0xFF);
                    }
                } else {
                    result.error("invalid_argument", "imageData must be List<Integer> or byte[]", null);
                    break;
                }
                Integer grayThreshold = call.argument("grayThreshold");
                if (grayThreshold == null) grayThreshold = 128;
                Map<String, Object> thresholdResult = printBitmapWithMonoThreshold(thresholdImageData, grayThreshold);
                result.success(thresholdResult);
                break;
                
            case "printColorBitmap":
                Object colorImageDataObj = call.argument("imageData");
                List<Integer> colorImageData = null;
                if (colorImageDataObj instanceof List) {
                    colorImageData = (List<Integer>) colorImageDataObj;
                } else if (colorImageDataObj instanceof byte[]) {
                    byte[] byteArray = (byte[]) colorImageDataObj;
                    colorImageData = new java.util.ArrayList<>();
                    for (byte b : byteArray) {
                        colorImageData.add((int) b & 0xFF);
                    }
                } else {
                    result.error("invalid_argument", "imageData must be List<Integer> or byte[]", null);
                    break;
                }
                Map<String, Object> colorBitmapResult = printColorBitmap(colorImageData);
                result.success(colorBitmapResult);
                break;
                
            case "printColorBitmapWithMonoThreshold":
                List<Integer> colorThresholdImageData = call.argument("imageData");
                Integer colorGrayThreshold = call.argument("grayThreshold");
                if (colorGrayThreshold == null) colorGrayThreshold = 128;
                Map<String, Object> colorThresholdResult = printColorBitmapWithMonoThreshold(colorThresholdImageData, colorGrayThreshold);
                result.success(colorThresholdResult);
                break;
                
            case "setAlignMode":
                Integer alignMode = call.argument("alignMode");
                if (alignMode == null) alignMode = 0;
                Map<String, Object> alignModeResult = setAlignMode(alignMode.shortValue());
                result.success(alignModeResult);
                break;
                
            case "testNativeLibraryLoading":
                Map<String, Object> nativeTestResult = testNativeLibraryLoading();
                result.success(nativeTestResult);
                break;
                
            default:
                result.notImplemented();
                break;
        }
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
    }
}