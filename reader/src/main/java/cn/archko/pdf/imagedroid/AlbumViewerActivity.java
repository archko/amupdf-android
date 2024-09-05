package cn.archko.pdf.imagedroid;

import android.app.ProgressDialog;
import android.widget.Toast;

import org.vudroid.core.BaseViewerActivity;
import org.vudroid.core.DecodeService;
import org.vudroid.core.DecodeServiceBase;
import org.vudroid.core.codec.CodecDocument;

import cn.archko.pdf.core.common.AppExecutors;
import cn.archko.pdf.imagedroid.codec.AlbumContext;

public class AlbumViewerActivity extends BaseViewerActivity {

    protected ProgressDialog progressDialog;

    @Override
    protected DecodeService createDecodeService() {
        return new DecodeServiceBase(new AlbumContext());
    }

    public void loadDocument(String path, boolean crop) {
        boolean autoCrop = false;
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading");
        progressDialog.show();

        AppExecutors.Companion.getInstance().diskIO().execute(() -> {
            CodecDocument document = decodeService.open(path, false);
            AppExecutors.Companion.getInstance().mainThread().execute(() -> {
                progressDialog.dismiss();
                if (null == document) {
                    Toast.makeText(this, "Open Failed", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
                isDocLoaded = (true);
                documentView.showDocument(autoCrop);
                seekbarControls.update(decodeService.getPageCount(), 0);
            });
        });
    }
}
