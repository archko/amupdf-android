package com.artifex.sonui.editor;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.artifex.solib.ArDkLib;
import com.artifex.solib.ConfigOptions;
import com.artifex.solib.MuPDFDoc;
import com.artifex.solib.MuPDFWidget;
import com.artifex.solib.Worker;

import java.util.ArrayList;
import java.util.List;

public class PDFFormTextEditor extends PDFFormEditor
{
    private Rect[] mTextRects = null;
    private int mSelStart = -1;
    private int mSelEnd = -1;
    private TextView.OnEditorActionListener mEditActionListener;
    private SelectionHandle mSelectionHandleUpper =null;
    private SelectionHandle mSelectionHandleLower =null;
    private boolean mDragging = false;
    private TextWatcher mWatcher = null;
    private boolean messageDisplayed = false;

    public PDFFormTextEditor(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void start(DocMuPdfPageView pageView, final int pageNumber, final MuPDFDoc doc,
                      final DocView docView, final MuPDFWidget widget, final Rect bounds,
                      final PDFFormTextEditor.EditorListener editorListener)
    {
        //  create handles
        mSelectionHandleUpper = ((Activity)getContext()).findViewById(R.id.pdf_form_text_editor_handle_upper);
        mSelectionHandleLower = ((Activity)getContext()).findViewById(R.id.pdf_form_text_editor_handle_lower);

        //  start the base class
        super.start(pageView, pageNumber, doc, docView, widget, bounds, editorListener);

        //  show the keyboard
        Utilities.showKeyboard(getContext());

        //  set a text change listener on the EditText
        mWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                setWidgetText(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };
        mEditText.addTextChangedListener(mWatcher);

        mEditActionListener = new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
            {
                if (actionId == EditorInfo.IME_ACTION_DONE)
                {
                    boolean stopped = stop();
                    if (!stopped)
                        return true;
                    mEditorListener.onStopped();
                }
                return false;
            }
        };
        mEditText.setOnEditorActionListener(mEditActionListener);

        //  set a key listener
        mEditText.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event)
            {
                if (event.getAction() == KeyEvent.ACTION_DOWN)
                {
                    boolean canStop = false;
                    if (keyCode==KeyEvent.KEYCODE_TAB)
                        canStop = true;
                    if (!mWidget.isMultiline() && keyCode==KeyEvent.KEYCODE_ENTER )
                        canStop = true;

                    if (canStop)
                    {
                        if (stop()) {
                            mEditorListener.onStopped();
                            return true;
                        }
                    }
                }
                return false;
            }
        });

        setHandleListeners();

        invalidate();
    }

    private void setHandleListeners()
    {
        mSelectionHandleUpper.setSelectionHandleListener(new SelectionHandle.SelectionHandleListener() {
            @Override
            public void onStartDrag(SelectionHandle handle) {
                mDragging = true;
                hideMenu();
            }

            @Override
            public void onDrag(SelectionHandle handle) {
                Point p = handle.getPoint();
                onDragSelection(mSelectionHandleUpper.getKind(), p);
            }

            @Override
            public void onEndDrag(SelectionHandle handle) {
                mDragging = false;
                handlesMatchSelection();
                showMenu();
            }
        });
        mSelectionHandleLower.setSelectionHandleListener(new SelectionHandle.SelectionHandleListener() {
            @Override
            public void onStartDrag(SelectionHandle handle) {
                mDragging = true;
                hideMenu();
            }

            @Override
            public void onDrag(SelectionHandle handle) {
                Point p = handle.getPoint();
                onDragSelection(mSelectionHandleLower.getKind(), p);
            }

            @Override
            public void onEndDrag(SelectionHandle handle) {
                mDragging = false;
                handlesMatchSelection();
                showMenu();
            }
        });
    };

    private void onDragSelection(int kind, Point p)
    {
        int selStart = mEditText.getSelectionStart();
        int selEnd = mEditText.getSelectionEnd();
        int selNow = selectionFromTap(p.x, p.y);

        if (selNow>=0)
        {
            if (kind==SelectionHandle.KIND_START)
            {
                if (p.y<0)
                    selNow = 0;
                if (selNow>=selEnd-1)
                    selNow=selEnd-1;
                setEditTextSelection(selNow, selEnd);
                invalidate();
            }
            else if (kind==SelectionHandle.KIND_END)
            {
                if (selNow<=selStart+1)
                    selNow = selStart+1;
                String text = mEditText.getText().toString();
                selNow = Math.min(selNow, text.length());
                setEditTextSelection(selStart, selNow);
                invalidate();
            }
        }
    }

    private void setEditTextSelection(int start, int end)
    {
        mEditText.setSelection(start, end);
        onSelectionChanged(start, end);
    }

    private void handlesMatchSelection()
    {
        onSelectionChanged(mSelStart, mSelEnd);
    }

    private void onSelectionChanged(int selStart, int selEnd)
    {
        //  grab the new selection values and invalidate.
        //  this gets the caret drawn in its new location.
        mSelStart = selStart;
        mSelEnd = selEnd;

        if (mTextRects==null || mTextRects.length<=0)
            return;

        //  not while dragging
        if (!mDragging)
        {
            if (mSelStart==mSelEnd)
            {
                mSelectionHandleUpper.hide();
                mSelectionHandleLower.hide();
            }
            else
            {
                Rect r;

                r = pageToView(mTextRects[mSelStart]);
                mSelectionHandleUpper.setPoint(r.left, r.top);

                int last = Math.min(mSelEnd-1, mTextRects.length-1);
                r = pageToView(mTextRects[last]);
                mSelectionHandleLower.setPoint(r.right, r.bottom);

                mSelectionHandleUpper.show();
                mSelectionHandleLower.show();
            }
        }

        invalidate();
    }

    private Rect pageToView(Rect r)
    {
        //  convert from page coordinates to this view

        Rect rect = new Rect(r);
        double factor = mPageView.getFactor();

        rect.offset(-mWidgetBounds.left, -mWidgetBounds.top);

        rect.left *= factor;
        rect.top *= factor;
        rect.right *= factor;
        rect.bottom *= factor;

        return rect;
    }

    private Rect viewToPage(Rect r)
    {
        //  convert from this view's coordinates to the page.

        Rect rect = new Rect(r);
        double factor = mPageView.getFactor();

        rect.left /= factor;
        rect.top /= factor;
        rect.right /= factor;
        rect.bottom /= factor;

        rect.offset(mWidgetBounds.left, mWidgetBounds.top);

        return rect;

    }

    private void setWidgetText(String text)
    {
        boolean accepted = mWidget.setValue(text + ' ');
        mDoc.update(mPageNumber);
        mWaitingForRender = true;
    }

    @Override
    protected void scrollCaretIntoView()
    {
        if (mSelStart == mSelEnd)
        {
            //  get the current caret rect
            Rect rect = getCaretRect();

            //  transform to page coordinates
            rect = viewToPage(rect);

            //  scroll it.
            //  use a margin to avoid inaccuracies of a few pixels that may result
            //  from coordinate transformations.
            RectF box = new RectF(rect);
            mDocView.scrollBoxIntoView(mPageNumber, box, true, Utilities.convertDpToPixel(50));
        }
    }

    @Override
    protected void scrollIntoView()
    {
        if (mSelStart == mSelEnd)
            scrollCaretIntoView();
        else
        {
            int last = Math.min(mSelEnd-1, mTextRects.length-1);
            Rect rect = new Rect(mTextRects[mSelStart].left, mTextRects[mSelStart].top, mTextRects[last].right, mTextRects[last].bottom);
            RectF box = new RectF(rect);
            mDocView.scrollBoxIntoView(mPageNumber, box, true, Utilities.convertDpToPixel(50));
        }
    }

    @Override
    protected void setupInput()
    {
        int inputType;
        switch (mWidget.getTextFormat())
        {
            case MuPDFWidget.CONTENT_UNRESTRAINED:
            case MuPDFWidget.CONTENT_SPECIAL:
            default:
                inputType = InputType.TYPE_CLASS_TEXT;
                break;

            case MuPDFWidget.CONTENT_NUMBER:
                inputType = InputType.TYPE_CLASS_NUMBER;
                break;

            case MuPDFWidget.CONTENT_DATE:
                inputType = InputType.TYPE_DATETIME_VARIATION_DATE;
                break;

            case MuPDFWidget.CONTENT_TIME:
                inputType = InputType.TYPE_DATETIME_VARIATION_TIME;
                break;
        }
        inputType |= InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
        mEditText.setInputType(inputType);

        //  set whether multiline or not
        boolean isMultiline = mWidget.isMultiline();
        mEditText.setSingleLine(!isMultiline);

        int imeOpt = EditorInfo.IME_ACTION_NONE;
        if (!isMultiline)
            imeOpt = EditorInfo.IME_ACTION_DONE;

        imeOpt |= EditorInfo.IME_FLAG_NO_EXTRACT_UI;
        imeOpt |= EditorInfo.IME_FLAG_NO_FULLSCREEN;

        mEditText.setImeOptions(imeOpt);

        //  enforce maximum character count
        int maxChars = mWidget.getMaxChars();
        InputFilter[] filters;
        if (maxChars > 0) {
            filters = new InputFilter[]{new InputFilter.LengthFilter(maxChars)};
        } else {
            filters = new InputFilter[]{};
        }
        mEditText.setFilters(filters);
    }

    private boolean mSetInitialSelection = false;

    @Override
    protected void setInitialValue()
    {
        //  start in the editing state
        mWidget.setEditingState(true);

        //  get the widget's current value and set it in the edit field
        String str = mWidget.getValue().trim();
        mEditText.setText(str);
        setWidgetText(str);
        mWaitingForRender = true;
        mSetInitialSelection = true;
    }

    @Override
    protected void doubleTap(float x, float y) {
        //  coordinates are relative to the EditText

        if (mTextRects != null && mTextRects.length > 0) {
            for (int i = 0; i < mTextRects.length; i++) {
                Rect rect = new Rect(mTextRects[i].left, mTextRects[i].top, mTextRects[i].right, mTextRects[i].bottom);
                rect = pageToView(rect);

                if (rect.contains((int) x, (int) y)) {
                    String allText = mEditText.getText().toString();
                    if (allText.length()>0) {

                        //  look for a word
                        int selStart = Math.min(i, allText.length()-1);
                        while (selStart > 0 && allText.charAt(selStart) != ' ' && allText.charAt(selStart) != '\n')
                            selStart--;
                        if (allText.charAt(selStart) == ' ' || allText.charAt(selStart) == '\n')
                            selStart++;

                        int selEnd = Math.min(i, allText.length()-1);
                        while (selEnd < allText.length() - 1 && allText.charAt(selEnd) != ' ' && allText.charAt(selEnd) != '\n')
                            selEnd++;
                        if (allText.charAt(selEnd) == ' ' || allText.charAt(selEnd) == '\n')
                            selEnd--;

                        setEditTextSelection(selStart, selEnd + 1);
                        invalidate();
                        showMenu();
                        return;
                    }
                }
            }

            //  no word selected, so treat like a single-tap
            singleTap(x, y);
        }
        Utilities.showKeyboard(getContext());

    }

    private int selectionFromTap(float x, float y)
    {
        //  coordinates are relative to the EditText

        if (mTextRects != null && mTextRects.length > 0) {
            for (int i = 0; i < mTextRects.length; i++) {
                Rect rect = new Rect(mTextRects[i].left, mTextRects[i].top, mTextRects[i].right, mTextRects[i].bottom);
                rect = pageToView(rect);

                if (rect.contains((int) x, (int) y)) {
                    return i;
                }
            }

            //  no word selected, so find the row
            int lastInRow = -1;
            if (mTextRects != null && mTextRects.length > 0) {
                for (int i = 0; i < mTextRects.length; i++) {
                    Rect rect = new Rect(mTextRects[i].left, mTextRects[i].top, mTextRects[i].right, mTextRects[i].bottom);
                    rect = pageToView(rect);

                    if (y >= rect.top && y <= rect.bottom) {
                        lastInRow = i;
                    }
                }
            }
            if (lastInRow != -1) {
                return lastInRow;
            }

            //  off the end I guess
            return mTextRects.length - 1;
        }

        return -1;
    }

    @Override
    protected void singleTap(float x, float y)
    {
        int i = selectionFromTap(x, y);
        if (i >= 0 && i<=mEditText.getText().length())
        {
            setEditTextSelection(i, i);
            showMenu();
            invalidate();
        }
        Utilities.showKeyboard(getContext());
    }

    //  when we change the form field text, we set this to true.
    //  when the render resulting from the text change is done,
    //  it's set back to false.
    //  While it's true (presumably a short time), we don't draw the caret
    //  This could be improved.

    protected boolean mWaitingForRender = false;

    @Override
    public void onRenderComplete()
    {
        super.onRenderComplete();

        if (!mWaitingForRender)
            return;

        getTextCharRects(new Runnable() {
            @Override
            public void run() {

                if (mSetInitialSelection)
                {
                    //  set the initial selection
                    int textLen = mEditText.getText().length();
                    setEditTextSelection(0, textLen);
                    mSetInitialSelection = false;
                }
                else
                {
                    //  set normal selection
                    onSelectionChanged(mEditText.getSelectionStart(), mEditText.getSelectionEnd());
                }

                //  keep the caret in view
                if (mWaitingForRender)
                    scrollIntoView();
                mWaitingForRender = false;
                invalidate();
            }
        });
    }

    private void getTextCharRects(final Runnable runnable)
    {
        //  get an array of Rects describing where the text characters are.

        // There will be one more character rectangle then implied by the text
        // because, while editing, we add a space character to the end of what
        // we supply to mupdf. No rectangle is returned for return characters,
        // so we use the one for the character after the return (from iOS)

        mDoc.getWorker().add(new Worker.Task()
        {
            Rect[] rects;

            public void work()
            {
                //  this should be done on the background thread.
                rects = mWidget.textRects();
            }

            public void run()
            {
                String text = mEditText.getText().toString();
                String newText = "";

                List<Rect> newRects = new ArrayList<>();
                int rectIndex = 0;
                for (char ch : text.toCharArray())
                {
                    if (rectIndex >= rects.length)
                        break;

                    if (ch == '\n' || ch == '\r')
                    {
                        Rect r;
                        if (rectIndex == 0)
                        {
                            r = new Rect(rects[rectIndex].left, rects[rectIndex].top, rects[rectIndex].left, rects[rectIndex].bottom);
                        }
                        else
                        {
                            r = new Rect(rects[rectIndex-1].right, rects[rectIndex-1].top, rects[rectIndex-1].right, rects[rectIndex-1].bottom);
                        }
                        newRects.add(r);
                        newText += ch;
                    }
                    else
                    {
                        if (rectIndex < rects.length && useRect(rects[rectIndex]))
                        {
                            newRects.add(rects[rectIndex]);
                            newText += ch;
                        }
                        rectIndex++;
                    }
                }

                //  process leftovers
                while (rectIndex < rects.length) {
                    newRects.add(rects[rectIndex]);
                    rectIndex++;
                }

                mTextRects = newRects.toArray(new Rect[newRects.size()]);

                //  let our caller know we're done
                if (runnable!=null)
                    runnable.run();

                if (!newText.equals(text))
                {
                    int selStart = mEditText.getSelectionStart();
                    int selEnd = mEditText.getSelectionEnd();
                    mEditText.setText(newText);
                    if (selStart==selEnd)
                    {
                        if (selStart>newText.length())
                            selStart = newText.length();
                        setEditTextSelection(selStart, selStart);
                    }
                }
            }
        });
    }

    private boolean useRect(Rect target)
    {
        //  get the current widget rect
        Rect wr = new Rect(mWidgetBounds);

        //  get the target rect being considered
        Rect tr = new Rect(target);

        //  do they overlap?
        //  use a copy of 'wr' because intersect() will change it
        if (!new Rect(wr).intersect(tr))
            return false;

        //  must include the entire width
        if (tr.left<wr.left || tr.right>wr.right)
            return false;

        return true;
    }

    private void drawCharRects(Canvas canvas)
    {
        Paint paint = new Paint();
        paint.setColor(Color.BLUE);     //set a color
        paint.setStrokeWidth(2);       // set your stroke width
        paint.setStyle(Paint.Style.STROKE);

        if (mTextRects != null && mTextRects.length > 0) {
            for (int i = 0; i < mTextRects.length; i++) {
                Rect rect = new Rect(mTextRects[i].left, mTextRects[i].top, mTextRects[i].right, mTextRects[i].bottom);
                rect = pageToView(rect);
                canvas.drawRect(rect, paint);
            }
        }
    }

    private Rect getCaretRect()
    {
        int cursorWidth = Utilities.convertDpToPixel(4);

        Rect rect = new Rect();
        Rect r;
        if (mTextRects==null || mTextRects.length == 0) {
            //  no text
            rect.set(2, 0, 2 + cursorWidth, getHeight());
        }
        else if (mSelStart<mTextRects.length)
        {
            r = mTextRects[mSelStart];
            rect.set(r.left, r.top, r.left, r.bottom);

            rect = pageToView(rect);
            rect.right = rect.left + cursorWidth;
        }
        else
        {
            r = mTextRects[mTextRects.length-1];
            rect.set(r.left, r.top, r.left, r.bottom);

            rect = pageToView(rect);
            rect.right = rect.left + cursorWidth;
        }

        return rect;
    }

    private void drawSelection(Canvas canvas)
    {
        if (mSelStart == mSelEnd)
        {
            //  this is a caret selection

            Paint paint = new Paint();
            paint.setColor(Color.RED);
            int cursorWidth = Utilities.convertDpToPixel(4);

            //  get the caret
            Rect rect = getCaretRect();

            //  clip the caret to our bounds
            Rect drawR = new Rect(rect);
            Rect thisRect = new Rect(0, 0, getWidth(), getHeight());
            if (thisRect.intersect(rect))
                canvas.drawRect(drawR, paint);

        }
        else
        {
            //  a range

            Paint paint = new Paint();
            paint.setColor(Color.DKGRAY);
            paint.setAlpha(50);

            if (mTextRects!=null && mTextRects.length>0)
            {
                int last = Math.min(mSelEnd, mTextRects.length);
                for (int i = mSelStart; i < last; i++)
                {
                    Rect rect = new Rect(mTextRects[i].left, mTextRects[i].top, mTextRects[i].right, mTextRects[i].bottom);
                    rect = pageToView(rect);
                    canvas.drawRect(rect, paint);
                }
            }
        }
    }

    @Override
    public void onDraw(Canvas canvas)
    {
        if (mStopped)
            return;

        super.onDraw(canvas);

        if (mDocViewAtRest && !mWaitingForRender)
        {
            //  we only draw the selection when the view is not moving.
            //  Otherwise it seems to swim around.
            drawSelection(canvas);
        }
    }

    @Override
    protected SOEditText getEditText() {
        return findViewById(R.id.pdf_text_editor);
    }

    private boolean setFinalValue()
    {
        //  un-set editing state
        mWidget.setEditingState(false);

        //  reassert the value, without the trailing ' '
        //  set a listener for messages that mupdf's Javascript might emit.
        MuPDFDoc.JsEventListener listener = new MuPDFDoc.JsEventListener() {
            @Override
            public void onAlert(String message) {
                //  display a message if it's not yet been done
                if (!messageDisplayed) {
                    messageDisplayed = true;
                    Utilities.showMessageAndWait((Activity) getContext(), "", message, new Runnable() {
                        @Override
                        public void run() {
                            //  we can display another message now.
                            messageDisplayed = false;
                            Utilities.showKeyboard(getContext());
                        }
                    });
                }
                mEditText.requestFocus();
            }
        };
        mDoc.setJsEventListener(listener);

        String str = mEditText.getText().toString();
        boolean accepted = mWidget.setValue(str);
        if (!accepted)
            mWidget.setEditingState(true);
        else {
            mDoc.update(mPageNumber);
        }
        return accepted;
    }

    @Override
    public boolean stop()
    {
        if (mStopped)
            return true;

        //  fix 700656 (we should do this anyway)
        hideMenu();

        boolean ok = setFinalValue();
        if (!ok)
            return false;

        super.stop();

        if (mSelectionHandleUpper != null)
            mSelectionHandleUpper.hide();
        if (mSelectionHandleLower != null)
            mSelectionHandleLower.hide();

        if (mEditText != null)
        {
            if (mWatcher!=null)
                mEditText.removeTextChangedListener(mWatcher);
            mEditText.setOnEditorActionListener(null);
            mEditText.setOnKeyListener(null);
        }

        return true;
    }

    @Override
    protected void show()
    {
        mEditText.requestFocus();

        super.show();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event)
    {
        //  first let's check to see if there's an annotation in the parent page view
        //  could be selected with this tap
        if (event.getAction()==MotionEvent.ACTION_DOWN)
        {
            int x = (int)event.getRawX();
            int y = (int)event.getRawY();
            if (mPageView.selectAnnotation(x, y))
            {
                //  it was selected, so stop editing the widget
                mPageView.stopPreviousEditor();
                return true;
            }
        }

        //  give our handles a crack.
        if (mSelectionHandleUpper.dispatchTouchEvent(event))
            return true;
        if (mSelectionHandleLower.dispatchTouchEvent(event))
            return true;

        //  default handling
        return super.dispatchTouchEvent(event);
    }

    @Override
    protected void onGlobalLayout()
    {
        super.onGlobalLayout();

        mSelectionHandleUpper.setMayDraw(mDocViewAtRest);
        mSelectionHandleLower.setMayDraw(mDocViewAtRest);

        handlesMatchSelection();
    }

    private String getSelectedText()
    {
        int selStart = mEditText.getSelectionStart();
        int selEnd = mEditText.getSelectionEnd();
        String selText = mEditText.getText().subSequence(selStart, selEnd).toString();
        return selText;
    }

    private void deleteSelectedText()
    {
        int selStart = mEditText.getSelectionStart();
        int selEnd = mEditText.getSelectionEnd();
        if (selStart!=selEnd)
        {
            String text = mEditText.getText().toString();
            String s1 = text.substring(0, selStart);
            String s2 = text.substring(selEnd);
            mEditText.setText(s1+s2);
            setEditTextSelection(selStart, selStart);
        }
    }

    private PopupWindow popupWindow;

    public void showMenu()
    {
        //  only one menu at a time, please.
        //  but just remove the one that's there. This may be related to
        //  700657 - Crash by double-tapping
        if (popupWindow !=null)
            hideMenu();

        //  if copy/paste is not allowed at all, do nothing.
        final ConfigOptions docCfgOpts = mDocView.getDocConfigOptions();
        if (docCfgOpts != null)
        {
            if (!docCfgOpts.isExtClipboardInEnabled() && !docCfgOpts.isExtClipboardOutEnabled())
                return;
        }

        LayoutInflater layoutInflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        final View popupView = layoutInflater.inflate(R.layout.sodk_editor_form_edittext_popup, null);
        popupWindow = new PopupWindow(
                popupView,
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT, true);

        popupWindow.setOutsideTouchable(true);
        popupWindow.setFocusable(false);

        //  allow for the height of the lower handle, if it's visible
        int extra = 0;
        if (mSelectionHandleLower.getVisibility()==View.VISIBLE)
            extra = mSelectionHandleLower.getHeight();
        popupWindow.showAsDropDown(this, 0, extra);

        popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                popupWindow = null;
            }
        });

        View selAllView = popupView.findViewById(R.id.select_all);
        View cutView = popupView.findViewById(R.id.cut);
        View copyView = popupView.findViewById(R.id.copy);
        View pasteView = popupView.findViewById(R.id.paste);

        int selStart = mEditText.getSelectionStart();
        int selEnd = mEditText.getSelectionEnd();
        int textLen = mEditText.getText().toString().length();

        //  hide unusable items
        if (selStart==selEnd) {
            cutView.setVisibility(View.GONE);
            copyView.setVisibility(View.GONE);
        }
        if (selStart==0 && selEnd==textLen) {
            selAllView.setVisibility(View.GONE);
        }
        if (docCfgOpts != null)
        {
            if (!docCfgOpts.isExtClipboardInEnabled())
                pasteView.setVisibility(View.GONE);
            if (!docCfgOpts.isExtClipboardOutEnabled())
                copyView.setVisibility(View.GONE);
        }

        selAllView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                hideMenu();
                mEditText.selectAll();
                setEditTextSelection(mEditText.getSelectionStart(), mEditText.getSelectionEnd());
                invalidate();
                showMenu();
            }
        });
        cutView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                hideMenu();
                String selText = getSelectedText();
                if (docCfgOpts.isExtClipboardOutEnabled())
                    ArDkLib.putTextToClipboard(selText);
                deleteSelectedText();
            }
        });

        copyView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                hideMenu();
                if (docCfgOpts.isExtClipboardOutEnabled())
                {
                    String selText = getSelectedText();
                    ArDkLib.putTextToClipboard(selText);
                }
            }
        });

        pasteView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                hideMenu();
                deleteSelectedText();
                String clip = "";
                if (docCfgOpts.isExtClipboardInEnabled())
                    clip = ArDkLib.getClipboardText();
                int selStart = mEditText.getSelectionStart();
                mEditText.getText().insert(selStart, clip);
            }
        });
    }

    private void hideMenu()
    {
        if (popupWindow!=null)
            popupWindow.dismiss();
        popupWindow = null;
    }

}
