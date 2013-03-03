package com.tassadar.lorrismobile;

import android.view.MotionEvent;

public class DoubleSwipeGesture {
    
    public interface DoubleSwipeListener {
        public void onDoubleSwipeLeft();
        public void onDoubleSwipeRight();
        public void onDoubleSwipeUp();
        public void onDoubleSwipeDown();
    }

    private static final int ST_NONE      = 0;
    private static final int ST_PROGRESS  = 1;
    private static final int ST_INVALID   = 2;

    public DoubleSwipeGesture(DoubleSwipeListener l) {
        m_state = ST_NONE;
        m_listener = l;
        m_pointers = new Pointer[] { new Pointer(), new Pointer() };
    }

    public void onTouchEvent(MotionEvent e) {
        switch(e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                m_state = ST_NONE;
                m_pointers[0].id = e.getPointerId(0);
                m_pointers[0].startx = e.getX();
                m_pointers[0].starty = e.getY();
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
            {
                if(m_state != ST_NONE)
                    break;

                m_state = ST_PROGRESS;

                int idx = e.getActionIndex();
                m_pointers[1].id = e.getPointerId(idx);
                m_pointers[1].startx = e.getX(idx);
                m_pointers[1].starty = e.getY(idx);
                break;
            }
            case MotionEvent.ACTION_POINTER_UP:
            {
                if(m_state != ST_PROGRESS)
                    break;

                for(Pointer p : m_pointers) {
                    int idx = e.findPointerIndex(p.id);
                    p.x = e.getX(idx);
                    p.y = e.getY(idx);
                }

                Pointer p1 = m_pointers[0];
                Pointer p2 = m_pointers[1];

                boolean hor1 = Math.abs(p1.startx - p1.x) > Math.abs(p1.starty - p1.y);
                boolean hor2 = Math.abs(p2.startx - p2.x) > Math.abs(p2.starty - p2.y);
                
                if(hor1 != hor2) {
                    m_state = ST_INVALID;
                    break;
                }

                // Horizontal
                if(hor1) {
                    boolean left1 = p1.startx > p1.x;
                    boolean left2 = p2.startx > p2.x;

                    if(left1 != left2) {
                        m_state = ST_INVALID;
                        break;
                    }

                    if(left1) m_listener.onDoubleSwipeLeft();
                    else      m_listener.onDoubleSwipeRight();
                }
                else // Vertical
                {
                    boolean up1 = p1.starty > p1.y;
                    boolean up2 = p2.starty > p2.y;

                    if(up1 != up2) {
                        m_state = ST_INVALID;
                        break;
                    }

                    if(up1) m_listener.onDoubleSwipeUp();
                    else    m_listener.onDoubleSwipeDown();
                }
                m_state = ST_INVALID;
                break;
            }
        }
    }

    private class Pointer {
        public int id;
        public float startx, starty;
        public float x, y;
    }

    private int m_state;
    private Pointer[] m_pointers;
    private DoubleSwipeListener m_listener;
}