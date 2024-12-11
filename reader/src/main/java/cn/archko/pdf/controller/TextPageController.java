package cn.archko.pdf.controller;

import android.view.View;

import cn.archko.pdf.viewmodel.DocViewModel;

/**
 * text,html,js,json.eg.
 *
 * @author archko
 */
public class TextPageController extends DefaultPageController {

    private final static String TAG = "TextPageController";

    public TextPageController(View view, DocViewModel docViewModel, PageControllerListener controlerListener) {
        super(view, docViewModel, controlerListener);
        reflowButton.setVisibility(View.GONE);
        imageButton.setVisibility(View.GONE);
        outlineButton.setVisibility(View.GONE);
        autoCropButton.setVisibility(View.GONE);
        ocrButton.setVisibility(View.GONE);
        ttsButton.setVisibility(View.VISIBLE);
    }

    public void update(int count, int page, ViewMode viewMode) {
        this.count = count;
        mPageSlider.setMax((count - 1));
        updatePageProgress(page);
    }

    @Override
    public void setReflowButton(int reflow) {
    }

}
