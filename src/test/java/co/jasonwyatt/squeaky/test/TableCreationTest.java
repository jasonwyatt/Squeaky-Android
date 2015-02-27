package co.jasonwyatt.squeaky.test;

import android.database.Cursor;

import co.jasonwyatt.squeaky.Table;

/**
 * Created by jason on 2/25/15.
 */
public class TableCreationTest extends BaseTestCase {
    public void testCreateTable() throws Exception {
        TestTable t = new TestTable();
        getDatabase().addTable(t);
        assertEquals("getTables().size() should be 1", getDatabase().getTables().size(), 1);

        getDatabase().prepare();

        Cursor c = getDatabase().query("SELECT model, version FROM versions");
        assertEquals("There should be one record in `versions`", c.getCount(), 1);
        c.moveToNext();
        assertEquals(c.getString(0), t.getName());
        assertEquals(c.getInt(1), t.getVersion());
    }

    public static class TestTable extends Table {
        @Override
        public String getName() {
            return "test_table";
        }

        @Override
        public int getVersion() {
            return 1;
        }

        @Override
        public String[] getCreateTable() {
            return new String[] {
                    "CREATE TABLE test_table (col1 INTEGER NOT NULL, col2 INTEGER NOT NULL)"
            };
        }

        @Override
        public String[] getMigration(int versionA, int versionB) {
            return new String[0];
        }
    }
}
