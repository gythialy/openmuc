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
package org.openmuc.framework.webui.channelconfigurator;

import org.openmuc.framework.config.ConfigService;
import org.openmuc.framework.config.ConfigWriteException;
import org.openmuc.framework.webui.spi.ResourceLoader;
import org.osgi.service.component.ComponentContext;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

/**
 * @author Frederic Robra
 */
public class Config {
    private static final String OPENMUC_CONF = System.getProperties().containsKey("org.openmuc.framework.channelconfig") ? System
            .getProperty("org.openmuc.framework.channelconfig") : "conf/channels.xml";
    private ResourceLoader loader;
    private ConfigService configService;
    private boolean changed = false;

    public void initContext(ComponentContext context) {
        loader = new ResourceLoader(context.getBundleContext());
    }

    public void initConfigService(ConfigService service) {
        configService = service;
    }

    public ResourceLoader getLoader() throws ProcessRequestException {
        if (loader == null) {
            throw new ProcessRequestException("Context not initialized");
        }
        return loader;
    }

    public ConfigService getConfigService() throws ProcessRequestException {
        if (configService == null) {
            throw new ProcessRequestException("Config service not initialized");
        }
        return configService;
    }

    public void save() throws ProcessRequestException {
        if (!changed) {
            backupConfig();
        }
        changed = true;
        try {
            configService.writeConfigToFile();
        } catch (ConfigWriteException e) {
            throw new ProcessRequestException(e);
        }
    }

    private void backupConfig() throws ProcessRequestException {
        String date = new SimpleDateFormat("yyyy-MM-dd'_'HH:mm:ss").format(new Date());
        try {
            copy(new File(OPENMUC_CONF), new File(OPENMUC_CONF + "." + date));
        } catch (IOException e) {
            throw new ProcessRequestException(e);
        }
    }

    public List<String> getBackups() {
        File dir = new File(OPENMUC_CONF).getAbsoluteFile().getParentFile();
        File[] files = dir.listFiles(new FileFilter() {

            @Override
            public boolean accept(File file) {
                if (file.getAbsolutePath().contains(OPENMUC_CONF)) {
                    return true;
                } else {
                    return false;
                }
            }
        });
        List<String> backups = new ArrayList<String>(files.length);

        for (File file : files) {
            if (file.isFile()) {
                backups.add(file.getName());
            }
        }
        return backups;
    }

    public void restoreBackup(String name) throws ProcessRequestException {
        changed = false;
        backupConfig();
        try {
            File dir = new File(OPENMUC_CONF).getAbsoluteFile().getParentFile();
            copy(new File(dir.getAbsolutePath() + File.separatorChar + name), new File(OPENMUC_CONF));
            configService.reloadConfigFromFile();
        } catch (Exception e) {
            throw new ProcessRequestException(e);
        }
    }

    public void deleteBackup(String name) {
        File dir = new File(OPENMUC_CONF).getAbsoluteFile().getParentFile();
        (new File(dir.getAbsolutePath() + File.separatorChar + name)).delete();
    }

    public String openBackupFile(String name) throws ProcessRequestException {
        File file = new File(name);
        StringBuilder fileContents = new StringBuilder((int) file.length());
        Scanner scanner = null;
        try {
            scanner = new Scanner(file);
            String lineSeperator = System.getProperty("line.separator");
            while (scanner.hasNextLine()) {
                fileContents.append(scanner.nextLine() + lineSeperator);
            }

        } catch (FileNotFoundException e) {
            throw new ProcessRequestException(e);
        } finally {
            scanner.close();
        }
        return fileContents.toString();
    }

    private static void copy(File src, File dst) throws IOException {
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            inputStream = new FileInputStream(src);
            outputStream = new FileOutputStream(dst);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
        }
    }

}
