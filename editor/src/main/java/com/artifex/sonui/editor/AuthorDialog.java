package com.artifex.sonui.editor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;

public class AuthorDialog
{
    public static void show(final Activity activity, final AuthorDialogListener listener, final String author)
    {
        ContextThemeWrapper ctw = new ContextThemeWrapper(activity, R.style.sodk_editor_alert_dialog_style);
        AlertDialog.Builder dialog = new AlertDialog.Builder(ctw);

        LayoutInflater li = LayoutInflater.from(activity);
        View mainView = li.inflate(R.layout.sodk_editor_author_dialog, null);
        dialog.setView(mainView);
        dialog.setTitle(activity.getResources().getString(R.string.sodk_editor_author_dialog_title));
        final SOEditText et = (SOEditText)(mainView.findViewById(R.id.editTextDialogUserInput));

        et.setText(author);
        et.selectAll();

        dialog.setPositiveButton(activity.getResources().getString(R.string.sodk_editor_update), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                Utilities.hideKeyboard(activity);
                dialog.dismiss();
                if (listener!=null)
                {
                    String author = et.getText().toString();
                    listener.onOK(author);
                }
            }
        });

        dialog.setNegativeButton(activity.getResources().getString(R.string.sodk_editor_retain), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Utilities.hideKeyboard(activity);
                dialog.dismiss();
                if (listener!=null)
                    listener.onCancel();
            }
        });

        dialog.create().show();
    }

    public interface AuthorDialogListener
    {
        void onOK(String author);
        void onCancel();
    }
}