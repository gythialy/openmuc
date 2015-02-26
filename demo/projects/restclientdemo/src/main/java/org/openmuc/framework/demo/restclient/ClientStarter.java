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
package org.openmuc.framework.demo.restclient;

import org.openmuc.framework.demo.restclient.clients.*;

import java.util.ArrayList;

public class ClientStarter {

    public static String name = "/rest";
    public static ArrayList<Option> optionsList = new ArrayList<Option>();
    public static long argCode = 0;
    public static int nbrArgs = 0;

    public static void main(String[] args) {

        String[] arg = args;
        if (arg.length == 0) {
            System.out.println(
                    "No arguments.\n\t-->\ttype for help: ./openmuc-demo-restclient --manual or\n\t   \t               " + "" +
                            "./openmuc-demo-restclient -m\n ");
            return;
        }
        initOptions();
        initOptionIDs();
        parseArgs(arg);
        if (argCode == -1) {
            System.out.println("Invalide command");
            return;
        }
        initValidArgCodes(); // TODO
        isArgCodeCoherent(); // TODO

        // Action Group 1
        if (argCode <= 8) {
            HelpClient.start((int) argCode, arg);
            return;
        }

        if (((argCode & 0xFFF0) != 0) && ((argCode & 0x000F) == 0)) {

            int code1 = (int) ((argCode & 0x7000) >> 12);
            Boolean cls = SettingsClient.start(code1, arg);
            if (!cls) {
                return;
            }
            int code2 = (int) ((argCode & 0x0070) >> 4);
            System.out.println(code2);
            int code3 = (int) ((argCode & 0x0F80) >> 7);
            switch (code2) {
                case 1:
                    Boolean ch = ChannelClient.start(code3, arg);
                    if (!ch) {
                        System.out.println("Error");
                    }
                    break;
                case 2:
                    Boolean dv = DeviceClient.start(code3, arg);
                    if (!dv) {
                        System.out.println("Error");
                    }
                    break;
                case 4:

                    Boolean dr = DriverClient.start(code3, arg);
                    if (!dr) {
                        System.out.println("Error");
                    }
                    break;
                default:
                    System.out.println("Do not use option -c (--channel), option -d (--device) and option -p (--driver) together.");
                    return;
            }

        }

    }

    private static void initOptions() {

        // 1st group of options
        Option optInfo = new Option('i', "info", "Prints information about the client.");
        optionsList.add(optInfo);

        Option optVersion = new Option('v', "version", "Prints current version information.");
        optionsList.add(optVersion);

        Option optMan = new Option('m', "manual", "Prints the full help for this client.");
        optionsList.add(optMan);

        Option optHelp = new Option('h', "help", "Prints usage information about a specific command.");
        optHelp.setNbrOfParamaters(1);
        optionsList.add(optHelp);

        // 2nd group of options
        Option optChannel = new Option('c', "channel", "/rest/channel");
        optChannel.setNbrOfParamaters(2);
        optionsList.add(optChannel);

        // 3rd group of options
        Option optDevice = new Option('d', "device", "/rest/device");
        optDevice.setNbrOfParamaters(20);
        optionsList.add(optDevice);

        // 4th group of options
        Option optDriver = new Option('p', "driver", "/rest/driver ( p stands for pilote.)");
        optDriver.setNbrOfParamaters(20);
        optionsList.add(optDriver);

        Option optWrite = new Option('w', "write", "Writes values to a given channel.");
        optWrite.setNbrOfParamaters(20);
        optionsList.add(optWrite);

        Option optGet = new Option('g', "get", "Gets config informations form the channel config file");
        optGet.setNbrOfParamaters(20);
        optionsList.add(optGet);

        Option optSet = new Option('s', "set", "sets config informations in the channel config file");
        optGet.setNbrOfParamaters(20);
        optionsList.add(optSet);

        Option optAll = new Option('a', "all", "Prints all available channels for a given driver or device.");
        optionsList.add(optAll);

        Option optHistory = new Option('H', "history", "Prints history record for a given channel.");
        optHistory.setNbrOfParamaters(2);
        optionsList.add(optHistory);

        // 5th group of options

        Option optReturnText = new Option('t', "text", "Displays server answers in text/plain format.");
        optionsList.add(optReturnText);

        Option optReturnJson = new Option('j', "json", "Displays server answers in json format.");
        optionsList.add(optReturnJson);

        Option optServer = new Option('S', "server", "Sets the server and the port number.");
        optServer.setNbrOfParamaters(1);
        optionsList.add(optServer);

    }

    private static void initOptionIDs() {
        int i = 0;
        long s = 1;
        for (i = 0; i < optionsList.size(); ++i) {
            Option opt = optionsList.get(i);
            opt.setID(s);
            optionsList.set(i, opt);
            s *= 2;
        }
    }

    public static void parseArgs(String[] arg) {
        int parameterNbr = 0;
        for (String str : arg) {
            Option tmpOption = null;
            ArrayList<Option> tmpOptionsList = new ArrayList<Option>();
            if (str.startsWith("--") && (str.length() >= 4)) {
                str = str.substring(2);
                tmpOption = new Option(' ', str, "");
            } else if (str.startsWith("-") && (str.length() == 2)) {
                tmpOption = new Option(str.charAt(1), "", "");
            } else if (str.startsWith("-") && (str.length() > 2)) {
                str = str.substring(1);
                int i = 0;
                for (i = 0; i < str.length(); ++i) {
                    Option tmpOption2 = new Option(str.charAt(i), "", "");
                    tmpOptionsList.add(tmpOption2);
                }
            } else if (!str.startsWith("-") && parameterNbr > 0) {
                parameterNbr--;
            } else {
                argCode = -1;
                return;
            }

            if (tmpOption != null) {
                Boolean found = false;
                for (Option opt : optionsList) {
                    if (opt.equals(tmpOption)) {
                        found = true;
                        parameterNbr = opt.getParameterNbr();
                        argCode += opt.getID();
                        nbrArgs++;
                    }
                }
                if (!found) {
                    argCode = -1;
                    return;
                }
                found = false;
            } else if (!tmpOptionsList.isEmpty()) {
                Boolean found = false;
                for (Option tmpOpt : tmpOptionsList) {
                    for (Option opt : optionsList) {
                        if (opt.equals(tmpOpt)) {
                            found = true;
                            argCode += opt.getID();
                            nbrArgs++;
                        }
                    }
                    if (!found) {
                        argCode = -1;
                        return;
                    }
                    found = false;
                }
            }
        }
    }

    private static Boolean isArgCodeCoherent() {
        /**
         * TODO
         */
        return true;
    }

    private static void initValidArgCodes() {

    }

    public static ArrayList<String> getParameters(Option opt, String[] args) {
        ArrayList<String> paramList = new ArrayList<String>();
        int i;
        for (i = 0; i < args.length; ++i) {
            String tmp = args[i].replace("-", "");
            if (tmp.equals(String.valueOf(opt.getName())) || tmp.equals(opt.getFullName())) {
                ++i;
                break;

            }
        }
        int j = i;
        if ((i < args.length)) {
            for (j = i; j < args.length; ++j) {
                if (!args[j].startsWith("-")) {
                    paramList.add(args[j]);
                } else {
                    break;
                }
            }
            if (paramList.isEmpty()) {
                return null;
            }
            return paramList;
        } else {
            return null;
        }
    }
}
