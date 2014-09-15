package org.openmuc.framework.server.modbus;

import net.wimpi.modbus.procimg.Register;
import org.openmuc.framework.data.ValueType;
import org.openmuc.framework.dataaccess.Channel;

import java.nio.ByteBuffer;

public class LinkedHoldingRegister implements Register {

    private final LinkedHoldingRegister nextRegister;
    private byte[] leadingBytes;
    private byte[] thisRegisterContent;
    private final Channel channel;
    private boolean hasLeadingRegister;
    int bytesFrom;
    int bytesTo;

    public LinkedHoldingRegister(Channel channel,
                                 LinkedHoldingRegister nextRegister,
                                 ValueType valueType,
                                 int bytesFrom,
                                 int bytesTo) {
        this.channel = channel;
        this.nextRegister = nextRegister;
        this.bytesFrom = bytesFrom;
        this.bytesTo = bytesTo;

        if (nextRegister != null) {
            nextRegister.hasLeadingRegister = true;
        }
    }

    @Override
    public int getValue() {
        int toReturn = ByteBuffer.wrap(toBytes()).getInt();
        return toReturn;
    }

    @Override
    public int toUnsignedShort() {
        short shortVal = ByteBuffer.wrap(toBytes()).getShort();
        int toReturn = shortVal & 0xFFFF;
        return toReturn;
    }

    @Override
    public short toShort() {
        short toReturn = ByteBuffer.wrap(toBytes()).getShort();
        return toReturn;
    }

    @Override
    public byte[] toBytes() {
        byte[] bytes = channel.getLatestRecord().getValue().asByteArray();
        byte[] toReturn = {bytes[bytesFrom], bytes[bytesTo]};
        return toReturn;
    }

    @Override
    public void setValue(int v) {
        byte[] fromBytes = {(byte) ((v >> 24) & 0xFF),
                            (byte) ((v >> 16) & 0xFF),
                            (byte) ((v >> 8) & 0xFF),
                            (byte) (v & 0xFF)};

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
                    channel.write(ValueType.newValue(channel.getValueType(),
                                                     concatenate(leadingBytes,
                                                                 thisRegisterContent)));
                } // else wait for leadingBytes from submit
            } else {
                channel.write(ValueType.newValue(channel.getValueType(), thisRegisterContent));
            }
        }
    }

    public void submit(byte[] leading) {
        this.leadingBytes = leading;
        if (thisRegisterContent != null) {
            if (nextRegister != null) {
                nextRegister.submit(concatenate(leadingBytes, thisRegisterContent));
            } else {
                channel.write(ValueType.newValue(channel.getValueType(),
                                                 concatenate(leadingBytes, thisRegisterContent)));
            }
        } // else wait for thisRegisterContent from setValue
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
}
