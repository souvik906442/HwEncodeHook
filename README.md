# HwEncodeHook

LSPosed module: Video Compressor & Converter (`com.videoconverter.videocompressor`) অ্যাপের
`libx264` (CPU) encode বদলে `h264_mediacodec` (hardware) চালায়।

## ইনস্টল
1. GitHub-এ push করুন, Actions থেকে `HwEncodeHook-debug` artifact নামান, APK ইনস্টল করুন।
2. LSPosed -> Modules -> HW Encode Hook চালু করুন, scope-এ Video Compressor টিক দিন।
3. Video Compressor অ্যাপ Force stop করে আবার খুলুন।

## কাজ না করলে
LSPosed -> Logs (Modules) এ `[HwEncodeHook]` লেখা লাইন দেখুন।
- `hooked in ...` : hook বসেছে
- `hardware encode: [...]` : argument বদলানো হয়েছে
- `hardware encode failed` : hardware ব্যর্থ, software দিয়ে আবার চলেছে
