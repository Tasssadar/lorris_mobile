package com.tassadar.lorrismobile.modules;

import jackpal.androidterm.emulatorview.ColorScheme;
import android.content.Context;
import android.content.SharedPreferences;

import com.tassadar.lorrismobile.BlobInputStream;
import com.tassadar.lorrismobile.BlobOutputStream;
import com.tassadar.lorrismobile.LorrisApplication;
import com.tassadar.lorrismobile.R;

public class TerminalSettings {
    public int fontSize;
    public int colors;
    public int enterKeyPress;
    public boolean hexMode;
    public boolean clearOnHex;
    public boolean hex16bytes;

    public void load(SharedPreferences p) {
        fontSize = p.getInt("term_fontSize", 13);
        colors = p.getInt("term_colors", 0);
        enterKeyPress = p.getInt("term_enterPress", 0);
        hexMode = false; // don't save this to preferences
        clearOnHex = p.getBoolean("term_clearOnHex", true);

        if(!p.contains("term_hex16bytes")) {
            Context ctx = LorrisApplication.getAppContext();
            if(ctx != null) {
                hex16bytes = ctx.getResources().getBoolean(R.bool.default_hex_16);
            } else
                hex16bytes = false;
        } else
            hex16bytes = p.getBoolean("term_hex16bytes", false);
    }

    public void save(SharedPreferences p) {
        SharedPreferences.Editor e = p.edit();
        e.putInt("term_fontSize", fontSize);
        e.putInt("term_colors", colors);
        e.putInt("term_enterPress", enterKeyPress);
        e.putBoolean("term_clearOnHex", clearOnHex);
        e.putBoolean("term_hex16bytes", hex16bytes);
        e.commit();
    }

    public void loadFromStr(BlobInputStream str) {
        fontSize = str.readInt("fontSize", fontSize);
        colors = str.readInt("colors", colors);
        enterKeyPress = str.readInt("enterPress", enterKeyPress);
        hexMode = str.readBool("hexMode", hexMode);
        clearOnHex = str.readBool("clearOnHex", clearOnHex);
        hex16bytes = str.readBool("hex16bytes", hex16bytes);
    }

    public void saveToStr(BlobOutputStream str) {
        str.writeInt("fontSize", fontSize);
        str.writeInt("colors", colors);
        str.writeInt("enterPress", enterKeyPress);
        str.writeBool("hexMode", hexMode);
        str.writeBool("clearOnHex", clearOnHex);
        str.writeBool("hex16bytes", hex16bytes);
    }

    public byte[] getEnterKeyPressSeq() {
        final byte[][] seq = { 
            { 0xD, 0xA }, // 0: \r\n
            { 0xA },      // 1: \n
            { 0xD },      // 2: \r
            { 0xA, 0xD }, // 3: \n\r
        };

        if(enterKeyPress >= 0 && enterKeyPress < seq.length) 
            return seq[enterKeyPress];
        return seq[0];
    }

    public ColorScheme getColorScheme() {
        final int[][] color_vals = {
            { 0xFFFFFFFF, 0xFF000000 }, // 0: Black+white
            { 0xFF000000, 0xFFFFFFFF }, // 1: White+black
            { 0xFFFFFFFF, 0xFF0000FF }, // 2: Blue+white
            { 0xFF00FF00, 0xFF000000 }, // 3: Black+green
        };

        if(colors >= 0 && colors < color_vals.length) 
            return new ColorScheme(color_vals[colors]);
        return new ColorScheme(color_vals[0]);
    }
}