package com.artifex.sonui.editor;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.Selection;
import android.text.Spanned;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.CorrectionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputContentInfo;
import android.view.inputmethod.InputMethodManager;

import com.artifex.solib.ArDkDoc;
import com.artifex.solib.SODoc;

public class InputView extends View implements KeyEvent.Callback
{
    //  input connection
    private BIC bic;

    //  current document and document view
    private ArDkDoc mDoc;
    private NUIDocView nuiDocView;

    //  for logging
    private static final String TAG = "InputView";
    private static final boolean enableLogging = false;

    //  constructor
    public InputView(Context context, ArDkDoc doc, NUIDocView ndv)
    {
        super(context);

        setFocusable(true);
        setFocusableInTouchMode(true);

        mDoc = doc;
        nuiDocView = ndv;

        InputMethodManager imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
        bic = new BIC(this, true, imm);
    }

    //  set focus to us
    public void setFocus() {
        requestFocus();
    }

    //  reset the editable
    public void resetEditable() {
        bic.resetEditable();
    }

    //  update the editable
    public void updateEditable() {
        // Update the editable and reset the input view.
        bic.updateEditable(true);
    }

    /*
     * onCreateInputConnection does the necessary to convince the system
     * that we are some kind of text editor control
     */
    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs)
    {
        Configuration config = this.getResources().getConfiguration();

        outAttrs.imeOptions = EditorInfo.IME_NULL;

        //  If the physical keyboard is not traditional phone format
        //  then don't show an extracted text view in landscape where
        //  the keyboard completely covers the display

        if(config.keyboard != Configuration.KEYBOARD_12KEY )
            outAttrs.imeOptions |= EditorInfo.IME_FLAG_NO_EXTRACT_UI;

        //  Tell the keyboard we accept multiline normal text
        outAttrs.inputType = InputType.TYPE_CLASS_TEXT;

        outAttrs.initialCapsMode = bic.getCursorCapsMode(outAttrs.inputType);
        outAttrs.privateImeOptions = null;
        outAttrs.initialSelStart =  Selection.getSelectionStart(bic.myEditable);
        outAttrs.initialSelEnd =  Selection.getSelectionEnd(bic.myEditable);

        outAttrs.actionLabel = null;
        outAttrs.actionId = 0;
        outAttrs.extras = null;
        outAttrs.hintText = null;

        return bic;
    }

    //  This class handles text entry events from the text entry method

    private class BIC extends BaseInputConnection
    {
        /*
         * We need to provide an editable text object for predictive text
         * to work with
         */
        public SpannableStringBuilder myEditable;

        //  Object to call back on with text string updates
        private ExtractedTextRequest etReq = null;

        private InputMethodManager imm;

        //  Reference to parent view
        private View view;

        // Bounds of the current composing region span.
        private int composingRegionStart;
        private int composingRegionEnd;

        /*
         * Track the SODoc selection start/end.
         */
        private int soSelectionStart;
        private int soSelectionEnd;

        /*
         * Track the length of the editable. Used to make a decision on
         * updating the editable due to deleting towards it's start.
         */
        private int editableLen;

        public BIC(View               targetView,
                   boolean            fullEditor,
                   InputMethodManager immIn)
        {
            super(targetView, fullEditor);

            myEditable = new SpannableStringBuilder();
            myEditable.clear();
            myEditable.clearSpans();

            Selection.setSelection(myEditable,0);

            imm  = immIn;
            view = targetView;
        }

        private void logEditableState(String primitive, String[] extra)
        {
            if (enableLogging)
            {
                // Standard data to be logged.
                Log.d(TAG, primitive + " Editable: '" +
                           myEditable.toString() + "'");

                int selStart = Selection.getSelectionStart(myEditable);
                int selEnd   = Selection.getSelectionEnd(myEditable);

                Log.d(TAG, primitive + " Editable Selection Start: [" +
                           String.valueOf(selStart) + "] End: [" +
                           String.valueOf(selEnd) + "]");

                Log.d(TAG, primitive + " Saved IME Region Start: [" +
                           String.valueOf(composingRegionStart) + "] End: [" +
                           String.valueOf(composingRegionEnd) + "]");

                // Primitive specific data to be logged.
                for (int i=0; i<extra.length; i++)
                {
                    Log.d(TAG, primitive + " " + extra[i]);
                }
            }
        }

        // Clear the editable tracking variables.
        private void clearEditableTracking()
        {
            composingRegionStart = 0;
            composingRegionEnd   = 0;
            soSelectionStart     = 0;
            soSelectionEnd       = 0;
            editableLen          = 0;
        }

        // Reset the input method and associated data.
        public void resetEditable()
        {
            if (enableLogging)
            {
                logEditableState("resetEditable", new String[] {});
            }

            nuiDocView.setIsComposing(false);

            myEditable.clear();
            myEditable.clearSpans();

            clearEditableTracking();

            imm.restartInput(view);
        }

        /*
         * Reload the editable from the SODoc.
         */
        public void updateEditable(boolean resetInputView)
        {
            if (enableLogging)
            {
                logEditableState("updateEditable", new String[] {});
            }

            if (mDoc == null)
            {
                return;
            }

            // Read the text around the selection point from the document.
            SODoc.SOSelectionContext context = ((SODoc)mDoc).getSelectionContext();

            if (context != null)
            {
                /*
                 * Clear out the existing editable tracking data and allocate
                 * a new editable.
                 */
                nuiDocView.setIsComposing(false);
                clearEditableTracking();
                myEditable.clearSpans();

                if (context.text == null)
                {
                    myEditable = new SpannableStringBuilder();
                    Selection.setSelection(myEditable, context.start);
                }
                else
                {
                    myEditable = new SpannableStringBuilder(context.text);

                    if (context.length == 0)
                    {
                        Selection.setSelection(myEditable, context.start);
                    }
                    else
                    {
                        Selection.setSelection(myEditable,
                                               context.start,
                                               context.start + context.length);
                    }
                }

                // We track the SODoc selection start/end. Note them here.
                soSelectionStart = context.start;
                soSelectionEnd   = context.start + context.length;

                imm.updateSelection (view,
                                     soSelectionStart,
                                     soSelectionEnd,
                                     0,
                                     0);
            }

            // We track the editable length. Note it here.
            editableLen = myEditable.length();

            /*
             * Tell the IME the editable has changed.
             *
             * This interrupts the IME's motion of composing text.
             */
            if (resetInputView)
            {
                imm.restartInput(view);
            }
        }

        /*
         * The following BaseInputConnection function implementations are for
         * logging purposes only.
         */

        @Override
        public CharSequence getTextBeforeCursor(int length, int flags)
        {
            CharSequence text = super.getTextBeforeCursor(length, flags);

            if (enableLogging)
            {
                logEditableState("getTextBeforeCursor", new String[]
                        { "Length: [" + length + "] " +
                          "Flags: [" + flags + "]" +
                          "Text: '" + text.toString() + "'"});
            }

            return text;
        }

        @Override
        public CharSequence getTextAfterCursor(int length, int flags)
        {
            CharSequence text = super.getTextAfterCursor(length, flags);

            if (enableLogging)
            {
                logEditableState("getTextAfterCursor", new String[]
                        { "Length: [" + length + "] " +
                          "Flags: [" + flags + "]" +
                          "Text: '" + text.toString() + "'"});
            }

            return text;
        }

        @Override
        public boolean commitCompletion(CompletionInfo text)
        {
            if (enableLogging)
            {
                logEditableState("commitCompletion", new String[]
                        { "Object: [" + text.toString() + "]" });
            }

            return super.commitCompletion(text);
        }

        @Override
        public boolean commitContent(InputContentInfo inputContentInfo,
                                     int flags,
                                     Bundle opts)
        {
            if (enableLogging)
            {
                logEditableState("commitContent", new String[] {});
            }

            return super.commitContent(inputContentInfo, flags, opts);
        }

        @Override
        public boolean commitCorrection(CorrectionInfo correctionInfo)
        {
            if (enableLogging)
            {
                logEditableState("commitCorrection", new String[]
                        { "Offset: [" + correctionInfo.getOffset() + "] " +
                          "Old: [" + correctionInfo.getOldText() + "] " +
                          "New: [" + correctionInfo.getNewText() + "]" });
            }

            return super.commitCorrection(correctionInfo);
        }

        /*
         * BaseInputConnection function implementations.
         */

        private void updateExtractedText()
        {
            final ExtractedTextRequest req = etReq;

            if (req != null)
            {

                ExtractedText outText  = new ExtractedText();
                int partialStartOffset = -1;
                int partialEndOffset   = -1;

                if (myEditable != null)
                {

                    final int N = myEditable.length();

                    outText.partialStartOffset = outText.partialEndOffset = -1;
                    partialStartOffset         = 0;
                    partialEndOffset           = N;

                    if ((req.flags& InputConnection.GET_TEXT_WITH_STYLES) != 0)
                    {
                        outText.text =
                            myEditable.subSequence(partialStartOffset,
                                                   partialEndOffset);
                    }
                    else
                    {
                        outText.text = TextUtils.substring(myEditable,
                                                           partialStartOffset,
                                                           partialEndOffset);
                    }
                    outText.flags = 0;

                    outText.startOffset   = 0;
                    outText.selectionStart =
                            Selection.getSelectionStart(myEditable);
                    outText.selectionEnd =
                            Selection.getSelectionEnd(myEditable);

                    imm.updateExtractedText(view, req.token, outText);
                }
                else
                {
                    outText.flags          = 0;
                    outText.startOffset    = 0;
                    outText.selectionStart = 0;
                    outText.selectionEnd   = 0;

                    imm.updateExtractedText(view, req.token, outText);
                }
            }
        }

        @Override
        public ExtractedText getExtractedText(ExtractedTextRequest request,
                                              int flags)
        {
            if (enableLogging)
            {
                logEditableState("getExtractedText", new String[] {});
            }

            ExtractedText outText  = new ExtractedText();
            int partialStartOffset = -1;
            int partialEndOffset   = -1;

            etReq = request;

            if (myEditable != null)
            {
                final int N = myEditable.length();

                outText.partialStartOffset = outText.partialEndOffset = -1;
                partialStartOffset = 0;
                partialEndOffset = N;

                if ((request.flags&InputConnection.GET_TEXT_WITH_STYLES) != 0)
                {
                    outText.text = myEditable.subSequence(partialStartOffset,
                                                          partialEndOffset);
                }
                else
                {
                    outText.text = TextUtils.substring(myEditable,
                                                       partialStartOffset,
                                                       partialEndOffset);
                }

                outText.flags          = 0;
                outText.startOffset    = 0;
                outText.selectionStart =
                        Selection.getSelectionStart(myEditable);
                outText.selectionEnd   =
                        Selection.getSelectionEnd(myEditable);

                return outText;
            }

            return null;
        }

        /*
         * Obtain the composing region bounds from the editable.
         *
         * There be multiple composing spans, but the bounds should
         * be identical for each.
         */
        private void getComposingRegion()
        {
            // Obtain all editable spans.
            Object[] sps =
                myEditable.getSpans(0, myEditable.length(), Object.class);

            composingRegionStart = -1;
            composingRegionEnd   = -1;

            if (sps == null)
            {
                return;
            }

            // Obtain the bounds from the first composing span.
            for (int i=sps.length-1; i>=0; i--)
            {
                Object o = sps[i];
                if ((myEditable.getSpanFlags(o)&Spanned.SPAN_COMPOSING) != 0)
                {
                    int start = myEditable.getSpanStart(o);
                    int end   = myEditable.getSpanEnd(o);

                    // Ensure the region start comes before the region end.
                    composingRegionStart = Math.max(0, Math.min(start, end));
                    composingRegionEnd   = Math.min(myEditable.length(),
                                                    Math.max(start, end));

                    break;
                }
            }
        }

        @Override
        public Editable getEditable()
        {
            return myEditable;
        }

        @Override
        public boolean setComposingRegion(int start, int end)
        {
            // Get the composing region from the editable.
            getComposingRegion();

            if (enableLogging)
            {
                logEditableState("setComposingRegion", new String[]
                        { "Start: [" + String.valueOf(start) + "] " +
                          "End: ["   + String.valueOf(end) + "]" });
            }

            /*
             * Tell the UI we are composing.
             *
             * This is primarily to prevent selection resize handles being
             * displayed for any, non caret, selection.
             */
            nuiDocView.setIsComposing(true);

            return super.setComposingRegion(start, end);
        }

        @Override
        public boolean setSelection (int start, int end)
        {
            // Ensure the start comes before the end.
            int selStart = Math.min(start, end);
            int selEnd   = Math.max(start, end);

            /*
             * Figure out how to adjust the SO selection to match
             * that requested.
             */
            int startDelta = selStart - soSelectionStart;
            int endDelta   = selEnd - soSelectionEnd;

            ((SODoc)mDoc).adjustSelection(startDelta, endDelta, 1);

            if (enableLogging)
            {
                logEditableState("setSelection", new String[]
                        { "New Selection Start: [" + selStart +
                          "] End: [" + selEnd + "]",
                          "SO Selection Start: [" + soSelectionStart +
                          "] End: [" + soSelectionEnd + "]",
                          "SO Selection Delta Start: [" + startDelta +
                          "] End: [" + endDelta + "]" });
            }

            soSelectionStart += startDelta;
            soSelectionEnd   += endDelta;

            return super.setSelection(start, end);
        }

        @Override
        public boolean setComposingText(CharSequence  text,
                                        int           newCursorPosition)
        {
            // Get the composing region from the editable.
            getComposingRegion();

            boolean  result;

            /*
             * Tell the UI we are entering text.
             *
             * This affects the scrolling of the selection into view.
             */
            nuiDocView.onTyping();

            /*
             * Tell the UI we are composing.
             *
             * This is primarily to prevent selection resize handles being
             * displayed for any, non caret, selection.
             */
            nuiDocView.setIsComposing(true);

            String currentComposition = text.toString();

            /*
             * Calculate the delta required to extend the SO selection to cover
             * the composing region or editable selection.
             */
            int startDelta;
            int endDelta;

            if (composingRegionStart == -1 && composingRegionEnd == -1)
            {
                /*
                 * If there was no composing span we insert at the
                 * selection point.
                 */
                int startTmp = Selection.getSelectionStart(myEditable);
                int endTmp   = Selection.getSelectionEnd(myEditable);

                int start = Math.max(0, Math.min(startTmp, endTmp));
                int end   = Math.min(myEditable.length(),
                                     Math.max(startTmp, endTmp));

                startDelta = start - soSelectionStart;
                endDelta   = end   - soSelectionEnd;
            }
            else
            {
                /*
                 * Move the selection start/end to match the composing
                 * region.
                 */
                startDelta = composingRegionStart - soSelectionStart;
                endDelta   = composingRegionEnd   - soSelectionEnd;
            }

            if (enableLogging)
            {
                logEditableState("setComposingText", new String[]
                        { "Text: '" + text.toString() + "'",
                          "New Cursor: [" + String.valueOf(newCursorPosition) +
                          "]",
                          "SO Selection Start: [" + soSelectionStart +
                          "] End: [" + soSelectionEnd + "]",
                          "SO Selection Delta Start: [" + startDelta +
                          "] End: [" + endDelta + "]" });
            }

            /*
             * Post the composing text to the library. The selection will
             * move to the end of the text.
             */
            ((SODoc)mDoc).adjustSelection(startDelta, endDelta, 0);
            ((SODoc)mDoc).setSelectionText(currentComposition);

            /*
             * Note the length of the written text for future
             * delta calculations.
             */
            int soSelectionLen = text.length();

            // Update the SODoc selection tracking variables.
            soSelectionStart = (soSelectionStart + startDelta) + soSelectionLen;
            soSelectionEnd   = soSelectionStart;

            result = super.setComposingText(text, newCursorPosition);

            /*
             * In the case where text is being deleted we need to update
             * (refresh) the editable as the selection point hits the
             * beginning of the editable.
             */
            if (editableLen > myEditable.length() && soSelectionStart == 0)
            {
                updateEditable(true);
                return true;
            }

            editableLen = myEditable.length();

            return result;
        }

        @Override
        public boolean commitText(CharSequence text, int newCursorPosition)
        {
            // Get the composing region from the editable.
            getComposingRegion();

            boolean result;

            /*
             * Tell the UI we are entering text.
             *
             * This affects the scrolling of the selection into view.
             */
            nuiDocView.onTyping();

            /*
             * Tell the UI we are composing.
             *
             * This is primarily to prevent selection resize handles being
             * displayed for any, non caret, selection.
             */
            nuiDocView.setIsComposing(true);

            String currentComposition = text.toString();

            if (newCursorPosition != 1)
            {
                if (enableLogging) {
                    Log.e(TAG,
                          "commitText Warning Only a New Cursor Position Of " +
                          "1 is currently supported.");
                }
            }

            /*
             * Calculate the delta required to extend the SO selection to cover
             * the composing region or editable selection.
             */
            int startDelta;
            int endDelta;

            if (composingRegionStart == -1 && composingRegionEnd == -1)
            {
                /*
                 * If there was no composing span we insert at the
                 * selection point.
                 */
                int startTmp = Selection.getSelectionStart(myEditable);
                int endTmp   = Selection.getSelectionEnd(myEditable);

                int start = Math.max(0, Math.min(startTmp, endTmp));
                int end   = Math.min(myEditable.length(),
                                     Math.max(startTmp, endTmp));

                startDelta = start - soSelectionStart;
                endDelta   = end   - soSelectionEnd;
            }
            else
            {
                /*
                 * Move the selection start/end to match the composing
                 * region.
                 */
                startDelta = composingRegionStart - soSelectionStart;
                endDelta   = composingRegionEnd   - soSelectionEnd;
            }

            if (enableLogging)
            {
                logEditableState("commitText", new String[]
                        { "Text: '" + text.toString() + "'",
                          "New Cursor: [" + String.valueOf(newCursorPosition) +
                          "]",
                          "SO Selection Start: [" + soSelectionStart +
                          "] End: [" + soSelectionEnd + "]",
                          "SO Selection Delta Start: [" + startDelta +
                          "] End: [" + endDelta + "]" });
            }

            /*
             * Post the composing text to the library. The selection will
             * move to the end of the text.
             */
            ((SODoc)mDoc).adjustSelection(startDelta, endDelta, 0);
            ((SODoc)mDoc).setSelectionText(currentComposition);

            // Update our SODoc selection tracking variables.
            soSelectionStart = (soSelectionStart + startDelta) + text.length();
            soSelectionEnd   = soSelectionStart;

            result = super.commitText(text, newCursorPosition);

            /*
             * In the case where text is being deleted we need to update
             * (refresh) the editable as the selection point hits the
             * beginning of the editable.
             */
            if (editableLen > myEditable.length() && soSelectionStart == 0)
            {
                updateEditable(true);
                return true;
            }

            editableLen = myEditable.length();

            return result;
        }

        @Override
        public boolean deleteSurroundingText(int leftLength, int rightLength)
        {
            // Get the composing region from the editable.
            getComposingRegion();

            boolean result;

            /*
             * Tell the UI we are entering text.
             *
             * This affects the scrolling of the selection into view.
             */
            nuiDocView.onTyping();

            if (enableLogging)
            {
                logEditableState("deleteSurroundingText", new String[]
                        { "LeftLength: [" + String.valueOf(leftLength) +
                          "] RightLength: [" + String.valueOf(rightLength) +
                          "]" });
            }

            int startTmp = Selection.getSelectionStart(myEditable);
            int endTmp   = Selection.getSelectionEnd(myEditable);

            int start = Math.max(0, Math.min(startTmp, endTmp));
            int end   = Math.min(myEditable.length(),
                                 Math.max(startTmp, endTmp));

            /*
             * Ensure the supplied deltas are within the bounds of the
             * editable.
             */
            if (start - leftLength < 0)
            {
                leftLength = start;
            }

            if (end + rightLength > myEditable.length())
            {
                rightLength = myEditable.length() - end;
            }

            // Delete data to the right of the selection.
            int startDelta = end - soSelectionStart;
            int endDelta   = end + rightLength - soSelectionEnd;

            // Delete the selected region.
            ((SODoc)mDoc).adjustSelection(startDelta, endDelta, 0);
            ((SODoc)mDoc).setSelectionText("");

            // Delete data to the left of the original selection.
            startDelta = start - end - leftLength;
            endDelta   = start - end;

            // Delete the selected region.
            ((SODoc)mDoc).adjustSelection(startDelta, endDelta, 0);
            ((SODoc)mDoc).setSelectionText("");

            // Not where the SO selection is now,
            soSelectionStart = soSelectionStart + startDelta;
            soSelectionEnd   = soSelectionStart;

            result = super.deleteSurroundingText(leftLength, rightLength);

            /*
             * In the case where text is being deleted we need to update
             * (refresh) the editable as the selection point hits the
             * beginning of the editable.
             */
            if (soSelectionStart == 0)
            {
                /*
                 * Defer the updating of the editable.
                 *
                 * This is to account for the case, seen with the
                 * Samsung IME, whereby accepting a suggestion is split
                 * into two actions:
                 *   - deleteSurroundingText()
                 *   - commitText()
                 *
                 * If the deletion takes us to the start of the editable
                 * we need to refresh and reset the IME. However if
                 * we do that in the above scenario the commitText()
                 * doesn't happen.
                 *
                 * So we defer it, allowing any commitText() to  sneak in.
                 */
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (soSelectionStart == 0)
                        {
                            updateEditable(true);
                        }
                    }
                });

                return true;
            }

            editableLen = myEditable.length();

            return result;
        }

        @Override
        public boolean finishComposingText()
        {
            // Get the composing region from the editable.
            getComposingRegion();

            boolean result;

            if (enableLogging)
            {
                logEditableState("finishComposingText", new String[] {});
            }

            result = super.finishComposingText();

            nuiDocView.setIsComposing(false);

            return result;
        }

        @Override
        public boolean sendKeyEvent(KeyEvent event)
        {
            int keyCode = event.getKeyCode();

            if (event.getAction() == KeyEvent.ACTION_UP)
            {
                if (enableLogging)
                {
                    logEditableState("sendKeyEvent", new String[]
                            { "Key Event: [" +
                              KeyEvent.keyCodeToString(keyCode) + "]" });
                }

                switch(keyCode)
                {
                    case KeyEvent.KEYCODE_FORWARD_DEL:
                    {
                        /*
                         * Tell the UI we are entering text.
                         *
                         * This affects the scrolling of the selection into
                         * view.
                         */
                        nuiDocView.onTyping();

                        int selStart = Selection.getSelectionStart(myEditable);
                        int selEnd   = Selection.getSelectionEnd(myEditable);

                        if (selStart == selEnd)
                        {
                            // Delete 1 character to the right of the selection.
                            deleteSurroundingText(0,1);
                        }
                        else
                        {
                            // Delete the selection region.
                            setComposingText("", 1);
                        }

                        break;
                    }

                    case KeyEvent.KEYCODE_DEL:
                    {
                        /*
                         * Tell the UI we are entering text.
                         *
                         * This affects the scrolling of the selection into
                         * view.
                         */
                        nuiDocView.onTyping();

                        int selStart = Selection.getSelectionStart(myEditable);
                        int selEnd   = Selection.getSelectionEnd(myEditable);

                        if (selStart == selEnd)
                        {
                            // Delete 1 character to the left of the selection.
                            deleteSurroundingText(1,0);
                        }
                        else
                        {
                            // Delete the selection region.
                            setComposingText("", 1);
                        }

                        break;
                    }

                    case KeyEvent.KEYCODE_DPAD_LEFT:
                    {
                        /*
                         * Tell the UI we are entering text.
                         *
                         * This affects the scrolling of the selection into
                         * view.
                         */
                        nuiDocView.onTyping();

                        /*
                         * If we have reached the first character in the
                         * editable then it's time for a refresh.
                         */
                        if (soSelectionStart == 0)
                        {
                            /*
                             * IME's will call one of the following to
                             * get the latest editable so we do not reset
                             * the IME here:
                             *   - getExtractedText()
                             *   - getTextBeforeCursor()
                             *   - getTextAfterCursor()
                             */
                            updateEditable(false);
                        }
                        else
                        {
                            // Adjust the selection to the left.
                            ((SODoc)mDoc).adjustSelection(-1, -1, 1);
                            soSelectionStart--;
                            soSelectionEnd--;

                            /*
                             * Update the editable selection and inform the IME
                             * what action we have taken.
                             */
                            Selection.setSelection(myEditable, soSelectionStart);
                            imm.updateSelection (view,
                                                 soSelectionStart,
                                                 soSelectionEnd,
                                                 0,
                                                 0);
                        }

                        break;
                    }

                    case KeyEvent.KEYCODE_DPAD_RIGHT:
                    {
                        /*
                         * Tell the UI we are entering text.
                         *
                         * This affects the scrolling of the selection into
                         * view.
                         */
                        nuiDocView.onTyping();

                        /*
                         * If we have reached the last character in the
                         * editable then it's time for a refresh.
                         */
                        if (soSelectionStart == myEditable.length())
                        {
                            /*
                             * IME's will call one of the following to
                             * get the latest editable so we do not reset
                             * the IME here:
                             *   - getExtractedText()
                             *   - getTextBeforeCursor()
                             *   - getTextAfterCursor()
                             */
                            updateEditable(false);
                        }
                        else
                        {
                            // Adjust the selection to the right.
                            ((SODoc)mDoc).adjustSelection(1, 1, 1);
                            soSelectionStart++;
                            soSelectionEnd++;

                            /*
                             * Update the editable selection and inform the IME
                             * what action we have taken.
                             */
                            Selection.setSelection(myEditable, soSelectionStart);
                            imm.updateSelection (view,
                                                 soSelectionStart,
                                                 soSelectionEnd,
                                                 0,
                                                 0);
                        }

                        break;
                    }

                    case KeyEvent.KEYCODE_ENTER:
                    {
                        /*
                         * Tell the UI we are entering text.
                         *
                         * This affects the scrolling of the selection into
                         * view.
                         */
                        nuiDocView.onTyping();

                        /*
                         * Add a newline to the editable.
                         *
                         * This is what the IME should have done instead
                         * of sending the key event.
                         */
                        finishComposingText();
                        commitText("\n" , 1);

                        // Let the IME know we have changed the editable.
                        imm.restartInput(view);

                        break;
                    }

                    default:
                    {
                        if (enableLogging)
                        {
                            Log.d(TAG, "sendKeyEvent Passing Key Event [" +
                                       KeyEvent.keyCodeToString(keyCode) +
                                       "] to NUIDocView");
                        }

                        // Pass the key event to the window key handler.
                        if (nuiDocView != null)
                        {
                            nuiDocView.doKeyDown(keyCode, event);
                        }
                    }
                }
            }

            return true;
        }

        @Override
        public void closeConnection ()
        {
            super.closeConnection();

            nuiDocView.setIsComposing(false);
        }
    }
}
