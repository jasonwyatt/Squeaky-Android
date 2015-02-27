package co.jasonwyatt.squeaky;

/**
* Created by jason on 2/25/15.
*/
public class DatabaseException extends RuntimeException {
    public DatabaseException(String detailMessage) {
        super(detailMessage);
    }

    public DatabaseException(String detailMessage, Throwable parent) {
        super(detailMessage, parent);
    }
}
