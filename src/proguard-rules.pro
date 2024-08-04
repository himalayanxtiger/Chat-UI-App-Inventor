# Add any ProGuard configurations specific to this
# extension here.

-keep public class com.xtiger.chatui.ChatUI {
    public *;
 }
-keeppackagenames gnu.kawa**, gnu.expr**

-optimizationpasses 4
-allowaccessmodification
-mergeinterfacesaggressively

-repackageclasses 'com/xtiger/chatui/repack'
-flattenpackagehierarchy
-dontpreverify
-dontwarn