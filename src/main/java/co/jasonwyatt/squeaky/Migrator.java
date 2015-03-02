package co.jasonwyatt.squeaky;

import android.database.sqlite.SQLiteDatabase;

interface Migrator {
    public void onCreate(SQLiteDatabase db);
    public void onUpgrade(SQLiteDatabase db);
    public void onDowngrade(SQLiteDatabase db);
}
