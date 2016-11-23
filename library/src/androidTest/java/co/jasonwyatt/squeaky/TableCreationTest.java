package co.jasonwyatt.squeaky;

import android.database.Cursor;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import static org.assertj.core.api.Assertions.assertThat;

import co.jasonwyatt.squeaky.Database;
import co.jasonwyatt.squeaky.Table;

/**
 * @author jason
 *
 */
@RunWith(AndroidJUnit4.class)
public class TableCreationTest {
    @Test
    public void testCreateTable() throws Exception {
        Database db = new Database(InstrumentationRegistry.getContext(), getClass().getSimpleName());
        TestTable t = new TestTable();
        db.addTable(t);
        assertThat(db.getTables().size()).isEqualTo(1);

        db.prepare();

        Cursor c = db.query("SELECT table_name, version FROM versions");
        assertThat(c.getCount()).isEqualTo(1);
        c.moveToNext();
        assertThat(c.getString(0)).isEqualToIgnoringCase(t.getName());
        assertThat(c.getInt(1)).isEqualTo(t.getVersion());
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
        public String[] getMigration(int nextVersion) {
            return new String[0];
        }
    }
}
