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
# ============================================================
# Project rules (R8 / minifyEnabled true)
# ============================================================

# Keep line numbers for readable Crashlytics stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes Signature,InnerClasses,EnclosingMethod,Exceptions
-keepattributes *Annotation*,RuntimeVisible*Annotations,AnnotationDefault

# --- App models (Gson / Room / DB cache) ---
-keep class com.iptv.online.smart.liveplayer.tv.Model.** { *; }
-keep class com.iptv.online.smart.liveplayer.tv.Ads.AdUnitConfig { *; }
-keep class com.iptv.online.smart.liveplayer.tv.adsutils.** { *; }

# --- Gson ---
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# --- Retrofit / OkHttp ---
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keep,allowobfuscation interface retrofit2.** { *; }
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# --- ERain Studio ad SDK + AdMob mediation ---
-keep class com.ads.module.** { *; }
-dontwarn com.ads.module.**
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.android.gms.internal.ads.** { *; }
-dontwarn com.google.android.gms.**
-keep class com.applovin.** { *; }
-dontwarn com.applovin.**
-keep class com.bytedance.** { *; }
-dontwarn com.bytedance.**
-keep class com.mbridge.** { *; }
-dontwarn com.mbridge.**
-keep class com.ironsource.** { *; }
-dontwarn com.ironsource.**
-keep class com.unity3d.** { *; }
-dontwarn com.unity3d.**
-keep class com.vungle.** { *; }
-dontwarn com.vungle.**
-keep class com.facebook.ads.** { *; }
-dontwarn com.facebook.ads.**

# --- Adjust / Facebook SDK (used by the ad SDK) ---
-keep class com.adjust.sdk.** { *; }
-dontwarn com.adjust.sdk.**
-keep class com.facebook.** { *; }
-dontwarn com.facebook.**

# --- Billing ---
-keep class com.android.billingclient.api.** { *; }

# --- Firebase / Crashlytics / Messaging / Remote Config ---
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keepnames class com.iptv.online.smart.liveplayer.tv.FirebaseMessageReceiver

# --- OneSignal ---
-keep class com.onesignal.** { *; }
-dontwarn com.onesignal.**

# --- GDPR / DevConfig modules (jitpack) ---
-keep class com.itg.** { *; }
-dontwarn com.itg.**

# --- ExoPlayer ---
-keep class com.google.android.exoplayer2.** { *; }
-dontwarn com.google.android.exoplayer2.**

# --- Glide ---
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { <init>(...); }
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** { **[] $VALUES; public *; }

# --- Lottie / Shimmer / Calligraphy (reflection on views) ---
-keep class com.airbnb.lottie.** { *; }
-dontwarn com.airbnb.lottie.**
-keep class io.github.inflationx.** { *; }

# --- Room ---
-keep class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.paging.**

# --- Hilt / Dagger ---
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep,allowobfuscation @interface dagger.hilt.android.AndroidEntryPoint

# --- Android framework bits used via reflection ---
-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}
-keepclassmembers class **.R$* { public static <fields>; }
-keep class * implements android.os.Parcelable { public static final ** CREATOR; }
-keepclassmembers enum * { *; }

# Kotlin
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
