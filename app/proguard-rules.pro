-keep class com.masar.portal.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# Kotlinx Serialization
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keepclassmembers class **$$serializer { *; }
-keepclassmembers class * { *** Companion; }
-keepclasseswithmembers class * { kotlinx.serialization.KSerializer serializer(...); }
