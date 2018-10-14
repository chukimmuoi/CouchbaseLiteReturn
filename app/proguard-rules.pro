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
#============================================Couchbase==============================================
#https://github.com/couchbase/couchbase-lite-android/issues/806#issuecomment-209709473
-keep class com.couchbase.touchdb.TDCollateJSON { *; }
-dontwarn com.couchbase.touchdb.TDCollateJSON.**

-keep class com.couchbase.lite.**{ *; }
-dontwarn com.couchbase.lite.**

-keep class com.couchbase.lite.util.**{ *; }
-dontwarn com.couchbase.lite.util.**

-keep class com.couchbase.lite.store.**{ *; }
-dontwarn com.couchbase.lite.store.**

-keep class com.couchbase.lite.Manager.**{ *; }
-dontwarn com.couchbase.lite.Manager.**

-keep class com.couchbase.lite.Database.**{ *; }
-dontwarn com.couchbase.lite.Database.**

-keep class com.couchbase.cbforest.**{ *; }
-dontwarn com.couchbase.cbforest.**

-keep class couchbase.lite.listener.**{ *; }
-dontwarn couchbase.lite.listener.**

-keep class Acme.Serve.Serve.**{ *; }
-dontwarn Acme.Serve.Serve.**

-keep class Acme.Serve.SimpleAcceptor.**{ *; }
-dontwarn Acme.Serve.SimpleAcceptor.**

-keep class com.couchbase.lite.listener.LiteServer.serve.**{ *; }
-dontwarn com.couchbase.lite.listener.LiteServer.serve.**

-keep class Acme.Serve.**{ *; }
-dontwarn Acme.Serve.**

#https://stackoverflow.com/questions/12583148/proguard-cant-find-referenced-class
-keep class javax.servlet.*{ *; }
-dontwarn javax.servlet.**

# jackson
-keepattributes *Annotation*,EnclosingMethod,Signature
-keepnames class com.fasterxml.jackson.** { *; }
 -dontwarn com.fasterxml.jackson.databind.**
 -keep class org.codehaus.** { *; }
 -keepclassmembers public final enum org.codehaus.jackson.annotate.JsonAutoDetect$Visibility {
 public static final org.codehaus.jackson.annotate.JsonAutoDetect$Visibility *; }
-keep public class your.class.** {
  public void set*(***);
  public *** get*();
}
#============================================Couchbase==============================================
