package co.jasonwyatt.squeaky;

/**
 * SQLite table definition class.  Extend {@link Table} for each of your project's models.
 */
public abstract class Table {
    /**
     * Get the name of the table.
     * @return Name of the table.
     */
    public abstract String getName();

    /**
     * Get the current version of the table. This will be used by
     * {@link Database#onUpgrade(android.database.sqlite.SQLiteDatabase)} and
     * {@link Database#onCreate(android.database.sqlite.SQLiteDatabase)} to automatically migrate
     * your table from its current version in the DB to the version returned here.
     * @return Current version of the table.
     */
    public abstract int getVersion();

    /**
     * Get an array of SQL statements used to create the table defined for the version sepecified by
     * {@link #getVersion()}.
     * @return SQL statements used to set up the table.
     */
    public abstract String[] getCreateTable();

    /**
     * Get an array of SQL statements used to migrate the table from its current version to another
     * version.
     * @param versionA Current version of the table.
     * @param versionB Next version of the table. Not necessarily the version provided by
     *                 {@link #getVersion()} (especially if the current version in the DB
     *                 is more than one
     *                 version behind the result of {@link #getVersion()}.
     * @return SQL statements used to migrate the table.
     */
    public abstract String[] getMigration(int versionA, int versionB);
}
