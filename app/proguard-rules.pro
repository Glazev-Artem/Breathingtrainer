# SDK-зависимости поставляют собственные consumer ProGuard rules. Широкие keep/dontwarn
# здесь намеренно не используются: они скрывали реальные ошибки и запрещали R8 удалять
# неиспользуемый код целых SDK.

# Release APK must not retain account, purchase, SDK or exception details in logcat.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
}
