package com.tassadar.lorrismobile;

import android.content.Context;
import android.graphics.Rect;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.Toast;


public class TooltipLongClickListener implements OnLongClickListener {
    private static final int ESTIMATED_TOAST_HEIGHT_DIPS = 48;

    @Override
    public boolean onLongClick(View v) {
        CharSequence text = v.getContentDescription();
        if(TextUtils.isEmpty(text))
            return false;

        final int[] screenPos = new int[2]; // origin is device display
        final Rect displayFrame = new Rect(); // includes decorations (e.g. status bar)
        v.getLocationOnScreen(screenPos);
        v.getWindowVisibleDisplayFrame(displayFrame);
 
        final Context context = v.getContext();
        final int viewWidth = v.getWidth();
        final int viewHeight = v.getHeight();
        final int viewCenterX = screenPos[0] + viewWidth / 2;
        final int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        final int estimatedToastHeight = (int) (ESTIMATED_TOAST_HEIGHT_DIPS
                * context.getResources().getDisplayMetrics().density);
 
        Toast cheatSheet = Toast.makeText(context, text, Toast.LENGTH_SHORT);
        boolean showBelow = screenPos[1] < estimatedToastHeight;
        if (showBelow) {
            // Show below
            // Offsets are after decorations (e.g. status bar) are factored in
            cheatSheet.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL,
                    viewCenterX - screenWidth / 2,
                    screenPos[1] - displayFrame.top + viewHeight);
        } else {
            // Show above
            // Offsets are after decorations (e.g. status bar) are factored in
            cheatSheet.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL,
                    viewCenterX - screenWidth / 2,
                    displayFrame.bottom - screenPos[1]);
        }
 
        cheatSheet.show();
        return true;
    }

    public static TooltipLongClickListener get() {
        if(instance == null)
            instance = new TooltipLongClickListener();
        return instance;
    }

    private static TooltipLongClickListener instance = null;
}