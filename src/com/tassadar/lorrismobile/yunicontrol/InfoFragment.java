package com.tassadar.lorrismobile.yunicontrol;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.tassadar.lorrismobile.R;


public class InfoFragment extends YuniControlFragment {

    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.yunicontrol_base, container, false);

        m_adapter = new ArrayAdapter<CharSequence>(getActivity(), android.R.layout.simple_spinner_item);
        m_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        loadContentView(v);
        return v;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        loadContentView(getView());
    }

    private void loadContentView(View v) {
        Activity act = getActivity();
        if(v == null || act == null)
            return;

        int axes = m_axes.size();
        int btns = m_buttons.size();
        int tristate = m_tristate.size();

        LinearLayout base = (LinearLayout)v.findViewById(R.id.yunicontrol_info_base);
        base.removeAllViews();

        m_axes.clear();
        m_buttons.clear();
        m_tristate.clear();

        View content = View.inflate(act, R.layout.yunicontrol_info, null);
        base.addView(content);

        setAxisCount(axes);
        setButtonCount(btns);
        setTristateCount(tristate);

        TextView n = (TextView)v.findViewById(R.id.board_name);
        n.setText(m_boardName);

        View b = v.findViewById(R.id.yc_timeout_btn);
        b.setOnClickListener(m_onTryAgainClickListener);

        setItemsVisibility(m_itemsVisibility);
    }

    public void setAxisCount(int count)
    {
        View v = getView();
        Activity act = getActivity();
        if(act == null || v == null || m_axes.size() == count)
            return;

        int ax_size = m_axes.size();
        LinearLayout axis_layout = (LinearLayout)v.findViewById(R.id.bar_layout);

        while(ax_size != count)
        {
            if(ax_size < count)
            {
                RelativeLayout l = (RelativeLayout)act.getLayoutInflater()
                        .inflate(R.layout.axis_item, axis_layout, false);

                ((TextView)l.findViewById(R.id.bar_text)).
                setText(getResources().getString(R.string.pot) + " " + ax_size);

                axis_layout.addView(l);
                m_axes.add(l);
                ++ax_size;
            }
            else
            {
                --ax_size;
                axis_layout.removeView(m_axes.remove(ax_size));
            }
        }
    }

    public void setButtonCount(int count)
    {
        View v = getView();
        Activity act = getActivity();
        if(v == null || act == null || m_buttons.size() == count)
            return;

        int size = m_buttons.size();
        LinearLayout btn_layout = (LinearLayout)v.findViewById(R.id.button_layout);

        while(size != count)
        {
            if(size < count)
            {
                RelativeLayout l = (RelativeLayout)act.getLayoutInflater()
                        .inflate(R.layout.button_item, btn_layout, false);

                ((TextView)l.findViewById(R.id.button_text)).setText(String.valueOf(size));

                btn_layout.addView(l);
                m_buttons.add(l);
                ++size;
            }
            else
            {
                --size;
                btn_layout.removeView(m_buttons.remove(size));
            }
        }
    }

    public void setTristateCount(int count)
    {
        View v = getView();
        Activity act = getActivity();
        if(act == null || v == null || m_tristate.size() == count)
            return;

        int size = m_tristate.size();
        LinearLayout btn_layout = (LinearLayout)v.findViewById(R.id.tristate_layout);

        while(size != count)
        {
            if(size < count)
            {
                RelativeLayout l = (RelativeLayout)act.getLayoutInflater()
                        .inflate(R.layout.tristate_item, btn_layout, false);

                ((TextView)l.findViewById(R.id.tristate_text)).setText(String.valueOf(size));

                btn_layout.addView(l);
                m_tristate.add(l);
                ++size;
            }
            else
            {
                --size;
                btn_layout.removeView(m_tristate.remove(size));
            }
        }
    }

    private void setItemsVisibility(int state) {
        View v = getView();
        if(v == null)
            return;

        m_itemsVisibility = state;

        View p = v.findViewById(R.id.board_name);
        p.setVisibility(state == 0 ? View.VISIBLE : View.GONE);

        p = v.findViewById(R.id.yc_info_bar);
        p.setVisibility(state == 1 ? View.VISIBLE : View.GONE);

        p = v.findViewById(R.id.yc_timeout_btn);
        p.setVisibility(state == 2 ? View.VISIBLE : View.GONE);
        p = v.findViewById(R.id.yc_timeout_text);
        p.setVisibility(state == 2 ? View.VISIBLE : View.GONE);
    }

    private void setUpFromInfo(BoardInfo i) {
        View v = getView();
        if(v == null)
            return;

        TextView n = (TextView)v.findViewById(R.id.board_name);
        n.setText(i.name);
        m_boardName = i.name;

        setAxisCount(i.potCount);
        setButtonCount(i.btnCount);
        setTristateCount(i.triStateCount);
    }

    @Override
    public void onPacketReceived(Packet pkt) {
        switch(pkt.opcode) {
            case Protocol.CMSG_POT:
            {
                for(int i = 0; !pkt.atEnd() && i < m_axes.size(); ++i) {
                    RelativeLayout l = m_axes.get(i);
                    ProgressBar bar = (ProgressBar)l.findViewById(R.id.progress_bar);
                    bar.setProgress((short)pkt.read16() + 32768);
                }
                break;
            }
            case Protocol.CMSG_BUTTONS:
            {
                int itr = 0;
                while(!pkt.atEnd()) {
                    int s = pkt.read8();
                    for(int i = 0; i < 8 && i+itr < m_buttons.size(); ++i) {
                        RelativeLayout l = m_buttons.get(i+itr);
                        RadioButton btn = (RadioButton)l.findViewById(R.id.button_radio);
                        btn.setChecked((s & (1 << i)) != 0);
                    }
                    itr += 8;
                }
                break;
            }
            case Protocol.CMSG_TRISTATE:
            {
                for(int i = 0; !pkt.atEnd() && i < m_tristate.size(); ++i) {
                    RelativeLayout l = m_tristate.get(i);
                    Button btn = (Button)l.findViewById(R.id.tristate_button);
                    btn.setText(String.valueOf(pkt.read8()));
                }
                break;
            }
            case Protocol.CMSG_BOARD_VOLTAGE:
            {
                View v = getView();
                if(v == null)
                    break;
                TextView t = (TextView)v.findViewById(R.id.cur_voltage);
                t.setText(String.format("%.1f", (float)pkt.read16()/1000));
                break;
            }
        }
    }

    @Override
    public void onInfoRequested() {
        setAxisCount(0);
        setButtonCount(0);
        setTristateCount(0);
        setItemsVisibility(1);
    }

    @Override
    public void onInfoReceived(GlobalInfo i) {
        setItemsVisibility(i != null ? 0 : 2);
        if(i != null && i.boards.length != 0)
            setUpFromInfo(i.boards[0]);
    }

    @Override
    public void onBoardChange(BoardInfo b) {
        setUpFromInfo(b);
    }

    private final OnClickListener m_onTryAgainClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            m_protocol.requestGlobalInfo();
        }
    };

    private List<RelativeLayout> m_axes = new ArrayList<RelativeLayout>();
    private List<RelativeLayout> m_buttons = new ArrayList<RelativeLayout>();
    private List<RelativeLayout> m_tristate = new ArrayList<RelativeLayout>();
    private String m_boardName;
    private int m_itemsVisibility;

    private ArrayAdapter<CharSequence> m_adapter;
}
