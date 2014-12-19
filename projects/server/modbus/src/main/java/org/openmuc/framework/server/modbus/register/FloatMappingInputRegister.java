package org.openmuc.framework.server.modbus.register;

import org.openmuc.framework.data.FloatValue;
import org.openmuc.framework.data.Value;
import org.openmuc.framework.dataaccess.Channel;

public class FloatMappingInputRegister extends MappingInputRegister {

    public FloatMappingInputRegister(Channel channel, int byteHigh, int byteLow) {
        super(channel, byteHigh, byteLow);
    }

    @Override
    public byte[] toBytes() {
        byte[] bytes;
        if (useUnscaledValues) {
            Value value = channel.getLatestRecord().getValue();
            bytes = new FloatValue(value.asFloat() / (float) channel.getScalingFactor()).asByteArray();
        } else {
            bytes = new FloatValue(channel.getLatestRecord().getValue().asFloat()).asByteArray();
        }
        byte[] toReturn = {bytes[highByte], bytes[lowByte]};
        return toReturn;
    }

}
