package cn.archko.pdf.controller;

/**
 * @author: archko 2024/12/11 :11:34
 */
public interface PageControlerListener {

    void changeOrientation(int ori);

    void back();

    void showOutline();

    void gotoPage(int page);

    void toggleReflow();

    void toggleReflowImage();

    void toggleCrop();

    void toggleTts();

    void ocr();
}