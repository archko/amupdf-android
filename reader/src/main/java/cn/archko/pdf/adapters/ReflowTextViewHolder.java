package cn.archko.pdf.adapters;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.text.Html;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import cn.archko.pdf.common.Logcat;
import cn.archko.pdf.common.ParseTextMain;
import cn.archko.pdf.common.ReflowViewCache;
import cn.archko.pdf.common.StyleHelper;
import cn.archko.pdf.entity.BitmapBean;
import cn.archko.pdf.entity.ReflowBean;
import cn.archko.pdf.utils.BitmapUtils;

/**
 * @author: archko 2019-02-21 :09:18
 */
public class ReflowTextViewHolder extends BaseViewHolder {

    public PDFTextView pageView;

    public ReflowTextViewHolder(PDFTextView itemView) {
        super(itemView);
        pageView = itemView;
    }

    public void bindAsText(byte[] result, int screenHeight, int screenWidth, float systemScale) {
        /*String text = ParseTextMain.Companion.getInstance().parseAsText(result);
        //Logcat.d("text", text = UnicodeDecoder.unescape2(text));
        Html.ImageGetter imageGetter = new Html.ImageGetter() {
            @Override
            public Drawable getDrawable(String source) {
                //Log.d("text", source);
                Bitmap bitmap = BitmapUtils.base64ToBitmap(source.replaceAll("data:image/(png|jpeg);base64,", "")*//*.replaceAll("\\s", "")*//*);

                if (null == bitmap ||
                        (bitmap.getWidth() < PDFTextView.minImgHeight
                                && bitmap.getHeight() < PDFTextView.minImgHeight)) {
                    Logcat.d("text", "bitmap decode failed.");
                    return null;
                }
                float width = bitmap.getWidth() * systemScale;
                float height = bitmap.getHeight() * systemScale;
                int sw = screenHeight;
                if (isScreenPortrait(pageView.getContext())) {
                    sw = screenWidth;
                }
                if (width > sw) {
                    float ratio = sw / width;
                    height = ratio * height;
                    width = sw;
                }
                Drawable drawable = new BitmapDrawable(null, bitmap);
                drawable.setBounds(0, 0, (int) width, (int) height);
                return drawable;
            }
        };
        Spanned spanned = AHtml.fromHtml(text, imageGetter, null);
        pageView.textView.setText(spanned);*/
    }

    public void bindAsList(byte[] result, int screenHeight, int screenWidth, float systemScale) {
        List<ReflowBean> text = ParseTextMain.Companion.getInstance().parseAsList(result, 0);
        bindAsList(text, screenHeight, screenWidth, systemScale, null);
    }

    public void bindAsList(List<ReflowBean> text, int screenHeight, int screenWidth, float systemScale, ReflowViewCache reflowViewCache) {
        recycleViews(reflowViewCache);
        pageView.applyStyle();
        for (ReflowBean reflowBean : text) {
            if (reflowBean.getType() == ReflowBean.TYPE_STRING) {
                pageView.addTextView(reflowBean.getData(), reflowViewCache);
            } else {
                pageView.addImageView(reflowBean.getData(), systemScale, screenHeight, screenWidth, reflowViewCache);
            }
        }
    }

    public void recycleViews(ReflowViewCache reflowViewCache) {
        if (null != reflowViewCache) {
            for (int i = 0; i < pageView.getChildCount(); i++) {
                final View child = pageView.getChildAt(i);
                if (child instanceof TextView) {
                    reflowViewCache.addTextView((TextView) child);
                } else if (child instanceof ImageView) {
                    reflowViewCache.addImageView((ImageView) child);
                }
            }
            pageView.removeAllViews();
        }
    }

    private static String IMAGE_HEADER = "base64,";

    private static BitmapBean decodeBitmap(String base64Source, float systemScale, int screenHeight, int screenWidth, Context context) {
        if (TextUtils.isEmpty(base64Source)) {
            return null;
        }
        //Logcat.longLog("text", base64Source);
        if (!base64Source.contains(IMAGE_HEADER)) {
            return null;
        }
        int index = base64Source.indexOf(IMAGE_HEADER);
        base64Source = base64Source.substring(index + IMAGE_HEADER.length());
        //Logcat.d("base:" + base64Source);
        Bitmap bitmap = BitmapUtils.base64ToBitmap(base64Source.replaceAll("\"/></p>", "")/*.replaceAll("\\s", "")*/);

        if (null == bitmap
                || (bitmap.getWidth() < PDFTextView.minImgHeight
                && bitmap.getHeight() < PDFTextView.minImgHeight)) {
            Logcat.i("text", "bitmap decode failed.");
            return null;
        }
        float width = bitmap.getWidth() * systemScale;
        float height = bitmap.getHeight() * systemScale;
        if (Logcat.loggable) {
            Logcat.d(String.format("width:%s, height:%s systemScale:%s", bitmap.getWidth(), bitmap.getHeight(), systemScale));
        }
        int sw = screenHeight;
        if (isScreenPortrait(context)) {
            sw = screenWidth;
        }
        if (width > sw) {
            float ratio = sw / width;
            height = ratio * height;
            width = sw;
        }

        return new BitmapBean(bitmap, width, height);
    }

