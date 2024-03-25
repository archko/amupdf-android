package org.vudroid.pdfdroid;

import android.content.Intent;
import android.net.Uri;
import android.view.View;

import com.jeremyliao.liveeventbus.LiveEventBus;

import org.vudroid.core.BaseViewerActivity;
import org.vudroid.core.DecodeService;
import org.vudroid.core.DecodeServiceBase;
import org.vudroid.core.DocumentView;
import org.vudroid.pdfdroid.codec.PdfContext;
import org.vudroid.pdfdroid.codec.PdfDocument;

import cn.archko.pdf.common.MenuHelper;
import cn.archko.pdf.common.OutlineHelper;
import cn.archko.pdf.core.common.Event;
import cn.archko.pdf.core.decode.MupdfDocument;
import cn.archko.pdf.listeners.OutlineListener;

public class PdfViewerActivity extends BaseViewerActivity implements OutlineListener {

    private static final int OUTLINE_REQUEST = 0;

    @Override
    protected DecodeService createDecodeService() {
        return new DecodeServiceBase(new PdfContext());
    }

    @Override
    protected void onPause() {
        super.onPause();
        Uri uri = getIntent().getData();
        String filePath = Uri.decode(uri.getEncodedPath());
        DocumentView documentView = getDocumentView();
        getPdfViewModel().saveBookProgress(
                filePath,
                getDecodeService().getPageCount(),
                documentView.getCurrentPage(),
                documentView.getZoomModel().getZoom() * 1000f,
                documentView.getScrollX(),
                documentView.getScrollY()
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LiveEventBus
                .get(Event.ACTION_STOPPED)
                .post(null);
    }

    private OutlineHelper mOutlineHelper;
    private MenuHelper mMenuHelper;

    public void openOutline() {
        DecodeServiceBase service = (DecodeServiceBase) getDecodeService();
        PdfDocument document = (PdfDocument) service.getDocument();
        if (null == mOutlineHelper) {
            MupdfDocument mupdfDocument = new MupdfDocument(this);
            mupdfDocument.setDocument(document.getCore());
            mOutlineHelper = new OutlineHelper(mupdfDocument, this);
        }
        //mOutlineHelper.openOutline(getDocumentView().getCurrentPage(), OUTLINE_REQUEST);
        if (mMenuHelper == null) {
            mMenuHelper = new MenuHelper(null, mOutlineHelper, getSupportFragmentManager());
            mMenuHelper.setupOutline(currentPage());
        }

        mMenuHelper.updateSelection(currentPage());

        if (mOutlineHelper.hasOutline()) {
            if (getPageSeekBarControls().getLayoutOutline().getVisibility() == View.GONE) {
                getPageSeekBarControls().getLayoutOutline().setVisibility(View.VISIBLE);
            } else {
                getPageSeekBarControls().getLayoutOutline().setVisibility(View.GONE);
            }
        } else {
            getPageSeekBarControls().getLayoutOutline().setVisibility(View.GONE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case OUTLINE_REQUEST:
                if (resultCode >= 0)
                    getDocumentView().goToPage(resultCode - RESULT_FIRST_USER);
                getPageSeekBarControls().hide();
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onSelectedOutline(int index) {
        if (index >= 0) {
            getDocumentView().goToPage(index - RESULT_FIRST_USER);
        }
        getPageSeekBarControls().hide();
        getPageControls().hide();
    }
}
