package com.bandcamp.squeaky;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.bandcamp.squeaky.util.Logger;

/**
 * Database is the central class in Squeaky.  Instances of {@link Database} are responsible for
 * managing SQLite tables assigned to them as instances of {@link Table} using
 * {@link Database#addTable(Table)}.<br/><br/>
 *
 * Once all required Tables have been added to the database, a call to {@link Database#prepare()}
 * will trigger any necessary migrations and will place the Database instance in to a state where
 * it is ready to accept queries.
 */
public class Database {
    private static final String DEFAULT_VERSIONS_TABLE_NAME = "versions";
    private final Class<? extends DatabaseHelper> mHelperClass;
    private static final int SQLITE_DB_VERSION = 1;
    private HashMap<String, Table> mTables = new HashMap<>();

    private final VersionsTable mVersionsTable;
    private final String mName;
    private final Context mContext;
    private DatabaseHelper mHelper;
    private SQLiteDatabase mWritableDB;
    private SQLiteDatabase mReadableDB;
    private boolean mPrepared;

    /**
     * Creates a new instance of {@link Database} using the default {@link DatabaseHelper} class.
     * @param context Android context.
     * @param name Name of the database.
     */
    public Database(Context context, String name) {
        this(context, name, DatabaseHelper.class);
    }

    /**
     * Creates a new instance of {@link Database}, allowing for a customized {@link DatabaseHelper}
     * class to be provided.
     * @param context Android context.
     * @param name Name of the database.
     * @param helper Helper used to listen to requests from Android's SQLiteDatabase system for
     *               creation/migration and passes those requests on to {@link Database}
     */
    public Database(Context context, String name, Class<? extends DatabaseHelper> helper) {
        this(context, name, DEFAULT_VERSIONS_TABLE_NAME, DatabaseHelper.class);
    }

    /**
     * Creates a new instance of {@link Database}, allowing for a customized name of the table used
     * to track {@link Table} versions.
     * @param context Android context.
     * @param name Name of the database.
     * @param versionsTableName Name of the versions table.
     */
    public Database(Context context, String name, String versionsTableName) {
        this(context, name, versionsTableName, DatabaseHelper.class);
    }

    /**
     * Creates a new instance of {@link Database}, allowing for a customized {@link DatabaseHelper}
     * class and a custom version table to be provided.
     * @param context Android context.
     * @param name Name of the database.
     * @param versionsTableName Name of the versions table.
     * @param helper Helper used to listen to requests from Android's SQLiteDatabase system for
     *               creation/migration and passes those requests on to {@link Database}
     */
    public Database(Context context, String name, String versionsTableName, Class<? extends DatabaseHelper> helper) {
        mHelperClass = helper;
        mName = name;
        mContext = context;
        mVersionsTable = new VersionsTable(this, versionsTableName);
    }

    /**
     * Get the name of the database.
     * @return Name of the database.
     */
    public String getName() {
        return mName;
    }

    /**
     * Add a {@link Table} to the Database definition.
     * @param t Table to add.
     */
    public void addTable(Table t) {
        if (!mTables.containsKey(t.getName())) {
            mTables.put(t.getName(), t);
        }
    }

    /**
     * Prepare the database by instantiating a {@link DatabaseHelper} and establishing connections
     * to the database.  Calling {@link #prepare()} again after the Database is already prepared
     * will throw a {@link DatabaseException}.  If you need to re-prepare a database (to add or
     * migrate a {@link Table} definition), call {@link #close()} first, add your tables, then
     * re-call {@link #prepare()}.
     * @see #isPrepared()
     */
    public void prepare() {
        if (mPrepared) {
            throw new DatabaseException("Cannot re-prepare a prepared database!");
        }
        try {
            Constructor<? extends DatabaseHelper> c = mHelperClass.getDeclaredConstructor(Context.class, String.class, int.class);
            mHelper = c.newInstance(mContext, mName, SQLITE_DB_VERSION);
            mWritableDB = mHelper.getWritableDatabase();
            mReadableDB = mHelper.getReadableDatabase();
            doMigrations(mWritableDB);
            mPrepared = true;
        } catch (NoSuchMethodException e) {
            throw new DatabaseException("Provided database helper class does not expose a constructor of the form (Context, String, int)", e);
        } catch (InvocationTargetException e) {
            throw new DatabaseException("Error instantiating database helper.", e);
        } catch (InstantiationException e) {
            throw new DatabaseException("Error instantiating database helper.", e);
        } catch (IllegalAccessException e) {
            throw new DatabaseException("Error instantiating database helper.", e);
        }
    }

