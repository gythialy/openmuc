package org.openmuc.framework.data;

abstract class NumberValue implements Value {
    private final Number value;

    public NumberValue(Number value) {
        this.value = value;
    }

    @Override
    public double asDouble() {
        return this.value.doubleValue();
    }

    @Override
    public float asFloat() {
        return this.value.floatValue();
    }

    @Override
    public long asLong() {
        return this.value.longValue();
    }

    @Override
    public int asInt() {
        return this.value.intValue();
    }

    @Override
    public short asShort() {
        return this.value.shortValue();
    }

    @Override
    public byte asByte() {
        return this.value.byteValue();
    }

    @Override
    public boolean asBoolean() {
        return this.value.doubleValue() != 0.0;
    }

    @Override
    public byte[] asByteArray() {
        return null;
    }

    @Override
    public String asString() {
        return this.value.toString();
    }

    @Override
    public String toString() {
        return asString();
    }

}
