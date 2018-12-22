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
package org.openmuc.framework.driver.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.codec.binary.Base64;
import org.openmuc.framework.config.ChannelScanInfo;
import org.openmuc.framework.data.BooleanValue;
import org.openmuc.framework.data.ByteArrayValue;
import org.openmuc.framework.data.ByteValue;
import org.openmuc.framework.data.DoubleValue;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.FloatValue;
import org.openmuc.framework.data.IntValue;
import org.openmuc.framework.data.LongValue;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.ShortValue;
import org.openmuc.framework.data.StringValue;
import org.openmuc.framework.data.Value;
import org.openmuc.framework.data.ValueType;
import org.openmuc.framework.dataaccess.Channel;
import org.openmuc.framework.dataaccess.DataAccessService;
import org.openmuc.framework.driver.rest.helper.JsonWrapper;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.framework.driver.spi.ChannelValueContainer;
import org.openmuc.framework.driver.spi.Connection;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.RecordsReceivedListener;
import org.openmuc.framework.lib.json.Const;

public class RestConnection implements Connection {

    private final JsonWrapper wrapper;
    private URL url;
    private URLConnection con;
    private String baseAddress;
    private final int timeout;
    private boolean isHTTPS;
    private final String authString;

    private final DataAccessService dataAccessService;
    private String connectionAddress;

    private boolean checkTimestamp = false;

    RestConnection(String deviceAddress, String credentials, int timeout, boolean checkTimestamp,
            DataAccessService dataAccessService) throws ConnectionException {

        this.checkTimestamp = checkTimestamp;
        this.dataAccessService = dataAccessService;
        this.timeout = timeout;
        wrapper = new JsonWrapper();
        authString = new String(Base64.encodeBase64(credentials.getBytes()));

        if (!deviceAddress.endsWith("/")) {
            this.baseAddress = deviceAddress + "/rest/channels/";
            this.connectionAddress = deviceAddress + "/rest/connect/";
        }
        else {
            this.baseAddress = deviceAddress + "rest/channels/";
            this.connectionAddress = deviceAddress + "rest/connect/";
        }

        if (deviceAddress.startsWith("https://")) {
            isHTTPS = true;
        }
        else {
            isHTTPS = false;
        }

        if (isHTTPS) {
            TrustManager[] trustManager = getTrustManager();

            try {
                SSLContext sc = SSLContext.getInstance("SSL");
                sc.init(null, trustManager, new java.security.SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            } catch (KeyManagementException e1) {
                throw new ConnectionException(e1.getMessage());
            } catch (NoSuchAlgorithmException e) {
                throw new ConnectionException(e.getMessage());
            }

            // Create all-trusting host name verifier
            HostnameVerifier allHostsValid = getHostnameVerifier();
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        }
    }

    private HostnameVerifier getHostnameVerifier() {
        return new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };
    }

