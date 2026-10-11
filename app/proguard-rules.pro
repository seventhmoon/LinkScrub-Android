# ProGuard / R8 rules for LinkScrub

# Preserve annotations and signatures needed for reflection and protobuf schema generation
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Firebase Performance / Protocol Buffers (Protobuf Lite)
# Protobuf Lite uses reflection (MessageSchema.reflectField) to look up generated fields by name (e.g. sessionId_, isAuto_).
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
    <methods>;
}
-keep class com.google.protobuf.GeneratedMessageLite { *; }

# Keep Firebase Performance metrics and session message models
-keep class com.google.firebase.perf.v1.** { *; }


