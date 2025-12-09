package cn.archko.pdf.imagedroid;

import android.app.ProgressDialog;
import android.text.TextUtils;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.vudroid.core.BaseViewerActivity;
import org.vudroid.core.DecodeService;
import org.vudroid.core.DecodeServiceBase;
import org.vudroid.core.codec.CodecDocument;

import cn.archko.pdf.core.common.AppExecutors;
import cn.archko.pdf.core.entity.APage;
import cn.archko.pdf.imagedroid.codec.AlbumContext;
import cn.archko.pdf.imagedroid.codec.AlbumDocument;

public class AlbumViewerActivity extends BaseViewerActivity {

    protected ProgressDialog progressDialog;

    @Override
    protected DecodeService createDecodeService() {
        return new DecodeServiceBase(new AlbumContext());
    }

    public void loadDocument(String path, boolean crop) {
        boolean autoCrop = false;
        if (TextUtils.isEmpty(path)) {
            String dir = getIntent().getStringExtra("dir");
            if (TextUtils.isEmpty(dir)) {

            } else {
                AppExecutors.Companion.getInstance().diskIO().execute(() -> {
                    CodecDocument document = AlbumDocument.openDocument(dir);
                    AppExecutors.Companion.getInstance().mainThread().execute(() -> {
                        if (null == document) {
                            Toast.makeText(this, "Open Failed", Toast.LENGTH_LONG).show();
                            finish();
                            return;
                        }
                        if (document.getPageCount() == 0) {
                            Toast.makeText(this, "no images", Toast.LENGTH_LONG).show();
                            finish();
                            return;
                        }

                        ((DecodeServiceBase) decodeService).set(dir, false, document);
                        isDocLoaded = true;
                        documentView.showDocument(autoCrop);
                        seekbarControls.update(decodeService.getPageCount(), 0);
                    });
                });
            }

            return;
        }
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading");
        progressDialog.show();

        AppExecutors.Companion.getInstance().diskIO().execute(() -> {
            CodecDocument document = decodeService.open(path, false, false);
            AppExecutors.Companion.getInstance().mainThread().execute(() -> {
                progressDialog.dismiss();
                if (null == document) {
                    Toast.makeText(this, "Open Failed", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
                AlbumDocument albumDocument = (AlbumDocument) document;
                if (albumDocument.getPageCount() == 0) {
                    Toast.makeText(this, "no images", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
                isDocLoaded = true;

                APage firstPageSize = decodeService.getPageSizeBean().getList().get(0);
                float width = firstPageSize.getWidth();
                float height = firstPageSize.getHeight();
                System.out.println("old.scrollOrientation:$scrollOrientation, width:$width-$height");
                // 如果图片的高度小于宽度的1/3，则切换为横向滚动
                if (height < width / 3) {
                    seekbarControls.setOrientation(LinearLayout.HORIZONTAL);
                    documentView.setOriention(LinearLayout.HORIZONTAL);
                }

                documentView.showDocument(autoCrop);
                seekbarControls.update(decodeService.getPageCount(), 0);
            });
        });
    }
}
