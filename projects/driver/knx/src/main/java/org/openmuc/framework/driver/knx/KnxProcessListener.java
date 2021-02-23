/*
 * Copyright 2011-2021 Fraunhofer ISE
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
package org.openmuc.framework.driver.knx;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.driver.knx.value.KnxValue;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.framework.driver.spi.RecordsReceivedListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tuwien.auto.calimero.DetachEvent;
import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.process.ProcessEvent;
import tuwien.auto.calimero.process.ProcessListener;

public class KnxProcessListener implements ProcessListener {

    private static Logger logger = LoggerFactory.getLogger(KnxProcessListener.class);

    private List<ChannelRecordContainer> containers;
    private RecordsReceivedListener listener;
    private final Map<GroupAddress, byte[]> cachedValues;

    public KnxProcessListener() {
        cachedValues = new LinkedHashMap<>();

        containers = null;
        listener = null;
    }

    public synchronized void registerOpenMucListener(List<ChannelRecordContainer> containers,
            RecordsReceivedListener listener) {
        this.containers = containers;
        this.listener = listener;
    }

    public synchronized void unregisterOpenMucListener() {
        containers = null;
        listener = null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see tuwien.auto.calimero.process.ProcessListener#groupWrite(tuwien.auto.calimero.process.ProcessEvent)
     */
    @Override
    public void groupWrite(ProcessEvent e) {
        if (listener != null) {
            long timestamp = System.currentTimeMillis();
            for (ChannelRecordContainer container : containers) {
                KnxGroupDP groupDP = (KnxGroupDP) container.getChannelHandle();
                if (groupDP.getMainAddress().equals(e.getDestination())) {
                    KnxValue value = groupDP.getKnxValue();
                    value.setData(e.getASDU());
                    logger.debug("Group write: " + e.getDestination());

                    Record record = new Record(value.getOpenMucValue(), timestamp, Flag.VALID);

                    listener.newRecords(createNewRecords(container, record));
                    break;
                }
            }
        }
        cachedValues.put(e.getDestination(), e.getASDU());
    }

    /*
     * (non-Javadoc)
     * 
     * @see tuwien.auto.calimero.process.ProcessListener#detached(tuwien.auto.calimero.DetachEvent)
     */
    @Override
    public void detached(DetachEvent e) {

    }

    public Map<GroupAddress, byte[]> getCachedValues() {
        return cachedValues;
    }

    private static List<ChannelRecordContainer> createNewRecords(ChannelRecordContainer container, Record record) {
        List<ChannelRecordContainer> recordContainers = new ArrayList<>();
        container.setRecord(record);
        recordContainers.add(container);
        return recordContainers;
    }

}
