package cn.archko.pdf.tts;

import android.annotation.TargetApi;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.EngineInfo;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.text.TextUtils;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import cn.archko.pdf.R;
import cn.archko.pdf.core.App;
import cn.archko.pdf.core.common.Logcat;

public class TTSEngine {

    public static final String FINISHED_SIGNAL = "Finished";
    public static final String STOP_SIGNAL = "Stoped";
    public static final String UTTERANCE_ID_DONE = "LirbiReader";
    public static final String WAV = ".wav";
    public static final String MP3 = ".mp3";
    private static final String TAG = "TTSEngine";
    private static TTSEngine INSTANCE = new TTSEngine();
    volatile TextToSpeech textToSpeech;
    volatile MediaPlayer mp;
    Timer mTimer;
    Object helpObject = new Object();
    HashMap<String, String> map = new HashMap<>();
    HashMap<String, String> mapTemp = new HashMap<>();

    OnInitListener listener = new OnInitListener() {

        @Override
        public void onInit(int status) {
            Logcat.d(TAG, "onInit, SUCCESS:"+( status == TextToSpeech.SUCCESS));
            if (status == TextToSpeech.ERROR) {
                //Toast.makeText(App.Companion.getInstance(), R.string.msg_unexpected_error, Toast.LENGTH_LONG).show();
            }
        }
    };
    private String text = "";

