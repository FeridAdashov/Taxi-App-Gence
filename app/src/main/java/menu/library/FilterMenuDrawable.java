package menu.library;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;

import org.jetbrains.annotations.NotNull;

import texel.texel.gencetaxiapp.R;

/**
 * Created by linroid on 15/3/10.
 */
public class FilterMenuDrawable extends Drawable {
    private Paint paint;
    private Context context;
    private int radius, drawable;

    private int lineWidth = 8;
    private float expandProgress = 0;

    public FilterMenuDrawable(Context ctx, int color, int radius, int drawable) {
        this.context = ctx;
        this.radius = radius;
        this.drawable = drawable;

        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(color);
        paint.setStrokeWidth(lineWidth);
    }

    public float getExpandProgress() {
        return expandProgress;
    }

    public void setExpandProgress(float expandProgress) {
        this.expandProgress = expandProgress;
        invalidateSelf();
    }

    @Override
    public int getIntrinsicWidth() {
        return (int) (radius * 0.8f);
    }

    @Override
    public int getIntrinsicHeight() {
        return (int) (radius * 0.8f);
    }

    @Override
    public void draw(@NotNull Canvas canvas) {
        if (expandProgress <= 0.5f) {
            //draw three line
//            drawTopLine(canvas, expandProgress);
//            drawMiddleLine(canvas, expandProgress);
//            drawBottomLine(canvas, expandProgress);

            Drawable d = ContextCompat.getDrawable(context, drawable);
            d.setBounds(getBounds().left, getBounds().top, getBounds().right, getBounds().bottom);
            d.draw(canvas);
        } else {
            // draw for cancel
            drawTopLeftLine(canvas, expandProgress);
            drawBottomLeftLine(canvas, expandProgress);
        }
    }

    private void drawBottomLeftLine(Canvas canvas, float progress) {
        int ly = (int) (getBounds().bottom - getIntrinsicHeight() * progress);
        int ry = (int) (getBounds().top + getIntrinsicHeight() * progress);
        canvas.drawLine(getBounds().left, ly, getBounds().right, ry, paint);
    }

    private void drawTopLeftLine(Canvas canvas, float progress) {
        int ry = (int) (getBounds().bottom - getIntrinsicHeight() * progress);
        int ly = (int) (getBounds().top + getIntrinsicHeight() * progress);
        canvas.drawLine(getBounds().left, ly, getBounds().right, ry, paint);
    }


    private void drawTopLine(Canvas canvas, float progress) {
        int y = getBounds().top + (int) (getIntrinsicHeight() * progress) + lineWidth;
        canvas.drawLine(getBounds().left, y, getBounds().left + getIntrinsicWidth(), y, paint);
    }

    private void drawMiddleLine(Canvas canvas, float progress) {
        int y = getBounds().top + getIntrinsicHeight() / 2;
        int len = getIntrinsicWidth() / 2;
        int centerX = getBounds().centerX();
        canvas.drawLine(centerX - len / 2, y, centerX + len / 2, y, paint);
    }

    private void drawBottomLine(Canvas canvas, float progress) {
        int y = getBounds().top + (int) (getIntrinsicHeight() * (1 - progress)) - lineWidth;
        int len = getIntrinsicWidth() / 4;
        int centerX = getBounds().centerX();
        canvas.drawLine(centerX - len / 2, y, centerX + len / 2, y, paint);
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        paint.setColorFilter(cf);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
