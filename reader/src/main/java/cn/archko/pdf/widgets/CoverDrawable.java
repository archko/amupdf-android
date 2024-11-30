package cn.archko.pdf.widgets;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

public class CoverDrawable extends Drawable {

   Bitmap bitmap;
   Paint defPaint;
   final static int alphaLevels = 16;
   final static int shadowSizePercent = 6;
   final static int minAlpha = 40;
   final static int maxAlpha = 180;
   final Paint[] shadowPaints = new Paint[alphaLevels + 1];

   public CoverDrawable(Bitmap bitmap) {
      super();
      this.bitmap = bitmap;
      defPaint = new Paint();
      defPaint.setColor(0xFF000000);
      defPaint.setFilterBitmap(true);
      for (int i = 0; i <= alphaLevels; i++) {
         int alpha = (maxAlpha - minAlpha) * i / alphaLevels + minAlpha;
         shadowPaints[i] = new Paint();
         shadowPaints[i].setColor((alpha << 24) | 0x101010);
      }
   }

   public void drawShadow(Canvas canvas, Rect bookRect, Rect shadowRect) {
      int d = shadowRect.bottom - bookRect.bottom;
      if (d <= 0)
         return;
      Rect l = new Rect(shadowRect);
      Rect r = new Rect(shadowRect);
      Rect t = new Rect(shadowRect);
      Rect b = new Rect(shadowRect);
      for (int i = 0; i < d; i++) {
         shadowRect.left++;
         shadowRect.right--;
         shadowRect.top++;
         shadowRect.bottom--;
         if (shadowRect.bottom < bookRect.bottom || shadowRect.right < bookRect.right) {
            break;
         }
         l.set(shadowRect);
         l.top = bookRect.bottom;
         l.right = l.left + 1;
         t.set(shadowRect);
         t.left = bookRect.right;
         t.right--;
         t.bottom = t.top + 1;
         r.set(shadowRect);
         r.left = r.right - 1;
         b.set(shadowRect);
         b.top = b.bottom - 1;
         b.left++;
         b.right--;
         int index = i * alphaLevels / d;
         Paint paint = shadowPaints[index];
         if (!l.isEmpty()) {
            canvas.drawRect(l, paint);
         }
         if (!r.isEmpty()) {
            canvas.drawRect(r, paint);
         }
         if (!t.isEmpty()) {
            canvas.drawRect(t, paint);
         }
         if (!b.isEmpty()) {
            canvas.drawRect(b, paint);
         }
      }
   }

   boolean checkShadowSize(int bookSize, int shadowSize) {
      if (bookSize < 10) {
         return false;
      }
      int p = 100 * shadowSize / bookSize;
      if (p >= 0 && p >= shadowSizePercent - 2 && p <= shadowSizePercent + 2) {
         return true;
      }
      return false;
   }

   @Override
   public void draw(Canvas canvas) {
      try {
         Rect fullrc = getBounds();
         if (fullrc.width() < 5 || fullrc.height() < 5) {
            return;
         }
         int w = bitmap.getWidth();
         int h = bitmap.getHeight();
         int shadowW = fullrc.width() - w;
         int shadowH = fullrc.height() - h;
         if (!checkShadowSize(w, shadowW) || !checkShadowSize(h, shadowH)) {
            w = fullrc.width() * 100 / (100 + shadowSizePercent);
            h = fullrc.height() * 100 / (100 + shadowSizePercent);
            shadowW = fullrc.width() - w;
            shadowH = fullrc.height() - h;
         }
         Rect rc = new Rect(fullrc.left, fullrc.top, fullrc.right - shadowW, fullrc.bottom - shadowH);
         if (bitmap != null) {
            //log.d("Image for " + bitmap + " is found in cache, drawing...");
            Rect dst = getBestCoverSize(rc, bitmap.getWidth(), bitmap.getHeight());
            try {
               canvas.drawBitmap(bitmap, null, dst, defPaint);
            } catch (Exception ignored) {
            }
            if (shadowSizePercent > 0) {
               Rect shadowRect = new Rect(rc.left + shadowW, rc.top + shadowH, rc.right + shadowW, rc.bottom + shadowW);
               drawShadow(canvas, rc, shadowRect);
            }
            return;
         }
      } catch (Exception e) {
      }
   }

   @Override
   public int getIntrinsicHeight() {
      return bitmap.getHeight() * (100 + shadowSizePercent) / 100;
   }

   @Override
   public int getIntrinsicWidth() {
      return bitmap.getWidth() * (100 + shadowSizePercent) / 100;
   }

   @Override
   public int getOpacity() {
      return PixelFormat.TRANSPARENT; // part of pixels are transparent
   }

   @Override
   public void setAlpha(int alpha) {
      // ignore, not supported
   }

   @Override
   public void setColorFilter(ColorFilter cf) {
      // ignore, not supported
   }

   private Rect getBestCoverSize(Rect dst, int srcWidth, int srcHeight) {
      int w = dst.width();
      int h = dst.height();
      if (srcWidth < 20 || srcHeight < 20) {
         return dst;
      }
      int sw = srcHeight * w / h;
      int sh = srcWidth * h / w;
      if (sw <= w) {
         sh = h;
      } else {
         sw = w;
      }
      int dx = (w - sw) / 2;
      int dy = (h - sh) / 2;
      return new Rect(dst.left + dx, dst.top + dy, dst.left + sw + dx, dst.top + sh + dy);
   }

}
