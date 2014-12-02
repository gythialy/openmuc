package org.openmuc.framework.server.modbus.register;

import net.wimpi.modbus.procimg.InputRegister;
import org.openmuc.framework.dataaccess.Channel;
import org.openmuc.framework.server.modbus.ModbusServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

public abstract class MappingInputRegister implements InputRegister {
    private static Logger logger = LoggerFactory.getLogger(ModbusServer.class);
    boolean useUnscaledValues;
    protected Channel channel;
    protected int highByte;
    protected int lowByte;

    public MappingInputRegister(Channel channel, int byteHigh, int byteLow) {
        this.channel = channel;
        this.highByte = byteHigh;
        this.lowByte = byteLow;

        try {
            String scalingProperty = System.getProperty(
                    "org.openmuc.framework.server.modbus.useUnscaledValues");
            useUnscaledValues = Boolean.parseBoolean(scalingProperty);
        }
        catch (Exception e) {
            /* will stick to default setting. */
        }
    }

    @Override
    public int getValue() {
        int toReturn = ByteBuffer.wrap(toBytes()).getShort(); /*
                                                             * toBytes always only contains two bytes. So cast from
															 * short.
															 */
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
    public abstract byte[] toBytes();
}
