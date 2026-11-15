package com.bhakt.hwencodehook;

import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Video Compressor & Converter (com.videoconverter.videocompressor) ffmpeg-kit দিয়ে libx264 (CPU)
 * ব্যবহার করে। এই hook ffmpeg চালানোর ঠিক আগে argument বদলে h264_mediacodec (hardware) বসায়।
 * hardware encode ব্যর্থ হলে আগের (মূল) argument দিয়ে আবার চালায়।
 */
public class MainHook implements IXposedHookLoadPackage {

    private static final String TARGET_PKG = "com.videoconverter.videocompressor";
    private static final String TAG = "[HwEncodeHook] ";
    private static final String EXTRA_ORIG = "hw_orig_args";
    private static final int RETURN_OK = 0;
    private static final int RETURN_CANCEL = 255;

    // mediacodec encoder এই option গুলো বোঝে না, তাই value সহ বাদ যাবে
    private static final List<String> DROP_WITH_VALUE = Arrays.asList(
            "-crf", "-preset", "-profile:v", "-tune", "-x264-params", "-x264opts",
            "-color_range", "-level", "-level:v");

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!TARGET_PKG.equals(lpparam.packageName)) return;

        try {
            XposedHelpers.findAndHookMethod(
                    "com.arthenica.ffmpegkit.FFmpegKitConfig",
                    lpparam.classLoader,
                    "nativeFFmpegExecute",
                    long.class, String[].class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            try {
                                String[] orig = (String[]) param.args[1];
                                String[] repl = rewrite(orig);
                                if (repl != null) {
                                    param.setObjectExtra(EXTRA_ORIG, orig);
                                    param.args[1] = repl;
                                    XposedBridge.log(TAG + "hardware encode: " + Arrays.toString(repl));
                                }
                            } catch (Throwable t) {
                                XposedBridge.log(TAG + "rewrite failed: " + t);
                            }
                        }

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            try {
                                String[] orig = (String[]) param.getObjectExtra(EXTRA_ORIG);
                                if (orig == null) return;
                                Object res = param.getResult();
                                int code = (res instanceof Integer) ? (Integer) res : RETURN_OK;
                                if (code != RETURN_OK && code != RETURN_CANCEL) {
                                    XposedBridge.log(TAG + "hardware encode failed (" + code
                                            + "), software x264 দিয়ে আবার চেষ্টা");
                                    Object[] a = new Object[]{param.args[0], orig};
                                    Object r2 = XposedBridge.invokeOriginalMethod(
                                            (Member) param.method, param.thisObject, a);
                                    param.setResult(r2);
                                }
                            } catch (Throwable t) {
                                XposedBridge.log(TAG + "fallback failed: " + t);
                            }
                        }
                    });
            XposedBridge.log(TAG + "hooked in " + lpparam.processName);
        } catch (Throwable t) {
            XposedBridge.log(TAG + "hook failed: " + t);
        }
    }

    /** libx264 না থাকলে null ফেরত দেয় (কিছু বদলানো হয় না)। */
    static String[] rewrite(String[] args) {
        if (args == null) return null;
        boolean has264 = false;
        for (String a : args) {
            if ("libx264".equals(a)) { has264 = true; break; }
        }
        if (!has264) return null;

        List<String> out = new ArrayList<>(args.length);
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if (DROP_WITH_VALUE.contains(a)) {
                i++; // value ও বাদ
                continue;
            }
            if ("libx264".equals(a)) {
                out.add("h264_mediacodec");
            } else {
                out.add(a);
            }
        }
        return out.toArray(new String[0]);
    }
}