    /**
     * Close the helper and database connections.
     */
    public void close() {
        mHelper.close();
        mWritableDB.close();
        mReadableDB.close();
        mPrepared = false;
    }

    /**
     * Returns whether or not the {@link Database} instance is prepared.
     * @return Whether or not the Database is prepared.
     */
    public boolean isPrepared() {
        return mPrepared;
    }

    /**
     * Query the database.
     * @param stmt SQL Query
     * @param bindArgs Parameters mapping to '?'s in the stmt.
     * @return An instance of {@link android.database.Cursor} giving you access to the results of
     *          the query.
     */
    public synchronized Cursor query(String stmt, Object... bindArgs) {
        if (!mPrepared) {
            throw new DatabaseException("Database "+getName()+" not prepared yet.");
        }
        String[] args = null;
        if (bindArgs != null) {
            args = new String[bindArgs.length];
            for (int i = 0; i < bindArgs.length; i++) {
                Object arg = bindArgs[i];
                if (arg instanceof String) {
                    args[i] = arg.toString();
                } else {
                    args[i] = arg.toString();
                }
            }
        }

        Cursor result = getReadableDB().rawQuery(stmt, args);
        Logger.i(stmt+";", args);
        return result;
    }

    // Used before mReadableDB is available.
    private Cursor querySimple(SQLiteDatabase db, String stmt, Object... bindArgs) {
        String[] args = null;
        if (bindArgs != null) {
            args = new String[bindArgs.length];
            for (int i = 0; i < bindArgs.length; i++) {
                Object arg = bindArgs[i];
                if (arg instanceof String) {
                    args[i] = DatabaseUtils.sqlEscapeString((String) arg);
                } else {
                    args[i] = arg.toString();
                }
            }
        }

        Cursor result = db.rawQuery(stmt, args);
        Logger.i(stmt+";", args);
        return result;
    }

    /**
     * Insert a record in to the database and receive its <code>rowid</code> or <code>_id</code>
     * value as a result.
     * @param stmt Insert query. (not enforced, but encouraged)
     * @param bindArgs Arguments to bind to '?'s in the query.
     * @return Value of the new record's <code>rowid</code>/<code>_id</code> column.
     */
    public synchronized long insert(String stmt, Object... bindArgs) {
        SQLiteStatement statement = getWritableDB().compileStatement(stmt);
        bindArgs(statement, bindArgs);
        Logger.i(stmt+";", bindArgs);
        return statement.executeInsert();
    }

    /**
     * Run an update/delete query on the database
     * @param stmt Query to execute.
     * @param bindArgs Arguments to bind to '?'s in the query.
     * @return Number of affected rows.
     */
    public synchronized int update(String stmt, Object... bindArgs) {
        if (bindArgs == null) {
            return updateBatch(new String[] {stmt}, null, false);
        }
        return updateBatch(new String[] {stmt}, new Object[][] {bindArgs}, false);
    }

