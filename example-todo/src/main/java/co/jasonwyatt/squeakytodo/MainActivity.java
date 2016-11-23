package co.jasonwyatt.squeakytodo;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<List<Todo>>, Observer {

    private Adapter mAdapter;
    private static Observable sObservable = new Observable() {
        @Override
        public void notifyObservers(Object arg) {
            setChanged();
            super.notifyObservers(arg);
        }

        @Override
        public synchronized boolean hasChanged() {
            return true;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportLoaderManager().initLoader(R.id.todo_items_loader_id, null, this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);

        RecyclerView rv = (RecyclerView) findViewById(R.id.content_main);
        rv.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mAdapter = new Adapter();
        rv.setAdapter(mAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sObservable.addObserver(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sObservable.deleteObserver(this);
    }

    @Override
    public Loader<List<Todo>> onCreateLoader(int id, Bundle args) {
        return new Todo.Loader(this);
    }

    @Override
    public void onLoadFinished(Loader<List<Todo>> loader, List<Todo> data) {
        mAdapter.setTodoItems(data);
    }

    @Override
    public void onLoaderReset(Loader<List<Todo>> loader) {
    }

    @Override
    public void update(Observable observable, Object o) {
        if (o instanceof Todo) {
            Todo todo = (Todo) o;
            if (todo.getId() >= 0) {
                AsyncTask.execute(new Todo.SaveRunnable(App.getInstance().getDB(), todo));
            }
        }
    }

    private static class Adapter extends RecyclerView.Adapter<TodoHolder> {
        private List<Todo> mTodoItems;

        Adapter() {
            mTodoItems = new ArrayList<>(0);
        }

        void setTodoItems(List<Todo> todoItems) {
            mTodoItems = todoItems;
            notifyDataSetChanged();
        }

        @Override
        public TodoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inf = LayoutInflater.from(parent.getContext());
            return new TodoHolder(inf, parent);
        }

        @Override
        public void onBindViewHolder(TodoHolder holder, int position) {
            holder.bind(mTodoItems.get(position));
        }

        @Override
        public int getItemCount() {
            return mTodoItems.size();
        }
    }

    private static class TodoHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener, CompoundButton.OnCheckedChangeListener {
        private final CheckBox mCheckbox;
        private final TextView mContent;
        private Todo mItem;

        TodoHolder(LayoutInflater inf, ViewGroup parent) {
            super(inf.inflate(R.layout.todo_item, parent, false));

            mCheckbox = (CheckBox) itemView.findViewById(R.id.todo_finished);
            mContent = (TextView) itemView.findViewById(R.id.todo_content);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);

            mCheckbox.setOnCheckedChangeListener(this);
        }

        void bind(Todo item) {
            mItem = item;
            mCheckbox.setChecked(item.getFinishedDate() != null);
            mContent.setText(item.getContent());
        }

        @Override
        public void onClick(View view) {
            if (view == itemView) {
                mCheckbox.toggle();
            }
        }

        @Override
        public boolean onLongClick(View view) {
            return false;
        }

        @Override
        public void onCheckedChanged(CompoundButton view, boolean checked) {
            mItem.setFinishedDate(checked ? System.currentTimeMillis() : null);
            MainActivity.sObservable.notifyObservers(mItem);
        }
    }
}
