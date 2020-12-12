package cn.archko.pdf.widgets;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import cn.archko.pdf.R;
import cn.archko.pdf.presenter.PageViewPresenter;
import cn.archko.pdf.utils.FileUtils;

/**
 * page seek controls
 *
 * @author archko
 */
public class APageSeekBarControls extends LinearLayout implements View.OnClickListener {

    private final static String TAG = "APageSeekBarControls";

    PageViewPresenter mPageViewPresenter;
    protected SeekBar mPageSlider;
    protected int mPageSliderRes = 1;
    protected TextView mPageNumberView;
    protected Runnable gotoPageRunnable = null;

    private ImageButton mReflowButton;
    private ImageButton mOutlineButton;
    private ImageButton mAutoCropButton;
    private TextView mPath;
    private TextView mTitle;
    private ImageButton mBackButton;
    private FrameLayout mLayoutOutline;

    public APageSeekBarControls(Context context, PageViewPresenter pageViewPresenter) {
        super(context);
        onCreate(context);
        mPageViewPresenter = pageViewPresenter;
    }

    public void onCreate(Context context) {
        setOrientation(LinearLayout.VERTICAL);
        LayoutInflater.from(context).inflate(R.layout.view_seek_bar_controls, this);

        mPageSlider = findViewById(R.id.seek_bar);
        //mPageSlider.setId(10000);
        mPageNumberView = findViewById(R.id.page_num);
        mReflowButton = findViewById(R.id.reflowButton);
        mOutlineButton = findViewById(R.id.outlineButton);
        mAutoCropButton = findViewById(R.id.autoCropButton);
        mPath = findViewById(R.id.path);
        mTitle = findViewById(R.id.title);
        mBackButton = findViewById(R.id.back_button);
        mBackButton.setColorFilter(Color.argb(0xFF, 255, 255, 255));
        mLayoutOutline = findViewById(R.id.layout_outline);

        mReflowButton.setOnClickListener(this);
        mOutlineButton.setOnClickListener(this);
        mAutoCropButton.setOnClickListener(this);
        mBackButton.setOnClickListener(this);

        mPageSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onStopTrackingTouch(SeekBar seekBar) {
                gotoPage((seekBar.getProgress() + mPageSliderRes / 2) / mPageSliderRes);
                showPageSlider(false);
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int index = (progress + mPageSliderRes / 2) / mPageSliderRes;
                mPageNumberView.setText(String.format("%s / %s", index + 1, mPageViewPresenter.getPageCount()));
                showPageSlider(false);
            }
        });
        this.gotoPageRunnable = new Runnable() {
            public void run() {
                fadePageSlider();
            }
        };
    }

    public void showPageSlider(boolean force) {
        //mPageSlider.setVisibility(View.VISIBLE);
        mPageNumberView.setVisibility(View.VISIBLE);

        if (!force) {
            return;
        }

        int index = null != mPageViewPresenter ? mPageViewPresenter.getCurrentPageIndex() : 0;
        updatePageProgress(index);
    }

    public void updatePageProgress(int index) {
        int count = null != mPageViewPresenter ? mPageViewPresenter.getPageCount() : 0;
        mPageNumberView.setText(String.format("%d / %d", index + 1, count));
        mPageSlider.setMax((count - 1) * mPageSliderRes);
        mPageSlider.setProgress(index * mPageSliderRes);
    }

    protected void fadePageSlider() {
        //mPageSlider.setVisibility(View.GONE);
        mPageNumberView.setVisibility(View.GONE);
    }

    public void updateTitle(String path) {
        mPath.setText(FileUtils.getDir((path)));
        mTitle.setText(FileUtils.getName(path));
    }

    public void showGotoPageView() {
        if (null != mPageViewPresenter) {
            int smax = Math.max(mPageViewPresenter.getPageCount() - 1, 1);
            mPageSliderRes = ((10 + smax - 1) / smax) * 2;
            showPageSlider(true);
        }
    }

    protected void gotoPage(int page) {
        if (null != mPageViewPresenter && mPageViewPresenter.getCurrentPageIndex() != page) {
            mPageViewPresenter.goToPageIndex(page);
        }
    }

    public void toggleSeekControls() {
        if (getVisibility() == View.VISIBLE) {
            hide();
        } else {
            show();
        }
    }

    public void show() {
        setVisibility(VISIBLE);
        showGotoPageView();
    }

    public void hide() {
        setVisibility(GONE);
    }

    public void fade() {
        show();
        postDelayed(new Runnable() {
            @Override
            public void run() {
                hide();
            }
        }, 3000);
    }

    public void showReflow(boolean reflow) {
        this.mReflowButton.setVisibility(reflow ? VISIBLE : GONE);
    }

    public ImageButton getReflowButton() {
        return mReflowButton;
    }

    public ImageButton getAutoCropButton() {
        return mAutoCropButton;
    }

    @Override
    public void onClick(View v) {
        if (R.id.outlineButton == v.getId()) {
            mPageViewPresenter.showOutline();
        } else if (R.id.back_button == v.getId()) {
            mPageViewPresenter.back();
        } else if (R.id.reflowButton == v.getId()) {
            mPageViewPresenter.reflow();
        } else if (R.id.autoCropButton == v.getId()) {
            mPageViewPresenter.autoCrop();
        }
    }

    public FrameLayout getLayoutOutline() {
        return mLayoutOutline;
    }
}
