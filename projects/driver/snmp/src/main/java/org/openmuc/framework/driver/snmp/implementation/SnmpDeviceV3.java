/*
 * Copyright 2011-18 Fraunhofer ISE
 *
 * This file is part of OpenMUC.
 * For more information visit http://www.openmuc.org
 *
 * OpenMUC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenMUC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenMUC.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.openmuc.framework.driver.snmp.implementation;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.driver.snmp.SnmpDriver.SnmpDriverSettingVariableNames;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.snmp4j.UserTarget;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.AuthMD5;
import org.snmp4j.security.PrivDES;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.security.UsmUser;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;

public class SnmpDeviceV3 extends SnmpDevice {

    // private UserTarget target; // snmp v3 target
    private final String username;
    private final String securityName;
    private final OID authenticationProtocol;
    private final String authenticationPassphrase;
    private final OID privacyProtocol;
    private final String privacyPassphrase;

    /**
     * snmp constructor takes primary parameters in order to create snmp object. this implementation uses UDP protocol
     * 
     * @param address
     *            Contains ip and port. accepted string "X.X.X.X/portNo" or "udp:X.X.X.X/portNo"
     * @param username
     *            String containing username
     * @param securityName
     *            the security name of the user (typically the user name). [required by snmp4j library]
     * @param authenticationProtocol
     *            the authentication protocol ID to be associated with this user. If set to <code>null</code>, this user
     *            only supports unauthenticated messages. [required by snmp4j library] eg. AuthMD5.ID
     * @param authenticationPassphrase
     *            the authentication pass phrase. If not <code>null</code>, <code>authenticationProtocol</code> must
     *            also be not <code>null</code>. RFC3414 §11.2 requires pass phrases to have a minimum length of 8
     *            bytes. If the length of <code>authenticationPassphrase</code> is less than 8 bytes an
     *            <code>IllegalArgumentException</code> is thrown. [required by snmp4j library]
     * @param privacyProtocol
     *            the privacy protocol ID to be associated with this user. If set to <code>null</code>, this user only
     *            supports not encrypted messages. [required by snmp4j library] eg. PrivDES.ID
     * @param privacyPassphrase
     *            the privacy pass phrase. If not <code>null</code>, <code>privacyProtocol</code> must also be not
     *            <code>null</code>. RFC3414 §11.2 requires pass phrases to have a minimum length of 8 bytes. If the
     *            length of <code>authenticationPassphrase</code> is less than 8 bytes an
     *            <code>IllegalArgumentException</code> is thrown. [required by snmp4j library]
     * 
     * @throws ConnectionException
     *             thrown if SNMP listen or initialization failed
     * @throws ArgumentSyntaxException
     *             thrown if Device address foramt is wrong
     */

    public SnmpDeviceV3(String address, String username, String securityName, OID authenticationProtocol,
            String authenticationPassphrase, OID privacyProtocol, String privacyPassphrase)
            throws ConnectionException, ArgumentSyntaxException {
        super(address, authenticationPassphrase);

        this.username = username;
        this.securityName = securityName;
        this.authenticationProtocol = authenticationProtocol;
        this.authenticationPassphrase = authenticationPassphrase;
        this.privacyProtocol = privacyProtocol;
        this.privacyPassphrase = privacyPassphrase;
    }

    /**
     * snmp constructor takes primary parameters in order to create snmp object. this implementation uses UDP protocol
     * Default values: authenticationProtocol = AuthMD5.ID; privacyProtocol = PrivDES.ID;
     * 
     * @param address
     *            Contains ip and port. accepted string "X.X.X.X/portNo" or "udp:X.X.X.X/portNo"
     * @param username
     *            String containing username
     * @param securityName
     *            the security name of the user (typically the user name). [required by snmp4j library]
     * @param authenticationPassphrase
     *            the authentication pass phrase. If not <code>null</code>, <code>authenticationProtocol</code> must
     *            also be not <code>null</code>. RFC3414 §11.2 requires pass phrases to have a minimum length of 8
     *            bytes. If the length of <code>authenticationPassphrase</code> is less than 8 bytes an
     *            <code>IllegalArgumentException</code> is thrown. [required by snmp4j library]
     * @param privacyPassphrase
     *            the privacy pass phrase. If not <code>null</code>, <code>privacyProtocol</code> must also be not
     *            <code>null</code>. RFC3414 §11.2 requires pass phrases to have a minimum length of 8 bytes. If the
     *            length of <code>authenticationPassphrase</code> is less than 8 bytes an
     *            <code>IllegalArgumentException</code> is thrown. [required by snmp4j library]
     * 
     * @throws ConnectionException
     *             thrown if SNMP listen or initialization failed
     * @throws ArgumentSyntaxException
     *             thrown if Device address foramt is wrong
     */

    public SnmpDeviceV3(String address, String username, String securityName, String authenticationPassphrase,
            String privacyPassphrase) throws ConnectionException, ArgumentSyntaxException {
        super(address, authenticationPassphrase);

        this.username = username;
        this.securityName = securityName;
        authenticationProtocol = AuthMD5.ID;
        this.authenticationPassphrase = authenticationPassphrase;
        privacyProtocol = PrivDES.ID;
        this.privacyPassphrase = privacyPassphrase;
    }

    @Override
    void setTarget() {
        int securityLevel = -1;
        if (authenticationPassphrase == null || authenticationPassphrase.trim().equals("")) {
            // No Authentication and no Privacy
            securityLevel = SecurityLevel.NOAUTH_NOPRIV;
        }
        else {
            // With Authentication
            if (privacyPassphrase == null || privacyPassphrase.trim().equals("")) {
                // No Privacy
                securityLevel = SecurityLevel.AUTH_NOPRIV;
            }
            else {
                // With Privacy
                securityLevel = SecurityLevel.AUTH_PRIV;
            }

        }
        snmp.getUSM()
                .addUser(new OctetString(username),
                        new UsmUser(new OctetString(securityName), authenticationProtocol,
                                new OctetString(authenticationPassphrase), privacyProtocol,
                                new OctetString(privacyPassphrase)));
        // create the target
        target = new UserTarget();
        target.setAddress(targetAddress);
        target.setRetries(retries);
        target.setTimeout(timeout);
        target.setVersion(SnmpConstants.version3);
        target.setSecurityLevel(securityLevel);
        target.setSecurityName(new OctetString(securityName));
    }

    public String getInterfaceAddress() {
        return null;
    }

    @Override
    public String getDeviceAddress() {
        return targetAddress.toString();
    }

    public String getSettings() {
        return SnmpDriverSettingVariableNames.SNMP_VERSION.toString() + "=" + SNMPVersion.V3 + ":"
                + SnmpDriverSettingVariableNames.USERNAME + "=" + username + ":"
                + SnmpDriverSettingVariableNames.SECURITYNAME + "=" + securityName + ":"
                + SnmpDriverSettingVariableNames.AUTHENTICATIONPASSPHRASE + "=" + authenticationPassphrase + ":"
                + SnmpDriverSettingVariableNames.PRIVACYPASSPHRASE + "=" + privacyPassphrase;

    }

    public Object getConnectionHandle() {
        return this;
    }

}
