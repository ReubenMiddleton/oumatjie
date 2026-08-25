# Most libraries here (Retrofit, OkHttp, kotlinx.serialization, Play services) ship their own
# consumer rules inside their AAR, so a lot of this is redundant with those — kept explicit
# anyway so the requirement is visible here rather than only inside a dependency's jar, and
# because androidx.pdf is an alpha library without a long R8 track record. See
# docs/DECISIONS.md, "R8 / release build".

# Retrofit: keep the annotations and generic signatures its dynamic proxy needs at runtime.
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, AnnotationDefault
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>

# kotlinx.serialization: keep the compiler-generated $serializer companions for our own DTOs.
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class com.granify.app.data.gmail.** {
    *** Companion;
}
-keepclasseswithmembers class com.granify.app.data.gmail.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.granify.app.data.gmail.**$$serializer { *; }

# Fragments (ours and the library's) are recreated by FragmentManager via reflection when
# restoring state, so their no-arg constructors must survive shrinking.
-keep public class * extends androidx.fragment.app.Fragment

# androidx.pdf is 1.0.0-alpha19 with no long track record against R8; keep it whole rather
# than risk a shrink-only crash in a library we can't easily patch ourselves.
-keep class androidx.pdf.** { *; }
-dontwarn androidx.pdf.**
