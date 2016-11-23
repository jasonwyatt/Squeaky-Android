package co.jasonwyatt.squeaky;

/**
 * Exception class that extends RuntimeException and is used by Squeaky to encapsulate errors and
 * exceptions related to migration/creation/querying.
*/
public class DatabaseException extends RuntimeException {
    public DatabaseException(String detailMessage) {
        super(detailMessage);
    }

    public DatabaseException(String detailMessage, Throwable parent) {
        super(detailMessage, parent);
    }
}
