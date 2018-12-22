/*
 * Copyright 2011-18 Fraunhofer ISE
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

package org.openmuc.framework.core.authentication;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.openmuc.framework.authentication.AuthenticationService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = AuthenticationService.class, scope = ServiceScope.SINGLETON)
public class Authentication implements AuthenticationService {
    private static final String DEFAULT_SHADOW_FILE_LOCATION = "conf/shadow";

    private static final Logger logger = LoggerFactory.getLogger(Authentication.class);

    private final String path;
    private final Map<String, String> shadow = new HashMap<>();

    public Authentication() {
        this.path = initPath();
        File file = new File(this.path);

        if (!file.exists()) {
            register("admin", "admin");
        }
        else {
            loadShadowFromFile();
        }
    }

    private static String initPath() {
        String path = System.getProperty("bundles.configuration.location");
        if (path == null) {
            return DEFAULT_SHADOW_FILE_LOCATION;
        }

        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        return path + "/shadow";
    }

    @Override
    public void register(String user, String pw) {
        pw += generateHash(user); // use the hash of the username as salt

        String hash = generateHash(pw);

        setUserHashPair(user, hash);
    }

    @Override
    public boolean login(String user, String pw) {
        if (!shadow.containsKey(user)) {
            return false;
        }

        // use the hash of the username as salt
        String pwToCheck = pw + generateHash(user);

        String hash = generateHash(pwToCheck);

        String storedHash = shadow.get(user);

        return hash.equals(storedHash);
    }

    @Override
    public void delete(String user) {
        shadow.remove(user);

        writeShadowToFile();
    }

    @Override
    public boolean contains(String user) {
        return shadow.containsKey(user);
    }

    @Override
    public Set<String> getAllUsers() {
        Set<String> registeredUsers = new HashSet<>();
        registeredUsers.addAll(Collections.unmodifiableSet(shadow.keySet()));
        return registeredUsers;
    }

    private static String generateHash(String pw) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = sha256.digest(pw.getBytes());
            return bytesToHexString(hashedBytes);
        } catch (NoSuchAlgorithmException e) {
            // should not occur.
            logger.error("Failed to generate hash.", e);
            return "";
        }

    }

    private static String bytesToHexString(byte[] hashedBytes) {
        StringBuilder hash = new StringBuilder();
        for (byte hashedByte : hashedBytes) {
            hash.append(String.format("%02x", hashedByte));
        }
        return hash.toString();
    }

    private void setUserHashPair(String user, String hash) {
        shadow.put(user, hash);

        writeShadowToFile();
    }

    private void writeShadowToFile() {
        StringBuilder textSb = new StringBuilder();

        for (String key : shadow.keySet()) {
            textSb.append(key + ":" + shadow.get(key) + "\n");
        }
        try (Writer output = new BufferedWriter(new FileWriter(new File(path)));) {
            output.write(textSb.toString());
            output.flush();
        } catch (IOException e) {
            logger.warn("Failed to write shadow.", e);
        }

    }

    private void loadShadowFromFile() {
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] temp = line.split(":");

                shadow.put(temp[0], temp[1]);
            }
        } catch (IOException e) {
            logger.warn("Failed to load shadow.", e);
        }
    }
}
