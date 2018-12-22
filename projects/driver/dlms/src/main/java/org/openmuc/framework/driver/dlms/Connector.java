package org.openmuc.framework.driver.dlms;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.xml.bind.DatatypeConverter;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.driver.dlms.settings.DeviceAddress;
import org.openmuc.framework.driver.dlms.settings.DeviceSettings;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.jdlms.AuthenticationMechanism;
import org.openmuc.jdlms.ConnectionBuilder;
import org.openmuc.jdlms.DlmsConnection;
import org.openmuc.jdlms.SecuritySuite;
import org.openmuc.jdlms.SecuritySuite.EncryptionMechanism;
import org.openmuc.jdlms.SerialConnectionBuilder;
import org.openmuc.jdlms.TcpConnectionBuilder;
import org.openmuc.jdlms.settings.client.ReferencingMethod;

class Connector {

    public static DlmsConnection buildDlmsConection(DeviceAddress deviceAddress, DeviceSettings deviceSettings)
            throws ArgumentSyntaxException, ConnectionException {

        ReferencingMethod refMethod = extractReferencingMethod(deviceSettings);

        try {
            return newConnectionBuilder(deviceAddress, deviceSettings)
                    .setSecuritySuite(setSecurityLevel(deviceSettings))
                    .setChallengeLength(deviceSettings.getChallengeLength())
                    .setClientId(deviceSettings.getClientId())
                    .setSystemTitle(deviceSettings.getManufacturerId(), deviceSettings.getDeviceId())
                    .setLogicalDeviceId(deviceSettings.getLogicalDeviceAddress())
                    .setPhysicalDeviceAddress(deviceAddress.getPhysicalDeviceAddress())
                    .setResponseTimeout(deviceSettings.getResponseTimeout())
                    .setSystemTitle(deviceSettings.getManufacturerId(), deviceSettings.getDeviceId())
                    .setReferencingMethod(refMethod)
                    .build();
        } catch (IOException e) {
            throw new ConnectionException(e.getMessage());
        }
    }

    private static ConnectionBuilder<?> newConnectionBuilder(DeviceAddress deviceAddress, DeviceSettings deviceSettings)
            throws ArgumentSyntaxException {

        switch (deviceAddress.getConnectionType().toLowerCase()) {
        case "tcp":
            return newTcpConectionBuilderFor(deviceAddress, deviceSettings);

        case "serial":
            return newSerialConectionBuilderFor(deviceAddress);

        default:
            throw new ArgumentSyntaxException("Only TCP and Serial are supported connection types.");
        }
    }

    private static ReferencingMethod extractReferencingMethod(DeviceSettings deviceSettings) {
        if (deviceSettings.useSn()) {
            return ReferencingMethod.SHORT;
        }
        else {
            return ReferencingMethod.LOGICAL;
        }
    }

    private static ConnectionBuilder<TcpConnectionBuilder> newTcpConectionBuilderFor(DeviceAddress deviceAddress,
            DeviceSettings deviceSettings) {
        TcpConnectionBuilder tcpConnectionBuilder = new TcpConnectionBuilder(deviceAddress.getHostAddress())
                .setPort(deviceAddress.getPort());
        if (deviceAddress.useHdlc()) {
            tcpConnectionBuilder.useHdlc();
        }
        else {
            tcpConnectionBuilder.useWrapper();
        }
        return tcpConnectionBuilder;
    }

    private static ConnectionBuilder<?> newSerialConectionBuilderFor(DeviceAddress deviceAddress) {
        SerialConnectionBuilder serialConnectionBuilder = new SerialConnectionBuilder(deviceAddress.getSerialPort())
                .setBaudRate(deviceAddress.getBaudrate())
                .setBaudRateChangeTime(deviceAddress.getBaudRateChangeDelay())
                .setIec21Address(deviceAddress.getIec21Address());

        if (deviceAddress.enableBaudRateHandshake()) {
            serialConnectionBuilder.enableHandshake();
        }
        else {
            serialConnectionBuilder.disableHandshake();
        }
        return serialConnectionBuilder;
    }

    private static SecuritySuite setSecurityLevel(DeviceSettings deviceSettings) throws ArgumentSyntaxException {
        EncryptionMechanism encryptionMechanism;
        AuthenticationMechanism authenticationMechanism;

        try {
            encryptionMechanism = EncryptionMechanism.getInstance(deviceSettings.getEncryptionMechanism());
        } catch (IllegalArgumentException e) {
            throw new ArgumentSyntaxException(
                    "The given Encryption Mechanism " + deviceSettings.getEncryptionMechanism() + " is unknown.");
        }
        try {
            authenticationMechanism = AuthenticationMechanism.forId(deviceSettings.getAuthenticationMechanism());
        } catch (IllegalArgumentException e) {
            throw new ArgumentSyntaxException(
                    "The given Authentication Mechanism " + deviceSettings.getEncryptionMechanism() + " is unknown.");
        }
        byte[] encryptionKeyBytes = null;
        if ((encryptionMechanism == EncryptionMechanism.AES_GMC_128)
                || authenticationMechanism == AuthenticationMechanism.HLS5_GMAC) {
            encryptionKeyBytes = deviceSettings.getEncryptionKey();
        }

        byte[] authKeyData = extractAuthKey(deviceSettings, encryptionMechanism, authenticationMechanism);

        byte[] pwData = extractPassword(deviceSettings, authenticationMechanism);

        return SecuritySuite.builder()
                .setAuthenticationMechanism(authenticationMechanism)
                .setEncryptionMechanism(encryptionMechanism)
                .setAuthenticationKey(authKeyData)
                .setGlobalUnicastEncryptionKey(encryptionKeyBytes)
                .setPassword(pwData)
                .build();
    }

    private static byte[] extractAuthKey(DeviceSettings deviceSettings, EncryptionMechanism encryptionMechanism,
            AuthenticationMechanism authenticationMechanism) {
        boolean hlsAuth = encryptionMechanism == EncryptionMechanism.AES_GMC_128
                || authenticationMechanism == AuthenticationMechanism.HLS5_GMAC;

        if (hlsAuth) {
            return deviceSettings.getAuthenticationKey();
        }
        else {
            return null;
        }
    }

    private static byte[] extractPassword(DeviceSettings deviceSettings,
            AuthenticationMechanism authenticationMechanism) {
        if (authenticationMechanism != AuthenticationMechanism.LOW) {
            return null;
        }

        if (deviceSettings.getPassword().startsWith("0x")) {
            String hexStr = deviceSettings.getPassword().substring(2);
            return DatatypeConverter.parseHexBinary(hexStr);
        }
        else {
            return deviceSettings.getPassword().getBytes(StandardCharsets.US_ASCII);
        }

    }

    /**
     * Don't let anyone instantiate this class.
     */
    private Connector() {
    }

}
