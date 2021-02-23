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

package org.openmuc.framework.datalogger.slotsdb;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import org.openmuc.framework.data.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FileObjectProxy {

    private static final Logger logger = LoggerFactory.getLogger(FileObjectProxy.class);

    private final File rootNode;
    private HashMap<String, FileObjectList> openFilesHM;
    private final HashMap<String, String> encodedLabels;
    private final SimpleDateFormat sdf;
    private final Date date;
    private final Timer timer;
    private List<File> days;
    private long size;

    /*
     * Flush Period in Seconds. if flush_period == 0 -> write directly to disk.
     */
    private int flush_period = 0;
    private int limit_days;
    private int limit_size;
    private int max_open_files;

    private String strCurrentDay;
    private long currentDayFirstTS;
    private long currentDayLastTS;

    /**
     * Creates an instance of a FileObjectProxy<br>
     * The rootNodePath (output folder) usually is specified in JVM flag: org.openmuc.mux.dbprovider.slotsdb.dbfolder
     * 
     * @param rootNodePath
     *            root node path
     */
    public FileObjectProxy(String rootNodePath) {
        timer = new Timer();
        date = new Date();
        sdf = new SimpleDateFormat("yyyyMMdd");

        if (!rootNodePath.endsWith("/")) {
            rootNodePath += "/";
        }
        logger.info("Storing to: " + rootNodePath);

        rootNode = new File(rootNodePath);
        rootNode.mkdirs();
        openFilesHM = new HashMap<>();
        encodedLabels = new HashMap<>();

        loadDays();

        if (SlotsDb.FLUSH_PERIOD != null) {
            flush_period = Integer.parseInt(SlotsDb.FLUSH_PERIOD);
            logger.info("Flushing Data every: " + flush_period + "s. to disk.");
            createScheduledFlusher();
        }
        else {
            logger.info("No Flush Period set. Writing Data directly to disk.");
        }

        if (SlotsDb.DATA_LIFETIME_IN_DAYS != null) {
            limit_days = Integer.parseInt(SlotsDb.DATA_LIFETIME_IN_DAYS);
            logger.info("Maximum lifetime of stored Values: " + limit_days + " Days.");
            createScheduledDeleteJob();
        }
        else {
            logger.info("Maximum lifetime of stored Values: UNLIMITED Days.");
        }

        if (SlotsDb.MAX_DATABASE_SIZE != null) {
            limit_size = Integer.parseInt(SlotsDb.MAX_DATABASE_SIZE);
            if (limit_size < SlotsDb.MINIMUM_DATABASE_SIZE) {
                limit_size = SlotsDb.MINIMUM_DATABASE_SIZE;
            }
            logger.info("Size Limit: " + limit_size + " MB.");
            createScheduledSizeWatcher();
        }
        else {
            logger.info("Size Limit: UNLIMITED MB.");
        }

        if (SlotsDb.MAX_OPEN_FOLDERS != null) {
            max_open_files = Integer.parseInt(SlotsDb.MAX_OPEN_FOLDERS);
            logger.info("Maximum open Files for Database changed to: " + max_open_files);
        }
        else {
            max_open_files = SlotsDb.MAX_OPEN_FOLDERS_DEFAULT;
            logger.info("Maximum open Files for Database is set to: " + max_open_files + " (default).");
        }
    }

    /*
     * loads a sorted list of all days in SLOTSDB. Necessary for search- and delete jobs.
     */
    private void loadDays() {
        days = new Vector<>();
        for (File f : rootNode.listFiles()) {
            if (f.isDirectory()) {
                days.add(f);
            }
        }
        days = sortFolders(days);
    }

    private List<File> sortFolders(List<File> days) {
        Collections.sort(days, new Comparator<File>() {

            @Override
            public int compare(File f1, File f2) {
                int i = 0;
                try {
                    i = Long.valueOf(sdf.parse(f1.getName()).getTime()).compareTo(sdf.parse(f2.getName()).getTime());
                } catch (ParseException e) {
                    logger.error("Error during sorting Files: Folder doesn't match yyyymmdd Format?");
                }
                return i;
            }
        });
        return days;
    }

    /**
     * Creates a Thread, that causes Data Streams to be flushed every x-seconds.<br>
     * Define flush-period in seconds with JVM flag: org.openmuc.mux.dbprovider.slotsdb.flushperiod
     */
    private void createScheduledFlusher() {
        timer.schedule(new Flusher(), flush_period * 1000l, flush_period * 1000l);
    }

    class Flusher extends TimerTask {

        @Override
        public void run() {
            try {
                flush();
            } catch (IOException e) {
                logger.error("Flushing Data failed in IOException: " + e.getMessage());
            }
        }
    }

    private void createScheduledDeleteJob() {
        timer.schedule(new DeleteJob(), SlotsDb.INITIAL_DELAY, SlotsDb.DATA_EXPIRATION_CHECK_INTERVAL);
    }

    class DeleteJob extends TimerTask {

        @Override
        public void run() {
            try {
                deleteFoldersOlderThen(limit_days);
            } catch (IOException e) {
                logger.error("Deleting old Data failed in IOException: " + e.getMessage());
            }
        }

        private void deleteFoldersOlderThen(int limit_days) throws IOException {
            Calendar limit = Calendar.getInstance();
            limit.setTimeInMillis(System.currentTimeMillis() - (86400000L * limit_days));
            Iterator<File> iterator = days.iterator();
            try {
                while (iterator.hasNext()) {
                    File curElement = iterator.next();
                    if (sdf.parse(curElement.getName()).getTime() + 86400000 < limit
                            .getTimeInMillis()) { /*
                                                   * compare folder 's oldest value to limit
                                                   */
                        logger.info("Folder: " + curElement.getName() + " is older then " + limit_days
                                + " Days. Will be deleted.");
                        deleteRecursiveFolder(curElement);
                    }
                    else {
                        /* oldest existing Folder is not to be deleted yet */
                        break;
                    }
                }
                loadDays();
            } catch (ParseException e) {
                logger.error("Error during sorting Files: Any Folder doesn't match yyyymmdd Format?");
            }
        }
    }

    private void createScheduledSizeWatcher() {
        timer.schedule(new SizeWatcher(), SlotsDb.INITIAL_DELAY, SlotsDb.DATA_EXPIRATION_CHECK_INTERVAL);
    }

    class SizeWatcher extends TimerTask {

        @Override
        public void run() {
            try {
                while ((getDiskUsage(rootNode) / 1000000 > limit_size)
                        && (days.size() >= 2)) { /*
                                                  * avoid deleting current folder
                                                  */
                    deleteOldestFolder();
                }
            } catch (IOException e) {
                logger.error("Deleting old Data failed in IOException: " + e.getMessage());
            }
        }

        private void deleteOldestFolder() throws IOException {
            if (days.size() >= 2) {
                logger.info("Exceeded Maximum Database Size: " + limit_size + " MB. Current size: " + (size / 1000000)
                        + " MB. Deleting: " + days.get(0).getCanonicalPath());
                deleteRecursiveFolder(days.get(0));
                days.remove(0);
                clearOpenFilesHashMap();
            }
        }
    }

    private synchronized void deleteRecursiveFolder(File folder) {
        if (folder.exists()) {
            for (File f : folder.listFiles()) {
                if (f.isDirectory()) {
                    deleteRecursiveFolder(f);
                    if (f.delete()) {
                        ;
                    }
                }
                else {
                    f.delete();
                }
            }
            folder.delete();
        }
    }

    /*
     * recursive function to get the size of a folder. sums up all files. needs an initial LONG to store size to.
     */
    private long getDiskUsage(File folder) throws IOException {
        size = 0;
        recursive_size_walker(folder);
        return size;
    }

    private void recursive_size_walker(File folder) throws IOException {
        for (File f : folder.listFiles()) {
            size += f.length();
            if (f.isDirectory()) {
                recursive_size_walker(f);
            }
        }
    }

    /**
     * Appends a new Value to Slots Database.
     * 
     * @param id
     *            ID
     * @param value
     *            Value
     * @param timestamp
     *            time stamp
     * @param state
     *            State
     * @param storingPeriod
     *            storing period
     * @throws IOException
     *             if an I/O error occurs.
     */
    public synchronized void appendValue(String id, double value, long timestamp, byte state, long storingPeriod)
            throws IOException {
        FileObject toStoreIn = null;

        id = encodeLabel(id);

        String strDate = getStrDate(timestamp);

        /*
         * If there is no FileObjectList for this folder, a new one will be created. (This will be the first value
         * stored for this day) Eventually existing FileObjectLists from the day before will be flushed and closed. Also
         * the Hashtable size will be monitored, to not have too many opened Filestreams.
         */
        if (!openFilesHM.containsKey(id + strDate)) {
            deleteEntryFromLastDay(timestamp, id);
            controlHashtableSize();
            FileObjectList first = new FileObjectList(rootNode.getPath() + "/" + strDate + "/" + id);
            openFilesHM.put(id + strDate, first);

            /*
             * If FileObjectList for this label does not contain any FileObjects yet, a new one will be created. Data
             * will be stored and List reloaded for next Value to store.
             */
            if (first.size() == 0) {
                toStoreIn = new FileObject(
                        rootNode.getPath() + "/" + strDate + "/" + id + "/" + timestamp + SlotsDb.FILE_EXTENSION);
                toStoreIn.createFileAndHeader(timestamp, storingPeriod);
                toStoreIn.append(value, timestamp, state);
                toStoreIn.close(); /* close() also calls flush(). */
                openFilesHM.get(id + strDate).reLoadFolder();
                return;
            }
        }

        /*
         * There is a FileObjectList for this day.
         */
        FileObjectList listToStoreIn = openFilesHM.get(id + strDate);
        if (listToStoreIn.size() > 0) {
            toStoreIn = listToStoreIn.getCurrentFileObject();

            /*
             * If StartTimeStamp is newer then the Timestamp of the value to store, this value can't be stored.
             */
            if (toStoreIn.getStartTimeStamp() > timestamp) {
                return;
            }
        }
        if (toStoreIn == null) {
            throw new IOException("\"Store in\" is null.");
        }

        /*
         * The storing Period may have changed. In this case, a new FileObject must be created.
         */
        if (toStoreIn.getStoringPeriod() == storingPeriod || toStoreIn.getStoringPeriod() == 0) {
            toStoreIn = openFilesHM.get(id + strDate).getCurrentFileObject();
            toStoreIn.append(value, timestamp, state);
            if (flush_period == 0) {
                toStoreIn.flush();
            }
            else {
                return;
            }
        }
        else {
            /*
             * Intervall changed -> create new File (if there are no newer values for this day, or file)
             */
            if (toStoreIn.getTimestampForLatestValue() < timestamp) {
                toStoreIn = new FileObject(
                        rootNode.getPath() + "/" + strDate + "/" + id + "/" + timestamp + SlotsDb.FILE_EXTENSION);
                toStoreIn.createFileAndHeader(timestamp, storingPeriod);
                toStoreIn.append(value, timestamp, state);
                if (flush_period == 0) {
                    toStoreIn.flush();
                }
                openFilesHM.get(id + strDate).reLoadFolder();
            }
        }
    }

    private String encodeLabel(String label) throws IOException {
        String encodedLabel = encodedLabels.get(label);
        if (encodedLabel == null) {
            encodedLabel = URLEncoder.encode(label, Charset.defaultCharset().toString()); // encodes label to supported
                                                                                          // String for Filenames.
            encodedLabels.put(label, encodedLabel);
        }
        return encodedLabel;
    }

    public synchronized Record read(String label, long timestamp) throws IOException {
        // label = URLEncoder.encode(label,Charset.defaultCharset().toString());
        // //encodes label to supported String for Filenames.
        label = encodeLabel(label);

        String strDate = getStrDate(timestamp);

        if (!openFilesHM.containsKey(label + strDate)) {
            controlHashtableSize();
            FileObjectList fol = new FileObjectList(rootNode.getPath() + "/" + strDate + "/" + label);
            openFilesHM.put(label + strDate, fol);
        }
        FileObject toReadFrom = openFilesHM.get(label + strDate).getFileObjectForTimestamp(timestamp);
        if (toReadFrom != null) {
            return toReadFrom.read(timestamp); // null if no value for timestamp
                                               // is available
        }
        return null;
    }

    public synchronized List<Record> read(String label, long start, long end) throws IOException {
        if (logger.isTraceEnabled()) {
            logger.trace("Called: read(" + label + ", " + start + ", " + end + ")");
        }

        List<Record> toReturn = new Vector<>();

        if (start > end) {
            logger.trace("Invalid Read Request: startTS > endTS");
            return toReturn;
        }

        if (start == end) {
            toReturn.add(read(label, start)); // let other read function handle.
            toReturn.removeAll(Collections.singleton(null));
            return toReturn;
        }
        if (end > 50000000000000L) { /*
                                      * to prevent buffer overflows. in cases of multiplication
                                      */
            end = 50000000000000L;
        }

        // label = URLEncoder.encode(label,Charset.defaultCharset().toString());
        // //encodes label to supported String for Filenames.
        label = encodeLabel(label);

        String strStartDate = getStrDate(start);
        String strEndDate = getStrDate(end);

        List<FileObject> toRead = new Vector<>();

        if (!strStartDate.equals(strEndDate)) {
            logger.trace("Reading Multiple Days. Scanning for Folders.");
            List<FileObjectList> days = new Vector<>();

            /*
             * Check for Folders matching criteria: Folder contains data between start & end timestamp. Folder contains
             * label.
             */
            String strSubfolder;
            for (File folder : rootNode.listFiles()) {
                if (folder.isDirectory()) {
                    if (isFolderBetweenStartAndEnd(folder.getName(), start, end)) {
                        if (Arrays.asList(folder.list()).contains(label)) {
                            strSubfolder = rootNode.getPath() + "/" + folder.getName() + "/" + label;
                            days.add(new FileObjectList(strSubfolder));
                            logger.trace(strSubfolder + " contains " + SlotsDb.FILE_EXTENSION + " files to read from.");
                        }
                    }
                }
            }
            /*
             * Sort days, because rootNode.listFiles() is unsorted. FileObjectLists MUST be sorted, otherwise data
             * output wouldn't be sorted.
             */
            Collections.sort(days, new Comparator<FileObjectList>() {

                @Override
                public int compare(FileObjectList f1, FileObjectList f2) {
                    return Long.valueOf(f1.getFirstTS()).compareTo(f2.getFirstTS());
                }
            });

            /*
             * Create a list with all file-objects that must be read for this reading request.
             */
            if (days.size() == 0) {
                return toReturn;
            }
            else if (days.size() == 1) {
                toRead.addAll(days.get(0).getFileObjectsFromTo(start, end));
            }
            else { // days.size()>1
                toRead.addAll(days.get(0).getFileObjectsStartingAt(start));
                for (int i = 1; i < days.size() - 1; i++) {
                    toRead.addAll(days.get(i).getAllFileObjects());
                }
                toRead.addAll(days.get(days.size() - 1).getFileObjectsUntil(end));
            }
            toRead.removeAll(Collections.singleton(null));
        }
        else { // Start == End Folder -> only 1 FileObjectList must be read.
            File folder = new File(rootNode.getPath() + "/" + strStartDate + "/" + label);
            FileObjectList fol;
            if (folder.list() != null) {
                if (folder.list().length > 0) { // Are there Files in the
                                                // folder, that should be read?
                    fol = new FileObjectList(rootNode.getPath() + "/" + strStartDate + "/" + label);
                    toRead.addAll(fol.getFileObjectsFromTo(start, end));
                }
            }
        }
        logger.trace("Found " + toRead.size() + " " + SlotsDb.FILE_EXTENSION + " files to read from.");

        /*
         * Read all FileObjects: first (2nd,3rd,4th....n-1) last first and last will be read separately, to not exceed
         * timestamp range.
         */
        if (toRead != null) {
            if (toRead.size() > 1) {
                toReturn.addAll(toRead.get(0).read(start, toRead.get(0).getTimestampForLatestValue()));
                toRead.get(0).close();
                for (int i = 1; i < toRead.size() - 1; i++) {
                    toReturn.addAll(toRead.get(i).readFully());
                    toRead.get(i).close();
                }
                toReturn.addAll(
                        toRead.get(toRead.size() - 1).read(toRead.get(toRead.size() - 1).getStartTimeStamp(), end));
                toRead.get(toRead.size() - 1).close();

                /*
                 * Some Values might be null -> remove
                 */
                toReturn.removeAll(Collections.singleton(null));

            }
            else if (toRead.size() == 1) { // single FileObject
                toReturn.addAll(toRead.get(0).read(start, end));
                toReturn.removeAll(Collections.singleton(null));
            }
        }
        logger.trace("Selected " + SlotsDb.FILE_EXTENSION + " files contain " + toReturn.size() + " Values.");
        return toReturn;
    }

    /**
     * Parses a Timestamp in Milliseconds from a String in yyyyMMdd Format <br>
     * e.g.: 25.Sept.2011: 20110925 <br>
     * would return: 1316901600000 ms. equal to (25.09.2011 - 00:00:00) <br>
     * 
     * @param name
     *            in "yyyyMMdd" Format
     * @param start
     *            start time stamp
     * @param end
     *            ens time stamp
     * @return boolean true if yes else false
     */
    private boolean isFolderBetweenStartAndEnd(String name, long start, long end) {
        try {
            sdf.parse(name);
        } catch (ParseException e) {
            logger.error("Unable to parse Timestamp from: " + name + " folder. " + e.getMessage());
        }
        if (start <= sdf.getCalendar().getTimeInMillis() + 86399999 && sdf.getCalendar().getTimeInMillis() <= end) { // if
                                                                                                                     // start
                                                                                                                     // <=
                                                                                                                     // folder.lastTSofDay
                                                                                                                     // &&
                                                                                                                     // folder.firstTSofDay
                                                                                                                     // <=
                                                                                                                     // end
            return true;
        }
        return false;
    }

    /*
     * strCurrentDay holds the current Day in yyyyMMdd format, because SimpleDateFormat uses a lot cpu-time.
     * currentDayFirstTS and ... currentDayLastTS mark the first and last timestamp of this day. If a TS exceeds this
     * range, strCurrentDay, currentDayFirstTS, currentDayLastTS will be updated.
     */
    private String getStrDate(long timestamp) throws IOException {
        if (strCurrentDay != null) {
            if (timestamp >= currentDayFirstTS && timestamp <= currentDayLastTS) {
                return strCurrentDay;
            }
        }
        /*
         * timestamp for other day or not initialized yet.
         */
        date.setTime(timestamp);
        strCurrentDay = sdf.format(date);
        try {
            currentDayFirstTS = sdf.parse(strCurrentDay).getTime();
        } catch (ParseException e) {
            logger.error("Unable to parse Timestamp from: " + currentDayFirstTS + " String.");
        }
        currentDayLastTS = currentDayFirstTS + 86399999;
        return strCurrentDay;
    }

    private void deleteEntryFromLastDay(long timestamp, String label) throws IOException {
        String strDate = getStrDate(timestamp - 86400000);
        if (openFilesHM.containsKey(label + strDate)) {
            /*
             * Value for new day has been registered! Close and flush all connections! Empty Hashtable!
             */
            clearOpenFilesHashMap();
            logger.info(
                    "Started logging to a new Day. <" + strDate + "> Folder has been closed and flushed completely.");
            /* reload days */
            loadDays();
        }
    }

    private void clearOpenFilesHashMap() throws IOException {
        Iterator<FileObjectList> itr = openFilesHM.values().iterator();
        while (itr.hasNext()) { // kick out everything
            itr.next().closeAllFiles();
        }
        openFilesHM = new HashMap<>();
    }

    private void controlHashtableSize() throws IOException {
        /*
         * hm.size() doesn't really represent the number of open files, because it contains FileObjectLists, which may
         * contain 1 ore more FileObjects. In most cases, there is only 1 File in a List. There will be a second File if
         * storage Intervall is reconfigured. Continuous reconfiguring of measurement points may lead to a
         * "Too many open files" Exception. In this case SlotsDb.MAX_OPEN_FOLDERS should be decreased...
         */
        if (openFilesHM.size() > max_open_files) {
            logger.debug("More then " + max_open_files
                    + " DataStreams are opened. Flushing and closing some to not exceed OS-Limit.");
            Iterator<FileObjectList> itr = openFilesHM.values().iterator();
            for (int i = 0; i < (max_open_files / 5); i++) { // randomly kick
                                                             // out some of
                                                             // the
                                                             // FileObjectLists.
                                                             // -> the needed
                                                             // ones will be
                                                             // reinitialized,
                                                             // no problem
                                                             // here.
                itr.next().closeAllFiles();
                itr.remove();
            }
        }
    }

    /**
     * Flushes all Datastreams from all FileObjectLists and FileObjects
     * 
     * @throws IOException
     *             if an I/O error occurs.
     */
    public synchronized void flush() throws IOException {

        Iterator<FileObjectList> itr = openFilesHM.values().iterator();
        while (itr.hasNext()) {
            itr.next().flush();
        }

        logger.info("Data from " + openFilesHM.size() + " Folders flushed to disk.");
    }
}
