package cn.archko.pdf.colorpicker;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

/**
 * @author: archko 2019/9/30 :14:22
 */
public class ColorPickerDialog extends AlertDialog {

    private ColorPicker colorPickerView;
    private OnColorSelectedListener onColorSelectedListener;

    public ColorPickerDialog(Context context, int initialColor, OnColorSelectedListener onColorSelectedListener) {
        super(context);

        this.onColorSelectedListener = onColorSelectedListener;

        RelativeLayout relativeLayout = new RelativeLayout(context);
        LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);

        colorPickerView = new ColorPicker(context);
        colorPickerView.setColor(initialColor);

        relativeLayout.addView(colorPickerView, layoutParams);

        setButton(BUTTON_POSITIVE, context.getString(android.R.string.ok), onClickListener);
        setButton(BUTTON_NEGATIVE, context.getString(android.R.string.cancel), onClickListener);

        setView(relativeLayout);

    }

    public void updateColor(int initialColor) {
        colorPickerView.setColor(initialColor);
    }

    public void setOnColorSelectedListener(OnColorSelectedListener onColorSelectedListener) {
        this.onColorSelectedListener = onColorSelectedListener;
    }

    private OnClickListener onClickListener = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case BUTTON_POSITIVE:
                    int selectedColor = colorPickerView.getColor();
                    onColorSelectedListener.onColorSelected(selectedColor);
                    break;
                case BUTTON_NEGATIVE:
                    dialog.dismiss();
                    break;
            }
        }
    };

    public interface OnColorSelectedListener {
        public void onColorSelected(int color);
    }

}