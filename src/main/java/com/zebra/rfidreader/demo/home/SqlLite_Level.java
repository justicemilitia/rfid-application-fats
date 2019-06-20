package com.zebra.rfidreader.demo.home;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SqlLite_Level extends SQLiteOpenHelper {

    public SqlLite_Level(Context c){
        super(c,"dbfatssettings",null,1);
    }

    public void onCreate (SQLiteDatabase db)
    {

        String sql = "create table settings (SettingsId integer primary key autoincrement"+
        ", SQLPath text)";

        db.execSQL(sql);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int eski, int yeni) {

        db.execSQL("drop table if exists settings");

    }


}
