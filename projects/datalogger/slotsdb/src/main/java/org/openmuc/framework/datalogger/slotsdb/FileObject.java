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

package org.openmuc.framework.datalogger.slotsdb;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import org.openmuc.framework.data.DoubleValue;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.Record;

public final class FileObject {

    private long startTimeStamp; // byte 0-7 in file (cached)
    private long storagePeriod; // byte 8-15 in file (cached)
    private final File dataFile;
    private DataOutputStream dos;
    private BufferedOutputStream bos;
    private FileOutputStream fos;
    private DataInputStream dis;
    private FileInputStream fis;
    private boolean canWrite;
    private boolean canRead;
    /*
     * File length will be cached to avoid system calls an improve I/O Performance
     */
    private long length = 0;

    public FileObject(String filename) throws IOException {
        canWrite = false;
        canRead = false;
        dataFile = new File(filename);
        length = dataFile.length();
        if (dataFile.exists() && length >= 16) {
            /*
             * File already exists -> get file Header (startTime and step-frequency) TODO: compare to starttime and
             * frequency in constructor! new file needed? update to file-array!
             */
            try {
                fis = new FileInputStream(dataFile);
                try {
                    dis = new DataInputStream(fis);
                    try {
                        startTimeStamp = dis.readLong();
                        storagePeriod = dis.readLong();
                    } finally {
                        if (dis != null) {
                            dis.close();
                            dis = null;
                        }
                    }
                } finally {
                    if (dis != null) {
                        dis.close();
                        dis = null;
                    }
                }
            } finally {
                if (fis != null) {
                    fis.close();
                    fis = null;
                }
            }
        }
    }

    public FileObject(File file) throws IOException {
        canWrite = false;
        canRead = false;
        dataFile = file;
        length = dataFile.length();
        if (dataFile.exists() && length >= 16) {
            /*
             * File already exists -> get file Header (startTime and step-frequency)
             */
            fis = new FileInputStream(dataFile);
            try {
                dis = new DataInputStream(fis);
                try {
                    startTimeStamp = dis.readLong();
                    storagePeriod = dis.readLong();
                } finally {
                    if (dis != null) {
                        dis.close();
                        dis = null;
                    }
                }
            } finally {
                if (fis != null) {
                    fis.close();
                    fis = null;
                }
            }

        }
    }

    private void enableOutput() throws IOException {
        /*
         * Close Input Streams, for enabling output.
         */
        if (dis != null) {
            dis.close();
            dis = null;
        }
        if (fis != null) {
            fis.close();
            fis = null;
        }

        /*
         * enabling output
         */
        if (fos == null || dos == null || bos == null) {
            fos = new FileOutputStream(dataFile, true);
            bos = new BufferedOutputStream(fos);
            dos = new DataOutputStream(bos);
        }
        canRead = false;
        canWrite = true;
    }

    private void enableInput() throws IOException {
        /*
         * Close Output Streams for enabling input.
         */
        if (dos != null) {
            dos.flush();
            dos.close();
            dos = null;
        }
        if (bos != null) {
            bos.close();
            bos = null;
        }
        if (fos != null) {
            fos.close();
            fos = null;
        }

        /*
         * enabling input
         */
        if (fis == null || dis == null) {
            fis = new FileInputStream(dataFile);
            dis = new DataInputStream(fis);
        }
        canWrite = false;
        canRead = true;
    }

    /**
     * Return the Timestamp of the first stored Value in this File.
     * 
     * @return timestamp as long
     */
    public long getStartTimeStamp() {
        return startTimeStamp;
    }

    /**
     * Returns the step frequency in seconds.
     * 
     * @return step frequency in seconds
     */
    public long getStoringPeriod() {
        return storagePeriod;
    }

    /**
     * creates the file, if it doesn't exist.
     * 
     * @param startTimeStamp
     *            for file header
     * @param stepIntervall
     *            for file header
     * @throws IOException
     *             if an I/O error occurs.
     */
    public void createFileAndHeader(long startTimeStamp, long stepIntervall) throws IOException {
        if (!dataFile.exists() || length < 16) {
            dataFile.getParentFile().mkdirs();
            if (dataFile.exists() && length < 16) {
                dataFile.delete(); // file corrupted (header shorter that 16
            }
            // bytes)
            dataFile.createNewFile();
            this.startTimeStamp = startTimeStamp;
            storagePeriod = stepIntervall;

            /*
             * Do not close Output streams, because after writing the header -> data will follow!
             */
            fos = new FileOutputStream(dataFile);
            bos = new BufferedOutputStream(fos);
            dos = new DataOutputStream(bos);
            dos.writeLong(startTimeStamp);
            dos.writeLong(stepIntervall);
            dos.flush();
            length += 16; /* wrote 2*8 Bytes */
            canWrite = true;
            canRead = false;
        }
    }

    public void append(double value, long timestamp, byte flag) throws IOException {
        long writePosition = getBytePosition(timestamp);
        if (writePosition == length) {
            /*
             * value for this timeslot has not been saved yet "AND" some value has been stored in last timeslot
             */
            if (!canWrite) {
                enableOutput();
            }

            dos.writeDouble(value);
            dos.writeByte(flag);
            length += 9;
        }
        else {
            if (length > writePosition) {
                /*
                 * value has already been stored for this timeslot -> handle? AVERAGE, MIN, MAX, LAST speichern?!
                 */
            }
            else {
                /*
                 * there are missing some values missing -> fill up with NaN!
                 */
                if (!canWrite) {
                    enableOutput();
                }
                long rowsToFillWithNan = (writePosition - length) / 9;// TODO:
                                                                      // stimmt
                                                                      // Berechnung?
                for (int i = 0; i < rowsToFillWithNan; i++) {
                    dos.writeDouble(Double.NaN); // TODO: festlegen welcher Wert
                                                 // undefined sein soll NaN
                                                 // ok?
                    dos.writeByte(Flag.NO_VALUE_RECEIVED_YET.getCode()); // TODO:
                                                                         // festlegen
                    // welcher Wert
                    // undefined sein
                    // soll 00 ok?
                    length += 9;
                }
                dos.writeDouble(value);
                dos.writeByte(flag);
                length += 9;
            }
        }
        /*
         * close(); OutputStreams will not be closed or flushed. Data will be written to disk after calling flush()
         * method.
         */
    }

