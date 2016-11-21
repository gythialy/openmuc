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
package org.openmuc.framework.datalogger.slotsdb;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Vector;

/**
 * Class representing a folder in a SlotsDatabase.<br>
 * <br>
 * ./rootnode/20110129/ID1/1298734198000.opm <br>
 * /1298734598000.opm <br>
 * /ID2/ <br>
 * /20110130/ID1/ <br>
 * /ID2/ <br>
 * <br>
 * Usually there is only 1 File in a Folder/FileObjectList<br>
 * But there might be more then 1 file in terms of reconfiguration.<br>
 * <br>
 * 
 */
public final class FileObjectList {

    private List<FileObject> files;
    // private File folder;
    private String foldername;
    private long firstTS;
    private int size;

    /**
     * Creates a FileObjectList<br>
     * and creates a FileObject for every File
     * 
     * @param foldername
     *            name of the folder
     * @throws IOException
     *             if an I/O error occurs.
     */
    public FileObjectList(String foldername) throws IOException {
        // File folder = new File(foldername);
        this.foldername = foldername;
        reLoadFolder(foldername);
    }

    /**
     * Reloads the List
     * 
     * @param foldername
     *            containing Files
     * @throws IOException
     *             if an I/O error occurs.
     */
    public void reLoadFolder(String foldername) throws IOException {
        this.foldername = foldername;
        reLoadFolder();
    }

    /**
     * Reloads the List
     * 
     * @throws IOException
     *             if an I/O error occurs.
     */
    public void reLoadFolder() throws IOException {
        File folder = new File(foldername);

        files = new Vector<>(1);
        if (folder.isDirectory()) {
            for (File file : folder.listFiles()) {
                if (file.length() >= 16) { // otherwise is corrupted or empty
                                           // file.
                    String[] split = file.getName().split("\\.");
                    if (("." + split[split.length - 1]).equals(SlotsDb.FILE_EXTENSION)) {
                        files.add(new FileObject(file));
                    }
                }
                else {
                    file.delete();
                }
            }
            if (files.size() > 1) {
                sortList(files);
            }
        }

        size = files.size();

        /*
         * set first Timestamp for this FileObjectList if there are no files -> first TS = TS@ 00:00:00 o'clock.
         */
        if (size == 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            try {
                sdf.parse(folder.getParentFile().getName());
            } catch (ParseException e) {
                throw new IOException("Unable to parse Timestamp from folder: " + folder.getParentFile().getName()
                        + ". Expected Folder in yyyyMMdd Format!");
            }
            firstTS = sdf.getCalendar().getTimeInMillis();
        }
        else {
            firstTS = files.get(0).getStartTimeStamp();
        }
        folder = null;
    }

    /*
     * bubble sort to sort files in directory. usually there is only 1 file, might be 2... will also work for more. but
     * not very fast.
     */
    private void sortList(List<FileObject> toSort) {
        int j = 0;
        FileObject tmp;
        boolean switched = true;
        while (switched) {
            switched = false;
            j++;
            for (int i = 0; i < toSort.size() - j; i++) {
                if (toSort.get(i).getStartTimeStamp() > toSort.get(i + 1).getStartTimeStamp()) {
                    tmp = toSort.get(i);
                    toSort.set(i, toSort.get(i + 1));
                    toSort.set(i + 1, tmp);
                    switched = true;
                }
            }
        }
    }

    /**
     * Returns the last created FileObject
     * 
     * @return last created FileObject
     */
    public FileObject getCurrentFileObject() {
        return get(size - 1);
    }

    /**
     * Returns the File Object at any position in list.
     * 
     * @param position
     *            position as int
     * @return FileObject at position
     */
    public FileObject get(int position) {
        return files.get(position);
    }

    /**
     * Returns the size (Number of Files in this Folder/FileObjectList)
     * 
     * @return number of FileObjects
     */
    public int size() {
        return size;
    }

