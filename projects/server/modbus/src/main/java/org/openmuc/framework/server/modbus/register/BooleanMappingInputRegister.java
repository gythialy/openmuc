package org.openmuc.framework.server.modbus.register;

import org.openmuc.framework.dataaccess.Channel;

public class BooleanMappingInputRegister extends MappingInputRegister {

    public BooleanMappingInputRegister(Channel channel, int byteHigh, int byteLow) {
        super(channel, byteHigh, byteLow);
    }

    @Override
    public byte[] toBytes() {
        if (channel.getLatestRecord().getValue().asBoolean() == true) {
            byte[] bytes = {(byte) 0x01};
            return bytes;
        } else if (channel.getLatestRecord().getValue().asBoolean() == false) {
            byte[] bytes = {(byte) 0x00};
            return bytes;
        }
        return null;
    }

}
