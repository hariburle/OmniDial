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

# Room and libphonenumber both ship their own consumer ProGuard rules. The broad
# `-keep ... { *; }` rules that used to live here barred R8 from optimizing exactly the two
# libraries on the app's hot paths (database access and phone-number parsing).
# The one genuinely load-bearing rule -- keeping RoomDatabase subclasses, which Room loads
# reflectively as `<Name>_Impl` -- is also shipped by room-runtime, but is kept here explicitly
# so a future dependency bump cannot silently break database instantiation.

# Room database rules
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.**
-dontwarn androidx.sqlite.db.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Google libphonenumber metadata and classes
-dontwarn com.google.i18n.phonenumbers.**
-dontwarn com.google.i18n.phonenumbers.data.**

