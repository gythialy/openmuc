/*
 * Copyright 2011-15 Fraunhofer ISE
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
package org.openmuc.framework.driver.canopen;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import org.openmuc.framework.config.ChannelScanInfo;
import org.openmuc.framework.data.Record;
import org.openmuc.jcanopen.datatypes.Unsigned16;
import org.openmuc.jcanopen.exc.CanException;
import org.openmuc.jcanopen.pdo.PDOMapping;
import org.openmuc.jcanopen.pdo.PDOObject;
import org.openmuc.jcanopen.pdo.PDOParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Frederic Robra
 * 
 */
public class ChannelScanner implements Callable<List<ChannelScanInfo>> {

	private static Logger logger = LoggerFactory.getLogger(ChannelScanner.class);

	private final int from;
	private final int to;
	private final CanopenConnection canConnection;

	public ChannelScanner(int from, int to, CanopenConnection canConnection) {
		this.from = from;
		this.to = to;
		this.canConnection = canConnection;
	}

	@Override
	public List<ChannelScanInfo> call() throws Exception {
		List<ChannelScanInfo> infos = new LinkedList<ChannelScanInfo>();
		for (int i = from; i < to; i++) {
			String channelSyntax = "SDO:" + i + ":0x1000:0x0";
			try {
				Record record = canConnection.readSDO(new SDOObject(channelSyntax), 1000);
				String info = "CAN ID: " + i + " - CANopen DS-" + Unsigned16.parse(record.getValue().asByteArray());
				logger.info("found channel {} device info: {}", channelSyntax, info);
				infos.add(new ChannelScanInfo(channelSyntax, info, null, null));

				/*
				 * Search for all possible PDOs, by iterating through all possible mappings. getPDOMapping() will throw
				 * an exception if no mapping is found
				 */
				for (int j = 0; j < 0x1FF; j++) {
					PDOParameter parameter = new PDOParameter(canConnection.link, i, j);
					PDOMapping mapping = parameter.getPDOMapping();
					for (int k = 0; k < mapping.getMappedObjects().length; k++) {
						PDOObject object = mapping.getMappedObjects()[k];
						System.out.println(mapping.getCobId() + " - " + object);
						StringBuilder address = new StringBuilder();
						address.append("PDO:0x").append(Integer.toHexString(mapping.getCobId()).toUpperCase());
						address.append(":").append(k).append(":");
						address.append(object.getLength());
						StringBuilder description = new StringBuilder();
						description.append("Mapped object: ").append(object);
						if (!parameter.isValid()) {
							description.append(" PDO is disabled");
						}
						infos.add(new ChannelScanInfo(address.toString(), description.toString(), null, null));
					}

				}

			} catch (CanException e) {
				logger.trace("{} not found: {}", channelSyntax, e.getMessage());
			}
		}
		return infos;
	}

}
