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

import org.openmuc.framework.data.DoubleValue;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.dataaccess.Channel;
import org.openmuc.framework.dataaccess.DataAccessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * Client connection handler. For each connected client a Connection object will be created. Handles the client
 * requests.<br>
 * <p/>
 * The following commands are currently supported: <li>
 * ?org.openmuc.core.datamanager.dataserver <li>?get [label] <li>?get_age [label] <li>
 * ?get_value <li>?set_value <li>?get_values <li>?configure_report <li>
 * ?get_directory <li>?disconnect
 * <p/>
 * <br>
 * <br>
 * FIXME Reporting doesn't work with the current version!
 */
public class Connection extends Thread {

    private static Logger logger = LoggerFactory.getLogger(Connection.class);

    private final Socket sock;
    private final InputStream input;
    private final OutputStream output;
    private final BufferedReader br;
    private final PrintWriter out;
    private final AsciiSocketServer server;
    private boolean running = true;
    private final Vector<Report> reports = new Vector<Report>();

    private static int reportHandle = 0;

    private final DataAccessService dataAccessService;

    public Connection(DataAccessService dataAccessService, Socket sock, AsciiSocketServer server)
            throws IOException {
        this.sock = sock;
        this.server = server;

        input = sock.getInputStream();
        output = sock.getOutputStream();

        this.dataAccessService = dataAccessService;

        br = new BufferedReader(new InputStreamReader(input));
        out = new PrintWriter(output, true);
        this.sock.setSoTimeout(60000);

    }

    public void close() {
        // TODO ?`Wofuer wird diese Funktion verwendet ?
        try {
            sock.close();
        }
        catch (IOException e) {
            logger.error(e.getMessage());
        }
        // this.interrupt();
    }

    @Override
    public void run() {
        String msg;

        while (!sock.isInputShutdown() && (running == true)) {

            try {
                Thread.sleep(100);
            }
            catch (InterruptedException e1) {
                e1.printStackTrace();
            }

            try {
                msg = br.readLine();
                if (msg != null) {
                    logger.info("REQUEST: " + msg);

                    if (msg.startsWith("?org.openmuc.mux.dataserver")) {
                        out.println("!org.openmuc.mux.dataserver_response");
                    } else if (msg.startsWith("?get ")) {
                        getReply(msg);
                    } else if (msg.startsWith("?get_age ")) {
                        getAge(msg);
                    } else if (msg.startsWith("?get_value ")) {
                        getValueReply(msg);
                    } else if (msg.startsWith("?set_value ")) {
                        setValueReply(msg);
                    } else if (msg.startsWith("?get_all_values")) {
                        getAllValuesReply(msg);
                    } else if (msg.startsWith("?get_values ")) {
                        getValuesReply(msg);
                    } else if (msg.startsWith("?configure_report ")) {
                        configureReport(msg);
                    } else if (msg.startsWith("?get_directory")) {
                        getDirectoryReply(msg);
                    } else if (msg.startsWith("?disconnect")) {
                        out.println("!DISCONNECTED");
                        sock.close();
                        running = false;
                    } else {
                        out.println("!ERROR");
                    }
                } else {
                    logger.info("CLOSE SOCKET");
                    sock.close();
                    running = false;
                }
            }
            catch (InterruptedIOException iioe) {
                try {
                    sock.close();
                }
                catch (IOException e) {
                    logger.error(e.getMessage());
                }
                logger.info("Timeout occurred (60s) - killing connection");
                break;
            }
            catch (Exception e) {
                logger.info(e.getMessage());
                running = false;
            }

			/* create reports on demand */
            // TODO Implement reporting with timer / separate thread
            // if (store.getStore() != null) {
            // for (Report report : reports) {
            // report.emitReport(out);
            // }
            // }
            // else {
            // if (reports.size() > 0) reports.clear();
            // }

        }

        logger.info("CONNECTION CLOSED!");
        server.connectionClosed(this);
    }

    private void getAllValuesReply(String msg) {
        List<String> directory = dataAccessService.getAllIds();

        out.print("!GET_ALL_VALUES_RESPONSE");

        for (String label : directory) {
            Channel dataChannel = dataAccessService.getChannel(label);

            if (dataChannel != null) {
                out.print(" " + label + ":" + dataChannel.getLatestRecord().getValue());
            }
        }

        out.println();

    }

    private void setValueReply(String msg) {
        StringTokenizer st = new StringTokenizer(msg);
        String label, value;

        st.nextToken(); // Skip command
        label = st.nextToken();
        value = st.nextToken();

        if ((label == null) || (value == null)) {
            out.println("!ERROR: argument missing!");
            return;
        }

        Channel channel = dataAccessService.getChannel(label);

        if (channel != null) {

            double dblVal = Double.valueOf(value);

            channel.write(new DoubleValue(dblVal));
        } else {
            out.println("!ERROR: label not found!");
            return;
        }

        out.println("!OK");
    } /* setValueReply() */

