package com.artifex.sonui.editor;

public interface DragHandleListener {
    void onStartDrag(DragHandle handle);
    void onDrag(DragHandle handle);
    void onEndDrag(DragHandle handle);
}
