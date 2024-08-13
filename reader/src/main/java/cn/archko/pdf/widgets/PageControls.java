package cn.archko.pdf.widgets;

import android.graphics.Color;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import cn.archko.pdf.R;
import cn.archko.pdf.core.utils.FileUtils;

/**
 * page seek controls
 *
 * @author archko
 */
public class PageControls implements View.OnClickListener {

    private final static String TAG = "PageControls";

    protected View topLayout;
    protected View bottomLayout;
    protected SeekBar mPageSlider;
    protected TextView mPageNumber;

    private ImageButton mReflowButton;
    private ImageButton mOutlineButton;
    private ImageButton mAutoCropButton;
    private ImageButton mOriButton;
    private ImageButton ttsButton;
    private TextView mPath;
    private TextView mTitle;
    private ImageButton mBackButton;
    private int ori = LinearLayout.VERTICAL;
    private int count = 1;
    private ControlListener controlListener;

    public interface ControlListener {
        void changeOrientation(int ori);

        void back();

        void showOutline();

        void gotoPage(int page);

        void toggleReflow();

        void toggleCrop();

        void toggleTts();
    }

    public PageControls(View view, ControlListener controlListener) {
        this.controlListener = controlListener;

        topLayout = view.findViewById(R.id.top_layout);
        bottomLayout = view.findViewById(R.id.bottom_layout);

        mPageSlider = view.findViewById(R.id.seek_bar);
        mPageNumber = view.findViewById(R.id.page_num);
        mReflowButton = view.findViewById(R.id.reflowButton);
        mOutlineButton = view.findViewById(R.id.outlineButton);
        mAutoCropButton = view.findViewById(R.id.autoCropButton);
        mOriButton = view.findViewById(R.id.oriButton);
        ttsButton = view.findViewById(R.id.ttsButton);
        mPath = view.findViewById(R.id.path);
        mTitle = view.findViewById(R.id.title);
        mBackButton = view.findViewById(R.id.back_button);

        mReflowButton.setOnClickListener(this);
        mOutlineButton.setOnClickListener(this);
        mAutoCropButton.setOnClickListener(this);
        mOriButton.setOnClickListener(this);
        ttsButton.setOnClickListener(this);
        mBackButton.setOnClickListener(this);

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

    public void updateTitle(String path) {
        mPath.setText(FileUtils.getDir((path)));
        mTitle.setText(FileUtils.getName(path));
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
        topLayout.setVisibility(View.VISIBLE);
        bottomLayout.setVisibility(View.VISIBLE);
        updateOrientation();
    }

    public void hide() {
        topLayout.setVisibility(View.GONE);
        bottomLayout.setVisibility(View.GONE);
    }

    public void showReflow(boolean reflow) {
        //mReflowButton.setVisibility(reflow ? View.VISIBLE : View.GONE);
        if (reflow) {
            mReflowButton.setColorFilter(Color.argb(0xFF, 172, 114, 37));
        } else {
            mReflowButton.setColorFilter(Color.argb(0xFF, 255, 255, 255));
        }
    }

    public ImageButton getReflowButton() {
        return mReflowButton;
    }

    public ImageButton getAutoCropButton() {
        return mAutoCropButton;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (R.id.outlineButton == id) {
            controlListener.showOutline();
        } else if (R.id.back_button == id) {
            controlListener.back();
        } else if (R.id.reflowButton == id) {
            controlListener.toggleReflow();
        } else if (R.id.autoCropButton == id) {
            controlListener.toggleCrop();
        } else if (R.id.ttsButton == id) {
            controlListener.toggleTts();
        } else if (R.id.oriButton == id) {
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

    public int topVisibility() {
        return topLayout.getVisibility();
    }

    public int bottomVisibility() {
        return bottomLayout.getVisibility();
    }
}
