package de.shellfire.vpn.android;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.viewpager.widget.ViewPager;

/**
 * Disallow swipping
 */

public class SelectionViewPager extends ViewPager {


    public SelectionViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);

    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return false;
    }

}