    private void getAge(String msg) {
        StringTokenizer st = new StringTokenizer(msg);
        String label;

        try {
            st.nextToken(); // Skip command

            label = st.nextToken();

            logger.debug("?get_age label: (" + label + ")");

            long lastUpdate;
            long age;

            Channel channel = dataAccessService.getChannel(label);

            lastUpdate = channel.getLatestRecord().getTimestamp();

            age = System.currentTimeMillis() - lastUpdate;

            out.println("!GET_AGE_RESPONSE " + label + " " + age + " ms");

        }
        catch (Exception e) {
            out.println("!ERROR");
            logger.error(e.getMessage());
        }
    }

    private void configureReport(String msg) {
        StringTokenizer st = new StringTokenizer(msg);

        try {
            Report report;
            String type;

            st.nextToken(); // Skip command

            type = st.nextToken();

            if (type.equals("periodic")) {
                String interval = st.nextToken();

                int intvl = Integer.parseInt(interval);

                report = new PeriodicReport(reportHandle++, intvl);
            } else {
                out.println("!ERROR: Unknown report type!");
                return;
            }

            String label;

            while (st.hasMoreTokens()) {
                label = st.nextToken();
                Channel storage = dataAccessService.getChannel(label);

                if (storage != null) {
                    report.addDataStorage(storage);
                } else {
                    out.println("!ERROR: label not known!");
                    return;
                }
            }

            reports.add(report);

            out.println("!CONFIGURE_REPORT_RESPONSE handle:" + report.getHandle());
        }
        catch (Exception e) {
            out.println("!ERROR");
            logger.error(e.getMessage());
        }
    }

    private void getReply(String msg) {
        StringTokenizer st = new StringTokenizer(msg);
        String label;

        try {
            st.nextToken(); // Skip command

            label = st.nextToken();

            logger.debug("?get label: (" + label + ")");

            Channel channel = dataAccessService.getChannel(label);

            Record value = channel.getLatestRecord();

            if (value != null) {
                out.println("!GET_RESPONSE " + label + " " + value.getValue());
            } else {
                out.println("!ERROR: Value not found!");
            }

        }
        catch (Exception e) {
            out.println("!ERROR");
            logger.error(e.getMessage());
        }
    }

    private void getValueReply(String msg) {
        StringTokenizer st = new StringTokenizer(msg);
        String label;
        String timestamp;

        st.nextToken(); // Skip command

        label = st.nextToken();
        timestamp = st.nextToken();

        logger.debug("?get_value label: (" + label + ") at time " + timestamp);

        Channel channel = dataAccessService.getChannel(label);

        Record value = null;
        try {
            value = channel.getLoggedRecord(Long.parseLong(timestamp));
        }
        catch (Exception e) {
            out.println("!ERROR");
            logger.error(e.getMessage());
        }

        if (value != null) {
            out.println("!GET_VALUE_RESPONSE " + label + " " + value.getValue());
        } else {
            out.println("!ERROR: Value not found!");
        }

    }

    private void getValuesReply(String msg) {
        StringTokenizer st = new StringTokenizer(msg);
        String label;
        String startTime;
        String endTime;

        try {
            st.nextToken(); // Skip command

            label = st.nextToken();

            startTime = st.nextToken();

            logger.debug("?get_values label: (" + label + ") at time " + startTime);

            Channel storage = dataAccessService.getChannel(label);

            List<Record> values;

            System.out.println("got storage for label: " + label);

            if (storage == null) {
                System.out.println("Storage == null !!!");
            }

            System.out.println("storage: " + storage.toString());

            if (st.hasMoreTokens()) {
                endTime = st.nextToken();
                values = storage.getLoggedRecords(Long.parseLong(startTime),
                                                  Long.parseLong(endTime));
                System.out.printf("getValues(%d,%d)",
                                  Long.parseLong(startTime),
                                  Long.parseLong(endTime));
            } else {
                values = storage.getLoggedRecords(Long.parseLong(startTime));
                System.out.printf("getValues(%d)", Long.parseLong(startTime));
            }
            System.out.println("result size:" + values.size());

            out.print("!GET_VALUES_RESPONSE");

            for (Record value : values) {
                out.print(" (" + value.getTimestamp() + "," + value.getValue() + ")");
            }

            System.out.println("step5");

            out.println();

        }
        catch (Exception e) {
            out.println("!ERROR");
            logger.error(e.getMessage());
        }

    }

    private void getDirectoryReply(String msg) {

        List<String> directory = dataAccessService.getAllIds();

        out.print("!GET_DIRECTORY_RESPONSE");

        for (String label : directory) {
            out.print(" " + label);
        }

        out.println();

    }

}