    private TrustManager[] getTrustManager() {
        return new TrustManager[] { new X509TrustManager() {
            @Override
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
            }

            @Override
            public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
            }
        } };
    }

    private Record readChannel(String channelAddress, ValueType valueType) throws ConnectionException {
        Record newRecord = null;
        try {
            newRecord = wrapper.toRecord(get(channelAddress), valueType);
        } catch (IOException e) {
            throw new ConnectionException(e.getMessage());
        }
        return newRecord;
    }

    private long readChannelTimestamp(String channelAddress) throws ConnectionException {
        long timestamp = -1;
        try {
            if (channelAddress.endsWith("/")) {
                channelAddress += Const.TIMESTAMP;
            }
            else {
                channelAddress += '/' + Const.TIMESTAMP;
            }
            timestamp = wrapper.toTimestamp(get(channelAddress));
        } catch (IOException e) {
            throw new ConnectionException(e.getMessage());
        }
        return timestamp;
    }

    private List<ChannelScanInfo> readDeviceChannelList() throws ConnectionException {
        try {
            return wrapper.tochannelScanInfos(get(""));
        } catch (IOException e) {
            throw new ConnectionException(e.getMessage());
        }
    }

    private Flag writeChannel(String channelAddress, Value value, ValueType valueType) throws ConnectionException {
        Record remoteRecord = new Record(value, System.currentTimeMillis(), Flag.VALID);
        return put(channelAddress, wrapper.fromRecord(remoteRecord, valueType));
    }

    void connect() throws ConnectionException {
        try {
            url = new URL(connectionAddress);
            con = url.openConnection();
            setConnectionProberties();
        } catch (MalformedURLException e) {
            throw new ConnectionException("malformed URL: " + connectionAddress);
        } catch (IOException e) {
            throw new ConnectionException(e.getMessage());
        }

        try {
            con.connect();
            checkResponseCode(con);
        } catch (IOException e) {
            throw new ConnectionException(e.getMessage());
        }
    }

    private InputStream get(String suffix) throws ConnectionException {
        InputStream stream = null;
        try {
            url = new URL(baseAddress + suffix);
            con = url.openConnection();
            setConnectionProberties();
            stream = con.getInputStream();
        } catch (MalformedURLException e) {
            throw new ConnectionException("malformed URL: " + baseAddress);
        } catch (IOException e) {
            throw new ConnectionException(e.getMessage());
        }

        checkResponseCode(con);
        return stream;
    }

    private Flag put(String suffix, String output) throws ConnectionException {
        try {
            url = new URL(baseAddress + suffix);
            con = url.openConnection();
            con.setDoOutput(true);
            setConnectionProberties();
            if (isHTTPS) {
                ((HttpsURLConnection) con).setRequestMethod("PUT");
            }
            else {
                ((HttpURLConnection) con).setRequestMethod("PUT");
            }
            OutputStreamWriter out = new OutputStreamWriter(con.getOutputStream());
            out.write(output);
            out.close();
        } catch (MalformedURLException e) {
            throw new ConnectionException("malformed URL: " + baseAddress);
        } catch (IOException e) {
            throw new ConnectionException(e.getMessage());
        }

        return checkResponseCode(con);
    }

    private void setConnectionProberties() {
        con.setConnectTimeout(timeout);
        con.setReadTimeout(timeout);
        con.setRequestProperty("Connection", "Keep-Alive");
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Accept", "application/json");
        con.setRequestProperty("Authorization", "Basic " + authString);
    }

    private Flag checkResponseCode(URLConnection con) throws ConnectionException {
        int respCode;
        try {
            if (isHTTPS) {
                respCode = ((HttpsURLConnection) con).getResponseCode();
                if (!(respCode >= 200 && respCode < 300)) {
                    throw new ConnectionException(
                            "HTTPS " + respCode + ":" + ((HttpsURLConnection) con).getResponseMessage());
                }
            }
            else {
                respCode = ((HttpURLConnection) con).getResponseCode();
                if (!(respCode >= 200 && respCode < 300)) {
                    throw new ConnectionException(
                            "HTTP " + respCode + ":" + ((HttpURLConnection) con).getResponseMessage());
                }
            }
            return Flag.VALID;
        } catch (IOException e) {
            throw new ConnectionException(e.getMessage());
        }
    }

    @Override
    public void disconnect() {

        if (isHTTPS) {
            ((HttpsURLConnection) con).disconnect();
        }
        else {
            ((HttpURLConnection) con).disconnect();
        }
    }

    @Override
    public Object read(List<ChannelRecordContainer> containerList, Object obj, String arg3) throws ConnectionException {
        // TODO: add grouping (reading device/driver in once)
        for (ChannelRecordContainer container : containerList) {
            Record record;
            if (checkTimestamp) {
                String channelId = container.getChannel().getId();
                Channel channel = dataAccessService.getChannel(channelId);
                record = channel.getLatestRecord();

                if (record.getTimestamp() == null || record.getFlag() != Flag.VALID
                        || record.getTimestamp() < readChannelTimestamp(container.getChannelAddress())) {
                    record = readChannel(container.getChannelAddress(), container.getChannel().getValueType());
                }
            }
            else {
                record = readChannel(container.getChannelAddress(), container.getChannel().getValueType());
            }
            if (record != null) {
                container.setRecord(record);
            }
            else {
                container.setRecord(new Record(Flag.DRIVER_ERROR_READ_FAILURE));
            }
        }
        return null;
    }

    @Override
    public List<ChannelScanInfo> scanForChannels(String settings) throws ConnectionException {
        return readDeviceChannelList();
    }

    @Override
    public void startListening(List<ChannelRecordContainer> arg1, RecordsReceivedListener arg2)
            throws ConnectionException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object write(List<ChannelValueContainer> container, Object containerListHandle) throws ConnectionException {
        for (ChannelValueContainer cont : container) {
            Value value = cont.getValue();
            ValueType valueType = getValueType(value);
            Flag flag = writeChannel(cont.getChannelAddress(), value, valueType);
            cont.setFlag(flag);
        }
        return null;
    }

    private ValueType getValueType(Value value) {
        ValueType valueType = ValueType.DOUBLE;

        if (value instanceof DoubleValue) {
            valueType = ValueType.DOUBLE;
        }
        else if (value instanceof StringValue) {
            valueType = ValueType.STRING;
        }
        else if (value instanceof ByteArrayValue) {
            valueType = ValueType.BYTE_ARRAY;
        }
        else if (value instanceof LongValue) {
            valueType = ValueType.LONG;
        }
        else if (value instanceof BooleanValue) {
            valueType = ValueType.BOOLEAN;
        }
        else if (value instanceof FloatValue) {
            valueType = ValueType.FLOAT;
        }
        else if (value instanceof IntValue) {
            valueType = ValueType.INTEGER;
        }
        else if (value instanceof ShortValue) {
            valueType = ValueType.SHORT;
        }
        else if (value instanceof ByteValue) {
            valueType = ValueType.BYTE;
        }
        return valueType;
    }

}
