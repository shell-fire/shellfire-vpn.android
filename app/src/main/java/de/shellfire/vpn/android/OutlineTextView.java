package de.shellfire.vpn.android;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.MaskFilter;
import android.graphics.Paint;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Pair;

import androidx.appcompat.widget.AppCompatTextView;

import java.util.ArrayList;
import java.util.Locale;
import java.util.WeakHashMap;

public class OutlineTextView extends AppCompatTextView {
    private final PorterDuffXfermode porterDuffXfermodeAtop = new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP);
    private final Rect rect = new Rect();
    private final PorterDuffXfermode porterDuffXfermodeOut = new PorterDuffXfermode(PorterDuff.Mode.DST_OUT);
    private ArrayList<Shadow> outerShadows;
    private ArrayList<Shadow> innerShadows;
    private float innerShadowRadius;
    private float outerShadowRadius;

    private WeakHashMap<String, Pair<Canvas, Bitmap>> canvasStore;

    private Canvas tempCanvas;
    private Bitmap tempBitmap;

    private Drawable foregroundDrawable;

    private float strokeWidth;
    private Integer strokeColor;
    private Join strokeJoin;
    private float strokeMiter;

    private int[] lockedCompoundPadding;
    private boolean frozen = false;
    private MaskFilter innerShadowBlurMaskFilter;

    public OutlineTextView(Context context) {
        super(context);
        init(null);
    }

    public OutlineTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public OutlineTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs);
    }

    public void init(AttributeSet attrs) {
        outerShadows = new ArrayList<>();
        innerShadows = new ArrayList<>();
        if (canvasStore == null) {
            canvasStore = new WeakHashMap<>();
        }

        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.OutlineTextView);

            String typefaceName = a.getString(R.styleable.OutlineTextView_typeface);
            if (typefaceName != null) {
                Typeface tf = Typeface.createFromAsset(getContext().getAssets(), String.format("fonts/%s.ttf", typefaceName));
                setTypeface(tf);
            }

            if (a.hasValue(R.styleable.OutlineTextView_foreground)) {
                Drawable foreground = a.getDrawable(R.styleable.OutlineTextView_foreground);
                if (foreground != null) {
                    this.setForegroundDrawable(foreground);
                } else {
                    this.setTextColor(a.getColor(R.styleable.OutlineTextView_foreground, 0xff000000));
                }
            }

            if (a.hasValue(R.styleable.OutlineTextView_mybackground)) {
                Drawable background = a.getDrawable(R.styleable.OutlineTextView_mybackground);
                if (background != null) {
                    this.setBackgroundDrawable(background);
                } else {
                    this.setBackgroundColor(a.getColor(R.styleable.OutlineTextView_mybackground, 0xff000000));
                }
            }

            if (a.hasValue(R.styleable.OutlineTextView_innerShadowColor)) {
                this.addInnerShadow(a.getFloat(R.styleable.OutlineTextView_innerShadowRadius, 0),
                        a.getFloat(R.styleable.OutlineTextView_innerShadowDx, 0),
                        a.getFloat(R.styleable.OutlineTextView_innerShadowDy, 0),
                        a.getColor(R.styleable.OutlineTextView_innerShadowColor, 0xff000000));
            }

            if (a.hasValue(R.styleable.OutlineTextView_outerShadowColor)) {
                this.addOuterShadow(a.getFloat(R.styleable.OutlineTextView_outerShadowRadius, 0),
                        a.getFloat(R.styleable.OutlineTextView_outerShadowDx, 0),
                        a.getFloat(R.styleable.OutlineTextView_outerShadowDy, 0),
                        a.getColor(R.styleable.OutlineTextView_outerShadowColor, 0xff000000));
            }

            innerShadowRadius = a.getFloat(R.styleable.OutlineTextView_innerShadowRadius, 0.0001f);
            outerShadowRadius = a.getFloat(R.styleable.OutlineTextView_outerShadowRadius, 0.0001f);
            innerShadowBlurMaskFilter = new BlurMaskFilter(innerShadowRadius, BlurMaskFilter.Blur.NORMAL);

            if (a.hasValue(R.styleable.OutlineTextView_strokeColor)) {
                float strokeWidth = a.getFloat(R.styleable.OutlineTextView_strokeWidthShellfire, 1);
                int strokeColor = a.getColor(R.styleable.OutlineTextView_strokeColor, 0xff000000);
                float strokeMiter = a.getFloat(R.styleable.OutlineTextView_strokeMiter, 10);
                Join strokeJoin = null;
                switch (a.getInt(R.styleable.OutlineTextView_strokeJoinStyle, 0)) {
                    case 0:
                        strokeJoin = Join.MITER;
                        break;
                    case 1:
                        strokeJoin = Join.BEVEL;
                        break;
                    case 2:
                        strokeJoin = Join.ROUND;
                        break;
                }
                this.setStroke(strokeWidth, strokeColor, strokeJoin, strokeMiter);
            }
            a.recycle();
        }
    }

    public void setStroke(float width, int color, Join join, float miter) {
        strokeWidth = width;
        strokeColor = color;
        strokeJoin = join;
        strokeMiter = miter;
        // requestLayout();
        invalidate();
    }

    public void setStroke(float width, int color) {
        setStroke(width, color, Join.MITER, 10);
    }

    public void addOuterShadow(float r, float dx, float dy, int color) {
        if (r == 0) {
            r = 0.0001f;
        }
        outerShadows.add(new Shadow(r, dx, dy, color));
        invalidate();
    }

    public void addInnerShadow(float r, float dx, float dy, int color) {
        if (r == 0) {
            r = 0.0001f;
        }
        innerShadows.add(new Shadow(r, dx, dy, color));
        invalidate();
    }

    public void clearInnerShadows() {
        innerShadows.clear();
        invalidate();
    }

    public void clearOuterShadows() {
        outerShadows.clear();
        invalidate();
    }

    public void setForegroundDrawable(Drawable d) {
        this.foregroundDrawable = d;
        invalidate();
    }

    public Drawable getForeground() {
        return this.foregroundDrawable == null ? new ColorDrawable(this.getCurrentTextColor()) : this.foregroundDrawable;
    }

    @Override
    public void onDraw(Canvas canvas) {
        freeze();
        Drawable restoreBackground = this.getBackground();
        Drawable[] restoreDrawables = this.getCompoundDrawables();
        int restoreColor = this.getCurrentTextColor();

        this.setCompoundDrawables(null, null, null, null);

        for (Shadow shadow : outerShadows) {
            this.setShadowLayer(shadow.r, shadow.dx, shadow.dy, shadow.color);
            super.onDraw(canvas);
        }
        this.setShadowLayer(0, 0, 0, 0);
        this.setTextColor(restoreColor);

        if (this.foregroundDrawable != null && this.foregroundDrawable instanceof BitmapDrawable) {
            generateTempCanvas();
            super.onDraw(tempCanvas);
            Paint paint = ((BitmapDrawable) this.foregroundDrawable).getPaint();
            paint.setXfermode(porterDuffXfermodeAtop);

            canvas.getClipBounds(this.rect);
            this.foregroundDrawable.setBounds(this.rect);
            this.foregroundDrawable.draw(tempCanvas);
            canvas.drawBitmap(tempBitmap, 0, 0, null);
            tempCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        }

        if (strokeColor != null) {
            TextPaint paint = this.getPaint();
            paint.setStyle(Style.STROKE);
            paint.setStrokeJoin(strokeJoin);
            paint.setStrokeMiter(strokeMiter);
            this.setTextColor(strokeColor);
            paint.setStrokeWidth(strokeWidth);
            super.onDraw(canvas);
            paint.setStyle(Style.FILL);
            this.setTextColor(restoreColor);
        }
        if (!innerShadows.isEmpty()) {
            generateTempCanvas();
            TextPaint paint = this.getPaint();
            for (Shadow shadow : innerShadows) {
                this.setTextColor(shadow.color);
                super.onDraw(tempCanvas);
                this.setTextColor(0xFF000000);
                paint.setXfermode(porterDuffXfermodeOut);
                paint.setMaskFilter(innerShadowBlurMaskFilter);

                tempCanvas.save();
                tempCanvas.translate(shadow.dx, shadow.dy);
                super.onDraw(tempCanvas);
                tempCanvas.restore();
                canvas.drawBitmap(tempBitmap, 0, 0, null);
                tempCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

                paint.setXfermode(null);
                paint.setMaskFilter(null);
                this.setTextColor(restoreColor);
                this.setShadowLayer(0, 0, 0, 0);
            }
        }

        if (restoreDrawables != null) {
            this.setCompoundDrawablesWithIntrinsicBounds(restoreDrawables[0], restoreDrawables[1], restoreDrawables[2], restoreDrawables[3]);
        }
        this.setBackgroundDrawable(restoreBackground);
        this.setTextColor(restoreColor);

        unfreeze();
        super.onDraw(canvas);
    }

    private void generateTempCanvas() {
        String key = String.format(Locale.getDefault(), "%dx%d", getWidth(), getHeight());
        Pair<Canvas, Bitmap> stored = canvasStore.get(key);

        if (stored != null) {
            tempCanvas = stored.first;
            tempBitmap = stored.second;
        } else {
            tempCanvas = new Canvas();
            tempBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
            tempCanvas.setBitmap(tempBitmap);
            canvasStore.put(key, new Pair<>(tempCanvas, tempBitmap));
        }
    }

    // Keep these things locked while onDraw in processing
    public void freeze() {
        lockedCompoundPadding = new int[]{
                getCompoundPaddingLeft(),
                getCompoundPaddingRight(),
                getCompoundPaddingTop(),
                getCompoundPaddingBottom()
        };
        frozen = true;
    }

    public void unfreeze() {
        frozen = false;
    }

    @Override
    public void requestLayout() {
        if (!frozen) super.requestLayout();
    }

    @Override
    public void postInvalidate() {
        if (!frozen) super.postInvalidate();
    }

    @Override
    public void postInvalidate(int left, int top, int right, int bottom) {
        if (!frozen) super.postInvalidate(left, top, right, bottom);
    }

    @Override
    public void invalidate() {
        if (!frozen) super.invalidate();
    }

    @Override
    public void invalidate(Rect rect) {
        if (!frozen) super.invalidate(rect);
    }

    @Override
    public void invalidate(int l, int t, int r, int b) {
        if (!frozen) super.invalidate(l, t, r, b);
    }

    @Override
    public int getCompoundPaddingLeft() {
        return !frozen ? super.getCompoundPaddingLeft() : lockedCompoundPadding[0];
    }

    @Override
    public int getCompoundPaddingRight() {
        return !frozen ? super.getCompoundPaddingRight() : lockedCompoundPadding[1];
    }

    @Override
    public int getCompoundPaddingTop() {
        return !frozen ? super.getCompoundPaddingTop() : lockedCompoundPadding[2];
    }

    @Override
    public int getCompoundPaddingBottom() {
        return !frozen ? super.getCompoundPaddingBottom() : lockedCompoundPadding[3];
    }

    public static class Shadow {
        final float r;
        final float dx;
        final float dy;
        final int color;

        public Shadow(float r, float dx, float dy, int color) {
            this.r = r;
            this.dx = dx;
            this.dy = dy;
            this.color = color;
        }
    }
}
