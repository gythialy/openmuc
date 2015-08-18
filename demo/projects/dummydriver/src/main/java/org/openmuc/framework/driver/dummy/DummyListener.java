/*
 * This file is part of OpenMUC.
 * For more information visit http://www.openmuc.org
 *
 * You are free to use code of this sample file in any
 * way you like and without any restrictions.
 *
 */
package org.openmuc.framework.driver.dummy;

import java.util.List;

import org.openmuc.framework.data.DoubleValue;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.framework.driver.spi.RecordsReceivedListener;

final class DummyListener extends Thread {

	private List<ChannelRecordContainer> containers;
	private final RecordsReceivedListener listener;

	Double p = Math.random();

	public DummyListener(List<ChannelRecordContainer> containers, RecordsReceivedListener listener) {
		this.containers = containers;
		this.listener = listener;
	}

	@Override
	public void run() {
		while (true) {
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
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
