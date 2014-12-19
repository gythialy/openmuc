package org.openmuc.framework.server.modbus.register;

import net.wimpi.modbus.procimg.InputRegister;
import net.wimpi.modbus.procimg.Register;
import org.openmuc.framework.data.*;
import org.openmuc.framework.dataaccess.Channel;

import java.nio.ByteBuffer;

/**
 * This Class implements a linked holding register for Modbus server. The reason behind this class is to collect the
 * input over multiple registers and write into one single channnel. Therefore it is necessary to concatenate the
 * register contents.
 * <p/>
 * Bytes are submitted from one to next register after receiving. Example: [register1] -&gt; [register2] -&gt;
 * [register3] -&gt; [register4] = (represents 64 bytes Long/Double value) 0x01 0x02 -&gt; 0x03 0x04 -&gt; 0x01 0x02
 * -&gt; 0x03 0x04
 * <p/>
 * register1 submits 2 bytes to register2 register2 submits 4 bytes to register3 register3 submits 6 bytes to register4
 * register4 writes channel with 8 bytes value.
 * <p/>
 * The behavior of submission is safe against the order the registers are written.
 *
 * @author sfey
 */
public class LinkedMappingHoldingRegister extends MappingInputRegister implements Register {

    private final LinkedMappingHoldingRegister nextRegister;
    private byte[] leadingBytes;
    private byte[] thisRegisterContent;
    private boolean hasLeadingRegister;
    private final ValueType valueType;
    private final InputRegister inputRegister;

    public LinkedMappingHoldingRegister(MappingInputRegister inputRegister, Channel channel, LinkedMappingHoldingRegister nextRegister, ValueType valueType, int byteHigh, int byteLow) {
        super(channel, byteHigh, byteLow);
        this.nextRegister = nextRegister;
        this.valueType = valueType;
        this.inputRegister = inputRegister;

        if (nextRegister != null) {
            nextRegister.hasLeadingRegister = true;
        }
    }

    @Override
    public void setValue(int v) {
        byte[] fromBytes = {(byte) ((v >> 24) & 0xFF), (byte) ((v >> 16) & 0xFF), (byte) ((v >> 8) & 0xFF), (byte) (v & 0xFF)};

        setValue(fromBytes);
    }

    @Override
    public void setValue(short s) {
        byte[] fromBytes = {(byte) ((s >> 8) & 0xFF), (byte) (s & 0xFF)};

        setValue(fromBytes);
    }

    @Override
    public void setValue(byte[] bytes) {
        this.thisRegisterContent = bytes;
        if (nextRegister != null) {
            if (hasLeadingRegister) {
                if (leadingBytes != null) {
                    nextRegister.submit(concatenate(leadingBytes, thisRegisterContent));
                }
            } else {
                nextRegister.submit(thisRegisterContent);
            }
        } else {
            if (hasLeadingRegister) {
                if (leadingBytes != null) {
                    writeChannel(newValue(valueType, concatenate(leadingBytes, thisRegisterContent)));
                } /* else wait for leadingBytes from submit */
            } else {
                writeChannel(newValue(valueType, thisRegisterContent));
            }
        }
    }

    public void submit(byte[] leading) {
        this.leadingBytes = leading;
        if (thisRegisterContent != null) {
            if (nextRegister != null) {
                nextRegister.submit(concatenate(leadingBytes, thisRegisterContent));
            } else {
                writeChannel(newValue(valueType, concatenate(leadingBytes, thisRegisterContent)));
            }
        } /* else wait for thisRegisterContent from setValue */
    }

    public static Value newValue(ValueType fromType, byte[] fromBytes) throws TypeConversionException {
        switch (fromType) {
            case BOOLEAN:
                if (fromBytes[0] == 0x00) {
                    return new BooleanValue(false);
                } else {
                    return new BooleanValue(true);
                }
            case DOUBLE:
                return new DoubleValue(ByteBuffer.wrap(fromBytes).getDouble());
            case FLOAT:
                return new FloatValue(ByteBuffer.wrap(fromBytes).getFloat());
            case LONG:
                return new LongValue(ByteBuffer.wrap(fromBytes).getLong());
            case INTEGER:
                return new IntValue(ByteBuffer.wrap(fromBytes).getInt());
            case SHORT:
                return new ShortValue(ByteBuffer.wrap(fromBytes).getShort());
            case BYTE:
                return new ByteValue(fromBytes[0]);
            case BYTE_ARRAY:
                return new ByteArrayValue(fromBytes);
            case STRING:
                return new StringValue(new String(fromBytes));
            default:
                return null;
        }
    }

    private byte[] concatenate(byte[] one, byte[] two) {
        if (one == null) {
            return two;
        }

        if (two == null) {
            return one;
        }

        byte[] combined = new byte[one.length + two.length];

        for (int i = 0; i < combined.length; ++i) {
            combined[i] = i < one.length ? one[i] : two[i - one.length];
        }

        return combined;
    }

    @Override
    public byte[] toBytes() {
        return inputRegister.toBytes();
    }

    private void writeChannel(Value value) {
        if (useUnscaledValues) {
            channel.write(new DoubleValue(value.asDouble() * channel.getScalingFactor()));
        } else {
            channel.write(value);
        }
    }
}
