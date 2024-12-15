package cn.archko.pdf.imagedroid.codec;

import com.artifex.mupdf.fitz.Quad;

import org.vudroid.core.codec.CodecDocument;
import org.vudroid.core.codec.CodecPage;
import org.vudroid.core.codec.OutlineLink;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import cn.archko.pdf.core.common.IntentFile;
import cn.archko.pdf.core.entity.ReflowBean;

public class AlbumDocument implements CodecDocument {

    ArrayList<File> files;
    int count = 0;

    private static FileFilter createFileFilter() {
        return file -> {
            if (file.length() > 10_000_000) {
                return false;
            }
            if (file.isHidden()) {
                return false;
            }

            if (file.isDirectory())
                return false;
            String fname = file.getName().toLowerCase(Locale.ROOT);

            return IntentFile.INSTANCE.isImage(fname);
        };
    }

    public static AlbumDocument openDocument(String fname) {
        ArrayList<File> files;
        File file = new File(fname);
        if (file.isDirectory()) {
            File[] fileArray = file.listFiles(createFileFilter());
            files = new ArrayList<>(fileArray != null ? Arrays.asList(fileArray) : Collections.<File>emptyList());
            Collections.sort(files, (o1, o2) -> {
                if (o1 == null && o2 == null) {
                    return 0;
                }
                if (o1.isDirectory() && o2.isFile()) return -1;
                if (o1.isFile() && o2.isDirectory()) return 1;
                if (o1.lastModified() - o2.lastModified() > 0) {
                    return -1;
                } else if (o1.lastModified() - o2.lastModified() < 0) { //jdk7以上需要对称,自反,传递性.
                    return 1;
                } else {
                    return 0;
                }
            });
        } else {
            files = new ArrayList<>();
            files.add(file);
        }
        AlbumDocument document = new AlbumDocument(files);
        return document;
    }

    public AlbumDocument(ArrayList<File> files) {
        this.files = files;
        if (null != files) {
            count = files.size();
        }
    }

    public CodecPage getPage(int pageNumber) {
        return AlbumPage.createPage(files.get(pageNumber).getAbsolutePath(), pageNumber);
    }

    public int getPageCount() {
        return count;
    }

    @Override
    protected void finalize() throws Throwable {
        recycle();
        super.finalize();
    }

    public synchronized void recycle() {
    }

    @Override
    public List<OutlineLink> loadOutline() {
        return new ArrayList<>();
    }

    @Override
    public List<ReflowBean> decodeReflowText(int index) {
        return Collections.emptyList();
    }

    @Override
    public Quad[][] search(String text, int pageNum) {
        return new Quad[0][];
    }
}
