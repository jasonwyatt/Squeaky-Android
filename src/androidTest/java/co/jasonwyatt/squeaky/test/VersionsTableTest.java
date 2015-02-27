package co.jasonwyatt.squeaky.test;

import android.database.Cursor;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

import co.jasonwyatt.squeaky.Database;
import co.jasonwyatt.squeaky.DatabaseException;

@RunWith(RobolectricTestRunner.class)
public class VersionsTableTest {
    private Database mDatabase;

    @Test
    public void prepareTest() throws Exception {
        mDatabase = new Database(Robolectric.application, getClass().getSimpleName());
        assertThat(mDatabase.isPrepared()).isFalse();
        mDatabase.prepare();
        assertThat(mDatabase.isPrepared()).isTrue();

        try {
            mDatabase.prepare();
            failBecauseExceptionWasNotThrown(DatabaseException.class);
        } catch (DatabaseException e) {
        }

        assertThat(mDatabase.getWritableDB()).isNotNull();
        assertThat(mDatabase.getReadableDB()).isNotNull();
    }

    @Test
    public void testEmptyVersionsTable() throws Exception {
        mDatabase = new Database(Robolectric.application, getClass().getSimpleName());
        mDatabase.prepare();
        assertThat(mDatabase.isPrepared()).isTrue();

        Cursor c = mDatabase.query("SELECT * FROM versions");
        assertThat(c.getCount()).isEqualTo(0);
    }
}