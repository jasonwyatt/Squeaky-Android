package co.jasonwyatt.squeaky;

import android.database.Cursor;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author jason
 */
@RunWith(AndroidJUnit4.class)
public class QueryTest {
    private Database db;

    @Before
    public void setUp() {
        db = new Database(InstrumentationRegistry.getContext(), getClass().getSimpleName());
        db.addTable(new TestTable());
        db.prepare();
    }

    @After
    public void tearDown() {
        db.update("DROP TABLE test");
        db.update("DROP TABLE versions");
    }

    @Test
    public void select_bindArgs_types_are_inferred() {
        assertQueryCount(1, "SELECT * FROM test WHERE a = ?", 1);
        assertQueryCount(1, "SELECT * FROM test WHERE b = ?", 1);
        assertQueryCount(1, "SELECT * FROM test WHERE c = ?", 1);
        assertQueryCount(1, "SELECT * FROM test WHERE d = ?", 1);

        assertQueryCount(1, "SELECT * FROM test WHERE a = ?", '1');
        assertQueryCount(1, "SELECT * FROM test WHERE b = ?", '1');
        assertQueryCount(1, "SELECT * FROM test WHERE c = ?", '1');
        assertQueryCount(1, "SELECT * FROM test WHERE d = ?", '1');

        assertQueryCount(1, "SELECT * FROM test WHERE a = ?", "1");
        assertQueryCount(1, "SELECT * FROM test WHERE b = ?", "1");
        assertQueryCount(1, "SELECT * FROM test WHERE c = ?", "1");
        assertQueryCount(1, "SELECT * FROM test WHERE d = ?", "1");

        assertQueryCount(1, "SELECT * FROM test WHERE a = ?", 1.0);
        assertQueryCount(1, "SELECT * FROM test WHERE b = ?", 1.0);
        assertQueryCount(0, "SELECT * FROM test WHERE c = ?", 1.0); // 0 because "1" != "1.0"
        assertQueryCount(1, "SELECT * FROM test WHERE d = ?", 1.0);

        assertQueryCount(1, "SELECT * FROM test WHERE a = ?", "1.0");
        assertQueryCount(1, "SELECT * FROM test WHERE b = ?", "1.0");
        assertQueryCount(0, "SELECT * FROM test WHERE c = ?", "1.0"); // 0 because "1" != "1.0"
        assertQueryCount(1, "SELECT * FROM test WHERE d = ?", "1.0");
    }

    @Test
    public void insert_bindArgs_types_are_inferred() {
        db.insert("INSERT INTO test (a,b,c,d) VALUES (?, ?, ?, ?)", 2, 2, 2, 2);
        assertQueryCount(1, "SELECT * FROM test WHERE a = ?", 2);
        assertQueryCount(1, "SELECT * FROM test WHERE b = ?", 2);
        assertQueryCount(1, "SELECT * FROM test WHERE c = ?", 2);
        assertQueryCount(1, "SELECT * FROM test WHERE d = ?", 2);
        db.update("DELETE FROM test");

        db.insert("INSERT INTO test (a,b,c,d) VALUES (?, ?, ?, ?)", '2', '2', '2', '2');
        assertQueryCount(1, "SELECT * FROM test WHERE a = ?", 2);
        assertQueryCount(1, "SELECT * FROM test WHERE b = ?", 2);
        assertQueryCount(1, "SELECT * FROM test WHERE c = ?", 2);
        assertQueryCount(1, "SELECT * FROM test WHERE d = ?", 2);
        db.update("DELETE FROM test");

        db.insert("INSERT INTO test (a,b,c,d) VALUES (?, ?, ?, ?)", "2", "2", "2", "2");
        assertQueryCount(1, "SELECT * FROM test WHERE a = ?", 2);
        assertQueryCount(1, "SELECT * FROM test WHERE b = ?", 2);
        assertQueryCount(1, "SELECT * FROM test WHERE c = ?", 2);
        assertQueryCount(1, "SELECT * FROM test WHERE d = ?", 2);
        db.update("DELETE FROM test");

        db.insert("INSERT INTO test (a,b,c,d) VALUES (?, ?, ?, ?)", 2.0, 2.0, 2.0, 2.0);
        assertQueryCount(1, "SELECT * FROM test WHERE a = ?", 2);
        assertQueryCount(1, "SELECT * FROM test WHERE b = ?", 2);
        assertQueryCount(0, "SELECT * FROM test WHERE c = ?", 2); // 0 because "2" != "2.0"
        assertQueryCount(1, "SELECT * FROM test WHERE d = ?", 2);
        db.update("DELETE FROM test");

        db.insert("INSERT INTO test (a,b,c,d) VALUES (?, ?, ?, ?)", "2.0", "2.0", "2.0", "2.0");
        assertQueryCount(1, "SELECT * FROM test WHERE a = ?", 2);
        assertQueryCount(1, "SELECT * FROM test WHERE b = ?", 2);
        assertQueryCount(0, "SELECT * FROM test WHERE c = ?", 2); // 0 because "2" != "2.0"
        assertQueryCount(1, "SELECT * FROM test WHERE d = ?", 2);
        db.update("DELETE FROM test");
    }

