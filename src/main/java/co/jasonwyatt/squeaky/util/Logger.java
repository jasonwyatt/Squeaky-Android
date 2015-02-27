package co.jasonwyatt.squeaky.util;

import android.database.Cursor;
import android.database.DatabaseUtils;
import android.util.Log;

import java.util.ArrayList;

import co.jasonwyatt.squeaky.Database;
import co.jasonwyatt.squeaky.Table;

/**
 * Created by jason on 2/25/15.
 */
public class Logger {
    public static final String TAG = "Squeaky";
    private static boolean sEnabled = true;

    public static boolean enabled() {
        return sEnabled;
    }

    public static void setEnabled(boolean enabled) {
        sEnabled = enabled;
    }

    public static void d(Object...pieces) {
        if (!enabled()) {
            return;
        }
        Log.d(TAG, join(pieces));
    }

    public static void e(Object...pieces) {
        Log.e(TAG, join(pieces));
    }

    public static void e(Throwable err, Object...pieces) {
        Log.e(TAG, join(pieces), err);
    }

    public static void i(Object...pieces) {
        if (!enabled()) {
            return;
        }
        Log.i(TAG, join(pieces));
    }

    public static void w(Object...pieces) {
        Log.w(TAG, join(pieces));
    }

    public static void w(Throwable err, Object...pieces) {
        Log.w(TAG, join(pieces), err);
    }

    public static void dumpTables(Database db) {
        dumpTables(db, 0);
    }

    public static void dumpTables(Database db, int limit) {
        Logger.i("+------------------------------------------------------------------------------+");
        Logger.i("|                             Squeaky Table Dump                               |");
        Logger.i("+------------------------------------------------------------------------------+");
        dump(db, db.getVersionsTable(), 0);
        for (Table t : db.getTables()) {
            Logger.i("");
            dump(db, t, limit);
        }
        Logger.i("");
        Logger.i("--------------------------------------------------------------------------------");
    }

    public static void dump(Database db, Table t) {
        dump(db, t, 0);
    }

    public static void dump(Database db, Table t, int limit) {
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

        Logger.i(builder.toString());
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
                    if (p instanceof String) {
                        sb2.append(DatabaseUtils.sqlEscapeString(p.toString()));
                    } else {
                        sb2.append(p);
                    }
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
