package com.vanillage.raytraceantixray.data;

public class LongWrapper {
    protected long value;

    public LongWrapper(long value) {
        this.value = value;
    }

    public final long getValue() {
        return value;
    }

    @Override
    public final boolean equals(Object obj) {
        return this == obj || obj instanceof LongWrapper && value == ((LongWrapper) obj).value;
    }

    @Override
    public final int hashCode() {
        return Long.hashCode(value);
    }
}
