package co.jasonwyatt.squeaky.util;

import android.database.Cursor;
import android.support.annotation.IntDef;
import android.util.Log;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import co.jasonwyatt.squeaky.Database;
import co.jasonwyatt.squeaky.Table;

/**
 * Created by jason on 2/25/15.
 */
public class Logger {
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({Log.ASSERT, Log.DEBUG, Log.ERROR, Log.INFO, Log.VERBOSE, Log.WARN})
    public @interface LogLevel {}

    public static final String TAG = "Squeaky";
    private static boolean sEnabled = true;
    private static AtomicInteger sLogLevel = new AtomicInteger(Log.VERBOSE);

    /**
     * Gets whether or not the logger is enabled.
     * @return True if logger is enabled.
     * @deprecated Use {@link #enabled(int)} instead.
     */
    @Deprecated
    public static boolean enabled() {
        return enabled(Log.DEBUG);
    }

    /**
     * Gets whether or not the logger is enabled at the specified priority..
     * @param level Level of logging to test against.
     * @return True if it's enabled.
     */
    public static boolean enabled(@LogLevel int level) {
        return sEnabled && sLogLevel.get() <= level;
    }

    public static void setEnabled(boolean enabled) {
        sEnabled = enabled;
    }

    public static void setLevel(@LogLevel int level) {
        sLogLevel.set(level);
    }

    public static void d(Object...pieces) {
        if (!enabled(Log.DEBUG)) {
            return;
        }
        Log.d(TAG, join(pieces));
    }

    public static void e(Object...pieces) {
        if (!enabled(Log.ERROR)) {
            return;
        }
        Log.e(TAG, join(pieces));
    }

    public static void e(Throwable err, Object...pieces) {
        if (!enabled(Log.ERROR)) {
            return;
        }
        Log.e(TAG, join(pieces), err);
    }

    public static void i(Object...pieces) {
        if (!enabled(Log.INFO)) {
            return;
        }
        Log.i(TAG, join(pieces));
    }

    public static void w(Object...pieces) {
        if (!enabled(Log.WARN)) {
            return;
        }
        Log.w(TAG, join(pieces));
    }

    public static void w(Throwable err, Object...pieces) {
        if (!enabled(Log.WARN)) {
            return;
        }
        Log.w(TAG, join(pieces), err);
    }

    public static void dumpTables(Database db) {
        dumpTables(db, 0);
    }

    public static void dumpTables(Database db, int limit) {
        StringBuilder builder = new StringBuilder();
        builder.append("\n+------------------------------------------------------------------------------+\n");
        builder.append("|                             Squeaky Table Dump                               |\n");
        builder.append("+------------------------------------------------------------------------------+\n");
        builder.append("\n");
        dump(db, db.getVersionsTable(), 0, builder);
        for (Table t : db.getTables()) {
            builder.append("\n");
            dump(db, t, limit, builder);
        }
        builder.append("\n");
        builder.append("--------------------------------------------------------------------------------");
        i(builder.toString());
    }

    public static void dump(Database db, Table t) {
        StringBuilder builder = new StringBuilder();
        dump(db, t, 0, builder);
        i(builder.toString());
    }

    public static void dump(Database db, Table t, StringBuilder builder) {
        dump(db, t, 0, builder);
    }

    public static void dump(Database db, Table t, int limit) {
        StringBuilder builder = new StringBuilder();
        dump(db, t, limit, builder);
        i(builder.toString());
    }

    public static void dump(Database db, Table t, int limit, StringBuilder b) {
        String limitClause = "";
        if (limit > 0) {
            limitClause = " LIMIT "+limit;
        }

        String query = "SELECT * FROM "+t.getName()+limitClause;
        Cursor c = db.query(query);
        int columnCount = c.getColumnCount();
        int[] maxWidths = new int[columnCount];
        String[] columnNames = new String[columnCount];
        for (int i = 0; i < columnCount; i++) {
            columnNames[i] = c.getColumnName(i);
            maxWidths[i] = columnNames[i].length();
        }

        ArrayList<String[]> rows = new ArrayList<>();
        while (c.moveToNext()) {
            String[] columns = new String[columnCount];
            for (int i = 0; i < columnCount; i++) {
                switch (c.getType(i)) {
                    case Cursor.FIELD_TYPE_STRING:
                        columns[i] = c.getString(i);
                        break;
                    case Cursor.FIELD_TYPE_BLOB:
                        columns[i] = "**BLOB**";
                        break;
                    case Cursor.FIELD_TYPE_FLOAT:
                        columns[i] = c.getFloat(i)+"";
                        break;
                    case Cursor.FIELD_TYPE_INTEGER:
                        columns[i] = c.getLong(i)+"";
                        break;
                    case Cursor.FIELD_TYPE_NULL:
                        columns[i] = "null";
                        break;
                }
                if (columns[i].length() > maxWidths[i]) maxWidths[i] = columns[i].length();
            }
            rows.add(columns);
        }

        int totalWidthWithBorders = 1;
        for (int i=0; i < columnCount; i++) {
            totalWidthWithBorders += maxWidths[i] + 3;
        }

        StringBuilder builder = new StringBuilder();
        builder.append("Table: `"+t.getName()+"`\n");
        // opener.
        for (int i = 0; i < totalWidthWithBorders; i++) {
            if (i == 0 || i == totalWidthWithBorders-1) {
                builder.append('+');
            } else {
                builder.append('-');
            }
        }
        builder.append('\n');

        // columns.
        for (int i = 0; i < columnCount; i++) {
            builder.append("| ");
            builder.append(String.format("%"+maxWidths[i]+"s", columnNames[i]));
            builder.append(' ');
        }
        builder.append("|\n");
        for (int i = 0; i < totalWidthWithBorders; i++) {
            if (i == 0 || i == totalWidthWithBorders-1) {
                builder.append('+');
            } else {
                builder.append('-');
            }
        }
        builder.append('\n');

        // rows.
        for (String[] row : rows) {
            for (int i = 0; i < columnCount; i++) {
                builder.append("| ");
                builder.append(String.format("%"+maxWidths[i]+"s", row[i]));
                builder.append(' ');
            }
            builder.append("|\n");
        }

        // closer.
        for (int i = 0; i < totalWidthWithBorders; i++) {
            if (i == 0 || i == totalWidthWithBorders-1) {
                builder.append('+');
            } else {
                builder.append('-');
            }
        }

        b.append(builder.toString());
        b.append("\n");
        c.close();
    }

    public static String join(Object...pieces) {
        StringBuilder sb = new StringBuilder();
        for (Object o : pieces) {
            if (o instanceof Object[]) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("[");
                boolean isFirst = true;
                for (Object p : (Object[])o) {
                    if (!isFirst) {
                        sb2.append(", ");
                    }
                    sb2.append(p);
                    isFirst = false;
                }
                sb2.append("] ");
                sb.append(sb2);
            } else {
                sb.append(o);
                sb.append(" ");
            }
        }
        return sb.toString();
    }
}
