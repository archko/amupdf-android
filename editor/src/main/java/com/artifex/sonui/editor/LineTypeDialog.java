package com.artifex.sonui.editor;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;

import com.artifex.solib.ArDkDoc;
import com.artifex.solib.SODoc;

import kankan.wheel.widget.OnWheelScrollListener;
import kankan.wheel.widget.WheelView;
import kankan.wheel.widget.adapters.WheelViewAdapter;

public class LineTypeDialog
{
    private static final int POPUP_OFFSET = 30;

    private static final int SOLineStyle_Solid             = 0;
    private static final int SOLineStyle_DashSys           = 1;
    private static final int SOLineStyle_DotSys            = 2;
    private static final int SOLineStyle_DashDotSys        = 3;
    private static final int SOLineStyle_DashDotDotSys     = 4;
    private static final int SOLineStyle_DotGEL            = 5;
    private static final int SOLineStyle_DashGEL           = 6;
    private static final int SOLineStyle_LongDashGEL       = 7;
    private static final int SOLineStyle_DashDotGEL        = 8;
    private static final int SOLineStyle_LongDashDotGEL    = 9;
    private static final int SOLineStyle_LongDashDotDotGEL = 10;

    private static final int styles[] = {
            SOLineStyle_Solid,
            SOLineStyle_DotSys,
            SOLineStyle_DotGEL,
            SOLineStyle_DashSys,
            SOLineStyle_DashGEL,
            SOLineStyle_LongDashGEL,
            SOLineStyle_DashDotSys,
            SOLineStyle_DashDotGEL,
            SOLineStyle_LongDashDotGEL,
            SOLineStyle_DashDotDotSys,
            SOLineStyle_LongDashDotDotGEL
    };

    private static final float patterns[][] = {
            {1,0},
            {1,1},
            {1,3},
            {3,1},
            {4,3},
            {8,3},
            {3,1,1,1},
            {4,3,1,3},
            {8,3,1,3},
            {3,1,1,1,1,1},
            {8,3,1,3,1,3}
    };

    public static void show(Context context, View anchor, final ArDkDoc doc)
    {
        int currentValue = ((SODoc)doc).getSelectionLineType();

        View layout = View.inflate(context, R.layout.sodk_editor_line_width_dialog, null);

        View wv = layout.findViewById(R.id.wheel);
        final WheelView wheel = (WheelView)wv;

        final LineTypeAdapter adapter = new LineTypeAdapter(context, styles);

        wheel.setViewAdapter(adapter);
        wheel.setVisibleItems(5);

        wheel.setCurrentItem(0);
        for (int i=0;i<styles.length;i++)
        {
            if (styles[i] == currentValue)
                wheel.setCurrentItem(i);
        }

        wheel.addScrollingListener(new OnWheelScrollListener() {
            @Override
            public void onScrollingStarted(WheelView wheel) {}

            @Override
            public void onScrollingFinished(WheelView wheel) {
                ((SODoc)doc).setSelectionLineType(styles[wheel.getCurrentItem()]);
            }
        });

        //  make a popup window
        final NUIPopupWindow popup = new NUIPopupWindow(layout,
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        popup.setFocusable(true);

        //  add a dismiss listener
        popup.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                //  remove scrolling listeners
                wheel.removeAllScrollingListeners();
            }
        });

        //  now show the popup
        popup.showAsDropDown(anchor,POPUP_OFFSET,POPUP_OFFSET);
    }

    public static class LineTypeAdapter implements WheelViewAdapter
    {
        public LineTypeAdapter(Context context, int styles[]) {
            super();
            mStyles = styles;
            mContext = context;
        }

        private Context mContext;
        private int mStyles[];

        @Override
        public View getItem(int position, View convertView, ViewGroup parent)
        {
            //  make a View if needed.
            if (convertView == null)
            {
                LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.sodk_editor_line_type_item, parent, false);
            }

            //  get the value
            int style = mStyles[position];

            DottedLineView bar = ((DottedLineView)convertView.findViewById(R.id.bar));
            bar.setPattern(patterns[position]);

            return convertView;
        }

        @Override
        public int getItemsCount() {
            return mStyles.length;
        }

        @Override
        public View getEmptyItem(View convertView, ViewGroup parent) {
            return null;
        }

        @Override
        public void registerDataSetObserver(DataSetObserver observer) {
        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) {
        }
    }
}
