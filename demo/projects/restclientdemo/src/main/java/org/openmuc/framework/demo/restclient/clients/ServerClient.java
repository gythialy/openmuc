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
package org.openmuc.framework.demo.restclient.clients;

import org.openmuc.framework.demo.restclient.ClientStarter;
import org.openmuc.framework.demo.restclient.JsonHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class ServerClient {
    public static String USER_AGENT = "Rest Client";

    public static void sendGET(String string) {
        String urlStr = SettingsClient.getProtocol() + SettingsClient.getServer() + ClientStarter.name + string;
        URL url;
        try {
            url = new URL(urlStr);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", USER_AGENT);
            int responseCode = connection.getResponseCode();
            System.out.println("\nSending 'GET' request to URL : " + urlStr);

            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                StringBuffer response = new StringBuffer();
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();
                System.out.println(response.toString());
            } else {
                System.out.println("Response Code : " + responseCode);
            }

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public static void sendGETHistory(String chId, long from, long until) {
        String query = "/history?from=" + from + "&until=" + until;
        sendGET(chId + query);

    }

    public static void sendPUT(String string, String string2) {
        String urlStr = SettingsClient.getProtocol() + SettingsClient.getServer() + ClientStarter.name + string;
        URL url;
        try {
            url = new URL(urlStr);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setRequestProperty("Content-Type", "application/json");
            OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
            out.write(JsonHelper.StringValueToJson(string2));
            out.close();
            int responseCode = connection.getResponseCode();
            System.out.println("\nSending 'PUT' request to URL : " + urlStr);

            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                StringBuffer response = new StringBuffer();
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();
                System.out.println(response.toString());
            } else {
                System.out.println("Response Code : " + responseCode);
            }

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

    }

    public static void sendPUTd(String string, ArrayList<String> wParam) {
        String urlStr = SettingsClient.getProtocol() + SettingsClient.getServer() + ClientStarter.name + string;
        URL url;
        try {
            url = new URL(urlStr);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setRequestProperty("Content-Type", "application/json");
            OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
            System.out.println(JsonHelper.ChannelValuesToJson(wParam));
            out.write(JsonHelper.ChannelValuesToJson(wParam));
            out.close();
            int responseCode = connection.getResponseCode();
            System.out.println("\nSending 'PUT' request to URL : " + urlStr);

            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                StringBuffer response = new StringBuffer();
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();
                System.out.println(response.toString());
            } else {
                System.out.println("Response Code : " + responseCode);
            }

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

    }
}
