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

package org.openmuc.framework.webui.dataexporter;

import org.openmuc.framework.data.Record;
import org.openmuc.framework.dataaccess.Channel;
import org.openmuc.framework.dataaccess.DataAccessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.util.DateParser;

import java.io.PrintWriter;
import java.util.*;

public final class CsvDataExport {

    private final static Logger logger = LoggerFactory.getLogger(DataExporter.class);

    DataAccessService dataAccessService;

    public CsvDataExport(DataAccessService dataAccessService) {
        this.dataAccessService = dataAccessService;
    }

    public void exportData(String[] labels, PrintWriter pw, Long start, Long stop, int dateFormat) {
        Vector<List<Record>> allValues = new Vector<List<Record>>();

        Channel channel;

        for (String label : labels) {
            channel = dataAccessService.getChannel(label);
            List<Record> records;
            try {
                records = channel.getLoggedRecords(start, stop);
            } catch (Exception e) {
                logger.error(e.getMessage());
                continue;
            }
            allValues.add(records);
        }

        printFile(labels, pw, dateFormat, allValues);
    }

    private void printFile(String[] labels, PrintWriter pw, int dateFormat, List<List<Record>> allValues) {
        /* Write Header */
        switch (dateFormat) {
            case 1:
                pw.print("#ISOTIME UNIXTIME ");
                break;
            case 2:
                pw.print("#ISOTIME ");
                break;
            case 3:
                pw.print("#UNIXTIME ");
                break;
            case 4:
                pw.print("#JAVATIME ");
                break;
        }

        for (String label : labels) {
            pw.print(label + ' ');
        }

        pw.println();

        Date date = new Date();

		/* BitSet size = List count */
        BitSet bs = new BitSet(allValues.size());

        int pointer[] = new int[allValues.size()]; /*
                                                     * pointer to current field in each list
													 */
        for (int i = 0; i < pointer.length; i++) {
            pointer[i] = 0; /* starting at 0 */
        }

        int limits[] = new int[allValues.size()]; /*
												 * maximum number of elements in one list
												 */
        for (int i = 0; i < limits.length; i++) {
            limits[i] = allValues.get(i).size(); /*
												 * initialize with sizes of each list
												 */
        }

        long ts; /* timestamp for comparison */
        long smallestts = 0; /* smalles timestamp in a row */

        do {
            smallestts = Long.MAX_VALUE;
            for (int i = 0; i < pointer.length; i++) { /*
														 * iterate over all lists, find smallest timestamps
														 */
                if (pointer[i] != limits[i]) { /* Elements left in List<Value>? */
                    ts = allValues.get(i).get(pointer[i]).getTimestamp();
                } else {
                    ts = Long.MAX_VALUE;
                }

                if (ts < smallestts) { /* new smallest element found in row */
                    bs.set(i); /* mark spot */
                    smallestts = ts;
                    for (int k = 0; k < i; k++) {
						/* clear left side of BitSet until here */
                        bs.clear(k);
                    }
                } else if (ts == smallestts) { /* another smallest timestamp found */
                    bs.set(i);
                } else {
                    bs.clear(i);
                }
            }

			/*
			 * Now BitSet looks something like this: BitSet: [false, false, false, true, true, false, false] "true"
			 * marks the lists that contain a Value for same smallest timestamp.
			 */

			/* write dateFormat column */
            switch (dateFormat) {
                case 1:
                    date.setTime(smallestts);
                    pw.print(DateParser.getIsoDate(date));
                    pw.print(' ');
                    pw.print(smallestts / 1000);
                    break;
                case 2:
                    date.setTime(smallestts);
                    pw.print(DateParser.getIsoDate(date));
                    break;
                case 3:
                    pw.print(smallestts / 1000);
                    break;
                case 4:
                    pw.print(smallestts);
                    break;
            }

			/*
			 * interpreting BitSet. get corresponding Values from List<Value> and Print them to File. if one Value has
			 * been printed, pointer for this List is increased.
			 */
            for (int j = 0; j < pointer.length; j++) {
                if (bs.get(j)) {
                    pw.printf(" %.2f", allValues.get(j).get(pointer[j]).getValue().asDouble());
                    pointer[j]++;
                } else {
                    pw.print(" -");
                }
            }
            pw.println();

            if (Arrays.equals(pointer, limits)) { /*
												 * Export has finished, when pointer equals limits.
												 */
                pw.close();
                return;
            }

        } while (true);
    }
}
