package cn.archko.pdf.tts;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.speech.tts.UtteranceProgressListener;
import android.view.KeyEvent;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import org.vudroid.core.codec.CodecDocument;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import cn.archko.pdf.R;
import cn.archko.pdf.core.common.Logcat;

@TargetApi(Build.VERSION_CODES.O)
public class TTSService extends Service {

    public static final String EXTRA_PATH = "EXTRA_PATH";
    public static final String EXTRA_ANCHOR = "EXTRA_ANCHOR";
    public static final String EXTRA_INT = "INT";

    private static final String TAG = "TTSService";

    public static String ACTION_PLAY_CURRENT_PAGE = "ACTION_PLAY_CURRENT_PAGE";
    private final BroadcastReceiver blueToothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Logcat.d("blueToothReceiver", String.valueOf(intent));
            ///TTSEngine.get().stop(mMediaSessionCompat);
            TTSNotification.showLast();
        }
    };
    int width;
    int height;
    AudioManager mAudioManager;
    //MediaSessionCompat mMediaSessionCompat;
    boolean isActivated;
    boolean isPlaying;
    Object audioFocusRequest;
    volatile boolean isStartForeground = false;
    CodecDocument cache;
    String path;
    int wh;
    int emptyPageCount = 0;
    final OnAudioFocusChangeListener listener = new OnAudioFocusChangeListener() {

        @Override
        public void onAudioFocusChange(int focusChange) {
            Logcat.d("onAudioFocusChange", String.valueOf(focusChange));
            /*if (AppState.get().isEnableAccessibility) {
                return;
            }

            if (!AppState.get().stopReadingOnCall) {
                return;
            }
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                Logcat.d("Ingore Duck");
                return;
            }

            if (focusChange < 0) {
                isPlaying = TTSEngine.get().isPlaying();
                Logcat.d("onAudioFocusChange", "Is playing:"+ isPlaying);
                TTSEngine.get().stop(mMediaSessionCompat);
                TTSNotification.showLast();
            } else {
                if (isPlaying) {
                    playPage("", AppSP.get().lastBookPage, null);
                }
            }
            EventBus.getDefault().post(new TtsStatus());*/
        }
    };
    private WakeLock wakeLock;

    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(
                            new AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_MEDIA)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                    .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                                    .build())
                    .setAcceptsDelayedFocusGain(true)
                    .setWillPauseWhenDucked(false)
                    .setOnAudioFocusChangeListener(listener)
                    .build();
        }
    }

    public TTSService() {
        Logcat.d(TAG, "Create constructor");
    }

    public static void playLastBook() {
        //playBookPage(AppSP.get().lastBookPage, AppSP.get().lastBookPath, "", AppSP.get().lastBookWidth, AppSP.get().lastBookHeight, AppSP.get().lastFontSize, AppSP.get().lastBookTitle);
    }

    public static void updateTimer() {
        //TempHolder.get().timerFinishTime = System.currentTimeMillis() + AppState.get().ttsTimer * 60 * 1000;
        //Logcat.d("Update-timer", TempHolder.get().timerFinishTime, AppState.get().ttsTimer);
    }

    /*public static void playPause(Context context, DocumentController controller) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, Manifest.permission.POST_NOTIFICATIONS)) {
//                final Intent i = new Intent();
//                i.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
//                i.setData( Uri.fromParts("package", context.getPackageName(), null));
//                context.startActivity(i);
            } else {
                ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 11);
                return;
            }

        }


        if (TTSEngine.get().isPlaying()) {
            PendingIntent next = PendingIntent.getService(context, 0, new Intent(TTSNotification.TTS_PAUSE, null, context, TTSService.class), PendingIntent.FLAG_IMMUTABLE);
            try {
                next.send();
            } catch (CanceledException e) {
                Logcat.e(e);
            }
        } else {
            if (controller != null) {
                TTSService.playBookPage(controller.getCurentPageFirst1() - 1, controller.getCurrentBook().getPath(), "", controller.getBookWidth(), controller.getBookHeight(), BookCSS.get().fontSizeSp, controller.getTitle());
            }
        }
    }*/

    public static void playBookPage(int page, String path, String anchor, int width, int height, int fontSize, String title) {
        Logcat.d(TAG, String .format("playBookPage,%s-%s-%s-%s", page, path, width, height));

        /*TTSEngine.get().stop(null);

        AppSP.get().lastBookWidth = width;
        AppSP.get().lastBookHeight = height;
        AppSP.get().lastFontSize = fontSize;
        AppSP.get().lastBookTitle = title;
        AppSP.get().lastBookPage = page;


        Intent intent = playBookIntent(page, path, anchor);

        if (Build.VERSION.SDK_INT >= 26) {
            LibreraApp.context.startForegroundService(intent);
        } else {
            LibreraApp.context.startService(intent);
        }*/
    }

    /*private static Intent playBookIntent(int page, String path, String anchor) {
        Intent intent = new Intent(LibreraApp.context, TTSService.class);
        intent.setAction(TTSService.ACTION_PLAY_CURRENT_PAGE);
        intent.putExtra(EXTRA_INT, page);
        intent.putExtra(EXTRA_PATH, path);
        intent.putExtra(EXTRA_ANCHOR, anchor);
        return intent;
    }*/

    @Override
    public void onCreate() {
        Logcat.d(TAG, "Create");
        startMyForeground();
        //

        PowerManager myPowerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = myPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Librera:TTSServiceLock");

        //AppProfile.init(getApplicationContext());

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);





        //mAudioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);

        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        PendingIntent penginIntent = PendingIntent.getActivity(getApplicationContext(), 0, mediaButtonIntent, PendingIntent.FLAG_IMMUTABLE);

        /*mMediaSessionCompat = new MediaSessionCompat(getApplicationContext(), "Tag", null, penginIntent);
        mMediaSessionCompat.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mMediaSessionCompat.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public boolean onMediaButtonEvent(Intent intent) {
                KeyEvent event = (KeyEvent) intent.getExtras().get(Intent.EXTRA_KEY_EVENT);

                boolean isPlaying = TTSEngine.get().isPlaying();

                Logcat.d(TAG, "onMediaButtonEvent", "isActivated", isActivated, "isPlaying", isPlaying, "event", event);


                final List<Integer> list = Arrays.asList(KeyEvent.KEYCODE_HEADSETHOOK, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_MEDIA_STOP, KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PAUSE);

                if (KeyEvent.ACTION_DOWN == event.getAction()) {
                    if (list.contains(event.getKeyCode())) {
                        Logcat.d(TAG, "onMediaButtonEvent", "isPlaying", isPlaying, "isFastBookmarkByTTS", AppState.get().isFastBookmarkByTTS);

                        if (AppState.get().isFastBookmarkByTTS) {
                            if (isPlaying) {
                                TTSEngine.get().fastTTSBookmakr(getBaseContext(), AppSP.get().lastBookPath, AppSP.get().lastBookPage + 1, AppSP.get().lastBookPageCount);
                            } else {
                                playPage("", AppSP.get().lastBookPage, null);
                            }
                        } else {
                            if (isPlaying) {
                                TTSEngine.get().stop(mMediaSessionCompat);
                            } else {
                                playPage("", AppSP.get().lastBookPage, null);
                            }
                        }
                    } else if (KeyEvent.KEYCODE_MEDIA_NEXT == event.getKeyCode()) {
                        TTSEngine.get().stop(mMediaSessionCompat);
                        playPage("", AppSP.get().lastBookPage + 1, null);

                    } else if (KeyEvent.KEYCODE_MEDIA_PREVIOUS == event.getKeyCode()) {
                        TTSEngine.get().stop(mMediaSessionCompat);
                        playPage("", AppSP.get().lastBookPage - 1, null);


                    }
                }


                EventBus.getDefault().post(new TtsStatus());
                TTSNotification.showLast();
                //  }
                return true;
            }
        });

        Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setClass(this, MediaButtonReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        try {
            mMediaSessionCompat.setMediaButtonReceiver(pendingIntent);
        } catch (Exception e) {
            Logcat.e(e);
        }*/

        //setSessionToken(mMediaSessionCompat.getSessionToken());


        // mMediaSessionCompat.setPlaybackState(new
        // PlaybackStateCompat.Builder().setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE).setState(PlaybackStateCompat.STATE_CONNECTING,
        // 0, 0f).build());

        TTSEngine.get().getTTS();

        if (Build.VERSION.SDK_INT >= 24) {
            MediaPlayer mp = new MediaPlayer();
            try {
                final AssetFileDescriptor afd = getAssets().openFd("silence.mp3");
                mp.setDataSource(afd);
                mp.prepareAsync();
                mp.start();
                mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        try {
                            afd.close();
                        } catch (IOException e) {
                            //Logcat.e(e);
                        }
                    }
                });

                Logcat.d("silence");
            } catch (IOException e) {
                Logcat.d("silence error");
                //Logcat.e(e);
            }
        }


        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(blueToothReceiver, filter);

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void startMyForeground() {
        if (true) {
            if (!isStartForeground) {
                startServiceWithNotification();
                isStartForeground = true;
            }
        }
    }

    private void startServiceWithNotification() {
        PendingIntent stopDestroy = PendingIntent.getService(this, 0, new Intent(TTSNotification.TTS_STOP_DESTROY, null, this, TTSService.class), PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new NotificationCompat.Builder(this, TTSNotification.DEFAULT) //
                .setSmallIcon(R.drawable.glyphicons_smileys_100_headphones) //
                //.setContentTitle(App.getApplicationName(this)) //
                //.setContentText(getString(R.string.please_wait))
                //.addAction(R.drawable.glyphicons_599_menu_close, getString(R.string.stop), stopDestroy)//
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)//
                .build();


        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(TTSNotification.NOT_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(TTSNotification.NOT_ID, notification);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        updateTimer();
        startMyForeground();

        /*MediaButtonReceiver.handleIntent(mMediaSessionCompat, intent);
        Logcat.d(TAG, "onStartCommand", intent);
        if (intent == null) {
            return START_STICKY;
        }
        Logcat.d(TAG, "onStartCommand", intent.getAction());
        if (intent.getExtras() != null) {
            Logcat.d(TAG, "onStartCommand", intent.getAction(), intent.getExtras());
            for (String key : intent.getExtras().keySet())
                Logcat.d(TAG, key, "=>", intent.getExtras().get(key));
        }

        if (TTSNotification.TTS_STOP_DESTROY.equals(intent.getAction())) {
            TTSEngine.get().mp3Destroy();
            BookCSS.get().mp3BookPath(null);
            AppState.get().mp3seek = 0;
            TTSEngine.get().stop(mMediaSessionCompat);

            TTSEngine.get().stopDestroy();

            if (wakeLock.isHeld()) {
                wakeLock.release();
            }
            EventBus.getDefault().post(new TtsStatus());

            TTSNotification.hideNotification();
            stopForeground(true);
            stopSelf();

            return START_STICKY;
        }

        if (TTSNotification.TTS_PLAY_PAUSE.equals(intent.getAction())) {
            if (TTSEngine.get().isMp3PlayPause()) {
                return START_STICKY;
            }

            if (TTSEngine.get().isPlaying()) {
                TTSEngine.get().stop(mMediaSessionCompat);
            } else {
                playPage("", AppSP.get().lastBookPage, null);
            }
            if (wakeLock.isHeld()) {
                wakeLock.release();
            }
            TTSNotification.showLast();

        }
        if (TTSNotification.TTS_PAUSE.equals(intent.getAction())) {
            if (TTSEngine.get().isMp3PlayPause()) {
                return START_STICKY;
            }

            TTSEngine.get().stop(mMediaSessionCompat);
            if (wakeLock.isHeld()) {
                wakeLock.release();
            }
            TTSNotification.showLast();
        }

        if (TTSNotification.TTS_PLAY.equals(intent.getAction())) {
            if (TTSEngine.get().isMp3PlayPause()) {
                return START_STICKY;
            }

            TTSEngine.get().stop(mMediaSessionCompat);

            playPage("", AppSP.get().lastBookPage, null);
            if (!wakeLock.isHeld()) {
                wakeLock.acquire();
            }
            TTSNotification.showLast();
        }
        if (TTSNotification.TTS_NEXT.equals(intent.getAction())) {
            if (TTSEngine.get().isMp3()) {
                TTSEngine.get().mp3Next();
                return START_STICKY;
            }

            AppSP.get().lastBookParagraph = 0;
            TTSEngine.get().stop(mMediaSessionCompat);
            playPage("", AppSP.get().lastBookPage + 1, null);
            if (!wakeLock.isHeld()) {
                wakeLock.acquire();
            }
        }
        if (TTSNotification.TTS_PREV.equals(intent.getAction())) {
            if (TTSEngine.get().isMp3()) {
                TTSEngine.get().mp3Prev();
                return START_STICKY;
            }

            AppSP.get().lastBookParagraph = 0;
            TTSEngine.get().stop(mMediaSessionCompat);
            playPage("", AppSP.get().lastBookPage - 1, null);
            if (!wakeLock.isHeld()) {
                wakeLock.acquire();
            }
        }

        if (ACTION_PLAY_CURRENT_PAGE.equals(intent.getAction())) {
            if (TTSEngine.get().isMp3PlayPause()) {
                TTSNotification.show(AppSP.get().lastBookPath, -1, -1);
                return START_STICKY;
            }

            int pageNumber = intent.getIntExtra(EXTRA_INT, -1);
            AppSP.get().lastBookPath = intent.getStringExtra(EXTRA_PATH);
            String anchor = intent.getStringExtra(EXTRA_ANCHOR);

            if (pageNumber != -1) {
                playPage("", pageNumber, anchor);
            }
            if (!wakeLock.isHeld()) {
                wakeLock.acquire();
            }

        }

        EventBus.getDefault().post(new TtsStatus());*/

        return START_STICKY;
    }

    /*public CodecDocument getDC() {
        try {
            if (AppSP.get().lastBookPath != null && AppSP.get().lastBookPath.equals(path) && cache != null && wh == AppSP.get().lastBookWidth + AppSP.get().lastBookHeight) {
                Logcat.d(TAG, "CodecDocument from cache", AppSP.get().lastBookPath);
                return cache;
            }
            if (cache != null) {
                cache.recycle();
                cache = null;
            }
            path = AppSP.get().lastBookPath;
            cache = ImageExtractor.singleCodecContext(AppSP.get().lastBookPath, "");
            if (cache == null) {
                TTSNotification.hideNotification();
                return null;
            }
            cache.getPageCount(AppSP.get().lastBookWidth, AppSP.get().lastBookHeight, BookCSS.get().fontSizeSp);
            wh = AppSP.get().lastBookWidth + AppSP.get().lastBookHeight;
            Logcat.d(TAG, "CodecDocument new", AppSP.get().lastBookPath, AppSP.get().lastBookWidth, AppSP.get().lastBookHeight);
            return cache;
        } catch (Exception e) {
            Logcat.e(e);
            return null;
        }
    }*/

    private void playPage(String preText, int pageNumber, String anchor) {
        /*mMediaSessionCompat.setActive(true);

        if(!AppState.get().allowOtherMusic) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mAudioManager.requestAudioFocus((AudioFocusRequest) audioFocusRequest);
            } else {
                mAudioManager.requestAudioFocus(listener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            }
        }

        Logcat.d("playPage", preText, pageNumber, anchor);
        if (pageNumber != -1) {
            isActivated = true;
            EventBus.getDefault().post(new MessagePageNumber(pageNumber));
            AppSP.get().lastBookPage = pageNumber;
            CodecDocument dc = getDC();
            if (dc == null) {
                Logcat.d(TAG, "CodecDocument", "is NULL");
                TTSNotification.hideNotification();
                return;
            }

            AppSP.get().lastBookPageCount = dc.getPageCount();
            Logcat.d(TAG, "CodecDocument PageCount", pageNumber, AppSP.get().lastBookPageCount);
            if (pageNumber >= AppSP.get().lastBookPageCount) {


                Vibro.vibrate(1000);

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                }
                TTSEngine.get().getTTS().setOnUtteranceCompletedListener(null);
                TTSEngine.get().speek(LibreraApp.context.getString(R.string.the_book_is_over));

                EventBus.getDefault().post(new TtsStatus());

                stopSelf();
                return;
            }


            CodecPage page = dc.getPage(pageNumber);
            String pageHTML = page.getPageHTML();
            page.recycle();
            pageHTML = TxtUtils.replaceHTMLforTTS(pageHTML);


            if (TxtUtils.isNotEmpty(anchor)) {
                int indexOf = pageHTML.indexOf(anchor);
                if (indexOf > 0) {
                    pageHTML = pageHTML.substring(indexOf);
                    Logcat.d("find anchor new text", pageHTML);
                }
            }

            Logcat.d(TAG, pageHTML);

            if (TxtUtils.isEmpty(pageHTML)) {
                Logcat.d("empty page play next one", emptyPageCount);
                emptyPageCount++;
                if (emptyPageCount < 3) {
                    playPage("", AppSP.get().lastBookPage + 1, null);
                }
                return;
            }
            emptyPageCount = 0;

            String[] parts = TxtUtils.getParts(pageHTML);
            String firstPart = pageNumber + 1 >= AppSP.get().lastBookPageCount || AppState.get().ttsTunnOnLastWord ? pageHTML : parts[0];
            final String secondPart = pageNumber + 1 >= AppSP.get().lastBookPageCount || AppState.get().ttsTunnOnLastWord ? "" : parts[1];

            if (TxtUtils.isNotEmpty(preText)) {
                char last = preText.charAt(preText.length() - 1);
                if (last == '-') {
                    preText = TxtUtils.replaceLast(preText, "-", "");
                    firstPart = preText + firstPart;
                } else {
                    firstPart = preText + " " + firstPart;
                }
            }
            final String preText1 = preText;

            if (Build.VERSION.SDK_INT >= 15) {
                TTSEngine.get().getTTS().setOnUtteranceProgressListener(new UtteranceProgressListener() {

                    @Override
                    public void onStart(String utteranceId) {
                        Logcat.d(TAG, "onUtteranceCompleted onStart", utteranceId);
                    }

                    @Override
                    public void onError(String utteranceId) {
                        Logcat.d(TAG, "onUtteranceCompleted onError", utteranceId);
                        if (!utteranceId.equals(TTSEngine.UTTERANCE_ID_DONE)) {
                            return;
                        }
                        TTSEngine.get().stop(mMediaSessionCompat);
                        EventBus.getDefault().post(new TtsStatus());

                    }

                    @Override
                    public void onDone(String utteranceId) {
                        Logcat.d(TAG, "onUtteranceCompleted", utteranceId);
                        if (utteranceId.startsWith(TTSEngine.STOP_SIGNAL)) {
                            TTSEngine.get().stop(mMediaSessionCompat);
                            return;
                        }
                        if (utteranceId.startsWith(TTSEngine.FINISHED_SIGNAL)) {
                            if (TxtUtils.isNotEmpty(preText1)) {
                                AppSP.get().lastBookParagraph = Integer.parseInt(utteranceId.replace(TTSEngine.FINISHED_SIGNAL, ""));
                            } else {
                                AppSP.get().lastBookParagraph = Integer.parseInt(utteranceId.replace(TTSEngine.FINISHED_SIGNAL, "")) + 1;
                            }
                            return;
                        }

                        if (!utteranceId.equals(TTSEngine.UTTERANCE_ID_DONE)) {
                            Logcat.d(TAG, "onUtteranceCompleted skip", "");
                            return;
                        }

                        if (System.currentTimeMillis() > TempHolder.get().timerFinishTime) {
                            Logcat.d(TAG, "Update-timer-Stop1");
                            stopSelf();
                            return;
                        }

                        AppSP.get().lastBookParagraph = 0;
                        playPage(secondPart, AppSP.get().lastBookPage + 1, null);


                    }
                });
            } else {
                TTSEngine.get().getTTS().setOnUtteranceCompletedListener(new OnUtteranceCompletedListener() {

                    @Override
                    public void onUtteranceCompleted(String utteranceId) {
                        if (utteranceId.startsWith(TTSEngine.STOP_SIGNAL)) {
                            TTSEngine.get().stop(mMediaSessionCompat);
                            return;
                        }
                        if (utteranceId.startsWith(TTSEngine.FINISHED_SIGNAL)) {
                            if (TxtUtils.isNotEmpty(preText1)) {
                                AppSP.get().lastBookParagraph = Integer.parseInt(utteranceId.replace(TTSEngine.FINISHED_SIGNAL, ""));
                            } else {
                                AppSP.get().lastBookParagraph = Integer.parseInt(utteranceId.replace(TTSEngine.FINISHED_SIGNAL, "")) + 1;
                            }
                            return;
                        }

                        if (!utteranceId.equals(TTSEngine.UTTERANCE_ID_DONE)) {
                            Logcat.d(TAG, "onUtteranceCompleted skip", "");
                            return;
                        }

                        Logcat.d(TAG, "onUtteranceCompleted", utteranceId);
                        if (System.currentTimeMillis() > TempHolder.get().timerFinishTime) {
                            Logcat.d(TAG, "Update-timer-Stop2");
                            stopSelf();
                            return;
                        }

                        AppSP.get().lastBookParagraph = 0;
                        playPage(secondPart, AppSP.get().lastBookPage + 1, null);


                    }

                });
            }

            TTSEngine.get().speek(firstPart);

            TTSNotification.show(AppSP.get().lastBookPath, pageNumber + 1, dc.getPageCount());
            Logcat.d("TtsStatus send");
            EventBus.getDefault().post(new TtsStatus());

            TTSNotification.showLast();

            new Thread(() -> {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                }
                AppBook load = SharedBooks.load(AppSP.get().lastBookPath);
                load.currentPageChanged(pageNumber + 1, AppSP.get().lastBookPageCount);

                SharedBooks.save(load, false);
                AppProfile.save(this);
            }, "@T TTS Save").start();
        }*/
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        isStartForeground = false;
        unregisterReceiver(blueToothReceiver);
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }

        //TTSEngine.get().stop(mMediaSessionCompat);
        TTSEngine.get().shutdown();

        TTSNotification.hideNotification();

        isActivated = false;

        //mAudioManager.abandonAudioFocus(listener);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mAudioManager.abandonAudioFocusRequest((AudioFocusRequest) audioFocusRequest);
        } else {
            mAudioManager.abandonAudioFocus(listener);
        }

        //mMediaSessionCompat.setCallback(null);
        //mMediaSessionCompat.release();

        if (cache != null) {
            cache.recycle();
        }
        path = null;
        Logcat.d(TAG, "onDestroy");
    }

}
