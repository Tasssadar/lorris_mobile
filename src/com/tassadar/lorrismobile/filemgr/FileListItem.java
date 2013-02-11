package com.tassadar.lorrismobile.filemgr;

import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tassadar.lorrismobile.R;

public class FileListItem implements OnClickListener {
    public interface FileItemClicked {
        void onFileItemSelected(String name, boolean is_folder);
    }

    public FileListItem(FileManagerActivity activity, ViewGroup parent, String file_name, boolean is_folder) {
        m_view = View.inflate(activity, R.layout.file_list_item, parent);

        m_file_name = file_name;
        m_is_folder = is_folder;
        m_listener = activity;

        int icon = is_folder ? R.drawable.collections_collection : R.drawable.collections_view_as_list;

        TextView t = (TextView)m_view.findViewById(R.id.file_name);
        t.setCompoundDrawablesWithIntrinsicBounds(icon, 0, 0, 0);
        t.setText(file_name);

        m_view.setOnClickListener(this);
    }

    public View getView() {
        return m_view;
    }

    @Override
    public void onClick(View v) {
        m_listener.onFileItemSelected(m_file_name, m_is_folder);
    }

    private View m_view;
    private String m_file_name;
    private FileItemClicked m_listener;
    private boolean m_is_folder;
}