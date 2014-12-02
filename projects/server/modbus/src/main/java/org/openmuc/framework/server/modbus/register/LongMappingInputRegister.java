package org.openmuc.framework.server.modbus.register;

import org.openmuc.framework.data.LongValue;
import org.openmuc.framework.data.Value;
import org.openmuc.framework.dataaccess.Channel;

public class LongMappingInputRegister extends MappingInputRegister {

    public LongMappingInputRegister(Channel channel, int byteHigh, int byteLow) {
        super(channel, byteHigh, byteLow);
    }

    @Override
    public byte[] toBytes() {
        byte[] bytes;
        if (useUnscaledValues) {
            Value value = channel.getLatestRecord().getValue();
            bytes = new LongValue(value.asLong() / (long) channel.getScalingFactor()).asByteArray();
        } else {
            bytes = new LongValue(channel.getLatestRecord().getValue().asLong()).asByteArray();
        }
        byte[] toReturn = {bytes[highByte], bytes[lowByte]};
        return toReturn;
    }

}
