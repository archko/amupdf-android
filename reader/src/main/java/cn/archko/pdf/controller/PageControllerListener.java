package cn.archko.pdf.controller;

/**
 * @author: archko 2024/12/11 :11:34
 */
public interface PageControllerListener {

    void changeOrientation(int ori);

    void back();

    void showOutline();

    void gotoPage(int page);

    void toggleReflow();

    void toggleReflowImage();

    void toggleCrop();

    void toggleTts();

    void setSelection(boolean selection);

    void setDraw(boolean draw);

    void ocr();

    void prev(String string);

    void next(String string);

    void clearSearch();

    void showSearch();

    void ai();

    void bookmark();

    void preview();

    void selectFont();
}