    public long getTimestampForLatestValue() {
        return startTimeStamp + (((length - 16) / 9) - 1) * storagePeriod;
    }

    /**
     * calculates the position in a file for a certain timestamp
     * 
     * @param timestamp
     *            the searched timestamp
     * @return position the position of the timestamp
     */
    private long getBytePosition(long timestamp) {
        if (timestamp >= startTimeStamp) {

            /*
             * get position for timestamp 117 000: 117 000 - 100 000 = 17 000 17 * 000 / 5 000 = 3.4 Math.round(3.4) = 3
             * 3*(8+1) = 27 27 + 16 = 43 = position to store to!
             */
            // long pos = (Math.round((double) (timestamp - startTimeStamp) /
            // storagePeriod) * 9) + 16; /* slower */

            double pos = (double) (timestamp - startTimeStamp) / storagePeriod;
            if (pos % 1 != 0) { /* faster */
                pos = Math.round(pos);
            }
            return (long) (pos * 9 + 16);
        }
        else {
            // not in file! should never happen...
            return -1;
        }
    }

    /*
     * Calculates the closest timestamp to wanted timestamp getByteposition does a similar thing (Math.round()), for
     * byte position.
     */
    private long getClosestTimestamp(long timestamp) {
        // return Math.round((double) (timestamp -
        // startTimeStamp)/storagePeriod)*storagePeriod+startTimeStamp; /*
        // slower */

        double ts = (double) (timestamp - startTimeStamp) / storagePeriod;
        if (ts % 1 != 0) {
            ts = Math.round(ts);
        }
        return (long) ts * storagePeriod + startTimeStamp;
    }

    public Record read(long timestamp) throws IOException {
        timestamp = getClosestTimestamp(timestamp); // round to: startTimestamp
        // + n*stepIntervall
        if (timestamp >= startTimeStamp && timestamp <= getTimestampForLatestValue()) {
            if (!canRead) {
                enableInput();
            }
            fis.getChannel().position(getBytePosition(timestamp));
            Double toReturn = dis.readDouble();
            if (!Double.isNaN(toReturn)) {
                return new Record(new DoubleValue(toReturn), timestamp, Flag.newFlag(dis.readByte()));
            }
        }
        return null;
    }

    /**
     * Returns a List of Value Objects containing the measured Values between provided start and end timestamp
     * 
     * @param start
     *            start timestamp
     * @param end
     *            end timestamp
     * @return a list of records
     * @throws IOException
     *             if an I/O error occurs.
     */
    public List<Record> read(long start, long end) throws IOException {
        start = getClosestTimestamp(start); // round to: startTimestamp +
                                            // n*stepIntervall
        end = getClosestTimestamp(end); // round to: startTimestamp +
                                        // n*stepIntervall

        List<Record> toReturn = new Vector<>();

        if (start < end) {
            if (start < startTimeStamp) {
                // of this file.
                start = startTimeStamp;
            }
            if (end > getTimestampForLatestValue()) {
                end = getTimestampForLatestValue();
            }

            if (!canRead) {
                enableInput();
            }

            long timestampcounter = start;
            long startPos = getBytePosition(start);
            long endPos = getBytePosition(end);

            fis.getChannel().position(startPos);

            byte[] b = new byte[(int) (endPos - startPos) + 9];
            dis.read(b, 0, b.length);
            ByteBuffer bb = ByteBuffer.wrap(b);
            bb.rewind();

            for (int i = 0; i <= (endPos - startPos) / 9; i++) {
                double d = bb.getDouble();
                Flag s = Flag.newFlag(bb.get());
                if (!Double.isNaN(d)) {
                    toReturn.add(new Record(new DoubleValue(d), timestampcounter, s));
                }
                timestampcounter += storagePeriod;
            }

        }
        else if (start == end) {
            toReturn.add(read(start));
            toReturn.removeAll(Collections.singleton(null));
        }
        return toReturn; // Always return a list -> might be empty -> never is
                         // null, to avoid NP's
    }

    public List<Record> readFully() throws IOException {
        return read(startTimeStamp, getTimestampForLatestValue());
    }

    /**
     * Closes and Flushes underlying Input- and OutputStreams
     * 
     * @throws IOException
     *             if an I/O error occurs.
     */
    public void close() throws IOException {
        canRead = false;
        canWrite = false;
        if (dos != null) {
            dos.flush();
            dos.close();
            dos = null;
        }
        if (fos != null) {
            fos.close();
            fos = null;
        }
        if (dis != null) {
            dis.close();
            dis = null;
        }
        if (fis != null) {
            fis.close();
            fis = null;
        }
    }

    /**
     * Flushes the underlying Data Streams.
     * 
     * @throws IOException
     *             if an I/O error occurs.
     */
    public void flush() throws IOException {
        if (dos != null) {
            dos.flush();
        }
    }
}
