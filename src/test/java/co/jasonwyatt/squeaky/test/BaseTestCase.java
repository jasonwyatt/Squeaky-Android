package co.jasonwyatt.squeaky.test;

import android.test.AndroidTestCase;

import co.jasonwyatt.squeaky.Database;

/**
 * Created by jason on 2/25/15.
 */
public abstract class BaseTestCase extends AndroidTestCase {
    private Database mDatabase;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mDatabase = new Database(getContext(), getClass().getSimpleName());
    }

    public Database getDatabase() {
        return mDatabase;
    }
}
