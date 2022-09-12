package cn.archko.pdf.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.artifex.sonui.editor.Utilities;

import cn.archko.mupdf.R;

public class PasswordDialog {

    public static void show(final Activity activity, final PasswordDialogListener listener) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(activity);

        LayoutInflater li = LayoutInflater.from(activity);
        View mainView = li.inflate(R.layout.password_dialog, null);
        dialog.setView(mainView);
        dialog.setTitle(activity.getResources().getString(R.string.sodk_editor_password_for_document));
        final EditText et = mainView.findViewById(R.id.editTextDialogUserInput);

        dialog.setPositiveButton(activity.getResources().getString(R.string.sodk_editor_ok),
                (dialog1, which) -> {
                    Utilities.hideKeyboard(activity);
                    String content = et.getText().toString();
                    if (TextUtils.isEmpty(content)) {
                        Toast.makeText(activity, R.string.sodk_editor_password_for_document, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    dialog1.dismiss();
                    if (listener != null) {
                        listener.onOK(content);
                    }
                });

        dialog.setNegativeButton(activity.getResources().getString(R.string.sodk_editor_cancel),
                (dialog12, which) -> {
                    Utilities.hideKeyboard(activity);
                    dialog12.dismiss();
                    if (listener != null) {
                        listener.onCancel();
                    }
                });

        dialog.create().show();
    }

    public interface PasswordDialogListener {
        void onOK(String content);

        void onCancel();
    }
}