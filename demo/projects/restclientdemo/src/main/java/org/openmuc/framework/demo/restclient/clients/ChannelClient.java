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

import java.util.ArrayList;

import org.openmuc.framework.demo.restclient.ClientStarter;
import org.openmuc.framework.demo.restclient.Option;

public class ChannelClient {

	public static Boolean start(int code, String[] args) {
		ArrayList<String> param = ClientStarter.getParameters(new Option('c', "channel", ""), args);

		switch (code) {

		case 0:
			if (param == null) {

				ServerClient.sendGET("/channel");
				return true;
			}
			else {
				for (String chId : param) {
					System.out.println(chId);
					ServerClient.sendGET("/channel/" + chId);
				}
				return true;
			}
		case 8:
			if (param == null) {

				ServerClient.sendGET("/channel");
				return true;
			}
			else {
				System.out.println("If option -a (--all) is used you do not have to specify channels.");
				return false;
			}
		case 16:
			ArrayList<String> hParam = ClientStarter.getParameters(new Option('H', "history", ""), args);
			if (param == null) {
				System.out.println("Enter at least one channelId as parameter for option -c (--channel) ");
				return false;
			}
			else if (hParam == null) {
				System.out.println("Enter at least one timeStamp (from) as parameter for option -H (--history) ");
				return false;
			}
			else if (!hParam.get(0).matches("[0-9]+")) {
				System.out.println("Enter a valid timeStamp (from) as parameter for option -H (--history) ");
				return false;
			}
			else if (hParam.size() == 2) {
				long from = Long.parseLong(hParam.get(0));
				long until = Long.parseLong(hParam.get(1));
				for (String chId : param) {
					ServerClient.sendGETHistory("/channel/" + chId, from, until);
				}
				return true;
			}
			else if (hParam.size() == 1) {
				long from = Long.parseLong(hParam.get(0));
				long until = System.currentTimeMillis();
				for (String chId : param) {
					ServerClient.sendGETHistory("/channel/" + chId, from, until);
				}
				return true;
			}
			else {
				return false;
			}
		case 1:
			ArrayList<String> wParam = ClientStarter.getParameters(new Option('w', "write", ""), args);
			if (param == null || (wParam.size() > param.size())) {
				System.out
						.println("One or more channels to write in are missing as parameter for option -c (--channel)");
				return false;
			}
			else if (wParam == null || wParam.size() < param.size()) {
				System.out.println("One or more values to write are missing as parameter for option -w (--write)");
				return false;
			}
			else {
				int i = 0;
				for (String chId : param) {
					ServerClient.sendPUT("/channel/" + chId, wParam.get(i));
					++i;
				}
				return true;
			}
		case 2:
			ArrayList<String> gParam = ClientStarter.getParameters(new Option('g', "get", ""), args);
			if (param == null || gParam.size() > param.size()) {
				System.out
						.println("One or more channels to get config information from are missing as parameter for option -c (--channel)");
				return false;
			}
			else if (param == null || gParam.size() < param.size()) {
				System.out
						.println("One or more config field names to get are missing as parameter for option -g (--get)");
				return false;
			}
			else {
				for (String chId : param) {
				}
				return true;
			}
		case 4:
			ArrayList<String> sParam = ClientStarter.getParameters(new Option('s', "set", ""), args);
			if (param == null) {
				System.out.println("Option -c (--channel) needs one channel to set config information to");
				return false;
			}
			else if (param.size() > 1) {
				System.out.println("Option -s (--set) can set only one channel at once");
				return false;
			}
			else if ((sParam.size() & 01) == 1) {
				System.out
						.println("Option -s (--set) need an even number of parameters: <Config Field Name> <Value to set> ...");
				return false;
			}
			else {
				for (String chId : param) {
				}
			}
		default:
			System.out
					.println("Do not use use the options -a (--all), -H (--history), -w (--write), -g (--get) and -s (--set) together.");
			return false;
		}

	}

}
