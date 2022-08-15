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

import java.util.concurrent.CompletableFuture;

import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish;

/**
 * MqttWriter stub that simulates successful publishes when connection is simulated as connected
 */
public class MqttWriterStub extends MqttWriter {
    public MqttWriterStub(MqttConnection connection) {
        super(connection, "test");
    }

    @Override
    CompletableFuture<Mqtt3Publish> publish(String topic, byte[] message) {
        CompletableFuture<Mqtt3Publish> future = new CompletableFuture<>();
        future.complete(Mqtt3Publish.builder().topic("test").build());
        return future;
    }
}
