package cn.archko.pdf.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import androidx.core.text.HtmlCompat;
import androidx.recyclerview.awidget.ARecyclerView;
import cn.archko.pdf.R;
import cn.archko.pdf.common.StyleHelper;
import cn.archko.pdf.core.cache.ReflowViewCache;
import cn.archko.pdf.core.common.ParseTextMain;
import cn.archko.pdf.core.entity.BitmapBean;
import cn.archko.pdf.core.entity.ReflowBean;
import cn.archko.pdf.core.utils.Utils;

/**
 * @author: archko 2019-02-21 :09:18
 */
public class ReflowTextViewHolder extends ARecyclerView.ViewHolder {

    public PDFTextView pageView;

    public ReflowTextViewHolder(PDFTextView itemView) {
        super(itemView);
        pageView = itemView;
    }

    public void bindAsList(List<ReflowBean> text, int screenHeight, int screenWidth,
                           float systemScale, ReflowViewCache reflowViewCache, boolean showBookmark) {
        recycleViews(reflowViewCache);
        pageView.applyStyle();
        for (ReflowBean reflowBean : text) {
            if (reflowBean.getType() == ReflowBean.TYPE_STRING) {
                pageView.addTextView(reflowBean.getData(), reflowViewCache, showBookmark);
            } else {
                pageView.addImageView(reflowBean.getData(), systemScale, screenHeight, screenWidth, reflowViewCache, showBookmark);
            }
        }
    }

    public void bindAsReflowBean(ReflowBean reflowBean, int screenHeight, int screenWidth,
                                 float systemScale, ReflowViewCache reflowViewCache, boolean showBookmark) {
        recycleViews(reflowViewCache);
        pageView.applyStyle();
        if (reflowBean.getType() == ReflowBean.TYPE_STRING) {
            pageView.addTextView(reflowBean.getData(), reflowViewCache, showBookmark);
        } else {
            pageView.addImageView(reflowBean.getData(), systemScale, screenHeight, screenWidth, reflowViewCache, showBookmark);
        }
    }

    /**
     * 直接加载整页 HTML（图文混排）
     */
    public void bindHtml(String html, int screenHeight, int screenWidth,
                         float systemScale, ReflowViewCache reflowViewCache, boolean showBookmark) {
        recycleViews(reflowViewCache);
        pageView.bindHtml(html, systemScale, screenHeight, screenWidth, reflowViewCache, showBookmark);
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

    public static class PDFTextView extends LinearLayout {

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

        void addTextView(String text, ReflowViewCache cacheViews, boolean showBookmark) {
            if (TextUtils.isEmpty(text)) {
                return;
            }
            TextView textView = null;
            if (null != cacheViews && cacheViews.textViewCount() > 0) {
                textView = cacheViews.getAndRemoveTextView(0);
            } else {
                textView = new TextView(getContext());
                textView.setTextIsSelectable(false);
                if (ParseTextMain.INSTANCE.getMinImgHeight() == 32f) {
                    ParseTextMain.INSTANCE.setMinImgHeight(textView.getPaint().measureText("我") + 5);
                }
            }
            LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            lp.gravity = Gravity.CENTER_HORIZONTAL;
            addView(textView, lp);

            applyStyleForText(getContext(), textView);

            textView.setText(text);

            addBookmark(showBookmark);
        }

        private void addBookmark(boolean showBookmark) {
            LayoutParams lp;
            if (showBookmark) {
                lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                lp.leftMargin = Utils.dipToPixel(2);
                lp.topMargin = Utils.dipToPixel(2);
                TextView bm = new TextView(getContext());
                bm.setText("B");
                bm.setGravity(Gravity.CENTER);
                bm.setBackgroundResource(R.drawable.bg_bookmark_cicle);
                bm.setTextColor(Color.MAGENTA);
                bm.setPadding(Utils.dipToPixel(4), 0, Utils.dipToPixel(4), 0);
                bm.setTextSize(18);
                addView(bm, lp);
            }
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

        void addImageView(String text, float systemScale, int screenHeight, int screenWidth, ReflowViewCache reflowViewCache, boolean showBookmark) {
            BitmapBean bean = ParseTextMain.INSTANCE.decodeBitmap(text, systemScale, screenHeight, screenWidth, getContext());
            if (null != bean && bean.getBitmap() != null) {
                ImageView imageView = null;
                if (null != reflowViewCache && reflowViewCache.imageViewCount() > 0) {
                    imageView = reflowViewCache.getAndRemoveImageView(0);
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
            addBookmark(showBookmark);
        }

        /**
         * 直接加载整页 HTML（图文混排）
         */
        void bindHtml(String html, float systemScale, int screenHeight, int screenWidth,
                      ReflowViewCache reflowViewCache, boolean showBookmark) {
            // PDFTextView类中没有recycleViews方法，直接清空视图
            removeAllViews();
            applyStyle();
            TextView tv = null;
            if (null != reflowViewCache && reflowViewCache.textViewCount() > 0) {
                tv = reflowViewCache.getAndRemoveTextView(0);
            } else {
                tv = new TextView(getContext());
                tv.setTextIsSelectable(false);
            }
            LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            lp.gravity = Gravity.CENTER_HORIZONTAL;
            addView(tv, lp);
            applyStyleForText(getContext(), tv);

            // Html.fromHtml 加载图文，imageGetter 负责把 base64 <img> 解码成 Drawable
            tv.setText(HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY,
                    source -> {
                        BitmapBean bean = ParseTextMain.INSTANCE.decodeBitmap(source, systemScale, screenHeight, screenWidth, getContext());
                        if (bean == null || bean.getBitmap() == null) {
                            return new ColorDrawable(0x00000000);
                        }
                        BitmapDrawable drawable = new BitmapDrawable(getResources(), bean.getBitmap());
                        drawable.setBounds(0, 0, (int) bean.getWidth(), (int) bean.getHeight());
                        return drawable;
                    }, null));

            addBookmark(showBookmark);
        }
    }
}