package de.robv.android.xposed;

import java.lang.reflect.Member;

public abstract class XC_MethodHook {
    public XC_MethodHook() {}

    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {}

    protected void afterHookedMethod(MethodHookParam param) throws Throwable {}

    public class Unhook {}

    public static class MethodHookParam {
        public Member method;
        public Object thisObject;
        public Object[] args;

        public Object getResult() { throw new RuntimeException("stub"); }
        public void setResult(Object result) { throw new RuntimeException("stub"); }
        public Throwable getThrowable() { throw new RuntimeException("stub"); }
        public boolean hasThrowable() { throw new RuntimeException("stub"); }
        public void setObjectExtra(String key, Object value) { throw new RuntimeException("stub"); }
        public Object getObjectExtra(String key) { throw new RuntimeException("stub"); }
    }
}
