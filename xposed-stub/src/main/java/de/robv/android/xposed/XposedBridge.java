package de.robv.android.xposed;

import java.lang.reflect.Member;

public class XposedBridge {
    public static void log(String text) { throw new RuntimeException("stub"); }

    public static void log(Throwable t) { throw new RuntimeException("stub"); }

    public static Object invokeOriginalMethod(Member method, Object thisObject, Object[] args) throws Throwable {
        throw new RuntimeException("stub");
    }
}