    static boolean isScreenPortrait(Context context) {
        Configuration mConfiguration = context.getResources().getConfiguration(); //获取设置的配置信息
        int ori = mConfiguration.orientation; //获取屏幕方向
        if (ori == Configuration.ORIENTATION_LANDSCAPE) {
            //横屏
            return false;
        } else if (ori == Configuration.ORIENTATION_PORTRAIT) {
            //竖屏
        }
        return true;
    }

    public static class PDFTextView extends LinearLayout {

        static float minImgHeight = 32;
        private StyleHelper styleHelper;

        public PDFTextView(Context context, StyleHelper styleHelper) {
            super(context);
            this.styleHelper = styleHelper;
            setOrientation(VERTICAL);
            setMinimumHeight(480);
            setPadding(styleHelper.getStyleBean().getLeftPadding(), styleHelper.getStyleBean().getTopPadding(),
                    styleHelper.getStyleBean().getRightPadding(), styleHelper.getStyleBean().getBottomPadding());
            /*textView = new TextView(context);
            LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.gravity = Gravity.CENTER_HORIZONTAL;
            addView(textView, lp);
            setPadding(0, 50, 0, 40);
            setBackgroundColor(context.getResources().getColor(R.color.white));

            mPaint = textView.getPaint();
            mTextSize = mPaint.getTextSize();
            mPaint.setTextSize(mTextSize * 1.2f);
            // html:行间距要调小,会导致图文重排图片上移,段间距偏大,行间距也偏大,
            // xhtml:默认不修改,文本行间距偏小,段间距偏大.
            // text:所有文本不换行,显示不对.剩下与xhtml相同.如果不使用Html.from设置,有换行,但显示不了图片.
            // textView.setLineSpacing(0, 0.8f);
            setStyleForText(context);*/
        }

        void applyStyle() {
            setBackgroundColor(styleHelper.getStyleBean().getBgColor());
        }

        void addTextView(String text, ReflowViewCache cacheViews) {
            if (TextUtils.isEmpty(text)) {
                return;
            }
            TextView textView = null;
            if (null != cacheViews && cacheViews.textViewCount() > 0) {
                textView = cacheViews.getTextView(0);
            } else {
                textView = new TextView(getContext());
                textView.setTextIsSelectable(false);
                if (minImgHeight == 32) {
                    minImgHeight = textView.getPaint().measureText("我") + 5;
                }
            }
            LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.gravity = Gravity.CENTER_HORIZONTAL;
            addView(textView, lp);

            applyStyleForText(getContext(), textView);

            textView.setText(Html.fromHtml(text));
        }

        /**
         * html:行间距要调小,会导致图文重排图片上移,段间距偏大,行间距也偏大,
         * xhtml:默认不修改,文本行间距偏小,段间距偏大.
         * text:所有文本不换行,显示不对.剩下与xhtml相同.如果不使用Html.from设置,有换行,但显示不了图片.
         * textView.setLineSpacing(0, 0.8f);
         *
         * @param context
         * @param textView
         */
        void applyStyleForText(Context context, TextView textView) {
            textView.setTextSize(styleHelper.getStyleBean().getTextSize());

            textView.setTextColor(styleHelper.getStyleBean().getFgColor());
            textView.setLineSpacing(0, styleHelper.getStyleBean().getLineSpacingMult());

            Typeface typeface = styleHelper.getFontHelper().getTypeface();
            textView.setTypeface(typeface);
        }

        void addImageView(String text, float systemScale, int screenHeight, int screenWidth, ReflowViewCache reflowViewCache) {
            BitmapBean bean = decodeBitmap(text, systemScale, screenHeight, screenWidth, getContext());
            if (null != bean && bean.getBitmap() != null) {
                ImageView imageView = null;
                if (null != reflowViewCache && reflowViewCache.imageViewCount() > 0) {
                    imageView = reflowViewCache.getImageView(0);
                } else {
                    imageView = new ImageView(getContext());
                    imageView.setAdjustViewBounds(true);
                    imageView.setScaleType(ImageView.ScaleType.FIT_XY);
                }
                LayoutParams lp = new LayoutParams((int) bean.getWidth(), (int) bean.getHeight());
                lp.bottomMargin = 20;
                lp.leftMargin = 10;
                lp.rightMargin = 10;
                lp.gravity = Gravity.CENTER_HORIZONTAL;
                addView(imageView, lp);
                imageView.setImageBitmap(bean.getBitmap());
            }
        }
    }
}