    {
        map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, UTTERANCE_ID_DONE);
    }

    {
        mapTemp.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "Temp");
    }

    public static TTSEngine get() {
        return INSTANCE;
    }

    //public static AppBookmark fastTTSBookmakr(DocumentController dc) {
    //    return fastTTSBookmakr(dc.getActivity(), dc.getCurrentBook().getPath(), dc.getCurentPageFirst1(), dc.getPageCount());
    //}

    /*public static AppBookmark fastTTSBookmakr(Context c, String bookPath, int page, int pages) {
        Logcat.d("fastTTSBookmakr", page, pages);

        if (pages == 0) {
            Logcat.d("fastTTSBookmakr skip");
            return null;
        }
        boolean hasBookmark = BookmarksData.get().hasBookmark(bookPath, page, pages);

        if (!hasBookmark) {
            final AppBookmark bookmark = new AppBookmark(bookPath, c.getString(R.string.fast_bookmark), MyMath.percent(page, pages));
            BookmarksData.get().add(bookmark);

            String TEXT = c.getString(R.string.fast_bookmark) + " " + TxtUtils.LONG_DASH1 + " " + c.getString(R.string.page) + " " + page + "";
            Toast.makeText(c, TEXT, Toast.LENGTH_SHORT).show();
            return bookmark;
        }
        Vibro.vibrate();
        return null;
    }*/

    public static String engineToString(EngineInfo info) {
        return info.label;
    }

    public void shutdown() {
        Logcat.d(TAG, "shutdown");

        synchronized (helpObject) {
            if (textToSpeech != null) {
                textToSpeech.shutdown();
            }
            textToSpeech = null;
        }

    }

    public TextToSpeech getTTS() {
        return getTTS(null);
    }

    public TextToSpeech getTTS(OnInitListener onLisnter) {
        synchronized (helpObject) {
            if (TTSEngine.get().isMp3() && mp == null) {
                //TTSEngine.get().loadMP3(BookCSS.get().mp3BookPathGet());
            }

            if (textToSpeech != null) {
                return textToSpeech;
            }
            if (onLisnter == null) {
                onLisnter = listener;
            }
            textToSpeech = new TextToSpeech(App.Companion.getInstance(), onLisnter);
        }

        return textToSpeech;

    }

    public synchronized boolean isShutdown() {
        return textToSpeech == null;
    }

    public void stop() {
        //stop(null);
    }

    /*public void stop(MediaSessionCompat mediaSessionCompat) {
        if (mediaSessionCompat != null) {
            mediaSessionCompat.setActive(false);
        }
        if (!AppState.get().allowOtherMusic) {
            try {

                AudioManager mAudioManager = (AudioManager) LibreraApp.context.getSystemService(Context.AUDIO_SERVICE);
                mAudioManager.abandonAudioFocus(null);
            } catch (Exception e) {
                Logcat.e(e);
            }
        }

        Logcat.d(TAG, "stop");
        synchronized (helpObject) {


            if (ttsEngine != null) {
                if (Build.VERSION.SDK_INT >= 15) {
                    ttsEngine.setOnUtteranceProgressListener(null);
                } else {
                    ttsEngine.setOnUtteranceCompletedListener(null);
                }
                ttsEngine.stop();
                EventBus.getDefault().post(new TtsStatus());
            }
        }
    }*/

    public void stopDestroy() {
        Logcat.d(TAG, "stop");
        //TxtUtils.dictHash = "";
        synchronized (helpObject) {
            if (textToSpeech != null) {
                textToSpeech.shutdown();
            }
            textToSpeech = null;
        }
        //AppSP.get().lastBookParagraph = 0;
    }

    public TextToSpeech setTTSWithEngine(String engine) {
        shutdown();
        synchronized (helpObject) {
            textToSpeech = new TextToSpeech(App.Companion.getInstance(), listener, engine);
        }
        return textToSpeech;
    }

    public synchronized void speek(final String text) {
        this.text = text;

        /*if (AppSP.get().tempBookPage != AppSP.get().lastBookPage) {
            AppSP.get().tempBookPage = AppSP.get().lastBookPage;
            AppSP.get().lastBookParagraph = 0;
        }

        Logcat.d(TAG, "speek", AppSP.get().lastBookPage, "par", AppSP.get().lastBookParagraph);

        if (TxtUtils.isEmpty(text)) {
            return;
        }*/
        if (textToSpeech == null) {
            Logcat.d("getTTS-status was null");
        } else {
            Logcat.d("getTTS-status not null");
        }

        textToSpeech = getTTS(new OnInitListener() {

            @Override
            public void onInit(int status) {
                Logcat.d("getTTS-status:"+ status);
                if (status == TextToSpeech.SUCCESS) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                    speek(text);
                }
            }
        });

        /*textToSpeech.setPitch(AppState.get().ttsPitch);
        if (AppState.get().ttsSpeed == 0.0f) {
            AppState.get().ttsSpeed = 0.01f;
        }
        textToSpeech.setSpeechRate(AppState.get().ttsSpeed);
        Logcat.d(TAG, "Speek s", AppState.get().ttsSpeed);
        Logcat.d(TAG, "Speek AppSP.get().lastBookParagraph", AppSP.get().lastBookParagraph);

        if (AppState.get().ttsPauseDuration > 0 && text.contains(TxtUtils.TTS_PAUSE)) {
            String[] parts = text.split(TxtUtils.TTS_PAUSE);
            textToSpeech.playSilence(0l, TextToSpeech.QUEUE_FLUSH, mapTemp);
            for (int i = AppSP.get().lastBookParagraph; i < parts.length; i++) {
                String big = parts[i];
                big = big.trim();

                if (TxtUtils.isNotEmpty(big)) {
                    if (big.length() == 1 && !Character.isLetterOrDigit(big.charAt(0))) {
                        Logcat.d("Skip: " + big);
                        continue;

                    }
                    if (big.contains(TxtUtils.TTS_SKIP)) {
                        continue;
                    }

                    if (big.contains(TxtUtils.TTS_STOP)) {
                        HashMap<String, String> mapStop = new HashMap<String, String>();
                        mapStop.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, STOP_SIGNAL);
                        textToSpeech.playSilence(AppState.get().ttsPauseDuration, TextToSpeech.QUEUE_ADD, mapStop);
                        Logcat.d("Add stop signal");
                    }
                    if (big.contains(TxtUtils.TTS_NEXT)) {
                        textToSpeech.playSilence(0L, TextToSpeech.QUEUE_ADD, map);
                        Logcat.d("next-page signal");
                        break;
                    }

                    HashMap<String, String> mapTemp1 = new HashMap<String, String>();
                    mapTemp1.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, FINISHED_SIGNAL + i);

                    textToSpeech.speak(big, TextToSpeech.QUEUE_ADD, mapTemp1);
                    textToSpeech.playSilence(AppState.get().ttsPauseDuration, TextToSpeech.QUEUE_ADD, mapTemp);
                    Logcat.d("pageHTML-parts", i, big);
                }
            }
            textToSpeech.playSilence(0L, TextToSpeech.QUEUE_ADD, map);
        } else {
            String textToPlay = text.replace(TxtUtils.TTS_PAUSE, "");
            Logcat.d("pageHTML-parts-single", text);
            textToSpeech.speak(textToPlay, TextToSpeech.QUEUE_FLUSH, map);
        }*/
    }

    /*public void speakToFile(final DocumentController controller, final ResultResponse<String> info, int from, int to) {
        File dirFolder = new File(BookCSS.get().ttsSpeakPath, "TTS_" + controller.getCurrentBook().getName());
        if (!dirFolder.exists()) {
            dirFolder.mkdirs();
        }
        if (!dirFolder.exists()) {
            info.onResultRecive(controller.getActivity().getString(R.string.file_not_found) + " " + dirFolder.getPath());
            return;
        }

        String path = dirFolder.getPath();
        speakToFile(controller, from - 1, path, info, from - 1, to);
    }*/

    /*public void speakToFile(final DocumentController controller, final int page, final String folder, final ResultResponse<String> info, int from, int to) {
        Logcat.d("speakToFile", page, controller.getPageCount());
        if (textToSpeech == null) {
            Logcat.d("TTS is null");
            if (controller != null && controller.getActivity() != null) {
                Toast.makeText(controller.getActivity(), R.string.msg_unexpected_error, Toast.LENGTH_SHORT).show();
            }
            return;
        }

        textToSpeech.setPitch(AppState.get().ttsPitch);
        textToSpeech.setSpeechRate(AppState.get().ttsSpeed);

        if (page >= to || !TempHolder.isRecordTTS) {
            Logcat.d("speakToFile finish", page, controller.getPageCount());
            info.onResultRecive((controller.getActivity().getString(R.string.success)));
            TempHolder.isRecordTTS = false;
            return;
        }

        info.onResultRecive((page + 1) + " / " + to);

        DecimalFormat df = new DecimalFormat("0000");
        String pageName = "page-" + df.format(page + 1);
        final String wav = new File(folder, pageName + WAV).getPath();
        String fileText = controller.getTextForPage(page);
        controller.recyclePage(page);


        Logcat.d("synthesizeToFile", fileText);
        if (TxtUtils.isEmpty(fileText)) {
            speakToFile(controller, page + 1, folder, info, from, to);
        } else {
            if (fileText.length() > 3950) {
                fileText = TxtUtils.substringSmart(fileText, 3950) + " " + controller.getString(R.string.text_is_too_long);
                Logcat.d("Text-too-long", page);
            }

            textToSpeech.synthesizeToFile(fileText, map, wav);

            TTSEngine.get().getTTS().setOnUtteranceCompletedListener(new OnUtteranceCompletedListener() {

                @Override
                public void onUtteranceCompleted(String utteranceId) {
                    Logcat.d("speakToFile onUtteranceCompleted", page, controller.getPageCount());

                    if (AppState.get().isConvertToMp3) {
                        try {
                            File file = new File(wav);
                            Lame lame = new Lame();

                            InputStream input = new BufferedInputStream(new FileInputStream(file));
                            input.mark(44);
                            int bitrate = MobiParserIS.asInt_LITTLE_ENDIAN(input, 24, 4);
                            Logcat.d("bitrate", bitrate);
                            input.close();
                            input = new FileInputStream(file);

                            byte[] bytes = IOUtils.toByteArray(input);

                            short[] shorts = new short[bytes.length / 2];
                            ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);

                            lame.open(1, bitrate, 128, 4);
                            byte[] res = lame.encode(shorts, 44, shorts.length);
                            lame.close();
                            File toFile = new File(wav.replace(".wav", ".mp3"));
                            toFile.delete();
                            IO.copyFile(new ByteArrayInputStream(res), toFile);
                            input.close();
                            file.delete();
                        } catch (Exception e) {
                            Logcat.e(e);
                        }
                    }
                    //lame.encode();

                    speakToFile(controller, page + 1, folder, info, from, to);
                }
            });
        }
    }*/

    public boolean isTempPausing() {
        /*if (AppState.get().isEnableAccessibility) {
            return true;
        }*/
        return mp != null || textToSpeech != null;
    }

    public boolean isPlaying() {
        //if (TempHolder.isRecordTTS) {
        //    return false;
        //}
        if (isMp3()) {
            return mp != null && mp.isPlaying();
        }

        synchronized (helpObject) {
            if (textToSpeech == null) {
                return false;
            }
            return textToSpeech != null && textToSpeech.isSpeaking();
        }
    }

    public boolean hasNoEngines() {
        try {
            return textToSpeech != null && (textToSpeech.getEngines() == null || textToSpeech.getEngines().size() == 0);
        } catch (Exception e) {
            return true;
        }
    }

    public String getCurrentLang() {
        try {
            if (Build.VERSION.SDK_INT >= 21 && textToSpeech != null && textToSpeech.getDefaultVoice() != null && textToSpeech.getDefaultVoice().getLocale() != null) {
                return textToSpeech.getDefaultVoice().getLocale().getDisplayLanguage();
            }
        } catch (Exception e) {
            Logcat.e(e);
        }
        return "---";
    }

    public int getEngineCount() {
        try {
            if (textToSpeech == null || textToSpeech.getEngines() == null) {
                return -1;
            }

            return textToSpeech.getEngines().size();
        } catch (Exception e) {
            Logcat.e(e);
        }
        return 0;
    }

    public String getCurrentEngineName() {
        try {
            if (textToSpeech != null) {
                String enginePackage = textToSpeech.getDefaultEngine();
                List<EngineInfo> engines = textToSpeech.getEngines();
                for (final EngineInfo eInfo : engines) {
                    if (eInfo.name.equals(enginePackage)) {
                        return engineToString(eInfo);
                    }
                }
            }
        } catch (Exception e) {
            Logcat.e(e);
        }
        return "---";
    }

    public void loadMP3(String ttsPlayMp3Path) {
        loadMP3(ttsPlayMp3Path, false);
    }

    public void loadMP3(String ttsPlayMp3Path, final boolean play) {
        Logcat.d("loadMP3-", ttsPlayMp3Path);
        if (TextUtils.isEmpty(ttsPlayMp3Path) || !new File(ttsPlayMp3Path).isFile()) {
            Logcat.d("loadMP3-skip mp3");
            return;
        }
        try {
            mp3Destroy();
            mp = new MediaPlayer();
            mp.setDataSource(ttsPlayMp3Path);
            mp.prepare();
            mp.setOnCompletionListener(new OnCompletionListener() {

                @Override
                public void onCompletion(MediaPlayer mp) {
                    mp.pause();
                }
            });
            if (play) {
                mp.start();
            }

            mTimer = new Timer();

            /*mTimer.schedule(new TimerTask() {

                @Override
                public void run() {
                    AppState.get().mp3seek = mp.getCurrentPosition();
                    //Logcat.d("Run timer-task");
                    EventBus.getDefault().post(new TtsStatus());
                };
            }, 1000, 1000);*/
        } catch (Exception e) {
            Logcat.e(e);
        }
    }

    public MediaPlayer getMP() {
        return mp;
    }

    public void mp3Destroy() {
        if (mp != null) {
            mp.stop();
            mp.reset();
            mp = null;
            if (mTimer != null) {
                mTimer.purge();
                mTimer.cancel();
                mTimer = null;
            }
        }
        Logcat.d("mp3Desproy");
    }

    public void mp3Next() {
        int seek = mp.getCurrentPosition();
        mp.seekTo(seek + 5 * 1000);
    }

    public void mp3Prev() {
        int seek = mp.getCurrentPosition();
        mp.seekTo(seek - 5 * 1000);
    }

    public boolean isMp3PlayPause() {
        if (isMp3()) {
            if (mp == null) {
                //loadMP3(BookCSS.get().mp3BookPathGet());
            }
            if (mp == null) {
                return false;
            }
            if (mp.isPlaying()) {
                mp.pause();
            } else {
                mp.start();
            }
            TTSNotification.showLast();
            return true;
        }
        return false;
    }

    public void playMp3() {
        if (mp != null) {
            mp.start();
        }
    }

    public void pauseMp3() {
        if (mp != null) {
            mp.pause();
        }
    }

    public boolean isMp3() {
        //return !TextUtils.isEmpty(BookCSS.get().mp3BookPathGet());
        return true;
    }

    public void seekTo(int i) {
        if (mp != null) {
            mp.seekTo(i);
        }

    }

}
