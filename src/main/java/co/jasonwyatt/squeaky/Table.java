package co.jasonwyatt.squeaky;

/**
 * Created by jason on 2/25/15.
 */
public abstract class Table {
    public abstract String getName();
    public abstract int getVersion();
    public abstract String[] getCreateTable();
    public abstract String[] getMigration(int versionA, int versionB);
}
