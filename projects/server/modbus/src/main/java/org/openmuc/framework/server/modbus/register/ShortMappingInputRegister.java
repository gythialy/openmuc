package org.openmuc.framework.server.modbus.register;

import org.openmuc.framework.data.ShortValue;
import org.openmuc.framework.data.Value;
import org.openmuc.framework.dataaccess.Channel;

public class ShortMappingInputRegister extends MappingInputRegister {

    public ShortMappingInputRegister(Channel channel, int byteHigh, int byteLow) {
        super(channel, byteHigh, byteLow);
    }

    @Override
    public byte[] toBytes() {
        byte[] bytes;
        if (useUnscaledValues) {
            Value value = channel.getLatestRecord().getValue();
            bytes = new ShortValue((short) (value.asShort() / (short) channel.getScalingFactor())).asByteArray();
        } else {
            bytes = new ShortValue(channel.getLatestRecord().getValue().asShort()).asByteArray();
        }
        byte[] toReturn = {bytes[highByte], bytes[lowByte]};
        return toReturn;
    }

}
