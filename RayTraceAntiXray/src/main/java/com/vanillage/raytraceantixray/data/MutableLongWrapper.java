package com.vanillage.raytraceantixray.data;

public final class MutableLongWrapper extends LongWrapper {
    public MutableLongWrapper(long value) {
        super(value);
    }

    public void setValue(long value) {
        this.value = value;
    }
}
