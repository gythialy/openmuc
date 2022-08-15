/*
 * Copyright 2011-2022 Fraunhofer ISE
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

package org.openmuc.framework.driver.iec61850;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import javax.naming.ConfigurationException;

import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmuc.framework.config.ChannelScanInfo;
import org.openmuc.framework.data.BooleanValue;
import org.openmuc.framework.data.ByteArrayValue;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.FloatValue;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.Value;
import org.openmuc.framework.dataaccess.Channel;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.framework.driver.spi.ChannelValueContainer;
import org.openmuc.framework.driver.spi.ConnectionException;

import com.beanit.iec61850bean.BasicDataAttribute;
import com.beanit.iec61850bean.ClientAssociation;
import com.beanit.iec61850bean.ClientEventListener;
import com.beanit.iec61850bean.ClientSap;
import com.beanit.iec61850bean.Fc;
import com.beanit.iec61850bean.FcModelNode;
import com.beanit.iec61850bean.ModelNode;
import com.beanit.iec61850bean.Report;
import com.beanit.iec61850bean.SclParseException;
import com.beanit.iec61850bean.SclParser;
import com.beanit.iec61850bean.ServerEventListener;
import com.beanit.iec61850bean.ServerModel;
import com.beanit.iec61850bean.ServerSap;
import com.beanit.iec61850bean.ServiceError;

public class Iec61850ConnectionTest extends Thread implements ClientEventListener, ServerEventListener {

    int port = TestHelper.getAvailablePort();
    String host = "127.0.0.1";
    ClientSap clientSap = new ClientSap();
    ServerSap serverSap = null;
    ClientAssociation clientAssociation = null;
    ServerModel serversServerModel = null;

    @BeforeEach
    public void initialize() throws SclParseException, IOException {
        clientSap.setTSelRemote(new byte[] { 0, 1 });
        clientSap.setTSelLocal(new byte[] { 0, 0 });

        // ---------------------------------------------------
        // ----------------- Start test server------------------
        serverSap = TestHelper.runServer("src/test/resources/testOpenmuc.icd", port, serverSap, serversServerModel,
                this);
        start();
        System.out.println("IED Server is running");
        clientSap.setApTitleCalled(new int[] { 1, 1, 999, 1 });
    }

    @Test
    public void testScanForChannels()
            throws IOException, ServiceError, ConfigurationException, javax.naming.ConfigurationException,
            SclParseException, InterruptedException, UnsupportedOperationException, ConnectionException {

        System.out.println("Attempting to connect to server " + host + " on port " + port);

        clientAssociation = clientSap.associate(InetAddress.getByName(host), port, null, this);

        ServerModel serverModel = SclParser.parse("src/test/resources/testOpenmuc.icd").get(0);
        clientAssociation.setServerModel(serverModel);
        getAllBdas(serverModel, clientAssociation);

        Iec61850Connection testConnection = new Iec61850Connection(clientAssociation, serverModel);
        List<ChannelScanInfo> testChannelScanList = testConnection.scanForChannels("");

        for (int i = 14; i < 23; i++) {
            System.out.print(testChannelScanList.get(i).getChannelAddress() + "\n");
            Assert.assertEquals("BYTE_ARRAY", testChannelScanList.get(i).getValueType().toString());
        }
        Assert.assertEquals("LONG", testChannelScanList.get(23).getValueType().toString());
        Assert.assertEquals("BOOLEAN", testChannelScanList.get(24).getValueType().toString());
        Assert.assertEquals("FLOAT", testChannelScanList.get(25).getValueType().toString());
        Assert.assertEquals("DOUBLE", testChannelScanList.get(26).getValueType().toString());
        Assert.assertEquals("BYTE", testChannelScanList.get(27).getValueType().toString());
        Assert.assertEquals("SHORT", testChannelScanList.get(28).getValueType().toString());
        Assert.assertEquals("SHORT", testChannelScanList.get(29).getValueType().toString());
        Assert.assertEquals("INTEGER", testChannelScanList.get(30).getValueType().toString());
        Assert.assertEquals("INTEGER", testChannelScanList.get(31).getValueType().toString());
        Assert.assertEquals("LONG", testChannelScanList.get(32).getValueType().toString());
        Assert.assertEquals("LONG", testChannelScanList.get(33).getValueType().toString());
        Assert.assertEquals("BYTE_ARRAY", testChannelScanList.get(34).getValueType().toString());

    }

    @Test
    public void testRead()
            throws IOException, ServiceError, ConfigurationException, javax.naming.ConfigurationException,
            SclParseException, InterruptedException, UnsupportedOperationException, ConnectionException {

        System.out.println("Attempting to connect to server " + host + " on port " + port);

        clientAssociation = clientSap.associate(InetAddress.getByName(host), port, null, this);

        ServerModel serverModel = SclParser.parse("src/test/resources/testOpenmuc.icd").get(0);
        clientAssociation.setServerModel(serverModel);
        getAllBdas(serverModel, clientAssociation);
        // ------------SCAN FOR CHANNELS-------------------
        Iec61850Connection testIec61850Connection = new Iec61850Connection(clientAssociation, serverModel);
        List<ChannelRecordContainer> testRecordContainers = new ArrayList<>();
        List<ChannelScanInfo> testChannelScanList = testIec61850Connection.scanForChannels("");

        for (int i = 14; i < 34; i++) {
            testRecordContainers.add(new ChannelRecordContainerImpl(testChannelScanList.get(i).getChannelAddress()));
        }
        // ----------READ-------------------
        testIec61850Connection.read(testRecordContainers, null, "");

        System.out.print("recordContainer:" + testRecordContainers.get(0).getRecord() + "\n");
        Assert.assertEquals("[64]", testRecordContainers.get(0).getRecord().getValue().toString());

        System.out.print("recordContainer:" + testRecordContainers.get(0).getRecord() + "\n");
        Assert.assertEquals("[64]", testRecordContainers.get(0).getRecord().getValue().toString());

    }

    @Test
    public void testWrite()
            throws IOException, ServiceError, ConfigurationException, javax.naming.ConfigurationException,
            SclParseException, InterruptedException, UnsupportedOperationException, ConnectionException {

        System.out.println("Attempting to connect to server " + host + " on port " + port);

        clientAssociation = clientSap.associate(InetAddress.getByName(host), port, null, this);

        ServerModel serverModel = SclParser.parse("src/test/resources/testOpenmuc.icd").get(0);
        clientAssociation.setServerModel(serverModel);
        getAllBdas(serverModel, clientAssociation);

        // ------------SCAN FOR CHANNELS-------------------

        Iec61850Connection testIec61850Connection = new Iec61850Connection(clientAssociation, serverModel);
        List<ChannelScanInfo> testChannelScanList = testIec61850Connection.scanForChannels("");

        // ----------WRITE-----------------
        List<ChannelValueContainer> testChannelValueContainers = new ArrayList<>();

        byte[] newValue = { 0x44 };
        testChannelValueContainers.add(new ChannelValueContainerImpl(testChannelScanList.get(14).getChannelAddress(),
                new ByteArrayValue(newValue)));
        testChannelValueContainers.add(new ChannelValueContainerImpl(testChannelScanList.get(25).getChannelAddress(),
                new FloatValue((float) 12.5)));
        testChannelValueContainers.add(
                new ChannelValueContainerImpl(testChannelScanList.get(24).getChannelAddress(), new BooleanValue(true)));

        testIec61850Connection.write(testChannelValueContainers, null);

        // Create record container to read the changes made by "write"
        List<ChannelRecordContainer> testRecordContainers = new ArrayList<>();
        for (int i = 0; i < 34; i++) {
            testRecordContainers.add(new ChannelRecordContainerImpl(testChannelScanList.get(i).getChannelAddress()));
        }
        testIec61850Connection.read(testRecordContainers, null, "");

        Assert.assertEquals("[68]", testRecordContainers.get(14).getRecord().getValue().toString());
        Assert.assertEquals("12.5", testRecordContainers.get(25).getRecord().getValue().toString());
        Assert.assertEquals("true", testRecordContainers.get(24).getRecord().getValue().toString());
    }

    @AfterEach
    public void closeServerSap() {
        clientAssociation.disconnect();
        serverSap.stop();

    }

    private void getAllBdas(ServerModel serverModel, ClientAssociation clientAssociation)
            throws ServiceError, IOException {

        for (ModelNode ld : serverModel) {
            for (ModelNode ln : ld) {
                getDataRecursive(ln, clientAssociation);
            }
        }
    }

    private static void getDataRecursive(ModelNode modelNode, ClientAssociation clientAssociation)
            throws ServiceError, IOException {
        if (modelNode.getChildren() == null) {
            return;
        }
        for (ModelNode childNode : modelNode) {
            FcModelNode fcChildNode = (FcModelNode) childNode;
            if (fcChildNode.getFc() != Fc.CO) {
                System.out.println("calling GetDataValues(" + childNode.getReference() + ")");
                clientAssociation.getDataValues(fcChildNode);
            }
            // clientAssociation.setDataValues(fcChildNode);
            getDataRecursive(childNode, clientAssociation);
        }
    }

    @Override
    public List<ServiceError> write(List<BasicDataAttribute> bdas) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void serverStoppedListening(ServerSap serverSAP) {
        // TODO Auto-generated method stub

    }

    @Override
    public void newReport(Report report) {
        // TODO Auto-generated method stub

    }

    @Override
    public void associationClosed(IOException e) {
        // TODO Auto-generated method stub

    }

    public static class ChannelRecordContainerImpl implements ChannelRecordContainer {

        private Object channelHandle;
        private Record record;
        private final String channelAddress;

        public ChannelRecordContainerImpl(String channelAddress) {
            this.channelAddress = channelAddress;
        }

        @Override
        public Record getRecord() {
            return this.record;
        }

        @Override
        public Channel getChannel() {
            return null;
        }

        @Override
        public String getChannelAddress() {
            return this.channelAddress;
        }

        @Override
        public Object getChannelHandle() {
            return this.channelHandle;
        }

        @Override
        public void setRecord(Record record) {
            this.record = record;
        }

        @Override
        public void setChannelHandle(Object handle) {
            this.channelHandle = handle;
        }

        @Override
        public ChannelRecordContainer copy() {
            return null;
        }

    }

    private static class ChannelValueContainerImpl implements ChannelValueContainer {

        private final String channelAddress;
        private final Value value;
        private Flag flag;

        public ChannelValueContainerImpl(String channelAddress, Value value) {
            this.channelAddress = channelAddress;
            this.value = value;
        }

        @Override
        public String getChannelAddress() {
            return this.channelAddress;
        }

        @Override
        public Object getChannelHandle() {
            return null;
        }

        @Override
        public void setChannelHandle(Object handle) {

        }

        @Override
        public Value getValue() {
            return this.value;
        }

        @Override
        public void setFlag(Flag flag) {
            this.flag = flag;
        }

        @Override
        public Flag getFlag() {
            return flag;
        }

    }

}
