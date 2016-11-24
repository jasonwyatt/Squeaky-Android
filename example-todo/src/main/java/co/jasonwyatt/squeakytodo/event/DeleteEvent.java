package co.jasonwyatt.squeakytodo.event;

import co.jasonwyatt.squeakytodo.Todo;

/**
 * @author jason
 */
public class DeleteEvent {
    private final Todo mItem;

    public DeleteEvent(Todo item) {
        mItem = item;
    }

    public Todo getItem() {
        return mItem;
    }
}
