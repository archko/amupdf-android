package cn.archko.pdf.controller;

import android.view.View;

import cn.archko.pdf.R;
import cn.archko.pdf.viewmodel.DocViewModel;

/**
 * epub,mobi,azw3
 *
 * @author archko
 */
public class EpubPageController extends PdfPageController {

    private final static String TAG = "EpubPageController";
    private View reflowLayout;

    public EpubPageController(View view, DocViewModel docViewModel, PageControllerListener controlListener) {
        super(view, docViewModel, controlListener);

        reflowButton.setVisibility(View.VISIBLE);
        imageButton.setVisibility(View.VISIBLE);
        autoCropButton.setVisibility(View.VISIBLE);
        outlineButton.setVisibility(View.VISIBLE);
        oriButton.setVisibility(View.VISIBLE);
        ttsButton.setVisibility(View.VISIBLE);
        ocrButton.setVisibility(View.VISIBLE);

        reflowLayout = view.findViewById(R.id.reflow_layout);
        reflowLayout.setVisibility(View.VISIBLE);
    }

    public void update(int count, int page, ViewMode viewMode) {
        super.update(count, page);
        if (viewMode == ViewMode.REFLOW) {
            reflowLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public int visibility() {
        return bottomLayout.getVisibility();
    }

    public void show() {
        topLayout.setVisibility(View.VISIBLE);
        bottomLayout.setVisibility(View.VISIBLE);
        reflowLayout.setVisibility(View.VISIBLE);
        updateOrientation();
    }

    public void hide() {
        topLayout.setVisibility(View.GONE);
        bottomLayout.setVisibility(View.GONE);
        reflowLayout.setVisibility(View.GONE);
    }
}
