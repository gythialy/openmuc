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

package org.openmuc.framework.core.authentication;

import org.openmuc.framework.authentication.AuthenticationService;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public final class Authentication implements AuthenticationService {

    public String path;

    public Authentication() {
        path = System.getProperty("bundles.configuration.location");
        if (path == null) {
            path = "shadow";
        } else {
            if (path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }
            path += "/shadow";
        }
        File file = new File(path);
        if (!file.exists()) {
            register("admin", "admin");
        }
    }

    @Override
    public void register(String user, String pwd) {
        pwd += generateHash(user); // use the hash of the username as salt

        String hash = generateHash(pwd);

        setUser(user, hash);
    }

    @Override
    public boolean login(String user, String pwd) {
        boolean isAuthenticated = false;
        HashMap<String, String> shadow = getShadow();

        pwd += generateHash(user); // use the hash of the username as salt
        String hash = generateHash(pwd);
        if (shadow.containsKey(user)) {
            String storedHash = shadow.get(user);
            if (hash.equals(storedHash)) {
                isAuthenticated = true;
            }
        }

        return isAuthenticated;
    }

    @Override
    public void delete(String user) {
        HashMap<String, String> shadow = getShadow();

        shadow.remove(user);

        writeShadow(shadow);
    }

    @Override
    public boolean contains(String user) {
        HashMap<String, String> shadow = getShadow();
        return shadow.containsKey(user);
    }

    @Override
    public Set<String> getAllUsers() {
        HashMap<String, String> shadow = getShadow();

        Set<String> registeredUsers = new HashSet<String>();
        registeredUsers.addAll(Collections.unmodifiableSet(shadow.keySet()));

        return registeredUsers;
    }

    private String generateHash(String pwd) {
        StringBuilder hash = new StringBuilder();

        MessageDigest sha;
        try {
            sha = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = sha.digest(pwd.getBytes());
            char[] digits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

            for (byte hashedByte : hashedBytes) {
                hash.append(digits[(hashedByte & 0xf0) >> 4]);
                hash.append(digits[(hashedByte & 0x0f)]);
            }

        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return hash.toString();
    }

    private void setUser(String user, String hash) {
        HashMap<String, String> shadow = getShadow();
        shadow.put(user, hash);

        writeShadow(shadow);
    }

    private void writeShadow(HashMap<String, String> shadow) {
        String text = "";

        for (String key : shadow.keySet()) {
            text += key + ":" + shadow.get(key) + "\n";
        }
        try {
            Writer output = new BufferedWriter(new FileWriter(new File(path)));
            output.write(text);
            output.flush();
            output.close();
        } catch (IOException e) {
        }

    }

    private HashMap<String, String> getShadow() {
        HashMap<String, String> shadow = new HashMap<String, String>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));
            try {
                String line = "";

                while ((line = reader.readLine()) != null) {
                    String[] temp = line.split(":");

                    shadow.put(temp[0], temp[1]);
                }
            } finally {
                reader.close();
            }

        } catch (Exception e) {
        }

        return shadow;
    }
}
