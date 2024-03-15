package cn.archko.pdf.listeners;

import android.view.View;

/**
 * @author: archko 2024/2/14 :15:26
 */
public interface ClickListener<T> {

    void click(T t, int pos);
    
    void longClick(T t, int pos, View view);
}
