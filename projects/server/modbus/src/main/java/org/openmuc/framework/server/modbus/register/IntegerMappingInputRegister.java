package org.openmuc.framework.server.modbus.register;

import org.openmuc.framework.data.IntValue;
import org.openmuc.framework.data.Value;
import org.openmuc.framework.dataaccess.Channel;

public class IntegerMappingInputRegister extends MappingInputRegister {

    public IntegerMappingInputRegister(Channel channel, int byteHigh, int byteLow) {
        super(channel, byteHigh, byteLow);
    }

    @Override
    public byte[] toBytes() {
        byte[] bytes;
        if (useUnscaledValues) {
            Value value = channel.getLatestRecord().getValue();
            bytes = new IntValue(value.asInt() / (int) channel.getScalingFactor()).asByteArray();
        } else {
            bytes = new IntValue(channel.getLatestRecord().getValue().asInt()).asByteArray();
        }
        byte[] toReturn = {bytes[highByte], bytes[lowByte]};
        return toReturn;
    }

}
