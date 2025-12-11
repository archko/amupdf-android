package cn.archko.pdf.tts;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import androidx.core.app.NotificationCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.archko.pdf.core.common.Logcat;
import cn.archko.pdf.core.entity.ReflowBean;

/**
 * TTS前台服务，用于后台持续朗读
 */
public class TtsForegroundService extends Service implements TextToSpeech.OnInitListener {

    private static final String TAG = "TtsForegroundService";
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "tts_service_channel";

    // Binder用于Activity绑定
    private final TtsServiceBinder binder = new TtsServiceBinder();

    private TextToSpeech textToSpeech;
    private boolean isInitialized = false;

    // TTS监听器
    private TtsProgressListener progressListener;

    // 数据队列
    private final List<ReflowBean> ttsQueue = new ArrayList<>();
    private final Map<String, ReflowBean> keyMap = new HashMap<>();
    private ReflowBean currentBean;
    private int currentIndex = 0;
    private boolean isSpeaking = false;

    // 前台服务相关
    private boolean isForegroundStarted = false;
    private PowerManager.WakeLock wakeLock;
    private AudioManager audioManager;

    @Override
    public void onCreate() {
        super.onCreate();
        Logcat.d(TAG, "onCreate");

        createNotificationChannel();
        initAudioManager();
        initWakeLock();
        initializeTts();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Logcat.d(TAG, "onStartCommand");
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Logcat.d(TAG, "onBind");
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Logcat.d(TAG, "onDestroy");

        shutdown();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "TTS朗读服务",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("文档朗读服务");
            channel.setSound(null, null);

            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        Intent stopIntent = new Intent(this, TtsForegroundService.class);
        stopIntent.setAction("STOP");
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this, 0, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("文档朗读")
                .setContentText(isSpeaking ? "正在朗读..." : "已暂停")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .addAction(android.R.drawable.ic_delete, "停止", stopPendingIntent)
                .setOngoing(true)
                .build();
    }

    private void startForegroundIfNeeded() {
        if (!isForegroundStarted) {
            startForeground(NOTIFICATION_ID, createNotification());
            isForegroundStarted = true;
            Logcat.d(TAG, "startForeground");
        }
    }

    private void stopForegroundIfNeeded() {
        if (isForegroundStarted) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE);
            } else {
                stopForeground(true);
            }
            isForegroundStarted = false;
            Logcat.d(TAG, "stopForeground");
        }
    }

    private void initAudioManager() {
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
    }

    private void initWakeLock() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TtsForegroundService:TTS");
    }

    private void initializeTts() {
        textToSpeech = new TextToSpeech(this, this);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true;
            textToSpeech.setLanguage(java.util.Locale.CHINA);
            setupTtsListener();
            Logcat.d(TAG, "TTS initialized successfully");
        } else {
            Logcat.e(TAG, "TTS initialization failed");
        }
    }

    private void setupTtsListener() {
        textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                isSpeaking = true;
                startForegroundIfNeeded();
                if (progressListener != null && utteranceId != null) {
                    ReflowBean bean = keyMap.get(utteranceId);
                    if (bean != null) {
                        progressListener.onStart(bean);
                    }
                }
            }

            @Override
            public void onDone(String utteranceId) {
                Logcat.d(TAG, "onDone: " + utteranceId);
                if (utteranceId != null) {
                    ReflowBean finishedBean = keyMap.remove(utteranceId);
                    if (finishedBean != null) {
                        if (progressListener != null) {
                            progressListener.onDone(finishedBean);
                        }
                        ttsQueue.remove(finishedBean);
                    }
                }

                // 检查是否还有内容需要朗读
                if (currentIndex < ttsQueue.size()) {
                    speakNext();
                } else {
                    isSpeaking = false;
                    stopForegroundIfNeeded();
                    if (wakeLock.isHeld()) {
                        wakeLock.release();
                    }
                    if (progressListener != null) {
                        progressListener.onFinish();
                    }
                }
            }

            @Override
            public void onError(String utteranceId) {
                Logcat.d(TAG, "onError: " + utteranceId);
                if (utteranceId != null) {
                    ReflowBean errorBean = keyMap.remove(utteranceId);
                    if (errorBean != null) {
                        ttsQueue.remove(errorBean);
                    }
                }
                // 继续下一个
                if (currentIndex < ttsQueue.size()) {
                    speakNext();
                } else {
                    isSpeaking = false;
                    stopForegroundIfNeeded();
                    if (wakeLock.isHeld()) {
                        wakeLock.release();
                    }
                }
            }
        });
    }

    private void speakNext() {
        if (currentIndex < ttsQueue.size()) {
            currentBean = ttsQueue.get(currentIndex);
            String text = currentBean.getData();
            if (text != null && !text.trim().isEmpty()) {
                HashMap<String, String> params = new HashMap<>();
                params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, currentBean.getPage());
                keyMap.put(currentBean.getPage(), currentBean);
                textToSpeech.speak(text, TextToSpeech.QUEUE_ADD, params);
                currentIndex++;
            } else {
                currentIndex++;
                speakNext(); // 跳过空内容
            }
        }
    }

    public void setProgressListener(TtsProgressListener listener) {
        this.progressListener = listener;
    }

    public void speak(ReflowBean bean) {
        if (isInitialized) {
            reset();
            addToQueue(bean);
            speakNext();
        }
    }

    public void addToQueue(ReflowBean bean) {
        ttsQueue.add(bean);
        if (!isSpeaking && isInitialized) {
            speakNext();
        }
    }

    public void stop() {
        if (textToSpeech != null) {
            textToSpeech.stop();
        }
        isSpeaking = false;
        if (progressListener != null) {
            progressListener.onFinish();
        }
        stopForegroundIfNeeded();
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    public void pause() {
        if (textToSpeech != null) {
            textToSpeech.stop();
        }
        isSpeaking = false;
        stopForegroundIfNeeded();
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    public boolean isSpeaking() {
        return isSpeaking;
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public void reset() {
        keyMap.clear();
        ttsQueue.clear();
        currentIndex = 0;
        currentBean = null;
        isSpeaking = false;
    }

    public int getQueueSize() {
        return ttsQueue.size();
    }

    public ReflowBean getCurrentBean() {
        return currentBean;
    }

    public List<ReflowBean> getQueue() {
        return new ArrayList<>(ttsQueue);
    }

    private void shutdown() {
        if (textToSpeech != null) {
            textToSpeech.shutdown();
            textToSpeech = null;
        }
        isInitialized = false;
        reset();
        stopForegroundIfNeeded();
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    // Binder类
    public class TtsServiceBinder extends Binder {
        public TtsForegroundService getService() {
            return TtsForegroundService.this;
        }
    }

    // TTS监听器接口
    public interface TtsProgressListener {
        void onStart(ReflowBean bean);

        void onDone(ReflowBean bean);

        void onFinish();
    }
}
