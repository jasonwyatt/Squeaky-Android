package co.jasonwyatt.squeakytodo;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.WorkerThread;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;

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

    @WorkerThread
    public static void saveTodo(Database db, Todo todo) {
        db.update("UPDATE todos SET create_date = ?, content = ?, finished_date = ? WHERE rowid = ?", todo.getCreateDate(), todo.getContent(), todo.getFinishedDate(), todo.getId());
    }

    @WorkerThread
    public static List<Todo> getTodos(Database db) {
        List<Todo> todos = new LinkedList<>();

        Cursor c = db.query("SELECT rowid, create_date, content, finished_date FROM todos ORDER BY finished_date ASC, create_date ASC");
        try {
            while (c.moveToNext()) {
                todos.add(new Todo(c.getInt(0), c.getLong(1), c.getString(2), c.isNull(3) ? null : c.getLong(3)));
            }
        } finally {
            c.close();
        }

        return todos;
    }

    public static class SaveRunnable implements Runnable {
        private final Database mDB;
        private final Todo mTodo;

        public SaveRunnable(Database db, Todo todo) {
            mDB = db;
            mTodo = todo;
        }

        @Override
        public void run() {
            saveTodo(mDB, mTodo);
        }
    }

    public static class Table extends co.jasonwyatt.squeaky.Table {
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

    public static class Loader extends AsyncTaskLoader<List<Todo>> {
        public Loader(Context context) {
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