    /**
     * Closes all files in this List. This will also cause DataOutputStreams to be flushed.
     * 
     * @throws IOException
     *             if an I/O error occurs.
     */
    public void closeAllFiles() throws IOException {
        for (FileObject f : files) {
            f.close();
        }
    }

    /**
     * Returns a FileObject in this List for a certain Timestamp. If there is no FileObject containing this Value, null
     * will be returned.
     * 
     * @param timestamp
     *            the timestamp of the FileObject
     * @return FileObject of timestamp
     */
    public FileObject getFileObjectForTimestamp(long timestamp) {
        if (files.size() > 1) {
            for (FileObject f : files) {
                if (f.getStartTimeStamp() <= timestamp && f.getTimestampForLatestValue() >= timestamp) {
                    // File
                    // found!
                    return f;
                }
            }
        }
        else if (files.size() == 1) {
            if (files.get(0).getStartTimeStamp() <= timestamp
                    && files.get(0).getTimestampForLatestValue() >= timestamp) {
                // contains
                // this
                // TS
                return files.get(0);
            }
        }
        return null;
    }

    /**
     * Returns All FileObject in this List, which contain Data starting at given timestamp.
     * 
     * @param timestamp
     *            timestamp of FileObjects
     * @return list of all FileObjects with timestamp
     */
    public List<FileObject> getFileObjectsStartingAt(long timestamp) {
        List<FileObject> toReturn = new Vector<>(1);
        for (int i = 0; i < files.size(); i++) {
            if (files.get(i).getTimestampForLatestValue() >= timestamp) {
                toReturn.add(files.get(i));
            }
        }
        return toReturn;
    }

    /**
     * Returns all FileObjects in this List.
     * 
     * @return list of all FileObjects
     */
    public List<FileObject> getAllFileObjects() {
        return files;
    }

    /**
     * Returns all FileObjects which contain Data before ending at given timestamp.
     * 
     * @param timestamp
     *            time stamp
     * @return FileObject until timestamp
     */
    public List<FileObject> getFileObjectsUntil(long timestamp) {
        List<FileObject> toReturn = new Vector<>(1);
        for (int i = 0; i < files.size(); i++) {
            if (files.get(i).getStartTimeStamp() <= timestamp) {
                toReturn.add(files.get(i));
            }
        }
        return toReturn;
    }

    /**
     * Returns all FileObjects which contain Data from start to end timestamps
     * 
     * @param start
     *            start time stamp
     * @param end
     *            end time stamp
     * @return all FileObject between start and end
     */
    public List<FileObject> getFileObjectsFromTo(long start, long end) {
        List<FileObject> toReturn = new Vector<>(1);
        if (files.size() > 1) {
            for (int i = 0; i < files.size(); i++) {
                if ((files.get(i).getStartTimeStamp() <= start && files.get(i).getTimestampForLatestValue() >= start)
                        || (files.get(i).getStartTimeStamp() <= end && files.get(i).getTimestampForLatestValue() >= end)
                        || (files.get(i).getStartTimeStamp() >= start
                                && files.get(i).getTimestampForLatestValue() <= end)) {
                    // needed files.
                    toReturn.add(files.get(i));
                }
            }
        }
        else if (files.size() == 1) {
            if (files.get(0).getStartTimeStamp() <= end && files.get(0).getTimestampForLatestValue() >= start) {
                // contains
                // this
                // TS
                toReturn.add(files.get(0));
            }
        }
        return toReturn;
    }

    /**
     * Returns first recorded timestamp of oldest FileObject in this list. If List is empty, this timestamp will be set
     * to 00:00:00 o'clock
     * 
     * @return first time stamp of oldest FileObject
     */
    public long getFirstTS() {
        return firstTS;
    }

    /**
     * Flushes all FileObjects in this list.
     * 
     * @throws IOException
     *             if an I/O error occurs.
     */
    public void flush() throws IOException {
        for (FileObject f : files) {
            f.flush();
        }
    }
}
