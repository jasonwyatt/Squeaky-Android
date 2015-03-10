package com.bandcamp.squeaky;

/**
 * BlobValue is an interface used to designate a value passed to
 * {@link Database#query(String, Object...)}, {@link Database#update(String, Object...)},
 * or {@link Database#insert(String, Object...)}
 */
public interface BlobValue {
    /**
     * Get the raw bytes intended for a BLOB column in the database.
     * @return Raw bytes for the database.
     */
    public byte[] getBytes();
}
