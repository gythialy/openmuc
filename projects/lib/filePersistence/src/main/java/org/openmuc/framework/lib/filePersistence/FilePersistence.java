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

package org.openmuc.framework.lib.filePersistence;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides configurable RAM friendly file persistence functionality
 */
public class FilePersistence {
    private static final Logger logger = LoggerFactory.getLogger(FilePersistence.class);
    private final Path DIRECTORY;
    private int maxFileCount;
    private final long MAX_FILE_SIZE_BYTES;
    private final Map<String, Integer> nextFile = new HashMap<>();
    private final Map<String, Long> readBytes = new HashMap<>();
    private static final List<String> BUFFERS = new ArrayList<>();
    public static final String DEFAULT_FILENAME = "buffer.0.log";
    public static final String DEFAULT_FILE_PREFIX = "buffer";
    public static final String DEFAULT_FILE_SUFFIX = "log";

    /**
     * @param directory
     *            the directory in which files are stored
     * @param maxFileCount
     *            the maximum number of files created. Must be greater than 0
     * @param maxFileSizeKb
     *            the maximum file size in kB when fileSize is reached a new file is created or the oldest overwritten
     */
    public FilePersistence(String directory, int maxFileCount, long maxFileSizeKb) {
        DIRECTORY = FileSystems.getDefault().getPath(directory);
        // convert to byte since bytes are used internally to compare with payload
        MAX_FILE_SIZE_BYTES = maxFileSizeKb * 1024;
        setMaxFileCount(maxFileCount);
        createDirectory();
    }

    private void setMaxFileCount(int maxFileCount) {
        this.maxFileCount = maxFileCount;
        if (this.maxFileCount <= 0) {
            throw new IllegalArgumentException("maxFileSize is 0");
        }
    }

    private void createDirectory() {
        if (!DIRECTORY.toFile().exists()) {
            if (!DIRECTORY.toFile().mkdirs()) {
                logger.error("The directory {} could not be created", DIRECTORY);
            }
        }
    }

    /**
     * @param buffer
     *            directory without file name. Filename is automatically added by FilePersistence
     * @param payload
     *            the data to be written. needs to be smaller than MAX_FILE_SIZE
     * @throws IOException
     *             when writing fails
     */
    public void writeBufferToFile(String buffer, byte[] payload) throws IOException {

        // buffer = topic for mqtt e.g. topic/test/openmuc

        checkPayLoadSize(payload.length);
        registerBuffer(buffer);
        Path filePath = Paths.get(DIRECTORY.toString(), buffer, DEFAULT_FILENAME);
        File file = createFileIfNotExist(filePath);
        if (isFileFull(file.length(), payload.length)) {
            handleFullFile(buffer, payload, file);
        }
        else {
            appendToFile(file, payload);
        }
    }

    private void registerBuffer(String buffer) throws IOException {
        if (!BUFFERS.contains(buffer)) {
            BUFFERS.add(buffer);
            writeBufferList();
        }
    }

    private void removeBufferIfEmpty(String buffer) throws IOException {
        if (!fileExistsFor(buffer)) {
            boolean buffersChanged = BUFFERS.remove(buffer);
            if (buffersChanged) {
                writeBufferList();
            }
        }
    }

    private void writeBufferList() throws IOException {
        File buffers = Paths.get(DIRECTORY.toString(), "buffer_list").toFile();
        FileWriter writer = new FileWriter(buffers);
        for (String registeredBuffer : BUFFERS) {
            writer.write(registeredBuffer + '\n');
        }
        writer.close();
    }

    public String[] getBuffers() {
        if (BUFFERS.isEmpty()) {
            Path buffers = Paths.get(DIRECTORY.toString(), "buffer_list");
            if (buffers.toFile().exists()) {
                try {
                    BUFFERS.addAll(Files.readAllLines(buffers));
                } catch (IOException e) {
                    logger.error("Could not read buffer_list. Message: {}", e.getMessage());
                }
            }
        }
        return BUFFERS.toArray(new String[0]);
    }

    private File createFileIfNotExist(Path filePath) throws IOException {
        File file = filePath.toFile();
        if (!file.exists()) {
            logger.info("create new file: {}", file.getAbsolutePath());
            Path storagePath = Paths.get(file.getAbsolutePath());
            Files.createDirectories(storagePath.getParent());
            Files.createFile(storagePath);
        }
        return file;
    }

    private void appendToFile(File file, byte[] payload) throws IOException {
        FileOutputStream fileStream = new FileOutputStream(file, true);
        fileStream.write(payload);
        fileStream.write("\n".getBytes());
        fileStream.close();
    }

    private void handleFullFile(String filePath, byte[] payload, File file) throws IOException {
        if (maxFileCount > 1) {
            handleMultipleFiles(filePath, payload, file);

        }
        else {
            handleSingleFile(filePath, payload, file);
        }
    }

    private void handleSingleFile(String filePath, byte[] payload, File file) {
        throw new UnsupportedOperationException("right now only maxFileCount >= 2 supported");
    }

