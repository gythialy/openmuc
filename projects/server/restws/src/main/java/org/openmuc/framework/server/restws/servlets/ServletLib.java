package org.openmuc.framework.server.restws.servlets;

import java.io.BufferedReader;
import java.io.IOException;

public class ServletLib {

    public ServletLib() {

    }

    public String buildString(BufferedReader br) {
        StringBuilder text = new StringBuilder();
        String line;
        try {
            while ((line = br.readLine()) != null) {
                text.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return text.toString();
    }
}
