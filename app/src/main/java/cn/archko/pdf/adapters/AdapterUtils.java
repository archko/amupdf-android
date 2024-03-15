package cn.archko.pdf.adapters;

import android.widget.ImageView;
import android.widget.ProgressBar;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import cn.archko.mupdf.R;

/**
 * @author: archko 2024/3/12 :13:39
 */
public class AdapterUtils {
    public static final List<String> exts = new ArrayList<>();

    static {
        exts.add("pdf");
        exts.add("xps");
        exts.add("cbz");
        exts.add("png");
        exts.add("jpe");
        exts.add("jpeg");
        exts.add("jpg");
        exts.add("jfif");
        exts.add("jfif-tbnl");
        exts.add("tif");
        exts.add("tiff");
        exts.add("bmp");
        exts.add("epub");
        exts.add("mobi");
        exts.add("txt");
        exts.add("log");
        exts.add("ppt");
        exts.add("pptx");
        exts.add("doc");
        exts.add("docx");
        exts.add("xls");
        exts.add("xlsx");
        exts.add("json");
        exts.add("js");
    }

    public static final String getExtensionWithDot(final String name) {
        if (name == null) {
            return "";
        }
        final int index = name.lastIndexOf(".");
        if (index == -1) {
            return "";
        }
        return name.substring(index);
    }

    public static final String getExtension(final String name) {
        if (name == null) {
            return "";
        }
        final int index = name.lastIndexOf(".");
        if (index == -1) {
            return "";
        }
        return name.substring(index + 1);
    }

    public static void setIcon(String ext, ImageView imageView) {
        int drawableId = R.drawable.browser_icon_any;
        if ("pdf".equalsIgnoreCase(ext)) {
            drawableId = R.drawable.browser_icon_pdf;
        } else if ("epub".equalsIgnoreCase(ext) || "mobi".equalsIgnoreCase(ext)) {
            drawableId = R.drawable.browser_icon_epub;
        } else if ("png".equalsIgnoreCase(ext) || "jpg".equalsIgnoreCase(ext) || "jpeg".equalsIgnoreCase(ext)) {
            drawableId = R.drawable.browser_icon_image;
        } else if ("txt".equalsIgnoreCase(ext) || "log".equalsIgnoreCase(ext)
                || "js".equalsIgnoreCase(ext) || "json".equalsIgnoreCase(ext)
                || "html".equalsIgnoreCase(ext) || "xhtml".equalsIgnoreCase(ext)) {
            drawableId = R.drawable.browser_icon_txt;
        } /*else if ("ppt".equalsIgnoreCase(ext)) {
            drawableId = R.drawable.browser_icon_ppt;
        } else if ("pptx".equalsIgnoreCase(ext)) {
            drawableId = R.drawable.browser_icon_pptx;
        } else if ("doc".equalsIgnoreCase(ext)) {
            drawableId = R.drawable.browser_icon_doc;
        } else if ("docx".equalsIgnoreCase(ext)) {
            drawableId = R.drawable.browser_icon_docx;
        } else if ("xls".equalsIgnoreCase(ext)) {
            drawableId = R.drawable.browser_icon_xls;
        } else if ("xlsx".equalsIgnoreCase(ext)) {
            drawableId = R.drawable.browser_icon_xlsx;
        } */

        imageView.setImageResource(drawableId);
    }

    public static void setProgress(ProgressBar progressbar, int progress, int count) {
        progressbar.setMax(progress);
        progressbar.setProgress(count);
    }

    public static boolean isSupportExt(@NotNull String fname) {
        return exts.contains(getExtension(fname));
    }

    public static boolean isPlainTxt(String name) {
        String fname = name.toLowerCase();
        return fname.endsWith("txt") || fname.endsWith("log") || fname.endsWith("json") || fname.endsWith("js") || fname.endsWith("html") || fname.endsWith("xhtml");
    }
}
