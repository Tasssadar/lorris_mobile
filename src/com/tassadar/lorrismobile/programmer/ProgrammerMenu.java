package com.tassadar.lorrismobile.programmer;

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

public class ProgrammerMenu extends Fragment implements OnClickListener, android.content.DialogInterface.OnClickListener {

    public interface ProgrammerMenuListener {
        public void onProgTypeChanged(int type);
    }

    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.programmer_menu, container, false);

        View b = v.findViewById(R.id.prog_type);
        b.setOnClickListener(this);
        b.setOnLongClickListener(TooltipLongClickListener.get());

        return v;
    }

    @Override
    public void onStart() {
        super.onStart();

        setActiveProg(m_activeProg);
    }

    public void setListener(ProgrammerMenuListener listener) {
        m_listener = listener;
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.prog_type:
                if(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
                    showProgMenuLegacy();
                else
                    showProgMenuICS(v);
                break;
        }
    }

    public void setActiveProg(int type) {
        m_activeProg = type;

        View v = getView();
        if(v == null)
            return;

        Button b = (Button)v.findViewById(R.id.prog_type);
        b.setText(ProgrammerImpl.progTypeToName(type));
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        String[] types = getResources().getStringArray(R.array.programmer_types);
        if(which < 0 || which >= types.length)
            return;

        int type = ProgrammerImpl.progNameToType(types[which]);
        if(type != -1)
            m_listener.onProgTypeChanged(type);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void showProgMenuICS(View v) {
        PopupMenu popup = new PopupMenu(getActivity(), v);
        popup.setOnMenuItemClickListener(new PopupListener());

        Menu m = popup.getMenu();
        String[] types = getResources().getStringArray(R.array.programmer_types);
        for(String t : types)
            m.add(t);

        popup.show();
    }

    private void showProgMenuLegacy() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.prog_type)
            .setItems(R.array.programmer_types, this);
        builder.create().show();
    }

    private class PopupListener implements OnMenuItemClickListener {
        @Override
        public boolean onMenuItemClick(MenuItem it) {
            int type = ProgrammerImpl.progNameToType(it.getTitle().toString());
            if(type != -1)
                m_listener.onProgTypeChanged(type);

            return true;
        }
    }

    private ProgrammerMenuListener m_listener;
    private int m_activeProg;
}
