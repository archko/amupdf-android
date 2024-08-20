package org.vudroid.core;

import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import cn.archko.pdf.R;

/**
 * page seek controls
 *
 * @author archko
 */
public class SeekbarControls implements View.OnClickListener {

    private final static String TAG = "SeekbarControls";

    protected View bottomLayout;
    protected SeekBar mPageSlider;
    protected TextView mPageNumber;
    private ImageButton mOriButton;
    private int ori = LinearLayout.VERTICAL;
    private int count = 1;
    private ControlListener controlListener;

    public interface ControlListener {
        void changeOrientation(int ori);

        void gotoPage(int page);
    }

    public SeekbarControls(View view, ControlListener controlListener) {
        this.controlListener = controlListener;
        bottomLayout = view.findViewById(R.id.bottom_layout);

        mPageSlider = view.findViewById(R.id.seek_bar);
        mPageNumber = view.findViewById(R.id.page_num);
        mOriButton = view.findViewById(R.id.oriButton);

        mOriButton.setOnClickListener(this);

        mPageSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onStopTrackingTouch(SeekBar seekBar) {
                gotoPage((seekBar.getProgress()));
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int index = (progress);
                mPageNumber.setText(String.format("%s / %s", index + 1, count));
            }
        });
    }

    public void update(int count, int page) {
        this.count = count;
        mPageSlider.setMax((count - 1));
        updatePageProgress(page);
    }

    public void updatePageProgress(int index) {
        mPageNumber.setText(String.format("%d / %d", index + 1, count));
        mPageSlider.setProgress(index);
    }

    private void gotoPage(int page) {
        controlListener.gotoPage(page);
    }

    public void toggleControls() {
        if (bottomLayout.getVisibility() == View.VISIBLE) {
            hide();
        } else {
            show();
        }
    }

    public int getOrientation() {
        return ori;
    }

    public void setOrientation(int ori) {
        this.ori = ori;
    }

    private void updateOrientation() {
        if (ori == LinearLayout.VERTICAL) {
            mOriButton.setImageResource(R.drawable.ic_vertical);
        } else {
            mOriButton.setImageResource(R.drawable.ic_horizontal);
        }
    }

    public void show() {
        bottomLayout.setVisibility(View.VISIBLE);
        updateOrientation();
    }

    public void hide() {
        bottomLayout.setVisibility(View.GONE);
    }

    public ImageButton getOriButton() {
        return mOriButton;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (R.id.oriButton == id) {
            if (ori == LinearLayout.VERTICAL) {
                ori = LinearLayout.HORIZONTAL;
            } else {
                ori = LinearLayout.VERTICAL;
            }
            updateOrientation();
            controlListener.changeOrientation(ori);
        }
    }

    public int visibility() {
        return bottomLayout.getVisibility();
    }
}
