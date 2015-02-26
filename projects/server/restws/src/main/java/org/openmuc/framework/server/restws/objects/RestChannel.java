package org.openmuc.framework.server.restws.objects;

import org.openmuc.framework.data.Record;

public class RestChannel {

    private String id;
    private Record record;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Record getRecord() {
        return record;
    }

    public void setRecord(Record record) {
        this.record = record;
    }

}
