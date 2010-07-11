/*
 * Copyright (C) 2009-2010 Aubort Jean-Baptiste (Rorist)
 * Licensed under GNU's GPL 2, see README
 */

// http://standards.ieee.org/regauth/oui/oui.txt

package info.lamatricexiste.network.Network;

import info.lamatricexiste.network.R;
import info.lamatricexiste.network.Utils.Prefs;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteDatabaseCorruptException;
import android.preference.PreferenceManager;
import android.util.Log;

public class HardwareAddress {

    private final String TAG = "HardwareAddress";
    private final String DB_PATH = "/data/data/info.lamatricexiste.network/";
    private final String DB_NAME = "oui.db";
    private SQLiteDatabase db = null;
    private WeakReference<Activity> mActivity;

    public HardwareAddress(Activity activity) {
        mActivity = new WeakReference<Activity>(activity);
        try {
            db = SQLiteDatabase.openDatabase(DB_PATH + DB_NAME, null,
                    SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        } catch (SQLiteException e) {
            Log.e(TAG, e.getMessage());
            final Activity d = mActivity.get();
            if (d != null) {
                Context ctxt = d.getApplicationContext();
                Editor edit = PreferenceManager.getDefaultSharedPreferences(ctxt).edit();
                edit.putInt(Prefs.KEY_RESET_NICDB, 1);
                edit.commit();
            }
        }
    }

    public void dbClose() {
        if (db != null) {
            db.close();
        }
    }

    public String getHardwareAddress(String ip) {
        // Get intf
        String intf = "(tiwlan0|eth0)";
        final Activity d = mActivity.get();
        if (d != null) {
            NetInfo net = new NetInfo(d.getApplicationContext());
            intf = net.intf;
        }
        // Get HW Addr
        String hw = "00:00:00:00:00:00";
        try {
            FileReader fileReader = new FileReader("/proc/net/arp");
            String ptrn = "^" + ip.replace(".", "\\.")
                    + "\\s+0x1\\s+0x2\\s+([:0-9a-fA-F]+)\\s+\\*\\s+" + intf + "$";
            Pattern pattern = Pattern.compile(ptrn);
            BufferedReader bufferedReader = new BufferedReader(fileReader, 2);
            String line;
            Matcher matcher;
            while ((line = bufferedReader.readLine()) != null) {
                matcher = pattern.matcher(line);
                if (matcher.matches()) {
                    hw = matcher.group(1);
                }
            }
            bufferedReader.close();
            fileReader.close();
        } catch (IOException e) {
            Log.d(TAG, "Can't open/read file ARP: " + e.getMessage());
        }
        return hw;
    }

    public String getNicVendor(String hw) throws SQLiteDatabaseCorruptException {
        final Activity a = mActivity.get();
        if (a != null) {
            final Context ctxt = a.getApplicationContext();
            String ni = ctxt.getString(R.string.info_unknown);
            if (db != null) {
                String macid = hw.replace(":", "").substring(0, 6).toUpperCase();
                // Db request
                try {
                    Cursor c = db
                            .rawQuery("select vendor from oui where mac='" + macid + "'", null);
                    if (c.getCount() > 0) {
                        c.moveToFirst();
                        ni = c.getString(c.getColumnIndex("vendor"));
                    }
                    c.close();
                } catch (SQLiteException e) {
                    Log.e(TAG, e.getMessage());
                    Editor edit = PreferenceManager.getDefaultSharedPreferences(ctxt).edit();
                    edit.putInt(Prefs.KEY_RESET_NICDB, 1);
                    edit.commit();
                }
            }
            return ni;
        }
        return "Unknown";
    }
}
