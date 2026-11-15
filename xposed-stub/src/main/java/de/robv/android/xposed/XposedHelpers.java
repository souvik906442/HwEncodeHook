package de.robv.android.xposed;

public class XposedHelpers {
    public static Class<?> findClass(String className, ClassLoader classLoader) {
        throw new RuntimeException("stub");
    }

    public static XC_MethodHook.Unhook findAndHookMethod(String className, ClassLoader classLoader,
                                                         String methodName, Object... parameterTypesAndCallback) {
        throw new RuntimeException("stub");
    }
}
