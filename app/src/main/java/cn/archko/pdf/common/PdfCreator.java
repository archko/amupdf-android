package cn.archko.pdf.common;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.pdf.PdfDocument;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import cn.archko.mupdf.R;
import cn.archko.pdf.utils.StreamUtils;
import cn.archko.pdf.utils.Utils;

public class PdfCreator {

    /**
     * Page width for our PDF.
     */
    public static final double PDF_PAGE_WIDTH = 8.3 * 72 * 2;
    /**
     * Page height for our PDF.
     */
    public static final double PDF_PAGE_HEIGHT = 11.7 * 72 * 2;

    public static void create(Context context, ViewGroup parent, String sourcePath, String destPath) {
        String content = StreamUtils.readStringFromFile(sourcePath);
        try {
            createPdf(context, parent, content, destPath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param context
     * @param parent  需要有一个临时的布局
     * @param path    保存的文件名
     * @throws FileNotFoundException
     */
    public static void createPdf(Context context, ViewGroup parent, String content, String path) throws FileNotFoundException {
        PdfDocument pdfDocument = new PdfDocument();

        int pageWidth = (int) PDF_PAGE_WIDTH;
        int pageHeight = (int) PDF_PAGE_HEIGHT;

        TextView contentView = (TextView) LayoutInflater.from(context).inflate(R.layout.pdf_content, parent, false);
        contentView.setText("q我");
        //contentView.setPadding(Utils.dipToPixel(20), Utils.dipToPixel(20), Utils.dipToPixel(20), Utils.dipToPixel(20));
        //contentView.setTextSize(Utils.dipToPixel(12));

        int lineCount = contentView.getLineCount();
        int lineHeight = contentView.getLineHeight();
        if (contentView.getLineSpacingMultiplier() > 0) {
            lineHeight = (int) (lineHeight * contentView.getLineSpacingMultiplier());
        }
        Layout layout = contentView.getLayout();
        int start = 0;
        int end;
        int pageH = 0;
        int paddingTopAndBottom = Utils.dipToPixel(context, 40);
        //循环遍历打印每一行
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (; i < lineCount; i++) {
            end = layout.getLineEnd(i);

            String line = content.substring(start, end); //指定行的内容
            start = end;

            Log.d("TextView", "line===" + line);
            sb.append(line);
            pageH += lineHeight;
            if (pageH >= (pageHeight - paddingTopAndBottom)) {
                Log.d("TextView", "============page==========" + i);
                createPage(context, parent, pdfDocument, pageWidth, (pageHeight - paddingTopAndBottom), i + 1, sb.toString());
                pageH = 0;
                sb.setLength(0);
            }
        }
        if (sb.length() > 0) {
            createPage(context, parent, pdfDocument, pageWidth, (pageHeight - paddingTopAndBottom), i, sb.toString());
        }

        savePdf(path, pdfDocument);
    }

    public static void createPage(Context context, ViewGroup parent, PdfDocument pdfDocument, int pageWidth, int pageHeight, int pageNo, String content) {
        TextView contentView = (TextView) LayoutInflater.from(context).inflate(R.layout.pdf_content, parent, false);
        contentView.setText(content);

        PdfDocument.PageInfo pageInfo = new PdfDocument
                .PageInfo.Builder(pageWidth, pageHeight, pageNo)
                .create();

        PdfDocument.Page page = pdfDocument.startPage(pageInfo);

        Canvas pageCanvas = page.getCanvas();
        int measureWidth = View.MeasureSpec.makeMeasureSpec(pageWidth, View.MeasureSpec.EXACTLY);
        int measuredHeight = View.MeasureSpec.makeMeasureSpec(pageHeight, View.MeasureSpec.EXACTLY);

        contentView.measure(measureWidth, measuredHeight);
        contentView.layout(0, 0, pageWidth, pageHeight);
        contentView.draw(pageCanvas);

        // finish the page
        pdfDocument.finishPage(page);
    }

    public static void savePdf(String path, PdfDocument document) throws FileNotFoundException {
        FileOutputStream outputStream = new FileOutputStream(path);
        try {
            document.writeTo(outputStream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            document.close();
        }
    }

}
