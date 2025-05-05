package cn.archko.pdf.tts;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.UtteranceProgressListener;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import cn.archko.pdf.core.App;
import cn.archko.pdf.core.common.Logcat;
import cn.archko.pdf.core.entity.ReflowBean;
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
    //保存正在进行中的数据列表
    private final Map<String, ReflowBean> keys = new HashMap<>();
    private final List<ReflowBean> ttsContent = new ArrayList<>();

    public boolean isInitStatus() {
        return initStatus;
    }

    private final OnInitListener listener = new OnInitListener() {

        @Override
        public void onInit(int status) {
            if (status == TextToSpeech.SUCCESS) {
                initStatus = true;
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
    private TtsProgressListener progressListener;

    public List<ReflowBean> getTtsContent() {
        return ttsContent;
    }

    public void setSpeakListener(TtsProgressListener progressListener) {
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
                    ReflowBean key = keys.get(utteranceId);
                    Logcat.d(TAG, "onStart:" + utteranceId + " key:" + key);
                    if (null != key) {
                        progressListener.onStart(key);
                    }
                }
            }

            //utteranceId这个是内容
            @Override
            public void onDone(String utteranceId) {
                //Logcat.d(TAG, "onDone:" + utteranceId);
                ReflowBean key = keys.remove(utteranceId);
                ttsContent.remove(key);
                if (null != key) {
                    progressListener.onDone(key);
                }
                if (ttsContent.isEmpty()) {
                    progressListener.onFinish();
                }
            }

            @Override
            public void onError(String utteranceId) {
                ReflowBean key = keys.remove(utteranceId);
                Logcat.d(TAG, String.format("onError:%s, %s", utteranceId, key));
                ttsContent.remove(key);
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
        }
        keys.clear();

        textToSpeech.stop();
        for (int i = 0; i < ttsContent.size(); i++) {
            ReflowBean bean = ttsContent.get(i);
            keys.put(bean.getPage(), bean);
            resumeSpeak(bean);
        }
        return true;
    }

    /**
     * 从指定的内容开始朗读
     *
     * @param text
     * @return
     */
    public boolean resumeFromKey(ReflowBean bean) {
        if (null == textToSpeech || keys.isEmpty() || ttsContent.isEmpty()) {
            Logcat.d(TAG, "no content.");
            return false;
        }
        if (keys.size() != ttsContent.size()) {
            Logcat.d(TAG, "something wrong.");
        }
        keys.clear();

        textToSpeech.stop();
        boolean found = false;
        List<ReflowBean> list = new ArrayList<>();
        for (int i = 0; i < ttsContent.size(); i++) {
            if (TextUtils.equals(ttsContent.get(i).getPage(), bean.getPage())) {
                found = true;
            }
            if (found) {
                list.add(ttsContent.get(i));
            }
        }
        ttsContent.clear();
        ttsContent.addAll(list);
        for (ReflowBean reflowBean : list) {
            keys.put(reflowBean.getPage(), reflowBean);
            resumeSpeak(reflowBean);
        }
        return true;
    }

    public boolean resumeFromKeys(List<ReflowBean> beans, int pos) {
        if (null == textToSpeech) {
            Logcat.d(TAG, "no tts.");
            return false;
        }
        if (beans.size() < pos) {
            Logcat.d(TAG, "something wrong.");
        }
        keys.clear();
        textToSpeech.stop();
        List<ReflowBean> list = new ArrayList<>();
        for (int i = pos; i < beans.size(); i++) {
            list.add(beans.get(i));
        }
        ttsContent.clear();
        ttsContent.addAll(list);
        for (ReflowBean reflowBean : list) {
            keys.put(reflowBean.getPage(), reflowBean);
            resumeSpeak(reflowBean);
        }
        return true;
    }

    public int getFirst() {
        if (!ttsContent.isEmpty()) {
            ReflowBean bean = ttsContent.get(0);
            try {
                String[] arr = bean.getPage().split("-");
                return Utils.parseInt(arr[0]);
            } catch (Exception e) {
                Logcat.e("reflow", String.format("%s, %s", bean, e));
            }
        }
        return -1;
    }

    public int getLast() {
        if (!ttsContent.isEmpty()) {
            ReflowBean bean = ttsContent.get(ttsContent.size() - 1);
            String[] arr = bean.getPage().split("-");
            try {
                return Utils.parseInt(arr[0]);
            } catch (Exception e) {
                Logcat.e("reflow", String.format("%s, %s", bean, e));
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

    private void doSpeak(ReflowBean bean) {
        if (textToSpeech == null) {
            Logcat.d(TAG, "textToSpeech not initialized");
            return;
        }
        textToSpeech.speak(bean.getData(), TextToSpeech.QUEUE_ADD, null, bean.getPage());
    }

    public void resumeSpeak(final ReflowBean bean) {
        doSpeak(bean);
    }

    public void speak(final List<ReflowBean> beans) {
        for (ReflowBean bean : beans) {
            keys.put(bean.getPage(), bean);
            ttsContent.add(bean);
            doSpeak(bean);
        }
    }

    public void speak(final ReflowBean bean) {
        if (textToSpeech == null) {
            Logcat.d(TAG, "textToSpeech not initialized");
            return;
        }

        keys.put(bean.getPage(), bean);
        ttsContent.add(bean);
        doSpeak(bean);
    }

    public interface TtsProgressListener {
        void onStart(ReflowBean utteranceId);

        void onDone(ReflowBean utteranceId);

        void onFinish();
    }
}
