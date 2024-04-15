package com.lifengqiang.videoeditor.ui.mediatrackeditor.video;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.OverScroller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class MyHorizontalScrollView extends ViewGroup {
    private final int imageWidth, imageHeight;
    private int maxWidth = 0;
    private int paddingWidth = 0;
    private int scrollOffsetX = 0;
    private final OverScroller scroller;
    private OnScrollChangedListener onScrollChangedListener;

    public MyHorizontalScrollView(@NonNull Context context) {
        this(context, null);
    }

    public MyHorizontalScrollView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        float density = context.getResources().getDisplayMetrics().density;
        imageWidth = (int) (density * 80);
        imageHeight = (int) (density * 64);
        scroller = new OverScroller(context);
    }

    public void addVideoSection(VideoSectionData data) {
        for (String slice : data.getSlices()) {
            Drawable drawable = BitmapDrawable.createFromPath(slice);
            ImageView image = new ImageView(getContext());
            image.setImageDrawable(drawable);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(imageWidth, imageHeight);
            addView(image, params);
            maxWidth += imageWidth;
        }
    }

    public void scrollOffsetX(int value) {
        if (setScrollOffsetX(value)) {
            performScrollChangedListener(false);
            requestLayout();
        }
    }

    public void scrollOffsetX(float ratio) {
        scrollOffsetX((int) (maxWidth * ratio));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        paddingWidth = MeasureSpec.getSize(widthMeasureSpec) / 2;
        for (int i = 0; i < getChildCount(); i++) {
            View view = getChildAt(i);
            LayoutParams params = view.getLayoutParams();
            int width = MeasureSpec.makeMeasureSpec(params.width, MeasureSpec.EXACTLY);
            int height = MeasureSpec.makeMeasureSpec(params.height, MeasureSpec.EXACTLY);
            view.measure(width, height);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int offsetX = paddingWidth - scrollOffsetX;
        for (int i = 0; i < getChildCount(); i++) {
            View view = getChildAt(i);
            int width = MeasureSpec.getSize(view.getMeasuredWidth());
            int height = MeasureSpec.getSize(view.getMeasuredHeight());
            view.layout(offsetX, 0, offsetX + width, height);
            offsetX += width;
        }
    }

    private float previousX = 0;
    private float ppreviousX = 0;
    private long touchTime = 0;

    private long speedUpTimeNa = 0;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float x = e.getX();
        float diff;
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                previousX = x;
                ppreviousX = x;
                if (!scroller.isFinished()) {
                    scroller.forceFinished(true);
                    removeCallbacks(animationRunnable);
                } else {
                    performScrollStateChangedListener(true);
                }
                touchTime = System.currentTimeMillis();
                speedUpTimeNa = System.nanoTime();
                break;
            case MotionEvent.ACTION_MOVE:
                long currentTime = System.currentTimeMillis();
                if (currentTime - touchTime > 100) {
                    ppreviousX = previousX;
                    speedUpTimeNa = System.nanoTime();
                }
                touchTime = System.currentTimeMillis();
                diff = x - previousX;
                previousX = x;
                if (setScrollOffsetX((int) (scrollOffsetX - diff))) {
                    requestLayout();
                    performScrollChangedListener(true);
                }
                break;
            case MotionEvent.ACTION_UP:
                if (System.currentTimeMillis() - touchTime > 10) {
                    performScrollStateChangedListener(false);
                    break;
                }
                diff = ppreviousX - x;
                if (Math.abs(diff) < 10) {
                    performScrollStateChangedListener(false);
                    break;
                }
                long time = System.nanoTime() - speedUpTimeNa;
                double ratio = (double) time / 1000000000.0;
                int velocityX = (int) ((double) diff / ratio);
                scroller.fling(scrollOffsetX, 0,
                        velocityX, 0,
                        0, maxWidth,
                        0, 0);
                post(animationRunnable);
                break;
        }
        return true;
    }

    private final Runnable animationRunnable = new Runnable() {
        @Override
        public void run() {
            if (scroller.computeScrollOffset()) {
                if (setScrollOffsetX(scroller.getCurrX())) {
                    requestLayout();
                    performScrollChangedListener(true);
                }
                postDelayed(this, 16);
            } else {
                performScrollStateChangedListener(false);
            }
        }
    };

    private boolean setScrollOffsetX(int value) {
        if (value == scrollOffsetX) return false;
        if (value < 0) {
            if (scrollOffsetX == 0) return false;
            scrollOffsetX = 0;
        } else if (value > maxWidth) {
            if (scrollOffsetX == maxWidth) return false;
            scrollOffsetX = maxWidth;
        } else
            scrollOffsetX = value;
        return true;
    }

    private void performScrollChangedListener(boolean isUser) {
        if (onScrollChangedListener != null) {
            onScrollChangedListener.onScrollChanged(this, isUser, scrollOffsetX);
        }
    }

    private void performScrollStateChangedListener(boolean running) {
        if (onScrollChangedListener != null) {
            onScrollChangedListener.onScrollStateChanged(this, running);
        }
    }

    public int getMaxContentWidth() {
        return maxWidth;
    }

    public int getImageWidth() {
        return imageWidth;
    }

    public int getImageHeight() {
        return imageHeight;
    }

    public int getScrollOffsetX() {
        return scrollOffsetX;
    }

    public void setOnScrollChangedListener(OnScrollChangedListener onScrollChangedListener) {
        this.onScrollChangedListener = onScrollChangedListener;
    }

    public interface OnScrollChangedListener {
        void onScrollStateChanged(MyHorizontalScrollView view, boolean running);

        void onScrollChanged(MyHorizontalScrollView view, boolean isUser, int dx);
    }
}
