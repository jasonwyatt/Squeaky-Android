package co.jasonwyatt.squeaky;

import android.database.sqlite.SQLiteDatabase;

/**
 * Created by jason on 2/25/15.
 */
public interface Migrator {
    public void onCreate(SQLiteDatabase db);
    public void onUpgrade(SQLiteDatabase db);
    public void onDowngrade(SQLiteDatabase db);
}
