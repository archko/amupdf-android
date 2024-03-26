package cn.archko.pdf.imagedroid;

import android.app.ProgressDialog;

import org.vudroid.core.BaseViewerActivity;
import org.vudroid.core.DecodeService;
import org.vudroid.core.DecodeServiceBase;

import cn.archko.pdf.core.common.AppExecutors;
import cn.archko.pdf.imagedroid.codec.AlbumContext;

public class AlbumViewerActivity extends BaseViewerActivity {

    protected ProgressDialog progressDialog;

    @Override
    protected DecodeService createDecodeService() {
        return new DecodeServiceBase(new AlbumContext());
    }

    public void loadDocument(String path) {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading");
        progressDialog.show();

        AppExecutors.Companion.getInstance().diskIO().execute(() -> {
            getDecodeService().open(path);
            AppExecutors.Companion.getInstance().mainThread().execute(() -> {
                progressDialog.dismiss();
                getDocumentView().showDocument();
            });
        });
    }
}
