package co.jasonwyatt.squeakytodo;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.support.annotation.WorkerThread;
import android.support.v4.content.AsyncTaskLoader;

import java.util.LinkedList;
import java.util.List;

import co.jasonwyatt.squeaky.Database;

/**
 * @author jason
 */

public class Todo {
    private final long mCreateDate;
    private final String mContent;
    private Long mFinishedDate;
    private final int mId;

    public Todo(int id, long createDate, String content, Long finishedDate) {
        mId = id;
        mCreateDate = createDate;
        mContent = content;
        mFinishedDate = finishedDate;
    }

    public Todo(String s) {
        mId = -1;
        mCreateDate = System.currentTimeMillis();
        mContent = s;
        mFinishedDate = null;
    }

    public int getId() {
        return mId;
    }

    public long getCreateDate() {
        return mCreateDate;
    }

    public String getContent() {
        return mContent;
    }

    public Long getFinishedDate() {
        return mFinishedDate;
    }

    public void setFinishedDate(Long date) {
        mFinishedDate = date;
    }

    @SuppressWarnings("WeakerAccess")
    @WorkerThread
    public static void saveTodo(Database db, Todo todo) {
        if (todo.getId() >= 0) {
            db.update("UPDATE todos SET create_date = ?, content = ?, finished_date = ? WHERE rowid = ?", todo.getCreateDate(), todo.getContent(), todo.getFinishedDate(), todo.getId());
        } else {
            db.insert("INSERT INTO todos (create_date, content) VALUES (?, ?)", todo.getCreateDate(), todo.getContent());
        }
    }

    @SuppressWarnings("WeakerAccess")
    @WorkerThread
    public static List<Todo> getTodos(Database db) {
        List<Todo> todos = new LinkedList<>();

        Cursor c = db.query("SELECT rowid, create_date, content, finished_date, case when finished_date IS NULL then 0 else 1 end AS is_finished FROM todos ORDER BY is_finished ASC, finished_date DESC, create_date ASC");
        try {
            while (c.moveToNext()) {
                todos.add(new Todo(c.getInt(0), c.getLong(1), c.getString(2), c.isNull(3) ? null : c.getLong(3)));
            }
        } finally {
            c.close();
        }

        return todos;
    }

    @SuppressWarnings("WeakerAccess")
    @WorkerThread
    public static void deleteTodo(Database db, Todo todo) {
        if (todo.getId() >= 0) {
            db.update("DELETE FROM todos WHERE rowid = ?", todo.getId());
        }
    }

    static class SaveTask extends AsyncTask<Void, Void, List<Todo>> {
        private final Database mDB;
        private final Todo mTodo;

        SaveTask(Database db, Todo todo) {
            mDB = db;
            mTodo = todo;
        }

        @Override
        protected List<Todo> doInBackground(Void... voids) {
            saveTodo(mDB, mTodo);
            return getTodos(mDB);
        }
    }

    static class DeleteTask extends AsyncTask<Void, Void, List<Todo>> {
        private final Database mDB;
        private final Todo mTodo;

        DeleteTask(Database db, Todo todo) {
            mDB = db;
            mTodo = todo;
        }

        @Override
        protected List<Todo> doInBackground(Void... voids) {
            deleteTodo(mDB, mTodo);
            return getTodos(mDB);
        }
    }

    static class Table extends co.jasonwyatt.squeaky.Table {
        private static final String NAME = "todos";

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
                    "CREATE TABLE todos (" +
                        "create_date INTEGER NOT NULL," +
                        "content TEXT NOT NULL," +
                        "finished_date INTEGER" +
                    ")",
                    "INSERT INTO todos (create_date, content) VALUES ("+System.currentTimeMillis()+", 'Try creating a new todo item..'), ("+System.currentTimeMillis()+", 'Use Squeaky in my app.')"
            };
        }

        @Override
        public String[] getMigration(int nextVersion) {
            return new String[0];
        }
    }

    static class Loader extends AsyncTaskLoader<List<Todo>> {
        Loader(Context context) {
            super(context);
        }

        @Override
        public List<Todo> loadInBackground() {
            return Todo.getTodos(App.getInstance().getDB());
        }

        @Override
        protected void onStartLoading() {
            forceLoad();
        }
    }
}
