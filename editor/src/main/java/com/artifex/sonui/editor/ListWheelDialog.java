package com.artifex.sonui.editor;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import kankan.wheel.widget.WheelView;
import kankan.wheel.widget.adapters.ArrayWheelAdapter;

public class ListWheelDialog
{
    private static boolean buttonDismissed = false;
    public static boolean wasDismissedWithButton() {return buttonDismissed;}

    //  callers should supply one of these
    interface ListWheelDialogListener
    {
        void update(String val);
        void cancel();
    }

    public static void show(Context context, final String[] values, String currentValue, final ListWheelDialogListener listener)
    {
        //  get the layout
        final View layout = View.inflate(context, R.layout.sodk_editor_wheel_chooser_dialog, null);

        //  setup the wheel
        View wv = layout.findViewById(R.id.wheel);
        final WheelView wheel = (WheelView)wv;
        final ArrayWheelAdapter<String> adapter = new ArrayWheelAdapter<String>(context, values);
        wheel.setViewAdapter(adapter);
        wheel.setVisibleItems(5);

        //  set wheel current value
        int currentIndex = -1;
        for (int i=0; i<values.length; i++)
        {
            if (currentValue.equals(values[i]))
            {
                currentIndex = i;
                break;
            }
        }
        if (currentIndex>=0)
            wheel.setCurrentItem(currentIndex);

        //  create the dialog
        final AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(layout)
                .create();

        buttonDismissed = false;

        //  cancel button
        layout.findViewById(R.id.sodk_editor_cancel_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonDismissed = true;
                dialog.dismiss();
                listener.cancel();
            }
        });

        //  update button
        layout.findViewById(R.id.sodk_editor_update_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonDismissed = true;
                dialog.dismiss();
                listener.update(values[wheel.getCurrentItem()]);
            }
        });

        //  set a key listener
        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_UP)
                {
                    if (keyCode==KeyEvent.KEYCODE_TAB || keyCode==KeyEvent.KEYCODE_ENTER )
                    {
                        dialog.dismiss();
                    }
                }
                return true;
            }
        });

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                listener.cancel();
            }
        });

        Window window = dialog.getWindow();
        window.setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
        window.setGravity(Gravity.CENTER);

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        //  show it
        dialog.show();
    }
}
