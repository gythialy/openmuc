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

package org.openmuc.framework.driver.ehz.iec62056_21;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InterruptedIOException;

import org.openmuc.jrxtx.DataBits;
import org.openmuc.jrxtx.Parity;
import org.openmuc.jrxtx.SerialPort;
import org.openmuc.jrxtx.SerialPortBuilder;
import org.openmuc.jrxtx.StopBits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IecReceiver {

    private static Logger logger = LoggerFactory.getLogger(IecReceiver.class);
    // public static final int PROTOCOL_NORMAL = 0;
    // public static final int PROTOCOL_SECONDARY = 1;
    // public static final int PROTOCOL_HDLC = 2;
    //
    // public static final int MODE_DATA_READOUT = 0;
    // public static final int MODE_PROGRAMMING = 1;
    // public static final int MODE_BINARY_HDLC = 2;,

    private SerialPort serialPort;
    private final byte[] msgBuffer = new byte[10000];
    private final byte[] inputBuffer = new byte[2000];
    private final DataInputStream inStream;

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

    public IecReceiver(String iface) throws IOException {
        this.serialPort = SerialPortBuilder.newBuilder(iface)
                .setBaudRate(9600)
                .setDataBits(DataBits.DATABITS_7)
                .setStopBits(StopBits.STOPBITS_1)
                .setParity(Parity.EVEN)
                .build();

        inStream = new DataInputStream(serialPort.getInputStream());

        if (inStream.available() > 0) {
            inStream.skip(inStream.available());
        }
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
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
                    if (!start && input == '/') {
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
                Thread.currentThread().interrupt();
            }
        } while (!time.isEnd());

        if (time.isEnd()) {
            throw new InterruptedIOException("Timeout");
        }

        byte[] frame = new byte[bufferIndex];

        for (int i = 0; i < bufferIndex; i++) {
            frame[i] = msgBuffer[i];
        }

        return frame;
    }

    public void changeBaudrate(int baudrate) {
        try {
            logger.debug("Change baudrate to: {}.", baudrate);

            this.serialPort.setBaudRate(baudrate);
        } catch (IOException e) {
            logger.warn("Failed to change the baud rate.", e);
        }
    }

    public void close() {
        try {
            serialPort.close();
        } catch (IOException e) {
            logger.warn("Failed to close the serial port properly.", e);
        }
        serialPort = null;
    }

}
