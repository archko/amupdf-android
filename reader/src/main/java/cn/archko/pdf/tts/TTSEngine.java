package cn.archko.pdf.tts;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import cn.archko.pdf.core.App;
import cn.archko.pdf.core.common.Logcat;
import cn.archko.pdf.core.utils.Utils;

public class TTSEngine {

    private static final String TAG = "TTSEngine";

    public static List<String> getEngines(Context context) {
        ArrayList<String> engines = new ArrayList<>();
        PackageManager pm = context.getPackageManager();
        Intent intent = new Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE);
        List<ResolveInfo> resolveInfo = pm.queryIntentServices(intent, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo info : resolveInfo) {
            engines.add(info.serviceInfo.packageName);
        }
        return engines;
    }

    public static TTSEngine get() {
        return Factory.instance;
    }

    private static final class Factory {
        private static final TTSEngine instance = new TTSEngine();
    }

    private TextToSpeech textToSpeech;
    private boolean initStatus = false;
    private final Map<String, String> keys = new HashMap<>();
    private final List<String> ttsContent = new ArrayList<>();

    public boolean isInitStatus() {
        return initStatus;
    }

    private final OnInitListener listener = new OnInitListener() {

        @Override
        public void onInit(int status) {
            if (status == TextToSpeech.SUCCESS) {
                initStatus = true;
                String content = "语言数据丢失或不支持该语言";
                //textToSpeech.speak(content, TextToSpeech.QUEUE_ADD, null, null);
                //textToSpeech.speak("requestedVisible:true, getLeash:Surface(name=Surface(name=ea7ad61 NavigationBar0", TextToSpeech.QUEUE_ADD, null, null);
                //textToSpeech.speak("TTS是语音合成应用的一种，它将储存于电脑中的文件，如帮助文件或者网页，转换成自然语音输出。\n" +
                //        "著作权归作者所有。商业转载请联系作者获得授权，非商业转载请注明出处。", TextToSpeech.QUEUE_ADD, null, null);
                //设置首选语言为中文,注意，语言可能是不可用的，结果将指示此
                int result = textToSpeech.setLanguage(Locale.CHINA);
                Log.e(TAG, "初始化:" + result);
                if (result == TextToSpeech.LANG_AVAILABLE) {
                    //检查文档中其他可能的结果代码。
                    // 例如，语言可能对区域设置可用，但对指定的国家和变体不可用
                    // TTS引擎已成功初始化。
                    // 允许用户按下按钮让应用程序再次发言。
                } else {
                    //语言数据丢失或不支持该语言。
                }
            } else {
                // 初始化失败
                Log.e(TAG, "初始化失败");
            }
        }
    };
    private ProgressListener progressListener;

    public void setSpeakListener(ProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    public void shutdown() {
        Logcat.d(TAG, "shutdown");

        if (textToSpeech != null) {
            textToSpeech.shutdown();
        }
        textToSpeech = null;
        initStatus = false;
        reset();
    }

    public void reset() {
        keys.clear();
        ttsContent.clear();
    }

    public void getTTS(OnInitListener listener) {
        if (listener == null) {
            listener = this.listener;
        }
        List<String> engines = getEngines(App.Companion.getInstance());
        if (null == engines || engines.isEmpty()) {
            listener.onInit(TextToSpeech.ERROR);
            return;
        }
        if (textToSpeech != null) {
            initStatus = true;
            listener.onInit(TextToSpeech.SUCCESS);
            return;
        }
        textToSpeech = new TextToSpeech(App.Companion.getInstance(), listener);
        textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                if (null != progressListener) {
                    String key = keys.get(utteranceId);
                    //Logcat.d(TAG, "onStart:" + utteranceId + " key:" + key);
                    progressListener.onStart(key);
                }
            }

            //utteranceId这个是内容
            @Override
            public void onDone(String utteranceId) {
                //Logcat.d(TAG, "onDone:" + utteranceId);
                ttsContent.remove(utteranceId);
                String key = keys.remove(utteranceId);
                //if (null != progressListener) {
                //    progressListener.onDone(key);
                //}
            }

            @Override
            public void onError(String utteranceId) {
                Logcat.d(TAG, "onError:" + utteranceId);
                ttsContent.remove(utteranceId);
                keys.remove(utteranceId);
            }
        });
    }

    public void stop() {
        if (null != textToSpeech) {
            textToSpeech.stop();
        }
    }

    public boolean resume() {
        if (null == textToSpeech || keys.isEmpty() || ttsContent.isEmpty()) {
            Logcat.d(TAG, "no content.");
            return false;
        }
        if (keys.size() != ttsContent.size()) {
            Logcat.d(TAG, "something wrong.");
            ttsContent.clear();
            ttsContent.addAll(keys.keySet());
        }

        textToSpeech.stop();
        for (int i = 0; i < ttsContent.size(); i++) {
            String content = ttsContent.get(i);
            resumeSpeak(content);
        }
        return true;
    }

    public int getLast() {
        if (!ttsContent.isEmpty()) {
            String ss = keys.get(ttsContent.size() - 1);
            String[] arr = ss.split("-");
            try {
                return Utils.parseInt(arr[0]);
            } catch (Exception e) {
                Logcat.e(ss, e);
            }
        }
        return 0;
    }

    public boolean isSpeaking() {
        if (textToSpeech == null) {
            Logcat.d(TAG, "textToSpeech not initialized");
            return false;
        }

        return textToSpeech.isSpeaking();
    }

    public void resumeSpeak(final String text) {
        if (textToSpeech == null) {
            Logcat.d(TAG, "textToSpeech not initialized");
            return;
        }

        textToSpeech.speak(text, TextToSpeech.QUEUE_ADD, null, text);
    }

    public void speak(final String key, final String text) {
        if (textToSpeech == null) {
            Logcat.d(TAG, "textToSpeech not initialized");
            return;
        }

        keys.put(text, key);
        ttsContent.add(text);
        textToSpeech.speak(text, TextToSpeech.QUEUE_ADD, null, text);
    }

    public interface ProgressListener {
        void onStart(String utteranceId);

        void onDone(String utteranceId);
    }
}
