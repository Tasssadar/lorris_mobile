package com.tassadar.lorrismobile.yunicontrol;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;

import com.tassadar.lorrismobile.R;
import com.tassadar.lorrismobile.TooltipLongClickListener;

public class YuniControlMenu extends Fragment implements OnClickListener, android.content.DialogInterface.OnClickListener {
    public interface YCMenuListener {
        void onBoardSelected(int idx);
    }

    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.yunicontrol_menu, container, false);

        m_boardBtn = (Button)v.findViewById(R.id.select_board);
        m_boardBtn.setOnClickListener(this);
        m_boardBtn.setOnLongClickListener(TooltipLongClickListener.get());
        m_boardBtn.setEnabled(false);
        return v;
    }

    public void setListener(YCMenuListener listener) {
        m_listener = listener;
    }

    @Override
    public void onClick(View v) {
        if(m_listener == null)
            return;

        switch(v.getId()) {
            case R.id.select_board:
                if(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
                    showBoardMenuLegacy();
                else
                    showBoardMenuICS(v);
                break;
        }
    }

    public void setInfo(GlobalInfo info) {
        m_info = info;
        m_boardBtn.setEnabled(info != null && m_info.boards.length > 0);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void showBoardMenuICS(View v) {
        PopupMenu popup = new PopupMenu(getActivity(), v);
        popup.setOnMenuItemClickListener(new PopupListener());

        Menu m = popup.getMenu();
        for(int i = 0; i < m_info.boards.length; ++i)
            m.add(m_info.boards[i].name);

        popup.show();
    }

    private void showBoardMenuLegacy() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.select_board);

        CharSequence items[] = new CharSequence[m_info.boards.length];
        for(int i = 0; i < m_info.boards.length; ++i)
            items[i] = m_info.boards[i].name;

        builder.setItems(items, this);
        builder.create().show();
    }

    private class PopupListener implements OnMenuItemClickListener {
        @Override
        public boolean onMenuItemClick(MenuItem it) {
            for(int i = 0; i < m_info.boards.length; ++i) {
                if(m_info.boards[i].name.equals(it.getTitle().toString())) {
                    m_listener.onBoardSelected(i);
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        m_listener.onBoardSelected(which);
    }

    private YCMenuListener m_listener;
    private Button m_boardBtn;
    private GlobalInfo m_info;
}
