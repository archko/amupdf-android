package org.vudroid.core.codec;

import java.util.List;

/**
 * @author: archko 2024/12/16 :14:45
 */
public class SearchResult {
    public int page;
    public List<PageTextBox> boxes;
    public String text;

    public SearchResult(int i, List<PageTextBox> boxes, String text) {
        page = i;
        this.boxes = boxes;
        this.text = text;
    }

    /*public String getResultText() {
        StringBuilder sb = new StringBuilder();
        if (boxes != null && !boxes.isEmpty()) {
            for (PageTextBox pageTextBox : boxes) {
                if (!TextUtils.isEmpty(pageTextBox.text)) {
                    sb.append(pageTextBox.text).append(" ");
                }
            }
        }
        return sb.toString();
    }*/
}
