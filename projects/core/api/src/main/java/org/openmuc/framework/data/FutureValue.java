package org.openmuc.framework.data;

import org.openmuc.framework.dataaccess.Channel;

/**
 * Class used to write values in the future.
 * 
 * @see Channel#writeFuture(java.util.List)
 */
public class FutureValue {

    private final Value value;
    private final long writeTime;

    /**
     * Construct a new future value.
     * 
     * @param value
     *            a value.
     * @param writeTime
     *            the write time in the future.
     */
    public FutureValue(Value value, long writeTime) {
        if (writeTime <= System.currentTimeMillis()) {
            throw new IllegalArgumentException("Write time must be in the future.");
        }
        this.value = value;
        this.writeTime = writeTime;
    }

    /**
     * The future value.
     * 
     * @return the value.
     */
    public Value getValue() {
        return value;
    }

    /**
     * The write time.
     * 
     * @return the write time.
     */
    public Long getWriteTime() {
        return writeTime;
    }

}
