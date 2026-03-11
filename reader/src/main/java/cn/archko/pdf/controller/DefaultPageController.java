package cn.archko.pdf.controller;

import android.graphics.Color;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import cn.archko.pdf.R;
import cn.archko.pdf.core.utils.FileUtils;
import cn.archko.pdf.viewmodel.DocViewModel;

/**
 * 公用的顶部栏,有公共的按钮与对应的处理事件
 *
 * @author archko
 */
public abstract class DefaultPageController implements IPageController, View.OnClickListener {

    private final static String TAG = "PdfPageController";

    protected View topLayout;
    protected View bottomLayout;
    protected SeekBar mPageSlider;
    protected TextView mPageNumber;

    protected ImageButton ttsButton;
    protected ImageButton selectButton;
    protected ImageButton penButton;
    protected ImageButton autoCropButton;
    protected ImageButton outlineButton;
    protected ImageButton reflowButton;
    protected ImageButton imageButton;
    protected ImageButton oriButton;
    protected ImageButton ocrButton;
    protected ImageButton aiButton;
    protected ImageButton bookmarkButton;
    protected ImageButton previewButton;
    //protected TextView pathView;
    protected TextView titleView;
    protected View layoutTitle;
    protected View layoutSearch;
    protected ImageButton searchButton;
    protected ImageButton nextBtn;
    protected ImageButton prevBtn;
    protected ImageButton closeBtn;
    protected ImageButton mBackButton;
    protected int ori = LinearLayout.VERTICAL;
    protected boolean selection = false;
    protected boolean draw = false;
    protected int count = 1;
    protected PageControllerListener controllerListener;
    protected DocViewModel docViewModel;

    public DefaultPageController(View view, DocViewModel docViewModel, PageControllerListener controlListener) {
        this.controllerListener = controlListener;
        this.docViewModel = docViewModel;

        topLayout = view.findViewById(R.id.top_layout);
        bottomLayout = view.findViewById(R.id.bottom_layout);

        mPageSlider = view.findViewById(R.id.seek_bar);
        mPageNumber = view.findViewById(R.id.page_num);

        ttsButton = view.findViewById(R.id.ttsButton);
        selectButton = view.findViewById(R.id.selectButton);
        penButton = view.findViewById(R.id.penButton);
        autoCropButton = view.findViewById(R.id.autoCropButton);
        outlineButton = view.findViewById(R.id.outlineButton);
        reflowButton = view.findViewById(R.id.reflowButton);
        imageButton = view.findViewById(R.id.imageButton);
        oriButton = view.findViewById(R.id.oriButton);
        ocrButton = view.findViewById(R.id.ocrButton);
        aiButton = view.findViewById(R.id.aiButton);
        bookmarkButton = view.findViewById(R.id.bookmarkButton);
        previewButton = view.findViewById(R.id.previewButton);
        //pathView = view.findViewById(R.id.path);
        titleView = view.findViewById(R.id.title);
        layoutTitle = view.findViewById(R.id.layout_path);
        layoutSearch = view.findViewById(R.id.layout_search);
        searchButton = view.findViewById(R.id.searchButton);
        nextBtn = view.findViewById(R.id.nextButton);
        prevBtn = view.findViewById(R.id.prevButton);
        closeBtn = view.findViewById(R.id.closeButton);
        mBackButton = view.findViewById(R.id.back_button);

        ttsButton.setOnClickListener(this);
        selectButton.setOnClickListener(this);
        penButton.setOnClickListener(this);
        autoCropButton.setOnClickListener(this);
        outlineButton.setOnClickListener(this);
        reflowButton.setOnClickListener(this);
        imageButton.setOnClickListener(this);
        oriButton.setOnClickListener(this);
        mBackButton.setOnClickListener(this);
        ocrButton.setOnClickListener(this);
        aiButton.setOnClickListener(this);
        bookmarkButton.setOnClickListener(this);
        previewButton.setOnClickListener(this);
        nextBtn.setOnClickListener(this);
        prevBtn.setOnClickListener(this);
        closeBtn.setOnClickListener(this);
        searchButton.setOnClickListener(this);

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
        //pathView.setText(FileUtils.getDir((path)));
        titleView.setText(FileUtils.getName(path));
    }

    public void gotoPage(int page) {
        controllerListener.gotoPage(page);
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

    public void updateOrientation() {
        if (ori == LinearLayout.VERTICAL) {
            oriButton.setImageResource(R.drawable.ic_vertical);
        } else {
            oriButton.setImageResource(R.drawable.ic_horizontal);
        }
    }

    public void setSelection(boolean selection) {
        this.selection = selection;
    }

    public void setOraw(boolean draw) {
        this.draw = draw;
    }

    public void updateSelection() {
        if (selection) {
            selectButton.setColorFilter(Color.argb(0xFF, 0, 255, 0));
        } else {
            selectButton.setColorFilter(Color.argb(0xFF, 255, 255, 255));
        }
    }

    public void updateDraw() {
        if (draw) {
            penButton.setColorFilter(Color.argb(0xFF, 0, 255, 0));
        } else {
            penButton.setColorFilter(Color.argb(0xFF, 255, 255, 255));
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

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (R.id.outlineButton == id) {
            controllerListener.showOutline();
        } else if (R.id.back_button == id) {
            controllerListener.back();
        } else if (R.id.reflowButton == id) {
            controllerListener.toggleReflow();
        } else if (R.id.imageButton == id) {
            controllerListener.toggleReflowImage();
        } else if (R.id.autoCropButton == id) {
            controllerListener.toggleCrop();
        } else if (R.id.ttsButton == id) {
            controllerListener.toggleTts();
        } else if (R.id.oriButton == id) {
            if (ori == LinearLayout.VERTICAL) {
                ori = LinearLayout.HORIZONTAL;
            } else {
                ori = LinearLayout.VERTICAL;
            }
            updateOrientation();
            controllerListener.changeOrientation(ori);
        } else if (R.id.ocrButton == id) {
            controllerListener.ocr();
        } else if (R.id.previewButton == id) {
            controllerListener.preview();
        } else if (R.id.searchButton == id) {
            controllerListener.showSearch();
        } else if (R.id.closeButton == id) {
            layoutSearch.setVisibility(View.GONE);
            layoutTitle.setVisibility(View.VISIBLE);
            controllerListener.clearSearch();
        } else if (R.id.nextButton == id) {
            controllerListener.next("");
        } else if (R.id.prevButton == id) {
            controllerListener.prev("");
        } else if (R.id.selectButton == id) {
            selection = !selection;
            updateSelection();
            if (selection){
                draw=false;
                updateDraw();;
            }
            controllerListener.setSelection(selection);
        } else if (R.id.penButton == id) {
            draw = !draw;
            updateDraw();
            if (draw) {
                selection = false;
                updateSelection();
            }
            controllerListener.setDraw(draw);
        } else if (R.id.aiButton == id) {
            controllerListener.ai();
        } else if (R.id.bookmarkButton == id) {
            controllerListener.bookmark();
        }
    }

    @Override
    public int visibility() {
        return bottomLayout.getVisibility();
    }

    @Override
    public int topVisibility() {
        return topLayout.getVisibility();
    }

    @Override
    public int bottomVisibility() {
        return bottomLayout.getVisibility();
    }

    @Override
    public void showSearch() {
        layoutSearch.setVisibility(View.VISIBLE);
        layoutTitle.setVisibility(View.GONE);
    }
}