    /**
     * Run a bunch of updates/deletes/inserts on the database at once, optionally within a
     * transaction.
     * @param stmts Array of statements to execute.
     * @param bindArgs Arguments to bind to '?'s in the queries. (Optional, if not needed, pass
     *                 null)  Condition: bindArgs.length == stmts.length if not null.
     * @param withTransaction Whether or not to execute the updates within a transaction.
     * @return Number of updated records.
     */
    public synchronized int updateBatch(String[] stmts, Object[][] bindArgs, boolean withTransaction) {
        boolean hasArgs = bindArgs != null;
        if (hasArgs && bindArgs.length != stmts.length) {
            throw new DatabaseException("bindArgs.length != stmts.length");
        }

        if (withTransaction) {
            getWritableDB().beginTransaction();
        }

        int rows = 0;
        for (int i = 0; i < stmts.length; i++) {
            SQLiteStatement statement = getWritableDB().compileStatement(stmts[i]);
            if (hasArgs) {
                bindArgs(statement, bindArgs[i]);
            }
            try {
                rows += statement.executeUpdateDelete();
            } finally {
                statement.close();
            }
            Logger.i(stmts[i]+";", bindArgs[i]);
        }

        if (withTransaction) {
            getWritableDB().endTransaction();
        }

        return rows;
    }

    private void updateSimple(SQLiteDatabase db, String stmt, Object... bindArgs) {
        if (bindArgs != null) {
            updateBatchSimple(db, new String[]{stmt}, new Object[][]{bindArgs});
        } else {
            updateBatchSimple(db, new String[]{stmt}, null);
        }
    }

    private void updateBatchSimple(SQLiteDatabase db, String[] stmts, Object[][] bindArgs) {
        boolean hasArgs = bindArgs != null;
        if (hasArgs && bindArgs.length != stmts.length) {
            throw new DatabaseException("bindArgs.length != stmts.length");
        }

        for (int i = 0; i < stmts.length; i++) {
            if (!stmts[i].endsWith(";")) {
                stmts[i] += ";";
            }
            if (hasArgs && bindArgs[i] != null) {
                Logger.i(stmts[i], bindArgs[i]);
                db.execSQL(stmts[i], bindArgs[i]);
            } else {
                Logger.i(stmts[i]);
                db.execSQL(stmts[i]);
            }
        }
    }

    private void bindArgs(SQLiteStatement statement, Object[] args) {
        if (args == null) {
            return;
        }
        for (int i = 0; i < args.length; i++) {
            Object o = args[i];
            if (o instanceof String) {
                statement.bindString(i + 1, (String) o);
            } else if (o instanceof byte[]) {
                statement.bindBlob(i + 1, (byte[]) o);
            } else if (o instanceof Integer) {
                statement.bindLong(i+1, (Integer) o);
            } else if (o instanceof Long) {
                statement.bindLong(i + 1, (Long) o);
            } else if (o instanceof Double || o instanceof Float) {
                statement.bindDouble(i+1, (Double) o);
            } else if (o == null) {
                statement.bindNull(i+1);
            } else if (o instanceof BlobValue) {
                statement.bindBlob(i+1, ((BlobValue) o).getBytes());
            } else {
                statement.bindString(i+1, o.toString());
            }
        }
    }

    private void doMigrations(SQLiteDatabase db) {
        Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);
        boolean needVersionsTable = true;
        while ( c.moveToNext() ) {
            if (c.getString(0).equals(mVersionsTable.getName())) {
                needVersionsTable = false;
                break;
            }
        }
        c.close();

        if (needVersionsTable) {
            updateBatchSimple(db, mVersionsTable.getCreateTable(), null);
        }

        Map<String, Integer> versions = mVersionsTable.getTableVersions(db);

