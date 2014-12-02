package org.openmuc.framework.server.modbus.register;

import org.openmuc.framework.data.DoubleValue;
import org.openmuc.framework.data.Value;
import org.openmuc.framework.dataaccess.Channel;

public class DoubleMappingInputRegister extends MappingInputRegister {

    public DoubleMappingInputRegister(Channel channel, int byteHigh, int byteLow) {
        super(channel, byteHigh, byteLow);
    }

    @Override
    public byte[] toBytes() {
        byte[] bytes;
        if (useUnscaledValues) {
            Value value = channel.getLatestRecord().getValue();
            bytes = new DoubleValue(value.asDouble() / channel.getScalingFactor()).asByteArray();
        } else {
            bytes = new DoubleValue(channel.getLatestRecord().getValue().asDouble()).asByteArray();
        }
        byte[] toReturn = {bytes[highByte], bytes[lowByte]};
        return toReturn;
    }

}
