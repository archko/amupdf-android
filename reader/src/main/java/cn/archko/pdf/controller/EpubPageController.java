package cn.archko.pdf.controller;

import android.graphics.Color;
import android.view.View;

import cn.archko.pdf.core.entity.BookProgress;
import cn.archko.pdf.viewmodel.DocViewModel;

/**
 * epub,mobi,azw3
 *
 * @author archko
 */
public class EpubPageController extends DefaultPageController {

    private final static String TAG = "EpubPageController";

    public EpubPageController(View view, DocViewModel docViewModel, PageControllerListener controlListener) {
        super(view, docViewModel, controlListener);

        reflowButton.setVisibility(View.VISIBLE);
        imageButton.setVisibility(View.VISIBLE);
        autoCropButton.setVisibility(View.VISIBLE);
        outlineButton.setVisibility(View.VISIBLE);
        oriButton.setVisibility(View.VISIBLE);
        ttsButton.setVisibility(View.VISIBLE);
        ocrButton.setVisibility(View.VISIBLE);
    }

    public void update(int count, int page, ViewMode viewMode) {
        super.update(count, page);

        showReflow(docViewModel.getReflow());

        reflowButton.setVisibility(View.VISIBLE);
        imageButton.setVisibility(View.VISIBLE);
        autoCropButton.setVisibility(View.VISIBLE);
        outlineButton.setVisibility(View.VISIBLE);
        ttsButton.setVisibility(View.VISIBLE);
        ocrButton.setVisibility(View.VISIBLE);

        if (viewMode == ViewMode.REFLOW_SCAN) {
            oriButton.setVisibility(View.GONE);
        } else {
            oriButton.setVisibility(View.VISIBLE);
        }
    }

    public void showReflow(int reflow) {
        boolean shouldReflow = reflow == BookProgress.REFLOW_TXT;
        if (shouldReflow) {
            reflowButton.setColorFilter(Color.argb(0xFF, 172, 114, 37));
        } else {
            reflowButton.setColorFilter(Color.argb(0xFF, 255, 255, 255));
        }
    }

    @Override
    public void setReflowButton(int reflow) {
    }
}
