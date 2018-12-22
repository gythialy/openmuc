package org.openmuc.framework.server.modbus;

class ServerSettings {

    private int unitId = 15;
    private int port = 502;
    private String address = "127.0.0.1";
    private boolean master = false;

    private static final String NAME = ServerSettings.class.getPackage().getName().toLowerCase();

    private static final String PORT_STRING = System.getProperty(NAME + ".port");
    private static final String ADDRESS = System.getProperty(NAME + ".address");
    private static final String UNITID_STRING = System.getProperty(NAME + ".unitId");
    private static final String MASTER_STRING = System.getProperty(NAME + ".master");

    ServerSettings() {
        if (PORT_STRING != null) {
            port = Integer.parseInt(PORT_STRING);
        }
        if (UNITID_STRING != null) {
            unitId = Integer.parseInt(UNITID_STRING);
        }
        if (ADDRESS != null) {
            address = ADDRESS;
        }
        if (MASTER_STRING != null) {
            master = Boolean.parseBoolean(MASTER_STRING);
        }
    }

    int getUnitId() {
        return unitId;
    }

    int getPort() {
        return port;
    }

    String getAddress() {
        return address;
    }

    boolean isMaster() {
        return master;
    }

}
