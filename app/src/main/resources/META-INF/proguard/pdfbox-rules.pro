# PDFBox specific rules

# Ignore references to JP2Decoder
-dontwarn com.gemalto.jp2.JP2Decoder

# Other PDFBox dependencies
-dontwarn javax.xml.bind.**
-dontwarn javax.activation.**
-dontwarn java.lang.invoke.**
-dontwarn org.bouncycastle.**

# Keep PDFBox classes
-keep class com.tom_roush.pdfbox.** { *; }
-keep class com.tom_roush.fontbox.** { *; }
-keep class com.tom_roush.xmpbox.** { *; }
-keep class com.tom_roush.harmony.awt.** { *; } 