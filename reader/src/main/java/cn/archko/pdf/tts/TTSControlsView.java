package cn.archko.pdf.tts;

import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import java.io.File;

import cn.archko.pdf.R;
import cn.archko.pdf.core.common.Logcat;

public class TTSControlsView extends FrameLayout {

    private Handler handler;
    private ImageView ttsPlayPause;
    private ImageView ttsStop;
    //private DocumentController controller;
    private ImageView ttsDialog;
    private View layoutMp3;
    private SeekBar seekMp3;
    private TextView seekCurrent;
    private TextView seekMax;
    private TextView trackName;
    private int colorTint;
    Runnable update = new Runnable() {

        @Override
        public void run() {
            try {
                if (TTSEngine.get().isMp3()) {
                    initMp3();
                    if (TTSEngine.get().mp != null) {
                        //seekCurrent.setText(TxtUtils.getMp3TimeString(TTSEngine.get().mp.getCurrentPosition()));
                        //seekMax.setText(TxtUtils.getMp3TimeString(TTSEngine.get().mp.getDuration()));

                        seekMp3.setMax(TTSEngine.get().mp.getDuration());
                        seekMp3.setProgress(TTSEngine.get().mp.getCurrentPosition());

                        udateButtons();
                    }

                } else {
                    layoutMp3.setVisibility(View.GONE);
                    trackName.setVisibility(View.GONE);
                }

                Logcat.d("TtsStatus-isPlaying:" + TTSEngine.get().isPlaying());
                ttsPlayPause.setImageResource(TTSEngine.get().isPlaying() ? R.drawable.glyphicons_174_pause : R.drawable.glyphicons_175_play);
            } catch (Exception e) {
                //Logcat.e(e);
            }
        }
    };

    public TTSControlsView(final Context context, AttributeSet attrs) {
        super(context, attrs);

        View view = LayoutInflater.from(getContext()).inflate(R.layout.tts_mp3_line, this, false);
        addView(view);

        handler = new Handler();
        seekMp3 = view.findViewById(R.id.seekMp3);
        seekCurrent = view.findViewById(R.id.seekCurrent);
        seekMax = view.findViewById(R.id.seekMax);
        layoutMp3 = view.findViewById(R.id.layoutMp3);
        ttsStop = view.findViewById(R.id.ttsStop);
        ttsPlayPause = view.findViewById(R.id.ttsPlay);

        trackName = view.findViewById(R.id.trackName);

        ttsDialog = view.findViewById(R.id.ttsDialog);
        ttsDialog.setVisibility(View.GONE);
        trackName.setVisibility(View.GONE);

        /*if (AppState.get().isUiTextColor) {
            colorTint = AppState.get().uiTextColor;
        } else {
            colorTint = AppState.get().tintColor;
        }
        if (colorTint == Color.WHITE && AppState.get().isDayNotInvert) {
            colorTint = Color.BLACK;
        }
        if (colorTint == Color.BLACK && !AppState.get().isDayNotInvert) {
            colorTint = Color.WHITE;
        }
        if (colorTint == Color.WHITE) {
            colorTint = MagicHelper.otherColor(Color.WHITE, 0.2f);
        }*/

        int alpha = 240;
        //TintUtil.setTintImageWithAlpha(ttsPlayPause, colorTint, alpha);
        //TintUtil.setTintImageWithAlpha(ttsDialog, colorTint, alpha);
        //TintUtil.setTintText(trackName, colorTint);

        ttsStop.setOnClickListener(v -> {
            try {
                PendingIntent next = PendingIntent.getService(
                        context,
                        0,
                        new Intent(TTSNotification.TTS_STOP_DESTROY, null, context, TTSService.class),
                        PendingIntent.FLAG_IMMUTABLE);
                next.send();
            } catch (CanceledException e) {
                Logcat.e(e);
            }
        });

        ttsPlayPause.setOnClickListener(v -> {
            /*if (TTSEngine.get().getEngineCount() == 0) {
                Urls.openTTS(getContext());
            } else {
                TTSService.playPause(context, controller);
            }*/
        });

        //TintUtil.setDrawableTint(seekMp3.getProgressDrawable(), colorTint, alpha);

        if (Build.VERSION.SDK_INT >= 16) {
            //TintUtil.setDrawableTint(seekMp3.getThumb(), colorTint, alpha);
        }
        //TintUtil.setTintText(seekCurrent, colorTint);
        //TintUtil.setTintText(seekMax, colorTint);

        layoutMp3.setVisibility(View.GONE);
        initMp3();

        trackName.setOnClickListener(v -> {
            /*MyPopupMenu menu = new MyPopupMenu(v);
            for (final File file : TTSTracks.getAllMp3InFolder()) {
                menu.getMenu().add(file.getName()).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        TTSEngine.get().stop();
                        BookCSS.get().mp3BookPath(file.getPath());
                        TTSEngine.get().loadMP3(file.getPath(), true);
                        udateButtons();
                        return false;
                    }


                });
            }
            menu.show();*/

        });
        //Apps.accessibilityButtonSize(ttsPlayPause);
        //Apps.accessibilityButtonSize(ttsDialog);
    }

    public void addOnDiaLogcatRunnable(final Runnable run) {
        ttsDialog.setVisibility(View.VISIBLE);
        ttsDialog.setOnClickListener(v -> run.run());
    }

    public void initMp3() {
        if (TTSEngine.get().isMp3() && layoutMp3.getVisibility() == View.GONE) {
            layoutMp3.setVisibility(View.VISIBLE);
            trackName.setVisibility(View.VISIBLE);

            udateButtons();

            seekMp3.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        Logcat.d("Seek-onProgressChanged", String.valueOf(progress));
                        TTSEngine.get().mp.seekTo(progress);
                    }
                }
            });

        }
    }

    public void udateButtons() {
        trackName.setText(TTSTracks.getCurrentTrackName());

        boolean isMulty = TTSTracks.isMultyTracks();
        //ttsPrevTrack.setVisibility(TxtUtils.visibleIf(isMulty));
        //ttsNextTrack.setVisibility(TxtUtils.visibleIf(isMulty));

        //TintUtil.setTintImageWithAlpha(ttsPrevTrack, TTSTracks.getPrevTrack() != null ? colorTint : Color.GRAY);
        //TintUtil.setTintImageWithAlpha(ttsNextTrack, TTSTracks.getNextTrack() != null ? colorTint : Color.GRAY);

    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        //EventBus.getDefault().register(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        //EventBus.getDefault().unregister(this);
        handler.removeCallbacksAndMessages(null);
    }

    public void onTTSStatus(TtsStatus status) {
        if (ttsPlayPause != null) {
            handler.removeCallbacksAndMessages(null);
            handler.postDelayed(update, 200);
        }
    }

    public void reset() {
        //TTSEngine.get().loadMP3(BookCSS.get().mp3BookPathGet());
        update.run();
    }

}
