package com.tassadar.lorrismobile;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.HorizontalScrollView;


public class SwipableHorizontalScrollView extends HorizontalScrollView {

    public SwipableHorizontalScrollView(Context context) {
        super(context);
    }

    public SwipableHorizontalScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SwipableHorizontalScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_MOVE && getParent() != null)
            getParent().requestDisallowInterceptTouchEvent(true);

        return super.onTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true;
    }
}
