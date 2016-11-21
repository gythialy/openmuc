/*
 * Copyright 2011-16 Fraunhofer ISE
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

package org.openmuc.framework.driver.ehz.iec62056_21;

import java.io.DataInputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

public class IecReceiver {

    private static Logger logger = LoggerFactory.getLogger(IecReceiver.class);
    // public final static int PROTOCOL_NORMAL = 0;
    // public final static int PROTOCOL_SECONDARY = 1;
    // public final static int PROTOCOL_HDLC = 2;
    //
    // public final static int MODE_DATA_READOUT = 0;
    // public final static int MODE_PROGRAMMING = 1;
    // public final static int MODE_BINARY_HDLC = 2;,

    private CommPortIdentifier portId;
    private SerialPort serialPort;
    private final byte[] msgBuffer = new byte[10000];
    private final byte[] inputBuffer = new byte[2000];
    private DataInputStream inStream;

    private class Timeout extends Thread {
        private final long time;
        private boolean end;

        public Timeout(long msTimeout) {
            time = msTimeout;
            end = false;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(time);
            } catch (InterruptedException e) {
            }
            end = true;
            return;
        }

        public boolean isEnd() {
            return end;
        }
    }

    public IecReceiver(String iface) throws Exception {
        try {
            portId = CommPortIdentifier.getPortIdentifier(iface);
            serialPort = (SerialPort) portId.open("ehz_connector", 2000);

            serialPort.setSerialPortParams(9600, SerialPort.DATABITS_7, SerialPort.STOPBITS_1, SerialPort.PARITY_EVEN);

            inStream = new DataInputStream(serialPort.getInputStream());

            if (inStream.available() > 0) {
                inStream.read(inputBuffer);
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        } catch (PortInUseException e) {
            throw new Exception("Port " + iface + " in use!");
        } catch (UnsupportedCommOperationException e) {
            throw new Exception("Error setting communication parameters!");
        } catch (IOException e) {
            throw new Exception("Cannot catch output stream!");
        }

    }

    public byte[] receiveMessage(long msTimeout) throws IOException {
        Timeout time = new Timeout(msTimeout);
        time.start();

        int bufferIndex = 0;
        boolean start = false;
        boolean end = false;
        inStream.skip(inStream.available()); // inStream to current state

        do {
            if (inStream.available() > 0) {
                int read = inStream.read(inputBuffer);

                for (int i = 0; i < read; i++) {
                    byte input = inputBuffer[i];
                    if (input == '/' && !start) {
                        start = true;
                        bufferIndex = 0;
                    }
                    msgBuffer[bufferIndex] = input;
                    bufferIndex++;
                    if (input == '!' && start) {
                        end = true;
                    }
                }
            }
            if (end && start) {
                break;
            }

            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
            }
        } while (!time.isEnd());

        if (time.isEnd()) {
            throw new IOException("Timeout");
        }

        byte[] frame = new byte[bufferIndex];

        for (int i = 0; i < bufferIndex; i++) {
            frame[i] = msgBuffer[i];
        }

        return frame;
    }

    public void changeBaudrate(int baudrate) {
        try {
            logger.debug("Change baudrate to: " + baudrate);
            serialPort.setSerialPortParams(baudrate, SerialPort.DATABITS_7, SerialPort.STOPBITS_1,
                    SerialPort.PARITY_EVEN);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void close() {
        serialPort.close();
        serialPort = null;
    }

}
