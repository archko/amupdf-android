package cn.archko.pdf.controller;

import android.view.View;

import cn.archko.pdf.viewmodel.DocViewModel;

/**
 * epub,mobi,azw3
 *
 * @author archko
 */
public class EpubPageController extends PdfPageController {

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
        fontButton.setVisibility(View.VISIBLE);
    }

    public void update(int count, int page, ViewMode viewMode) {
        super.update(count, page);

        fontButton.setVisibility(View.VISIBLE);
    }
}
