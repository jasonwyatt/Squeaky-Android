package co.jasonwyatt.squeakytodo;

import android.app.Application;

import co.jasonwyatt.squeaky.Database;

/**
 * @author jason
 */

public class App extends Application {
    private static App sInstance;
    private Database mDB;

    @Override
    public void onCreate() {
        super.onCreate();

        mDB = new Database(App.this, "todos");
        mDB.addTable(new Todo.Table());
        mDB.prepare();

        sInstance = this;
    }

    public Database getDB() {
        return mDB;
    }

    public static App getInstance() {
        return sInstance;
    }
}
