package co.jasonwyatt.squeaky;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import co.jasonwyatt.squeaky.util.Logger;

/**
 * Created by jason on 2/25/15.
 */
public class Database implements Migrator {
    private final Class<? extends DatabaseHelper> mHelperClass;
    private int mVersion = 1;
    private ArrayList<Table> mTables = new ArrayList<>();

    private final VersionsTable mVersionsTable;
    private final String mName;
    private final Context mContext;
    private DatabaseHelper mHelper;
    private SQLiteDatabase mWritableDB;
    private SQLiteDatabase mReadableDB;
    private boolean mPrepared;

    public Database(Context context, String name) {
        this(context, name, DatabaseHelper.class);
    }

    public Database(Context context, String name, Class<? extends DatabaseHelper> helper) {
        mHelperClass = helper;
        mName = name;
        mContext = context;
        mVersionsTable = new VersionsTable();
    }

    public String getName() {
        return mName;
    }

    public void addTable(Table t) {
        mTables.add(t);
        mVersion += t.getVersion();
    }

    public void prepare() {
        if (mPrepared) {
            throw new DatabaseException("Cannot re-prepare a prepared database!");
        }
        try {
            Constructor<? extends DatabaseHelper> c = mHelperClass.getDeclaredConstructor(Context.class, String.class, int.class, Migrator.class);
            mHelper = c.newInstance(mContext, mName, mVersion, this);
            mWritableDB = mHelper.getWritableDatabase();
            mReadableDB = mHelper.getReadableDatabase();
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

    public boolean isPrepared() {
        return mPrepared;
    }

    public Cursor query(String stmt) {
        return query(stmt, null);
    }

    public Cursor query(String stmt, Object[] bindArgs) {
        if (!mPrepared) {
            throw new DatabaseException("Database "+getName()+" not prepared yet.");
        }
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

        Cursor result = getReadableDB().rawQuery(stmt, args);
        Logger.i(stmt+";", args);
        return result;
    }

    private Cursor querySimple(SQLiteDatabase db, String stmt, Object[] bindArgs) {
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

    public long insert(String stmt, Object[] bindArgs) {
        SQLiteStatement statement = getWritableDB().compileStatement(stmt);
        bindArgs(statement, bindArgs);
        Logger.i(stmt+";", bindArgs);
        return statement.executeInsert();
    }

    public int update(String stmt) {
        return updateBatch(new String[]{stmt}, null, false);
    }

    public int update(String stmt, Object[] bindArgs) {
        return updateBatch(new String[] {stmt}, new Object[][] {bindArgs}, false);
    }

    public int updateBatch(String[] stmts) {
        return updateBatch(stmts, null, false);
    }

    public int updateBatch(String[] stmts, boolean withTransaction) {
        return updateBatch(stmts, null, withTransaction);
    }

    public int updateBatch(String[] stmts, Object[][] bindArgs) {
        return updateBatch(stmts, bindArgs, false);
    }

    public int updateBatch(String[] stmts, Object[][] bindArgs, final boolean withTransaction) {
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

    private void updateSimple(SQLiteDatabase db, String stmt, Object[] bindArgs) {
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
            if (hasArgs && bindArgs[i] != null) {
                db.execSQL(stmts[i], bindArgs[i]);
                Logger.i(stmts[i]+";", bindArgs[i]);
            } else {
                db.execSQL(stmts[i]);
                Logger.i(stmts[i]+";");
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

    @Override
    public void onCreate(SQLiteDatabase db) {
        updateBatchSimple(db, mVersionsTable.getCreateTable(), null);

        ArrayList<String> queries = new ArrayList<>();
        HashMap<String, Integer> versions = new HashMap<>();
        for (Table t : mTables) {
            String [] createStmts = t.getCreateTable();
            for (String stmt : createStmts) {
                queries.add(stmt);
            }

            versions.put(t.getName(), t.getVersion());
        }

        Object[] versionArgs = new Object[versions.size()*2];
        Set<String> keys = versions.keySet();
        Iterator<String> keyIter = keys.iterator();
        StringBuilder versionsBuilder = new StringBuilder();
        for (int i = 0; i < keys.size(); i++) {
            String model = keyIter.next();
            versionArgs[2*i] = model;
            versionArgs[2*i+1] = versions.get(model);

            if (i != 0) {
                versionsBuilder.append(", ");
            }
            versionsBuilder.append("(?, ?)");
        }
        if (versions.size() > 0) {
            queries.add("INSERT INTO versions (model, version) VALUES " + versionsBuilder.toString());
        }

        Object[][] args = new Object[queries.size()][];
        if (versions.size() > 0) {
            args[queries.size() - 1] = versionArgs;
        }

        updateBatchSimple(db, queries.toArray(new String[queries.size()]), args);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db) {
        Map<String, Integer> versions = mVersionsTable.getTableVersions(this, db);

        for (Table t : mTables) {
            if (versions.containsKey(t.getName())) {
                // have a version, need to upgrade?
                boolean changed = false;
                for (int currentVersion = versions.get(t.getName()); currentVersion < t.getVersion(); currentVersion++) {
                    changed = true;
                    Logger.d("Upgrading", t.getName(), "from", "v"+currentVersion, "to", "v"+(currentVersion+1));
                    String[] migrateStmts = t.getMigration(currentVersion, currentVersion+1);
                    updateBatchSimple(db, migrateStmts, null);
                }
                if (changed) {
                    updateSimple(db, "UPDATE versions SET version = ? WHERE model = ?", new Object[]{t.getName(), t.getVersion()});
                }
            } else {
                // need to create.
                Logger.d("Creating new table:", t.getName(), "at version", t.getVersion());
                String[] createStmts = t.getCreateTable();
                updateBatchSimple(db, createStmts, null);
                updateSimple(db, "INSERT INTO versions (model, version) VALUES (?, ?)", new Object[]{t.getName(), t.getVersion()});
            }
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db) {
        Map<String, Integer> versions = mVersionsTable.getTableVersions(this, db);

        for (Table t : mTables) {
            if (versions.containsKey(t.getName())) {
                // have a version, need to upgrade?
                boolean changed = false;
                for (int currentVersion = versions.get(t.getName()); currentVersion > t.getVersion(); currentVersion--) {
                    changed = true;
                    Logger.d("Downgrading", t.getName(), "from", "v"+currentVersion, "to", "v"+(currentVersion+1));
                    String[] migrateStmts = t.getMigration(currentVersion, currentVersion-1);
                    updateBatchSimple(db, migrateStmts, null);
                }
                if (changed) {
                    updateSimple(db, "UPDATE versions SET version = ? WHERE model = ?", new Object[]{t.getName(), t.getVersion()});
                }
            } else {
                // need to create.
                Logger.d("Creating new table:", t.getName(), "at version", t.getVersion());
                String[] createStmts = t.getCreateTable();
                updateBatchSimple(db, createStmts, null);
                updateSimple(db, "INSERT INTO versions (model, version) VALUES (?, ?)", new Object[]{t.getName(), t.getVersion()});
            }
        }
    }

    public Context getContext() {
        return mContext;
    }

    public Table getVersionsTable() {
        return mVersionsTable;
    }

    public List<Table> getTables() {
        return mTables;
    }

    public SQLiteDatabase getWritableDB() {
        if (mWritableDB == null) {
            mWritableDB = mHelper.getWritableDatabase();
        }
        return mWritableDB;
    }

    public SQLiteDatabase getReadableDB() {
        if (mReadableDB == null) {
            mReadableDB = mHelper.getReadableDatabase();
        }
        return mReadableDB;
    }

    public static class VersionsTable extends Table {
        public static final String NAME = "versions";

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        public int getVersion() {
            return 1;
        }

        @Override
        public String[] getCreateTable() {
            return new String[] {
                    "CREATE TABLE versions (\n" +
                    "    `model` TEXT NOT NULL,\n"+
                    "    `version` INTEGER NOT NULL\n"+
                    ")"
            };
        }

        @Override
        public String[] getMigration(int versionA, int versionB) {
            return new String[0];
        }

        public int getTableVersion(Database db, SQLiteDatabase sqldb, String modelName) {
            Cursor c = db.querySimple(sqldb, "SELECT version FROM versions WHERE model = ?", new Object[]{modelName});
            int version = -1;
            while (c.moveToNext()) {
                version = c.getInt(0);
            }
            return version;
        }

        public Map<String, Integer> getTableVersions(Database db, SQLiteDatabase sqldb) {
            Cursor c = db.querySimple(sqldb, "SELECT model, version FROM versions", null);
            HashMap<String, Integer> result = new HashMap<>();

            int modelIndex = c.getColumnIndex("model");
            int versionIndex = c.getColumnIndex("version");

            while (c.moveToNext()) {
                result.put(c.getString(modelIndex), c.getInt(versionIndex));
            }

            return result;
        }
    }

}
