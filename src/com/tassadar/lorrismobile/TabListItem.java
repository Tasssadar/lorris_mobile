package com.tassadar.lorrismobile;

import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

public class TabListItem {
    public interface FileItemClicked {
        void onFileItemChecked(String name, boolean is_folder, boolean checked);
    }
    
    public TabListItem(WorkspaceActivity activity, ViewGroup parent, String tab_name) {
        m_view = View.inflate(activity, R.layout.tab_list_item, parent);
        TextView t = (TextView)m_view.findViewById(R.id.tab_name);
        t.setText(tab_name);
    }
    
    public View getView() {
        return m_view;
    }
    
    private class BoxChangeListener implements OnCheckedChangeListener {
        @Override
        public void onCheckedChanged(CompoundButton btn, boolean checked) {
        }
    }
    
    private class TextClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {
        }
    }
    
    private View m_view;
    private String m_file_name;
    private FileItemClicked m_listener;
}