    @Test
    public void update_bindArgs_types_are_inferred() {
        db.update("UPDATE test SET a = ?, b = ?, c = ?, d = ?", 2, 2, 2, 2);
        assertQueryCount(1, "SELECT * FROM test WHERE a = ?", 2);
        assertQueryCount(1, "SELECT * FROM test WHERE b = ?", 2);
        assertQueryCount(1, "SELECT * FROM test WHERE c = ?", 2);
        assertQueryCount(1, "SELECT * FROM test WHERE d = ?", 2);

        db.update("UPDATE test SET a = ?, b = ?, c = ?, d = ?", '3', '3', '3', '3');
        assertQueryCount(1, "SELECT * FROM test WHERE a = ?", 3);
        assertQueryCount(1, "SELECT * FROM test WHERE b = ?", 3);
        assertQueryCount(1, "SELECT * FROM test WHERE c = ?", 3);
        assertQueryCount(1, "SELECT * FROM test WHERE d = ?", 3);

        db.update("UPDATE test SET a = ?, b = ?, c = ?, d = ?", "4", "4", "4", "4");
        assertQueryCount(1, "SELECT * FROM test WHERE a = ?", 4);
        assertQueryCount(1, "SELECT * FROM test WHERE b = ?", 4);
        assertQueryCount(1, "SELECT * FROM test WHERE c = ?", 4);
        assertQueryCount(1, "SELECT * FROM test WHERE d = ?", 4);

        db.update("UPDATE test SET a = ?, b = ?, c = ?, d = ?", 5.0, 5.0, 5.0, 5.0);
        assertQueryCount(1, "SELECT * FROM test WHERE a = ?", 5);
        assertQueryCount(1, "SELECT * FROM test WHERE b = ?", 5);
        assertQueryCount(0, "SELECT * FROM test WHERE c = ?", 5); // 0 because "5" != "5.0"
        assertQueryCount(1, "SELECT * FROM test WHERE d = ?", 5);

        db.update("UPDATE test SET a = ?, b = ?, c = ?, d = ?", "6.0", "6.0", "6.0", "6.0");
        assertQueryCount(1, "SELECT * FROM test WHERE a = ?", 6);
        assertQueryCount(1, "SELECT * FROM test WHERE b = ?", 6);
        assertQueryCount(0, "SELECT * FROM test WHERE c = ?", 6); // 0 because "6" != "6.0"
        assertQueryCount(1, "SELECT * FROM test WHERE d = ?", 6);
    }

    private void assertQueryCount(int expected, String query, Object... bindArgs) {
        Cursor c = db.query(query, bindArgs);
        assertThat(c.getCount()).isEqualTo(expected);
        c.close();
    }

    private static class TestTable extends Table {
        @Override
        public String getName() {
            return "test";
        }

        @Override
        public int getVersion() {
            return 1;
        }

        @Override
        public String[] getCreateTable() {
            return new String[] {
                    "CREATE TABLE test (a INTEGER, b REAL, c TEXT, d NUMERIC)",
                    "INSERT INTO test (a, b, c, d) VALUES " +
                            "(1, 1.0, '1', 1)"
            };
        }

        @Override
        public String[] getMigration(int nextVersion) {
            return new String[0];
        }
    }
}
