package co.jasonwyatt.squeaky.test;

import android.database.Cursor;
import android.test.AndroidTestCase;

import co.jasonwyatt.squeaky.Database;
import co.jasonwyatt.squeaky.DatabaseException;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class VersionsTableTest extends AndroidTestCase {
    private Database mDatabase;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mDatabase = new Database(getContext(), getClass().getSimpleName());
    }

    public void testPrepare() throws Exception {
        assertFalse("isPrepared() should return false before preparation", mDatabase.isPrepared());
        mDatabase.prepare();
        assertTrue("isPrepared() should return true after preparation", mDatabase.isPrepared());

        try {
            mDatabase.prepare();
        } catch (DatabaseException e) {
            assertNotNull("prepare() should throw an exception if called after the database is already prepared", e);
        }
    }

    public void testEmptyVersionsTable() throws Exception {
        mDatabase.prepare();
        assertTrue("isPrepared() should return true after preparation", mDatabase.isPrepared());

        Cursor c = mDatabase.query("SELECT * FROM versions");
        assertEquals("No rows should be returned.", c.getCount(), 0);
    }
}