package com.bandcamp.squeaky.test;

import android.database.Cursor;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

import com.bandcamp.squeaky.Database;
import com.bandcamp.squeaky.DatabaseException;

@RunWith(AndroidJUnit4.class)
public class VersionsTableTest {
    private Database mDatabase;

    @Test
    public void prepareTest() throws Exception {
        mDatabase = new Database(InstrumentationRegistry.getContext(), getClass().getSimpleName());
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
        mDatabase = new Database(InstrumentationRegistry.getContext(), getClass().getSimpleName());
        mDatabase.prepare();
        assertThat(mDatabase.isPrepared()).isTrue();

        Cursor c = mDatabase.query("SELECT * FROM versions");
        assertThat(c.getCount()).isEqualTo(0);
    }
}