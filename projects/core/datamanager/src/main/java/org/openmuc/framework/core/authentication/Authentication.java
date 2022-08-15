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
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.openmuc.framework.authentication.AuthenticationService;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.useradmin.Authorization;
import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = AuthenticationService.class, scope = ServiceScope.SINGLETON)
public class Authentication implements AuthenticationService {
    private static final String DEFAULT_SHADOW_FILE_LOCATION = "conf/shadow";

    private static final Logger logger = LoggerFactory.getLogger(Authentication.class);
    private final Map<String, String> shadow = new HashMap<>();
    private String path;
    private UserAdmin userAdmin;
    private boolean userAdminInitiated;

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

    @Activate
    public void activate() {
        this.path = initPath();
        userAdminInitiated = false;
    }

    @Override
    public void register(String user, String pw, String group) {
        logger.info("register");
        pw += generateHash(user); // use the hash of the username as salt

        String hash = generateHash(pw);

        setUserHashPair(user, hash, group);
    }

    @Override
    public void registerNewUser(String user, String pw) {
        pw += generateHash(user); // use the hash of the username as salt

        String hash = generateHash(pw);

        setUserHashPair(user, hash, "normal");
        writeShadowToFile();
    }

    @Override
    public boolean login(String userName, String pw) {
        initUserAdminIfNotDone();
        // use the hash of the username as salt
        String pwToCheck = pw + generateHash(userName);

        String hash = generateHash(pwToCheck);

        User user = userAdmin.getUser("name", userName);

        return user.getProperties().get("password").equals(hash);
    }

    @Override
    public void delete(String user) {
        userAdmin.removeRole(user);

        writeShadowToFile();
    }

    @Override
    public boolean contains(String user) {
        return getAllUsers().contains(user);
    }

    @Override
    public Set<String> getAllUsers() {
        Set<String> registeredUsers = new HashSet<>();
        Role[] allRoles = getAllRoleObjects();

        for (Role role : Arrays.asList(allRoles)) {
            User user = (User) role;
            String userName = (String) user.getProperties().get("name");
            if (userName != null) {
                registeredUsers.add(userName);
            }
        }

        return registeredUsers;
    }

    private void setUserHashPair(String user, String hash, String group) {
        User newUser = (User) userAdmin.createRole(user, Role.USER);
        Group grp = (Group) userAdmin.createRole(group, Role.GROUP);

        if (grp == null) {
            grp = (Group) userAdmin.getRole(group);
        }

        if (newUser == null) {
            newUser = (User) userAdmin.getRole(user);
        }

        @SuppressWarnings("unchecked")
        Dictionary<String, String> properties = newUser.getProperties();
        properties.put("name", user);
        properties.put("password", hash);
        properties.put("group", group);

        grp.addMember(newUser);
    }

    @Override
    public void writeShadowToFile() {
        Role[] allRoles = getAllRoleObjects();
        StringBuilder textSb = prepareStringBuilder(allRoles);

        try (Writer output = new BufferedWriter(new FileWriter(new File(path)));) {
            output.write(textSb.toString());
            output.flush();
        } catch (IOException e) {
            logger.warn("Failed to write shadow.", e);
        }

    }

    private Role[] getAllRoleObjects() {
        Role[] allUser = null;
        try {
            allUser = userAdmin.getRoles(null);
        } catch (InvalidSyntaxException e) {
            logger.error(e.getMessage());
        }

        return allUser;
    }

    private StringBuilder prepareStringBuilder(Role[] allUser) {
        StringBuilder textSb = new StringBuilder();

        for (Role role : Arrays.asList(allUser)) {
            User user = (User) role;
            if (user.getProperties().get("name") != null) {
                textSb.append(user.getProperties().get("name") + ";");
                textSb.append(user.getProperties().get("password") + ";");
                textSb.append(user.getProperties().get("group") + ";\n");
            }
        }

        return textSb;
    }

    private void loadShadowFromFile() {
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] temp = line.split(";");
                setUserHashPair(temp[0], temp[1], temp[2]);
            }
        } catch (IOException e) {
            logger.warn("Failed to load shadow.", e);
        }
    }

    @Override
    public boolean isUserAdmin(String userName) {
        User user = userAdmin.getUser("name", userName);
        Authorization loggedUser = userAdmin.getAuthorization(user);

        return loggedUser.hasRole("admin");

    }

    @Reference
    protected void setUserAdmin(UserAdmin userAdmin) {
        this.userAdmin = userAdmin;
    }

    protected void unsetUserAdmin(UserAdmin userAdmin) {

    }

    private void initUserAdminIfNotDone() {
        if (userAdminInitiated) {
            return;
        }

        File file = new File(this.path);
        if (!file.exists()) {
            register("admin", "admin", "adminGrp");
            writeShadowToFile();
        }
        else {
            loadShadowFromFile();
        }
        userAdminInitiated = true;
    }
}
