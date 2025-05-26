package cn.archko.pdf.controller;

/**
 * @author archko
 */
public interface IPageController {

    void update(int count, int page);

    void updatePageProgress(int index);

    void updateTitle(String path);

    void setReflowButton(int reflow);

    void gotoPage(int page);

    void toggleControls();

    void show();

    void hide();

    int visibility();

    int topVisibility();

    int bottomVisibility();

    void showSearch();
}
