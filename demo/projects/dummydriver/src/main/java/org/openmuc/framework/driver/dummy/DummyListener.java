/*
 * Copyright 2011-14 Fraunhofer ISE
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

package org.openmuc.framework.driver.dummy;

import org.openmuc.framework.data.DoubleValue;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.framework.driver.spi.RecordsReceivedListener;

import java.util.List;

final class DummyListener extends Thread {

    private List<ChannelRecordContainer> containers;
    private final RecordsReceivedListener listener;

    Double p = Math.random();

    public DummyListener(List<ChannelRecordContainer> containers,
                         RecordsReceivedListener listener) {
        this.containers = containers;
        this.listener = listener;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(3000);
            }
            catch (InterruptedException e) {
                return;
            }
            long receiveTime = System.currentTimeMillis();
            synchronized (containers) {
                for (ChannelRecordContainer container : containers) {
                    container.setRecord(new Record(new DoubleValue(Math.sin(p)), receiveTime));
                    p += 1.0 / 90 % 2 * Math.PI;
                }
                listener.newRecords(containers);
            }
        }
    }

    public void setNewContainers(List<ChannelRecordContainer> containers) {
        synchronized (containers) {
            this.containers = containers;
        }
    }

    public void shutdown() {
        interrupt();
    }

}
