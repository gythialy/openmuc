/*
 * Copyright 2011-14 Fraunhofer ISE
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

public class SettingsClient {

	private static Boolean printJson;
	private static Boolean printText;
	private static String server;
	private static String protocol;

	// TODO use https

	public static Boolean start(int code, String[] args) {
		switch (code) {
		case 0:
		case 2:
			setDefaultSettings();
			return true;
		case 1:
			setDefaultSettings();
			setPrintText(true);
			setPrintJson(false);
			return true;
		case 4:
		case 6:
			setDefaultSettings();
			return setServer(args);
		case 5:
			setDefaultSettings();
			setPrintText(true);
			setPrintJson(false);
			return setServer(args);
		default:
			System.out.println("Do not use option -j (--json) and option -t (--text) together.");
			return false;
		}
	}

	private static Boolean setServer(String[] args) {
		int i;
		for (i = 0; i < args.length; ++i) {
			if (args[i].equals("-S") || args[i].equals("--server")) {
				++i;
				break;
			}
		}
		if ((i < args.length) && !args[i].startsWith("-")) {
			SettingsClient.server = args[i];
			return true;
		}
		else {
			System.out.println("Option -S (--server) needs parameter. ");
			return false;
		}

	}

	private static void setDefaultSettings() {
		setPrintJson(true);
		setPrintText(false);
		setServer("localhost:8888");
		setProtocol("http://");
	}

	public static Boolean getPrintJson() {
		return printJson;
	}

	public static void setPrintJson(Boolean printJson) {
		SettingsClient.printJson = printJson;
	}

	public static Boolean getPrintText() {
		return printText;
	}

	public static void setPrintText(Boolean printText) {
		SettingsClient.printText = printText;
	}

	public static String getServer() {
		return server;
	}

	public static void setServer(String server) {
		SettingsClient.server = server;
	}

	public static String getProtocol() {
		return protocol;
	}

	public static void setProtocol(String protocol) {
		SettingsClient.protocol = protocol;
	}
}