    private void handleMultipleFiles(String buffer, byte[] payload, File file) throws IOException {

        int nextFile = this.nextFile.getOrDefault(buffer, 1);
        String newFileName = DEFAULT_FILE_PREFIX + '.' + nextFile + '.' + DEFAULT_FILE_SUFFIX;
        if (++nextFile == maxFileCount) {
            nextFile = 1;
        }
        this.nextFile.put(buffer, nextFile);
        Path path = Paths.get(DIRECTORY.toString(), buffer, DEFAULT_FILENAME);
        Path newPath = Paths.get(DIRECTORY.toString(), buffer, newFileName);
        Files.move(path, newPath, StandardCopyOption.REPLACE_EXISTING);

        logger.info("move file from: {} to {}", path, newPath);

        Files.createFile(path);
        appendToFile(path.toFile(), payload);
    }

    private boolean isFileFull(long fileLength, int payloadLength) {
        return fileLength + payloadLength + 1 > MAX_FILE_SIZE_BYTES;
    }

    private void checkPayLoadSize(int payloadLength) throws IOException {
        if (payloadLength >= MAX_FILE_SIZE_BYTES) {
            throw new IOException("Payload is bigger than maxFileSize. Current maxFileSize is "
                    + (MAX_FILE_SIZE_BYTES / 1024) + "kB");
        }
    }

    /**
     * @param buffer
     *            the name of the buffer (e.g. the topic or queue name)
     * @return if a file buffer exists
     */
    public boolean fileExistsFor(String buffer) {
        String fileName = DEFAULT_FILE_PREFIX + ".0." + DEFAULT_FILE_SUFFIX;
        return Paths.get(DIRECTORY.toString(), buffer, fileName).toFile().exists();
    }

    public byte[] getMessage(String buffer) {
        Path filePath = getOldestFilePath(buffer);
        Long position = getFilePosition(filePath.toString());
        String message = "";
        try {
            message = readLine(buffer, filePath.toFile(), position);
        } catch (IOException e) {
            logger.error("An error occurred while reading the buffer {}. Error message: {}", buffer, e.getMessage());
        }

        return message.getBytes();
    }

    private String readLine(String buffer, File file, Long position) throws IOException {
        FileInputStream fileStream = new FileInputStream(file);
        StringBuilder lineBuilder = new StringBuilder();
        fileStream.skip(position);

        int nextChar = fileStream.read();
        position++;
        while (nextChar != -1 && nextChar != '\n') {
            lineBuilder.appendCodePoint(nextChar);
            nextChar = fileStream.read();
            position++;
        }
        fileStream.close();

        setFilePosition(file.toString(), position);
        deleteIfEmpty(file, position);
        removeBufferIfEmpty(buffer);

        return lineBuilder.toString();
    }

    private void deleteIfEmpty(File file, Long position) throws IOException {
        FileInputStream fileStream = new FileInputStream(file);
        fileStream.skip(position);
        int nextChar = fileStream.read();
        fileStream.close();
        if (nextChar == -1) {
            boolean deleted = file.delete();
            if (!deleted) {
                throw new IOException("Empty file could not be deleted!");
            }
            else {
                setFilePosition(file.toString(), 0L);
            }
        }
    }

    private void setFilePosition(String filePathString, Long position) {
        readBytes.put(filePathString, position);
    }

    private Long getFilePosition(String filePathString) {
        Long position = readBytes.get(filePathString);
        if (position == null) {
            position = 0L;
        }
        return position;
    }

    private Path getOldestFilePath(String buffer) {
        Path directoryPath = Paths.get(DIRECTORY.toString(), buffer);
        String[] bufferFiles = directoryPath.toFile().list((file, s) -> s.endsWith(".log"));
        String oldestFile = DEFAULT_FILENAME;
        if (bufferFiles.length > 1) {
            oldestFile = findOldestFile(buffer);
        }
        return Paths.get(directoryPath.toString(), oldestFile);
    }

    private String findOldestFile(String buffer) {
        String oldestFile = DEFAULT_FILENAME;
        int nextFile = this.nextFile.getOrDefault(buffer, 1);
        for (int i = 0; i < maxFileCount; i++) {
            String fileName = DEFAULT_FILE_PREFIX + '.' + nextFile + '.' + DEFAULT_FILE_SUFFIX;
            if (Paths.get(DIRECTORY.toString(), buffer, fileName).toFile().exists()) {
                oldestFile = fileName;
                break;
            }
            if (++nextFile == maxFileCount) {
                nextFile = 1;
            }
        }
        return oldestFile;
    }

    public void restructure() throws IOException {
        for (String buffer : getBuffers()) {
            Path bufferPath = getOldestFilePath(buffer);
            Long position = getFilePosition(bufferPath.toString());
            if (position.equals(0L)) {
                continue;
            }
            Path temp = bufferPath.getParent();
            temp = Paths.get(temp.toString(), "temp");
            try {
                Files.move(bufferPath, temp, StandardCopyOption.REPLACE_EXISTING);
            } catch (DirectoryNotEmptyException e) {
                logger.error(bufferPath.toString() + " -> " + temp.toString());
            }
            Files.createFile(bufferPath);
            FileInputStream inputStream = new FileInputStream(temp.toFile());
            inputStream.skip(position);
            FileOutputStream outputStream = new FileOutputStream(bufferPath.toFile(), true);
            int nextChar = inputStream.read();
            while (nextChar != -1) {
                outputStream.write(nextChar);
                nextChar = inputStream.read();
            }
            inputStream.close();
            outputStream.close();
            temp.toFile().delete();
            setFilePosition(bufferPath.toString(), 0L);
        }
    }
}
