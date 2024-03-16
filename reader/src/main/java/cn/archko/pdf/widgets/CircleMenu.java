package cn.archko.pdf.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: archko 2024/3/15 :21:40
 */
public class CircleMenu extends View {

    private static final String TAG = "CircleMenu";

    public static final int MAX_WIDTH = 280;
    private int abroadRadius = 100;           //外部半径
    private Paint circlePaint;          //外部圆画笔
    private float paintSize = 24f;           //画笔宽度
    private float offsetAngle = 0f;          //初始角度

    private Paint textPaint;            //文字画笔

    private Paint bitmapPaint;              //图片画笔
    private Resources mResources;

    private float textRadius;
    private float radiusSize;               //半径+画笔的宽度一半 = 看到圆的半径
    private float size;          //内边到外边距的大小

    private Map<Integer, Float> angleMap;            //记录每个Itme的角度值

    /**
     * 文字的大小
     */
    private float mTextSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, 16, getResources().getDisplayMetrics());

    private static int abroadBgColor = Color.parseColor("#3783f6");          //圆形菜单颜色

    /**
     * 菜单Item
     */
    private List<BaseMenu> menus = new ArrayList<>();

    {
        BaseMenu menu = new BaseMenu(Color.BLUE, 1f, "menu1", -1, 0);
        menus.add(menu);
        menu = new BaseMenu(Color.RED, 1f, "menu2", -1, 0);
        menus.add(menu);
        menu = new BaseMenu(Color.GREEN, 1f, "menu3", -1, 0);
        menus.add(menu);
        menu = new BaseMenu(Color.YELLOW, 1f, "menu4", -1, 0);
        menus.add(menu);
        menu = new BaseMenu(Color.CYAN, 1f, "menu5", -1, 0);
        menus.add(menu);
        menu = new BaseMenu(Color.MAGENTA, 1f, "menu6", -1, 0);
        menus.add(menu);
        menu = new BaseMenu(Color.BLACK, 1f, "menu7", -1, 0);
        menus.add(menu);
        menu = new BaseMenu(Color.BLUE, 1f, "menu8", -1, 0);
        menus.add(menu);
    }

    private float X = 100;          //默认位置
    private float Y = 100;
    private float angle;            //每个Item所占的角度
    Path path = new Path();  //路径

    public CircleMenu(Context context) {
        this(context, null);
    }

    public CircleMenu(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CircleMenu(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mResources = getResources();

        angleMap = new HashMap<>();

        circlePaint = new Paint();
        circlePaint.setColor(abroadBgColor);
        //circlePaint.setAlpha(80);
        circlePaint.setStrokeWidth(paintSize);
        circlePaint.setAntiAlias(true);
        circlePaint.setStyle(Paint.Style.STROKE);


        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(mTextSize);

        bitmapPaint = new Paint();
        //bitmapPaint.setFilterBitmap(true);
        //bitmapPaint.setDither(true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        //控制圆显示最大的大小，最大为280，最小为宽高最小值的三分之2
        if (Math.min(getMeasuredWidth(), getMeasuredHeight()) > MAX_WIDTH) {
            abroadRadius = MAX_WIDTH;
        } else {
            abroadRadius = Math.min(getMeasuredWidth(), getMeasuredHeight()) * 3 / 5;
        }
        X = getMeasuredWidth() / 2f;
        Y = getMeasuredHeight() / 2f;
        radiusSize = abroadRadius + (paintSize / 2);
        size = radiusSize - paintSize;
        Log.e(TAG, "中心点X：" + X + "    中心点Y：" + Y + "     半径：" + abroadRadius);

        paintSize = (float) abroadRadius / 2;           //取画笔的宽度为半径的三分之一

        circlePaint.setStrokeWidth(paintSize);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //canvas.drawCircle(X, Y, abroadRadius, circlePaint);

        int itemSize = menus.size();
        angle = 360f / itemSize;
        float centerX = X;//中点
        float centerY = Y;
        textRadius = abroadRadius;
        RectF rectF =
                new RectF(centerX - abroadRadius, centerY - abroadRadius, centerX + abroadRadius, centerY + abroadRadius);
        //计算添加文字的区域
        final RectF textf = new RectF(centerX - textRadius, centerY - textRadius, centerX + textRadius, centerY + textRadius);

        for (int i = 0; i < itemSize; i++) {
            //计算扇形中间点的位子
            BaseMenu baseMenu = menus.get(i);

            circlePaint.setColor(menus.get(i).getColor());
            canvas.drawArc(
                    rectF,
                    offsetAngle,
                    angle + offsetAngle,
                    true,
                    circlePaint
            );

            if (baseMenu.getType() == 0) {
                drawText(textf, textRadius, offsetAngle, angle, baseMenu, canvas, itemSize);
            } else if (baseMenu.getType() == 1) {
                drawBitmap((int) centerX, (int) centerY, (int) textRadius, offsetAngle, angle, baseMenu.getResId(), canvas);
            }
            offsetAngle += angle;
            angleMap.put(i, offsetAngle);
        }

        //canvasText(canvas);
    }

    private void canvasText(Canvas canvas) {
        //计算每个占位的角度
        int itemSize = menus.size();
        angle = 360f / itemSize;
        float centerX = X;//中点
        float centerY = Y;
        Log.e(TAG, "X:" + X + "   Y:" + Y);
        textRadius = abroadRadius;
        //计算添加文字的区域
        final RectF textf = new RectF(centerX - textRadius, centerY - textRadius, centerX + textRadius, centerY + textRadius);
        for (int i = 0; i < itemSize; i++) {
            //计算扇形中间点的位子
            BaseMenu baseMenu = menus.get(i);
            if (baseMenu.getType() == 0) {
                drawText(textf, textRadius, offsetAngle, angle, baseMenu, canvas, itemSize);
            } else if (baseMenu.getType() == 1) {
                drawBitmap((int) centerX, (int) centerY, (int) textRadius, offsetAngle, angle, baseMenu.getResId(), canvas);
            }
            offsetAngle += angle;
            angleMap.put(i, offsetAngle);            //记录每个Itme的位子
        }
    }

    /**
     * 绘制图片
     */
    private void drawBitmap(int X, int Y, float radius, float offsetAngle, float angle, int imgId, Canvas canvas) {
        float imgWidth = radius / 6;
        float x = (float) (X + radius * Math.cos(Math.toRadians(offsetAngle + (angle / 4))));
        float y = (float) (Y + radius * Math.sin(Math.toRadians(offsetAngle + (angle / 4))));
        RectF rectf = new RectF(x - imgWidth * 2 / 3,
                y - imgWidth * 2 / 3, x + imgWidth
                * 2 / 3,
                y + imgWidth * 2 / 3);
        Bitmap bitmap = ((BitmapDrawable) mResources.getDrawable(imgId)).getBitmap();
        canvas.drawBitmap(bitmap, null, rectf, null);
    }

    /**
     * 绘制文本
     */
    private void drawText(RectF range, float radius, float startAngle, float sweepAngle,
                          BaseMenu menu, Canvas canvas, int itemCount) {
        path.reset();
        path.addArc(range, startAngle, sweepAngle);
        float textWidth = textPaint.measureText(menu.getContent());
        // 利用水平偏移让文字居中
        float hOffset = (float) (radius * Math.PI / itemCount / 2 - textWidth / 2);// 水平偏移
        float vOffset = radius / 2 / 6;// 垂直偏移
        canvas.drawTextOnPath(menu.getContent(), path, hOffset, vOffset, textPaint);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float downX;
        float downY;
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
                //防止按下直接消失，所以我们这里直接返回为true
                return true;
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();
                downY = event.getY();
                Log.e(TAG, "ACTION_DOWN:X:" + downX + "    Y:" + downY);
                float distanceX = Math.abs(X - downX);
                float distanceY = Math.abs(Y - downY);
                float distanceZ = (float) Math.sqrt(Math.pow(distanceX, 2) + Math.pow(distanceY, 2));

                if (distanceZ < radiusSize && distanceZ > size) {
                    float radius = 0;
                    // 第一象限
                    if (downX >= getMeasuredWidth() / 2f && downY >= getMeasuredHeight() / 2f) {
                        Log.e(TAG, "ACTION_DOWN:X:" + downX + "    Y:" + downY);
                        radius = (int) (Math.atan((downY - getMeasuredHeight() / 2f)
                                / (downX - getMeasuredWidth() / 2f)) * 180 / Math.PI);
                    }
                    // 第二象限
                    if (downX <= getMeasuredWidth() / 2f && downY >= getMeasuredHeight() / 2f) {
                        Log.e(TAG, "ACTION_DOWN:X:" + downX + "    Y:" + downY);
                        radius = (int) (Math.atan((getMeasuredWidth() / 2f - downX)
                                / (downY - getMeasuredHeight() / 2f))
                                * 180 / Math.PI + 90);
                    }
                    // 第三象限
                    if (downX <= getMeasuredWidth() / 2f && downY <= getMeasuredHeight() / 2f) {
                        Log.e(TAG, "ACTION_DOWN:X:" + downX + "    Y:" + downY);
                        radius = (int) (Math.atan((getMeasuredHeight() / 2f - downY)
                                / (getMeasuredWidth() / 2f - downX))
                                * 180 / Math.PI + 180);
                    }
                    // 第四象限
                    if (downX >= getMeasuredWidth() / 2f && downY <= getMeasuredHeight() / 2f) {
                        Log.e(TAG, "ACTION_DOWN:X:" + downX + "    Y:" + downY);
                        radius = (int) (Math.atan((downX - getMeasuredWidth() / 2f)
                                / (getMeasuredHeight() / 2f - downY))
                                * 180 / Math.PI + 270);
                    }

                    //遍历Map
                    for (int i : angleMap.keySet()) {
                        int x = (int) (angleMap.get(i) - angle);
                        //判断点击的位置，是否在item的区间
                        if (radius > angleMap.get(i) - angle && radius < angleMap.get(i)) {
                            Log.e(TAG, "x:" + x + "     y:" + angleMap.get(i) + "    radius:" + radius);
                            circleOnClickItemListener.onItem(this, i);
                            break;
                        }
                    }
                    return true;
                }

                break;
        }

        return super.onTouchEvent(event);
    }

    //判断显示隐藏
    private boolean isShowView = false;

    public boolean isShowView() {
        return isShowView;
    }

    //外部提供显示隐藏的方法
    public void isShow(float x, float y, float width, float height) {
        isShowView = true;
        setVisibility(VISIBLE);
        calculation(x, y, width, height);
        angleMap.clear();
        invalidate();
        angleMap = new HashMap<>();
        offsetAngle = 0;
    }

    private CircleOnClickItemListener circleOnClickItemListener;

    public void setCircleOnClickItemListener(CircleOnClickItemListener circleOnClickItemListener) {
        this.circleOnClickItemListener = circleOnClickItemListener;
    }

    //计算，显示的位置
    public void calculation(float downX, float downY, float width, float height) {
        int dpValue = 25;
        downY = downY - dip2px(dpValue);

        //防止点击角落的位子，或者边缘的位子，圆弧菜单有有一部分显示不出来，所以我们计算点击的位子，到边缘的距离
        if (downX > width - radiusSize) {
            X = downX > downX - radiusSize ? width - radiusSize : downX;
        } else {
            X = Math.max(downX, radiusSize);
        }
        if (downY >= height - radiusSize) {
            Y = height - radiusSize - dip2px(dpValue);

        } else {
            Y = Math.max(downY, radiusSize);
        }
    }

    //如果显示，就隐藏
    public void chie() {
        isShowView = false;
        setVisibility(INVISIBLE);
    }

    private int dip2px(float dpValue) {
        final float scale = getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public interface CircleOnClickItemListener {
        void onItem(View view, int pos);
    }

}