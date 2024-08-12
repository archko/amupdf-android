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

public class TTSEngine {

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

    public static final String UTTERANCE_ID_DONE = "dragon";
    private static final String TAG = "TTSEngine";
    private static TTSEngine INSTANCE = new TTSEngine();
    private TextToSpeech textToSpeech;
    private boolean initStatus = false;
    private Map<String, String> content = new HashMap<>();
    private List<String> keys = new ArrayList<>();

    public boolean isInitStatus() {
        return initStatus;
    }

    private OnInitListener listener = new OnInitListener() {

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

    public static TTSEngine get() {
        return INSTANCE;
    }

    public void shutdown() {
        Logcat.d(TAG, "shutdown");

        if (textToSpeech != null) {
            textToSpeech.shutdown();
        }
        textToSpeech = null;
        reset();
    }

    public void reset() {
        content.clear();
        keys.clear();
    }

    public TextToSpeech getTTS() {
        return getTTS(null);
    }

    public TextToSpeech getTTS(OnInitListener onLisnter) {
        List<String> engines = getEngines(App.Companion.getInstance());
        if (null == engines || engines.isEmpty()) {
            onLisnter.onInit(TextToSpeech.ERROR);
            return null;
        }
        if (textToSpeech != null) {
            return textToSpeech;
        }
        if (onLisnter == null) {
            onLisnter = listener;
        }
        textToSpeech = new TextToSpeech(App.Companion.getInstance(), onLisnter);

        return textToSpeech;
    }

    public void stop() {
        if (null != textToSpeech) {
            textToSpeech.stop();
        }
    }

    public boolean resume() {
        if (null == textToSpeech || content.isEmpty() || keys.isEmpty()) {
            Logcat.d(TAG, "no content.");
            return false;
        }
        if (content.size() != keys.size()) {
            Logcat.d(TAG, "something wrong.");
            keys.clear();
            keys.addAll(content.keySet());
        }

        textToSpeech.stop();
        String key = keys.remove(keys.size() - 1);
        speak(key, content.get(key));

        return true;
    }

    public void speak(final String key, final String text) {
        if (textToSpeech == null) {
            Logcat.d(TAG, "textToSpeech not initialized");
            return;
        }

        content.put(key, text);
        keys.add(key);
        textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                //Logcat.d(TAG, "onStart:" + utteranceId);
            }

            @Override
            public void onDone(String utteranceId) {
                //Logcat.d(TAG, "onDone:" + utteranceId);
                keys.remove(utteranceId);
                content.remove(utteranceId);
            }

            @Override
            public void onError(String utteranceId) {
                Logcat.d(TAG, "onError:" + utteranceId);
                keys.remove(utteranceId);
                content.remove(utteranceId);
            }
        });
        textToSpeech.speak(text, TextToSpeech.QUEUE_ADD, null, text);
    }
}
