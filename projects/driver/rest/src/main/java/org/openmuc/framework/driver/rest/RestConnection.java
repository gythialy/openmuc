/*
 * Copyright 2011-16 Fraunhofer ISE
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
import org.openmuc.framework.data.FloatValue;
import org.openmuc.framework.data.IntValue;
import org.openmuc.framework.data.LongValue;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.ShortValue;
import org.openmuc.framework.data.StringValue;
import org.openmuc.framework.data.Value;
import org.openmuc.framework.data.ValueType;
import org.openmuc.framework.driver.rest.helper.JsonWrapper;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.framework.driver.spi.ChannelValueContainer;
import org.openmuc.framework.driver.spi.Connection;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.RecordsReceivedListener;

public class RestConnection implements Connection {

    private JsonWrapper wrapper;
    private URL url;
    private URLConnection con;
    private String deviceAddress;
    private int timeout;
    private boolean isHTTPS;
    private String authString;

    // private final static Logger logger = LoggerFactory.getLogger(RestConnection.class);

    RestConnection(String deviceAddress, String credentials, int timeout) throws ConnectionException {

        this.timeout = timeout;
        wrapper = new JsonWrapper();
        authString = new String(Base64.encodeBase64(credentials.getBytes()));

        if (!deviceAddress.endsWith("/")) {
            this.deviceAddress = deviceAddress + "/channels/";
        }
        else {
            this.deviceAddress = deviceAddress + "channels/";
        }

        if (deviceAddress.startsWith("https://")) {
            isHTTPS = true;
        }
        else {
            isHTTPS = false;
        }

        if (isHTTPS) {
            TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
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

            try {
                SSLContext sc = SSLContext.getInstance("SSL");
                sc.init(null, trustAllCerts, new java.security.SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            } catch (KeyManagementException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }

            // Create all-trusting host name verifier
            HostnameVerifier allHostsValid = new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };

            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
            // HttpsURLConnection.setFollowRedirects(false);
        }
    }

    private Record readChannel(String channelAddress, ValueType valueType) throws ConnectionException {

        Record newRecord = null;
        try {
            newRecord = wrapper.toRecord(get(channelAddress), valueType);
        } catch (IOException e) {
            e.printStackTrace();
            throw new ConnectionException(e.getMessage());
        }
        return newRecord;
    }

    private List<ChannelScanInfo> readChannels() throws ConnectionException {

        List<ChannelScanInfo> remoteChannels = null;
        try {
            remoteChannels = wrapper.toChannelList(get(""));
        } catch (IOException e) {
            e.printStackTrace();
            throw new ConnectionException(e.getMessage());
        }
        return remoteChannels;
    }

    private void writeChannel(String channelAddress, Value value, ValueType valueType) throws ConnectionException {

        Record remoteRecord = new Record(value, System.currentTimeMillis());
        put(channelAddress, wrapper.fromRecord(remoteRecord, valueType));
    }

    void connect() throws ConnectionException {

        try {
            url = new URL(deviceAddress);
            con = url.openConnection();
            con.setRequestProperty("Connection", "Keep-Alive");
            con.setConnectTimeout(timeout);
            con.setReadTimeout(timeout);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            throw new ConnectionException("malformed URL: " + deviceAddress);
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
            url = new URL(deviceAddress + suffix);
            con = url.openConnection();
            con.setRequestProperty("Connection", "Keep-Alive");
            con.setConnectTimeout(timeout);
            con.setReadTimeout(timeout);
            stream = con.getInputStream();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            throw new ConnectionException("malformed URL: " + deviceAddress);
        } catch (IOException e) {
            e.printStackTrace();
            throw new ConnectionException(e.getMessage());
        }

        checkResponseCode(con);
        return stream;
    }

    private void put(String suffix, String output) throws ConnectionException {

        try {
            url = new URL(deviceAddress + suffix);
            con = url.openConnection();
            con.setConnectTimeout(timeout);
            con.setDoOutput(true);
            con.setRequestProperty("Connection", "Keep-Alive");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Accept", "application/json");
            con.setRequestProperty("Authorization", "Basic " + authString);
            con.setReadTimeout(timeout);
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
            e.printStackTrace();
            throw new ConnectionException("malformed URL: " + deviceAddress);
        } catch (IOException e) {
            e.printStackTrace();
            throw new ConnectionException(e.getMessage());
        }

        checkResponseCode(con);
    }

    private void checkResponseCode(URLConnection con) throws ConnectionException {

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
        } catch (IOException e) {
            e.printStackTrace();
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
    public Object read(List<ChannelRecordContainer> container, Object obj, String arg3)
            throws UnsupportedOperationException, ConnectionException {

        for (ChannelRecordContainer cont : container) {

            Record record = readChannel(cont.getChannelAddress(), cont.getChannel().getValueType());

            if (record != null) {
                cont.setRecord(record);
            }
        }
        return null;
    }

    @Override
    public List<ChannelScanInfo> scanForChannels(String settings)
            throws UnsupportedOperationException, ConnectionException {

        return readChannels();
    }

    @Override
    public void startListening(List<ChannelRecordContainer> arg1, RecordsReceivedListener arg2)
            throws UnsupportedOperationException, ConnectionException {

        throw new UnsupportedOperationException();
    }

    @Override
    public Object write(List<ChannelValueContainer> container, Object containerListHandle)
            throws UnsupportedOperationException, ConnectionException {

        for (ChannelValueContainer cont : container) {
            Value value = cont.getValue();

            if (value instanceof DoubleValue) {
                writeChannel(cont.getChannelAddress(), cont.getValue(), ValueType.DOUBLE);
            }
            else if (value instanceof StringValue) {
                writeChannel(cont.getChannelAddress(), cont.getValue(), ValueType.STRING);
            }
            else if (value instanceof ByteArrayValue) {
                writeChannel(cont.getChannelAddress(), cont.getValue(), ValueType.BYTE_ARRAY);
            }
            else if (value instanceof LongValue) {
                writeChannel(cont.getChannelAddress(), cont.getValue(), ValueType.LONG);
            }
            else if (value instanceof BooleanValue) {
                writeChannel(cont.getChannelAddress(), cont.getValue(), ValueType.BOOLEAN);
            }
            else if (value instanceof FloatValue) {
                writeChannel(cont.getChannelAddress(), cont.getValue(), ValueType.FLOAT);
            }
            else if (value instanceof IntValue) {
                writeChannel(cont.getChannelAddress(), cont.getValue(), ValueType.INTEGER);
            }
            else if (value instanceof ShortValue) {
                writeChannel(cont.getChannelAddress(), cont.getValue(), ValueType.SHORT);
            }
            else if (value instanceof ByteValue) {
                writeChannel(cont.getChannelAddress(), cont.getValue(), ValueType.BYTE);
            }
        }
        return null;
    }
}
