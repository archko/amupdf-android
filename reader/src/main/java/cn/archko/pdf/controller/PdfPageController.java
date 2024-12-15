package cn.archko.pdf.controller;

import android.graphics.Color;
import android.view.View;

import cn.archko.pdf.R;
import cn.archko.pdf.core.entity.BookProgress;
import cn.archko.pdf.viewmodel.DocViewModel;

/**
 * pdf,xps,djvu
 *
 * @author archko
 */
public class PdfPageController extends DefaultPageController {

    private final static String TAG = "PdfPageController";

    public PdfPageController(View view, DocViewModel docViewModel, PageControllerListener controlerListener) {
        super(view, docViewModel, controlerListener);

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
        showReflowImage(docViewModel.getReflow());

        reflowButton.setVisibility(View.VISIBLE);
        imageButton.setVisibility(View.VISIBLE);
        autoCropButton.setVisibility(View.VISIBLE);
        outlineButton.setVisibility(View.VISIBLE);
        ttsButton.setVisibility(View.VISIBLE);
        ocrButton.setVisibility(View.VISIBLE);

        if (viewMode == ViewMode.REFLOW_SCAN) {
            oriButton.setVisibility(View.GONE);
            searchButton.setVisibility(View.GONE);
        } else {
            oriButton.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void setReflowButton(int reflow) {
        boolean crop;
        if (reflow == BookProgress.REFLOW_TXT) {
            crop = false;
            reflowButton.setColorFilter(Color.argb(0xFF, 172, 114, 37));
            imageButton.setColorFilter(Color.argb(0xFF, 255, 255, 255));
        } else if (reflow == BookProgress.REFLOW_SCAN) {
            crop = false;
            reflowButton.setColorFilter(Color.argb(0xFF, 255, 255, 255));
            imageButton.setColorFilter(Color.argb(0xFF, 172, 114, 37));
        } else {
            reflowButton.setColorFilter(Color.argb(0xFF, 255, 255, 255));
            imageButton.setColorFilter(Color.argb(0xFF, 255, 255, 255));
            crop = docViewModel.checkCrop();
        }
        setCropButton(crop);
    }

    private void setCropButton(boolean crop) {
        if (crop) {
            autoCropButton.setImageResource(R.drawable.ic_crop);
        } else {
            autoCropButton.setImageResource(R.drawable.ic_no_crop);
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

    public void showReflowImage(int reflow) {
        boolean shouldReflow = reflow == BookProgress.REFLOW_SCAN;
        if (shouldReflow) {
            imageButton.setColorFilter(Color.argb(0xFF, 172, 114, 37));
        } else {
            imageButton.setColorFilter(Color.argb(0xFF, 255, 255, 255));
        }
    }
}
