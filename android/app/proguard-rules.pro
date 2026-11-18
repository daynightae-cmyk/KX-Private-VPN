# KX Private VPN — release shrinking rules
# Keep the WireGuard tunnel, Compose, and reflection-sensitive code intact.

# WireGuard Android tunnel library (Go backend + Java bindings)
-keep class com.wireguard.** { *; }
-dontwarn com.wireguard.**
-keep class go.** { *; }
-dontwarn go.**

# kotlinx.coroutines used by the VPN state machine
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# ZXing QR decoding
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# DataStore / preferences persistence
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# Compose / lifecycle
-keep class androidx.lifecycle.** { *; }
-dontwarn androidx.lifecycle.**
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep the app's own model classes that are persisted or parsed reflectively
-keep class com.knoux.kxprivatevpn.data.** { *; }
-keep class com.knoux.kxprivatevpn.platform.** { *; }

# Never obfuscate the VPN service so the system can bind it
-keep class com.knoux.kxprivatevpn.platform.vpn.** { *; }

# Remove logging in release (defense in depth for sensitive connection metadata)
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
    public static int i(...);
}
