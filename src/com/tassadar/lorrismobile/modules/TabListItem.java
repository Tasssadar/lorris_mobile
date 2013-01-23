package com.tassadar.lorrismobile.modules;

import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tassadar.lorrismobile.R;
import com.tassadar.lorrismobile.WorkspaceActivity;

public class TabListItem {
    public interface TabItemClicked {
        void onTabItemClicked();
    }
    
    public TabListItem(WorkspaceActivity activity, ViewGroup parent, String tab_name) {
        m_view = View.inflate(activity, R.layout.tab_list_item, parent);
        TextView t = (TextView)m_view.findViewById(R.id.tab_name);
        t.setText(tab_name);

        LinearLayout l = (LinearLayout)m_view.findViewById(R.id.tab_item_layout);
        l.setOnClickListener(new ClickListener());
    }
    
    public View getView() {
        return m_view;
    }

    public void setActive(boolean active) {
        LinearLayout l = (LinearLayout)m_view.findViewById(R.id.tab_item_layout);

        if(active)
            l.setBackgroundResource(R.drawable.list_selected);
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

    private View m_view;
    private TabItemClicked m_listener;
}
