package cn.archko.pdf.tts;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import cn.archko.pdf.R;

public class TTSActivity extends Activity {

    public static void start(Context context) {
        context.startActivity(new Intent(context, TTSActivity.class));
    }

    public static final String TAG = "TTSActivity";
    public static final int CHECK_REQUEST_CODE = 0x010;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.tts_controller);
        View ttsPlayPause = findViewById(R.id.ttsPlay);
        ttsPlayPause.setOnClickListener(v -> togglePlay());

        TTSEngine.get().getTTS(status -> {
                    if (status == TextToSpeech.SUCCESS) {
                        //TTSEngine.get().speak("1", "TextToSpeech.QUEUE_ADD, null, null");
                        //TTSEngine.get().speak("2", "requestedVisible:true, getLeash:Surface(name=Surface(name=ea7ad61 NavigationBar0");
                        //TTSEngine.get().speak("3", "TTS是语音合成应用的一种，它将储存于电脑中的文件，如帮助文件或者网页，转换成自然语音输出。\n" +
                        //        "著作权归作者所有。商业转载请联系作者获得授权，非商业转载请注明出处。");
                    } else {
                        // 初始化失败
                        Log.e(TAG, "初始化失败");
                    }
                }
        );

        //Intent checkIntent = new Intent();
        //checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        //startActivityForResult(checkIntent, CHECK_REQUEST_CODE);
    }

    private void togglePlay() {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        checkSupportTTSLocale(requestCode, resultCode, data);
    }

    public List<Locale> checkSupportTTSLocale(int requestCode, int resultCode, Intent data) {
        ArrayList<Locale> locales = new ArrayList<>();
        if (requestCode == CHECK_REQUEST_CODE && resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS && data != null) {
            ArrayList<String> availableVoices = data.getStringArrayListExtra(TextToSpeech.Engine.EXTRA_AVAILABLE_VOICES);
            if (availableVoices != null && availableVoices.size() > 0) {
                for (String voice : availableVoices) {
                    String[] split = voice.split("-");
                    if (split.length > 0) {
                        Locale locale = split.length == 1 ? new Locale(split[0]) : (split.length == 2 ? new Locale(split[0], split[1]) : new Locale(split[0], split[1], split[2]));
                        locales.add(locale);
                    }
                }
            }
        }
        Log.e(TAG, "checkSupportTTSLocale:" + locales); //[zho_CHN, eng_USA]

        return locales;
    }
}
