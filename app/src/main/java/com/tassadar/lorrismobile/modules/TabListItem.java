package com.tassadar.lorrismobile.modules;

import java.lang.ref.WeakReference;

import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.tassadar.lorrismobile.R;
import com.tassadar.lorrismobile.WorkspaceActivity;

public class TabListItem {
    public interface TabItemClicked {
        void onTabItemClicked();
        void onTabCloseRequested();
    }
    
    public TabListItem(WorkspaceActivity activity, ViewGroup parent, String tab_name) {
        m_view = View.inflate(activity, R.layout.tab_list_item, parent);
        TextView t = (TextView)m_view.findViewById(R.id.tab_name);
        t.setText(tab_name);
        
        ImageButton b = (ImageButton)m_view.findViewById(R.id.close_btn);
        b.setOnClickListener(new CloseBtnClickListener());

        ViewGroup l = (ViewGroup)m_view.findViewById(R.id.tab_item_layout);
        l.setOnClickListener(new ClickListener());

        m_closeBtnEnlarged = false;
    }
    
    public View getView() {
        return m_view;
    }

    public void setActive(boolean active) {
        ViewGroup l = (ViewGroup)m_view.findViewById(R.id.tab_item_layout);

        if(active)
            l.setBackgroundResource(R.drawable.dark_blue);
        else
            l.setBackgroundResource(R.drawable.transparent_btn);
    }

    public void setOnClickListener(TabItemClicked listener) {
        m_listener = listener;
    }

    private class ClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            if(m_listener != null)
                m_listener.onTabItemClicked();
        }
    }
    
    private class CloseBtnClickListener implements OnClickListener {
        @Override
        public void onClick(View btnView) {
            if(m_closeBtnEnlarged && m_listener != null) {
                m_listener.onTabCloseRequested();
                return;
            }
            
            ImageButton b = (ImageButton)btnView;
            ButtonShrinkRunnable r = new ButtonShrinkRunnable(b);

            b.setMinimumWidth(b.getWidth()+10);
            b.setMinimumHeight(b.getHeight()+10);
            b.requestLayout();

            b.postDelayed(r,  2000);
            m_closeBtnEnlarged = true;
        }
        
    }

    private class ButtonShrinkRunnable implements Runnable {
        private int m_origSize;
        private WeakReference<ImageButton> m_btn;

        public ButtonShrinkRunnable(ImageButton btn) {
            m_btn = new WeakReference<ImageButton>(btn);
            m_origSize = btn.getWidth();
        }

        @Override
        public void run() {
            m_closeBtnEnlarged = false;
            ImageButton b = m_btn.get();
            if(b != null) {
                b.setMinimumWidth(m_origSize);
                b.setMinimumHeight(m_origSize);
                b.requestLayout();
            }
        }
    }

    private View m_view;
    private TabItemClicked m_listener;
    private boolean m_closeBtnEnlarged;
}
