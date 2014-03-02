package com.tassadar.lorrismobile.joystick;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.tassadar.lorrismobile.Utils;


public class JoystickView extends SurfaceView implements SurfaceHolder.Callback, OnSeekBarChangeListener {

    public JoystickView(Context context) {
        super(context);
        init();
    }

    public JoystickView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
        init();
    }

    public JoystickView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        getHolder().addCallback(this);
        setFocusable(true);
        setFocusableInTouchMode(true);

        m_pointerId = -1;
        m_linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        m_linePaint.setColor(Color.RED);
        m_linePaint.setStyle(Style.STROKE);
        m_linePaint.setStrokeWidth(3);

        m_circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        m_circlePaint.setColor(Color.YELLOW);
        m_circlePaint.setStyle(Style.FILL);

        m_invertX = m_invertY = false;
        m_maxValue = 32767;
        m_axis3Value = 500;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(m_pointerId == -1) {
            if(event.getActionMasked() == MotionEvent.ACTION_DOWN)
                m_pointerId = event.getPointerId(event.getActionIndex());
            else
                return false;
        } else if(event.getPointerId(event.getActionIndex()) != m_pointerId)
            return false;

        SurfaceHolder holder = getHolder();
        Canvas c = holder.lockCanvas();
        if (c == null)
            return false;

        drawBase(c);
        if (event.getActionMasked() != MotionEvent.ACTION_UP)
            c.drawCircle(event.getX(), event.getY(), 40, m_circlePaint);
        holder.unlockCanvasAndPost(c);

        processMovement(event);
        return true;
    }

    private void drawBase(Canvas c) {
        c.drawColor(Color.BLACK);
        c.drawCircle(m_centerX, m_centerY, m_radius, m_linePaint);
        c.drawCircle(m_centerX, m_centerY, 10, m_circlePaint);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
            int height) {
        m_centerX = width/2;
        m_centerY = height/2;
        m_radius = Utils.min(m_centerX - 20, m_centerY - 20);

        Canvas c = holder.lockCanvas();
        if(c != null) {
            drawBase(c);
            holder.unlockCanvasAndPost(c);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) { }
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) { }

    private void processMovement(MotionEvent e) {
        if(e.getActionMasked() == MotionEvent.ACTION_UP) {
            m_listener.onValueChanged(0, 0);
            m_pointerId = -1;
            return;
        }

        int distX = (int)e.getX() - m_centerX;
        int distY = m_centerY - (int)e.getY(); // invert by default

        if(Math.abs(distX) > m_radius+20 || Math.abs(distY) > m_radius+20)
            return;

        if(m_invertX) distX = -distX;
        if(m_invertY) distY = -distY;

        distX = Utils.clamp(distX, m_radius);
        distY = Utils.clamp(distY, m_radius);

        distX = (((distX*1000)/m_radius)*m_maxValue)/1000;
        distY = (((distY*1000)/m_radius)*m_maxValue)/1000;

        m_listener.onValueChanged(distY, distX);
    }

    public void setListener(JoystickListener l) {
        m_listener = l;
    }

    public int getMaxValue() {
        return m_maxValue;
    }

    public void setMaxValue(int val) {
        m_maxValue = val;
    }

    public int getAxis3Value() {
        return m_axis3Value;
    }

    public boolean isInvertedX() { return m_invertX; }
    public boolean isInvertedY() { return m_invertY; }

    public void setInvertX(boolean invert) {
        m_invertX = invert;
    }

    public void setInvertY(boolean invert) {
        m_invertY = invert;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        //int val = ((progress-500)*m_maxValue)/1000;

        m_axis3Value = progress;
        int val = ((progress-500)*1000)/1000;
        m_listener.onAxis3Changed(val);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) { }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) { }

    private Paint m_linePaint;
    private Paint m_circlePaint;
    private int m_centerX, m_centerY;
    private int m_pointerId;
    private int m_radius;
    private JoystickListener m_listener;
    private boolean m_invertX;
    private boolean m_invertY;
    private int m_maxValue;
    private int m_axis3Value;
}
