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
package org.openmuc.framework.server.asciisocket;

import org.openmuc.framework.dataaccess.Channel;

import java.io.PrintWriter;
import java.util.LinkedList;

public abstract class Report {
    private final LinkedList<Channel> data;
    private final int handle;

    public Report(int handle) {
        data = new LinkedList<Channel>();
        this.handle = handle;
    }

    public int getHandle() {
        return handle;
    }

    public void emitReport(PrintWriter out) {
        out.print("!REPORT handle:" + handle + " time:" + System.currentTimeMillis());

        for (Channel channel : data) {
            out.print(" " + channel.getId() + ":" + channel.getLatestRecord().getValue());
        }

        out.println();
    }

    public void addDataStorage(Channel ds) {
        data.add(ds);
    }
}
