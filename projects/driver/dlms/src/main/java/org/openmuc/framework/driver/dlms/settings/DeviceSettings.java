package org.openmuc.framework.driver.dlms.settings;

import org.openmuc.framework.config.ArgumentSyntaxException;

public class DeviceSettings extends GenericSetting {

    @Option(value = "ld", range = "int")
    private int logicalDeviceAddress = 1;

    @Option("cid")
    private int clientId = 16;

    @Option("sn")
    private boolean useSn = false;

    @Option("emech")
    private int encryptionMechanism = -1;

    @Option("amech")
    private int authenticationMechanism = 0;

    @Option("ekey")
    private byte[] encryptionKey = {};

    @Option("akey")
    private byte[] authenticationKey = {};

    @Option("pass")
    private String paswd = "";

    @Option("cl")
    private int challengeLength = 16;

    @Option("rt")
    private int responseTimeout = 20_000;

    @Option("mid")
    private String manufacturerId = "MMM";

    @Option("did")
    private long deviceId = 1;

    public DeviceSettings(String settings) throws ArgumentSyntaxException {
        super.parseFields(settings);
    }

    public int getLogicalDeviceAddress() {
        return logicalDeviceAddress;
    }

    public int getClientId() {
        return clientId;
    }

    public boolean useSn() {
        return useSn;
    }

    public int getEncryptionMechanism() {
        return encryptionMechanism;
    }

    public int getAuthenticationMechanism() {
        return authenticationMechanism;
    }

    public String getPassword() {
        return paswd;
    }

    public byte[] getEncryptionKey() {
        return encryptionKey;
    }

    public byte[] getAuthenticationKey() {
        return authenticationKey;
    }

    public int getChallengeLength() {
        return challengeLength;
    }

    public int getResponseTimeout() {
        return responseTimeout;
    }

    public String getManufacturerId() {
        return manufacturerId;
    }

    public long getDeviceId() {
        return deviceId;
    }

}
