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

import org.openmuc.framework.demo.restclient.ClientStarter;
import org.openmuc.framework.demo.restclient.Option;

public class HelpClient {

	public static Boolean start(int code, String[] args) {
		switch (code) {
		case 1:
			System.out.println("This is the client for OpenMUC-Rest Server.\nWritten by: Arsalane Arrach in July 2014");
			break;
		case 2:
			System.out.println("Client version: 0.1");
			break;
		case 3:
			System.out.println("This is the client for OpenMUC-Rest Server.\nWritten by: Arsalane Arrach in July 2014");
			System.out.println("Client version: 0.1");
			break;
		case 4:
			for (Option opt : ClientStarter.optionsList) {
				System.out.println(opt.getName() + ", " + opt.getFullName());
				System.out.println("\t" + opt.getDescription());
			}
			break;
		case 8:
			return setCommand(args);
		default:
			System.out
					.println("Do not use option -m (--manual) or option -h (--help) together with -i (--info) or -v (--version)");
			return false;
		}
		return true;
	}

	private static Boolean setCommand(String[] args) {
		int i;
		for (i = 0; i < args.length; ++i) {
			if (args[i].equals("-h") || args[i].equals("--help")) {
				++i;
				break;
			}
		}
		if ((i < args.length) && !args[i].startsWith("-")) {
			Boolean found = false;
			for (Option opt : ClientStarter.optionsList) {
				if (args[i].equals(opt.getFullName()) || args[i].equals(opt.getName())) {
					found = true;
					System.out.println(opt.getName() + ", " + opt.getFullName());
					System.out.println("\t" + opt.getDescription());
					break;
				}

			}
			if (!found) {
				System.out.println(args[i] + " is unknown.");
				return false;
			}
			return true;
		}
		else {
			System.out.println("Option -h (--help) needs parameter. ");
			return false;
		}
	}

}
