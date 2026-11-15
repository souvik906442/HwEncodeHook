package com.bhakt.hwencodehook;

import android.media.MediaMetadataRetriever;

import java.io.File;
import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {

    private static final String TARGET_PKG = "com.videoconverter.videocompressor";
    private static final String TAG = "[HwEncodeHook] ";
    private static final String EXTRA_ORIG = "hw_orig_args";
    private static final int RETURN_OK = 0;
    private static final int RETURN_CANCEL = 255;
    private static final String EXTRA_INSIZE = "hw_in_size";
    private static final double BITRATE_FRACTION = 0.4;
    private static final double MAX_OUT_RATIO = 0.9;

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
                                    String in = valueAfter(orig, "-i");
                                    long inSize = in == null ? 0 : new File(in).length();
                                    param.setObjectExtra(EXTRA_INSIZE, Long.valueOf(inSize));
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
                                boolean tooBig = false;
                                if (code == RETURN_OK) {
                                    Object o = param.getObjectExtra(EXTRA_INSIZE);
                                    long inSize = (o instanceof Long) ? (Long) o : 0L;
                                    File out = new File(orig[orig.length - 1]);
                                    if (inSize > 0 && out.exists() && out.length() > inSize * MAX_OUT_RATIO) {
                                        tooBig = true;
                                        XposedBridge.log(TAG + "output " + out.length() + " B is not smaller than input "
                                                + inSize + " B");
                                    }
                                }
                                if (tooBig || (code != RETURN_OK && code != RETURN_CANCEL)) {
                                    XposedBridge.log(TAG + "hardware encode failed or too big (" + code
                                            + "), retry with software x264");
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

    static String[] rewrite(String[] args) {
        if (args == null) return null;
        boolean has264 = false;
        for (String a : args) {
            if ("libx264".equals(a)) { has264 = true; break; }
        }
        if (!has264) return null;

        List<String> out = new ArrayList<>(args.length);
        long cur = 0;
        for (int i = 0; i < args.length - 1; i++) {
            if ("-b:v".equals(args[i]) || "-vb".equals(args[i])) cur = parseBitrate(args[i + 1]);
        }
        String in = valueAfter(args, "-i");
        long src = in == null ? 0 : probeBitrate(in);
        long target = src > 0 ? (long) (src * BITRATE_FRACTION) : 0;
        boolean setBr = false;
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if (DROP_WITH_VALUE.contains(a)) {
                i++;
                continue;
            }
            if ("-b:v".equals(a) || "-vb".equals(a)) {
                long use = cur;
                if (target > 0 && (cur <= 0 || cur > target)) use = target;
                out.add("-b:v");
                out.add(String.valueOf(use > 0 ? use : cur));
                setBr = true;
                i++;
                continue;
            }
            if ("libx264".equals(a)) {
                out.add("h264_mediacodec");
            } else {
                out.add(a);
            }
        }
        if (!setBr) {
            long use = target > 0 ? target : 1_000_000L;
            int pos = out.size() - 1;
            out.add(pos, String.valueOf(use));
            out.add(pos, "-b:v");
        }
        return out.toArray(new String[0]);
    }

    static String valueAfter(String[] args, String key) {
        for (int i = 0; i < args.length - 1; i++) {
            if (key.equals(args[i])) return args[i + 1];
        }
        return null;
    }

    static long parseBitrate(String v) {
        try {
            String t = v.trim().toLowerCase();
            double m = 1;
            if (t.endsWith("k")) { m = 1000; t = t.substring(0, t.length() - 1); }
            else if (t.endsWith("m")) { m = 1_000_000; t = t.substring(0, t.length() - 1); }
            return (long) (Double.parseDouble(t) * m);
        } catch (Throwable e) {
            return 0;
        }
    }

    static long probeBitrate(String path) {
        MediaMetadataRetriever r = new MediaMetadataRetriever();
        try {
            r.setDataSource(path);
            String b = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
            return b == null ? 0 : Long.parseLong(b);
        } catch (Throwable e) {
            return 0;
        } finally {
            try { r.release(); } catch (Throwable ignored) { }
        }
    }
}
