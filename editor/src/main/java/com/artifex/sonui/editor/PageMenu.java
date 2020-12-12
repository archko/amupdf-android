package com.artifex.sonui.editor;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupWindow;

public class PageMenu implements PopupWindow.OnDismissListener
{
    //  these are set in the constructor.
    private final Context mContext;
    private final View mAnchor;
    private ActionListener mListener;
    private boolean mAllowDelete;

    //  only one at a time please.
    private static PageMenu singleton = null;

    //  a popup window for us.
    private NUIPopupWindow popupWindow;

    //  constructor
    public PageMenu(Context context, View anchor, boolean allowDelete, ActionListener listener)
    {
        mContext = context;
        mAnchor = anchor;
        mListener = listener;
        mAllowDelete = allowDelete;
    }

    //  time to show the dialog
    public void show()
    {
        //  remember us
        singleton = this;

        //  get the layout
        View popupView = LayoutInflater.from(mContext).inflate(R.layout.sodk_editor_page_menu, null);

        //  hook up the buttons
        Button b1 = (Button)popupView.findViewById(R.id.delete_button);
        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                mListener.onDelete();
            }
        });

        Button b2 = (Button)popupView.findViewById(R.id.duplicate_button);
        b2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                mListener.onDuplicate();
            }
        });

        //  hide the delete button if it's not allowed
        if (!mAllowDelete)
        {
            View v = popupView.findViewById(R.id.delete_button_wrapper);
            if (v!=null)
                v.setVisibility(View.GONE);
        }

        //  put everything in a popup window
        popupWindow = new NUIPopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        popupWindow.setFocusable(true);
        popupWindow.setOnDismissListener(this);
        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);

        //  ... and show it above the anchor (which is a view in the page list)
        int h1 = mAnchor.getHeight();
        int h2 = popupView.getMeasuredHeight();
        popupWindow.showAsDropDown(mAnchor, 0, -h1-h2);
    }

    //  internal function to dismiss the popup.
    private void dismiss()
    {
        popupWindow.dismiss();
        singleton = null;
    }

    //  this function is called when the user taps outside the popup.
    //  we make sure to dismiss it properly.
    @Override
    public void onDismiss() {
        dismiss();
    }

    public interface ActionListener
    {
        void onDelete();
        void onDuplicate();
    }

}
