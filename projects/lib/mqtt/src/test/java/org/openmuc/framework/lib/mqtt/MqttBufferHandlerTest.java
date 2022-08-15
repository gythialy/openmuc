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
package org.openmuc.framework.lib.mqtt;

import java.io.File;
import java.nio.file.FileSystems;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MqttBufferHandlerTest {
    private static final String DIRECTORY = "/tmp/openmuc/buffer_handler_test";
    private static final String LOREM_IPSUM_1_KB = "Imperdiet Volutpat Sit Himenaeos Nunc Potenti Pharetra Porta Bibendum Sem Sociosqu Maecenas Vitae Metus Varius Ut Vulputate Eleifend Netus Scelerisque Ac Lobortis Mi Iaculis In Praesent Rutrum Tristique Aenean Quam Curabitur Consectetur Mattis Suscipit Ac Adipiscing Egestas Sagittis Viverra Nullam Nisi Gravida Leo Himenaeos At Quam In Gravida Rhoncus Neque Consequat Augue Faucibus Nostra In Ullamcorper Donec Nunc Conubia Hendrerit Consectetur Massa Lacinia Tempus Massa Fringilla Ut Est Condimentum Cubilia Fermentum Tincidunt Ac Eu Purus Bibendum Urna Elit Orci Phasellus Viverra Egestas Bibendum Maecenas Mauris Ultrices Elementum Quam Facilisis Mi Mauris Auctor Nibh Cubilia Erat Massa Non Leo Sodales Fames Consectetur Lorem Eros Dui Per Augue Urna Mollis Fames Nisl Sagittis Platea Sem Eget Sagittis Nulla Eget Convallis Venenatis Faucibus Enim Proin Bibendum Egestas Imperdiet Semper Id Molestie Leo Felis Metus Platea Sapien Elementum Risus Curabitur Risus Mi Morbi Pellentesque Nostra Condimentum Nisl In Suscipi";

    @AfterAll
    static void cleanUp() {
        deleteDirectory(FileSystems.getDefault().getPath(DIRECTORY).toFile());
    }

    private static void deleteDirectory(File directory) {
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

    private MqttBufferHandler getBufferHandlerWithFilePersistenceEnabled() {
        return new MqttBufferHandler(1, 2, 2, DIRECTORY);
    }

    private MqttBufferHandler getBufferHandlerWithFilePersistenceDisabled() {
        return new MqttBufferHandler(1, 0, 0, "");
    }

    @Test
    void addToRAMBuffer() {
        MqttBufferHandler bufferHandler = getBufferHandlerWithFilePersistenceEnabled();
        bufferHandler.add("test", LOREM_IPSUM_1_KB.getBytes());

        // No file created
        Assertions.assertEquals(0, bufferHandler.getBuffers().length);

        bufferHandler.removeNextMessage();
        bufferHandler.add("test", LOREM_IPSUM_1_KB.getBytes());

        // Still no file created, remove freed up space
        Assertions.assertEquals(0, bufferHandler.getBuffers().length);
    }

    @Test
    void addToFileBuffer() {
        MqttBufferHandler bufferHandler = getBufferHandlerWithFilePersistenceEnabled();
        bufferHandler.add("test", LOREM_IPSUM_1_KB.getBytes());
        bufferHandler.add("test", LOREM_IPSUM_1_KB.getBytes());

        // RAM full, file created
        Assertions.assertEquals(1, bufferHandler.getBuffers().length);
    }

    @Test
    void addToRAMBufferWithFilePersistenceDisabled() {
        MqttBufferHandler bufferHandler = getBufferHandlerWithFilePersistenceDisabled();
        bufferHandler.add("test", LOREM_IPSUM_1_KB.getBytes());
        bufferHandler.add("test", LOREM_IPSUM_1_KB.getBytes());

        // RAM full, no file created
        Assertions.assertFalse(bufferHandler.isEmpty());
        Assertions.assertEquals(0, bufferHandler.getBuffers().length);

        // RAM limit recognized, only one message stored
        bufferHandler.removeNextMessage();
        Assertions.assertTrue(bufferHandler.isEmpty());
    }

    @Test
    void isEmpty() {
        MqttBufferHandler bufferHandler = getBufferHandlerWithFilePersistenceDisabled();

        // returns true if no message was added
        Assertions.assertTrue(bufferHandler.isEmpty());

        bufferHandler.add("test", LOREM_IPSUM_1_KB.getBytes());
        // returns false if message was added
        Assertions.assertFalse(bufferHandler.isEmpty());

        bufferHandler.add("test", LOREM_IPSUM_1_KB.substring(512).getBytes());
        bufferHandler.add("test", LOREM_IPSUM_1_KB.substring(512).getBytes());
        bufferHandler.removeNextMessage();
        // returns true if not all added messages were removed
        Assertions.assertFalse(bufferHandler.isEmpty());

        bufferHandler.removeNextMessage();
        // returns true if all added messages were removed
        Assertions.assertTrue(bufferHandler.isEmpty());
    }

    @Test
    void removeNextMessage() {
        MqttBufferHandler bufferHandler = getBufferHandlerWithFilePersistenceDisabled();
        bufferHandler.add("test", LOREM_IPSUM_1_KB.substring(0, 512).getBytes());
        bufferHandler.add("test", LOREM_IPSUM_1_KB.substring(512).getBytes());

        // Oldest message is returned first
        Assertions.assertArrayEquals(LOREM_IPSUM_1_KB.substring(0, 512).getBytes(),
                bufferHandler.removeNextMessage().message);
        Assertions.assertArrayEquals(LOREM_IPSUM_1_KB.substring(512).getBytes(),
                bufferHandler.removeNextMessage().message);

        bufferHandler.add("test", LOREM_IPSUM_1_KB.substring(0, 341).getBytes());
        bufferHandler.add("test", LOREM_IPSUM_1_KB.substring(341, 682).getBytes());
        bufferHandler.add("test", LOREM_IPSUM_1_KB.substring(682).getBytes());
        bufferHandler.add("test_2", LOREM_IPSUM_1_KB.substring(0, 341).getBytes());

        // Oldest message is overriden
        Assertions.assertArrayEquals(LOREM_IPSUM_1_KB.substring(341, 682).getBytes(),
                bufferHandler.removeNextMessage().message);
        Assertions.assertArrayEquals(LOREM_IPSUM_1_KB.substring(682).getBytes(),
                bufferHandler.removeNextMessage().message);
        Assertions.assertEquals("test_2", bufferHandler.removeNextMessage().topic);
    }
}
