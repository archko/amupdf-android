package cn.archko.pdf.imagedroid;

import org.vudroid.core.BaseViewerActivity;
import org.vudroid.core.DecodeService;
import org.vudroid.core.DecodeServiceBase;

import cn.archko.pdf.imagedroid.codec.AlbumContext;

public class AlbumViewerActivity extends BaseViewerActivity {
    @Override
    protected DecodeService createDecodeService() {
        return new DecodeServiceBase(new AlbumContext());
    }
}