        for (Table t : mTables.values()) {
            if (versions.containsKey(t.getName())) {
                // have a version, need to upgrade?
                boolean changed = false;
                for (int currentVersion = versions.get(t.getName()); currentVersion < t.getVersion(); currentVersion++) {
                    changed = true;
                    Logger.d("Upgrading", t.getName(), "from", "v"+currentVersion, "to", "v"+(currentVersion+1));
                    String[] migrateStmts = t.getMigration(currentVersion+1);
                    updateBatchSimple(db, migrateStmts, null);
                }
                if (changed) {
                    updateSimple(db, "UPDATE "+mVersionsTable.getName()+" SET version = ? WHERE table_name = ?", t.getVersion(), t.getName());
                }
            } else {
                // need to create.
                Logger.d("Creating new table:", t.getName(), "at version", t.getVersion());
                String[] createStmts = t.getCreateTable();
                updateBatchSimple(db, createStmts, null);
                updateSimple(db, "INSERT INTO "+mVersionsTable.getName()+" (table_name, version) VALUES (?, ?)", t.getName(), t.getVersion());
            }
        }
    }

    /**
     * Get the {@link android.content.Context} associated with the Database.
     * @return {@link android.content.Context} associated with the Database.
     */
    public Context getContext() {
        return mContext;
    }

    /**
     * Get the instance of {@link Database.VersionsTable} used by this Database.
     * @return The {@link Database.VersionsTable} used by the Database.
     */
    public Table getVersionsTable() {
        return mVersionsTable;
    }

    /**
     * Get the {@link Table} instances associated with this Database.
     * @return {@link Table} instances associated with the Database.
     */
    public Collection<Table> getTables() {
        return mTables.values();
    }

    /**
     * Get a raw SQLiteDatabase connection for writing.
     * @return Raw SQLite Database connection.
     */
    public SQLiteDatabase getWritableDB() {
        if (mWritableDB == null) {
            mWritableDB = mHelper.getWritableDatabase();
        }
        return mWritableDB;
    }

    /**
     * Get a raw SQLiteDatabase connection for reading.
     * @return Raw SQLite Database connection.
     */
    public SQLiteDatabase getReadableDB() {
        if (mReadableDB == null) {
            mReadableDB = mHelper.getReadableDatabase();
        }
        return mReadableDB;
    }

    /**
     * {@link Table} definition used to define the SQLite table which tracks the current versions of
     * all other {@link Table}s in the database.
     */
    public static final class VersionsTable extends Table {
        private final Database mDb;
        private final String mName;

        public VersionsTable(Database db, String name) {
            mDb = db;
            mName = name;
        }

        @Override
        public String getName() {
            return mName;
        }

        @Override
        public int getVersion() {
            return 1;
        }

        @Override
        public String[] getCreateTable() {
            return new String[] {
                    "CREATE TABLE "+getName()+" (\n" +
                    "    `table_name` TEXT NOT NULL,\n"+
                    "    `version` INTEGER NOT NULL\n"+
                    ")"
            };
        }

        @Override
        public String[] getMigration(int nextVersion) {
            return new String[0];
        }

        /**
         * Get the current version of a particular table in the {@link Database}.
         * @param sqldb SQLiteDatabase connection.
         * @param tableName Name of the table for which to retrieve the version.
         * @return Version of the table with name {@param tableName}
         */
        public int getTableVersion(SQLiteDatabase sqldb, String tableName) {
            Cursor c = mDb.querySimple(sqldb, "SELECT version FROM "+getName()+" WHERE table_name = ?", tableName);
            int version = -1;
            while (c.moveToNext()) {
                version = c.getInt(0);
            }
            c.close();
            return version;
        }

        /**
         * Get all current versions of tables in the {@link Database}
         * @param sqldb SQLiteDatabase connection.
         * @return Mapping from {@link Table#getName()} to its current version
         *         in the database.
         */
        public Map<String, Integer> getTableVersions(SQLiteDatabase sqldb) {
            Cursor c = mDb.querySimple(sqldb, "SELECT table_name, version FROM "+getName());
            HashMap<String, Integer> result = new HashMap<>();

            int modelIndex = c.getColumnIndex("table_name");
            int versionIndex = c.getColumnIndex("version");

            while (c.moveToNext()) {
                result.put(c.getString(modelIndex), c.getInt(versionIndex));
            }

            c.close();
            return result;
        }
    }

}
