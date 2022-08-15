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
import java.io.IOException;
import java.nio.file.FileSystems;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class FilePersistenceTest {
    private static final String DIRECTORY = "/tmp/openmuc/filepersistence";
    private static final String LOREM_IPSUM_1_KB = "Imperdiet Volutpat Sit Himenaeos Nunc Potenti Pharetra Porta Bibendum Sem Sociosqu Maecenas Vitae Metus Varius Ut Vulputate Eleifend Netus Scelerisque Ac Lobortis Mi Iaculis In Praesent Rutrum Tristique Aenean Quam Curabitur Consectetur Mattis Suscipit Ac Adipiscing Egestas Sagittis Viverra Nullam Nisi Gravida Leo Himenaeos At Quam In Gravida Rhoncus Neque Consequat Augue Faucibus Nostra In Ullamcorper Donec Nunc Conubia Hendrerit Consectetur Massa Lacinia Tempus Massa Fringilla Ut Est Condimentum Cubilia Fermentum Tincidunt Ac Eu Purus Bibendum Urna Elit Orci Phasellus Viverra Egestas Bibendum Maecenas Mauris Ultrices Elementum Quam Facilisis Mi Mauris Auctor Nibh Cubilia Erat Massa Non Leo Sodales Fames Consectetur Lorem Eros Dui Per Augue Urna Mollis Fames Nisl Sagittis Platea Sem Eget Sagittis Nulla Eget Convallis Venenatis Faucibus Enim Proin Bibendum Egestas Imperdiet Semper Id Molestie Leo Felis Metus Platea Sapien Elementum Risus Curabitur Risus Mi Morbi Pellentesque Nostra Condimentum Nisl In Suscipi";

    @AfterEach
    void cleanUp() {
        deleteDirectory(FileSystems.getDefault().getPath(DIRECTORY).toFile());
    }

    private void deleteDirectory(File directory) {
        if (!directory.exists()) {
            return;
        }
        for (File child : directory.listFiles()) {
            if (child.isDirectory()) {
                deleteDirectory(child);
            }
            else {
                child.delete();
            }
        }
        directory.delete();
    }

    private FilePersistence getFilePersistence() {
        return new FilePersistence(DIRECTORY, 2, 1);
    }

    private void write512Byte(FilePersistence filePersistence, String buffer) throws IOException {
        filePersistence.writeBufferToFile(buffer, LOREM_IPSUM_1_KB.substring(513).getBytes());
    }

    private void write512ByteUnique(FilePersistence filePersistence, String buffer, int id) throws IOException {
        String message = LOREM_IPSUM_1_KB.substring(514);
        message += id;
        filePersistence.writeBufferToFile(buffer, message.getBytes());
    }

    private void write1KB(FilePersistence filePersistence, String buffer) throws IOException {
        filePersistence.writeBufferToFile(buffer, LOREM_IPSUM_1_KB.substring(1).getBytes());
    }

    @Test
    void invalidMaxFileCount() {
        Exception e = Assertions.assertThrows(IllegalArgumentException.class,
                () -> new FilePersistence(DIRECTORY, 0, 1));
        Assertions.assertEquals("maxFileSize is 0", e.getMessage());
    }

    @Test
    void writeWithTooBigPayload() {
        FilePersistence filePersistence = getFilePersistence();
        // maxFileSize is 1024 Bytes, payload + newline char = 1025 Bytes
        Assertions.assertThrows(IOException.class,
                () -> filePersistence.writeBufferToFile("test", LOREM_IPSUM_1_KB.getBytes()));
    }

    @Test
    void registerBuffer() throws IOException {
        FilePersistence filePersistence = getFilePersistence();
        write1KB(filePersistence, "test");
        Assertions.assertEquals("test", filePersistence.getBuffers()[0]);
        write1KB(filePersistence, "test2");
        Assertions.assertEquals("test", filePersistence.getBuffers()[0]);
        Assertions.assertEquals("test2", filePersistence.getBuffers()[1]);
    }

    @Test
    void writeBufferToFile() throws IOException {
        FilePersistence filePersistence = getFilePersistence();
        String buffer = "test";
        File file1 = FileSystems.getDefault().getPath(DIRECTORY, buffer, "buffer.0.log").toFile();
        File file2 = FileSystems.getDefault().getPath(DIRECTORY, buffer, "buffer.1.log").toFile();
        File file3 = FileSystems.getDefault().getPath(DIRECTORY, buffer, "buffer.2.log").toFile();
        write512Byte(filePersistence, buffer); // 512 B
        Assertions.assertTrue(file1.exists() && !file2.exists() && !file3.exists());
        write512Byte(filePersistence, buffer); // 512 B + 512 B = 1024 B
        // File not full
        Assertions.assertTrue(file1.exists() && !file2.exists() && !file3.exists());
        write512Byte(filePersistence, buffer); // 1024 B + 512 B > 1024 B -> new file 512 B
        Assertions.assertTrue(file1.exists() && file2.exists() && !file3.exists());
        // maxFileCount = 2 recognized -> no new file (rotation)
        write1KB(filePersistence, buffer); // 512 B + 1024 B > 1024 B -> override file
        Assertions.assertTrue(file1.exists() && file2.exists() && !file3.exists());
    }

    @Test
    void writeRotationTwoFiles() throws IOException {
        FilePersistence filePersistence = getFilePersistence();
        String buffer = "test";
        write512Byte(filePersistence, buffer);
        write1KB(filePersistence, buffer);

        // Newline is not part of message so length is 1 Byte less
        Assertions.assertEquals(511, filePersistence.getMessage(buffer).length);
        Assertions.assertEquals(1023, filePersistence.getMessage(buffer).length);
        // buffer empty
        Assertions.assertFalse(filePersistence.fileExistsFor(buffer));

        write1KB(filePersistence, buffer); // new file 1024 B
        write512ByteUnique(filePersistence, buffer, 1); // new file 512 B
        write512ByteUnique(filePersistence, buffer, 2); // 512 B + 512 B = 1024 B
        write512ByteUnique(filePersistence, buffer, 3); // > 1024 B message is overriden
        write512ByteUnique(filePersistence, buffer, 4); // 1024 B
        write512ByteUnique(filePersistence, buffer, 5); // > 1024 B message is overriden

        Assertions.assertEquals('3', filePersistence.getMessage(buffer)[510]);
        Assertions.assertEquals('4', filePersistence.getMessage(buffer)[510]);
        Assertions.assertEquals('5', filePersistence.getMessage(buffer)[510]);
        Assertions.assertFalse(filePersistence.fileExistsFor(buffer));
    }

    @Test
    void writeRotationThreeFiles() throws IOException {
        FilePersistence filePersistence = new FilePersistence(DIRECTORY, 3, 1);
        String buffer = "test";
        write512Byte(filePersistence, buffer);
        write1KB(filePersistence, buffer);

        // Newline is not part of message so length is 1 Byte less
        Assertions.assertEquals(511, filePersistence.getMessage(buffer).length);
        Assertions.assertEquals(1023, filePersistence.getMessage(buffer).length);
        // buffer empty
        Assertions.assertFalse(filePersistence.fileExistsFor(buffer));

        write1KB(filePersistence, buffer); // new file 1024 B
        write512ByteUnique(filePersistence, buffer, 1); // new file 512 B
        write512ByteUnique(filePersistence, buffer, 2); // 512 B + 512 B = 1024 B
        write512ByteUnique(filePersistence, buffer, 3); // > 1024 B message is new file
        write512ByteUnique(filePersistence, buffer, 4); // 1024 B
        write512ByteUnique(filePersistence, buffer, 5); // > 1024 B message is overriden
        write512ByteUnique(filePersistence, buffer, 6); // 1024 B
        write512ByteUnique(filePersistence, buffer, 7); // > 1024 B message is overriden

        Assertions.assertEquals('3', filePersistence.getMessage(buffer)[510]);
        Assertions.assertEquals('4', filePersistence.getMessage(buffer)[510]);
        Assertions.assertEquals('5', filePersistence.getMessage(buffer)[510]);
        Assertions.assertEquals('6', filePersistence.getMessage(buffer)[510]);
        Assertions.assertEquals('7', filePersistence.getMessage(buffer)[510]);
        Assertions.assertFalse(filePersistence.fileExistsFor(buffer));
    }
}
