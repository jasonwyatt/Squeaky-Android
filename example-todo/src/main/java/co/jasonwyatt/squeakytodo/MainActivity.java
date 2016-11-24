package co.jasonwyatt.squeakytodo;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.yarolegovich.lovelydialog.LovelyTextInputDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import co.jasonwyatt.squeakytodo.event.DeleteEvent;

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
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new LovelyTextInputDialog(view.getContext())
                        .setConfirmButton(R.string.save, new LovelyTextInputDialog.OnTextInputConfirmListener() {
                            @Override
                            public void onTextInputConfirmed(String text) {
                                sObservable.notifyObservers(new Todo(text.trim()));
                            }
                        })
                        .show();
            }
        });

        RecyclerView rv = (RecyclerView) findViewById(R.id.content_main);
        rv.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mAdapter = new Adapter();
        rv.setAdapter(mAdapter);
        rv.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
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
            new Todo.SaveTask(App.getInstance().getDB(), (Todo) o) {
                @Override
                protected void onPostExecute(List<Todo> todos) {
                    mAdapter.setTodoItems(todos);
                }
            }.execute();
        }

        if (o instanceof DeleteEvent) {
            new Todo.DeleteTask(App.getInstance().getDB(), ((DeleteEvent) o).getItem()) {
                @Override
                protected void onPostExecute(List<Todo> todos) {
                    mAdapter.setTodoItems(todos);
                }
            }.execute();
        }
    }

    private static class Adapter extends RecyclerView.Adapter<TodoHolder> {
        private List<Todo> mTodoItems;

        Adapter() {
            mTodoItems = new ArrayList<>(0);
        }

        void setTodoItems(final List<Todo> todoItems) {
            DiffUtil.DiffResult res = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                @Override
                public int getOldListSize() {
                    return mTodoItems.size();
                }

                @Override
                public int getNewListSize() {
                    return todoItems.size();
                }

                @Override
                public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                    return mTodoItems.get(oldItemPosition).getId() == todoItems.get(newItemPosition).getId();
                }

                @Override
                public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                    Todo oude = mTodoItems.get(oldItemPosition);
                    Todo nieuw = todoItems.get(newItemPosition);

                    boolean contentEquals = false;
                    if (oude.getContent() == null && nieuw.getContent() == null) {
                        contentEquals = true;
                    } else if (oude.getContent() !=  null && nieuw.getContent() != null && oude.getContent().equals(nieuw.getContent())) {
                        contentEquals = true;
                    }

                    boolean finishedEquals = false;
                    if (oude.getFinishedDate() == null && nieuw.getFinishedDate() == null) {
                        finishedEquals = true;
                    } else if (oude.getFinishedDate() != null && nieuw.getFinishedDate() != null && oude.getFinishedDate().equals(nieuw.getFinishedDate())) {
                        finishedEquals = true;
                    }

                    return contentEquals && finishedEquals;
                }
            });
            mTodoItems = todoItems;
            res.dispatchUpdatesTo(this);
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
            PopupMenu menu = new PopupMenu(view.getContext(), view);
            menu.inflate(R.menu.item_popup);
            menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    if (item.getItemId() == R.id.delete) {
                        sObservable.notifyObservers(new DeleteEvent(mItem));
                    }
                    return false;
                }
            });
            menu.show();
            return true;
        }

        @Override
        public void onCheckedChanged(CompoundButton view, boolean checked) {
            mItem.setFinishedDate(checked ? System.currentTimeMillis() : null);
            MainActivity.sObservable.notifyObservers(mItem);
        }
    }
}
