package co.jasonwyatt.squeaky;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by jason on 2/25/15.
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    private final Migrator mMigrator;

    public DatabaseHelper(Context context, String name, int version, Migrator m) {
        super(context, name, null, version);
        mMigrator = m;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        mMigrator.onCreate(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        mMigrator.onUpgrade(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        mMigrator.onDowngrade(db);
    }
}
