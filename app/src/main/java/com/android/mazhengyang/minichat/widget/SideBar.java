package com.android.mazhengyang.minichat.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

public class SideBar extends View {

    private static final String TAG = "MiniChat." + SideBar.class.getSimpleName();

    private static final int TEXTSIZE = 30;
    //    // 26个字母
//    public static String[] b = {"A", "B", "C", "D", "E", "F", "G", "H", "I",
//            "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V",
//            "W", "X", "Y", "Z", "#"};
    public String[] sortLetters;
    private int highlightIndex = -1;
    private Paint paint;
    private TextView letterToastView;

    private OnTouchingLetterChangedListener onTouchingLetterChangedListener;

    public SideBar(Context context) {
        super(context);
        Log.d(TAG, "SideBar: context");
        init();
    }

    public SideBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        Log.d(TAG, "SideBar: context attrs");
        init();
    }

    public SideBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        Log.d(TAG, "SideBar: context attrs defStyleAttr");
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setTextSize(TEXTSIZE);
    }

    public void setOnTouchingLetterChangedListener(
            OnTouchingLetterChangedListener onTouchingLetterChangedListener) {
        this.onTouchingLetterChangedListener = onTouchingLetterChangedListener;
    }

    public interface OnTouchingLetterChangedListener {
        void onTouchingLetterChanged(String s);
    }

    public void setLetterToastView(TextView letterToastView) {
        this.letterToastView = letterToastView;
    }

    public void setSortLetters(String[] sortLetters) {
        if (sortLetters == null || sortLetters.length < 0) {
            return;
        }
        this.sortLetters = sortLetters;
        invalidate();
    }

    /**
     * @param widthMeasureSpec
     * @param heightMeasureSpec
     */
    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int dw = 0;
        int dh = 0;

        dw += (getPaddingLeft() + getPaddingRight());
        dh += (getPaddingTop() + getPaddingBottom());

        float space = dip2px(getContext(), 15);

        if (sortLetters != null) {
            for (int i = 0; i < sortLetters.length; i++) {
                float textHeight = paint.measureText(sortLetters[i]) + space;
                dh += textHeight;
            }
        }

        final int measuredWidth = resolveSizeAndState(dw, widthMeasureSpec, 0);
        final int measuredHeight = resolveSizeAndState(dh, heightMeasureSpec, 0);
        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    /**
     * @param canvas
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (sortLetters == null || sortLetters.length < 0) {
            return;
        }
        int width = getWidth();
        int height = getHeight();
        Log.d(TAG, "onDraw: width=" + width + ", height=" + height);
        for (int i = 0; i < sortLetters.length; i++) {
            int singleHeight = height / sortLetters.length;
            paint.setColor(Color.BLACK);
            paint.setAntiAlias(true);
            paint.setTextSize(TEXTSIZE);
            float textHeight = paint.measureText(sortLetters[i]);
            if (i == highlightIndex) {
                paint.setColor(Color.GREEN);
                paint.setFakeBoldText(true);
            }
            float xPos = width / 2 - textHeight / 2;
            float yPos = singleHeight * i + singleHeight;
            canvas.drawText(sortLetters[i], xPos, yPos, paint);
            paint.reset();
        }

    }

    /**
     * @param event
     * @return
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {

        if (sortLetters == null || sortLetters.length <= 0) {
            return false;
        }

        int action = event.getAction();
        float y = event.getY();
        int old = highlightIndex;
        int index = (int) (y / getHeight() * sortLetters.length);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (letterToastView != null) {
                    letterToastView.setText(sortLetters[index]);
                    letterToastView.setVisibility(View.VISIBLE);
                }
                if (onTouchingLetterChangedListener != null) {
                    onTouchingLetterChangedListener.onTouchingLetterChanged(sortLetters[index]);
                }
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                highlightIndex = -1;
                if (letterToastView != null) {
                    letterToastView.setVisibility(View.INVISIBLE);
                }
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                if (old != index) {
                    if (index >= 0 && index < sortLetters.length) {
                        if (letterToastView != null) {
                            letterToastView.setText(sortLetters[index]);
                        }
                        highlightIndex = index;
                        invalidate();
                        if (onTouchingLetterChangedListener != null) {
                            onTouchingLetterChangedListener.onTouchingLetterChanged(sortLetters[index]);
                        }
                    }
                }
                break;
        }
        return true;
    }

    /**
     * @param context
     * @param dpValue
     * @return
     */
    public static float dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return dpValue * scale + 0.5f;
    }

}