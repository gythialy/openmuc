package org.openmuc.framework.server.restws.objects;

import org.openmuc.framework.data.Flag;

public class RestRecord {

    private Long timestamp;
    private Flag flag;
    private Object value;

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public Flag getFlag() {
        return flag;
    }

    public void setFlag(Flag flag) {
        this.flag = flag;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

}
