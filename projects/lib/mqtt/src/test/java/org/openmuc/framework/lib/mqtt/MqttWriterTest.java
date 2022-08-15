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

package org.openmuc.framework.lib.mqtt;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import com.hivemq.client.internal.mqtt.MqttClientConfig;
import com.hivemq.client.mqtt.lifecycle.MqttClientConnectedListener;
import com.hivemq.client.mqtt.lifecycle.MqttClientDisconnectedContext;
import com.hivemq.client.mqtt.lifecycle.MqttClientDisconnectedListener;
import com.hivemq.client.mqtt.lifecycle.MqttClientReconnector;
import com.hivemq.client.mqtt.lifecycle.MqttDisconnectSource;

@ExtendWith(MockitoExtension.class)
public class MqttWriterTest {

    private static final String DIRECTORY = "/tmp/openmuc/mqtt_writer_test";
    private static MqttClientConnectedListener connectedListener;
    private static MqttClientDisconnectedListener disconnectedListener;
    private MqttWriter mqttWriter;

    @BeforeEach
    void setup() {
        MqttConnection connection = mock(MqttConnection.class);

        doAnswer((Answer<Void>) invocation -> {
            connectedListener = invocation.getArgument(0);
            return null;
        }).when(connection).addConnectedListener(any(MqttClientConnectedListener.class));

        doAnswer((Answer<Void>) invocation -> {
            disconnectedListener = invocation.getArgument(0);
            return null;
        }).when(connection).addDisconnectedListener(any(MqttClientDisconnectedListener.class));

        when(connection.getSettings())
                .thenReturn(new MqttSettings("localhost", 1883, null, null, false, 1, 1, 2, 5000, 10, DIRECTORY));

        mqttWriter = new MqttWriterStub(connection);
        connectedListener.onConnected(() -> null);
    }

    @AfterAll
    static void cleanUp() {
        deleteDirectory(FileSystems.getDefault().getPath(DIRECTORY).toFile());
    }

    private static void deleteDirectory(File directory) {
        if (!directory.exists()) {
            return;
        }
        for (File child : directory.listFiles()) {
            if (child.isDirectory()) {
                deleteDirectory(child);
            }
            else {
                child.delete();
            }
        }
        directory.delete();
    }

    @Test
    void testWriteWithReconnectionAndSimulatedDisconnection() throws IOException, InterruptedException {
        MqttClientDisconnectedContext disconnectedContext = mock(MqttClientDisconnectedContext.class);
        MqttClientReconnector reconnector = mock(MqttClientReconnector.class);
        when(reconnector.isReconnect()).thenReturn(true);
        MqttClientConfig config = mock(MqttClientConfig.class);
        when(config.getServerHost()).thenReturn("test");
        Throwable cause = mock(Throwable.class);
        when(cause.getMessage()).thenReturn("test");
        MqttDisconnectSource source = MqttDisconnectSource.USER;
        when(disconnectedContext.getReconnector()).thenReturn(reconnector);
        when(disconnectedContext.getClientConfig()).thenReturn(config);
        when(disconnectedContext.getCause()).thenReturn(cause);
        when(disconnectedContext.getSource()).thenReturn(source);
        disconnectedListener.onDisconnected(disconnectedContext);

        String topic = "topic1";

        File file = FileSystems.getDefault().getPath(DIRECTORY, "topic1", "buffer.0.log").toFile();
        File file1 = FileSystems.getDefault().getPath(DIRECTORY, "topic1", "buffer.1.log").toFile();

        String message300bytes = "Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Aenean commodo ligula "
                + "eget dolor. Aenean massa. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur "
                + "ridiculus mus. Donec quam felis, ultricies nec, pellentesque eu, pretium quis, sem. Nulla consequat"
                + " massa quis enim. Donec.";

        mqttWriter.write(topic, message300bytes.getBytes()); // 300
        mqttWriter.write(topic, message300bytes.getBytes()); // 600
        mqttWriter.write(topic, message300bytes.getBytes()); // 900
        // buffer limit not yet reached
        // assertFalse(file.exists() || file1.exists());
        mqttWriter.write(topic, message300bytes.getBytes()); // 1200 > 1024 write to file => 0
        // buffer limit reached, first file written
        assertTrue(file.exists() && !file1.exists());
        mqttWriter.write(topic, message300bytes.getBytes()); // 300
        mqttWriter.write(topic, message300bytes.getBytes()); // 600
        mqttWriter.write(topic, message300bytes.getBytes()); // 900
        mqttWriter.write(topic, message300bytes.getBytes()); // 1200 > 1024 write to file
        // buffer limit reached, second file written
        assertTrue(file.exists() && file1.exists());

        // simulate connection
        connectedListener.onConnected(() -> null);

        // wait for recovery thread to terminate
        Thread.sleep(1000);

        // files should be emptied and therefore removed
        assertFalse(file.exists() || file1.exists());
    }
}
